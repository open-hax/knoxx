import os
import time
import subprocess
import threading
from typing import Tuple, Generator

import numpy as np
from flask import Flask, jsonify, request, Response, stream_with_context
from huggingface_hub import snapshot_download


MAX_AUDIO_DURATION_S = 25.0  # NPU KV cache can't handle very long audio

def env(name: str, default: str) -> str:
    value = os.getenv(name)
    return value if value is not None and value.strip() != "" else default


PORT = int(env("PORT", "8010"))
MODEL_DIR = env("MODEL_DIR", "/data/models")
MODEL_ID = env("WHISPER_MODEL_ID", "anubhav200/openai-whisper-small-openvino-int4")
REQUESTED_DEVICE = env("WHISPER_DEVICE", "NPU")
NPU_COMPILER_TYPE = env("WHISPER_NPU_COMPILER_TYPE", "DRIVER")

# Audio chunking for long recordings
# The NPU model (whisper-small-int4) works best with full 20-25s clips.
# Shorter chunks (e.g. 10s) cause KV-cache corruption and garbage output.
CHUNK_DURATION_S = min(
    float(env("STT_CHUNK_DURATION_S", "25.0")),
    MAX_AUDIO_DURATION_S
)
CHUNK_OVERLAP_S = float(env("STT_CHUNK_OVERLAP_S", "1.0"))
SAMPLE_RATE = 16000

def ensure_model(model_id: str, model_dir: str) -> None:
    os.makedirs(model_dir, exist_ok=True)
    expected = os.path.join(model_dir, "openvino_encoder_model.xml")
    if os.path.exists(expected):
        return

    # Download full snapshot; these OpenVINO whisper repos are small-ish (~250MB).
    snapshot_download(
        repo_id=model_id,
        local_dir=model_dir,
        local_dir_use_symlinks=False,
    )


def decode_to_f32le_16k_mono(audio_bytes: bytes) -> np.ndarray:
    """Decode arbitrary audio bytes to float32 PCM @ 16kHz, mono.

    Uses ffmpeg so we can accept browser MediaRecorder formats like webm/opus.
    """

    cmd = [
        "ffmpeg",
        "-hide_banner",
        "-loglevel",
        "error",
        "-i",
        "pipe:0",
        "-ac",
        "1",
        "-ar",
        "16000",
        "-f",
        "f32le",
        "pipe:1",
    ]
    proc = subprocess.run(
        cmd,
        input=audio_bytes,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError(
            "ffmpeg decode failed: " + proc.stderr.decode("utf-8", errors="replace")
        )

    pcm = np.frombuffer(proc.stdout, dtype=np.float32)
    if pcm.size == 0:
        raise RuntimeError("ffmpeg decode produced empty audio")
    return pcm


def list_available_devices() -> list[str]:
    try:
        import openvino as ov

        return list(ov.Core().available_devices)
    except Exception:
        return []


def load_pipeline(requested_device: str) -> Tuple[object, str, str | None]:
    import openvino_genai  # imported late so the module import failure is explicit

    init_error: str | None = None
    try:
        kwargs = {}
        if requested_device.upper() == "NPU":
            # On some installs the driver compiler path fails for Whisper; prefer compiler-in-plugin.
            kwargs["NPU_COMPILER_TYPE"] = NPU_COMPILER_TYPE

        pipe = openvino_genai.WhisperPipeline(MODEL_DIR, device=requested_device, **kwargs)
        return pipe, requested_device, None
    except Exception as e:
        init_error = repr(e)
        print(f"[stt-npu] Failed to init device={requested_device}: {init_error}")

        # If NPU init fails, fall back to CPU so the service is still usable.
        if requested_device.upper() != "CPU":
            pipe = openvino_genai.WhisperPipeline(MODEL_DIR, device="CPU")
            return pipe, "CPU", init_error

        raise


def _acquire_pipe_lock(timeout: float = 30.0) -> bool:
    """Try to acquire the NPU inference lock with a timeout."""
    return _pipe_lock.acquire(timeout=timeout)


def _is_garbage_text(text: str) -> bool:
    """Detect repetitive/garbage output that indicates a stuck NPU KV cache."""
    if not text:
        return False
    t = text.strip()
    if len(t) < 2:
        return False
    # If the text is mostly the same character repeated (e.g. "᎒ ᎒ ᎒" or "გ გ გ")
    unique_chars = set(t.replace(" ", ""))
    if len(unique_chars) <= 2 and len(t) > 10:
        return True
    # If a single non-ASCII character dominates
    ascii_count = sum(1 for c in t if c.isascii() or c.isspace())
    if ascii_count < len(t) * 0.2 and len(t) > 15:
        return True
    return False


def _recreate_pipeline() -> None:
    """Recreate the Whisper pipeline to clear a corrupted KV cache."""
    print("[stt-npu] Recreating pipeline to clear stuck KV cache…")
    try:
        pipe, device, _ = load_pipeline(REQUESTED_DEVICE)
        app.config["PIPE"] = pipe
        app.config["DEVICE"] = device
        print("[stt-npu] Pipeline recreated successfully on", device)
    except Exception as e:
        print("[stt-npu] Failed to recreate pipeline:", e)
        raise


def _generate(audio: np.ndarray) -> str:
    """Run inference while holding the lock. Caller must have acquired the lock."""
    result = app.config["PIPE"].generate(audio)
    text = getattr(result, "text", None)
    text = text.strip() if text else str(result).strip()

    # If the model produced garbage, try once more with a fresh pipeline.
    if _is_garbage_text(text):
        print("[stt-npu] Garbage detected (", text[:30], "…). Recreating pipeline and retrying…")
        _recreate_pipeline()
        result = app.config["PIPE"].generate(audio)
        text = getattr(result, "text", None)
        text = text.strip() if text else str(result).strip()
    return text


def _longest_common_suffix_prefix(a: str, b: str) -> int:
    """Return the length of the longest suffix of `a` that matches a prefix of `b`."""
    max_len = min(len(a), len(b))
    for length in range(max_len, 0, -1):
        if a[-length:] == b[:length]:
            return length
    return 0


def transcribe_chunked_gen(audio: np.ndarray) -> Generator[str, None, None]:
    """Split long audio into overlapping chunks, transcribe each, and yield."""
    total_samples = audio.shape[0]
    chunk_samples = int(CHUNK_DURATION_S * SAMPLE_RATE)
    overlap_samples = int(CHUNK_OVERLAP_S * SAMPLE_RATE)
    stride = max(1, chunk_samples - overlap_samples)

    prev_text = ""
    pos = 0

    while pos < total_samples:
        end = min(pos + chunk_samples, total_samples)
        chunk = audio[pos:end]

        acquired = _acquire_pipe_lock(timeout=30.0)
        if not acquired:
            yield " [Error: NPU busy] "
            pos += stride
            continue
        try:
            text = _generate(chunk)
        finally:
            _pipe_lock.release()

        if text:
            # Deduplicate overlap with previous segment
            overlap = _longest_common_suffix_prefix(prev_text, text)
            if overlap > 0:
                text = text[overlap:].strip()
            
            if text:
                prev_text = text # For the next overlap check
                yield text

        pos += stride


app = Flask(__name__)
# Only one NPU inference at a time to avoid KV-cache contention / hangs.
_pipe_lock = threading.Lock()


@app.get("/health")
def health():
    return jsonify(
        {
            "ok": True,
            "model_id": MODEL_ID,
            "requested_device": REQUESTED_DEVICE,
            "device": app.config.get("DEVICE", REQUESTED_DEVICE),
            "available_devices": app.config.get("AVAILABLE_DEVICES", []),
            "init_error": app.config.get("INIT_ERROR"),
        }
    )


@app.post("/transcribe")
def transcribe():
    audio_bytes = request.get_data(cache=False)
    if not audio_bytes:
        return jsonify({"detail": "Empty request body"}), 400

    start = time.time()
    try:
        audio = decode_to_f32le_16k_mono(audio_bytes)
    except Exception as e:
        return jsonify({"detail": str(e)}), 400

    duration_s = float(audio.shape[0]) / SAMPLE_RATE

    def generate_stream():
        try:
            if duration_s <= CHUNK_DURATION_S:
                # Short audio — single inference
                acquired = _acquire_pipe_lock(timeout=30.0)
                if not acquired:
                    yield f"data: {{\"error\": \"NPU busy\"}}\n\n"
                    return
                try:
                    text = _generate(audio)
                    yield f"data: {{\"text\": \"{text}\", \"final\": true}}\n\n"
                finally:
                    _pipe_lock.release()
            else:
                # Long audio — chunked transcription stream
                for segment in transcribe_chunked_gen(audio):
                    yield f"data: {{\"text\": \"{segment}\", \"final\": false}}\n\n"
                yield f"data: {{\"final\": true}}\n\n"
        except Exception as e:
            yield f"data: {{\"error\": \"{str(e)}\"}}\n\n"

    return Response(stream_with_context(generate_stream()), mimetype="text/event-stream")


def main() -> None:
    ensure_model(MODEL_ID, MODEL_DIR)
    app.config["AVAILABLE_DEVICES"] = list_available_devices()
    pipe, device, init_error = load_pipeline(REQUESTED_DEVICE)
    app.config["PIPE"] = pipe
    app.config["DEVICE"] = device
    app.config["INIT_ERROR"] = init_error
    # Bind 0.0.0.0 for docker; publish to 127.0.0.1 at compose-level.
    # threaded=True so a stuck NPU inference doesn't block new HTTP requests.
    app.run(host="0.0.0.0", port=PORT, threaded=True)


if __name__ == "__main__":
    main()

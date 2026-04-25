import os
import time
import subprocess
import threading
from typing import Tuple

import numpy as np
from flask import Flask, jsonify, request
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
CHUNK_DURATION_S = float(env("STT_CHUNK_DURATION_S", "20.0"))
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


def _generate(audio: np.ndarray) -> str:
    """Run inference while holding the lock. Caller must have acquired the lock."""
    result = app.config["PIPE"].generate(audio)
    text = getattr(result, "text", None)
    return text.strip() if text else str(result).strip()


def _longest_common_suffix_prefix(a: str, b: str) -> int:
    """Return the length of the longest suffix of `a` that matches a prefix of `b`."""
    max_len = min(len(a), len(b))
    for length in range(max_len, 0, -1):
        if a[-length:] == b[:length]:
            return length
    return 0


def transcribe_chunked(audio: np.ndarray) -> str:
    """Split long audio into overlapping chunks, transcribe each, and merge."""
    total_samples = audio.shape[0]
    chunk_samples = int(CHUNK_DURATION_S * SAMPLE_RATE)
    overlap_samples = int(CHUNK_OVERLAP_S * SAMPLE_RATE)
    stride = max(1, chunk_samples - overlap_samples)

    segments: list[str] = []
    pos = 0

    while pos < total_samples:
        end = min(pos + chunk_samples, total_samples)
        chunk = audio[pos:end]

        acquired = _acquire_pipe_lock(timeout=30.0)
        if not acquired:
            raise RuntimeError("NPU is busy processing another request. Try again in a moment.")
        try:
            text = _generate(chunk)
        finally:
            _pipe_lock.release()

        if text:
            # Deduplicate overlap with previous segment
            if segments:
                overlap = _longest_common_suffix_prefix(segments[-1], text)
                if overlap > 0:
                    text = text[overlap:].strip()
            if text:
                segments.append(text)

        pos += stride

    return " ".join(segments)


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

    try:
        if duration_s <= CHUNK_DURATION_S:
            # Short audio — single inference
            acquired = _acquire_pipe_lock(timeout=30.0)
            if not acquired:
                return jsonify({
                    "detail": "NPU is busy processing another request. Try again in a moment.",
                    "device": app.config.get("DEVICE"),
                }), 503
            try:
                text = _generate(audio)
            finally:
                _pipe_lock.release()
        else:
            # Long audio — chunked transcription
            text = transcribe_chunked(audio)
    except Exception as e:
        return jsonify({"detail": str(e), "device": app.config.get("DEVICE")}), 500

    total_s = max(0.0001, time.time() - start)
    rtf = duration_s / total_s
    return jsonify(
        {
            "text": text,
            "device": app.config.get("DEVICE"),
            "model_id": MODEL_ID,
            "duration_s": duration_s,
            "rtf": rtf,
        }
    )


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

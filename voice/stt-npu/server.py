import os
import time
import json
import subprocess
import threading
from typing import Any, Generator, Protocol, Tuple

import numpy as np
from flask import Flask, jsonify, request, Response, stream_with_context
from huggingface_hub import snapshot_download


MAX_AUDIO_DURATION_S = 25.0  # NPU KV cache can't handle very long audio


class WhisperPipelineLike(Protocol):
    def generate(self, audio: np.ndarray, **kwargs: Any) -> Any:
        ...

def env(name: str, default: str) -> str:
    value = os.getenv(name)
    return value if value is not None and value.strip() != "" else default


PORT = int(env("PORT", "8010"))
MODEL_DIR = env("MODEL_DIR", "/data/models")
MODEL_ID = env("WHISPER_MODEL_ID", "OpenVINO/whisper-medium.en-int8-ov")
TIMED_MODEL_ID = env("STT_TIMED_MODEL_ID", MODEL_ID)
TIMED_MODEL_DIR = env(
    "STT_TIMED_MODEL_DIR",
    MODEL_DIR if TIMED_MODEL_ID == MODEL_ID else f"{MODEL_DIR}-timed",
)
REQUESTED_DEVICE = env("WHISPER_DEVICE", "NPU")
NPU_COMPILER_TYPE = env("WHISPER_NPU_COMPILER_TYPE", "DRIVER")
ENABLE_WORD_TIMESTAMPS = env("STT_WORD_TIMESTAMPS", "false").lower() in {"1", "true", "yes", "on"}
TIMED_REQUESTED_DEVICE = env("STT_TIMED_DEVICE", "CPU" if ENABLE_WORD_TIMESTAMPS else REQUESTED_DEVICE)

# Audio chunking for long recordings
# The NPU model (whisper-small-int4) works best with full 20-25s clips.
# Shorter chunks (e.g. 10s) cause KV-cache corruption and garbage output.
CHUNK_DURATION_S = min(
    float(env("STT_CHUNK_DURATION_S", "25.0")),
    MAX_AUDIO_DURATION_S
)
CHUNK_OVERLAP_S = float(env("STT_CHUNK_OVERLAP_S", "1.0"))
SAMPLE_RATE = 16000
# Normalize Discord/browser voice captures before Whisper.  The filter chain is
# intentionally conservative: remove rumble/ultrasonic room noise and smooth
# uneven mic gain without changing speech timing. Set STT_AUDIO_FILTERS="" to
# disable for A/B testing.
AUDIO_FILTERS = env("STT_AUDIO_FILTERS", "highpass=f=80,lowpass=f=7800,dynaudnorm=f=150:g=15")

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
    ]
    if AUDIO_FILTERS.strip():
        cmd.extend(["-af", AUDIO_FILTERS])
    cmd.extend(["-f", "f32le", "pipe:1"])
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


def load_pipeline(requested_device: str) -> Tuple[WhisperPipelineLike, str, str | None]:
    return load_pipeline_for_model(MODEL_DIR, requested_device)


def load_pipeline_for_model(model_dir: str, requested_device: str) -> Tuple[WhisperPipelineLike, str, str | None]:
    import openvino_genai  # imported late so the module import failure is explicit

    init_error: str | None = None
    try:
        kwargs = {}
        if requested_device.upper() == "NPU":
            # On some installs the driver compiler path fails for Whisper; prefer compiler-in-plugin.
            kwargs["NPU_COMPILER_TYPE"] = NPU_COMPILER_TYPE

        # OpenVINO GenAI 2026.1 NPU fails Whisper pipeline construction when
        # word_timestamps is supplied here (rv_readers.size assertion). Keep the
        # NPU pipeline loadable and request word timestamps at generate-time.
        if ENABLE_WORD_TIMESTAMPS and requested_device.upper() != "NPU":
            kwargs["word_timestamps"] = True
        pipe = openvino_genai.WhisperPipeline(model_dir, device=requested_device, **kwargs)
        return pipe, requested_device, None
    except Exception as e:
        init_error = repr(e)
        print(f"[stt-npu] Failed to init device={requested_device}: {init_error}")

        # If NPU init fails, fall back to CPU so the service is still usable.
        if requested_device.upper() != "CPU":
            kwargs = {"word_timestamps": True} if ENABLE_WORD_TIMESTAMPS else {}
            pipe = openvino_genai.WhisperPipeline(model_dir, device="CPU", **kwargs)
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


def _get_timed_pipe() -> tuple[WhisperPipelineLike, str, str | None]:
    """Lazily load the batch/timed pipeline so realtime STT stays light."""
    pipe = app.config.get("TIMED_PIPE")
    if pipe is not None:
        return pipe, app.config.get("TIMED_DEVICE", REQUESTED_DEVICE), app.config.get("TIMED_INIT_ERROR")

    ensure_model(TIMED_MODEL_ID, TIMED_MODEL_DIR)
    pipe, device, init_error = load_pipeline_for_model(TIMED_MODEL_DIR, TIMED_REQUESTED_DEVICE)
    app.config["TIMED_PIPE"] = pipe
    app.config["TIMED_DEVICE"] = device
    app.config["TIMED_INIT_ERROR"] = init_error
    return pipe, device, init_error


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


def _result_text(result: Any) -> str:
    text = getattr(result, "text", None)
    if text:
        return text.strip()
    texts = getattr(result, "texts", None)
    if texts:
        return " ".join(str(item).strip() for item in texts if str(item).strip()).strip()
    return str(result).strip()


def _chunk_to_dict(chunk: Any, offset_s: float) -> dict:
    return {
        "start_s": float(getattr(chunk, "start_ts", 0.0)) + offset_s,
        "end_s": float(getattr(chunk, "end_ts", 0.0)) + offset_s,
        "text": str(getattr(chunk, "text", "")).strip(),
    }


def _word_to_dict(word: Any, offset_s: float) -> dict:
    return {
        "start_s": float(getattr(word, "start_ts", 0.0)) + offset_s,
        "end_s": float(getattr(word, "end_ts", 0.0)) + offset_s,
        "word": str(getattr(word, "word", "")).strip(),
    }


def _generate_timed(audio: np.ndarray, offset_s: float = 0.0) -> dict:
    """Return text plus optional segment/word timestamps for one chunk."""
    pipe, _, _ = _get_timed_pipe()
    result = pipe.generate(audio, return_timestamps=True, word_timestamps=ENABLE_WORD_TIMESTAMPS)
    text = _result_text(result)
    if _is_garbage_text(text):
        print("[stt-npu] Garbage detected in timed transcription. Recreating timed pipeline and retrying…")
        app.config.pop("TIMED_PIPE", None)
        pipe, _, _ = _get_timed_pipe()
        result = pipe.generate(audio, return_timestamps=True, word_timestamps=ENABLE_WORD_TIMESTAMPS)
        text = _result_text(result)
    chunks = getattr(result, "chunks", None) or []
    words = getattr(result, "words", None) or []
    return {
        "text": text,
        "segments": [_chunk_to_dict(chunk, offset_s) for chunk in chunks],
        "words": [_word_to_dict(word, offset_s) for word in words],
    }


def transcribe_timed_chunked(audio: np.ndarray) -> dict:
    total_samples = audio.shape[0]
    chunk_samples = int(CHUNK_DURATION_S * SAMPLE_RATE)
    overlap_samples = int(CHUNK_OVERLAP_S * SAMPLE_RATE)
    stride = max(1, chunk_samples - overlap_samples)
    chunks = []
    pos = 0
    while pos < total_samples:
        end = min(pos + chunk_samples, total_samples)
        chunk = audio[pos:end]
        offset_s = float(pos) / SAMPLE_RATE
        acquired = _acquire_pipe_lock(timeout=30.0)
        if not acquired:
            chunks.append({"offset_s": offset_s, "error": "NPU busy"})
            pos += stride
            continue
        try:
            timed = _generate_timed(chunk, offset_s=offset_s)
        finally:
            _pipe_lock.release()
        chunks.append({"offset_s": offset_s, **timed})
        pos += stride
    return {
        "text": " ".join(chunk.get("text", "") for chunk in chunks if chunk.get("text", "")).strip(),
        "chunks": chunks,
        "segments": [segment for chunk in chunks for segment in chunk.get("segments", [])],
        "words": [word for chunk in chunks for word in chunk.get("words", [])],
    }


def _longest_common_suffix_prefix(a: str, b: str) -> int:
    """Return the length of the longest suffix of `a` that matches a prefix of `b`."""
    max_len = min(len(a), len(b))
    for length in range(max_len, 0, -1):
        if a[-length:] == b[:length]:
            return length
    return 0


def _sse_event(payload: dict) -> str:
    """Encode one Server-Sent Events data line as valid JSON."""
    return "data: " + json.dumps(payload, ensure_ascii=False) + "\n\n"


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
            "timed_model_id": TIMED_MODEL_ID,
            "timed_model_loaded": app.config.get("TIMED_PIPE") is not None,
            "timed_requested_device": TIMED_REQUESTED_DEVICE,
            "requested_device": REQUESTED_DEVICE,
            "device": app.config.get("DEVICE", REQUESTED_DEVICE),
            "timed_device": app.config.get("TIMED_DEVICE"),
            "available_devices": app.config.get("AVAILABLE_DEVICES", []),
            "init_error": app.config.get("INIT_ERROR"),
            "timed_init_error": app.config.get("TIMED_INIT_ERROR"),
            "chunk_duration_s": CHUNK_DURATION_S,
            "chunk_overlap_s": CHUNK_OVERLAP_S,
            "audio_filters": AUDIO_FILTERS,
            "word_timestamps": ENABLE_WORD_TIMESTAMPS,
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
                    yield _sse_event({"error": "NPU busy"})
                    return
                try:
                    text = _generate(audio)
                    yield _sse_event({"text": text, "final": True})
                finally:
                    _pipe_lock.release()
            else:
                # Long audio — chunked transcription stream
                for segment in transcribe_chunked_gen(audio):
                    yield _sse_event({"text": segment, "final": False})
                yield _sse_event({"final": True})
        except Exception as e:
            yield _sse_event({"error": str(e)})

    return Response(stream_with_context(generate_stream()), mimetype="text/event-stream")


@app.post("/transcribe-timed")
def transcribe_timed():
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
            acquired = _acquire_pipe_lock(timeout=30.0)
            if not acquired:
                return jsonify({"detail": "NPU busy"}), 503
            try:
                result = _generate_timed(audio)
            finally:
                _pipe_lock.release()
            result["chunks"] = [{"offset_s": 0.0, **result}]
        else:
            result = transcribe_timed_chunked(audio)
    except Exception as e:
        return jsonify({"detail": str(e)}), 500

    elapsed = time.time() - start
    return jsonify(
        {
            **result,
            "model_id": TIMED_MODEL_ID,
            "realtime_model_id": MODEL_ID,
            "device": app.config.get("TIMED_DEVICE", app.config.get("DEVICE", REQUESTED_DEVICE)),
            "timed_requested_device": TIMED_REQUESTED_DEVICE,
            "realtime_device": app.config.get("DEVICE", REQUESTED_DEVICE),
            "timed_device": app.config.get("TIMED_DEVICE"),
            "timed_init_error": app.config.get("TIMED_INIT_ERROR"),
            "duration_s": duration_s,
            "rtf": elapsed / duration_s if duration_s > 0 else None,
            "return_timestamps": True,
            "word_timestamps": ENABLE_WORD_TIMESTAMPS,
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

import os
import time
import subprocess
from typing import Tuple

import numpy as np
from flask import Flask, jsonify, request
from huggingface_hub import snapshot_download


def env(name: str, default: str) -> str:
    value = os.getenv(name)
    return value if value is not None and value.strip() != "" else default


PORT = int(env("PORT", "8010"))
MODEL_DIR = env("MODEL_DIR", "/data/models")
MODEL_ID = env("WHISPER_MODEL_ID", "anubhav200/openai-whisper-small-openvino-int4")
REQUESTED_DEVICE = env("WHISPER_DEVICE", "NPU")
NPU_COMPILER_TYPE = env("WHISPER_NPU_COMPILER_TYPE", "DRIVER")


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


app = Flask(__name__)


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

    duration_s = float(audio.shape[0]) / 16000.0

    try:
        result = app.config["PIPE"].generate(audio)
        text = getattr(result, "text", None)
        if text is None:
            text = str(result)
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
    app.run(host="0.0.0.0", port=PORT)


if __name__ == "__main__":
    main()

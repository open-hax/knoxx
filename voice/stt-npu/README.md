# Knoxx STT (Whisper on Intel NPU via OpenVINO GenAI)

This is a small HTTP service that transcribes audio using **Whisper** running on **Intel NPU** through **OpenVINO GenAI**.

It is designed to be used from Knoxx via the backend proxy route:

- `POST /api/voice/stt` (Knoxx backend) → forwards to this service.

## Endpoints

- `GET /health`
  - returns `{ ok, device, model_id }`
- `POST /transcribe`
  - accepts raw audio bytes (any format ffmpeg can decode)
  - returns `{ text, device, model_id, duration_s, rtf }`

## Environment variables

- `PORT` (default `8010`)
- `WHISPER_DEVICE` (default `NPU`)
- `WHISPER_MODEL_ID` (default `anubhav200/openai-whisper-small-openvino-int4`)
- `MODEL_DIR` (default `/data/models`)

## Local test (service only)

```bash
curl -sS http://127.0.0.1:8010/health | jq

# Example: send an audio file (webm/wav/mp3 all ok)
curl -sS \
  -H 'Content-Type: audio/webm' \
  --data-binary @./sample.webm \
  http://127.0.0.1:8010/transcribe | jq
```

## Notes

- For Docker, you must pass the NPU device node (typically `/dev/accel/accel0`) and add the container user to the host `render` group (GID varies by host).
- For OpenVINO to *actually* see `NPU` inside Docker, you also need the host NPU user-space runtime libraries (Level Zero) and the driver compiler library to be visible inside the container (see `services/openplanner/.env` + compose mounts).
- The model is downloaded from HuggingFace on first boot into `MODEL_DIR`.

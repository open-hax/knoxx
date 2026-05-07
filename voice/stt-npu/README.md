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
- `WHISPER_MODEL_ID` (default `OpenVINO/whisper-medium.en-int8-ov`)
- `MODEL_DIR` (default `/data/models`)
- `STT_CHUNK_DURATION_S` (default `25.0`, capped at 25s for the NPU path)
- `STT_CHUNK_OVERLAP_S` (default `1.0`)
- `STT_AUDIO_FILTERS` (default `highpass=f=80,lowpass=f=7800,dynaudnorm=f=150:g=15`; set to empty string to disable)

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
- The default is now an English-only medium Whisper (`OpenVINO/whisper-medium.en-int8-ov`) to reduce wrong-language guesses such as Korean when the actual speech is English. If you need multilingual transcription, override `WHISPER_MODEL_ID` back to a multilingual OpenVINO Whisper model.
- For poor Discord voice transcriptions, first prefer better capture windows before changing models: keep natural short pauses in one utterance, avoid sub-second chunks, and let this sidecar normalize mic gain/noise before Whisper. If quality is still bad after that, override `WHISPER_MODEL_ID` with a larger compatible OpenVINO Whisper model and use a fresh `MODEL_DIR`.

# Knoxx STT (Whisper on Intel NPU via OpenVINO GenAI)

This is a small HTTP service that transcribes audio using **Whisper** running on **Intel NPU** through **OpenVINO GenAI**.

It is designed to be used from Knoxx via the backend proxy route:

- `POST /api/voice/stt` (Knoxx backend) → forwards to this service.

## Endpoints

- `GET /health`
  - returns `{ ok, device, model_id }`
- `POST /transcribe`
  - accepts raw audio bytes (any format ffmpeg can decode)
  - streams JSON Server-Sent Events for realtime conversational transcription
- `POST /transcribe-timed`
  - accepts raw audio bytes (any format ffmpeg can decode)
  - returns batch JSON with `{ text, chunks, segments, words, device, model_id, duration_s, rtf }`
  - `segments` are Whisper timestamp chunks; `words` is populated only when `STT_WORD_TIMESTAMPS=true`

## Environment variables

- `PORT` (default `8010`)
- `WHISPER_DEVICE` (default `NPU`)
- `WHISPER_MODEL_ID` (default `OpenVINO/whisper-medium.en-int8-ov`)
- `MODEL_DIR` (default `/data/models`)
- `STT_TIMED_MODEL_ID` (default `WHISPER_MODEL_ID`; PM2 defaults this to `OpenVINO/whisper-medium-int8-ov` for multilingual batch song/reference alignment)
- `STT_TIMED_MODEL_DIR` (default `MODEL_DIR` when `STT_TIMED_MODEL_ID=WHISPER_MODEL_ID`, otherwise `${MODEL_DIR}-timed`)
- `STT_TIMED_DEVICE` (default `CPU` when word timestamps are enabled; PM2 defaults batch song/reference alignment to CPU because OpenVINO GenAI 2026.1 NPU loads Whisper but fails timestamped timed inference)
- `STT_CHUNK_DURATION_S` (default `25.0`, capped at 25s for the NPU path)
- `STT_CHUNK_OVERLAP_S` (default `1.0`)
- `STT_AUDIO_FILTERS` (default `highpass=f=80,lowpass=f=7800,dynaudnorm=f=150:g=15`; set to empty string to disable)
- `STT_WORD_TIMESTAMPS` (default `false`; set `true` to construct the OpenVINO Whisper pipeline with word-level timestamp support for batch song/lyrics alignment)

## Local test (service only)

```bash
curl -sS http://127.0.0.1:8010/health | jq

# Example: send an audio file (webm/wav/mp3 all ok)
curl -sS \
  -H 'Content-Type: audio/webm' \
  --data-binary @./sample.webm \
  http://127.0.0.1:8010/transcribe | jq

# Batch timing endpoint for lyrics/music analysis.
curl -sS \
  -H 'Content-Type: audio/mpeg' \
  --data-binary @./song.mp3 \
  http://127.0.0.1:8010/transcribe-timed | jq
```

## Notes

- For Docker, you must pass the NPU device node (typically `/dev/accel/accel0`) and add the container user to the host `render` group (GID varies by host).
- For OpenVINO to *actually* see `NPU` inside Docker, you also need the host NPU user-space runtime libraries (Level Zero) and the driver compiler library to be visible inside the container (see `services/openplanner/.env` + compose mounts).
- The model is downloaded from HuggingFace on first boot into `MODEL_DIR`.
- The realtime `/transcribe` default is intentionally an English-only medium Whisper (`OpenVINO/whisper-medium.en-int8-ov`). Earlier multilingual Whisper runs repeatedly misidentified English speech as Korean, so the `.en` model is the safer default for the live conversational pipeline.
- The batch `/transcribe-timed` endpoint can use a separate lazy-loaded model via `STT_TIMED_MODEL_ID`. PM2 defaults it to multilingual `OpenVINO/whisper-medium-int8-ov` for song/reference alignment, while keeping the live realtime pipeline on `.en`. The timed model is not loaded at service startup; the first timed request downloads/loads it, then reuses it in-process.
- Realtime `/transcribe` can run on NPU. For word-timestamped `/transcribe-timed`, PM2 defaults `STT_TIMED_DEVICE=CPU`: OpenVINO GenAI 2026.1 can load Whisper on NPU without constructor word timestamps, but timestamped timed inference hit Level Zero `ZE_RESULT_ERROR_UNKNOWN` during graph execution.
- For poor Discord voice transcriptions, first prefer better capture windows before changing models: keep natural short pauses in one utterance, avoid sub-second chunks, and let this sidecar normalize mic gain/noise before Whisper. If quality is still bad after that, override `WHISPER_MODEL_ID` with a larger compatible OpenVINO Whisper model and use a fresh `MODEL_DIR`.
- For songs, prefer `/transcribe-timed` on a separated vocal stem. It checkpoints timing as JSON and keeps Whisper as the text/timing layer, not the melody extractor.

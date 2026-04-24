# Skill: Music spectrogram analysis (no custom tools)

Goal: analyze a local audio file using *only basic tools* (read/write/edit/bash).

## Dependency check

```bash
command -v ffmpeg
command -v ffprobe
ffmpeg -version | head -n 1
```

If `ffmpeg` is missing:

- Prefer asking the operator to install it on the host/container.
- Or run analysis that does not require decoding (e.g. filename + size + sha256).

## Inputs

- `AUDIO_PATH`: workspace-relative path (example: `Music/foo.wav`)

## Outputs (convention)

Write artifacts next to the input when possible, or under:

- `Music/renders/` (recommended)

Suggested filenames:

- `NAME-spectrogram.png`
- `NAME-waveform.png`
- `NAME-ffprobe.json`

## Commands

### 1) Inspect (ffprobe)

```bash
ffprobe -hide_banner -v error \
  -show_format -show_streams \
  -print_format json \
  "$AUDIO_PATH" > "Music/renders/$(basename "$AUDIO_PATH").ffprobe.json"
```

### 2) Render a spectrogram (image)

```bash
mkdir -p Music/renders

OUT="Music/renders/$(basename "$AUDIO_PATH").spectrogram.png"

ffmpeg -y -hide_banner -v error \
  -i "$AUDIO_PATH" \
  -lavfi "showspectrumpic=s=1024x640:legend=disabled" \
  -frames:v 1 \
  "$OUT"
```

### 3) Render a waveform (image)

```bash
OUT="Music/renders/$(basename "$AUDIO_PATH").waveform.png"

ffmpeg -y -hide_banner -v error \
  -i "$AUDIO_PATH" \
  -lavfi "showwavespic=s=1200x320:colors=0x7dd3fc" \
  -frames:v 1 \
  "$OUT"
```

## How to return the result (important)

In the assistant response:

1) Summarize key properties from the `ffprobe` JSON (duration, sample rate, channels, codec).
2) Link to the rendered images using Markdown so the UI auto-embeds them:

```md
Spectrogram: [spectrogram](Music/renders/foo.wav.spectrogram.png)

Waveform: [waveform](Music/renders/foo.wav.waveform.png)
```

Do **not** claim BPM/key/genre unless you actually computed it.

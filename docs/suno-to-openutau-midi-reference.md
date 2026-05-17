# Suno-to-OpenUTAU/MIDI Reference Pipeline

## Intent

Turn high-quality Suno Pro examples into executable local music references: lyrics, arrangement grammar, MIDI sketches, OpenUTAU/USTX vocal plans, spectrogram analysis, and timing artifacts. The goal is not to clone Suno. The goal is to extract the composition laws that made the songs work, then give Knoxx/Fork Tales agents concrete examples they can compile consistently.

## Source Examples

### Breath Cache / 揺れるカーテン

- Lyrics/source prompt: `/home/err/devel/Music/fork-tales/corpus/揺れるカーテン_2.txt`
- Tempo/mode: `78-104 BPM drift`, `432Hz`, `C-F modal minor with chromatic intrusion`
- Core palette: color-piano strikes, fractured arpeggios, glitch dust, spectral overtones, air-pressure bass haze, silence pockets
- Form: somatic monologue -> verse -> breath-led pre-chorus -> big bilingual chorus -> chopped ritual post-chorus -> noise-ambient verse -> post-classical bridge -> ritual glitch breakdown -> final chorus -> breath outro
- Vocal law: breath is the clock. Spoken/whispered phrases become sung hooks only after the body stabilizes.

### 存在論的な“反・虚無”の論証として

- Root audio: `/home/err/devel/Music/heresy_between/存在論的な“反・虚無”の論証として.mp3`
- Full prompt/metadata: `/home/err/devel/Music/heresy_between/存在論的な“反・虚無”の論証として/存在論的な“反・虚無”の論証として.txt`
- Suno metadata JSON: `/home/err/devel/Music/heresy_between/存在論的な“反・虚無”の論証として/存在論的な“反・虚無”の論証として.json`
- Audio facts: `303.36s`, `48kHz stereo MP3`, mean volume about `-13.3 dB`, peak `0.0 dB`
- Core palette: live drum intro as protagonist, stable kick/sub, half-time gravity around 100 BPM, breakcore as emotional noise, minimal top layers, upright bass/sax cameos, ghost choir/vocoder, spoken Japanese breath texture
- Theme: a one-line act creates meaning and responsibility inside uncertainty.

## Recovered Composition Grammar

### Macro Form

Use long pop architecture, but make it breathe like ritual rather than radio:

1. Intro as proof-of-body: breath, room, notification/click, drum count, no full harmony yet.
2. Verse 1 as small object: one message, one bodily sensation, one image.
3. Pre-chorus as philosophical pivot: agency appears before explanation.
4. Chorus as oath: repeat the phrase that binds the whole song.
5. Verse 2 as social field: timeline, room, screen, grief, noise.
6. Bridge as sanctuary: drums withdraw; sax/choir/piano enters briefly; silence becomes a buffer.
7. Final chorus as transformation: same hook, larger body, no cheap resolution.
8. Outro as residue: breath/click/one line, not a big cadence.

### Rhythm

- Use half-time weight even when surface fragments are fast.
- Let drums lead the arrangement, not decorate it.
- Treat breakcore fragments as emotional static in transitions, fills, and post-chorus cuts.
- Keep kick/sub stable when the upper layers fracture.
- Prefer tempo bands and micro-drift for human unease: `78-104 BPM` for breath-IDM, `~100 BPM` for live-drum ritual pop.

### Harmony

- Center around modal minor rather than functional major/minor pop.
- Use C/F gravity for Fork Tales vocal comfort and OpenUTAU tractability.
- Chromatic intrusion should be short: knife-flashes, passing tones, altered bass notes, not constant jazz density.
- Bridge can borrow suspended/ambiguous voicings; chorus should return to a simpler modal anchor.

### Timbre

- Piano is color, not accompaniment: short percussive strikes and broken arpeggio shards.
- Bass is air pressure: sub haze, upright-bass cameos, simple roots, not busy riffs.
- Choir is ghost architecture: vocoder/choral pads behind the lead, not an always-on wall.
- Noise is semantic: clicks, notifications, glitch dust, breath consonants, nervous-system flicker.
- Sax/guitar/orchestra appear as apparitions, then vanish.

### Vocal Law

- The lead vocal can be minimal. The emotion comes from placement, air, and timing.
- Use Japanese as the main sung/spoken body; English appears as signal-flare hooks.
- Spoken lines must be short and placed on downbeats or in silence pockets.
- Do not sing section labels, parentheticals, or stage directions.
- Hooks should be bilingual but not overloaded:
  - `息で起動して / breathe me online`
  - `一行だけのメッセージで`
  - `No nation, no name - just resonance`
- For OpenUTAU/Ritsu VCV, prefer hiragana lyric syllables in USTX. Keep kanji in the human-facing lyrics, then prepare a separate kana performance layer.

## Local Compiler Pipeline

Use staged state transitions, not one resident mega-model:

1. Source analysis: read lyrics, metadata, audio facts, spectrogram.
2. Stem split: Demucs, one song at a time. Prefer 4-stem first; use 6-stem only when piano/guitar isolation matters.
3. Instrument transcription: Basic Pitch on bass/other/piano/guitar stems. Export MIDI with pitch bends when useful.
4. Vocal pitch: torchcrepe/CREPE on separated vocals. Convert f0 contour into note events with cleanup.
5. Lyrics timing: Knoxx `stt-npu` `/transcribe-timed` on separated vocal stems. Use Whisper as text/timing, not melody extraction.
6. Performance model: keep MIDI notes separate from language alignment JSON.
7. Vocal synthesis: generate USTX from cleaned kana lyric events, not raw mixed-language prompt text.
8. Render: persisted OpenUTAU exporter at `/home/err/devel/orgs/stakira/OpenUtau/.opencode/openutau-patched/OpenUtau`.
9. Mix: duck the bed around the vocal; leave breath/silence pockets intact.

## Data Model

Keep these artifacts per reference song:

- `source.json`: title, ids, prompt path, audio path, model/source metadata
- `analysis.md`: human-readable form, motifs, mix notes, spectrogram observations
- `sections.json`: section labels, approximate start/end times, energy curve, arrangement roles
- `stems/`: Demucs outputs
- `midi/`: Basic Pitch and hand-cleaned MIDI files
- `vocal/f0.json`: pitch contour and confidence
- `vocal/notes.mid`: cleaned vocal melody
- `lyrics/alignment.json`: Whisper segment/word timing
- `openutau/*.ustx`: kana performance plans for OpenUTAU
- `renders/*.wav`: rendered vocal stems
- `mixes/*.wav`: final local reconstructions

## Agent Instructions

When composing a new Fork Tales song from these examples:

- Start from the section grammar, not from an audio prompt.
- Write the lyric as human text first, then derive a kana performance layer.
- Build a MIDI skeleton before asking any audio model for a bed.
- Never let the bed determine the song. Drums, bass, and vocal timing define the body.
- Use generative audio models only as texture fill or arrangement sketching unless a stronger local model replaces MusicGen-small.
- Always leave a receipt: audio facts, generated paths, model settings, and what was hand-authored vs inferred.

## Mythic Compression

This corpus is not trying to make "AI songs." It is trying to make receipts that sing. A breath becomes a clock. A click becomes a snare. A one-line message becomes ethics. A country dissolves into frequency. The local pipeline should preserve that law: the song is a contract between body, signal, and responsibility, compiled into MIDI, USTX, alignment JSON, and finally sound.

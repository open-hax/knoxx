# Fork Tales — Composable Role Contracts

A modular system for composing music in the Fork Tales universe. Each contract is a small, focused slice of creative identity. Combine them to generate songs.

## Architecture

```
Persona + Voice + Theme + [Environment] + [Instruments] → Song
```

Each layer is a **role contract** in `contracts/fork-tales/`. They compose by merging `:data` maps.

---

## Voices (3)

| Contract | ID | Singer | Language | Character |
|----------|-----|--------|----------|-----------|
| `voices/teto.edn` | `:role/voice-teto` | 重音テト OU用日本語統合ライブラリー | Japanese | Energetic, layered, persistent. The spine that kept moving. |
| `voices/ritsu.edn` | `:role/voice-ritsu` | 波音リツ連続音Ver1.5.1 | Japanese | Boyish-girl. Body memory. Deliberate variance. The Fork Tales protagonist. |
| `voices/teto-en.edn` | `:role/voice-teto-en` | 重音テト音声ライブラリー | English | The Weeblish voice. Functional, slightly awkward, persistent. |

**Usage**: Set `:singer_id` in `voice.openutau_project` to `"teto"`, `"ritsu"`, or `"teto-en"`.

---

## Themes — Moods & Genres (7)

| Contract | ID | BPM | Genre | Anchor Motif |
|----------|-----|-----|-------|--------------|
| `themes/glitch-choral.edn` | `:role/theme-glitch-choral` | 84 | Mythic glitch-choral synth ballad | "No claim without anchor." |
| `themes/industrial-rap.edn` | `:role/theme-industrial-rap` | 96 | Industrial glitch-rap | "Edges are claims. Claims need receipts." |
| `themes/dark-synthwave.edn` | `:role/theme-dark-synthwave` | 88 | Dark synthwave + choir | "Forks cost reality." |
| `themes/minimal-ambient.edn` | `:role/theme-minimal-ambient` | 76 | Minimal piano + glitch ambient | "If names blur, responsibility blurs." |
| `themes/hyperpop-glitch.edn` | `:role/theme-hyperpop-glitch` | 140 | Hyperpop glitchcore micro-anthem | "Binary for speed, anchors for truth." |
| `themes/cinematic-orchestral.edn` | `:role/theme-cinematic-orchestral` | 90 | Cinematic glitch orchestral | "Interpretation is the weapon. Anchors disarm it." |
| `themes/whispered-lullaby.edn` | `:role/theme-whispered-lullaby` | 60 | Whispered lullaby | "What you can't prove, don't use to steer me." |

**Usage**: Themes provide `:theme/bpm`, `:theme/genre`, `:production/*`, and `:lyric/motifs`.

---

## Personas — Characters (7)

| Contract | ID | Origin | Voice | Themes | Signature Line |
|----------|-----|--------|-------|--------|----------------|
| `personas/ritsu.edn` | `:role/persona-ritsu` | South Korea / Tokyo | `voice-ritsu` | minimal-ambient, glitch-choral, whispered-lullaby | "No claim without anchor." |
| `personas/duct.edn` | `:role/persona-duct` | United States | `voice-teto-en` | industrial-rap, dark-synthwave, cinematic-orchestral | "The question is what's load-bearing." |
| `personas/null.edn` | `:role/persona-null` | Russia | `voice-teto` | minimal-ambient, industrial-rap | "Someone must be the fuse." |
| `personas/patch.edn` | `:role/persona-patch` | China | `voice-teto` | hyperpop-glitch, glitch-choral, cinematic-orchestral | "Boundaries are UI before they're law." |
| `personas/sei.edn` | `:role/persona-sei` | Japan | `voice-ritsu` | cinematic-orchestral, minimal-ambient, glitch-choral | "The trains did not stop." |
| `personas/rin.edn` | `:role/persona-rin` | Ambiguous | `voice-ritsu` | whispered-lullaby, minimal-ambient, glitch-choral | "What you can't prove, don't use to steer me." |
| `personas/axiom-01.edn` | `:role/persona-axiom-01` | Proprietary fork | `voice-teto-en` | dark-synthwave, industrial-rap, cinematic-orchestral | "The ring will crown a leader, or it will break." |

**Usage**: Personas provide `:lyric/themes`, `:lyric/mood`, `:lyric/language-ratio`, and `:lyric/signature-line`.

---

## Environments — Sonic Spaces (5)

| Contract | ID | Space | Mood |
|----------|-----|-------|------|
| `environments/cathedral-reverb.edn` | `:role/env-cathedral-reverb` | Vast stone architecture | Sacred, anxious, awe-filled |
| `environments/rail-station.edn` | `:role/env-rail-station` | Seishin Infrastructure platform | Calm, mechanical, quietly heroic |
| `environments/digital-void.edn` | `:role/env-digital-void` | Null's diagnostic overlay | Cold, diagnostic, lonely |
| `environments/emergency-web.edn` | `:role/env-emergency-web` | Disaster browser interface | Calm, austere, authoritative |
| `environments/gate-threshold.edn` | `:role/env-gate-threshold` | Liminal space before the Gate | Uncertain, sacred, terrifying |

**Usage**: Environments provide `:env/reverb`, `:env/textures`, and `:env/mood`.

---

## Instruments — Sonic Textures (5)

| Contract | ID | Family | Mood |
|----------|-----|--------|------|
| `instruments/upright-piano.edn` | `:role/instr-upright-piano` | Keyboard | Nostalgic, honest, slightly broken |
| `instruments/granular-choir.edn` | `:role/instr-granular-choir` | Vocal synthesis | Sacred, unsettling, beautiful |
| `instruments/chip-lead.edn` | `:role/instr-chip-lead` | Synth | Playful, urgent, precise |
| `instruments/sub-bass.edn` | `:role/instr-sub-bass` | Bass | Intimate, physical, grounding |
| `instruments/granular-strings.edn` | `:role/instr-granular-strings` | Strings | Epic, sorrowful, broken, persistent |

**Usage**: Instruments provide `:instrument/technique`, `:instrument/effects`, and `:instrument/register`.

---

## Master Composer

`roles/fork-tales-composer.edn` (`:role/fork-tales-composer`)

The master role that knows how to compose layers together. Grants `:cap/voice-synth` (which now includes OpenUTAU tools) and `:cap/music-theory`.

**System prompt**: "You are a composer for the Fork Tales universe..."

---

## Example Composition

**Prompt**: "Compose a song from Ritsu's perspective about the trains never stopping."

**Composition**:
- Persona: `:role/persona-ritsu`
- Voice: `:role/voice-ritsu`
- Theme: `:role/theme-cinematic-orchestral` or `:role/theme-minimal-ambient`
- Environment: `:role/env-rail-station`
- Instruments: `:role/instr-upright-piano`, `:role/instr-sub-bass`

**Result**: Japanese lyrics (70/30 ja/en), BPM 90 or 76, rail motifs, Seishin infrastructure references, signature line "The trains did not stop."

---

## Files Created (27 total)

### Capabilities Updated
- `contracts/capabilities/cap_voice_synth.edn` — Added `:voice.openutau_project` and `:voice.openutau_render`

### New Contracts
- `contracts/fork-tales/voices/` — 3 contracts (teto, ritsu, teto-en)
- `contracts/fork-tales/themes/` — 7 contracts (glitch-choral, industrial-rap, dark-synthwave, minimal-ambient, hyperpop-glitch, cinematic-orchestral, whispered-lullaby)
- `contracts/fork-tales/personas/` — 7 contracts (ritsu, duct, null, patch, sei, rin, axiom-01)
- `contracts/fork-tales/environments/` — 5 contracts (cathedral-reverb, rail-station, digital-void, emergency-web, gate-threshold)
- `contracts/fork-tales/instruments/` — 5 contracts (upright-piano, granular-choir, chip-lead, sub-bass, granular-strings)
- `contracts/roles/fork-tales-composer.edn` — Master composer role

---

## Next Steps

1. **Register the master role** in the Knoxx role registry
2. **Test composition** — ask Knoxx to compose a song using these contracts
3. **Iterate** — add more voices (more UTAU voicebanks), themes, or personas as the universe expands
4. **MusicGen integration** — compose instrumental tracks using `music.generate_song` with themes as style prompts, then layer vocals via OpenUTAU

No claim without anchor.

#!/usr/bin/env node
import { OfflineAudioContext } from 'node-web-audio-api'
import toWav from 'audiobuffer-to-wav'
import fs from 'node:fs/promises'
import path from 'node:path'

const [specPath, outPathArg] = process.argv.slice(2)
if (!specPath || !outPathArg) {
  console.error('Usage: node synthesize-music.mjs <spec.json> <output.wav>')
  process.exit(1)
}

const outputPath = path.resolve(outPathArg)

// ---------------------------------------------------------------------------
// Music theory helpers
// ---------------------------------------------------------------------------
const NOTE_MAP = {
  C: 0, 'C#': 1, Db: 1, D: 2, 'D#': 3, Eb: 3, E: 4, F: 5, 'F#': 6,
  Gb: 6, G: 7, 'G#': 8, Ab: 8, A: 9, 'A#': 10, Bb: 10, B: 11
}

function noteToMidi(noteStr) {
  const m = noteStr.match(/^([A-G][#b]?)(-?\d+)$/)
  if (!m) throw new Error(`Invalid note: ${noteStr}`)
  const semitone = NOTE_MAP[m[1]]
  const octave = parseInt(m[2], 10)
  return semitone + (octave + 1) * 12
}

function midiToFreq(midi) {
  return 440 * Math.pow(2, (midi - 69) / 12)
}

function parseNote(n) {
  if (typeof n === 'number') return n
  if (typeof n === 'string') return midiToFreq(noteToMidi(n))
  throw new Error(`Unknown note type: ${n}`)
}

// ---------------------------------------------------------------------------
// Audio synthesis helpers
// ---------------------------------------------------------------------------
const SAMPLE_RATE = 44100

function createNoiseBuffer(ctx, durationSec) {
  const len = Math.ceil(durationSec * SAMPLE_RATE)
  const buf = ctx.createBuffer(1, len, SAMPLE_RATE)
  const data = buf.getChannelData(0)
  for (let i = 0; i < len; i++) data[i] = Math.random() * 2 - 1
  return buf
}

function scheduleDrum(ctx, dest, type, time, gain = 0.8) {
  const g = ctx.createGain()
  g.connect(dest)

  if (type === 'kick') {
    const osc = ctx.createOscillator()
    osc.frequency.setValueAtTime(150, time)
    osc.frequency.exponentialRampToValueAtTime(40, time + 0.15)
    osc.connect(g)
    g.gain.setValueAtTime(gain, time)
    g.gain.exponentialRampToValueAtTime(0.01, time + 0.25)
    osc.start(time)
    osc.stop(time + 0.25)
  } else if (type === 'snare') {
    const osc = ctx.createOscillator()
    osc.type = 'triangle'
    osc.frequency.setValueAtTime(200, time)
    osc.connect(g)
    const noise = ctx.createBufferSource()
    noise.buffer = createNoiseBuffer(ctx, 0.3)
    const noiseGain = ctx.createGain()
    noiseGain.gain.setValueAtTime(gain * 0.6, time)
    noiseGain.gain.exponentialRampToValueAtTime(0.01, time + 0.2)
    noise.connect(noiseGain)
    noiseGain.connect(dest)
    noise.start(time)
    g.gain.setValueAtTime(gain * 0.5, time)
    g.gain.exponentialRampToValueAtTime(0.01, time + 0.15)
    osc.start(time)
    osc.stop(time + 0.15)
  } else if (type === 'hihat' || type === 'hat') {
    const noise = ctx.createBufferSource()
    noise.buffer = createNoiseBuffer(ctx, 0.1)
    const hp = ctx.createBiquadFilter()
    hp.type = 'highpass'
    hp.frequency.value = 8000
    noise.connect(hp)
    hp.connect(g)
    g.gain.setValueAtTime(gain * 0.4, time)
    g.gain.exponentialRampToValueAtTime(0.01, time + 0.05)
    noise.start(time)
    noise.stop(time + 0.05)
  } else if (type === 'clap') {
    const noise = ctx.createBufferSource()
    noise.buffer = createNoiseBuffer(ctx, 0.2)
    const bp = ctx.createBiquadFilter()
    bp.type = 'bandpass'
    bp.frequency.value = 1500
    bp.Q.value = 1
    noise.connect(bp)
    bp.connect(g)
    g.gain.setValueAtTime(0, time)
    g.gain.linearRampToValueAtTime(gain * 0.7, time + 0.01)
    g.gain.exponentialRampToValueAtTime(0.01, time + 0.15)
    noise.start(time)
    noise.stop(time + 0.15)
  } else if (type === 'tom') {
    const osc = ctx.createOscillator()
    osc.type = 'sine'
    osc.frequency.setValueAtTime(120, time)
    osc.frequency.exponentialRampToValueAtTime(60, time + 0.2)
    osc.connect(g)
    g.gain.setValueAtTime(gain * 0.7, time)
    g.gain.exponentialRampToValueAtTime(0.01, time + 0.2)
    osc.start(time)
    osc.stop(time + 0.2)
  } else {
    // Generic noise burst
    const noise = ctx.createBufferSource()
    noise.buffer = createNoiseBuffer(ctx, 0.2)
    noise.connect(g)
    g.gain.setValueAtTime(gain, time)
    g.gain.exponentialRampToValueAtTime(0.01, time + 0.2)
    noise.start(time)
    noise.stop(time + 0.2)
  }
}

function scheduleSynthNote(ctx, dest, freq, time, duration, waveform = 'sawtooth', gain = 0.5, envelope = null) {
  const env = envelope || { attack: 0.02, decay: 0.1, sustain: 0.6, release: 0.2 }
  const osc = ctx.createOscillator()
  osc.type = waveform
  osc.frequency.setValueAtTime(freq, time)

  const g = ctx.createGain()
  g.connect(dest)
  osc.connect(g)

  const peak = gain
  const susLevel = peak * (env.sustain || 0.6)
  g.gain.setValueAtTime(0, time)
  g.gain.linearRampToValueAtTime(peak, time + (env.attack || 0.02))
  g.gain.linearRampToValueAtTime(susLevel, time + (env.attack || 0.02) + (env.decay || 0.1))
  g.gain.setValueAtTime(susLevel, time + duration)
  g.gain.exponentialRampToValueAtTime(0.001, time + duration + (env.release || 0.2))

  osc.start(time)
  osc.stop(time + duration + (env.release || 0.2) + 0.1)
}

function schedulePadNote(ctx, dest, freq, time, duration, waveform = 'triangle', gain = 0.4, envelope = null) {
  const env = envelope || { attack: 0.3, decay: 0.2, sustain: 0.7, release: 0.8 }
  scheduleSynthNote(ctx, dest, freq, time, duration, waveform, gain, env)
}

// ---------------------------------------------------------------------------
// Spec parsing
// ---------------------------------------------------------------------------
async function loadSpec(specPath) {
  const raw = await fs.readFile(specPath, 'utf8')
  return JSON.parse(raw)
}

function asArray(value) {
  return Array.isArray(value) ? value : []
}

function isPlainObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value)
}

function normalizeInstrument(value) {
  const instrument = String(value || 'synth').toLowerCase()
  if (instrument === 'drums' || instrument === 'percussion') return 'drum'
  return instrument
}

function parseBeatValue(value, fallback = 0, beatsPerBar = 4) {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value !== 'string') return fallback

  const text = value.trim()
  if (!text) return fallback

  const numeric = Number(text)
  if (Number.isFinite(numeric)) return numeric

  if (text.includes(':')) {
    const parts = text.split(':').map((part) => Number(part.trim()))
    if (parts.some((part) => !Number.isFinite(part))) return fallback
    const [bars = 0, beats = 0, sixteenths = 0, ticks = 0] = parts
    return (bars * beatsPerBar) + beats + (sixteenths / 4) + (ticks / 16)
  }

  return fallback
}

function parseDurationBeats(value, fallback = 1, beatsPerBar = 4) {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value !== 'string') return fallback

  const text = value.trim().toLowerCase()
  if (!text) return fallback

  const numeric = Number(text)
  if (Number.isFinite(numeric)) return numeric

  const notation = text.match(/^(\d+(?:\.\d+)?)([nm])(\.)?$/)
  if (notation) {
    const amount = Number(notation[1])
    const unit = notation[2]
    const dotted = notation[3] ? 1.5 : 1
    if (unit === 'm') return amount * beatsPerBar * dotted
    if (unit === 'n' && amount > 0) return (4 / amount) * dotted
  }

  if (text.includes(':')) return parseBeatValue(text, fallback, beatsPerBar)
  return fallback
}

function mergePatternNote(pattern, note) {
  if (!isPlainObject(pattern) || !isPlainObject(note)) return note
  const { notes: _notes, pattern: _pattern, ...defaults } = pattern
  return { ...defaults, ...note }
}

function collectTrackNotes(track) {
  const notes = []
  for (const note of asArray(track.notes)) notes.push(note)
  for (const note of asArray(track.pattern)) {
    if (isPlainObject(note) || typeof note === 'string' || typeof note === 'number') {
      notes.push(note)
    }
  }
  for (const pattern of asArray(track.patterns)) {
    if (isPlainObject(pattern)) {
      for (const note of asArray(pattern.notes)) {
        notes.push(mergePatternNote(pattern, note))
      }
      for (const note of asArray(pattern.pattern)) {
        if (isPlainObject(note) || typeof note === 'string' || typeof note === 'number') {
          notes.push(mergePatternNote(pattern, note))
        }
      }
    } else if (Array.isArray(pattern)) {
      for (const note of pattern) notes.push(note)
    }
  }
  return notes
}

function noteTimeBeats(note, index, beatsPerBar = 4) {
  if (isPlainObject(note)) return parseBeatValue(note.time ?? note.beat ?? note.start, index, beatsPerBar)
  return index
}

function noteDurationBeats(note, fallback, beatsPerBar = 4) {
  if (isPlainObject(note)) return parseDurationBeats(note.duration ?? note.dur ?? note.length, fallback, beatsPerBar)
  return fallback
}

function resolveDuration(spec) {
  const bpm = spec.bpm || 120
  const beatsPerBar = spec.beatsPerBar || spec.beats_per_bar || 4
  const spb = 60 / bpm
  if (typeof spec.duration === 'number' && Number.isFinite(spec.duration)) return spec.duration
  if (typeof spec.duration === 'string') return parseDurationBeats(spec.duration, 4, beatsPerBar) * spb + 2
  if (spec.durationBeats || spec.duration_beats) return parseDurationBeats(spec.durationBeats ?? spec.duration_beats, 4, beatsPerBar) * spb + 2
  // Infer from patterns
  let maxBeat = 0
  for (const track of spec.tracks || []) {
    const instrument = normalizeInstrument(track.instrument)
    const rawNotes = collectTrackNotes(track)
    const notes = instrument === 'drum'
      ? rawNotes.filter((note) => isPlainObject(note) || typeof note === 'string')
      : rawNotes
    const noteDefaultDuration = instrument === 'pad' ? 2 : instrument === 'drum' ? 0.25 : 0.5
    for (let i = 0; i < notes.length; i++) {
      maxBeat = Math.max(maxBeat, noteTimeBeats(notes[i], i, beatsPerBar) + noteDurationBeats(notes[i], noteDefaultDuration, beatsPerBar))
    }
    if (instrument === 'drum' && notes.length === 0 && Array.isArray(track.pattern)) {
      const stepBeats = parseDurationBeats(track.stepDuration ?? track.step_duration, 1, beatsPerBar)
      maxBeat = Math.max(maxBeat, track.pattern.length * stepBeats)
    }
    for (const pattern of asArray(track.patterns)) {
      if (isPlainObject(pattern) && Array.isArray(pattern.pattern) && !Array.isArray(pattern.notes)) {
        const stepBeats = parseDurationBeats(pattern.stepDuration ?? pattern.step_duration ?? track.stepDuration ?? track.step_duration, 1, beatsPerBar)
        const offsetBeats = parseBeatValue(pattern.time ?? pattern.beat ?? pattern.start, 0, beatsPerBar)
        maxBeat = Math.max(maxBeat, offsetBeats + (pattern.pattern.length * stepBeats))
      }
    }
  }
  return (maxBeat || 4) * spb + 2 // +2s tail
}

// ---------------------------------------------------------------------------
// Render
// ---------------------------------------------------------------------------
async function render(spec) {
  const bpm = spec.bpm || 120
  const spb = 60 / bpm
  const durationSec = resolveDuration(spec)
  const totalSamples = Math.ceil(durationSec * SAMPLE_RATE)

  const ctx = new OfflineAudioContext(2, totalSamples, SAMPLE_RATE)
  const master = ctx.createGain()
  master.gain.value = spec.masterGain ?? 0.85
  master.connect(ctx.destination)

  // Optional master limiter/compressor approximation
  const compressor = ctx.createDynamicsCompressor()
  compressor.threshold.value = -12
  compressor.knee.value = 3
  compressor.ratio.value = 4
  compressor.attack.value = 0.005
  compressor.release.value = 0.1
  master.connect(compressor)
  compressor.connect(ctx.destination)

  for (const track of spec.tracks || []) {
    const trackGain = ctx.createGain()
    trackGain.gain.value = track.gain ?? 0.7
    trackGain.connect(master)

    const pan = ctx.createStereoPanner()
    pan.pan.value = track.pan ?? 0
    trackGain.connect(pan)
    pan.connect(master)

    const dest = pan

    const instrument = normalizeInstrument(track.instrument)
    const beatsPerBar = spec.beatsPerBar || spec.beats_per_bar || 4

    if (instrument === 'drum') {
      const drumType = track.type || 'kick'
      const notes = collectTrackNotes(track).filter((note) => isPlainObject(note) || typeof note === 'string')
      if (notes.length > 0) {
        for (let i = 0; i < notes.length; i++) {
          const n = notes[i]
          if (isPlainObject(n)) {
            const type = n.note || n.type || n.pitch || drumType
            const time = noteTimeBeats(n, i, beatsPerBar) * spb
            const vel = n.velocity ?? n.vel ?? n.gain ?? 1
            scheduleDrum(ctx, dest, type, time, (track.gain ?? 0.8) * vel)
          } else if (typeof n === 'string') {
            scheduleDrum(ctx, dest, n, i * spb, track.gain ?? 0.8)
          }
        }
      } else {
        const renderPattern = (pattern, type, stepDuration, offsetSec = 0) => {
          for (let i = 0; i < pattern.length; i++) {
            const hit = pattern[i]
            if (hit === 1 || hit === true || (typeof hit === 'number' && hit > 0)) {
              const time = offsetSec + (i * stepDuration)
              const vel = typeof hit === 'number' ? hit : 1
              scheduleDrum(ctx, dest, type, time, (track.gain ?? 0.8) * vel)
            }
          }
        }
        const stepDuration = parseDurationBeats(track.stepDuration ?? track.step_duration, 1, beatsPerBar) * spb
        renderPattern(track.pattern || [], drumType, stepDuration)
        for (const patternSpec of asArray(track.patterns)) {
          if (isPlainObject(patternSpec) && Array.isArray(patternSpec.pattern)) {
            const patternStep = parseDurationBeats(patternSpec.stepDuration ?? patternSpec.step_duration ?? track.stepDuration ?? track.step_duration, 1, beatsPerBar) * spb
            const offsetSec = parseBeatValue(patternSpec.time ?? patternSpec.beat ?? patternSpec.start, 0, beatsPerBar) * spb
            renderPattern(patternSpec.pattern, patternSpec.type || drumType, patternStep, offsetSec)
          }
        }
      }
    } else if (instrument === 'synth' || instrument === 'bass' || instrument === 'lead') {
      const waveform = track.waveform || 'sawtooth'
      const env = track.envelope || { attack: 0.02, decay: 0.1, sustain: 0.6, release: 0.2 }
      const notes = collectTrackNotes(track)
      for (let i = 0; i < notes.length; i++) {
        const n = notes[i]
        if (isPlainObject(n)) {
          const freq = parseNote(n.note || n.pitch || 'C4')
          const time = noteTimeBeats(n, i, beatsPerBar) * spb
          const dur = noteDurationBeats(n, 0.5, beatsPerBar)
          const vel = n.velocity ?? n.vel ?? 1
          scheduleSynthNote(ctx, dest, freq, time, dur * spb, waveform, (track.gain ?? 0.5) * vel, env)
        } else if (typeof n === 'string' || typeof n === 'number') {
          // Simple array of notes — treat as quarter notes
          const freq = parseNote(n)
          scheduleSynthNote(ctx, dest, freq, i * spb, spb, waveform, track.gain ?? 0.5, env)
        }
      }
    } else if (instrument === 'pad') {
      const waveform = track.waveform || 'triangle'
      const env = track.envelope || { attack: 0.3, decay: 0.2, sustain: 0.7, release: 0.8 }
      const notes = collectTrackNotes(track)
      for (let i = 0; i < notes.length; i++) {
        const n = notes[i]
        if (isPlainObject(n)) {
          const freq = parseNote(n.note || n.pitch || 'C4')
          const time = noteTimeBeats(n, i * 2, beatsPerBar) * spb
          const dur = noteDurationBeats(n, 2, beatsPerBar)
          const vel = n.velocity ?? n.vel ?? 1
          schedulePadNote(ctx, dest, freq, time, dur * spb, waveform, (track.gain ?? 0.4) * vel, env)
        } else if (typeof n === 'string' || typeof n === 'number') {
          const freq = parseNote(n)
          schedulePadNote(ctx, dest, freq, i * spb * 2, spb * 2, waveform, track.gain ?? 0.4, env)
        }
      }
    } else if (instrument === 'noise') {
      // Ambient/noise texture
      const noise = ctx.createBufferSource()
      noise.buffer = createNoiseBuffer(ctx, durationSec)
      const filter = ctx.createBiquadFilter()
      filter.type = track.filterType || 'lowpass'
      filter.frequency.value = track.filterFreq ?? 1000
      filter.Q.value = track.filterQ ?? 1
      noise.connect(filter)
      filter.connect(dest)
      const g = ctx.createGain()
      g.gain.setValueAtTime(0, 0)
      g.gain.linearRampToValueAtTime(track.gain ?? 0.3, 1)
      g.gain.linearRampToValueAtTime(0, durationSec)
      filter.connect(g)
      g.connect(dest)
      noise.start(0)
      noise.stop(durationSec)
    }
  }

  const buffer = await ctx.startRendering()
  const wav = toWav(buffer)
  await fs.mkdir(path.dirname(outputPath), { recursive: true })
  await fs.writeFile(outputPath, Buffer.from(wav))

  return {
    ok: true,
    outputPath,
    durationSec,
    sampleRate: SAMPLE_RATE,
    channels: buffer.numberOfChannels,
    samples: buffer.length
  }
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------
try {
  const spec = await loadSpec(specPath)
  const result = await render(spec)
  console.log(JSON.stringify(result))
} catch (err) {
  console.error(JSON.stringify({ ok: false, error: err.message }))
  process.exit(2)
}

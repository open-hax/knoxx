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

function resolveDuration(spec) {
  if (spec.duration) return spec.duration
  // Infer from patterns
  let maxBeat = 0
  const spb = 60 / (spec.bpm || 120)
  for (const track of spec.tracks || []) {
    if (track.pattern) {
      maxBeat = Math.max(maxBeat, track.pattern.length)
    }
    if (track.notes) {
      for (const n of track.notes) {
        const t = typeof n === 'object' ? (n.time || n.beat || 0) : 0
        const dur = typeof n === 'object' ? (n.duration || n.dur || 0.5) : 0.5
        maxBeat = Math.max(maxBeat, t + dur)
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

    const instrument = track.instrument || 'synth'

    if (instrument === 'drum') {
      const drumType = track.type || 'kick'
      const pattern = track.pattern || []
      const stepDuration = track.stepDuration ?? spb
      for (let i = 0; i < pattern.length; i++) {
        const hit = pattern[i]
        if (hit === 1 || hit === true || (typeof hit === 'number' && hit > 0)) {
          const time = i * stepDuration
          const vel = typeof hit === 'number' ? hit : 1
          scheduleDrum(ctx, dest, drumType, time, (track.gain ?? 0.8) * vel)
        }
      }
    } else if (instrument === 'synth' || instrument === 'bass' || instrument === 'lead') {
      const waveform = track.waveform || 'sawtooth'
      const env = track.envelope || { attack: 0.02, decay: 0.1, sustain: 0.6, release: 0.2 }
      const notes = track.notes || track.pattern || []
      for (const n of notes) {
        if (typeof n === 'object' && n !== null) {
          const freq = parseNote(n.note || n.pitch || 'C4')
          const time = (n.time ?? n.beat ?? 0) * spb
          const dur = n.duration ?? n.dur ?? 0.5
          const vel = n.velocity ?? n.vel ?? 1
          scheduleSynthNote(ctx, dest, freq, time, dur * spb, waveform, (track.gain ?? 0.5) * vel, env)
        } else if (typeof n === 'string' || typeof n === 'number') {
          // Simple array of notes — treat as quarter notes
          const idx = notes.indexOf(n)
          const freq = parseNote(n)
          scheduleSynthNote(ctx, dest, freq, idx * spb, spb, waveform, track.gain ?? 0.5, env)
        }
      }
    } else if (instrument === 'pad') {
      const waveform = track.waveform || 'triangle'
      const env = track.envelope || { attack: 0.3, decay: 0.2, sustain: 0.7, release: 0.8 }
      const notes = track.notes || track.pattern || []
      for (const n of notes) {
        if (typeof n === 'object' && n !== null) {
          const freq = parseNote(n.note || n.pitch || 'C4')
          const time = (n.time ?? n.beat ?? 0) * spb
          const dur = n.duration ?? n.dur ?? 2
          const vel = n.velocity ?? n.vel ?? 1
          schedulePadNote(ctx, dest, freq, time, dur * spb, waveform, (track.gain ?? 0.4) * vel, env)
        } else if (typeof n === 'string' || typeof n === 'number') {
          const idx = notes.indexOf(n)
          const freq = parseNote(n)
          schedulePadNote(ctx, dest, freq, idx * spb * 2, spb * 2, waveform, track.gain ?? 0.4, env)
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

import { useCallback, useEffect, useRef, useState } from "react";

import { voiceSttTranscribe } from "../../lib/api";

type VoiceRecorderState =
  | { status: "idle" }
  | { status: "recording"; startedAt: number }
  | { status: "transcribing" }
  | { status: "error"; message: string };

// Time-domain silence detection thresholds
const SILENCE_THRESHOLD = 0.015; // RMS ~ -36dB (lenient enough for quiet rooms)
const SILENCE_DURATION_MS = 1800; // 1.8s pause before auto-stop
const MIN_RECORDING_MS = 800; // must record at least 800ms before auto-stop
const MAX_RECORDING_MS = 30000; // hard stop at 30s
const ANALYSIS_INTERVAL_MS = 100; // check every 100ms

// Chunking constants
const CHUNK_DURATION_S = 10; // Reduced from 15 to avoid potential proxy size limits
const CHUNK_OVERLAP_S = 1.0;
const SAMPLE_RATE_CANDIDATE = 44100;

function pickMediaRecorderMimeType(): string | undefined {
  const candidates = [
    "audio/webm;codecs=opus",
    "audio/webm",
    "audio/ogg;codecs=opus",
    "audio/ogg",
  ];

  if (typeof MediaRecorder === "undefined") return undefined;

  for (const candidate of candidates) {
    try {
      if (MediaRecorder.isTypeSupported(candidate)) return candidate;
    } catch {
      // ignore
    }
  }

  return undefined;
}

function calculateRMS(timeDomainData: Uint8Array): number {
  let sum = 0;
  for (let i = 0; i < timeDomainData.length; i++) {
    const normalized = (timeDomainData[i] - 128) / 128;
    sum += normalized * normalized;
  }
  return Math.sqrt(sum / timeDomainData.length);
}

function float32ToWavBlob(samples: Float32Array, sampleRate: number): Blob {
  const wavHeader = new ArrayBuffer(44);
  const view = new DataView(wavHeader);

  view.setUint32(0, 0x52494646, false); // "RIFF"
  view.setUint32(4, 36 + samples.length * 4, true);
  view.setUint32(8, 0x4c434b54, false); // "WAVE"
  view.setUint32(12, 0x666d74, false); // "fmt "
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true); // PCM
  view.setUint16(22, 1, true); // Mono
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * 4, true);
  view.setUint16(32, 4, true);
  view.setUint16(34, 32, true);
  view.setUint32(36, 0x617461, false); // "data"
  view.setUint32(40, samples.length * 4, true);

  const pcmBytes = new ArrayBuffer(samples.length * 4);
  new Float32Array(pcmBytes).set(samples);

  return new Blob([wavHeader, pcmBytes], { type: "audio/wav" });
}

function longestCommonSuffixPrefix(a: string, b: string): number {
  const maxLen = Math.min(a.length, b.length);
  for (let len = maxLen; len > 0; len--) {
    if (a.slice(-len) === b.slice(0, len)) {
      return len;
    }
  }
  return 0;
}

export function useVoiceRecorder({
  onTranscript,
  conversationMode = false,
  silenceThreshold = SILENCE_THRESHOLD,
}: {
  onTranscript: (text: string) => void;
  conversationMode?: boolean;
  silenceThreshold?: number;
}) {
  const [state, setState] = useState<VoiceRecorderState>({ status: "idle" });
  const recorderRef = useRef<MediaRecorder | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const chunksRef = useRef<BlobPart[]>([]);
  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const silenceStartRef = useRef<number | null>(null);
  const analysisIntervalRef = useRef<number | null>(null);
  const maxDurationTimeoutRef = useRef<number | null>(null);
  const onTranscriptRef = useRef(onTranscript);
  const audioLevelRef = useRef(0);

  const pcmBufferRef = useRef<Float32Array>(new Float32Array(0));
  const processorRef = useRef<ScriptProcessorNode | null>(null);
  const chunkTimerRef = useRef<number | null>(null);
  const accumulatedTextRef = useRef("");
  const isSendingChunkRef = useRef(false);

  onTranscriptRef.current = onTranscript;

  const cleanupAudio = useCallback(() => {
    if (analysisIntervalRef.current) {
      clearInterval(analysisIntervalRef.current);
      analysisIntervalRef.current = null;
    }
    if (maxDurationTimeoutRef.current) {
      clearTimeout(maxDurationTimeoutRef.current);
      maxDurationTimeoutRef.current = null;
    }
    if (chunkTimerRef.current) {
      clearInterval(chunkTimerRef.current);
      chunkTimerRef.current = null;
    }
    if (processorRef.current) {
      processorRef.current.disconnect();
      processorRef.current = null;
    }
    if (audioContextRef.current) {
      try {
        audioContextRef.current.close();
      } catch {
        // ignore
      }
      audioContextRef.current = null;
    }
    analyserRef.current = null;
    silenceStartRef.current = null;
    pcmBufferRef.current = new Float32Array(0);
    accumulatedTextRef.current = "";
    isSendingChunkRef.current = false;
  }, []);

  const cleanupRecorder = useCallback(() => {
    recorderRef.current = null;
    const stream = streamRef.current;
    streamRef.current = null;
    chunksRef.current = [];
    if (stream) {
      for (const track of stream.getTracks()) {
        try {
          track.stop();
        } catch {
          // ignore
        }
      }
    }
  }, []);

  const cleanup = useCallback(() => {
    cleanupAudio();
    cleanupRecorder();
  }, [cleanupAudio, cleanupRecorder]);

  useEffect(() => cleanup, [cleanup]);

  const stopRecording = useCallback(() => {
    const recorder = recorderRef.current;
    if (!recorder) return;
    try {
      recorder.stop();
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setState({ status: "error", message });
      cleanup();
    }
  }, [cleanup]);

  const sendChunk = useCallback(async () => {
    if (isSendingChunkRef.current) return;
    
    const buffer = pcmBufferRef.current;
    if (buffer.length === 0) return;

    const sampleRate = audioContextRef.current?.sampleRate || SAMPLE_RATE_CANDIDATE;
    const chunkSamples = CHUNK_DURATION_S * sampleRate;
    const overlapSamples = CHUNK_OVERLAP_S * sampleRate;

    const end = buffer.length;
    const start = Math.max(0, end - chunkSamples);
    const chunk = buffer.slice(start, end);
    pcmBufferRef.current = buffer.slice(Math.max(0, end - overlapSamples));

    isSendingChunkRef.current = true;
    try {
      const blob = float32ToWavBlob(chunk, sampleRate);
      const response = await voiceSttTranscribe(blob, "voice-chunk.wav");
      const text = (response.text ?? "").trim();
      if (text) {
        const overlap = longestCommonSuffixPrefix(accumulatedTextRef.current, text);
        const trimmedText = text.slice(overlap).trim();
        if (trimmedText) {
          accumulatedTextRef.current += (trimmedText.startsWith(" ") ? "" : " ") + trimmedText;
        }
      }
    } catch (e) {
      console.warn("Chunk transcription background failure:", e);
    } finally {
      isSendingChunkRef.current = false;
    }
  }, []);

  const startRecording = useCallback(async () => {
    if (typeof navigator === "undefined" || !navigator.mediaDevices?.getUserMedia) {
      setState({ status: "error", message: "Microphone recording is not available in this browser." });
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });

      streamRef.current = stream;
      chunksRef.current = [];
      pcmBufferRef.current = new Float32Array(0);
      accumulatedTextRef.current = "";

      const mimeType = pickMediaRecorderMimeType();
      const recorder = mimeType
        ? new MediaRecorder(stream, { mimeType })
        : new MediaRecorder(stream);
      recorderRef.current = recorder;

      recorder.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          chunksRef.current.push(event.data);
        }
      };

      recorder.onstop = () => {
        cleanupAudio();
        const blob = new Blob(chunksRef.current, { type: recorder.mimeType || "audio/webm" });
        void (async () => {
          try {
            setState({ status: "transcribing" });
            const response = await voiceSttTranscribe(blob, "voice-input.webm");
            const text = (response.text ?? "").trim();
            if (!text) {
              setState({ status: "error", message: "Transcription returned empty text." });
              return;
            }
            setState({ status: "idle" });
            onTranscriptRef.current(text);
          } catch (error) {
            const message = error instanceof Error ? error.message : String(error);
            setState({ status: "error", message });
          } finally {
            cleanupRecorder();
          }
        })();
      };

      recorder.onerror = (event) => {
        const message = (event as unknown as { error?: unknown }).error instanceof Error
          ? ((event as unknown as { error: Error }).error.message)
          : "Recording error";
        setState({ status: "error", message });
        cleanup();
      };

      recorder.start();
      const startedAt = Date.now();
      setState({ status: "recording", startedAt });

      maxDurationTimeoutRef.current = window.setTimeout(() => {
        if (recorderRef.current?.state === "recording") {
          stopRecording();
        }
      }, MAX_RECORDING_MS);

      const audioContext = new AudioContext();
      audioContextRef.current = audioContext;
      const source = audioContext.createMediaStreamSource(stream);
      const processor = audioContext.createScriptProcessor(4096, 1, 1);
      processor.onaudioprocess = (e) => {
        const input = e.inputBuffer.getChannelData(0);
        const newBuffer = new Float32Array(pcmBufferRef.current.length + input.length);
        newBuffer.set(pcmBufferRef.current);
        newBuffer.set(input, pcmBufferRef.current.length);
        pcmBufferRef.current = newBuffer;
      };
      source.connect(processor);
      processor.connect(audioContext.destination);
      processorRef.current = processor;

      chunkTimerRef.current = window.setInterval(sendChunk, CHUNK_DURATION_S * 1000);

      if (conversationMode) {
        const analyser = audioContext.createAnalyser();
        analyser.fftSize = 512;
        analyser.smoothingTimeConstant = 0.3;
        source.connect(analyser);
        analyserRef.current = analyser;
        silenceStartRef.current = null;

        const timeDomainData = new Uint8Array(analyser.fftSize);

        analysisIntervalRef.current = window.setInterval(() => {
          const analyserNode = analyserRef.current;
          if (!analyserNode || recorderRef.current?.state !== "recording") return;

          analyserNode.getByteTimeDomainData(timeDomainData);
          const rms = calculateRMS(timeDomainData);
          const elapsed = Date.now() - startedAt;
          audioLevelRef.current = rms;

          if (rms < silenceThreshold && elapsed >= MIN_RECORDING_MS) {
            if (silenceStartRef.current === null) {
              silenceStartRef.current = Date.now();
            } else if (Date.now() - silenceStartRef.current >= SILENCE_DURATION_MS) {
              stopRecording();
            }
          } else {
            silenceStartRef.current = null;
          }
        }, ANALYSIS_INTERVAL_MS);
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setState({ status: "error", message });
      cleanup();
    }
  }, [cleanup, cleanupAudio, cleanupRecorder, conversationMode, silenceThreshold, stopRecording, sendChunk]);

  const toggle = useCallback(() => {
    if (state.status === "recording") {
      stopRecording();
      return;
    }
    if (state.status === "transcribing") return;
    void startRecording();
  }, [startRecording, state.status, stopRecording]);

  return {
    state,
    toggle,
    startRecording,
    stopRecording,
    audioLevelRef,
  };
}

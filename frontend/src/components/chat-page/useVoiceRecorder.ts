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
    // time domain data is 0-255, centered at 128
    const normalized = (timeDomainData[i] - 128) / 128;
    sum += normalized * normalized;
  }
  return Math.sqrt(sum / timeDomainData.length);
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

      // Hard max duration safety net
      maxDurationTimeoutRef.current = window.setTimeout(() => {
        if (recorderRef.current?.state === "recording") {
          stopRecording();
        }
      }, MAX_RECORDING_MS);

      // Set up silence detection for conversation mode
      if (conversationMode) {
        const audioContext = new AudioContext();
        audioContextRef.current = audioContext;
        const source = audioContext.createMediaStreamSource(stream);
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
              // Silence threshold reached — auto-stop
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
  }, [cleanup, cleanupAudio, cleanupRecorder, conversationMode, silenceThreshold, stopRecording]);

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

import { MouseEvent, useCallback, useEffect, useRef, useState } from "react";
import { Badge } from "@open-hax/uxx";

import { voiceSttTranscribe } from "../../lib/api";

type ConversationVoiceButtonProps = {
  disabled?: boolean;
  onTranscript: (text: string) => void;
  title?: string;
  silenceThreshold?: number;
};

type VoiceState =
  | { status: "idle" }
  | { status: "recording"; startedAt: number }
  | { status: "transcribing" }
  | { status: "error"; message: string };

function pickMimeType(): string | undefined {
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

const SILENCE_THRESHOLD = 0.015;
const SILENCE_DURATION_MS = 1800;
const MIN_RECORDING_MS = 800;
const MAX_RECORDING_MS = 30000;
const ANALYSIS_INTERVAL_MS = 100;

export function ConversationVoiceButton({
  disabled,
  onTranscript,
  title = "Voice (auto-send on pause)",
  silenceThreshold = SILENCE_THRESHOLD,
}: ConversationVoiceButtonProps) {
  const [state, setState] = useState<VoiceState>({ status: "idle" });
  const recorderRef = useRef<MediaRecorder | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const chunksRef = useRef<BlobPart[]>([]);
  const audioCtxRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const silenceStartRef = useRef<number | null>(null);
  const intervalRef = useRef<number | null>(null);
  const maxTimeoutRef = useRef<number | null>(null);
  const onTranscriptRef = useRef(onTranscript);

  onTranscriptRef.current = onTranscript;

  const cleanupAudio = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
    if (maxTimeoutRef.current) {
      clearTimeout(maxTimeoutRef.current);
      maxTimeoutRef.current = null;
    }
    if (audioCtxRef.current) {
      try { audioCtxRef.current.close(); } catch { /* ignore */ }
      audioCtxRef.current = null;
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
        try { track.stop(); } catch { /* ignore */ }
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
      setState({ status: "error", message: "Microphone not available in this browser." });
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true },
      });

      streamRef.current = stream;
      chunksRef.current = [];

      const mimeType = pickMimeType();
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

      maxTimeoutRef.current = window.setTimeout(() => {
        if (recorderRef.current?.state === "recording") {
          stopRecording();
        }
      }, MAX_RECORDING_MS);

      // Silence-based auto-stop
      const audioCtx = new AudioContext();
      audioCtxRef.current = audioCtx;
      const source = audioCtx.createMediaStreamSource(stream);
      const analyser = audioCtx.createAnalyser();
      analyser.fftSize = 512;
      analyser.smoothingTimeConstant = 0.3;
      source.connect(analyser);
      analyserRef.current = analyser;
      silenceStartRef.current = null;

      const timeDomainData = new Uint8Array(analyser.fftSize);

      intervalRef.current = window.setInterval(() => {
        const node = analyserRef.current;
        if (!node || recorderRef.current?.state !== "recording") return;

        node.getByteTimeDomainData(timeDomainData);
        const rms = calculateRMS(timeDomainData);
        const elapsed = Date.now() - startedAt;

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
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setState({ status: "error", message });
      cleanup();
    }
  }, [cleanup, cleanupAudio, cleanupRecorder, silenceThreshold, stopRecording]);

  const handleClick = useCallback((event: MouseEvent<HTMLElement>) => {
    event.preventDefault();
    event.stopPropagation();

    if (state.status === "recording") {
      stopRecording();
      return;
    }
    if (state.status === "transcribing") return;
    void startRecording();
  }, [startRecording, state.status, stopRecording]);

  const glyph = state.status === "recording" ? "⏹️" : state.status === "transcribing" ? "⏳" : "🎙️";

  return (
    <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
      <button
        type="button"
        title={title}
        onClick={handleClick}
        disabled={disabled || state.status === "transcribing"}
        style={{
          width: 28,
          height: 28,
          borderRadius: 6,
          border: "1px solid var(--token-colors-border-default)",
          background: state.status === "recording"
            ? "var(--token-colors-alpha-red-_20)"
            : "var(--token-colors-button-ghost-bg)",
          color: state.status === "recording"
            ? "var(--token-colors-accent-red)"
            : "var(--token-colors-text-muted)",
          cursor: disabled || state.status === "transcribing" ? "not-allowed" : "pointer",
          opacity: disabled || state.status === "transcribing" ? 0.4 : 1,
          fontSize: 14,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          padding: 0,
          lineHeight: 1,
          transition: "background-color 120ms, color 120ms, opacity 120ms",
        }}
      >
        {glyph}
      </button>
      {state.status === "recording" ? <Badge size="sm" variant="warning">rec</Badge> : null}
      {state.status === "error" ? (
        <div style={{ fontSize: 11, color: "var(--token-colors-accent-red)" }} title={state.message}>
          err
        </div>
      ) : null}
    </div>
  );
}

export function VoiceLevelGauge({
  audioLevelRef,
  threshold,
  onThresholdChange,
}: {
  audioLevelRef: React.MutableRefObject<number>;
  threshold: number;
  onThresholdChange: (v: number) => void;
}) {
  const [level, setLevel] = useState(0);

  useEffect(() => {
    let raf: number;
    const tick = () => {
      setLevel(audioLevelRef.current);
      raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [audioLevelRef]);

  const maxLevel = 0.06;
  const pct = Math.min(100, (level / maxLevel) * 100);
  const thresholdPct = Math.min(100, (threshold / maxLevel) * 100);

  return (
    <div style={{ display: "flex", alignItems: "center", gap: 6 }} title={`Level: ${level.toFixed(4)} | Threshold: ${threshold.toFixed(3)}`}>
      <div style={{
        width: 8,
        height: 8,
        borderRadius: "50%",
        background: "var(--token-colors-accent-red)",
        boxShadow: "0 0 6px var(--token-colors-accent-red)",
      }} />
      <div style={{ position: "relative", width: 72, height: 14 }}>
        <div style={{
          position: "absolute",
          inset: 0,
          background: "var(--token-colors-surface-input)",
          borderRadius: 3,
          overflow: "hidden",
          border: "1px solid var(--token-colors-border-subtle)",
        }}>
          <div style={{
            width: `${pct}%`,
            height: "100%",
            background: level > threshold
              ? "var(--token-colors-accent-green)"
              : "var(--token-colors-accent-yellow)",
            transition: "width 40ms linear",
          }} />
          <div style={{
            position: "absolute",
            left: `${thresholdPct}%`,
            top: 0,
            bottom: 0,
            width: 2,
            background: "var(--token-colors-accent-red)",
            transform: "translateX(-50%)",
            opacity: 0.9,
          }} />
        </div>
        <input
          type="range"
          min={0.001}
          max={0.06}
          step={0.001}
          value={threshold}
          onChange={(e) => onThresholdChange(parseFloat(e.target.value))}
          style={{
            position: "absolute",
            inset: 0,
            width: "100%",
            height: "100%",
            opacity: 0,
            cursor: "pointer",
            margin: 0,
            padding: 0,
          }}
        />
      </div>
      <span style={{ fontSize: 9, color: "var(--token-colors-text-muted)", minWidth: 28, fontVariantNumeric: "tabular-nums" }}>
        {threshold.toFixed(3)}
      </span>
    </div>
  );
}

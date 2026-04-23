import { MouseEvent, useCallback, useEffect, useRef, useState } from "react";
import { Badge, Button } from "@open-hax/uxx";

import { voiceSttTranscribe } from "../../lib/api";

type VoiceInputButtonProps = {
  disabled?: boolean;
  onTranscript: (text: string) => void;
  idleLabel?: string;
  recordingLabel?: string;
  transcribingLabel?: string;
  /** Render as a compact icon button instead of a text button */
  iconOnly?: boolean;
  /** Optional title/tooltip for the button */
  title?: string;
};

type VoiceInputState =
  | { status: "idle" }
  | { status: "recording"; startedAt: number }
  | { status: "transcribing" }
  | { status: "error"; message: string };

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

export function VoiceInputButton({
  disabled,
  onTranscript,
  idleLabel = "Reply by voice",
  recordingLabel = "Stop recording",
  transcribingLabel = "Transcribing…",
  iconOnly = false,
  title: buttonTitle,
}: VoiceInputButtonProps) {
  const [state, setState] = useState<VoiceInputState>({ status: "idle" });
  const recorderRef = useRef<MediaRecorder | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const chunksRef = useRef<BlobPart[]>([]);

  const cleanup = useCallback(() => {
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

  useEffect(() => cleanup, [cleanup]);

  const startRecording = useCallback(async () => {
    if (disabled) return;

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
      const recorder = new MediaRecorder(stream, mimeType ? { mimeType } : undefined);
      recorderRef.current = recorder;

      recorder.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          chunksRef.current.push(event.data);
        }
      };

      recorder.onstop = () => {
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
            onTranscript(text);
          } catch (error) {
            const message = error instanceof Error ? error.message : String(error);
            setState({ status: "error", message });
          } finally {
            cleanup();
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
      setState({ status: "recording", startedAt: Date.now() });
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setState({ status: "error", message });
      cleanup();
    }
  }, [cleanup, disabled, onTranscript]);

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

  const glyph = state.status === "recording" ? "⏹️" : state.status === "transcribing" ? "⏳" : "🎤";
  const label = state.status === "recording"
    ? recordingLabel
    : state.status === "transcribing"
      ? transcribingLabel
      : idleLabel;

  if (iconOnly) {
    return (
      <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
        <button
          type="button"
          title={buttonTitle || label}
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
            cursor: "pointer",
            fontSize: 14,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: 0,
            lineHeight: 1,
          }}
        >
          {glyph}
        </button>
        {state.status === "recording" ? <Badge size="sm" variant="warning">rec</Badge> : null}
        {state.status === "error" ? <Badge size="sm" variant="error">{state.message}</Badge> : null}
      </div>
    );
  }

  return (
    <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
      <Button
        type="button"
        variant="ghost"
        size="sm"
        onClick={handleClick}
        disabled={disabled || state.status === "transcribing"}
      >
        {label}
      </Button>
      {state.status === "recording" ? <Badge size="sm" variant="warning">recording</Badge> : null}
      {state.status === "error" ? <Badge size="sm" variant="error">{state.message}</Badge> : null}
    </div>
  );
}

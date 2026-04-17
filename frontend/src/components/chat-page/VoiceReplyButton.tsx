import { useCallback, useEffect, useRef, useState } from "react";
import { Badge, Button } from "@open-hax/uxx";

import { voiceSttTranscribe } from "../../lib/api";

type VoiceReplyButtonProps = {
  disabled?: boolean;
  onTranscript: (text: string) => void;
};

type VoiceReplyState =
  | { status: "idle" }
  | { status: "recording"; startedAt: number }
  | { status: "transcribing" }
  | { status: "error"; message: string };

function pickMediaRecorderMimeType(): string | undefined {
  // Keep this conservative; ffmpeg in the STT container can decode most things.
  // We prefer opus-in-webm when available.
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

export function VoiceReplyButton({ disabled, onTranscript }: VoiceReplyButtonProps) {
  const [state, setState] = useState<VoiceReplyState>({ status: "idle" });
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
            const response = await voiceSttTranscribe(blob, "voice-reply.webm");
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

  const handleClick = useCallback(() => {
    if (state.status === "recording") {
      stopRecording();
      return;
    }

    // While transcribing, we ignore clicks.
    if (state.status === "transcribing") return;

    void startRecording();
  }, [startRecording, state.status, stopRecording]);

  return (
    <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
      <Button
        variant="ghost"
        size="sm"
        onClick={handleClick}
        disabled={disabled || state.status === "transcribing"}
      >
        {state.status === "recording" ? "Stop recording" : state.status === "transcribing" ? "Transcribing…" : "Reply by voice"}
      </Button>
      {state.status === "recording" ? <Badge size="sm" variant="warning">recording</Badge> : null}
      {state.status === "error" ? <Badge size="sm" variant="error">{state.message}</Badge> : null}
    </div>
  );
}

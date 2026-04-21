import { useCallback, useEffect, useRef, useState } from "react";
import { Badge, Button } from "@open-hax/uxx";

import { voiceTtsSynthesize } from "../../lib/api";

type SpeakAssistantButtonProps = {
  text: string;
  disabled?: boolean;
};

type SpeakState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "playing" }
  | { status: "error"; message: string };

export function SpeakAssistantButton({ text, disabled }: SpeakAssistantButtonProps) {
  const [state, setState] = useState<SpeakState>({ status: "idle" });
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const urlRef = useRef<string | null>(null);

  const cleanup = useCallback(() => {
    const audio = audioRef.current;
    audioRef.current = null;

    if (audio) {
      try {
        audio.pause();
      } catch {
        // ignore
      }
    }

    const url = urlRef.current;
    urlRef.current = null;
    if (url) {
      try {
        URL.revokeObjectURL(url);
      } catch {
        // ignore
      }
    }
  }, []);

  const stop = useCallback(() => {
    cleanup();
    setState({ status: "idle" });
  }, [cleanup]);

  useEffect(() => cleanup, [cleanup]);

  const speak = useCallback(async () => {
    if (disabled) return;

    if (state.status === "loading" || state.status === "playing") {
      stop();
      return;
    }

    const plainText = (text ?? "").trim();
    if (!plainText) {
      setState({ status: "error", message: "Nothing to speak." });
      return;
    }

    try {
      setState({ status: "loading" });
      const blob = await voiceTtsSynthesize({ text: plainText });
      const url = URL.createObjectURL(blob);
      urlRef.current = url;

      const audio = new Audio(url);
      audioRef.current = audio;

      audio.onended = () => stop();
      audio.onerror = () => {
        cleanup();
        setState({ status: "error", message: "Audio playback failed." });
      };

      await audio.play();
      setState({ status: "playing" });
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      cleanup();
      setState({ status: "error", message });
    }
  }, [disabled, state.status, stop, text]);

  return (
    <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
      <Button
        variant="ghost"
        size="sm"
        onClick={() => void speak()}
        disabled={disabled || state.status === "loading"}
      >
        {state.status === "playing" ? "Stop" : state.status === "loading" ? "Speaking…" : "Speak"}
      </Button>
      {state.status === "error" ? <Badge size="sm" variant="error">{state.message}</Badge> : null}
    </div>
  );
}

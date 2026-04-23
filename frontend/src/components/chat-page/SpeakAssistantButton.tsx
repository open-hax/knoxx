import { useCallback, useEffect, useRef, useState } from "react";
import { Badge, Button } from "@open-hax/uxx";

import { voiceTtsSynthesize } from "../../lib/api";

type SpeakAssistantButtonProps = {
  text: string;
  disabled?: boolean;
  /** Render as a compact icon button instead of a text button */
  iconOnly?: boolean;
  /** Optional title/tooltip for the button */
  title?: string;
};

type SpeakState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "playing" }
  | { status: "error"; message: string };

export function SpeakAssistantButton({ text, disabled, iconOnly = false, title: buttonTitle }: SpeakAssistantButtonProps) {
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

  const glyph = state.status === "playing" ? "🔇" : state.status === "loading" ? "⏳" : "🔊";
  const label = state.status === "playing" ? "Stop" : state.status === "loading" ? "Speaking…" : "Speak";

  if (iconOnly) {
    return (
      <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
        <button
          type="button"
          title={buttonTitle || label}
          onClick={() => void speak()}
          disabled={disabled || state.status === "loading"}
          style={{
            width: 28,
            height: 28,
            borderRadius: 6,
            border: "1px solid var(--token-colors-border-default)",
            background: state.status === "playing"
              ? "var(--token-colors-alpha-green-_20)"
              : "var(--token-colors-button-ghost-bg)",
            color: state.status === "playing"
              ? "var(--token-colors-accent-green)"
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
        {state.status === "error" ? <Badge size="sm" variant="error">{state.message}</Badge> : null}
      </div>
    );
  }

  return (
    <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
      <Button
        variant="ghost"
        size="sm"
        onClick={() => void speak()}
        disabled={disabled || state.status === "loading"}
      >
        {label}
      </Button>
      {state.status === "error" ? <Badge size="sm" variant="error">{state.message}</Badge> : null}
    </div>
  );
}

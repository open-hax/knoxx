import { MouseEvent, useCallback } from "react";
import { Badge, Button } from "@open-hax/uxx";

import { useVoiceRecorder } from "./useVoiceRecorder";

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

export function VoiceInputButton({
  disabled,
  onTranscript,
  idleLabel = "Reply by voice",
  recordingLabel = "Stop recording",
  transcribingLabel = "Transcribing…",
  iconOnly = false,
  title: buttonTitle,
}: VoiceInputButtonProps) {
  const {
    state,
    toggle,
    startRecording,
    stopRecording,
  } = useVoiceRecorder({
    onTranscript,
    conversationMode: false, // Push-to-talk doesn't need silence detection
  });

  const handleClick = useCallback((event: MouseEvent<HTMLElement>) => {
    event.preventDefault();
    event.stopPropagation();
    toggle();
  }, [toggle]);

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

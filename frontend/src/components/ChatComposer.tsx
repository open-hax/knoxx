import { FormEvent, KeyboardEvent, useState, useCallback, useRef, useEffect } from "react";
import { Badge } from "@open-hax/uxx";
import { MultimodalInput, type MultimodalAttachment } from "./chat-page/MultimodalInput";
import { VoiceInputButton } from "./chat-page/VoiceInputButton";
import { ConversationVoiceButton } from "./chat-page/ConversationVoiceButton";
import { SpeakAssistantButton } from "./chat-page/SpeakAssistantButton";
import type { ContentPart } from "../lib/types";

interface ChatComposerProps {
  onSend: (text: string, contentParts?: ContentPart[]) => void;
  isSending: boolean;
  /** Enable multimodal file uploads (images, audio, video, documents) */
  multimodalEnabled?: boolean;
  voiceInputEnabled?: boolean;
  liveControlEnabled?: boolean;
  liveControlText?: string;
  onLiveControlTextChange?: (value: string) => void;
  queueingControl?: "steer" | "follow_up" | null;
  onQueueLiveControl?: (kind: "steer" | "follow_up") => void | Promise<void>;
  onVoiceSteer?: (text: string) => void | Promise<void>;
  abortingTurn?: boolean;
  onAbortTurn?: () => void | Promise<void>;
  /** Latest assistant message content for the speak button */
  latestAssistantContent?: string;
  /** Auto-conversation voice toggle */
  autoConversationEnabled?: boolean;
  onToggleAutoConversation?: () => void;
  ttsEnabled?: boolean;
  ttsStatus?: string;
  ttsError?: string | null;
  /** Auto-recording is active (hands-free mic loop) */
  autoRecording?: boolean;
  /** Voice detection threshold for silence-based auto-stop */
  voiceThreshold?: number;
  onVoiceThresholdChange?: (value: number) => void;
  /** Live audio level from the voice recorder analyser */
  audioLevelRef?: React.MutableRefObject<number>;
  /** Undo last turn */
  onUndoMessages?: () => void | Promise<void>;
  undoDisabled?: boolean;
  /** Start a new chat */
  onNewChat?: () => void;
}

/**
 * Convert MultimodalAttachment to ContentPart for API transmission
 */
function attachmentToContentPart(attachment: MultimodalAttachment): Promise<ContentPart> {
  return new Promise((resolve) => {
    const type = attachment.type;
    const base: Omit<ContentPart, "data" | "url"> = {
      type,
      mimeType: attachment.file.type,
      filename: attachment.file.name,
      size: attachment.file.size,
    };

    if (attachment.preview) {
      // For images, use the data URL directly
      if (type === "image" && attachment.preview.startsWith("data:")) {
        resolve({ ...base, data: attachment.preview });
        return;
      }
      // For audio/video, we have object URLs - need to convert to base64
      if ((type === "audio" || type === "video") && attachment.preview.startsWith("blob:")) {
        fetch(attachment.preview)
          .then((res) => res.blob())
          .then((blob) => {
            const reader = new FileReader();
            reader.onload = () => {
              resolve({ ...base, data: reader.result as string });
            };
            reader.onerror = () => {
              // Fallback to URL if base64 conversion fails
              resolve({ ...base, url: attachment.preview });
            };
            reader.readAsDataURL(blob);
          })
          .catch(() => {
            resolve({ ...base, url: attachment.preview });
          });
        return;
      }
      // Fallback
      resolve({ ...base, url: attachment.preview });
      return;
    }

    // No preview - read file as base64
    const reader = new FileReader();
    reader.onload = () => {
      resolve({ ...base, data: reader.result as string });
    };
    reader.onerror = () => {
      resolve({ ...base });
    };
    reader.readAsDataURL(attachment.file);
  });
}

function GlyphButton({
  glyph,
  title,
  onClick,
  disabled,
  active,
  type = "button",
}: {
  glyph: string;
  title: string;
  onClick?: () => void;
  disabled?: boolean;
  active?: boolean;
  type?: "button" | "submit";
}) {
  return (
    <button
      type={type}
      title={title}
      onClick={onClick}
      disabled={disabled}
      style={{
        width: 28,
        height: 28,
        borderRadius: 6,
        border: "1px solid var(--token-colors-border-default)",
        background: active
          ? "var(--token-colors-alpha-green-_20)"
          : "var(--token-colors-button-ghost-bg)",
        color: active
          ? "var(--token-colors-accent-green)"
          : "var(--token-colors-text-muted)",
        cursor: disabled ? "not-allowed" : "pointer",
        opacity: disabled ? 0.4 : 1,
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
  );
}

function VoiceLevelGauge({
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

function ChatComposer({
  onSend,
  isSending,
  multimodalEnabled = true,
  voiceInputEnabled = false,
  liveControlEnabled = false,
  liveControlText = "",
  onLiveControlTextChange,
  queueingControl = null,
  onQueueLiveControl,
  abortingTurn = false,
  onAbortTurn,
  onVoiceSteer,
  latestAssistantContent,
  autoConversationEnabled = false,
  onToggleAutoConversation,
  ttsEnabled = false,
  ttsStatus = "idle",
  ttsError = null,
  autoRecording = false,
  voiceThreshold = 0.015,
  onVoiceThresholdChange,
  audioLevelRef,
  onUndoMessages,
  undoDisabled = false,
  onNewChat,
}: ChatComposerProps) {
  const [value, setValue] = useState("");
  const [attachments, setAttachments] = useState<MultimodalAttachment[]>([]);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const handleSubmit = useCallback(async (event: FormEvent) => {
    event.preventDefault();
    const trimmed = value.trim();
    if ((!trimmed && attachments.length === 0) || isSending) {
      return;
    }

    // Convert attachments to content parts
    let contentParts: ContentPart[] | undefined;
    if (attachments.length > 0) {
      contentParts = await Promise.all(attachments.map(attachmentToContentPart));
      // Add text as first content part if present
      if (trimmed) {
        contentParts.unshift({ type: "text", text: trimmed });
      }
    }

    onSend(trimmed, contentParts);
    setValue("");
    setAttachments([]);
  }, [value, attachments, isSending, onSend]);

  const handleKeyDown = useCallback((event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      if (liveControlEnabled) {
        const trimmed = liveControlText.trim();
        if (!trimmed || queueingControl !== null) return;
        if (event.ctrlKey) {
          void onQueueLiveControl?.("follow_up");
        } else {
          void onQueueLiveControl?.("steer");
        }
        return;
      }
      const trimmed = value.trim();
      if ((trimmed || attachments.length > 0) && !isSending) {
        handleSubmit(event as unknown as FormEvent);
      }
    }
  }, [liveControlEnabled, liveControlText, queueingControl, onQueueLiveControl, value, attachments, isSending, handleSubmit]);

  // Register global paste handler
  useEffect(() => {
    if (typeof window === "undefined" || !multimodalEnabled) return;
    const handlePaste = () => {
      setTimeout(() => textareaRef.current?.focus(), 0);
    };
    window.addEventListener("paste", handlePaste);
    return () => window.removeEventListener("paste", handlePaste);
  }, [multimodalEnabled]);

  const canSendNormal = (value.trim() || attachments.length > 0) && !isSending;
  const canSteer = liveControlText.trim().length > 0 && queueingControl === null;

  return (
    <form onSubmit={handleSubmit}>
      {/* Attachment previews */}
      {attachments.length > 0 && !liveControlEnabled && (
        <div
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: 8,
            marginBottom: 8,
            padding: 8,
            background: "var(--token-colors-background-elevated, #1a1a2e)",
            borderRadius: 8,
            border: "1px solid var(--token-colors-border-default, #333)",
          }}
        >
          {attachments.map((att) => (
            <div
              key={att.id}
              style={{
                position: "relative",
                borderRadius: 6,
                overflow: "hidden",
                border: "1px solid var(--token-colors-border-subtle, #444)",
                background: "var(--token-colors-background-surface, #16213e)",
              }}
            >
              {/* Preview */}
              {att.type === "image" && att.preview && (
                <img
                  src={att.preview}
                  alt={att.file.name}
                  style={{ width: 80, height: 80, objectFit: "cover", display: "block" }}
                />
              )}
              {att.type === "audio" && (
                <div
                  style={{
                    width: 160,
                    height: 60,
                    display: "flex",
                    alignItems: "center",
                    padding: "0 8px",
                    gap: 8,
                  }}
                >
                  <span style={{ fontSize: 20 }}>🎵</span>
                  <span
                    style={{
                      fontSize: 11,
                      overflow: "hidden",
                      textOverflow: "ellipsis",
                      whiteSpace: "nowrap",
                      color: "var(--token-colors-text-muted)",
                    }}
                  >
                    {att.file.name.slice(0, 20)}
                  </span>
                </div>
              )}
              {att.type === "video" && att.preview && (
                <video
                  src={att.preview}
                  style={{ width: 120, height: 80, display: "block", objectFit: "cover" }}
                />
              )}
              {att.type === "document" && (
                <div
                  style={{
                    width: 80,
                    height: 80,
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    justifyContent: "center",
                    gap: 4,
                  }}
                >
                  <span style={{ fontSize: 24 }}>📄</span>
                  <span
                    style={{
                      fontSize: 9,
                      overflow: "hidden",
                      textOverflow: "ellipsis",
                      whiteSpace: "nowrap",
                      maxWidth: 72,
                      color: "var(--token-colors-text-muted)",
                    }}
                  >
                    {att.file.name.slice(0, 16)}
                  </span>
                </div>
              )}

              {/* Remove button */}
              <button
                type="button"
                onClick={() =>
                  setAttachments((prev) => prev.filter((a) => a.id !== att.id))
                }
                style={{
                  position: "absolute",
                  top: 2,
                  right: 2,
                  width: 18,
                  height: 18,
                  borderRadius: "50%",
                  border: "none",
                  background: "rgba(0, 0, 0, 0.7)",
                  color: "white",
                  cursor: "pointer",
                  fontSize: 12,
                  lineHeight: 1,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                }}
                title="Remove attachment"
              >
                ×
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Textarea */}
      <textarea
        ref={textareaRef}
        rows={liveControlEnabled ? 2 : 3}
        className={`input resize-y w-full ${liveControlEnabled ? "min-h-14" : "min-h-20"}`}
        value={liveControlEnabled ? liveControlText : value}
        onChange={(event) => {
          if (liveControlEnabled) {
            onLiveControlTextChange?.(event.target.value);
          } else {
            setValue(event.target.value);
          }
        }}
        onKeyDown={handleKeyDown}
        disabled={liveControlEnabled ? queueingControl !== null : isSending}
        placeholder={
          liveControlEnabled
            ? "Steer the current turn (Enter) or queue a follow-up (Ctrl+Enter)..."
            : multimodalEnabled
              ? "Send a message, or drag/paste images, audio, video..."
              : "Send a prompt, test edge cases, compare behavior..."
        }
      />

      {/* Bottom toolbar — ALL buttons */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: 6,
          flexWrap: "wrap",
          marginTop: 8,
        }}
      >
        {/* Left-side actions */}
        {!liveControlEnabled && multimodalEnabled && (
          <MultimodalInput
            attachments={attachments}
            onAttachmentsChange={setAttachments}
            disabled={isSending}
            hidePreviews
          />
        )}

        {/* Voice input */}
        {voiceInputEnabled && (
          liveControlEnabled ? (
            <ConversationVoiceButton
              disabled={queueingControl !== null}
              onTranscript={(text) => void onVoiceSteer?.(text)}
              title="Voice steer (auto-send on pause)"
              silenceThreshold={voiceThreshold}
            />
          ) : autoConversationEnabled ? (
            <ConversationVoiceButton
              disabled={isSending || autoRecording}
              onTranscript={(text) => onSend(text)}
              title="Voice reply (auto-send on pause)"
              silenceThreshold={voiceThreshold}
            />
          ) : (
            <VoiceInputButton
              disabled={isSending}
              onTranscript={(text) => onSend(text)}
              idleLabel="Speak"
              recordingLabel="Stop"
              transcribingLabel="Transcribing…"
              iconOnly
              title="Voice input (sends immediately)"
            />
          )
        )}

        {latestAssistantContent && latestAssistantContent.trim().length > 0 && (
          <SpeakAssistantButton
            text={latestAssistantContent}
            iconOnly
            title="Speak latest assistant reply"
          />
        )}

        <GlyphButton
          glyph="↩️"
          title="Undo last turn"
          onClick={() => void onUndoMessages?.()}
          disabled={undoDisabled}
        />

        <GlyphButton
          glyph="🗑️"
          title="New chat"
          onClick={() => onNewChat?.()}
        />

        <div style={{ flex: 1 }} />

        {/* Right-side actions */}
        {ttsEnabled && (
          <GlyphButton
            glyph="🔊"
            title={autoConversationEnabled ? "Auto voice on" : "Auto voice off"}
            onClick={() => onToggleAutoConversation?.()}
            active={autoConversationEnabled}
          />
        )}

        {autoRecording && audioLevelRef && onVoiceThresholdChange ? (
          <VoiceLevelGauge
            audioLevelRef={audioLevelRef}
            threshold={voiceThreshold}
            onThresholdChange={onVoiceThresholdChange}
          />
        ) : null}

        {ttsStatus !== "idle" ? (
          <Badge
            size="sm"
            variant={ttsStatus === "error"
              ? "error"
              : ttsStatus === "playing" || ttsStatus === "streaming"
                ? "success"
                : "warning"}
          >
            {ttsStatus}
          </Badge>
        ) : null}

        {ttsError ? (
          <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>
            {ttsError}
          </div>
        ) : null}

        {liveControlEnabled ? (
          <>
            <GlyphButton
              glyph="➤"
              title="Steer (Enter)"
              onClick={() => void onQueueLiveControl?.("steer")}
              disabled={!canSteer}
            />
            <GlyphButton
              glyph="⏭"
              title="Queue follow-up (Ctrl+Enter)"
              onClick={() => void onQueueLiveControl?.("follow_up")}
              disabled={!canSteer}
            />
            <GlyphButton
              glyph="✕"
              title="Abort turn"
              onClick={() => void onAbortTurn?.()}
              disabled={abortingTurn || queueingControl !== null}
            />
          </>
        ) : (
          <GlyphButton
            glyph="➤"
            title="Send"
            type="submit"
            disabled={!canSendNormal}
          />
        )}
      </div>
    </form>
  );
}

export default ChatComposer;

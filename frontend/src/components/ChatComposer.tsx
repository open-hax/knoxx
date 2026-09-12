import { FormEvent, KeyboardEvent, useState, useCallback, useRef, useEffect } from "react";
import { Badge } from "@open-hax/uxx";
import { MultimodalInput, type MultimodalAttachment } from "./chat-page/MultimodalInput";
import { VoiceInputButton } from "./chat-page/VoiceInputButton";
import { ConversationVoiceButton, VoiceLevelGauge } from "./chat-page/ConversationVoiceButton";
import { SpeakAssistantButton } from "./chat-page/SpeakAssistantButton";
import type { ContentPart } from "../lib/types";
import type { ChatComposerProps } from "./chat-page/ChatWorkspacePane";
import { attachmentToContentPart } from "../lib/mediaEmbeds";

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
      className={`knoxx-chat-glyph${active ? " knoxx-chat-glyph-active" : ""}`}
    >
      {glyph}
    </button>
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
        <div className="knoxx-chat-attachments">
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
      <div className="knoxx-chat-toolbar">
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

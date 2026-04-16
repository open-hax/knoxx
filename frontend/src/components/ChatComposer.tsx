import { FormEvent, KeyboardEvent, useState, useCallback, useRef } from "react";
import { MultimodalInput, type MultimodalAttachment } from "./chat-page/MultimodalInput";
import type { ContentPart } from "../lib/types";

interface ChatComposerProps {
  onSend: (text: string, contentParts?: ContentPart[]) => void;
  isSending: boolean;
  /** Enable multimodal file uploads (images, audio, video, documents) */
  multimodalEnabled?: boolean;
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

function ChatComposer({ onSend, isSending, multimodalEnabled = true }: ChatComposerProps) {
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
      const trimmed = value.trim();
      if ((trimmed || attachments.length > 0) && !isSending) {
        handleSubmit(event as unknown as FormEvent);
      }
    }
  }, [value, attachments, isSending, handleSubmit]);

  const handlePaste = useCallback((e: ClipboardEvent) => {
    // Focus textarea after paste to continue typing
    setTimeout(() => textareaRef.current?.focus(), 0);
  }, []);

  // Register global paste handler
  useState(() => {
    if (typeof window !== "undefined" && multimodalEnabled) {
      window.addEventListener("paste", handlePaste);
      return () => window.removeEventListener("paste", handlePaste);
    }
  });

  return (
    <form className="mt-3" onSubmit={handleSubmit}>
      {/* Attachment previews */}
      {attachments.length > 0 && (
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

      <div className="flex gap-2 items-end">
        {/* Multimodal input button */}
        {multimodalEnabled && (
          <MultimodalInput
            attachments={attachments}
            onAttachmentsChange={setAttachments}
            disabled={isSending}
          />
        )}

        <textarea
          ref={textareaRef}
          rows={3}
          className="input min-h-20 flex-1 resize-y"
          value={value}
          onChange={(event) => setValue(event.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={
            multimodalEnabled
              ? "Send a message, or drag/paste images, audio, video..."
              : "Send a prompt, test edge cases, compare behavior..."
          }
        />
        <button type="submit" className="btn-primary h-fit" disabled={isSending}>
          {isSending ? "Sending..." : "Send"}
        </button>
      </div>
    </form>
  );
}

export default ChatComposer;

/**
 * MultimodalInput Component
 *
 * File upload input supporting images, audio, video, and documents.
 * Provides drag-and-drop, clipboard paste, and file picker interfaces.
 */

import { useState, useRef, useCallback, type ChangeEvent, type DragEvent } from "react";
import { Button, Badge } from "@open-hax/uxx";

export interface MultimodalAttachment {
  id: string;
  file: File;
  preview?: string; // Data URL for images, object URL for audio/video
  type: "image" | "audio" | "video" | "document";
  uploading?: boolean;
  error?: string;
}

interface MultimodalInputProps {
  attachments: MultimodalAttachment[];
  onAttachmentsChange: (attachments: MultimodalAttachment[]) => void;
  maxSizeBytes?: number;
  accept?: Record<string, string[]>;
  disabled?: boolean;
}

const DEFAULT_MAX_SIZE = 50 * 1024 * 1024; // 50MB

const DEFAULT_ACCEPT: Record<string, string[]> = {
  "image/*": [".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"],
  "audio/*": [".mp3", ".wav", ".ogg", ".m4a", ".flac", ".aac"],
  "video/*": [".mp4", ".webm", ".mov", ".avi", ".mkv"],
  "application/pdf": [".pdf"],
  "text/*": [".txt", ".md", ".json", ".csv"],
};

function getAttachmentType(file: File): MultimodalAttachment["type"] {
  if (file.type.startsWith("image/")) return "image";
  if (file.type.startsWith("audio/")) return "audio";
  if (file.type.startsWith("video/")) return "video";
  return "document";
}

function generateId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}

async function createPreview(file: File, type: MultimodalAttachment["type"]): Promise<string | undefined> {
  if (type === "image") {
    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = () => resolve(undefined);
      reader.readAsDataURL(file);
    });
  }
  if (type === "audio" || type === "video") {
    return URL.createObjectURL(file);
  }
  return undefined;
}

export function MultimodalInput({
  attachments,
  onAttachmentsChange,
  maxSizeBytes = DEFAULT_MAX_SIZE,
  accept = DEFAULT_ACCEPT,
  disabled = false,
}: MultimodalInputProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [dragCounter, setDragCounter] = useState(0);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const dropZoneRef = useRef<HTMLDivElement>(null);

  const processFiles = useCallback(async (files: FileList | File[]) => {
    const fileArray = Array.from(files);
    const newAttachments: MultimodalAttachment[] = [];

    for (const file of fileArray) {
      const type = getAttachmentType(file);
      
      // Check file size
      if (file.size > maxSizeBytes) {
        newAttachments.push({
          id: generateId(),
          file,
          type,
          error: `File too large (${(file.size / 1024 / 1024).toFixed(1)}MB). Max: ${(maxSizeBytes / 1024 / 1024).toFixed(0)}MB`,
        });
        continue;
      }

      const preview = await createPreview(file, type);
      newAttachments.push({
        id: generateId(),
        file,
        preview,
        type,
      });
    }

    onAttachmentsChange([...attachments, ...newAttachments]);
  }, [attachments, maxSizeBytes, onAttachmentsChange]);

  const handleFileSelect = useCallback((e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      processFiles(e.target.files);
      e.target.value = ""; // Reset for re-selecting same file
    }
  }, [processFiles]);

  const handleDragEnter = useCallback((e: DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragCounter((c) => c + 1);
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e: DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragCounter((c) => {
      const next = c - 1;
      if (next === 0) setIsDragging(false);
      return next;
    });
  }, []);

  const handleDragOver = useCallback((e: DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  }, []);

  const handleDrop = useCallback((e: DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    setDragCounter(0);

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      processFiles(e.dataTransfer.files);
    }
  }, [processFiles]);

  const handlePaste = useCallback(async (e: ClipboardEvent) => {
    const items = e.clipboardData?.items;
    if (!items) return;

    const files: File[] = [];
    for (const item of Array.from(items)) {
      if (item.kind === "file") {
        const file = item.getAsFile();
        if (file) files.push(file);
      }
    }

    if (files.length > 0) {
      await processFiles(files);
    }
  }, [processFiles]);

  // Register paste handler
  useState(() => {
    if (typeof window !== "undefined") {
      window.addEventListener("paste", handlePaste);
      return () => window.removeEventListener("paste", handlePaste);
    }
  });

  const removeAttachment = useCallback((id: string) => {
    const attachment = attachments.find((a) => a.id === id);
    if (attachment?.preview && (attachment.type === "audio" || attachment.type === "video")) {
      URL.revokeObjectURL(attachment.preview);
    }
    onAttachmentsChange(attachments.filter((a) => a.id !== id));
  }, [attachments, onAttachmentsChange]);

  const openFileDialog = useCallback(() => {
    fileInputRef.current?.click();
  }, []);

  const formatSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes}B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`;
    return `${(bytes / 1024 / 1024).toFixed(1)}MB`;
  };

  const typeColors: Record<MultimodalAttachment["type"], string> = {
    image: "var(--token-colors-alpha-green-_30)",
    audio: "var(--token-colors-alpha-purple-_30)",
    video: "var(--token-colors-alpha-blue-_30)",
    document: "var(--token-colors-alpha-orange-_30)",
  };

  const typeIcons: Record<MultimodalAttachment["type"], string> = {
    image: "🖼️",
    audio: "🎵",
    video: "🎬",
    document: "📄",
  };

  return (
    <div
      ref={dropZoneRef}
      onDragEnter={handleDragEnter}
      onDragLeave={handleDragLeave}
      onDragOver={handleDragOver}
      onDrop={handleDrop}
      style={{
        position: "relative",
      }}
    >
      {/* Hidden file input */}
      <input
        ref={fileInputRef}
        type="file"
        multiple
        accept={Object.entries(accept).flatMap(([_, exts]) => exts).join(",")}
        onChange={handleFileSelect}
        style={{ display: "none" }}
        disabled={disabled}
      />

      {/* Drop zone overlay */}
      {isDragging && (
        <div
          style={{
            position: "absolute",
            inset: 0,
            background: "rgba(0, 150, 255, 0.1)",
            border: "2px dashed var(--token-colors-alpha-blue-_50)",
            borderRadius: 8,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 10,
            pointerEvents: "none",
          }}
        >
          <div style={{ fontSize: 16, fontWeight: 600, color: "var(--token-colors-text-default)" }}>
            Drop files here
          </div>
        </div>
      )}

      {/* Attachment button */}
      <Button
        variant="ghost"
        size="sm"
        onClick={openFileDialog}
        disabled={disabled}
        title="Attach files (images, audio, video, documents)"
      >
        📎
      </Button>

      {/* Attachment previews */}
      {attachments.length > 0 && (
        <div
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: 8,
            marginTop: 8,
            padding: 8,
            background: "var(--token-colors-background-elevated)",
            borderRadius: 8,
          }}
        >
          {attachments.map((attachment) => (
            <div
              key={attachment.id}
              style={{
                position: "relative",
                border: `1px solid ${typeColors[attachment.type]}`,
                borderRadius: 8,
                overflow: "hidden",
                background: "var(--token-colors-background-surface)",
              }}
            >
              {/* Preview content */}
              {attachment.type === "image" && attachment.preview && (
                <img
                  src={attachment.preview}
                  alt={attachment.file.name}
                  style={{
                    width: 100,
                    height: 100,
                    objectFit: "cover",
                    display: "block",
                  }}
                />
              )}
              {attachment.type === "audio" && (
                <div
                  style={{
                    width: 200,
                    height: 60,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    background: "var(--token-colors-background-canvas)",
                  }}
                >
                  <audio
                    src={attachment.preview}
                    controls
                    style={{ width: "100%", height: 40 }}
                  />
                </div>
              )}
              {attachment.type === "video" && (
                <video
                  src={attachment.preview}
                  controls
                  style={{
                    width: 160,
                    height: 100,
                    display: "block",
                  }}
                />
              )}
              {attachment.type === "document" && (
                <div
                  style={{
                    width: 100,
                    height: 100,
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    justifyContent: "center",
                    padding: 8,
                  }}
                >
                  <span style={{ fontSize: 24 }}>{typeIcons[attachment.type]}</span>
                  <span
                    style={{
                      fontSize: 10,
                      textAlign: "center",
                      wordBreak: "break-all",
                      marginTop: 4,
                      color: "var(--token-colors-text-muted)",
                    }}
                  >
                    {attachment.file.name.slice(0, 20)}
                  </span>
                </div>
              )}

              {/* File info overlay */}
              <div
                style={{
                  position: "absolute",
                  bottom: 0,
                  left: 0,
                  right: 0,
                  background: "rgba(0,0,0,0.7)",
                  padding: "2px 4px",
                  fontSize: 9,
                  color: "white",
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                }}
              >
                <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                  {formatSize(attachment.file.size)}
                </span>
                <Badge size="sm" variant="default">
                  {attachment.type}
                </Badge>
              </div>

              {/* Remove button */}
              <button
                type="button"
                onClick={() => removeAttachment(attachment.id)}
                style={{
                  position: "absolute",
                  top: 2,
                  right: 2,
                  width: 20,
                  height: 20,
                  borderRadius: "50%",
                  border: "none",
                  background: "rgba(0,0,0,0.7)",
                  color: "white",
                  cursor: "pointer",
                  fontSize: 12,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                }}
                title="Remove"
              >
                ×
              </button>

              {/* Error indicator */}
              {attachment.error && (
                <div
                  style={{
                    position: "absolute",
                    inset: 0,
                    background: "rgba(255,0,0,0.2)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    padding: 4,
                  }}
                >
                  <span style={{ fontSize: 10, color: "var(--token-colors-status-eating)", textAlign: "center" }}>
                    {attachment.error}
                  </span>
                </div>
              )}

              {/* Uploading indicator */}
              {attachment.uploading && (
                <div
                  style={{
                    position: "absolute",
                    inset: 0,
                    background: "rgba(0,0,0,0.5)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  <span style={{ fontSize: 12, color: "white" }}>Uploading...</span>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default MultimodalInput;

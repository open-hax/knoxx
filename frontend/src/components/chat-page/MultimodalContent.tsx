/**
 * MultimodalContent Component
 *
 * Renders multimodal content (images, audio, video, documents) in chat messages.
 * Supports inline playback for audio/video and lightbox for images.
 */

import { useState, useCallback, useRef, useEffect } from "react";
import { Badge, Button } from "@open-hax/uxx";

export interface ContentPart {
  type: "text" | "image" | "audio" | "video" | "document";
  text?: string;
  url?: string;
  data?: string; // Base64 data URL
  mimeType?: string;
  filename?: string;
  size?: number;
}

interface MultimodalContentProps {
  parts: ContentPart[];
  onOpenInLightbox?: (url: string) => void;
  maxPreviewWidth?: number;
  maxPreviewHeight?: number;
}

interface ImageLightboxProps {
  src: string;
  alt?: string;
  onClose: () => void;
}

function ImageLightbox({ src, alt, onClose }: ImageLightboxProps) {
  const handleBackdropClick = useCallback((e: React.MouseEvent) => {
    if (e.target === e.currentTarget) onClose();
  }, [onClose]);

  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleEscape);
    return () => window.removeEventListener("keydown", handleEscape);
  }, [onClose]);

  return (
    <div
      onClick={handleBackdropClick}
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(0, 0, 0, 0.9)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 1000,
        padding: 40,
      }}
    >
      <button
        onClick={onClose}
        style={{
          position: "absolute",
          top: 20,
          right: 20,
          width: 40,
          height: 40,
          borderRadius: "50%",
          border: "none",
          background: "rgba(255,255,255,0.1)",
          color: "white",
          fontSize: 24,
          cursor: "pointer",
        }}
      >
        ×
      </button>
      <img
        src={src}
        alt={alt || "Image preview"}
        style={{
          maxWidth: "100%",
          maxHeight: "100%",
          objectFit: "contain",
          borderRadius: 8,
        }}
      />
    </div>
  );
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes}B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)}MB`;
}

function ImagePart({ 
  part, 
  onOpenInLightbox,
  maxWidth = 400,
  maxHeight = 300,
}: { 
  part: ContentPart; 
  onOpenInLightbox?: (url: string) => void;
  maxWidth?: number;
  maxHeight?: number;
}) {
  const src = part.data || part.url;
  if (!src) return null;

  return (
    <div
      style={{
        marginTop: 8,
        borderRadius: 8,
        overflow: "hidden",
        border: "1px solid var(--token-colors-border-default)",
      }}
    >
      <img
        src={src}
        alt={part.filename || "Image"}
        onClick={() => onOpenInLightbox?.(src)}
        style={{
          maxWidth,
          maxHeight,
          display: "block",
          cursor: onOpenInLightbox ? "pointer" : "default",
        }}
      />
      {part.filename && (
        <div
          style={{
            padding: "4px 8px",
            fontSize: 11,
            color: "var(--token-colors-text-muted)",
            background: "var(--token-colors-background-elevated)",
          }}
        >
          {part.filename}
          {part.size && ` • ${formatSize(part.size)}`}
        </div>
      )}
    </div>
  );
}

function AudioPart({ part }: { part: ContentPart }) {
  const src = part.data || part.url;
  if (!src) return null;

  return (
    <div
      style={{
        marginTop: 8,
        borderRadius: 8,
        overflow: "hidden",
        border: "1px solid var(--token-colors-border-default)",
        background: "var(--token-colors-background-elevated)",
        padding: 12,
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 8 }}>
        <span style={{ fontSize: 20 }}>🎵</span>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 12, fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
            {part.filename || "Audio file"}
          </div>
          {part.size && (
            <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)" }}>
              {formatSize(part.size)}
            </div>
          )}
        </div>
      </div>
      <audio
        src={src}
        controls
        style={{ width: "100%" }}
        preload="metadata"
      />
    </div>
  );
}

function VideoPart({ 
  part,
  maxWidth = 480,
  maxHeight = 360,
}: { 
  part: ContentPart;
  maxWidth?: number;
  maxHeight?: number;
}) {
  const src = part.data || part.url;
  if (!src) return null;

  return (
    <div
      style={{
        marginTop: 8,
        borderRadius: 8,
        overflow: "hidden",
        border: "1px solid var(--token-colors-border-default)",
      }}
    >
      <video
        src={src}
        controls
        preload="metadata"
        style={{
          maxWidth,
          maxHeight,
          display: "block",
        }}
      />
      {part.filename && (
        <div
          style={{
            padding: "4px 8px",
            fontSize: 11,
            color: "var(--token-colors-text-muted)",
            background: "var(--token-colors-background-elevated)",
          }}
        >
          {part.filename}
          {part.size && ` • ${formatSize(part.size)}`}
        </div>
      )}
    </div>
  );
}

function DocumentPart({ part }: { part: ContentPart }) {
  const src = part.data || part.url;
  const isPdf = part.mimeType === "application/pdf" || part.filename?.endsWith(".pdf");

  return (
    <div
      style={{
        marginTop: 8,
        borderRadius: 8,
        overflow: "hidden",
        border: "1px solid var(--token-colors-border-default)",
        background: "var(--token-colors-background-elevated)",
        padding: 12,
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
        <span style={{ fontSize: 32 }}>📄</span>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 13, fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
            {part.filename || "Document"}
          </div>
          <div style={{ display: "flex", gap: 8, marginTop: 2 }}>
            {part.mimeType && (
              <Badge size="sm" variant="default">{part.mimeType}</Badge>
            )}
            {part.size && (
              <span style={{ fontSize: 10, color: "var(--token-colors-text-muted)" }}>
                {formatSize(part.size)}
              </span>
            )}
          </div>
        </div>
        {src && (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => window.open(src, "_blank")}
          >
            {isPdf ? "Open PDF" : "Download"}
          </Button>
        )}
      </div>
    </div>
  );
}

export function MultimodalContent({
  parts,
  onOpenInLightbox,
  maxPreviewWidth = 400,
  maxPreviewHeight = 300,
}: MultimodalContentProps) {
  const [lightboxSrc, setLightboxSrc] = useState<string | null>(null);

  const handleOpenLightbox = useCallback((url: string) => {
    if (onOpenInLightbox) {
      onOpenInLightbox(url);
    } else {
      setLightboxSrc(url);
    }
  }, [onOpenInLightbox]);

  const handleCloseLightbox = useCallback(() => {
    setLightboxSrc(null);
  }, []);

  return (
    <div className="multimodal-content">
      {parts.map((part, index) => {
        switch (part.type) {
          case "text":
            return (
              <div key={index} style={{ whiteSpace: "pre-wrap" }}>
                {part.text}
              </div>
            );
          case "image":
            return (
              <ImagePart
                key={index}
                part={part}
                onOpenInLightbox={handleOpenLightbox}
                maxWidth={maxPreviewWidth}
                maxHeight={maxPreviewHeight}
              />
            );
          case "audio":
            return <AudioPart key={index} part={part} />;
          case "video":
            return (
              <VideoPart
                key={index}
                part={part}
                maxWidth={maxPreviewWidth * 1.2}
                maxHeight={maxPreviewHeight * 1.2}
              />
            );
          case "document":
            return <DocumentPart key={index} part={part} />;
          default:
            return null;
        }
      })}

      {/* Lightbox */}
      {lightboxSrc && (
        <ImageLightbox
          src={lightboxSrc}
          onClose={handleCloseLightbox}
        />
      )}
    </div>
  );
}

export default MultimodalContent;

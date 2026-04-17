/**
 * Multimodal Utilities
 *
 * Helper functions for handling file uploads, conversions, and multimodal message construction.
 */

import type { ContentPart, MessageAttachment } from "../lib/types";

/**
 * Convert a File to base64 data URL
 */
export async function fileToDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = () => reject(new Error("Failed to read file"));
    reader.readAsDataURL(file);
  });
}

/**
 * Convert a File to base64 string (without data URL prefix)
 */
export async function fileToBase64(file: File): Promise<string> {
  const dataUrl = await fileToDataUrl(file);
  const commaIndex = dataUrl.indexOf(",");
  return commaIndex >= 0 ? dataUrl.slice(commaIndex + 1) : dataUrl;
}

/**
 * Get the content type category from a MIME type
 */
export function getContentType(mimeType: string): ContentPart["type"] {
  if (mimeType.startsWith("image/")) return "image";
  if (mimeType.startsWith("audio/")) return "audio";
  if (mimeType.startsWith("video/")) return "video";
  return "document";
}

/**
 * Check if a MIME type is supported for multimodal messages
 */
export function isSupportedMimeType(mimeType: string): boolean {
  const supportedPrefixes = ["image/", "audio/", "video/", "application/pdf", "text/"];
  return supportedPrefixes.some((prefix) => mimeType.startsWith(prefix));
}

/**
 * Get file extension from filename
 */
export function getFileExtension(filename: string): string {
  const dotIndex = filename.lastIndexOf(".");
  return dotIndex >= 0 ? filename.slice(dotIndex + 1).toLowerCase() : "";
}

/**
 * Create a ContentPart from a File
 */
export async function fileToContentPart(file: File): Promise<ContentPart> {
  const type = getContentType(file.type);
  const data = await fileToDataUrl(file);
  
  return {
    type,
    data,
    mimeType: file.type,
    filename: file.name,
    size: file.size,
  };
}

/**
 * Create a MessageAttachment from a File
 */
export async function fileToMessageAttachment(file: File): Promise<MessageAttachment> {
  const type = getContentType(file.type);
  const data = await fileToDataUrl(file);
  
  return {
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`,
    type,
    filename: file.name,
    data,
    mimeType: file.type,
    size: file.size,
  };
}

/**
 * Convert MessageAttachment to ContentPart
 */
export function attachmentToContentPart(attachment: MessageAttachment): ContentPart {
  return {
    type: attachment.type,
    url: attachment.url,
    data: attachment.data,
    mimeType: attachment.mimeType,
    filename: attachment.filename,
    size: attachment.size,
  };
}

/**
 * Convert ContentPart to MessageAttachment
 */
export function contentPartToAttachment(part: ContentPart): MessageAttachment {
  return {
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`,
    type: part.type === "text" ? "document" : part.type,
    filename: part.filename || "unknown",
    url: part.url,
    data: part.data,
    mimeType: part.mimeType || "application/octet-stream",
    size: part.size || 0,
  };
}

/**
 * Build a multimodal message payload for API requests
 */
export interface MultimodalMessagePayload {
  role: "user" | "assistant" | "system";
  content: string | Array<{
    type: "text" | "image_url" | "input_audio" | "video_url";
    text?: string;
    image_url?: { url: string; detail?: "auto" | "low" | "high" };
    input_audio?: { data: string; format: string };
    video_url?: { url: string };
  }>;
}

/**
 * Convert ContentParts to OpenAI-style message content
 */
export function contentPartsToOpenAIFormat(parts: ContentPart[]): MultimodalMessagePayload["content"] {
  if (parts.length === 0) return "";
  
  const content: MultimodalMessagePayload["content"] = [];
  
  for (const part of parts) {
    switch (part.type) {
      case "text":
        content.push({
          type: "text",
          text: part.text || "",
        });
        break;
      case "image":
        content.push({
          type: "image_url",
          image_url: {
            url: part.data || part.url || "",
            detail: "auto",
          },
        });
        break;
      case "audio":
        // OpenAI audio format
        const format = part.mimeType?.split("/")[1] || "mp3";
        content.push({
          type: "input_audio",
          input_audio: {
            data: part.data?.split(",")[1] || part.data || "",
            format,
          },
        });
        break;
      case "video":
        content.push({
          type: "video_url",
          video_url: {
            url: part.data || part.url || "",
          },
        });
        break;
      case "document":
        // Documents are typically passed as URLs or embedded as text
        if (part.url) {
          content.push({
            type: "text",
            text: `[Document: ${part.filename || part.url}]`,
          });
        } else {
          content.push({
            type: "text",
            text: part.text || `[Document: ${part.filename || "unknown"}]`,
          });
        }
        break;
    }
  }
  
  return content;
}

/**
 * Convert ContentParts to Gemini-style message content
 */
export function contentPartsToGeminiFormat(parts: ContentPart[]): Array<{
  text?: string;
  inlineData?: { mimeType: string; data: string };
}> {
  const content: Array<{
    text?: string;
    inlineData?: { mimeType: string; data: string };
  }> = [];
  
  for (const part of parts) {
    switch (part.type) {
      case "text":
        content.push({ text: part.text || "" });
        break;
      case "image":
      case "audio":
      case "video":
        const data = part.data?.split(",")[1] || part.data || "";
        content.push({
          inlineData: {
            mimeType: part.mimeType || "application/octet-stream",
            data,
          },
        });
        break;
      case "document":
        content.push({
          text: part.text || `[Document: ${part.filename || "unknown"}]`,
        });
        break;
    }
  }
  
  return content;
}

/**
 * Validate file size
 */
export function validateFileSize(file: File, maxSizeBytes: number): { valid: boolean; error?: string } {
  if (file.size > maxSizeBytes) {
    return {
      valid: false,
      error: `File too large (${(file.size / 1024 / 1024).toFixed(1)}MB). Maximum allowed: ${(maxSizeBytes / 1024 / 1024).toFixed(0)}MB`,
    };
  }
  return { valid: true };
}

/**
 * Validate file type
 */
export function validateFileType(
  file: File,
  allowedTypes: string[]
): { valid: boolean; error?: string } {
  const isAllowed = allowedTypes.some((type) => {
    if (type.endsWith("/*")) {
      return file.type.startsWith(type.slice(0, -1));
    }
    return file.type === type;
  });
  
  if (!isAllowed) {
    return {
      valid: false,
      error: `File type "${file.type}" is not allowed. Allowed types: ${allowedTypes.join(", ")}`,
    };
  }
  return { valid: true };
}

/**
 * Format file size for display
 */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes}B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)}MB`;
  return `${(bytes / 1024 / 1024 / 1024).toFixed(1)}GB`;
}

/**
 * Generate a unique ID for attachments
 */
export function generateAttachmentId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}

/**
 * Create a preview URL for a file (for audio/video)
 */
export function createPreviewUrl(file: File): string {
  return URL.createObjectURL(file);
}

/**
 * Revoke a preview URL (cleanup)
 */
export function revokePreviewUrl(url: string): void {
  URL.revokeObjectURL(url);
}

/**
 * Default allowed MIME types for multimodal uploads
 */
export const DEFAULT_ALLOWED_MIME_TYPES = [
  "image/*",
  "audio/*",
  "video/*",
  "application/pdf",
  "text/plain",
  "text/markdown",
  "text/csv",
  "application/json",
];

/**
 * Default max file size (50MB)
 */
export const DEFAULT_MAX_FILE_SIZE = 50 * 1024 * 1024;

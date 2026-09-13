import { API_BASE, request } from "./api/core";
import type { MultimodalAttachment } from "../components/chat-page/types";
import type { ContentPart } from "./types";

type MediaKind = Exclude<ContentPart["type"], "text">;

const EXT_TO_KIND: Record<string, { kind: MediaKind; mimeType?: string }> = {
  ".png": { kind: "image", mimeType: "image/png" },
  ".apng": { kind: "image", mimeType: "image/png" },
  ".jpg": { kind: "image", mimeType: "image/jpeg" },
  ".jpeg": { kind: "image", mimeType: "image/jpeg" },
  ".gif": { kind: "image", mimeType: "image/gif" },
  ".webp": { kind: "image", mimeType: "image/webp" },
  ".svg": { kind: "image", mimeType: "image/svg+xml" },

  ".mp3": { kind: "audio", mimeType: "audio/mpeg" },
  ".wav": { kind: "audio", mimeType: "audio/wav" },
  ".ogg": { kind: "audio", mimeType: "audio/ogg" },
  ".m4a": { kind: "audio", mimeType: "audio/mp4" },
  ".flac": { kind: "audio", mimeType: "audio/flac" },
  ".aac": { kind: "audio", mimeType: "audio/aac" },

  ".mp4": { kind: "video", mimeType: "video/mp4" },
  ".webm": { kind: "video", mimeType: "video/webm" },
  ".mov": { kind: "video", mimeType: "video/quicktime" },
  ".avi": { kind: "video", mimeType: "video/x-msvideo" },

  ".pdf": { kind: "document", mimeType: "application/pdf" },
};

function stripQueryAndFragment(href: string): string {
  return href.split(/[?#]/, 1)[0] ?? href;
}

function extname(href: string): string | null {
  const raw = stripQueryAndFragment(href).trim();
  const lastDot = raw.lastIndexOf(".");
  if (lastDot < 0) return null;
  return raw.slice(lastDot).toLowerCase();
}

function basename(href: string): string {
  const raw = stripQueryAndFragment(href).replace(/\\/g, "/");
  const parts = raw.split("/").filter(Boolean);
  return parts.at(-1) ?? raw;
}

function isHttpUrl(href: string): boolean {
  return /^https?:\/\//i.test(href);
}

function parseMarkdownLinkTarget(rawTarget: string): string {
  // Best-effort parsing: (url "title") => url
  const trimmed = rawTarget.trim();
  if (!trimmed) return trimmed;
  const unwrapped = trimmed.startsWith("<") && trimmed.endsWith(">") ? trimmed.slice(1, -1) : trimmed;
  // Split on whitespace to drop an optional title.
  return unwrapped.split(/\s+/, 1)[0] ?? unwrapped;
}

function workspaceRawUrl(path: string): string {
  const normalized = path.replace(/^@/, "").trim();
  return `/api/workspace-media/raw?path=${encodeURIComponent(normalized)}`;
}

function hrefToContentPart(href: string, label?: string): ContentPart | null {
  const ext = extname(href);
  if (!ext) return null;
  const mapping = EXT_TO_KIND[ext];
  if (!mapping) return null;

  // Avoid auto-embedding remote media by default.
  // (Local same-origin URLs like /api/... are allowed.)
  const url = isHttpUrl(href) ? null : href.startsWith("/") ? href : workspaceRawUrl(href);
  if (!url) return null;

  const filename = (label ?? "").trim() || basename(href);
  return {
    type: mapping.kind,
    url,
    filename,
    mimeType: mapping.mimeType,
  };
}

function maskFencedCodeBlocks(markdown: string): { masked: string; blocks: string[] } {
  const blocks: string[] = [];
  const masked = markdown.replace(/```[\s\S]*?```/g, (match) => {
    const id = blocks.length;
    blocks.push(match);
    return `@@CODEBLOCK_${id}@@`;
  });
  return { masked, blocks };
}

function unmaskFencedCodeBlocks(masked: string, blocks: string[]): string {
  let out = masked;
  for (let i = 0; i < blocks.length; i += 1) {
    const token = `@@CODEBLOCK_${i}@@`;
    out = out.split(token).join(blocks[i] ?? "");
  }
  return out;
}

export interface MarkdownEmbedExtraction {
  markdown: string;
  contentParts: ContentPart[];
}

/**
 * Extracts media links from Markdown and returns:
 * - markdown: the Markdown with media links replaced by a short hint
 * - contentParts: derived embeds (image/audio/video/document)
 *
 * Supported syntax:
 * - ![alt](path/to/file.wav)
 * - [label](path/to/file.mp4)
 *
 * Workspace paths are resolved via /api/workspace-media/raw?path=...
 */
export function extractEmbedsFromMarkdown(markdown: string): MarkdownEmbedExtraction {
  if (!markdown) return { markdown, contentParts: [] };

  const { masked, blocks } = maskFencedCodeBlocks(markdown);
  const parts: ContentPart[] = [];
  const seen = new Set<string>();

  const replaced = masked
    // Images: ![alt](target)
    .replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (match, altText: string, rawTarget: string) => {
      const href = parseMarkdownLinkTarget(rawTarget);
      const part = hrefToContentPart(href, altText);
      if (!part || !part.url) return match;
      const key = `${part.type}:${part.url}`;
      if (!seen.has(key)) {
        seen.add(key);
        parts.push(part);
      }
      const label = (altText ?? "").trim() || part.filename || "media";
      return `${label} (embedded below)`;
    })
    // Links: [label](target)
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, (match, labelText: string, rawTarget: string, offset: number) => {
      // Skip images we already handled: they start with '!'
      if (offset > 0 && masked[offset - 1] === "!") return match;
      const href = parseMarkdownLinkTarget(rawTarget);
      const part = hrefToContentPart(href, labelText);
      if (!part || !part.url) return match;
      const key = `${part.type}:${part.url}`;
      if (!seen.has(key)) {
        seen.add(key);
        parts.push(part);
      }
      const label = (labelText ?? "").trim() || part.filename || "media";
      return `${label} (embedded below)`;
    });

  return {
    markdown: unmaskFencedCodeBlocks(replaced, blocks),
    contentParts: parts,
  };
}

/**
 * Convert MultimodalAttachment to ContentPart for API transmission
 */
export function attachmentToContentPart(attachment: MultimodalAttachment): Promise<ContentPart> {
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

// ── Audio Library (Broadcast Studio) ────────────────────────────────

export interface AudioFileEntry {
  name: string;
  path: string;
  ext: string;
  size: number;
  modified: number;
  mime: string;
}

export interface AudioLibraryResponse {
  ok: boolean;
  root: string;
  count: number;
  files: AudioFileEntry[];
}

export async function getAudioLibrary(options?: {
  path?: string;
  depth?: number;
}): Promise<AudioLibraryResponse> {
  const params = new URLSearchParams();
  if (options?.path) params.set("path", options.path);
  if (options?.depth != null) params.set("depth", String(options.depth));
  const qs = params.toString();
  return request<AudioLibraryResponse>(
    `/api/studio/audio-library${qs ? `?${qs}` : ""}`
  );
}

export async function ensureAudioDirectory(path: string): Promise<{ ok: boolean; path: string }> {
  return request(`/api/studio/audio-library/ensure-dir`, {
    method: "POST",
    body: JSON.stringify({ path }),
  });
}

export async function renameAudioFile(from: string, to: string): Promise<{ ok: boolean; from: string; to: string }> {
  return request(`/api/studio/audio-library/rename`, {
    method: "POST",
    body: JSON.stringify({ from, to }),
  });
}

export function getAudioStreamUrl(path: string): string {
  const params = new URLSearchParams({ path });
  return `${API_BASE}/api/studio/stream?${params.toString()}`;
}

export async function savePlaylistAsM3U(name: string, items: Array<{ path: string; name: string }>): Promise<{ ok: boolean; path: string; count: number }> {
  return request("/api/studio/save-m3u", {
    method: "POST",
    body: JSON.stringify({ name, items }),
  });
}

export function getM3UDownloadUrl(): string {
  return `${API_BASE}/api/studio/download-m3u`;
}

// ── Audio Labels ──────────────────────────────────────────────────

export async function getAudioLabels(filePath: string): Promise<{ ok: boolean; path: string; labels: string[] }> {
  return request(`/api/studio/labels?path=${encodeURIComponent(filePath)}`);
}

export async function getAllLabels(): Promise<{ ok: boolean; labels: string[] }> {
  return request(`/api/studio/labels?all=true`);
}

export async function addAudioLabel(filePath: string, label: string): Promise<{ ok: boolean; path: string; labels: string[] }> {
  return request(`/api/studio/labels/add`, {
    method: "POST",
    body: JSON.stringify({ path: filePath, label }),
  });
}

export async function removeAudioLabel(filePath: string, label: string): Promise<{ ok: boolean; path: string; labels: string[] }> {
  return request(`/api/studio/labels/remove`, {
    method: "POST",
    body: JSON.stringify({ path: filePath, label }),
  });
}

export async function getFilesByLabel(label: string): Promise<{ ok: boolean; label: string; files: string[] }> {
  return request(`/api/studio/labels/by-label?label=${encodeURIComponent(label)}`);
}

export async function syncAudioSymlinks(): Promise<{ ok: boolean; symlinks: number }> {
  return request(`/api/studio/sync-symlinks`, { method: "POST" });
}

export async function loadM3UPlaylist(filePath: string): Promise<{ ok: boolean; name: string; items: Array<{ path: string; name: string }> }> {
  return request(`/api/studio/load-m3u?path=${encodeURIComponent(filePath)}`);
}

export async function listPlaylists(): Promise<{ ok: boolean; playlists: Array<{ name: string; path: string; filename: string }> }> {
  return request(`/api/studio/playlists`);
}

export async function getAudioAssetUrl(audioPath: string, assetType: "waveform" | "spectrogram"): Promise<string> {
  return `${API_BASE}/api/studio/audio-asset?path=${encodeURIComponent(audioPath)}&type=${assetType}`;
}

export async function saveAudioAsset(audioPath: string, assetType: "waveform" | "spectrogram", imageData: string, mimeType?: string, width?: number, height?: number): Promise<{ ok: boolean }> {
  return request(`/api/studio/audio-asset`, {
    method: "POST",
    body: JSON.stringify({ path: audioPath, type: assetType, imageData, mimeType, width, height }),
  });
}

export interface DiscordAudioScanResponse {
  ok: boolean;
  scanned_at: string;
  import_root: string;
  channels_scanned: number;
  messages_scanned: number;
  attachments_found: number;
  imported_count: number;
  skipped_count: number;
  failed_count: number;
  manifest_path?: string;
}

export async function scanDiscordAudio(options?: {
  channel_ids?: string[];
  since_hours?: number;
  pages_per_channel?: number;
  limit_per_page?: number;
  max_channels?: number;
  import_root?: string;
}): Promise<DiscordAudioScanResponse> {
  return request(`/api/studio/discord-audio-scan`, {
    method: "POST",
    body: JSON.stringify(options ?? {}),
  });
}

export interface DiscordImageScanResponse {
  ok: boolean;
  scanned_at: string;
  import_root: string;
  channels_scanned: number;
  messages_scanned: number;
  attachments_found: number;
  imported_count: number;
  skipped_count: number;
  failed_count: number;
  manifest_path?: string;
}

export async function scanDiscordImages(options?: {
  channel_ids?: string[];
  since_hours?: number;
  pages_per_channel?: number;
  limit_per_page?: number;
  max_channels?: number;
  import_root?: string;
}): Promise<DiscordImageScanResponse> {
  return request(`/api/studio/discord-image-scan`, {
    method: "POST",
    body: JSON.stringify(options ?? {}),
  });
}

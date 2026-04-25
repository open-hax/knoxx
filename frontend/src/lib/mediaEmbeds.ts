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

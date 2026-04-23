const DEFAULT_PREFERRED_CHUNK_SIZE = 140;

function normalizeWhitespace(text: string): string {
  return text.replace(/\s+/g, " ").trim();
}

function firstSentenceBoundary(text: string): number {
  for (let index = 0; index < text.length; index += 1) {
    const char = text[index];
    if (![".", "!", "?", "\n"].includes(char)) continue;
    const next = text[index + 1];
    if (next == null || /\s/.test(next)) {
      return index + 1;
    }
  }
  return -1;
}

function whitespaceBoundaryBefore(text: string, preferredChunkSize: number): number {
  const limit = Math.min(preferredChunkSize, text.length);
  for (let index = limit; index >= 0; index -= 1) {
    if (/\s/.test(text[index] ?? "")) {
      return index;
    }
  }
  return -1;
}

export function extractAutoConversationChunks(
  buffer: string,
  forceFlush = false,
  preferredChunkSize = DEFAULT_PREFERRED_CHUNK_SIZE,
): { chunks: string[]; pending: string } {
  let remaining = buffer;
  const chunks: string[] = [];

  while (true) {
    const trimmed = normalizeWhitespace(remaining);
    if (!trimmed) {
      return { chunks, pending: "" };
    }

    const sentenceBoundary = firstSentenceBoundary(trimmed);
    if (sentenceBoundary > 0) {
      chunks.push(trimmed.slice(0, sentenceBoundary).trim());
      remaining = trimmed.slice(sentenceBoundary).trimStart();
      continue;
    }

    if (trimmed.length >= preferredChunkSize) {
      const whitespaceBoundary = whitespaceBoundaryBefore(trimmed, preferredChunkSize);
      if (whitespaceBoundary > 0) {
        chunks.push(trimmed.slice(0, whitespaceBoundary).trim());
        remaining = trimmed.slice(whitespaceBoundary).trimStart();
        continue;
      }
    }

    if (forceFlush) {
      chunks.push(trimmed);
      return { chunks, pending: "" };
    }

    return { chunks, pending: trimmed };
  }
}

export function ensureTrailingSpeechSpace(text: string): string {
  const normalized = normalizeWhitespace(text);
  return normalized.length > 0 ? `${normalized} ` : "";
}

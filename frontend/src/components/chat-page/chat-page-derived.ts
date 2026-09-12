import { useMemo } from 'react';
import type { RunDetail, RunEvent, ToolReceipt } from '../../lib/types';
import type { BrowseEntry, UseChatPageDerivedStateParams } from './types';
import { inferBrowseEntryKind, latestRunHydrationSources, parentPath } from './utils';

export function filterBrowseEntries(entries: BrowseEntry[], entryFilter: string, visibilityFilter: string, kindFilter: string): BrowseEntry[] {
  const query = entryFilter.trim().toLowerCase();
  return entries.filter((entry) => {
    const matchesQuery = !query || entry.name.toLowerCase().includes(query) || entry.path.toLowerCase().includes(query);
    if (!matchesQuery) return false;
    if (entry.type === 'dir') return true;
    const matchesVisibility = visibilityFilter === 'all' || entry.visibility === visibilityFilter;
    const matchesKind = kindFilter === 'all' || inferBrowseEntryKind(entry) === kindFilter;
    return matchesVisibility && matchesKind;
  });
}

export function visibilityStats(entries: BrowseEntry[]): { total: number; byVisibility: Record<string, number> } {
  const files = entries.filter((entry) => entry.type === 'file');
  const byVisibility = files.reduce<Record<string, number>>((acc, entry) => {
    const key = entry.visibility ?? 'internal';
    acc[key] = (acc[key] ?? 0) + 1;
    return acc;
  }, {});
  return {
    total: files.length,
    byVisibility,
  };
}

export function useChatPageDerivedState({
  browseData,
  entryFilter,
  visibilityFilter,
  kindFilter,
  semanticQuery,
  semanticResults,
  workspaceJob,
  latestRun,
  isSending,
  runtimeEvents,
  pendingAssistantId,
  conversationId,
}: UseChatPageDerivedStateParams) {
  const currentPath = browseData?.current_path ?? '';
  const currentParentPath = useMemo(() => parentPath(currentPath), [currentPath]);
  const stats = useMemo(() => visibilityStats(browseData?.entries ?? []), [browseData?.entries]);

  const filteredEntries = useMemo(() => {
    const entries = browseData?.entries ?? [];
    return filterBrowseEntries(entries, entryFilter, visibilityFilter, kindFilter);
  }, [browseData?.entries, entryFilter, visibilityFilter, kindFilter]);

  const semanticMode = semanticQuery.trim().length > 0;
  const activeEntryCount = semanticMode ? semanticResults.length : filteredEntries.length;
  const workspaceProgressPercent = workspaceJob && workspaceJob.total_files > 0
    ? Math.min(100, Math.round(((workspaceJob.processed_files + workspaceJob.failed_files) / workspaceJob.total_files) * 100))
    : 0;
  const latestToolReceipts = useMemo(() => (latestRun?.tool_receipts ?? []) as ToolReceipt[], [latestRun]);
  const liveToolReceipts = useMemo(() => (isSending && pendingAssistantId ? latestToolReceipts : []), [isSending, latestToolReceipts, pendingAssistantId]);
  const liveToolEvents = useMemo(() => (isSending ? runtimeEvents.filter((event) => ['tool_start', 'tool_update', 'tool_end'].includes(String(event.type ?? ''))) : []), [isSending, runtimeEvents]);
  const liveControlEnabled = Boolean(
    isSending
      && conversationId
      && runtimeEvents.some((event) => ['run_started', 'passive_hydration', 'assistant_first_token', 'tool_start'].includes(String(event.type ?? ''))),
  );
  const hydrationSources = useMemo(() => latestRunHydrationSources(latestRun), [latestRun]);

  return {
    activeEntryCount,
    assistantSurfaceBackground: 'var(--token-colors-background-surface)',
    assistantSurfaceBorder: 'var(--token-colors-border-default)',
    assistantSurfaceText: 'var(--token-colors-text-default)',
    currentParentPath,
    currentPath,
    filteredEntries,
    hydrationSources,
    latestToolReceipts,
    liveControlEnabled,
    liveToolEvents,
    liveToolReceipts,
    semanticMode,
    statsByVisibility: stats.byVisibility,
    statsTotal: stats.total,
    workspaceProgressPercent,
  };
}

export { inferBrowseEntryKind } from "./utils";

// Bounded projection of tool receipts for the chat timeline.
const TOOL_STRUCTURED_MAX_DEPTH = 5;
const TOOL_STRUCTURED_MAX_KEYS = 32;
const TOOL_STRUCTURED_MAX_ITEMS = 24;
const TOOL_RAW_TEXT_MAX_CHARS = 12000;

export function normalizeToolPreview(value?: string | null): string | null {
  if (typeof value !== "string") return null;
  const trimmed = value.trim();
  if (!trimmed) return null;
  const lowered = trimmed.toLowerCase();
  // Treat explicit "null" / "undefined" sentinel strings as missing.
  // NOTE: we still want inputs to be visible when captured; tool-specific
  // renderers upstream should avoid producing these sentinels.
  if (lowered === "null" || lowered === "undefined") return null;
  return trimmed;
}

function truncateText(value: string, max = 240): string {
  if (value.length <= max) return value;
  return `${value.slice(0, max).trimEnd()}…`;
}

function clipRawText(value: string, maxChars = TOOL_RAW_TEXT_MAX_CHARS): { text: string; truncated: boolean } {
  if (value.length <= maxChars) return { text: value, truncated: false };
  return { text: `${value.slice(0, maxChars).trimEnd()}…`, truncated: true };
}

export function asMarkdownPreview(value: string): string {
  const trimmed = value.trim();
  if (!trimmed) return "";
  if (/^(```|#{1,6}\s|>\s|[-*+]\s|\d+\.\s)/m.test(trimmed)) {
    return value;
  }
  if (value.includes("\n")) {
    return `\`\`\`text\n${value}\n\`\`\``;
  }
  return value;
}

function tryParseJson(value: string): unknown | null {
  const trimmed = value.trim();
  if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return null;
  try {
    return JSON.parse(trimmed) as unknown;
  } catch {
    return null;
  }
}

function isContentPartsArray(value: unknown): value is Array<Record<string, unknown>> {
  if (!Array.isArray(value) || value.length === 0) return false;
  return value.every((item) => {
    if (!item || typeof item !== "object" || Array.isArray(item)) return false;
    const record = item as Record<string, unknown>;
    return typeof record.text === "string" && (record.type === "text" || record.type === "output_text" || record.type === undefined);
  });
}

function unescapeJsonStringFragment(value: string): string {
  // Best-effort: handles common escapes we see in tool wrappers.
  return value
    .replace(/\\n/g, "\n")
    .replace(/\\t/g, "\t")
    .replace(/\\r/g, "\r")
    .replace(/\\"/g, '"')
    .replace(/\\\\/g, "\\");
}

function extractJsonLikeText(value: string): string | null {
  const trimmed = value.trim();
  if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return null;
  const hits: string[] = [];
  const regex = /"text"\s*:\s*"/g;
  let match: RegExpExecArray | null;
  while ((match = regex.exec(trimmed)) !== null && hits.length < 4) {
    let i = match.index + match[0].length;
    let out = "";
    let escaped = false;
    for (; i < trimmed.length; i += 1) {
      const ch = trimmed[i];
      if (escaped) {
        out += `\\${ch}`;
        escaped = false;
        continue;
      }
      if (ch === "\\") {
        escaped = true;
        continue;
      }
      if (ch === '"') {
        break;
      }
      out += ch;
    }
    const unescaped = unescapeJsonStringFragment(out).trim();
    if (unescaped) hits.push(unescaped);
  }
  return hits.length > 0 ? hits.join("\n\n") : null;
}

function summarizeStructuredValue(value: unknown): string | null {
  if (typeof value === "string") {
    return value.trim() ? value : null;
  }
  if (Array.isArray(value)) {
    if (isContentPartsArray(value)) {
      const joined = value
        .map((part) => (typeof part.text === "string" ? part.text : ""))
        .map((text) => text.trim())
        .filter(Boolean)
        .join("\n\n");
      return joined.trim() ? joined : null;
    }
    const lines = value
      .map((item) => summarizeStructuredValue(item))
      .filter((item): item is string => Boolean(item))
      .slice(0, 8);
    return lines.length > 0 ? lines.map((line) => `- ${line}`).join("\n") : null;
  }
  if (!value || typeof value !== "object") return null;
  const record = value as Record<string, unknown>;
  const preferredKeys = [
    "content",
    "text",
    "answer",
    "message",
    "result",
    "output",
    "preview",
    "summary",
    "translated_text",
    "corrected_text",
    "snippet",
  ];
  for (const key of preferredKeys) {
    const summarized = summarizeStructuredValue(record[key]);
    if (summarized) return summarized;
  }
  for (const key of ["rows", "hits", "results", "sources", "items", "documents"]) {
    const summarized = summarizeStructuredValue(record[key]);
    if (summarized) return summarized;
  }
  return null;
}

export function toolInputSummary(value: string): string | null {
  const parsed = tryParseJson(value);
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
    return value.trim() && !value.trim().startsWith("{") ? value : null;
  }
  const record = parsed as Record<string, unknown>;
  const parts = [record.query, record.q, record.path, record.url, record.document_id]
    .filter((item): item is string => typeof item === "string" && item.trim().length > 0)
    .slice(0, 2);
  return parts.length > 0 ? parts.join(" • ") : null;
}

function isPlainRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function valueBacktickFence(text: string): string {
  const matches = text.match(/`+/g);
  const maxRun = matches ? Math.max(...matches.map((m) => m.length)) : 0;
  return "`".repeat(maxRun + 1);
}

function fencedTextBlock(text: string, lang = "text"): string {
  const minFence = 3;
  const maxRun = (text.match(/`+/g) ?? []).reduce((max, run) => Math.max(max, run.length), 0);
  const fence = "`".repeat(Math.max(minFence, maxRun + 1));
  return `${fence}${lang}\n${text}\n${fence}`;
}

function inlineCode(text: string): string {
  const fence = valueBacktickFence(text);
  return `${fence}${text}${fence}`;
}

function formatScalarForMarkdown(value: unknown): string {
  if (value === null) return "null";
  if (value === undefined) return "undefined";
  if (typeof value === "number" || typeof value === "boolean") return String(value);
  if (typeof value === "string") {
    const trimmed = value.trim();
    if (!trimmed) return inlineCode("");
    if (trimmed.includes("\n")) {
      return `\n\n${fencedTextBlock(trimmed, "text")}`;
    }
    if (trimmed.length > 180) {
      return inlineCode(truncateText(trimmed, 180));
    }
    return inlineCode(trimmed);
  }
  return inlineCode(String(value));
}

export function structuredToMarkdown(value: unknown, depth = 0): string {
  if (depth >= TOOL_STRUCTURED_MAX_DEPTH) {
    return "- … (max depth)";
  }

  if (!isPlainRecord(value) && !Array.isArray(value)) {
    return formatScalarForMarkdown(value);
  }

  if (Array.isArray(value)) {
    if (value.length === 0) return "- (empty)";
    const items = value.slice(0, TOOL_STRUCTURED_MAX_ITEMS);
    const lines: string[] = [];
    for (const item of items) {
      if (isPlainRecord(item) || Array.isArray(item)) {
        const nested = structuredToMarkdown(item, depth + 1)
          .split("\n")
          .map((line) => `  ${line}`)
          .join("\n");
        lines.push(`-\n${nested}`);
      } else {
        lines.push(`- ${formatScalarForMarkdown(item)}`);
      }
    }
    const remaining = value.length - items.length;
    if (remaining > 0) {
      lines.push(`- … (${remaining} more item(s))`);
    }
    return lines.join("\n");
  }

  const keys = Object.keys(value);
  if (keys.length === 0) return "- (empty)";
  const visibleKeys = keys.slice(0, TOOL_STRUCTURED_MAX_KEYS);
  const lines: string[] = [];
  for (const key of visibleKeys) {
    const child = (value as Record<string, unknown>)[key];
    if (isPlainRecord(child) || Array.isArray(child)) {
      const nested = structuredToMarkdown(child, depth + 1)
        .split("\n")
        .map((line) => `  ${line}`)
        .join("\n");
      lines.push(`- ${inlineCode(key)}:\n${nested}`);
    } else {
      lines.push(`- ${inlineCode(key)}: ${formatScalarForMarkdown(child)}`);
    }
  }
  const remaining = keys.length - visibleKeys.length;
  if (remaining > 0) {
    lines.push(`- … (${remaining} more key(s))`);
  }
  return lines.join("\n");
}

export function toolPreviewMarkdown(value: string): string {
  const parsed = tryParseJson(value);
  if (parsed) {
    return structuredToMarkdown(parsed);
  }
  const extracted = extractJsonLikeText(value);
  if (extracted) {
    const clipped = clipRawText(extracted);
    const base = asMarkdownPreview(clipped.text);
    return clipped.truncated ? `${base}\n\n_(truncated)_` : base;
  }
  const clipped = clipRawText(value);
  const base = asMarkdownPreview(clipped.text);
  return clipped.truncated ? `${base}\n\n_(truncated)_` : base;
}

export function toolOutputMarkdown(value: string): string {
  const parsed = tryParseJson(value);
  if (parsed) {
    const summarized = summarizeStructuredValue(parsed);
    // If we can extract human text, show that as the primary output.
    if (summarized && summarized.trim().length > 0) {
      const clipped = clipRawText(summarized);
      const base = asMarkdownPreview(clipped.text);
      return clipped.truncated ? `${base}\n\n_(truncated)_` : base;
    }
    return structuredToMarkdown(parsed);
  }
  const extracted = extractJsonLikeText(value);
  if (extracted) {
    const clipped = clipRawText(extracted);
    const base = asMarkdownPreview(clipped.text);
    return clipped.truncated ? `${base}\n\n_(truncated)_` : base;
  }
  const clipped = clipRawText(value);
  const base = asMarkdownPreview(clipped.text);
  return clipped.truncated ? `${base}\n\n_(truncated)_` : base;
}

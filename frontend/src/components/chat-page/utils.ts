import type {
  ChatMessage,
  ChatTraceBlock,
  GroundedContextRow,
  MemorySessionRow,
  RunDetail,
  RunEvent,
  ToolReceipt,
} from "../../lib/types";
import type {
  HydrationSource,
  IngestionSource,
  PreviewResponse,
  WorkspaceJob,
} from "./types";

export function contextPath(row: GroundedContextRow): string {
  return row.source_path ?? row.message ?? row.source ?? row.id;
}

export function isWorkspaceSource(source: IngestionSource): boolean {
  const config = source.config ?? {};
  const rootPath = config.root_path ?? config["root-path"];
  return rootPath === "/app/workspace/devel" || (source.name === "devel workspace" && rootPath == null);
}

export function parentPath(path: string): string {
  const parts = path.split("/").filter(Boolean);
  if (parts.length <= 1) return "";
  return parts.slice(0, -1).join("/");
}

export function seedCanvasFromMessage(message: ChatMessage): { title: string; content: string; subject: string } {
  const firstLine = message.content.split("\n").find((line) => line.trim()) ?? "Draft";
  const title = firstLine.slice(0, 80);
  return {
    title,
    subject: title,
    content: message.content,
  };
}

export function slugify(value: string): string {
  return value
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 64) || "draft";
}

export function fileNameFromPath(path: string): string {
  const parts = path.split("/").filter(Boolean);
  return parts[parts.length - 1] || path;
}

export function seedCanvasFromPreview(preview: PreviewResponse): { title: string; subject: string; content: string; path: string } {
  const title = fileNameFromPath(preview.path).replace(/\.[^.]+$/, "") || "Scratchpad";
  return {
    title,
    subject: title,
    content: preview.content,
    path: preview.path,
  };
}

export function sourceUrlToPath(url: string): string {
  if (!url) return "";
  try {
    const parsed = new URL(url, window.location.origin);
    return decodeURIComponent(parsed.pathname.replace(/^\/+/, "")).split("?")[0].split("#")[0];
  } catch {
    return decodeURIComponent(url.replace(/^\/+/, "")).split("?")[0].split("#")[0];
  }
}

export function selectWorkspaceJob(jobs: WorkspaceJob[]): WorkspaceJob | null {
  if (jobs.length === 0) return null;
  const sorted = [...jobs].sort((a, b) => {
    const left = Date.parse(b.started_at || b.created_at || b.completed_at || "") || 0;
    const right = Date.parse(a.started_at || a.created_at || a.completed_at || "") || 0;
    return left - right;
  });
  const active = sorted.find((job) => job.status === "running" || job.status === "pending");
  if (active) return active;
  const completed = sorted.find((job) => job.status === "completed");
  if (completed) return completed;
  return sorted[0] ?? null;
}

export function formatMaybeDate(value?: string): string | null {
  if (!value) return null;
  const parsed = Date.parse(value);
  if (Number.isNaN(parsed)) return value;
  return new Date(parsed).toLocaleString();
}

export function truncateText(value: string, max = 240): string {
  if (value.length <= max) return value;
  return `${value.slice(0, max).trimEnd()}…`;
}

export function asMarkdownPreview(value: string): string {
  const trimmed = value.trim();
  if (!trimmed) return "";
  if (/^(```|#{1,6}\s|>\s|[-*+]\s|\d+\.\s)/m.test(trimmed)) {
    return value;
  }
  if (trimmed.startsWith("{") || trimmed.startsWith("[") || value.includes("\n")) {
    return `\`\`\`text\n${value}\n\`\`\``;
  }
  return value;
}

export function parseToolReceiptDetails(receipt: ToolReceipt): Record<string, unknown> | null {
  const raw = receipt.result_preview;
  if (typeof raw !== "string" || raw.trim().length === 0) return null;
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    const details = parsed?.details;
    return details && typeof details === "object" && !Array.isArray(details)
      ? details as Record<string, unknown>
      : null;
  } catch {
    return null;
  }
}

export function canvasArtifactFromToolReceipt(receipt: ToolReceipt): { title?: string; path?: string; content?: string } | null {
  const details = parseToolReceiptDetails(receipt);
  if (!details || details.canvas !== true) return null;
  return {
    title: typeof details.title === "string" ? details.title : undefined,
    path: typeof details.path === "string" ? details.path : undefined,
    content: typeof details.content === "string" ? details.content : undefined,
  };
}

export function isChatRole(value: unknown): value is ChatMessage["role"] {
  return value === "system" || value === "user" || value === "assistant";
}

function parseMemoryRowExtra(row: MemorySessionRow): Record<string, unknown> | null {
  if (!row.extra) return null;
  if (typeof row.extra === "object" && !Array.isArray(row.extra)) {
    return row.extra;
  }
  if (typeof row.extra === "string") {
    try {
      const parsed = JSON.parse(row.extra) as unknown;
      return parsed && typeof parsed === "object" && !Array.isArray(parsed)
        ? (parsed as Record<string, unknown>)
        : null;
    } catch {
      return null;
    }
  }
  return null;
}

export function memoryRowRunId(row: MemorySessionRow): string | null {
  const extra = parseMemoryRowExtra(row);
  const candidate = extra?.run_id ?? extra?.runId;
  return typeof candidate === "string" ? candidate : null;
}

function normalizeTraceStatus(value: unknown): ChatTraceBlock["status"] {
  return value === "streaming" || value === "done" || value === "error" ? value : undefined;
}

function parseMemoryRowTraceBlocks(row: MemorySessionRow): ChatMessage["traceBlocks"] {
  const extra = parseMemoryRowExtra(row);
  const raw = extra?.trace_blocks ?? extra?.traceBlocks;
  if (!Array.isArray(raw)) return undefined;
  const blocks = raw.flatMap((item, index) => {
    if (!item || typeof item !== "object") return [];
    const block = item as Record<string, unknown>;
    const kind = typeof block.kind === "string" ? block.kind : null;
    if (kind !== "agent_message" && kind !== "reasoning" && kind !== "tool_call") return [];
    return [{
      id: typeof block.id === "string" && block.id ? block.id : `${kind}:${index}`,
      kind,
      status: normalizeTraceStatus(block.status),
      at: typeof block.at === "string" ? block.at : undefined,
      content: typeof block.content === "string" ? block.content : undefined,
      toolName: typeof block.toolName === "string" ? block.toolName : undefined,
      toolCallId: typeof block.toolCallId === "string" ? block.toolCallId : undefined,
      inputPreview: typeof block.inputPreview === "string" ? block.inputPreview : undefined,
      outputPreview: typeof block.outputPreview === "string" ? block.outputPreview : undefined,
      updates: Array.isArray(block.updates) ? block.updates.filter((value): value is string => typeof value === "string") : undefined,
      isError: typeof block.isError === "boolean" ? block.isError : undefined,
    } satisfies NonNullable<ChatMessage["traceBlocks"]>[number]];
  });
  return blocks.length > 0 ? blocks : undefined;
}

function fallbackTraceBlocksByRunId(rows: MemorySessionRow[]): Map<string, NonNullable<ChatMessage["traceBlocks"]>> {
  const byRun = new Map<string, NonNullable<ChatMessage["traceBlocks"]>>();

  const append = (runId: string, block: NonNullable<ChatMessage["traceBlocks"]>[number]) => {
    const existing = byRun.get(runId) ?? [];
    existing.push(block);
    byRun.set(runId, existing);
  };

  rows.forEach((row, index) => {
    const runId = memoryRowRunId(row);
    if (!runId) return;

    if (row.kind === "knoxx.reasoning" && typeof row.text === "string" && row.text.trim()) {
      append(runId, {
        id: row.id || `reasoning:${index}`,
        kind: "reasoning",
        status: "done",
        at: typeof row.ts === "string" ? row.ts : undefined,
        content: row.text,
      });
      return;
    }

    if (row.kind === "knoxx.tool_receipt") {
      const extra = parseMemoryRowExtra(row);
      const receipt = extra?.receipt;
      const receiptRecord = receipt && typeof receipt === "object" && !Array.isArray(receipt)
        ? receipt as Record<string, unknown>
        : null;
      append(runId, {
        id: (typeof receiptRecord?.id === "string" && receiptRecord.id) || row.id || `tool:${index}`,
        kind: "tool_call",
        status: normalizeTraceStatus(
          typeof receiptRecord?.status === "string"
            ? (receiptRecord.status === "completed" ? "done" : receiptRecord.status === "failed" ? "error" : "streaming")
            : "done",
        ),
        at: typeof row.ts === "string" ? row.ts : undefined,
        toolName: typeof receiptRecord?.tool_name === "string" ? receiptRecord.tool_name : undefined,
        toolCallId: typeof receiptRecord?.id === "string" ? receiptRecord.id : undefined,
        inputPreview: typeof receiptRecord?.input_preview === "string" ? receiptRecord.input_preview : undefined,
        outputPreview: typeof receiptRecord?.result_preview === "string" ? receiptRecord.result_preview : typeof row.text === "string" ? row.text : undefined,
        updates: Array.isArray(receiptRecord?.updates) ? receiptRecord.updates.filter((value): value is string => typeof value === "string") : undefined,
        isError: typeof receiptRecord?.is_error === "boolean" ? receiptRecord.is_error : undefined,
      });
    }
  });

  return byRun;
}

export function memoryRowsToMessages(rows: MemorySessionRow[]): ChatMessage[] {
  const derivedTraceBlocks = fallbackTraceBlocksByRunId(rows);
  return rows.flatMap((row, index) => {
    const text = typeof row.text === "string" ? row.text : "";
    if (row.kind !== "knoxx.message" || !isChatRole(row.role) || text.trim().length === 0) {
      return [];
    }

    const runId = memoryRowRunId(row);

    return [{
      id: row.id || `${row.session ?? "memory"}:${index}`,
      role: row.role,
      content: text,
      model: typeof row.model === "string" ? row.model : null,
      runId,
      status: row.role === "assistant" || row.role === "system" ? "done" : undefined,
      traceBlocks: row.role === "assistant"
        ? (parseMemoryRowTraceBlocks(row) ?? (runId ? derivedTraceBlocks.get(runId) : undefined))
        : undefined,
    } satisfies ChatMessage];
  });
}

export function controlTimelineMessageFromEvent(
  event: RunEvent & { type?: string; preview?: string; run_id?: string; error?: string },
): ChatMessage | null {
  const type = String(event.type ?? "");
  const preview = typeof event.preview === "string" ? event.preview.trim() : "";
  const runId = typeof event.run_id === "string" ? event.run_id : null;
  const error = typeof event.error === "string" ? event.error.trim() : "";
  const title = {
    steer_queued: "Steer queued",
    follow_up_queued: "Follow-up queued",
    steer_failed: "Steer failed",
    follow_up_failed: "Follow-up failed",
  }[type];
  if (!title) return null;
  const id = `control:${type}:${runId ?? ""}:${preview || error}`;
  const parts = [`### ${title}`];
  if (preview) parts.push("", preview);
  if (error) parts.push("", `Error: ${error}`);
  if (runId) parts.push("", `Run: \`${runId.slice(0, 8)}\``);
  return {
    id,
    role: "system",
    content: parts.join("\n"),
    runId,
    status: type.endsWith("failed") ? "error" : "done",
  };
}

function traceToolBlockId(event: RunEvent & { tool_call_id?: string; tool_name?: string }): string {
  const toolCallId = typeof event.tool_call_id === "string" && event.tool_call_id.trim().length > 0
    ? event.tool_call_id
    : null;
  if (toolCallId) return `tool:${toolCallId}`;
  const toolName = typeof event.tool_name === "string" && event.tool_name.trim().length > 0
    ? event.tool_name
    : "tool";
  return `tool:${toolName}:${event.at ?? ""}`;
}

export function appendTraceTextDelta(
  blocks: ChatTraceBlock[],
  kind: "agent_message" | "reasoning",
  delta: string,
  at?: string,
): ChatTraceBlock[] {
  if (delta.length === 0) return blocks;

  const next = [...blocks];
  const last = next[next.length - 1];

  if (last && last.kind === kind && last.status === "streaming") {
    next[next.length - 1] = {
      ...last,
      content: `${last.content ?? ""}${delta}`,
      at: at ?? last.at,
    };
    return next;
  }

  next.push({
    id: crypto.randomUUID(),
    kind,
    status: "streaming",
    content: delta,
    at,
  });
  return next;
}

export function applyToolTraceEvent(
  blocks: ChatTraceBlock[],
  event: RunEvent & { type?: string; tool_name?: string; tool_call_id?: string; preview?: string; is_error?: boolean },
): ChatTraceBlock[] {
  const type = String(event.type ?? "");
  const blockId = traceToolBlockId(event);
  const next = [...blocks];
  const index = next.findIndex((block) => block.id === blockId);
  const existing = index >= 0 ? next[index] : null;

  if (type === "tool_start") {
    const block: ChatTraceBlock = {
      id: blockId,
      kind: "tool_call",
      toolName: event.tool_name,
      toolCallId: event.tool_call_id,
      inputPreview: typeof event.preview === "string" ? event.preview : undefined,
      status: "streaming",
      at: typeof event.at === "string" ? event.at : undefined,
      updates: [],
    };
    if (index >= 0) {
      next[index] = { ...existing, ...block } as ChatTraceBlock;
    } else {
      next.push(block);
    }
    return next;
  }

  if (type === "tool_update") {
    const preview = typeof event.preview === "string" ? event.preview : undefined;
    if (index >= 0) {
      next[index] = {
        ...(existing as ChatTraceBlock),
        status: "streaming",
        at: typeof event.at === "string" ? event.at : existing?.at,
        updates: preview
          ? [...((existing?.updates ?? []).slice(-7)), preview]
          : existing?.updates,
      };
    } else {
      next.push({
        id: blockId,
        kind: "tool_call",
        toolName: event.tool_name,
        toolCallId: event.tool_call_id,
        status: "streaming",
        at: typeof event.at === "string" ? event.at : undefined,
        updates: preview ? [preview] : [],
      });
    }
    return next;
  }

  if (type === "tool_end") {
    const block: ChatTraceBlock = {
      id: blockId,
      kind: "tool_call",
      toolName: event.tool_name,
      toolCallId: event.tool_call_id,
      status: event.is_error ? "error" : "done",
      outputPreview: typeof event.preview === "string" ? event.preview : undefined,
      isError: Boolean(event.is_error),
      at: typeof event.at === "string" ? event.at : undefined,
    };
    if (index >= 0) {
      next[index] = {
        ...(existing as ChatTraceBlock),
        ...block,
        updates: existing?.updates,
        inputPreview: existing?.inputPreview,
      };
    } else {
      next.push({ ...block, updates: [] });
    }
    return next;
  }

  return blocks;
}

export function finalizeTraceBlocks(
  blocks: ChatTraceBlock[],
  status: "done" | "error",
): ChatTraceBlock[] {
  return blocks.map((block) =>
    block.status === "streaming"
      ? {
          ...block,
          status,
          isError: status === "error" ? block.isError ?? block.kind === "tool_call" : block.isError,
        }
      : block,
  );
}

export function latestRunHydrationSources(run: RunDetail | null): HydrationSource[] {
  const passiveHydration = run?.resources?.passiveHydration as { results?: Array<Record<string, unknown>> } | undefined;
  if (!passiveHydration || !Array.isArray(passiveHydration.results)) return [];

  return passiveHydration.results.flatMap((result) => {
    const path = typeof result.path === "string" ? result.path : "";
    if (!path) return [];

    const source: HydrationSource = {
      title: typeof result.name === "string" && result.name.trim().length > 0 ? result.name : fileNameFromPath(path),
      path,
    };

    if (typeof result.snippet === "string") {
      source.section = result.snippet;
    }

    return [source];
  });
}

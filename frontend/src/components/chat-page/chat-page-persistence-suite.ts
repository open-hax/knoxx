import type { Dispatch, MutableRefObject, SetStateAction } from 'react';
import type { ChatMessage, ChatTraceBlock, MemorySessionRow, ProxxModelInfo, RunDetail, RunEvent } from '../../lib/types';
import type { PinnedContextItem } from './types';
import {
  useChatSessionPersistence,
  usePinnedContextPersistence,
  useProxxStatusPolling,
  useScratchpadPersistence,
} from './hooks';

type SetState<T> = Dispatch<SetStateAction<T>>;

type UseChatPagePersistenceSuiteParams = {
  makeId: () => string;
  sessionId: string;
  setSessionId: SetState<string>;
  systemPrompt: string;
  setSystemPrompt: SetState<string>;
  selectedModel: string;
  setSelectedModel: SetState<string>;
  selectedThinkingLevel: string;
  setSelectedThinkingLevel: SetState<string>;
  activeActorId: string;
  setActiveActorId: SetState<string>;
  activeAgentId: string;
  setActiveAgentId: SetState<string>;
  conversationId: string | null;
  setConversationId: SetState<string | null>;
  messages: ChatMessage[];
  setMessages: SetState<ChatMessage[]>;
  latestRun: RunDetail | null;
  setLatestRun: SetState<RunDetail | null>;
  runtimeEvents: RunEvent[];
  setRuntimeEvents: SetState<RunEvent[]>;
  isSending: boolean;
  setIsSending: SetState<boolean>;
  sidebarWidthPx: number;
  setSidebarWidthPx: SetState<number>;
  pendingAssistantIdRef: MutableRefObject<string | null>;
  activeRunIdRef: MutableRefObject<string | null>;
  sessionIdKey: string;
  sessionStateKey: string;
  sidebarWidthKey: string;
  scratchpadStorageKey: string;
  canvasTitle: string;
  setCanvasTitle: SetState<string>;
  canvasSubject: string;
  setCanvasSubject: SetState<string>;
  canvasPath: string;
  setCanvasPath: SetState<string>;
  canvasRecipients: string;
  setCanvasRecipients: SetState<string>;
  canvasCc: string;
  setCanvasCc: SetState<string>;
  canvasContent: string;
  setCanvasContent: SetState<string>;
  pinnedContextStorageKey: string;
  pinnedContext: PinnedContextItem[];
  setPinnedContext: SetState<PinnedContextItem[]>;
  setProxxReachable: SetState<boolean>;
  setProxxConfigured: SetState<boolean>;
  setProxxModels: SetState<ProxxModelInfo[]>;
};

export function useChatPagePersistenceSuite({
  makeId,
  sessionId,
  setSessionId,
  systemPrompt,
  setSystemPrompt,
  selectedModel,
  setSelectedModel,
  selectedThinkingLevel,
  setSelectedThinkingLevel,
  activeActorId,
  setActiveActorId,
  activeAgentId,
  setActiveAgentId,
  conversationId,
  setConversationId,
  messages,
  setMessages,
  latestRun,
  setLatestRun,
  runtimeEvents,
  setRuntimeEvents,
  isSending,
  setIsSending,
  sidebarWidthPx,
  setSidebarWidthPx,
  pendingAssistantIdRef,
  activeRunIdRef,
  sessionIdKey,
  sessionStateKey,
  sidebarWidthKey,
  scratchpadStorageKey,
  canvasTitle,
  setCanvasTitle,
  canvasSubject,
  setCanvasSubject,
  canvasPath,
  setCanvasPath,
  canvasRecipients,
  setCanvasRecipients,
  canvasCc,
  setCanvasCc,
  canvasContent,
  setCanvasContent,
  pinnedContextStorageKey,
  pinnedContext,
  setPinnedContext,
  setProxxReachable,
  setProxxConfigured,
  setProxxModels,
}: UseChatPagePersistenceSuiteParams) {
  useChatSessionPersistence({
    makeId,
    sessionId,
    setSessionId,
    systemPrompt,
    setSystemPrompt,
    selectedModel,
    setSelectedModel,
    selectedThinkingLevel,
    setSelectedThinkingLevel,
    activeActorId,
    setActiveActorId,
    activeAgentId,
    setActiveAgentId,
    conversationId,
    setConversationId,
    messages,
    setMessages,
    latestRun,
    setLatestRun,
    runtimeEvents,
    setRuntimeEvents,
    isSending,
    setIsSending,
    sidebarWidthPx,
    setSidebarWidthPx,
    pendingAssistantIdRef,
    activeRunIdRef,
    sessionIdKey,
    sessionStateKey,
    sidebarWidthKey,
  });

  useScratchpadPersistence({
    storageKey: scratchpadStorageKey,
    canvasTitle,
    setCanvasTitle,
    canvasSubject,
    setCanvasSubject,
    canvasPath,
    setCanvasPath,
    canvasRecipients,
    setCanvasRecipients,
    canvasCc,
    setCanvasCc,
    canvasContent,
    setCanvasContent,
  });

  usePinnedContextPersistence({
    storageKey: pinnedContextStorageKey,
    pinnedContext,
    setPinnedContext,
  });

  useProxxStatusPolling({
    selectedModel,
    setSelectedModel,
    setProxxReachable,
    setProxxConfigured,
    setProxxModels,
  });
}

// Recover persisted message history, including failed runs with audit traces.
export function isChatRole(value: unknown): value is ChatMessage["role"] {
  return value === "system" || value === "user" || value === "assistant";
}

export function parseMemoryRowExtra(row: MemorySessionRow): Record<string, unknown> | null {
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
  const hasPrimaryMessages = rows.some((row) => row.kind === "knoxx.message" && isChatRole(row.role) && typeof row.text === "string" && row.text.trim().length > 0);

  // Runs whose assistant answer was persisted carry their own trace timeline.
  // A run with no assistant message (failed, aborted, or empty output) would
  // otherwise drop its reasoning/tool blocks entirely and collapse to the lone
  // user message — losing the audit trail. Track which runs already have an
  // assistant turn so we can synthesize one for those that don't.
  const assistantRunIds = new Set<string>();
  for (const row of rows) {
    if (row.kind === "knoxx.message" && row.role === "assistant") {
      const rid = memoryRowRunId(row);
      if (rid) assistantRunIds.add(rid);
    }
  }

  return rows.flatMap((row, index) => {
    // Surface a no-assistant run's summary as a structured assistant turn so the
    // thinking + tool-call timeline stays visible for auditing instead of being
    // discarded. Successful runs (assistant row present) are handled below.
    if (row.kind === "knoxx.run") {
      const runId = memoryRowRunId(row);
      const blocks = runId ? derivedTraceBlocks.get(runId) : undefined;
      if (runId && !assistantRunIds.has(runId) && blocks && blocks.length > 0) {
        const extra = parseMemoryRowExtra(row);
        const status = typeof extra?.status === "string" ? extra.status : undefined;
        const synthesized: ChatMessage = {
          id: row.id || `${runId}:run`,
          role: "assistant",
          content: typeof row.text === "string" ? row.text : "",
          model: typeof row.model === "string" ? row.model : null,
          runId,
          status: status === "failed" ? "error" : "done",
          traceBlocks: blocks,
        };
        return [synthesized];
      }
      return [];
    }

    const text = typeof row.text === "string" ? row.text : "";
    const isPrimaryMessage = row.kind === "knoxx.message";
    const isLegacyReadableRow = !hasPrimaryMessages && !isPrimaryMessage && isChatRole(row.role) && text.trim().length > 0;
    if ((!isPrimaryMessage && !isLegacyReadableRow) || !isChatRole(row.role) || text.trim().length === 0) {
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

export function rewindTranscriptTurns(messages: ChatMessage[], turns = 1): ChatMessage[] {
  let remaining = [...messages];
  let turnsLeft = Math.max(1, turns);

  while (turnsLeft > 0 && remaining.length > 0) {
    const lastUserIndex = [...remaining]
      .map((message, index) => ({ message, index }))
      .reverse()
      .find(({ message }) => message.role === "user")?.index;

    if (lastUserIndex == null) {
      break;
    }

    remaining = remaining.slice(0, lastUserIndex);
    turnsLeft -= 1;
  }

  return remaining;
}

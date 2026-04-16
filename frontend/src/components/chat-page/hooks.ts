import { useEffect, useState, type Dispatch, type MutableRefObject, type SetStateAction } from "react";
import { getRun, getSessionStatus, listProxxModels, proxxHealth } from "../../lib/api";
import type { ChatMessage, MemorySessionSummary, ProxxModelInfo, RunDetail, RunEvent } from "../../lib/types";
import type { PinnedContextItem } from "./types";

type SetState<T> = Dispatch<SetStateAction<T>>;

export type ChatSessionSnapshot = {
  sessionId?: string;
  systemPrompt?: string;
  selectedModel?: string;
  conversationId?: string | null;
  messages?: ChatMessage[];
  latestRun?: RunDetail | null;
  runtimeEvents?: RunEvent[];
  isSending?: boolean;
};

const SESSION_INDEX_VERSION = 1;

type PersistedSessionIndex = {
  version: number;
  sessions: MemorySessionSummary[];
};

type ScratchpadSnapshot = {
  title?: string;
  subject?: string;
  path?: string;
  recipients?: string;
  cc?: string;
  content?: string;
};

export function getChatStorage(): Storage | null {
  if (typeof window === "undefined") return null;
  try {
    return window.sessionStorage;
  } catch {
    try {
      return window.localStorage;
    } catch {
      return null;
    }
  }
}

function getLegacyLocalStorage(): Storage | null {
  if (typeof window === "undefined") return null;
  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

function sessionSnapshotStorageKey(sessionStateKey: string, sessionId: string): string {
  return `${sessionStateKey}:${sessionId}`;
}

function sessionIndexStorageKey(sessionStateKey: string): string {
  return `${sessionStateKey}:index`;
}

function safeTrim(value: unknown): string | null {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : null;
}

function deriveSessionTitle(messages: ChatMessage[] | undefined): string {
  const firstUserMessage = messages?.find((message) => message.role === "user" && message.content.trim().length > 0)?.content?.trim();
  if (!firstUserMessage) {
    return "New chat";
  }
  return firstUserMessage.length > 72 ? `${firstUserMessage.slice(0, 72).trimEnd()}…` : firstUserMessage;
}

function normalizeSessionTimestamp(snapshot: ChatSessionSnapshot): string {
  const latestRunTimestamp = typeof snapshot.latestRun?.updated_at === "string" ? snapshot.latestRun.updated_at : null;
  if (latestRunTimestamp) {
    return latestRunTimestamp;
  }
  return new Date().toISOString();
}

function buildSessionSummary(sessionId: string, snapshot: ChatSessionSnapshot): MemorySessionSummary {
  const conversationId = safeTrim(snapshot.conversationId) ?? sessionId;
  const runStatus = snapshot.latestRun?.status;
  // Derive active status from run status rather than persisted isSending
  // since we never persist isSending=true anymore
  const isActivelySending = runStatus === "running" || runStatus === "queued";
  const activeStatus = isActivelySending
    ? "running"
    : runStatus === "completed" || runStatus === "failed"
      ? runStatus
      : (snapshot.messages?.length ?? 0) > 0
        ? "waiting_input"
        : "inactive";

  return {
    session: conversationId,
    title: deriveSessionTitle(snapshot.messages),
    title_model: null,
    last_ts: normalizeSessionTimestamp(snapshot),
    event_count: snapshot.messages?.length ?? 0,
    is_active: isActivelySending || activeStatus === "waiting_input",
    active_status: activeStatus,
    has_active_stream: isActivelySending,
    active_session_id: sessionId,
    local_only: true,
  };
}

function readSessionIndex(store: Storage | null, sessionStateKey: string): PersistedSessionIndex {
  if (!store) {
    return { version: SESSION_INDEX_VERSION, sessions: [] };
  }

  try {
    const raw = store.getItem(sessionIndexStorageKey(sessionStateKey));
    if (!raw) {
      return { version: SESSION_INDEX_VERSION, sessions: [] };
    }
    const parsed = JSON.parse(raw) as PersistedSessionIndex;
    const sessions = Array.isArray(parsed.sessions) ? parsed.sessions : [];
    return { version: SESSION_INDEX_VERSION, sessions };
  } catch {
    return { version: SESSION_INDEX_VERSION, sessions: [] };
  }
}

function writeSessionIndex(store: Storage | null, sessionStateKey: string, index: PersistedSessionIndex) {
  if (!store) return;
  store.setItem(sessionIndexStorageKey(sessionStateKey), JSON.stringify(index));
}

export function readPersistedChatSessionSnapshot(sessionStateKey: string, sessionId: string): ChatSessionSnapshot | null {
  const store = getChatStorage();
  if (!store || !sessionId) {
    return null;
  }

  try {
    const raw = store.getItem(sessionSnapshotStorageKey(sessionStateKey, sessionId));
    if (!raw) {
      return null;
    }
    return JSON.parse(raw) as ChatSessionSnapshot;
  } catch {
    return null;
  }
}

export function persistChatSessionSnapshot(sessionStateKey: string, sessionId: string, snapshot: ChatSessionSnapshot): void {
  const store = getChatStorage();
  if (!store || !sessionId) {
    return;
  }

  const normalizedSnapshot: ChatSessionSnapshot = {
    ...snapshot,
    sessionId,
  };
  store.setItem(sessionSnapshotStorageKey(sessionStateKey, sessionId), JSON.stringify(normalizedSnapshot));

  const index = readSessionIndex(store, sessionStateKey);
  const summary = buildSessionSummary(sessionId, normalizedSnapshot);
  const sessions = [summary, ...index.sessions.filter((entry) => entry.active_session_id !== sessionId)];
  writeSessionIndex(store, sessionStateKey, { version: SESSION_INDEX_VERSION, sessions });
}

export function initializePersistedChatSession(
  sessionStateKey: string,
  sessionId: string,
  conversationId: string,
  seed?: Partial<Pick<ChatSessionSnapshot, "selectedModel" | "systemPrompt">>,
): void {
  persistChatSessionSnapshot(sessionStateKey, sessionId, {
    sessionId,
    conversationId,
    selectedModel: seed?.selectedModel,
    systemPrompt: seed?.systemPrompt,
    messages: [],
    latestRun: null,
    runtimeEvents: [],
    isSending: false,
  });
}

export function listPersistedChatSessions(sessionStateKey: string): MemorySessionSummary[] {
  const store = getChatStorage();
  const index = readSessionIndex(store, sessionStateKey);
  return [...index.sessions].sort((left, right) => {
    const leftTime = Date.parse(left.last_ts ?? "") || 0;
    const rightTime = Date.parse(right.last_ts ?? "") || 0;
    return rightTime - leftTime;
  });
}

export function findPersistedChatSessionByConversation(sessionStateKey: string, conversationId: string): MemorySessionSummary | null {
  return listPersistedChatSessions(sessionStateKey).find((entry) => entry.session === conversationId) ?? null;
}

function appendConsoleLine(setConsoleLines: SetState<string[]>, line: string) {
  setConsoleLines((prev) => [...prev.slice(-400), line]);
}

type UseChatSessionPersistenceParams = {
  makeId: () => string;
  sessionId: string;
  setSessionId: SetState<string>;
  systemPrompt: string;
  setSystemPrompt: SetState<string>;
  selectedModel: string;
  setSelectedModel: SetState<string>;
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
};

export function useChatSessionPersistence({
  makeId,
  sessionId,
  setSessionId,
  systemPrompt,
  setSystemPrompt,
  selectedModel,
  setSelectedModel,
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
}: UseChatSessionPersistenceParams) {
  useEffect(() => {
    try {
      const store = getChatStorage();
      const legacy = getLegacyLocalStorage();
      let sid = store?.getItem(sessionIdKey) || "";
      if (!sid && legacy && legacy !== store) {
        sid = legacy.getItem(sessionIdKey) || "";
      }
      if (!sid) {
        sid = makeId();
        initializePersistedChatSession(sessionStateKey, sid, makeId());
      }
      store?.setItem(sessionIdKey, sid);
      if (legacy && legacy !== store) {
        legacy.removeItem(sessionIdKey);
      }
      setSessionId(sid);
    } catch {
      setSessionId(makeId());
    }
  }, [makeId, sessionIdKey, sessionStateKey, setSessionId]);

  useEffect(() => {
    if (!sessionId) return;
    try {
      const store = getChatStorage();
      const legacy = getLegacyLocalStorage();
      store?.setItem(sessionIdKey, sessionId);
      if (legacy && legacy !== store) {
        legacy.removeItem(sessionIdKey);
      }
    } catch {
      // ignore storage failures
    }
  }, [sessionId, sessionIdKey]);

  useEffect(() => {
    try {
      const raw = getChatStorage()?.getItem(sidebarWidthKey);
      if (!raw) return;
      const parsed = Number(raw);
      if (Number.isFinite(parsed)) {
        setSidebarWidthPx(Math.min(640, Math.max(260, parsed)));
      }
    } catch {
      // ignore storage failures
    }
  }, [setSidebarWidthPx, sidebarWidthKey]);

  useEffect(() => {
    try {
      getChatStorage()?.setItem(sidebarWidthKey, String(sidebarWidthPx));
    } catch {
      // ignore storage failures
    }
  }, [sidebarWidthKey, sidebarWidthPx]);

  useEffect(() => {
    if (!sessionId) return;
    try {
      const store = getChatStorage();
      const legacy = getLegacyLocalStorage();
      let parsed = readPersistedChatSessionSnapshot(sessionStateKey, sessionId);

      if (!parsed) {
        let legacyRaw = store?.getItem(sessionStateKey) || "";
        if (!legacyRaw && legacy && legacy !== store) {
          legacyRaw = legacy.getItem(sessionStateKey) || "";
        }
        if (legacyRaw) {
          parsed = JSON.parse(legacyRaw) as ChatSessionSnapshot;
          persistChatSessionSnapshot(sessionStateKey, sessionId, parsed);
          store?.removeItem(sessionStateKey);
          if (legacy && legacy !== store) {
            legacy.removeItem(sessionStateKey);
          }
        }
      }

      if (!parsed) return;

      if (typeof parsed.systemPrompt === "string") setSystemPrompt(parsed.systemPrompt);
      if (typeof parsed.selectedModel === "string") setSelectedModel(parsed.selectedModel);
      if (typeof parsed.conversationId === "string" || parsed.conversationId === null) {
        setConversationId(parsed.conversationId ?? null);
      }
      if (Array.isArray(parsed.messages)) {
        setMessages(parsed.messages.slice(-80));
        const pending = [...parsed.messages].reverse().find((message) => message.role === "assistant" && message.status === "streaming");
        pendingAssistantIdRef.current = pending?.id ?? null;
        if (!activeRunIdRef.current && typeof pending?.runId === "string") {
          activeRunIdRef.current = pending.runId;
        }
      }
      if (parsed.latestRun && typeof parsed.latestRun === "object") {
        setLatestRun(parsed.latestRun);
        if (typeof parsed.latestRun.run_id === "string") {
          activeRunIdRef.current = parsed.latestRun.run_id;
        }
      }
      if (Array.isArray(parsed.runtimeEvents)) {
        setRuntimeEvents(parsed.runtimeEvents.slice(-80));
      }
      // Never restore isSending from persisted state — always derive from runtime
      setIsSending(false);
    } catch {
      // ignore storage failures
    }
  }, [
    activeRunIdRef,
    pendingAssistantIdRef,
    sessionId,
    sessionStateKey,
    setConversationId,
    setIsSending,
    setLatestRun,
    setMessages,
    setRuntimeEvents,
    setSelectedModel,
    setSystemPrompt,
  ]);

  useEffect(() => {
    if (!sessionId) return;
    try {
      persistChatSessionSnapshot(sessionStateKey, sessionId, {
        sessionId,
        systemPrompt,
        selectedModel,
        conversationId,
        messages: messages.slice(-80),
        latestRun,
        runtimeEvents: runtimeEvents.slice(-80),
        // Never persist isSending as true — always derive from runtime state on load
        isSending: false,
      } satisfies ChatSessionSnapshot);
      getChatStorage()?.removeItem(sessionStateKey);
    } catch {
      // ignore storage failures
    }
  }, [sessionStateKey, sessionId, systemPrompt, selectedModel, conversationId, messages, latestRun, runtimeEvents, isSending]);
}

type UseChatSessionRecoveryParams = {
  sessionId: string;
  sessionStateKey: string;
  pendingAssistantIdRef: MutableRefObject<string | null>;
  activeRunIdRef: MutableRefObject<string | null>;
  setConversationId: SetState<string | null>;
  setIsSending: SetState<boolean>;
  setLatestRun: SetState<RunDetail | null>;
  setConsoleLines: SetState<string[]>;
};

export function useChatSessionRecovery({
  sessionId,
  sessionStateKey,
  pendingAssistantIdRef,
  activeRunIdRef,
  setConversationId,
  setIsSending,
  setLatestRun,
  setConsoleLines,
}: UseChatSessionRecoveryParams): boolean {
  const [isRecovering, setIsRecovering] = useState(false);

  useEffect(() => {
    if (!sessionId) return;

    const parsed = readPersistedChatSessionSnapshot(sessionStateKey, sessionId);
    if (!parsed) return;
    if (!parsed.isSending || !parsed.conversationId) return;

    let cancelled = false;

    const recoverSession = async () => {
      setIsRecovering(true);
      appendConsoleLine(setConsoleLines, "[session] checking session status...");

      try {
        const status = await getSessionStatus(sessionId, parsed.conversationId);
        if (cancelled) return;

        appendConsoleLine(
          setConsoleLines,
          `[session] status: ${status.status}, streaming: ${status.has_active_stream}, can_send: ${status.can_send}`,
        );

        if (status.status === "running" && status.has_active_stream) {
          setConversationId(status.conversation_id ?? null);
          setIsSending(true);
          appendConsoleLine(setConsoleLines, "[session] reconnecting to active stream...");
          return;
        }

        if (status.status === "running" && !status.has_active_stream) {
          setConversationId(status.conversation_id ?? null);
          setIsSending(true);
          appendConsoleLine(setConsoleLines, "[session] agent waiting for input, enable controls");
          return;
        }

        if (status.status === "completed" || status.status === "failed") {
          setIsSending(false);
          pendingAssistantIdRef.current = null;
          appendConsoleLine(setConsoleLines, `[session] session ${status.status}, ready for new message`);
          return;
        }

        if (status.status === "not_found" || status.status === "unknown") {
          // Clear stale pending assistant immediately — no session exists on the backend
          pendingAssistantIdRef.current = null;

          const lastRunId = parsed.messages
            ?.filter((message) => message.runId)
            .map((message) => message.runId)
            .pop();

          if (lastRunId) {
            appendConsoleLine(setConsoleLines, `[session] session not in Redis, checking run ${lastRunId.slice(0, 8)}...`);
            try {
              const run = await getRun(lastRunId);
              if (cancelled) return;

              if (run.status === "running" || run.status === "queued") {
                setLatestRun(run);
                setConversationId(run.conversation_id ?? null);
                setIsSending(true);
                activeRunIdRef.current = lastRunId;
                appendConsoleLine(setConsoleLines, "[session] run still active, polling...");
              } else {
                setIsSending(false);
                pendingAssistantIdRef.current = null;
                appendConsoleLine(setConsoleLines, `[session] run ${run.status}, ready for new message`);
              }
            } catch (runError) {
              // getRun failed (404 after restart, or network error) — treat as stale
              if (cancelled) return;
              appendConsoleLine(setConsoleLines, `[session] run not found or fetch failed, starting fresh`);
              setIsSending(false);
              pendingAssistantIdRef.current = null;
            }
          } else {
            setIsSending(false);
            appendConsoleLine(setConsoleLines, "[session] no active run, starting fresh");
          }
        }
      } catch (error) {
        if (cancelled) return;
        appendConsoleLine(setConsoleLines, `[session] recovery failed: ${(error as Error).message}`);
        setIsSending(false);
        pendingAssistantIdRef.current = null;
      } finally {
        if (!cancelled) {
          setIsRecovering(false);
        }
      }
    };

    const timeout = window.setTimeout(recoverSession, 500);
    return () => {
      cancelled = true;
      window.clearTimeout(timeout);
    };
  }, [activeRunIdRef, pendingAssistantIdRef, sessionId, sessionStateKey, setConsoleLines, setConversationId, setIsSending, setLatestRun]);

  return isRecovering;
}

type UseScratchpadPersistenceParams = {
  storageKey: string;
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
};

export function useScratchpadPersistence({
  storageKey,
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
}: UseScratchpadPersistenceParams) {
  useEffect(() => {
    if (typeof window === "undefined") return;
    try {
      const raw = window.localStorage.getItem(storageKey);
      if (!raw) return;
      const parsed = JSON.parse(raw) as ScratchpadSnapshot;
      if (typeof parsed.title === "string") setCanvasTitle(parsed.title);
      if (typeof parsed.subject === "string") setCanvasSubject(parsed.subject);
      if (typeof parsed.path === "string") setCanvasPath(parsed.path);
      if (typeof parsed.recipients === "string") setCanvasRecipients(parsed.recipients);
      if (typeof parsed.cc === "string") setCanvasCc(parsed.cc);
      if (typeof parsed.content === "string") setCanvasContent(parsed.content);
    } catch {
      // ignore storage failures
    }
  }, [setCanvasCc, setCanvasContent, setCanvasPath, setCanvasRecipients, setCanvasSubject, setCanvasTitle, storageKey]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    try {
      window.localStorage.setItem(
        storageKey,
        JSON.stringify({
          title: canvasTitle,
          subject: canvasSubject,
          path: canvasPath,
          recipients: canvasRecipients,
          cc: canvasCc,
          content: canvasContent,
        } satisfies ScratchpadSnapshot),
      );
    } catch {
      // ignore storage failures
    }
  }, [storageKey, canvasTitle, canvasSubject, canvasPath, canvasRecipients, canvasCc, canvasContent]);
}

type UsePinnedContextPersistenceParams = {
  storageKey: string;
  pinnedContext: PinnedContextItem[];
  setPinnedContext: SetState<PinnedContextItem[]>;
};

export function usePinnedContextPersistence({ storageKey, pinnedContext, setPinnedContext }: UsePinnedContextPersistenceParams) {
  useEffect(() => {
    if (typeof window === "undefined") return;
    try {
      const raw = window.localStorage.getItem(storageKey);
      if (!raw) return;
      const parsed = JSON.parse(raw) as PinnedContextItem[];
      if (Array.isArray(parsed)) setPinnedContext(parsed.slice(0, 24));
    } catch {
      // ignore storage failures
    }
  }, [setPinnedContext, storageKey]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    try {
      window.localStorage.setItem(storageKey, JSON.stringify(pinnedContext.slice(0, 24)));
    } catch {
      // ignore storage failures
    }
  }, [storageKey, pinnedContext]);
}

type UseProxxStatusPollingParams = {
  selectedModel: string;
  setSelectedModel: SetState<string>;
  setProxxReachable: SetState<boolean>;
  setProxxConfigured: SetState<boolean>;
  setProxxModels: SetState<ProxxModelInfo[]>;
};

export function useProxxStatusPolling({
  selectedModel,
  setSelectedModel,
  setProxxReachable,
  setProxxConfigured,
  setProxxModels,
}: UseProxxStatusPollingParams) {
  useEffect(() => {
    let timer: number | null = null;

    const poll = async () => {
      try {
        const status = await proxxHealth();
        setProxxReachable(Boolean(status.reachable));
        setProxxConfigured(Boolean(status.configured));
        const models = await listProxxModels();
        setProxxModels(models);
        if (!selectedModel) {
          const preferred = models.find((model) => model.id === status.default_model);
          setSelectedModel(preferred?.id ?? models[0]?.id ?? "");
        }
      } catch {
        setProxxReachable(false);
      }
    };

    void poll();
    timer = window.setInterval(() => {
      void poll();
    }, 5000);

    return () => {
      if (timer !== null) window.clearInterval(timer);
    };
  }, [selectedModel, setProxxConfigured, setProxxModels, setProxxReachable, setSelectedModel]);
}

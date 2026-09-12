import { getChatStorage, readPersistedChatSessionSnapshot, persistChatSessionSnapshot, initializePersistedChatSession, getLegacySessionStorage, migrateSessionStateFromLegacy, sessionSnapshotStorageKey } from "../../lib/storage";
import { useEffect, useState, type Dispatch, type MutableRefObject, type SetStateAction } from "react";
import { getRun, getSessionStatus } from "../../lib/api";
import type { ChatMessage, RunDetail, RunEvent } from "../../lib/types";
import type { ChatSessionSnapshot } from "./types";
export type { ChatSessionSnapshot } from "./types";

type SetState<T> = Dispatch<SetStateAction<T>>;

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
};

export function useChatSessionPersistence({
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
}: UseChatSessionPersistenceParams) {
  useEffect(() => {
    try {
      const store = getChatStorage();
      const legacy = getLegacySessionStorage();

      migrateSessionStateFromLegacy({ sessionStateKey, store, legacy });

      let sid = store?.getItem(sessionIdKey) || "";
      if (!sid && legacy && legacy !== store) {
        sid = legacy.getItem(sessionIdKey) || "";
      }
      if (!sid) {
        sid = makeId();
        initializePersistedChatSession(sessionStateKey, sid, makeId());
      }
      store?.setItem(sessionIdKey, sid);
      setSessionId(sid);
    } catch {
      setSessionId(makeId());
    }
  }, [makeId, sessionIdKey, sessionStateKey, setSessionId]);

  useEffect(() => {
    if (!sessionId) return;
    try {
      const store = getChatStorage();
      store?.setItem(sessionIdKey, sessionId);
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
      const legacy = getLegacySessionStorage();
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
      if (typeof parsed.selectedThinkingLevel === "string") setSelectedThinkingLevel(parsed.selectedThinkingLevel);
      if (typeof parsed.activeActorId === "string") setActiveActorId(parsed.activeActorId);
      if (typeof parsed.activeAgentId === "string") setActiveAgentId(parsed.activeAgentId);
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
    setSelectedThinkingLevel,
    setActiveActorId,
    setActiveAgentId,
    setSystemPrompt,
  ]);

  useEffect(() => {
    if (!sessionId) return;
    try {
      persistChatSessionSnapshot(sessionStateKey, sessionId, {
        sessionId,
        systemPrompt,
        selectedModel,
        selectedThinkingLevel,
        activeActorId,
        activeAgentId,
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
  }, [sessionStateKey, sessionId, systemPrompt, selectedModel, selectedThinkingLevel, activeActorId, activeAgentId, conversationId, messages, latestRun, runtimeEvents, isSending]);
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
    if (!parsed.conversationId) return;

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

        if (status.status === "running") {
          setConversationId(status.conversation_id ?? null);
          setIsSending(true);
          appendConsoleLine(setConsoleLines, "[session] agent still processing; waiting for resume or first token");
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


export { getChatStorage, readPersistedChatSessionSnapshot, persistChatSessionSnapshot, initializePersistedChatSession, listPersistedChatSessions, findPersistedChatSessionByConversation, usePinnedContextPersistence } from "../../lib/storage";
export { useScratchpadPersistence } from "./ChatScratchpadPanel";
export { useProxxStatusPolling } from "./chat-page-config";

import { useEffect, type Dispatch, type SetStateAction } from "react";
import type { ChatMessage, MemorySessionSummary } from "./types";
import type { ChatSessionSnapshot, PinnedContextItem } from "../components/chat-page/types";
type SetState<T> = Dispatch<SetStateAction<T>>;

// Safe localStorage helpers with quota protection
export function safeGetItem(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

export function safeSetItem(key: string, value: string): boolean {
  try {
    localStorage.setItem(key, value);
    return true;
  } catch {
    // Quota exceeded — try clearing old knoxx keys
    try {
      const keysToRemove: string[] = [];
      for (let i = 0; i < localStorage.length; i++) {
        const k = localStorage.key(i);
        if (k && k.startsWith("knoxx_") && k !== key) {
          keysToRemove.push(k);
        }
      }
      // Remove oldest half
      keysToRemove.slice(0, Math.floor(keysToRemove.length / 2)).forEach((k) => {
        try { localStorage.removeItem(k); } catch { /* ignore */ }
      });
      // Try again
      localStorage.setItem(key, value);
      return true;
    } catch {
      return false;
    }
  }
}

export function safeRemoveItem(key: string): void {
  try { localStorage.removeItem(key); } catch { /* ignore */ }
}

export function clearKnoxxStorage(): void {
  const keysToRemove: string[] = [];
  for (let i = 0; i < localStorage.length; i++) {
    const k = localStorage.key(i);
    if (k && k.startsWith("knoxx_")) {
      keysToRemove.push(k);
    }
  }
  keysToRemove.forEach((k) => {
    try { localStorage.removeItem(k); } catch { /* ignore */ }
  });
}

const SESSION_INDEX_VERSION = 1;

type PersistedSessionIndex = {
  version: number;
  sessions: MemorySessionSummary[];
};

export function getChatStorage(): Storage | null {
  if (typeof window === "undefined") return null;
  try {
    // Prefer localStorage so chat sessions survive reloads/new tabs.
    return window.localStorage;
  } catch {
    try {
      return window.sessionStorage;
    } catch {
      return null;
    }
  }
}

export function getLegacySessionStorage(): Storage | null {
  if (typeof window === "undefined") return null;
  try {
    return window.sessionStorage;
  } catch {
    return null;
  }
}

export function migrateSessionStateFromLegacy({
  sessionStateKey,
  store,
  legacy,
}: {
  sessionStateKey: string;
  store: Storage | null;
  legacy: Storage | null;
}): void {
  if (!store || !legacy) return;
  if (store === legacy) return;

  // Copy the chat-session index + per-session snapshots into the primary store.
  // This reverses a previous refactor that migrated localStorage -> sessionStorage,
  // which caused sessions to disappear when opening a new tab.
  try {
    for (let i = 0; i < legacy.length; i += 1) {
      const key = legacy.key(i);
      if (!key) continue;
      if (key === sessionStateKey || key.startsWith(`${sessionStateKey}:`)) {
        if (store.getItem(key) != null) continue;
        const value = legacy.getItem(key);
        if (value != null) {
          store.setItem(key, value);
        }
      }
    }
  } catch {
    // ignore migration failures
  }
}

export function sessionSnapshotStorageKey(sessionStateKey: string, sessionId: string): string {
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
  const hasStreamingMessage = Boolean(snapshot.messages?.some((message) => message.role === "assistant" && message.status === "streaming"));
  // Derive active status primarily from run status, but fall back to the persisted
  // snapshot for brand-new turns (where latestRun may not be persisted yet).
  const isActivelySending = runStatus === "running" || runStatus === "queued" || snapshot.isSending === true || hasStreamingMessage;
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
  seed?: Partial<Pick<ChatSessionSnapshot, "selectedModel" | "selectedThinkingLevel" | "systemPrompt" | "activeActorId" | "activeAgentId">>,
): void {
  persistChatSessionSnapshot(sessionStateKey, sessionId, {
    sessionId,
    conversationId,
    selectedModel: seed?.selectedModel,
    selectedThinkingLevel: seed?.selectedThinkingLevel,
    systemPrompt: seed?.systemPrompt,
    activeActorId: seed?.activeActorId,
    activeAgentId: seed?.activeAgentId,
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

export function mergeSessionPages(primary: MemorySessionSummary[], secondary: MemorySessionSummary[]): MemorySessionSummary[] {
  const statusScore = (item: MemorySessionSummary): number => {
    if (item.has_active_stream) return 50;
    const status = typeof item.active_status === "string" ? item.active_status : "";
    if (status === "running") return 40;
    if (status === "queued") return 35;
    if (status === "waiting_input") return 30;
    if (status === "failed") return 20;
    if (status === "completed") return 10;
    if (item.is_active) return 5;
    return 0;
  };

  const parseTs = (value?: string | null): number => {
    if (!value) return 0;
    const parsed = Date.parse(value);
    return Number.isFinite(parsed) ? parsed : 0;
  };

  const mergeEntry = (left: MemorySessionSummary, right: MemorySessionSummary): MemorySessionSummary => {
    const leftScore = statusScore(left);
    const rightScore = statusScore(right);
    const live = leftScore >= rightScore ? left : right;
    const lastTs = parseTs(left.last_ts ?? null) >= parseTs(right.last_ts ?? null) ? left.last_ts : right.last_ts;

    return {
      session: left.session,
      title: left.title || right.title,
      title_model: left.title_model ?? right.title_model ?? null,
      last_ts: lastTs,
      event_count: Math.max(left.event_count ?? 0, right.event_count ?? 0),

      // Liveness should always prefer the most-active view (local snapshot or Redis-enriched remote row).
      is_active: Boolean(live.is_active),
      active_status: live.active_status ?? left.active_status ?? right.active_status,
      has_active_stream: Boolean(live.has_active_stream),

      // Prefer the active session id from whichever side is live; otherwise keep any known id.
      active_session_id: live.active_session_id ?? left.active_session_id ?? right.active_session_id ?? null,

      // Only truly local-only if both sides are local-only.
      local_only: Boolean(left.local_only) && Boolean(right.local_only),
    };
  };

  const byId = new Map<string, MemorySessionSummary>();
  for (const item of primary) {
    byId.set(item.session, item);
  }
  for (const item of secondary) {
    const existing = byId.get(item.session);
    byId.set(item.session, existing ? mergeEntry(existing, item) : item);
  }
  return [...byId.values()];
}

export function sortSessions(items: MemorySessionSummary[]): MemorySessionSummary[] {
  return [...items].sort((left, right) => {
    const leftTime = Date.parse(left.last_ts ?? "") || 0;
    const rightTime = Date.parse(right.last_ts ?? "") || 0;
    if (rightTime !== leftTime) {
      return rightTime - leftTime;
    }
    if (left.is_active !== right.is_active) {
      return left.is_active ? -1 : 1;
    }
    return (left.title ?? left.session).localeCompare(right.title ?? right.session);
  });
}

import { renderHook, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { connectStream } from "../../lib/api";

import {
  initializePersistedChatSession,
  listPersistedChatSessions,
  persistChatSessionSnapshot,
  readPersistedChatSessionSnapshot,
  useChatSessionRecovery,
} from "./hooks";

vi.mock("../../lib/api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../../lib/api")>()),
  API_BASE: "",
  getRun: vi.fn(),
  getSessionStatus: vi.fn(),
  listProxxModels: vi.fn(),
  proxxHealth: vi.fn(),
}));

import { getRun, getSessionStatus } from "../../lib/api";

const SESSION_STATE_KEY = "knoxx_chat_session_state:test";

describe("chat session persistence helpers", () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    window.localStorage.clear();
  });

  it("stores snapshots per session id instead of using one global chat snapshot", () => {
    persistChatSessionSnapshot(SESSION_STATE_KEY, "session-a", {
      sessionId: "session-a",
      conversationId: "conversation-a",
      messages: [{ id: "msg-a", role: "user", content: "alpha" }],
      runtimeEvents: [],
      isSending: false,
    });

    persistChatSessionSnapshot(SESSION_STATE_KEY, "session-b", {
      sessionId: "session-b",
      conversationId: "conversation-b",
      messages: [{ id: "msg-b", role: "user", content: "beta" }],
      runtimeEvents: [],
      isSending: true,
    });

    expect(readPersistedChatSessionSnapshot(SESSION_STATE_KEY, "session-a")?.conversationId).toBe("conversation-a");
    expect(readPersistedChatSessionSnapshot(SESSION_STATE_KEY, "session-b")?.conversationId).toBe("conversation-b");
    expect(readPersistedChatSessionSnapshot(SESSION_STATE_KEY, "session-a")?.messages?.[0]?.content).toBe("alpha");
    expect(readPersistedChatSessionSnapshot(SESSION_STATE_KEY, "session-b")?.messages?.[0]?.content).toBe("beta");
  });

  it("lists local draft sessions so brand-new chats can appear in the sidebar immediately", () => {
    initializePersistedChatSession(SESSION_STATE_KEY, "session-new", "conversation-new", {
      selectedModel: "gemma4:e4b",
      systemPrompt: "stay grounded",
      activeAgentId: "knoxx_default",
    });

    const [entry] = listPersistedChatSessions(SESSION_STATE_KEY);
    expect(entry.session).toBe("conversation-new");
    expect(entry.active_session_id).toBe("session-new");
    expect(entry.local_only).toBe(true);
    expect(entry.title).toBe("New chat");
    expect(entry.active_status).toBe("inactive");
  });

  it("persists the active agent id with the chat session", () => {
    initializePersistedChatSession(SESSION_STATE_KEY, "session-agent", "conversation-agent", {
      activeAgentId: "knoxx_default",
    });

    expect(readPersistedChatSessionSnapshot(SESSION_STATE_KEY, "session-agent")?.activeAgentId).toBe("knoxx_default");
  });

  it("derives a readable title from the first user message", () => {
    persistChatSessionSnapshot(SESSION_STATE_KEY, "session-c", {
      sessionId: "session-c",
      conversationId: "conversation-c",
      messages: [
        { id: "msg-1", role: "user", content: "Investigate chat persistence isolation in Knoxx" },
        { id: "msg-2", role: "assistant", content: "On it", status: "streaming" },
      ],
      runtimeEvents: [],
      isSending: true,
    });

    const [entry] = listPersistedChatSessions(SESSION_STATE_KEY);
    expect(entry.title).toContain("Investigate chat persistence isolation");
    expect(entry.is_active).toBe(true);
    expect(entry.has_active_stream).toBe(true);
    expect(entry.active_status).toBe("running");
  });

  it("recovers backend-running sessions even when the local snapshot lost isSending", async () => {
    persistChatSessionSnapshot(SESSION_STATE_KEY, "session-recover", {
      sessionId: "session-recover",
      conversationId: "conversation-recover",
      messages: [{ id: "msg-1", role: "assistant", content: "", status: "streaming" }],
      runtimeEvents: [],
      isSending: false,
    });

    vi.mocked(getSessionStatus).mockResolvedValue({
      session_id: "session-recover",
      conversation_id: "conversation-recover",
      status: "running",
      has_active_stream: true,
      can_send: false,
      reason: "Session is actively streaming. Use steer or wait.",
    });
    vi.mocked(getRun).mockResolvedValue({
      run_id: "run-1",
      conversation_id: "conversation-recover",
      session_id: "session-recover",
      status: "running",
      answer: null,
      error: null,
    } as never);

    const pendingAssistantIdRef = { current: "assistant-pending" };
    const activeRunIdRef = { current: null };
    const setConversationId = vi.fn();
    const setIsSending = vi.fn();
    const setLatestRun = vi.fn();
    const setConsoleLines = vi.fn();

    renderHook(() => useChatSessionRecovery({
      sessionId: "session-recover",
      sessionStateKey: SESSION_STATE_KEY,
      pendingAssistantIdRef,
      activeRunIdRef,
      setConversationId,
      setIsSending,
      setLatestRun,
      setConsoleLines,
    }));

    await waitFor(() => {
      expect(getSessionStatus).toHaveBeenCalledWith("session-recover", "conversation-recover");
      expect(setConversationId).toHaveBeenCalledWith("conversation-recover");
      expect(setIsSending).toHaveBeenCalledWith(true);
    });
  });
});


describe("compiled WebSocket facade", () => {
  const original = Object.getOwnPropertyDescriptor(window, "knoxx");
  afterEach(() => {
    if (original) Object.defineProperty(window, "knoxx", original);
    else Reflect.deleteProperty(window, "knoxx");
  });

  it("resolves the adapter late and forwards API arguments and callable cleanup", () => {
    Reflect.deleteProperty(window, "knoxx");
    expect(() => connectStream({}, "session")).toThrow("connect_stream_js is unavailable");
    const disconnect = vi.fn();
    const setConversationId = vi.fn();
    const connection = Object.assign(disconnect, { disconnect, setConversationId });
    const adapter = vi.fn(() => connection);
    Object.defineProperty(window, "knoxx", { configurable: true, value: {
      frontend: { lib: { ws: { connect_stream_js: adapter } } },
    } });
    const handlers = { onToken: vi.fn() };
    expect(connectStream(handlers, "session", "conversation")).toBe(connection);
    expect(adapter).toHaveBeenCalledWith(handlers, "session", "conversation", "");
    connection.setConversationId(null);
    expect(setConversationId).toHaveBeenCalledWith(null);
    connection();
    expect(disconnect).toHaveBeenCalledTimes(1);
  });

  it("refuses a malformed compiled connection without substituting a transport", () => {
    Object.defineProperty(window, "knoxx", { configurable: true, value: {
      frontend: { lib: { ws: { connect_stream_js: () => ({}) } } },
    } });
    expect(() => connectStream({})).toThrow("returned an invalid connection");
  });
});

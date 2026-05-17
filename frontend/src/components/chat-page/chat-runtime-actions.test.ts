import { describe, expect, it, vi, beforeEach } from "vitest";
import { waitFor } from "@testing-library/react";

vi.mock("../../lib/api", () => ({
  getRun: vi.fn(),
  knoxxAbort: vi.fn(),
  knoxxChatStart: vi.fn(),
  knoxxControl: vi.fn(),
  knoxxUndoSessionTurn: vi.fn(),
}));

import { createChatRuntimeActions } from "./chat-runtime-actions";
import { getRun, knoxxChatStart } from "../../lib/api";
import type { ChatMessage, RunDetail, RunEvent } from "../../lib/types";

type SetState<T> = (value: T | ((previous: T) => T)) => void;

function createStateHarness<T>(initial: T): [() => T, SetState<T>] {
  let current = initial;
  return [() => current, (value) => {
    current = typeof value === "function"
      ? (value as (previous: T) => T)(current)
      : value;
  }];
}

function createRuntimeActionHarness(options: {
  makeId?: () => string;
  initialMessages?: ChatMessage[];
  sessionId?: string;
  conversationId?: string | null;
  selectedModel?: string;
  activeRunId?: string | null;
  pendingAssistantId?: string | null;
} = {}) {
  const [getMessages, setMessages] = createStateHarness<ChatMessage[]>(options.initialMessages ?? []);
  const [getLatestRun, setLatestRun] = createStateHarness<RunDetail | null>(null);
  const [getRuntimeEvents, setRuntimeEvents] = createStateHarness<RunEvent[]>([]);
  const [getIsSending, setIsSending] = createStateHarness(false);
  const [getConsoleLines, setConsoleLines] = createStateHarness<string[]>([]);
  const [getQueueingControl, setQueueingControl] = createStateHarness<"steer" | "follow_up" | null>(null);
  const [getAbortingTurn, setAbortingTurn] = createStateHarness(false);
  const [getConversationId, setConversationId] = createStateHarness<string | null>(options.conversationId ?? "conversation-1");
  const [getSessionId, setSessionId] = createStateHarness(options.sessionId ?? "session-1");

  const pendingAssistantIdRef = { current: options.pendingAssistantId ?? null as string | null };
  const activeRunIdRef = { current: options.activeRunId ?? null as string | null };

  const actions = createChatRuntimeActions({
    makeId: options.makeId ?? (() => {
      let counter = 0;
      return () => `msg-${++counter}`;
    })(),
    systemPrompt: "Stay grounded and explicit about uncertainty.",
    activeRole: "knowledge_worker",
    activeActorId: "chat_primary",
    activeAgentId: "knoxx_default",
    sessionId: getSessionId(),
    setSessionId,
    conversationId: getConversationId(),
    setConversationId,
    selectedModel: options.selectedModel ?? "gemma4:31b",
    selectedThinkingLevel: "medium",
    liveControlEnabled: false,
    liveControlText: "",
    setLiveControlText: vi.fn(),
    setMessages,
    setLatestRun,
    setRuntimeEvents,
    setIsSending,
    setConsoleLines,
    setQueueingControl,
    setAbortingTurn,
    pendingAssistantIdRef,
    activeRunIdRef,
    sessionIdKey: "session-key",
    sessionStateKey: "state-key",
  });

  return {
    actions,
    activeRunIdRef,
    pendingAssistantIdRef,
    getAbortingTurn,
    getConsoleLines,
    getConversationId,
    getIsSending,
    getLatestRun,
    getMessages,
    getQueueingControl,
    getRuntimeEvents,
    getSessionId,
  };
}

describe("createChatRuntimeActions.handleSend", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("sends the clean user message and forwards the steering prompt through agentSpec.system_prompt", async () => {
    vi.mocked(knoxxChatStart).mockResolvedValue({
      ok: true,
      queued: true,
      run_id: "run-1",
      conversation_id: "conversation-1",
      session_id: "session-1",
      model: "gemma4:31b",
    });
    vi.mocked(getRun).mockResolvedValue({
      run_id: "run-1",
      conversation_id: "conversation-1",
      session_id: "session-1",
      status: "running",
      answer: null,
      error: null,
    } as unknown as RunDetail);

    const [getMessages, setMessages] = createStateHarness<ChatMessage[]>([]);
    const [, setLatestRun] = createStateHarness<RunDetail | null>(null);
    const [, setRuntimeEvents] = createStateHarness<RunEvent[]>([]);
    const [, setIsSending] = createStateHarness(false);
    const [, setConsoleLines] = createStateHarness<string[]>([]);
    const [, setQueueingControl] = createStateHarness<"steer" | "follow_up" | null>(null);
    const [, setAbortingTurn] = createStateHarness(false);
    const [, setConversationId] = createStateHarness<string | null>("conversation-1");
    const [, setSessionId] = createStateHarness("session-1");

    const pendingAssistantIdRef = { current: null as string | null };
    const activeRunIdRef = { current: null as string | null };

    const actions = createChatRuntimeActions({
      makeId: (() => {
        let counter = 0;
        return () => `msg-${++counter}`;
      })(),
      systemPrompt: "Stay grounded and explicit about uncertainty.",
      activeRole: "knowledge_worker",
      activeActorId: "chat_primary",
      activeAgentId: "knoxx_default",
      sessionId: "session-1",
      setSessionId,
      conversationId: "conversation-1",
      setConversationId,
      selectedModel: "gemma4:31b",
      selectedThinkingLevel: "medium",
      liveControlEnabled: false,
      liveControlText: "",
      setLiveControlText: vi.fn(),
      setMessages,
      setLatestRun,
      setRuntimeEvents,
      setIsSending,
      setConsoleLines,
      setQueueingControl,
      setAbortingTurn,
      pendingAssistantIdRef,
      activeRunIdRef,
      sessionIdKey: "session-key",
      sessionStateKey: "state-key",
    });

    await actions.handleSend("testing?");

    expect(knoxxChatStart).toHaveBeenCalledWith({
      message: "testing?",
      conversation_id: "conversation-1",
      session_id: "session-1",
      run_id: null,
      model: "gemma4:31b",
      thinkingLevel: "medium",
      contentParts: undefined,
      agentSpec: {
        actor_id: "chat_primary",
        contract_id: "knoxx_default",
        role: "knowledge_worker",
        system_prompt: "Stay grounded and explicit about uncertainty.",
      },
    });

    expect(getMessages().map((message) => ({ role: message.role, content: message.content }))).toEqual([
      { role: "user", content: "testing?" },
      { role: "assistant", content: "" },
    ]);
  });

  it("keeps session, conversation, and run identifiers monotonic while a queued run hydrates", async () => {
    vi.mocked(knoxxChatStart).mockResolvedValue({
      ok: true,
      queued: true,
      run_id: "run-1",
      conversation_id: "conversation-2",
      session_id: "session-1",
      model: "gemma4:31b",
    });
    vi.mocked(getRun).mockResolvedValue({
      run_id: "run-1",
      conversation_id: "conversation-2",
      session_id: "session-1",
      status: "completed",
      answer: "Final hydrated answer.",
      error: null,
      model: "gemma4:31b",
      sources: [],
      contentParts: [],
    } as unknown as RunDetail);

    const harness = createRuntimeActionHarness({ sessionId: "session-1", conversationId: "conversation-1" });

    await harness.actions.handleSend("testing?");

    expect(knoxxChatStart).toHaveBeenCalledWith(expect.objectContaining({
      message: "testing?",
      conversation_id: "conversation-1",
      session_id: "session-1",
      run_id: null,
    }));
    expect(harness.getConversationId()).toBe("conversation-2");
    expect(harness.getSessionId()).toBe("session-1");
    expect(harness.activeRunIdRef.current).toBe("run-1");

    await waitFor(() => expect(harness.getLatestRun()?.run_id).toBe("run-1"));

    const messages = harness.getMessages();
    expect(messages.map((message) => message.role)).toEqual(["user", "assistant"]);
    expect(messages[1]).toMatchObject({
      content: "Final hydrated answer.",
      model: "gemma4:31b",
      runId: "run-1",
      status: "done",
    });
    expect(harness.pendingAssistantIdRef.current).toBeNull();
    expect(harness.getRuntimeEvents()).toEqual([]);
  });
});

describe("createChatRuntimeActions.handleNewChat", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("resets transient runtime state and advances to a fresh session/conversation pair", () => {
    const ids = ["session-new", "conversation-new"];
    const harness = createRuntimeActionHarness({
      makeId: () => ids.shift() ?? "extra-id",
      initialMessages: [
        { id: "u1", role: "user", content: "old question" },
        { id: "a1", role: "assistant", content: "old answer", status: "streaming" },
      ],
      sessionId: "session-old",
      conversationId: "conversation-old",
      activeRunId: "run-old",
      pendingAssistantId: "a1",
    });

    harness.actions.handleNewChat();

    expect(harness.getSessionId()).toBe("session-new");
    expect(harness.getConversationId()).toBe("conversation-new");
    expect(harness.getMessages()).toEqual([]);
    expect(harness.getLatestRun()).toBeNull();
    expect(harness.getRuntimeEvents()).toEqual([]);
    expect(harness.activeRunIdRef.current).toBeNull();
    expect(harness.pendingAssistantIdRef.current).toBeNull();
    expect(harness.getIsSending()).toBe(false);
  });
});

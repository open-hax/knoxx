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
  const [, setQueueingControl] = createStateHarness<"steer" | "follow_up" | null>(null);
  const [, setAbortingTurn] = createStateHarness(false);
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
    getConsoleLines,
    getConversationId,
    getIsSending,
    getLatestRun,
    getMessages,
    getRuntimeEvents,
    getSessionId,
  };
}

const completedRun: RunDetail = {
  run_id: "run-1",
  created_at: "2026-10-09T18:00:00Z",
  updated_at: "2026-10-09T18:00:01Z",
  status: "completed",
  answer: "Final answer for the original turn.",
  request_messages: [],
  settings: {},
  resources: {},
};

beforeEach(() => {
  vi.resetAllMocks();
});

describe("createChatRuntimeActions.handleSend", () => {
  it("sends the clean user message and forwards the steering prompt through agentSpec.system_prompt", async () => {
    vi.mocked(knoxxChatStart).mockResolvedValue({
      ok: true,
      queued: true,
      run_id: "run-1",
      conversation_id: "conversation-1",
      session_id: "session-1",
      model: "gemma4:31b",
    });
    vi.mocked(getRun).mockResolvedValue({ ...completedRun, status: "running", answer: null });

    const harness = createRuntimeActionHarness();
    await harness.actions.handleSend("testing?");

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

    expect(harness.getMessages().map((message) => ({ role: message.role, content: message.content }))).toEqual([
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
      ...completedRun, conversation_id: "conversation-2", session_id: "session-1",
      answer: "Final hydrated answer.", model: "gemma4:31b", sources: [], contentParts: [],
    });

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

describe("createChatRuntimeActions.loadRunDetail", () => {
  it('keeps an active turn associated when an ordinary running-detail request fails', async () => {
    vi.mocked(getRun).mockRejectedValue(new Error('502 run detail unavailable'));
    const harness = createRuntimeActionHarness({
      initialMessages: [{ id: 'assistant-1', role: 'assistant', content: 'Streaming', status: 'streaming' }],
      pendingAssistantId: 'assistant-1', activeRunId: 'run-1',
    });
    await harness.actions.loadRunDetail('run-1');
    expect(harness.pendingAssistantIdRef.current).toBe('assistant-1');
    expect(harness.getMessages()[0]).toMatchObject({ content: 'Streaming', status: 'streaming' });
    await harness.actions.handleUndoLastTurn();
    expect(harness.getConsoleLines()).toContain('[undo] wait for the active turn to finish or abort it first');
  });

  it.each([
    { activeRun: 'run-2', pendingAssistant: 'assistant-old' },
    { activeRun: 'run-1', pendingAssistant: 'assistant-new' },
  ])('preserves a newer association when terminal hydration rejects with $activeRun/$pendingAssistant', async ({ activeRun, pendingAssistant }) => {
    let rejectRun!: (error: Error) => void;
    vi.mocked(getRun).mockReturnValue(new Promise((_, reject) => { rejectRun = reject; }));
    const harness = createRuntimeActionHarness({
      initialMessages: [{ id: 'assistant-old', role: 'assistant', content: 'Retained answer', status: 'done' }],
      pendingAssistantId: 'assistant-old', activeRunId: 'run-1',
    });
    const terminalRead = harness.actions.loadRunDetail('run-1', true);
    harness.activeRunIdRef.current = activeRun;
    harness.pendingAssistantIdRef.current = pendingAssistant;
    rejectRun(new Error('502 run detail unavailable'));
    await terminalRead;
    expect(harness.pendingAssistantIdRef.current).toBe(pendingAssistant);
    expect(harness.getMessages()[0]).toMatchObject({ content: 'Retained answer', status: 'done' });
  });

  it.each(["run-2", "run-1"])("does not hydrate a newer assistant when a deferred GET resolves with active run %s", async (activeRunId) => {
    let resolveRun!: (run: RunDetail) => void;
    let resolveStart!: (response: Awaited<ReturnType<typeof knoxxChatStart>>) => void;
    vi.mocked(getRun).mockReturnValueOnce(new Promise((resolve) => { resolveRun = resolve; }));
    vi.mocked(knoxxChatStart).mockReturnValue(new Promise((resolve) => { resolveStart = resolve; }));
    const harness = createRuntimeActionHarness({
      initialMessages: [{ id: "assistant-old", role: "assistant", content: "Original stream", status: "streaming" }],
      pendingAssistantId: "assistant-old",
      activeRunId: "run-1",
    });
    const loadingOriginalRun = harness.actions.loadRunDetail("run-1");
    const startingNewRun = harness.actions.handleSend("A new question");
    const newAssistantId = harness.pendingAssistantIdRef.current;
    expect(newAssistantId).not.toBe("assistant-old");
    // A new queued run or a late WS event can rebind this ref before the old GET resolves.
    harness.activeRunIdRef.current = activeRunId;
    resolveRun(completedRun);
    await loadingOriginalRun;

    expect(harness.getMessages().find((message) => message.id === newAssistantId)).toMatchObject({
      role: "assistant", content: "", status: "streaming",
    });
    expect(harness.pendingAssistantIdRef.current).toBe(newAssistantId);
    expect(harness.getMessages()[0].content).toBe(activeRunId === "run-1" ? completedRun.answer : "Original stream");

    vi.mocked(getRun).mockResolvedValue({ ...completedRun, run_id: "run-2", status: "running", answer: null });
    resolveStart({ ok: true, queued: true, run_id: "run-2", conversation_id: "conversation-1", session_id: "session-1" });
    await startingNewRun;
  });

  it("retains the original assistant association through a 404 retry", async () => {
    vi.useFakeTimers();
    try {
      vi.mocked(getRun).mockRejectedValueOnce(new Error("404 run not yet persisted")).mockResolvedValue(completedRun);
      const harness = createRuntimeActionHarness({
        initialMessages: [
          { id: "assistant-old", role: "assistant", content: "Original stream", status: "streaming" },
          { id: "assistant-new", role: "assistant", content: "New stream", status: "streaming" },
        ],
        pendingAssistantId: "assistant-old",
        activeRunId: "run-1",
      });
      await harness.actions.loadRunDetail("run-1");
      harness.pendingAssistantIdRef.current = "assistant-new";
      await vi.advanceTimersByTimeAsync(250);

      expect(getRun).toHaveBeenCalledTimes(2);
      expect(harness.getMessages()[0].content).toBe(completedRun.answer);
      expect(harness.getMessages()[1]).toMatchObject({ content: "New stream", status: "streaming" });
      expect(harness.pendingAssistantIdRef.current).toBe("assistant-new");
    } finally {
      vi.useRealTimers();
    }
  });

  it.each(["completed", "failed"] as const)("keeps %s hydration when a stale running GET resolves later", async (terminalStatus) => {
    let resolveRunning!: (run: RunDetail) => void;
    let resolveTerminal!: (run: RunDetail) => void;
    vi.mocked(getRun)
      .mockReturnValueOnce(new Promise((resolve) => { resolveRunning = resolve; }))
      .mockReturnValueOnce(new Promise((resolve) => { resolveTerminal = resolve; }));
    const harness = createRuntimeActionHarness({
      initialMessages: [{ id: "assistant-1", role: "assistant", content: "Streaming", status: "streaming" }],
      pendingAssistantId: "assistant-1",
      activeRunId: "run-1",
    });
    const runningRead = harness.actions.loadRunDetail("run-1");
    const terminalRead = harness.actions.loadRunDetail("run-1");
    const terminalRun: RunDetail = {
      ...completedRun, status: terminalStatus, answer: terminalStatus === "failed" ? null : completedRun.answer,
      error: terminalStatus === "failed" ? "Provider failed" : undefined,
      contentParts: [{ type: "text", text: "Final structured answer" }],
      sources: [{ url: "https://example.test/source-1", title: "Retained source" }], model: "terminal-model",
    };
    resolveTerminal(terminalRun);
    await terminalRead;
    const finalMessage = harness.getMessages()[0];
    expect(finalMessage.status).toBe(terminalStatus === "completed" ? "done" : "error");
    expect(harness.pendingAssistantIdRef.current).toBeNull();
    resolveRunning({ ...completedRun, status: "running", answer: null, contentParts: [], sources: [], model: "stale-model" });
    await runningRead;
    expect(harness.getLatestRun()).toEqual(terminalRun);
    expect(harness.getMessages()[0]).toEqual(finalMessage);
    expect(harness.pendingAssistantIdRef.current).toBeNull();
  });

  it("keeps terminal status when a later running read starts after hydration cleared the assistant ref", async () => {
    vi.mocked(getRun).mockResolvedValueOnce(completedRun)
      .mockResolvedValueOnce({ ...completedRun, status: "running", answer: null, contentParts: [], sources: [] });
    const harness = createRuntimeActionHarness({
      initialMessages: [{ id: "assistant-1", role: "assistant", content: "Streaming", status: "streaming" }],
      pendingAssistantId: "assistant-1", activeRunId: "run-1",
    });
    await harness.actions.loadRunDetail("run-1");
    await harness.actions.loadRunDetail("run-1");
    expect(harness.getLatestRun()).toEqual(completedRun);
    expect(harness.getMessages()[0]).toMatchObject({ content: completedRun.answer, status: "done" });
  });
});

describe("createChatRuntimeActions.handleNewChat", () => {
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

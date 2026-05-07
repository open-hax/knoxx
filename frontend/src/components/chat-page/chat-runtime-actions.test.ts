import { describe, expect, it, vi, beforeEach } from "vitest";

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
});

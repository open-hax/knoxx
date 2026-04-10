import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { createChatRuntimeActions } from "./chat-runtime-actions";
import * as api from "../../lib/api";
import type { ChatMessage, RunDetail } from "../../lib/types";

// Mock the api module
vi.mock("../../lib/api", () => ({
  getRun: vi.fn(),
  knoxxChatStart: vi.fn(),
  knoxxControl: vi.fn(),
}));

// Mock getChatStorage from hooks
vi.mock("./hooks", () => ({
  getChatStorage: () => ({
    setItem: vi.fn(),
    removeItem: vi.fn(),
  }),
}));

describe("createChatRuntimeActions", () => {
  let mockStorage: { [key: string]: string };
  let mockSetItem: ReturnType<typeof vi.fn>;
  let mockRemoveItem: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.useFakeTimers();
    mockStorage = {};
    mockSetItem = vi.fn((key: string, value: string) => { mockStorage[key] = value; });
    mockRemoveItem = vi.fn((key: string) => { delete mockStorage[key]; });

    vi.mocked(api.getRun).mockReset();
    vi.mocked(api.knoxxChatStart).mockReset();
    vi.mocked(api.knoxxControl).mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  const makeTestParams = () => ({
    makeId: () => `id-${Date.now()}`,
    systemPrompt: "",
    sessionId: "session-123",
    setSessionId: vi.fn(),
    conversationId: null as string | null,
    setConversationId: vi.fn(),
    selectedModel: "glm-5",
    liveControlEnabled: true,
    liveControlText: "steer message",
    setLiveControlText: vi.fn(),
    setMessages: vi.fn(),
    setLatestRun: vi.fn(),
    setRuntimeEvents: vi.fn(),
    setIsSending: vi.fn(),
    setConsoleLines: vi.fn(),
    setQueueingControl: vi.fn(),
    pendingAssistantIdRef: { current: null } as React.MutableRefObject<string | null>,
    activeRunIdRef: { current: null } as React.MutableRefObject<string | null>,
    sessionIdKey: "knoxx-session-id",
    sessionStateKey: "knoxx-session-state",
  });

  describe("handleNewChat", () => {
    it("creates new session ID", () => {
      const params = makeTestParams();
      const actions = createChatRuntimeActions(params);

      actions.handleNewChat();

      expect(params.setSessionId).toHaveBeenCalled();
      const newSessionId = params.setSessionId.mock.calls[0][0];
      expect(newSessionId).toMatch(/^id-/);
    });

    it("clears messages and conversation", () => {
      const params = makeTestParams();
      const actions = createChatRuntimeActions(params);

      actions.handleNewChat();

      expect(params.setMessages).toHaveBeenCalledWith([]);
      expect(params.setConversationId).toHaveBeenCalledWith(null);
    });

    it("clears runtime state", () => {
      const params = makeTestParams();
      const actions = createChatRuntimeActions(params);

      actions.handleNewChat();

      expect(params.setLatestRun).toHaveBeenCalledWith(null);
      expect(params.setRuntimeEvents).toHaveBeenCalledWith([]);
      expect(params.setLiveControlText).toHaveBeenCalledWith("");
      expect(params.setIsSending).toHaveBeenCalledWith(false);
    });

    it("clears refs", () => {
      const params = makeTestParams();
      params.activeRunIdRef.current = "run-123";
      params.pendingAssistantIdRef.current = "msg-456";
      const actions = createChatRuntimeActions(params);

      actions.handleNewChat();

      expect(params.activeRunIdRef.current).toBeNull();
      expect(params.pendingAssistantIdRef.current).toBeNull();
    });
  });

  describe("handleSend", () => {
    it("logs error when no session", async () => {
      const params = makeTestParams();
      params.sessionId = "";
      const actions = createChatRuntimeActions(params);

      await actions.handleSend("hello");

      expect(params.setConsoleLines).toHaveBeenCalled();
      const consoleUpdater = params.setConsoleLines.mock.calls[0][0];
      const lines = consoleUpdater([]);
      expect(lines.some((l: string) => l.includes("session not ready"))).toBe(true);
    });

    it("logs error when no model selected", async () => {
      const params = makeTestParams();
      params.selectedModel = "";
      const actions = createChatRuntimeActions(params);

      await actions.handleSend("hello");

      expect(params.setConsoleLines).toHaveBeenCalled();
      const consoleUpdater = params.setConsoleLines.mock.calls[0][0];
      const lines = consoleUpdater([]);
      expect(lines.some((l: string) => l.includes("no model selected"))).toBe(true);
    });

    it("creates user and assistant messages", async () => {
      vi.mocked(api.knoxxChatStart).mockResolvedValue({
        run_id: "run-123",
        conversation_id: "conv-456",
        model: "glm-5",
      });

      const params = makeTestParams();
      const actions = createChatRuntimeActions(params);

      await actions.handleSend("hello world");

      expect(params.setMessages).toHaveBeenCalled();
      const messagesCall = params.setMessages.mock.calls[0][0];
      const messages = messagesCall([]);
      expect(messages).toHaveLength(2);
      expect(messages[0].role).toBe("user");
      expect(messages[0].content).toBe("hello world");
      expect(messages[1].role).toBe("assistant");
      expect(messages[1].status).toBe("streaming");
    });

    it("prepends system prompt when set", async () => {
      vi.mocked(api.knoxxChatStart).mockResolvedValue({
        run_id: "run-123",
        conversation_id: "conv-456",
      });

      const params = makeTestParams();
      params.systemPrompt = "You are a helpful assistant.";
      const actions = createChatRuntimeActions(params);

      await actions.handleSend("hello");

      expect(api.knoxxChatStart).toHaveBeenCalledWith(
        expect.objectContaining({
          message: expect.stringContaining("Session steering note:"),
        })
      );
    });

    it("sets isSending to true", async () => {
      vi.mocked(api.knoxxChatStart).mockResolvedValue({
        run_id: "run-123",
        conversation_id: "conv-456",
      });

      const params = makeTestParams();
      const actions = createChatRuntimeActions(params);

      await actions.handleSend("hello");

      expect(params.setIsSending).toHaveBeenCalledWith(true);
    });

    it("updates conversation ID from response", async () => {
      vi.mocked(api.knoxxChatStart).mockResolvedValue({
        run_id: "run-123",
        conversation_id: "conv-new",
      });

      const params = makeTestParams();
      const actions = createChatRuntimeActions(params);

      await actions.handleSend("hello");

      expect(params.setConversationId).toHaveBeenCalledWith("conv-new");
    });

    it("handles API errors", async () => {
      vi.mocked(api.knoxxChatStart).mockRejectedValue(new Error("Network error"));

      const params = makeTestParams();
      let capturedMessages: ChatMessage[] = [];
      params.setMessages = vi.fn((updater) => {
        capturedMessages = updater(capturedMessages);
      });
      const actions = createChatRuntimeActions(params);

      await actions.handleSend("hello");

      // The last message should be the assistant with error
      const lastMessage = capturedMessages[capturedMessages.length - 1];
      expect(lastMessage.role).toBe("assistant");
      expect(lastMessage.content).toContain("Agent request failed");
      expect(lastMessage.status).toBe("error");
      expect(params.setIsSending).toHaveBeenCalledWith(false);
    });
  });

  describe("loadRunDetail", () => {
    it("loads run and updates latestRun", async () => {
      const mockRun: RunDetail = {
        run_id: "run-123",
        status: "completed",
        answer: "Hello back!",
        model: "glm-5",
        sources: [],
      };
      vi.mocked(api.getRun).mockResolvedValue(mockRun);

      const params = makeTestParams();
      params.activeRunIdRef.current = "run-123";
      const actions = createChatRuntimeActions(params);

      await actions.loadRunDetail("run-123");

      expect(api.getRun).toHaveBeenCalledWith("run-123");
      expect(params.setLatestRun).toHaveBeenCalledWith(mockRun);
    });

    it("updates pending assistant message with answer", async () => {
      const mockRun: RunDetail = {
        run_id: "run-123",
        status: "completed",
        answer: "The answer is 42.",
        model: "glm-5",
        sources: [{ title: "Source", url: "https://example.com" }],
      };
      vi.mocked(api.getRun).mockResolvedValue(mockRun);

      const params = makeTestParams();
      params.activeRunIdRef.current = "run-123";
      params.pendingAssistantIdRef.current = "msg-456";
      const actions = createChatRuntimeActions(params);

      await actions.loadRunDetail("run-123");

      expect(params.setMessages).toHaveBeenCalled();
    });

    it("does not update if runId changed", async () => {
      const mockRun: RunDetail = { run_id: "run-123", status: "completed" };
      vi.mocked(api.getRun).mockResolvedValue(mockRun);

      const params = makeTestParams();
      params.activeRunIdRef.current = "run-other"; // Different run ID
      const actions = createChatRuntimeActions(params);

      await actions.loadRunDetail("run-123");

      expect(params.setLatestRun).not.toHaveBeenCalled();
    });

    it("handles errors gracefully", async () => {
      vi.mocked(api.getRun).mockRejectedValue(new Error("Run not found"));

      const params = makeTestParams();
      const actions = createChatRuntimeActions(params);

      await actions.loadRunDetail("run-123");

      expect(params.setConsoleLines).toHaveBeenCalled();
    });
  });

  describe("queueLiveControl", () => {
    it("does nothing when liveControlText is empty", async () => {
      const params = makeTestParams();
      params.liveControlText = "";
      const actions = createChatRuntimeActions(params);

      await actions.queueLiveControl("steer");

      expect(api.knoxxControl).not.toHaveBeenCalled();
    });

    it("does nothing when conversationId is null", async () => {
      const params = makeTestParams();
      params.conversationId = null;
      const actions = createChatRuntimeActions(params);

      await actions.queueLiveControl("steer");

      expect(api.knoxxControl).not.toHaveBeenCalled();
    });

    it("does nothing when liveControlEnabled is false", async () => {
      const params = makeTestParams();
      params.liveControlEnabled = false;
      const actions = createChatRuntimeActions(params);

      await actions.queueLiveControl("steer");

      expect(api.knoxxControl).not.toHaveBeenCalled();
    });

    it("calls knoxxControl with correct params", async () => {
      vi.mocked(api.knoxxControl).mockResolvedValue({
        run_id: "run-789",
        conversation_id: "conv-456",
      });

      const params = makeTestParams();
      params.conversationId = "conv-456";
      params.activeRunIdRef.current = "run-123";
      const actions = createChatRuntimeActions(params);

      await actions.queueLiveControl("steer");

      expect(api.knoxxControl).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: "steer",
          message: "steer message",
          conversation_id: "conv-456",
          session_id: "session-123",
          run_id: "run-123",
        })
      );
    });

    it("sets queueingControl during request", async () => {
      vi.mocked(api.knoxxControl).mockResolvedValue({
        run_id: "run-789",
      });

      const params = makeTestParams();
      params.conversationId = "conv-456";
      const actions = createChatRuntimeActions(params);

      await actions.queueLiveControl("follow_up");

      expect(params.setQueueingControl).toHaveBeenCalledWith("follow_up");
      expect(params.setQueueingControl).toHaveBeenCalledWith(null);
    });

    it("clears liveControlText on success", async () => {
      vi.mocked(api.knoxxControl).mockResolvedValue({
        run_id: "run-789",
      });

      const params = makeTestParams();
      params.conversationId = "conv-456";
      const actions = createChatRuntimeActions(params);

      await actions.queueLiveControl("steer");

      expect(params.setLiveControlText).toHaveBeenCalledWith("");
    });

    it("handles errors", async () => {
      vi.mocked(api.knoxxControl).mockRejectedValue(new Error("Control failed"));

      const params = makeTestParams();
      params.conversationId = "conv-456";
      const actions = createChatRuntimeActions(params);

      await actions.queueLiveControl("steer");

      expect(params.setConsoleLines).toHaveBeenCalled();
    });
  });

  describe("updateMessageById", () => {
    it("updates matching message", () => {
      const params = makeTestParams();
      const actions = createChatRuntimeActions(params);

      actions.updateMessageById("msg-1", (msg) => ({
        ...msg,
        content: "updated",
      }));

      expect(params.setMessages).toHaveBeenCalled();
      const updater = params.setMessages.mock.calls[0][0];
      const messages = [{ id: "msg-1", content: "original" }, { id: "msg-2", content: "other" }];
      const result = updater(messages);
      expect(result[0].content).toBe("updated");
      expect(result[1].content).toBe("other");
    });

    it("leaves non-matching messages unchanged", () => {
      const params = makeTestParams();
      const actions = createChatRuntimeActions(params);

      actions.updateMessageById("nonexistent", (msg) => ({
        ...msg,
        content: "updated",
      }));

      expect(params.setMessages).toHaveBeenCalled();
      const updater = params.setMessages.mock.calls[0][0];
      const messages = [{ id: "msg-1", content: "original" }];
      const result = updater(messages);
      expect(result[0].content).toBe("original");
    });
  });

  describe("appendMessageIfMissing", () => {
    it("appends message when not present", () => {
      const params = makeTestParams();
      const actions = createChatRuntimeActions(params);

      const newMessage: ChatMessage = { id: "msg-new", role: "user", content: "new" };
      actions.appendMessageIfMissing(newMessage);

      expect(params.setMessages).toHaveBeenCalled();
      const updater = params.setMessages.mock.calls[0][0];
      const existing = [{ id: "msg-1", content: "existing" }];
      const result = updater(existing);
      expect(result).toHaveLength(2);
      expect(result[1].id).toBe("msg-new");
    });

    it("does not append when message exists", () => {
      const params = makeTestParams();
      const actions = createChatRuntimeActions(params);

      const existingMessage: ChatMessage = { id: "msg-1", role: "user", content: "existing" };
      actions.appendMessageIfMissing(existingMessage);

      expect(params.setMessages).toHaveBeenCalled();
      const updater = params.setMessages.mock.calls[0][0];
      const existing = [{ id: "msg-1", content: "existing" }];
      const result = updater(existing);
      expect(result).toHaveLength(1);
    });
  });
});

import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { renderHook, act } from "@testing-library/react";
import {
  getChatStorage,
  useChatSessionPersistence,
  useChatSessionRecovery,
  useScratchpadPersistence,
  usePinnedContextPersistence,
  useProxxStatusPolling,
} from "./hooks";
import * as api from "../../lib/api";
import type { ChatMessage, RunDetail, RunEvent, PinnedContextItem } from "./types";

// Mock the api module
vi.mock("../../lib/api", () => ({
  getRun: vi.fn(),
  getSessionStatus: vi.fn(),
  listProxxModels: vi.fn(),
  proxxHealth: vi.fn(),
}));

describe("getChatStorage", () => {
  const originalSessionStorage = window.sessionStorage;
  const originalLocalStorage = window.localStorage;

  afterEach(() => {
    Object.defineProperty(window, "sessionStorage", { value: originalSessionStorage, writable: true, configurable: true });
    Object.defineProperty(window, "localStorage", { value: originalLocalStorage, writable: true, configurable: true });
  });

  it("returns sessionStorage when available", () => {
    expect(getChatStorage()).toBe(window.sessionStorage);
  });

  it("returns null when window is undefined", () => {
    const originalWindow = globalThis.window;
    // @ts-expect-error - testing undefined window
    delete globalThis.window;
    expect(getChatStorage()).toBeNull();
    globalThis.window = originalWindow;
  });
});

describe("useChatSessionPersistence", () => {
  let mockStorage: { [key: string]: string };
  let mockSessionStorage: Storage;
  let mockLocalStorage: Storage;

  beforeEach(() => {
    mockStorage = {};
    mockSessionStorage = {
      getItem: (key: string) => mockStorage[key] ?? null,
      setItem: (key: string, value: string) => { mockStorage[key] = value; },
      removeItem: (key: string) => { delete mockStorage[key]; },
      clear: () => { mockStorage = {}; },
      key: (index: number) => Object.keys(mockStorage)[index] ?? null,
      length: Object.keys(mockStorage).length,
    };
    Object.defineProperty(window, "sessionStorage", { value: mockSessionStorage, writable: true, configurable: true });
    Object.defineProperty(window, "localStorage", { value: mockSessionStorage, writable: true, configurable: true });

    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  const makeTestParams = () => ({
    makeId: () => "test-session-id",
    sessionId: "",
    setSessionId: vi.fn(),
    systemPrompt: "",
    setSystemPrompt: vi.fn(),
    selectedModel: "",
    setSelectedModel: vi.fn(),
    conversationId: null as string | null,
    setConversationId: vi.fn(),
    messages: [] as ChatMessage[],
    setMessages: vi.fn(),
    latestRun: null as RunDetail | null,
    setLatestRun: vi.fn(),
    runtimeEvents: [] as RunEvent[],
    setRuntimeEvents: vi.fn(),
    isSending: false,
    setIsSending: vi.fn(),
    sidebarWidthPx: 300,
    setSidebarWidthPx: vi.fn(),
    pendingAssistantIdRef: { current: null } as React.MutableRefObject<string | null>,
    activeRunIdRef: { current: null } as React.MutableRefObject<string | null>,
    sessionIdKey: "knoxx-session-id",
    sessionStateKey: "knoxx-session-state",
    sidebarWidthKey: "knoxx-sidebar-width",
  });

  it("creates new session ID when none exists", () => {
    const params = makeTestParams();
    renderHook(() => useChatSessionPersistence(params));
    act(() => { vi.runAllTimers(); });
    expect(params.setSessionId).toHaveBeenCalledWith("test-session-id");
  });

  it("uses existing session ID from storage", () => {
    mockStorage["knoxx-session-id"] = "existing-session";
    const params = makeTestParams();
    renderHook(() => useChatSessionPersistence(params));
    act(() => { vi.runAllTimers(); });
    expect(params.setSessionId).toHaveBeenCalledWith("existing-session");
  });

  it("restores sidebar width from storage", () => {
    mockStorage["knoxx-sidebar-width"] = "400";
    const params = makeTestParams();
    renderHook(() => useChatSessionPersistence(params));
    act(() => { vi.runAllTimers(); });
    expect(params.setSidebarWidthPx).toHaveBeenCalledWith(400);
  });

  it("clamps sidebar width to 260-640 range", () => {
    mockStorage["knoxx-sidebar-width"] = "100";
    const params = makeTestParams();
    renderHook(() => useChatSessionPersistence(params));
    act(() => { vi.runAllTimers(); });
    expect(params.setSidebarWidthPx).toHaveBeenCalledWith(260);
  });

  it("restores session state from storage", () => {
    mockStorage["knoxx-session-state"] = JSON.stringify({
      systemPrompt: "custom prompt",
      selectedModel: "glm-5",
      conversationId: "conv-123",
      messages: [{ id: "m1", role: "user", content: "hello" }],
      isSending: true,
    });
    const params = makeTestParams();
    renderHook(() => useChatSessionPersistence(params));
    act(() => { vi.runAllTimers(); });
    expect(params.setSystemPrompt).toHaveBeenCalledWith("custom prompt");
    expect(params.setSelectedModel).toHaveBeenCalledWith("glm-5");
    expect(params.setConversationId).toHaveBeenCalledWith("conv-123");
    expect(params.setMessages).toHaveBeenCalled();
    expect(params.setIsSending).toHaveBeenCalledWith(true);
  });

  it("truncates messages to last 80 on restore", () => {
    const messages = Array.from({ length: 100 }, (_, i) => ({ id: `m${i}`, role: "user", content: `msg ${i}` }));
    mockStorage["knoxx-session-state"] = JSON.stringify({ messages });
    const params = makeTestParams();
    renderHook(() => useChatSessionPersistence(params));
    act(() => { vi.runAllTimers(); });
    expect(params.setMessages).toHaveBeenCalledWith(messages.slice(-80));
  });

  it("saves session state to storage when values change", () => {
    const params = makeTestParams();
    params.sessionId = "session-123";
    params.systemPrompt = "test prompt";
    params.selectedModel = "glm-5";
    params.conversationId = "conv-456";
    params.messages = [{ id: "m1", role: "user", content: "hi" }];
    params.isSending = false;

    renderHook(() => useChatSessionPersistence(params));
    act(() => { vi.runAllTimers(); });

    const saved = JSON.parse(mockStorage["knoxx-session-state"]);
    expect(saved.sessionId).toBe("session-123");
    expect(saved.systemPrompt).toBe("test prompt");
    expect(saved.selectedModel).toBe("glm-5");
    expect(saved.conversationId).toBe("conv-456");
  });
});

describe("useChatSessionRecovery", () => {
  let mockStorage: { [key: string]: string };
  let mockSessionStorage: Storage;

  beforeEach(() => {
    mockStorage = {};
    mockSessionStorage = {
      getItem: (key: string) => mockStorage[key] ?? null,
      setItem: (key: string, value: string) => { mockStorage[key] = value; },
      removeItem: (key: string) => { delete mockStorage[key]; },
      clear: () => { mockStorage = {}; },
      key: (index: number) => Object.keys(mockStorage)[index] ?? null,
      length: Object.keys(mockStorage).length,
    };
    Object.defineProperty(window, "sessionStorage", { value: mockSessionStorage, writable: true, configurable: true });

    vi.useFakeTimers();
    vi.mocked(api.getSessionStatus).mockReset();
    vi.mocked(api.getRun).mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  const makeTestParams = () => ({
    sessionId: "session-123",
    sessionStateKey: "knoxx-session-state",
    pendingAssistantIdRef: { current: null } as React.MutableRefObject<string | null>,
    activeRunIdRef: { current: null } as React.MutableRefObject<string | null>,
    setConversationId: vi.fn(),
    setIsSending: vi.fn(),
    setLatestRun: vi.fn(),
    setConsoleLines: vi.fn(),
  });

  it("does nothing when no saved state exists", () => {
    const params = makeTestParams();
    renderHook(() => useChatSessionRecovery(params));
    act(() => { vi.runAllTimers(); });
    expect(api.getSessionStatus).not.toHaveBeenCalled();
  });

  it("does nothing when saved state has no isSending", () => {
    mockStorage["knoxx-session-state"] = JSON.stringify({
      conversationId: "conv-123",
      isSending: false,
    });
    const params = makeTestParams();
    renderHook(() => useChatSessionRecovery(params));
    act(() => { vi.runAllTimers(); });
    expect(api.getSessionStatus).not.toHaveBeenCalled();
  });

  it("recovers session with active stream", async () => {
    mockStorage["knoxx-session-state"] = JSON.stringify({
      conversationId: "conv-123",
      isSending: true,
    });
    vi.mocked(api.getSessionStatus).mockResolvedValue({
      status: "running",
      has_active_stream: true,
      can_send: false,
      conversation_id: "conv-123",
    });

    const params = makeTestParams();
    renderHook(() => useChatSessionRecovery(params));
    await act(async () => {
      vi.advanceTimersByTime(600);
    });

    expect(api.getSessionStatus).toHaveBeenCalledWith("session-123", "conv-123");
    expect(params.setIsSending).toHaveBeenCalledWith(true);
    expect(params.setConversationId).toHaveBeenCalledWith("conv-123");
  });

  it("recovers session with completed status", async () => {
    mockStorage["knoxx-session-state"] = JSON.stringify({
      conversationId: "conv-123",
      isSending: true,
    });
    vi.mocked(api.getSessionStatus).mockResolvedValue({
      status: "completed",
      has_active_stream: false,
      can_send: true,
      conversation_id: "conv-123",
    });

    const params = makeTestParams();
    renderHook(() => useChatSessionRecovery(params));
    await act(async () => {
      vi.advanceTimersByTime(600);
    });

    expect(params.setIsSending).toHaveBeenCalledWith(false);
    expect(params.pendingAssistantIdRef.current).toBeNull();
  });

  it("falls back to getRun when session not found", async () => {
    mockStorage["knoxx-session-state"] = JSON.stringify({
      conversationId: "conv-123",
      isSending: true,
      messages: [{ id: "m1", role: "assistant", runId: "run-456", content: "hi" }],
    });
    vi.mocked(api.getSessionStatus).mockResolvedValue({
      status: "not_found",
      has_active_stream: false,
      can_send: true,
      conversation_id: null,
    });
    vi.mocked(api.getRun).mockResolvedValue({
      run_id: "run-456",
      status: "running",
      conversation_id: "conv-123",
    } as RunDetail);

    const params = makeTestParams();
    renderHook(() => useChatSessionRecovery(params));
    await act(async () => {
      vi.advanceTimersByTime(600);
    });

    expect(api.getRun).toHaveBeenCalledWith("run-456");
    expect(params.setLatestRun).toHaveBeenCalled();
    expect(params.activeRunIdRef.current).toBe("run-456");
  });

  it("handles recovery errors", async () => {
    mockStorage["knoxx-session-state"] = JSON.stringify({
      conversationId: "conv-123",
      isSending: true,
    });
    vi.mocked(api.getSessionStatus).mockRejectedValue(new Error("Network error"));

    const params = makeTestParams();
    renderHook(() => useChatSessionRecovery(params));
    await act(async () => {
      vi.advanceTimersByTime(600);
    });

    expect(params.setIsSending).toHaveBeenCalledWith(false);
    expect(params.pendingAssistantIdRef.current).toBeNull();
  });
});

describe("useScratchpadPersistence", () => {
  let mockLocalStorage: Storage;
  let mockStorage: { [key: string]: string };

  beforeEach(() => {
    mockStorage = {};
    mockLocalStorage = {
      getItem: (key: string) => mockStorage[key] ?? null,
      setItem: (key: string, value: string) => { mockStorage[key] = value; },
      removeItem: (key: string) => { delete mockStorage[key]; },
      clear: () => { mockStorage = {}; },
      key: (index: number) => Object.keys(mockStorage)[index] ?? null,
      length: Object.keys(mockStorage).length,
    };
    Object.defineProperty(window, "localStorage", { value: mockLocalStorage, writable: true, configurable: true });

    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  const makeTestParams = () => ({
    storageKey: "knoxx-scratchpad",
    canvasTitle: "",
    setCanvasTitle: vi.fn(),
    canvasSubject: "",
    setCanvasSubject: vi.fn(),
    canvasPath: "",
    setCanvasPath: vi.fn(),
    canvasRecipients: "",
    setCanvasRecipients: vi.fn(),
    canvasCc: "",
    setCanvasCc: vi.fn(),
    canvasContent: "",
    setCanvasContent: vi.fn(),
  });

  it("restores scratchpad from storage", () => {
    mockStorage["knoxx-scratchpad"] = JSON.stringify({
      title: "Test Title",
      subject: "Test Subject",
      path: "/docs/test.md",
      recipients: "user@example.com",
      cc: "cc@example.com",
      content: "Test content",
    });

    const params = makeTestParams();
    renderHook(() => useScratchpadPersistence(params));
    act(() => { vi.runAllTimers(); });

    expect(params.setCanvasTitle).toHaveBeenCalledWith("Test Title");
    expect(params.setCanvasSubject).toHaveBeenCalledWith("Test Subject");
    expect(params.setCanvasPath).toHaveBeenCalledWith("/docs/test.md");
    expect(params.setCanvasRecipients).toHaveBeenCalledWith("user@example.com");
    expect(params.setCanvasCc).toHaveBeenCalledWith("cc@example.com");
    expect(params.setCanvasContent).toHaveBeenCalledWith("Test content");
  });

  it("saves scratchpad to storage", () => {
    const params = makeTestParams();
    params.canvasTitle = "New Title";
    params.canvasSubject = "New Subject";
    params.canvasContent = "New content";

    renderHook(() => useScratchpadPersistence(params));
    act(() => { vi.runAllTimers(); });

    const saved = JSON.parse(mockStorage["knoxx-scratchpad"]);
    expect(saved.title).toBe("New Title");
    expect(saved.subject).toBe("New Subject");
    expect(saved.content).toBe("New content");
  });
});

describe("usePinnedContextPersistence", () => {
  let mockLocalStorage: Storage;
  let mockStorage: { [key: string]: string };

  beforeEach(() => {
    mockStorage = {};
    mockLocalStorage = {
      getItem: (key: string) => mockStorage[key] ?? null,
      setItem: (key: string, value: string) => { mockStorage[key] = value; },
      removeItem: (key: string) => { delete mockStorage[key]; },
      clear: () => { mockStorage = {}; },
      key: (index: number) => Object.keys(mockStorage)[index] ?? null,
      length: Object.keys(mockStorage).length,
    };
    Object.defineProperty(window, "localStorage", { value: mockLocalStorage, writable: true, configurable: true });

    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  const makeTestParams = () => ({
    storageKey: "knoxx-pinned-context",
    pinnedContext: [] as PinnedContextItem[],
    setPinnedContext: vi.fn(),
  });

  it("restores pinned context from storage", () => {
    const items = [
      { id: "c1", kind: "doc", path: "/docs/a.md", snippet: "content a" },
      { id: "c2", kind: "doc", path: "/docs/b.md", snippet: "content b" },
    ];
    mockStorage["knoxx-pinned-context"] = JSON.stringify(items);

    const params = makeTestParams();
    renderHook(() => usePinnedContextPersistence(params));
    act(() => { vi.runAllTimers(); });

    expect(params.setPinnedContext).toHaveBeenCalledWith(items);
  });

  it("truncates restored context to 24 items", () => {
    const items = Array.from({ length: 30 }, (_, i) => ({
      id: `c${i}`,
      kind: "doc",
      path: `/docs/${i}.md`,
      snippet: `content ${i}`,
    }));
    mockStorage["knoxx-pinned-context"] = JSON.stringify(items);

    const params = makeTestParams();
    renderHook(() => usePinnedContextPersistence(params));
    act(() => { vi.runAllTimers(); });

    expect(params.setPinnedContext).toHaveBeenCalledWith(items.slice(0, 24));
  });

  it("saves pinned context to storage", () => {
    const items = [
      { id: "c1", kind: "doc", path: "/docs/a.md", snippet: "content a" },
    ];
    const params = makeTestParams();
    params.pinnedContext = items;

    renderHook(() => usePinnedContextPersistence(params));
    act(() => { vi.runAllTimers(); });

    const saved = JSON.parse(mockStorage["knoxx-pinned-context"]);
    expect(saved).toEqual(items);
  });

  it("truncates saved context to 24 items", () => {
    const items = Array.from({ length: 30 }, (_, i) => ({
      id: `c${i}`,
      kind: "doc",
      path: `/docs/${i}.md`,
      snippet: `content ${i}`,
    }));
    const params = makeTestParams();
    params.pinnedContext = items;

    renderHook(() => usePinnedContextPersistence(params));
    act(() => { vi.runAllTimers(); });

    const saved = JSON.parse(mockStorage["knoxx-pinned-context"]);
    expect(saved.length).toBe(24);
  });
});

describe("useProxxStatusPolling", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.mocked(api.proxxHealth).mockReset();
    vi.mocked(api.listProxxModels).mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  const makeTestParams = () => ({
    selectedModel: "",
    setSelectedModel: vi.fn(),
    setProxxReachable: vi.fn(),
    setProxxConfigured: vi.fn(),
    setProxxModels: vi.fn(),
  });

  it("polls proxx health on mount", async () => {
    vi.mocked(api.proxxHealth).mockResolvedValue({ reachable: true, configured: true, default_model: "glm-5" });
    vi.mocked(api.listProxxModels).mockResolvedValue([
      { id: "glm-5", name: "GLM-5", provider: "zhipu" },
      { id: "gpt-4", name: "GPT-4", provider: "openai" },
    ]);

    const params = makeTestParams();
    renderHook(() => useProxxStatusPolling(params));
    
    // Run the initial poll (setTimeout + async)
    await act(async () => {
      await vi.runOnlyPendingTimersAsync();
    });

    expect(api.proxxHealth).toHaveBeenCalled();
    expect(params.setProxxReachable).toHaveBeenCalledWith(true);
    expect(params.setProxxConfigured).toHaveBeenCalledWith(true);
    expect(params.setProxxModels).toHaveBeenCalled();
  });

  it("sets default model from health response", async () => {
    vi.mocked(api.proxxHealth).mockResolvedValue({ reachable: true, configured: true, default_model: "glm-5" });
    vi.mocked(api.listProxxModels).mockResolvedValue([
      { id: "glm-5", name: "GLM-5", provider: "zhipu" },
      { id: "gpt-4", name: "GPT-4", provider: "openai" },
    ]);

    const params = makeTestParams();
    renderHook(() => useProxxStatusPolling(params));
    
    await act(async () => {
      await vi.runOnlyPendingTimersAsync();
    });

    expect(params.setSelectedModel).toHaveBeenCalledWith("glm-5");
  });

  it("sets first model when default not in list", async () => {
    vi.mocked(api.proxxHealth).mockResolvedValue({ reachable: true, configured: true, default_model: "unknown" });
    vi.mocked(api.listProxxModels).mockResolvedValue([
      { id: "glm-5", name: "GLM-5", provider: "zhipu" },
    ]);

    const params = makeTestParams();
    renderHook(() => useProxxStatusPolling(params));
    
    await act(async () => {
      await vi.runOnlyPendingTimersAsync();
    });

    expect(params.setSelectedModel).toHaveBeenCalledWith("glm-5");
  });

  it("does not change selectedModel when already set", async () => {
    vi.mocked(api.proxxHealth).mockResolvedValue({ reachable: true, configured: true, default_model: "glm-5" });
    vi.mocked(api.listProxxModels).mockResolvedValue([]);

    const params = makeTestParams();
    params.selectedModel = "gpt-4";
    renderHook(() => useProxxStatusPolling(params));
    
    await act(async () => {
      await vi.runOnlyPendingTimersAsync();
    });

    expect(params.setSelectedModel).not.toHaveBeenCalled();
  });

  it("handles proxx health errors", async () => {
    vi.mocked(api.proxxHealth).mockRejectedValue(new Error("Network error"));

    const params = makeTestParams();
    renderHook(() => useProxxStatusPolling(params));
    
    await act(async () => {
      await vi.runOnlyPendingTimersAsync();
    });

    expect(params.setProxxReachable).toHaveBeenCalledWith(false);
  });

  it("polls every 5 seconds", async () => {
    vi.mocked(api.proxxHealth).mockResolvedValue({ reachable: true, configured: true, default_model: "" });
    vi.mocked(api.listProxxModels).mockResolvedValue([]);

    const params = makeTestParams();
    renderHook(() => useProxxStatusPolling(params));
    
    // Initial poll
    await act(async () => {
      await vi.runOnlyPendingTimersAsync();
    });
    const initialCallCount = api.proxxHealth.mock.calls.length;
    expect(initialCallCount).toBeGreaterThanOrEqual(1);

    // Advance 5 seconds - this should trigger the interval callback
    vi.advanceTimersByTime(5000);
    await act(async () => {
      await vi.runOnlyPendingTimersAsync();
    });
    
    // Should have been called at least one more time
    expect(api.proxxHealth.mock.calls.length).toBeGreaterThan(initialCallCount);
  });
});

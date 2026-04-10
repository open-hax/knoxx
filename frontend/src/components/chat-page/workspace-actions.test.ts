import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { createChatWorkspaceActions } from "./workspace-actions";
import * as api from "../../lib/api";
import type { BrowseResponse, MemorySessionSummary, WorkspaceJob } from "./types";
import type { RunDetail } from "../../lib/types";

// Mock the api module
vi.mock("../../lib/api", () => ({
  getMemorySession: vi.fn(),
  listMemorySessions: vi.fn(),
}));

// Mock fetch globally
const originalFetch = global.fetch;

describe("createChatWorkspaceActions", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.mocked(api.getMemorySession).mockReset();
    vi.mocked(api.listMemorySessions).mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
    global.fetch = originalFetch;
  });

  const makeTestParams = () => ({
    currentPath: "/",
    showFiles: true,
    browseData: null as BrowseResponse | null,
    semanticQuery: "",
    setBrowseData: vi.fn(),
    setPreviewData: vi.fn(),
    setLoadingBrowse: vi.fn(),
    setLoadingPreview: vi.fn(),
    setSemanticResults: vi.fn(),
    setSemanticProjects: vi.fn(),
    setSemanticSearching: vi.fn(),
    setSyncingWorkspace: vi.fn(),
    setWorkspaceSourceId: vi.fn(),
    setWorkspaceJob: vi.fn(),
    recentSessionsRef: { current: [] as MemorySessionSummary[] },
    setRecentSessions: vi.fn(),
    setRecentSessionsHasMore: vi.fn(),
    setRecentSessionsTotal: vi.fn(),
    setLoadingRecentSessions: vi.fn(),
    setLoadingMoreRecentSessions: vi.fn(),
    setLoadingMemorySessionId: vi.fn(),
    setMessages: vi.fn(),
    setConversationId: vi.fn(),
    setLatestRun: vi.fn(),
    setRuntimeEvents: vi.fn(),
    setLiveControlText: vi.fn(),
    setIsSending: vi.fn(),
    setConsoleLines: vi.fn(),
    pendingAssistantIdRef: { current: null } as React.MutableRefObject<string | null>,
    activeRunIdRef: { current: null } as React.MutableRefObject<string | null>,
    fetchPreviewData: vi.fn().mockResolvedValue({ path: "/test.md", content: "test" }),
    loadRunDetail: vi.fn(),
    defaultSyncIntervalMinutes: 30,
    defaultFileTypes: [".md", ".txt"],
    defaultExcludePatterns: ["**/node_modules/**"],
  });

  describe("loadDirectory", () => {
    it("sets loading state", async () => {
      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({ current_path: "/", entries: [] }),
      });

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      const promise = actions.loadDirectory("/docs");
      expect(params.setLoadingBrowse).toHaveBeenCalledWith(true);
      await promise;
    });

    it("fetches browse data", async () => {
      const mockBrowseData: BrowseResponse = {
        current_path: "/docs",
        entries: [{ name: "readme.md", path: "/docs/readme.md", kind: "file" }],
      };
      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: async () => mockBrowseData,
      });

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      await actions.loadDirectory("/docs");

      expect(params.setBrowseData).toHaveBeenCalledWith(mockBrowseData);
      expect(params.setPreviewData).toHaveBeenCalledWith(null);
    });

    it("handles fetch errors", async () => {
      global.fetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
      });

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      await actions.loadDirectory("/docs");

      expect(params.setConsoleLines).toHaveBeenCalled();
    });

    it("clears loading state on error", async () => {
      global.fetch = vi.fn().mockRejectedValue(new Error("Network error"));

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      await actions.loadDirectory("/docs");

      expect(params.setLoadingBrowse).toHaveBeenCalledWith(false);
    });
  });

  describe("runSemanticSearch", () => {
    it("sets semantic searching state", async () => {
      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({ rows: [], projects: [] }),
      });

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      const promise = actions.runSemanticSearch("test query");
      expect(params.setSemanticSearching).toHaveBeenCalledWith(true);
      await promise;
    });

    it("fetches semantic results", async () => {
      const mockResults = {
        rows: [{ path: "/docs/a.md", score: 0.9, snippet: "test" }],
        projects: ["proj-1"],
      };
      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: async () => mockResults,
      });

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      await actions.runSemanticSearch("test query");

      expect(params.setSemanticResults).toHaveBeenCalledWith(mockResults.rows);
      expect(params.setSemanticProjects).toHaveBeenCalledWith(mockResults.projects);
    });

    it("handles empty query", async () => {
      global.fetch = vi.fn(); // Reset fetch mock
      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      await actions.runSemanticSearch("");

      expect(params.setSemanticResults).toHaveBeenCalledWith([]);
      expect(params.setSemanticProjects).toHaveBeenCalledWith([]);
      expect(global.fetch).not.toHaveBeenCalled();
    });

    it("clears searching state on completion", async () => {
      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({ rows: [], projects: [] }),
      });

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      await actions.runSemanticSearch("test");

      expect(params.setSemanticSearching).toHaveBeenCalledWith(false);
    });
  });

  describe("refreshRecentSessions", () => {
    it("fetches recent sessions", async () => {
      const mockSessions: MemorySessionSummary[] = [
        { session: "sess-1", message_count: 5, last_message_at: "2026-04-10T10:00:00Z" },
        { session: "sess-2", message_count: 3, last_message_at: "2026-04-09T15:00:00Z" },
      ];
      vi.mocked(api.listMemorySessions).mockResolvedValue({
        rows: mockSessions,
        total: 2,
      });

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      await actions.refreshRecentSessions();

      expect(params.setRecentSessions).toHaveBeenCalled();
      expect(params.setRecentSessionsTotal).toHaveBeenCalledWith(2);
    });

    it("sets hasMore based on total", async () => {
      vi.mocked(api.listMemorySessions).mockResolvedValue({
        rows: [{ session: "sess-1", message_count: 5, last_message_at: "2026-04-10T10:00:00Z" }],
        total: 10,
      });

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      await actions.refreshRecentSessions();

      expect(params.setRecentSessionsHasMore).toHaveBeenCalledWith(true);
    });

    it("merges with existing sessions", async () => {
      const existing: MemorySessionSummary[] = [
        { session: "sess-old", message_count: 1, last_message_at: "2026-04-01T10:00:00Z" },
      ];
      const params = makeTestParams();
      params.recentSessionsRef.current = existing;

      vi.mocked(api.listMemorySessions).mockResolvedValue({
        rows: [
          { session: "sess-1", message_count: 5, last_message_at: "2026-04-10T10:00:00Z" },
        ],
        total: 2,
      });

      const actions = createChatWorkspaceActions(params);
      await actions.refreshRecentSessions();

      // Should have merged old and new
      const call = params.setRecentSessions.mock.calls[0];
      const sessions = call[0];
      expect(sessions.some((s: MemorySessionSummary) => s.session === "sess-old")).toBe(true);
    });

    it("sets loading states", async () => {
      vi.mocked(api.listMemorySessions).mockResolvedValue({
        rows: [],
        total: 0,
      });

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      const promise = actions.refreshRecentSessions();
      expect(params.setLoadingRecentSessions).toHaveBeenCalledWith(true);
      await promise;
      expect(params.setLoadingRecentSessions).toHaveBeenCalledWith(false);
    });
  });

  describe("loadMoreRecentSessions", () => {
    it("loads additional sessions", async () => {
      const existing: MemorySessionSummary[] = [
        { session: "sess-1", message_count: 5, last_message_at: "2026-04-10T10:00:00Z" },
      ];
      const params = makeTestParams();
      params.recentSessionsRef.current = existing;

      vi.mocked(api.listMemorySessions).mockResolvedValue({
        rows: [
          { session: "sess-2", message_count: 3, last_message_at: "2026-04-09T10:00:00Z" },
        ],
        total: 2,
      });

      const actions = createChatWorkspaceActions(params);
      await actions.loadMoreRecentSessions();

      expect(api.listMemorySessions).toHaveBeenCalledWith(
        expect.objectContaining({ offset: 1 })
      );
    });

    it("sets loadingMore state", async () => {
      vi.mocked(api.listMemorySessions).mockResolvedValue({
        rows: [],
        total: 0,
      });

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      const promise = actions.loadMoreRecentSessions();
      expect(params.setLoadingMoreRecentSessions).toHaveBeenCalledWith(true);
      await promise;
      expect(params.setLoadingMoreRecentSessions).toHaveBeenCalledWith(false);
    });
  });

  describe("resumeMemorySession", () => {
    it("loads session and sets messages", async () => {
      vi.mocked(api.getMemorySession).mockResolvedValue({
        rows: [
          { id: "r1", kind: "knoxx.message", role: "user", text: "hello", created_at: "2026-04-10T10:00:00Z" },
          { id: "r2", kind: "knoxx.message", role: "assistant", text: "hi", created_at: "2026-04-10T10:01:00Z" },
        ],
      });

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      await actions.resumeMemorySession("sess-123");

      expect(params.setMessages).toHaveBeenCalled();
      expect(params.setLoadingMemorySessionId).toHaveBeenCalledWith(null);
    });

    it("sets loading state", async () => {
      vi.mocked(api.getMemorySession).mockResolvedValue({ rows: [] });

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      const promise = actions.resumeMemorySession("sess-123");
      expect(params.setLoadingMemorySessionId).toHaveBeenCalledWith("sess-123");
      await promise;
      expect(params.setLoadingMemorySessionId).toHaveBeenCalledWith(null);
    });

    it("clears live control text", async () => {
      vi.mocked(api.getMemorySession).mockResolvedValue({ rows: [] });

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      await actions.resumeMemorySession("sess-123");

      expect(params.setLiveControlText).toHaveBeenCalledWith("");
    });

    it("handles errors", async () => {
      vi.mocked(api.getMemorySession).mockRejectedValue(new Error("Session not found"));

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      await actions.resumeMemorySession("sess-123");

      expect(params.setConsoleLines).toHaveBeenCalled();
      expect(params.setLoadingMemorySessionId).toHaveBeenCalledWith(null);
    });
  });

  describe("ensureWorkspaceSync", () => {
    it("checks existing sources", async () => {
      global.fetch = vi.fn()
        .mockResolvedValueOnce({
          ok: true,
          json: async () => [{ source_id: "source-1", name: "devel workspace", config: { root_path: "/app/workspace/devel" } }],
        })
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({ job_id: "job-123" }),
        });

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      await actions.ensureWorkspaceSync();

      expect(params.setWorkspaceSourceId).toHaveBeenCalledWith("source-1");
    });

    it("creates source if none exists", async () => {
      global.fetch = vi.fn()
        .mockResolvedValueOnce({
          ok: true,
          json: async () => [],
        })
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({ source_id: "source-new", name: "devel workspace", config: { root_path: "/app/workspace/devel" } }),
        })
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({ job_id: "job-123" }),
        });

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      await actions.ensureWorkspaceSync();

      expect(params.setWorkspaceSourceId).toHaveBeenCalledWith("source-new");
    });

    it("sets syncing state", async () => {
      global.fetch = vi.fn()
        .mockResolvedValueOnce({
          ok: true,
          json: async () => [],
        })
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({ source_id: "source-new" }),
        })
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({ job_id: "job-123" }),
        });

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      const promise = actions.ensureWorkspaceSync();
      expect(params.setSyncingWorkspace).toHaveBeenCalledWith(true);
      await promise;
      expect(params.setSyncingWorkspace).toHaveBeenCalledWith(false);
    });

    it("handles errors", async () => {
      global.fetch = vi.fn().mockRejectedValue(new Error("Network error"));

      const params = makeTestParams();
      const actions = createChatWorkspaceActions(params);

      await actions.ensureWorkspaceSync();

      expect(params.setConsoleLines).toHaveBeenCalled();
      expect(params.setSyncingWorkspace).toHaveBeenCalledWith(false);
    });
  });
});

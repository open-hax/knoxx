import { describe, expect, it } from "vitest";
import { renderHook } from "@testing-library/react";
import { useChatPageDerivedState } from "./chat-page-derived";
import type { RunDetail, RunEvent } from "../../lib/types";
import type { BrowseResponse, SemanticSearchMatch, WorkspaceJob } from "./types";

function makeParams(overrides: Partial<Parameters<typeof useChatPageDerivedState>[0]> = {}) {
  return {
    browseData: null as BrowseResponse | null,
    entryFilter: "",
    semanticQuery: "",
    semanticResults: [] as SemanticSearchMatch[],
    workspaceJob: null as WorkspaceJob | null,
    latestRun: null as RunDetail | null,
    isSending: false,
    runtimeEvents: [] as RunEvent[],
    pendingAssistantId: null as string | null,
    conversationId: null as string | null,
    ...overrides,
  };
}

describe("useChatPageDerivedState", () => {
  describe("currentPath and currentParentPath", () => {
    it("returns empty path when browseData is null", () => {
      const { result } = renderHook(() => useChatPageDerivedState(makeParams()));
      expect(result.current.currentPath).toBe("");
      expect(result.current.currentParentPath).toBe("");
    });

    it("returns current_path from browseData", () => {
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            browseData: { current_path: "/docs/readme.md", entries: [] } as BrowseResponse,
          })
        )
      );
      expect(result.current.currentPath).toBe("/docs/readme.md");
      // parentPath("/docs/readme.md") = "docs" (no leading slash, no trailing slash)
      expect(result.current.currentParentPath).toBe("docs");
    });

    it("handles root path", () => {
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            browseData: { current_path: "/", entries: [] } as BrowseResponse,
          })
        )
      );
      expect(result.current.currentPath).toBe("/");
      expect(result.current.currentParentPath).toBe("");
    });
  });

  describe("filteredEntries", () => {
    it("returns empty array when browseData is null", () => {
      const { result } = renderHook(() => useChatPageDerivedState(makeParams()));
      expect(result.current.filteredEntries).toEqual([]);
    });

    it("returns all entries when entryFilter is empty", () => {
      const entries = [{ name: "a.md", path: "/a.md" }, { name: "b.md", path: "/b.md" }];
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            browseData: { current_path: "/", entries } as BrowseResponse,
          })
        )
      );
      expect(result.current.filteredEntries).toEqual(entries);
    });

    it("filters entries by name (case-insensitive)", () => {
      const entries = [
        { name: "README.md", path: "/README.md" },
        { name: "changelog.txt", path: "/changelog.txt" },
        { name: "notes.org", path: "/notes.org" },
      ];
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            browseData: { current_path: "/", entries } as BrowseResponse,
            entryFilter: "readme",
          })
        )
      );
      expect(result.current.filteredEntries.length).toBe(1);
      expect(result.current.filteredEntries[0].name).toBe("README.md");
    });

    it("filters entries by path (case-insensitive)", () => {
      const entries = [
        { name: "a.md", path: "/docs/README.md" },
        { name: "b.md", path: "/src/index.ts" },
      ];
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            browseData: { current_path: "/", entries } as BrowseResponse,
            entryFilter: "docs",
          })
        )
      );
      expect(result.current.filteredEntries.length).toBe(1);
      expect(result.current.filteredEntries[0].path).toBe("/docs/README.md");
    });
  });

  describe("semanticMode and activeEntryCount", () => {
    it("is semantic mode when semanticQuery has content", () => {
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            semanticQuery: "test query",
          })
        )
      );
      expect(result.current.semanticMode).toBe(true);
    });

    it("is not semantic mode when semanticQuery is empty", () => {
      const { result } = renderHook(() => useChatPageDerivedState(makeParams()));
      expect(result.current.semanticMode).toBe(false);
    });

    it("activeEntryCount uses semanticResults when in semantic mode", () => {
      const semanticResults = [{ path: "/a.md", score: 0.9, snippet: "..." }];
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            semanticQuery: "test",
            semanticResults,
            browseData: { current_path: "/", entries: [{ name: "b.md", path: "/b.md" }] } as BrowseResponse,
          })
        )
      );
      expect(result.current.semanticMode).toBe(true);
      expect(result.current.activeEntryCount).toBe(1);
    });

    it("activeEntryCount uses filteredEntries when not in semantic mode", () => {
      const entries = [{ name: "a.md", path: "/a.md" }, { name: "b.md", path: "/b.md" }];
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            browseData: { current_path: "/", entries } as BrowseResponse,
          })
        )
      );
      expect(result.current.semanticMode).toBe(false);
      expect(result.current.activeEntryCount).toBe(2);
    });
  });

  describe("workspaceProgressPercent", () => {
    it("returns 0 when no workspaceJob", () => {
      const { result } = renderHook(() => useChatPageDerivedState(makeParams()));
      expect(result.current.workspaceProgressPercent).toBe(0);
    });

    it("returns 0 when total_files is 0", () => {
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            workspaceJob: { total_files: 0, processed_files: 0, failed_files: 0 } as WorkspaceJob,
          })
        )
      );
      expect(result.current.workspaceProgressPercent).toBe(0);
    });

    it("calculates percentage correctly", () => {
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            workspaceJob: { total_files: 100, processed_files: 75, failed_files: 5 } as WorkspaceJob,
          })
        )
      );
      // (75 + 5) / 100 * 100 = 80%
      expect(result.current.workspaceProgressPercent).toBe(80);
    });

    it("clamps to 100% max", () => {
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            workspaceJob: { total_files: 100, processed_files: 110, failed_files: 0 } as WorkspaceJob,
          })
        )
      );
      expect(result.current.workspaceProgressPercent).toBe(100);
    });
  });

  describe("latestToolReceipts", () => {
    it("returns empty array when no latestRun", () => {
      const { result } = renderHook(() => useChatPageDerivedState(makeParams()));
      expect(result.current.latestToolReceipts).toEqual([]);
    });

    it("returns tool_receipts from latestRun", () => {
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            latestRun: { tool_receipts: [{ id: "r1", status: "done" }] } as RunDetail,
          })
        )
      );
      expect(result.current.latestToolReceipts.length).toBe(1);
    });
  });

  describe("liveToolReceipts", () => {
    it("returns empty when not sending", () => {
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            isSending: false,
            latestRun: { tool_receipts: [{ id: "r1" }] } as RunDetail,
            pendingAssistantId: "msg-123",
          })
        )
      );
      expect(result.current.liveToolReceipts).toEqual([]);
    });

    it("returns empty when no pendingAssistantId", () => {
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            isSending: true,
            latestRun: { tool_receipts: [{ id: "r1" }] } as RunDetail,
            pendingAssistantId: null,
          })
        )
      );
      expect(result.current.liveToolReceipts).toEqual([]);
    });

    it("returns latestToolReceipts when sending and has pendingAssistantId", () => {
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            isSending: true,
            latestRun: { tool_receipts: [{ id: "r1", status: "running" }] } as RunDetail,
            pendingAssistantId: "msg-123",
          })
        )
      );
      expect(result.current.liveToolReceipts.length).toBe(1);
    });
  });

  describe("liveToolEvents", () => {
    it("returns empty when not sending", () => {
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            isSending: false,
            runtimeEvents: [{ type: "tool_start" }] as RunEvent[],
          })
        )
      );
      expect(result.current.liveToolEvents).toEqual([]);
    });

    it("filters tool events when sending", () => {
      const runtimeEvents = [
        { type: "tool_start" },
        { type: "tool_update" },
        { type: "tool_end" },
        { type: "user_message" },
        { type: "assistant_message" },
      ] as RunEvent[];
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            isSending: true,
            runtimeEvents,
          })
        )
      );
      expect(result.current.liveToolEvents.length).toBe(3);
    });
  });

  describe("liveControlEnabled", () => {
    it("is false when not sending", () => {
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            isSending: false,
            conversationId: "conv-1",
            runtimeEvents: [{ type: "run_started" }] as RunEvent[],
          })
        )
      );
      expect(result.current.liveControlEnabled).toBe(false);
    });

    it("is false when no conversationId", () => {
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            isSending: true,
            conversationId: null,
            runtimeEvents: [{ type: "run_started" }] as RunEvent[],
          })
        )
      );
      expect(result.current.liveControlEnabled).toBe(false);
    });

    it("is false when no matching events", () => {
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            isSending: true,
            conversationId: "conv-1",
            runtimeEvents: [{ type: "user_message" }] as RunEvent[],
          })
        )
      );
      expect(result.current.liveControlEnabled).toBe(false);
    });

    it("is true when sending, has conversationId, and matching event", () => {
      const { result } = renderHook(() =>
        useChatPageDerivedState(
          makeParams({
            isSending: true,
            conversationId: "conv-1",
            runtimeEvents: [{ type: "run_started" }] as RunEvent[],
          })
        )
      );
      expect(result.current.liveControlEnabled).toBe(true);
    });
  });

  describe("surface styling", () => {
    it("returns expected CSS variables", () => {
      const { result } = renderHook(() => useChatPageDerivedState(makeParams()));
      expect(result.current.assistantSurfaceBackground).toBe("var(--token-colors-background-surface)");
      expect(result.current.assistantSurfaceBorder).toBe("var(--token-colors-border-default)");
      expect(result.current.assistantSurfaceText).toBe("var(--token-colors-text-default)");
    });
  });
});

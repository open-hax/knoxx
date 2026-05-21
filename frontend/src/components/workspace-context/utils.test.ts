import { describe, expect, it } from "vitest";
import type { ChatTraceBlock, MemorySessionRow } from "../../lib/types";
import {
  appendTraceTextDelta,
  contextPath,
  memoryRowsToMessages,
  selectWorkspaceJob,
  sourceUrlToPath,
} from "./utils";
import type { WorkspaceJob } from "./types";

describe("workspace-context shared utilities", () => {
  it("normalizes context paths and source URLs for both chat and context surfaces", () => {
    expect(contextPath({ id: "row-1", source: "fallback", source_path: "docs/guide.md" })).toBe("docs/guide.md");
    expect(sourceUrlToPath("/workspace/docs/guide.md?tab=preview#intro")).toBe("workspace/docs/guide.md");
  });

  it("selects active workspace jobs without mutating the caller's list", () => {
    const jobs: WorkspaceJob[] = [
      job("old-completed", "completed", "2026-05-01T00:00:00.000Z"),
      job("new-pending", "pending", "2026-05-03T00:00:00.000Z"),
      job("new-completed", "completed", "2026-05-04T00:00:00.000Z"),
    ];
    const orderBefore = jobs.map((item) => item.job_id);

    expect(selectWorkspaceJob(jobs)?.job_id).toBe("new-pending");
    expect(jobs.map((item) => item.job_id)).toEqual(orderBefore);
  });

  it("preserves assistant trace fallback rows when converting memory rows to chat messages", () => {
    const rows: MemorySessionRow[] = [
      {
        id: "row-user",
        kind: "knoxx.message",
        role: "user",
        text: "testing?",
        session: "pi:test",
        extra: { run_id: "run-1" },
      },
      {
        id: "row-assistant",
        kind: "knoxx.message",
        role: "assistant",
        text: "Final answer",
        session: "pi:test",
        extra: { run_id: "run-1" },
      },
      {
        id: "row-reasoning",
        kind: "knoxx.reasoning",
        role: "system",
        text: "Reasoning summary",
        session: "pi:test",
        extra: { run_id: "run-1" },
      },
    ];

    expect(memoryRowsToMessages(rows)).toEqual([
      {
        id: "row-user",
        role: "user",
        content: "testing?",
        model: null,
        runId: "run-1",
        status: undefined,
        traceBlocks: undefined,
      },
      {
        id: "row-assistant",
        role: "assistant",
        content: "Final answer",
        model: null,
        runId: "run-1",
        status: "done",
        traceBlocks: [
          { id: "row-reasoning", kind: "reasoning", status: "done", at: undefined, content: "Reasoning summary" },
        ],
      },
    ]);
  });

  it("collapses overlapping streaming trace deltas", () => {
    const blocks: ChatTraceBlock[] = [{ id: "reasoning-1", kind: "reasoning", status: "streaming", content: "The answer" }];

    expect(appendTraceTextDelta(blocks, "reasoning", "answer is stable.")).toEqual([
      { id: "reasoning-1", kind: "reasoning", status: "streaming", content: "The answer is stable.", at: undefined },
    ]);
  });
});

function job(job_id: string, status: string, created_at: string): WorkspaceJob {
  return {
    job_id,
    status,
    created_at,
    total_files: 0,
    processed_files: 0,
    failed_files: 0,
    skipped_files: 0,
    chunks_created: 0,
  };
}

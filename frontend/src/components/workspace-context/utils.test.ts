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

  it("surfaces a failed run with no assistant answer as a structured assistant turn", () => {
    // A failed/aborted run persists the user message + a knoxx.run summary +
    // reasoning/tool_receipt rows, but NO assistant knoxx.message. Previously the
    // reasoning/tool timeline was dropped and the session collapsed to just the
    // user message. It must now reconstruct a structured assistant turn so the
    // thinking + tool calls stay visible for auditing.
    const rows: MemorySessionRow[] = [
      {
        id: "trigger-x:user",
        kind: "knoxx.message",
        role: "user",
        text: "hey frankie",
        session: "pi:test",
        extra: { run_id: "trigger-x" },
      },
      {
        id: "trigger-x:tool:call_0",
        kind: "knoxx.tool_receipt",
        role: "system",
        text: "discord_read result",
        session: "pi:test",
        extra: { run_id: "trigger-x", receipt: { id: "call_0", tool_name: "discord_read", status: "completed" } },
      },
      {
        id: "trigger-x:summary",
        kind: "knoxx.run",
        role: "system",
        text: "Run trigger-x · status failed",
        session: "pi:test",
        extra: { run_id: "trigger-x", status: "failed" },
      },
    ];

    const messages = memoryRowsToMessages(rows);
    expect(messages.map((m) => m.role)).toEqual(["user", "assistant"]);
    const assistant = messages[1];
    expect(assistant.status).toBe("error");
    expect(assistant.runId).toBe("trigger-x");
    expect(assistant.traceBlocks).toEqual([
      {
        id: "call_0",
        kind: "tool_call",
        status: "done",
        at: undefined,
        toolName: "discord_read",
        toolCallId: "call_0",
        inputPreview: undefined,
        outputPreview: "discord_read result",
        updates: undefined,
        isError: undefined,
      },
    ]);
  });

  it("does not synthesize a run turn when the assistant answer is present", () => {
    const rows: MemorySessionRow[] = [
      { id: "r:assistant", kind: "knoxx.message", role: "assistant", text: "done", session: "s", extra: { run_id: "r" } },
      { id: "r:summary", kind: "knoxx.run", role: "system", text: "Run r", session: "s", extra: { run_id: "r", status: "completed" } },
    ];
    // Only the real assistant message; the run summary must not become a duplicate turn.
    expect(memoryRowsToMessages(rows).map((m) => m.id)).toEqual(["r:assistant"]);
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

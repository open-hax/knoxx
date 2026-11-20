import { describe, expect, it } from "vitest";
import type { ChatTraceBlock, MemorySessionRow } from "../../lib/types";
import {
  appendTraceTextDelta,
  replaceTraceText,
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

  it("preserves repeated characters and whitespace in literal streaming trace deltas", () => {
    const blocks: ChatTraceBlock[] = [{ id: "reasoning-1", kind: "reasoning", status: "streaming", content: "Knox" }];

    expect(appendTraceTextDelta(appendTraceTextDelta(blocks, "reasoning", "x\n"), "reasoning", "\n")).toEqual([
      { id: "reasoning-1", kind: "reasoning", status: "streaming", content: "Knoxx\n\n", at: undefined },
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


describe("explicit trace text corrections", () => {
  const prefix = "🌱 prior. ";
  const blocks: ChatTraceBlock[] = [
    { id: "prior", kind: "agent_message", status: "done", content: prefix },
    { id: "reason", kind: "reasoning", status: "streaming", content: "Reasoning" },
    { id: "tool", kind: "tool_call", status: "done", toolCallId: "tool-1" },
    { id: "current", kind: "agent_message", status: "streaming", content: "Draft" },
    { id: "later-tool", kind: "tool_call", status: "done", toolCallId: "tool-2" },
    { id: "obsolete", kind: "agent_message", status: "streaming", content: " stale" },
  ];

  it("preserves earlier provider text, tools, and reasoning while replacing only the current text tail", () => {
    const corrected = replaceTraceText(blocks, "agent_message", prefix + "Final", prefix.length);
    expect(corrected).toEqual([
      blocks[0], blocks[1], blocks[2], { ...blocks[3], content: "Final" }, blocks[4],
    ]);
    expect(replaceTraceText(corrected, "agent_message", prefix + "Final", prefix.length)).toEqual(corrected);
    expect(blocks[3].content).toBe("Draft");
  });

  it("preserves a prefix inside a crossing block and removes only matching-kind later blocks", () => {
    expect(replaceTraceText([{ ...blocks[0], content: "PrefixDraft" }, blocks[2], blocks[5]], "agent_message", "PrefixFinal", 6)).toEqual([
      { ...blocks[0], content: "PrefixFinal", status: "streaming" }, blocks[2],
    ]);
  });

  it("removes an empty corrected suffix without removing tools or the other text kind", () => {
    expect(replaceTraceText(blocks, "agent_message", prefix, prefix.length)).toEqual(blocks.slice(0, 3).concat(blocks[4]));
    expect(replaceTraceText(blocks, "reasoning", "", 0)).toEqual(blocks.filter((block) => block.kind !== "reasoning"));
  });

  it("preserves a completed prefix when an empty correction cuts a crossing block", () => {
    const prefixBlock: ChatTraceBlock = { id: "prior", kind: "agent_message", status: "done", content: "PrefixDraft" };
    expect(replaceTraceText([prefixBlock, blocks[2], blocks[5]], "agent_message", "Prefix", 6)).toEqual([
      { ...prefixBlock, content: "Prefix" }, blocks[2],
    ]);
  });

  it("retains an entire prior block when the corrected current message has no existing trace block", () => {
    const corrected = replaceTraceText(blocks.slice(0, 3), "agent_message", prefix + "Final", prefix.length);
    expect(corrected.slice(0, 3)).toEqual(blocks.slice(0, 3));
    expect(corrected[3]).toMatchObject({ kind: "agent_message", status: "streaming", content: "Final" });
  });

  it("allocates distinct text identities after a correction removes interleaved blocks", () => {
    const interleaved: ChatTraceBlock[] = [
      { id: "reasoning:0", kind: "reasoning", status: "streaming", content: "First thought" },
      { id: "agent_message:1", kind: "agent_message", status: "done", content: "Prior" },
      { id: "reasoning:2", kind: "reasoning", status: "streaming", content: "Second thought" },
      { id: "agent_message:3", kind: "agent_message", status: "done", content: "Kept" },
      { id: "tool:t", kind: "tool_call", status: "done", toolCallId: "t" },
    ];
    const cleared = replaceTraceText(interleaved, "reasoning", "", 0);
    const appended = appendTraceTextDelta(cleared, "agent_message", "Later");
    expect(appended.slice(0, 3)).toEqual([interleaved[1], interleaved[3], interleaved[4]]);
    expect(appended[3]).toMatchObject({ kind: "agent_message", content: "Later" });
    expect(new Set(appended.map(({ id }) => id)).size).toBe(appended.length);
    const fallback = replaceTraceText(cleared, "reasoning", "Fresh thought", 0);
    expect(fallback.slice(0, 3)).toEqual(cleared);
    expect(new Set(fallback.map(({ id }) => id)).size).toBe(fallback.length);
  });

  it.each(["agent_message", "reasoning"] as const)("recovers the entire %s snapshot when no channel prefix was retained", (kind) => {
    const untouched = blocks.filter((block) => block.kind !== kind);
    const snapshot = prefix + "Corrected";
    const corrected = replaceTraceText(untouched, kind, snapshot, prefix.length);
    expect(corrected.slice(0, untouched.length)).toEqual(untouched);
    expect(corrected.filter((block) => block.kind === kind).map((block) => block.content ?? "").join("")).toBe(snapshot);
    expect(replaceTraceText(corrected, kind, snapshot, prefix.length)).toEqual(corrected);
  });

  it.each(["🌱 pri", "Stale prior. "])("converges a partial or stale retained prefix %j to the complete snapshot", (retained) => {
    const original = [{ ...blocks[0], content: retained }, blocks[2], blocks[3], blocks[1], blocks[4], blocks[5]];
    const snapshot = prefix + "Corrected";
    const corrected = replaceTraceText(original, "agent_message", snapshot, prefix.length);
    expect(corrected.filter((block) => block.kind === "agent_message").map((block) => block.content ?? "").join("")).toBe(snapshot);
    expect(corrected.filter((block) => block.kind !== "agent_message")).toEqual([blocks[2], blocks[1], blocks[4]]);
    expect(replaceTraceText(corrected, "agent_message", snapshot, prefix.length)).toEqual(corrected);
    expect(original[0].content).toBe(retained);
  });
});

import { describe, expect, it } from "vitest";
import { applyToolTraceEvent, memoryRowsToMessages, novelAppendedText, rewindTranscriptTurns } from "./utils";
import type { ChatMessage, MemorySessionRow } from "../../lib/types";

function msg(id: string, role: ChatMessage["role"], content: string): ChatMessage {
  return { id, role, content };
}

describe("novelAppendedText", () => {
  it("collapses repeated cumulative assistant text into only the novel suffix", () => {
    expect(novelAppendedText("", "hello")).toBe("hello");
    expect(novelAppendedText("hello", "hello")).toBe("");
    expect(novelAppendedText("hello", "hello world")).toBe(" world");
    expect(novelAppendedText("hello world", "world")).toBe("");
    expect(novelAppendedText("hello world", "world again")).toBe(" again");
    expect(novelAppendedText("The", "TheThe model should reason once.")).toBe(" model should reason once.");
    expect(novelAppendedText("ha", "haha")).toBe("ha");
  });
});

describe("applyToolTraceEvent", () => {
  it("maps tool_start + tool_end events into a single tool_call trace block", () => {
    const started = applyToolTraceEvent([], {
      type: "tool_start",
      tool_name: "read",
      tool_call_id: "call-1",
      preview: "```yaml\npath: docs/guide.md\n```",
      at: "2026-04-22T17:00:00.000Z",
    });

    const finished = applyToolTraceEvent(started, {
      type: "tool_end",
      tool_name: "read",
      tool_call_id: "call-1",
      preview: "done",
      is_error: false,
      at: "2026-04-22T17:00:02.000Z",
    });

    expect(finished).toEqual([
      {
        id: "tool:call-1",
        kind: "tool_call",
        toolName: "read",
        toolCallId: "call-1",
        inputPreview: "```yaml\npath: docs/guide.md\n```",
        status: "done",
        outputPreview: "done",
        isError: false,
        at: "2026-04-22T17:00:02.000Z",
        updates: [],
      },
    ]);
  });
});

describe("memoryRowsToMessages", () => {
  it("preserves persisted assistant trace blocks from session memory", () => {
    const rows: MemorySessionRow[] = [{
      id: "row-1",
      kind: "knoxx.message",
      role: "assistant",
      text: "Final answer",
      session: "pi:test",
      extra: {
        run_id: "run-1",
        trace_blocks: [
          { id: "reasoning-1", kind: "reasoning", status: "done", content: "Reasoning summary" },
          { id: "tool-1", kind: "tool_call", status: "done", toolName: "read", outputPreview: "Useful result" },
        ],
      },
    }];

    const messages = memoryRowsToMessages(rows);

    expect(messages).toHaveLength(1);
    expect(messages[0].content).toBe("Final answer");
    expect(messages[0].runId).toBe("run-1");
    expect(messages[0].traceBlocks).toEqual([
      { id: "reasoning-1", kind: "reasoning", status: "done", content: "Reasoning summary", at: undefined, toolName: undefined, toolCallId: undefined, inputPreview: undefined, outputPreview: undefined, updates: undefined, isError: undefined },
      { id: "tool-1", kind: "tool_call", status: "done", content: undefined, at: undefined, toolName: "read", toolCallId: undefined, inputPreview: undefined, outputPreview: "Useful result", updates: undefined, isError: undefined },
    ]);
  });

  it("ignores run summaries and reasoning rows when normalized chat messages are present", () => {
    const rows: MemorySessionRow[] = [
      {
        id: "row-user",
        kind: "knoxx.message",
        role: "user",
        text: "testing?",
        session: "pi:test",
        extra: { run_id: "run-2" },
      },
      {
        id: "row-run",
        kind: "knoxx.run",
        role: "system",
        text: "Run run-2 · completed Answer: Final answer",
        session: "pi:test",
        extra: { run_id: "run-2" },
      },
      {
        id: "row-assistant",
        kind: "knoxx.message",
        role: "assistant",
        text: "Final answer",
        session: "pi:test",
        extra: { run_id: "run-2" },
      },
      {
        id: "row-reasoning",
        kind: "knoxx.reasoning",
        role: "system",
        text: "Reasoning summary",
        session: "pi:test",
        extra: { run_id: "run-2" },
      },
    ];

    const messages = memoryRowsToMessages(rows);

    expect(messages).toHaveLength(2);
    expect(messages.map((message) => message.role)).toEqual(["user", "assistant"]);
    expect(messages[1].content).toBe("Final answer");
    expect(messages[1].traceBlocks).toEqual([
      { id: "row-reasoning", kind: "reasoning", status: "done", at: undefined, content: "Reasoning summary" },
    ]);
  });
});

describe("rewindTranscriptTurns", () => {
  it("drops the latest user turn and everything after it", () => {
    expect(rewindTranscriptTurns([
      msg("system", "system", "seed"),
      msg("u1", "user", "first"),
      msg("a1", "assistant", "first answer"),
      msg("u2", "user", "second"),
      msg("a2", "assistant", "second answer"),
      msg("s2", "system", "follow-up queued"),
    ])).toEqual([
      msg("system", "system", "seed"),
      msg("u1", "user", "first"),
      msg("a1", "assistant", "first answer"),
    ]);
  });

  it("can rewind multiple user turns", () => {
    expect(rewindTranscriptTurns([
      msg("system", "system", "seed"),
      msg("u1", "user", "first"),
      msg("a1", "assistant", "first answer"),
      msg("u2", "user", "second"),
      msg("a2", "assistant", "second answer"),
    ], 2)).toEqual([
      msg("system", "system", "seed"),
    ]);
  });
});

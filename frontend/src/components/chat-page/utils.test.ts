import { describe, expect, it } from "vitest";
import { memoryRowsToMessages } from "./utils";
import type { MemorySessionRow } from "../../lib/types";

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
});

import { describe, expect, it } from "vitest";
import { extractToolCallBlocks, hasVisibleToolCalls } from "./ChatMessageList";
import type { ChatTraceBlock } from "../../lib/types";

function makeBlock(overrides: Partial<ChatTraceBlock> & { kind: ChatTraceBlock["kind"] }): ChatTraceBlock {
  return {
    id: `block-${Math.random().toString(36).slice(2)}`,
    status: "done",
    content: "",
    ...overrides,
  };
}

describe("extractToolCallBlocks", () => {
  it("returns empty array when no blocks provided", () => {
    expect(extractToolCallBlocks([])).toEqual([]);
  });

  it("returns empty array when blocks have no tool_call entries", () => {
    const blocks = [
      makeBlock({ kind: "agent_message", content: "hello" }),
      makeBlock({ kind: "reasoning", content: "thinking..." }),
    ];
    expect(extractToolCallBlocks(blocks)).toEqual([]);
  });

  it("filters to only tool_call blocks", () => {
    const toolBlock = makeBlock({
      kind: "tool_call",
      toolName: "graph_query",
      toolCallId: "tc-1",
      inputPreview: '{"query":"test"}',
      outputPreview: "results here",
    });
    const blocks = [
      makeBlock({ kind: "agent_message", content: "answer" }),
      toolBlock,
      makeBlock({ kind: "reasoning", content: "hmm" }),
      makeBlock({
        kind: "tool_call",
        toolName: "memory_search",
        toolCallId: "tc-2",
      }),
    ];
    const result = extractToolCallBlocks(blocks);
    expect(result).toHaveLength(2);
    expect(result[0].toolName).toBe("graph_query");
    expect(result[1].toolName).toBe("memory_search");
  });

  it("preserves all tool_call block fields", () => {
    const toolBlock = makeBlock({
      kind: "tool_call",
      toolName: "graph_query",
      toolCallId: "tc-1",
      status: "running",
      inputPreview: '{"q":"knoxx"}',
      outputPreview: undefined,
      updates: ["step 1"],
      isError: false,
    });
    const [result] = extractToolCallBlocks([toolBlock]);
    expect(result.id).toBe(toolBlock.id);
    expect(result.kind).toBe("tool_call");
    expect(result.toolName).toBe("graph_query");
    expect(result.toolCallId).toBe("tc-1");
    expect(result.status).toBe("running");
    expect(result.inputPreview).toBe('{"q":"knoxx"}');
    expect(result.outputPreview).toBeUndefined();
    expect(result.updates).toEqual(["step 1"]);
    expect(result.isError).toBe(false);
  });

  it("returns all blocks when every block is a tool_call", () => {
    const blocks = [
      makeBlock({ kind: "tool_call", toolName: "a" }),
      makeBlock({ kind: "tool_call", toolName: "b" }),
      makeBlock({ kind: "tool_call", toolName: "c" }),
    ];
    expect(extractToolCallBlocks(blocks)).toHaveLength(3);
  });
});

describe("hasVisibleToolCalls", () => {
  it("returns false for empty array", () => {
    expect(hasVisibleToolCalls([])).toBe(false);
  });

  it("returns false when only agent_message blocks exist", () => {
    const blocks = [
      makeBlock({ kind: "agent_message", content: "hello" }),
      makeBlock({ kind: "agent_message", content: "world" }),
    ];
    expect(hasVisibleToolCalls(blocks)).toBe(false);
  });

  it("returns false when only reasoning blocks exist", () => {
    const blocks = [
      makeBlock({ kind: "reasoning", content: "thinking..." }),
    ];
    expect(hasVisibleToolCalls(blocks)).toBe(false);
  });

  it("returns true when at least one tool_call block exists", () => {
    const blocks = [
      makeBlock({ kind: "agent_message", content: "answer" }),
      makeBlock({ kind: "tool_call", toolName: "graph_query" }),
      makeBlock({ kind: "reasoning", content: "hmm" }),
    ];
    expect(hasVisibleToolCalls(blocks)).toBe(true);
  });

  it("returns true for mixed blocks with tool_call last", () => {
    const blocks = [
      makeBlock({ kind: "reasoning", content: "first" }),
      makeBlock({ kind: "agent_message", content: "then" }),
      makeBlock({ kind: "tool_call", toolName: "memory_search" }),
    ];
    expect(hasVisibleToolCalls(blocks)).toBe(true);
  });

  it("returns true even for failed/error tool_call blocks", () => {
    const blocks = [
      makeBlock({
        kind: "tool_call",
        toolName: "broken_tool",
        status: "error",
        isError: true,
      }),
    ];
    expect(hasVisibleToolCalls(blocks)).toBe(true);
  });

  it("returns false for streaming agent_message with no tools", () => {
    const blocks = [
      makeBlock({ kind: "agent_message", status: "streaming", content: "still..." }),
    ];
    expect(hasVisibleToolCalls(blocks)).toBe(false);
  });
});

describe("unified message card contract", () => {
  // These tests document the invariant that both live (traceBlocks) and resumed
  // (no traceBlocks) assistant messages must render the same dark full-width card.
  // The rendering itself requires jsdom; these tests verify the data-level
  // contracts that the rendering depends on.

  it("live session with tool calls produces extractable tool blocks for collapsible section", () => {
    const liveTraceBlocks: ChatTraceBlock[] = [
      makeBlock({ kind: "reasoning", content: "let me check" }),
      makeBlock({
        kind: "tool_call",
        toolName: "graph_query",
        toolCallId: "tc-1",
        status: "completed",
        inputPreview: '{"query":"test"}',
        result_preview: "5 nodes found",
      }),
      makeBlock({ kind: "agent_message", content: "Based on the graph..." }),
      makeBlock({
        kind: "tool_call",
        toolName: "memory_search",
        toolCallId: "tc-2",
        status: "running",
      }),
      makeBlock({ kind: "agent_message", content: "And from memory..." }),
    ];

    // Tool calls are extracted for the collapsible <details> section
    const toolCalls = extractToolCallBlocks(liveTraceBlocks);
    expect(toolCalls).toHaveLength(2);
    expect(toolCalls.map((t) => t.toolName)).toEqual(["graph_query", "memory_search"]);

    // hasVisibleToolCalls gates the collapsible section rendering
    expect(hasVisibleToolCalls(liveTraceBlocks)).toBe(true);
  });

  it("resumed session (no traceBlocks) produces no tool call section", () => {
    // Resumed sessions come from memoryRowsToMessages() which does not set traceBlocks
    const resumedTraceBlocks: ChatTraceBlock[] = [];

    expect(extractToolCallBlocks(resumedTraceBlocks)).toEqual([]);
    expect(hasVisibleToolCalls(resumedTraceBlocks)).toBe(false);
  });

  it("live session without tools skips collapsible section entirely", () => {
    const noToolBlocks: ChatTraceBlock[] = [
      makeBlock({ kind: "reasoning", content: "thinking..." }),
      makeBlock({ kind: "agent_message", content: "The answer is 42." }),
    ];

    expect(hasVisibleToolCalls(noToolBlocks)).toBe(false);
    expect(extractToolCallBlocks(noToolBlocks)).toEqual([]);
  });

  it("agent_message and reasoning blocks are never treated as tool calls", () => {
    const kinds: ChatTraceBlock["kind"][] = ["agent_message", "reasoning"];
    for (const kind of kinds) {
      const blocks = [makeBlock({ kind, content: "data" })];
      expect(hasVisibleToolCalls(blocks), `kind=${kind} should not be a tool call`).toBe(false);
    }
  });
});

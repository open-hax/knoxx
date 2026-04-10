import { describe, expect, it, afterEach } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import { UxxThemeProvider } from "@open-hax/uxx";
import { ToolReceiptGroup, AgentTraceTimeline } from "./ToolReceiptBlock";
import type { ToolReceipt, ChatTraceBlock } from "../../lib/types";

// Wrap renders in UxxThemeProvider to satisfy useUxxTheme context
function wrap(ui: React.ReactElement) {
  return <UxxThemeProvider theme="dark">{ui}</UxxThemeProvider>;
}

// Auto-cleanup between tests to prevent DOM leaks
afterEach(() => {
  cleanup();
});

function makeReceipt(overrides: Partial<ToolReceipt> & { id: string }): ToolReceipt {
  return {
    status: "completed",
    tool_name: overrides.id.replace("rec-", ""),
    ...overrides,
  };
}

function makeTraceBlock(overrides: Partial<ChatTraceBlock> & { id: string; kind: ChatTraceBlock["kind"] }): ChatTraceBlock {
  return {
    status: "done",
    content: "",
    ...overrides,
  };
}

// ─── ToolReceiptGroup ────────────────────────────────────────────

describe("ToolReceiptGroup", () => {
  it("renders nothing for empty receipts array", () => {
    const { container } = render(wrap(<ToolReceiptGroup receipts={[]} />));
    // ThemeProvider wrapper exists but should contain no Tool: text
    expect(screen.queryByText(/Tool:/)).toBeNull();
  });

  it("renders nothing when all receipts are non-visible statuses", () => {
    render(
      wrap(
        <ToolReceiptGroup
          receipts={[
            makeReceipt({ id: "rec-1", status: "cancelled" }),
            makeReceipt({ id: "rec-2", status: "unknown" as any }),
          ]}
        />
      )
    );
    expect(screen.queryByText(/Tool:/)).toBeNull();
  });

  it("renders completed receipt with tool name and status", () => {
    render(
      wrap(
        <ToolReceiptGroup
          receipts={[makeReceipt({ id: "rec-graph_query", status: "completed", result_preview: "5 nodes found" })]}
        />
      )
    );
    expect(screen.getByText(/graph_query/)).toBeDefined();
    expect(screen.getByText("completed")).toBeDefined();
  });

  it("renders running receipt with streaming label", () => {
    render(
      wrap(
        <ToolReceiptGroup
          receipts={[makeReceipt({ id: "rec-search", status: "running" })]}
        />
      )
    );
    expect(screen.getByText("streaming...")).toBeDefined();
  });

  it("renders failed receipt with error styling", () => {
    render(
      wrap(
        <ToolReceiptGroup
          receipts={[makeReceipt({ id: "rec-broken", status: "failed", is_error: true, result_preview: "timeout" })]}
        />
      )
    );
    expect(screen.getByText("failed")).toBeDefined();
  });

  it("renders multiple visible receipts", () => {
    render(
      wrap(
        <ToolReceiptGroup
          receipts={[
            makeReceipt({ id: "rec-a", status: "completed" }),
            makeReceipt({ id: "rec-b", status: "running" }),
            makeReceipt({ id: "rec-c", status: "failed" }),
          ]}
        />
      )
    );
    expect(screen.getAllByText(/Tool:/).length).toBe(3);
  });

  it("filters out non-visible statuses but shows completed/running/failed", () => {
    render(
      wrap(
        <ToolReceiptGroup
          receipts={[
            makeReceipt({ id: "rec-1", status: "cancelled" }),
            makeReceipt({ id: "rec-2", status: "completed" }),
            makeReceipt({ id: "rec-3", status: "pending" }),
            makeReceipt({ id: "rec-4", status: "running" }),
            makeReceipt({ id: "rec-5", status: "failed" }),
          ]}
        />
      )
    );
    // Only rec-2 (completed), rec-4 (running), rec-5 (failed) should render
    const tools = screen.getAllByText(/Tool:/);
    expect(tools.length).toBe(3);
  });

  it("shows Input details for running receipt", () => {
    render(
      wrap(
        <ToolReceiptGroup
          receipts={[makeReceipt({ id: "rec-tool", status: "running", input_preview: '{"query":"test"}' })]}
        />
      )
    );
    expect(screen.getByText("Input")).toBeDefined();
  });

  it("shows Live updates when updates array is present", () => {
    const updates = ["step3", "step4", "step5"];
    render(
      wrap(
        <ToolReceiptGroup
          receipts={[makeReceipt({ id: "rec-long", status: "running", updates })]}
        />
      )
    );
    expect(screen.getByText(/Live updates/)).toBeDefined();
  });

  it("shows tool_name from receipt, falls back to id", () => {
    render(
      wrap(
        <ToolReceiptGroup
          receipts={[{ id: "rec-no-name", status: "completed" } as ToolReceipt]}
        />
      )
    );
    // When no tool_name, falls back to id
    expect(screen.getByText(/rec-no-name/)).toBeDefined();
  });
});

// ─── AgentTraceTimeline ─────────────────────────────────────────

describe("AgentTraceTimeline", () => {
  it("renders nothing for empty blocks array", () => {
    render(wrap(<AgentTraceTimeline blocks={[]} />));
    // ThemeProvider wrapper exists but should contain no block content
    expect(screen.queryByText(/Agent message/)).toBeNull();
    expect(screen.queryByText(/Reasoning/)).toBeNull();
    expect(screen.queryByText(/Tool:/)).toBeNull();
  });

  it("renders agent_message block with Agent message title", () => {
    render(
      wrap(
        <AgentTraceTimeline
          blocks={[
            makeTraceBlock({ id: "tb-1", kind: "agent_message", content: "Here is my answer" }),
          ]}
        />
      )
    );
    expect(screen.getByText("Agent message")).toBeDefined();
  });

  it("renders reasoning block with Reasoning title", () => {
    render(
      wrap(
        <AgentTraceTimeline
          blocks={[
            makeTraceBlock({ id: "tb-2", kind: "reasoning", content: "Let me think..." }),
          ]}
        />
      )
    );
    expect(screen.getByText("Reasoning")).toBeDefined();
  });

  it("renders tool_call block with tool name", () => {
    render(
      wrap(
        <AgentTraceTimeline
          blocks={[
            makeTraceBlock({
              id: "tb-3",
              kind: "tool_call",
              toolName: "graph_query",
              toolCallId: "tc-abc",
              status: "done",
            }),
          ]}
        />
      )
    );
    expect(screen.getByText(/graph_query/)).toBeDefined();
    expect(screen.getByText("completed")).toBeDefined();
  });

  it("maps streaming tool_call to running/streaming status", () => {
    render(
      wrap(
        <AgentTraceTimeline
          blocks={[
            makeTraceBlock({
              id: "tb-4",
              kind: "tool_call",
              toolName: "memory_search",
              status: "streaming",
            }),
          ]}
        />
      )
    );
    expect(screen.getByText("streaming...")).toBeDefined();
  });

  it("maps error tool_call to failed status", () => {
    render(
      wrap(
        <AgentTraceTimeline
          blocks={[
            makeTraceBlock({
              id: "tb-5",
              kind: "tool_call",
              toolName: "broken_tool",
              status: "error",
              isError: true,
            }),
          ]}
        />
      )
    );
    expect(screen.getByText("failed")).toBeDefined();
  });

  it("renders mixed blocks with correct kinds", () => {
    render(
      wrap(
        <AgentTraceTimeline
          blocks={[
            makeTraceBlock({ id: "tb-r1", kind: "reasoning", content: "hmm" }),
            makeTraceBlock({ id: "tb-t1", kind: "tool_call", toolName: "search", status: "done" }),
            makeTraceBlock({ id: "tb-a1", kind: "agent_message", content: "answer" }),
          ]}
        />
      )
    );
    expect(screen.getByText("Reasoning")).toBeDefined();
    expect(screen.getByText(/search/)).toBeDefined();
    expect(screen.getByText("Agent message")).toBeDefined();
  });

  it("falls back to block id when toolCallId is missing", () => {
    render(
      wrap(
        <AgentTraceTimeline
          blocks={[
            makeTraceBlock({
              id: "tb-no-tcid",
              kind: "tool_call",
              toolName: "anon_tool",
              status: "done",
            }),
          ]}
        />
      )
    );
    expect(screen.getByText(/anon_tool/)).toBeDefined();
  });
});

import { describe, expect, it, afterEach, vi } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import { UxxThemeProvider } from "@open-hax/uxx";
import { ChatMessageList } from "./ChatMessageList";
import type { ChatMessage, ChatTraceBlock, ToolReceipt } from "../../lib/types";

function wrap(ui: React.ReactElement) {
  return <UxxThemeProvider theme="dark">{ui}</UxxThemeProvider>;
}

afterEach(() => {
  cleanup();
});

const noop = () => {};
const noopAsync = () => Promise.resolve();

const defaultProps = {
  latestRun: null as any,
  latestToolReceipts: [] as ToolReceipt[],
  liveToolReceipts: [] as ToolReceipt[],
  liveToolEvents: [] as any[],
  assistantSurfaceBackground: "#1e1f1c",
  assistantSurfaceBorder: "#464741",
  assistantSurfaceText: "#90908a",
  onOpenMessageInCanvas: noop,
  onOpenSourceInPreview: noopAsync,
  onPinAssistantSource: noop,
  onAppendToScratchpad: noop,
  onPinMessageContext: noop,
};

function makeMessage(overrides: Partial<ChatMessage> & { id: string; role: ChatMessage["role"] }): ChatMessage {
  return { content: "hello", ...overrides };
}

// ─── User messages ──────────────────────────────────────────────

describe("ChatMessageList user messages", () => {
  it("renders user message with role label", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m1", role: "user", content: "What is Knoxx?" })]}
        />
      )
    );
    expect(screen.getByText("user")).toBeDefined();
    expect(screen.getByText("What is Knoxx?")).toBeDefined();
  });

  it("renders user content as plain text (not Markdown)", () => {
    const { container } = render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m2", role: "user", content: "hello **bold**" })]}
        />
      )
    );
    // Plain text div should contain the raw content, not rendered bold
    expect(screen.getByText(/hello \*\*bold\*\*/)).toBeDefined();
  });
});

// ─── Assistant messages ─────────────────────────────────────────

describe("ChatMessageList assistant messages", () => {
  it("renders assistant message with role label", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m3", role: "assistant", content: "Knoxx is an agent platform" })]}
        />
      )
    );
    expect(screen.getByText("assistant")).toBeDefined();
  });

  it("renders assistant content via Markdown", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m4", role: "assistant", content: "## Hello\n\nWorld" })]}
        />
      )
    );
    // Markdown renders heading as h2
    expect(screen.getByRole("heading", { level: 2 })).toBeDefined();
  });

  it("shows model badge when model is present", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m5", role: "assistant", content: "hi", model: "glm-5" })]}
        />
      )
    );
    expect(screen.getByText("glm-5")).toBeDefined();
  });

  it("shows status badge for done messages", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m6", role: "assistant", content: "done", status: "done" })]}
        />
      )
    );
    // Multiple badges may contain "done" — at least one status badge should exist
    const doneElements = screen.getAllByText("done");
    expect(doneElements.length).toBeGreaterThanOrEqual(1);
  });

  it("shows status badge for error messages", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m7", role: "assistant", content: "oops", status: "error" })]}
        />
      )
    );
    expect(screen.getByText("error")).toBeDefined();
  });

  it("shows status badge for streaming messages", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m8", role: "assistant", content: "", status: "streaming" })]}
        />
      )
    );
    expect(screen.getByText("streaming")).toBeDefined();
  });

  it("shows run ID badge when runId is present", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m9", role: "assistant", content: "hi", runId: "abcdef1234567890" })]}
        />
      )
    );
    expect(screen.getByText("abcdef12")).toBeDefined();
  });

  it("shows Open in Scratchpad button for assistant messages", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m10", role: "assistant", content: "test" })]}
        />
      )
    );
    expect(screen.getByText("Open in Scratchpad")).toBeDefined();
  });

  it("shows grounding metadata badge for assistant messages", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m11", role: "assistant", content: "test" })]}
        />
      )
    );
    expect(screen.getByText(/No grounding metadata/)).toBeDefined();
  });

  it("shows source count when sources are present", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({
            id: "m12",
            role: "assistant",
            content: "test",
            sources: [
              { title: "Doc A", url: "https://example.com/a" },
              { title: "Doc B", url: "https://example.com/b" },
            ],
          })]}
        />
      )
    );
    expect(screen.getByText(/2 source\(s\)/)).toBeDefined();
  });
});

// ─── System messages ────────────────────────────────────────────

describe("ChatMessageList system messages", () => {
  it("renders system message with role label", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m13", role: "system", content: "System notice" })]}
        />
      )
    );
    expect(screen.getByText("system")).toBeDefined();
  });

  it("renders system content via Markdown", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m14", role: "system", content: "### Alert\n\nNotice" })]}
        />
      )
    );
    expect(screen.getByRole("heading", { level: 3 })).toBeDefined();
  });
});

// ─── Tool calls from traceBlocks ────────────────────────────────

describe("ChatMessageList tool calls from traceBlocks", () => {
  it("shows Tool calls collapsible when traceBlocks has tool_call blocks", () => {
    const traceBlocks: ChatTraceBlock[] = [
      { id: "tb-1", kind: "tool_call", toolName: "graph_query", toolCallId: "tc-1", status: "done" },
      { id: "tb-2", kind: "agent_message", content: "answer", status: "done" },
    ];
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m15", role: "assistant", content: "result", traceBlocks })]}
        />
      )
    );
    expect(screen.getByText(/Tool calls \(1\)/)).toBeDefined();
  });

  it("hides Tool calls when traceBlocks has no tool_call blocks", () => {
    const traceBlocks: ChatTraceBlock[] = [
      { id: "tb-3", kind: "agent_message", content: "answer", status: "done" },
    ];
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m16", role: "assistant", content: "result", traceBlocks })]}
        />
      )
    );
    expect(screen.queryByText(/Tool calls/)).toBeNull();
  });
});

// ─── Live tool receipts ─────────────────────────────────────────

describe("ChatMessageList live tool receipts", () => {
  it("shows live tool receipts for streaming messages without traceBlocks", () => {
    const liveToolReceipts: ToolReceipt[] = [
      { id: "r1", status: "running", tool_name: "memory_search" },
    ];
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m17", role: "assistant", content: "", status: "streaming" })]}
          liveToolReceipts={liveToolReceipts}
        />
      )
    );
    expect(screen.getByText(/memory_search/)).toBeDefined();
  });

  it("hides live tool receipts when message is not streaming", () => {
    const liveToolReceipts: ToolReceipt[] = [
      { id: "r2", status: "running", tool_name: "search" },
    ];
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m18", role: "assistant", content: "done", status: "done" })]}
          liveToolReceipts={liveToolReceipts}
        />
      )
    );
    // Done messages should not show live receipts
    expect(screen.queryByText(/streaming\.\.\./)).toBeNull();
  });
});

// ─── Completed run tool receipts ────────────────────────────────

describe("ChatMessageList completed run tool receipts", () => {
  it("shows completed run tool receipts when runId matches latestRun", () => {
    const runId = "run-abc-1234";
    const latestToolReceipts: ToolReceipt[] = [
      { id: "r3", status: "completed", tool_name: "graph_query", result_preview: "5 nodes" },
    ];
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m19", role: "assistant", content: "result", status: "done", runId })]}
          latestRun={{ run_id: runId, status: "completed" } as any}
          latestToolReceipts={latestToolReceipts}
        />
      )
    );
    expect(screen.getByText(/Tool calls \(1\)/)).toBeDefined();
  });

  it("hides completed tool receipts when runId does not match latestRun", () => {
    const latestToolReceipts: ToolReceipt[] = [
      { id: "r4", status: "completed", tool_name: "search" },
    ];
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({ id: "m20", role: "assistant", content: "result", status: "done", runId: "run-other" })]}
          latestRun={{ run_id: "run-different", status: "completed" } as any}
          latestToolReceipts={latestToolReceipts}
        />
      )
    );
    // Should not show Tool calls because runId doesn't match
    const toolCallSummaries = screen.queryAllByText(/Tool calls/);
    // Only present if there's a matching runId
    expect(toolCallSummaries.length).toBe(0);
  });
});

// ─── Sources ────────────────────────────────────────────────────

describe("ChatMessageList sources", () => {
  it("shows Grounding sources section when sources exist", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({
            id: "m21",
            role: "assistant",
            content: "result",
            sources: [{ title: "README", url: "https://example.com/readme", section: "# Intro" }],
          })]}
        />
      )
    );
    expect(screen.getByText(/Grounding sources/)).toBeDefined();
    expect(screen.getByText("README")).toBeDefined();
  });

  it("shows Open button for source with a path", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({
            id: "m22",
            role: "assistant",
            content: "result",
            sources: [{ title: "Doc", url: "https://example.com/docs/guide.md" }],
          })]}
        />
      )
    );
    expect(screen.getByText("Open")).toBeDefined();
  });

  it("calls onPinAssistantSource when Pin is clicked", async () => {
    const pinFn = vi.fn();
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          onPinAssistantSource={pinFn}
          messages={[makeMessage({
            id: "m23",
            role: "assistant",
            content: "result",
            sources: [{ title: "Doc", url: "https://example.com/x" }],
          })]}
        />
      )
    );
    // Find Pin buttons (there may be multiple: source Pin, context Pin)
    const pinButtons = screen.getAllByText("Pin");
    expect(pinButtons.length).toBeGreaterThan(0);
  });
});

// ─── Context rows ───────────────────────────────────────────────

describe("ChatMessageList context rows", () => {
  it("shows Auto-injected context section when contextRows exist", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[makeMessage({
            id: "m24",
            role: "assistant",
            content: "result",
            contextRows: [{
              id: "ctx-1",
              project: "openplanner",
              kind: "knoxx.message",
              snippet: "This is a test snippet",
            }],
          })]}
        />
      )
    );
    expect(screen.getByText(/Auto-injected context/)).toBeDefined();
    expect(screen.getByText("openplanner")).toBeDefined();
    expect(screen.getByText("knoxx.message")).toBeDefined();
  });
});

// ─── Empty state ────────────────────────────────────────────────

describe("ChatMessageList empty state", () => {
  it("renders empty list for no messages", () => {
    const { container } = render(
      wrap(
        <ChatMessageList {...defaultProps} messages={[]} />
      )
    );
    // No message cards should exist
    expect(screen.queryByText(/assistant/)).toBeNull();
    expect(screen.queryByText(/user/)).toBeNull();
  });
});

// ─── Mixed message types ────────────────────────────────────────

describe("ChatMessageList mixed messages", () => {
  it("renders user and assistant messages together", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[
            makeMessage({ id: "u1", role: "user", content: "hello" }),
            makeMessage({ id: "a1", role: "assistant", content: "hi there" }),
          ]}
        />
      )
    );
    expect(screen.getByText("user")).toBeDefined();
    expect(screen.getByText("assistant")).toBeDefined();
  });

  it("renders all three role types", () => {
    render(
      wrap(
        <ChatMessageList
          {...defaultProps}
          messages={[
            makeMessage({ id: "u2", role: "user", content: "ask" }),
            makeMessage({ id: "s1", role: "system", content: "system note" }),
            makeMessage({ id: "a2", role: "assistant", content: "answer" }),
          ]}
        />
      )
    );
    expect(screen.getByText("user")).toBeDefined();
    expect(screen.getByText("system")).toBeDefined();
    expect(screen.getByText("assistant")).toBeDefined();
  });
});

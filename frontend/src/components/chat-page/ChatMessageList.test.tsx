import { describe, expect, it } from "vitest";
import { deriveAssistantPresentation } from "./ChatMessageList";
import type { ChatMessage } from "../../lib/types";

describe("deriveAssistantPresentation", () => {
  it("hides completed trace blocks from the main chat transcript once the final answer exists", () => {
    const message: ChatMessage = {
      id: "assistant-1",
      role: "assistant",
      content: "Final answer",
      status: "done",
      runId: "run-12345678",
      traceBlocks: [
        {
          id: "reasoning-1",
          kind: "reasoning",
          status: "done",
          content: "Reasoning summary",
        },
        {
          id: "assistant-trace-1",
          kind: "agent_message",
          status: "done",
          content: "Final answer",
        },
      ],
    };

    expect(deriveAssistantPresentation(message)).toEqual({
      effectiveStatus: "done",
      visibleTraceBlocks: [],
      showAssistantFinalCard: true,
    });
  });

  it("treats finalized trace-only streaming snapshots as done while keeping audit traces out of chat", () => {
    const message: ChatMessage = {
      id: "assistant-1",
      role: "assistant",
      content: "Final answer",
      status: "streaming",
      runId: "run-abcdef12",
      traceBlocks: [
        { id: "reasoning-1", kind: "reasoning", status: "done", content: "First reasoning" },
        { id: "assistant-trace-1", kind: "agent_message", status: "done", content: "Partial answer" },
        { id: "reasoning-2", kind: "reasoning", status: "done", content: "Second reasoning" },
        { id: "assistant-trace-2", kind: "agent_message", status: "done", content: "Final answer" },
      ],
    };

    expect(deriveAssistantPresentation(message)).toEqual({
      effectiveStatus: "done",
      visibleTraceBlocks: [],
      showAssistantFinalCard: true,
    });
  });

  it("keeps live trace blocks visible while the assistant is still streaming", () => {
    const traceBlocks = [
      { id: "reasoning-1", kind: "reasoning", status: "streaming", content: "Thinking..." },
      { id: "assistant-trace-1", kind: "agent_message", status: "streaming", content: "Partial" },
    ] as const;

    const message: ChatMessage = {
      id: "assistant-2",
      role: "assistant",
      content: "Partial",
      status: "streaming",
      traceBlocks: [...traceBlocks],
    };

    expect(deriveAssistantPresentation(message)).toEqual({
      effectiveStatus: "streaming",
      visibleTraceBlocks: [...traceBlocks],
      showAssistantFinalCard: false,
    });
  });

  it("keeps archived tool trace blocks visible when no separate run receipts are available", () => {
    const traceBlocks = [
      { id: "reasoning-1", kind: "reasoning", status: "done", content: "Reasoning summary" },
      { id: "tool-1", kind: "tool_call", status: "done", toolName: "discord.read", outputPreview: "failed" },
      { id: "assistant-trace-1", kind: "agent_message", status: "done", content: "Final answer" },
    ] as const;

    const message: ChatMessage = {
      id: "assistant-3",
      role: "assistant",
      content: "Final answer",
      status: "done",
      runId: "archived-run",
      traceBlocks: [...traceBlocks],
    };

    expect(deriveAssistantPresentation(message)).toEqual({
      effectiveStatus: "done",
      visibleTraceBlocks: [...traceBlocks],
      showAssistantFinalCard: true,
    });
  });

  it("hides archived tool trace blocks when separate run receipts will render them", () => {
    const traceBlocks = [
      { id: "tool-1", kind: "tool_call", status: "done", toolName: "discord.read", outputPreview: "ok" },
    ] as const;

    const message: ChatMessage = {
      id: "assistant-4",
      role: "assistant",
      content: "Final answer",
      status: "done",
      runId: "live-run",
      traceBlocks: [...traceBlocks],
    };

    expect(deriveAssistantPresentation(message, { hasSeparateToolReceipts: true })).toEqual({
      effectiveStatus: "done",
      visibleTraceBlocks: [],
      showAssistantFinalCard: true,
    });
  });
});

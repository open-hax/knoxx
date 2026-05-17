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
});

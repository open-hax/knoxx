import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ChatMessageList } from "./ChatMessageList";
import type { ChatMessage } from "../../lib/types";

describe("ChatMessageList", () => {
  it("renders the final assistant card even when trace blocks exist", () => {
    const messages: ChatMessage[] = [{
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
    }];

    render(
      <ChatMessageList
        messages={messages}
        latestRun={null}
        latestToolReceipts={[]}
        liveToolReceipts={[]}
        liveToolEvents={[]}
        assistantSurfaceBackground="black"
        assistantSurfaceBorder="gray"
        assistantSurfaceText="white"
        onSend={vi.fn()}
        onOpenMessageInCanvas={vi.fn()}
        onOpenSourceInPreview={vi.fn()}
        onPinAssistantSource={vi.fn()}
        onAppendToScratchpad={vi.fn()}
        onPinMessageContext={vi.fn()}
      />,
    );

    expect(screen.getByText("Reasoning summary")).toBeInTheDocument();
    expect(screen.getAllByText("Final answer")).toHaveLength(1);
    expect(screen.getByRole("button", { name: "Reply by voice" })).toBeInTheDocument();
  });

  it("only shows voice reply on the latest assistant message", () => {
    const messages: ChatMessage[] = [
      { id: "assistant-1", role: "assistant", content: "First", status: "done" },
      { id: "assistant-2", role: "assistant", content: "Second", status: "done" },
    ];

    render(
      <ChatMessageList
        messages={messages}
        latestRun={null}
        latestToolReceipts={[]}
        liveToolReceipts={[]}
        liveToolEvents={[]}
        assistantSurfaceBackground="black"
        assistantSurfaceBorder="gray"
        assistantSurfaceText="white"
        onSend={vi.fn()}
        onOpenMessageInCanvas={vi.fn()}
        onOpenSourceInPreview={vi.fn()}
        onPinAssistantSource={vi.fn()}
        onAppendToScratchpad={vi.fn()}
        onPinMessageContext={vi.fn()}
      />,
    );

    expect(screen.getAllByRole("button", { name: "Reply by voice" })).toHaveLength(1);
  });
});

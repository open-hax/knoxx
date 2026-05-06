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
        onOpenMessageInCanvas={vi.fn()}
        onOpenSourceInPreview={vi.fn()}
        onPinAssistantSource={vi.fn()}
        onAppendToScratchpad={vi.fn()}
        onPinMessageContext={vi.fn()}
      />,
    );

    expect(screen.getByText("Reasoning summary")).toBeInTheDocument();
    expect(screen.getAllByText("Final answer")).toHaveLength(1);

    const renderedAnswer = screen.getByText("Final answer");
    const actionGroup = screen.getByRole("group", { name: "Assistant message actions" });
    expect(renderedAnswer.compareDocumentPosition(actionGroup) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it("treats finalized trace-only streaming snapshots as done and hides duplicated agent trace blocks", () => {
    const messages: ChatMessage[] = [{
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
        onOpenMessageInCanvas={vi.fn()}
        onOpenSourceInPreview={vi.fn()}
        onPinAssistantSource={vi.fn()}
        onAppendToScratchpad={vi.fn()}
        onPinMessageContext={vi.fn()}
      />,
    );

    expect(screen.queryByText("streaming")).not.toBeInTheDocument();
    expect(screen.getAllByText("done").length).toBeGreaterThan(0);
    expect(screen.getByText("First reasoning")).toBeInTheDocument();
    expect(screen.getByText("Second reasoning")).toBeInTheDocument();
    expect(screen.queryByText("Partial answer")).not.toBeInTheDocument();
    expect(screen.getAllByText("Final answer")).toHaveLength(1);
  });

  it("renders assistant actions on every assistant message", () => {
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
        onOpenMessageInCanvas={vi.fn()}
        onOpenSourceInPreview={vi.fn()}
        onPinAssistantSource={vi.fn()}
        onAppendToScratchpad={vi.fn()}
        onPinMessageContext={vi.fn()}
      />,
    );

    expect(screen.getAllByRole("button", { name: "Open in Scratchpad" })).toHaveLength(2);
  });
});

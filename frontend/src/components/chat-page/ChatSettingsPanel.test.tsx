import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ChatSettingsPanel } from "./ChatSettingsPanel";
import type { AgentContractCatalogItem, ToolCatalogResponse } from "../../lib/types";

const agents: AgentContractCatalogItem[] = [
  { id: "knoxx_default", role: "knowledge_worker", model: "gemma4:31b" },
  { id: "planner", role: "planner", model: "qwen3:32b" },
];

const toolCatalog: ToolCatalogResponse = {
  role: "knowledge_worker",
  actor_id: "chat_primary",
  agent_id: "knoxx_default",
  capability_ids: ["search", "write"],
  system_prompt: "You are Knoxx. Follow the workspace contract carefully.",
  actor_system_prompt: "actor://chat_primary",
  agent_system_prompt: "agent://knoxx_default",
  task_prompt: "Help the user with planning tasks.",
  tools: [
    { id: "search", label: "Search", description: "Search docs", enabled: true },
    { id: "write", label: "Write", description: "Write files", enabled: false },
  ],
  email_enabled: false,
};

describe("ChatSettingsPanel", () => {
  it("renders the runtime prompt and tool inventory in separate sections", () => {
    render(
      <ChatSettingsPanel
        systemPrompt="Keep answers short"
        onSystemPromptChange={vi.fn()}
        conversationId="conv-123"
        activeRole="knowledge_worker"
        activeActorId="chat_primary"
        activeAgentId="knoxx_default"
        availableAgents={agents}
        onActiveAgentChange={vi.fn()}
        toolCatalog={toolCatalog}
      />,
    );

    expect(screen.getByText("Runtime prompt")).toBeInTheDocument();
    expect(screen.getByText("Effective system prompt")).toBeInTheDocument();
    expect(screen.getByText("You are Knoxx. Follow the workspace contract carefully.")).toBeInTheDocument();
    expect(screen.getByText("Enabled (1)")).toBeInTheDocument();
    expect(screen.getByText("Disabled (1)")).toBeInTheDocument();
    expect(screen.getByText("Search")).toBeInTheDocument();
    expect(screen.getByText("Write")).toBeInTheDocument();

    const scrollRegion = screen.getByTestId("chat-settings-scroll-region");
    expect(scrollRegion).toBeInTheDocument();
    expect(scrollRegion).toHaveStyle({
      maxHeight: "min(70vh, 40rem)",
      overflowY: "auto",
    });
  });

  it("propagates steering note and active agent changes", () => {
    const onSystemPromptChange = vi.fn();
    const onActiveAgentChange = vi.fn();

    render(
      <ChatSettingsPanel
        systemPrompt="Keep answers short"
        onSystemPromptChange={onSystemPromptChange}
        conversationId={null}
        activeRole="knowledge_worker"
        activeActorId="chat_primary"
        activeAgentId="knoxx_default"
        availableAgents={agents}
        onActiveAgentChange={onActiveAgentChange}
        toolCatalog={toolCatalog}
      />,
    );

    fireEvent.change(
      screen.getByPlaceholderText("Optional: steer the agent toward a specific outcome for upcoming turns..."),
      { target: { value: "Prefer concise plans" } },
    );
    fireEvent.change(screen.getByDisplayValue("knoxx_default"), { target: { value: "planner" } });

    expect(onSystemPromptChange).toHaveBeenCalledWith("Prefer concise plans");
    expect(onActiveAgentChange).toHaveBeenCalledWith("planner");
  });
});

import { describe, expect, it } from "vitest";

import { shouldApplyAgentModelSelection } from "./useChatWorkspaceController";

describe("shouldApplyAgentModelSelection", () => {
  it("uses the agent model when no model has been selected yet", () => {
    expect(shouldApplyAgentModelSelection({
      activeAgentId: "knoxx_default",
      previousAgentId: null,
      selectedModel: "",
      agentModel: "gemma4:31b",
    })).toBe(true);
  });

  it("uses the newly selected agent model when the active agent changes", () => {
    expect(shouldApplyAgentModelSelection({
      activeAgentId: "research_agent",
      previousAgentId: "knoxx_default",
      selectedModel: "qwen3:32b",
      agentModel: "gemma4:31b",
    })).toBe(true);
  });

  it("preserves a manual model override while the same agent stays active", () => {
    expect(shouldApplyAgentModelSelection({
      activeAgentId: "knoxx_default",
      previousAgentId: "knoxx_default",
      selectedModel: "qwen3:32b",
      agentModel: "gemma4:31b",
    })).toBe(false);
  });
});
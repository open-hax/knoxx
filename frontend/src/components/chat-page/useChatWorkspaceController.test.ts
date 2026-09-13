import { act, renderHook } from "@testing-library/react";
import { selectChatWorkspaceState, useChatWorkspaceState } from "./chat-page-config";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

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

describe("chat workspace state ownership", () => {
  const storedKeys = ["knoxx_last_chat_settings", "knoxx_session_actor_filter", "knoxx_exclude_eta_mu_sessions"];
  let storedValues: Array<string | null>;
  const options = { defaultRole: "editor", defaultActorId: "writer", initialShowCanvas: true,
    initialShowConsole: false, initialShowSettings: false, initialSidebarWidthPx: 320 };
  beforeEach(() => { storedValues = storedKeys.map(key => localStorage.getItem(key)); });
  afterEach(() => {
    vi.restoreAllMocks();
    storedKeys.forEach((key, index) => {
      const value = storedValues[index];
      if (value === null) localStorage.removeItem(key); else localStorage.setItem(key, value);
    });
  });

  it("restores persisted choices and retains stable refs through session state updates", () => {
    localStorage.setItem(storedKeys[0], JSON.stringify({activeAgentId: "reviewer", selectedModel: "manual-model", selectedThinkingLevel: "high"}));
    localStorage.setItem(storedKeys[1], "reviewer");
    localStorage.setItem(storedKeys[2], "false");
    const {result, rerender} = renderHook(() => useChatWorkspaceState(options));
    expect(result.current).toMatchObject({activeRole: "editor", activeActorId: "writer", activeAgentId: "reviewer",
      selectedModel: "manual-model", selectedThinkingLevel: "high", sessionActorFilter: "reviewer", excludeEtaMuSessions: false,
      showCanvas: true, showConsole: false, sidebarWidthPx: 320});
    const recentRef = result.current.recentSessionsRef;
    const runRef = result.current.activeRunIdRef;
    runRef.current = "run-live";
    const sessions = [{session: "session-new", event_count: 0, title: "New draft"}];
    act(() => result.current.setRecentSessions(sessions));
    rerender();
    expect(result.current.recentSessionsRef).toBe(recentRef);
    expect(recentRef.current).toBe(sessions);
    expect(result.current.activeRunIdRef).toBe(runRef);
    expect(runRef.current).toBe("run-live");
  });

  it("projects only requested state fields and preserves the actual React setter", () => {
    const {result} = renderHook(() => useChatWorkspaceState(options));
    const selected = selectChatWorkspaceState(result.current, ["messages", "setMessages"]);
    expect(Object.keys(selected)).toEqual(["messages", "setMessages"]);
    expect(selected.setMessages).toBe(result.current.setMessages);
    act(() => selected.setMessages([{id: "draft", role: "user", content: "Write clearly"}]));
    expect(result.current.messages[0].content).toBe("Write clearly");
    expect(selected).not.toHaveProperty("activeRunIdRef");
  });

  it("uses the existing defaults when browser preference storage is unavailable", () => {
    vi.spyOn(window, "localStorage", "get").mockImplementation(() => { throw new DOMException("Unavailable", "SecurityError"); });
    const {result} = renderHook(() => useChatWorkspaceState(options));
    expect(result.current).toMatchObject({activeAgentId: "", selectedModel: "", selectedThinkingLevel: "off",
      sessionActorFilter: "all", excludeEtaMuSessions: true, activeRole: "editor"});
  });
});

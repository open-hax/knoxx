import { beforeEach, describe, expect, it } from "vitest";

import { persistedSessionVisibleForActor, persistedSessionVisibleForFilter, preferredSessionModelForResume } from "./workspace-actions";
import { persistChatSessionSnapshot, type ChatSessionSnapshot } from "./hooks";
import type { ChatMessage } from "../../lib/types";

beforeEach(() => {
  localStorage.clear();
});

describe("preferredSessionModelForResume", () => {
  it("prefers the persisted session model when available", () => {
    const snapshot: ChatSessionSnapshot = { selectedModel: "gpt-5" };
    const transcript: ChatMessage[] = [
      { id: "a1", role: "assistant", content: "hello", model: "gemma4:31b" },
    ];

    expect(preferredSessionModelForResume(snapshot, transcript)).toBe("gpt-5");
  });

  it("falls back to the latest assistant model in the transcript", () => {
    const transcript: ChatMessage[] = [
      { id: "u1", role: "user", content: "hi" },
      { id: "a1", role: "assistant", content: "first", model: "gemma4:31b" },
      { id: "a2", role: "assistant", content: "latest", model: "gpt-5" },
    ];

    expect(preferredSessionModelForResume(null, transcript)).toBe("gpt-5");
  });
});

describe("persistedSessionVisibleForActor", () => {
  it("uses the current actor catalog for wildcard-capable local sessions", () => {
    persistChatSessionSnapshot("workspace", "session-1", {
      sessionId: "session-1",
      conversationId: "conversation-1",
      activeActorId: "chat_primary",
      activeAgentId: "knoxx_default",
      messages: [],
      runtimeEvents: [],
      isSending: false,
    });

    expect(
      persistedSessionVisibleForActor(
        "workspace",
        { session: "conversation-1", active_session_id: "session-1", local_only: true },
        "cms_chat",
        new Set(["knoxx_default"]),
      ),
    ).toBe(true);
  });

  it("falls back to chat_primary for legacy local sessions without agent metadata", () => {
    persistChatSessionSnapshot("workspace", "session-legacy", {
      sessionId: "session-legacy",
      conversationId: "conversation-legacy",
      messages: [],
      runtimeEvents: [],
      isSending: false,
    });

    expect(
      persistedSessionVisibleForActor(
        "workspace",
        { session: "conversation-legacy", active_session_id: "session-legacy", local_only: true },
        "chat_primary",
        new Set(),
      ),
    ).toBe(true);
    expect(
      persistedSessionVisibleForActor(
        "workspace",
        { session: "conversation-legacy", active_session_id: "session-legacy", local_only: true },
        "cms_chat",
        new Set(),
      ),
    ).toBe(false);
  });

  it("can exclude eta-mu-authored local sessions while showing all other actors", () => {
    persistChatSessionSnapshot("workspace", "session-eta-mu", {
      sessionId: "session-eta-mu",
      conversationId: "conversation-eta-mu",
      activeActorId: "eta-mu",
      messages: [],
      runtimeEvents: [],
      isSending: false,
    });

    expect(
      persistedSessionVisibleForFilter(
        "workspace",
        { session: "conversation-eta-mu", active_session_id: "session-eta-mu", local_only: true },
        "all",
        true,
        new Set(),
      ),
    ).toBe(false);
  });
});

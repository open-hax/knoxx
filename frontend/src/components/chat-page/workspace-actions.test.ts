import { describe, expect, it } from "vitest";

import { preferredSessionModelForResume } from "./workspace-actions";
import type { ChatSessionSnapshot } from "./hooks";
import type { ChatMessage } from "../../lib/types";

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
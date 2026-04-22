import { describe, expect, it } from "vitest";

import { ensureTrailingSpeechSpace, extractAutoConversationChunks } from "./auto-conversation-voice-utils";

describe("extractAutoConversationChunks", () => {
  it("emits completed sentences and keeps unfinished tails pending", () => {
    expect(extractAutoConversationChunks("Hello there. How are", false)).toEqual({
      chunks: ["Hello there."],
      pending: "How are",
    });
  });

  it("splits long text near whitespace when punctuation has not arrived", () => {
    const result = extractAutoConversationChunks(
      "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu nu xi omicron pi rho sigma tau",
      false,
      30,
    );

    expect(result.chunks[0]).toBe("alpha beta gamma delta epsilon");
    expect(result.pending.length).toBeGreaterThan(0);
  });

  it("flushes the last fragment when forced", () => {
    expect(extractAutoConversationChunks("still speaking without punctuation", true)).toEqual({
      chunks: ["still speaking without punctuation"],
      pending: "",
    });
  });
});

describe("ensureTrailingSpeechSpace", () => {
  it("normalizes whitespace and appends one trailing space", () => {
    expect(ensureTrailingSpeechSpace("  hello\nworld  ")).toBe("hello world ");
  });
});

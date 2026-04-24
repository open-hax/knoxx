import { describe, expect, it } from "vitest";
import { extractEmbedsFromMarkdown } from "./mediaEmbeds";

describe("extractEmbedsFromMarkdown", () => {
  it("extracts workspace-relative audio link and rewrites markdown", () => {
    const input = "Here is the track: [mix](Music/test.wav)";
    const out = extractEmbedsFromMarkdown(input);
    expect(out.markdown).toContain("mix (embedded below)");
    expect(out.markdown).not.toContain("(Music/test.wav)");
    expect(out.contentParts).toHaveLength(1);
    expect(out.contentParts[0]).toMatchObject({
      type: "audio",
      url: "/api/workspace-media/raw?path=Music%2Ftest.wav",
      filename: "mix",
    });
  });

  it("does not auto-embed remote http(s) media", () => {
    const input = "Remote: [clip](https://example.com/video.mp4)";
    const out = extractEmbedsFromMarkdown(input);
    expect(out.contentParts).toHaveLength(0);
    expect(out.markdown).toBe(input);
  });

  it("ignores fenced code blocks", () => {
    const input = [
      "```",
      "[mix](Music/test.wav)",
      "```",
      "Outside: [mix](Music/test.wav)",
    ].join("\n");
    const out = extractEmbedsFromMarkdown(input);
    expect(out.contentParts).toHaveLength(1);
    expect(out.markdown).toContain("```\n[mix](Music/test.wav)\n```");
    expect(out.markdown).toContain("Outside: mix (embedded below)");
  });
});

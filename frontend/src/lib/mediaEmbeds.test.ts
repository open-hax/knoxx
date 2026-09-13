import { afterEach, describe, expect, it, vi } from "vitest";
import { attachmentToContentPart, extractEmbedsFromMarkdown } from "./mediaEmbeds";

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


afterEach(() => { vi.unstubAllGlobals(); });

describe("attachment transmission", () => {
  it("reuses image data previews with exact browser file metadata", async () => {
    const file = new File(["png"], "diagram.png", { type: "image/png" });
    await expect(attachmentToContentPart({ id: "image", type: "image", file, preview: "data:image/png;base64,cG5n" }))
      .resolves.toEqual({ type: "image", mimeType: "image/png", filename: "diagram.png", size: 3, data: "data:image/png;base64,cG5n" });
  });

  it("reads a document with FileReader when it has no preview", async () => {
    const file = new File(["hello"], "notes.txt", { type: "text/plain" });
    await expect(attachmentToContentPart({ id: "document", type: "document", file }))
      .resolves.toEqual({ type: "document", mimeType: "text/plain", filename: "notes.txt", size: 5, data: "data:text/plain;base64,aGVsbG8=" });
  });

  it("keeps the blob URL fallback if loading a recording fails", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("recording expired")));
    const file = new File(["sound"], "reply.webm", { type: "audio/webm" });
    await expect(attachmentToContentPart({ id: "audio", type: "audio", file, preview: "blob:recording" }))
      .resolves.toEqual({ type: "audio", mimeType: "audio/webm", filename: "reply.webm", size: 5, url: "blob:recording" });
  });
});

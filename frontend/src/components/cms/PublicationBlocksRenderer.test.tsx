import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { PublicationBlocksRenderer, extractPublicationBlocks } from "./PublicationBlocksRenderer";

describe("PublicationBlocksRenderer", () => {
  it("renders hero, rich text, and playlist blocks", () => {
    const blocks = extractPublicationBlocks({
      blocks: [
        { id: "hero", type: "hero", title: "Night Broadcast", subtitle: "A curated signal chain" },
        { id: "intro", type: "rich_text", markdown: "Welcome to **the transmission**." },
        {
          id: "playlist",
          type: "playlist",
          title: "Signal Queue",
          description: "Two tracks for the late shift.",
          layout: "cards",
          show_labels: true,
          show_descriptions: true,
          show_duration: true,
          tracks: [
            {
              path: "Audio/broadcasts/one.mp3",
              title: "Opening Drop",
              duration: 61,
              labels: ["broadcast", "drop"],
              description: "A bright station identifier.",
            },
          ],
        },
      ],
    });

    const html = renderToStaticMarkup(<PublicationBlocksRenderer blocks={blocks} getAudioUrl={(path) => `/media/${path}`} />);

    expect(html).toContain("Night Broadcast");
    expect(html).toContain("A curated signal chain");
    expect(html).toContain("the transmission");
    expect(html).toContain("Signal Queue");
    expect(html).toContain("Opening Drop");
    expect(html).toContain("A bright station identifier.");
    expect(html).toContain("broadcast");
    expect(html).toContain("1:01");
    expect(html).toContain("/media/Audio/broadcasts/one.mp3");
  });

  it("filters hidden and unknown blocks during extraction", () => {
    const blocks = extractPublicationBlocks({
      blocks: [
        { id: "hidden", type: "heading", text: "Hidden", hidden: true },
        { id: "unknown", type: "iframe", src: "https://example.com" },
        { id: "visible", type: "heading", level: 2, text: "Visible" },
      ],
    });

    expect(blocks).toHaveLength(1);
    expect(blocks[0]?.type).toBe("heading");
    const html = renderToStaticMarkup(<PublicationBlocksRenderer blocks={blocks} />);
    expect(html).toContain("Visible");
    expect(html).not.toContain("Hidden");
  });
});

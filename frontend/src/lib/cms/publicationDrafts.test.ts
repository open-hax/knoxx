import { describe, expect, it } from "vitest";
import { buildPlaylistPublicationDraft, slugifyPublicationTitle } from "./publicationDrafts";

describe("publicationDrafts", () => {
  it("slugifies publication titles for stable CMS paths", () => {
    expect(slugifyPublicationTitle(" Brain Damage Transmissions! ")).toBe("brain-damage-transmissions");
  });

  it("builds a block CMS playlist publication draft", () => {
    const draft = buildPlaylistPublicationDraft({
      title: "Late Shift Signals",
      description: "A stack of bumpers for midnight relay.",
      now: new Date("2026-05-07T12:00:00.000Z"),
      tracks: [
        {
          path: "Audio/broadcasts/drop.wav",
          name: "drop.wav",
          duration: 42.4,
          labels: ["broadcast", "broadcast", "drop"],
          description: "A bright station identifier.",
        },
      ],
    });

    expect(draft.sourcePath).toBe("cms/playlists/2026-05-07-late-shift-signals.md");
    expect(draft.metadata.publication_kind).toBe("playlist");
    expect(draft.metadata.block_schema_version).toBe(1);
    expect(draft.metadata.source_audio_paths).toEqual(["Audio/broadcasts/drop.wav"]);
    expect(draft.metadata.blocks.map((block) => block.type)).toEqual(["hero", "rich_text", "playlist"]);
    expect(draft.content).toContain("# Late Shift Signals");
    expect(draft.content).toContain("## Playlist summary");

    const playlist = draft.metadata.blocks.find((block) => block.type === "playlist");
    expect(playlist?.type).toBe("playlist");
    if (playlist?.type === "playlist") {
      expect(playlist.tracks[0]?.title).toBe("drop");
      expect(playlist.tracks[0]?.labels).toEqual(["broadcast", "drop"]);
      expect(playlist.tracks[0]?.description).toBe("A bright station identifier.");
    }
  });

  it("keeps fallback markdown bounded for large playlists while preserving structured tracks", () => {
    const draft = buildPlaylistPublicationDraft({
      title: "Huge Queue",
      tracks: Array.from({ length: 962 }, (_, index) => ({
        path: `Audio/track-${index + 1}.mp3`,
        name: `track-${index + 1}.mp3`,
      })),
    });

    expect(draft.metadata.source_audio_paths).toHaveLength(962);
    expect(draft.content).toContain("This block publication contains 962 tracks");
    expect(draft.content).toContain("…and 922 more tracks");
    expect(draft.content.length).toBeLessThan(3000);
  });
});

import type { PublicationBlock, PlaylistTrackRef } from "../../components/cms/PublicationBlocksRenderer";

export type PlaylistPublicationTrackInput = {
  path: string;
  name?: string;
  title?: string;
  duration?: number;
  labels?: string[];
  description?: string;
  mime?: string;
  source_url?: string;
};

export type PlaylistPublicationDraftInput = {
  title: string;
  description?: string;
  tracks: PlaylistPublicationTrackInput[];
  now?: Date;
};

export type PlaylistPublicationDraft = {
  title: string;
  slug: string;
  sourcePath: string;
  content: string;
  metadata: {
    publication_kind: "playlist";
    block_schema_version: 1;
    source_audio_paths: string[];
    blocks: PublicationBlock[];
  };
};

function cleanTitle(value: string): string {
  return value.trim().replace(/\s+/g, " ") || "Broadcast Playlist";
}

export function slugifyPublicationTitle(title: string): string {
  return cleanTitle(title)
    .toLowerCase()
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 80) || "broadcast-playlist";
}

function trackTitle(track: PlaylistPublicationTrackInput): string {
  const candidate = track.title ?? track.name ?? track.path.split("/").pop() ?? track.path;
  return candidate.replace(/\.[a-z0-9]{2,5}$/i, "").trim() || track.path;
}

function uniqueStrings(values: Array<string | undefined>): string[] {
  return Array.from(new Set(values.filter((value): value is string => typeof value === "string" && value.trim().length > 0).map((value) => value.trim())));
}

function toTrackRef(track: PlaylistPublicationTrackInput): PlaylistTrackRef | null {
  if (!track.path.trim()) return null;
  return {
    path: track.path,
    title: trackTitle(track),
    duration: typeof track.duration === "number" && Number.isFinite(track.duration) ? track.duration : undefined,
    labels: uniqueStrings(track.labels ?? []),
    description: track.description?.trim() || undefined,
    mime: track.mime?.trim() || undefined,
    source_url: track.source_url?.trim() || undefined,
  };
}

function buildFallbackContent(title: string, description: string | undefined, tracks: PlaylistTrackRef[]): string {
  const previewTracks = tracks.slice(0, 40);
  const omittedCount = Math.max(0, tracks.length - previewTracks.length);
  const lines = [
    `# ${title}`,
    "",
    description?.trim() || `A Broadcast Studio playlist with ${tracks.length} track${tracks.length === 1 ? "" : "s"}.`,
    "",
    "## Playlist summary",
    "",
    `This block publication contains ${tracks.length} track${tracks.length === 1 ? "" : "s"}. The complete track snapshot is stored in structured publication metadata.`,
    "",
    ...previewTracks.map((track, index) => {
      const bits = [`${index + 1}. ${track.title}`];
      if (track.description) bits.push(` — ${track.description}`);
      return bits.join("");
    }),
    omittedCount > 0 ? `…and ${omittedCount} more tracks in the structured playlist block.` : "",
    "",
  ];
  return lines.join("\n");
}

export function buildPlaylistPublicationDraft(input: PlaylistPublicationDraftInput): PlaylistPublicationDraft {
  const title = cleanTitle(input.title);
  const slug = slugifyPublicationTitle(title);
  const createdAt = input.now ?? new Date();
  const datePrefix = createdAt.toISOString().slice(0, 10);
  const sourcePath = `cms/playlists/${datePrefix}-${slug}.md`;
  const tracks = input.tracks.map(toTrackRef).filter((track): track is PlaylistTrackRef => track !== null);
  const description = input.description?.trim() || undefined;
  const content = buildFallbackContent(title, description, tracks);
  const blocks: PublicationBlock[] = [
    {
      id: "hero",
      type: "hero",
      title,
      subtitle: description ?? `${tracks.length} track${tracks.length === 1 ? "" : "s"} from Broadcast Studio`,
    },
    {
      id: "intro",
      type: "rich_text",
      markdown: description ?? "A curated audio publication drafted from the Broadcast Studio queue.",
    },
    {
      id: "playlist",
      type: "playlist",
      title,
      description,
      layout: "cards",
      tracks,
      show_labels: true,
      show_descriptions: true,
      show_duration: true,
    },
  ];

  return {
    title,
    slug,
    sourcePath,
    content,
    metadata: {
      publication_kind: "playlist",
      block_schema_version: 1,
      source_audio_paths: tracks.map((track) => track.path),
      blocks,
    },
  };
}

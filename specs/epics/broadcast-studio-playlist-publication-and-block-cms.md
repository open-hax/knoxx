# Broadcast Studio Playlist Publication + Block CMS

Status: draft
Owner: Knoxx / OpenPlanner CMS / Broadcast Studio
Created: 2026-05-07
Related:
- `specs/epics/knowledge-ops-cms-data-model.md`
- `specs/archived/tasks/broadcast-studio-contract-ui-subagents.md`
- `specs/epics/knowledge-ops-gardens.md`
- `frontend/src/pages/CmsPage.tsx`
- `frontend/src/pages/BroadcastStudioPage.tsx`

## Problem

Broadcast Studio can curate audio files into queues/playlists and save `.m3u` artifacts, while the CMS can publish documents into gardens. These are currently separate product motions:

- playlists are operational/audio-library state;
- CMS documents are mostly raw markdown content;
- garden publication is document-centric;
- public rendering is closer to a simple blog than a composable page builder.

The desired product behavior is: **publish curated audio experiences** the same way CMS content is published, but without forcing playlists into raw markdown conventions.

The CMS also needs to evolve beyond one `title + markdown body` field. It should support sections, headings, callouts, playlist embeds, individual track cards, show notes, and later richer page components.

## Goals

1. Make playlists first-class publishable artifacts.
2. Upgrade the CMS content model from raw markdown to structured blocks.
3. Preserve OpenPlanner as the canonical store and CMS as a mutation/view layer.
4. Reuse existing garden publication semantics instead of inventing a separate playlist-public stack.
5. Let Broadcast Studio create a draft publication from a playlist/queue.
6. Let CMS refine that draft with headings, sections, text, and playlist/track blocks.
7. Render public pages from block data with typed audio components, not brittle markdown parsing.

## Non-goals

- Do not build a full Notion clone.
- Do not silently publish playlists without explicit review/publish action.
- Do not store canonical playlist publications only as `.m3u` text.
- Do not make `audio-labels.json` the publication source of truth.
- Do not require every CMS document to migrate to blocks in the first slice.
- Do not block existing markdown CMS editing while block documents are introduced.

## Product frame

A published item should be a **publication**, not necessarily an article.

Publication kinds:

| Kind | Purpose |
|------|---------|
| `article` | Classic CMS page or blog-style post |
| `playlist` | Curated audio playlist with track cards and commentary |
| `show_notes` | Broadcast episode notes with embedded playlist/audio sections |
| `landing_page` | Composed public page with sections, calls to action, and embedded resources |
| `collection` | Grouping page for multiple publications or playlists |

The common shape is:

```text
publication metadata + ordered blocks + garden publication state
```

## Canonical model

OpenPlanner remains canonical. A block publication is stored as a document/event with document semantics plus structured metadata.

### Publication document

```ts
type PublicationKind = "article" | "playlist" | "show_notes" | "landing_page" | "collection";
type PublicationStatus = "draft" | "review" | "public" | "archived";

type PublicationDocument = {
  id: string;
  title: string;
  slug: string;
  project: string;
  kind: "docs";
  source: "cms" | "broadcast-studio" | "manual" | "ai-drafted";
  visibility: PublicationStatus;
  content: string;              // compatibility fallback/rendered markdown summary
  metadata: {
    publication_kind: PublicationKind;
    block_schema_version: 1;
    blocks: PublicationBlock[];
    source_playlist_id?: string;
    source_playlist_path?: string;
    source_audio_paths?: string[];
    garden_publications?: GardenPublication[];
  };
};
```

`content` stays as a compatibility field for search and old renderers. The typed page renderer should prefer `metadata.blocks` when present.

### Garden publication

Reuse existing `metadata.garden_publications` pattern:

```ts
type GardenPublication = {
  garden_id: string;
  published_at: string;
  published_by: string;
  slug?: string;
  canonical_url?: string;
  translation_status?: "none" | "queued" | "partial" | "complete" | "failed";
  translated_languages?: string[];
};
```

## Block schema v1

Keep the first schema deliberately small.

```ts
type PublicationBlock =
  | HeroBlock
  | HeadingBlock
  | RichTextBlock
  | CalloutBlock
  | PlaylistBlock
  | TrackBlock
  | DividerBlock
  | CtaBlock;

type BaseBlock = {
  id: string;
  type: string;
  hidden?: boolean;
};

type HeroBlock = BaseBlock & {
  type: "hero";
  title: string;
  subtitle?: string;
  image_path?: string;
  audio_path?: string;
};

type HeadingBlock = BaseBlock & {
  type: "heading";
  level: 2 | 3 | 4;
  text: string;
};

type RichTextBlock = BaseBlock & {
  type: "rich_text";
  markdown: string;
};

type CalloutBlock = BaseBlock & {
  type: "callout";
  tone: "note" | "tip" | "warning" | "promo";
  title?: string;
  markdown: string;
};

type PlaylistBlock = BaseBlock & {
  type: "playlist";
  title?: string;
  description?: string;
  layout: "compact" | "cards" | "broadcast";
  tracks: PlaylistTrackRef[];
  show_labels?: boolean;
  show_descriptions?: boolean;
  show_duration?: boolean;
};

type TrackBlock = BaseBlock & {
  type: "track";
  track: PlaylistTrackRef;
  commentary?: string;
  show_player?: boolean;
};

type DividerBlock = BaseBlock & {
  type: "divider";
};

type CtaBlock = BaseBlock & {
  type: "cta";
  label: string;
  href: string;
  tone?: "primary" | "secondary";
};

type PlaylistTrackRef = {
  path: string;
  title: string;
  artist?: string;
  duration?: number;
  mime?: string;
  labels?: string[];
  description?: string;
  source_url?: string;
};
```

### Why blocks instead of markdown shortcodes

Markdown shortcodes like `[playlist id="..."]` are tempting but weak:

- hard to validate;
- hard to diff visually;
- hard for agents to mutate safely;
- hard to render as accessible interactive audio;
- easy to break when track paths contain punctuation.

Blocks give us typed validation, page-builder UI affordances, and stable agent/tool edits.

## Broadcast Studio playlist artifact

Broadcast Studio should distinguish:

| Artifact | Purpose |
|----------|---------|
| Queue | Ephemeral now-playing list |
| Saved playlist | Reusable studio artifact, currently `.m3u` plus metadata |
| Playlist publication draft | CMS/OpenPlanner document with block data |
| Published playlist page | Public garden-visible rendered page |

### Saved playlist metadata

For `.m3u` compatibility, keep saving M3U files, but add a companion JSON manifest when creating publishable playlists:

```json
{
  "schema": "knoxx.broadcast.playlist.v1",
  "playlist_id": "playlist_2026_05_07_brain_damage_transmissions",
  "title": "Brain Damage Transmissions",
  "description": "A late-night bumper pack of glitch sermons and ambient drops.",
  "created_at": "2026-05-07T00:00:00.000Z",
  "created_by": "...",
  "tracks": [
    {
      "path": "Audio/broadcasts/brain_damage_anthem.mp3",
      "title": "Brain Damage Anthem",
      "duration": 42.1,
      "labels": ["broadcast", "spoken word"],
      "description": "Heard description from the audio-context pipeline."
    }
  ]
}
```

The manifest is a source artifact. The publication draft embeds a snapshot of track refs so public pages remain stable even if the studio queue changes.

## Publish flow

### From Broadcast Studio

```text
Current queue / saved playlist
  -> "Create publication draft"
  -> choose publication kind: playlist | show_notes
  -> choose garden
  -> generate block draft:
       hero
       rich_text intro
       playlist block with track refs
       optional callout/cta
  -> open in CMS block editor
  -> review/edit
  -> publish to garden
```

### From CMS

```text
CMS block editor
  -> add playlist block
  -> choose saved playlist or current Broadcast Studio playlist
  -> edit heading/intro/track commentary
  -> publish to garden
```

## API surface proposal

### Playlist artifacts

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/studio/playlists` | GET | List saved playlists and manifests |
| `/api/studio/playlists` | POST | Save playlist with optional manifest metadata |
| `/api/studio/playlists/:id` | GET | Get playlist manifest + M3U path |
| `/api/studio/playlists/:id/publication-draft` | POST | Create CMS block publication draft from playlist |

### Block publications

Reuse `/api/openplanner/v1/cms/documents` where possible, adding block-aware payloads.

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/openplanner/v1/cms/documents` | POST | Create markdown or block document |
| `/api/openplanner/v1/cms/documents/:id` | PATCH | Update title/content/metadata.blocks |
| `/api/openplanner/v1/cms/documents/:id/blocks` | PUT | Replace ordered blocks |
| `/api/openplanner/v1/cms/documents/:id/blocks/:blockId` | PATCH | Update one block |
| `/api/openplanner/v1/cms/publish/:id/:garden_id` | POST | Publish block document to garden |
| `/api/openplanner/v1/gardens/:garden_id/publications/:slug` | GET | Public render payload |

First implementation can skip fine-grained block endpoints and use whole-document PATCH with `metadata.blocks`.

## UI proposal

### Broadcast Studio additions

Add a `Publish` panel or action near playlist controls:

- `Create publication draft`
- `Save playlist manifest`
- `Open in CMS`
- `Publish selected playlist...` should route to draft/review, not immediately public.

Draft wizard fields:

- title;
- slug;
- description/intro;
- garden;
- publication kind: playlist or show notes;
- layout: compact/cards/broadcast;
- toggles: show labels, show heard descriptions, show duration.

### CMS additions

Replace the single raw markdown body as the only editing surface with a dual-mode editor:

- `Markdown` tab: legacy body editor;
- `Blocks` tab: structured block list.

Block editor minimal controls:

- add heading;
- add text;
- add callout;
- add playlist;
- add track;
- move block up/down;
- hide/delete block;
- edit block fields.

The renderer should show blocks in order and fall back to markdown when no blocks exist.

## Renderer rules

Public renderer selects:

1. If `metadata.blocks` exists and `block_schema_version` is supported, render blocks.
2. Else render `content` as markdown.
3. Audio paths render through existing workspace media/audio stream route when allowed by publication visibility.
4. Track cards must include accessible controls and text alternatives.

Playlist block render variants:

| Layout | Behavior |
|--------|----------|
| `compact` | List of tracks with small play buttons |
| `cards` | Track cards with descriptions and labels |
| `broadcast` | Large now-playing style section with sequence/story notes |

## Agent interaction

Agents should mutate block documents through typed tools, not freeform markdown edits.

Potential tools:

```edn
{:tool/id "cms.publication.create_draft"}
{:tool/id "cms.publication.add_block"}
{:tool/id "cms.publication.update_block"}
{:tool/id "cms.publication.reorder_blocks"}
{:tool/id "cms.publication.publish"}
{:tool/id "broadcast.playlist.create_publication_draft"}
```

Policy:

- Agents may draft and propose blocks.
- Publishing requires explicit user confirmation.
- Mutating existing public publications should move status back to `review` unless the user explicitly republish-confirms.

## Migration plan

### Phase 0 — Spec and schema alignment

- Record this spec.
- Confirm current CMS publication endpoints and garden metadata behavior.
- Decide whether `metadata.blocks` lives directly in OpenPlanner event `extra.metadata` or under a stricter `extra.publication` namespace.

Verification:

- Spec accepted by operator.

### Phase 1 — Block renderer fallback

- Add frontend block renderer component.
- Update CMS document view to render blocks if present, markdown otherwise.
- Add unit tests for block renderer.

Verification:

- Existing markdown docs still render.
- A fixture block publication renders hero + text + playlist block.

### Phase 2 — Broadcast Studio draft creation

- Add `Create publication draft` button in Broadcast Studio for current queue/saved playlist.
- Build block draft client-side or via backend route.
- Create CMS/OpenPlanner document with `publication_kind=playlist` and `metadata.blocks`.
- Open CMS editor on the new document.

Verification:

- Queue of 2 tracks creates a CMS draft with a playlist block.
- Draft appears in CMS list and can be edited.

### Phase 3 — CMS block editor MVP

- Add Blocks tab beside markdown editor.
- Support heading, rich_text, playlist, track, callout.
- Move/delete/reorder blocks.
- Save blocks through CMS document PATCH.

Verification:

- Editor can create and persist blocks.
- Reload preserves block order and fields.

### Phase 4 — Garden public render

- Extend garden/public route to expose block payload.
- Render playlist publications as public pages.
- Enforce visibility/garden publication status.

Verification:

- Published playlist page is accessible via garden URL.
- Draft/review playlist page is not public.

### Phase 5 — Agent tools

- Add contract-gated tools for block mutation and playlist publication draft creation.
- Let Broadcast Studio/CMS agents propose structured blocks.
- Keep publish behind explicit confirmation.

Verification:

- Agent can draft a show-notes page from playlist + audio descriptions.
- Agent cannot publish without confirmation.

## First implementation slice

The smallest useful vertical slice:

1. Add `PublicationBlock` TypeScript types and `PublicationBlocksRenderer`.
2. Add a `Create publication draft` button in Broadcast Studio that creates a CMS document with:
   - title;
   - `metadata.publication_kind = "playlist"`;
   - `metadata.block_schema_version = 1`;
   - blocks: hero, rich_text intro, playlist.
3. Make CMS render block docs in preview/read mode, while still storing fallback markdown in `content`.
4. Do not build the full block editor yet; show the serialized block preview and allow raw fallback markdown editing.

## Open questions

1. Should playlist publication drafts be created by Knoxx backend routes or directly by frontend calls to OpenPlanner CMS endpoints?
2. Should saved playlist manifests live under `Music/playlists/`, `Audio/playlists/`, or a new `Broadcast/playlists/` root?
3. Should public audio stream URLs be garden-scoped signed URLs, or are workspace media routes acceptable for local-only MVP?
4. Should block documents remain `kind="docs"`, or should OpenPlanner add a separate `kind="publication"`?
5. Should a published playlist snapshot freeze labels/descriptions at publish time, or live-update from current graph/audio-context metadata?

## Recommended decisions

- Use `kind="docs"` for now with `metadata.publication_kind` to avoid broad OpenPlanner schema churn.
- Store block data under `metadata.blocks` for the MVP.
- Store playlist manifests under `Music/playlists/` unless the operator chooses a Broadcast-specific root.
- Freeze track order and paths at draft creation; allow descriptions/labels to be refreshed before publish.
- Render public pages from block snapshots, not live queue state.

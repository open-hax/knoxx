---
uuid: "knoxx-playlist-publication-label-provenance"
title: "Playlist Publication — Label Provenance in Block Metadata"
status: ready
priority: "P2"
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-29T00:00:00Z"
points: 3
category: "tasks"
---
# Playlist Publication — Label Provenance in Block Metadata

Parent epic: `knoxx-broadcast-studio-playlist-publication-and-block-cms`

## Problem

When a playlist is published as a CMS block document, label data is included on each track (`labels: string[]` in `PlaylistTrackRef`). However, there is no provenance record linking labels back to their graph IDs, nor any aggregate label summary at the publication level. This makes it impossible to query "which publications are tagged with label X" from the graph layer.

## Scope

### Files to change

- `frontend/src/lib/cms/publicationDrafts.ts` — add optional `label_ids: string[]` to `PlaylistPublicationTrackInput`; propagate into `PlaylistTrackRef` via `toTrackRef()`; add `label_provenance: Array<{ label: string; label_id: string }>` aggregate to `PlaylistPublicationDraft.metadata`
- `packages/gardens/publication-components/src/PublicationBlocksRenderer.tsx` (types only) — extend `PlaylistTrackRef` with `label_id?: string` field
- `frontend/src/pages/BroadcastStudioPage.tsx` — pass `item.labelIds` (graph label IDs) alongside `item.labels` when building the `tracks` array in `createPublicationDraftFromQueue()`; requires resolving label IDs from `allLabels` catalog by matching `label.label` strings to `label.label_id`
- `frontend/src/lib/cms/publicationDrafts.test.ts` — add test case asserting `label_provenance` is present in draft metadata when tracks have labels

### Out of scope

- Backend changes: the CMS document API stores metadata as-is; no schema change needed
- Graph edge writes at publication time (separate task)

## Definition of Done

- `buildPlaylistPublicationDraft({ tracks: [{ path: 'x', labels: ['jazz'], label_ids: ['label-42'] }] })` returns `metadata.label_provenance` containing `[{ label: 'jazz', label_id: 'label-42' }]`
- `publicationDrafts.test.ts` passes with the new provenance assertion
- `BroadcastStudioPage.tsx` `createPublicationDraftFromQueue()` maps `allLabels` catalog to resolve `label_ids` for each playlist item before calling `buildPlaylistPublicationDraft`
- `pnpm test` in `frontend/` passes with no new failures

---
Triage 2026-05-29: Concrete 3sp frontend-only task with explicit file-level scope and a clear, testable Definition of Done (label_provenance in draft metadata, passing Vitest assertions); no external blockers named and all required types/catalogs are in-repo. Verdict: accepted (P2). --tasks-dir orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---

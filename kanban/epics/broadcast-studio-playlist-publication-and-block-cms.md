---
uuid: "knoxx-broadcast-studio-playlist-publication-and-block-cms"
title: "Broadcast Studio Playlist Publication + Block CMS"
status: accepted
priority: "P2"
labels: "["epics"]"
created_at: "2026-05-28T22:40:14.377Z"
source: "specs/epics/broadcast-studio-playlist-publication-and-block-cms.md"
points: null
category: "epics"
---
# Broadcast Studio Playlist Publication + Block CMS

> Source: `specs/epics/broadcast-studio-playlist-publication-and-block-cms.md`

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

---
## Triage Notes (2026-05-28)

- **Status**: Keep as breakdown. Substantial 481-line spec, recent (May 7).
- **Action needed**: Split into child tasks (est. 3-5 tasks, each ≤5sp).
- **Key deliverables**:
  - Block CMS data model (blocks, pages, publications)
  - Playlist publication flow (CMS → garden → Studio layout)
  - Block renderer (audio, text, image, embed blocks)
  - Studio-like now-playing layout for published gardens
  - Label provenance tracking in published content
- **Blocked by**: CMS data model spec (`knowledge-ops-cms-data-model.md`)
- **Merged**: `garden-cms-playlist-chat-and-label-provenance.md` intent folded into this epic.

Breakdown 2026-05-29: Deep code inspection confirms substantial infrastructure already exists — BroadcastStudioPage.tsx (2575 lines) has a working `createPublicationDraftFromQueue()` handler; `publicationDrafts.ts` has `buildPlaylistPublicationDraft()` building block-schema CMS drafts; `@open-hax/garden-publication-components` has `PublicationBlocksRenderer` + `StudioPlaylistPlayer` (1081 lines across 7 files); CmsPage.tsx (1184 lines) renders block publications in the preview pane; `cms-templates.edn` contains `studio-playlist-page` template; `viewContract.ts` and `cms-block-registry.edn` are defined. What is NOT yet implemented: (1) label provenance field is absent from `publicationDrafts.ts` and the renderer type definitions — no `label_provenance` anywhere in frontend TS; (2) no dedicated published-garden viewer page/route using the broadcast layout — StudioPlaylistPlayer is only used in CmsPage preview; (3) the `broadcast` layout variant in PlaylistBlock is declared but PlaylistPublicationView delegates to StudioPlaylistPlayer for all layouts without distinction; (4) no e2e test covering the Studio→CMS draft→garden publish flow. Story points: 13sp (epic-level). Verdict: epic. Split into 3 child tasks each ≤5sp below. Blocked-by note: `knowledge-ops-cms-data-model` epic is `incoming` but the data model is already partially implemented in code — no hard code-level blocker found; the architectural flux described in the spec does not block these bounded deliverables. --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban

---
## Comment (2026-05-29)

Promoted to epic 2026-05-29: 13sp original split into subtasks. Icebox pending subtask completion.
---

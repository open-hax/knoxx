---
uuid: "knoxx-broadcast-studio-playlist-publication-and-block-cms"
title: "Broadcast Studio Playlist Publication + Block CMS"
status: breakdown
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.377Z"
source: "specs/epics/broadcast-studio-playlist-publication-and-block-cms.md"
points: null
category: epics
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

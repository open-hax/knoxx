---
uuid: "knoxx-published-garden-broadcast-viewer-route"
title: "Published Garden Broadcast Viewer Route"
status: ready
priority: "P2"
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-05-29T00:00:00Z"
points: 5
category: "tasks"
---
# Published Garden Broadcast Viewer Route

Parent epic: `knoxx-broadcast-studio-playlist-publication-and-block-cms`

## Problem

`StudioPlaylistPlayer` and `PublicationBlocksRenderer` exist and work inside the CmsPage preview pane, but there is no publicly accessible garden viewer route that renders a published playlist block document in the broadcast layout. Published content can only be previewed internally via CmsPage. There is no `/garden/:gardenId/doc/:docId` or similar read-only viewer that consumers can navigate to.

## Scope

### Files to create

- `frontend/src/pages/GardenDocumentPage.tsx` — new page component that:
  1. Fetches `/api/openplanner/v1/gardens/:gardenId` to resolve garden metadata
  2. Fetches `/api/openplanner/v1/cms/documents/:docId` to load the block document
  3. Extracts blocks via `extractPublicationBlocks(doc.metadata)` from `@open-hax/garden-publication-components`
  4. Renders `<PublicationBlocksRenderer>` with `getAudioUrl` wired to `/api/studio/stream?path=...`
  5. Shows a fallback markdown `<ReactMarkdown>` when no blocks are present
  6. Route: `/garden/:gardenId/doc/:docId`

### Files to modify

- `frontend/src/cljs/knoxx/frontend/app_routes.cljs` — register the new route `/garden/:gardenId/doc/:docId` → `GardenDocumentPage`
- `frontend/src/pages/CmsPage.tsx` — after successful publish toggle, add a "View published" link that navigates to `/garden/:gardenId/doc/:docId` using the resolved `cmsDocId` and `selectedGardenId`
- `packages/gardens/publication-components/src/PublicationBlocksRenderer.tsx` — ensure the `broadcast` layout value in `PlaylistBlock.layout` is handled as a distinct branch (currently delegates to `StudioPlaylistPlayer` via `PlaylistPublicationView` regardless of layout; add an explicit `data-layout="broadcast"` CSS class path that uses the existing `.publication-blocks__broadcast-hero` CSS already defined in the inline style block)

### Out of scope

- SSR / server-side rendering of garden pages (handled by separate garden worker package)
- Authentication/access control on the viewer (public gardens are already gated at the API level)

## Definition of Done

- Navigating to `/garden/<id>/doc/<docId>` in the running app renders a playlist block document with `StudioPlaylistPlayer` audio controls
- The route is registered in `app_routes.cljs`
- CmsPage shows a "View published" link after successful publish when `cmsDocId` is set
- `pnpm typecheck` in `frontend/` passes with no new errors
- `pnpm build` in `frontend/` produces no new bundle errors

---
Triage 2026-05-29: Task is concrete and implementation-ready at 5sp — one new GardenDocumentPage component, one route registration in app_routes.cljs, one CmsPage "View published" link, and a broadcast layout CSS class fix in PublicationBlocksRenderer, with all dependencies (StudioPlaylistPlayer, PublicationBlocksRenderer, publication APIs) already in place. Verdict: accepted (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---

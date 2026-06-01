---
uuid: "knoxx-studio-publication-e2e-smoke"
title: "Studio Publication Flow — E2E Smoke Test"
status: ready
priority: "P2"
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-29T00:00:00Z"
points: 3
category: "tasks"
---
# Studio Publication Flow — E2E Smoke Test

Parent epic: `knoxx-broadcast-studio-playlist-publication-and-block-cms`

## Problem

The Studio → CMS draft → garden publish flow has no automated end-to-end coverage. `publicationDrafts.test.ts` tests the draft builder in isolation, but there are no tests covering:
- The `createPublicationDraftFromQueue()` handler in BroadcastStudioPage
- The CmsPage render path when `block_schema_version: 1` metadata is present
- The `PublicationBlocksRenderer` with a playlist block containing audio tracks

## Scope

### Files to create or extend

- `frontend/src/lib/cms/publicationDrafts.test.ts` — extend existing tests: (a) assert `slugifyPublicationTitle` handles unicode/emoji; (b) assert `buildPlaylistPublicationDraft` with zero tracks still returns a valid draft with an empty playlist block; (c) assert `content` markdown fallback includes track titles
- `frontend/src/pages/CmsPage.test.tsx` — add a test that renders `<CmsPage>` with mocked fetch returning a CMS document whose `metadata.block_schema_version === 1` and `metadata.blocks` contains a hero + playlist block; assert `data-testid="publication-blocks-renderer"` appears in the output
- `packages/gardens/publication-components/src/PublicationBlocksRenderer.test.tsx` (already exists) — extend with a test case for the `playlist` block type with `layout: "broadcast"` asserting the `data-layout="broadcast"` attribute is set on the wrapping section element

### Files to check first

- `frontend/src/pages/CmsPage.test.tsx` — read current test setup to reuse mocking patterns
- `packages/gardens/publication-components/src/PublicationBlocksRenderer.test.tsx` — read existing tests

### Out of scope

- Playwright/AVA e2e against a running backend (requires integration environment)
- Testing the BroadcastStudioPage component directly (2575 lines, expensive to mount)

## Definition of Done

- `pnpm test` in `frontend/` passes with new CmsPage.test.tsx assertion on `publication-blocks-renderer` testid
- `pnpm test` in `packages/gardens/publication-components/` passes with broadcast layout test case
- `publicationDrafts.test.ts` passes with zero-track and unicode-title edge case assertions
- No existing tests regress

---
Triage 2026-05-29: Bounded 3sp subtask with concrete files identified, clear DoD (three test suites must pass), and no external dependencies — all referenced test files already exist and only need extension. Verdict: accepted (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---

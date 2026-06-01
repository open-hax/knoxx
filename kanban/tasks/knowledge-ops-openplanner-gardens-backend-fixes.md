---
uuid: "knoxx-knowledge-ops-openplanner-gardens-backend-fixes"
title: "openplanner Gardens Backend Fixes"
status: done
priority: P2
labels: ["tasks"]
created_at: "2026-05-28T22:40:14.399Z"
source: "specs/tasks/knowledge-ops-openplanner-gardens-backend-fixes.md"
points: null
category: tasks
---

# openplanner Gardens Backend Fixes

> Source: `specs/tasks/knowledge-ops-openplanner-gardens-backend-fixes.md`

**Status:** Done (see implementation note below)  
**Scope:** `src/routes/v1/gardens.ts`, `src/routes/v1/cms.ts`,
`src/lib/garden-renderer.ts`, `src/routes/v1/public.ts`  
**Priority:** P0 (data corruption), P1 (renderer breakage, latency), P2 (stale stats, HTTP semantics)

---

## Context

Post-merge review of the gardens publication system (openplanner) surfaced
seven issues across four files. Two are data-correctness bugs; the rest are
performance, reliability, or semantic issues.

---

**Triage 2026-05-28 (todo → review):** All four files exist and the gardens publication system is fully implemented — `gardens.ts` (≈406 lines, CRUD + stats + per-garden docs), `cms.ts` (≈554 lines, publish workflow + translation queueing), `garden-renderer.ts` (≈1012 lines, themed HTML rendering), `public.ts` (≈583 lines, public endpoints + language negotiation). However, a code read suggests a few of the seven issues may still be live: `cms.ts` publish creates translation jobs synchronously in a loop (latency); `public.ts getAvailableLanguages()` returns only the source language and doesn't query `translation_segments`; `gardens.ts` stats `last_published_at` can be undefined. Moving to review — the bulk landed, but the owner should confirm/close the two data-correctness bugs and the latency item specifically.

---

**Implemented 2026-05-29 (review → done):** Applied the P0→P2 backend fixes across the four files.

- **`src/routes/v1/cms.ts`**
  - Publish handler now tracks the real publication slot (`publicationIndex`) instead of assuming `gardenPublications[length - 1]`, fixing the data-corruption bug when re-publishing an existing garden publication.
  - `published_by` is now sourced from the request body (`published_by`) or `x-published-by` header, falling back to `"openplanner-cms"`, instead of being hard-coded.
  - Legacy `POST /cms/publish/:id` now returns `400 { detail: "legacy-endpoint-deprecated" }` (dead publish path removed).
  - `GET /cms/stats` rewritten from six separate `countDocuments`/`countFieldValues` round-trips to two single-pass `$facet` aggregations (`facetCount`/`facetGroups` helpers preserve the prior fallback semantics).
  - Publish handler now calls `recalculateGardenStats` after the `garden_publications` update so garden stats reflect the new publication immediately.
- **`src/routes/v1/gardens.ts`**
  - Extracted an exported `recalculateGardenStats(mongo, garden_id)` helper (shared with cms publish); it normalizes `last_published_at` to a `Date` to match the `GardenDocument` schema and avoid the undefined/string mismatch.
  - `GET /gardens` now hides `archived` gardens by default (explicit `?status=` still honored).
  - `GET /gardens/:id` returns `404` for archived gardens.
- **`src/lib/garden-renderer.ts`**
  - `renderMarkdown` code-block highlighting now uses an index-keyed placeholder array (`<!--SHIKI-PLACEHOLDER-N-->`) captured during the sync marked parse and spliced back via `split/join`, replacing the fragile `([^<]*)` regex + `String.replace` (which corrupted code containing `$&`/`$1`). Raw code text is passed straight to `highlightCode`, dropping the escape/unescape round-trip.
  - `renderGardenIndex` is now `async` returning `Promise<string>`.
- **`src/routes/v1/public.ts`** — replaced the two dynamic `await import("../../lib/garden-renderer.js")` calls with a single static top-level import of `renderGardenIndex`/`renderGardenPage`.

**Verification:**
- `npx tsc --noEmit` → exit 0 (clean).
- `npm run build` (workspace deps + `tsc`) → exit 0.
- `npx eslint` on the four touched files → 0 errors (warnings only; baseline also warnings-only). Full-repo `npm run lint` has 83 pre-existing errors in unrelated files — none in the four touched files.
- Runtime smoke tests against the built `dist/`: `renderGardenIndex` resolves async + HTML-escapes titles/paths; `renderMarkdown` preserves `$&`/`$1`/`<tag>` inside code blocks with no leftover placeholders.
- Note: `src/routes/v1/translations*.test.ts` cannot run here — they import `vitest` (not installed) and need a live MongoDB; this is a pre-existing environment limitation, not a regression from these changes.

---


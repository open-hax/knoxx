---
uuid: "knoxx-knowledge-ops-graph-weaver-live-sync-truth"
title: "Knowledge Ops — Graph-Weaver Live Sync Truth"
status: done
priority: P1
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-04-05T00:00:00Z"
source: "specs/tasks/knowledge-ops-graph-weaver-live-sync-truth.md"
points: 5
category: tasks
---

# Knowledge Ops — Graph-Weaver Live Sync Truth

> Source: `specs/tasks/knowledge-ops-graph-weaver-live-sync-truth.md`
> Parent: `knowledge-ops-graph-memory-reconciliation.md`
> Points: 5

Date: 2026-04-05
Status: done
Parent: `knowledge-ops-graph-memory-reconciliation.md`
Story points: 5

## Purpose

Ensure Graph-Weaver in `openplanner-graph` mode reflects current canonical OpenPlanner graph state rather than stale persisted state.

## Problem

The live local stack currently shows a contradiction:

- OpenPlanner graph export is empty
- Graph-Weaver reports tens of thousands of nodes/edges

---

**Implemented (2026-05-29):** Surfaced the local-source rebuild health (`localSync`) as first-class truth in the Graph-Weaver UI so the stale-vs-empty contradiction is visible rather than silent. The backend (`src/server.ts`) already tracked `localSync` (ok/mode/error/lastSuccessfulAt/lastAttemptAt/prunedOverlayNodes) per rebuild and returned it from `getStatus()`, but it was not exposed via GraphQL or the dashboard.

Changes (scoped to `packages/graph/graph-weaver`):
- `src/graphql.ts`: added a `LocalSync` GraphQL type and a `localSync: LocalSync!` field on `Status`; extended the `getStatus` return type in `GraphQLState` so TypeScript reflects the already-returned field.
- `public/app.js` (loadStatus): query now fetches `localSync { ok error mode lastAttemptAt }`; when `localSync.ok === false` the status line appends `⚠ STALE: OpenPlanner sync failed (<error>)` and toggles the `sync-failed` class.
- `public/style.css`: added `.status.sync-failed` (amber text on translucent red background, bordered) for the stale-sync warning.

**Verification:**
- `tsc -p tsconfig.json` → exit 0 (clean).
- `pnpm build` (incl. prebuild of webgl-graph-view + graph-weaver-aco) → exit 0; compiled `dist/graphql.js` contains the `LocalSync`/`localSync` schema.
- Runtime GraphQL check: `buildSchema(...)` parsed and `graphql({ query: "{ status { nodes edges localSync { ok error mode lastAttemptAt } } }" })` resolved with no errors, returning the failure state (`ok:false`, `error:"export empty"`, `mode:"openplanner-graph"`).
- No unit tests exist for this package; build + runtime schema query are the verification path.

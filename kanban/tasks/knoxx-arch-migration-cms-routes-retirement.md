---
uuid: "knoxx-arch-migration-cms-routes-retirement"
title: "Architecture Migration — Retire Legacy CMS Backend Routes"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Architecture Migration — Retire Legacy CMS Backend Routes

> Parent epic: `knoxx-knowledge-ops-architecture-migration`
> Points: 3

## Purpose

Remove or gate the legacy CMS route registrations in the knoxx backend that are superseded by the new domain-vertical CMS slice, preventing duplicate route conflicts and reducing dead code surface during the architecture migration.

## Scope

- Audit `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` for any CMS-prefixed route handlers (`/cms/*`) that belong to the old flat routing style
- For each legacy handler: either delete it outright (if the new domain slice covers it) or replace it with a `400 { detail: "legacy-endpoint-deprecated" }` tombstone (matching the pattern already used in `src/routes/v1/cms.ts` in openplanner)
- Confirm the contract kinds `:cms-block-registry`, `:cms-templates`, `:cms-template-registry` in `backend/src/cljs/knoxx/backend/law/contracts.cljs` still resolve correctly after removals
- Run `pnpm lint` and `pnpm typecheck` from `backend/` to confirm no broken requires

## Definition of done

- No two route handlers register the same CMS path pattern in the backend
- Any removed handler that previously had callers in `frontend/src/` is replaced by a tombstone or the frontend call site is updated to point at the new route
- `pnpm lint` exits zero; `pnpm typecheck` compiles cleanly

## Notes

Split from parent epic `knoxx-knowledge-ops-architecture-migration` on 2026-05-30.

---
uuid: "knoxx-tenant-org-scope-runs-memory"
title: "Tenant Isolation: Org-Scope Enforcement on Runs and Memory Routes"
status: "blocked"
priority: "P2"
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-29T00:00:00Z"
points: 3
category: "tasks"
---

# Tenant Isolation: Org-Scope Enforcement on Runs and Memory Routes

> Parent epic: `knoxx-knowledge-ops-mvp-phase1-epics` (Epic 1, Stories 1.2 / 1.5)

## Problem

Run listings (`/api/runs/*`) and memory lookups (`/api/memory/*`) currently return process-global results regardless of the resolved org context. Two memberships in different orgs see each other's runs and memory entries.

## Goal

Filter run and memory query results by the resolved `ctx-org-id` so cross-org data is invisible at the API layer.

## Affected Files

- `backend/src/cljs/knoxx/backend/infra/routes/memory.cljs` — add org-id filter to all list/search handlers
- `backend/src/cljs/knoxx/backend/infra/db/policy.cljs` — confirm or add `list-runs-for-org!` / `list-memory-for-org!` helpers as needed
- `backend/test/cljs/knoxx/backend/memory_routes_test.cljs` — add test asserting org-B cannot see org-A memory entries

## DoD

- `GET /api/memory/*` with org-A context returns zero results when all memory entries belong to org-B (unit test in `memory_routes_test.cljs`)
- `GET /api/runs/*` equivalent org-scope filter in place
- `pnpm -C backend test` passes
- `pnpm -C backend lint` passes
- Depends on: `knoxx-tenant-fail-closed-route-guard` being done first (ctx is non-nil when these handlers run)

---
Triage 2026-05-29: Task is a well-scoped 3sp slice with concrete DoD (org-scope filters on /api/runs/* and /api/memory/* with passing unit tests), but its own DoD explicitly requires knoxx-tenant-fail-closed-route-guard to be done first so that ctx-org-id is guaranteed non-nil when these handlers run — that dependency is currently still in incoming. Verdict: blocked (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---

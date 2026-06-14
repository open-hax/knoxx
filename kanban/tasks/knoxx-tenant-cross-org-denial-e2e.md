---
uuid: "knoxx-tenant-cross-org-denial-e2e"
title: "Tenant Isolation: Cross-Org Denial E2E Test Suite"
status: ready
priority: "P2"
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-29T00:00:00Z"
points: 2
category: "tasks"
---
# Tenant Isolation: Cross-Org Denial E2E Test Suite

> Parent epic: `knoxx-knowledge-ops-mvp-phase1-epics` (Epic 1, Story 1.5)

## Problem

`test/cljs/knoxx/backend/authz_test.cljs` covers only unit-level ctx accessor logic. There are no negative path tests proving cross-org listing denial, tool-policy deny enforcement, or unauthenticated request rejection on protected routes.

## Goal

Add a denial-path test namespace covering the critical cross-org and auth boundary cases so the isolation guarantees are CI-verified.

## Affected Files

- `backend/test/cljs/knoxx/backend/infra_authz_isolation_test.cljs` — new file (or extend `authz_test.cljs`)
- Tests should use existing test fixtures from `open_hax_policy_test.cljs` / `policy_actor_test.cljs` as reference patterns

## DoD

- Test: `GET /api/memory/*` with org-B token returns no org-A entries (asserts empty list, not 403)
- Test: `GET /api/admin/orgs` with a `knowledge_worker` membership returns 403
- Test: tool execution request with a deny-effect policy returns 403
- Test: request with no auth headers to any protected route returns 401
- All four assertions live in a single `deftest` suite that runs in `pnpm -C backend test`
- Depends on: `knoxx-tenant-fail-closed-route-guard`, `knoxx-tenant-org-scope-runs-memory`, `knoxx-tenant-policy-backed-tool-authz`

---
Triage 2026-05-29: Task is well-scoped (2sp, 4 concrete denial-path assertions, clear DoD) but all three named implementation dependencies — knoxx-tenant-fail-closed-route-guard, knoxx-tenant-org-scope-runs-memory, knoxx-tenant-policy-backed-tool-authz — are still in incoming, so the routes and policy enforcement being tested do not exist yet. Verdict: blocked (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---

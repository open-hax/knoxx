---
uuid: "knoxx-tenant-fail-closed-route-guard"
title: "Tenant Context: Fail-Closed Route Guard Middleware"
status: ready
priority: "P2"
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-29T00:00:00Z"
points: 3
category: "tasks"
---
# Tenant Context: Fail-Closed Route Guard Middleware

> Parent epic: `knoxx-knowledge-ops-mvp-phase1-epics` (Epic 1, Story 1.2)

## Problem

`infra/auth/authz.cljs` has `resolve-request-context!` and `with-request-context!`, but 85 route handlers across `routes/documents.cljs`, `routes/memory.cljs`, `routes/tools.cljs`, and `routes/admin.cljs` use `(when ctx (ensure-permission! ...))`. This is fail-open: when context resolution returns `nil` (no policy DB configured, or unresolved user), the `when` short-circuits and the request proceeds unauthenticated.

## Goal

Replace the `(when ctx ...)` pattern with a fail-closed equivalent that returns 401 when context is nil on protected routes.

## Affected Files

- `backend/src/cljs/knoxx/backend/infra/auth/authz.cljs` — add `require-context!` helper that throws/returns 401 when ctx is nil
- `backend/src/cljs/knoxx/backend/infra/routes/documents.cljs` — replace `(when ctx ...)` calls
- `backend/src/cljs/knoxx/backend/infra/routes/memory.cljs` — replace `(when ctx ...)` calls
- `backend/src/cljs/knoxx/backend/infra/routes/tools.cljs` — replace `(when ctx ...)` calls

## DoD

- `authz/require-context!` added — throws `{:status 401 :detail "authentication_required"}` when ctx is nil
- All `(when ctx (ensure-permission! ...))` occurrences in `routes/documents.cljs`, `routes/memory.cljs`, and `routes/tools.cljs` replaced with `(require-context! ctx) (ensure-permission! ...)`
- `pnpm -C backend test` passes
- `pnpm -C backend lint` passes with no new warnings
- A curl to any protected route with no auth headers returns HTTP 401 (not 200/500)

---
Triage 2026-05-29: Concrete security hardening task (fail-open to fail-closed auth) with clearly scoped affected files, a specific helper to add (`authz/require-context!`), and measurable DoD including passing tests, lint, and a curl verification — no blockers or missing decisions. Verdict: accepted (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---

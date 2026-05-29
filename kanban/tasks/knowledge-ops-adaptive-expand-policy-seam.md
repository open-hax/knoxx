---
uuid: "knoxx-knowledge-ops-adaptive-expand-policy-seam"
title: "Knowledge Ops — Adaptive Expand Policy Seam"
status: done
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-04-05T00:00:00Z"
source: "specs/tasks/knowledge-ops-adaptive-expand-policy-seam.md"
points: 2
category: tasks
---

# Knowledge Ops — Adaptive Expand Policy Seam

> Source: `specs/tasks/knowledge-ops-adaptive-expand-policy-seam.md`
> Parent: `knowledge-ops-adaptive-expand-policy-hook.md`
> Points: 2

Date: 2026-04-05
Status: done
Parent: `knowledge-ops-adaptive-expand-policy-hook.md`
Story points: 2

## Purpose

Introduce the smallest internal seam needed to swap graph expansion policy without changing the agent-facing graph query contract.

## Problem

Future adaptive traversal needs a place to plug in, but today that policy seam is not explicit. If adaptive behavior lands directly in query handlers, later experimentation will create contract churn and hidden coupling.

## Goals

---

**Implemented 2026-05-29 — graph expansion policy seam landed.**

Introduced an explicit, swappable policy seam for graph expansion bounds without changing the agent-facing graph query contract (output equivalence preserved).

New files:
- `backend/src/cljs/knoxx/backend/domain/graph/expansion_policy.cljs` — `IGraphExpansionPolicy` protocol with `bounded-search-params`, `bounded-expand-params`, `bounded-preview-params`, `bounded-writeback-params`; `DefaultGraphExpansionPolicy` record + `default-expansion-policy` constructor matching the legacy in-handler bounds (search `k` 1..12 default 7, `fetch-k` up to 3x capped at 36; expand `limit` 1..60 default 15, edge cost `1/edge-limit` only when edge-limit supplied; preview `limit` 1..12; writeback `batch-size` 1..16 default 3). Pure domain layer, no I/O.
- `backend/src/cljs/knoxx/backend/domain/graph/policy_registry.cljs` — `register-policy!` / `get-policy` (with `:default` fallback that never returns nil) / `policy-ids` / `init!`. Initialized with the default policy at namespace load and again at startup.

Edited:
- `infra/openplanner/memory.cljs` — `openplanner-memory-search!` and `openplanner-graph-query!` now resolve `(policy-registry/get-policy)` and use `bounded-search-params` / `bounded-expand-params` instead of inline `(max 1 (min …))` clamps. Bounds are identical to the prior literals; the edge-cost `maxCost` is still only set when `edge-limit` is truthy.
- `infra/routes/memory.cljs` — `memory-search-route!` resolves the search policy to bound `k` at the route boundary (idempotent with the handler-side bound); `memory-session-by-id-route!` honors an optional `?limit=` query param bounded by `bounded-preview-params`, defaulting to the prior full-row response when no limit is supplied.
- `bootstrap.cljs` — calls `graph-policy-registry/init!` at startup to register the default policy.

Verification (clean):
- `pnpm -C backend typecheck` → `[:server] Build completed. (307 files, 52 compiled, 0 warnings, 1.71s)`.
- `pnpm -C backend lint` → `errors: 0` (new graph files have zero warnings; pre-existing baseline warnings unchanged — the `memory-search-route!` span warning predates this change at 32 lines).
- `pnpm -C backend test` → `Ran 449 tests containing 1319 assertions. 0 failures, 0 errors.` confirming output equivalence with the prior behavior.


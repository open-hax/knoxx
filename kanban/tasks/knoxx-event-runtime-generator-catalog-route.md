---
uuid: "knoxx-event-runtime-generator-catalog-route"
title: "Expose generator resources in /api/admin/config/events status route"
status: "blocked"
priority: "P3"
labels: ["tasks", "2sp"]
created_at: "2026-05-29T00:00:00Z"
source: "epics/events-agent-runtime-separation.md"
points: 2
category: "tasks"
---

# Expose generator resources in /api/admin/config/events status route

> Parent epic: `knoxx-events-agent-runtime-separation`
   > Depends on: `knoxx-generator-runtime-create` (must be done first)

## Context

The `domain/event/tools.cljs` `events-status-execute` function (line 51-65) already reads `(:generator resources)` and includes it in the status string: `", generators=" (count (:generator resources))`. However the admin route that backs this tool (`GET /api/admin/config/events`) may not yet populate the `:generator` key in the control catalog response. This task ensures the route and catalog response include generator resources consistently with how triggers, schedules, actions, and sources are already included.

## Work

1. Locate the admin route handler for `GET /api/admin/config/events` in `infra/routes/` (likely `infra/routes/admin.cljs` or `infra/routes/tools.cljs`).

2. Confirm the route calls `domain/control/catalog.cljs` `catalog` function and that the returned `:resources` map includes `:generator` resources (it should — `catalog-resource-kinds` at line 26 includes `:generator`).

3. If the route bypasses the catalog and hand-builds the resources map, update it to include `(resources-of-kind rows :generator)`.

4. Confirm `GET /api/admin/config/events` JSON response has a `resources.generator` array (may be empty before any generator EDN files exist, which is fine).

5. Update `domain/event/tools.cljs` `events-status-execute` if needed so the generators count is correctly sourced from the catalog response key (currently `(:generator resources)` — verify this key name matches the route response).

## Affected files

- `backend/src/cljs/knoxx/backend/infra/routes/admin.cljs` or equivalent events route file
- `backend/src/cljs/knoxx/backend/domain/event/tools.cljs` (verify/fix generator key access)

## Definition of Done

- `GET /api/admin/config/events` response JSON includes `control.resources.generator` array (empty array is acceptable if no generator EDN files exist)
- `domain/event/tools.cljs` events-status string correctly counts generators from that key
- `pnpm -C backend run typecheck` exits 0
- `pnpm -C backend lint` exits 0

---
Triage 2026-05-29: Task is well-scoped (2sp) with a concrete DoD, but explicitly depends on `knoxx-generator-runtime-create` completing first — that task is currently in `incoming` status, so the generator runtime scaffolding this route needs to expose does not yet exist. Verdict: blocked (P3). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---

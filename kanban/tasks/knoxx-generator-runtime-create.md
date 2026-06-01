---
uuid: "knoxx-generator-runtime-create"
title: "Create domain/generator/runtime.cljs and wire into event-runtime lifecycle"
status: ready
priority: "P2"
labels: ["tasks", "3sp"]
created_at: "2026-05-29T00:00:00Z"
source: "epics/events-agent-runtime-separation.md"
points: 3
category: "tasks"
---
# Create domain/generator/runtime.cljs and wire into event-runtime lifecycle

> Parent epic: `knoxx-events-agent-runtime-separation`

## Context

Generator resources are fully defined in the contract schema (`law/contracts.cljs` lines 203-210: `:generator/kind`, `:generator/driver`, `:generator/emits`) and validated in `domain/control/catalog.cljs` (`generator-violations` function at line 197). However `domain/generator/runtime.cljs` does not exist, and `infra/event_runtime.cljs` never starts or stops a generator runtime. Generator resources loaded from disk are catalogued but never activated.

## Work

1. Create `backend/src/cljs/knoxx/backend/domain/generator/runtime.cljs`
   - Follow the same lifecycle pattern as `domain/schedule/runtime.cljs`: `running?*` atom, `start! [config]`, `stop! []`, `status [config]`
   - `start!` loads generator resources via `resources/list-resource-ids-sync config :generator` and logs each, delegating to the driver registry if applicable
   - `stop!` resets state atoms
   - `status` returns `{:running @running?* :generators [...]}`

2. Require and wire in `infra/event_runtime.cljs`:
   - Add `[knoxx.backend.domain.generator.runtime :as generator-runtime]` to `:require`
   - Call `(generator-runtime/start! config)` in `start!` after `source-runtime/start!`
   - Call `(generator-runtime/stop!)` in `stop!`
   - Add `:generators (generator-runtime/status config)` to `status` return map

## Affected files

- `backend/src/cljs/knoxx/backend/domain/generator/runtime.cljs` (new file)
- `backend/src/cljs/knoxx/backend/infra/event_runtime.cljs` (add require + 3 call sites)

## Definition of Done

- `domain/generator/runtime.cljs` exists with `start!`, `stop!`, `status` arities matching `domain/schedule/runtime`
- `infra/event_runtime.cljs` requires and calls generator-runtime in `start!`, `stop!`, and `status`
- `pnpm -C backend run typecheck` exits 0 (shadow-cljs compile server)
- `pnpm -C backend lint` exits 0 (clj-kondo)
- `GET /api/admin/config/events` response includes `:generators` key in status map

---
Triage 2026-05-29: Freshly-split epic subtask (3sp) with fully specified work — new domain/generator/runtime.cljs modeled on existing domain/schedule/runtime.cljs pattern, plus three explicit call sites in infra/event_runtime.cljs; DoD is concrete and verifiable with lint/typecheck commands. No blockers. Verdict: accepted (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---

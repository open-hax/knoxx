---
uuid: "knoxx-action-agent-spawn-boundary-fix"
title: "Fix domain/action/start-agent-session domain-to-infra boundary crossing"
status: ready
priority: "P2"
labels: ["tasks", "3sp"]
created_at: "2026-05-29T00:00:00Z"
source: "epics/events-agent-runtime-separation.md"
points: 3
category: "tasks"
---
# Fix domain/action/start-agent-session domain-to-infra boundary crossing

> Parent epic: `knoxx-events-agent-runtime-separation`

## Context

`domain/action/start_agent_session.cljs` line 5 has a direct import of `knoxx.backend.infra.agent.runner` — a domain namespace importing infra. This is the one blended-runtime pattern that remains in the action dispatch path. The action should receive a `spawn!` callable via the actor context map, not reach into the infra layer directly.

The current call site (line 129):
```clojure
(agents-runner/spawn-direct! config {...})
```

The actor context map `ctx` already carries `:config`, `:event`, `:trigger`. Adding `:spawn-agent!` to it at the infra dispatch layer lets the domain action stay pure.

## Work

1. In `domain/event/dispatch.cljs` `actor-context` function (line 79-88): add `:spawn-agent!` key pointing to `agents-runner/spawn-direct!`. This requires adding `[knoxx.backend.infra.agent.runner :as agents-runner]` to dispatch's require — which is an infra import from the domain event dispatcher. Evaluate whether the spawn fn should instead be injected at the `event-runtime` level and threaded through config, or simply added to dispatch as a seam. Document the decision in a code comment.

2. In `domain/action/start_agent_session.cljs`:
   - Remove the `[knoxx.backend.infra.agent.runner :as agents-runner]` require
   - Replace `(agents-runner/spawn-direct! config {...})` with `((:spawn-agent! ctx) config {...})`
   - Add guard: if `:spawn-agent!` is nil in ctx, throw `(js/Error. "No :spawn-agent! in action context")`

3. Update `domain/action/invoke_agent.cljs` similarly if it uses a `run-agent!` ctx key (already in ctx at line 5 — verify it does not also import infra).

## Affected files

- `backend/src/cljs/knoxx/backend/domain/action/start_agent_session.cljs` (remove infra require, use ctx fn)
- `backend/src/cljs/knoxx/backend/domain/event/dispatch.cljs` (add :spawn-agent! to actor-context)
- `backend/src/cljs/knoxx/backend/domain/action/invoke_agent.cljs` (verify, may need same pattern)

## Definition of Done

- `domain/action/start_agent_session.cljs` has no import of `knoxx.backend.infra.agent.runner`
- `clj-kondo` reports no domain-to-infra boundary violations in the action namespace
- `pnpm -C backend run typecheck` exits 0
- `pnpm -C backend lint` exits 0
- Manual smoke: fire a trigger that uses `:actions/start-agent-session` via `POST /api/admin/triggers/:id/fire` and confirm an agent session spawns

---
Triage 2026-05-29: Freshly-split epic subtask with a concrete 3-file scope — remove the direct infra require from `domain/action/start_agent_session.cljs`, inject `:spawn-agent!` via the actor context map at the dispatch layer, and verify with clj-kondo + typecheck + smoke test. No external blockers; DoD is fully specified and workable today. Verdict: accepted (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---

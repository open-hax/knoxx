---
uuid: "knoxx-action-scope-injection"
title: "Implement scope injection in action registry"
status: done
priority: "P1"
labels: ["tasks", "5sp", "action-scope-and-pipeline-collapse"]
created_at: "2026-06-09T00:00:00Z"
source: "docs/design/action-scope-and-pipeline-collapse.md"
points: 5
category: "tasks"
---
# Implement Scope Injection in Action Registry

> Parent epic: `knoxx-action-scope-and-pipeline-collapse`
> **Blocked by:** `knoxx-rich-action-registry` — scope resolution reads action metadata from the rich registry

## Context

The `:action/scope` field exists in schemas (`open_hax/contracts/schema.cljs:282`) but is never resolved or injected at runtime. This task wires it up.

## Work

1. Define `:action/scope` shape:
   - Format: `{:action/scope {:actions [:actions/... :actions/...]}}`
   - Each entry in the `:actions` vector is a keyword referencing another registered action
   - Scope is **flat** (direct references only, no transitive resolution in Phase 1)

2. In `domain/action/registry.cljs`:
   - Call `get-scope-declaration` (from rich registry) to get the raw scope data
   - Add `resolve-scope` function:
     - Takes action-key
     - Calls `(get-scope-declaration registry action-key)` to get `{:actions [:actions/... :actions/...]}`
     - Resolves each action key to a bound fn: `(fn [ctx action] (run-action! ctx action))`
     - Returns map of `action-key -> bound-fn`
     - If scope declaration is absent, returns empty map
   - Add cycle detection:
     - Use DFS with `visiting` set + depth limit of 32
     - Throw `js/Error` with message including the cycle path if detected
   - **TODO (Phase 2):** Add memoization keyed by `(:contract/id action)` + content hash

3. Change `run-action!` dispatch signature:
   - From: `(fn [_ctx action] (:action/kind action))`
   - To: `(fn [ctx action] ...)` where ctx is `{:event :scope :actor}`

4. Update **all 9** existing `defmethod run-action!` implementations:
   - `:invoke/noop` (`registry.cljs`)
   - `:actions/noop` (`registry.cljs`)
   - `:actions/hello-world` (`registry.cljs`)
   - `:actions/start-agent-session` (`domain/action/start_agent_session.cljs`)
   - `:actions/start-agent` (`domain/action/start_agent_session.cljs` — alias)
   - `:actions/run-pipeline` (`domain/action/run_pipeline.cljs`)
   - `:invoke/agent` (`domain/action/invoke_agent.cljs`)
   - `:invoke/sub-agent` (`domain/action/invoke_sub_agent.cljs`)
   - `:default` (`registry.cljs`)

5. Update `action-map` to pass `:action/with` through as arguments

6. Update event dispatch (`domain/event/dispatch.cljs`) to:
   - Load the action resource for the trigger's `:trigger/action`
   - Build scope via `resolve-scope`
   - Build ctx map with `:event`, `:scope`, `:actor`

7. Update `domain/action/dispatch.cljs` (pipeline dispatch path) to pass compatible ctx

8. Update test files with direct `run-action!` calls

## Definition of Done

- `resolve-scope` resolves action keys to bound functions
- `run-action!` receives ctx with `:event`, `:scope`, `:actor`
- All existing actions updated to new signature
- `pnpm -C backend exec shadow-cljs compile test` passes
- `pnpm -C backend typecheck` passes

## Risks

- Signature change touches every action — easy to miss one
- Scope cycles could hang the runtime — must detect and throw

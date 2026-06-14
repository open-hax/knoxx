---
uuid: "knoxx-run-steps-action"
title: "Implement :actions/run-steps to replace pipeline runner"
status: done
priority: "P1"
labels: ["tasks", "5sp", "action-scope-and-pipeline-collapse"]
created_at: "2026-06-09T00:00:00Z"
source: "docs/design/action-scope-and-pipeline-collapse.md"
points: 5
category: "tasks"
---
# Implement :actions/run-steps to Replace Pipeline Runner

> Parent epic: `knoxx-action-scope-and-pipeline-collapse`
> **Blocked by:** `knoxx-action-scope-injection` — this task requires the scope map and new `run-action!` signature

## Context

Pipelines are just composed actions. This task creates a single `:actions/run-steps` action that replaces `infra/pipeline-runner.cljs`.

## Work

### Step Shape Mapping

Old pipeline step format:
```clojure
{:step/id "fetch"
 :step/contract "discord_deep_synthesis"
 :step/data {:context {...} :output {:key "..." :ttl "..."}}}
```

New action step format:
```clojure
{:action :actions/discord-read
 :with {:channel-id "..."}}
```

### Implementation

1. In `domain/action/registry.cljs`, add `defmethod run-action! :actions/run-steps`:
   - Read `:steps` from `(:action/with action)` — a vector of `{:action :actions/... :with {...}}`
   - Steps are assumed **pre-ordered** (caller responsible for dependency ordering)
   - For each step:
     - Resolve action from `(:scope ctx)` — throw if not found in scope
     - Merge step's `:with` into a fresh action map
     - Invoke `(run-action! ctx step-action)`
   - Stop on first error: resolve with `{:ok false :error ... :failed-step ...}` (matches existing action convention of resolving rather than rejecting)
   - Support `:output` key in `(:action/with action)` for writing final result to temp memory

   **Note:** Steps are assumed **pre-ordered** (caller responsible for dependency ordering). Verify whether existing pipeline contracts have `:step/depends-on` annotations — if so, the pipeline migration task must preserve ordering in converted steps.

2. Migrate temp-memory interpolation:
   - Move `resolve-temps` from `infra/pipeline-runner.cljs` to `infra/temp_memory.cljs` (I/O boundary)
   - Rewrite as `^:async` using bare `await`
   - Keep `interpolate-map` in `shape/pipeline.cljs` (pure morphology)
   - `run-steps` calls `(await (temp-memory/resolve-temps step-with-map))` then `(shape.pipeline/interpolate-map step-with-map resolved)`

3. Handle `:agent` steps:
   - Old pipeline runner has special `:agent` step handling via `discord-io/start-agent-session!`
   - New approach: `:agent` steps become `{:action :actions/start-agent-session :with {...}}`
   - Ensure `start-agent-session` accepts the same context keys that pipeline-runner currently extracts (`:channelId`, `:channelName`, `:authorUsername`, `:content`, `:reason`)

4. Add characterization test:
   - Mock scope with two noop actions
   - `run-steps` invokes them in order
   - Second step receives output from first (via temp memory interpolation)
   - Test that error in step 2 prevents step 3 from running

## Definition of Done

- `:actions/run-steps` executes sequential steps from scope
- Temp-memory interpolation works with `^:async` + `await`
- Steps stop on first error (reject Promise)
- `:agent` pipeline steps work via `:actions/start-agent-session`
- Test passes
- `pnpm -C backend exec shadow-cljs compile test` passes
- `pnpm -C backend typecheck` passes

## Risks

- Temp-memory interpolation must preserve existing pipeline behavior exactly
- Agent steps in pipelines need context compatibility with `start-agent-session`
- Error behavior changes: old pipeline runner silently continued; new one stops on error

> **SUPERSEDED** by `resource-architecture.md` (2026-06-10). This document is retained for historical reference only.

# Action Scope, Pipeline Collapse, and Composable Actions

> Status: Draft
> Created: 2026-06-09
> Repo: `packages/agents/knoxx`

## Goal

Collapse the pipeline concept into the action concept so that:
1. **Actions are the universal unit of execution** — anything that "does something" is an action.
2. **Pipelines are just composed actions** — a sequence of action invocations is itself an action.
3. **Actions receive a scope catalog** — bound references to other registered actions available in the calling context.
4. **Trigger arguments are action arguments** — `:action/with` on triggers passes arguments into the action.

This eliminates the parallel pipeline runtime (`infra/pipeline-runner.cljs`) and consolidates all execution through the action registry (`domain/action/registry.cljs`).

## Problem

The codebase currently maintains two parallel execution models:

1. **Actions** (`domain/action/registry.cljs`) — single-shot functions dispatched by `:action/kind`.
2. **Pipelines** (`infra/pipeline-runner.cljs`) — multi-step sequences with temp-memory interpolation, dependency ordering, and separate agent/action step dispatch.

This duplication means:
- Two ways to define ordered execution (pipeline resource vs. action composition)
- Two dispatch paths to maintain
- Trigger `streamingBehavior` cannot be expressed because the action registry doesn't support arguments or composition
- `:action/scope` is defined in schemas (`open_hax/contracts/schema.cljs:282`) but never wired to anything

## Intended Model

### Action Signature

```clojure
(defn my-action
  [ctx action]
  ...)
```

Where:
- `ctx` — execution context map containing:
  - `:event` — the triggering event (or nil for direct invocation)
  - `:scope` — a map of `action-key -> bound-fn`, containing actions this action is allowed to call
  - `:actor` — the actor context (identity, permissions, credentials)
  - Plus any additional context (e.g., `:conversation-id`, `:config`)
- `action` — the action map containing:
  - `:with` — runtime arguments (from trigger `:trigger/with` or caller)

### Action Resource Shape

```clojure
{:contract/kind :action
 :contract/id "ussyverse-social-reply"
 :action/id :actions/start-agent-session
 :action/kind :actions/start-agent-session
 
 ;; The scope catalog: what other actions are available in this action's scope.
 ;; Each entry resolves to a bound function in the runtime scope map.
 :action/scope
 {:actions [:actions/agent-control
            :actions/noop]}
 
 ;; Malli schema for argument validation
 :action/params
 [:map
  [:agent-id :string]
  [:message :string]
  [:sticky-session? {:optional true} :boolean]
  [:streaming-behavior {:optional true} [:enum :steer :follow-up :queue :drop]]]}
```

### Trigger Resource Shape (unchanged semantics, richer arguments)

```clojure
{:contract/kind :trigger
 :contract/id "ussyverse_social_replies_event"
 :trigger/kind :event
 :trigger/listener "discord_automation"
 :trigger/events [:discord.message]
 :trigger/action :actions/start-agent-session
 :trigger/with
 {:agent-id "ussyverse_social_replies"
  :message "A Discord event fired..."
  :sticky-session? true
  :streaming-behavior :steer
  :session-max-messages 1000}}
```

### Scope Injection

When `run-action!` dispatches, it builds the `scope` map by:
1. Calling `get-scope-declaration` on the action registry to get the raw scope data
2. Resolving each `:actions/...` key to a bound fn: `(fn [ctx action] (run-action! ctx action))`
3. Injecting the scope map into `ctx` as `:scope`

This means actions can call other actions without knowing how they're registered.

### Pipeline Collapse

A "pipeline" becomes just another action:

```clojure
{:contract/kind :action
 :contract/id "deep-synthesis"
 :action/id :actions/run-steps
 :action/kind :actions/run-steps
 :action/scope
 {:actions [:actions/discord-read
            :actions/semantic-search
            :actions/synthesize
            :actions/discord-publish]}
 :action/params
 [:map [:steps :vector]]
 
 ;; The "pipeline" data lives in :action/with on the trigger
 :data
 {:steps
  [{:action :actions/discord-read :with {:channel-id "..."}}
   {:action :actions/semantic-search :with {:query "{{previous.output}}"}}
   {:action :actions/synthesize}
   {:action :actions/discord-publish :with {:channel-id "..."}}]}}
```

Wait — this mixes resource definition (the action) with invocation data (the steps). The invocation data should come from `:trigger/with`.

Actually, the cleaner model:
- The action resource declares its shape and scope
- The trigger passes the specific data via `:trigger/with`
- A generic `:actions/run-steps` action iterates over `:steps` from `action/with` and invokes each one from scope

### Solving streamingBehavior

The `streamingBehavior` problem you hit is solved naturally:

```clojure
;; In the trigger
:trigger/with
{:streaming-behavior :steer   ;; or :follow-up, :queue, :drop
 :sticky-session? true}

;; In :actions/start-agent-session
(defn start-agent-session
  [ctx action]
  (let [{:keys [streaming-behavior sticky-session?]} (:action/with action)
        conversation-id (:conversation-id ctx)
        agent-session (active-agent-session conversation-id)]
    (cond
      ;; Session active + steer/follow-up requested
      (and agent-session (streaming? agent-session)
           (#{:steer :follow-up} streaming-behavior))
      (try
        ((get-in ctx [:scope :actions/agent-control])
         (assoc ctx :action/with {:kind (name streaming-behavior)
                                  :message (get-in ctx [:event :event/payload :content])}))
        (catch :default err
          ;; TOCTOU fallback: session finished between check and call
          (if (str/includes? (.-message err) "No active running turn")
            (spawn-direct! ctx action)
            (throw err))))

      ;; Drop
      (= streaming-behavior :drop)
      {:ok false :dropped true}

      ;; Queue or default — spawn directly
      :else
      (spawn-direct! ctx action))))
```

`:actions/agent-control` is a single parameterized action that handles both steer and follow-up, avoiding duplicate event logging and WebSocket broadcast logic.

## Migration Plan

### Phase 0: Rich Action Registry (Prerequisite)

1. Refactor action registry from simple multimethod to rich registry with metadata
   - Support `:action/tool` metadata (name, description, parameters)
   - Support `:action/events` input/output contracts
   - Support `:action/scope` declarations
2. Connect tool registry to action registry — generate tool definitions from action metadata
3. Maintain backward compatibility with existing `defmethod run-action!`

**Verification:** All existing actions registered with metadata; tool registry reads from action registry

### Phase 1: Implement Scope Injection in Action Registry

1. Add `get-scope-declaration` protocol method (returns raw scope data)
2. Add `resolve-scope` function — wraps `get-scope-declaration`, returns bound fns
   - Scope is **flat** (direct references only, no transitive resolution in Phase 1)
   - Cycle detection: DFS with `visiting` set + depth limit 32
3. Change `run-action!` signature to `(ctx action)` where ctx includes `:event`, `:scope`, `:actor`
4. Update all existing `defmethod run-action!` implementations to accept new signature
5. Update `action-map` in `domain/action/registry.cljs` to pass `:action/with` through

**Verification:** `pnpm -C backend exec shadow-cljs compile test` — all existing tests still pass with shimmed scope

### Phase 2: Implement `:actions/run-steps` (Pipeline as Action)

1. Create `defmethod run-action! :actions/run-steps`
   - Reads `:steps` from `(:action/with action)`
   - Iterates steps, resolves each action from scope, invokes sequentially
   - Supports temp-memory interpolation via `{{memory.temp:key}}` (migrate from pipeline-runner)
   - Supports `:output` key for writing results
   - Steps are assumed pre-ordered (caller responsible for dependency ordering)

**Verification:** Existing pipeline tests pass using new `:actions/run-steps`

### Phase 3: Migrate Pipeline Resources to Action Resources

1. Convert `contracts/pipelines/*.edn` to `contracts/actions/*.edn`
2. Move `:pipeline/steps` to `:action/with {:steps [...]}` and `:output` to `:action/with`
3. Preserve step ordering from existing contracts
4. Update triggers that reference `:actions/run-pipeline` to pass `:pipeline-id` as `:action/with {:steps [...]}`
5. Update `:actions/run-pipeline` to delegate to `:actions/run-steps` with deprecation warning

**Verification:** `contracts/control_config_test.cljs` and pipeline tests pass

### Phase 4: Implement Streaming Behavior as Action Argument

1. Add `:actions/agent-control` action (parameterized by `:kind` — "steer" or "follow_up")
2. Update `:actions/start-agent-session` to read `:streaming-behavior` from `:action/with` and dispatch to `:actions/agent-control` from scope
3. Implement TOCTOU fallback: catch "not streaming" error → spawn new session
4. Move `streamingBehavior`, `stickySession`, `sessionMaxMessages` out of agent contracts and into trigger `:action/with`

**Verification:** Triggered runs with sticky sessions work correctly; manual smoke test

### Phase 5: Cleanup

1. Delete `infra/pipeline-runner.cljs`
2. Delete `:actions/run-pipeline`
3. Update `AGENTS.md` and design docs
4. Remove pipeline resource kind from schemas

## Affected Files

- `backend/src/cljs/knoxx/backend/domain/action/registry.cljs` — rich registry, scope injection, new signature
- `backend/src/cljs/knoxx/backend/infra/registry/tools.cljs` — read from action registry instead of hardcoded map
- `backend/src/cljs/knoxx/backend/domain/tools.cljs` — build tools from action metadata
- `backend/src/cljs/knoxx/backend/domain/action/run_pipeline.cljs` — delegate to run-steps
- `backend/src/cljs/knoxx/backend/infra/pipeline_runner.cljs` — migrate to run-steps, then delete
- `contracts/pipelines/*.edn` — convert to action resources
- `contracts/triggers/*.edn` — move invocation data to `:trigger/with`
- `backend/src/cljs/open_hax/contracts/schema.cljs` — action/scope schema validation
- `backend/src/cljs/knoxx/backend/domain/action/start_agent_session.cljs` — read streaming-behavior from args
- `contracts/agents/ussyverse_social_replies.edn` — remove streamingBehavior, stickySession
- `contracts/agents/ussyverse_social_creative.edn` — same
- `docs/design/action-registry-intent.md` — update with scope/pipeline collapse

## Risks

- Changing `run-action!` signature affects all existing actions — must update every defmethod
- Scope cycles could cause infinite loops — need cycle detection or depth limit
- Temp-memory interpolation in pipelines must be preserved during migration
- Some pipeline steps currently invoke `:agent` steps directly (start-agent-session) — need to ensure this still works via scope
- Rich registry refactor is high blast radius — all actions break if registry fails

## Definition of Done

- [ ] `run-action!` accepts `(ctx action)` with `:scope` map
- [ ] `:action/scope` on action resources is resolved and injected
- [ ] `:actions/run-steps` replaces pipeline runner
- [ ] All pipeline contracts migrated to action contracts
- [ ] `streamingBehavior` works via trigger `:action/with` arguments
- [ ] `pnpm -C backend exec shadow-cljs compile test` passes
- [ ] `pnpm -C backend typecheck` passes
- [ ] Manual smoke: trigger with sticky session + streamingBehavior :steer queues correctly

---
Triage 2026-06-09: This epic consolidates two parallel execution models (action and pipeline) into one, solves the streamingBehavior gap for event-triggered sticky sessions, and activates the dormant `:action/scope` schema. Bounded scope: 5 files for registry changes, ~4 pipeline contracts to migrate, 2 agent contracts to clean. Estimated 13sp. No external blockers. Verdict: accepted (P1/P2 boundary — P1 if sticky session fix is blocking, P2 otherwise). --tasks-dir .

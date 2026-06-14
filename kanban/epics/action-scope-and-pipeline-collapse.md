---
uuid: "knoxx-action-scope-and-pipeline-collapse"
title: "Resource Architecture: Namespace Files, Composite Resources, and Action Scope"
status: done
priority: "P1"
labels: "[\"epics\",\"actions\",\"pipeline\",\"scope\",\"resources\"]"
created_at: "2026-06-09T00:00:00Z"
source: "docs/design/resource-architecture.md"
points: null
category: "epics"
---
# Resource Architecture: Namespace Files, Composite Resources, and Action Scope

> Source: `docs/design/resource-architecture.md`
> Replaces: `event-trigger-action-runtime.md`, `action-scope-and-pipeline-collapse.md`

Date: 2026-06-10
Status: done (phases 0–12 implemented and verified 2026-06-10)
Repo: `packages/agents/knoxx`

## Goal

One execution model. One resource shape. No blended classes.

- Namespace files with `:namespace` + `:resources` replace individual `:contract/id` files
- Composite resources: single entry can be trigger + action + store simultaneously
- Anonymous actions via `:action/fn` inline
- `:trigger/with` is the sole argument mechanism
- `:action/scope` includes actions, filters, and stores
- Store protocol (`IStore`) for keyed persistence

## What's Already Done

Verified against code as of 2026-06-10:

- ✅ Rich action registry (`register-action!`, `get-tool`, `get-scope-declaration`, `list-tools`)
- ✅ Flat scope resolution (`resolve-scope` in `domain/action/registry.cljs`)
- ✅ `:actions/run-steps` for pipeline composition (temp-memory interpolation, error stopping)
- ✅ `:actions/agent-control` for steer/follow-up (TOCTOU fallback)
- ✅ Pipeline contracts migrated to action contracts (`contracts/actions/`)
- ✅ `pipeline_runner.cljs` deleted
- ✅ `run_pipeline.cljs` deprecated (delegates to run-steps)
- ✅ Multimethod dispatch with `:default` bridge to registered handlers

## Phases 6–12 (implemented 2026-06-10)

- ✅ **Phase 6 — Namespace resource loader**: namespace files (`:namespace` + `:resources`) expand through `domain/resources/namespace_file.cljs` + `parse-contract-file-records!`; qualified ids (`:ussyverse/social-replies`); individual `:contract/id` files still load.
- ✅ **Phase 7 — Composite resources**: one entry can be trigger + action + store; the loader emits one record per interpreter kind present and each interpreter reads only its own keys.
- ✅ **Phase 8 — Anonymous actions**: `:action/fn` compiles via `domain/action/anonymous.cljs` (fn values pass through; EDN `(fn [ctx action] ...)` forms interpret against a whitelisted pure-function set, fail closed). Never registered, never discoverable.
- ✅ **Phase 9 — `:trigger/with` sole mechanism**: normalize folds legacy `:trigger/agent`/`:trigger/task` into `:trigger/with`; `action-map` passes it as `:action/with`; live trigger contracts migrated.
- ✅ **Phase 10 — Store protocol**: `IStore` (`-insert`/`-find`) in `infra/store/protocol.cljs`; `MemoryCollection` default backend; `MongoCollection` wraps an injected native handle via `extern/mongo.cljs`; Malli guard in `law/store.cljs`; instances cached in `infra/store/registry.cljs`. Store instances are callable: `(store query)`.
- ✅ **Phase 11 — Scope expansion**: `interpreter/resolve-scope-decl` resolves `{:actions [...] :filters [...] :stores [...]}` into a flat scope map injected as `(:scope ctx)`; filter registry at `domain/filter/registry.cljs`.
- ✅ **Phase 12 — Dead field cleanup**: dead fields dropped from both schema registries and all contract EDN files; `contracts/AGENTS.md` rewritten for the new architecture.

New runtime entry point: `domain/action/interpreter.cljs` — `execute!` resolves
inline `:action/fn` → registered kinds → EDN action resources (by `:action/id`),
injecting scope before dispatch. Event dispatch routes through it.

### Follow-up (optional)

- Migrate remaining individual contract files to namespace format
  (`contracts/namespaces/ussyverse.edn` is the exemplar).
- `:action/fn` interpreter has no `await`; async composition stays in
  registered actions / `:actions/run-steps`.
- `MongoCollection` awaits a runtime that injects a real collection handle —
  no Mongo driver dependency was added.

## Affected Files

- `backend/src/cljs/knoxx/backend/domain/resources/loader.cljs` — namespace file loading
- `backend/src/cljs/knoxx/backend/domain/action/registry.cljs` — scope expansion, anonymous actions
- `backend/src/cljs/knoxx/backend/domain/trigger/normalize.cljs` — `:trigger/with` sole mechanism
- `backend/src/cljs/knoxx/backend/domain/event/dispatch.cljs` — composite resource handling
- `backend/src/cljs/open_hax/contracts/schema.cljs` — dead field removal, namespace schema
- `backend/src/cljs/knoxx/backend/law/contracts.cljs` — same
- `contracts/**/*.edn` — migrate to namespace format
- New: `backend/src/cljs/knoxx/backend/infra/store/` — IStore protocol, MongoCollection

## Risks

- Namespace file format is new — needs schema definition and loader changes
- Composite resources need clear interpreter separation
- Anonymous actions (`:action/fn`) need eval/compile strategy
- Store protocol needs MongoDB implementation
- Migration from individual files to namespace files is a large contract rewrite

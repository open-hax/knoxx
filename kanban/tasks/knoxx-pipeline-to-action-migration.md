---
uuid: "knoxx-pipeline-to-action-migration"
title: "Migrate pipeline resources to action resources"
status: done
priority: "P2"
labels: ["tasks", "5sp", "action-scope-and-pipeline-collapse"]
created_at: "2026-06-09T00:00:00Z"
source: "docs/design/action-scope-and-pipeline-collapse.md"
points: 5
category: "tasks"
---
# Migrate Pipeline Resources to Action Resources

> Parent epic: `knoxx-action-scope-and-pipeline-collapse`
> **Depends on:** `knoxx-run-steps-action`

## Context

Four pipeline contracts exist under `contracts/pipelines/`. They need to become action contracts. Steps live on the **action resource**; triggers only pass invocation-specific runtime params.

## Work

### 1. Convert pipeline contracts to action contracts

For each of:
- `contracts/pipelines/deep_synthesis_pipeline.edn`
- `contracts/pipelines/patrol_pipeline.edn`
- `contracts/pipelines/mentions_pipeline.edn`
- `contracts/pipelines/synthesis_pipeline.edn`

Changes:
- Change `:contract/kind` from `:pipeline` to `:action`
- Add `:action/id` and `:action/kind` (use `:actions/run-steps`)
- Move `:pipeline/steps` to `:action/with {:steps [...]}` (using new step shape)
- Migrate `:output` key from pipeline contract to `:action/with` (e.g., `{:output {:key "..." :ttl "..."}}`)
- Preserve step ordering — if existing steps have `:step/depends-on`, maintain the order in the new `:steps` vector
- Add `:action/scope` with relevant actions (Discord I/O, temp-memory, etc.)

### 2. Update triggers

Production triggers to update:
- `contracts/triggers/deep_synthesis_trigger.edn`
- `contracts/triggers/patrol_trigger.edn`
- `contracts/triggers/mentions_trigger.edn`
- `contracts/triggers/daily_synthesis.edn`

Changes:
- Change `:trigger/action` from `:actions/run-pipeline` to `:actions/run-steps`
- Change `:trigger/with` from `{:pipeline-id "..."}` to `{:action-id "..."}` (or let `:trigger/action` point directly to the action kind)
- Keep invocation-specific context in `:trigger/with` or `:data {:context ...}`

Fixture triggers to update:
- `backend/test/fixtures/hello-world-contracts/triggers/patrol_trigger.edn`
- `backend/test/fixtures/hello-world-contracts/triggers/deep_synthesis_trigger.edn`
- `backend/test/fixtures/hello-world-contracts/triggers/daily_synthesis_trigger.edn`

### 3. Update `:actions/run-pipeline`

In `domain/action/run_pipeline.cljs`:
- Load the action resource by pipeline-id
- Delegate to `:actions/run-steps` with the loaded steps
- Log deprecation warning: `"actions/run-pipeline is deprecated; use actions/run-steps directly"

### 4. Schema changes

In `backend/src/cljs/open_hax/contracts/schema.cljs`:
- Add `:action/with` to `ActionContract` (optional map, closed false)
- Deprecate `PipelineStep` and `PipelineContract` (keep for compatibility, mark deprecated)

In `backend/src/cljs/knoxx/backend/law/contracts.cljs`:
- Same changes as schema.cljs
- Update `infer-contract-class` to map `:pipeline` to `:action` for compatibility

### 5. Registry cleanup

In `backend/src/cljs/knoxx/backend/domain/registry/resource.cljs`:
- Deprecate `:registry/pipelines` and `pipelines-registry`
- Remove or mark with deprecation comment

### 6. Frontend cleanup

In `frontend/src/cljs/knoxx/frontend/pages/agents.cljs`:
- Remove pipeline-aware React logic (`pipeline-agent-targets`, `trigger->schedule-jobs`, `list-contracts "pipelines"`)
- Update to use action resources instead

### 7. Test updates

- `backend/test/cljs/knoxx/backend/domain/action/run_pipeline_test.cljs` — rewrite for `:actions/run-steps`
- `backend/test/cljs/knoxx/backend/pipeline_runner_test.cljs` — update terminology
- `backend/test/cljs/knoxx/backend/contracts/loader_test.cljs` — update pipeline class assertions

## Definition of Done

- All 4 pipeline contracts converted to action contracts
- Production and fixture triggers updated
- `:actions/run-pipeline` delegates with deprecation warning
- Schemas updated
- Registry cleaned up
- Frontend pipeline logic removed
- All tests pass
- `pnpm -C backend exec shadow-cljs compile test` passes
- `pnpm -C backend typecheck` passes

## Risks

- Frontend changes are out of scope for backend-only developers — may need separate frontend task
- Moving steps from resource to trigger invocation changes where edits happen
- Steps live on action resource; triggers pass runtime params — this separation must be documented clearly

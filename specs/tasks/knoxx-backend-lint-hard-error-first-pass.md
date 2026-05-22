# Knoxx Backend Lint — Hard Error First Pass

Date: 2026-05-21
Status: todo
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Reduce the lint failure from a noisy backlog to a trustworthy queue by fixing diagnostics that can create cascades or hide real compile failures.

## Problem

`pnpm -C backend lint` currently reports 176 errors. The highest-leverage errors are unresolved symbols/namespaces/vars, arity mismatches, and direct test access to private vars. These should be resolved before spending time on high-volume style warnings.

## Goals

1. Fix all concrete unresolved symbols not caused by the route `defroute` cascade.
2. Fix arity mismatches such as calls to `ensure-session-id` with the wrong argument count.
3. Fix or replace tests that call private vars directly.
4. Remove missing `:require` aliases that produce unresolved namespace warnings.
5. Rerun lint and record the new error count.

## Non-goals

1. Promise-chain migration except where it is required by a hard-error fix.
2. Function-length extraction except where it is required to make an error-local change readable.
3. Route macro/hook redesign; that has a dedicated task.
4. Adding `#_:clj-kondo/ignore` suppressions as the primary solution.

## Initial inventory

Known direct hard-error examples from the supplied lint output:

- `src/cljs/knoxx/backend/infra/core.cljs` — `prewarm-sdk-runtime!`
- `src/cljs/knoxx/backend/infra/core_memory.cljs` — `basename`, `normalize-devel-path`
- `src/cljs/knoxx/backend/infra/db/policy.cljs` — `ensure-bootstrap-allowlist-users!`
- `src/cljs/knoxx/backend/infra/routes/app.cljs` — `config`, `ensure-session-id` arity
- `src/cljs/knoxx/backend/infra/routes/documents.cljs` — `openplanner-graph-export!`
- `src/cljs/knoxx/backend/infra/routes/memory.cljs` — `lounge-messages*`
- `src/cljs/knoxx/backend/infra/stores/composite_session_store.cljs` — `list-active-runs`
- `src/cljs/open_hax/contracts/policy/fulfillment.cljs` — `match-params`
- `src/cljs/open_hax/contracts/policy/gate.cljs` — `match-params`
- `test/cljs/knoxx/backend/pipeline_runner_test.cljs` — private `dependency-order`, `interpolate-value`, `interpolate-map`
- `test/cljs/knoxx/backend/policy_actor_test.cljs` — private `default-membership-actor-id`
- `test/cljs/knoxx/backend/tools/temp_memory_test.cljs` — private `parse-ttl-ms`

## Implementation notes

- Prefer adding small public behavior-level functions only when they are real API seams.
- If a test needs a private helper, first consider testing through the public function that consumes it.
- If a helper is genuinely reusable across namespaces, move it into `shape.*`, `law.*`, or a specific non-`utils` namespace according to the architecture rules.
- For unresolved symbols, check whether the symbol was renamed, moved, or intentionally deleted before recreating it.

## Verification

Run at minimum:

```bash
pnpm -C backend lint
```

If any source behavior changes, also run:

```bash
pnpm -C backend exec shadow-cljs compile test
```

## Definition of done

- Non-route-cascade unresolved-symbol errors are gone or moved into a documented child task with exact locations.
- Arity mismatch errors are gone.
- Private-var test access errors are gone.
- A fresh lint run is recorded in the task notes or follow-up receipt.

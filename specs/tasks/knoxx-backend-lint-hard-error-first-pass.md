# Knoxx Backend Lint — Hard Error First Pass

Date: 2026-05-27
Status: review
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Reduce the lint failure from a noisy backlog to a trustworthy queue by fixing diagnostics that can hide real compile failures or make later refactors unsafe.

## Problem

`pnpm -C backend lint` initially reported 52 errors. Most were function-length errors, but the highest leverage first pass was the non-style error set: unresolved symbols, analyzer/type mismatches, and tests reaching into private vars.

After this pass, lint reports 33 errors, all currently function-length errors owned by `knoxx-backend-lint-function-length-extractions.md`.

These should be resolved before sweeping high-volume Promise-chain warnings, because they identify missing seams and stale tests.

## Goals

1. Fix all concrete unresolved symbols.
2. Fix analyzer/type mismatch errors in route code.
3. Fix or replace tests that call private vars directly.
4. Decide which function-length errors are safe to defer to the function-length task.
5. Rerun lint and record the new error count.

## Non-goals

1. Promise-chain migration except where required by a hard-error fix.
2. Function-length extraction except where required to make an error-local change readable.
3. Route macro/hook redesign beyond proving whether an error is real or hook drift.
4. Adding `#_:clj-kondo/ignore` suppressions as the primary solution.

## Current hard-error inventory

Non-function-length hard errors from the current lint run:

- `src/cljs/open_hax/contracts/policy/fulfillment.cljs:45` — unresolved `match-params`.
- `src/cljs/open_hax/contracts/policy/gate.cljs:43` — unresolved `match-params`.
- `src/cljs/knoxx/backend/infra/routes/memory.cljs:774` — analyzer reports expected `deref`, received function.
- `src/cljs/knoxx/backend/infra/routes/memory.cljs:790` — analyzer reports expected atom, received function.
- `test/cljs/knoxx/backend/pipeline_runner_test.cljs` — private `dependency-order`, `interpolate-value`, `interpolate-map`.
- `test/cljs/knoxx/backend/policy_actor_test.cljs` — private `default-membership-actor-id`.
- `test/cljs/knoxx/backend/tools/temp_memory_test.cljs` — private `parse-ttl-ms`.

Function-length errors are owned by `knoxx-backend-lint-function-length-extractions.md`, but any file touched here must not make length debt worse.

## Result — 2026-05-27

Resolved the non-length hard-error set:

- Fixed `match-params` calls in policy gate/fulfillment namespaces to call `match-params?`.
- Added `knoxx.backend.open-hax-policy-test` coverage for gate and fulfillment param matching, TTL filtering, strongest action selection, error-state matching, and message interpolation.
- Taught the `defroute` clj-kondo hook that `*`-suffixed extra deps are atom-like, clearing the `lounge-messages*` deref/swap analyzer errors.
- Moved pure pipeline ordering/interpolation seams into `knoxx.backend.shape.pipeline` and updated tests to stop reaching into private runner vars.
- Moved default membership actor-id resolution into `knoxx.backend.domain.actor.scope` and updated tests to use that public domain seam.
- Made `temp-memory/parse-ttl-ms` public because it is part of the documented TTL input contract for the temp-memory API.

Verification:

```bash
cd backend && pnpm exec clj-kondo --lint .clj-kondo/hooks \
  src/cljs/open_hax/contracts/policy/fulfillment.cljs \
  src/cljs/open_hax/contracts/policy/gate.cljs \
  src/cljs/knoxx/backend/shape/pipeline.cljs \
  src/cljs/knoxx/backend/infra/pipeline_runner.cljs \
  src/cljs/knoxx/backend/domain/actor/scope.cljs \
  src/cljs/knoxx/backend/infra/db/policy.cljs \
  src/cljs/knoxx/backend/infra/temp_memory.cljs \
  src/cljs/knoxx/backend/infra/routes/memory.cljs \
  test/cljs/knoxx/backend/open_hax_policy_test.cljs \
  test/cljs/knoxx/backend/pipeline_runner_test.cljs \
  test/cljs/knoxx/backend/policy_actor_test.cljs \
  test/cljs/knoxx/backend/tools/temp_memory_test.cljs
# remaining targeted error: memory-sessions-route! function length only

cd backend && pnpm run lint
# errors: 33, warnings: 1756; remaining errors are all function-length

pnpm -C backend exec shadow-cljs compile test
# 439 tests / 1286 assertions / 0 failures / 0 errors / 0 warnings

pnpm -C backend exec shadow-cljs compile server
# 0 warnings
```

## Implementation notes

- For `match-params`, determine whether the correct fix is a require, a move into a policy `shape/law` namespace, or restoring a deleted helper.
- For private-var tests, prefer public behavior assertions. If a helper encodes a reusable rule, move the rule to a real public domain/shape/law seam.
- For route analyzer errors, check whether clj-kondo is seeing the same macro expansion as Shadow. Fix real code first; fix hook modeling only if runtime code is valid.
- For unresolved symbols, check whether the symbol was renamed, moved, or intentionally deleted before recreating it.

## Verification

Targeted first:

```bash
pnpm -C backend exec clj-kondo --lint src/cljs/open_hax/contracts/policy/fulfillment.cljs src/cljs/open_hax/contracts/policy/gate.cljs
pnpm -C backend exec clj-kondo --lint src/cljs/knoxx/backend/infra/routes/memory.cljs
pnpm -C backend exec clj-kondo --lint test/cljs/knoxx/backend/pipeline_runner_test.cljs test/cljs/knoxx/backend/policy_actor_test.cljs test/cljs/knoxx/backend/tools/temp_memory_test.cljs
```

Batch gate:

```bash
pnpm -C backend lint
```

If any source behavior changes, also run:

```bash
pnpm -C backend exec shadow-cljs compile test
```

## Definition of done

- Non-length hard errors are gone or moved into a documented child task with exact locations.
- Private-var test access errors are gone.
- A fresh lint run is recorded in the task notes or follow-up receipt.
- No new warnings are introduced in touched files.

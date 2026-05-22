# Knoxx Backend CLJS Lint Remediation Epic

Date: 2026-05-21
Status: next
Source command: `pnpm -C backend lint`
Source log: `/tmp/pi-bash-dc1bfcd15722b998.log`
Story points: 34

## Purpose

Restore `pnpm -C backend lint` to Knoxx's zero-warning contract without hiding diagnostics, loosening lint policy, or papering over real runtime drift.

## Problem

The latest backend lint run exits `3` and reports:

- 176 errors
- 3347 warnings
- 2 info diagnostics

The failures are not one bug. They cluster into several work streams:

1. Unresolved symbols, unresolved namespaces, arity mismatches, and private-var test access are blocking correctness signals.
2. Several route namespaces appear to have `defroute`/route-context unresolved-symbol cascades.
3. Function-length lint is now an error for functions at or above 60 lines.
4. Raw `.then`/`.catch` promise chains dominate warning volume and conflict with the backend's modern CLJS style.
5. Unused bindings/requires/private vars dominate the remaining warning volume after promise chains.

## Goals

1. Make `pnpm -C backend lint` exit 0 with zero errors and zero warnings.
2. Preserve the four-category architecture split: `domain.*`, `infra.*`, `shape.*`, and `law.*`.
3. Fix correctness errors before high-volume style warnings.
4. Convert raw Promise chains to `promesa.core/p/let` or `^:async` + `await` where appropriate.
5. Split long functions by extracting named helpers with clear contracts rather than suppressing length checks.
6. Keep JS interop inside `knoxx.backend.extern.*` or existing low-level boundary adapters.
7. Leave Knoxx PM2 processes untouched unless a human explicitly approves a restart.

## Non-goals

1. Lowering clj-kondo severity or line-length thresholds to make the run green.
2. Broad feature rewrites unrelated to lint or compile correctness.
3. Reformat-only churn across unaffected namespaces.
4. Changing public route behavior except where required to fix genuine arity/symbol drift.
5. Restarting local or production Knoxx runtime processes.

## Current diagnostic map

Parsed from the supplied lint output:

| Category | Errors | Warnings | Notes |
|---|---:|---:|---|
| Unresolved symbol / namespace / var | 127 | 12 | Likely missing requires, macro hook drift, route context scope leaks, renamed vars, or stale tests. |
| Function length | 32 | 136 | Error threshold is `>=60`; warning threshold is `>=30`. |
| Private var access from tests | 15 | 43 | Tests need public seams or behavior-level assertions. |
| Arity mismatch | 2 | 0 | `ensure-session-id` calls are currently known examples. |
| Promise chain style | 0 | 1482 | Replace raw `.then`/`.catch` chains. |
| Unused binding/require/private var | 0 | 1605 | Mostly cleanup after hard errors are fixed. |
| Protocol mock completeness | 0 | 4 | Test reifies must implement required methods or use smaller protocols. |
| Redundant expressions | 0 | 8 | Mechanical cleanup. |

## Hotspots

Prioritize files with hard errors and high warning density:

- `backend/src/cljs/knoxx/backend/infra/routes/tools.cljs`
- `backend/src/cljs/knoxx/backend/infra/routes/mcp.cljs`
- `backend/src/cljs/knoxx/backend/infra/routes/actors.cljs`
- `backend/src/cljs/knoxx/backend/infra/routes/documents.cljs`
- `backend/src/cljs/knoxx/backend/infra/routes/studio.cljs`
- `backend/src/cljs/knoxx/backend/infra/routes/studio/discord_scan.cljs`
- `backend/src/cljs/knoxx/backend/infra/routes/memory.cljs`
- `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs`
- `backend/src/cljs/knoxx/backend/infra/routes/app.cljs`
- `backend/test/cljs/knoxx/backend/pipeline_runner_test.cljs`
- `backend/test/cljs/knoxx/backend/policy_actor_test.cljs`
- `backend/test/cljs/knoxx/backend/tools/temp_memory_test.cljs`

## Child tasks

1. `specs/tasks/knoxx-backend-lint-hard-error-first-pass.md`
2. `specs/tasks/knoxx-backend-lint-route-helper-symbols.md`
3. `specs/tasks/knoxx-backend-lint-function-length-extractions.md`
4. `specs/tasks/knoxx-backend-lint-promesa-migration-src.md`
5. `specs/tasks/knoxx-backend-lint-test-boundaries.md`
6. `specs/tasks/knoxx-backend-lint-unused-and-final-warnings.md`
7. `specs/tasks/knoxx-backend-lint-zero-warning-gate.md`

## Execution order

1. Run the hard-error first pass and rerun lint to collapse cascades.
2. Repair route macro/helper symbol drift, because it is likely responsible for many repeated unresolved names.
3. Fix remaining concrete unresolved symbols, arity mismatches, and private test API violations.
4. Split functions that exceed the error threshold.
5. Convert Promise chains in source namespaces, then tests.
6. Remove unused/redundant/protocol warnings.
7. Run the zero-warning gate and backend compile gates.

## Verification

Primary gate:

```bash
pnpm -C backend lint
```

Required final result:

- exit code 0
- zero errors
- zero warnings
- zero info diagnostics unless explicitly accepted by policy

Behavior gates after source changes:

```bash
pnpm -C backend exec shadow-cljs compile test
pnpm -C backend exec shadow-cljs compile server
```

## Definition of done

- All child tasks are completed or explicitly superseded.
- `pnpm -C backend lint` is green with no warnings.
- Relevant test/server compile gates pass after behavior-affecting changes.
- Any intentional public API seams added for tests are documented in the owning namespace or task notes.
- No lint suppression is introduced without an explicit task note explaining why suppression is the least-bad contract.

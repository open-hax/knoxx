# Knoxx Backend CLJS Lint, Coverage, and Architecture Remediation Epic

Date: 2026-05-27
Status: next
Source command: `pnpm -C backend lint`
Source log: `/tmp/knoxx-backend-lint-after-agent-template-extraction.log`
Test command: `pnpm -C backend test`
Story points: 40

## Purpose

Restore Knoxx backend lint to the zero-warning contract while improving test coverage, async architecture, and domain/shape/law boundaries. This is not a suppression campaign; it is a quality campaign that turns lint pressure into safer, better-factored backend code.

## Problem

The current backend command chain `pnpm -C backend lint && pnpm -C backend test` short-circuits at lint. Running `pnpm -C backend test` separately still passes, so the immediate blocker is lint quality, not the Shadow test runner.

Current evidence:

- After the second function-length extraction slice, `pnpm -C backend lint` exits non-zero with 31 errors and 1757 warnings; remaining errors are function-length only.
- `pnpm -C backend exec shadow-cljs compile test` passes 441 tests / 1294 assertions / 0 failures / 0 errors.
- Latest coverage is 57.35% lines, 42.10% branches, and 31.90% functions.
- The dominant warning class is raw Promise chaining, but older docs and linter text still point toward Promesa. Current intent is to steer new work toward `^:async` functions, `await`, named workflow stages, and context maps.

The failures are clustered across several architectural seams:

1. Long route, store, config, schema, and orchestration functions that hide multiple responsibilities.
2. Raw `.then` / `.catch` chains that bury control flow, error policy, and side effects in callbacks.
3. Some tests still warn on private-var access, but private-var hard errors have been moved to public seams.
4. Function-length errors now dominate the error set and should be fixed before broad style cleanup.
5. Warning-heavy files with low branch/function coverage, making naive refactors risky.

## Goals

1. Make `pnpm -C backend lint` exit 0 with zero errors and zero warnings.
2. Keep `pnpm -C backend test` and server compile gates green after behavior-affecting changes.
3. Raise or at least preserve backend coverage while remediating lint hotspots.
4. Replace raw Promise chains with named `^:async` / `await` workflows, not Promesa-by-default rewrites.
5. Split long functions by extracting named helpers with explicit category boundaries:
   - `domain.*` for pure decisions;
   - `shape.*` for shape transforms and schemas;
   - `law.*` for admissibility/guard contracts;
   - `infra.*` / boundary-specific `extern.*` for effects.
6. Add characterization tests before refactoring low-coverage, high-risk units.
7. Use the CLJS mutation harness for extracted pure logic where it gives useful signal.
8. Avoid Knoxx runtime restarts unless explicitly approved by a human.

## Non-goals

1. Lowering clj-kondo severity or line/function thresholds to make the run green.
2. Adding broad suppressions instead of addressing design issues.
3. Rewriting all routes or persistence modules in one pass.
4. Changing public route behavior except where current code is provably wrong.
5. Replacing every existing Promesa use; the goal is a current default style, not dependency churn.
6. Restarting PM2 or production/local runtime processes.

## Current diagnostic map

Parsed from `/tmp/knoxx-backend-lint-after-agent-template-extraction.log` after the second function-length extraction slice:

| Category | Errors | Warnings | Notes |
|---|---:|---:|---|
| Function length | 31 | 136 | Error threshold is `>=60`; warning threshold is `>=30`. |
| Promise chain style | 0 | 1501 | Linter guidance must point to `^:async` / `await` workflows. |
| Private var access | 0 | 35 | Remaining private-var diagnostics are warnings for later test-boundary cleanup. |
| Unresolved symbol | 0 | 0 | `match-params` hard errors fixed in first pass. |
| Analyzer/type mismatch | 0 | 0 | `lounge-messages*` route hook modeling fixed in first pass. |
| Unresolved namespace | 0 | 4 | Missing or stale requires. |
| Complexity | 0 | 2 | Secondary to extraction work. |
| Redundant let | 0 | 7 | Mostly mechanical cleanup. |
| Duplicate require | 0 | 1 | Mechanical cleanup in a touched test. |
| Other warnings | 0 | 71 | Clean after major refactors collapse cascades. |

## Coverage-informed hotspot map

The campaign should prioritize high lint density plus low coverage rather than highest warning count alone:

| File | Findings | Errors | Promise warnings | Line % | Branch % | Function % | Priority reason |
|---|---:|---:|---:|---:|---:|---:|---|
| `src/cljs/knoxx/backend/infra/db/policy.cljs` | 135 | 0 | 121 | 23.94 | 26.02 | 19.19 | Policy persistence is high-risk and under-covered. |
| `src/cljs/knoxx/backend/infra/routes/app.cljs` | 128 | 2 | 109 | 24.49 | 0.00 | 0.00 | Main chat/session route surface. |
| `src/cljs/knoxx/backend/infra/routes/memory.cljs` | 58 | 3 | 43 | 32.29 | 52.99 | 40.00 | Has hard errors and user-facing memory behavior. |
| `src/cljs/knoxx/backend/domain/bluesky/bluesky.cljs` | 54 | 0 | 53 | 40.08 | 0.00 | 0.00 | External posting/tool behavior. |
| `src/cljs/knoxx/backend/domain/discord/tools.cljs` | 52 | 0 | 43 | 41.15 | 0.00 | 0.00 | Tool side effects and Discord API behavior. |
| `src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 45 | 2 | 37 | 48.17 | 1.61 | 0.00 | Voice/listener runtime with long functions. |
| `src/cljs/knoxx/backend/infra/routes/studio.cljs` | 41 | 0 | 40 | 9.30 | 0.00 | 0.00 | Very low route coverage. |
| `src/cljs/knoxx/backend/infra/routes/tools.cljs` | 36 | 0 | 30 | 11.00 | 0.00 | 0.00 | Tool route behavior is almost uncharacterized. |
| `src/cljs/knoxx/backend/infra/stores/session_titles.cljs` | 35 | 1 | 33 | 43.62 | 35.80 | 25.00 | Async backfill and persistence side effects. |
| `src/cljs/knoxx/backend/domain/session_mycology.cljs` | 23 | 1 | 20 | 21.08 | 9.09 | 0.00 | Tool execution and receipt/spore side effects. |
| `src/cljs/knoxx/backend/infra/agent/turn.cljs` | 14 | 3 | 8 | 34.95 | 28.26 | 21.05 | Core agent turn lifecycle. |

## Architecture rules

1. Categories describe lawful movement; contracts decide admissibility. Do not confuse a shape extraction with a policy guard.
2. Put pure data transforms in `shape.*` only when they are domain-agnostic or shape-specific.
3. Put business decisions in `domain.*` and keep them I/O-free.
4. Put validation, guards, and state-transition admissibility in `law.*` or `contract/guard/*`.
5. Keep raw JS interop born and decoded in boundary-specific `extern.*` namespaces.
6. Do not create `utils` namespaces.
7. Do not make private helpers public solely for tests; expose a real seam or test through public behavior.
8. Do not convert Promise chains mechanically when they encode concurrency or fail-open/fail-closed policy.

## Child tasks

1. `specs/tasks/knoxx-backend-lint-hard-error-first-pass.md`
2. `specs/tasks/knoxx-backend-lint-coverage-characterization.md`
3. `specs/tasks/knoxx-backend-lint-route-helper-symbols.md`
4. `specs/tasks/knoxx-backend-lint-function-length-extractions.md`
5. `specs/tasks/knoxx-backend-lint-async-workflows-src.md`
6. `specs/tasks/knoxx-backend-lint-test-boundaries.md`
7. `specs/tasks/knoxx-backend-lint-unused-and-final-warnings.md`
8. `specs/tasks/knoxx-backend-lint-zero-warning-gate.md`

## Execution order

1. Fix hard lint errors that block trustworthy diagnostics: unresolved symbols, analyzer/type mismatches, and private-var test errors. Done for non-length errors on 2026-05-27.
2. Add coverage characterization around the first risky lint hotspots before moving behavior.
3. Repair route macro/helper lint modeling where it is still inaccurate, especially around `^:async` route bodies and `await` scope.
4. Split remaining function-length errors, extracting domain/shape/law/infra helpers according to responsibility.
5. Convert Promise-chain hotspots to named async workflows with tests preserving error and concurrency semantics.
6. Clean test-specific private API, async, and helper-length issues.
7. Remove final redundant, unused, complexity, and namespace warnings.
8. Run the zero-warning gate and record exact results.

## Verification

Primary final gate:

```bash
pnpm -C backend lint
```

Required after source behavior changes:

```bash
pnpm -C backend exec shadow-cljs compile test
pnpm -C backend exec shadow-cljs compile server
```

Coverage and mutation evidence for quality slices:

```bash
pnpm -C backend run test:coverage
cd backend && pnpm run mutation:smoke
```

Optional full mutation gate when runtime cost is acceptable:

```bash
cd backend && pnpm run test:mutation
```

## Acceptance criteria

- `pnpm -C backend lint` exits 0 with no warnings.
- `pnpm -C backend test` remains green.
- Backend server compile remains green for production-path changes.
- Coverage is preserved or improved for touched low-coverage hotspots.
- Linter Promise-chain messages direct contributors toward `^:async` / `await` workflows.
- Refactors include characterization tests before or alongside behavior movement.
- No broad lint suppressions are introduced without a written, file-specific justification.

## Definition of done

- All child tasks are completed or explicitly superseded.
- Final lint/test/server/coverage evidence is recorded in task notes or receipts.
- Any surviving mutation-test gaps from touched pure modules are either killed by tests or captured as follow-up work.
- No unverified claim of completion remains in the epic or task text.

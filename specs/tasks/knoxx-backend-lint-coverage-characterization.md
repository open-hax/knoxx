# Knoxx Backend Lint — Coverage-First Characterization

Date: 2026-05-27
Status: todo
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Before refactoring lint hotspots, add characterization tests around low-coverage units so lint cleanup improves design without erasing unknown behavior.

## Problem

`pnpm -C backend lint` is blocked by 52 errors and 1763 warnings, while the latest coverage run reports only 57.35% line coverage, 42.10% branch coverage, and 31.90% function coverage. Many of the warning-heavy files are also low-coverage route, policy, Discord, Bluesky, session, and persistence surfaces.

A lint-only campaign could make the code prettier while moving untested behavior. The work needs a coverage gate that answers: what behavior are we protecting before extraction?

## Goals

1. Join lint diagnostics with `backend/coverage/coverage-summary.json` before each lint-remediation slice.
2. Prioritize files where high lint density overlaps low branch/function coverage.
3. Add characterization tests for guards, parsing, fallback behavior, persistence transforms, and response envelopes before refactors.
4. Prefer pure helper tests and seam tests over route-heavy integration fixtures when they prove the risky behavior.
5. Use mutation smoke/default runs on refactored pure modules when feasible.
6. Record coverage deltas after each meaningful slice.

## Non-goals

1. Chasing coverage percentage by testing trivial getters or generated glue.
2. Building full end-to-end fixtures for every route before any refactor.
3. Requiring the unlimited mutation campaign for every lint slice.
4. Suppressing lint because a file lacks tests.

## Current coverage-informed hotspot map

Snapshot from the 2026-05-27 lint run plus the latest coverage summary:

| File | Lint findings | Errors | Promise warnings | Line % | Branch % | Function % | Why it matters |
|---|---:|---:|---:|---:|---:|---:|---|
| `src/cljs/knoxx/backend/infra/db/policy.cljs` | 135 | 0 | 121 | 23.94 | 26.02 | 19.19 | Policy persistence and authorization setup. |
| `src/cljs/knoxx/backend/infra/routes/app.cljs` | 128 | 2 | 109 | 24.49 | 0.00 | 0.00 | Main chat/session route surface. |
| `src/cljs/knoxx/backend/infra/routes/memory.cljs` | 58 | 3 | 43 | 32.29 | 52.99 | 40.00 | Memory/session browsing and ingestion routes. |
| `src/cljs/knoxx/backend/domain/bluesky/bluesky.cljs` | 54 | 0 | 53 | 40.08 | 0.00 | 0.00 | External social posting domain. |
| `src/cljs/knoxx/backend/domain/discord/tools.cljs` | 52 | 0 | 43 | 41.15 | 0.00 | 0.00 | Tool effects and external API interaction. |
| `src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 45 | 2 | 37 | 48.17 | 1.61 | 0.00 | Voice/listener runtime orchestration. |
| `src/cljs/knoxx/backend/infra/routes/studio.cljs` | 41 | 0 | 40 | 9.30 | 0.00 | 0.00 | Studio route behavior with very low coverage. |
| `src/cljs/knoxx/backend/infra/routes/tools.cljs` | 36 | 0 | 30 | 11.00 | 0.00 | 0.00 | Tool catalog/execution route surface. |
| `src/cljs/knoxx/backend/infra/stores/session_titles.cljs` | 35 | 1 | 33 | 43.62 | 35.80 | 25.00 | Session title backfill side effects. |
| `src/cljs/knoxx/backend/domain/session_mycology.cljs` | 23 | 1 | 20 | 21.08 | 9.09 | 0.00 | Receipt/spore tool behavior. |
| `src/cljs/knoxx/backend/infra/agent/turn.cljs` | 14 | 3 | 8 | 34.95 | 28.26 | 21.05 | Core agent turn lifecycle. |

## Characterization patterns

Use the narrowest test that protects behavior:

1. Pure shape/domain helper tests for parse/normalize/defaulting logic.
2. Protocol seam tests with small fake clients for persistence/network boundaries.
3. Route handler tests only when response status/body/header behavior is the contract.
4. Regression tests for lint refactors that touch fail-open/fail-closed policy behavior.
5. Mutation smoke/default runs for pure helpers with branch logic after extraction.

## Execution protocol

For each lint slice:

1. Run a targeted lint inventory for the candidate file.
2. Read coverage summary for that file.
3. Identify untested behaviors likely to move during refactor.
4. Add or extend tests first.
5. Refactor lint issues.
6. Run targeted lint and backend tests.
7. If behavior changed or branch logic was extracted, run coverage and mutation smoke where feasible.

## Verification

Minimum per slice:

```bash
pnpm -C backend exec clj-kondo --lint <touched-files>
pnpm -C backend exec shadow-cljs compile test
```

Coverage evidence after larger slices:

```bash
pnpm -C backend run test:coverage
```

Mutation evidence for extracted pure logic:

```bash
cd backend && pnpm run mutation:smoke
```

## Definition of done

- Every high-risk lint refactor has a pre-refactor or same-commit characterization test.
- Coverage deltas are recorded for touched hotspot files.
- The lint campaign does not reduce backend total line, branch, or function coverage without an explicit written reason.
- Surviving mutants in touched pure modules are either killed by new tests or recorded as follow-up gaps.

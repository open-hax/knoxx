# Knoxx Backend Lint — Test Boundaries and Async Cleanup

Date: 2026-05-27
Status: todo
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Make backend tests respect public API boundaries, modern async style, required protocol methods, and namespace requirements while preserving regression coverage.

## Problem

The lint run reports test-specific blockers and warnings:

- private-var access in `pipeline_runner_test.cljs`, `policy_actor_test.cljs`, and `tools/temp_memory_test.cljs`;
- raw `.then`/`.catch` Promise chains across many test files;
- incomplete protocol mocks/reifies in memory/message tests;
- missing requires such as `clojure.string`/`str` in some tests;
- long test helper functions such as `make-invoke-mcp-post!`.

## Goals

1. Replace private-var assertions with public behavior-level assertions or intentional public seams.
2. Convert test Promise chains to `deftest ^:async` with `await`, using named helpers when setup is complex.
3. Complete protocol mock implementations or narrow tests to smaller protocols.
4. Fix missing requires and unused test requires/refers.
5. Split test helpers that exceed the function-length error threshold.

## Non-goals

1. Deleting coverage just to silence lint.
2. Marking private vars public solely for tests without a real API reason.
3. Rewriting unrelated application code except to expose a legitimate public seam.
4. Weakening protocol definitions to fit tests.

## Initial affected tests

Hard errors:

- `test/cljs/knoxx/backend/pipeline_runner_test.cljs`
- `test/cljs/knoxx/backend/policy_actor_test.cljs`
- `test/cljs/knoxx/backend/tools/temp_memory_test.cljs`
- `test/cljs/knoxx/backend/mcp_http_test.cljs`

Warning-heavy tests include:

- `test/cljs/knoxx/backend/node/fs_test.cljs`
- `test/cljs/knoxx/backend/memory_routes_test.cljs`
- `test/cljs/knoxx/backend/model_routes_test.cljs`
- `test/cljs/knoxx/backend/contracts_routes_test.cljs`
- `test/cljs/knoxx/backend/contracts/loader_test.cljs`
- `test/cljs/knoxx/backend/tools/blaze_music_generate_test.cljs`

## Implementation notes

- Prefer `deftest ^:async` and `await` for modern CLJS tests; do not introduce Promesa just to silence a chain warning.
- Keep assertions linear; avoid reintroducing nested Promise chains under helper fns.
- If a private helper encodes a stable domain rule, move the rule to `domain.*`, `shape.*`, or `law.*` as appropriate and test it there.
- If a private helper is implementation detail, test through the public behavior that depends on it.

## Verification

Targeted lint for touched tests:

```bash
pnpm -C backend exec clj-kondo --lint <touched-test-files>
```

Required behavior gate:

```bash
pnpm -C backend exec shadow-cljs compile test
```

Final lint gate for this task:

```bash
pnpm -C backend lint
```

## Definition of done

- Test private-var access errors are gone.
- Test Promise-chain warnings are gone or explicitly assigned to a later slice.
- Protocol mock warnings are gone.
- No coverage is removed without a replacement assertion.

# Π Fork Tax 2026-06-04T18:33:20Z — trigger tests + domain refactor

## Snapshot
- **Branch:** `fix/frontend-es2022-lib`
- **Base target:** `origin/staging`
- **Scope:** Backend trigger test suite, domain condition registry refactor, event dispatch contract loading, source runtime discord integration

## Changed

1. **domain-condition-registry** — `backend/src/cljs/knoxx/backend/domain/condition/registry.cljs`
   and its test `backend/test/cljs/knoxx/backend/domain/condition/registry_test.cljs`.
2. **contracts-loader** — `backend/src/cljs/knoxx/backend/domain/contracts/loader.cljs`.
3. **discord-source** — `backend/src/cljs/knoxx/backend/domain/discord/source.cljs`.
4. **event-dispatch** — `backend/src/cljs/knoxx/backend/domain/event/dispatch.cljs`.
5. **source-runtime** — `backend/src/cljs/knoxx/backend/domain/source/runtime.cljs`.
6. **trigger-tests** — 11 new trigger test namespaces under
   `backend/test/cljs/knoxx/backend/triggers/` covering action invocation,
   contract root mismatch, contracts discovery, error propagation,
   event deduplication, production scenario, real contracts dispatch,
   source dispatch, trigger loading, trigger matching, and trigger validation.
7. **session-note** — `docs/notes/2026.06.04.09.47.41.md`.

## Verification
- `pnpm -C backend exec shadow-cljs compile test` → pass (523 tests, 1528 assertions, 0 failures, 0 errors)

## Concurrent Dirt
None. All working-tree changes are owned by this snapshot.

## Follow-up
- PR `fix/frontend-es2022-lib` → `staging`; merge after checks.

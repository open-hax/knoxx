# Π Fork Tax 2026-07-10T20:06:39Z — OpenPlanner direct-client integration (mongo mode)

## Snapshot
- **Branch:** `main`
- **Base target:** `origin/main`
- **Scope:** OpenPlanner direct-client integration: in-process MongoDB data plane via `@open-hax/openplanner-sdk`; REST delegation fallback; contract-runtime source path alignment; PRINCIPLE sync.

## Changed

1. **principle** — `.ημ/PRINCIPLE.edn` updated.
2. **tooling** — `backend/.clj-kondo/config.edn`, `backend/package.json`, `backend/pnpm-lock.yaml`, `pnpm-lock.yaml`.
3. **build** — `backend/shadow-cljs.edn` updated for contract-runtime source path and openplanner-sdk test resolution.
4. **runtime** — `bootstrap.cljs`, `infra/clients/openplanner.cljs`, `infra/config.cljs` updated to support `KNOXX_OPENPLANNER_CLIENT_MODE`.
5. **new-files** — `extern/openplanner_sdk.cljs`, `infra/clients/openplanner_mongo.cljs`, `test/cljs/knoxx/backend/extern_openplanner_sdk_test.cljs`, `test/js/openplanner_sdk_test_stub.mjs`.

## Verification
- ✅ `pnpm -C backend exec shadow-cljs compile server`: success, 0 warnings.
- ❌ `pnpm -C backend exec shadow-cljs compile test`: 1 failure in `knoxx.backend.infra.store-test` — `Store document failed schema validation`. This failure is recorded as a blocker and is not resolved by this snapshot.

## Concurrent Dirt
None. All working tree changes are owned by this snapshot.

## Blocked / Excluded
- `.claude/` — agent runtime state.
- `.shadow-cljs/` and `backend/.shadow-cljs/` — build artifacts.
- `node_modules/` and `backend/node_modules/` — installed dependencies.

## Commit
Base: `144cd66280c9aa468c9fa252dac0fa7321cf404c` (HEAD before this Π snapshot).

## Tag
`Π/20260710T200639Z-openplanner-mongo-direct-client` (points to the commit above).

## Follow-up
- Push `main` + Π tag to origin.
- Address the `infra.store-test` schema-validation failure before the next release.

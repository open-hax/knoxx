# Π Fork Tax 2026-06-03T20:21:42Z — coverage tests + label-gated testing deploy

## Snapshot
- **Branch:** `test/coverage-improvement`
- **Base target:** `origin/staging`
- **Scope:** Backend domain/law/shape test coverage, parse regex fix, frontend test env, label-gated testing deploy workflow

## Changed

1. **parse-fix** — `backend/src/cljs/knoxx/backend/shape/parse.cljs`: `parse-positive-int`
   used `#"\\."` (matches literal backslash+dot) instead of `#"\."`; decimal strings
   like "1.5" were parsed as 1 instead of rejected as NaN.
2. **coverage-tests** — new `backend/test/cljs/knoxx/backend/{domain/condition/builtin,domain/node/*,domain/time,law/*,shape/*}_test.cljs`.
3. **frontend-test-env** — `frontend/package.json`: `NODE_ENV=test` for vitest
   run/coverage/watch scripts.
4. **deploy-testing workflow** — `.github/workflows/deploy-testing.yml`: adding the
   `testing` label to an eligible PR (same-repo head, non-draft, owner in
   `TESTING_ALLOWED_OWNER_LOGINS`) runs preflight gates at the PR head and deploys it
   to the shared **staging** slot via
   `open-hax/services/.github/workflows/deploy-promethean.yml@main` (service: knoxx).
   Shares the `knoxx-staging` concurrency group with deploy-staging; queues, never
   cancels in-flight deploys.

## Verification
- `pnpm -C backend run test:coverage` → pass (exit 0)
- `pnpm -C frontend run test:coverage` → pass (exit 0)
- `actionlint .github/workflows/deploy-testing.yml` → clean

## Concurrent Dirt
None. All working-tree changes are owned by this snapshot.

## Follow-up
- PR `test/coverage-improvement` → `staging`; add `testing` label for the
  label-gated test deploy; merge after checks.

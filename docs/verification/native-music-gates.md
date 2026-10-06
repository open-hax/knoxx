# Native music hotfix gate evidence

Worktree: `/home/err/spaces/cephalon-music-fix/knoxx`.
Branch: `codex/fix-native-music-result`.
Base: `3409977bca4ba35e09967f9a99d50867a679a73c`.
RED commit: `1afb85991bd5d7ff4a6134c15da0dff77d98e15d`.

Commands below are run from the worktree root unless noted. The recorded
compiler/test environment used Node 24.14.1 and Java 21. Setting
`NPM_CONFIG_USERCONFIG=/dev/null` avoids loading the user npm configuration;
no `.env` or credential value was inspected or printed.

## RED and GREEN

The first RED invocation attempted a namespace filter nested under `:builds`.
Shadow's CLI merges into the selected build, so that filter was ineffective:
the **full** suite actually ran. This provided stronger RED evidence rather
than an empty or skipped test run:

```bash
cd backend
NPM_CONFIG_USERCONFIG=/dev/null node node_modules/shadow-cljs/cli/runner.js \
  --force-spawn \
  --config-merge '{:builds {:test {:ns-regexp "^knoxx\\.backend\\.extern\\.native-music-test$"}}}' \
  compile test
```

Result: 1,847 tests / 9,102 assertions; **six failures, zero errors**. The
real native process wrote nonzero RIFF/WAVE PCM, then the old code attempted
to JSON-parse the `{stdout, stderr}` object. All six failing assertions belong
to the new native metadata/path regression test. Shadow exited zero despite
the failing tests; `testCountersExitCode` correctly rejects that log with 1.

```bash
NPM_CONFIG_USERCONFIG=/dev/null scripts/verify-native-music.sh --source
```

Result: **3 tests / 18 assertions; zero failures, errors and compiler
warnings**. This command checks the counters and the executed namespace,
invokes the actual engine, verifies WAV bytes, and cleans its fixture.

## Required backend gates

For pnpm commands, the process-only prefix was:

```bash
NPM_CONFIG_USERCONFIG=/dev/null volta run --node 24.14.1 pnpm
```

| Command | Observed result |
| --- | --- |
| `pnpm -C backend exec shadow-cljs --force-spawn compile test` with the guarded test environment below | **PASS**: 1,848 tests / 9,107 assertions; 0 failures, 0 errors; 864 files, 101 compiled, 0 compiler warnings; process exit 0. Counters also checked independently. |
| `pnpm -C backend run test:smoke` | **PASS**: all 75 JavaScript tests; 0 failures, cancellations or skips. |
| `pnpm -C backend exec shadow-cljs --force-spawn compile server` | **PASS**: production server/typecheck target; 534 files, 500 compiled, 0 warnings, exit 0. This is the target used by the package's `typecheck` script. |
| `pnpm -C backend run build` | **PASS**: `shadow-cljs release server`; 532 files, 452 compiled, 0 warnings, exit 0. |
| `pnpm -C backend run lint` | **BLOCKED**: clj-kondo reports 8 errors and 288 warnings, exit 3 from clj-kondo. The pnpm/Volta wrapper reported a nonzero exit as well. |
| `clj-kondo --lint src/cljs/knoxx/backend/extern/native_music.cljs test/cljs/knoxx/backend/extern/native_music_test.cljs --fail-level warning` from `backend/` | **PASS**: 0 errors, 0 warnings, exit 0. The backend cwd is required to load its sanctioned `await` lint configuration. |
| `node backend/scripts/check-js-boundary.mjs --check` | **PASS**: 0 allow-listed non-extern generic extern import files, exit 0. Named externs are discovered automatically; no inventory policy change was necessary. |
| `node backend/scripts/check-error-boundaries.mjs --check` | **BLOCKED**: 35 existing silent catch sites, exit 1. All sites belong to 23 files byte-identical to the base revision. |
| `git diff --check`, `node --check scripts/verify-native-music.mjs`, `bash -n scripts/verify-native-music.sh` | **PASS**. |
| Verification script given an unsupported argument | **PASS negative check**: prints `FAIL Usage: ...` and exits 1. |
| Docker COPY/context assertion | **PASS static check**: the exact COPY names an existing nonempty synthesis engine inside the backend build context. No Docker image build or deployment is claimed. |

Full test invocation:

```bash
NPM_CONFIG_USERCONFIG=/dev/null \
CONTRACTS_DIR=test/fixtures/empty-contracts \
NODE_OPTIONS='--require /home/err/spaces/cephalon-music-fix/knoxx/backend/scripts/shadow-test-error-guard.cjs' \
volta run --node 24.14.1 pnpm -C backend exec shadow-cljs --force-spawn compile test
```

## Baseline blockers

A disposable archive of base revision `3409977b` was linted with the same
backend configuration. It also exited 3 with **8 errors / 288 warnings**.
Comparing all 296 diagnostics after normalizing line/column numbers found
**zero additions or removals**. The eight existing errors are the file-size
gate on:

- `backend/src/cljs/knoxx/backend/domain/bluesky/bluesky.cljs`
- `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs`
- `backend/src/cljs/knoxx/backend/domain/discord/tools.cljs`
- `backend/src/cljs/knoxx/backend/infra/db/policy.cljs`
- `backend/src/cljs/knoxx/backend/infra/routes/app.cljs`
- `backend/src/cljs/knoxx/backend/infra/routes/memory.cljs`
- `backend/src/cljs/knoxx/backend/infra/routes/resources.cljs`
- `backend/test/cljs/knoxx/backend/infra/translation_dispatch_test.cljs`

These failures were preserved. This change has GREEN functional and build
evidence; **the repository is not fully gate-green**. No remote PR, merge,
deployment, restart, publication, or agent launch was performed. Live validation
of this committed parser/packaging change remains for an operator using the
[native music verification script](native-music.md).

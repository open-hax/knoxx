# Build size-gate repair

PR #327 starts running the backend and frontend clj-kondo hook regression. Its
frontend hook previously waited for the next namespace before reporting file
size, missing a sole or final large file. The repair moves the existing hook
from cumulative commit `068781219908dba0f1c66a654c7568f3cc6e4d56` into this
prerequisite layer unchanged. It uses the invocation's owning filename and
reports while visiting that file. Promise, function-length, and complexity
checks retain their existing behavior.

The repository size CLI enforces the README's warning threshold of 350 lines
and error threshold of 500 lines. These are separate from the existing
clj-kondo hook thresholds of 400/800; neither README nor current main authorizes
relaxing the repository size budget to match the hook.

Run the direct checks from the Knoxx root with Node, pnpm, and clj-kondo
2025.07.28 installed:

```sh
node --test backend/test/js/kondo-file-size-hook.test.mjs backend/test/js/file-size-budget.test.mjs
pnpm -C backend test:smoke
node scripts/lint-file-sizes.mjs
```

The hook test invokes the real clj-kondo binary on a large file alone and in
both orders with a small file, checking that findings stay on their owner.
The budget test invokes the real size CLI for all eight configured extensions
at 349, 350, 499, and 500 lines, checking warning output and failure status.
Both suites remove their disposable fixtures. The existing backend test and
coverage commands include these regressions through `test/js/*.test.mjs`.

On PR #327 head `02731230d7695313d76072bcf898a016f545011c`, the direct checks
passed 1 of 10 tests: the frontend hook failed and all eight budget cases
failed. With this repair, all 10 pass, and the complete Node smoke suite
passes all 59 tests with no skips.

The full size check remains **failing**: 652 files checked, 61 errors and 48
warnings. Examples include `frontend/src/pages/BroadcastStudioPage.tsx`
(2,574 lines), `backend/src/cljs/knoxx/backend/infra/routes/app.cljs`
(1,820 lines), and `frontend/src/pages/DataPage.tsx` (1,633 lines). These are
existing source files, not changes in this repair. The gate continues to
return a failure rather than hiding them with larger thresholds. This evidence
does not establish a passing full source lint, a production build, or CI on
the subsequently merged head.

The subsequent default-discovery repair adds `ingestion/src` to the normal
root scan. The earlier defaults omitted that source tree even though the
budget applied to Clojure sources. The regression copies the real CLI and
configuration into a disposable repository with an oversized file in each of
the four source roots. It invokes the CLI with no positional targets or custom
configuration and requires all four failures to be reported. Before the fix,
the CLI reported only three; the eight explicit-file threshold tests still
passed. All nine size-CLI tests pass after the fix.

Measured from parent commit `226fcd48913aa2f613813b647033c64f9cfe6e42` with only
the default root added, the complete scan remains **failing**: 690 files,
66 errors and 55 warnings, compared with 661 files, 62 errors and 49 warnings
before ingestion was included. The newly visible ingestion findings are:

| Source path under `ingestion/src/kms_ingestion/` | Lines | Finding |
| --- | ---: | --- |
| `translation/worker.clj` | 694 | error |
| `api/routes.clj` | 661 | error |
| `graph.clj` | 570 | error |
| `drivers/opencode_sessions.clj` | 515 | error |
| `server.clj` | 465 | warning |
| `drivers/github.clj` | 461 | warning |
| `db.clj` | 394 | warning |
| `jobs/worker.clj` | 388 | warning |
| `drivers/eta_mu_sessions.clj` | 371 | warning |
| `drivers/audio.clj` | 353 | warning |

All 29 ingestion source files are unchanged from main commit
`c33b762340a2ab665e0166195c9b3f075dde6ccb`. This repair exposes that inherited
debt; it does not exempt it or change either threshold. Reproduce the ingestion
inventory with `node scripts/lint-file-sizes.mjs ingestion/src` and the default
scan with `pnpm run lint:size`; both return nonzero for these existing errors.

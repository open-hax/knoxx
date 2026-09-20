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

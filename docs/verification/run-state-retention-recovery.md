# Run heap retention repair

The run registry kept its order vector at 200 IDs, then checked whether that
already-bounded vector exceeded 200 before evicting values. That condition never
became true, so the heap retained old run records indefinitely. The native
regression stored 202 runs: its order had 200 IDs but its heap had 202 values.
Five assertions failed, including stale key eviction and updating a retained ID.

Retention now checks the actual heap size and selects the keys from the capped
order. The original registry and sink `defonce` vars remain in their original
namespace. SDK array normalization has a named extern boundary; tool trace
projection and missing-input enrichment are pure functions in `run-trace`.
The existing public state functions remain, and their mutation ordering is
unchanged apart from making the intended eviction execute.

The final native proof passed **8 tests / 26 assertions**, zero failures/errors,
with the fatal asynchronous guard and actual exit zero. Its distinct
`:run-state-proof` build compiled **67 inputs, six compiled, zero warnings**.
Tests cover bounded heap/order equality, stale keys, existing-ID promotion,
native array normalization, bounded tool progress, preserved inputs, error
completion, unrelated events, missing observations and rejected sentinel inputs.
All changed source/tests pass the seven explicitly enabled optional lint rules
from `AGENTS.md` with **zero errors/warnings**.

The proof used Node 24.20, CLJS 1.12.145 and frozen eta identity initialization
`fc3b6a09c6cd90ca200023cbc1fc54ec57a630a0` through local dependency overrides.
Full backend06 predates this repair; its green result is not transferred here.
Production compilation and a later combined run remain separate gates.

The initial attempt to compile the regression from staging replaced the
`:cljs` alias's source paths, hiding project namespaces. A second attempt
replaced its extra dependencies, hiding Shadow itself. Once the production
source freeze ended, the regression moved into the normal project test path
and compiled with the established dependency overrides. Neither failed
classpath attempt was counted as native test evidence.

```sh
cd backend
clojure -M:cljs scripts/compile-run-state-proof.clj
CONTRACTS_DIR=test/fixtures/empty-contracts \
  node --require ./scripts/shadow-test-error-guard.cjs \
  target/run-state-proof/tests.cjs
```

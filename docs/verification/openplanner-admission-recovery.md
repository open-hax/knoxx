# OpenPlanner concurrent Clio admission

Browser run12 reached automatic translation candidates, but its real queued and
started diagnostic projections raced at one `knoxx/openplanner` stream position.
One append won; the other correctly failed with `clio_application_stale_head`.
The projection was then logged as failed and its fact was absent. Serial fixture
calls had hidden this behavior.

The OpenPlanner driver now uses the existing local operation-lock boundary around
its entire replay/decision/append. The lock file is
`<resolved-ledger-directory>/openplanner-store.lock`, separate from the Clio ledger
inode. Independent driver handles therefore coordinate through the same process
and kernel lock. This follows the existing run and policy providers. Embeddings,
model calls and network work remain outside the operation lock. The generic Clio
application engine still rejects a conflicting writer that bypasses this driver;
no automatic retry or stale-history acceptance was added.

The first new test compile failed on an unmatched delimiter in the test itself.
The compiler helper exited1, and shell `set -e` prevented an old bundle from
running. After correcting the fixture, the unchanged production code reproduced
**8 failures in16 tests /95 assertions, zero errors**. The failures covered six
concurrent distinct writes across two independent provider handles, equal-event
idempotence, and the exact same-run queued/started projection path. Existing
OpenPlanner scope and translation behavior tests were included in the same gate.

The repaired native suite passes **16 /95**, zero failures/errors, **271 compiler
inputs and zero warnings**, with the fatal async-error guard enabled. The refused
command case proves that a failed transition releases admission for a following
valid writer. Reopen verifies all accepted facts and vectors, including absence
of duplicate history for equal events.

A separate native proof starts two fresh Node processes, opens the same ledger,
waits for both IPC readiness messages and releases both writers together. Each
process admits four events through the actual compiled OpenPlanner adapter.
After both processes exit0, a fresh provider verifies **8 events,8 vectors and16
canonical operations**, without invoking projection repair. The standalone
library compiled **117 inputs with zero warnings**. A20-second supervisor deadline
kills and joins unfinished workers before deleting the temporary ledger.

Both owned CLJS code/test paths and the standalone CLJS probe pass all seven
optional clj-kondo rules with warnings treated as failures. The driver cleanup
also removes seven previously hidden optional findings: shadowed local names and
missing documentation. The two compiler scripts are checked separately.

From `backend/` with the pinned activation sourced:

```sh
set -e
pnpm exec shadow-cljs clj-eval '(load-file "scripts/compile-openplanner-admission-proof.clj")'
NODE_ENV=test node --require ./scripts/shadow-test-error-guard.cjs target/openplanner-admission-proof/tests.cjs
pnpm exec shadow-cljs clj-eval '(load-file "scripts/compile-openplanner-process-proof.clj")'
NODE_ENV=test node scripts/verify-openplanner-admission.cjs
```

Independent identity and root reviews found no confirmed defect in the narrow
lock placement, returned Promise, unchanged operation arguments or failure
release. I checked that model work is outside the lock and that fresh readers
observe the complete ledger after concurrent native admission.

The existing shared lock helper polls until it acquires ownership and has no
independent production acquisition deadline. The10-second test join and20-second
process supervisor bound verification only; this patch does not claim a new lock
timeout policy. Full backend08 predates this repair. The next production/browser
build and the combined full suite remain separate acceptance gates, as do
current-head external reviews. No frozen browser output was rewritten by these
isolated tests.

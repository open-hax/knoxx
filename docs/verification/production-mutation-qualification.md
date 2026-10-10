# Mutation runner qualification

Run the focused runner controls before spending time on a campaign:

```sh
cd backend
clojure -M:mutation-test
python3 -m unittest discover -s test/python -p 'test_mutation_process.py'
node --test test/js/mutation-test-launcher.test.mjs test/js/shadow-test-error-guard.test.mjs
cd ..
python3 scripts/test-production-mutation-evidence.py
```

The runner temporarily edits a real source file in a clean committed checkout,
compiles the non-autorun `test-ci` target, runs its output in a separate guarded
Node child, and restores the original source. Source inventory includes `.cljs`
and `.cljc`; inactive reader-conditional branches and discarded reader forms
receive no candidate identity. Compile completion and the test entrypoint hash
are recorded alongside captured compile/test output. The test environment uses
the existing error preload and default empty-contracts fixture.

Only a normally exited child with complete untruncated output and complete owned
process cleanup can supply evidence. The green baseline requires exit zero,
positive tests/assertions, matching summaries, zero failures/errors and no fatal
marker. A kill requires normal exit one and completed red assertions under the
same conditions. A crash, signal, fatal async error, missing/mismatched summary,
compiler warning, timeout or capture/cleanup failure remains invalid. Schema 2
reports retain the evidence; the verifier rechecks output hashes and counters
instead of trusting a `killed` label.

The subprocess adapter requires Linux `/proc`, pidfds and process-local subreaper
support. It supervises only its fresh child process group and adopted descendants,
including descendants that exit their parent's group. Output is bounded to
8 MiB and cleanup to two seconds. SIGTERM/INT use bounded cleanup. SIGKILL or power
loss cannot execute cleanup or source restoration; such a run supplies no
qualification result. Recover only the disposable owned checkout, never a shared
service or another agent's process.

Equivalent exclusions are **empty by default**. An optional `--equivalents FILE`
selects a manifest inside `backend/` that must match tracked committed HEAD bytes.
Its EDN shape has `:schema-version 1`, an ancestor `:basis-source-sha`, and an
`:entries` vector. Each entry supplies the exact candidate `:fingerprint`,
`:relative-path`, complete source `:source-sha256`, semantic `:rationale` and a
real external Knoxx PR `:review-reference`. The source must be unchanged since
that basis. The owning repository must actually review that adjudication: a URL
with the expected grammar proves provenance shape, not that a reviewer agreed.
This implementation creates no equivalent adjudication or approval.

Exclusions remain inspectable in the report with manifest hash and basis. They
receive zero evaluated or killed credit. Selection replenishes from the same
deterministic ordinal partition after exclusions. Invalid executions and real
survivors are retained and fail the run; replenishment never disguises them.
The four-batch verifier still requires at least 250 distinct completed assertion
kills in each batch, no duplicate global fingerprints, and the exact source SHA.

First qualify a baseline and two to five actual candidates on the committed
source using the production workflow's Java/Clojure/pnpm/Node/SDK inputs. Record
compile and test durations before the whole campaign. A synthetic fixture or a
dry-run candidate manifest supplies no baseline, mutation or production credit.
The `test-ci` SDK substitutes remain test boundaries; they do not qualify native
SDK behavior. Services consumes the reviewed product workflow and immutable
source result; it does not implement these mutation semantics.

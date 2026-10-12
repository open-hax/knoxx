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

To verify actual campaign evidence, select one completed production workflow
run and its exact40hex source S. Download all four artifacts from that same run:
`production-mutation-0`, `production-mutation-1`, `production-mutation-2`, and
`production-mutation-3`. Each supplies `batch-N.edn.json` and `payload-store/`.
Use a fresh evidence directory containing exactly those four batch JSON reports
and one merged `payload-store/` directory with the union of their digest-named
blobs. If the same digest occurs in several artifacts, its bytes must be
identical; refuse a differing duplicate rather than overwriting it. Preserve the
original downloads and failed verifier output. This is the layout produced by
the workflow's `actions/download-artifact` step with `merge-multiple: true`.

From the checkout at the same reviewed source S, run the committed verifier:

```sh
SOURCE_SHA='<exact40hex-source-S>'
EVIDENCE_DIR='<fresh-directory-with-four-batch-json-reports-and-payload-store>'
python3 scripts/verify-production-mutation-evidence.py "$SOURCE_SHA" "$EVIDENCE_DIR"
```

A successful verifier reports the exact source and at least1000 distinct
completed assertion kills across four batches. It rechecks full retained
compilation/test/source evidence, loader layout and physical CAS bytes. It
verifies each shared nonloader blob on first use and rehashes every unique blob
once again at the final exact-store check; loader bytes are reread for each
payload's import parsing. Its cache belongs to one verifier invocation. This
rejects changed bytes even when size, inode and modification time are preserved.
These focused synthetic and cross-runtime contract controls supply no campaign,
integration/e2e, native SDK or production readiness credit.

The runner temporarily edits a real source file in a clean committed checkout,
compiles the non-autorun `test-ci` target, runs its output in a separate guarded
Node child, and restores the original source. Source inventory includes `.cljs`
and `.cljc`; inactive reader-conditional branches and discarded reader forms
receive no candidate identity. The immutable source HEAD is captured before the
run. Before and after compilation and testing, the runner requires that HEAD,
an unchanged index and no untracked inputs; tracked inputs must be unchanged
except for the exact source file and rewrite bound to the current mutant.
The test environment uses
the existing error preload and default empty-contracts fixture.

Only a normally exited child with complete untruncated output and complete owned
process cleanup can supply evidence. The green baseline requires exit zero,
positive tests/assertions, matching summaries, zero failures/errors and no fatal
marker. A kill requires normal exit one and completed red assertions under the
same conditions. A crash, signal, fatal async error, missing/mismatched summary,
compiler warning, timeout or capture/cleanup failure remains invalid. Schema 3
reports retain the evidence; the verifier rechecks output hashes and counters
instead of trusting a `killed` label.

The nonbundled `test-ci.cjs` loader is not the complete executed artifact.
Compilation records that loader plus every flat emitted runtime JavaScript and
source-map file under `.shadow-cljs/builds/test-ci/dev/out/cljs-runtime`, with
sorted path, kind, byte length and SHA-256 entries. The supported pinned Shadow
layout has one runtime JS file for each ordered `SHADOW_IMPORT` entry, plus
`goog.base.js`. Source maps are optional companions of those JavaScript files;
every physically emitted map is included. The observed output has a map for
`goog.base.js` and none for `shadow.module.main.append.js`. Missing or extra
runtime JS, unknown map companions, nested or symlinked output refuses custody.
The loader must use that exact relative runtime root. A toolchain layout change
requires a reviewed contract update rather than silently dropping output.

Exact bytes are stored once by digest in `target/mutation/payload-store` and
uploaded with the report. Start with a fresh output directory; the runner refuses
an existing payload store and never removes prior evidence. After merging the
four artifacts, the verifier requires exactly the union of referenced digest
files in the store. `--output-dir` must be backend-relative with no parent or
absolute path traversal. A portable inventory hash covers all files; a second
code hash excludes maps. The actual complete inventory and source binding must
still match immediately before and after the test child. The independent
verifier reads the copied bytes, checks their lengths and hashes, checks the
loader/import inventory and matches the test's recorded input to its compile.
It also requires each killed mutant's code identity to differ from the green
baseline. An unchanged loader with changed runtime code is allowed. Unchanged
code or a maps-only change is invalid and receives no equivalent exclusion or
kill credit. These observations do not claim continuous filesystem monitoring
or custody of code outside the emitted payload; package locks and the configured
toolchain remain separate inputs.

Earlier schema 2 reports, including the three-candidate pilot, remain historical
evidence with entrypoint-only custody. They do not satisfy schema 3 admission.
The old pilot remains failed with two killed mutants and one survivor; this
repair does not reinterpret its result or supply a new campaign.

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

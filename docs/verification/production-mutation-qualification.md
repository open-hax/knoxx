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

Production qualification uses the protected workflow's authenticated executor.
It has three distinct source coordinates: **C** is the current protected Knoxx
controller revision, **G** is the current protected initiating caller revision,
and **S** is the selected immutable application source. The direct Knoxx
`workflow_dispatch` route has C = G. The reusable route accepts only the
protected `open-hax/services` `.github/workflows/deploy-stack.yml` caller and
requires the native run-attempt `referenced_workflows` entry to identify C.
`github.workflow_sha` is not substituted for the called revision.

Before application execution, the controller requires S to be an ancestor of C
and compares the committed execution inputs of both sources: mutation runner,
subprocess and test adapters, test fixtures, compiler macro inputs, setup action,
package/lock configuration and verifier. The selected application's code may
change; the executor and its configuration may not. Every matching input is
also compared with its physical checkout bytes. An older S with different
executor inputs refuses qualification rather than falling back to its old
verifier. A protected main advance during the run also refuses the current
context; this conservative route supplies no immutable-main exception.

The integration/e2e job and all four mutation jobs must actually succeed in
one native run attempt. Their diagnostic artifacts have fixed names
`production-mutation-RUN-ATTEMPT-S-N`, where N is 0 through 3. Failed jobs still
retain diagnostic uploads, but cannot reach the signing job. A separate job
with no application execution downloads only the exact current-attempt native
artifact IDs and signs the four complete `batch-N.edn.json` subjects using
`actions/attest@1e69f48acb82d1966a394da916b4c1698aa569d6` (v4.2.2).
Only this signing job has OIDC and attestation write permissions. It performs
no compiler or test invocation and has no `always()` path.

The qualification job runs the verifier from C and keeps S in the separate
`application/` checkout. It downloads the same native artifact IDs into a fresh
external evidence directory containing four reports and the merged digest-named
`payload-store/`. Every report must pass the real official `gh attestation
verify` command. The verifier then checks the verified certificate's signer
workflow/C, initiating caller repository identity/ref/G and workflow path,
GitHub-hosted runner, and exact run/attempt. The signed subject digest binds the
report's complete bytes, including S and its compile/test/payload evidence.
The caller-controlled attestation predicate is not identity authority. The
standard provenance source digest represents G, not S.

The production entry point remains:

```sh
python3 scripts/verify-production-mutation-evidence.py "$SOURCE_SHA" "$EVIDENCE_DIR"
```

It requires fresh authenticated native workflow context and refuses a manual
local invocation or unsigned synthetic reports. An unavailable official tool,
missing or unsupported certificate field, stale caller/controller, wrong run
attempt, partial native pagination, failed producer or changed report bytes
remains a failed qualification. Real official CLI output and native hosted
execution must be qualified before this source proposal supplies deployment
credit; mocked adapter controls establish no cryptographic verification.
The currently pinned Services caller must first receive reviewed controller
and permission updates (`id-token: write` and `attestations: write`) before its
reusable route is usable.

Local evidence characterization uses the explicitly named
`validate_structural_evidence(S, directory)` API. It validates report, canonical
identity and physical payload custody only; it grants **no production credit**.
A synthetic fixture can retain one shared runtime payload and red output under
many real canonical identities for these structural controls. Those repetitions
are not 1000 executions and cannot pass the production entry point unsigned.

This route declares the protected workflow and its reviewed executor to be the
trusted builder. It authenticates their successful execution and resulting
reports; it does not independently replay every compiler invocation or prove
that a compromised trusted builder, runner/toolchain or hostile application
cannot lie. Unique labels or report-supplied verification booleans are not a
substitute for this authenticated trust boundary.

A successful verifier reports the exact source and at least 1000 distinct
completed assertion kills across four batches. It rechecks full retained
compilation/test/source evidence, loader layout and physical CAS bytes. It
verifies each shared nonloader blob on first use and rehashes every unique blob
once again at the final exact-store check; loader bytes are reread for each
payload's import parsing. Its cache belongs to one verifier invocation. This
rejects changed bytes even when size, inode and modification time are preserved.
These focused synthetic and cross-runtime contract controls supply no campaign,
integration/e2e, native SDK or production readiness credit.

The verifier invokes `clojure -Srepro -M:mutation --source-bindings S` in the
guarded S checkout's backend before structural admission and again after its
byte checks. The C verifier supplies that backend explicitly; its own source
location must not accidentally regenerate identities from C. This
read-only canonical command uses the existing source discovery, fingerprint and
source-preserving rewrite functions to regenerate every candidate's original
and mutated source hashes. It checks clean HEAD/index/input custody before and
after regeneration, rejects symlinked source paths, and compares each source
file with its immutable committed bytes. Python matches each claimed binding
and its deterministic batch ordinal to that inventory; it implements no second
mutation identity or rewrite law. A fabricated path, fingerprint, hash or reused
binding cannot earn a distinct kill. The final inventory must still match.
Only the runner's base JVM dependencies are needed for this command; it does not
select the CLJS/SDK classpath, compile artifacts or run tests. Uploaded manifests
and reports cannot replace regeneration from the actual checkout. The workflow
keeps downloaded evidence under runner temp so it does not dirty that checkout.

The verifier controls generate bindings once from the canonical command in a
private committed source fixture. Repeated transport reads are then mocked for
synthetic report/output controls. Those source bindings establish the test's
membership contract, not real compile/test execution or campaign qualification.

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
Every exclusion belongs to that batch's canonical ordinal partition. Killed
identities must equal the first selected canonical partition prefix after only
those explicit exclusions; omission or reordering of a survivor cannot replace
it with a later candidate. These checks remain canonical structural admission,
not an alternative mutation discovery or selection policy.

First qualify a baseline and two to five actual candidates on the committed
source using the production workflow's Java/Clojure/pnpm/Node/SDK inputs. Record
compile and test durations before the whole campaign. A synthetic fixture or a
dry-run candidate manifest supplies no baseline, mutation or production credit.
The `test-ci` SDK substitutes remain test boundaries; they do not qualify native
SDK behavior. Services consumes the reviewed product workflow and immutable
source result; it does not implement these mutation semantics.

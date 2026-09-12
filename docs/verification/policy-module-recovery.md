# Policy module recovery

The policy facade had grown to 1,681 lines, crossing the existing 400-line lint
warning and 800-line error thresholds. The recovery preserves the existing
provider and authorization behavior while separating its responsibilities.

The public `infra.db.policy` namespace retains all 93 public function names.
Its verified identity binding checks and context adapters remain in that
facade, including the `db!`, `build-request-context` and initialization override
seams used by the existing authorization tests. The facade is now 348 lines.

| Owner | Responsibility |
| --- | --- |
| `law.policy-values` | Pure identifier, role-alias and bootstrap normalization, shared by CLJS and the JVM |
| `infra.db.policy.connection` and `.support` | Mongo connection/index/audit effects and contract-file adapters |
| `.roles`, `.hydration`, `.role-queries` | Role assignment, row hydration and permission directory operations |
| `.projection`, `.bootstrap` | Actor contract projection and bootstrap reconciliation |
| `.organizations`, `.users` | Provider-selected directory commands and queries |
| `.credentials`, `.sessions`, `.invites` | Credential lookup and legacy Mongo session/invitation operations |

Each extracted implementation was compared structurally against its original
form before replacement. All **171 bodies and function metadata** matched after
normalizing namespace qualification and the visibility of helpers shared across
modules. Reader-generated anonymous argument names and regular-expression
objects were normalized for that comparison. The dependency graph has no cycles;
the largest source file is the public facade. No lint rule or threshold changed.

The credential fixture exposed a separate false-positive test. Its old
"credentials map regardless of backend" assertion called the real Mongo client,
then accepted the empty fallback after a connection refusal. The replacement
asserts the exact projected row and connection/index/query sequence through the
existing row-level adapter seam. Unavailable storage has a separate explicit
empty-result test. Blank-provider validation asserts its exact error and refuses
any attempted connection. The Clio test now also guards the real Mongo connection
function, so moving the connection implementation cannot weaken that refusal.

Obstacles encountered during verification:

- Shadow's `clj-run` accepts a namespace/function, not a script filename. The
  first compile invocation failed before compilation. Loading the script through
  `clj-eval` runs the intended build.
- The first exact credential-row fixture used a fixed-arity replacement for a
  multi-arity CLJS adapter. The real compiler exposed the mismatch: one failure
  and one error. The fixture now matches the actual `[provider]` and
  `[db provider]` signatures.
- A wider compile found three admin assertions that referenced a private helper
  in the former namespace. They now reference the credential module directly,
  retaining every assertion. That suite is included in the focused proof.

Run the focused native proof from `backend/`:

```bash
pnpm exec shadow-cljs clj-eval '(load-file "scripts/compile-policy-proof.clj")'
CONTRACTS_DIR=test/fixtures/empty-contracts NODE_ENV=test \
  node --require ./scripts/shadow-test-error-guard.cjs target/policy-proof/tests.cjs
```

The build derives the existing test configuration under a distinct build ID and
writes only `target/policy-proof`; it preserves the browser lane's release
snapshot and the full suite's output. The final native compile included 264
files with zero warnings; the guarded runtime passed **41 tests / 175
assertions**, with no Mongo connection attempt. Scoped lint passed with **zero
errors and zero warnings**. Pure normalization laws also pass on
Babashka and JVM Clojure: **3 tests / 13 assertions** on each runtime.

This is a behavior-preserving policy extraction and fixture repair. It does not
change Mail permissions, identity binding semantics, provider selection or the
remaining external-service feature coverage. The complete application suite,
release build, browser proof and global lint remain separate acceptance gates.

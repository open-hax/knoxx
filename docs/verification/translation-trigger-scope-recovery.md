# Translation trigger scope recovery

Browser11 accepted the English source, then the translation agent failed because
OpenPlanner session creation had no organization. Admission had already verified
organization, membership and project; those coordinates disappeared while the
claim became an event and the generic trigger became a run.

Three boundaries now preserve that authority:

- Newly constructed dispatch records retain the admitted membership alongside
  organization and project. Membership stays outside the logical translation
  key, but remains immutable within one attempt.
- Translation events carry a closed `:scope` payload. Its only fields are
  organization, membership and optional project. Extra role, permission and
  trust fields fail validation.
- The publication trigger explicitly opts into `:scope-from-event`. The generic
  start action first requires the existing trusted runtime and matching emitter,
  validates scope, then applies the same scoped config to contract resolution
  and the runner. The auth context receives the scope and the original private
  process identity token; roles and tools still come from resolved policy.

An absent project removes the ambient project. A trigger without the opt-in
ignores event-carried scope. HTTP or serialized data cannot manufacture the
private identity token.

Historical claims without membership remain readable. They cannot manufacture
new scoped agent authority. An attempted replay fails visibly and remains
retriable; a later explicit admission creates a new attempt from the verified
context. Completed durable evidence does not need a fabricated membership to
remain readable.

The first native regression used the real dispatcher, generic action, queue,
agent turn and temporary Clio run/thread providers. It observed the wrong
ambient org/project at the actual planner scope validator. It also exposed an
independent ordering bug: the runner appended a durable started event before
its run had been admitted. Clio correctly rejected the absent run; the later
initial run write surfaced that queued failure. That repair belongs to the
runner admission change and is included in the complete native proof below.

The fixture boundaries are explicit: contract resolution supplies one finite
agent, policy checks avoid external policy infrastructure, hydration executes
the real scope validator, and prompting returns one finite response. Queue
reservation, turn orchestration, initial run/thread persistence and event flush
remain real. The proof checks persisted organization and membership and that
the actual turn reaches the model boundary. Negative cases require the expected
refusal code and zero resolution/spawn effects, so an unrelated native arity
error cannot masquerade as authorization refusal.

Current completed checks: the existing translation laws plus the new scope laws
pass **20 tests / 157 assertions**, with the native asynchronous error guard and
actual exit zero. The distinct law build compiled **76 inputs, 75 compiled,
zero warnings**. The new portable scope laws also pass on the JVM: **2 tests /
14 assertions**. All new scope code and touched constructors pass the seven
explicit optional linters. At the minimal scope checkpoint, four historical size warnings remained in
`law/translation_dispatch.cljc`: its file size, `pin-refusal`,
`source-drift-refusal` and `translation-receipt`. The subsequent
[report extraction](translation-dispatch-report-recovery.md) clears all four.
No warning threshold changed.
The complete native proof now passes **34 tests / 228 assertions**, with the
fatal asynchronous guard and actual exit zero. Its distinct build compiled
**536 inputs, 109 compiled, zero warnings**. This includes the runner's coherent
initial-admission repair, real persisted run/thread checks, explicit refusal
codes, no-opt-in behavior, unforgeable token refusal, and historical replay
followed by a fresh verified admission. The runner's broader fixture suite is
a separate check; this result does not stand in for it.

Verification uses Node 24.20, ClojureScript 1.12.145, and frozen eta identity
initialization `fc3b6a09c6cd90ca200023cbc1fc54ec57a630a0` through local dependency
overrides. Standard pinned dependency promotion, production compilation and the
next browser tour are separate gates owned by the parent recovery task.

```sh
cd backend
clojure -M:cljs scripts/compile-translation-scope-proof.clj --laws
CONTRACTS_DIR=test/fixtures/empty-contracts \
  node --require ./scripts/shadow-test-error-guard.cjs \
  target/translation-scope-laws/tests.cjs

clojure -M:cljs scripts/compile-translation-scope-proof.clj
CONTRACTS_DIR=test/fixtures/empty-contracts \
  node --require ./scripts/shadow-test-error-guard.cjs \
  target/translation-scope-proof/tests.cjs
```

The proof compiler uses distinct build IDs for law-only and complete runs. It
returns nonzero on an actual compiler exception. During regression setup, an
incorrect working directory prevented a fixture edit, an extra parenthesis
failed parsing, and a fixed-arity spawn double did not match the real
multi-arity boundary. Each was corrected before final verification; their
outcomes are diagnostic failures, not passing proof. The first fixture also
used keyword thinking where the real turn expects a string, and expected model
output inside a settlement that intentionally carries only status. Both were
corrected to the actual contracts before the scope failure was assessed.

The Services deployment reviewers required runtime evidence of the loaded opt-in,
not only a string in the EDN file. `status-snapshot` now reports `scopeFromEvent`
alongside the existing event policy flags. Its regression runs the real catalog
and trigger normalizer with explicit resource definitions and checks canonical
true, explicit false, absence and the supported camel-case alias. The combined
scope/catalog proof passes **35 tests / 234 assertions**, guarded exit zero;
**542 inputs, 13 compiled, zero warnings**. The catalog source and regression
also pass all seven optional lint rules with zero findings. This successor
includes the runner's session-reclaim refusal repair.

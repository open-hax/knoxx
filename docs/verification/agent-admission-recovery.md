# Runtime admission reconstruction — 12 September 2026

This checkpoint reconstructs source lost during automated workspace maintenance.
It does not inherit earlier test results. The retained checkout contains compiler
and lint caches, but none of the 26 run-provider source paths or pending admission
sources. The former `/tmp` publication archives are also absent.

The run-provider rescue is restored byte-for-byte from remote `ae5799aaf39ff5453926f611977847fe936f6ac7`.
That commit itself was an unverified reconstruction of lost local `8550c8db`.
Its original provenance manifest remains unchanged. The local-service audit is
restored from `c7a1c65078ce86fa04e995119ff5cbe36472daf6`.

The separately reconstructed changes have the following obligations:

- Runtime producers create occurrence UUIDs once. Retry submits the same event
  unchanged; equal text and equal timestamps do not imply equal occurrences.
- A public agent turn awaits the initial thread write before heap registration,
  `run_started`, and model invocation. Original admission failures reach callers.
  A previously accepted run snapshot remains durable after a thread refusal;
  this boundary does not claim a transaction across two ledgers.
- Mailbox `operation_id` belongs to the caller's domain command and takes
  precedence over an SDK transport call ID. Repeating the command unchanged is
  idempotent even when the SDK call ID changes. Distinct operation IDs under the
  same legacy transport ID produce distinct messages.
- Selecting Mongo clears the preceding EDN run provider and its writer before
  Mongo initialization. An unrelated EDN mailbox can initialize independently.
- Timeout fixtures use real isolated Clio run/thread providers with required
  timestamps. Positive timeout checks use a deliberately pending model response,
  avoiding scheduler-dependent short-timer races.
- Passive hydration outage coverage binds a real organization and asserts that
  the provider was reached, so an earlier missing-auth refusal cannot satisfy it.

## Fresh verification after reconstruction

- Admission, timeout, existing turn behavior and actual HTTP/SDK mailbox checks:
  31 tests / 74 assertions, zero failures/errors, with the fatal async guard;
  521 compiled inputs, zero compiler warnings.
- Run provider contracts, durable routes, Mongo compare-and-swap boundary and
  complete EDN snapshot input: 20 tests / 136 assertions, zero failures/errors,
  fatal guard enabled; 439 compiled inputs, zero compiler warnings.
- Advertised backend `pnpm run test:smoke`: 38 tests, zero skips/failures.
- `pnpm run boundary:check` passes. The changed admission, mailbox, native
  fixture and Mongo decoder boundaries have zero clj-kondo errors/warnings.
- Full backend lint remains red in historical large modules and functions;
  the observed baseline was nine errors and 268 warnings before the separate
  HTTP/publication refactor. The turn module's 800-line error is removed by
  moving pure initial-state projections to CLJC and admission I/O to its own
  infrastructure namespace. Remaining warnings are not suppressed.

The first focused execution stopped because dependency installation had not
finished. After installation, a reconstructed test double lacked the original
function's second arity. Restoring both arities made the regression exercise
the intended thread refusal. It now confirms the original refusal object,
no heap registration, no `run_started`, and no model invocation.

The known `changes` mailbox ID is readable after moving SSE to
`/api/actors/mailbox/events/stream`; actual HTTP creation/read and anonymous
stream denial are included above. Caller command IDs remain unrestricted.

The final run-state source SHA256 is
`fede8c8ef4dc651fd9b8b4201ee7984d2c648c14cc6f78ef3249f6e7f192f9d2`,
which matches the pre-loss digest. This establishes byte identity for that one
path; other reconstructed paths have no original digest and require fresh
evidence. The gates above use the unchanged local eta-mu `dbbe6415` source via
explicit dependency overrides, not the older published dependency pins.

The actual native Mongo process proof also passed: one test / 13 assertions,
zero failures/errors; 102 compiled inputs, zero warnings. It exercises 20
concurrent events, exact retry, changed-ID conflict, process restart and expiry.

## Full-suite findings

The first full backend compile passed with 919 inputs and zero warnings. Its
guarded execution failed 47 assertions and then reported three fatal asynchronous
rejections before reaching a final test summary. The failures were investigated:

- Pure publication fixtures still supplied a three-argument source-acceptance
  callback. The current contract receives publication intent and revision. The
  fixtures now assert the same exact document/source locale/revision using that
  real signature.
- Dispatch orchestration fixtures already stubbed resource files and revisions
  but lacked the new source-review read dependency. An explicit, scoped accepted
  source read fixture now supports those unit tests while leaving
  `acceptance-facts!` active. Different document/tenant reads refuse. These unit
  fixtures do not claim to prove actual source acceptance or filesystem provenance.
- Mongo thread patching exposed a real defect: its native decoder returned BSON
  metadata and Date objects, then the portable thread validator refused that
  provider's own readback. The named decoder now removes Mongo-owned `_id`,
  `createdAt`, `updatedAt` and `expiresAt` fields from all thread read paths.
  Application fields remain intact. The existing read/patch regression now also
  checks that a subsequent read equals the returned portable result.

The focused repair suite passed 65 tests / 396 assertions, with the fatal guard
and zero failures/errors; 490 compiled inputs, zero warnings. All seven changed
fixture/native boundary files pass scoped clj-kondo with zero errors/warnings.

The full suite must be rerun after these repairs. Advertised production build
with final dependency pins and complete browser verification remain pending.
Compiler exit zero alone is not a test result.

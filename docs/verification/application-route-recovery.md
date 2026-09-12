# Application route recovery

The application route namespace was 1,817 lines and failed the unchanged size
rules with one error and 11 warnings. It mixed registration, dependency probes,
chat policy composition, session recovery and HTTP response handling. The public
facade now retains all 79 original public names and the original registration
order. The original file had no multi-arity definitions or `defonce` state to
move. A structural comparison proved all 142 function/route bodies and their
metadata equal after the initial namespace split, before the ten long handlers
were decomposed into named helpers.

The modules under `infra.routes.app` own chat preparation, session views,
recovery, abort, status, route registration and publication composition.
`extern.app-http`, `extern.app-health` and `extern.app-session-requests` own the
extracted transport operations. Eight portable input-normalization functions
live in `shape.agent-chat-input.cljc`. The module graph is acyclic. The facade
still captures the same public dependency values; publication repair, internal
admission and route registration preserve their existing order and scope.

Native Fastify tests exposed a stale boundary independently of the split:
`infra.http/request-body` returns CLJS maps, but old handlers used `aget` to read
them. Valid JSON `sessionId` values were discarded, producing HTTP 400 for undo
and administration abort. After repairing malformed test parentheses and
preserving the actual variadic/multi-arity interfaces of test doubles, the
unchanged application produced **eight failed assertions out of 59**. Named
request decoders and keyword lookups now retain the fields. The same correction
covers conversation abort, semantic-job parameters and the existing ingestion
file entrypoint. Native requests verify numeric options and actual temporary
file contents. This does not add the previously unregistered ingestion PUT
entrypoint to the top-level route catalog.

Independent review found another existing defect in session-only administration
abort: the default empty requested run identifier prevented fallback to the
session's known run. The new native regression produced **two failures out of
75 assertions**. Normalizing that empty identifier before fallback now marks
both the persisted session and the known in-memory run aborted. Explicit run
selection and the default operator reason remain unchanged.

The seven optional lint rules also exposed a broken `defroute` analysis model.
Its synthetic local `await` shadowed the native form; simply removing that local
then exposed incorrectly represented async metadata. clj-kondo requires metadata
nodes under the token's `:meta` key, rather than Clojure symbol metadata. The hook
now models a native async handler correctly. Existing hook tests plus a real
shadowing regression went from two failures to **three passing tests**. Genuine
unused-local, shadowed-variable and literal type errors remain detectable. No
lint suppression, size threshold change or warning downgrade was introduced.

Verification on the final application source:

- Isolated native build: **602 compiler inputs, zero warnings**.
- Fatal-async-guard execution: **21 tests / 75 assertions, zero failures/errors**.
- Portable input laws: **3 tests / 8 assertions** under both Babashka and the JVM.
- All owned application source/tests pass the seven optional lint rules and
  existing size/complexity checks with zero errors and warnings.
- Independent review checked public ownership, registration order, dependency
  maps and async boundaries, and supplied the abort fallback regression above.

Run the focused build from `backend` with
`pnpm exec shadow-cljs clj-eval '(load-file "scripts/compile-app-proof.clj")'`, then
`CONTRACTS_DIR=test/fixtures/empty-contracts NODE_ENV=test node --require ./scripts/shadow-test-error-guard.cjs target/app-proof/tests.cjs`.
The distinct target avoids rewriting frozen browser artifacts. An early malformed
test exposed that `clj-eval` itself can exit zero after a compile exception and
leave an older bundle on disk. The helper now catches compilation failures and
exits nonzero. An injected compilation exception verified the helper exits with
status 1; those old-bundle runs are not counted as verification.

The complete backend suite and production browser workflow are subsequent gates
on the combined source and final dependency pins. This focused proof does not
claim those wider gates passed or remove older application/service limitations.

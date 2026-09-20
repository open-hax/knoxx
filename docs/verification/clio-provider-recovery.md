# Recovered Clio application providers

This checkpoint reconstructs application persistence after the sandbox workspace
loss. These files are not claimed to match the lost golden implementation byte
for byte. The canonical Clio package remains the ledger authority; each provider
replays accepted finite operations into a disposable projection.

The recovered providers cover translation split/evidence stores, local policy,
run state and ordered runtime events, conversation threads, expiring application
caches and fixed-window counters, and delegated MCP authorization codes/tokens.
Legacy cache/thread/MCP entry points select the installed provider for omitted
or nil database arguments. An explicit Mongo handle selects Mongo. A refused or
failed Clio operation never falls back to Mongo or publishes staged cache state.

Run events preserve runtime `run_id`, `session_id`, `conversation_id`, `type` and
`at` fields. The provider assigns each event a stable per-run `sequence`; a
caller-supplied event ID cannot be reused with a changed payload. Numeric cursors
are strict sequence cursors; ISO timestamp cursors retain legacy strict `at`
filtering. Run expiry hides views without deleting accepted history.

## Fresh checks

From `backend`, with the declared Node and JVM toolchains active:

```sh
pnpm exec shadow-cljs compile provider-recovery-proof
node --require ./scripts/shadow-test-error-guard.cjs target/provider-recovery-proof/test.cjs
```

The distinct build ID isolates the compiled runtime from full-suite Shadow
builds. Changing only `output-to` on the same build ID does not isolate it.

Latest focused result: **11 tests, 77 assertions, zero failures/errors**; **223
compiler files, zero warnings**. All 33 owned source/test checkpoint paths also
pass clj-kondo with zero errors/warnings. Checks exercise real temporary ledgers,
restart, corruption, concurrent writers, namespace fidelity, operation collision,
code single-use, scoped token revocation, run expiry, cache isolation between
providers, and refusal before publishing cache changes. Every legacy cache facade
is exercised with a Mongo accessor that throws if consulted.

The Mongo identity barrier regression checks member/user/org/actor changes during
role hydration. The implementation uses bounded optimistic revalidation, not a
transactional snapshot; changes after the final read remain possible. Its facade
lives in the separately reviewed identity/policy checkpoint.

## Obstacles and limits

- A native async CLJS test can throw before its first assertion while an unguarded
  Node runner exits successfully. The shared preload now observes uncaught async
  failure before accepting process exit; see `async-test-errors.md`.
- The first new checks exposed two fixture defects: settled-result status is a
  keyword, and a mocked multi-arity function must retain its arity table. Corrected
  fixtures now execute the intended negative paths.
- The new policy barrier test caught legacy errors with only a native
  `statusCode` property. The restored binding checks now return portable
  `ex-info` status/code data, preserving visible 403/409 refusals.
- Running Shadow from the repository root selected the workspace configuration.
  The documented command runs from `backend`, where this distinct target exists.
- Clio change subscribers receive no private event data and run only after a
  state-changing append succeeds. Callback failures are reported independently.
  This immediate notification covers writers in the current process; a separate
  cross-process filesystem watcher is not implemented here.
- Delegated token/code ledgers store hashes and closed grant facts, not raw bearer
  or authorization-code bytes. Expiry is checked during admission preparation,
  before Clio's append lock; it is not a lock-linearized wall-clock guarantee.
- Mongo adapters require an actual Mongo service for real transport verification.
  This checkpoint does not claim that a native mock proves Mongo deployment or
  that the full recovered application/browser has passed. The first combined
  production compile reported three missing `available?` warnings; that helper
  has been restored and a fresh combined build remains required.

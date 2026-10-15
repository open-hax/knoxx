---
uuid: "f9159291-0f7f-4797-b149-994860bb5331"
title: "Decompose internal Bluesky tools below the zero-warning file-size limit"
status: incoming
priority: P1
points: 5
labels: "tasks,5sp,bluesky,lint,planning"
category: tasks
epic: "knoxx-knoxx-backend-cljs-lint-remediation"
---

# Decompose internal Bluesky tools below the zero-warning file-size limit

## Context

This is an incoming, unadmitted planning story. Five points are provisional.
No implementation, completed characterization, accepted estimate, readiness,
creation event, write-ID or birth timestamp is asserted by this initial source.
Planning review and lawful Rheos readiness precede RED or application changes.

The public source baseline is Knoxx PR387 head
`2ba3b84de4fff8acbff9dabe9049b0ab287b6635`; private successor work is excluded.
At that commit, `backend/src/cljs/knoxx/backend/domain/bluesky/bluesky.cljs` is
927 physical lines, SHA256
`ce9f12b88f4d369f68d0e79d6a65fa22b4ac6de857f9cc1e20c33139a50bb8b0`.
Its provider orchestration and raw native interop live in a domain namespace.
The unchanged primary lint hook warns at 400 lines and errors at 800 lines.
The separate existing size gate warns at 350 and errors at 500; satisfying only
399 lines would not satisfy every zero-warning check.

The observed production source caller is
`backend/src/cljs/knoxx/backend/infra/agent/hydration.cljs`: its require and
three-argument constructor call use the old namespace. The application packages
are private; their explicit server/aggregate exports and resource manifests do
not expose the old module's 77 CLJS bindings as a published SDK. Preserve the
actual product tool/JS/MCP/HTTP contract, rather than keeping an impure domain
facade for unused source names. A newly discovered real caller must be migrated
explicitly and recorded; do not assume arbitrary external dynamic callers.

The existing remediation epic is
[Knoxx Backend CLJS Lint, Coverage, and Architecture Remediation](../epics/knoxx-backend-cljs-lint-remediation.md),
UUID `knoxx-knoxx-backend-cljs-lint-remediation`. Its literal 40-point estimate
and existing children/history remain unchanged. The new epic relationship is a
reviewable planning proposal, not a claim of an adopted child aggregate.

## Outcome

Move actual Bluesky orchestration into `knoxx.backend.infra.bluesky`, migrate the
observed hydration consumer, and remove the obsolete
`knoxx.backend.domain.bluesky.bluesky` module after qualification. Every resulting
or changed source/test file is below 400 physical lines and meets any stricter
existing size threshold. Owning lint is zero errors and zero warnings with
unchanged configuration. Preserve the 19-tool product ABI and observable behavior.

## Scope

- Keep portable, named Malli parameter laws and pure URI/row/record transformations
  in `.cljc` where practical. Describe contracts before shape, named extern,
  domain and infra composition; preserve their existing admissibility policy.
- Put native params, callback invocation, errors, clock, UTF-8/JS-regex behavior,
  concurrency and JS encoding behind a boundary-specific named extern adapter.
  Do not port JS regex behavior to JVM regex merely to make a file portable.
- Keep current client, credential and media sequencing in infra, through the
  existing 18-method `IBlueskyClient` seam. Preserve the client implementation,
  provider URLs/default accessor, credential source and shared media policy.
- Assemble the existing ordered tool registry in the minimum cohesive source
  groups the real extraction requires. Split read/write assembly only when its
  actual cohesion/size warrants it. There is no fixed module count, one-file-per-
  tool requirement, generic dispatcher or new provider/client protocol.
- Preserve the inexpensive two/three-argument constructor behavior in the new
  infra owner, the aggregate hydration and `create-knoxx-custom-tools-js`
  entrypoints, and canonical/underscored MCP names. Remove old production imports;
  leave immutable archaeology witnesses and historical snapshot manifests intact.

### Characterization prerequisite and admission boundary

Before extraction, obtain an admitted bounded Bluesky characterization slice
under the existing [Coverage-First Characterization](knoxx-backend-lint-coverage-characterization.md)
owner, UUID `knoxx-knoxx-backend-lint-coverage-characterization`, or explicitly
review a different owning route. That card is currently `todo`; this source does
not change its scope, points, status or historical evidence.

The prerequisite is qualified predecessor source and passing results through the
current product constructor with a strict 18-method client fixture, not a claim
that characterization already exists. Record exact source and results before
moving the extraction. It does not require completion of every other hotspot in
the broader characterization card, this extraction, a future Services/CMS rollout
or epic closure. No reverse completion edge is introduced. No dependency header
is invented to model unqualified source as a completed card.

## Non-goals

Other full-lint failures; general `domain.text`, provider/client or API migrations;
new Bluesky features, tool IDs, credentials or schema policy; preserving all 77
old source helper names; impure compatibility aliases; rewriting history;
changing lint thresholds, ignores, test counts or file accounting; production,
provider, database, board-state, Services or CMS rollout effects.

## Acceptance criteria

- [ ] Predecessor characterization is admitted and passes against the unchanged
  product-facing constructor before extraction. The fixture implements only the
  real 18-method client contract and refuses unexpected provider/transport effects.
  No real publish, repost, like, follow, delete, DM or provider request occurs.
- [ ] Preserve the exact ordered 19-ID inventory, labels/descriptions, parameter
  schemas and execution ABI: `bluesky.publish`, `bluesky.profile`, `bluesky.search`,
  `bluesky.author.feed`, `bluesky.timeline`, `bluesky.repost`, `bluesky.like`,
  `bluesky.unlike`, `bluesky.follow`, `bluesky.unfollow`, `bluesky.delete`,
  `bluesky.thread`, `bluesky.notifications`, `bluesky.followers`, `bluesky.follows`,
  `bluesky.chat.list`, `bluesky.chat.read`, `bluesky.chat.send`, `bluesky.chat.react`.
  Preserve all 13 current parameter schemas and resource/registry metadata.
- [ ] Characterize and retain nil/restricted auth behavior and denial before tool
  construction; callback slot a/b/c precedence; validation/update/error order;
  auth before malformed AT-URI refusal; follow/profile session ordering; reply
  references; and existing clock placement. Do not tighten validation or fix an
  unrelated behavior inside the extraction.
- [ ] Preserve image concurrency and input-order results despite reversed completion;
  hashtags-before-URLs facet ordering, UTF-8 offsets, overlaps and native regex
  semantics; and current vector-valued clip rendering. Positive and negative
  fixtures distinguish a contract refusal from setup/missing-symbol errors.
- [ ] The normal predecessor owning lint result contains the real size refusal for
  the unchanged 927-line source. A future namespace/import missing from the
  predecessor is not causal RED. After approved extraction, the same lint policy
  clears that owned diagnostic, all existing assertions remain, and new native
  conversion tests exercise real boundaries rather than namespace spelling.
- [ ] New pure law/shape mechanisms are portable `.cljc` where practical; raw JS
  is confined to named externs. New infra composes effects without policy copies.
  Hydration uses the new owner and the old domain module leaves the production
  graph, with no impure facade. Every changed/resulting source/test file remains
  below 400 and clears the stricter configured size gate without exemptions.
- [ ] Required Node/compiled tests, backend typecheck/server build, owned lint,
  boundary checks and controlled MCP actor/auth contracts have actual terminal
  results. Retain a normal full-lint result with all unrelated debt; do not turn
  a selected pass into a full-gate, deployment or global lint pass.
- [ ] Provide a runnable human verifier and matching `docs/verification/` page,
  following `scripts/verify-mcp-auth.sh` and `docs/verification/mcp-auth.md`.
  It verifies the owned local process/checkout first, exercises the real tool
  catalog/auth boundary and controlled Bluesky behavior, owns and cleans its
  temporary fixtures, reports what each result proves, and exits nonzero on a
  failed check. Use the strict client fixture for effectful behavior; no real
  provider write or existing PM2 restart is authorized. Clearly separate actual
  live MCP observations from fixture-only behavior. If an existing owned seam
  cannot support a required live check, report that concrete gap and review its
  scope before building another platform; do not silently skip or claim it passed.

## Verification

Planning source only at this stage; none of the application commands below has
been run or credited by this card.

After admitted characterization and lawful implementation readiness:

1. Capture the exact predecessor and run the unchanged `pnpm -C backend lint`
   failure; retain full counters and the specific Bluesky file-size finding.
2. Run `pnpm -C backend test` for the registered Node and compiled suites. Its
   existing `run-shadow-tests-ci` execution satisfies the required
   `shadow-cljs compile test` qualification when that command actually runs it;
   run `pnpm -C backend exec shadow-cljs compile test` separately only if the
   normal command did not execute it. Retain test/assertion/failure/error
   counters, not just the compiler exit.
3. Run `pnpm -C backend typecheck` (the existing server compile) and retain its
   zero-warning result; this supplies the same production server target check
   without claiming a second invocation or a release/provider integration.
4. Run owning clj-kondo on every changed/new source/test path with the existing
   backend config, `pnpm -C backend boundary:check`, the existing error-boundary
   check and applicable controlled MCP actor/auth contracts. Record any narrower
   boundary inventory mechanism actually available; do not invent a bypass.
5. Run the configured size gate for the changed/resulting files and one ordinary
   full backend lint. Keep every unrelated diagnostic visible and do not expand
   this story into unrelated cleanup merely to make a total disappear.
6. Run the source-bound human verifier against an owned disposable local Knoxx
   process and retain its actual proof and limitations. A selftest/fake server
   does not supply live-runtime credit. Any newly changed UI needs the existing
   browser-tour convention; no UI change is planned here.

## Risks and estimate

Provisional five-point breakdown: one for integrating and qualifying admitted
predecessor characterization, one for portable laws/shapes, one for named
native-boundary preservation, one for infra
assembly/caller migration, and one for production gates/human verification. This
is an effort proposal, not a guarantee of five files or accepted epic capacity.

Stop and re-estimate if normal discovery finds a real additional consumer,
provider/client/protocol change, unbounded fixture or live-verification platform,
or behavior change that cannot fit this source-preserving slice. Existing async
work is historical evidence, not authorization for a new file-size migration.
Planning review must assess the epic relationship and estimate; it must not
silently rewrite the epic's 40 points or infer spare capacity from old child
arithmetic. No new readiness or implementation permission is claimed here.

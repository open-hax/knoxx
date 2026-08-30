---
uuid: knoxx-ingestion-scoped-service-identity-handoff
title: Replace ingestion identity headers with scoped service delegation
status: incoming
priority: P1
points: 8
labels: tasks, 8sp, has-parent, ingestion, auth, security, boundaries
created_at: 2026-08-30T03:58:03.000Z
category: tasks
---

# Replace ingestion identity headers with scoped service delegation

> GitHub issue: [#287](https://github.com/open-hax/knoxx/issues/287)
> Required child of: `knoxx-frontend-api-fail-closed-identity-defaults` (#2)
> Coupled contracts: `knoxx-translation-config-trusted-auth-context` (#283),
> `knoxx-translations-event-sourced`, and
> `knoxx-versioned-resolved-translation-config` (#275)

## Signal

The fail-closed identity repair in #2 rejects any request that combines an API key with asserted
public identity headers. Two production JVM callers still depend on exactly that combination.

`ingestion/src/kms_ingestion/translation/worker.clj` function `knoxx-headers` sends the configured
Knoxx API key with `x-knoxx-user-email` and can add a batch-specific
`x-knoxx-membership-id`. It uses those headers for translation config, segment reads, agent start,
and polling. The worker's principal resolution deliberately distinguishes explicit batch
membership, a configured system administrator, and same-organization membership, so merely
dropping the headers would erase existing per-batch authority semantics.

`ingestion/src/kms_ingestion/drivers/audio.clj` has a separate `knoxx-headers` producer. It sends
the same API-key/user-email pair for audio agent start and run polling. In addition,
`kms-ingestion.config` defaults `KNOXX_USER_EMAIL` to `system-admin@open-hax.local`, which is an
ambient identity fallback rather than authentication.

Issue #2 must close the public-wire collision with one exact rule: any public identity header plus
session or API-key material on a protected route is rejected before context or effect, with no
strip-and-continue outcome. Its implementation cannot complete until these workers have one
server-verifiable authority each. This child owns that independently implementable JVM/service
migration. Board PR #286 only plans and projects the dependency; it does not deliver the broader
delegation runtime.

The translation worker also has a live legacy single-job carrier. It polls
`/translations/jobs/next`, launches Knoxx work without batch membership, and calls
`mark-job-status` for processing, complete, and failed. That carrier needs its own admitted job
manifest, epoch, and non-ambient status authority; it cannot sit outside the batch-only delegation
and sole-writer rules.

## Scope

1. Inventory both production `knoxx-headers` functions, their configuration readers, and every
   translation/audio Knoxx call site. Include translation config, segment reads, direct agent
   starts, run polling, the `/translations/jobs/next` legacy single-job carrier, batch jobs, each
   job/batch status writer, and audio analysis.
2. Remove every worker-emitted `x-knoxx-*` identity header. Remove the `KNOXX_USER_EMAIL` production
   identity input and its default `system-admin@open-hax.local`; do not retain, rename, replace, or
   move that ambient fallback.
3. Give each ingestion deployment a dedicated machine credential whose API key authenticates only
   the server-mapped ingestion service principal. The key cannot select a membership, accept a
   companion user email, or inherit a default administrator. Missing, malformed, expired, or
   unmapped production credentials fail before any protected request. On protected routes any
   public identity header combined with that API key is rejected before context or effect; there is
   no strip-and-continue case. Credentialless deliberately public reads remain non-authorizing and
   cannot use identity headers to select scope.
4. Audio operations that need no delegated user run only as the admitted service principal, under
   explicit audio-analysis permissions. They send the API key as the sole credential and no
   identity assertion.
5. Treat batch membership from a row as a delegation request, not an asserted identity.
   Under the service API key alone, a named server broker verifies explicit delegation permission,
   resolves one active membership in the requested organization, confirms the current batch tuple
   through the registered OpenPlanner batch adapter, and either refuses or installs an immutable
   Knoxx-owned `SourceManifest`. That manifest binds its id/digest, batch id, allowed documents,
   canonical source revisions/digests, target languages, and segment-coordinate policy, but does not
   pretend that model-selected logical boundaries already exist. A proposal-scoped capability can
   read only those canonical bytes. After a `SegmentationProposal` is validated, Knoxx derives a new
   immutable `TranslationManifest` containing every complete admitted `SegmentCoordinate` and mints
   the separate short-lived capability used by the bound translation turn. One coordinate is
   organization, project, garden, document, source language, source revision, authoritative source
   span/slice identity and digest, segment index, and target language. No worker row, launch payload,
   query, tool argument, caller-provided `source_text`, or untranslated model output can select or
   widen the admitted translation manifest.
6. Admit the legacy single-job carrier separately through the same broker. Under the service API key
   alone, the broker resolves `/translations/jobs/next` output through the existing OpenPlanner
   adapter, then installs one Knoxx-owned job manifest, its explicit service-principal permission,
   current Knoxx authority epoch, organization, admitted complete segment coordinates, and allowed
   job operations. It mints a job-scoped capability or
   returns a typed non-effect. Membership is absent from the job request, capability, and derived
   context; a membership-bearing job is rejected as the wrong carrier type. The job needs no
   fabricated member identity, and ambiguous, unowned, cross-organization, or epoch-stale work never
   launches. The Knoxx authority store owns processing, complete, and failed transitions and projects
   them through the existing registered OpenPlanner boundary; direct `mark-job-status` is retired.
7. For a batch, the delegation capability binds the service principal, batch id, membership id,
   organization id, and carrier type. For a membershipless legacy job it binds the service
   principal, job id, organization id, and carrier type with no fabricated member. Both variants bind
   manifest id and digest;
   Knoxx-owned authority epoch; allowed routes or operations; expiry; and nonce. It is
   audience-bound to Knoxx,
   sender-constrained to the ingestion deployment's workload signing key by public-key thumbprint,
   and cannot be widened by request body, query, or header data. It is not a reusable bearer.
   The canonical batch authority tuple is service principal, batch id, membership id, organization
   id; its manifest and epoch narrow that tuple further and never widen it.
8. Subsequent batch or job work uses that capability authority as its sole credential: never API key plus
   capability and never capability plus identity header. Each call carries a fresh per-request proof
   over the HTTP method, canonical route and complete request target, normalized query, body digest,
   capability id and digest, timestamp, sequence, and unique proof id. The proof demonstrates
   possession of the bound workload key; it is part of the one delegated credential authority, not a
   second principal or permission source.
9. Define one deterministic request-target canonicalizer shared by the worker and verifier. It
   allowlists query keys, rejects blank values plus duplicate and unknown query keys, normalizes UTF-8
   and percent-encoding, and serializes sorted keys. Equivalent reordered queries have one canonical
   target; any missing, added, or substituted selector changes the signed value and fails.
10. The capability resolver rechecks expiry, audience, operation, carrier state, manifest digest,
   Knoxx-owned authority epoch, and the active membership when the carrier is a delegated batch
   before deriving request context. It atomically consumes each `(capability id, sequence, proof id)`
   and records an idempotency receipt. Valid sequential calls use fresh proofs. An exact retry or
   concurrent same-route replay receives the cached receipt or a typed non-effect and causes no
   duplicate protected effect; a proof from another sender or for different request material fails.
11. Couple translation-config authority explicitly to #283. Ordinary membership-bearing API keys
    remain typed member principals under #283. The ingestion deployment key is instead a broker-only,
    membershipless service principal and cannot access the config repository. Only after admission,
    the sender-constrained batch/job capability may authorize a manifest-scoped read-only config GET
    as its sole authority; it can never authorize PATCH, and API key plus capability is rejected.
12. Preserve the live worker's model-selected logical splits through two explicit phases. A first,
   server-owned broker resolves an immutable `ProposalModelSelection` from the Knoxx translation
   config boundary before launch. That selection pins the config resource/version and catalog model
   id used to start the unbound `SegmentationProposal` run; the model receives no config-repository
   credential and cannot choose or widen the selection. The proposal run receives only that selected
   model plus the proposal-scoped source manifest and cannot call `save_translation`, read the config
   repository or config payload, or persist a candidate. It returns only proposed document/span
   boundaries. Server code exact-matches those boundaries to canonical source bytes,
   rejects gaps, overlaps, ambiguity, reordering, or source drift, derives the complete
   `SegmentCoordinate` plus stable attempt/effect ids, atomically installs the immutable translation
   manifest and turn claim, and exact-matches config admission to the pinned proposal selection. A
   changed selection discards the proposal and restarts preflight; it never mixes one model's spans
   with another model/config snapshot. Only then does the server start a separate bound translation
   turn. Server-owned
   launch code for that bound turn attaches an immutable authority envelope containing capability id, service principal,
   carrier type, batch or job id, membership id when applicable, organization id, allowed operations,
   translation-manifest id and digest, every complete immutable segment coordinate, Knoxx-owned authority epoch,
   and a canonical ordered map of every pre-admitted `AttemptEffectIdentity` with its stable attempt
   id and stable effect id, plus the immutable turn-claim reference/digest that resolves to exactly
   that map and a derived authorization-lease id. Resource policies are constructed from that
   envelope, never from the launch payload. A registered launch adapter loads document content by
   the manifest's source revisions/digests, or rejects a supplied content digest that does not match;
   the worker cannot substitute prompt content for an allowed document. Agent/model input cannot
   supply, replace, or widen the envelope, and the run cannot inherit ambient tool authority.
13. Recheck the envelope before every protected effect that outlives the request, including active
    membership and current batch for a delegated batch or current job for the single-job carrier.
    At the named `save_translation` boundary, define and reuse
    one complete immutable `SegmentCoordinate` across this effect fence, #275 attempt admission,
    event grouping, and the current projection unique index: organization, project, garden,
    document, source language, source revision, authoritative source span/slice identity and digest,
    segment index, and target language. `AttemptIdentity` adds the pre-admitted stable attempt id, and
    the event unique index uses that composite rather than the segment coordinate alone.
    `AttemptEffectIdentity` adds the pre-admitted stable effect id to `AttemptIdentity`; the
    server-managed segment ledger atomically claims that admitted attempt/effect exactly once and
    never claims the `SegmentCoordinate` globally. Intentional retranslations with different stable
    attempt ids therefore remain possible on one coordinate. Every retry exact-matches the admitted
    manifest id/digest, authority epoch, effect id, and canonical payload; substituting any of them
    conflicts with no new claim or write. Caller/model substitutions fail before an OpenPlanner
    request. `save_translation` selects one envelope member and exact-echoes its stable attempt id and
    stable effect id; it cannot mint identity from caller/model content, a transport tool-call id, or
    a new save invocation. The server reconstructs the full pre-admitted `AttemptEffectIdentity` and
    rejects an unknown or partial echo before the ledger. The event-owned production schema requires
    both stable ids, and its `CanonicalAttemptEvent` carries the full `AttemptEffectIdentity` plus the
    delegated manifest id/digest and authority epoch as compared/digested facts; neither boundary may
    drop or regenerate part of the identity before OpenPlanner.
14. Bind persisted source content to the manifest rather than the model. The proposal adapter loads
    canonical source bytes for the admitted revision. A model may propose a logical segment boundary,
    but the server must exact-match its source span and slice digest to one unique authoritative
    source slice before translation admission. Altered bytes, a different span, an ambiguous match, a
    gap, an overlap, or reordered/duplicated content fails before the bound turn exists. The later
    `save_translation` call only exact-echoes a pre-admitted member and cannot perform proposal
    admission. The server persists canonical bytes and coordinate from that slice; whole-document
    revision plus segment index alone is not sufficient authority.
15. Treat every signed segment selector as both authorized and consumed. Inventory
    `translation-segments-op`: after exact capability/manifest matching it forwards `document_id`
    and every other allowlisted selector to the data adapter, with no dropped field or default that
    widens the query. Progress and completion derive only from accepted receipts for the current
    manifest, epoch, and effect set/run, never generic segment counts. Reads from another document,
    or a same-document/other-run or other-epoch result, cannot advance current work.
16. Inventory both live `save_translation` sinks but narrow delegated ingestion to the
    OpenPlanner-segment-only `save-openplanner-segment!` path. A delegated envelope cannot carry or
    select publication `dispatch_key`, publication `run_id`, or `save-publication-translation!`;
    injected publication discriminators or policy fail before `content/write!` or any other content
    effect. The publication filesystem trigger/receipt transaction is outside #287 and requires its
    own protocol; this card does not pretend it shares the delegated-ingestion authority store.
17. Keep the new protocol inside Knoxx. A Knoxx-owned `DelegatedIngestionAuthorityStore` is the
    single durable manifest, authority-epoch, effect-fence, transition-CAS, and outbox authority for
    delegated batch and legacy-job execution. It wraps the existing registered OpenPlanner
    direct-Mongo or REST client boundary; it requires no new OpenPlanner endpoint, CAS primitive,
    epoch field, or atomicity guarantee. Exhaustively inventory and migrate
    `next-batch!` queued-to-processing/attempt mutation, `update-batch!`, the update-status route,
    worker `mark-batch-status`, legacy `mark-job-status`, direct Mongo/REST status calls, initial
    processing/run ids, per-document progress, partial/complete/failed/revoked/operator transitions,
    and any `resolve-dispatch!` outcome that currently acts as delegated-ingestion terminal truth.
    No arbitrary direct setter remains. Completion, revocation, failure, and operator decisions all
    use this same Knoxx gateway. OpenPlanner status fields are a derived projection for this workflow,
    not a second CAS authority.
18. Serialize delegated segment effects and carrier state on that Knoxx-owned manifest and epoch.
    One local transaction validates active carrier state, manifest, epoch, full
    `AttemptEffectIdentity`, and canonical retry equality; appends the immutable attempt event; claims
    its logical effect exactly once; and enqueues the existing `save-openplanner-segment!` call. A
    Knoxx-owned `ProjectionDispatchLease` serializes projection intents by complete
    `SegmentCoordinate`, persists the intended attempt ordinal plus canonical projection-payload
    digest, and permits at most one remote invocation for that intent. The existing OpenPlanner sink
    is not a causal/idempotency authority: it stores neither attempt/effect identity nor the lease.
    Therefore an acknowledged response records the immutable local receipt, while a lost or ambiguous
    response never causes automatic redispatch. While the coordinate lease blocks later projection
    intents, the adapter may reconcile an exact read-back of the intended canonical payload as the
    completed effect. Any non-matching or unprovable outcome becomes a durable
    `AmbiguousProjectionOutcome`: carrier completion and later projection dispatch on that coordinate
    remain blocked for operator resolution, with no claimed receipt and no overwrite. Equal retries
    reuse the local event/effect state and cannot create another semantic attempt or remote call. A
    terminal CAS uses expected manifest, epoch, and transition id
    and serializes with every leased outbox dispatch, so an effect is either observed before terminal
    linearization or never sent; an old-epoch intent is refused locally with zero later dispatch.
19. Keep scopes honest while removing second terminal truth. Immutable translation attempt/effect
    receipts and publication immutable workflow evidence may remain auditable local records, but a
    mutable Knoxx `resolve-dispatch!` outcome cannot decide delegated-ingestion batch/job terminal
    state. Such evidence is derived from the authoritative gateway/outbox receipt and cannot outvote,
    reopen, or independently close the carrier.
20. Make terminal transitions idempotent and observable. A completion-versus-revocation race on the
    same epoch lets exactly one transition id win; the loser receives the canonical terminal receipt
    and cannot overwrite it. The Knoxx store publishes that receipt through its durable outbox so
    adapters invalidate derived leases and cancel pending and active runs. Delayed delivery cannot permit a
    write because the Knoxx authority store rejects the old epoch before outbox dispatch; retry
    reconciliation never reopens a terminal batch or job. Every subsequent translation write returns
    a terminal non-effect.
21. Preserve legacy batches only when the broker can resolve their configured service principal to a
   single explicitly delegable active membership. Ambiguous, cross-organization, or unresolved
   legacy work stays queued or fails with a typed non-effect result; it never falls back to system
   administrator.
22. Run a whole-repository identity-emitter scan over production sources, tests, generated assets,
    scripts, and operator docs. Update `scripts/verify-publication-tour.sh` and other fixtures that
    currently authenticate with public identity headers; retain such headers only in clearly named
    malicious-header probes.

## Contract / invariants

- An API key authenticates only the server-mapped ingestion service principal and cannot select a
  membership.
- A batch membership is a delegation request, not an asserted identity. Possessing a batch id or
  membership id confers no authority.
- Batch delegation requires explicit delegation permission, one active membership, the matching
  organization, and a current batch confirmed by the authoritative adapter. Legacy single-job
  admission requires explicit job permission, matching organization, and the current OpenPlanner
  job manifest/epoch without fabricating membership.
- No request contains more than one credential authority. Broker requests use only the API key;
  delegated work uses only the issued sender-constrained capability and its per-request possession
  proof as one composite credential authority.
- Public `x-knoxx-*` headers are always untrusted and never distinguish an internal worker.
- On protected routes, any public identity header plus session/API-key credential material is
  rejected before context or effects, with no strip-and-continue path. Credentialless public reads
  remain non-authorizing.
- Ordinary membership-bearing API keys and the broker-only membershipless ingestion key are distinct
  principal types. Only an admitted capability may authorize ingestion config GET; it never PATCHes.
- A capability grants only its bound tuple and operations. Unknown fields, blank identifiers,
  malformed claims, tuple/manifest/resource changes, and missing issuer/audience metadata fail
  closed.
- Translation and audio retain their existing domain behavior, ordering, payloads, polling, and
  error handling after the authority transport changes.
- No fixed service identity, API key, or capability becomes ambient system-administrator authority.
- Arbitrary membership, tuple substitution, forged sender proof, and other confused-deputy
  negatives produce zero protected effects and no administrator fallback.
- Spawned runs and internal tool effects receive authority only from their immutable server-owned
  envelope. The agent, prompt, payload, and tool arguments cannot select or widen that authority.
- `SegmentCoordinate`, the complete immutable segment coordinate, is organization, project, garden, document, source
  language, source revision, authoritative source span/slice identity and digest, segment index, and
  target language. Event grouping and the current projection unique index use that coordinate.
  `AttemptIdentity` additionally binds stable attempt id and supplies event uniqueness;
  `AttemptEffectIdentity` adds the admitted effect id for exact-once ledger claims. A delegated
  attempt also exact-matches manifest and epoch on retry.
- The Knoxx-owned `DelegatedIngestionAuthorityStore` is the sole delegated-ingestion
  manifest/epoch/effect-fence/terminal CAS authority. OpenPlanner is reached only through its
  existing stable client boundary and stores a derived carrier/segment projection, not a competing
  epoch or terminal decision.
- The terminal batch/job/revocation CAS and each admitted segment outbox intent share the Knoxx
  authority epoch: a leased effect observed before the transition may stand, but no old-epoch intent
  is dispatched after terminal linearization. Publication immutable receipts remain evidence for a
  separate workflow and are never delegated-ingestion terminal authority.

## TDD / proof

1. RED-prove that the exact current translation and audio `knoxx-headers` producers send API key
   plus identity and that the translation producer accepts arbitrary batch membership.
2. GREEN-prove API-key-only audio and non-delegated service operations resolve exactly the configured
   service principal. Pair each public identity header with the valid API key and prove mandatory
   rejection before context, policy lookup, or downstream work—never strip-and-continue. Identity
   headers alone, a second credential, and unmapped keys fail too; a credentialless public read
   remains non-authorizing.
3. Exercise capability issuance with an authorized service principal and a current active
   batch/membership/organization tuple, then with an admitted legacy single-job manifest from
   `/translations/jobs/next`. Capture each credential and prove its decoded/validated claims bind
   carrier type, complete immutable coordinate set, authoritative manifest id/digest, Knoxx-owned
   batch or job authority epoch, audience, operations, expiry, and nonce. The batch variant binds
   its admitted membership; the legacy-job variant proves membership absent and explicit
   service-principal job permission.
4. Send an arbitrary membership with a valid service key; a swapped batch, membership, or
   organization; and an expired, replayed, forged, or cross-route capability. Every variant fails
   before policy lookup, segment read, agent start, or audio effect, with no administrator fallback.
5. Prove the valid translation path retains translation config, segment read, agent start, and
   polling behavior across explicit-membership, same-organization, and safely admitted legacy
   batches. Prove the legacy single-job path retains polling plus processing, complete, and failed
   behavior under a job-scoped capability and Knoxx transition receipts projected through the
   existing OpenPlanner adapter. Batch/job
   completion or revocation invalidates its capability; `mark-job-status` is unreachable directly.
6. Prove the valid audio path retains audio start and run polling under the service principal,
   including concurrency limits, timeouts, payloads, and error behavior.
7. Add a cross-process fake Knoxx server that captures every request. Valid worker traffic contains
   exactly one credential and no public identity; collision and delegation negatives produce zero
   protected effects.
8. Make the whole-repository identity-emitter scan a regression assertion. Production worker,
   frontend, self-client, proxy, and media paths contain no unauthorized public identity emitter;
   malicious-header probes remain explicit and cannot be used as helper code.
9. Run focused ingestion translation/audio tests, focused backend credential/capability tests, both
   relevant full suites, real-server probes, compile/typecheck, and strict changed-surface lint with
   zero warnings.
10. Exercise the live dynamic-split path as an unbound proposal run followed by admission and a
    separate bound translation turn. Prove the broker resolves and pins `ProposalModelSelection`
    from the Knoxx config boundary before the proposal turn, the run receives that selected model and
    source-manifest bytes but no config credential or payload, emits a `SegmentationProposal`, and
    cannot call `save_translation` or persist a candidate. Reject ambiguous, gapped, overlapped,
    duplicated, reordered, and source-drifted
    proposals before admission. Capture the bound run and prove its immutable authority envelope binds
    the complete coordinate, capability id, translation manifest, Knoxx epoch, translation resource
    scope, immutable turn-claim digest, and every pre-admitted `AttemptEffectIdentity`. Make `save_translation`
    exact-echo the selected stable attempt/effect ids and prove it cannot mint either from model
    output, caller bytes, transport tool-call id, or invocation order.
    Substitute organization, project, garden, document, source/target language, source revision,
    authoritative source span/slice, source text, segment index, manifest, and epoch through launch
    input and `save_translation` arguments; every variant fails before OpenPlanner. The valid
    model-proposed logical boundary exact-matches one authoritative source slice before the bound
    turn, persists canonical source bytes once, and leaves logical segmentation behavior unchanged.
    Change the config version/model between proposal and admission and prove preflight discards the
    proposal and restarts instead of launching a mixed-snapshot bound turn.
11. Exercise a completion/revocation race at agent start, before the first effect, and during an
    in-flight `save_translation`, plus a completion-versus-revocation race on one expected epoch.
    Exactly one terminal CAS wins; the authoritative transition invalidates the epoch-bound fence and
    cancels pending and active work. There are zero post-transition writes even when outbox/cancel
    delivery is delayed, while a pre-transition committed write remains exactly once and auditable.
12. Capture a valid sender-constrained proof and run an exact same-route replay before and after the
    legitimate call, plus concurrent duplicates. The server atomically admits at most one execution,
    returns the same cached receipt or typed non-effect to retries, and produces no duplicate
    protected effect. Valid translation config, segment, start, and polling sequences use fresh
    proofs and retain normal behavior.
13. Canonicalize two semantically equivalent reordered/percent-encoded queries and prove they produce
    the same complete request target before proof-id replay enforcement. Reject duplicate, unknown,
    blank, and ambiguous encodings. For segment reads, a query-substitution probe changes each of
    `project`, `org_id`, `document_id`, `source_lang`, `target_lang`, and `limit`; every change
    produces signature failure before route handling or data access. Separately prove that
    `translation-segments-op` consumes and forwards every validated selector, especially
    `document_id`, into the adapter. Seed another document plus same-document/other-run and
    other-epoch rows; only receipts for the current manifest, epoch, and effect set advance progress
    or terminal state, and generic counts cannot close work.
14. Replace every delegated-ingestion status writer with the Knoxx-owned gateway: `next-batch!`,
    `update-batch!`, the update-status route, direct Mongo and REST client setters,
    `mark-batch-status`, `mark-job-status`, and mutable local `resolve-dispatch!` terminal outcomes.
    Route claims, attempts, processing/run/progress, completion, failure, and revocation through the
    same Knoxx manifest+epoch CAS and outbox protocol; make any arbitrary direct status call fail the
    regression inventory. Fault-inject before and after the Knoxx terminal status CAS and on both
    sides of the existing OpenPlanner adapter call; drop/duplicate the outbox receipt, retry the same
    transition id, and race an old-epoch segment intent. Race completion against revocation with
    different transition ids. The canonical terminal receipt converges without reopening or a
    post-linearization dispatch, while immutable translation and publication workflow receipts remain
    evidence and cannot decide batch/job status. Assert that neither direct-Mongo nor REST mode
    requires a new OpenPlanner route, field, CAS, or atomic writer.
15. Pass a delegated ingestion envelope carrying `dispatch_key`, publication `run_id`, publication
    sink policy, and each discriminator in nested/renamed forms. Every case is rejected before
    `save-publication-translation!` and `content/write!`; the valid envelope can select only
    `save-openplanner-segment!`. Test the publication workflow separately without claiming its
    filesystem write shares #287's delegated-ingestion authority store.
16. Cross-test #283's typed authority seam. A membership-bearing member API key retains its admitted
    config behavior; the raw membershipless ingestion key reaches only the broker. A valid admitted
    capability as sole authority can GET only its manifest-scoped config, never PATCH. API key plus
    capability and every tuple/manifest/epoch substitution fail before repository I/O.
17. Make the #287 fence, event grouping/current projection, event uniqueness, and #275 attempt
    admission serialize and compare the byte-identical complete immutable segment coordinate. The event unique
    index uses `AttemptIdentity`, while the projection unique index uses `SegmentCoordinate` and the
    exact-once ledger uses `AttemptEffectIdentity`; none substitutes the shorter or broader identity.
    Save two intentional retranslations with different stable attempt ids on the same coordinate and
    prove two events plus consecutive ordinals, one current projection, and no global-coordinate
    claim conflict. Cross
    organization/project/garden/document/source-language/source-revision/source-span/segment/target
    fixtures never alias; attempts also exact-match manifest and epoch. Migration detects the former
    short-key collisions instead of silently overwriting either record.
18. At the real Knoxx authority-store and existing OpenPlanner adapter boundary, let two pre-admitted
    members on the same `SegmentCoordinate`
    with different stable attempt ids succeed exactly once each. Capture both requests and prove the
    full distinct `AttemptEffectIdentity`, manifest id/digest, expected epoch, and canonical payload
    reach the atomic local writer and existing adapter call. An unknown or substituted attempt,
    effect, manifest, or epoch produces no ledger claim or outbox dispatch; a same-identity equal
    retry returns the original receipt. Drop the response after the existing sink applies the first
    write: the coordinate lease prevents the second retranslation and any repeat remote call until an
    exact canonical projection read-back records the first receipt. Return a non-matching or
    permanently unprovable read-back and prove `AmbiguousProjectionOutcome` blocks completion and
    later same-coordinate projection dispatch without fabricating a receipt or overwriting state.

## Non-goals

- Treating batch, membership, organization, email, or identity headers as credentials.
- Reusing the generic API key as both service authentication and delegated user authority.
- Building a generic delegation framework for unrelated services.
- Changing translation selection, segment semantics, audio analysis behavior, or queue policy.
- Delivering the publication filesystem trigger/receipt atomicity protocol.
- Claiming that board PR #286 delivers this runtime; it records the required child work only.

## Done when

Translation and audio ingestion send no public identity headers, each protected request has exactly
one server-verifiable authority, audio runs as its service principal, and translation batch plus
legacy single-job authority comes only from a narrowly bound policy-admitted sender-constrained
capability. Exact retries cannot duplicate an effect; signed request targets cover selectors that
the route actually consumes; source text exact-matches one authoritative slice; and the complete
immutable coordinate cannot alias another project, garden, locale, revision, span, segment, or
target. Delegated ingestion can reach only the OpenPlanner segment sink. Every batch/job writer uses
the one Knoxx-owned manifest+epoch CAS and outbox gateway over the existing OpenPlanner adapter,
which produces zero post-transition dispatches without erasing immutable
translation/publication evidence. The proposal phase preserves model-selected logical splits, and
the projection adapter never treats a coordinate-only upsert as causal/idempotency evidence: an
ambiguous response is reconciled under a per-coordinate lease or fails closed. The required happy
paths remain compatible.
Issue #283 preserves typed member-key behavior and read-only delegated config, and #2 can reject the legacy
collision without breaking either worker. Board PR #286 remains planning-only proof, not delivery of
this runtime.

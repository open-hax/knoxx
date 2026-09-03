---
uuid: "knoxx-versioned-resolved-translation-config"
title: "Emit immutable provenance from the resolved translation config boundary"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent", "translations", "transduction", "config", "provenance"]
created_at: "2026-08-29T18:22:35Z"
points: 5
category: tasks
---
# Emit immutable provenance from the resolved translation config boundary

> Parent epic: `knoxx-transduction-provider-pipeline`
> GitHub issue: [#275](https://github.com/open-hax/knoxx/issues/275)
> Depends on: `knoxx-translation-config-publication-dependency-removal` (#273)
> Depends on: `knoxx-cms-contract-validation` and
> `knoxx-file-resource-repository-provider` for provider-neutral resource versions
> Depends on: `knoxx-resource-repository-snapshot-observation` (#282) for one
> provider-neutral multi-resource observation and exact absence entry
> Depends on: `knoxx-translation-config-trusted-auth-context` (#283) for session/API-key
> scope that cannot be selected by caller identity headers
> Integrates with: `knoxx-translations-event-sourced` for server-admitted attempt identity

## Purpose

The production `knoxx.backend.infra.routes.translation-config/resolved-config!` boundary
currently returns only three effective values. Once the global default and optional
organization override have been merged, their resource revisions disappear. A candidate can
therefore name the values it used but cannot prove which immutable policy revisions selected
its provider.

Evolve the existing facade to preserve its read-only consumer and emit one authenticated,
immutable artifact for provider invocation. Do not add a parallel configuration authority or
reconstruct provenance after the provider call.

## Contract

### One authority, two typed operations

The existing translation-config facade owns every effectful repository operation: it invokes
`observe-many`, sequences data-dependent fixed-point expansion/retry, and passes each immutable
`ResourceObservation` through explicit translation-config law validation to one pure domain
resolver. The domain resolver performs no I/O, validation, or structure conversion; it owns
only deterministic global/organization precedence and catalog/reference selection. Pure
`shape.translation-config` morphisms own wire/domain structure conversion, while
`law.translation-config` owns schemas, invariants, and boundary admissibility. Those shape and
law namespaces add no configuration authority. The facade exposes two explicit operations over
that one domain resolver:

1. **Inspect effective config** — the existing authenticated
   `GET /api/translations/config`/`config-response!` use case accepts only trusted
   session/API-key organization context from #283 and returns a typed `EffectiveConfigView`.
   Client identity headers cannot select its scope. It performs no attempt reservation or
   write and carries no provider-invocation authority.
2. **Admit config for attempt** — `admit-resolved-config-for-attempt!` requires the
   server-admitted attempt/source/operation context and returns the attested
   `ResolvedConfigArtifact` below.

The two output types are not interchangeable: provider invocation rejects an
`EffectiveConfigView`, even when its values equal the current artifact. Both operations use the
same effectful observation facade and pure resolver and, at one repository snapshot, return
identical effective values. This is one authority with two use cases, not a second adapter,
precedence implementation, orchestration path, or config store.

### Attempt-admitted artifact

The resolved artifact contains:

- effective typed configuration values;
- the server-derived effective organization used for resolution plus authenticated actor and
  delegation evidence when a trusted system administrator targets another organization;
- the server-admitted composite attempt identity (event grouping key plus caller-stable attempt
  id), immutable source identity/revision, and canonical operation;
- the repository contract/schema version and snapshot observation identity;
- the final composite `RepositoryOperationReceipt` identity/digest and its ordered
  per-coordinate `authorization-policy-version` bindings for the exact observation, preserving
  distinct requested versions of the same resource identity;
- exact canonical identity and resource-scoped version of the global default;
- exact canonical identity and resource-scoped version of the optional organization override;
- exact canonical identity/resource version of the selected model-catalog entry plus the
  version-pinned closure/digest of any referenced provider/allowlisting policy;
- a resolution-policy/schema version; and
- a deterministic artifact identity/digest over the ordered contributing facts; and
- a trusted immutable resolution attestation covering the entire artifact, including absence.

Resource versions come from the provider-neutral repository contract and its retained
revisions, never file mtimes, manifest hashes, current re-reads, or caller input. Resolution
uses #282's `observe-many` operation with provider-neutral coordinates—`:current` for the global
and organization-override identities and exact retained versions for version-pinned references—
rather than composing sequential single-resource reads. The returned observation has one
linearization point and one scoped identity over every requested coordinate, present version,
and permitted current-only absence entry.
When the override is absent, the artifact carries that repository-authoritative absence for its
exact canonical identity, not a caller-computable marker.

The selected model coordinate is data-dependent, so the effectful facade reaches one final
fixed-point observation without pretending sequential reads are atomic. It provisionally
observes the two current config coordinates and gives that immutable observation to the pure
resolver to derive only the selected model/catalog identity: today's config value is an ID, not a
versioned resource reference. The next pass observes the config coordinates again plus that model
identity as `:current`. Only the returned model authority record and public version may derive its
validated exact provider-policy reference coordinates. Each newly observed exact revision may
introduce another pinned reference frontier, so the facade repeats until the selected model
identity and current version are unchanged, the complete transitive closure contributes no new
or changed coordinate, and every parent still names the exact observed child revision.

The final `observe-many` contains both current config coordinates, the selected model as
`:current`, and its complete exact-version policy closure. Translation-config law verifies that
the final config still selects that model, the final model record still names the direct pinned
dependencies, and every transitive parent still names its observed child before the pure domain
resolver consumes the admitted typed result. A config selector, current model, direct reference,
or transitively discovered coordinate change restarts the full sequence. The final observation's
returned model version is the artifact's selected-model revision; no step fabricates that version
or reads the model outside `observe-many`. Advancing current policy from P1 to P2 cannot move a
reference pinned to P1. An unavailable pinned version fails with the canonical reference error
and produces no artifact, attestation, admission, or provider call; it is never replaced by
current or treated as optional absence. Only the final single observation is attested. An
unrelated catalog entry is excluded and cannot rotate the artifact.

An authorization denial from an expanded **provisional** frontier cannot reveal which coordinate
failed and cannot be treated as absence, but it also cannot permanently retain a speculative
coordinate that current config/model authority no longer selects. Within the same server-owned
retry budget, the facade discards that entire failed frontier and reboots from only the canonical
current config coordinates, then reobserves the newly selected model as `:current` and derives a
fresh exact closure. A denial during either minimal current pass is terminal. The facade records
the canonical fingerprint of each denied expanded frontier; deriving the same frontier again is
terminal with exactly `{:error/type :authorization/forbidden :error/reason
:resource-observation-denied}` rather than retrying or dropping a still-selected member. Only a
different freshly derived frontier may be attempted. If it retains the denied member it denies
again and terminates under the same bounded rule; if current authority has removed that obsolete
coordinate, the new complete frontier may stabilize normally. No failed pass emits a partial
observation/receipt or externally visible intermediate error, and no rebootstrap reuses prior
authorization evidence.

The attempt-admission operation mints an opaque `ResolvedConfigAttestation` using server-held
signing/MAC authority or an equivalent append-only receipt store outside caller-controlled
bytes. It binds the complete composite attempt identity, authenticated actor/origin
organization, effective organization plus delegation evidence, repository contract/schema
version, snapshot/observation identity, the exact final `RepositoryOperationReceipt` and every
ordered per-coordinate authorization-policy-version binding, every present resource version,
the absent-override witness, selected model-catalog revision/provider-policy closure,
resolution-policy/schema version, and artifact digest. The authorization entries are keyed and
digested by complete selector/version/reference-provenance coordinate, so P1 and P2 of one
identity cannot collapse, reorder, or substitute each other. Admission rejects a receipt for
another principal, effective
scope, capability, requested coordinate set, observation identity, or provisional fixed-point
read. Validation verifies the attestation and immutable receipt evidence without re-reading
current config. A later override creation therefore does not invalidate an
already-started attempt, while merely omitting an existing override or recomputing the digest
cannot fabricate a valid artifact. Unrelated resource writes cannot rotate the artifact.
Attempt consumers change atomically to use the admitted operation; inspection consumers retain
the read-only operation on the same configuration boundary.

Fixed-point sequencing is bounded by a server-owned `ObservationRetryPolicy`: a maximum number
of complete observation passes, a monotonic server deadline, and cancellation checked before
and after every repository observation. Clients cannot extend the budget or supply its clock.
Each selector or policy-reference change, denied expanded frontier, and rebootstrap consumes a
pass and restarts from the canonical config coordinate set. If the set never stabilizes before
any limit, the facade returns the stable typed error
`{:error/type :translation-config/unstable-observation :error/reason
:retry-budget-exhausted :observation/attempt-count <bounded-count>}`; cancellation uses the same
error type with `:error/reason :cancelled`. Neither result exposes resource values or presence,
mints a `ResolvedConfigArtifact`/attestation, installs attempt admission, or invokes a provider.
Any provisional repository operation receipts remain audit evidence only. A stable pass within
the budget continues through the unchanged final-observation validation and admission path.

Authorization policy remains separate from semantic repository observation identity. If policy
V1 allows the final observation and V2 later allows the same unchanged resources, the resource
versions and observation identity remain stable but the new operation receipt, resolved
artifact, and attestation bind V2 and therefore rotate. If V2 denies, no new artifact is
admitted and the V1 receipt cannot be replayed as authority. An idempotent retry of the already
admitted composite attempt retains its original V1 artifact as historical evidence rather than
performing a new read under V2.

There is no free-floating reusable attestation. Before provider invocation, the server derives
the same canonical `AttemptIdentity` used by `knoxx-translations-event-sourced`: the full
`{:org-id :document-id :segment-index :target-lang}` grouping key, where `:org-id` is the
server-derived effective organization, plus the caller-stable `attempt_id`.

Every translation-bound agent turn installs an immutable `TranslationTurnClaim` containing a
non-empty, canonically ordered collection of provider-neutral attempt-claim members, one for every
exact segment/target composite that turn may save. The initiating workflow accepts or creates
stable member ids with their exact grouping/source/request facts before provider work. Duplicate
composites and non-canonical encodings are rejected before persistence, and the claim
identity/digest makes member addition, removal, or replacement a conflict after admission.
Publication dispatch and ordinary-chat preflight use distinct turn-claim variants but the same
turn-wide admission law; an unbound chat turn does not receive `save_translation`. Before config
observation, the canonical claim binds its variant and stable variant-specific initiator facts:
publication-dispatch binds the durable dispatch-claim identity and workflow idempotency key, while
ordinary-chat binds the interactive translation-start claim identity. These are immutable
comparison facts, not alternate admission-slot keys.

Before claim persistence, the initiator owns a stable `turn_id` and canonical member-set digest.
Every member must name the same server-derived effective organization; a mixed-organization
member set is rejected before config observation or persistence. The durable admission slot
identity is the pair of that effective organization and `turn_id`, and `turn_id` is unique only
within that effective organization. The member-set digest, final claim digest, and execution
digest are immutable record facts, but never key the admission slot; changing one cannot create a
second slot for the same organization and turn id. The facade performs one coherent authenticated
config observation for that complete member set and mints an immutable
`TranslationTurnExecutionSnapshot`. The snapshot is bound to exactly that `turn_id` and member-set
digest and carries the common provider/model identity, exact semantic
config/provider-policy resource revisions and repository operation evidence, normalized provider
session parameters, and canonical
`provider-session-config-digest`. Member-specific source and request facts remain in their
attempt artifacts but cannot change that execution digest. Authorization-operation receipts and
their policy versions remain evidence outside the provider-session digest: an authorization-only
allow-policy rotation may rotate that evidence without pretending the model session changed. If
the members legitimately resolve to different execution digests, they are inadmissible as one
turn. Preflight returns
`{:error/type :translation/turn-partition-required :turn/id <turn-id>
:partitions <canonical-ordered-partition-plan>}` to the authorized initiator. The plan groups exact
member identities by observed provider-session config digest in canonical order, but is
non-authorizing: it carries no claim, snapshot, artifact, admission receipt, or reusable token and
starts no provider/model session. The whole attempted turn persists nothing. The facade does not
mint or derive a child `turn_id` and never auto-installs any planned group. Each planned group
requires a new initiation with its own stable `turn_id`, claim variant, and variant-specific
initiator facts before a fresh observation/admission. A lost partition response is safe to retry
because there is no parent or child state; the fresh observation returns its current canonical
plan or admits one now-coherent turn. Separate explicit initiations are independent turns, not a
partially committed parent batch. The snapshot is not a free-floating attestation and cannot be
replayed under another turn or member set. The final turn-claim digest then includes the snapshot
identity/digest; the snapshot never includes that final digest, so the identity graph is canonical
and non-circular.

The facade derives the complete ordered map of member artifacts from that exact turn snapshot
instead of performing a new current-config observation per member. Each embedded
`AttemptConfigAdmission` contains its composite identity, immutable source/canonical request
facts, turn-snapshot identity/digest, and full attested artifact. Candidate artifacts and staged
map entries are not reservations and cannot authorize provider invocation.

One atomic unique-insert/compare operation, `TranslationTurnConfigAdmission`, authorizes and
installs the final turn claim, execution snapshot, complete ordered member-admission map, and one
immutable `TranslationTurnAdmissionReceipt`. It targets that organization-scoped admission slot.
Its internal installed-slot read precedes any new `observe-many` or current reauthorization on
every initiation/retry. A canonically equal installed record is the already-linearized result and
returns unchanged without new current reauthorization. Empty and mismatched installed-slot
outcomes remain internal and are collapsed behind one non-disclosing authorization boundary
before either outcome can become caller-visible.

That boundary is a server-owned `authorize-translation-turn-admission-slot` operation over the
authenticated principal, effective scope/delegation, required turn-admission capability, and the
canonical slot coordinate `(server-derived effective organization, turn_id)`. Slot authorization
only permits the caller to learn that its stable facts conflict with a slot it is currently
allowed to address; it cannot authorize or satisfy config observation, whole-turn admission, or
provider/session work. A denial returns the same canonical, byte-identical result for empty and
mismatched slots, emits no conflict or existence bit, and reaches no `observe-many`, repository
observation, candidate construction, provider call, or session start. An authorized mismatch
returns one redacted, non-enumerating conflict shape. An authorized empty slot proceeds through a
fresh `observe-many`, complete candidate construction, and the final atomic compare/install.

The slot-disclosure decision linearizes with authorization-policy rotation under the
`TranslationTurnAdmissionAuthority` fence, but its allow result is not reusable admission
authority. The empty path must still perform the exact-resource authorization below at final
linearization. If a concurrent winner appears after the early internal read, the final operation
again returns a canonically equal installed record without new reauthorization; otherwise it
collapses the still-empty and mismatched outcomes and freshly repeats the slot authorization under
the fence. A current slot denial returns the same canonical denial for either outcome, an allowed
mismatch returns the redacted conflict, and only an allowed empty outcome proceeds to the final
exact-resource authorization and install. Neither the early slot-authorization result nor its
receipt can be reused at this later comparison.

At the empty slot's final linearization point, a fresh server-owned
`authorize-translation-turn-admission` operation evaluates the authenticated principal and
effective scope/delegation against the required turn-admission capability and the exact canonical
resource-coordinate set from the complete observation. The earlier `observe-many` receipt remains
historical provenance and never satisfies or authorizes this admission operation.

Authorization-policy rotation and admission take the same serializable
`TranslationTurnAdmissionAuthority` fence (or one equivalent co-located transaction). The current
allow decision, authorization-policy version, unique-slot compare, and whole-record install
linearize before that fence releases. A rotation ordered first returns the canonical denial and
installs nothing; an admission ordered first commits under the then-current allow and records its
exact authorization-policy evidence. Later revocation cannot rewrite that admitted history, but no
ordering may commit a turn after a denial already linearized. Implementations cannot approximate
this contract with an unfenced check followed by an independent insert.

The receipt binds the authenticated principal, effective scope/delegation, required capability,
stable turn id, claim variant and variant-specific initiator facts, canonical member-set digest,
execution snapshot/digest, every ordered member artifact, and the exact authorization-policy
evidence used at the operation's linearization point. That evidence comes from the fresh fenced
operation, not from the observation receipt. Nothing is externally visible until the
whole map commits; there is no partial member, pre-artifact, or `:pending` turn reservation. The
agent session starts only from that complete admitted record. It receives the turn claim,
snapshot, and member map through authenticated server context; each later `save_translation` call
only selects, echoes, and validates one member and cannot become an identity-minting,
collection-expansion, configuration-selection, or reauthorization boundary. Raw attempt ids are
not globally unique:
the same value under a different segment or target-language grouping is a distinct embedded
attempt. The atomic turn install is every member's shared durable freshness boundary.

A crash or injected failure before that atomic commit leaves no persisted turn claim, execution
snapshot, member admission, or provider/session side effect. Retry may reuse only the initiator's
stable turn/member ids. It first performs the internal installed-slot comparison. An equal record
returns as durable history. Every non-equal outcome must pass the common slot authorization before
an authorized empty slot can perform a fresh `observe-many` authorization/config observation and
build a new complete candidate turn; an unauthorized caller receives the same denial whether the
slot is mismatched or empty. Retry equality is evaluated over the effective organization, stable
turn id, authenticated principal/effective delegation/required capability, claim variant and its
complete stable variant-specific initiator facts, and complete caller-stable canonical
member/source/request facts.
The freshly observed candidate execution digest is server-derived, cannot be supplied by
the caller, and is excluded from retry equality; it is discarded when those stable facts match an
installed record.

The earlier operation receipt remains audit history and cannot authorize an empty-slot retry. A
current denial on an empty slot installs nothing; an allow-to-allow policy or semantic config
rotation is reflected by the fresh whole-turn observation. A crash after commit but before
response returns the exact complete record on an equal retry before any new current authorization.
That installed readback relies on the installed record, not the earlier receipt. A postcommit
policy denial cannot invalidate or rewrite the installed result; it blocks only a new empty-slot
admission. Concurrent callers with equal stable facts, including the same claim variant and
variant-specific initiator facts, may build different unattached candidate turns whose execution
digests straddle a semantic config change, but exactly one whole record wins; every loser discards
its candidates and returns the installed winner. Reusing the same organization-scoped slot with a
changed authenticated initiator, claim variant, variant-specific initiator facts,
source/request facts, or turn membership returns the redacted conflict only after current slot
authorization; a denied caller instead receives the slot-existence-neutral canonical denial before
repository observation.
After installation, substituting the installed execution digest in a final claim, authenticated
session, member artifact, or save also conflicts. A genuinely different execution configuration
requires an explicit new initiation with a stable `turn_id` and separately admitted turn; the
facade cannot infer it from the installed slot. There is no snapshot-derived missing-member
installation and therefore no later reauthorization seam.

A canonically equal retry of the same composite identity retrieves the same attested
artifact—even after configuration changes or a lost response. Reusing the same composite with
changed source/request facts, or replaying its token under another composite identity, conflicts
before provider invocation. Historic verification remains valid without a wall-clock expiry
silently invalidating durable evidence; freshness is enforced by one-time composite attempt
binding, not by trusting client time.

Provider selection receives this artifact and durable attempt/candidate evidence carries it
unchanged. A configuration update after attempt admission creates a different artifact for a
later attempt but cannot rewrite the provenance of the admitted attempt. The artifact is valid
only for its effective organization, authenticated actor/delegation, source, operation, and
attempt; client-fabricated, unauthorized cross-tenant, cross-attempt, stale, missing, or
value/revision-mismatched artifacts fail before provider invocation without an existence leak.

The publication-free namespace closure from #273 remains an invariant.

## TDD / proof

1. Global-only resolution returns the exact global revision, snapshot-bound absence witness for
   the override identity, byte-stable artifact identity, and trusted attestation.
2. Global plus organization override resolution names both ordered revisions and binds to the
   effective organization. An ordinary actor cannot select another tenant; an authenticated
   system administrator's explicit delegated target succeeds and remains bound with actor
   evidence.
3. Updating either contributor or the repository contract/schema version changes the artifact;
   updating the selected catalog revision or changing its pinned provider-policy reference also
   changes it. Advancing the provider policy from P1 to P2 while the selected catalog still pins
   P1 leaves the artifact on P1; updating an unrelated resource or unselected catalog entry does
   not change it. Rotate only authorization policy V1 allow to V2 allow:
   semantic resource/observation identity remains stable while the exact final operation
   receipt/policy bindings and new-attempt artifact rotate. V2 deny admits no new artifact;
   replaying the V1 receipt or substituting another actor/scope/coordinate-set receipt fails. When
   one closure contains P1 and P2 coordinates for the same identity, dropping, reordering, or
   substituting either coordinate's authorization entry fails attestation/admission.
4. Resolve with no override, create one, then persist a candidate: its receipt retains and
   verifies the originally attested absence without re-reading current config. Updating a
   present contributor exercises the same race law. Deterministic fake/file barriers prove the
   observation is a complete before-state or after-state, never a torn pair from sequential
   reads. Race a config selector change, selected catalog update, and provider-policy reference
   update during fixed-point expansion; the admitted artifact names one final coexistent closure
   or retries, never a mixed catalog decision. With catalog C1 pinned to policy P1, advance the
   policy's current revision to P2 between passes and prove the final exact-version coordinate
   still returns P1. Extend the fixture to C1 -> P1 -> Q1, advance Q's current revision to Q2,
   and prove recursive discovery observes Q1 rather than stopping at P1 or floating to Q2. Only
   catalog C2 or a newly selected pinned parent explicitly naming P2/Q2 may produce that closure;
   any direct or transitive coordinate-set change restarts the bounded fixed-point pass.
5. Cross-tenant, fabricated, stale, missing, value/revision-mismatched, unsigned, and
   signature/receipt-replayed artifacts all fail before provider invocation and append no
   candidate/history. A caller-computed digest plus a forged absent marker is insufficient.
   Reusing an old token under a new composite attempt or source fails; a same-attempt
   lost-response retry returns the identical artifact, while changed same-composite reuse
   conflicts. Two segments and two target languages reuse one raw `attempt_id` and are admitted
   as independently identified entries under their distinct grouping keys inside one atomic turn
   map. The same raw `turn_id` under two organizations creates independent admission slots and
   records with no collision or cross-tenant read. Within the same organization, the same
   `turn_id` with changed membership, claim variant, or variant-specific initiator facts conflicts
   for a currently authorized slot caller instead of creating a digest-keyed second record or
   returning a claim of the wrong type. A caller that lacks the current turn-admission capability
   probes one known occupied `turn_id` with mismatched facts and one unused empty `turn_id`; both
   return the same canonical byte-identical denial and expose no conflict, repository/config
   observation, receipt, provider call, or session. Granting slot authority makes the authorized
   mismatch return only the redacted conflict while the authorized empty slot continues to fresh
   observation and final admission authorization.
6. Inject crashes after the final observation, after attestation minting, immediately before
   atomic turn install, while staging each member, and after install/before response. Every
   pre-install crash leaves no claim, snapshot, member admission, or provider side effect and
   retry must freshly observe/authorize; the post-install retry returns the exact complete turn.
   Place a deterministic barrier after the final `observe-many` result and before admission
   reauthorization, rotate policy from allow to deny, and prove the admission installs nothing
   even when the earlier observation receipt said allow. Race rotation against the fenced
   admission and prove the only outcomes are a complete turn under then-current allow followed by
   revocation, or a denial with no record/session. There is no turn commit after a denial already
   linearized, and replaying the old receipt cannot select a third outcome.
   Commit a turn under allow, lose the response, rotate policy to deny, and make an equal retry.
   Installed-slot comparison precedes reauthorization, so the retry returns the exact committed
   record. This postcommit retry starts no second provider/session and does not use the old receipt
   as authority. An authorized mismatch returns the redacted conflict, while a denied changed-slot
   probe and a genuinely new empty-slot turn return the same canonical denial before observation
   and install nothing. Thus the authorized mismatch conflict coexists with the historical equal
   retry returning the exact committed record after revocation without creating an occupancy
   oracle.
   Race callers with equal stable initiator facts whose observations straddle a config change:
   authenticated initiator, claim variant, stable variant-specific initiator facts, and
   member/source/request facts are the retry projection; the
   server-derived candidate execution digest is excluded from retry equality, one whole
   admission wins, and all equal losers return the installed winner rather than their candidate.
   Changed request/member facts conflict, as does substituting the installed execution digest
   after admission. No durable partial/pending record or reusable old receipt is observable.
7. Existing consumers, backend compile, unit/integration tests, and MCP E2E pass with every
   publication-owned namespace absent from the config boundary closure.
8. The real authenticated GET route resolves global/override precedence without an attempt id,
   creates no attempt/attestation record, and returns the expected wire view. At the same fake
   repository snapshot its values equal the attempt artifact, but passing that view to provider
   invocation fails before any side effect. A valid organization-A session plus identity
   headers naming a real organization-B membership cannot read B; header-only, expired, and
   forged-session requests reach no repository operation and reveal no config existence/value.
9. Namespace/source guards keep the domain resolver free of law/shape/infra/store/extern
   imports, validators, codecs, Promise use, and repository calls; keep `law.*` free of I/O;
   and keep `shape.*` pure, domain-agnostic, and limited to structure morphisms. Domain table
   tests receive already validated typed observations, law tests decide admissibility, shape
   round trips prove structure only, and facade integration tests prove `observe-many`
   sequencing, fixed-point retry, receipt binding, and both typed operations without a second
   effectful orchestration path.
10. A fake repository alternates a selector or provider-policy reference on every pass. The
    facade terminates at the configured attempt/deadline bound with the typed unstable-observation
    error and creates no artifact, attestation, attempt admission, or provider call. Cancellation
    at each observation boundary has the same fail-closed proof; a closure that stabilizes on the
    last permitted pass succeeds normally.
11. Config selects model identity M as a string. Observe M1 as `:current`; M1 pins provider
    policy P1 while that policy's current revision advances through P2 and P3. The final
    mixed-coordinate observation and artifact bind M1 plus retained P1 exactly without inventing
    a model version, substituting policy current state, or exhausting retry. Race M1 with M2 that
    pins P2: the final result is either coherent M1/P1 or M2/P2, never M2/P1. An unavailable pinned
    version returns the canonical referenced-version error and creates no partial observation,
    artifact, attestation, admission, or provider call.
12. Start an expanded pass from M1 -> P1 -> Q1, then make Q1 read policy deny while current model
    changes to M2 -> P2 with no Q1 edge. The denied old frontier yields no partial evidence; one
    bounded minimal rebootstrap observes M2, derives the different P2 frontier, and may succeed.
    Controls where current authority still derives M1/P1/Q1, where the replacement closure still
    contains Q1, or where a minimal config/model coordinate denies all return the identical
    non-enumerating denial with no artifact, attestation, admission, or provider call. Repeating a
    denied frontier fingerprint is terminal, and alternating obsolete frontiers exhausts the
    existing bounded retry policy rather than looping or silently dropping coordinates.
13. Exercise attempt admission through both a publication dispatch claim and an ordinary-chat
    interactive claim. Each path atomically installs its turn claim, snapshot, and complete member
    map before the model turn. An ordinary-chat fixture claims two segments, saves them in both
    orders, and binds each call to its own embedded artifact; a lost-response retry returns the
    exact complete turn and member event. Inject failure after staging the first member: no claim,
    snapshot, member admission, provider call, or session is visible, and retry performs a fresh
    authorized whole-turn observation. An omitted member cannot save, and adding/replacing a
    member after admission conflicts. Reuse the same organization, `turn_id`, principal/capability,
    and member facts across a publication-dispatch claim and ordinary-chat interactive claim in
    both race orders: one variant may win, but the other conflicts without receiving that winner or
    starting a mismatched session. Changing only the dispatch-claim identity/workflow idempotency
    key or interactive translation-start claim identity has the same conflict. An incomplete
    interactive target exposes no `save_translation` tool and creates no turn admission; neither
    claim type can mint at save time.
14. Prepare one turn-wide execution snapshot and all member artifacts under provider/model config
    V1. Advance current config to V2 at barriers before authorization, during map construction, and
    immediately before atomic commit. Each run yields only a complete authorized V1 turn or aborts
    and freshly admits a complete V2 turn; it never persists V1/V2 siblings. A config-straddling
    concurrent loser with the same stable initiator facts, same claim variant, and same
    variant-specific initiator facts discards its unattached candidate and returns the installed
    winner; the candidate digest is not a retry conflict. Rotate authorization
    policy allow-to-allow and allow-to-deny across the same barriers: a retry cannot reuse the old
    receipt, current allow produces a fresh whole-turn receipt, and current deny produces no
    admission or session. Substituting one member artifact, replaying the snapshot under another
    turn/member set, or changing normalized session parameters fails the whole commit. A
    multi-digest member set returns the canonical `:translation/turn-partition-required` plan with
    no claim, snapshot, member admission, receipt, provider call, or session. Inject a lost plan
    response and prove retry has no parent/child state to recover; under the same observation it
    recomputes the same plan. The facade never mints child ids or installs one group. Only later
    explicit initiations, each with a distinct stable `turn_id` and complete claim facts, may admit
    the planned groups as independent turns. The one-way turn-id/member-set -> snapshot ->
    final-claim digest graph remains canonical.

## Non-goals

- Choosing provider/model policy or changing configuration precedence.
- Implementing the resource repository contract in this card.
- Adding a second resolver/precedence implementation, adapter authority, or shadow config
  store; the read-only view and admitted artifact must share one resolver.
- Owning translation attempt history, evaluation, publication, or representation.

## Done when

- One existing production facade preserves read-only inspection and admits an authenticated,
  versioned artifact through separate, non-interchangeable operations over one pure domain
  resolver; the facade alone owns observation I/O and fixed-point sequencing, `law.*` owns
  validation, and `shape.*` owns wire/domain morphisms.
- Global and optional override revisions—or trusted snapshot-bound absence—are mechanically
  attributable and immutable together with the selected model-catalog/provider-policy closure.
- Resolution consumes one #282 `observe-many` result; fake/file race proofs admit no torn
  combination from sequential reads, and repository schema-version rotation changes observation
  plus artifact identity.
- The selected model is observed as current before its returned record/version derives exact
  policy coordinates; final recheck yields one coherent model/policy closure without fabricated
  versions, current-policy substitution, or pinned-version absence witnesses.
- The artifact and attestation carry the exact final repository operation receipt and ordered
  per-coordinate authorization-policy versions, including multiple exact versions of one
  identity; policy rotation reauthorizes new attempts without rotating semantic
  resource/observation versions, and historical receipts never become capabilities.
- One-time server attempt admission supplies freshness: later attempts cannot replay old policy,
  while idempotent retries of the same composite attempt retain their exact artifact and
  cross-group reuse of a raw id remains independent.
- Publication-dispatch and ordinary-chat initiators share that admission law without making a
  publication dispatch claim mandatory for an admitted interactive translation.
- One turn-wide execution snapshot supplies the truthful provider/model/config provenance shared
  by every member event; current-config races cannot create a mixed-artifact model session.
- Turn admission authorizes and installs the claim, snapshot, complete member map, and operation
  receipt atomically; crash/race proofs expose no partial member, reused receipt capability,
  stranded reservation, or loser-selected artifact.
- Provider invocation and durable evidence carry the identical artifact end to end.
- Config races and cross-tenant/fabricated artifacts fail closed with the negative proofs above.
- Fixed-point churn and cancellation terminate within the server-owned retry policy and cannot
  produce partial invocation authority or an unbounded repository loop.
- The existing GET route has a route-level no-attempt/no-write proof and cannot mint invocation
  authority or derive scope from client identity headers.
- Publication-free closure, backend compile/tests, and MCP E2E pass.

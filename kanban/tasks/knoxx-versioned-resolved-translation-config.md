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
Each selector or policy-reference change consumes a pass and restarts from the canonical config
coordinate set. If the set never stabilizes before any limit, the facade returns the stable typed
error `{:error/type :translation-config/unstable-observation :error/reason
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

There is no free-floating reusable attestation. Before provider invocation, the server
derives the same canonical `AttemptIdentity` used by
`knoxx-translations-event-sourced`: the full
`{:org-id :document-id :segment-index :target-lang}` grouping key, where `:org-id` is the
server-derived effective organization, plus the caller-stable
`attempt_id`. The initiating dispatch/workflow accepts or creates and durably pins that id with
the exact grouping/source/request facts before it starts a provider or model session. An agent
session receives the pin through authenticated server context; a later `save_translation` call
only echoes and validates it and cannot become the identity minting boundary. The facade may
perform authenticated observations and mint a candidate artifact
before durable admission, but those values are not an attempt reservation and cannot authorize
provider invocation. It then atomically unique-inserts/compares one complete
`AttemptConfigAdmission` containing the composite identity, immutable source/canonical request
facts, and full attested artifact. There is no externally visible pre-artifact or `:pending`
reservation. Raw attempt ids are not globally unique: the same value under a different segment
or target-language grouping is a distinct attempt and receives its own current configuration.
That complete atomic install is the durable freshness boundary.

A crash after any observation or attestation-minting step but before the atomic install leaves
no attempt admission; a retry may resolve current configuration and the first complete install
wins. A crash after install but before response returns the installed artifact on retry.
Concurrent canonically equal callers may resolve different candidate snapshots, but exactly one
complete record is installed; every loser discards its candidate and returns the installed
winner. Changed source/request facts for that composite identity conflict. Orphan repository
operation receipts remain audit history only, and an unattached attestation fails provider
invocation because no matching installed admission exists.

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
   independently under their distinct grouping keys.
6. Inject crashes after the final observation, after attestation minting, immediately before
   atomic install, and after install/before response. Every pre-install crash leaves no attempt
   state or provider side effect and retry may resolve anew; the post-install retry returns the
   exact stored artifact. Race equal callers whose observations straddle a config change: one
   complete admission wins and all equal losers return it rather than their candidate; changed
   request facts conflict. No durable partial/pending record is observable.
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
- Attempt admission installs identity, canonical request/source facts, and the complete attested
  artifact atomically; crash/race proofs expose no stranded reservation, unattached invocation
  authority, or loser-selected artifact.
- Provider invocation and durable evidence carry the identical artifact end to end.
- Config races and cross-tenant/fabricated artifacts fail closed with the negative proofs above.
- Fixed-point churn and cancellation terminate within the server-owned retry policy and cannot
  produce partial invocation authority or an unbounded repository loop.
- The existing GET route has a route-level no-attempt/no-write proof and cannot mint invocation
  authority or derive scope from client identity headers.
- Publication-free closure, backend compile/tests, and MCP E2E pass.

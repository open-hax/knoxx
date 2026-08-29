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
  per-resource `authorization-policy-version` bindings for the exact observation;
- exact canonical identity and resource-scoped version of the global default;
- exact canonical identity and resource-scoped version of the optional organization override;
- exact canonical identity/resource version of the selected model-catalog entry plus the
  version-pinned closure/digest of any referenced provider/allowlisting policy;
- a resolution-policy/schema version; and
- a deterministic artifact identity/digest over the ordered contributing facts; and
- a trusted immutable resolution attestation covering the entire artifact, including absence.

Resource versions come from the provider-neutral repository contract and its retained
revisions, never file mtimes, manifest hashes, current re-reads, or caller input. Resolution
uses #282's `observe-many` operation for the global and organization-override identities,
rather than composing sequential single-resource reads. The returned observation has one
linearization point and one scoped identity over every present version and exact absence entry.
When the override is absent, the artifact carries that repository-authoritative absence for its
exact canonical identity, not a caller-computable marker.

The selected model identity is data-dependent, so the effectful facade reaches one final
fixed-point observation without pretending sequential reads are atomic. It provisionally
observes the two config identities, gives that immutable observation to the pure resolver to
derive the selected catalog identity, observes the expanded identity set, gives the new
observation back to the resolver to derive referenced provider-policy identities, and repeats
until the required canonical set is stable. The facade then obtains one final `observe-many`
result containing the unchanged config resources, selected model, and complete pinned policy
closure; the pure resolver validates and resolves only that result. Any changed selector or
reference restarts effectful sequencing; only the final single observation is attested. An
unrelated catalog entry is excluded and cannot rotate the artifact.

The attempt-admission operation mints an opaque `ResolvedConfigAttestation` using server-held
signing/MAC authority or an equivalent append-only receipt store outside caller-controlled
bytes. It binds the complete composite attempt identity, authenticated actor/origin
organization, effective organization plus delegation evidence, repository contract/schema
version, snapshot/observation identity, the exact final `RepositoryOperationReceipt` and every
ordered authorization-policy version, every present resource version, the absent-override
witness, selected model-catalog revision/provider-policy closure, resolution-policy/schema
version, and artifact digest. Admission rejects a receipt for another principal, effective
scope, capability, requested identity set, observation identity, or provisional fixed-point
read. Validation verifies the attestation and immutable receipt evidence without re-reading
current config. A later override creation therefore does not invalidate an
already-started attempt, while merely omitting an existing override or recomputing the digest
cannot fabricate a valid artifact. Unrelated resource writes cannot rotate the artifact.
Attempt consumers change atomically to use the admitted operation; inspection consumers retain
the read-only operation on the same configuration boundary.

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
`attempt_id`. Config admission atomically reserves that composite identity with the immutable
source revision and canonical request facts, resolves current configuration, and consumes the
new attestation into that slot. Raw attempt ids are not globally unique: the same value under a
different segment or target-language grouping is a distinct attempt and receives its own
current configuration. That first composite admission is the freshness boundary.

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
   updating the selected model/provider policy also changes it; updating an unrelated resource
   or unselected catalog entry does not. Rotate only authorization policy V1 allow to V2 allow:
   semantic resource/observation identity remains stable while the exact final operation
   receipt/policy bindings and new-attempt artifact rotate. V2 deny admits no new artifact;
   replaying the V1 receipt or substituting another actor/scope/request-set receipt fails.
4. Resolve with no override, create one, then persist a candidate: its receipt retains and
   verifies the originally attested absence without re-reading current config. Updating a
   present contributor exercises the same race law. Deterministic fake/file barriers prove the
   observation is a complete before-state or after-state, never a torn pair from sequential
   reads. Race a config selector change, selected catalog update, and provider-policy reference
   update during fixed-point expansion; the admitted artifact names one final coexistent closure
   or retries, never a mixed catalog decision.
5. Cross-tenant, fabricated, stale, missing, value/revision-mismatched, unsigned, and
   signature/receipt-replayed artifacts all fail before provider invocation and append no
   candidate/history. A caller-computed digest plus a forged absent marker is insufficient.
   Reusing an old token under a new composite attempt or source fails; a same-attempt
   lost-response retry returns the identical artifact, while changed same-composite reuse
   conflicts. Two segments and two target languages reuse one raw `attempt_id` and are admitted
   independently under their distinct grouping keys.
6. Existing consumers, backend compile, unit/integration tests, and MCP E2E pass with every
   publication-owned namespace absent from the config boundary closure.
7. The real authenticated GET route resolves global/override precedence without an attempt id,
   creates no attempt/attestation record, and returns the expected wire view. At the same fake
   repository snapshot its values equal the attempt artifact, but passing that view to provider
   invocation fails before any side effect. A valid organization-A session plus identity
   headers naming a real organization-B membership cannot read B; header-only, expired, and
   forged-session requests reach no repository operation and reveal no config existence/value.
8. Namespace/source guards keep the domain resolver free of law/shape/infra/store/extern
   imports, validators, codecs, Promise use, and repository calls; keep `law.*` free of I/O;
   and keep `shape.*` pure, domain-agnostic, and limited to structure morphisms. Domain table
   tests receive already validated typed observations, law tests decide admissibility, shape
   round trips prove structure only, and facade integration tests prove `observe-many`
   sequencing, fixed-point retry, receipt binding, and both typed operations without a second
   effectful orchestration path.

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
- The artifact and attestation carry the exact final repository operation receipt and ordered
  authorization-policy versions; policy rotation reauthorizes new attempts without rotating
  semantic resource/observation versions, and historical receipts never become capabilities.
- One-time server attempt admission supplies freshness: later attempts cannot replay old policy,
  while idempotent retries of the same composite attempt retain their exact artifact and
  cross-group reuse of a raw id remains independent.
- Provider invocation and durable evidence carry the identical artifact end to end.
- Config races and cross-tenant/fabricated artifacts fail closed with the negative proofs above.
- The existing GET route has a route-level no-attempt/no-write proof and cannot mint invocation
  authority or derive scope from client identity headers.
- Publication-free closure, backend compile/tests, and MCP E2E pass.

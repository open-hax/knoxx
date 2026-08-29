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

One pure effective-config resolver owns repository reads, global/organization precedence,
catalog validation, and wire/domain codecs. The existing translation-config facade exposes two
explicit operations over it:

1. **Inspect effective config** — the existing authenticated
   `GET /api/translations/config`/`config-response!` use case accepts organization context only
   and returns a typed `EffectiveConfigView`. It performs no attempt reservation or write and
   carries no provider-invocation authority.
2. **Admit config for attempt** — `admit-resolved-config-for-attempt!` requires the
   server-admitted attempt/source/operation context and returns the attested
   `ResolvedConfigArtifact` below.

The two output types are not interchangeable: provider invocation rejects an
`EffectiveConfigView`, even when its values equal the current artifact. Both operations call
the same resolver and, at one repository snapshot, return identical effective values. This is
one authority with two use cases, not a second adapter, precedence implementation, or config
store.

### Attempt-admitted artifact

The resolved artifact contains:

- effective typed configuration values;
- the authenticated organization used for resolution;
- the server-admitted attempt id, immutable source identity/revision, and canonical operation;
- exact canonical identity and resource-scoped version of the global default;
- exact canonical identity and resource-scoped version of the optional organization override;
- a resolution-policy/schema version; and
- a deterministic artifact identity/digest over the ordered contributing facts; and
- a trusted immutable resolution attestation covering the entire artifact, including absence.

Resource versions come from the provider-neutral repository contract and its retained
revisions, never file mtimes, manifest hashes, current re-reads, or caller input. Resolution
observes the global and organization-override identities in one consistent repository snapshot.
When the override is absent, the artifact carries a trusted absence witness for its exact
canonical identity and that snapshot, not a caller-computable marker.

The attempt-admission operation mints an opaque `ResolvedConfigAttestation` using server-held
signing/MAC authority or an equivalent append-only receipt store outside caller-controlled
bytes. It binds
the authenticated organization, snapshot/observation identity, every present resource version,
the absent-override witness, policy/schema version, and artifact digest. Validation verifies
that authority without re-reading current config. A later override creation therefore does not
invalidate an already-started attempt, while merely omitting an existing override or recomputing
the digest cannot fabricate a valid artifact. Unrelated resource writes cannot rotate the
artifact. Attempt consumers change atomically to use the admitted operation; inspection
consumers retain the read-only operation on the same configuration boundary.

There is no free-floating reusable attestation. Before provider invocation, the server
atomically admits/reserves the caller-stable `attempt_id` for the authenticated organization,
immutable source revision, and canonical operation/request, resolves current configuration,
and consumes the new attestation into that attempt slot. That first admission is the freshness
boundary. A new attempt id always resolves current configuration and cannot present an older
attempt's token. A canonically equal retry of the same attempt retrieves the same attested
artifact—even after configuration changes or a lost response—while changed attempt-id reuse,
source/request mismatch, or cross-attempt replay conflicts before provider invocation. Historic
verification remains valid without a wall-clock expiry silently invalidating durable evidence;
freshness is enforced by one-time attempt binding, not by trusting client time.

Provider selection receives this artifact and durable attempt/candidate evidence carries it
unchanged. A configuration update after attempt admission creates a different artifact for a
later attempt but cannot rewrite the provenance of the admitted attempt. The artifact is valid
only for its authenticated organization, source, operation, and attempt; client-fabricated,
cross-tenant, cross-attempt, stale, missing, or value/revision-mismatched artifacts fail before
provider invocation without an existence leak.

The publication-free namespace closure from #273 remains an invariant.

## TDD / proof

1. Global-only resolution returns the exact global revision, snapshot-bound absence witness for
   the override identity, byte-stable artifact identity, and trusted attestation.
2. Global plus organization override resolution names both ordered revisions and binds to the
   authenticated organization.
3. Updating either contributor changes the artifact; updating an unrelated resource does not.
4. Resolve with no override, create one, then persist a candidate: its receipt retains and
   verifies the originally attested absence without re-reading current config. Updating a
   present contributor exercises the same race law.
5. Cross-tenant, fabricated, stale, missing, value/revision-mismatched, unsigned, and
   signature/receipt-replayed artifacts all fail before provider invocation and append no
   candidate/history. A caller-computed digest plus a forged absent marker is insufficient.
   Reusing an old token under a new attempt or source fails; a same-attempt lost-response retry
   returns the identical artifact, while changed reuse conflicts.
6. Existing consumers, backend compile, unit/integration tests, and MCP E2E pass with every
   publication-owned namespace absent from the config boundary closure.
7. The real authenticated GET route resolves global/override precedence without an attempt id,
   creates no attempt/attestation record, and returns the expected wire view. At the same fake
   repository snapshot its values equal the attempt artifact, but passing that view to provider
   invocation fails before any side effect.

## Non-goals

- Choosing provider/model policy or changing configuration precedence.
- Implementing the resource repository contract in this card.
- Adding a second resolver/precedence implementation, adapter authority, or shadow config
  store; the read-only view and admitted artifact must share one resolver.
- Owning translation attempt history, evaluation, publication, or representation.

## Done when

- One existing production facade preserves read-only inspection and admits an authenticated,
  versioned artifact through separate, non-interchangeable operations over one resolver.
- Global and optional override revisions—or trusted snapshot-bound absence—are mechanically
  attributable and immutable.
- One-time server attempt admission supplies freshness: later attempts cannot replay old policy,
  while idempotent retries of the same source/operation/attempt retain their exact artifact.
- Provider invocation and durable evidence carry the identical artifact end to end.
- Config races and cross-tenant/fabricated artifacts fail closed with the negative proofs above.
- The existing GET route has a route-level no-attempt/no-write proof and cannot mint invocation
  authority.
- Publication-free closure, backend compile/tests, and MCP E2E pass.

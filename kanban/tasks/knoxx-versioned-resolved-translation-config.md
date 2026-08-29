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

## Purpose

The production `knoxx.backend.infra.routes.translation-config/resolved-config!` boundary
currently returns only three effective values. Once the global default and optional
organization override have been merged, their resource revisions disappear. A candidate can
therefore name the values it used but cannot prove which immutable policy revisions selected
its provider.

Evolve the existing boundary to emit one authenticated, immutable artifact. Do not add a
parallel configuration authority or reconstruct provenance after the provider call.

## Contract

The resolved artifact contains:

- effective typed configuration values;
- the authenticated organization used for resolution;
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

The existing boundary mints an opaque `ResolvedConfigAttestation` using server-held signing/MAC
authority or an equivalent append-only receipt store outside caller-controlled bytes. It binds
the authenticated organization, snapshot/observation identity, every present resource version,
the absent-override witness, policy/schema version, and artifact digest. Validation verifies
that authority without re-reading current config. A later override creation therefore does not
invalidate an already-started attempt, while merely omitting an existing override or recomputing
the digest cannot fabricate a valid artifact. Unrelated resource writes cannot rotate the
artifact. The existing `resolved-config!` contract and its consumers change atomically so there
is still one configuration boundary.

Provider selection receives this artifact and durable attempt/candidate evidence carries it
unchanged. A configuration update after resolution creates a different future artifact but
cannot rewrite the provenance of an already-started attempt. The artifact is valid only for
its authenticated organization; client-fabricated, cross-tenant, stale, missing, or
value/revision-mismatched artifacts fail before provider invocation without an existence leak.

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
6. Existing consumers, backend compile, unit/integration tests, and MCP E2E pass with every
   publication-owned namespace absent from the config boundary closure.

## Non-goals

- Choosing provider/model policy or changing configuration precedence.
- Implementing the resource repository contract in this card.
- Adding a second resolved-config adapter or shadow configuration store.
- Owning translation attempt history, evaluation, publication, or representation.

## Done when

- One existing production boundary returns an authenticated, versioned resolved artifact.
- Global and optional override revisions—or trusted snapshot-bound absence—are mechanically
  attributable and immutable.
- Provider invocation and durable evidence carry the identical artifact end to end.
- Config races and cross-tenant/fabricated artifacts fail closed with the negative proofs above.
- Publication-free closure, backend compile/tests, and MCP E2E pass.

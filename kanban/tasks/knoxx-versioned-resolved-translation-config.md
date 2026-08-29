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
- a deterministic artifact identity/digest over the ordered contributing facts.

Resource versions come from the provider-neutral repository contract and its retained
revisions, never file mtimes, manifest hashes, current re-reads, or caller input. Global-only
resolution has an explicit absent-override value, not a fabricated revision. Unrelated
resource writes cannot rotate the artifact. The existing `resolved-config!` contract and its
consumers change atomically so there is still one configuration boundary.

Provider selection receives this artifact and durable attempt/candidate evidence carries it
unchanged. A configuration update after resolution creates a different future artifact but
cannot rewrite the provenance of an already-started attempt. The artifact is valid only for
its authenticated organization; client-fabricated, cross-tenant, stale, missing, or
value/revision-mismatched artifacts fail before provider invocation without an existence leak.

The publication-free namespace closure from #273 remains an invariant.

## TDD / proof

1. Global-only resolution returns the exact global revision, explicit no-override marker, and
   a byte-stable artifact identity.
2. Global plus organization override resolution names both ordered revisions and binds to the
   authenticated organization.
3. Updating either contributor changes the artifact; updating an unrelated resource does not.
4. Resolve, change config, then persist a candidate: its receipt retains the originally
   resolved artifact and never re-reads current config for provenance.
5. Cross-tenant, fabricated, stale, missing, and value/revision-mismatched artifacts all fail
   before provider invocation and append no candidate/history.
6. Existing consumers, backend compile, unit/integration tests, and MCP E2E pass with every
   publication-owned namespace absent from the config boundary closure.

## Non-goals

- Choosing provider/model policy or changing configuration precedence.
- Implementing the resource repository contract in this card.
- Adding a second resolved-config adapter or shadow configuration store.
- Owning translation attempt history, evaluation, publication, or representation.

## Done when

- One existing production boundary returns an authenticated, versioned resolved artifact.
- Global and optional override revisions are mechanically attributable and immutable.
- Provider invocation and durable evidence carry the identical artifact end to end.
- Config races and cross-tenant/fabricated artifacts fail closed with the negative proofs above.
- Publication-free closure, backend compile/tests, and MCP E2E pass.

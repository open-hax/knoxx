---
uuid: "knoxx-resource-repository-cms"
title: "Resource repository CMS — file-first contracts with replaceable providers"
status: incoming
priority: P2
labels: ["epics", "cms", "resources", "repository", "contracts", "providers"]
created_at: "2026-08-13T00:00:00Z"
points: 0
category: epics
---
# Resource repository CMS — file-first contracts with replaceable providers

## Signal

The useful open-source CMS core is not a visual editor and not a specific backend.
It is a **resource repository contract**: identity, schema, references, versioning,
read/write/list operations, atomic multi-resource observations, and provider boundaries over
content/resource data.

Knoxx should have a file/EDN provider because it is transparent, Git-native,
diffable, scriptable, AI-readable, and fast to develop against. A client using Optimizely
should be able to substitute an Optimizely provider without changing the consumers of
the repository contract.

## Ownership rule

```text
resource contracts
      <-> repository boundary
             |-- EDN/files/Git provider
             |-- Optimizely provider
             |-- other provider
```

The repository owns persistence and retrieval of declared resources. It does **not** own:

- publication reconciliation/effects;
- translation/transduction;
- SME evaluation;
- HTML/React/static representation;
- a particular visual editing UI.

Resource versions identify one immutable canonical payload/provenance revision. References
are version-pinned and resolve to an exposed immutable transitive closure, so an unrelated
sibling write or a referenced target's later update cannot silently change evidence already
bound to an older resource revision.

Consumers that need more than one resource use one provider-neutral snapshot observation.
That operation returns one linearized set of present versions and exact absence entries;
sequential consumer reads are not a substitute.

## First provider

The open-source reference provider should use the existing namespace/resource EDN shape
where possible rather than inventing a second CMS document language.

Content-specific facets may layer on the generic resource repository, but `content` is
not assumed to be the only resource family.

## Children / board moves

- `knoxx-cms-contract-validation` — reframe around repository/provider laws and production verification instead of validating a legacy OpenPlanner REST dependency.
- `knoxx-file-resource-repository-provider` — define and prove the EDN/file-backed read/write/list/version boundary.
- `knoxx-resource-repository-snapshot-observation` — add one linearizable multi-resource
  observation and authoritative absence contract across fake/file providers.
- Future: provider compatibility tests that a second implementation can satisfy without consumers branching on provider identity.
- Future: narrow editing interfaces (CLI/MCP/UI) as consumers of the same resource-write operations.

## Relationship to active publication epic

`knoxx-contract-owned-publication-pipeline` is already moving publication intent into
resources. Let that work land. This epic separates **where resource intent is stored and
edited** from publication semantics and effects.

The active/future card `knoxx-cms-resource-backed-publication-ui` should be treated as an
integration adapter: a UI editing publication resources. It is not the definition of the
CMS/repository capability itself.

## Relationship to the old visual CMS epic

`knoxx-folder-backed-visual-cms-design-spec` remains iceboxed. Its valuable file-backed
idea moves here; its visual-page-builder scope does not become a prerequisite.

## Non-goals

- Competing with Optimizely as a product suite.
- Requiring a frontend before resource operations are complete.
- Baking publication state, translation jobs, or review workflow into repository laws.
- Making filesystem details visible to every repository consumer.

## Done when

- Resource consumers can read/write/list canonical resources through a provider-neutral boundary.
- The EDN/file implementation proves the open-source workflow end to end.
- Provider identity is not part of semantic resource contracts.
- Old resource revisions and their complete version-pinned reference closures remain
  addressable after current resources advance.
- Multi-resource consumers can observe one state that existed, including exact requested
  absences, without provider-specific reads or unrelated-write identity churn.
- Publication, transduction, evaluation, and representation can each consume repository data without acquiring repository-provider knowledge.

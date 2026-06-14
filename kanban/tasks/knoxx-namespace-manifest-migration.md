---
uuid: "knoxx-namespace-manifest-migration"
title: "Migrate Contract Files to Namespace Manifests"
status: pending
priority: "P2"
labels: ["tasks", "5sp", "contract-runtime-deployment"]
created_at: "2026-06-10T00:00:00Z"
source: "docs/design/resource-architecture.md"
points: 5
category: "tasks"
---
# Migrate Contract Files to Namespace Manifests

> Parent epic: `knoxx-contract-runtime-deployment`

## Context

The manifest grammar covers all 17 resource kinds. Exemplars exist
(`contracts/namespaces/ussyverse.edn`, `contracts/namespaces/hello_world.edn`).
Remaining individual files stay valid but should migrate domain by domain.

## Work

Per domain: create `contracts/namespaces/<domain>.edn`, move entries in,
rewrite references to owner-namespaced fields, drop `:data` in favor of
kind-namespaced fields, delete the individual files, verify dedup logs clean.

Suggested order (loosest coupling first):

1. fork-tales (agents + schedule + trigger)
2. broadcast-studio (agents + sub-agents)
3. muses, page_defaults
4. devel sources, knoxx-session sources
5. generators + schedules (knoxx_schedule)
6. models + model_families (rewrite `:model-family/id` refs as `:model/family`)
7. roles + capabilities + actors — LAST; blocked by
   `knoxx-qualified-id-resolution`

## Definition of Done

- [ ] Each migrated domain loads from its manifest with qualified ids
- [ ] No `:data` fields in migrated entries
- [ ] No dedup collisions logged at startup
- [ ] Tests pass

## Risks

- Identity strings change; audit/session naming derived from resource ids
- Slug-based role/cap lookup blocks the final phase

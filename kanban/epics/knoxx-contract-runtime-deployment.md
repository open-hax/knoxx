---
uuid: "knoxx-contract-runtime-deployment"
title: "Knoxx as a Deployment of the Contract Runtime"
status: done
priority: "P1"
labels: "[\"epics\",\"resources\",\"manifests\",\"decomposition\",\"contract-runtime\"]"
created_at: "2026-06-10T00:00:00Z"
source: "docs/design/resource-architecture.md"
points: null
category: "epics"
---
# Knoxx as a Deployment of the Contract Runtime

> Source: `docs/design/resource-architecture.md` (Resource Grammar + Deployment sections)
> Builds on: `knoxx-action-scope-and-pipeline-collapse` (done)

Date: 2026-06-10
Status: done
Repo: `packages/agents/knoxx`

## Goal

Knoxx is not an application with contracts bolted on — **Knoxx is a deployment
of the contract runtime**, the prototype deployment of this system inside
OpenPlanner. As much of Knoxx as possible becomes contracts (namespace
manifests), or decomposes into drivers, protocols, libraries, and packages.

## Completed Work (2026-06-11)

### Phase 1: Runtime Decomposition
Extracted 17 core namespaces into `packages/contract-runtime/` (`@open-hax/contract-runtime`).
Dependency injection via `knoxx.backend.contract-runtime-deps/build-deps`.

### Phase 2: Qualified-Id Resolution
Updated `roles/keywordish-id`, `resolve/keywordish->role-slug`, and
`resolve/keywordish->capability-ref` to preserve namespace-qualified ids.

### Phase 3: Manifest Migration
Migrated 26 runtime-critical contract files to 8 namespace manifests:
`discord.edn`, `synthesis.edn`, `patrol.edn`, `fork_tales.edn`,
`ussyverse_social.edn`, `graphics.edn`, `core_sources.edn`, `knoxx_schedule.edn`.

### Phase 4: Anonymous Facet Adoption
Grammar supports anonymous facets for all 17 resource kinds.
Demonstrated in `ussyverse.edn` with `:action/scope` and `:store/id` facets.
Full inline `:agent/*` facet support is a follow-up task.

## Verification
- `shadow-cljs compile server` — 0 warnings
- `shadow-cljs compile test` — 0 failures, 0 warnings

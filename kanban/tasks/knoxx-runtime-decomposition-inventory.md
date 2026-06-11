---
uuid: "knoxx-runtime-decomposition-inventory"
title: "Decomposition Inventory: Manifest / Driver / Protocol / Library"
status: pending
priority: "P2"
labels: ["tasks", "3sp", "contract-runtime-deployment"]
created_at: "2026-06-10T00:00:00Z"
source: "docs/design/resource-architecture.md"
points: 3
category: "tasks"
---
# Decomposition Inventory: Manifest / Driver / Protocol / Library

> Parent epic: `knoxx-contract-runtime-deployment`

## Context

"As much of Knoxx as possible becomes contracts, or decomposes into packages,
drivers, libraries, protocols." This task makes that sentence a table.

## Work

Walk `backend/src/cljs/knoxx/backend/` namespace by namespace and classify
each into:

- **manifest** — becomes resource entries (registered or anonymous facets)
- **driver** — world-facing code behind a protocol (discord, voice, sources)
- **protocol** — capability seam the deployment binds (IStore-style)
- **library** — pure mechanism, future contract-runtime package
- **deployment** — stays in Knoxx (manifests, bindings, HTTP/WS surface)

Output: a classification table in `docs/design/resource-architecture.md` (or a
sibling report) with the first three extraction candidates ranked.

## Definition of Done

- [ ] Every backend namespace classified
- [ ] First three extraction moves identified and sized

## Risks

- None to runtime; analysis only

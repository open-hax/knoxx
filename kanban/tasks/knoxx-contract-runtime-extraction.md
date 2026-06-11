---
uuid: "knoxx-contract-runtime-extraction"
title: "Extract the Contract Runtime Core as a Package"
status: pending
priority: "P3"
labels: ["tasks", "8sp", "contract-runtime-deployment"]
created_at: "2026-06-10T00:00:00Z"
source: "docs/design/resource-architecture.md"
points: 8
category: "tasks"
---
# Extract the Contract Runtime Core as a Package

> Parent epic: `knoxx-contract-runtime-deployment`

## Context

Knoxx is the prototype deployment of the contract runtime inside OpenPlanner.
The runtime core has no Knoxx-specific opinion and should be a reusable
package.

## Work

1. Inventory the deployment-agnostic core: resource loader + namespace-file
   grammar, schema/law validation, action registry + interpreter +
   anonymous safe-eval, condition/filter registries, store protocol +
   backends, event normalize/dispatch skeleton.
2. Decide the package home (e.g. `packages/libs/contract-runtime`) and the
   namespace prefix (`open-hax.contract-runtime.*`).
3. Move with thin Knoxx-side aliases first; cut over imports incrementally.
4. Knoxx keeps: namespace manifests, driver bindings (discord, voice,
   eta-mu sessions), HTTP/WS surface, deployment config.

## Definition of Done

- [ ] Core namespaces live in the package with no knoxx.* dependencies
- [ ] Knoxx consumes the package; behavior unchanged; tests pass

## Risks

- Hidden knoxx.* coupling in loader/dispatch (config shape, error-observatory)
- pnpm workspace + shadow-cljs source-path wiring

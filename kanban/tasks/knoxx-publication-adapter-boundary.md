---
uuid: "knoxx-publication-adapter-boundary"
title: "Define publication reconciliation and adapter boundary"
status: breakdown
priority: P1
labels: ["tasks", "has-parent", "publication", "adapters", "decouple"]
created_at: "2026-08-12T00:00:00Z"
points: 0
category: tasks
---
# Define publication reconciliation and adapter boundary

> Parent epic: `knoxx-contract-owned-publication-pipeline`

## Purpose

Coordinate the replaceable publication effect boundary without mixing pure reconciliation law, external side effects, and observation/proof in one 8-point implementation card.

The original 8sp scope is preserved exactly as three executable children totaling 8 points.

## Child breakdown

1. **P1 / accepted / 3sp** `knoxx-publication-reconcile-plan-laws` — pure reconciliation decisions: non-public removal, garden admissibility, concrete revision resolution, blockers, and path-aware drift.
2. **P1 / accepted / 3sp** `knoxx-publication-adapter-effects-idempotency` — `IPublicationTarget`, publish/remove/observe effects, path replacement, and stable retry/idempotency semantics.
3. **P1 / accepted / 2sp** `knoxx-publication-receipts-fake-adapter-proof` — receipt/projection shape plus fake-adapter tests proving convergence with no OpenPlanner dependency.

```text
reconciliation law (3)
        ↓
adapter effects + idempotency (3)
        ↓
receipts + fake-adapter proof (2)
```

## Boundary invariant

```clojure
(defn reconcile! [resource-index intent facts adapter ctx artifact]
  (let [plan (publication-plan/reconcile-plan resource-index intent facts)]
    ;; The law decides. The adapter only performs the requested effect.
    (publication-effects/execute-plan! adapter ctx plan artifact)))
```

Desired state remains in resources, decision law remains pure, adapters perform effects, and receipts describe what actually happened.

## Coordination rules

- `:withheld`, `:archived`, or an archived garden must never reach publish effects.
- Translation/review blockers govern publication, never removal.
- `:source/current` resolves to a concrete revision before effect planning.
- Publication path participates in drift and publish operation identity.
- Retry safety belongs to the adapter-effect contract, not the pure planner.
- Runtime receipts are evidence and may not rewrite desired resource state.
- OpenPlanner-specific identifiers stay below the adapter boundary.

## Done when

- All three child cards are complete.
- Their points sum to the original 8sp without double-counting this coordination card.
- The E2E publication path can consume the resulting planner, adapter, and receipt contracts with OpenPlanner REST absent.

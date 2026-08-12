---
uuid: "knoxx-publication-adapter-effects-idempotency"
title: "Implement publication adapter effects and idempotency contract"
status: accepted
priority: P1
labels: ["tasks", "3sp", "has-parent", "publication", "adapters", "idempotency"]
created_at: "2026-08-12T00:00:00Z"
points: 3
category: tasks
---
# Implement publication adapter effects and idempotency contract

> Parent task: `knoxx-publication-adapter-boundary`
> Parent epic: `knoxx-contract-owned-publication-pipeline`

## Purpose

Define the effect boundary that executes an already-decided publication plan without owning publication semantics.

## Scope

- Define `IPublicationTarget` with publish, remove, and observe operations.
- Keep adapter-specific ids below the boundary.
- Compute one stable publish idempotency key from effect-relevant materialization identity.
- Include publication id, adapter id, garden, locale, path, and concrete revision in that key.
- Treat publication identity as one logical materialization: a path move replaces/removes the prior route instead of leaving both public.
- Define replay behavior after ambiguous external responses: the same key must return the existing converged result or equivalent receipt rather than creating duplicates.
- Consume `:previous` observation from the pure plan when replacing stale materialization.
- Keep OpenPlanner, filesystem, Git, or object storage as interchangeable implementations of the same protocol.

## CLJS pseudocode

```clojure
(ns knoxx.backend.infra.publication-effects)

(defprotocol IPublicationTarget
  (publish! [target ctx intent artifact previous idempotency-key])
  (remove! [target ctx intent observed])
  (observe! [target ctx intent]))

(defn publish-idempotency-key [adapter-id intent concrete-revision]
  (assert (some? concrete-revision))
  (stable/hash
   [(:publication/id intent)
    adapter-id
    (:publication/garden intent)
    (:publication/locale intent)
    (:publication/path intent)
    concrete-revision]))

(defn execute-plan! [adapter ctx plan artifact]
  (case (:op plan)
    :publish
    (let [key (publish-idempotency-key
               (adapter/id adapter)
               (:intent plan)
               (:concrete-revision plan))]
      (publish! adapter
                ctx
                (:intent plan)
                artifact
                (:previous plan)
                key))

    :remove
    (remove! adapter ctx (:intent plan) (:observed plan))

    :noop
    {:receipt/type :publication/noop
     :reason (:reason plan)}

    :blocked
    {:receipt/type :publication/blocked
     :blockers (:blockers plan)}))
```

Adapter replay law sketch:

```clojure
(defn publish-once! [store op]
  (if-let [receipt (store/by-idempotency-key (:idempotency/key op))]
    receipt
    (store/materialize-and-record! op)))
```

## Laws

- The adapter may execute a plan but may not reinterpret desired publication state.
- Replaying an identical publish key cannot create another public artifact.
- A path change creates a distinct operation and makes the previous route unavailable after convergence.
- Remove operations remain possible regardless of translation/review blockers because the planner already resolved those semantics.
- Adapter failures produce failure/drift evidence and do not mutate desired resource state.

## Done when

- A fake effect implementation proves repeated identical publish calls converge to one materialization.
- A path change replaces the old route.
- Remove works for prior materializations.
- No OpenPlanner-specific identifier appears in the domain plan or resource contract.
- Receipt-producing results are ready for the observation/proof child card.

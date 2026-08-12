---
uuid: "knoxx-publication-adapter-boundary"
title: "Define publication reconciliation and adapter boundary"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent", "publication", "adapters", "decouple"]
created_at: "2026-08-12T00:00:00Z"
points: 5
category: tasks
---
# Define publication reconciliation and adapter boundary

> Parent epic: `knoxx-contract-owned-publication-pipeline`

## Purpose

Make publication effects replaceable. The domain computes the desired materialization and a reconciliation plan; an adapter performs the effect and records a receipt. OpenPlanner, if retained, becomes one implementation of this boundary rather than the owner of publication semantics.

## Scope

- Define a narrow publication target protocol/interface at the effect boundary.
- Keep planning pure: compare desired publication intent with observed receipts/projections and emit actions.
- Require adapters to return receipts describing what they actually materialized, including target, document, locale, revision, and adapter identity.
- Make retry/idempotency semantics explicit before the first adapter implementation.
- Provide an OpenPlanner adapter only as transitional compatibility if still needed; do not expose OpenPlanner ids above the adapter.
- Permit future static-filesystem/Git/object-store publication adapters without changing document/publication laws.

## CLJS pseudocode

```clojure
(ns knoxx.backend.domain.publication-reconcile)

(defn reconcile-plan [intent facts]
  (let [blockers (gate/publication-blockers intent facts)
        observed (facts/materialized-publication facts intent)]
    (cond
      (seq blockers)
      {:op :blocked :blockers blockers}

      (= :archived (:publication/state intent))
      {:op :remove :intent intent}

      (not= (:publication/revision intent)
            (:materialized/revision observed))
      {:op :publish :intent intent}

      :else
      {:op :noop :intent intent})))
```

Effect boundary:

```clojure
(defprotocol IPublicationTarget
  (publish! [target ctx intent artifact])
  (remove! [target ctx intent])
  (observe! [target ctx intent]))

(defn execute-plan! [adapter ctx plan artifact]
  (case (:op plan)
    :publish (publish! adapter ctx (:intent plan) artifact)
    :remove  (remove! adapter ctx (:intent plan))
    :noop    {:receipt/type :publication/noop}
    :blocked {:receipt/type :publication/blocked
              :blockers (:blockers plan)}))
```

Receipt shape sketch:

```clojure
{:receipt/type :publication/materialized
 :publication/id :knoxx.docs/translation-pipeline-es
 :adapter/id :publication/static-garden
 :document/id :knoxx.docs/translation-pipeline
 :locale :es
 :revision "abc123"
 :target :gardens/promethean}
```

## Contract obligations

- Repeating the same `:publish` plan with the same revision must converge without duplicate public artifacts.
- Adapter-specific identifiers never become part of publication resource identity.
- Adapter failure records failure/drift; it does not mutate desired state back to the old observed state.
- `observe!` may report reality but cannot redefine what should be published.

## Done when

- A fake adapter can drive the complete reconciler tests with no OpenPlanner dependency.
- OpenPlanner can be unplugged/replaced without changing domain publication code or resource contracts.
- Every successful materialization produces a receipt suitable for drift calculation and deploy verification.

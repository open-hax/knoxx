---
uuid: "knoxx-publication-adapter-boundary"
title: "Define publication reconciliation and adapter boundary"
status: accepted
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
- Handle non-publication states (`:withheld`, `:archived`) before translation/review blockers so removal can never be prevented by missing/stale translation evidence.
- Resolve revision selectors such as `:source/current` to a concrete revision before comparing desired and observed materialization.
- Require adapters to return receipts describing what they actually materialized, including target, document, locale, concrete revision, adapter identity, and idempotency key.
- Make retry/idempotency semantics explicit before the first adapter implementation.
- Provide an OpenPlanner adapter only as transitional compatibility if still needed; do not expose OpenPlanner ids above the adapter.
- Permit future static-filesystem/Git/object-store publication adapters without changing document/publication laws.

## CLJS pseudocode

```clojure
(ns knoxx.backend.domain.publication-reconcile)

(defn concrete-revision [intent facts]
  (case (:publication/revision intent)
    :source/current (facts/current-source-revision
                     facts
                     (:publication/document intent))
    (:publication/revision intent)))

(defn non-public-plan [intent observed]
  (if observed
    {:op :remove :intent intent}
    {:op :noop :intent intent}))

(defn reconcile-plan [intent facts]
  (let [state    (:publication/state intent)
        observed (facts/materialized-publication facts intent)]
    (if (contains? #{:withheld :archived} state)
      (non-public-plan intent observed)
      (let [revision (concrete-revision intent facts)
            blockers (gate/publication-blockers intent facts)]
        (cond
          (seq blockers)
          {:op :blocked :blockers blockers :intent intent}

          (not= revision (:materialized/revision observed))
          {:op :publish
           :intent intent
           :concrete-revision revision}

          :else
          {:op :noop
           :intent intent
           :concrete-revision revision})))))
```

Stable publish operation identity:

```clojure
(defn publish-idempotency-key [adapter-id intent concrete-revision]
  (stable/hash
   [(:publication/id intent)
    adapter-id
    (:publication/garden intent)
    (:publication/locale intent)
    concrete-revision]))
```

Effect boundary:

```clojure
(defprotocol IPublicationTarget
  (publish! [target ctx intent artifact idempotency-key])
  (remove! [target ctx intent])
  (observe! [target ctx intent]))

(defn execute-plan! [adapter ctx plan artifact]
  (case (:op plan)
    :publish
    (let [key (publish-idempotency-key
               (adapter/id adapter)
               (:intent plan)
               (:concrete-revision plan))]
      ;; Adapter must return the prior successful receipt when this key has
      ;; already materialized the same operation; retries do not duplicate.
      (publish! adapter ctx (:intent plan) artifact key))

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
 :idempotency/key "...stable..."
 :document/id :knoxx.docs/translation-pipeline
 :locale :es
 :revision "abc123"
 :target :gardens/promethean}
```

## Contract obligations

- `:withheld` and `:archived` can only remove or no-op; they can never enter the publish branch.
- Removal is not gated by translation/review blockers. An archive request can always remove a stale public artifact.
- Revision selectors are normalized before desired/observed comparison; unchanged `:source/current` publications do not republish every tick.
- Repeating the same `:publish` operation with the same idempotency key returns the existing materialization receipt or equivalent converged result; it cannot create duplicate public artifacts.
- Adapter-specific identifiers never become part of publication resource identity.
- Adapter failure records failure/drift; it does not mutate desired state back to the old observed state.
- `observe!` may report reality but cannot redefine what should be published.

## Done when

- Fake-adapter tests prove `:withheld` and `:archived` never publish and remove existing materializations even when translation facts are blocked/stale.
- `:source/current` resolves once to a concrete revision and no-ops when the receipt already names that revision.
- Retrying a publish after an ambiguous external response with the same stable idempotency key does not duplicate materialization.
- A fake adapter can drive the complete reconciler tests with no OpenPlanner dependency.
- OpenPlanner can be unplugged/replaced without changing domain publication code or resource contracts.
- Every successful materialization produces a receipt suitable for drift calculation and deploy verification.

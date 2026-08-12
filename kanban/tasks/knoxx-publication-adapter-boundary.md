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
- Keep planning pure: compare desired publication intent with resource admissibility and observed receipts/projections, then emit actions.
- Handle non-publication states (`:withheld`, `:archived`) before translation/review blockers so removal can never be prevented by missing/stale translation evidence.
- Apply garden admissibility before publication blockers: a publication targeting an archived garden must remove any existing materialization even if the publication intent itself still says `:published`.
- Resolve revision selectors such as `:source/current` to a concrete revision before comparing desired and observed materialization.
- Treat an unresolved revision selector as a publication blocker. A publish operation, idempotency key, or materialization receipt may never carry a nil/unknown concrete revision.
- Treat publication path as part of materialized topology. Path-only changes must produce drift and reconciliation rather than `:noop`.
- Require adapters to return receipts describing what they actually materialized, including target, document, locale, concrete revision, publication path, adapter identity, and idempotency key.
- Make retry/idempotency semantics explicit before the first adapter implementation. The stable publish operation key includes every effect-relevant materialization dimension, including path.
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

(defn garden-active? [resource-index intent]
  (= :active
     (:garden/status
      (get-in resource-index
              [:gardens (:publication/garden intent)]))))

(defn non-public-plan [intent observed reason]
  (if observed
    {:op :remove
     :intent intent
     :reason reason
     :observed observed}
    {:op :noop
     :intent intent
     :reason reason}))

(defn desired-materialization [intent revision]
  {:materialized/revision revision
   :materialized/path (:publication/path intent)})

(defn observed-materialization [observed]
  (select-keys observed
               [:materialized/revision
                :materialized/path]))

(defn reconcile-plan [resource-index intent facts]
  (let [state    (:publication/state intent)
        observed (facts/materialized-publication facts intent)]
    (cond
      (contains? #{:withheld :archived} state)
      (non-public-plan intent observed :publication-not-public)

      (not (garden-active? resource-index intent))
      (non-public-plan intent observed :garden-not-active)

      :else
      (let [revision (concrete-revision intent facts)
            blockers (cond-> (vec (gate/publication-blockers intent facts))
                       (nil? revision)
                       (conj :publication-revision-unresolved))]
        (if (seq blockers)
          {:op :blocked
           :blockers blockers
           :intent intent}
          (let [desired (desired-materialization intent revision)]
            (if (not= desired (observed-materialization observed))
              {:op :publish
               :intent intent
               :desired desired
               :previous observed
               :concrete-revision revision}
              {:op :noop
               :intent intent
               :desired desired
               :concrete-revision revision})))))))
```

Stable publish operation identity:

```clojure
(defn publish-idempotency-key [adapter-id intent concrete-revision]
  (assert (some? concrete-revision))
  (stable/hash
   [(:publication/id intent)
    adapter-id
    (:publication/garden intent)
    (:publication/locale intent)
    (:publication/path intent)
    concrete-revision]))
```

Effect boundary:

```clojure
(defprotocol IPublicationTarget
  (publish! [target ctx intent artifact previous idempotency-key])
  (remove! [target ctx intent observed])
  (observe! [target ctx intent]))

(defn execute-plan! [adapter ctx plan artifact]
  (case (:op plan)
    :publish
    (let [key (publish-idempotency-key
               (adapter/id adapter)
               (:intent plan)
               (:concrete-revision plan))]
      ;; Adapter treats publication identity as one logical materialization.
      ;; If :publication/path moved, the prior path is removed/replaced as part
      ;; of this convergent operation; the old route cannot remain public.
      ;; Replaying the same key returns the existing converged receipt.
      (publish! adapter
                ctx
                (:intent plan)
                artifact
                (:previous plan)
                key))

    :remove  (remove! adapter ctx (:intent plan) (:observed plan))
    :noop    {:receipt/type :publication/noop
              :reason (:reason plan)}
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
 :path "/translation-pipeline"
 :target :gardens/promethean
 :materialized/revision "abc123"
 :materialized/path "/translation-pipeline"}
```

## Contract obligations

- `:withheld` and `:archived` can only remove or no-op; they can never enter the publish branch.
- A `:published` intent targeting an archived garden also can only remove or no-op until the garden is active again.
- Removal is not gated by translation/review blockers. An archive request or garden archive can always remove a stale public artifact.
- Revision selectors are normalized before desired/observed comparison; unchanged `:source/current` publications do not republish every tick.
- If a selector cannot resolve to a concrete revision, reconciliation returns `:blocked` with `:publication-revision-unresolved`; adapters never receive a nil revision.
- Desired/observed convergence compares concrete revision **and publication path**. A path-only edit is drift.
- Path participates in publish idempotency identity. Retrying the same publication/revision/path converges; changing the path creates a distinct operation that replaces/removes the prior route rather than leaving both public.
- Repeating the same `:publish` operation with the same idempotency key returns the existing materialization receipt or equivalent converged result; it cannot create duplicate public artifacts.
- Adapter-specific identifiers never become part of publication resource identity.
- Adapter failure records failure/drift; it does not mutate desired state back to the old observed state.
- `observe!` may report reality but cannot redefine what should be published.

## Done when

- Fake-adapter tests prove `:withheld` and `:archived` never publish and remove existing materializations even when translation facts are blocked/stale.
- Fake-adapter tests prove archiving a target garden removes an already-public artifact even while its publication intent remains `:published`.
- `:source/current` resolves once to a concrete revision and no-ops when the receipt already names that revision and path.
- A fixture with no current source revision blocks with `:publication-revision-unresolved`, produces no publish action, and never computes a publish idempotency key.
- Changing only `:publication/path` emits a publish/move plan, records the new path, and leaves the old path unavailable.
- Retrying a path-aware publish after an ambiguous external response with the same stable idempotency key does not duplicate materialization.
- A fake adapter can drive the complete reconciler tests with no OpenPlanner dependency.
- OpenPlanner can be unplugged/replaced without changing domain publication code or resource contracts.
- Every successful materialization produces a receipt suitable for revision/path drift calculation and deploy verification.

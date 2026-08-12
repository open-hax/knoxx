---
uuid: "knoxx-publication-reconcile-plan-laws"
title: "Define pure publication reconciliation plan laws"
status: accepted
priority: P1
labels: ["tasks", "3sp", "has-parent", "publication", "reconciliation", "laws"]
created_at: "2026-08-12T00:00:00Z"
points: 3
category: tasks
---
# Define pure publication reconciliation plan laws

> Parent task: `knoxx-publication-adapter-boundary`
> Parent epic: `knoxx-contract-owned-publication-pipeline`

## Purpose

Extract the pure decision layer that compares desired publication intent with admissibility and observed facts and emits `:publish`, `:remove`, `:noop`, or `:blocked` without performing I/O.

## Scope

- Resolve `:source/current` to a concrete revision before comparing desired and observed state.
- Add `:publication-revision-unresolved` when a selector cannot resolve.
- Handle `:withheld` and `:archived` before translation/review blockers.
- Treat an archived garden as non-public and remove any existing materialization.
- Compare both concrete revision and publication path for convergence.
- Preserve prior observation in publish/remove plans so the effect layer can replace or remove stale routes.
- Keep the function deterministic and independent of OpenPlanner, Fastify, Mongo, or any adapter implementation.

## CLJS pseudocode

```clojure
(ns knoxx.backend.domain.publication-plan)

(defn concrete-revision [intent facts]
  (case (:publication/revision intent)
    :source/current
    (facts/current-source-revision
     facts (:publication/document intent))

    (:publication/revision intent)))

(defn desired-materialization [intent revision]
  {:materialized/revision revision
   :materialized/path (:publication/path intent)})

(defn reconcile-plan [resource-index intent facts]
  (let [state     (:publication/state intent)
        garden    (get-in resource-index
                          [:gardens (:publication/garden intent)])
        observed  (facts/materialized-publication facts intent)]
    (cond
      (contains? #{:withheld :archived} state)
      {:op (if observed :remove :noop)
       :reason :publication-not-public
       :intent intent
       :observed observed}

      (not= :active (:garden/status garden))
      {:op (if observed :remove :noop)
       :reason :garden-not-active
       :intent intent
       :observed observed}

      :else
      (let [revision (concrete-revision intent facts)
            blockers (cond-> (vec (gate/publication-blockers intent facts))
                       (nil? revision)
                       (conj :publication-revision-unresolved))]
        (if (seq blockers)
          {:op :blocked
           :intent intent
           :blockers blockers}
          (let [desired (desired-materialization intent revision)
                current (select-keys observed
                                     [:materialized/revision
                                      :materialized/path])]
            (if (= desired current)
              {:op :noop
               :intent intent
               :desired desired
               :concrete-revision revision}
              {:op :publish
               :intent intent
               :desired desired
               :previous observed
               :concrete-revision revision})))))))
```

## Laws

- Non-public states can only remove or no-op.
- Garden archive dominates publication intent and translation blockers.
- Removal is never blocked by missing/stale translation evidence.
- No publish plan may carry a nil concrete revision.
- Path-only changes are drift.
- Same resources + facts always produce the same plan.

## Done when

- Pure tests cover published/noop, path-only drift, revision drift, unresolved revision, withheld removal, publication archive removal, archived-garden removal, and translation/review blocking.
- Tests require no adapter or OpenPlanner process.
- The next child can consume the plan without reimplementing semantic decisions.

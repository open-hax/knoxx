---
uuid: "knoxx-publication-receipts-fake-adapter-proof"
title: "Define publication receipts and prove the boundary with a fake adapter"
status: accepted
priority: P1
labels: ["tasks", "2sp", "has-parent", "publication", "receipts", "tests"]
created_at: "2026-08-12T00:00:00Z"
points: 2
category: tasks
---
# Define publication receipts and prove the boundary with a fake adapter

> Parent task: `knoxx-publication-adapter-boundary`
> Parent epic: `knoxx-contract-owned-publication-pipeline`

## Purpose

Close the adapter boundary with one stable observation shape and focused tests that prove desired state, reconciliation, effects, and observed state remain separable with OpenPlanner absent.

## Scope

- Define the minimum successful materialization receipt needed for drift and deploy verification.
- Include publication id, adapter id, idempotency key, document, garden/target, locale, concrete revision, and publication path.
- Define failed/blocked/noop receipts separately from successful materialization evidence.
- Build an in-memory fake `IPublicationTarget` implementing the same effect protocol as production adapters.
- Prove publish, replay, path move, withheld/archive removal, archived-garden removal, and observation after convergence.
- Prove adapter failure leaves desired resource intent untouched and visible as drift.
- Prove OpenPlanner can be completely absent from these tests.

## CLJS pseudocode

```clojure
(ns knoxx.backend.domain.publication-receipts)

(def PublicationMaterializedReceipt
  [:map
   [:receipt/type [:= :publication/materialized]]
   [:publication/id qualified-keyword?]
   [:adapter/id keyword?]
   [:idempotency/key string?]
   [:document/id qualified-keyword?]
   [:target qualified-keyword?]
   [:locale keyword?]
   [:revision string?]
   [:path string?]
   [:materialized/revision string?]
   [:materialized/path string?]])

(defn observed-materialization [receipt]
  (when (= :publication/materialized (:receipt/type receipt))
    (select-keys receipt
                 [:materialized/revision
                  :materialized/path])))
```

Fake adapter sketch:

```clojure
(defrecord FakePublicationTarget [state]
  IPublicationTarget
  (publish! [_ _ intent artifact previous key]
    (if-let [receipt (get-in @state [:by-key key])]
      receipt
      (let [receipt (fake/materialize! state intent artifact previous key)]
        (swap! state assoc-in [:by-key key] receipt)
        receipt)))

  (remove! [_ _ intent observed]
    (fake/remove! state intent observed))

  (observe! [_ _ intent]
    (fake/observe state intent)))
```

Focused proof:

```clojure
(deftest publication-boundary-without-openplanner
  (let [adapter (fake-publication-target)
        plan    (publication-plan/reconcile-plan resources intent facts)
        first   (effects/execute-plan! adapter ctx plan artifact)
        replay  (effects/execute-plan! adapter ctx plan artifact)]
    (is (= first replay))
    (is (= 1 (fake/materialization-count adapter)))
    (is (= (publication-receipts/observed-materialization first)
           {:materialized/revision "abc123"
            :materialized/path "/docs/demo"}))))
```

## Laws

- Receipts describe effects; they never become desired-state authority.
- Only concrete revisions appear in successful materialization receipts.
- Receipt path and revision are sufficient for the planner's drift comparison.
- Replaying the same effect identity yields the same converged observation.
- Removing the fake adapter's runtime projection does not change resource intent.

## Done when

- Fake-adapter tests cover the complete planner/effect/receipt seam with zero OpenPlanner calls.
- Successful receipts satisfy the schema and feed the planner's observed projection directly.
- Failure/noop/blocked evidence cannot be mistaken for a successful materialization.
- The parent `knoxx-publication-adapter-boundary` can close without carrying implementation points of its own.

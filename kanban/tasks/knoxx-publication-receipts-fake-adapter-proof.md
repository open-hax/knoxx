---
category: "tasks"
labels: ["tasks", "2sp", "has-parent", "publication", "receipts", "tests"]
write-id: "1786565796398-0.18ujvt6yfxde3dc8gs"
points: "2"
title: "Define publication receipts and prove the boundary with a fake adapter"
priority: "P1"
status: "ready"
uuid: "knoxx-publication-receipts-fake-adapter-proof"
created_at: "2026-08-12T00:00:00Z"
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

## TDD plan

Test namespaces:

- `knoxx.backend.domain.publication-receipts-test`
  (`backend/test/cljs/knoxx/backend/domain/publication_receipts_test.cljs`)
- `knoxx.backend.domain.publication-boundary-test`
  (`backend/test/cljs/knoxx/backend/domain/publication_boundary_test.cljs`)

Receipt shapes first:

1. `materialized-receipt-validates` — a complete successful receipt satisfies
   `PublicationMaterializedReceipt`; omitting idempotency key, adapter id,
   concrete revision, or path fails.
2. `materialized-receipt-rejects-selector-revision` — `:source/current` as
   `:revision` or `:materialized/revision` fails; only concrete revisions
   appear in successful receipts.
3. `failed-blocked-noop-receipts-are-distinct` — none of them satisfies
   `PublicationMaterializedReceipt`, and `observed-materialization` returns nil
   for each. A blocked receipt can never be mistaken for a materialization.
4. `observed-materialization-feeds-planner-drift` — the map returned by
   `observed-materialization` is exactly the key set the planner compares
   (`:materialized/revision`, `:materialized/path`), asserted against the
   planner's own selection.

Whole-seam proof second, all `^:async` where effects are awaited:

5. `publication-boundary-without-openplanner` — the card's own sketch: plan,
   execute, replay, assert equal receipts and one materialization, and assert
   the observed materialization equals the expected revision/path.
6. `boundary-covers-path-move` — publish, then re-plan after a path change:
   one public route, prior route gone.
7. `boundary-covers-withheld-and-archive-removal` — flipping desired state to
   `:withheld` and to `:archived` each converge to removal.
8. `boundary-covers-archived-garden-removal`.
9. `observation-after-convergence-matches-plan` — `observe!` after convergence
   yields facts that make the planner emit `:noop`.
10. `adapter-failure-leaves-intent-untouched` — failure surfaces as drift and
    the resource intent is unchanged.
11. `no-openplanner-in-test-graph` — assert the test namespace's transitive
    requires contain no `openplanner` segment, so absence is proven rather than
    assumed.

## Done when

- Fake-adapter tests cover the complete planner/effect/receipt seam with zero OpenPlanner calls.
- Successful receipts satisfy the schema and feed the planner's observed projection directly.
- Failure/noop/blocked evidence cannot be mistaken for a successful materialization.
- The parent `knoxx-publication-adapter-boundary` can close without carrying implementation points of its own.

---
Ready gate 2026-08-12: sized 2sp (<=5, eligible to implement). Walked accepted -> breakdown -> ready via the Rheos promethean FSM. Scope, laws and acceptance criteria confirmed on the card; TDD plan section names the failing tests to write first. Closes the adapter-boundary roll-up; depends on the planner and effect cards.

---

Pre-implementation review 2026-08-13 (CodeRabbit, not yet actioned — this card is still `ready`, not started): `FakePublicationTarget`/`IPublicationTarget` is sketched inside the `knoxx.backend.domain.publication-receipts` namespace; move the fake adapter to a test or `infra.*` namespace and keep only receipt schemas/projections in `domain.*`. Only `PublicationMaterializedReceipt` is defined — add schemas for the failed/blocked/noop variants too, have `execute-plan!` return values that conform to them, and validate the materialized receipt before `observed-materialization` reads `:materialized/revision`/`:materialized/path` off it. The fake adapter should also implement the same atomic replay-protection guarantee flagged on `knoxx-publication-adapter-effects-idempotency`, or its tests don't actually prove the production contract.

---

---
category: "tasks"
labels: ["tasks", "3sp", "has-parent", "publication", "reconciliation", "laws"]
write-id: "1786565795198-0.3mpx1qopeqohc3oewey"
points: "3"
title: "Define pure publication reconciliation plan laws"
priority: "P1"
status: "ready"
uuid: "knoxx-publication-reconcile-plan-laws"
created_at: "2026-08-12T00:00:00Z"
---

# Define pure publication reconciliation plan laws

> Parent task: `knoxx-publication-adapter-boundary`
> Parent epic: `knoxx-contract-owned-publication-pipeline`

## Purpose

Extract the pure decision layer that compares desired publication intent with admissibility and observed facts and emits `:publish`, `:remove`, `:noop`, or `:blocked` without performing I/O.

## Scope

- Consume `publication-gate/publication-evidence` so `:source/current` is resolved once and the same concrete revision drives translation/review admissibility and materialization.
- Treat `:publication-revision-unresolved` from that shared evidence result as a hard publication blocker.
- Handle `:withheld` and `:archived` before translation/review blockers.
- Treat an archived garden as non-public and remove any existing materialization.
- Compare both concrete revision and publication path for convergence.
- Preserve prior observation in publish/remove plans so the effect layer can replace or remove stale routes.
- Keep the function deterministic and independent of OpenPlanner, Fastify, Mongo, or any adapter implementation.

## CLJS pseudocode

```clojure
(ns knoxx.backend.domain.publication-plan)

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
      (let [{:keys [concrete-revision blockers]}
            (gate/publication-evidence intent facts)]
        (if (seq blockers)
          {:op :blocked
           :intent intent
           :blockers blockers
           :concrete-revision concrete-revision}
          (let [desired (desired-materialization intent concrete-revision)
                current (select-keys observed
                                     [:materialized/revision
                                      :materialized/path])]
            (if (= desired current)
              {:op :noop
               :intent intent
               :desired desired
               :concrete-revision concrete-revision}
              {:op :publish
               :intent intent
               :desired desired
               :previous observed
               :concrete-revision concrete-revision})))))))
```

## Laws

- Non-public states can only remove or no-op.
- Garden archive dominates publication intent and translation blockers.
- Removal is never blocked by missing/stale translation evidence.
- Reconciliation does not independently resolve a revision after gate evaluation; one concrete revision fact is shared across evidence and materialization.
- No publish plan may carry a nil concrete revision.
- Path-only changes are drift.
- Same resources + facts always produce the same plan.

## TDD plan

Test namespace: `knoxx.backend.domain.publication-plan-test`
(`backend/test/cljs/knoxx/backend/domain/publication_plan_test.cljs`).
Pure tests only — no adapter, no OpenPlanner process.

Precedence first, because ordering is the semantic risk:

1. `withheld-with-observation-removes` — `:withheld` plus an existing
   materialization yields `{:op :remove :reason :publication-not-public}`.
2. `withheld-without-observation-noops`.
3. `archived-with-observation-removes` — an archived publication intent removes
   even when translation evidence is missing, stale, or unapproved. This is the
   "archive before blockers" regression.
4. `archived-garden-removes` — an active intent into an archived garden removes
   any materialization and never publishes.
5. `removal-is-never-blocked` — for every blocker combination, a non-public
   state still yields `:remove`/`:noop`, never `:blocked`.

Convergence second:

6. `converged-state-noops` — desired revision and path equal to observed yields
   `:noop`.
7. `revision-drift-publishes` and `path-only-drift-publishes` — each yields
   `:publish` carrying `:previous` observation so the effect layer can replace
   the stale route.
8. `blocked-plan-carries-blockers` — translation/review blockers yield
   `{:op :blocked}` with the gate's blocker set.
9. `unresolved-revision-is-hard-blocker` —
   `:publication-revision-unresolved` from shared evidence blocks, and no
   publish plan is emitted.
10. `no-publish-plan-has-nil-revision` — property-style sweep over the fixture
    matrix asserting every `:publish` plan carries a non-nil
    `:concrete-revision`.
11. `plan-shares-gate-concrete-revision` — with `:source/current` resolving to
    `"probe-revision"`, both `:concrete-revision` and
    `:desired :materialized/revision` equal the value the gate resolved; the
    planner does not resolve a revision itself.
12. `plan-is-deterministic` — same resources and facts, same plan.

Then implement `knoxx.backend.domain.publication-plan` until green.

## Done when

- Pure tests cover published/noop, path-only drift, revision drift, unresolved revision, withheld removal, publication archive removal, archived-garden removal, and translation/review blocking.
- A `:source/current` test proves the exact concrete revision checked by the gate is the one emitted in `:desired` and `:concrete-revision` by the plan.
- Tests require no adapter or OpenPlanner process.
- The next child can consume the plan without reimplementing semantic decisions.

---
Ready gate 2026-08-12: sized 3sp (<=5, eligible to implement). Walked accepted -> breakdown -> ready via the Rheos promethean FSM. Scope, laws and acceptance criteria confirmed on the card; TDD plan section names the failing tests to write first. Depends on the gate card for the shared publication-evidence result; sequence it after knoxx-translation-publication-gate.

---

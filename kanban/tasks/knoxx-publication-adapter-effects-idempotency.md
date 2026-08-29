---
category: "tasks"
labels: ["tasks", "3sp", "has-parent", "publication", "adapters", "idempotency"]
write-id: "1786608046220-0.usz4p9hqg4ee0llkwrt"
points: "3"
title: "Implement publication adapter effects and idempotency contract"
priority: "P1"
status: "review"
uuid: "knoxx-publication-adapter-effects-idempotency"
created_at: "2026-08-12T00:00:00Z"
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

## TDD plan

Test namespace: `knoxx.backend.infra.publication-effects-test`
(`backend/test/cljs/knoxx/backend/infra/publication_effects_test.cljs`).
Driven by an in-memory `IPublicationTarget` — no OpenPlanner, no network.

Idempotency key first:

1. `key-is-stable-across-calls` — the same intent and concrete revision produce
   the same key twice.
2. `key-includes-every-effect-dimension` — one test per dimension
   (publication id, adapter id, garden, locale, path, concrete revision):
   changing it changes the key. Changing a non-effect field
   (`:translation/review`) does not.
3. `key-requires-concrete-revision` — a nil concrete revision asserts rather
   than hashing a nil.

Plan execution second:

4. `execute-plan!-dispatches-by-op` — `:publish` calls `publish!` with the plan's
   `:previous`, `:remove` calls `remove!` with `:observed`, `:noop` and
   `:blocked` produce receipts and perform no effect (assert the fake recorded
   zero calls).
5. `^:async replay-of-identical-publish-converges` — executing the same publish
   plan twice returns an equal receipt and leaves exactly one materialization.
6. `^:async ambiguous-response-replay-is-safe` — a fake whose first response is
   recorded but reported as ambiguous returns the existing converged receipt on
   replay instead of creating a duplicate.
7. `^:async path-move-replaces-previous-route` — a publish whose path differs
   from `:previous` leaves the old route unavailable and exactly one route
   public.
8. `^:async remove-works-for-prior-materialization` — removal succeeds for an
   observed materialization and is idempotent on a second call.
9. `adapter-does-not-reinterpret-desired-state` — the effect layer given a
   `:remove` plan for a `:published` intent still removes; semantics come from
   the plan.
10. `adapter-failure-produces-drift-not-mutation` — a throwing fake yields
    failure/drift evidence and the desired resource map is unchanged
    (assert on identity of the intent map).
11. `no-openplanner-identifier-in-plan-or-contract` — grep assertion that the
    plan and resource contract namespaces contain no OpenPlanner id.

Then implement `knoxx.backend.infra.publication-effects` until green.

## Done when

- A fake effect implementation proves repeated identical publish calls converge to one materialization.
- A path change replaces the old route.
- Remove works for prior materializations.
- No OpenPlanner-specific identifier appears in the domain plan or resource contract.
- Receipt-producing results are ready for the observation/proof child card.

---
Ready gate 2026-08-12: sized 3sp (<=5, eligible to implement). Walked accepted -> breakdown -> ready via the Rheos promethean FSM. Scope, laws and acceptance criteria confirmed on the card; TDD plan section names the failing tests to write first. Depends on knoxx-publication-reconcile-plan-laws for the plan shape it executes.
---

Pre-implementation review 2026-08-13 (CodeRabbit, not yet actioned — this card is still `ready`, not started): `execute-plan!`'s pseudocode has no fallback for an unrecognized `:op` (falls through to `nil` instead of a typed failure) and doesn't validate the incoming plan or the adapter's publish!/remove! result against an explicit contract at the effect boundary — add both before implementing. Also, `publish-once!`'s replay protection is a separate existence-check + materialize, which is not atomic; a concurrent call or a crash between the two steps can duplicate a public artifact or leave unrecoverable state. Use one atomic idempotency-key reservation instead.
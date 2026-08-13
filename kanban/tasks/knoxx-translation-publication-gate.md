---
category: "tasks"
labels: ["tasks", "5sp", "has-parent", "translations", "publication", "review"]
write-id: "1786565794577-0.67qrow20j6dyyftf8ew"
points: "5"
title: "Gate publication on translation and review receipts without making workflow state contractual"
priority: "P1"
status: "ready"
uuid: "knoxx-translation-publication-gate"
created_at: "2026-08-12T00:00:00Z"
---

# Gate publication on translation and review receipts without making workflow state contractual

> Parent epic: `knoxx-contract-owned-publication-pipeline`

## Purpose

Make translation/review a publication prerequisite derived from contract intent plus runtime receipts. Desired state says what must eventually be published; receipts say what has actually happened; a pure decision computes blockers.

Do **not** encode `:translating`, `:reviewing`, `:worker-failed`, or similar operational states into publication resources.

## Scope

- Define the minimum receipt/projection facts needed to admit a translated publication: translation exists for the requested **concrete** revision/locale, required review passed for that same revision, and no superseding source revision invalidated it.
- Resolve revision selectors such as `:source/current` exactly once before any translation/review evidence lookup; selector tokens are never used as receipt revision identities.
- Return the resolved concrete revision alongside blocker data so reconciliation, queueing, and materialization all consume the same revision fact.
- Consume the resolved document source locale from `PublicationIntent`; never default source language in the gate.
- Compute blockers as pure domain data.
- Feed blockers into CMS and the publication reconciler.
- Trigger/queue translation work from missing **or stale** publication evidence without making the queue authoritative; work is keyed to the concrete revision, not `:source/current`.
- Require `:publication/state :published` plus the translation admissibility guard before deriving translation work. Archived and withheld intents stay in the resolver projection by design, so an evidence-only check would keep queueing obsolete content for them forever.
- Treat approval as revision-specific: when the source revision changes, the old approval remains historical evidence but cannot satisfy the replacement translation.
- Ensure a corrected/re-reviewed translation can satisfy the same immutable publication intent without editing that intent solely to clear a workflow state.

## CLJS pseudocode

```clojure
(ns knoxx.backend.domain.publication-gate)

(defn translation-required? [intent]
  (not= (:publication/locale intent)
        (:document/source-locale intent)))

(defn resolve-concrete-revision [intent facts]
  (case (:publication/revision intent)
    :source/current
    (facts/current-source-revision
     facts (:publication/document intent))

    (:publication/revision intent)))

(defn publication-evidence [intent facts]
  (let [revision (resolve-concrete-revision intent facts)
        blockers
        (cond-> []
          (nil? revision)
          (conj :publication-revision-unresolved)

          (and revision
               (translation-required? intent)
               (not (translation/translated-revision?
                     facts
                     (:publication/document intent)
                     (:publication/locale intent)
                     revision)))
          (conj :translation-missing)

          (and revision
               (= :required (:translation/review intent))
               (not (review/approved?
                     facts
                     (:publication/document intent)
                     (:publication/locale intent)
                     revision)))
          (conj :translation-review-required)

          (and revision
               (translation/source-revision-superseded?
                facts intent revision))
          (conj :translation-stale))]
    {:concrete-revision revision
     :blockers blockers}))

(defn publication-blockers [intent facts]
  (:blockers (publication-evidence intent facts)))

(defn publication-admissible? [intent facts]
  (let [{:keys [concrete-revision blockers]}
        (publication-evidence intent facts)]
    (and (= :published (:publication/state intent))
         (some? concrete-revision)
         (empty? blockers))))
```

Queueing is derivative and uses the exact same concrete revision selected for evidence checks.

Queueing is also gated on the intent actually wanting publication. The resolver
deliberately preserves `:archived` and `:withheld` intents in its projection, so an
orchestrator reconciling that projection would otherwise keep submitting obsolete
content to the translation worker indefinitely. Desired state and the translation
admissibility guard are checked before any work is derived; blockers contribute only
translation/review evidence:

```clojure
(defn translation-work-eligible? [intent]
  (and (= :published (:publication/state intent))
       (translation-required? intent)))

(defn reconcile-translation-work [intent facts]
  (let [{:keys [concrete-revision blockers]}
        (publication-evidence intent facts)
        blockers (set blockers)]
    (when (and (translation-work-eligible? intent)
               concrete-revision
               (or (contains? blockers :translation-missing)
                   (contains? blockers :translation-stale)))
      {:action/id :actions/request-translation
       :action/with {:document (:publication/document intent)
                     :locale (:publication/locale intent)
                     :revision concrete-revision
                     :replace-stale? (contains? blockers :translation-stale)}})))
```

Review reset is semantic rather than destructive:

```clojure
(defn review-satisfies-intent? [approval intent concrete-revision]
  (and (= (:review/document approval) (:publication/document intent))
       (= (:review/locale approval) (:publication/locale intent))
       (= (:review/revision approval) concrete-revision)
       (= :approved (:review/state approval))))
```

A stale translation does **not** delete the old approval receipt. The old receipt simply stops satisfying the new concrete revision, so the replacement translation requires a new approval when review is `:required`.

## Laws

- A revision selector is resolved before translation or review evidence is queried; `:source/current` is never compared directly to a receipt revision.
- One `publication-evidence` result supplies the concrete revision used by blockers, translation work, reconciliation, and materialization.
- If `:source/current` cannot resolve, the gate emits `:publication-revision-unresolved`; it does not query evidence or queue revisionless translation work.
- Removing all worker/job rows must not change desired publication intent.
- A new source revision invalidates a translation/review receipt for the old revision unless the contract explicitly pins the old revision.
- Stale evidence queues replacement translation work; it cannot remain blocked indefinitely with no derivable action.
- Only an intent whose desired state is `:published` can derive translation work. Archived and withheld intents remain in the projection as history and never produce queue actions.
- Required review cannot be bypassed by an adapter directly observing a translated artifact.
- Re-running the pure gate over the same intent + facts produces the same concrete revision and blocker set.

## TDD plan

Test namespace: `knoxx.backend.domain.publication-gate-test`
(`backend/test/cljs/knoxx/backend/domain/publication_gate_test.cljs`).
All tests are pure; no adapter, no worker, no OpenPlanner.

Revision resolution first:

1. `source-current-resolves-once` — with `:source/current` resolving to
   `"probe-revision"`, the translation and review fact lookups are called with
   `"probe-revision"`. Assert on a recording fact stub that
   `:source/current` never appears in any lookup argument.
2. `unresolvable-source-current-blocks` — when the current source revision is
   nil, blockers are exactly `[:publication-revision-unresolved]`, no evidence
   lookup happens, and no translation work is derived.
3. `evidence-returns-concrete-revision` — `publication-evidence` returns
   `:concrete-revision` alongside `:blockers`, and `publication-blockers` is
   derived from that one result.

Blocker semantics second:

4. `translation-missing-blocks` / `review-required-blocks` /
   `stale-translation-blocks` — one test each, asserting the exact blocker
   keyword.
5. `source-locale-comes-from-intent` — a publication whose locale equals the
   document source locale requires no translation; the gate never defaults a
   language.
6. `gate-is-deterministic` — the same intent and facts produce an identical
   concrete revision and blocker set across repeated calls.

Queue derivation last — the review thread's regression:

7. `archived-intent-derives-no-translation-work` — an archived intent missing
   its target translation yields `nil` from `reconcile-translation-work`.
8. `withheld-intent-derives-no-translation-work` — same for `:withheld`.
9. `published-intent-derives-translation-work` — the otherwise identical
   published intent does derive `:actions/request-translation`. Tests 7-9 share
   one fixture so only `:publication/state` differs.
10. `work-is-keyed-to-concrete-revision` — the derived action carries
    `"probe-revision"`, never `:source/current`, and sets `:replace-stale?`
    only for stale evidence.
11. `approval-is-revision-specific` — an approval for the old revision does not
    satisfy the replacement revision, while the old receipt remains intact.

Then implement `knoxx.backend.domain.publication-gate` until green.

## Done when

- CMS can explain exactly why a requested publication is blocked.
- A `:source/current` fixture resolving to `"probe-revision"` checks translation and approval receipts against `"probe-revision"`, never against `:source/current`.
- The reconciler consumes the same `:concrete-revision` returned by `publication-evidence` rather than independently selecting another revision.
- The reconciler refuses to publish a locale/revision that lacks required translation/review evidence.
- Missing and stale translations both derive translation work keyed to the concrete revision.
- An archived intent and a withheld intent, each missing its target translation, derive no translation work, while the otherwise identical published intent does.
- A replacement revision cannot inherit approval from an older translation, while historical approval receipts remain intact.
- No mutable worker/review state is promoted into the declarative resource graph.

---
Ready gate 2026-08-12: sized 5sp (<=5, eligible to implement). Walked accepted -> breakdown -> ready via the Rheos promethean FSM. Scope, laws and acceptance criteria confirmed on the card; TDD plan section names the failing tests to write first. Pure card: no adapter, worker or OpenPlanner dependency in its tests.

---

Pre-implementation review 2026-08-13 (CodeRabbit, not yet actioned — this card is still `ready`, not started): `publication-blockers`, `publication-admissible?`, and `reconcile-translation-work` each call `publication-evidence` independently in the sketch. If `:source/current` resolves to a different concrete revision between calls, the decision that admitted publication can disagree with the revision actually queued/materialized. Compute `publication-evidence` once at the orchestration boundary and thread that single result through all three, and add a test that changes the current-revision result between calls to prove one revision is retained throughout.

---

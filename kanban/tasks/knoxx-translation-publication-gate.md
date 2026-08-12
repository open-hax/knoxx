---
uuid: "knoxx-translation-publication-gate"
title: "Gate publication on translation and review receipts without making workflow state contractual"
status: accepted
priority: P1
labels: ["tasks", "5sp", "has-parent", "translations", "publication", "review"]
created_at: "2026-08-12T00:00:00Z"
points: 5
category: tasks
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

Queueing is derivative and uses the exact same concrete revision selected for evidence checks:

```clojure
(defn reconcile-translation-work [intent facts]
  (let [{:keys [concrete-revision blockers]}
        (publication-evidence intent facts)
        blockers (set blockers)]
    (when (and concrete-revision
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
- Required review cannot be bypassed by an adapter directly observing a translated artifact.
- Re-running the pure gate over the same intent + facts produces the same concrete revision and blocker set.

## Done when

- CMS can explain exactly why a requested publication is blocked.
- A `:source/current` fixture resolving to `"probe-revision"` checks translation and approval receipts against `"probe-revision"`, never against `:source/current`.
- The reconciler consumes the same `:concrete-revision` returned by `publication-evidence` rather than independently selecting another revision.
- The reconciler refuses to publish a locale/revision that lacks required translation/review evidence.
- Missing and stale translations both derive translation work keyed to the concrete revision.
- A replacement revision cannot inherit approval from an older translation, while historical approval receipts remain intact.
- No mutable worker/review state is promoted into the declarative resource graph.

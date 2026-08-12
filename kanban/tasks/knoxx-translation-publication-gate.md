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

- Define the minimum receipt/projection facts needed to admit a translated publication: translation exists for requested revision/locale, required review passed, no superseding source revision invalidated it.
- Consume the resolved document source locale from `PublicationIntent`; never default source language in the gate.
- Compute blockers as pure domain data.
- Feed blockers into CMS and the publication reconciler.
- Trigger/queue translation work from missing **or stale** publication evidence without making the queue authoritative.
- Treat approval as revision-specific: when the source revision changes, the old approval remains historical evidence but cannot satisfy the replacement translation.
- Ensure a corrected/re-reviewed translation can satisfy the same immutable publication intent without editing that intent solely to clear a workflow state.

## CLJS pseudocode

```clojure
(ns knoxx.backend.domain.publication-gate)

(defn translation-required? [intent]
  (not= (:publication/locale intent)
        (:document/source-locale intent)))

(defn publication-blockers [intent facts]
  (cond-> []
    (and (translation-required? intent)
         (not (translation/translated-revision?
               facts
               (:publication/document intent)
               (:publication/locale intent)
               (:publication/revision intent))))
    (conj :translation-missing)

    (and (= :required (:translation/review intent))
         (not (review/approved?
               facts
               (:publication/document intent)
               (:publication/locale intent)
               (:publication/revision intent))))
    (conj :translation-review-required)

    (translation/source-revision-superseded? facts intent)
    (conj :translation-stale)))

(defn publication-admissible? [intent facts]
  (and (= :published (:publication/state intent))
       (empty? (publication-blockers intent facts))))
```

Queueing is derivative and handles stale evidence as replacement work:

```clojure
(defn reconcile-translation-work [intent facts]
  (let [blockers (set (publication-blockers intent facts))]
    (when (or (contains? blockers :translation-missing)
              (contains? blockers :translation-stale))
      {:action/id :actions/request-translation
       :action/with {:document (:publication/document intent)
                     :locale (:publication/locale intent)
                     :revision (:publication/revision intent)
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

- Removing all worker/job rows must not change desired publication intent.
- A new source revision invalidates a translation/review receipt for the old revision unless the contract explicitly pins the old revision.
- Stale evidence queues replacement translation work; it cannot remain blocked indefinitely with no derivable action.
- Required review cannot be bypassed by an adapter directly observing a translated artifact.
- Re-running the pure gate over the same intent + facts produces the same blocker set.

## Done when

- CMS can explain exactly why a requested publication is blocked.
- The reconciler refuses to publish a locale/revision that lacks required translation/review evidence.
- Missing and stale translations both derive translation work.
- A replacement revision cannot inherit approval from an older translation, while historical approval receipts remain intact.
- No mutable worker/review state is promoted into the declarative resource graph.

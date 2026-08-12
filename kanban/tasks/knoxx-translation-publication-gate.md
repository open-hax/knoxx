---
uuid: "knoxx-translation-publication-gate"
title: "Gate publication on translation and review receipts without making workflow state contractual"
status: incoming
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
- Compute blockers as pure domain data.
- Feed blockers into CMS and the publication reconciler.
- Trigger/queue translation work from unmet publication intent without making the queue authoritative.
- Ensure a corrected/re-reviewed translation can satisfy the same immutable publication intent without editing that intent solely to clear a workflow state.

## CLJS pseudocode

```clojure
(ns knoxx.backend.domain.publication-gate)

(defn translation-required? [intent]
  (not= (:publication/locale intent)
        (:document/source-locale intent :en)))

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

Queueing is derivative:

```clojure
(defn reconcile-translation-work [intent facts]
  (when (some #{:translation-missing}
              (publication-blockers intent facts))
    {:action/id :actions/request-translation
     :action/with {:document (:publication/document intent)
                   :locale (:publication/locale intent)
                   :revision (:publication/revision intent)}}))
```

## Laws

- Removing all worker/job rows must not change desired publication intent.
- A new source revision invalidates a translation/review receipt for the old revision unless the contract explicitly pins the old revision.
- Required review cannot be bypassed by an adapter directly observing a translated artifact.
- Re-running the pure gate over the same intent + facts produces the same blocker set.

## Done when

- CMS can explain exactly why a requested publication is blocked.
- The reconciler refuses to publish a locale/revision that lacks required translation/review evidence.
- No mutable worker/review state is promoted into the declarative resource graph.

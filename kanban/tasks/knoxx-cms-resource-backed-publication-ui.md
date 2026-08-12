---
uuid: "knoxx-cms-resource-backed-publication-ui"
title: "Make CMS read and write publication intent through Knoxx resources"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent", "cms", "publication", "frontend"]
created_at: "2026-08-12T00:00:00Z"
points: 5
category: tasks
---
# Make CMS read and write publication intent through Knoxx resources

> Parent epic: `knoxx-contract-owned-publication-pipeline`

## Purpose

Stop `CmsPage` from treating OpenPlanner garden rows and `metadata.garden_publications` as publication truth. The CMS should become an editor/view over Knoxx resource intent.

## Current coupling to remove

- garden discovery through `/api/openplanner/v1/gardens`;
- `CmsDocMetadata.garden_publications` as the source for published garden ids;
- publish/unpublish actions whose semantic result exists only in OpenPlanner metadata.

## Scope

- Load document/garden/publication views from the Knoxx publication facade.
- Render desired publication state separately from observed adapter state.
- Make publish/unpublish/archive edits update the relevant publication resource through the existing resource-write boundary (or the narrowest new resource-write facade required).
- Preserve explicit locale/revision/policy dimensions when editing.
- Do not make the frontend parse EDN itself; frontend consumes normalized API data.
- Keep dirty/save semantics explicit: editing body content and editing publication intent are separate mutations even when exposed in one CMS surface.

## CLJS pseudocode

Backend facade shape:

```clojure
(defn cms-document-view [resource-index receipts document-id]
  {:document (publication/document-view resource-index document-id)
   :observed (publication-status/observed-for receipts document-id)})

(defn put-publication-intent! [ctx publication-id patch]
  (let [current (resources/resolve-one ctx publication-id)
        next    (publication/apply-edit current patch)]
    (law/assert! publication/PublicationIntent next)
    (resources/write! ctx next)))
```

Frontend page flow:

```clojure
(defn load-cms! []
  (-> (api/request "/api/publications/documents")
      (.then #(swap! state assoc
                     :documents (:documents %)
                     :gardens (:gardens %)))))

(defn request-publish! [{:keys [publication-id locale revision]}]
  (api/request
   (str "/api/publications/intents/" (js/encodeURIComponent publication-id))
   {:method "PATCH"
    :body {:publication/state :published
           :publication/locale locale
           :publication/revision revision}}))
```

UI state must distinguish:

```clojure
{:desired :published
 :observed :pending     ; receipt/projection, not contract state
 :blockers [:translation-review-required]}
```

## Watch out

- Do not preserve `publishedGardenIds` as a second client-side authority; derive selected/publication badges from the returned desired-state projection.
- Do not silently write runtime timestamps/job ids back into resources after publishing.
- A failed adapter call must leave desired state unchanged and surface drift, not revert the user's requested contract state unless the user explicitly changes it.

## Done when

- CMS can render document publication topology with OpenPlanner REST disabled.
- Publishing from the CMS changes a Knoxx publication resource and the UI reflects desired state from that resource.
- No CMS code reads `garden_publications` to determine semantic publication state.

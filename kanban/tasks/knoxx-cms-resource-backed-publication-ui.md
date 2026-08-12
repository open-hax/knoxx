---
uuid: "knoxx-cms-resource-backed-publication-ui"
title: "Make CMS read and write publication intent through Knoxx resources"
status: accepted
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

This cutover happens only after `knoxx-openplanner-publication-state-migration` has imported the existing topology and conflicts have been resolved.

## Current coupling to remove

- garden discovery through `/api/openplanner/v1/gardens`;
- `CmsDocMetadata.garden_publications` as the source for published garden ids;
- publish/unpublish actions whose semantic result exists only in OpenPlanner metadata.

## Scope

- Load document/garden/publication views from the Knoxx publication facade.
- Define one normalized wire contract for list and document responses; do not wrap `publication/document-view` under another `:document` key.
- Map resource `:publication/state` to wire-level `:desired` in the backend facade and combine it with observed receipt status there.
- Render desired publication state separately from observed adapter state.
- Make publish/unpublish/archive edits update only the relevant publication **state** through the existing resource-write boundary (or the narrowest new resource-write facade required).
- Treat publication identity dimensions — document, garden, locale, revision selector — as immutable for state edits. Any re-key must be an explicit operation with duplicate/conflict validation.
- Do not make the frontend parse EDN itself; frontend consumes normalized API data.
- Keep dirty/save semantics explicit: editing body content and editing publication intent are separate mutations even when exposed in one CMS surface.

## CLJS pseudocode

Normalized backend facade:

```clojure
(def PublicationWire
  [:map
   [:id qualified-keyword?]
   [:document qualified-keyword?]
   [:garden qualified-keyword?]
   [:locale :keyword]
   [:revision [:or :string :keyword]]
   [:path :string]
   [:desired [:enum :published :withheld :archived]]
   [:observed {:optional true} :keyword]
   [:blockers [:vector :keyword]]])

(def CmsDocumentWire
  [:map
   [:document :map]
   [:publications [:vector PublicationWire]]])

(def CmsListWire
  [:map
   [:documents [:vector CmsDocumentWire]]
   [:gardens [:vector :map]]])

(defn publication->wire [receipts intent]
  {:id (:publication/id intent)
   :document (:publication/document intent)
   :garden (:publication/garden intent)
   :locale (:publication/locale intent)
   :revision (:publication/revision intent)
   :path (:publication/path intent)
   :desired (:publication/state intent)
   :observed (publication-status/observed-for receipts intent)
   :blockers (publication-status/blockers-for receipts intent)})

(defn cms-document-view [resource-index receipts document-id]
  (let [{:keys [document publications]}
        (publication/document-view resource-index document-id)]
    {:document document
     :publications (mapv #(publication->wire receipts %) publications)}))

(defn cms-list-view [resource-index receipts]
  (let [{:keys [documents gardens]}
        (publication/list-document-views resource-index)]
    {:documents (mapv #(cms-document-view resource-index receipts
                                           (get-in % [:document :document/id]))
                      documents)
     :gardens gardens}))
```

State-only mutation boundary:

```clojure
(def PublicationStatePatch
  [:map [:publication/state [:enum :published :withheld :archived]]])

(defn put-publication-state! [ctx publication-id patch]
  (law/assert! PublicationStatePatch patch)
  (let [current (resources/resolve-one ctx publication-id)
        next    (assoc current :publication/state
                               (:publication/state patch))]
    ;; locale/revision/document/garden cannot move through this endpoint.
    (law/assert! publication/PublicationIntentResource next)
    (resources/write! ctx next)))

(defn rekey-publication! [ctx publication-id new-identity]
  (publication/assert-no-identity-conflict! ctx publication-id new-identity)
  (resources/rekey! ctx publication-id new-identity))
```

Frontend page flow consumes those exact keys:

```clojure
(defn load-cms! []
  (-> (api/request "/api/publications/documents")
      (.then #(swap! state assoc
                     :documents (:documents %)
                     :gardens (:gardens %)))))

(defn request-publish! [{:keys [publication-id]}]
  (api/request
   (str "/api/publications/intents/" (js/encodeURIComponent publication-id))
   {:method "PATCH"
    :body {:publication/state :published}}))
```

Wire state is explicit and non-authoritative on the observed side:

```clojure
{:desired :published
 :observed :pending
 :blockers [:translation-review-required]}
```

## Watch out

- Do not preserve `publishedGardenIds` as a second client-side authority; derive selected/publication badges from `:desired` in the returned publication projection.
- Do not silently write runtime timestamps/job ids back into resources after publishing.
- A failed adapter call must leave desired state unchanged and surface drift, not revert the user's requested contract state unless the user explicitly changes it.
- State PATCHes cannot change locale, revision, document, or garden. Re-keying is explicit and conflict-checked.

## Done when

- CMS can render document publication topology with OpenPlanner REST disabled.
- List and document routes expose one consistent `{documents, gardens}` / `{document, publications}` vocabulary with publication `{desired, observed, blockers}` fields.
- Publishing from the CMS changes only the Knoxx publication resource's desired state and the UI reflects it from the normalized response.
- Publication identity cannot silently move through publish/unpublish/archive edits.
- No CMS code reads `garden_publications` to determine semantic publication state.

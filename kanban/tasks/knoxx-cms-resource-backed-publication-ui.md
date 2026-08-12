---
uuid: "knoxx-cms-resource-backed-publication-ui"
title: "Make CMS read and write publication intent through Knoxx resources"
status: accepted
priority: P2
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
- Define one normalized JSON wire contract for list and document responses; do not wrap `publication/document-view` under another `:document` key.
- Treat JSON enum values as strings at the HTTP boundary and explicitly convert them to/from domain keywords. Keywordizing object keys is not sufficient.
- Keep JSON wire **keys** unqualified. The shared `knoxx.frontend.lib.api/request` helper serializes with `clj->js` and decodes with `js->clj :keywordize-keys true`, so keyword namespaces never survive the round trip; qualified domain keys are produced by explicit adapter mapping, never by wire validation.
- Reuse the resource wire identity convention: keyword ids serialize as `namespace/name` with no EDN leading colon and decode back to the same qualified keyword.
- Give document and garden rows explicit JSON wire contracts/codecs too; never return raw resource maps and rely on `clj->js` for identity or enum serialization.
- Map resource `:publication/state` to wire-level `"desired"` string values in the backend facade and combine it with observed receipt status there.
- Encode keyword-valued response fields symmetrically (document/garden/publication IDs, source/target locales, garden status, desired/observed state, blockers), then explicitly decode them into CLJS UI-domain values in the frontend helper.
- Render desired publication state separately from observed adapter state.
- Make publish/unpublish/archive edits update only the relevant publication **state** through the existing resource-write boundary (or the narrowest new resource-write facade required).
- Treat publication identity dimensions — document, garden, locale, revision selector — as immutable for state edits. Any re-key must be an explicit operation with duplicate/conflict validation.
- Keep Fastify/native request objects inside the HTTP adapter: decode once, then read route params and body from the decoded CLJS request map.
- Do not make the frontend parse EDN itself; frontend consumes normalized JSON API data.
- Keep dirty/save semantics explicit: editing body content and editing publication intent are separate mutations even when exposed in one CMS surface.
- New asynchronous frontend/adapter CLJS uses native `^:async` + `await`, not `.then` chains.

## CLJS pseudocode

JSON wire contracts use JSON-safe scalar values throughout:

```clojure
(def DocumentWireJson
  [:map
   [:id :string]
   [:title :string]
   [:source-locale :string]
   [:source [:map [:path :string]]]])

(def GardenWireJson
  [:map
   [:id :string]
   [:title :string]
   [:status [:enum "active" "archived"]]])

(def PublicationWireJson
  [:map
   [:id :string]
   [:document :string]
   [:garden :string]
   [:locale :string]
   [:revision :string]
   [:path :string]
   [:desired [:enum "published" "withheld" "archived"]]
   [:observed {:optional true} [:maybe :string]]
   [:blockers [:vector :string]]])

(def CmsDocumentWireJson
  [:map
   [:document DocumentWireJson]
   [:publications [:vector PublicationWireJson]]])

(def CmsListWireJson
  [:map
   [:documents [:vector CmsDocumentWireJson]]
   [:gardens [:vector GardenWireJson]]])
```

Backend encoding is explicit rather than relying on incidental keyword serialization. Qualified keyword ids never include an EDN leading colon:

```clojure
(defn encode-keyword [x]
  (when x
    (if-let [ns (namespace x)]
      (str ns "/" (name x))
      (name x))))

(defn encode-id [x]
  (if (keyword? x)
    (encode-keyword x)
    (str x)))

(defn decode-id [x]
  (keyword x))

(defn document->wire [document]
  (law/assert!
   DocumentWireJson
   {:id (encode-id (:document/id document))
    :title (:document/title document)
    :source-locale (encode-keyword (:document/source-locale document))
    :source (:document/source document)}))

(defn garden->wire [garden]
  (law/assert!
   GardenWireJson
   {:id (encode-id (:garden/id garden))
    :title (:garden/title garden)
    :status (encode-keyword (:garden/status garden))}))

(defn publication->wire [receipts intent]
  (law/assert!
   PublicationWireJson
   {:id (encode-id (:publication/id intent))
    :document (encode-id (:publication/document intent))
    :garden (encode-id (:publication/garden intent))
    :locale (encode-keyword (:publication/locale intent))
    :revision (if (keyword? (:publication/revision intent))
                (encode-keyword (:publication/revision intent))
                (:publication/revision intent))
    :path (:publication/path intent)
    :desired (encode-keyword (:publication/state intent))
    :observed (some-> (publication-status/observed-for receipts intent)
                      encode-keyword)
    :blockers (mapv encode-keyword
                    (publication-status/blockers-for receipts intent))}))

(defn cms-document-view [resource-index receipts document-id]
  (let [{:keys [document publications]}
        (publication/document-view resource-index document-id)]
    (law/assert!
     CmsDocumentWireJson
     {:document (document->wire document)
      :publications (mapv #(publication->wire receipts %) publications)})))

(defn cms-list-view [resource-index receipts]
  (let [{:keys [documents gardens]}
        (publication/list-document-views resource-index)]
    (law/assert!
     CmsListWireJson
     {:documents (mapv #(cms-document-view resource-index receipts
                                            (get-in % [:document :document/id]))
                       documents)
      :gardens (mapv garden->wire gardens)})))
```

State mutation has separate JSON and domain contracts. The JSON wire key is
deliberately **unqualified**: `knoxx.frontend.lib.api/request` serializes bodies with
`clj->js`, which drops keyword namespaces (`:publication/state` leaves as JSON
`"state"`), and decodes responses with `js->clj :keywordize-keys true`, which yields
`:state`. A wire contract requiring `:publication/state` would therefore reject every
publish request from the CMS before it reached the domain patch. The wire contract
validates `:state` and the adapter maps it explicitly onto the qualified domain key:

```clojure
(def PublicationStatePatchJson
  [:map [:state [:enum "published" "withheld" "archived"]]])

(def PublicationStatePatch
  [:map [:publication/state [:enum :published :withheld :archived]]])

(defn decode-publication-state-patch [wire]
  (law/assert! PublicationStatePatchJson wire)
  (let [domain {:publication/state (keyword (:state wire))}]
    (law/assert! PublicationStatePatch domain)
    domain))

(defn put-publication-state! [ctx publication-id domain-patch]
  (law/assert! PublicationStatePatch domain-patch)
  (let [current (resources/resolve-one ctx publication-id)
        next    (assoc current :publication/state
                               (:publication/state domain-patch))]
    ;; locale/revision/document/garden cannot move through this endpoint.
    (law/assert! publication/PublicationIntentResource next)
    (resources/write! ctx next)))

(defn rekey-publication! [ctx publication-id new-identity]
  (publication/assert-no-identity-conflict! ctx publication-id new-identity)
  (resources/rekey! ctx publication-id new-identity))
```

The Fastify/HTTP adapter decodes once and reads both body and route identity from the decoded CLJS map:

```clojure
(defn ^:async patch-publication-state-route! [ctx req]
  (let [request        (decode-request req)
        wire           (:body request)
        publication-id (decode-id (get-in request [:params :publication-id]))
        domain         (decode-publication-state-patch wire)]
    (await (put-publication-state! ctx publication-id domain))))
```

Frontend wire decoding is symmetric across every resource row:

```clojure
(defn decode-document-wire [wire]
  (-> wire
      (update :id decode-id)
      (update :source-locale keyword)))

(defn decode-garden-wire [wire]
  (-> wire
      (update :id decode-id)
      (update :status keyword)))

(defn decode-publication-wire [wire]
  (-> wire
      (update :id decode-id)
      (update :document decode-id)
      (update :garden decode-id)
      (update :locale keyword)
      (update :desired keyword)
      (update :observed #(when % (keyword %)))
      (update :blockers #(mapv keyword %))))

(defn decode-cms-list-wire [wire]
  (-> wire
      (update :documents
              #(mapv (fn [doc]
                       (-> doc
                           (update :document decode-document-wire)
                           (update :publications
                                   (fn [publications]
                                     (mapv decode-publication-wire publications)))))
                     %))
      (update :gardens #(mapv decode-garden-wire %))))
```

Frontend page flow consumes those exact keys using native async/await and serializes ids through the same colon-free resource-id convention:

```clojure
(defn ^:async load-cms! []
  (let [wire     (await (api/request "/api/publications/documents"))
        response (decode-cms-list-wire wire)]
    (swap! state assoc
           :documents (:documents response)
           :gardens (:gardens response))))

(defn ^:async request-publish! [{:keys [publication-id]}]
  (await
   (api/request
    (str "/api/publications/intents/"
         (js/encodeURIComponent (encode-id publication-id)))
    {:method "PATCH"
     :body {:state "published"}})))
```

UI-domain state remains keyword-oriented after explicit frontend decoding:

```clojure
{:desired :published
 :observed :pending
 :blockers [:translation-review-required]}
```

## Watch out

- Do not preserve `publishedGardenIds` as a second client-side authority; derive selected/publication badges from `:desired` in the decoded publication projection.
- Do not return raw document/garden resource maps at the JSON boundary; their IDs/locales/statuses use the same explicit wire convention as publication rows.
- Do not rely on `(str keyword)` for resource wire ids: `:docs/probe` must serialize as `"docs/probe"`, never `":docs/probe"`.
- Do not read route params from the raw Fastify request after decoding; native request handles stay at the adapter edge.
- Do not rely on JSON serialization to preserve Clojure keyword values. Wire enums are strings; domain enums are keywords; adapters convert explicitly in both directions.
- Do not put namespaced keys in a JSON wire contract. `clj->js` erases the namespace on the way out, so a `:publication/state` wire requirement can never be satisfied by a body sent through `api/request`; declare `:state` and map it.
- Do not silently write runtime timestamps/job ids back into resources after publishing.
- A failed adapter call must leave desired state unchanged and surface drift, not revert the user's requested contract state unless the user explicitly changes it.
- State PATCHes cannot change locale, revision, document, or garden. Re-keying is explicit and conflict-checked.

## Done when

- CMS can render document publication topology with OpenPlanner REST disabled.
- List and document routes expose one consistent `{documents, gardens}` / `{document, publications}` vocabulary with explicit JSON-safe document, garden, and publication contracts.
- A state PATCH body sent through `api/request` as `{:state "published"}` — the exact JSON `{"state":"published"}` that `clj->js` produces — passes `PublicationStatePatchJson` and decodes to domain `{:publication/state :published}` before Malli validates `PublicationStatePatch`.
- A test asserts the publish request built by the frontend helper is accepted by the backend wire contract, so wire key and wire validator cannot drift apart.
- Qualified resource ids round-trip exactly: domain `:docs/probe` -> JSON `"docs/probe"` -> domain `:docs/probe`, and PATCH URLs contain no spurious colon.
- Document source locale and garden status round-trip through explicit string encoding/keyword decoding, never incidental `clj->js` behavior.
- The PATCH adapter obtains `publication-id` from decoded request params, never by keyword lookup on the native Fastify object.
- Response keyword-valued fields round-trip through explicit JSON encoding and frontend decoding without relying on implicit keyword serialization.
- `load-cms!` uses `^:async`/`await` and contains no Promise `.then` chain.
- Publishing from the CMS changes only the Knoxx publication resource's desired state and the UI reflects it from the normalized response.
- Publication identity cannot silently move through publish/unpublish/archive edits.
- No CMS code reads `garden_publications` to determine semantic publication state.

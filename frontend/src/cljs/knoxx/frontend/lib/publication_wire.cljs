(ns knoxx.frontend.lib.publication-wire
  "Decode the CMS publication wire into UI-domain values, and build the requests
  that go back.

  The frontend never parses EDN. It receives normalized JSON, and every
  keyword-valued field is decoded explicitly here — symmetrically with the
  backend encoders — so the UI works in keywords while the wire stays scalar.

  Ids arrive as `\"namespace/name\"` with no leading colon and decode back to the
  same qualified keyword, so two documents differing only by namespace never
  collapse onto one UI row."
  (:require [knoxx.frontend.lib.api :as api]
            [open-hax.publication-wire :as wire]))

;; ── Identity and selectors ────────────────────────────────────────────────
;;
;; Re-exported from `open-hax.publication-wire`, which the backend contracts are
;; also built from. Restating them here is what would let the two sides drift.

(def decode-id wire/decode-id)
(def encode-id wire/encode-id)
(def decode-revision wire/decode-revision)

;; ── Row decoders ───────────────────────────────────────────────────────────

(defn decode-document-wire
  [wire]
  (-> wire
      (update :id decode-id)
      (update :source-locale keyword)))

(defn decode-garden-wire
  [wire]
  (-> wire
      (update :id decode-id)
      (update :status keyword)))

(defn decode-publication-wire
  [wire]
  (-> wire
      (update :id decode-id)
      (update :document decode-id)
      (update :garden decode-id)
      (update :locale keyword)
      (update :revision decode-revision)
      (update :desired keyword)
      (update :observed #(when (some? %) %))
      (update :blockers #(mapv keyword (or % [])))))

(defn decode-cms-document-wire
  [wire]
  (-> wire
      (update :document decode-document-wire)
      (update :publications #(mapv decode-publication-wire %))))

(defn decode-cms-list-wire
  [wire]
  (-> wire
      (update :documents #(mapv decode-cms-document-wire %))
      (update :gardens #(mapv decode-garden-wire %))))

;; ── Requests ───────────────────────────────────────────────────────────────

(def list-path "/api/cms/publications/documents")

(defn intent-path
  [publication-id]
  (str "/api/cms/publications/intents/"
       (js/encodeURIComponent (encode-id publication-id))))

(defn ^:async load-cms!
  "Fetch and decode the whole publication topology."
  []
  (decode-cms-list-wire (await (api/request list-path))))

(defn ^:async set-publication-state!
  "Change only a publication's desired state.

   The body is built by `open-hax.publication-wire/state-patch-body`, the same
   namespace the backend's `PublicationStatePatchJson` derives its key and enum
   values from. The key is unqualified because `api/request` serializes with
   `clj->js`, which would drop a namespace."
  [publication-id state]
  (await (api/request (intent-path publication-id)
                      {:method "PATCH"
                       :body (wire/state-patch-body state)})))

(defn ^:async publish!
  [publication-id]
  (await (set-publication-state! publication-id :published)))

(defn ^:async unpublish!
  [publication-id]
  (await (set-publication-state! publication-id :withheld)))

(defn ^:async archive!
  [publication-id]
  (await (set-publication-state! publication-id :archived)))

;; ── Derived UI state ───────────────────────────────────────────────────────

(defn published-garden-ids
  "Gardens a document is published to, derived from decoded `:desired` state.

   Deliberately a derivation and not stored state: a second client-side
   authority is exactly what let the old CMS disagree with the resource graph."
  [cms-document]
  (->> (:publications cms-document)
       (filter #(= :published (:desired %)))
       (mapv :garden)))

(defn drifted?
  "True when desired publication is not reflected by observed evidence."
  [publication]
  (or (and (= :published (:desired publication)) (nil? (:observed publication)))
      (and (not= :published (:desired publication)) (some? (:observed publication)))))

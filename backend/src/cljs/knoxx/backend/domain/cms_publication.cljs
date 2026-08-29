(ns knoxx.backend.domain.cms-publication
  "Wire codecs for the CMS publication surface.

  Pure. Every keyword-valued field is encoded explicitly on the way out and
  decoded explicitly on the way in; nothing relies on `clj->js` to preserve a
  keyword, because it does not.

  Desired state and observed state are separate fields. Desired comes from the
  resource; observed comes from receipts and may legitimately disagree — that
  disagreement is drift, and the CMS has to be able to show it rather than
  pretend one of them is the truth."
  (:require [knoxx.backend.domain.publication-receipts :as receipts]
            [knoxx.backend.law.cms-publication :as law]
            [knoxx.backend.law.publication :as publication]
            [knoxx.backend.shape.resource-identity :as identity]
            [open-hax.publication-wire :as wire]))

(def encode-revision
  "Shared with the frontend so a selector token cannot be spelled two ways."
  wire/encode-revision)

(def decode-revision wire/decode-revision)

;; ── Encoders ───────────────────────────────────────────────────────────────

(defn document->wire
  [document]
  (publication/assert-valid!
   (:document/id document)
   law/DocumentWireJson
   {:id (identity/encode-keyword (:document/id document))
    :title (or (:document/title document) "")
    :source-locale (identity/encode-keyword (:document/source-locale document))
    ;; Explicitly one field, not the whole `:document/source` map.
    :source {:path (get-in document [:document/source :path])}}))

(defn garden->wire
  [garden]
  (publication/assert-valid!
   (:garden/id garden)
   law/GardenWireJson
   {:id (identity/encode-keyword (:garden/id garden))
    :title (or (:garden/title garden) "")
    :status (identity/encode-keyword (:garden/status garden))}))

(defn publication->wire
  "Desired state from the resource, observed state and blockers from runtime
   evidence, combined here rather than in the frontend."
  [{:keys [observed blockers]} intent]
  (publication/assert-valid!
   (:publication/id intent)
   law/PublicationWireJson
   {:id (identity/encode-keyword (:publication/id intent))
    :document (identity/encode-keyword (:publication/document intent))
    :garden (identity/encode-keyword (:publication/garden intent))
    :locale (identity/encode-keyword (:publication/locale intent))
    :revision (encode-revision (:publication/revision intent))
    :path (:publication/path intent)
    :desired (identity/encode-keyword (:publication/state intent))
    :observed (some-> observed :materialized/revision)
    :blockers (mapv identity/encode-keyword (or blockers []))}))

(defn- evidence-for
  [receipts-by-publication blockers-by-publication intent]
  {:observed (get receipts-by-publication (:publication/id intent))
   :blockers (get blockers-by-publication (:publication/id intent))})

(defn document-view->wire
  "One document plus its publications. `document-view` is the resolver's
   `{:document ... :publications [...]}`; it is encoded flat, not wrapped again."
  [evidence document-view]
  (publication/assert-valid!
   (get-in document-view [:document :document/id])
   law/CmsDocumentWireJson
   {:document (document->wire (:document document-view))
    :publications (mapv #(publication->wire (evidence-for (:receipts evidence)
                                                          (:blockers evidence)
                                                          %)
                                            %)
                        (:publications document-view))}))

(defn list-view->wire
  [evidence list-view]
  (publication/assert-valid!
   :cms/list-view
   law/CmsListWireJson
   {:documents (mapv #(document-view->wire evidence %) (:documents list-view))
    :gardens (mapv garden->wire (:gardens list-view))}))

(defn receipts->observed
  "Index receipts by publication id, keeping only successful materializations."
  [receipt-list]
  (reduce (fn [acc receipt]
            (if-let [publication-id (:publication/id receipt)]
              (assoc acc publication-id
                     (receipts/observed-materialization receipt))
              acc))
          {}
          receipt-list))

;; ── Decoders ───────────────────────────────────────────────────────────────

(defn decode-publication-state-patch
  "Decode the unqualified wire key onto the canonical qualified domain key.

   Both contracts are asserted: the wire shape first, so a body carrying
   `:publication/state` is rejected rather than silently accepted alongside
   `:state`, and the domain shape after, so the mapping itself is checked."
  [wire]
  (publication/assert-valid! :cms/state-patch-wire law/PublicationStatePatchJson wire)
  (publication/assert-valid! :cms/state-patch
                             law/PublicationStatePatch
                             {:publication/state
                              (wire/decode-state (get wire wire/state-patch-key))}))

(def identity-keys
  "Publication identity. Immutable for a state edit — a re-key is a separate,
   conflict-checked operation, never a side effect of publishing."
  [:publication/document :publication/garden :publication/locale :publication/revision])

(defn apply-state-patch
  "Return the intent with only its desired state changed.

   Identity is re-asserted from the current resource rather than taken from the
   patch, so even a patch that somehow carried identity fields could not move
   them."
  [current domain-patch]
  (publication/assert-valid! :cms/state-patch law/PublicationStatePatch domain-patch)
  (let [next-intent (merge (assoc current :publication/state
                                  (:publication/state domain-patch))
                           (select-keys current identity-keys))]
    (publication/assert-valid! (:publication/id current)
                               publication/PublicationIntentResource
                               next-intent)))

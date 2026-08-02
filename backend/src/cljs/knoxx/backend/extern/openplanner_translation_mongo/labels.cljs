(ns knoxx.backend.extern.openplanner-translation-mongo.labels
  "Tenant-scoped translation segment labelling."
  (:require [knoxx.backend.extern.openplanner-translation-mongo.common :as common]
            [knoxx.backend.law.openplanner-translation :as contract]
            [openplanner.translations.core :as translation]))

(defn- segment-label
  "Build the label document written for one reviewed segment."
  [segment-id org-id segment request {:keys [version corrected-text now]}]
  {:segment_id (str segment-id)
   :org_id org-id
   :project (common/jget segment "project")
   :labeler_id (or (common/nonblank (:labeler_id request)) "unknown")
   :labeler_email (or (common/nonblank (:labeler_email request)) "unknown")
   :label_version version
   :adequacy (:adequacy request)
   :fluency (:fluency request)
   :terminology (:terminology request)
   :risk (:risk request)
   :overall (:overall request)
   :corrected_text corrected-text
   :editor_notes (common/nonblank (:editor_notes request))
   :created_at now})

(defn- labelled-status
  "Wire status a segment moves to once this label is applied."
  [segment request corrected-text]
  (translation/status-wire
   (translation/next-segment-status
    {:current-status (common/jget segment "status")
     :overall (:overall request)
     :corrected-text corrected-text})))

(defn- ^:async apply-label-status!
  "Persist the labelled status, carrying any correction into the translation."
  [collection-map selector new-status corrected-text now]
  (await (.updateOne (:segments collection-map)
                     selector
                     #js {"$set"
                          (clj->js
                           (cond-> {:status new-status :updated_at now}
                             corrected-text (assoc :translated_text corrected-text)))})))

(defn- ^:async record-segment-label!
  "Reserve a label version, persist the label, and move the segment's status."
  [collection-map selector segment segment-id org-id request]
  (let [version (await (common/next-label-version!
                        (:segments collection-map)
                        (:labels collection-map)
                        selector
                        segment-id))
        now (js/Date.)
        corrected-text (common/nonblank (:corrected_text request))
        label (segment-label segment-id org-id segment request
                             {:version version
                              :corrected-text corrected-text
                              :now now})
        inserted (await (.insertOne (:labels collection-map) (clj->js label)))
        label-id (some-> inserted (common/jget "insertedId") .toString)
        new-status (labelled-status segment request corrected-text)]
    (await (apply-label-status! collection-map selector new-status
                                corrected-text now))
    {:ok true
     :label_id label-id
     :label (assoc (dissoc label :created_at) :id label-id :ts (common/iso now))
     :new_status new-status
     :graph_memory (when (= new-status "approved")
                     (await (common/upsert-graph-memory!
                             collection-map segment corrected-text)))}))

(defn ^:async label-segment!
  "Label one tenant-scoped segment; returns a top-level label_id and new status."
  [segment-id payload]
  (await (common/ensure-indexes!))
  (let [request (contract/assert-valid! :label-translation-segment/request
                                        contract/LabelTranslationSegmentRequest
                                        (or payload {}))
        org-id (common/required-org-id! (:org_id request))
        collection-map (common/collections (await (common/db!)))
        selector #js {"_id" (common/object-id! segment-id "segment")
                      "org_id" org-id}
        segment (await (.findOne (:segments collection-map) selector))]
    (when-not segment
      (throw (js/Error. "Segment not found")))
    (common/assert-response!
     :label-translation-segment/response
     contract/LabelTranslationSegmentResponse
     (await (record-segment-label! collection-map selector segment
                                   segment-id org-id request)))))

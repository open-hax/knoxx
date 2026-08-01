(ns knoxx.backend.extern.openplanner-translation-mongo.segments
  "Tenant-scoped translation segment, label, and SFT persistence."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.openplanner-translation-mongo.common :as common]
            [knoxx.backend.law.openplanner-translation :as contract]
            [openplanner.translations.core :as translation]))

(defn ^:async list-segments!
  "List tenant-scoped translation segments; returns {:segments :total :has_more}."
  [opts]
  (await (common/ensure-indexes!))
  (let [request (contract/assert-valid! :translation-segments/request
                                        contract/TranslationSegmentsRequest
                                        (or opts {}))
        org-id (common/required-org-id! (:org_id request))
        selector (assoc (common/filter-map request
                                           [:project :status :source_lang :target_lang :domain :document_id])
                        :org_id org-id)
        limit (common/normalized-query-number (:limit request) 50 1 100)
        offset (common/normalized-query-number (:offset request) 0 0 nil)
        {:keys [segments labels]} (common/collections (await (common/db!)))
        rows (await (.toArray (-> (.find segments (clj->js selector))
                                  (.sort #js {"created_at" 1})
                                  (.skip offset)
                                  (.limit limit))))
        ids (mapv common/string-id (array-seq rows))
        count-rows (if (seq ids)
                     (await (.toArray
                             (.aggregate labels
                                         (clj->js [{:$match {:segment_id {:$in ids}}}
                                                   {:$group {:_id "$segment_id"
                                                             :count {:$sum 1}}}]))))
                     #js [])
        count-by-id (into {}
                          (map (fn [row]
                                 [(common/jget row "_id")
                                  (common/jget row "count")])
                               (array-seq count-rows)))
        total (await (.countDocuments segments (clj->js selector)))
        response {:segments (mapv #(common/segment-view
                                    % [] (get count-by-id (common/string-id %) 0))
                                  (array-seq rows))
                  :total total
                  :has_more (< (+ offset (.-length rows)) total)}]
    (common/assert-response! :translation-segments/response
                             contract/TranslationSegmentsResponse
                             response)))

(defn ^:async segment!
  "Load one tenant-scoped segment; returns the segment with its labels."
  [segment-id opts]
  (await (common/ensure-indexes!))
  (let [scope (contract/assert-valid! :translation-segment/scope
                                      contract/TenantScopeRequest
                                      (or opts {}))
        org-id (common/required-org-id! (:org_id scope))
        {:keys [segments labels]} (common/collections (await (common/db!)))
        row (await (.findOne segments
                             #js {"_id" (common/object-id! segment-id "segment")
                                  "org_id" org-id}))]
    (when-not row
      (throw (js/Error. "Segment not found")))
    (let [label-rows (await (.toArray
                             (-> (.find labels #js {"segment_id" (str segment-id)})
                                 (.sort #js {"created_at" -1}))))
          response (common/segment-view row (vec (array-seq label-rows)) nil)]
      (common/assert-response! :translation-segment/response
                               contract/TranslationSegmentResponse
                               response))))

(defn- normalized-segment
  [input]
  (when-not (and (some? (:segment_index input))
                 (integer? (:segment_index input))
                 (not (neg? (:segment_index input))))
    (throw (js/Error. "segment_index must be an explicitly provided non-negative integer")))
  (let [normalized (translation/normalize-segment
                    {:source-text (:source_text input)
                     :translated-text (:translated_text input)
                     :source-lang (:source_lang input)
                     :target-lang (:target_lang input)
                     :document-id (:document_id input)
                     :segment-index (:segment_index input)
                     :status (:status input)
                     :mt-model (:mt_model input)
                     :confidence (:confidence input)
                     :domain (:domain input)
                     :content-type (:content_type input)
                     :url-context (:url_context input)
                     :garden-id (:garden_id input)
                     :org-id (common/required-org-id! (:org_id input))
                     :project (:project input)})
        errors (translation/segment-errors normalized)]
    (when (seq errors)
      (throw (ex-info "Invalid translation segment" {:errors errors})))
    normalized))

(defn ^:async create-segment!
  "Upsert one tenant-scoped segment; reports actual content modification."
  [input]
  (await (common/ensure-indexes!))
  (let [request (contract/assert-valid! :create-translation-segment/request
                                        contract/CreateTranslationSegmentRequest
                                        (or input {}))
        segment (normalized-segment request)
        now (js/Date.)
        segment-doc {:source_text (:source-text segment)
                     :translated_text (:translated-text segment)
                     :source_lang (:source-lang segment)
                     :target_lang (:target-lang segment)
                     :document_id (:document-id segment)
                     :segment_index (:segment-index segment)
                     :status (translation/status-wire (:status segment))
                     :mt_model (:mt-model segment)
                     :confidence (:confidence segment)
                     :domain (:domain segment)
                     :content_type (:content-type segment)
                     :url_context (:url-context segment)
                     :garden_id (:garden-id segment)
                     :org_id (:org-id segment)
                     :project (:project segment)}
        segment-identity {:document_id (:document-id segment)
                          :segment_index (:segment-index segment)
                          :target_lang (:target-lang segment)
                          :org_id (:org-id segment)}
        {:keys [segments]} (common/collections (await (common/db!)))
        existing (await (.findOne segments (clj->js segment-identity)))
        modified? (and existing
                       (not (common/segment-doc-matches? existing segment-doc)))
        should-write? (or (nil? existing) modified?)
        result (when should-write?
                 (await (.findOneAndUpdate
                         segments
                         (clj->js segment-identity)
                         #js {"$set" (clj->js (assoc segment-doc :updated_at now))
                              "$setOnInsert" #js {"created_at" now}}
                         #js {"upsert" true
                              "returnDocument" "after"
                              "includeResultMetadata" true})))
        row (or (common/jget result "value") existing)]
    (when-not row
      (throw (js/Error. "Failed to create translation segment")))
    (common/assert-response!
     :create-translation-segment/response
     contract/CreateTranslationSegmentResponse
     {:ok true
      :id (common/string-id row)
      :status (common/jget row "status")
      :upserted (boolean (some-> result
                                 (common/jget "lastErrorObject")
                                 (common/jget "upserted")))
      :modified (boolean modified?)})))

(defn ^:async create-segments-batch!
  "Import tenant-scoped segments; returns imported and error counts."
  [payload]
  (await (common/ensure-indexes!))
  (let [request (contract/assert-valid! :create-translation-segments-batch/request
                                        contract/CreateTranslationSegmentsBatchRequest
                                        (or payload {}))
        org-id (common/required-org-id! (:org_id request))
        rows (:segments request)]
    (when-not (seq rows)
      (throw (js/Error. "No segments provided")))
    (loop [row-index 0
           remaining rows
           results []
           errors []]
      (if-let [row (first remaining)]
        (let [attempt (try
                        {:created (await (create-segment!
                                          (assoc row
                                                 :segment_index (or (:segment_index row) row-index)
                                                 :org_id org-id
                                                 :project (or (:project request) (:project row)))))}
                        (catch :default err
                          {:error (or (.-message err) (str err))}))]
          (if-let [created (:created attempt)]
            (recur (inc row-index)
                   (next remaining)
                   (conj results {:index row-index
                                  :id (:id created)
                                  :status (:status created)})
                   errors)
            (recur (inc row-index)
                   (next remaining)
                   results
                   (conj errors {:index row-index :error (:error attempt)}))))
        (common/assert-response!
         :create-translation-segments-batch/response
         contract/CreateTranslationSegmentsBatchResponse
         (cond-> {:ok true
                  :imported (count results)
                  :errors (count errors)
                  :results results}
           (seq errors) (assoc :errors_detail errors)))))))

(defn ^:async label-segment!
  "Label one tenant-scoped segment; returns a top-level label_id and new status."
  [segment-id payload]
  (await (common/ensure-indexes!))
  (let [request (contract/assert-valid! :label-translation-segment/request
                                        contract/LabelTranslationSegmentRequest
                                        (or payload {}))
        org-id (common/required-org-id! (:org_id request))
        collection-map (common/collections (await (common/db!)))
        oid (common/object-id! segment-id "segment")
        selector #js {"_id" oid "org_id" org-id}
        segment (await (.findOne (:segments collection-map) selector))]
    (when-not segment
      (throw (js/Error. "Segment not found")))
    (let [version (await (common/next-label-version!
                          (:segments collection-map)
                          (:labels collection-map)
                          selector
                          segment-id))
          now (js/Date.)
          corrected-text (common/nonblank (:corrected_text request))
          label {:segment_id (str segment-id)
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
                 :created_at now}
          inserted (await (.insertOne (:labels collection-map) (clj->js label)))
          label-id (some-> inserted (common/jget "insertedId") .toString)
          new-status (translation/status-wire
                      (translation/next-segment-status
                       {:current-status (common/jget segment "status")
                        :overall (:overall request)
                        :corrected-text corrected-text}))]
      (await (.updateOne (:segments collection-map)
                         selector
                         #js {"$set"
                              (clj->js
                               (cond-> {:status new-status :updated_at now}
                                 corrected-text (assoc :translated_text corrected-text)))}))
      (let [graph-memory (when (= new-status "approved")
                           (await (common/upsert-graph-memory!
                                   collection-map segment corrected-text)))
            response {:ok true
                      :label_id label-id
                      :label (assoc (dissoc label :created_at)
                                    :id label-id
                                    :ts (common/iso now))
                      :new_status new-status
                      :graph_memory graph-memory}]
        (common/assert-response! :label-translation-segment/response
                                 contract/LabelTranslationSegmentResponse
                                 response)))))

(defn- ^:async latest-corrections
  [labels segment-ids]
  (if-not (seq segment-ids)
    {}
    (let [rows (await (.toArray
                       (.aggregate labels
                                   (clj->js [{:$match {:segment_id {:$in segment-ids}
                                                       :corrected_text {:$exists true
                                                                        :$nin [nil ""]}}}
                                             {:$sort {:created_at -1}}
                                             {:$group {:_id "$segment_id"
                                                       :corrected_text {:$first "$corrected_text"}}}]))))]
      (into {}
            (map (fn [row]
                   [(common/jget row "_id")
                    (common/jget row "corrected_text")])
                 (array-seq rows))))))

(defn ^:async export-sft!
  "Export approved tenant segments as a bounded newline-delimited prompt/target payload."
  [opts]
  (await (common/ensure-indexes!))
  (let [request (contract/assert-valid! :translation-export-sft/request
                                        contract/TranslationSftRequest
                                        (or opts {}))
        org-id (common/required-org-id! (:org_id request))
        selector (assoc (common/filter-map request [:project :target_lang])
                        :status "approved"
                        :org_id org-id)
        limit (common/normalized-query-number (:limit request) 5000 1 50000)
        {:keys [segments labels]} (common/collections (await (common/db!)))
        rows (vec (array-seq
                   (await (.toArray
                           (-> (.find segments (clj->js selector))
                               (.sort #js {"_id" 1})
                               (.limit limit))))))
        include-corrected? (not (contains? #{false "false"}
                                           (:include_corrected request)))
        corrections (if include-corrected?
                      (await (latest-corrections labels
                                                 (mapv common/string-id rows)))
                      {})
        result (->> rows
                    (map (fn [row]
                           (translation/sft-row
                            {:source-lang (common/jget row "source_lang")
                             :target-lang (common/jget row "target_lang")
                             :source-text (common/jget row "source_text")
                             :translated-text (common/jget row "translated_text")
                             :corrected-text (get corrections
                                                  (common/string-id row))})))
                    (map #(js/JSON.stringify (clj->js %)))
                    (str/join "\n"))]
    (common/assert-response! :translation-export-sft/response
                             contract/TranslationSftResponse
                             result)))
(ns knoxx.backend.extern.openplanner-translation-mongo.documents
  "Tenant-scoped translation manifests, documents, and document reviews."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.openplanner-translation-mongo.common :as common]
            [knoxx.backend.law.openplanner-translation :as contract]
            [openplanner.translations.core :as translation]))

(defn ^:async manifest!
  "Build a tenant-scoped translation manifest with indexed label filtering."
  [opts]
  (await (common/ensure-indexes!))
  (let [request (contract/assert-valid! :translation-export-manifest/request
                                        contract/TranslationManifestRequest
                                        (or opts {}))
        org-id (common/required-org-id! (:org_id request))
        segment-selector (assoc (common/filter-map request [:project])
                                :org_id org-id)
        label-selector (cond-> {:org_id org-id}
                         (:project request) (assoc :project (:project request)))
        {:keys [segments labels]} (common/collections (await (common/db!)))
        language-rows (await
                       (.toArray
                        (.aggregate
                         segments
                         (clj->js [{:$match segment-selector}
                                   {:$group {:_id "$target_lang"
                                             :total {:$sum 1}
                                             :approved {:$sum {:$cond [{:$eq ["$status" "approved"]} 1 0]}}
                                             :rejected {:$sum {:$cond [{:$eq ["$status" "rejected"]} 1 0]}}
                                             :pending {:$sum {:$cond [{:$eq ["$status" "pending"]} 1 0]}}
                                             :in_review {:$sum {:$cond [{:$eq ["$status" "in_review"]} 1 0]}}}}]))))
        label-rows (await
                    (.toArray
                     (.aggregate
                      labels
                      (clj->js [{:$match label-selector}
                                {:$addFields {:segment_object_id
                                              {:$convert {:input "$segment_id"
                                                          :to "objectId"
                                                          :onError nil
                                                          :onNull nil}}}}
                                {:$lookup {:from common/segment-collection-name
                                           :localField "segment_object_id"
                                           :foreignField "_id"
                                           :as "segment"}}
                                {:$unwind "$segment"}
                                {:$facet
                                 {:corrections
                                  [{:$match {:corrected_text {:$exists true
                                                             :$nin [nil ""]}}}
                                   {:$group {:_id {:target_lang "$segment.target_lang"
                                                   :segment_id "$segment_id"}}}
                                   {:$group {:_id "$_id.target_lang"
                                             :count {:$sum 1}}}]
                                  :labelers
                                  [{:$group {:_id "$labeler_email"
                                             :segments_labeled {:$sum 1}}}]
                                  :label_counts
                                  [{:$group {:_id "$segment.target_lang"
                                             :count {:$sum 1}}}]}}]))))
        stats (first (array-seq label-rows))
        corrections (into {}
                          (map (fn [row]
                                 [(common/jget row "_id")
                                  (common/jget row "count")])
                               (array-seq (or (common/jget stats "corrections") #js []))))
        label-counts (into {}
                           (map (fn [row]
                                  [(common/jget row "_id")
                                   (common/jget row "count")])
                                (array-seq (or (common/jget stats "label_counts") #js []))))
        languages (mapv (fn [row]
                          {:target-lang (common/jget row "_id")
                           :total (common/jget row "total")
                           :approved (common/jget row "approved")
                           :rejected (common/jget row "rejected")
                           :pending (common/jget row "pending")
                           :in-review (common/jget row "in_review")})
                        (array-seq language-rows))
        shaped (translation/manifest-shape
                {:project (:project request)
                 :languages languages
                 :corrections-by-language corrections
                 :labelers (mapv (fn [row]
                                   {:email (common/jget row "_id")
                                    :segments-labeled (common/jget row "segments_labeled")})
                                 (array-seq (or (common/jget stats "labelers") #js [])))})
        shaped (update shaped :languages
                       (fn [by-language]
                         (into {}
                               (map (fn [[language data]]
                                      [language
                                       (assoc data
                                              :avg_labels_per_segment
                                              (if (pos? (:total_segments data))
                                                (/ (get label-counts language 0)
                                                   (:total_segments data))
                                                0))])
                                    by-language))))
        response (assoc shaped :generated_at (.toISOString (js/Date.)))]
    (common/assert-response! :translation-export-manifest/response
                             contract/TranslationManifestResponse
                             response)))

(defn ^:async documents!
  "List tenant-scoped translated documents with aggregate status counts."
  [opts]
  (await (common/ensure-indexes!))
  (let [request (contract/assert-valid! :translation-documents/request
                                        contract/TranslationDocumentsRequest
                                        (or opts {}))
        org-id (common/required-org-id! (:org_id request))
        selector (assoc (common/filter-map request
                                           [:project :target_lang :source_lang :garden_id])
                        :org_id org-id)
        {:keys [segments events]} (common/collections (await (common/db!)))
        rows (await
              (.toArray
               (.aggregate
                segments
                (clj->js [{:$match selector}
                          {:$group {:_id {:document_id "$document_id"
                                         :target_lang "$target_lang"}
                                    :source_lang {:$first "$source_lang"}
                                    :garden_id {:$first "$garden_id"}
                                    :project {:$first "$project"}
                                    :total_segments {:$sum 1}
                                    :approved {:$sum {:$cond [{:$eq ["$status" "approved"]} 1 0]}}
                                    :pending {:$sum {:$cond [{:$eq ["$status" "pending"]} 1 0]}}
                                    :rejected {:$sum {:$cond [{:$eq ["$status" "rejected"]} 1 0]}}
                                    :in_review {:$sum {:$cond [{:$eq ["$status" "in_review"]} 1 0]}}}}
                          {:$sort {"_id.document_id" 1
                                   "_id.target_lang" 1}}]))))
        document-ids (mapv #(common/jget (common/jget % "_id") "document_id")
                           (array-seq rows))
        event-rows (if (seq document-ids)
                     (await
                      (.toArray
                       (.find events
                              (clj->js
                               (common/event-selector {:$in document-ids} org-id)))))
                     #js [])
        titles (into {}
                     (map (fn [row]
                            (let [extra (common/jget row "extra")]
                              [(common/jget row "_id")
                               {:title (or (common/nonblank (common/jget extra "title"))
                                           "Untitled")
                                :visibility (or (common/nonblank
                                                 (common/jget extra "visibility"))
                                                "internal")}]))
                          (array-seq event-rows)))
        shaped (mapv
                (fn [row]
                  (let [id-object (common/jget row "_id")
                        document-id (common/jget id-object "document_id")
                        event-meta (get titles document-id)]
                    (translation/document-list-row
                     {:document-id document-id
                      :target-lang (common/jget id-object "target_lang")
                      :source-lang (common/jget row "source_lang")
                      :garden-id (common/jget row "garden_id")
                      :project (common/jget row "project")
                      :total (common/jget row "total_segments")
                      :approved (common/jget row "approved")
                      :pending (common/jget row "pending")
                      :rejected (common/jget row "rejected")
                      :in-review (common/jget row "in_review")
                      :title (:title event-meta)
                      :visibility (:visibility event-meta)})))
                (array-seq rows))
        response {:documents shaped :total (count shaped)}]
    (common/assert-response! :translation-documents/response
                             contract/TranslationDocumentsResponse
                             response)))

(defn ^:async document!
  "Load a tenant document, using segment text as source metadata for manual imports."
  [document-id target-lang opts]
  (await (common/ensure-indexes!))
  (let [scope (contract/assert-valid! :translation-document/scope
                                      contract/TenantScopeRequest
                                      (or opts {}))
        org-id (common/required-org-id! (:org_id scope))
        collection-map (common/collections (await (common/db!)))
        rows (await
              (.toArray
               (.aggregate
                (:segments collection-map)
                (clj->js [{:$match {:document_id (str document-id)
                                    :target_lang (str target-lang)
                                    :org_id org-id}}
                          {:$sort {:segment_index 1 :created_at -1}}
                          {:$group {:_id "$segment_index"
                                    :doc {:$first "$$ROOT"}}}
                          {:$replaceRoot {:newRoot "$doc"}}
                          {:$sort {:segment_index 1}}]))))]
    (when (zero? (.-length rows))
      (throw (js/Error. "Document translation not found")))
    (let [event (await
                 (.findOne
                  (:events collection-map)
                  (clj->js (common/event-selector (str document-id) org-id))))
          segments (vec (array-seq rows))
          ids (mapv common/string-id segments)
          label-rows (if (seq ids)
                       (await
                        (.toArray
                         (-> (.find (:labels collection-map)
                                    (clj->js {:segment_id {:$in ids}}))
                             (.sort #js {"created_at" -1}))))
                       #js [])
          labels-by-id (group-by #(common/jget % "segment_id")
                                 (array-seq label-rows))
          extra (when event (common/jget event "extra"))
          first-segment (first segments)
          fallback-content (str/join "\n"
                                     (keep #(common/nonblank
                                             (common/jget % "source_text"))
                                           segments))
          shaped-segments (mapv #(common/segment-view
                                  % (get labels-by-id (common/string-id %) []) nil)
                                segments)
          summary (translation/summarize-segments shaped-segments)
          response {:document
                    {:id (str document-id)
                     :title (or (common/nonblank (common/jget extra "title"))
                                "Untitled")
                     :content (or (common/jget extra "content")
                                  (common/jget extra "text")
                                  fallback-content
                                  "")
                     :source_lang (or (common/nonblank
                                       (common/jget extra "language"))
                                      (common/nonblank
                                       (common/jget first-segment "source_lang"))
                                      "en")
                     :visibility (or (common/nonblank
                                      (common/jget extra "visibility"))
                                     "internal")
                     :source_path (or (common/jget extra "sourcePath")
                                      (common/jget extra "source_path"))}
                    :segments shaped-segments
                    :summary {:total_segments (:total-segments summary)
                              :approved (:approved summary)
                              :pending (:pending summary)
                              :rejected (:rejected summary)
                              :in_review (:in-review summary)
                              :overall_status (translation/status-wire
                                               (:overall-status summary))}}]
      (common/assert-response! :translation-document/response
                               contract/TranslationDocumentResponse
                               response))))

(defn- override-for
  [overrides segment]
  (let [index-key (str (common/jget segment "segment_index"))
        segment-key (common/string-id segment)]
    (or (get overrides index-key)
        (get overrides (keyword index-key))
        (get overrides segment-key)
        (get overrides (keyword segment-key))
        {})))

(defn- ^:async review-one!
  [collection-map org-id request overrides segment]
  (try
    (let [segment-id (common/string-id segment)
          selector #js {"_id" (common/jget segment "_id")
                         "org_id" org-id}
          version (await (common/next-label-version!
                          (:segments collection-map)
                          (:labels collection-map)
                          selector
                          segment-id))
          override (override-for overrides segment)
          plan (translation/document-review-label-plan
                {:segment-id segment-id
                 :labeler-id (:labeler_id request)
                 :labeler-email (:labeler_email request)
                 :overall (or (:overall override) (:overall request))
                 :corrected-text (:corrected_text override)
                 :editor-notes (or (:editor_notes override)
                                   (:editor_notes request))})
          now (js/Date.)
          label (assoc (dissoc plan :next_status)
                       :org_id org-id
                       :project (common/jget segment "project")
                       :label_version version
                       :created_at now)
          inserted (await (.insertOne (:labels collection-map) (clj->js label)))
          inserted-id (common/jget inserted "insertedId")]
      (try
        (let [next-status (:next_status plan)
              update-result (await
                             (.updateOne
                              (:segments collection-map)
                              selector
                              #js {"$set"
                                   (clj->js
                                    (cond-> {:status next-status
                                             :updated_at now}
                                      (:corrected_text label)
                                      (assoc :translated_text
                                             (:corrected_text label))))}))]
          (when (zero? (or (common/jget update-result "matchedCount") 0))
            (throw (js/Error. "Segment disappeared during document review")))
          (let [graph-result (if (= next-status "approved")
                               (await (common/upsert-graph-memory!
                                       collection-map
                                       segment
                                       (:corrected_text label)))
                               {:success true})]
            {:success true
             :graph-success (boolean (:success graph-result))
             :graph-error (:error graph-result)}))
        (catch :default err
          ;; A label without its corresponding segment status is misleading.
          ;; Remove it when the segment write fails; the reserved version may
          ;; have a harmless gap but can never be duplicated.
          (when inserted-id
            (await (.deleteOne (:labels collection-map) #js {"_id" inserted-id})))
          {:success false :error (or (.-message err) (str err))})))
    (catch :default err
      {:success false :error (or (.-message err) (str err))})))

(defn- ^:async review-in-chunks!
  [collection-map org-id request overrides segments]
  (loop [remaining segments
         results []]
    (if (seq remaining)
      (let [chunk (vec (take 20 remaining))
            chunk-results (await
                           (js/Promise.all
                            (clj->js
                             (mapv #(review-one!
                                     collection-map org-id request overrides %)
                                   chunk))))]
        (recur (drop 20 remaining)
               (into results
                     (map #(if (map? %)
                             %
                             (js->clj % :keywordize-keys true))
                          (array-seq chunk-results)))))
      results)))

(defn ^:async review-document!
  "Review a tenant document in bounded chunks with exact success/failure counts."
  [document-id target-lang payload]
  (await (common/ensure-indexes!))
  (let [request (contract/assert-valid! :review-translation-document/request
                                        contract/ReviewTranslationDocumentRequest
                                        (or payload {}))
        org-id (common/required-org-id! (:org_id request))
        collection-map (common/collections (await (common/db!)))
        selector {:document_id (str document-id)
                  :target_lang (str target-lang)
                  :org_id org-id}
        segment-array (await
                       (.toArray
                        (-> (.find (:segments collection-map) (clj->js selector))
                            (.sort #js {"segment_index" 1}))))
        segments (vec (array-seq segment-array))]
    (when-not (seq segments)
      (throw (js/Error. "No segments found for this document+language pair")))
    (let [overrides (or (:segment_overrides request) {})
          results (await (review-in-chunks!
                          collection-map org-id request overrides segments))
          successful (filter :success results)
          reviewed-count (count successful)
          graph-failures (count (remove :graph-success successful))
          response {:ok true
                    :document_id (str document-id)
                    :target_lang (str target-lang)
                    :segments_reviewed reviewed-count
                    :segments_failed (- (count segments) reviewed-count)
                    :overall (:overall request)
                    :overrides_applied (count overrides)
                    :graph_memory_failures graph-failures}]
      (common/assert-response! :review-translation-document/response
                               contract/ReviewTranslationDocumentResponse
                               response))))
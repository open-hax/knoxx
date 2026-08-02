(ns knoxx.backend.extern.openplanner-translation-mongo.documents
  "Tenant-scoped translation manifests, documents, and document reviews."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.openplanner-translation-mongo.common :as common]
            [knoxx.backend.law.openplanner-translation :as contract]
            [openplanner.translations.core :as translation]))

(defn- status-count
  "Aggregation expression counting segments whose status equals `status`."
  [status]
  {:$sum {:$cond [{:$eq ["$status" status]} 1 0]}})

(defn- language-stats-pipeline
  "Per-language segment status totals for the matched tenant scope."
  [segment-selector]
  [{:$match segment-selector}
   {:$group {:_id "$target_lang"
             :total {:$sum 1}
             :approved (status-count "approved")
             :rejected (status-count "rejected")
             :pending (status-count "pending")
             :in_review (status-count "in_review")}}])

(defn- label-stats-pipeline
  "Correction, labeler, and label-count facets joined back to their segments."
  [label-selector]
  [{:$match label-selector}
   {:$addFields {:segment_object_id {:$convert {:input "$segment_id"
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
     [{:$match {:corrected_text {:$exists true :$nin [nil ""]}}}
      {:$group {:_id {:target_lang "$segment.target_lang"
                      :segment_id "$segment_id"}}}
      {:$group {:_id "$_id.target_lang" :count {:$sum 1}}}]
     :labelers
     [{:$group {:_id "$labeler_email" :segments_labeled {:$sum 1}}}]
     :label_counts
     [{:$group {:_id "$segment.target_lang" :count {:$sum 1}}}]}}])

(defn- facet-rows
  [stats facet]
  (array-seq (or (common/jget stats facet) #js [])))

(defn- count-by-id
  "Index `{_id count}` facet rows by their grouping key."
  [rows]
  (into {}
        (map (fn [row] [(common/jget row "_id") (common/jget row "count")]))
        rows))

(defn- language-summary
  [row]
  {:target-lang (common/jget row "_id")
   :total (common/jget row "total")
   :approved (common/jget row "approved")
   :rejected (common/jget row "rejected")
   :pending (common/jget row "pending")
   :in-review (common/jget row "in_review")})

(defn- labeler-summary
  [row]
  {:email (common/jget row "_id")
   :segments-labeled (common/jget row "segments_labeled")})

(defn- with-average-labels
  "Annotate each shaped language with its mean label count per segment."
  [shaped label-counts]
  (update shaped :languages
          (fn [by-language]
            (into {}
                  (map (fn [[language data]]
                         [language
                          (assoc data :avg_labels_per_segment
                                 (if (pos? (:total_segments data))
                                   (/ (get label-counts language 0)
                                      (:total_segments data))
                                   0))]))
                  by-language))))

(defn- manifest-response
  "Shape the manifest wire response from the language and label aggregations."
  [request language-rows stats]
  (-> (translation/manifest-shape
       {:project (:project request)
        :languages (mapv language-summary language-rows)
        :corrections-by-language (count-by-id (facet-rows stats "corrections"))
        :labelers (mapv labeler-summary (facet-rows stats "labelers"))})
      (with-average-labels (count-by-id (facet-rows stats "label_counts")))
      (assoc :generated_at (.toISOString (js/Date.)))))

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
                        (.aggregate segments
                                    (clj->js (language-stats-pipeline segment-selector)))))
        label-rows (await
                    (.toArray
                     (.aggregate labels
                                 (clj->js (label-stats-pipeline label-selector)))))
        response (manifest-response request
                                    (array-seq language-rows)
                                    (first (array-seq label-rows)))]
    (common/assert-response! :translation-export-manifest/response
                             contract/TranslationManifestResponse
                             response)))

(defn- document-stats-pipeline
  "Per document+language status totals for the matched tenant scope."
  [selector]
  [{:$match selector}
   {:$group {:_id {:document_id "$document_id" :target_lang "$target_lang"}
             :source_lang {:$first "$source_lang"}
             :garden_id {:$first "$garden_id"}
             :project {:$first "$project"}
             :total_segments {:$sum 1}
             :approved (status-count "approved")
             :pending (status-count "pending")
             :rejected (status-count "rejected")
             :in_review (status-count "in_review")}}
   {:$sort {"_id.document_id" 1 "_id.target_lang" 1}}])

(defn- event-metadata-index
  "Index source-event titles and visibility by document id."
  [event-rows]
  (into {}
        (map (fn [row]
               (let [extra (common/jget row "extra")]
                 [(common/jget row "_id")
                  {:title (or (common/nonblank (common/jget extra "title")) "Untitled")
                   :visibility (or (common/nonblank (common/jget extra "visibility"))
                                   "internal")}])))
        (array-seq event-rows)))

(defn- document-list-row
  [titles row]
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

(defn- ^:async document-events!
  "Load the source events backing a set of document ids, if any."
  [events document-ids org-id]
  (if (seq document-ids)
    (await
     (.toArray
      (.find events (clj->js (common/event-selector {:$in document-ids} org-id)))))
    #js []))

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
               (.aggregate segments (clj->js (document-stats-pipeline selector)))))
        document-ids (mapv #(common/jget (common/jget % "_id") "document_id")
                           (array-seq rows))
        titles (event-metadata-index
                (await (document-events! events document-ids org-id)))
        shaped (mapv (partial document-list-row titles) (array-seq rows))
        response {:documents shaped :total (count shaped)}]
    (common/assert-response! :translation-documents/response
                             contract/TranslationDocumentsResponse
                             response)))

(defn- document-segments-pipeline
  "Latest revision of each segment index for one document+language pair."
  [document-id target-lang org-id]
  [{:$match {:document_id (str document-id)
             :target_lang (str target-lang)
             :org_id org-id}}
   {:$sort {:segment_index 1 :created_at -1}}
   {:$group {:_id "$segment_index" :doc {:$first "$$ROOT"}}}
   {:$replaceRoot {:newRoot "$doc"}}
   {:$sort {:segment_index 1}}])

(defn- ^:async labels-by-segment-id
  "Group each segment's labels, newest first, keyed by segment id."
  [labels ids]
  (if (seq ids)
    (group-by #(common/jget % "segment_id")
              (array-seq
               (await
                (.toArray
                 (-> (.find labels (clj->js {:segment_id {:$in ids}}))
                     (.sort #js {"created_at" -1}))))))
    {}))

(defn- document-metadata
  "Shape document metadata, falling back to segment text for manual imports.

  Manually imported segments have no source event, so `extra` is nil and the
  joined source text stands in for the document body."
  [document-id extra segments]
  {:id (str document-id)
   :title (or (common/nonblank (common/jget extra "title")) "Untitled")
   :content (or (common/jget extra "content")
                (common/jget extra "text")
                (str/join "\n"
                          (keep #(common/nonblank (common/jget % "source_text"))
                                segments))
                "")
   :source_lang (or (common/nonblank (common/jget extra "language"))
                    (common/nonblank (common/jget (first segments) "source_lang"))
                    "en")
   :visibility (or (common/nonblank (common/jget extra "visibility")) "internal")
   :source_path (or (common/jget extra "sourcePath")
                    (common/jget extra "source_path"))})

(defn- document-summary
  [shaped-segments]
  (let [summary (translation/summarize-segments shaped-segments)]
    {:total_segments (:total-segments summary)
     :approved (:approved summary)
     :pending (:pending summary)
     :rejected (:rejected summary)
     :in_review (:in-review summary)
     :overall_status (translation/status-wire (:overall-status summary))}))

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
                (clj->js (document-segments-pipeline document-id target-lang org-id)))))]
    (when (zero? (.-length rows))
      (throw (js/Error. "Document translation not found")))
    (let [event (await
                 (.findOne
                  (:events collection-map)
                  (clj->js (common/event-selector (str document-id) org-id))))
          segments (vec (array-seq rows))
          labels-by-id (await (labels-by-segment-id (:labels collection-map)
                                                    (mapv common/string-id segments)))
          shaped-segments (mapv #(common/segment-view
                                  % (get labels-by-id (common/string-id %) []) nil)
                                segments)
          response {:document (document-metadata
                               document-id
                               (when event (common/jget event "extra"))
                               segments)
                    :segments shaped-segments
                    :summary (document-summary shaped-segments)}]
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

(defn- review-label-plan
  "Merge the document-wide review request with this segment's override."
  [request overrides segment segment-id]
  (let [override (override-for overrides segment)]
    (translation/document-review-label-plan
     {:segment-id segment-id
      :labeler-id (:labeler_id request)
      :labeler-email (:labeler_email request)
      :overall (or (:overall override) (:overall request))
      :corrected-text (:corrected_text override)
      :editor-notes (or (:editor_notes override) (:editor_notes request))})))

(defn- review-label
  "Build the label document persisted for one reviewed segment."
  [plan org-id segment version now]
  (assoc (dissoc plan :next_status)
         :org_id org-id
         :project (common/jget segment "project")
         :label_version version
         :created_at now))

(defn- ^:async apply-review-status!
  "Move the segment to its reviewed status and refresh graph memory."
  [collection-map segment selector label next-status now]
  (let [update-result (await
                       (.updateOne
                        (:segments collection-map)
                        selector
                        #js {"$set"
                             (clj->js
                              (cond-> {:status next-status :updated_at now}
                                (:corrected_text label)
                                (assoc :translated_text (:corrected_text label))))}))]
    (when (zero? (or (common/jget update-result "matchedCount") 0))
      (throw (js/Error. "Segment disappeared during document review")))
    (let [graph-result (if (= next-status "approved")
                         (await (common/upsert-graph-memory!
                                 collection-map segment (:corrected_text label)))
                         {:success true})]
      {:success true
       :graph-success (boolean (:success graph-result))
       :graph-error (:error graph-result)})))

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
          plan (review-label-plan request overrides segment segment-id)
          now (js/Date.)
          label (review-label plan org-id segment version now)
          inserted (await (.insertOne (:labels collection-map) (clj->js label)))
          inserted-id (common/jget inserted "insertedId")]
      (try
        (await (apply-review-status! collection-map segment selector label
                                     (:next_status plan) now))
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
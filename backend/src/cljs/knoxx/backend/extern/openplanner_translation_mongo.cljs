(ns knoxx.backend.extern.openplanner-translation-mongo
  "Direct MongoDB boundary for OpenPlanner translation projections.

   This namespace owns MongoDB JavaScript interop. Public functions accept and
   return CLJS data and reuse OpenPlanner's existing ClojureScript translation
   domain rules instead of reimplementing them in TypeScript."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.mongo-client :as mongo-client]
            [knoxx.backend.law.openplanner-translation :as contract]
            [openplanner.translations.core :as translation]
            ["mongodb" :refer [ObjectId]]))

(def ^:private segment-collection-name "translation_segments")
(def ^:private label-collection-name "translation_labels")
(def ^:private batch-collection-name "translation_batches")
(def ^:private event-collection-name "events")
(def ^:private graph-node-collection-name "graph_nodes")
(def ^:private graph-edge-collection-name "graph_edges")

(defonce ^:private index-promise* (atom nil))

(defn- jget
  [obj key]
  (when obj (aget obj key)))

(defn- string-id
  [row]
  (some-> (or (jget row "_id") (jget row "id")) .toString))

(defn- iso
  [value]
  (cond
    (nil? value) nil
    (instance? js/Date value) (.toISOString value)
    (fn? (jget value "toISOString")) (.toISOString value)
    :else (str value)))

(defn- nonblank
  [value]
  (translation/nonblank-string value))

(defn- required-org-id!
  [value]
  (or (nonblank value)
      (throw (js/Error. "org_id is required for direct translation storage"))))

(defn- object-id!
  [value label]
  (try
    (ObjectId. (str value))
    (catch :default _
      (throw (js/Error. (str "Invalid " label " ID"))))))

(defn- collections
  [db]
  {:segments (.collection db segment-collection-name)
   :labels (.collection db label-collection-name)
   :batches (.collection db batch-collection-name)
   :events (.collection db event-collection-name)
   :graph-nodes (.collection db graph-node-collection-name)
   :graph-edges (.collection db graph-edge-collection-name)})

(defn- ^:async db!
  []
  (or (mongo-client/get-db)
      (await (mongo-client/init-mongo!))
      (throw (js/Error. "MongoDB is unavailable for direct translation storage"))))

(defn- ^:async create-indexes!
  [db]
  (let [{:keys [segments labels batches]} (collections db)]
    ;; Keep the established OpenPlanner index definition. Tenant scope is
    ;; enforced in every selector without changing the shared index contract.
    (await (.createIndex segments
                         #js {"document_id" 1 "segment_index" 1 "target_lang" 1}
                         #js {"unique" true "name" "segment_unique_idx"}))
    (await (js/Promise.all
            #js [(.createIndex segments #js {"status" 1})
                 (.createIndex segments #js {"target_lang" 1})
                 (.createIndex segments #js {"garden_id" 1})
                 (.createIndex segments #js {"org_id" 1})
                 (.createIndex segments #js {"project" 1})
                 (.createIndex labels #js {"segment_id" 1 "created_at" -1})
                 (.createIndex batches #js {"garden_id" 1 "target_lang" 1 "status" 1})
                 (.createIndex batches #js {"org_id" 1 "status" 1 "created_at" 1})]))
    true))

(defn ^:async ensure-indexes!
  []
  (if-let [pending @index-promise*]
    (await pending)
    (let [pending (create-indexes! (await (db!)))]
      (reset! index-promise* pending)
      (try
        (await pending)
        (catch :default err
          (reset! index-promise* nil)
          (throw err))))))

(defn- filter-map
  [source keys]
  (reduce (fn [result key]
            (if-let [value (get source key)]
              (assoc result key value)
              result))
          {}
          keys))

(defn- segment-view
  ([row] (segment-view row [] nil))
  ([row labels label-count]
   (cond->
    {:id (string-id row)
     :source_text (jget row "source_text")
     :translated_text (jget row "translated_text")
     :source_lang (jget row "source_lang")
     :target_lang (jget row "target_lang")
     :document_id (jget row "document_id")
     :segment_index (jget row "segment_index")
     :status (jget row "status")
     :confidence (jget row "confidence")
     :mt_model (jget row "mt_model")
     :domain (jget row "domain")
     :garden_id (jget row "garden_id")
     :tenant_id (jget row "org_id")
     :org_id (jget row "org_id")
     :project (jget row "project")
     :labels (mapv (fn [label]
                     {:id (string-id label)
                      :segment_id (jget label "segment_id")
                      :labeler_id (jget label "labeler_id")
                      :labeler_email (jget label "labeler_email")
                      :adequacy (jget label "adequacy")
                      :fluency (jget label "fluency")
                      :terminology (jget label "terminology")
                      :risk (jget label "risk")
                      :overall (jget label "overall")
                      :corrected_text (jget label "corrected_text")
                      :editor_notes (jget label "editor_notes")
                      :ts (iso (jget label "created_at"))})
                   labels)
     :ts (iso (jget row "created_at"))}
     (some? label-count) (assoc :label_count label-count))))

(defn- batch-view
  [row]
  (when row
    {:id (string-id row)
     :batch_id (jget row "batch_id")
     :garden_id (jget row "garden_id")
     :target_lang (jget row "target_lang")
     :source_lang (jget row "source_lang")
     :project (jget row "project")
     :org_id (jget row "org_id")
     :status (jget row "status")
     :document_ids (vec (or (some-> (jget row "document_ids") array-seq) []))
     :completed_documents (vec (or (some-> (jget row "completed_documents") array-seq) []))
     :failed_documents (mapv #(js->clj % :keywordize-keys true)
                             (or (some-> (jget row "failed_documents") array-seq) []))
     :attempts (or (jget row "attempts") 0)
     :created_at (iso (jget row "created_at"))
     :updated_at (iso (jget row "updated_at"))
     :started_at (iso (jget row "started_at"))
     :completed_at (iso (jget row "completed_at"))
     :agent_session_id (jget row "agent_session_id")
     :agent_conversation_id (jget row "agent_conversation_id")
     :agent_run_id (jget row "agent_run_id")
     :error (jget row "error")}))

(defn- assert-response!
  [id schema value]
  (contract/assert-valid! id schema value))

(defn ^:async list-segments!
  [opts]
  (await (ensure-indexes!))
  (let [request (contract/assert-valid! :translation-segments/request
                                        contract/TranslationSegmentsRequest
                                        (or opts {}))
        org-id (required-org-id! (:org_id request))
        filter (assoc (filter-map request [:project :status :source_lang :target_lang :domain :document_id])
                      :org_id org-id)
        limit (-> (or (:limit request) 50) js/Number (max 1) (min 100))
        offset (-> (or (:offset request) 0) js/Number (max 0))
        {:keys [segments labels]} (collections (await (db!)))
        rows (await (.toArray (-> (.find segments (clj->js filter))
                                  (.sort #js {"created_at" 1})
                                  (.skip offset)
                                  (.limit limit))))
        ids (mapv string-id (array-seq rows))
        count-rows (if (seq ids)
                     (await (.toArray
                             (.aggregate labels
                                         (clj->js [{:$match {:segment_id {:$in ids}}}
                                                   {:$group {:_id "$segment_id" :count {:$sum 1}}}]))))
                     #js [])
        count-by-id (into {} (map (fn [row] [(jget row "_id") (jget row "count")])
                                  (array-seq count-rows)))
        total (await (.countDocuments segments (clj->js filter)))
        response {:segments (mapv #(segment-view % [] (get count-by-id (string-id %) 0))
                                  (array-seq rows))
                  :total total
                  :has_more (< (+ offset (.-length rows)) total)}]
    (assert-response! :translation-segments/response contract/TranslationSegmentsResponse response)))

(defn ^:async segment!
  [segment-id opts]
  (await (ensure-indexes!))
  (let [scope (contract/assert-valid! :translation-segment/scope
                                      contract/TenantScopeRequest
                                      (or opts {}))
        org-id (required-org-id! (:org_id scope))
        {:keys [segments labels]} (collections (await (db!)))
        row (await (.findOne segments #js {"_id" (object-id! segment-id "segment")
                                           "org_id" org-id}))]
    (when-not row
      (throw (js/Error. "Segment not found")))
    (let [label-rows (await (.toArray (-> (.find labels #js {"segment_id" (str segment-id)})
                                          (.sort #js {"created_at" -1}))))
          response (segment-view row (vec (array-seq label-rows)) nil)]
      (assert-response! :translation-segment/response contract/TranslationSegmentResponse response))))

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
                     :org-id (required-org-id! (:org_id input))
                     :project (:project input)})
        errors (translation/segment-errors normalized)]
    (when (seq errors)
      (throw (ex-info "Invalid translation segment" {:errors errors})))
    normalized))

(defn ^:async create-segment!
  [input]
  (await (ensure-indexes!))
  (let [request (contract/assert-valid! :create-translation-segment/request
                                        contract/CreateTranslationSegmentRequest
                                        (or input {}))
        segment (normalized-segment request)
        now (js/Date.)
        doc {:source_text (:source-text segment)
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
             :project (:project segment)
             :updated_at now}
        identity {:document_id (:document-id segment)
                  :segment_index (:segment-index segment)
                  :target_lang (:target-lang segment)
                  :org_id (:org-id segment)}
        {:keys [segments]} (collections (await (db!)))
        result (await (.findOneAndUpdate segments
                                         (clj->js identity)
                                         #js {"$set" (clj->js doc)
                                              "$setOnInsert" #js {"created_at" now}}
                                         #js {"upsert" true
                                              "returnDocument" "after"
                                              "includeResultMetadata" true}))
        row (jget result "value")]
    (when-not row
      (throw (js/Error. "Failed to create translation segment")))
    (assert-response!
     :create-translation-segment/response
     contract/CreateTranslationSegmentResponse
     {:ok true
      :id (string-id row)
      :status (jget row "status")
      :upserted (boolean (some-> result (jget "lastErrorObject") (jget "upserted")))
      :modified (boolean (some-> result (jget "lastErrorObject") (jget "updatedExisting")))})))

(defn ^:async create-segments-batch!
  [payload]
  (let [request (contract/assert-valid! :create-translation-segments-batch/request
                                        contract/CreateTranslationSegmentsBatchRequest
                                        (or payload {}))
        org-id (required-org-id! (:org_id request))
        rows (:segments request)]
    (when-not (seq rows)
      (throw (js/Error. "No segments provided")))
    (loop [index 0
           remaining rows
           results []
           errors []]
      (if-let [row (first remaining)]
        (let [attempt (try
                        {:created (await (create-segment! (assoc row
                                                                :segment_index (or (:segment_index row) index)
                                                                :org_id org-id
                                                                :project (or (:project request) (:project row)))))}
                        (catch :default err
                          {:error (or (.-message err) (str err))}))]
          (if-let [created (:created attempt)]
            (recur (inc index)
                   (next remaining)
                   (conj results {:index index :id (:id created) :status (:status created)})
                   errors)
            (recur (inc index)
                   (next remaining)
                   results
                   (conj errors {:index index :error (:error attempt)}))))
        (assert-response!
         :create-translation-segments-batch/response
         contract/CreateTranslationSegmentsBatchResponse
         (cond-> {:ok true
                  :imported (count results)
                  :errors (count errors)
                  :results results}
           (seq errors) (assoc :errors_detail errors)))))))

(defn- ^:async upsert-graph-memory!
  [collections segment corrected-text]
  (let [plan (translation/graph-memory-plan
              {:segment-id (string-id segment)
               :source-text (jget segment "source_text")
               :translated-text (jget segment "translated_text")
               :corrected-text corrected-text
               :source-lang (jget segment "source_lang")
               :target-lang (jget segment "target_lang")
               :document-id (jget segment "document_id")
               :domain (jget segment "domain")
               :content-type (jget segment "content_type")})]
    (if-not (:ok? plan)
      {:success false :error (:error plan)}
      (try
        (let [now (js/Date.)]
          (await (js/Promise.all
                  #js [(.updateOne (:graph-nodes collections)
                                   #js {"id" (get-in plan [:node :id])}
                                   #js {"$set" (clj->js (assoc (:node plan) :updated_at now))
                                        "$setOnInsert" #js {"created_at" now}}
                                   #js {"upsert" true})
                       (.updateOne (:graph-edges collections)
                                   #js {"id" (get-in plan [:edge :id])}
                                   #js {"$set" (clj->js (assoc (:edge plan) :updated_at now))
                                        "$setOnInsert" #js {"created_at" now}}
                                   #js {"upsert" true})]))
          {:success true})
        (catch :default err
          {:success false :error (or (.-message err) (str err))})))))

(defn ^:async label-segment!
  [segment-id payload]
  (await (ensure-indexes!))
  (let [request (contract/assert-valid! :label-translation-segment/request
                                        contract/LabelTranslationSegmentRequest
                                        (or payload {}))
        org-id (required-org-id! (:org_id request))
        db (await (db!))
        cs (collections db)
        oid (object-id! segment-id "segment")
        selector #js {"_id" oid "org_id" org-id}
        segment (await (.findOne (:segments cs) selector))]
    (when-not segment
      (throw (js/Error. "Segment not found")))
    (let [version (inc (await (.countDocuments (:labels cs) #js {"segment_id" (str segment-id)})))
          now (js/Date.)
          corrected-text (nonblank (:corrected_text request))
          label {:segment_id (str segment-id)
                 :labeler_id (or (nonblank (:labeler_id request)) "unknown")
                 :labeler_email (or (nonblank (:labeler_email request)) "unknown")
                 :label_version version
                 :adequacy (:adequacy request)
                 :fluency (:fluency request)
                 :terminology (:terminology request)
                 :risk (:risk request)
                 :overall (:overall request)
                 :corrected_text corrected-text
                 :editor_notes (nonblank (:editor_notes request))
                 :created_at now}
          inserted (await (.insertOne (:labels cs) (clj->js label)))
          new-status (translation/status-wire
                      (translation/next-segment-status
                       {:current-status (jget segment "status")
                        :overall (:overall request)
                        :corrected-text corrected-text}))]
      (await (.updateOne (:segments cs)
                         selector
                         #js {"$set" (clj->js (cond-> {:status new-status :updated_at now}
                                               corrected-text (assoc :translated_text corrected-text)))}))
      (let [graph-memory (when (= new-status "approved")
                           (await (upsert-graph-memory! cs segment corrected-text)))
            response {:ok true
                      :label (assoc (dissoc label :created_at)
                                    :id (some-> inserted (jget "insertedId") .toString)
                                    :ts (iso now))
                      :new_status new-status
                      :graph_memory graph-memory}]
        (assert-response! :label-translation-segment/response
                          contract/LabelTranslationSegmentResponse
                          response)))))

(defn- ^:async latest-corrections
  [labels segment-ids]
  (if-not (seq segment-ids)
    {}
    (let [rows (await (.toArray
                       (.aggregate labels
                                   (clj->js [{:$match {:segment_id {:$in segment-ids}
                                                       :corrected_text {:$exists true :$nin [nil ""]}}}
                                             {:$sort {:created_at -1}}
                                             {:$group {:_id "$segment_id"
                                                       :corrected_text {:$first "$corrected_text"}}}]))))]
      (into {} (map (fn [row] [(jget row "_id") (jget row "corrected_text")])
                    (array-seq rows))))))

(defn ^:async export-sft!
  [opts]
  (await (ensure-indexes!))
  (let [request (contract/assert-valid! :translation-export-sft/request
                                        contract/TranslationSftRequest
                                        (or opts {}))
        org-id (required-org-id! (:org_id request))
        filter (assoc (filter-map request [:project :target_lang])
                      :status "approved"
                      :org_id org-id)
        {:keys [segments labels]} (collections (await (db!)))
        rows (vec (array-seq (await (.toArray (-> (.find segments (clj->js filter))
                                                   (.sort #js {"_id" 1}))))))
        include-corrected? (not (contains? #{false "false"} (:include_corrected request)))
        corrections (if include-corrected?
                      (await (latest-corrections labels (mapv string-id rows)))
                      {})
        result (->> rows
                    (map (fn [row]
                           (translation/sft-row
                            {:source-lang (jget row "source_lang")
                             :target-lang (jget row "target_lang")
                             :source-text (jget row "source_text")
                             :translated-text (jget row "translated_text")
                             :corrected-text (get corrections (string-id row))})))
                    (map #(js/JSON.stringify (clj->js %)))
                    (str/join "\n"))]
    (assert-response! :translation-export-sft/response contract/TranslationSftResponse result)))

(defn ^:async manifest!
  [opts]
  (await (ensure-indexes!))
  (let [request (contract/assert-valid! :translation-export-manifest/request
                                        contract/TranslationManifestRequest
                                        (or opts {}))
        request (if (map? request) request {:project request})
        org-id (required-org-id! (:org_id request))
        filter (assoc (filter-map request [:project]) :org_id org-id)
        {:keys [segments labels]} (collections (await (db!)))
        language-rows (await (.toArray
                              (.aggregate segments
                                          (clj->js [{:$match filter}
                                                    {:$group {:_id "$target_lang"
                                                              :total {:$sum 1}
                                                              :approved {:$sum {:$cond [{:$eq ["$status" "approved"]} 1 0]}}
                                                              :rejected {:$sum {:$cond [{:$eq ["$status" "rejected"]} 1 0]}}
                                                              :pending {:$sum {:$cond [{:$eq ["$status" "pending"]} 1 0]}}
                                                              :in_review {:$sum {:$cond [{:$eq ["$status" "in_review"]} 1 0]}}}}]))))
        label-match (cond-> {"segment.org_id" org-id}
                      (:project request) (assoc "segment.project" (:project request)))
        label-rows (await (.toArray
                           (.aggregate labels
                                       (clj->js [{:$addFields {:segment_object_id
                                                                {:$convert {:input "$segment_id"
                                                                            :to "objectId"
                                                                            :onError nil
                                                                            :onNull nil}}}}
                                                 {:$lookup {:from segment-collection-name
                                                            :localField "segment_object_id"
                                                            :foreignField "_id"
                                                            :as "segment"}}
                                                 {:$unwind "$segment"}
                                                 {:$match label-match}
                                                 {:$facet
                                                  {:corrections [{:$match {:corrected_text {:$exists true :$nin [nil ""]}}}
                                                                 {:$group {:_id {:target_lang "$segment.target_lang"
                                                                                 :segment_id "$segment_id"}}}
                                                                 {:$group {:_id "$_id.target_lang" :count {:$sum 1}}}]
                                                   :labelers [{:$group {:_id "$labeler_email"
                                                                       :segments_labeled {:$sum 1}}}]
                                                   :label_counts [{:$group {:_id "$segment.target_lang"
                                                                           :count {:$sum 1}}}]}}]))))
        stats (first (array-seq label-rows))
        corrections (into {} (map (fn [row] [(jget row "_id") (jget row "count")])
                                  (array-seq (or (jget stats "corrections") #js []))))
        label-counts (into {} (map (fn [row] [(jget row "_id") (jget row "count")])
                                   (array-seq (or (jget stats "label_counts") #js []))))
        languages (mapv (fn [row]
                          {:target-lang (jget row "_id")
                           :total (jget row "total")
                           :approved (jget row "approved")
                           :rejected (jget row "rejected")
                           :pending (jget row "pending")
                           :in-review (jget row "in_review")})
                        (array-seq language-rows))
        shaped (translation/manifest-shape
                {:project (:project request)
                 :languages languages
                 :corrections-by-language corrections
                 :labelers (mapv (fn [row]
                                   {:email (jget row "_id")
                                    :segments-labeled (jget row "segments_labeled")})
                                 (array-seq (or (jget stats "labelers") #js [])))})
        shaped (update shaped :languages
                       (fn [by-language]
                         (into {}
                               (map (fn [[lang data]]
                                      [lang (assoc data
                                                   :avg_labels_per_segment
                                                   (if (pos? (:total_segments data))
                                                     (/ (get label-counts lang 0)
                                                        (:total_segments data))
                                                     0))]))
                               by-language)))
        response (assoc shaped :generated_at (.toISOString (js/Date.)))]
    (assert-response! :translation-export-manifest/response
                      contract/TranslationManifestResponse
                      response)))

(defn ^:async documents!
  [opts]
  (await (ensure-indexes!))
  (let [request (contract/assert-valid! :translation-documents/request
                                        contract/TranslationDocumentsRequest
                                        (or opts {}))
        org-id (required-org-id! (:org_id request))
        filter (assoc (filter-map request [:project :target_lang :source_lang :garden_id])
                      :org_id org-id)
        {:keys [segments events]} (collections (await (db!)))
        rows (await (.toArray
                     (.aggregate segments
                                 (clj->js [{:$match filter}
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
                                           {:$sort {"_id.document_id" 1 "_id.target_lang" 1}}]))))
        document-ids (mapv #(jget (jget % "_id") "document_id") (array-seq rows))
        event-rows (if (seq document-ids)
                     (await (.toArray (.find events (clj->js {:_id {:$in document-ids}}))))
                     #js [])
        titles (into {} (map (fn [row]
                               (let [extra (jget row "extra")]
                                 [(jget row "_id")
                                  {:title (or (nonblank (jget extra "title")) "Untitled")
                                   :visibility (or (nonblank (jget extra "visibility")) "internal")}]))
                             (array-seq event-rows)))
        shaped (mapv (fn [row]
                       (let [id-obj (jget row "_id")
                             document-id (jget id-obj "document_id")
                             meta (get titles document-id)]
                         (translation/document-list-row
                          {:document-id document-id
                           :target-lang (jget id-obj "target_lang")
                           :source-lang (jget row "source_lang")
                           :garden-id (jget row "garden_id")
                           :project (jget row "project")
                           :total (jget row "total_segments")
                           :approved (jget row "approved")
                           :pending (jget row "pending")
                           :rejected (jget row "rejected")
                           :in-review (jget row "in_review")
                           :title (:title meta)
                           :visibility (:visibility meta)})))
                     (array-seq rows))
        response {:documents shaped :total (count shaped)}]
    (assert-response! :translation-documents/response
                      contract/TranslationDocumentsResponse
                      response)))

(defn ^:async document!
  [document-id target-lang opts]
  (await (ensure-indexes!))
  (let [scope (contract/assert-valid! :translation-document/scope
                                      contract/TenantScopeRequest
                                      (or opts {}))
        org-id (required-org-id! (:org_id scope))
        cs (collections (await (db!)))
        rows (await (.toArray
                     (.aggregate (:segments cs)
                                 (clj->js [{:$match {:document_id (str document-id)
                                                    :target_lang (str target-lang)
                                                    :org_id org-id}}
                                           {:$sort {:segment_index 1 :created_at -1}}
                                           {:$group {:_id "$segment_index" :doc {:$first "$$ROOT"}}}
                                           {:$replaceRoot {:newRoot "$doc"}}
                                           {:$sort {:segment_index 1}}]))))]
    (when (zero? (.-length rows))
      (throw (js/Error. "Document translation not found")))
    (let [event (await (.findOne (:events cs) #js {"_id" (str document-id)}))]
      (when-not event
        (throw (js/Error. "Document not found")))
      (let [segments (vec (array-seq rows))
            ids (mapv string-id segments)
            label-rows (if (seq ids)
                         (await (.toArray
                                 (-> (.find (:labels cs) (clj->js {:segment_id {:$in ids}}))
                                     (.sort #js {"created_at" -1}))))
                         #js [])
            labels-by-id (group-by #(jget % "segment_id") (array-seq label-rows))
            extra (jget event "extra")
            shaped-segments (mapv #(segment-view % (get labels-by-id (string-id %) []) nil)
                                  segments)
            summary (translation/summarize-segments shaped-segments)
            response {:document {:id (str document-id)
                                 :title (or (nonblank (jget extra "title")) "Untitled")
                                 :content (or (jget extra "content") (jget extra "text") "")
                                 :source_lang (or (nonblank (jget extra "language")) "en")
                                 :visibility (or (nonblank (jget extra "visibility")) "internal")
                                 :source_path (or (jget extra "sourcePath") (jget extra "source_path"))}
                      :segments shaped-segments
                      :summary {:total_segments (:total-segments summary)
                                :approved (:approved summary)
                                :pending (:pending summary)
                                :rejected (:rejected summary)
                                :in_review (:in-review summary)
                                :overall_status (translation/status-wire (:overall-status summary))}}]
        (assert-response! :translation-document/response
                          contract/TranslationDocumentResponse
                          response)))))

(defn- override-for
  [overrides segment]
  (or (get overrides (str (jget segment "segment_index")))
      (get overrides (string-id segment))
      {}))

(defn ^:async review-document!
  [document-id target-lang payload]
  (await (ensure-indexes!))
  (let [request (contract/assert-valid! :review-translation-document/request
                                        contract/ReviewTranslationDocumentRequest
                                        (or payload {}))
        org-id (required-org-id! (:org_id request))
        cs (collections (await (db!)))
        selector {:document_id (str document-id)
                  :target_lang (str target-lang)
                  :org_id org-id}
        segment-array (await (.toArray
                              (-> (.find (:segments cs) (clj->js selector))
                                  (.sort #js {"segment_index" 1}))))
        segments (vec (array-seq segment-array))]
    (when-not (seq segments)
      (throw (js/Error. "No segments found for this document+language pair")))
    (let [overrides (or (:segment_overrides request) {})
          ids (mapv string-id segments)
          version-rows (await (.toArray
                               (.aggregate (:labels cs)
                                           (clj->js [{:$match {:segment_id {:$in ids}}}
                                                     {:$group {:_id "$segment_id" :count {:$sum 1}}}]))))
          versions (into {} (map (fn [row] [(jget row "_id") (jget row "count")])
                                 (array-seq version-rows)))
          now (js/Date.)
          plans (mapv (fn [segment]
                        (let [segment-id (string-id segment)
                              override (override-for overrides segment)
                              plan (translation/document-review-label-plan
                                    {:segment-id segment-id
                                     :labeler-id (:labeler_id request)
                                     :labeler-email (:labeler_email request)
                                     :overall (or (:overall override) (:overall request))
                                     :corrected-text (:corrected_text override)
                                     :editor-notes (or (:editor_notes override)
                                                       (:editor_notes request))})]
                          {:segment segment
                           :label (assoc (dissoc plan :next_status)
                                         :label_version (inc (get versions segment-id 0))
                                         :created_at now)
                           :next-status (:next_status plan)}))
                      segments)]
      (await (.bulkWrite (:labels cs)
                         (clj->js (mapv (fn [{:keys [label]}]
                                         {:insertOne {:document label}})
                                       plans))
                         #js {"ordered" false}))
      (await (.bulkWrite (:segments cs)
                         (clj->js (mapv (fn [{:keys [segment label next-status]}]
                                         {:updateOne
                                          {:filter {:_id (jget segment "_id") :org_id org-id}
                                           :update {:$set (cond-> {:status next-status
                                                                  :updated_at now}
                                                           (:corrected_text label)
                                                           (assoc :translated_text (:corrected_text label)))}}})
                                       plans))
                         #js {"ordered" false}))
      (let [graph-results (await (js/Promise.all
                                  (clj->js
                                   (mapv (fn [{:keys [segment label next-status]}]
                                           (if (= next-status "approved")
                                             (upsert-graph-memory! cs segment (:corrected_text label))
                                             (js/Promise.resolve {:success true})))
                                         plans))))
            graph-failures (count (remove :success
                                          (map #(if (map? %)
                                                  %
                                                  (js->clj % :keywordize-keys true))
                                               (array-seq graph-results))))
            response {:ok true
                      :document_id (str document-id)
                      :target_lang (str target-lang)
                      :segments_reviewed (count segments)
                      :segments_failed 0
                      :overall (:overall request)
                      :overrides_applied (count overrides)
                      :graph_memory_failures graph-failures}]
        (assert-response! :review-translation-document/response
                          contract/ReviewTranslationDocumentResponse
                          response)))))

(defn ^:async create-batch!
  [payload]
  (await (ensure-indexes!))
  (let [request (contract/assert-valid! :create-translation-batch/request
                                        contract/CreateTranslationBatchRequest
                                        (or payload {}))
        org-id (required-org-id! (:org_id request))
        document-ids (mapv str (:document_ids request))
        now (js/Date.)
        batch {:batch_id (.randomUUID js/crypto)
               :garden_id (:garden_id request)
               :target_lang (:target_lang request)
               :source_lang (or (:source_lang request) "en")
               :project (or (:project request) "devel")
               :org_id org-id
               :status "queued"
               :document_ids document-ids
               :completed_documents []
               :failed_documents []
               :created_at now}
        {:keys [batches]} (collections (await (db!)))
        inserted (await (.insertOne batches (clj->js batch)))
        response {:ok true
                  :batch_id (:batch_id batch)
                  :id (some-> inserted (jget "insertedId") .toString)
                  :status "queued"
                  :document_ids document-ids}]
    (assert-response! :create-translation-batch/response
                      contract/CreateTranslationBatchResponse
                      response)))

(defn ^:async list-batches!
  [opts]
  (await (ensure-indexes!))
  (let [request (contract/assert-valid! :translation-batches/request
                                        contract/TranslationBatchesRequest
                                        (or opts {}))
        org-id (required-org-id! (:org_id request))
        filter (assoc (filter-map request [:garden_id :target_lang :status]) :org_id org-id)
        {:keys [batches]} (collections (await (db!)))
        rows (await (.toArray (-> (.find batches (clj->js filter))
                                  (.sort #js {"created_at" -1})
                                  (.limit 50))))
        response {:batches (mapv batch-view (array-seq rows))}]
    (assert-response! :translation-batches/response
                      contract/TranslationBatchesResponse
                      response)))

(defn ^:async next-batch!
  [opts]
  (await (ensure-indexes!))
  (let [scope (contract/assert-valid! :next-translation-batch/scope
                                      contract/TenantScopeRequest
                                      (or opts {}))
        org-id (required-org-id! (:org_id scope))
        {:keys [batches]} (collections (await (db!)))
        row (await (.findOneAndUpdate batches
                                      #js {"status" "queued" "org_id" org-id}
                                      #js {"$set" #js {"status" "processing"
                                                       "started_at" (js/Date.)
                                                       "updated_at" (js/Date.)}
                                           "$inc" #js {"attempts" 1}}
                                      #js {"sort" #js {"created_at" 1}
                                           "returnDocument" "after"}))
        response {:batch (batch-view row)}]
    (assert-response! :next-translation-batch/response
                      contract/NextTranslationBatchResponse
                      response)))

(defn- batch-selector
  [batch-id org-id]
  (try
    #js {"_id" (ObjectId. (str batch-id)) "org_id" org-id}
    (catch :default _
      #js {"batch_id" (str batch-id) "org_id" org-id})))

(defn ^:async batch!
  [batch-id opts]
  (await (ensure-indexes!))
  (let [scope (contract/assert-valid! :translation-batch/scope
                                      contract/TenantScopeRequest
                                      (or opts {}))
        org-id (required-org-id! (:org_id scope))
        {:keys [batches]} (collections (await (db!)))
        row (await (.findOne batches (batch-selector batch-id org-id)))]
    (when-not row
      (throw (js/Error. "Batch not found")))
    (assert-response! :translation-batch/response
                      contract/TranslationBatchResponse
                      (batch-view row))))

(defn ^:async update-batch!
  [batch-id payload]
  (await (ensure-indexes!))
  (let [request (contract/assert-valid! :update-translation-batch/request
                                        contract/UpdateTranslationBatchRequest
                                        (or payload {}))
        org-id (required-org-id! (:org_id request))
        status (:status request)
        now (js/Date.)
        set-fields (cond-> {:status status :updated_at now}
                     (= status "processing")
                     (merge {:started_at now}
                            (select-keys request [:agent_session_id
                                                  :agent_conversation_id
                                                  :agent_run_id]))
                     (contains? #{"complete" "partial" "failed"} status)
                     (merge {:completed_at now}
                            (select-keys request [:error])))
        push-fields (cond-> {}
                      (:completed_document request)
                      (assoc :completed_documents (:completed_document request))
                      (:failed_document request)
                      (assoc :failed_documents (:failed_document request)))
        update-doc (cond-> {:$set set-fields}
                     (seq push-fields) (assoc :$push push-fields))
        {:keys [batches]} (collections (await (db!)))
        result (await (.updateOne batches
                                  (batch-selector batch-id org-id)
                                  (clj->js update-doc)))]
    (when (zero? (jget result "matchedCount"))
      (throw (js/Error. "Batch not found")))
    (assert-response! :update-translation-batch/response
                      contract/UpdateTranslationBatchResponse
                      {:ok true :batch_id (str batch-id) :status status})))

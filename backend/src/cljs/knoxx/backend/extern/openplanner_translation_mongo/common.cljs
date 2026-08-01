(ns knoxx.backend.extern.openplanner-translation-mongo.common
  "Shared MongoDB interop and invariants for Knoxx translation storage."
  (:require [knoxx.backend.infra.mongo-client :as mongo-client]
            [knoxx.backend.law.openplanner-translation :as contract]
            [openplanner.translations.core :as translation]
            ["mongodb" :refer [ObjectId]]))

(def segment-collection-name "translation_segments")
(def label-collection-name "translation_labels")
(def batch-collection-name "translation_batches")
(def event-collection-name "events")
(def graph-node-collection-name "graph_nodes")
(def graph-edge-collection-name "graph_edges")

(def ^:private segment-org-index-name "segment_org_unique_idx")
(def ^:private legacy-segment-index-name "segment_unique_idx")
(def ^:private segment-org-index-key
  #js {"org_id" 1 "document_id" 1 "segment_index" 1 "target_lang" 1})
(def ^:private segment-org-index-fields
  [["org_id" 1]
   ["document_id" 1]
   ["segment_index" 1]
   ["target_lang" 1]])
(defonce ^:private index-promise* (atom nil))

(defn jget
  "Read a JavaScript object property, returning nil for a nil object."
  [obj property]
  (when obj (aget obj property)))

(defn string-id
  "Return a Mongo row's _id or id as a string."
  [row]
  (some-> (or (jget row "_id") (jget row "id")) .toString))

(defn iso
  "Normalize a date-like value to an ISO string."
  [value]
  (cond
    (nil? value) nil
    (instance? js/Date value) (.toISOString value)
    (fn? (jget value "toISOString")) (.toISOString value)
    :else (str value)))

(defn nonblank
  "Normalize a value to a trimmed, nonblank string or nil."
  [value]
  (translation/nonblank-string value))

(defn required-org-id!
  "Defensively require a nonblank organization id for non-route callers."
  [value]
  (or (nonblank value)
      (throw (js/Error. "org_id is required for direct translation storage"))))

(defn object-id!
  "Parse a Mongo ObjectId or throw a named invalid-id error."
  [value label]
  (try
    (ObjectId. (str value))
    (catch :default _
      (throw (js/Error. (str "Invalid " label " ID"))))))

(defn collections
  "Return the translation-related Mongo collections for a database."
  [db]
  {:segments (.collection db segment-collection-name)
   :labels (.collection db label-collection-name)
   :batches (.collection db batch-collection-name)
   :events (.collection db event-collection-name)
   :graph-nodes (.collection db graph-node-collection-name)
   :graph-edges (.collection db graph-edge-collection-name)})

(defn ^:async db!
  "Resolve the shared Mongo database or throw when unavailable."
  []
  (or (mongo-client/get-db)
      (await (mongo-client/init-mongo!))
      (throw (js/Error. "MongoDB is unavailable for direct translation storage"))))

(defn- index-key-matches?
  [index]
  (let [index-key (jget index "key")
        ordered-fields (when index-key
                         (mapv (fn [field]
                                 [field (jget index-key field)])
                               (array-seq (js/Object.keys index-key))))]
    (and index-key
         (= (count segment-org-index-fields) (count ordered-fields))
         (= segment-org-index-fields ordered-fields))))

(defn- index-by-name
  [indexes index-name]
  (first (filter #(= index-name (jget % "name"))
                 (array-seq indexes))))

(defn- ^:async collection-indexes
  [collection]
  (try
    (await (.indexes collection))
    (catch :default err
      (if (= "NamespaceNotFound" (jget err "codeName"))
        #js []
        (throw err)))))

(defn- ^:async ensure-segment-org-index!
  [segments]
  (let [indexes (await (collection-indexes segments))
        current (index-by-name indexes segment-org-index-name)
        legacy (index-by-name indexes legacy-segment-index-name)]
    (cond
      current
      (do
        (when-not (index-key-matches? current)
          (throw (js/Error. "segment_org_unique_idx has an incompatible key pattern")))
        (when legacy
          (await (.dropIndex segments legacy-segment-index-name))))

      (and legacy (index-key-matches? legacy))
      ;; #210 may already have migrated the key while retaining the legacy
      ;; name. That state is fully safe and avoids an equivalent-index conflict.
      true

      :else
      (do
        ;; Build the replacement first. If same-tenant duplicates exist,
        ;; Mongo rejects the build and the stricter legacy index remains intact.
        (await (.createIndex segments
                             segment-org-index-key
                             #js {"unique" true "name" segment-org-index-name}))
        (when legacy
          (await (.dropIndex segments legacy-segment-index-name)))))))

(defn- ^:async backfill-label-scope!
  [labels]
  ;; Labels created before tenant denormalization inherit scope from their
  ;; authoritative segment. Invalid/orphan labels are deliberately discarded
  ;; from the merge rather than being assigned an inferred tenant.
  (await (.toArray
          (.aggregate labels
                      (clj->js [{:$match {:$or [{:org_id {:$exists false}}
                                                {:org_id nil}]}}
                                {:$addFields {:segment_object_id
                                              {:$convert {:input "$segment_id"
                                                          :to "objectId"
                                                          :onError nil
                                                          :onNull nil}}}}
                                {:$lookup {:from segment-collection-name
                                           :localField "segment_object_id"
                                           :foreignField "_id"
                                           :as "segment"}}
                                {:$unwind "$segment"}
                                {:$set {:org_id "$segment.org_id"
                                        :project "$segment.project"}}
                                {:$project {:segment 0 :segment_object_id 0}}
                                {:$merge {:into label-collection-name
                                          :on "_id"
                                          :whenMatched "merge"
                                          :whenNotMatched "discard"}}])))))

(defn- ^:async create-indexes!
  [db]
  (let [{:keys [segments labels batches]} (collections db)]
    (await (ensure-segment-org-index! segments))
    (await (.createIndex labels #js {"segment_id" 1 "created_at" -1}))
    (await (.createIndex labels #js {"org_id" 1 "project" 1}))
    (await (backfill-label-scope! labels))
    (await (js/Promise.all
            #js [(.createIndex segments #js {"status" 1})
                 (.createIndex segments #js {"target_lang" 1})
                 (.createIndex segments #js {"garden_id" 1})
                 (.createIndex segments #js {"org_id" 1})
                 (.createIndex segments #js {"project" 1})
                 (.createIndex batches #js {"garden_id" 1 "target_lang" 1 "status" 1})
                 (.createIndex batches #js {"org_id" 1 "status" 1 "created_at" 1})]))
    true))

(defn- ^:async init-indexes!
  []
  (await (create-indexes! (await (db!)))))

(defn ^:async ensure-indexes!
  "Ensure indexes and label-scope backfill once, returning true."
  []
  (if-let [pending @index-promise*]
    (await pending)
    (let [pending (init-indexes!)]
      ;; Store the promise before its first await so concurrent callers share it.
      (reset! index-promise* pending)
      (try
        (await pending)
        (catch :default err
          (reset! index-promise* nil)
          (throw err))))))

(defn filter-map
  "Copy present fields from a map without retaining nil values."
  [source field-keys]
  (reduce (fn [result field]
            (if-let [value (get source field)]
              (assoc result field value)
              result))
          {}
          field-keys))

(defn normalized-query-number
  "Return a finite, floored query number bounded by lower and optional upper limits."
  [value default-value lower-bound upper-bound]
  (let [parsed (js/Number (or value default-value))
        finite (if (js/Number.isFinite parsed) parsed default-value)
        bounded (max lower-bound (js/Math.floor finite))]
    (if upper-bound (min upper-bound bounded) bounded)))

(defn event-selector
  "Build a tenant-scoped source-event selector for one id or an id clause."
  [id-clause org-id]
  {:_id id-clause
   :$or [{:org_id org-id}
         {:orgId org-id}
         {:tenant_id org-id}
         {"extra.org_id" org-id}
         {"extra.orgId" org-id}
         {"extra.tenant_id" org-id}
         {"meta.org_id" org-id}
         {"meta.orgId" org-id}
         {"meta.tenant_id" org-id}]})

(defn segment-view
  "Normalize a Mongo segment row and its labels into the API wire map."
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

(defn batch-view
  "Normalize a Mongo batch row, including owning tenant and membership."
  [row]
  (when row
    {:id (string-id row)
     :batch_id (jget row "batch_id")
     :garden_id (jget row "garden_id")
     :target_lang (jget row "target_lang")
     :source_lang (jget row "source_lang")
     :project (jget row "project")
     :org_id (jget row "org_id")
     :membership_id (jget row "membership_id")
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

(defn assert-response!
  "Validate and return a boundary response map."
  [contract-id schema value]
  (contract/assert-valid! contract-id schema value))

(defn segment-doc-matches?
  "Return true when a Mongo row already contains every persisted segment field."
  [row segment-doc]
  (boolean
   (and row
        (every? (fn [[field value]]
                  (= (jget row (name field)) value))
                segment-doc))))

(defn ^:async next-label-version!
  "Atomically reserve the next per-segment label version."
  [segments labels selector segment-id]
  (let [latest (await (.findOne labels
                                #js {"segment_id" (str segment-id)}
                                #js {"sort" #js {"label_version" -1}
                                     "projection" #js {"label_version" 1}}))
        baseline (or (jget latest "label_version") 0)
        row (await (.findOneAndUpdate
                    segments
                    selector
                    (clj->js [{:$set {:label_version_counter
                                      {:$add [{:$ifNull ["$label_version_counter" baseline]} 1]}}}])
                    #js {"returnDocument" "after"}))]
    (when-not row
      (throw (js/Error. "Segment disappeared while reserving label version")))
    (jget row "label_version_counter")))

(defn ^:async upsert-graph-memory!
  "Upsert an approved segment into graph memory, returning {:success ...}."
  [collection-map segment corrected-text]
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
                  #js [(.updateOne (:graph-nodes collection-map)
                                   #js {"id" (get-in plan [:node :id])}
                                   #js {"$set" (clj->js (assoc (:node plan) :updated_at now))
                                        "$setOnInsert" #js {"created_at" now}}
                                   #js {"upsert" true})
                       (.updateOne (:graph-edges collection-map)
                                   #js {"id" (get-in plan [:edge :id])}
                                   #js {"$set" (clj->js (assoc (:edge plan) :updated_at now))
                                        "$setOnInsert" #js {"created_at" now}}
                                   #js {"upsert" true})]))
          {:success true})
        (catch :default err
          {:success false :error (or (.-message err) (str err))})))))
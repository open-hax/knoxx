(ns knoxx.backend.extern.openplanner-translation-mongo.batches
  "Tenant- and membership-scoped translation batch persistence."
  (:require [knoxx.backend.extern.openplanner-translation-mongo.common :as common]
            [knoxx.backend.law.openplanner-translation :as contract]
            ["mongodb" :refer [ObjectId]]))

(defn ^:async create-batch!
  "Create a tenant- and membership-scoped batch; returns its public identifiers."
  [payload]
  (await (common/ensure-indexes!))
  (let [request (contract/assert-valid! :create-translation-batch/request
                                        contract/CreateTranslationBatchRequest
                                        (or payload {}))
        org-id (common/required-org-id! (:org_id request))
        document-ids (mapv str (:document_ids request))
        now (js/Date.)
        batch {:batch_id (.randomUUID js/crypto)
               :garden_id (:garden_id request)
               :target_lang (:target_lang request)
               :source_lang (or (:source_lang request) "en")
               :project (or (:project request) "devel")
               :org_id org-id
               :membership_id (:membership_id request)
               :status "queued"
               :document_ids document-ids
               :completed_documents []
               :failed_documents []
               :attempts 0
               :created_at now
               :updated_at now}
        {:keys [batches]} (common/collections (await (common/db!)))
        inserted (await (.insertOne batches (clj->js batch)))
        response {:ok true
                  :batch_id (:batch_id batch)
                  :id (some-> inserted (common/jget "insertedId") .toString)
                  :status "queued"
                  :document_ids document-ids}]
    (common/assert-response! :create-translation-batch/response
                             contract/CreateTranslationBatchResponse
                             response)))

(defn ^:async list-batches!
  "List tenant-scoped translation batches; returns {:batches [...]}."
  [opts]
  (await (common/ensure-indexes!))
  (let [request (contract/assert-valid! :translation-batches/request
                                        contract/TranslationBatchesRequest
                                        (or opts {}))
        org-id (common/required-org-id! (:org_id request))
        selector (assoc (common/filter-map request
                                           [:garden_id :target_lang :status])
                        :org_id org-id)
        {:keys [batches]} (common/collections (await (common/db!)))
        rows (await (.toArray
                     (-> (.find batches (clj->js selector))
                         (.sort #js {"created_at" -1})
                         (.limit 50))))
        response {:batches (mapv common/batch-view (array-seq rows))}]
    (common/assert-response! :translation-batches/response
                             contract/TranslationBatchesResponse
                             response)))

(defn ^:async next-batch!
  "Claim the next tenant-scoped queued batch; returns {:batch batch-or-nil}."
  [opts]
  (await (common/ensure-indexes!))
  (let [scope (contract/assert-valid! :next-translation-batch/scope
                                      contract/TenantScopeRequest
                                      (or opts {}))
        org-id (common/required-org-id! (:org_id scope))
        now (js/Date.)
        {:keys [batches]} (common/collections (await (common/db!)))
        row (await (.findOneAndUpdate
                    batches
                    #js {"status" "queued" "org_id" org-id}
                    #js {"$set" #js {"status" "processing"
                                     "started_at" now
                                     "updated_at" now}
                         "$inc" #js {"attempts" 1}}
                    #js {"sort" #js {"created_at" 1}
                         "returnDocument" "after"}))
        response {:batch (common/batch-view row)}]
    (common/assert-response! :next-translation-batch/response
                             contract/NextTranslationBatchResponse
                             response)))

(defn- batch-selector
  [batch-id org-id]
  (try
    #js {"_id" (ObjectId. (str batch-id)) "org_id" org-id}
    (catch :default _
      #js {"batch_id" (str batch-id) "org_id" org-id})))

(defn ^:async batch!
  "Load one tenant-scoped batch; returns the normalized batch map."
  [batch-id opts]
  (await (common/ensure-indexes!))
  (let [scope (contract/assert-valid! :translation-batch/scope
                                      contract/TenantScopeRequest
                                      (or opts {}))
        org-id (common/required-org-id! (:org_id scope))
        {:keys [batches]} (common/collections (await (common/db!)))
        row (await (.findOne batches (batch-selector batch-id org-id)))]
    (when-not row
      (throw (js/Error. "Batch not found")))
    (common/assert-response! :translation-batch/response
                             contract/TranslationBatchResponse
                             (common/batch-view row))))

(defn ^:async update-batch!
  "Update one tenant-scoped batch; returns its id and new status."
  [batch-id payload]
  (await (common/ensure-indexes!))
  (let [request (contract/assert-valid! :update-translation-batch/request
                                        contract/UpdateTranslationBatchRequest
                                        (or payload {}))
        org-id (common/required-org-id! (:org_id request))
        status (:status request)
        now (js/Date.)
        set-fields (cond-> {:status status :updated_at now}
                     (= status "processing")
                     (merge {:started_at now}
                            (select-keys request
                                         [:agent_session_id
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
        {:keys [batches]} (common/collections (await (common/db!)))
        result (await (.updateOne batches
                                  (batch-selector batch-id org-id)
                                  (clj->js update-doc)))]
    (when (zero? (or (common/jget result "matchedCount") 0))
      (throw (js/Error. "Batch not found")))
    (common/assert-response! :update-translation-batch/response
                             contract/UpdateTranslationBatchResponse
                             {:ok true
                              :batch_id (str batch-id)
                              :status status})))
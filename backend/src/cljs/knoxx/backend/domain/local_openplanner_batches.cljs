(ns knoxx.backend.domain.local-openplanner-batches
  "Finite scoped batch admission, atomic claim and progress transitions."
  (:require [knoxx.backend.law.openplanner-translation :as contract]))

(defn- checked [schema value] (contract/assert-valid! :local/translation-batch schema value))
(defn- visible? [scope row]
  (and (= (:org_id scope) (:org_id row))
       (every? (fn [key] (or (nil? (get scope key)) (= (get scope key) (get row key))))
               [:garden_id :target_lang :status :dispatch_key])))
(defn- public-row [row] (dissoc row :membership_id))

(defn create
  "Bind a dispatch key to its original tenant-owned request."
  [state request id timestamp]
  (checked contract/CreateTranslationBatchRequest request)
  (let [key [(:org_id request) (:dispatch_key request)]
        existing-id (when (:dispatch_key request) (get-in state [:batch-dispatch key]))
        existing (get-in state [:batches existing-id])
        row (or existing (assoc request :id id :batch_id id :status "queued" :attempts 0
                                        :created_at timestamp :updated_at timestamp
                                        :completed_documents [] :failed_documents []))]
    (when (and existing (not= request (get-in state [:batch-inputs existing-id])))
      (throw (ex-info "Translation dispatch key was reused with different arguments"
                      {:status 409 :code "openplanner_batch_conflict"})))
    [(if existing state
         (cond-> (-> state (assoc-in [:batches id] row) (assoc-in [:batch-inputs id] request))
           (:dispatch_key request) (assoc-in [:batch-dispatch key] id)))
     (assoc (public-row row) :ok true)]))

(defn batches
  "List bounded tenant batch metadata without membership credentials."
  [state scope]
  (checked contract/TranslationBatchesRequest scope)
  {:batches (->> (vals (:batches state)) (filter #(visible? scope %))
                 (sort-by (juxt :created_at :id)) reverse (take 50) (mapv public-row))})

(defn batch
  "Return one tenant-owned batch; cross-tenant ids remain absent."
  [state id scope]
  (checked contract/TenantScopeRequest scope)
  (let [row (get-in state [:batches id])]
    (when-not (and row (visible? scope row))
      (throw (ex-info "Translation batch not found" {:status 404})))
    (public-row row)))

(defn claim
  "Claim the oldest queued tenant batch in one accepted ledger transition."
  [state scope timestamp]
  (checked contract/NextTranslationBatchScope scope)
  (if-let [row (first (sort-by (juxt :created_at :id)
                              (filter #(and (visible? scope %) (= "queued" (:status %)))
                                      (vals (:batches state)))))]
    (let [claimed (-> row (assoc :status "processing" :started_at timestamp :updated_at timestamp)
                      (update :attempts inc))]
      [(assoc-in state [:batches (:id row)] claimed)
       {:batch (if (:include_membership scope) claimed (public-row claimed))}])
    [state {:batch nil}]))

(defn update-status
  "Record scoped batch progress while retaining previously completed documents."
  [state id request timestamp]
  (checked contract/UpdateTranslationBatchRequest request)
  (let [row (batch state id request)
        allowed? (or (= (:status row) (:status request))
                     (= "queued" (:status row)) (= "processing" (:status row)))]
    (when-not allowed?
      (throw (ex-info "Completed translation batch cannot be reopened"
                      {:status 409 :code "openplanner_batch_terminal"})))
    (let [next-row (cond-> (merge (get-in state [:batches id])
                                 (select-keys request [:status :error :agent_session_id :agent_conversation_id :agent_run_id])
                                 {:updated_at timestamp})
                     (:completed_document request)
                     (update :completed_documents #(vec (distinct (conj % (:completed_document request)))))
                     (:failed_document request)
                     (update :failed_documents #(vec (distinct (conj % (:failed_document request))))))]
      [(assoc-in state [:batches id] next-row) {:ok true :batch_id id :status (:status next-row)}])))

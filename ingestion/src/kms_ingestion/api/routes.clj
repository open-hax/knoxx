(ns kms-ingestion.api.routes
  "Reitit routes for the ingestion API."
  (:require
   [clj-http.client :as http]
   [clojure.string :as str]
   [kms-ingestion.api.common :as common]
   [kms-ingestion.api.query-support :as query]
   [kms-ingestion.api.workspace-support :as workspace]
   [kms-ingestion.config :as config]
   [kms-ingestion.db :as db]
   [kms-ingestion.drivers.registry :as registry]
   [kms-ingestion.jobs.worker :as worker]
   [muuntaja.core :as m]
   [reitit.ring.middleware.muuntaja :as muuntaja]))

(def json->clj common/json->clj)
(def uuid-str common/uuid-str)
(def ts-str common/ts-str)
(def get-tenant-id common/get-tenant-id)
(def request-body->map common/request-body->map)
(def role-presets query/role-presets)
(def federated-fts query/federated-fts)
(def call-proxx-chat query/call-proxx-chat)
(def devel-answer-system-prompt query/devel-answer-system-prompt)
(def build-answer-user-prompt query/build-answer-user-prompt)
(def browse-path-handler workspace/browse-path-handler)
(def semantic-file-search-handler workspace/semantic-file-search-handler)
(def preview-file-handler workspace/preview-file-handler)

(defn source-response
  ([source]
   (source-response source {:include-collections? true
                            :include-last-scan? true
                            :include-last-error? true
                            :include-updated-at? true}))
  ([source {:keys [include-collections? include-last-scan? include-last-error? include-updated-at?]}]
   (cond-> {:source_id (uuid-str (:source_id source))
            :tenant_id (:tenant_id source)
            :driver_type (:driver_type source)
            :name (:name source)
            :config (json->clj (:config source))
            :state (json->clj (:state source))
            :enabled (:enabled source)
            :created_at (str (:created_at source))}
     include-collections?
     (assoc :collections (json->clj (:collections source)))

     include-last-scan?
     (assoc :last_scan_at (str (:last_scan_at source)))

     include-last-error?
     (assoc :last_error (:last_error source))

     include-updated-at?
     (assoc :updated_at (str (:updated_at source))))))

(defn job-response
  ([job]
   (job-response job {:include-created-at? true}))
  ([job {:keys [include-created-at?]}]
   (cond-> {:job_id (uuid-str (:job_id job))
            :source_id (uuid-str (:source_id job))
            :tenant_id (:tenant_id job)
            :status (:status job)
            :total_files (:total_files job)
            :processed_files (:processed_files job)
            :failed_files (:failed_files job)
            :skipped_files (:skipped_files job)
            :chunks_created (:chunks_created job)
            :started_at (ts-str (:started_at job))
            :completed_at (ts-str (:completed_at job))
            :error_message (:error_message job)}
     include-created-at?
     (assoc :created_at (ts-str (:created_at job))))))

;; ============================================================
;; Drivers API
;; ============================================================

(defn list-drivers-handler
  [_request]
  {:status 200
   :body (registry/list-drivers)})

;; ============================================================
;; Sources API
;; ============================================================

(defn list-sources-handler
  [request]
  (let [tenant-id (get-tenant-id request)
        sources (db/list-sources tenant-id)]
    {:status 200
     :body (map source-response sources)}))

(defn create-source-handler
  [request]
  (let [tenant-id (get-tenant-id request)
        body (request-body->map request)
        driver-type (or (:driver_type body) (:driver-type body))
        name (:name body)
        config (or (:config body) {})
        collections (or (:collections body) [tenant-id])
        file-types (or (:file_types body) (:file-types body))
        include-patterns (or (:include_patterns body) (:include-patterns body))
        exclude-patterns (or (:exclude_patterns body) (:exclude-patterns body))
        _ (when-not driver-type
            (println "[kms-ingestion] create-source missing driver_type. body type=" (type (:body request)) "parsed=" body))
        source (db/create-source!
                {:tenant-id tenant-id
                 :driver-type driver-type
                 :name name
                 :config config
                 :collections collections
                 :file-types file-types
                 :include-patterns include-patterns
                 :exclude-patterns exclude-patterns})]
    {:status 200
     :body (source-response source {:include-collections? false
                                    :include-last-scan? false
                                    :include-last-error? false
                                    :include-updated-at? false})}))

(defn get-source-handler
  [request]
  (let [tenant-id (get-tenant-id request)
        source-id (-> request :path-params :source_id)
        source (db/get-source source-id tenant-id)]
    (if source
      {:status 200
       :body (source-response source)}
      {:status 404
       :body {:error "Source not found"}})))

(defn delete-source-handler
  [request]
  (let [tenant-id (get-tenant-id request)
        source-id (-> request :path-params :source_id)
        result (db/delete-source! source-id tenant-id)]
    (if result
      {:status 200
       :body {:status "deleted" :source_id source-id}}
      {:status 404
       :body {:error "Source not found"}})))

;; ============================================================
;; Jobs API
;; ============================================================

(defn list-jobs-handler
  [request]
  (let [tenant-id (get-tenant-id request)
        source-id (or (-> request :query-params :source_id)
                      (get (:query-params request) "source_id"))
        status (or (-> request :query-params :status)
                   (get (:query-params request) "status"))
        limit (when-let [l (or (-> request :query-params :limit)
                               (get (:query-params request) "limit"))]
                (Integer/parseInt l))
        jobs (db/list-jobs {:tenant-id tenant-id
                            :source-id source-id
                            :status status
                            :limit limit})]
    {:status 200
     :body (map job-response jobs)}))

(defn create-job-handler
  [request]
  (let [tenant-id (get-tenant-id request)
        body (request-body->map request)
        source-id (:source_id body)
        full-scan (:full_scan body false)
        source (db/get-source source-id tenant-id)]
    (if-not source
      {:status 404
       :body {:error "Source not found"}}
      (if-not (:enabled source)
        {:status 400
         :body {:error "Source is disabled"}}
        (let [job (db/create-job! source-id tenant-id {:full_scan full-scan})
              job-id (uuid-str (:job_id job))]
          (db/mark-source-scanned! source-id)
          (worker/queue-job! job-id source)
          {:status 200
           :body {:job_id job-id
                  :source_id source-id
                  :tenant_id tenant-id
                  :status (:status job)
                  :created_at (ts-str (:created_at job))}})))))

(defn get-job-handler
  [request]
  (let [tenant-id (get-tenant-id request)
        job-id (-> request :path-params :job_id)
        job (db/get-job job-id tenant-id)]
    (if job
      {:status 200
       :body (job-response job)}
      {:status 404
       :body {:error "Job not found"}})))

(defn cancel-job-handler
  [request]
  (let [tenant-id (get-tenant-id request)
        job-id (-> request :path-params :job_id)
        job (db/get-job job-id tenant-id)]
    (if (and job (= (:status job) "running"))
      (do
        (db/update-job! job-id {:status "cancelled"
                                :completed_at (java.sql.Timestamp/from (java.time.Instant/now))})
        {:status 200
         :body {:status "cancelled" :job_id job-id}})
      {:status 400
       :body {:error "Job cannot be cancelled"}})))

;; ============================================================
;; Health
;; ============================================================

(defn health-handler
  [_request]
  {:status 200
   :body {:status "ok" :service "kms-ingestion"}})

;; ============================================================
;; Query API
;; ============================================================

(defn query-presets-handler
  [_request]
  {:status 200
   :body {:presets role-presets}})

(defn query-gardens-handler
  [_request]
  (let [base-url (config/openplanner-url)
        api-key (config/openplanner-api-key)
        headers (cond-> {"Content-Type" "application/json"}
                  (not (str/blank? api-key))
                  (assoc "Authorization" (str "Bearer " api-key)))
        resp (http/get
              (str base-url "/v1/gardens")
              {:headers headers
               :accept :json
               :as :json
               :throw-exceptions false
               :socket-timeout 30000
               :connection-timeout 30000})]
    (if (= 200 (:status resp))
      {:status 200 :body (:body resp)}
      {:status 502 :body {:error "OpenPlanner gardens unavailable" :details (:body resp)}})))

(defn search-handler
  [request]
  (let [tenant-id (get-tenant-id request)
        body (request-body->map request)
        q (:q body)
        role (:role body)
        projects (:projects body)
        kinds (:kinds body)
        limit (or (:limit body) 10)]
    (if (str/blank? q)
      {:status 400 :body {:error "q is required"}}
      {:status 200
       :body (federated-fts {:tenant-id tenant-id
                             :q q
                             :role role
                             :projects projects
                             :kinds kinds
                             :limit limit})})))

(defn answer-handler
  [request]
  (let [tenant-id (get-tenant-id request)
        body (request-body->map request)
        q (:q body)
        role (:role body)
        projects (:projects body)
        kinds (:kinds body)
        model (:model body)
        system-prompt (or (:system_prompt body) (:system-prompt body))
        limit (or (:limit body) 8)]
    (if (str/blank? q)
      {:status 400 :body {:error "q is required"}}
      (let [{:keys [rows projects] :as result}
            (federated-fts {:tenant-id tenant-id
                            :q q
                            :role role
                            :projects projects
                            :kinds kinds
                            :limit limit})
            proxx-result (call-proxx-chat {:model model
                                           :system-prompt (or system-prompt
                                                              (devel-answer-system-prompt {:projects projects
                                                                                           :context-found? (seq rows)}))
                                           :messages [{:role "user"
                                                       :content (build-answer-user-prompt {:q q
                                                                                           :projects projects
                                                                                           :rows rows})}]})
            timeout? (or (common/timeout-error? (:error proxx-result))
                         (contains? #{408 504} (:status proxx-result)))]
        (if (:ok proxx-result)
          {:status 200
           :body {:projects projects
                  :count (:count result)
                  :rows rows
                  :model model
                  :answer_mode "model"
                  :answer (:text proxx-result)}}
          {:status (if timeout? 504 502)
           :body {:error (if timeout?
                           "Proxx timed out while generating the chat response."
                           "Proxx failed while generating the chat response.")
                  :error_code (if timeout? "proxx_timeout" "proxx_request_failed")
                  :model_error (:error proxx-result)
                  :projects projects
                  :count (:count result)
                  :rows rows
                  :model model}})))))

;; ============================================================
;; Routes
;; ============================================================

(def routes
  [["/health"
    {:get health-handler}]

   ["/api/query"
    ["/presets" {:get query-presets-handler}]
    ["/gardens" {:get query-gardens-handler}]
    ["/search" {:post search-handler}]
    ["/answer" {:post answer-handler}]]

   ["/api/ingestion"
    {:middleware [muuntaja/format-middleware]
     :muuntaja m/instance}

    ["/drivers"
     {:get list-drivers-handler}]

    ["/browse"
     {:get browse-path-handler}]

    ["/search"
     {:post semantic-file-search-handler}]

    ["/file"
     {:get preview-file-handler}]

    ["/sources"
     {:get list-sources-handler
      :post create-source-handler}]

    ["/sources/:source_id"
     {:get get-source-handler
      :delete delete-source-handler}]

    ["/jobs"
     {:get list-jobs-handler
      :post create-job-handler}]

    ["/jobs/:job_id"
     {:get get-job-handler}]

    ["/jobs/:job_id/cancel"
     {:post cancel-job-handler}]]])

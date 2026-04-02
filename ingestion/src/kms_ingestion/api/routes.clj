(ns kms-ingestion.api.routes
  "Reitit routes for the ingestion API."
  (:require
    [kms-ingestion.db :as db]
    [kms-ingestion.config :as config]
    [kms-ingestion.drivers.registry :as registry]
    [kms-ingestion.jobs.worker :as worker]
    [cheshire.core :as json]
    [clj-http.client :as http]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [muuntaja.core :as m])
  (:import
    [java.io File InputStream Reader]
    [java.util UUID]))

;; ============================================================
;; Helpers
;; ============================================================

(defn json->clj
  "Parse JSON string to Clojure data."
  [s]
  (when s
    (if (string? s)
      (json/parse-string s keyword)
      s)))

(defn clj->json
  "Serialize Clojure data to JSON string."
  [x]
  (json/generate-string x))

(defn uuid-str
  "Convert UUID to string."
  [x]
  (str x))

(defn get-tenant-id
  "Extract tenant-id from request."
  [request]
  (or (-> request :params :tenant_id)
      (get (:params request) "tenant_id")
      (-> request :query-params :tenant_id)
      (get (:query-params request) "tenant_id")
      "devel"))

(def role-presets
  {"knowledge" ["devel-docs"]
   "development" ["devel-code" "devel-docs" "cephalon-hive"]
   "devsecops" ["devel-config" "devel-code" "devel-docs" "devel-data" "devel-events" "sintel"]
   "analyst" ["devel-data" "devel-events" "sintel" "devel-docs"]
   "owner" ["devel-docs" "devel-code" "devel-config" "devel-data" "devel-events" "cephalon-hive" "sintel"]
   "cto" ["devel-docs" "devel-code" "devel-config" "devel-data" "devel-events" "cephalon-hive" "sintel"]})

(defn expand-projects
  [tenant-id role projects]
  (let [normalized-projects (->> projects (filter some?) vec)]
    (cond
      (seq normalized-projects)
      normalized-projects

      (and role (contains? role-presets role))
      (get role-presets role)

      :else
      [(str tenant-id "-docs")])))

(defn call-openplanner-fts
  [{:keys [q limit project kind]}]
  (let [base-url (config/openplanner-url)
        api-key (config/openplanner-api-key)
        payload (cond-> {:q q :limit (or limit 10)}
                  project (assoc :project project)
                  kind (assoc :kind kind))
        headers (cond-> {"Content-Type" "application/json"}
                  (not (str/blank? api-key))
                  (assoc "Authorization" (str "Bearer " api-key)))
        resp (http/post
              (str base-url "/v1/search/fts")
              {:headers headers
               :body (clj->json payload)
               :content-type :json
               :accept :json
               :as :json
               :throw-exceptions false
               :socket-timeout 30000
               :connection-timeout 30000})]
    (if (= 200 (:status resp))
      (get-in resp [:body :rows] [])
      [])))

(defn federated-fts
  [{:keys [tenant-id q role projects kinds limit]}]
  (let [resolved-projects (expand-projects tenant-id role projects)
        resolved-kinds (seq kinds)
        search-args (if resolved-kinds
                      (for [project resolved-projects
                            kind resolved-kinds]
                        {:q q :limit limit :project project :kind kind})
                      (for [project resolved-projects]
                        {:q q :limit limit :project project}))
        rows (mapcat call-openplanner-fts search-args)]
    {:projects resolved-projects
     :count (count rows)
     :rows (take (or limit 10) rows)}))

(defn call-proxx-chat
  [{:keys [messages model system-prompt]}]
  (let [base-url (config/proxx-url)
        auth-token (config/proxx-auth-token)
        headers (cond-> {"Content-Type" "application/json"}
                  (not (str/blank? auth-token))
                  (assoc "Authorization" (str "Bearer " auth-token)))
        payload {:model (or model (config/proxx-default-model))
                 :messages messages
                 :stream false
                 :temperature 0.2}
        payload (cond-> payload
                  system-prompt (assoc :system_prompt system-prompt))
        resp (http/post
              (str base-url "/v1/chat/completions")
              {:headers headers
               :body (clj->json payload)
               :content-type :json
               :accept :json
               :as :json
               :throw-exceptions false
               :socket-timeout 60000
               :connection-timeout 30000})]
    (if (= 200 (:status resp))
      (or (get-in resp [:body :choices 0 :message :content])
          (get-in resp [:body :choices 0 :text])
          "")
      nil)))

(defn format-search-context
  [rows]
  (->> rows
       (map-indexed (fn [idx row]
                      (str "[" (inc idx) "] project=" (:project row)
                           " kind=" (:kind row)
                           " source=" (:source row) "\n"
                           (:snippet row))))
       (str/join "\n\n")))

(defn grounded-summary
  [projects rows]
  (if-not (seq rows)
    "No relevant context found in the selected lakes."
    (let [by-project (frequencies (map :project rows))
          by-source (frequencies (map :source rows))
          by-kind (frequencies (map :kind rows))
          latest-ts (->> rows (map :ts) (remove nil?) sort last)
          project-summary (->> by-project (map (fn [[k v]] (str k " (" v ")"))) (str/join ", "))
          source-summary (->> by-source (map (fn [[k v]] (str k " (" v ")"))) (str/join ", "))
          kind-summary (->> by-kind (map (fn [[k v]] (str k " (" v ")"))) (str/join ", "))
          snippet-summary (->> rows (take 3) (map :snippet) (remove str/blank?) (str/join " | "))]
      (str "Found " (count rows) " result(s) across " (count projects) " lake(s): " project-summary ". "
           "Sources: " source-summary ". "
           "Kinds: " kind-summary ". "
           (when latest-ts (str "Latest event: " latest-ts ". "))
           (when (not (str/blank? snippet-summary))
             (str "Representative snippets: " snippet-summary))))))

(defn request-body->map
  "Return a request body as a Clojure map regardless of whether muuntaja decoded it.
   Falls back to parsing raw JSON from InputStream/Reader/string bodies."
  [request]
  (let [body (or (:body-params request) (:body request))]
    (cond
      (map? body)
      body

      (string? body)
      (or (json/parse-string body keyword) {})

      (instance? InputStream body)
      (let [s (slurp body)]
        (if (str/blank? s)
          {}
          (json/parse-string s keyword)))

      (instance? Reader body)
      (let [s (slurp body)]
        (if (str/blank? s)
          {}
          (json/parse-string s keyword)))

      :else
      {})))

(def text-preview-extensions
  #{".md" ".markdown" ".txt" ".rst" ".org" ".adoc"
    ".clj" ".cljs" ".cljc" ".edn" ".ts" ".tsx" ".js" ".jsx" ".py" ".sh" ".sql"
    ".json" ".jsonl" ".yaml" ".yml" ".toml" ".ini" ".cfg" ".conf" ".env" ".properties"
    ".html" ".css" ".xml" ".csv" ".tsv"})

(defn- workspace-root-file []
  (let [base (.getCanonicalFile (io/file (config/workspace-path)))
        base-packages (.getCanonicalFile (io/file base "packages"))
        devel-root (.getCanonicalFile (io/file base "devel"))
        devel-packages (.getCanonicalFile (io/file devel-root "packages"))]
    (cond
      ;; Already mounted at the devel workspace root
      (.exists ^File base-packages)
      base

      ;; Mounted at $HOME, but devel exists beneath it
      (.exists ^File devel-packages)
      devel-root

      :else
      base)))

(defn- resolve-workspace-file
  [path]
  (let [root (workspace-root-file)
        candidate (if (or (nil? path) (str/blank? path))
                    root
                    (let [p (str path)]
                      (if (.isAbsolute (File. p))
                        (io/file p)
                        (io/file root p))))
        canonical (.getCanonicalFile ^File candidate)
        root-path (.getPath root)
        candidate-path (.getPath canonical)]
    (when (or (= candidate-path root-path)
              (str/starts-with? candidate-path (str root-path File/separator)))
      canonical)))

(defn- rel-workspace-path
  [^File file]
  (let [root (workspace-root-file)
        root-path (.toPath root)
        file-path (.toPath (.getCanonicalFile file))]
    (if (= (.toString root-path) (.toString file-path))
      ""
      (str (.relativize root-path file-path)))))

(defn- file-ext
  [name]
  (let [n (str/lower-case (or name ""))
        idx (.lastIndexOf n ".")]
    (if (neg? idx) "" (subs n idx))))

(defn- text-previewable?
  [^File file]
  (let [ext (file-ext (.getName file))]
    (contains? text-preview-extensions ext)))

(defn browse-path-handler
  [request]
  (let [requested (or (-> request :query-params :path)
                      (get (:query-params request) "path"))
        target (resolve-workspace-file requested)]
    (println "[BROWSE] requested=" requested "query-params=" (:query-params request) "target=" (some-> target .getPath))
    (cond
      (nil? target)
      {:status 400 :body {:error "path must stay within workspace root"}}

      (not (.exists ^File target))
      {:status 404 :body {:error "path not found"}}

      (not (.isDirectory ^File target))
      {:status 400 :body {:error "path is not a directory"}}

      :else
      (let [entries (->> (or (.listFiles ^File target) (into-array File []))
                         (sort-by (fn [^File f] [(if (.isDirectory f) 0 1) (.toLowerCase (.getName f))]))
                         (map (fn [^File f]
                                {:name (.getName f)
                                 :path (rel-workspace-path f)
                                 :type (if (.isDirectory f) "dir" "file")
                                 :size (when (.isFile f) (.length f))
                                 :previewable (and (.isFile f) (text-previewable? f))}))
                         vec)]
        {:status 200
         :body {:workspace_root (str (workspace-root-file))
                :current_path (rel-workspace-path target)
                :entries entries}}))))

(defn preview-file-handler
  [request]
  (let [requested (or (-> request :query-params :path)
                      (get (:query-params request) "path"))
        target (resolve-workspace-file requested)]
    (cond
      (nil? target)
      {:status 400 :body {:error "path must stay within workspace root"}}

      (not (.exists ^File target))
      {:status 404 :body {:error "file not found"}}

      (not (.isFile ^File target))
      {:status 400 :body {:error "path is not a file"}}

      (not (text-previewable? target))
      {:status 400 :body {:error "file is not previewable as text"}}

      :else
      (let [content (slurp target)
            limit 12000
            truncated (> (count content) limit)]
        {:status 200
         :body {:path (rel-workspace-path target)
                :size (.length ^File target)
                :truncated truncated
                :content (if truncated (subs content 0 limit) content)}}))))

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
     :body (map (fn [s]
                  {:source_id (uuid-str (:source_id s))
                   :tenant_id (:tenant_id s)
                   :driver_type (:driver_type s)
                   :name (:name s)
                   :config (json->clj (:config s))
                   :state (json->clj (:state s))
                   :collections (json->clj (:collections s))
                   :last_scan_at (str (:last_scan_at s))
                   :last_error (:last_error s)
                   :enabled (:enabled s)
                   :created_at (str (:created_at s))
                   :updated_at (str (:updated_at s))})
                sources)}))

(defn create-source-handler
  [request]
  (let [tenant-id (get-tenant-id request)
        body (request-body->map request)
        ;; Handle both string and keyword keys
        driver-type (or (:driver_type body) (:driver-type body))
        name (:name body)
        config (or (:config body) {})
        collections (or (:collections body) ["devel_docs"])
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
     :body {:source_id (uuid-str (:source_id source))
            :tenant_id (:tenant_id source)
            :driver_type (:driver_type source)
            :name (:name source)
            :config (json->clj (:config source))
            :state (json->clj (:state source))
            :enabled (:enabled source)
            :created_at (str (:created_at source))}}))

(defn get-source-handler
  [request]
  (let [tenant-id (get-tenant-id request)
        source-id (-> request :path-params :source_id)
        source (db/get-source source-id tenant-id)]
    (if source
      {:status 200
       :body {:source_id (uuid-str (:source_id source))
              :tenant_id (:tenant_id source)
              :driver_type (:driver_type source)
              :name (:name source)
              :config (json->clj (:config source))
              :state (json->clj (:state source))
              :collections (json->clj (:collections source))
              :last_scan_at (str (:last_scan_at source))
              :last_error (:last_error source)
              :enabled (:enabled source)
              :created_at (str (:created_at source))
              :updated_at (str (:updated_at source))}}
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
     :body (map (fn [j]
                  {:job_id (uuid-str (:job_id j))
                   :source_id (uuid-str (:source_id j))
                   :tenant_id (:tenant_id j)
                   :status (:status j)
                   :total_files (:total_files j)
                   :processed_files (:processed_files j)
                   :failed_files (:failed_files j)
                   :skipped_files (:skipped_files j)
                   :chunks_created (:chunks_created j)
                   :started_at (str (:started_at j))
                   :completed_at (str (:completed_at j))
                   :error_message (:error_message j)
                   :created_at (str (:created_at j))})
                jobs)}))

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
          ;; Start job in background
          (worker/queue-job! job-id source)
          {:status 200
           :body {:job_id job-id
                  :source_id source-id
                  :tenant_id tenant-id
                  :status (:status job)
                  :created_at (str (:created_at job))}})))))

(defn get-job-handler
  [request]
  (let [tenant-id (get-tenant-id request)
        job-id (-> request :path-params :job_id)
        job (db/get-job job-id tenant-id)]
    (if job
      {:status 200
       :body {:job_id (uuid-str (:job_id job))
              :source_id (uuid-str (:source_id job))
              :tenant_id (:tenant_id job)
              :status (:status job)
              :total_files (:total_files job)
              :processed_files (:processed_files job)
              :failed_files (:failed_files job)
              :skipped_files (:skipped_files job)
              :chunks_created (:chunks_created job)
              :started_at (str (:started_at job))
              :completed_at (str (:completed_at job))
              :error_message (:error_message job)
              :created_at (str (:created_at job))}}
      {:status 404
       :body {:error "Job not found"}})))

(defn cancel-job-handler
  [request]
  (let [tenant-id (get-tenant-id request)
        job-id (-> request :path-params :job_id)
        job (db/get-job job-id tenant-id)]
    (if (and job (= (:status job) "running"))
      (do
        (db/update-job! job-id {:status "cancelled" :completed_at (java.sql.Timestamp/from (java.time.Instant/now))})
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
            answer (grounded-summary projects rows)]
        {:status 200
         :body {:projects projects
                :count (:count result)
                :rows rows
                :answer answer}}))))

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

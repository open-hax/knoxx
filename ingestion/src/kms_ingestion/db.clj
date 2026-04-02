(ns kms-ingestion.db
  "Database connection pool and queries."
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.connection :as connection]
   [next.jdbc.result-set :as rs]
   [kms-ingestion.config :as config])
  (:import
   [com.zaxxer.hikari HikariDataSource]))

(defonce ^:private datasource (atom nil))

(defn parse-database-url
  "Parse DATABASE_URL and convert to JDBC format.
   Handles both postgresql://user:pass@host:port/db and jdbc:postgresql://... formats."
  [url]
  (cond
    ;; Already JDBC format
    (.startsWith url "jdbc:")
    url
    
    ;; Parse postgresql://user:pass@host:port/db format
    (.startsWith url "postgresql://")
    (let [;; Extract parts: postgresql://user:pass@host:port/db
          without-scheme (subs url 13) ; Remove "postgresql://"
          [credentials-and-host path] (.split without-scheme "/" 2)
          [credentials host-port] (.split credentials-and-host "@")
          [user pass] (if (.contains credentials ":")
                        (.split credentials ":" 2)
                        [credentials ""])
          [host port] (if (.contains host-port ":")
                        (.split host-port ":" 2)
                        [host-port "5432"])]
      (format "jdbc:postgresql://%s:%s/%s?user=%s&password=%s" 
              host port path user pass))
    
    :else
    (str "jdbc:" url)))

(defn get-ds
  "Get the current datasource."
  []
  @datasource)

(defn init!
  "Initialize the database connection pool."
  []
  (let [db-url (config/database-url)
        jdbc-url (parse-database-url db-url)
        ds (connection/->pool
             HikariDataSource
             {:jdbcUrl jdbc-url
              :maximumPoolSize 10
              :minimumIdle 2})]
    (reset! datasource ds)
    (println "Database pool initialized")))

(defn execute!
  "Execute a SQL statement."
  [sql & params]
  (jdbc/execute! (get-ds) (into [sql] params)
                 {:return-keys true
                  :builder-fn rs/as-unqualified-maps}))

(defn query
  "Query for multiple rows."
  [sql & params]
  (jdbc/execute! (get-ds) (into [sql] params)
                 {:builder-fn rs/as-unqualified-maps}))

(defn query-one
  "Query for a single row."
  [sql & params]
  (jdbc/execute-one! (get-ds) (into [sql] params)
                     {:builder-fn rs/as-unqualified-maps}))

;; ============================================================
;; Ingestion Sources
;; ============================================================

(defn list-sources
  "List all ingestion sources for a tenant."
  [tenant-id]
  (query "SELECT * FROM ingestion_sources WHERE tenant_id = ? ORDER BY created_at DESC" tenant-id))

(defn get-source
  "Get a source by ID."
  [source-id tenant-id]
  (query-one "SELECT * FROM ingestion_sources WHERE source_id = ?::uuid AND tenant_id = ?"
             source-id tenant-id))

(defn create-source!
  "Create a new ingestion source."
  [{:keys [tenant-id driver-type name config collections file-types include-patterns exclude-patterns]}]
  (query-one
   "INSERT INTO ingestion_sources (tenant_id, driver_type, name, config, collections, file_types, include_patterns, exclude_patterns)
    VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb)
    RETURNING *"
   tenant-id driver-type name
   (cheshire.core/generate-string config)
   (cheshire.core/generate-string (or collections ["devel_docs"]))
   (when file-types (cheshire.core/generate-string file-types))
   (when include-patterns (cheshire.core/generate-string include-patterns))
   (when exclude-patterns (cheshire.core/generate-string exclude-patterns))))

(defn delete-source!
  "Delete a source."
  [source-id tenant-id]
  (query-one "DELETE FROM ingestion_sources WHERE source_id = ?::uuid AND tenant_id = ? RETURNING source_id"
             source-id tenant-id))

;; ============================================================
;; Ingestion Jobs
;; ============================================================

(defn list-jobs
  "List jobs with optional filters."
  [{:keys [tenant-id source-id status limit]}]
  (let [sql (str "SELECT * FROM ingestion_jobs WHERE tenant_id = ?"
                 (when source-id " AND source_id = ?::uuid")
                 (when status " AND status = ?")
                 " ORDER BY created_at DESC LIMIT ?")
        params (filter some? [tenant-id source-id status (or limit 50)])]
    (apply query sql params)))

(defn get-job
  "Get a job by ID."
  [job-id tenant-id]
  (query-one "SELECT * FROM ingestion_jobs WHERE job_id = ?::uuid AND tenant_id = ?"
             job-id tenant-id))

(defn create-job!
  "Create a new ingestion job."
  [source-id tenant-id config]
  (query-one
   "INSERT INTO ingestion_jobs (source_id, tenant_id, config) VALUES (?::uuid, ?, ?::jsonb) RETURNING *"
   source-id tenant-id (cheshire.core/generate-string config)))

(defn update-job!
  "Update job status and progress."
  [job-id updates]
  (let [set-clauses (for [[k _] updates]
                      (str (name k) " = ?"))
        sql (str "UPDATE ingestion_jobs SET " (clojure.string/join ", " set-clauses)
                 " WHERE job_id = ?::uuid RETURNING *")
        params (concat (vals updates) [job-id])]
    (apply query-one sql params)))

;; ============================================================
;; File State
;; ============================================================

(defn get-existing-hashes
  "Get all existing file hashes for a source."
  [source-id]
  (into {}
        (map (juxt :file_id :content_hash))
        (query "SELECT file_id, content_hash FROM ingestion_file_state WHERE source_id = ?::uuid"
               source-id)))

(defn upsert-file-state!
  "Insert or update file state."
  [{:keys [file-id source-id tenant-id path content-hash status chunks collections metadata]}]
  (query-one
   "INSERT INTO ingestion_file_state (file_id, source_id, tenant_id, path, content_hash, status, chunks, collections, metadata, last_ingested_at)
    VALUES (?, ?::uuid, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, NOW())
    ON CONFLICT (file_id, source_id) DO UPDATE SET
      content_hash = EXCLUDED.content_hash,
      status = EXCLUDED.status,
      chunks = EXCLUDED.chunks,
      collections = EXCLUDED.collections,
      last_ingested_at = NOW(),
      updated_at = NOW()
    RETURNING *"
   file-id source-id tenant-id path content-hash (or status "ingested")
   chunks
   (cheshire.core/generate-string (or collections ["devel_docs"]))
   (when metadata (cheshire.core/generate-string metadata))))

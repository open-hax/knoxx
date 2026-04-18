(ns kms-ingestion.db
  "Database connection pool and queries."
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
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
    (.startsWith url "jdbc:")
    url
    (.startsWith url "postgresql://")
    (let [without-scheme (subs url 13)
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

(defn get-ds [] @datasource)

(defn init!
  []
  (let [db-url  (config/database-url)
        jdbc-url (parse-database-url db-url)
        ds (connection/->pool
             HikariDataSource
             {:jdbcUrl jdbc-url
              :maximumPoolSize 10
              :minimumIdle 2})]
    (reset! datasource ds)
    (jdbc/execute! ds [(str
                        "CREATE EXTENSION IF NOT EXISTS pgcrypto;"
                        "CREATE TABLE IF NOT EXISTS tenants ("
                        " tenant_id VARCHAR(64) PRIMARY KEY,"
                        " name VARCHAR(256) NOT NULL,"
                        " domains JSONB DEFAULT '[]',"
                        " config JSONB DEFAULT '{}',"
                        " created_at TIMESTAMP DEFAULT NOW()"
                        ");"
                        "CREATE TABLE IF NOT EXISTS ingestion_sources ("
                        " source_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),"
                        " tenant_id TEXT NOT NULL REFERENCES tenants(tenant_id),"
                        " driver_type TEXT NOT NULL,"
                        " name TEXT NOT NULL,"
                        " config JSONB NOT NULL DEFAULT '{}',"
                        " collections JSONB DEFAULT '[]',"
                        " file_types JSONB DEFAULT '[]',"
                        " include_patterns JSONB DEFAULT '[]',"
                        " exclude_patterns JSONB DEFAULT '[]',"
                        " state JSONB DEFAULT '{}',"
                        " last_scan_at TIMESTAMPTZ,"
                        " last_error TEXT,"
                        " enabled BOOLEAN NOT NULL DEFAULT true,"
                        " created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),"
                        " updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()"
                        ");"
                        "CREATE INDEX IF NOT EXISTS idx_ingestion_sources_tenant ON ingestion_sources(tenant_id);"
                        "CREATE INDEX IF NOT EXISTS idx_ingestion_sources_type ON ingestion_sources(driver_type);"
                        "CREATE TABLE IF NOT EXISTS ingestion_jobs ("
                        " job_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),"
                        " source_id UUID NOT NULL REFERENCES ingestion_sources(source_id),"
                        " tenant_id TEXT NOT NULL,"
                        " status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'running', 'completed', 'failed', 'cancelled')),"
                        " total_files INTEGER DEFAULT 0,"
                        " processed_files INTEGER DEFAULT 0,"
                        " failed_files INTEGER DEFAULT 0,"
                        " skipped_files INTEGER DEFAULT 0,"
                        " chunks_created INTEGER DEFAULT 0,"
                        " started_at TIMESTAMPTZ,"
                        " completed_at TIMESTAMPTZ,"
                        " error_message TEXT,"
                        " config JSONB DEFAULT '{}',"
                        " created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()"
                        ");"
                        "CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_source ON ingestion_jobs(source_id);"
                        "CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_status ON ingestion_jobs(status);"
                        "CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_tenant ON ingestion_jobs(tenant_id);"
                        "CREATE TABLE IF NOT EXISTS ingestion_file_state ("
                        " file_id TEXT,"
                        " source_id UUID NOT NULL REFERENCES ingestion_sources(source_id),"
                        " tenant_id TEXT NOT NULL,"
                        " path TEXT NOT NULL,"
                        " content_hash TEXT,"
                        " status TEXT NOT NULL DEFAULT 'ingested' CHECK (status IN ('pending', 'ingested', 'failed', 'deleted')),"
                        " chunks INTEGER DEFAULT 0,"
                        " collections JSONB DEFAULT '[]',"
                        " last_ingested_at TIMESTAMPTZ,"
                        " error_message TEXT,"
                        " metadata JSONB DEFAULT '{}',"
                        " created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),"
                        " updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),"
                        " PRIMARY KEY (file_id, source_id)"
                        ");"
                        "ALTER TABLE ingestion_file_state ALTER COLUMN content_hash DROP NOT NULL;"
                        "CREATE INDEX IF NOT EXISTS idx_ingestion_file_state_source ON ingestion_file_state(source_id);"
                        "CREATE INDEX IF NOT EXISTS idx_ingestion_file_state_hash ON ingestion_file_state(content_hash);"
                        "CREATE INDEX IF NOT EXISTS idx_ingestion_file_state_status ON ingestion_file_state(status);"
                        ;; Epistemic records table
                        "CREATE TABLE IF NOT EXISTS epistemic_records ("
                        " id UUID PRIMARY KEY DEFAULT gen_random_uuid(),"
                        " kind TEXT NOT NULL CHECK (kind IN ('fact','obs','inference','attestation','judgment','actor-binding')),"
                        " tenant_id TEXT NOT NULL REFERENCES tenants(tenant_id),"
                        " org_id TEXT,"
                        " actor_id TEXT,"
                        " contract_id TEXT,"
                        " causedby UUID,"
                        " p NUMERIC(4,3) CHECK (p >= 0 AND p <= 1),"
                        " payload JSONB NOT NULL,"
                        " src TEXT,"
                        " asserted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),"
                        " created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()"
                        ");"
                        "CREATE INDEX IF NOT EXISTS idx_epistemic_kind     ON epistemic_records(kind);"
                        "CREATE INDEX IF NOT EXISTS idx_epistemic_tenant   ON epistemic_records(tenant_id);"
                        "CREATE INDEX IF NOT EXISTS idx_epistemic_actor    ON epistemic_records(actor_id);"
                        "CREATE INDEX IF NOT EXISTS idx_epistemic_contract ON epistemic_records(contract_id);"
                        "CREATE INDEX IF NOT EXISTS idx_epistemic_causedby ON epistemic_records(causedby);")])
    (println "Database pool initialized")))

(defn execute!
  [sql & params]
  (jdbc/execute! (get-ds) (into [sql] params)
                 {:return-keys true
                  :builder-fn rs/as-unqualified-maps}))

(defn query
  [sql & params]
  (jdbc/execute! (get-ds) (into [sql] params)
                 {:builder-fn rs/as-unqualified-maps}))

(defn query-one
  [sql & params]
  (jdbc/execute-one! (get-ds) (into [sql] params)
                     {:builder-fn rs/as-unqualified-maps}))

(defn ensure-tenant!
  [tenant-id name]
  (query-one
   "INSERT INTO tenants (tenant_id, name, domains, config)
    VALUES (?, ?, '[]'::jsonb, '{}'::jsonb)
    ON CONFLICT (tenant_id) DO UPDATE SET name = EXCLUDED.name
    RETURNING tenant_id"
   tenant-id name))

;; ============================================================
;; Epistemic Records
;; ============================================================

(defn insert-epistemic-record!
  "Insert a single validated epistemic record into the store.
   `rec` is a map conforming to the EpistemicRecord union in epistemic.cljc.
   `tenant-id` is the owning tenant (required).
   Optional provenance keys: :actor-id :org-id :contract-id :causedby."
  [tenant-id rec & {:keys [actor-id org-id contract-id causedby]}]
  (let [kind     (name (:kind rec))
        p        (:p rec)
        src      (str (:src rec))
        payload  (json/generate-string rec)]
    (query-one
     "INSERT INTO epistemic_records
        (kind, tenant_id, org_id, actor_id, contract_id, causedby, p, payload, src)
      VALUES (?, ?, ?, ?, ?, ?::uuid, ?, ?::jsonb, ?)
      RETURNING *"
     kind tenant-id org-id actor-id contract-id causedby p payload src)))

(defn insert-epistemic-records!
  "Batch insert a seq of epistemic records for a tenant.
   Returns a seq of inserted rows."
  [tenant-id records & {:as opts}]
  (mapv #(apply insert-epistemic-record! tenant-id % (apply concat opts)) records))

(defn query-epistemic
  "Query epistemic records by kind and/or actor for a tenant.
   Options: :kind (string or keyword), :actor-id, :contract-id, :limit"
  [tenant-id & {:keys [kind actor-id contract-id limit]}]
  (let [clauses (cond-> ["tenant_id = ?"]
                  kind        (conj "kind = ?")
                  actor-id    (conj "actor_id = ?")
                  contract-id (conj "contract_id = ?"))
        params  (cond-> [tenant-id]
                  kind        (conj (name kind))
                  actor-id    (conj actor-id)
                  contract-id (conj contract-id))
        sql     (str "SELECT * FROM epistemic_records WHERE "
                     (str/join " AND " clauses)
                     " ORDER BY asserted_at DESC LIMIT ?")]
    (apply query sql (conj params (or limit 100)))))

(defn get-epistemic-record
  "Fetch a single epistemic record by UUID."
  [id]
  (query-one "SELECT * FROM epistemic_records WHERE id = ?::uuid" id))

;; ============================================================
;; Ingestion Sources
;; ============================================================

(defn list-sources [tenant-id]
  (query "SELECT * FROM ingestion_sources WHERE tenant_id = ? ORDER BY created_at DESC" tenant-id))

(defn list-enabled-sources []
  (query "SELECT * FROM ingestion_sources WHERE enabled = true ORDER BY created_at ASC"))

(defn get-source [source-id tenant-id]
  (query-one "SELECT * FROM ingestion_sources WHERE source_id = ?::uuid AND tenant_id = ?"
             source-id tenant-id))

(defn create-source!
  [{:keys [tenant-id driver-type name config collections file-types include-patterns exclude-patterns]}]
  (query-one
   "INSERT INTO ingestion_sources (tenant_id, driver_type, name, config, collections, file_types, include_patterns, exclude_patterns)
     VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb)
     RETURNING *"
   tenant-id driver-type name
   (json/generate-string config)
   (json/generate-string (or collections ["devel-docs" "devel-code" "devel-config" "devel-data"]))
   (when file-types (json/generate-string file-types))
   (when include-patterns (json/generate-string include-patterns))
   (when exclude-patterns (json/generate-string exclude-patterns))))

(defn mark-source-scanned! [source-id]
  (query-one
   "UPDATE ingestion_sources SET last_scan_at = NOW(), updated_at = NOW() WHERE source_id = ?::uuid RETURNING *"
   source-id))

(defn source-has-active-job? [source-id]
  (boolean
   (query-one
    "SELECT job_id FROM ingestion_jobs WHERE source_id = ?::uuid AND status IN ('pending', 'running') LIMIT 1"
    source-id)))

(defn source-has-file-state? [source-id]
  (boolean
   (query-one
    "SELECT file_id FROM ingestion_file_state WHERE source_id = ?::uuid LIMIT 1"
    source-id)))

(defn delete-source! [source-id tenant-id]
  (query-one "DELETE FROM ingestion_sources WHERE source_id = ?::uuid AND tenant_id = ? RETURNING source_id"
             source-id tenant-id))

;; ============================================================
;; Ingestion Jobs
;; ============================================================

(defn list-jobs [{:keys [tenant-id source-id status limit]}]
  (let [sql (str "SELECT * FROM ingestion_jobs WHERE tenant_id = ?"
                 (when source-id " AND source_id = ?::uuid")
                 (when status " AND status = ?")
                 " ORDER BY created_at DESC LIMIT ?")
        params (filter some? [tenant-id source-id status (or limit 50)])]
    (apply query sql params)))

(defn get-job [job-id tenant-id]
  (query-one "SELECT * FROM ingestion_jobs WHERE job_id = ?::uuid AND tenant_id = ?"
             job-id tenant-id))

(defn get-job-by-id [job-id]
  (query-one "SELECT * FROM ingestion_jobs WHERE job_id = ?::uuid" job-id))

(defn latest-job-for-source [source-id]
  (query-one
   "SELECT * FROM ingestion_jobs WHERE source_id = ?::uuid ORDER BY created_at DESC LIMIT 1"
   source-id))

(defn create-job! [source-id tenant-id config]
  (query-one
   "INSERT INTO ingestion_jobs (source_id, tenant_id, config) VALUES (?::uuid, ?, ?::jsonb) RETURNING *"
   source-id tenant-id (json/generate-string config)))

(defn update-job! [job-id updates]
  (let [set-clauses (for [[k _] updates] (str (name k) " = ?"))
        sql (str "UPDATE ingestion_jobs SET " (str/join ", " set-clauses)
                 " WHERE job_id = ?::uuid RETURNING *")
        params (concat (vals updates) [job-id])]
    (apply query-one sql params)))

;; ============================================================
;; File State
;; ============================================================

(defn get-existing-hashes [source-id]
  (into {}
        (map (juxt :file_id :content_hash))
        (query "SELECT file_id, content_hash FROM ingestion_file_state WHERE source_id = ?::uuid" source-id)))

(defn get-existing-state [source-id]
  (into {}
        (map (fn [row]
               (let [metadata (:metadata row)
                     metadata-map (cond
                                    (nil? metadata) {}
                                    (instance? org.postgresql.util.PGobject metadata)
                                    (json/parse-string (.getValue ^org.postgresql.util.PGobject metadata) true)
                                    (string? metadata)
                                    (json/parse-string metadata true)
                                    (map? metadata) metadata
                                    :else {})]
                 [(:file_id row)
                  {:content_hash (:content_hash row)
                   :path (:path row)
                   :status (:status row)
                   :metadata metadata-map}])))
        (query "SELECT file_id, path, content_hash, status, metadata FROM ingestion_file_state WHERE source_id = ?::uuid AND status != 'pending'"
               source-id)))

(defn reset-orphaned-jobs! []
  (execute!
   "UPDATE ingestion_jobs
      SET status = 'cancelled',
          completed_at = COALESCE(completed_at, NOW()),
          error_message = COALESCE(error_message, 'cancelled on worker restart')
    WHERE status IN ('pending', 'running')"))

(defn reset-failed-files! [source-id]
  (let [count-result (query-one
                      "SELECT COUNT(*) as count FROM ingestion_file_state WHERE source_id = ?::uuid AND status = 'failed'"
                      source-id)
        count (or (:count count-result) 0)]
    (when (pos? count)
      (execute!
       "UPDATE ingestion_file_state
          SET status = 'pending', error_message = NULL, metadata = '{}', updated_at = NOW()
        WHERE source_id = ?::uuid AND status = 'failed'"
       source-id))
    count))

(defn upsert-file-state!
  [{:keys [file-id source-id tenant-id path content-hash status chunks collections metadata]}]
  (query-one
   "INSERT INTO ingestion_file_state (file_id, source_id, tenant_id, path, content_hash, status, chunks, collections, metadata, last_ingested_at)
     VALUES (?, ?::uuid, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, NOW())
     ON CONFLICT (file_id, source_id) DO UPDATE SET
       content_hash = EXCLUDED.content_hash,
       status = EXCLUDED.status,
       chunks = EXCLUDED.chunks,
       collections = EXCLUDED.collections,
       metadata = EXCLUDED.metadata,
       last_ingested_at = NOW(),
       updated_at = NOW()
   RETURNING *"
   file-id source-id tenant-id path content-hash (or status "ingested")
   chunks
   (json/generate-string (or collections [tenant-id]))
   (when metadata (json/generate-string metadata))))

(defn list-file-states-under-path [tenant-id path-prefix]
  (let [normalized (str/trim (or path-prefix ""))]
    (if (str/blank? normalized)
      (query "SELECT path, status, chunks, metadata, last_ingested_at FROM ingestion_file_state WHERE tenant_id = ?" tenant-id)
      (query "SELECT path, status, chunks, metadata, last_ingested_at FROM ingestion_file_state WHERE tenant_id = ? AND (path = ? OR path LIKE ?)"
             tenant-id normalized (str normalized "/%")))))

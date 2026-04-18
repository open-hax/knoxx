(ns kms-ingestion.jobs.worker
  "Job processing worker for ingestion."
  (:require
   [clojure.string :as str]
   [kms-ingestion.config :as config]
   [kms-ingestion.db :as db]
   [kms-ingestion.drivers.local :as local]
   [kms-ingestion.drivers.protocol :as protocol]
   [kms-ingestion.drivers.registry :as registry]
   [kms-ingestion.graph :as graph]
   [kms-ingestion.jobs.control :as control]
   [kms-ingestion.jobs.ingest-support :as support])
  (:import
   [java.sql Timestamp]
   [java.time Duration Instant]))

(defn init-executor! [] (control/init-executor!))
(defn submit-task! [f] (control/submit-task! f))

(declare process-job!)

(defn queue-job! [job-id source]
  (when-not (control/executor-ready?)
    (init-executor!))
  (submit-task! (fn [] (process-job! job-id source))))

(defn- persist-result!
  [source source-id collections result]
  (let [file     (:file result)
        metadata (cond-> {:size (:size file)
                          :modified_at (some-> (:modified-at file) str)}
                   (= :failed (:status result))
                   (assoc :error (:error result)))]
    (db/upsert-file-state!
     {:file-id      (:id file)
      :source-id    source-id
      :tenant-id    (:tenant_id source)
      :path         (:path file)
      :content-hash (:content-hash file)
      :status       (if (= :success (:status result)) "ingested" "failed")
      :chunks       (if (= :success (:status result)) (:chunks result) 0)
      :collections  collections
      :metadata     metadata})))

(defn- ingest-file
  [job-id driver file-meta {:keys [tenant-id source-id ragussy-url collections
                                    use-openplanner? openplanner-url openplanner-api-key
                                    graph-context driver-type]}]
  (when (config/ingest-throttle-enabled?)
    (let [cpu-cores    (control/container-cpu-cores)
          target-cores (* (config/ingest-max-load-per-core) (control/available-cores))]
      (when (and cpu-cores (> cpu-cores target-cores))
        (Thread/sleep (long (control/control-delay-ms cpu-cores))))))
  (try
    (let [file-data         (protocol/extract driver (:id file-meta))
          content-hash      (when (:content file-data) (local/sha256 (:id file-meta)))
          file-meta-hash    (assoc file-meta :content-hash content-hash)
          payload-file      (assoc file-data :content-hash content-hash)
          pi-sessions?      (= driver-type "pi-sessions")
          promptdb?         (= driver-type "promptdb")
          ingest-result
          (cond
            ;; ── promptdb: epistemic records bypass chunking entirely ──
            promptdb?
            (let [records (:epistemic-records file-data)]
              (if (seq records)
                (do
                  (db/insert-epistemic-records! tenant-id records)
                  {:status :success :chunks (count records)})
                {:status :failed :error "promptdb driver returned no epistemic-records"}))

            ;; ── pi-sessions: structured events via openplanner ────────
            pi-sessions?
            (support/ingest-pi-session-via-openplanner!
             job-id tenant-id source-id openplanner-url openplanner-api-key payload-file)

            ;; ── everything else: normal text-chunking path ────────────
            (:content file-data)
            (if use-openplanner?
              (support/ingest-via-openplanner!
               job-id tenant-id source-id openplanner-url openplanner-api-key payload-file graph-context)
              (support/ingest-via-ragussy! ragussy-url collections payload-file))

            :else
            {:status :failed :error "no content"})]
      (assoc ingest-result :file file-meta-hash))
    (catch Exception e
      (when (and (= driver-type "local") (config/openplanner-url))
        (control/note-openplanner-failure! job-id (.getMessage e)))
      {:status :failed :file file-meta :error (.getMessage e)})))

(defn process-job!
  "Process an ingestion job."
  [job-id source]
  (control/log! (str "[JOB " job-id "] Starting..."))
  (try
    (db/update-job! job-id {:status "running"
                            :started_at (Timestamp/from (Instant/now))})
    (let [source-id       (str (:source_id source))
          tenant-id       (:tenant_id source)
          job-row         (db/get-job-by-id job-id)
          job-config      (or (support/parse-jsonish (:config job-row)) {})
          full-scan?      (true? (or (:full_scan job-config) (:full-scan job-config)))
          watch-paths     (vec (or (:watch_paths job-config) (:watch-paths job-config) []))
          deleted-paths   (vec (or (:deleted_paths job-config) (:deleted-paths job-config) []))
          existing-state  (db/get-existing-state source-id)
          existing-hashes (into {} (map (fn [[fid row]] [fid (:content_hash row)]) existing-state))
          driver-type     (:driver_type source)
          driver-config   (or (support/parse-jsonish (:config source)) {})
          driver          (registry/create-driver driver-type driver-config)]
      (control/log! (str "[JOB " job-id "] Created " driver-type " driver"))
      (let [reset-count (db/reset-failed-files! source-id)]
        (when (pos? reset-count)
          (control/log! (str "[JOB " job-id "] Reset " reset-count " failed files for retry"))))
      (when-let [state (support/parse-jsonish (:state source))]
        (.set-state driver state))
      (control/log! (str "[JOB " job-id "] Starting discovery..."
                         (when full-scan? " full-scan=true")
                         (when (seq watch-paths)
                           (str " incremental=" (count watch-paths) " changed, deleted=" (count deleted-paths)))))
      (let [discover-opts  (cond-> {:existing-state existing-state}
                             (seq watch-paths) (assoc :include-patterns watch-paths))
            root-path      (or (:root-path driver-config) (:root_path driver-config))
            streaming?     (= driver-type "local")
            streamed-files (when streaming? (local/stream-files root-path discover-opts))
            discovery      (when-not streaming? (.discover driver discover-opts))
            file-entries   (if streaming? (vec streamed-files) (vec (:files discovery)))
            total-files    (+ (count file-entries) (count deleted-paths))
            graph-context  (graph/index-context existing-state file-entries)]
        (when-not streaming?
          (control/log! (str "[JOB " job-id "] Discovery: "
                             (:new-files discovery) " new, "
                             (:changed-files discovery) " changed, "
                             (:unchanged-files discovery) " unchanged")))
        (db/update-job! job-id {:total_files total-files})
        ;; Orphan detection
        (when (seq existing-state)
          (let [orphaned-ids (atom [])]
            (doseq [[file-id _] existing-state]
              (when-not (.exists (java.io.File. (str file-id)))
                (swap! orphaned-ids conj file-id)))
            (when (seq @orphaned-ids)
              (control/log! (str "[JOB " job-id "] " (count @orphaned-ids) " orphaned files"))
              (doseq [file-id @orphaned-ids]
                (db/upsert-file-state!
                 {:file-id      file-id
                  :source-id    source-id
                  :tenant-id    tenant-id
                  :path         (or (:path (get existing-state file-id)) file-id)
                  :content-hash (:content_hash (get existing-state file-id))
                  :status       "deleted"
                  :chunks       0
                  :collections  (or (support/parse-jsonish (:collections source)) [tenant-id])
                  :metadata     {:deleted true}})))))
        (when (seq deleted-paths)
          (support/apply-deleted-paths! source source-id existing-hashes
                                        (or (support/parse-jsonish (:collections source)) [tenant-id])
                                        deleted-paths))
        (let [batch-size         (config/ingest-batch-size)
              batch-parallelism  (config/ingest-batch-parallelism)
              ragussy-url        (config/ragussy-url)
              openplanner-url    (config/openplanner-url)
              openplanner-api-key (config/openplanner-api-key)
              use-openplanner?   (not (str/blank? openplanner-url))
              collections        (or (support/parse-jsonish (:collections source)) [tenant-id])
              ingest-opts        {:tenant-id          tenant-id
                                  :source-id          source-id
                                  :ragussy-url        ragussy-url
                                  :collections        collections
                                  :use-openplanner?   use-openplanner?
                                  :openplanner-url    openplanner-url
                                  :openplanner-api-key openplanner-api-key
                                  :graph-context      graph-context
                                  :driver-type        driver-type}]
          (control/log! (str "[JOB " job-id "] Ingest target: "
                             (cond
                               (= driver-type "promptdb") "epistemic-store"
                               use-openplanner?           "openplanner"
                               :else                      "ragussy")
                             ", batch-size=" batch-size))
          (loop [remaining    (seq file-entries)
                 discovered   0
                 processed    0
                 failed       0
                 chunks-total 0
                 doc-ids      []
                 start-time   (Instant/now)]
            (let [job-status (:status (db/get-job-by-id job-id))]
              (cond
                (= job-status "cancelled")
                (control/log! (str "[JOB " job-id "] Cancel acknowledged"))

                (empty? remaining)
                (let [elapsed (.getSeconds (Duration/between start-time (Instant/now)))]
                  (when (and (config/semantic-edge-build-enabled?) (seq doc-ids) use-openplanner?)
                    (control/log! (str "[JOB " job-id "] Building semantic edges for " (count doc-ids) " docs..."))
                    (let [r (support/build-semantic-edges-incremental! openplanner-url openplanner-api-key doc-ids)]
                      (control/log! (if (= :success (:status r))
                                      (str "[JOB " job-id "] Edges: " (:edges r))
                                      (str "[JOB " job-id "] Edge build failed: " (:error r))))))
                  (control/log! (str "[JOB " job-id "] Done: " processed " ok / " failed " fail / " chunks-total " chunks in " elapsed "s"))
                  (db/update-job! job-id {:status          "completed"
                                          :total_files      (+ discovered (count deleted-paths))
                                          :processed_files  (+ processed (count deleted-paths))
                                          :failed_files     failed
                                          :chunks_created   chunks-total
                                          :completed_at     (Timestamp/from (Instant/now))})
                  (db/mark-source-scanned! source-id))

                :else
                (let [batch           (vec (take batch-size remaining))
                      n               (count batch)
                      p               (min n (max 1 batch-parallelism))
                      results         (control/bounded-future-mapv p #(ingest-file job-id driver % ingest-opts) batch)
                      _               (doseq [r results] (when-not (= driver-type "promptdb") (persist-result! source source-id collections r)))
                      ok              (count (filter #(= :success (:status %)) results))
                      fail            (count (filter #(= :failed  (:status %)) results))
                      new-chunks      (reduce + 0 (map :chunks (filter #(= :success (:status %)) results)))
                      new-doc-ids     (vec (keep :doc-id (filter #(= :success (:status %)) results)))]
                  (control/log! (str "[JOB " job-id "] Batch: ok=" ok " fail=" fail " chunks=" new-chunks))
                  (db/update-job! job-id {:total_files     (+ discovered n (count deleted-paths))
                                          :processed_files  (+ processed ok (count deleted-paths))
                                          :failed_files     (+ failed fail)
                                          :chunks_created   (+ chunks-total new-chunks)})
                  (when (pos? (if (config/ingest-throttle-enabled?) (control/control-delay-ms (control/container-cpu-cores)) 0))
                    (Thread/sleep (long (control/control-delay-ms (control/container-cpu-cores)))))
                  (recur (drop batch-size remaining)
                         (+ discovered n)
                         (+ processed ok)
                         (+ failed fail)
                         (+ chunks-total new-chunks)
                         (into doc-ids new-doc-ids)
                         start-time))))))
      (control/log! (str "[JOB " job-id "] Driver state: " (.get-state driver)))))
    (catch Exception e
      (control/log! (str "[JOB " job-id "] FAILED: " (.getMessage e)))
      (.printStackTrace e)
      (db/update-job! job-id {:status "failed" :error_message (.getMessage e)}))))

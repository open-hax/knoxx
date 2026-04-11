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

(defn init-executor!
  "Initialize the job executor thread pool."
  []
  (control/init-executor!))

(defn submit-task!
  "Submit a task to the executor."
  [f]
  (control/submit-task! f))

(declare process-job!)

(defn queue-job!
  "Queue a job for execution."
  [job-id source]
  (when-not (control/executor-ready?)
    (init-executor!))
  (submit-task! (fn [] (process-job! job-id source))))

(defn- persist-result!
  [source source-id collections result]
  (let [file (:file result)
        metadata (cond-> {:size (:size file)
                          :modified_at (some-> (:modified-at file) str)}
                   (= :failed (:status result))
                   (assoc :error (:error result)))]
    (db/upsert-file-state!
     {:file-id (:id file)
      :source-id source-id
      :tenant-id (:tenant_id source)
      :path (:path file)
      :content-hash (:content-hash file)
      :status (if (= :success (:status result)) "ingested" "failed")
      :chunks (if (= :success (:status result)) (:chunks result) 0)
      :collections collections
      :metadata metadata})))

(defn- ingest-file
  [job-id driver file-meta {:keys [tenant-id source-id ragussy-url collections
                                   use-openplanner? openplanner-url openplanner-api-key
                                   graph-context]}]
  (when (config/ingest-throttle-enabled?)
    (let [cpu-cores (control/container-cpu-cores)
          target-cores (* (config/ingest-max-load-per-core) (control/available-cores))]
      (when (and cpu-cores (> cpu-cores target-cores))
        (let [delay (control/control-delay-ms cpu-cores)]
          (when (pos? delay)
            (Thread/sleep (long delay)))))))
  (try
    (let [file-data (protocol/extract driver (:id file-meta))
          content-hash (when (:content file-data)
                         (local/sha256 (:id file-meta)))
          file-meta-with-hash (assoc file-meta :content-hash content-hash)
          payload-file (assoc file-data :content-hash content-hash)
          ingest-result (if (:content file-data)
                          (if use-openplanner?
                            (support/ingest-via-openplanner! job-id tenant-id source-id openplanner-url openplanner-api-key payload-file graph-context)
                            (support/ingest-via-ragussy! ragussy-url collections payload-file))
                          {:status :failed :error "no content"})]
      (assoc ingest-result :file file-meta-with-hash))
    (catch Exception e
      (when use-openplanner?
        (control/note-openplanner-failure! job-id (.getMessage e)))
      {:status :failed
       :file file-meta
       :error (.getMessage e)})))

(defn process-job!
  "Process an ingestion job."
  [job-id source]
  (control/log! (str "[JOB " job-id "] Starting..."))
  (try
    (db/update-job! job-id {:status "running"
                            :started_at (Timestamp/from (Instant/now))})
    (let [source-id (str (:source_id source))
          tenant-id (:tenant_id source)
          job-row (db/get-job-by-id job-id)
          job-config (or (support/parse-jsonish (:config job-row)) {})
          full-scan? (true? (or (:full_scan job-config) (:full-scan job-config)))
          watch-paths (vec (or (:watch_paths job-config) (:watch-paths job-config) []))
          deleted-paths (vec (or (:deleted_paths job-config) (:deleted-paths job-config) []))
          existing-state (db/get-existing-state source-id)
          ;; For full-scan, we still need existing-state to detect orphans
          existing-hashes (into {} (map (fn [[file-id row]] [file-id (:content_hash row)]) existing-state))
          driver-type (:driver_type source)
          driver-config (or (support/parse-jsonish (:config source)) {})
          driver (registry/create-driver driver-type driver-config)]
      (control/log! (str "[JOB " job-id "] Created " driver-type " driver"))
      ;; Reset failed files so they get retried automatically
      (let [reset-count (db/reset-failed-files! source-id)]
        (when (pos? reset-count)
          (control/log! (str "[JOB " job-id "] Reset " reset-count " failed files for retry"))))
      (when-let [state (support/parse-jsonish (:state source))]
        (.set-state driver state))
      (control/log! (str "[JOB " job-id "] Starting discovery..."
                         (when full-scan? " full-scan=true")
                         (when (seq watch-paths)
                           (str " incremental=" (count watch-paths) " changed, deleted=" (count deleted-paths)))))
      (let [discover-opts (cond-> {:existing-state existing-state}
                            (seq watch-paths) (assoc :include-patterns watch-paths))
            root-path (or (:root-path driver-config) (:root_path driver-config))
            streaming? (= driver-type "local")
            streamed-files (when streaming?
                             (local/stream-files root-path discover-opts))
            discovery (when-not streaming?
                        (.discover driver discover-opts))
            file-entries (if streaming?
                           (vec streamed-files)
                           (vec (:files discovery)))
            total-files (+ (count file-entries) (count deleted-paths))
            graph-context (graph/index-context existing-state file-entries)]
        (when-not streaming?
          (control/log! (str "[JOB " job-id "] Discovery complete: "
                             (:new-files discovery) " new, "
                             (:changed-files discovery) " changed, "
                             (:unchanged-files discovery) " unchanged")))
        (db/update-job! job-id {:total_files total-files})
        ;; Detect orphaned files: files that exist in DB but not on filesystem
        ;; Note: We check file existence directly, not just discovery, because
        ;; discovery only returns new/changed files (unchanged files are skipped)
        (when (seq existing-state)
          (let [orphaned-ids (atom [])]
            (doseq [[file-id _state] existing-state]
              (let [f (java.io.File. (str file-id))]
                (when-not (.exists f)
                  (swap! orphaned-ids conj file-id))))
            (when (seq @orphaned-ids)
              (control/log! (str "[JOB " job-id "] Detected " (count @orphaned-ids) " orphaned files to mark as deleted"))
              (doseq [file-id @orphaned-ids]
                (db/upsert-file-state!
                 {:file-id file-id
                  :source-id source-id
                  :tenant-id tenant-id
                  :path (or (:path (get existing-state file-id)) file-id)
                  :content-hash (:content_hash (get existing-state file-id))
                  :status "deleted"
                  :chunks 0
                  :collections (or (support/parse-jsonish (:collections source)) [tenant-id])
                  :metadata {:deleted true}})))))
        (when (seq deleted-paths)
          (support/apply-deleted-paths! source source-id existing-hashes
                                        (or (support/parse-jsonish (:collections source)) [tenant-id])
                                        deleted-paths))
        (let [files file-entries
              batch-size (config/ingest-batch-size)
              batch-parallelism (config/ingest-batch-parallelism)
              ragussy-url (config/ragussy-url)
              openplanner-url (config/openplanner-url)
              openplanner-api-key (config/openplanner-api-key)
              use-openplanner? (not (str/blank? openplanner-url))
              collections (or (support/parse-jsonish (:collections source)) [tenant-id])
              ingest-opts {:tenant-id tenant-id
                           :source-id source-id
                           :ragussy-url ragussy-url
                           :collections collections
                           :use-openplanner? use-openplanner?
                           :openplanner-url openplanner-url
                           :openplanner-api-key openplanner-api-key
                           :graph-context graph-context}]
          (control/log! (str "[JOB " job-id "] Ingest target: "
                             (if use-openplanner? "openplanner" "ragussy")
                             ", batch-size=" batch-size
                             ", batch-parallelism=" batch-parallelism
                             (when streaming? ", mode=streaming")))
          (loop [remaining (seq files)
                 discovered 0
                 processed 0
                 failed 0
                 chunks-total 0
                 start-time (Instant/now)]
            (let [job-status (:status (db/get-job-by-id job-id))]
              (cond
                (= job-status "cancelled")
                (control/log! (str "[JOB " job-id "] Cancel acknowledged, stopping worker loop"))

                (empty? remaining)
                (let [elapsed (.getSeconds (Duration/between start-time (Instant/now)))]
                  (control/log! (str "[JOB " job-id "] Completed: "
                                     discovered " discovered, "
                                     processed " processed, "
                                     failed " failed, "
                                     (count deleted-paths) " deleted, "
                                     chunks-total " chunks in "
                                     elapsed "s"))
                  (db/update-job! job-id {:status "completed"
                                          :total_files (+ discovered (count deleted-paths))
                                          :processed_files (+ processed (count deleted-paths))
                                          :failed_files failed
                                          :chunks_created chunks-total
                                          :completed_at (Timestamp/from (Instant/now))})
                  (db/mark-source-scanned! source-id))

                :else
                (let [batch (vec (take batch-size remaining))
                      batch-size-actual (count batch)
                      batch-parallelism-actual (min batch-size-actual (max 1 batch-parallelism))
                      results (control/bounded-future-mapv batch-parallelism-actual
                                                           #(ingest-file job-id driver % ingest-opts)
                                                           batch)
                      _ (doseq [result results]
                          (persist-result! source source-id collections result))
                      batch-processed (count (filter #(= :success (:status %)) results))
                      batch-failed (count (filter #(= :failed (:status %)) results))
                      batch-chunks (reduce + 0 (map :chunks (filter #(= :success (:status %)) results)))]
                  (control/log! (str "[JOB " job-id "] Batch done: processed=" batch-processed
                                     " failed=" batch-failed
                                     " chunks=" batch-chunks
                                     " discovered=" batch-size-actual))
                  (db/update-job! job-id {:total_files (+ discovered batch-size-actual (count deleted-paths))
                                          :processed_files (+ processed batch-processed (count deleted-paths))
                                          :failed_files (+ failed batch-failed)
                                          :chunks_created (+ chunks-total batch-chunks)})
                  (let [cpu-cores (control/container-cpu-cores)
                        delay (if (config/ingest-throttle-enabled?)
                                (control/control-delay-ms cpu-cores)
                                0)]
                    (when (pos? delay)
                      (Thread/sleep (long delay))))
                  (recur (drop batch-size remaining)
                         (+ discovered batch-size-actual)
                         (+ processed batch-processed)
                         (+ failed batch-failed)
                         (+ chunks-total batch-chunks)
                         start-time))))))
      (let [driver-state (.get-state driver)]
        (control/log! (str "[JOB " job-id "] Driver state: " driver-state)))))
    (catch Exception e
      (control/log! (str "[JOB " job-id "] FAILED: " (.getMessage e)))
      (.printStackTrace e)
      (db/update-job! job-id {:status "failed"
                              :error_message (.getMessage e)}))))

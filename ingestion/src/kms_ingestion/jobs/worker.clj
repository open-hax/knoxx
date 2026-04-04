(ns kms-ingestion.jobs.worker
  "Job processing worker for ingestion."
   (:require
    [kms-ingestion.db :as db]
    [kms-ingestion.drivers.registry :as registry]
    [kms-ingestion.drivers.local :as local]
    [kms-ingestion.drivers.protocol :as protocol]
    [kms-ingestion.config :as config]
    [cheshire.core :as json]
    [clj-http.client :as http]
     [clojure.string :as str])
  (:import
    [java.lang.management ManagementFactory]
    [java.time Instant Duration]
    [java.sql Timestamp]
    [java.util UUID]
    [java.util.concurrent Executors ExecutorService]))

;; Thread pool for job execution
(defonce ^:private executor (atom nil))

(defn init-executor!
  "Initialize the job executor thread pool."
  []
  (reset! executor (Executors/newFixedThreadPool 4))
  (println "Job executor initialized with 4 threads"))

(defn submit-task!
  "Submit a task to the executor."
  [f]
  (when @executor
    (.submit ^ExecutorService @executor f)))

(defn- log!
  [& xs]
  (apply println xs)
  (flush))

(defn- parse-jsonish
  [value]
  (cond
    (nil? value) nil
    (string? value) (json/parse-string value keyword)
    (map? value) value
    (vector? value) value
    (instance? org.postgresql.util.PGobject value)
    (let [s (.getValue ^org.postgresql.util.PGobject value)]
      (when-not (str/blank? s)
        (json/parse-string s keyword)))
    :else value))

(defn- read-cgroup-cpu-stats
  "Read cgroup CPU stats. Returns {:usage_usec :nr_periods :nr_throttled} or nil."
  []
  (try
    (let [cpu-stat (slurp "/sys/fs/cgroup/cpu.stat")
          usage (when-let [m (re-find #"usage_usec (\d+)" cpu-stat)]
                  (Long/parseLong (second m)))
          periods (when-let [m (re-find #"nr_periods (\d+)" cpu-stat)]
                    (Long/parseLong (second m)))
          throttled (when-let [m (re-find #"nr_throttled (\d+)" cpu-stat)]
                      (Long/parseLong (second m)))]
      (when (and usage periods)
        {:usage_usec usage :nr_periods periods :nr_throttled throttled}))
    (catch Exception _ nil)))

(defn- read-cgroup-cpu-max
  "Read cgroup CPU quota/period. Returns {:quota_us :period_us} or nil."
  []
  (try
    (let [cpu-max (slurp "/sys/fs/cgroup/cpu.max")]
      (if-let [[_ quota period] (re-find #"(\d+) (\d+)" cpu-max)]
        (let [q (Long/parseLong quota)
              p (Long/parseLong period)]
          (when (and (pos? q) (pos? p))
            {:quota_us q :period_us p}))
        nil))
    (catch Exception _ nil)))

(defn- read-cgroup-cpu-max-v1
  "Read cgroup v1 CPU quota/period. Returns {:quota_us :period_us} or nil."
  []
  (try
    (let [quota (slurp "/sys/fs/cgroup/cpu/cpu.cfs_quota_us")
          period (slurp "/sys/fs/cgroup/cpu/cpu.cfs_period_us")]
      (let [q (Long/parseLong (str/trim quota))
            p (Long/parseLong (str/trim period))]
        (when (and (pos? q) (pos? p))
          {:quota_us q :period_us p})))
    (catch Exception _ nil)))

(defonce ^:private cpu-stats (atom {:usage 0 :periods 0 :timestamp 0}))
(defonce ^:private control-state (atom {:ema-cores 0.0}))

(defn- container-cpu-cores
  "Calculate container CPU usage in cores using cgroup stats.
   Returns raw core count (e.g. 3.75 means 375% of 1 core).
   Falls back to host load average if cgroup stats unavailable."
  []
  (if-let [{:keys [usage_usec]} (read-cgroup-cpu-stats)]
    (let [{:keys [usage timestamp]} @cpu-stats
          now (System/currentTimeMillis)
          delta-us (- usage_usec usage)
          delta-ms (- now timestamp)]
      (swap! cpu-stats assoc :usage usage_usec :timestamp now)
      (if (and (pos? delta-us) (pos? delta-ms) (number? timestamp) (pos? timestamp))
        (/ delta-us (* delta-ms 1000.0))
        (do
          (swap! cpu-stats assoc :usage usage_usec :timestamp now)
          0.0)))
    ;; Fallback: host load average
    (try
      (let [bean (ManagementFactory/getOperatingSystemMXBean)
            load (.getSystemLoadAverage bean)]
        (when (and (number? load) (not (neg? load)))
          load))
      (catch Exception _ nil))))

(defn- host-core-count
  "Read total physical core count from /proc/cpuinfo.
   Falls back to Runtime/availableProcessors."
  []
  (try
    (let [lines (line-seq (java.io.BufferedReader. (java.io.FileReader. "/proc/cpuinfo")))
          procs (count (filter #(.startsWith ^String % "processor") lines))]
      (if (pos? procs) procs (.availableProcessors (Runtime/getRuntime))))
    (catch Exception _ (.availableProcessors (Runtime/getRuntime)))))

(defonce ^:private host-cores (delay (host-core-count)))

(defn- available-cores
  []
  @host-cores)

(defn- smoothed-cpu-cores
  []
  (let [current (or (container-cpu-cores) 0.0)
        alpha 0.25
        previous (:ema-cores @control-state)
        ema (+ (* alpha current) (* (- 1.0 alpha) previous))]
    (swap! control-state assoc :ema-cores ema)
    ema))

(defn- control-delay-ms
  "Smoothed pacing controller.
   Keeps a small baseline spacing to avoid burstiness even when under target,
   then ramps up delay steeply as CPU approaches/exceeds the target.
   Target = max-load-per-core * host cores."
  [cpu-cores]
  (let [cores (available-cores)
        target (* (config/ingest-max-load-per-core) cores)
        ratio (if (pos? target) (/ cpu-cores target) 0)]
    (cond
      (< ratio 0.25) 8
      (< ratio 0.50) 15
      (< ratio 0.70) 30
      (< ratio 0.85) 60
      (< ratio 1.00) 120
      (< ratio 1.20) 250
      (< ratio 1.50) 500
      (< ratio 2.00) 1000
      :else 2000)))

(defn- maybe-throttle!
  [job-id]
  (when (config/ingest-throttle-enabled?)
    (let [cpu-cores (smoothed-cpu-cores)
          target-cores (* (config/ingest-max-load-per-core) (available-cores))
          delay (control-delay-ms cpu-cores)]
      (when (and cpu-cores (>= cpu-cores (* 0.7 target-cores)))
        (log! (str "[JOB " job-id "] Throttling: cpu="
                   (format "%.1f" cpu-cores) " cores"
                   " > "
                   (format "%.1f" target-cores) " cores"
                   ", delay=" delay "ms")))
      (when (pos? delay)
        (Thread/sleep (long delay))))))

;; ============================================================
;; Job Execution
;; ============================================================

(declare process-job!)

(defn queue-job!
  "Queue a job for execution."
  [job-id source]
  (when-not @executor
    (init-executor!))
  (submit-task! (fn [] (process-job! job-id source))))

(def doc-exts
  #{".md" ".markdown" ".txt" ".rst" ".org" ".adoc" ".tex" ".bib"})

(def code-exts
  #{".clj" ".cljs" ".cljc" ".edn" ".ts" ".tsx" ".js" ".jsx"
    ".py" ".rb" ".php" ".java" ".kt" ".go" ".rs" ".c" ".cc" ".cpp" ".h" ".hpp"
    ".sh" ".bash" ".zsh" ".fish" ".sql"})

(def config-exts
  #{".json" ".jsonc" ".yaml" ".yml" ".toml" ".ini" ".cfg" ".conf" ".env" ".properties"})

(def data-exts
  #{".jsonl" ".csv" ".tsv" ".parquet"})

(defn- file-ext [path]
  (let [p (str/lower-case (or path ""))
        idx (.lastIndexOf p ".")]
    (if (neg? idx) "" (subs p idx))))

(defn- classify-file-lake [path]
  (let [p (str/lower-case (or path ""))
        ext (file-ext p)
        name (last (str/split p #"/"))]
    (cond
      (or (str/includes? p "/data/")
          (str/includes? p "/datasets/")
          (data-exts ext))
      :data

      (or (str/includes? p "/docs/")
          (str/includes? p "/specs/")
          (str/includes? p "/notes/")
          (str/includes? p "/inbox/")
          (doc-exts ext))
      :docs

      (or (= name "dockerfile")
          (str/starts-with? name ".env")
          (str/includes? p "/config/")
          (str/includes? p "/configs/")
          (config-exts ext))
      :config

      (code-exts ext)
      :code

      :else
      :docs)))

(defn- lake-project [tenant-id lake]
  (str tenant-id "-" (name lake)))

(defn- stable-event-id [source-id file-id]
  (str (UUID/nameUUIDFromBytes (.getBytes (str source-id "|" file-id) "UTF-8"))))

(defn- derive-domain [path]
  (let [parts (->> (str/split (str path) #"/") (remove str/blank?) vec)]
    (cond
      (empty? parts) "general"
      (= 1 (count parts)) "general"
      :else (or (first parts) "general"))))

(defn- absolute-file-id
  [root-path rel-path]
  (.getAbsolutePath (java.io.File. (str root-path) (str rel-path))))

(defn- apply-deleted-paths!
  [source source-id existing-hashes collections deleted-paths]
  (let [driver-config (or (parse-jsonish (:config source)) {})
        root-path (or (:root-path driver-config) (:root_path driver-config))]
    (doseq [rel-path deleted-paths]
      (db/upsert-file-state!
       {:file-id (absolute-file-id root-path rel-path)
        :source-id source-id
        :tenant-id (:tenant_id source)
        :path rel-path
        :content-hash (get existing-hashes (absolute-file-id root-path rel-path))
        :status "deleted"
        :chunks 0
        :collections collections
        :metadata {:deleted true}}))))

(defn- ingest-via-ragussy! [ragussy-url collections file]
  (let [resp (http/post
              (str ragussy-url "/api/rag/ingest/text")
              {:headers {"Content-Type" "application/json"}
               :body (json/generate-string
                      {:text (:content file)
                       :source (:path file)
                       :collection (first collections)})
               :as :json
               :socket-timeout 60000
               :connection-timeout 60000})]
    (if (= 200 (:status resp))
      {:status :success
       :chunks (or (-> resp :body :chunks) 0)
       :target :ragussy}
      {:status :failed
       :error (:body resp)
       :target :ragussy})))

(defn- ingest-via-openplanner! [tenant-id source-id openplanner-url openplanner-api-key file]
  (let [lake (classify-file-lake (:path file))
         project (lake-project tenant-id lake)
         doc-id (stable-event-id source-id (:id file))
         title (:path file)
         payload {:document {:id doc-id
                             :title title
                             :content (:content file)
                             :project project
                             :kind (name lake)
                             :visibility "internal"
                             :source "kms-ingestion"
                             :sourcePath (:path file)
                             :domain (derive-domain (:path file))
                             :language "en"
                             :createdBy "kms-ingestion"
                             :metadata {:tenant_id tenant-id
                                        :lake (name lake)
                                        :path (:path file)
                                        :content_hash (:content-hash file)
                                        :source_id source-id
                                        :file_id (:id file)}}}
         headers (cond-> {"Content-Type" "application/json"}
                   (not (str/blank? openplanner-api-key))
                   (assoc "Authorization" (str "Bearer " openplanner-api-key)))
         resp (http/post
              (str openplanner-url "/v1/documents")
              {:headers headers
                :body (json/generate-string payload)
                :as :json
                :socket-timeout 60000
                :connection-timeout 60000})]
    (if (= 200 (:status resp))
      {:status :success
       :chunks 1
       :target :openplanner
       :lake (name lake)
       :doc-id doc-id}
      {:status :failed
       :error (:body resp)
       :target :openplanner
       :lake (name lake)})))

(defn process-job!
  "Process an ingestion job."
  [job-id source]
  (log! (str "[JOB " job-id "] Starting..."))

  (try
    ;; Update job status
    (db/update-job! job-id {:status "running"
                            :started_at (Timestamp/from (Instant/now))})

    ;; Get existing file hashes
        (let [source-id (str (:source_id source))
              tenant-id (:tenant_id source)
              job-row (db/get-job-by-id job-id)
              job-config (or (parse-jsonish (:config job-row)) {})
              watch-paths (vec (or (:watch_paths job-config) (:watch-paths job-config) []))
              deleted-paths (vec (or (:deleted_paths job-config) (:deleted-paths job-config) []))
              existing-state (db/get-existing-state source-id)
              existing-hashes (into {} (map (fn [[file-id row]] [file-id (:content_hash row)]) existing-state))

          ;; Create driver
          driver-type (:driver_type source)
          driver-config (or (parse-jsonish (:config source)) {})
          driver (registry/create-driver driver-type driver-config)]

      (log! (str "[JOB " job-id "] Created " driver-type " driver"))

      ;; Restore driver state
      (when-let [state (parse-jsonish (:state source))]
        (.set-state driver state))

      ;; Discover/process files
      (log! (str "[JOB " job-id "] Starting discovery..."
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
            total-files (if streaming?
                          (count deleted-paths)
                          (+ (:new-files discovery) (:changed-files discovery) (count deleted-paths)))]

        (when-not streaming?
          (log! (str "[JOB " job-id "] Discovery complete: "
                     (:new-files discovery) " new, "
                     (:changed-files discovery) " changed, "
                     (:unchanged-files discovery) " unchanged")))

        ;; Update job totals
        (db/update-job! job-id {:total_files total-files})
        (when (seq deleted-paths)
          (apply-deleted-paths! source source-id existing-hashes
                                (or (parse-jsonish (:collections source)) ["devel-docs" "devel-code" "devel-config" "devel-data"])
                                deleted-paths))

        (let [files (if streaming? streamed-files (:files discovery))
              batch-size (config/ingest-batch-size)
              ragussy-url (config/ragussy-url)
              openplanner-url (config/openplanner-url)
              openplanner-api-key (config/openplanner-api-key)
              use-openplanner? (not (str/blank? openplanner-url))
              collections (or (parse-jsonish (:collections source)) ["devel-docs" "devel-code" "devel-config" "devel-data"])]
          (log! (str "[JOB " job-id "] Ingest target: " (if use-openplanner? "openplanner" "ragussy")
                     ", batch-size=" batch-size
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
                  (log! (str "[JOB " job-id "] Cancel acknowledged, stopping worker loop"))

                  (empty? remaining)
                  ;; Done
                  (let [elapsed (.getSeconds (Duration/between start-time (Instant/now)))]
                    (log! (str "[JOB " job-id "] Completed: "
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
                   ;; Process batch - throttle per-file for smooth CPU control
                   (let [batch (vec (take batch-size remaining))
                         batch-size-actual (count batch)
                         target-cores (* (config/ingest-max-load-per-core) (available-cores))

                         ;; Ingest each file with per-file CPU throttle
                         results (mapv (fn [file-meta]
                                         (do
                                           (when (config/ingest-throttle-enabled?)
                                             (let [cpu-cores (container-cpu-cores)]
                                               (when (and cpu-cores (> cpu-cores target-cores))
                                                    (let [delay (control-delay-ms cpu-cores)]
                                                   (when (pos? delay)
                                                     (Thread/sleep (long delay)))))))
                                            (try
                                              (let [file-data (protocol/extract driver (:id file-meta))
                                                    content-hash (when (:content file-data)
                                                                   (local/sha256 (:id file-meta)))
                                                    file-meta-with-hash (assoc file-meta :content-hash content-hash)
                                                    ingest-result (if (:content file-data)
                                                                    (if use-openplanner?
                                                                      (ingest-via-openplanner! tenant-id source-id openplanner-url openplanner-api-key (assoc file-data :content-hash content-hash))
                                                                      (ingest-via-ragussy! ragussy-url collections (assoc file-data :content-hash content-hash)))
                                                                    {:status :failed :error "no content"})]
                                                (assoc ingest-result :file file-meta-with-hash))
                                             (catch Exception e
                                               {:status :failed
                                                :file file-meta
                                                :error (.getMessage e)}))))
                                       batch)

                      ;; Update file state
                       _ (doseq [result results]
                           (cond
                             (= :success (:status result))
                             (db/upsert-file-state!
                              {:file-id (-> result :file :id)
                               :source-id source-id
                               :tenant-id (:tenant_id source)
                               :path (-> result :file :path)
                               :content-hash (-> result :file :content-hash)
                               :status "ingested"
                               :chunks (:chunks result)
                               :collections collections
                               :metadata {:size (-> result :file :size)
                                          :modified_at (some-> (-> result :file :modified-at) str)}})

                              (= :failed (:status result))
                              (db/upsert-file-state!
                               {:file-id (-> result :file :id)
                                :source-id source-id
                                :tenant-id (:tenant_id source)
                                :path (-> result :file :path)
                                :content-hash (-> result :file :content-hash)
                                :status "failed"
                                :chunks 0
                                :collections collections
                                :metadata {:error (:error result)
                                           :size (-> result :file :size)
                                           :modified_at (some-> (-> result :file :modified-at) str)}})))

                      ;; Count results
                       batch-processed (count (filter #(= :success (:status %)) results))
                       batch-failed (count (filter #(= :failed (:status %)) results))
                       batch-chunks (reduce + 0 (map :chunks (filter #(= :success (:status %)) results)))]
                    (log! (str "[JOB " job-id "] Batch done: processed=" batch-processed
                               " failed=" batch-failed
                               " chunks=" batch-chunks
                               " discovered=" batch-size-actual))
                    (db/update-job! job-id {:total_files (+ discovered batch-size-actual (count deleted-paths))
                                            :processed_files (+ processed batch-processed (count deleted-paths))
                                             :failed_files (+ failed batch-failed)
                                             :chunks_created (+ chunks-total batch-chunks)})
                      ;; Adaptive delay: scales with CPU usage
                      (let [cpu-cores (container-cpu-cores)
                            delay (if (config/ingest-throttle-enabled?)
                                    (control-delay-ms cpu-cores)
                                    0)]
                        (when (pos? delay)
                          (Thread/sleep (long delay))))

                    (recur (drop batch-size remaining)
                          (+ discovered batch-size-actual)
                           (+ processed batch-processed)
                           (+ failed batch-failed)
                           (+ chunks-total batch-chunks)
                          start-time))))))

        ;; Save driver state
        (let [driver-state (.get-state driver)]
          (log! (str "[JOB " job-id "] Driver state: " driver-state)))))

    (catch Exception e
      (log! (str "[JOB " job-id "] FAILED: " (.getMessage e)))
      (.printStackTrace e)
      (db/update-job! job-id {:status "failed"
                              :error_message (.getMessage e)}))))

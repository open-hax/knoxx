(ns kms-ingestion.jobs.worker
  "Job processing worker for ingestion."
  (:require
   [kms-ingestion.db :as db]
   [kms-ingestion.drivers.registry :as registry]
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

(defn- system-load-per-core
  []
  (let [bean (ManagementFactory/getOperatingSystemMXBean)
        load (.getSystemLoadAverage bean)
        cores (.availableProcessors (Runtime/getRuntime))]
    (when (and (number? load) (not (neg? load)) (pos? cores))
      (/ load cores))))

(defn- maybe-throttle!
  [job-id]
  (when (config/ingest-throttle-enabled?)
    (loop [attempt 0]
      (when-let [load (system-load-per-core)]
        (when (> load (config/ingest-max-load-per-core))
          (when (zero? (mod attempt 5))
            (log! (str "[JOB " job-id "] Throttling: load/core="
                       (format "%.2f" load)
                       " > "
                       (format "%.2f" (config/ingest-max-load-per-core))
                       ", sleeping "
                       (config/ingest-throttle-sleep-ms)
                       "ms")))
          (Thread/sleep (long (config/ingest-throttle-sleep-ms)))
          (recur (inc attempt)))))))

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
              existing-hashes (db/get-existing-hashes source-id)
          
          ;; Create driver
          driver-type (:driver_type source)
          driver-config (or (parse-jsonish (:config source)) {})
          driver (registry/create-driver driver-type driver-config)]
      
      (log! (str "[JOB " job-id "] Created " driver-type " driver"))
      
      ;; Restore driver state
      (when-let [state (parse-jsonish (:state source))]
        (.set-state driver state))
      
      ;; Discover files
      (log! (str "[JOB " job-id "] Starting discovery..."))
      (let [discovery (.discover driver {:existing-hashes existing-hashes})
            total-files (+ (:new-files discovery) (:changed-files discovery))]
        
        (log! (str "[JOB " job-id "] Discovery complete: "
                   (:new-files discovery) " new, "
                   (:changed-files discovery) " changed, "
                   (:unchanged-files discovery) " unchanged"))
        
        ;; Update job totals
        (db/update-job! job-id {:total_files total-files})
        
        (if (zero? total-files)
          (do
            (log! (str "[JOB " job-id "] No new files to process"))
            (db/update-job! job-id {:status "completed"
                                    :completed_at (Timestamp/from (Instant/now))}))
          
          ;; Process files
          (let [files (filter #(not= (existing-hashes (:id %)) (:content-hash %))
                              (:files discovery))
                batch-size (config/ingest-batch-size)
                ragussy-url (config/ragussy-url)
                openplanner-url (config/openplanner-url)
                openplanner-api-key (config/openplanner-api-key)
                use-openplanner? (not (str/blank? openplanner-url))
                collections (or (parse-jsonish (:collections source)) ["devel_docs"])]
            (log! (str "[JOB " job-id "] Ingest target: " (if use-openplanner? "openplanner" "ragussy")
                       ", files=" (count files) ", batch-size=" batch-size))
            
            (loop [remaining files
                    processed 0
                    failed 0
                    chunks-total 0
                    start-time (Instant/now)]
              (if (empty? remaining)
                ;; Done
                (let [elapsed (.getSeconds (Duration/between start-time (Instant/now)))]
                  (log! (str "[JOB " job-id "] Completed: "
                             processed " processed, "
                             failed " failed, "
                             chunks-total " chunks in "
                             elapsed "s"))
                  (db/update-job! job-id {:status "completed"
                                          :processed_files processed
                                          :failed_files failed
                                          :chunks_created chunks-total
                                          :completed_at (Timestamp/from (Instant/now))}))
                
                ;; Process batch
                (let [batch (take batch-size remaining)
                      file-ids (map :id batch)
                      _ (maybe-throttle! job-id)
                      files-with-content (.extract-batch driver file-ids)
                       
                        ;; Ingest each file
                       results (->> files-with-content
                                    (filter :content)
                                    (mapv (fn [file]
                                            (try
                                              (let [ingest-result (if use-openplanner?
                                                                    (ingest-via-openplanner! tenant-id source-id openplanner-url openplanner-api-key file)
                                                                    (ingest-via-ragussy! ragussy-url collections file))]
                                                (assoc ingest-result :file file))
                                              (catch Exception e
                                                {:status :failed
                                                 :file file
                                                 :error (.getMessage e)})))))
                      
                      ;; Update file state
                      _ (doseq [result results]
                          (when (= :success (:status result))
                            (db/upsert-file-state!
                             {:file-id (-> result :file :id)
                              :source-id source-id
                              :tenant-id (:tenant_id source)
                              :path (-> result :file :path)
                              :content-hash (-> result :file :content-hash)
                              :status "ingested"
                              :chunks (:chunks result)
                              :collections collections})))
                      
                      ;; Count results
                      batch-processed (count (filter #(= :success (:status %)) results))
                      batch-failed (count (filter #(= :failed (:status %)) results))
                      batch-chunks (reduce + 0 (map :chunks (filter #(= :success (:status %)) results)))]
                  (log! (str "[JOB " job-id "] Batch done: processed=" batch-processed
                             " failed=" batch-failed
                             " chunks=" batch-chunks
                             " remaining=" (max 0 (- (count remaining) batch-size))))
                  (when (pos? (config/ingest-batch-delay-ms))
                    (Thread/sleep (long (config/ingest-batch-delay-ms))))
                   
                  (recur (drop batch-size remaining)
                         (+ processed batch-processed)
                         (+ failed batch-failed)
                         (+ chunks-total batch-chunks)
                         start-time))))))
        
        ;; Save driver state
        (let [driver-state (.get-state driver)]
          (db/update-job! job-id {:status "completed"})
          (log! (str "[JOB " job-id "] Driver state: " driver-state)))))
    
    (catch Exception e
      (log! (str "[JOB " job-id "] FAILED: " (.getMessage e)))
      (.printStackTrace e)
      (db/update-job! job-id {:status "failed"
                              :error_message (.getMessage e)}))))

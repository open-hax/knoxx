(ns kms-ingestion.translation.worker
  "Translation worker that processes translation_jobs using the agent runtime.
  
  Polls MongoDB translation_jobs collection for queued jobs.
  Uses OpenPlanner API to fetch documents and save segments.
  Recursively processes jobs until queue is empty."
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [kms-ingestion.config :as config]
   [kms-ingestion.translation.agent :as agent])
  (:import
   [java.time Duration Instant]))

(defonce running? (atom false))
(defonce worker-thread (atom nil))
(defonce poll-interval-ms (atom 5000))

(defn- openplanner-url
  [path]
  (str (config/openplanner-url) "/v1" path))

(defn- openplanner-headers
  []
  (let [api-key (config/openplanner-api-key)]
    (println "[translation-worker] API Key:" (if (str/blank? api-key) "<empty>" api-key))
    (cond-> {"Content-Type" "application/json"}
      (not (str/blank? api-key))
      (assoc "Authorization" (str "Bearer " api-key)))))

(defn- fetch-json
  [url]
  (let [conn (.openConnection (java.net.URL. url))]
    (doseq [[k v] (openplanner-headers)]
      (.setRequestProperty conn k v))
    (.setRequestMethod conn "GET")
    (let [code (.getResponseCode conn)]
      (if (= 200 code)
        (let [body (slurp (.getInputStream conn))]
          (json/parse-string body keyword))
        (let [error-body (try (slurp (.getErrorStream conn)) (catch Exception _ "unknown error"))]
          (throw (ex-info (str "HTTP " code ": " error-body) {:url url :code code})))))))

(defn- post-json
  [url body]
  (let [conn (.openConnection (java.net.URL. url))
        body-str (json/generate-string body)]
    (doseq [[k v] (openplanner-headers)]
      (.setRequestProperty conn k v))
    (.setRequestMethod conn "POST")
    (.setDoOutput conn true)
    (.write (java.io.OutputStreamWriter. (.getOutputStream conn)) body-str)
    (let [code (.getResponseCode conn)]
      (if (or (= 200 code) (= 201 code))
        (let [body (slurp (.getInputStream conn))]
          (json/parse-string body keyword))
        (let [error-body (try (slurp (.getErrorStream conn)) (catch Exception _ "unknown error"))]
          (throw (ex-info (str "HTTP " code ": " error-body) {:url url :code code})))))))

(defn- fetch-next-job
  []
  (try
    (let [result (fetch-json (openplanner-url "/translations/jobs/next"))]
      (:job result))
    (catch Exception e
      (println "[translation-worker] Failed to fetch next job:" (.getMessage e))
      nil)))

(defn- mark-job-status
  [job-id status & {:keys [error]}]
  (try
    (post-json (openplanner-url (str "/translations/jobs/" job-id "/status"))
               (cond-> {:status status}
                 error (assoc :error error)))
    (catch Exception e
      (println "[translation-worker] Failed to mark job" job-id status ":" (.getMessage e)))))

(defn- process-job
  [job]
  (let [job-id (str (:_id job))
        document-id (:document_id job)
        garden-id (:garden_id job)
        source-lang (:source_lang job)
        target-lang (:target_language job)]
    (println "[translation-worker] Processing job" job-id "document" document-id "->" target-lang)
    (try
      (mark-job-status job-id "processing")
      (agent/run-translation-session!
       {:job-id job-id
        :document-id document-id
        :garden-id garden-id
        :source-lang source-lang
        :target-lang target-lang})
      (mark-job-status job-id "complete")
      (println "[translation-worker] Job" job-id "completed")
      (catch Exception e
        (println "[translation-worker] Job" job-id "failed:" (.getMessage e))
        (mark-job-status job-id "failed" :error (.getMessage e))))))

(defn- poll-loop
  []
  (while @running?
    (try
      (when-let [job (fetch-next-job)]
        (process-job job))
      (Thread/sleep @poll-interval-ms)
      (catch Exception e
        (println "[translation-worker] Poll error:" (.getMessage e))
        (Thread/sleep @poll-interval-ms)))))

(defn start!
  "Start the translation worker."
  []
  (when-not @running?
    (reset! running? true)
    (println "[translation-worker] Starting translation worker")
    (println "[translation-worker] OpenPlanner:" (config/openplanner-url))
    (reset! worker-thread (future (poll-loop)))))

(defn stop!
  "Stop the translation worker."
  []
  (reset! running? false)
  (when-let [thread @worker-thread]
    (future-cancel thread)
    (reset! worker-thread nil))
  (println "[translation-worker] Stopped"))

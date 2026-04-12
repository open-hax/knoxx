(ns kms-ingestion.translation.worker
  "Translation worker that processes translation_jobs via the Knoxx agent runtime.
  
  Polls OpenPlanner for queued translation jobs.
  Calls Knoxx backend /api/knoxx/chat/start with translator role.
  Agent uses restricted tools: read, memory_search, memory_session, graph_query, save_translation."
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [kms-ingestion.config :as config])
  (:import
   [java.net URL]
   [java.io OutputStreamWriter]
   [java.util UUID]))

(defonce running? (atom false))
(defonce worker-thread (atom nil))
(defonce poll-interval-ms (atom 5000))

(defn- openplanner-url
  [path]
  (str (config/openplanner-url) "/v1" path))

(defn- knoxx-url
  [path]
  (str (or (config/knoxx-backend-url) "http://knoxx-backend:8000") path))

(defn- openplanner-headers
  []
  (let [api-key (config/openplanner-api-key)]
    (cond-> {"Content-Type" "application/json"}
      (not (str/blank? api-key))
      (assoc "Authorization" (str "Bearer " api-key)))))

(defn- knoxx-headers
  []
  (let [api-key (config/knoxx-api-key)]
    (cond-> {"Content-Type" "application/json"}
      (not (str/blank? api-key))
      (assoc "X-API-Key" api-key))))

(defn- fetch-json
  [url headers]
  (let [conn (.openConnection (URL. url))]
    (doseq [[k v] headers]
      (.setRequestProperty conn k v))
    (.setRequestMethod conn "GET")
    (let [code (.getResponseCode conn)]
      (if (= 200 code)
        (let [body (slurp (.getInputStream conn))]
          (json/parse-string body keyword))
        (let [error-body (try (slurp (.getErrorStream conn)) (catch Exception _ "unknown error"))]
          (throw (ex-info (str "HTTP " code ": " error-body) {:url url :code code})))))))

(defn- post-json
  [url headers body]
  (let [conn (.openConnection (URL. url))
        body-str (json/generate-string body)]
    (doseq [[k v] headers]
      (.setRequestProperty conn k v))
    (.setRequestMethod conn "POST")
    (.setDoOutput conn true)
    (with-open [writer (OutputStreamWriter. (.getOutputStream conn))]
      (.write writer body-str)
      (.flush writer))
    (let [code (.getResponseCode conn)]
      (if (or (= 200 code) (= 201 code) (= 202 code))
        (let [body (slurp (.getInputStream conn))]
          (json/parse-string body keyword))
        (let [error-body (try (slurp (.getErrorStream conn)) (catch Exception _ "unknown error"))]
          (throw (ex-info (str "HTTP " code ": " error-body) {:url url :code code})))))))

(defn- fetch-next-job
  []
  (try
    (let [result (fetch-json (openplanner-url "/translations/jobs/next") (openplanner-headers))]
      (:job result))
    (catch Exception e
      (println "[translation-worker] Failed to fetch next job:" (.getMessage e))
      nil)))

(defn- mark-job-status
  [job-id status & {:keys [error]}]
  (try
    (post-json (openplanner-url (str "/translations/jobs/" job-id "/status"))
               (openplanner-headers)
               (cond-> {:status status}
                 error (assoc :error error)))
    (catch Exception e
      (println "[translation-worker] Failed to mark job" job-id status ":" (.getMessage e)))))

(defn- fetch-document
  [document-id]
  (let [result (fetch-json (openplanner-url (str "/documents/" document-id)) (openplanner-headers))]
    (:document result)))

(defn- process-job
  [job]
  (let [job-id (or (:id job) (str (:_id job)))
        document-id (:document_id job)
        garden-id (:garden_id job)
        project (:project job)
        source-lang (:source_lang job)
        target-lang (:target_language job)]
    (println "[translation-worker] Processing job" job-id "document" document-id "->" target-lang)
    (try
      (mark-job-status job-id "processing")
      
      ;; Fetch document to get title
      (let [document (fetch-document document-id)
            doc-title (or (:title document) "Untitled")
            conversation-id (str "translation-" job-id)
            run-id (str (UUID/randomUUID))]
        
        (println "[translation-worker] Document:" doc-title)
        
        ;; Call Knoxx agent with translator role
        (let [agent-request {:conversation_id conversation-id
                             :run_id run-id
                             :message (str "Translate the document " document-id 
                                          " from " source-lang " to " target-lang ". "
                                          "Document title: " doc-title ". "
                                          "Garden: " garden-id ". Project: " project ". "
                                          "Read the source content, translate it, and save each segment using save_translation.")
                             :model "glm-5"
                             :auth_context {:role "translator"
                                           :toolPolicies [{:toolId "read" :effect "allow"}
                                                          {:toolId "memory_search" :effect "allow"}
                                                          {:toolId "memory_session" :effect "allow"}
                                                          {:toolId "graph_query" :effect "allow"}
                                                          {:toolId "save_translation" :effect "allow"}]}}
              _ (println "[translation-worker] Calling Knoxx agent...")
              result (post-json (knoxx-url "/api/knoxx/chat/start")
                               (knoxx-headers)
                               agent-request)]
          
          (println "[translation-worker] Agent started:" (:conversation_id result) "run:" (:run_id result))
          
          ;; TODO: Wait for agent to complete via WebSocket or polling
          ;; For now, mark complete after starting
          (mark-job-status job-id "complete")
          (println "[translation-worker] Job" job-id "completed")))
      
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
    (println "[translation-worker] Knoxx:" (config/knoxx-backend-url))
    (reset! worker-thread (future (poll-loop)))))

(defn stop!
  "Stop the translation worker."
  []
  (reset! running? false)
  (when-let [thread @worker-thread]
    (future-cancel thread)
    (reset! worker-thread nil))
  (println "[translation-worker] Stopped"))

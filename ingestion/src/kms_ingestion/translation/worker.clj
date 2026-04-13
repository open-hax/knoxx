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
   [java.net URL URLEncoder]
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
  (let [api-key (config/knoxx-api-key)
        user-email (config/knoxx-user-email)]
    (cond-> {"Content-Type" "application/json"
             "x-knoxx-user-email" user-email}
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

(defn- url-encode [s]
  (URLEncoder/encode (str s) "UTF-8"))

(defn- fetch-document
  [document-id]
  (let [result (fetch-json (openplanner-url (str "/documents/" document-id)) (openplanner-headers))]
    (:document result)))

(defn- fetch-knoxx-segments
  [project document-id source-lang target-lang]
  (fetch-json (str (knoxx-url "/api/translations/segments")
                   "?project=" (url-encode project)
                   "&document_id=" (url-encode document-id)
                   "&source_lang=" (url-encode source-lang)
                   "&target_lang=" (url-encode target-lang)
                   "&limit=100")
              (knoxx-headers)))

(defn- wait-for-new-segments
  [project document-id source-lang target-lang initial-total timeout-ms]
  (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (let [result (fetch-knoxx-segments project document-id source-lang target-lang)
            total (long (or (:total result) 0))]
        (cond
          (> total initial-total)
          total

          (> (System/currentTimeMillis) deadline)
          (throw (ex-info (str "Timed out waiting for translated segments for document " document-id
                               " -> " target-lang)
                          {:document-id document-id
                           :target-lang target-lang
                           :initial-total initial-total
                           :timeout-ms timeout-ms}))

          :else
          (do
            (Thread/sleep 2000)
            (recur)))))))

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
      
      ;; Fetch document to get title/content and baseline segment count
      (let [document (fetch-document document-id)
            doc-title (or (:title document) "Untitled")
            doc-content (or (:content document) (:text document) "")
            run-id (str (UUID/randomUUID))
            conversation-id (str "translation-" job-id "-" run-id)
            session-id (str (UUID/randomUUID))
            initial-total (long (or (:total (fetch-knoxx-segments project document-id source-lang target-lang)) 0))]

        (println "[translation-worker] Document:" doc-title)
        (println "[translation-worker] Existing segments:" initial-total)

        ;; Call Knoxx agent with translator agent_spec on the general direct runtime
        (let [system-prompt (str "You are the Knoxx translator agent. Translate the supplied document from " source-lang
                                 " to " target-lang ".\n\n"
                                 "Rules:\n"
                                 "1. Preserve meaning, tone, markdown structure, links, and list structure where possible.\n"
                                 "2. Split the source into logical segments and call save_translation for every translated segment.\n"
                                 "3. Every save_translation call must include source_text, translated_text, source_lang, target_lang, document_id, garden_id, project, and segment_index.\n"
                                 "4. Set project to '" project "' and garden_id to '" garden-id "'.\n"
                                 "5. translated_text must be in " target-lang "; do not copy source text for normal prose.\n"
                                 "6. Do not output commentary or alternatives; a brief completion acknowledgment is enough after all save_translation calls succeed.")
              agent-request {:conversation_id conversation-id
                             :session_id session-id
                             :run_id run-id
                             :message (str "Translate document " document-id " (title: " doc-title ") from " source-lang " to " target-lang ".\n\n"
                                          "SOURCE DOCUMENT:\n"
                                          doc-content)
                             :agent_spec {:role "translator"
                                          :system_prompt system-prompt
                                          :model "glm-5"
                                          :thinking_level "off"
                                          :tool_policies [{:toolId "read" :effect "allow"}
                                                          {:toolId "memory_search" :effect "allow"}
                                                          {:toolId "memory_session" :effect "allow"}
                                                          {:toolId "graph_query" :effect "allow"}
                                                          {:toolId "save_translation" :effect "allow"}]
                                          :resource_policies {:project project
                                                              :garden_id garden-id
                                                              :document_id document-id
                                                              :source_lang source-lang
                                                              :target_lang target-lang}}
                             :model "glm-5"}
              _ (println "[translation-worker] Calling Knoxx agent...")
              result (post-json (knoxx-url "/api/knoxx/direct/start")
                                (knoxx-headers)
                                agent-request)
              _ (println "[translation-worker] Agent started:" (:conversation_id result) "run:" (:run_id result))
              final-total (wait-for-new-segments project document-id source-lang target-lang initial-total 120000)]

          (println "[translation-worker] New segment total:" final-total)
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

(ns kms-ingestion.translation.worker
  "Translation worker that processes translation_batches via the Knoxx agent runtime.
  
  Batches all documents for a garden+target_lang into a single agent session.
  The agent gets cross-document context for consistent terminology and style.
  
  Batch model:
  - One batch = one garden + one target language + N published documents
  - One agent session translates all documents in the batch
  - Worker polls for next batch, starts session, monitors progress"
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
(defonce poll-interval-ms (atom 10000))

(def ^:private translation-config-ttl-ms 30000)
(def translation-model-cache* (atom {:model nil :fetched-at 0}))

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

(defn- fetch-translation-config-model
  "Fetch translation model from OpenPlanner /v1/translations/config. Returns string or nil."
  []
  (try
    (let [result (fetch-json (openplanner-url "/translations/config") (openplanner-headers))
          model (get-in result [:config :model])
          normalized (str/trim (str (or model "")))]
      (when-not (str/blank? normalized)
        normalized))
    (catch Exception e
      (println "[translation-worker] Failed to fetch translation config:" (.getMessage e))
      nil)))

(defn- resolve-translation-model
  "Resolve the model used by the translation agent.

   Precedence:
   1) OpenPlanner /v1/translations/config
   2) TRANSLATION_MODEL env var (kms-ingestion.config/translation-model)
   3) Fallback: glm-5"
  []
  (let [now (System/currentTimeMillis)
        {:keys [model fetched-at]} @translation-model-cache*]
    (if (and model (< (- now fetched-at) translation-config-ttl-ms))
      model
      (let [fresh (or (fetch-translation-config-model)
                      (some-> (config/translation-model) str str/trim)
                      "glm-5")]
        (reset! translation-model-cache* {:model fresh :fetched-at now})
        fresh))))

(defn- url-encode [s]
  (URLEncoder/encode (str s) "UTF-8"))

;; ─── Fetch helpers ────────────────────────────────────────────────────

(defn- fetch-next-batch
  "Poll for the next queued translation batch."
  []
  (try
    (let [result (fetch-json (openplanner-url "/translations/batches/next") (openplanner-headers))]
      (:batch result))
    (catch Exception e
      (println "[translation-worker] Failed to fetch next batch:" (.getMessage e))
      nil)))

(defn- mark-batch-status
  [batch-id status & {:keys [error agent-session-id agent-conversation-id agent-run-id completed-document failed-document]}]
  (try
    (let [payload (cond-> {:status status}
                    error (assoc :error error)
                    agent-session-id (assoc :agent_session_id agent-session-id)
                    agent-conversation-id (assoc :agent_conversation_id agent-conversation-id)
                    agent-run-id (assoc :agent_run_id agent-run-id)
                    completed-document (assoc :completed_document completed-document)
                    failed-document (assoc :failed_document failed-document))]
      (post-json (openplanner-url (str "/translations/batches/" (url-encode batch-id) "/status"))
                 (openplanner-headers)
                 payload))
    (catch Exception e
      (println "[translation-worker] Failed to mark batch" batch-id status ":" (.getMessage e)))))

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
  "Wait until segment count increases past initial-total, or timeout."
  [project document-id source-lang target-lang initial-total timeout-ms]
  (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (let [result (try (fetch-knoxx-segments project document-id source-lang target-lang)
                        (catch Exception _ {:total initial-total}))
            total (long (or (:total result) 0))]
        (cond
          (> total initial-total)
          total

          (> (System/currentTimeMillis) deadline)
          (do
            (println "[translation-worker] Timeout waiting for segments for" document-id "->" target-lang
                     "(got" total "expected >" initial-total ")")
            total)

          :else
          (do
            (Thread/sleep 2000)
            (recur)))))))

;; ─── Batch processing ─────────────────────────────────────────────────

(defn- build-batch-prompt
  "Build the system prompt for a batch translation session."
  [source-lang target-lang project garden-id]
  (str "You are the Knoxx translator agent. Translate ALL supplied documents from " source-lang
       " to " target-lang ".\n\n"
       "Rules:\n"
       "1. Preserve meaning, tone, markdown structure, links, and list structure where possible.\n"
       "2. For each document, split the source into logical segments and call save_translation for every translated segment.\n"
       "3. Every save_translation call must include source_text, translated_text, source_lang, target_lang, document_id, garden_id, project, and segment_index.\n"
       "4. Set project to '" project "' and garden_id to '" garden-id "'.\n"
       "5. translated_text must be in " target-lang "; do not copy source text for normal prose.\n"
       "6. Maintain CONSISTENT TERMINOLOGY across all documents within this batch. If you translate a technical term one way in the first document, use the same translation in subsequent documents.\n"
       "7. After completing all documents, provide a brief summary of translations done.\n"
       "8. Do not skip any documents. Translate ALL of them."))

(defn- build-batch-message
  "Build the user message containing all document content for the batch."
  [documents source-lang target-lang]
  (let [inventory (->> documents
                       (map-indexed (fn [idx doc]
                                      (str (inc idx) ". " (:title doc "Untitled") " (id: " (:id doc) ")")))
                       (str/join "\n"))
        contents (->> documents
                      (map (fn [doc]
                             (str "\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                                  "DOCUMENT: " (:title doc "Untitled") "\n"
                                  "DOCUMENT_ID: " (:id doc) "\n"
                                  "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
                                  (:content doc ""))))
                      (str/join "\n"))]
    (str "Translate the following " (count documents) " documents from " source-lang " to " target-lang ".\n\n"
         "DOCUMENT INVENTORY:\n" inventory "\n\n"
         "FULL DOCUMENT CONTENT:\n" contents)))

(defn- process-batch
  "Process a translation batch: start one agent session for all documents."
  [batch]
  (let [batch-id (or (:batch_id batch) (:id batch) (str (:_id batch)))
        garden-id (:garden_id batch)
        project (:project batch)
        source-lang (:source_lang batch "en")
        target-lang (:target_lang batch)
        document-ids (:document_ids batch)]
    (println "[translation-worker] Processing batch" batch-id
             "garden" garden-id "->" target-lang
             (count document-ids) "documents")
    (try
      ;; Mark batch as processing
      (mark-batch-status batch-id "processing")

      ;; Fetch all document content
      (let [documents (doall
                        (for [doc-id document-ids]
                          (try
                            (let [doc (fetch-document doc-id)]
                              {:id doc-id
                               :title (:title doc "Untitled")
                               :content (or (:content doc) (:text doc) "")})
                            (catch Exception e
                              (println "[translation-worker] Failed to fetch document" doc-id ":" (.getMessage e))
                              nil))))
            valid-docs (remove nil? documents)]

        (if (empty? valid-docs)
          (do
            (println "[translation-worker] No valid documents in batch" batch-id)
            (mark-batch-status batch-id "failed" :error "No valid documents to translate"))

          ;; Start agent session with all documents
          (let [run-id (str (UUID/randomUUID))
                conversation-id (str "translation-batch-" batch-id "-" run-id)
                session-id (str (UUID/randomUUID))
                system-prompt (build-batch-prompt source-lang target-lang project garden-id)
                batch-message (build-batch-message valid-docs source-lang target-lang)
                translation-model (resolve-translation-model)
                agent-request {:conversation_id conversation-id
                               :session_id session-id
                               :run_id run-id
                               :message batch-message
                               :agent_spec {:role "translator"
                                            :system_prompt system-prompt
                                            :model translation-model
                                            :thinking_level "off"
                                            :tool_policies [{:toolId "read" :effect "allow"}
                                                            {:toolId "memory_search" :effect "allow"}
                                                            {:toolId "memory_session" :effect "allow"}
                                                            {:toolId "graph_query" :effect "allow"}
                                                            {:toolId "save_translation" :effect "allow"}]
                                            :resource_policies {:project project
                                                                :garden_id garden-id
                                                                :source_lang source-lang
                                                                :target_lang target-lang}}
                               :model translation-model}
                _ (println "[translation-worker] Calling Knoxx agent with" (count valid-docs) "documents...")
                result (post-json (knoxx-url "/api/knoxx/direct/start")
                                  (knoxx-headers)
                                  agent-request)
                _ (println "[translation-worker] Agent started:" (:conversation_id result) "run:" (:run_id result))]

            ;; Mark batch with agent session info
            (mark-batch-status batch-id "processing"
                               :agent-session-id session-id
                               :agent-conversation-id conversation-id
                               :agent-run-id run-id)

            ;; Wait for each document's segments to appear
            (let [completed (atom [])
                  failed (atom [])]
              (doseq [doc valid-docs]
                (try
                  (let [doc-id (:id doc)
                        initial-total (long (or (:total (fetch-knoxx-segments project doc-id source-lang target-lang)) 0))
                        _ (println "[translation-worker] Waiting for segments:" doc-id "initial:" initial-total)
                        final-total (wait-for-new-segments project doc-id source-lang target-lang initial-total (* 180000 (count valid-docs)))]
                    (if (> final-total initial-total)
                      (do
                        (println "[translation-worker] Document" doc-id "translated:" final-total "segments")
                        (swap! completed conj doc-id)
                        (mark-batch-status batch-id "processing" :completed-document doc-id))
                      (do
                        (println "[translation-worker] Document" doc-id "timed out")
                        (swap! failed conj {:document_id doc-id :error "Timed out waiting for segments"}))))
                  (catch Exception e
                    (println "[translation-worker] Document" (:id doc) "error:" (.getMessage e))
                    (swap! failed conj {:document_id (:id doc) :error (.getMessage e)}))))

              ;; Final batch status
              (cond
                (empty? @failed)
                (mark-batch-status batch-id "complete")

                (= (count @failed) (count valid-docs))
                (mark-batch-status batch-id "failed" :error "All documents failed")

                :else
                (mark-batch-status batch-id "partial"))

              (println "[translation-worker] Batch" batch-id "done:"
                       (count @completed) "completed,"
                       (count @failed) "failed")))))

      (catch Exception e
        (println "[translation-worker] Batch" batch-id "failed:" (.getMessage e))
        (mark-batch-status batch-id "failed" :error (.getMessage e))))))

;; ─── Legacy single-job support (backward compat) ─────────────────────

(defn- fetch-next-job
  "Poll for next queued single translation job (legacy)."
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

(defn- process-job
  "Process a single translation job (legacy mode)."
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
      (let [translation-model (resolve-translation-model)
            document (fetch-document document-id)
            doc-title (or (:title document) "Untitled")
            doc-content (or (:content document) (:text document) "")
            run-id (str (UUID/randomUUID))
            conversation-id (str "translation-" job-id "-" run-id)
            session-id (str (UUID/randomUUID))
            initial-total (long (or (:total (fetch-knoxx-segments project document-id source-lang target-lang)) 0))
            system-prompt (str "You are the Knoxx translator agent. Translate the supplied document from " source-lang
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
                                        :model translation-model
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
                           :model translation-model}
            _ (println "[translation-worker] Calling Knoxx agent...")
            result (post-json (knoxx-url "/api/knoxx/direct/start")
                              (knoxx-headers)
                              agent-request)
            _ (println "[translation-worker] Agent started:" (:conversation_id result) "run:" (:run_id result))
            final-total (wait-for-new-segments project document-id source-lang target-lang initial-total 120000)]
        (println "[translation-worker] New segment total:" final-total)
        (mark-job-status job-id "complete")
        (println "[translation-worker] Job" job-id "completed"))
      (catch Exception e
        (println "[translation-worker] Job" job-id "failed:" (.getMessage e))
        (mark-job-status job-id "failed" :error (.getMessage e))))))

;; ─── Poll loop ────────────────────────────────────────────────────────

(defn- poll-loop
  "Poll for batches first, then fall back to legacy single jobs."
  []
  (while @running?
    (try
      ;; Prefer batches
      (if-let [batch (fetch-next-batch)]
        (process-batch batch)
        ;; Fall back to legacy single jobs
        (when-let [job (fetch-next-job)]
          (process-job job)))
      (Thread/sleep @poll-interval-ms)
      (catch Exception e
        (println "[translation-worker] Poll error:" (.getMessage e))
        (Thread/sleep @poll-interval-ms)))))

(defn start!
  "Start the translation worker."
  []
  (when-not @running?
    (reset! running? true)
    (println "[translation-worker] Starting translation worker (batch mode)")
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

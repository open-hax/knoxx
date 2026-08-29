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
(def ^:private system-admin-role-slugs #{"system_admin" "system-admin"})
(def ^:private unresolved-principal-cooldown-ms 300000)
(def translation-model-cache* (atom {:model nil :fetched-at 0}))

;; Batch ids whose execution principal could not be resolved safely, mapped to
;; the millisecond timestamp of that decision. A cooled-down batch keeps its
;; queue position instead of being failed, but the worker stops re-resolving
;; its identity on every poll.
(defonce ^:private unresolved-principals* (atom {}))

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
  "Build Knoxx request headers for the worker identity.

  Deliberately carries no organization header. `x-knoxx-org-id` does not rescope
  an already-resolved context: request resolution feeds it to
  `find-membership-row-by-email-and-org!`, which filters the user's memberships
  to that organization, so a system admin with no membership in a legacy batch's
  org would fail authentication outright. The batch organization travels as an
  authorized scope instead — agent resource policy for the run, and an explicit
  `org_id` on the segment reads."
  ([] (knoxx-headers nil))
  ([membership-id]
   (let [api-key (config/knoxx-api-key)
         user-email (config/knoxx-user-email)]
     (cond-> {"Content-Type" "application/json"
              "x-knoxx-user-email" user-email}
       (not (str/blank? (str membership-id)))
       (assoc "x-knoxx-membership-id" (str membership-id))
       (not (str/blank? api-key))
       (assoc "X-API-Key" api-key)))))

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
        (let [error-body (try (slurp (.getErrorStream conn))
                              (catch Exception _ "unknown error"))]
          (throw (ex-info (str "HTTP " code ": " error-body)
                          {:url url :code code})))))))

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
        (let [response-body (slurp (.getInputStream conn))]
          (json/parse-string response-body keyword))
        (let [error-body (try (slurp (.getErrorStream conn))
                              (catch Exception _ "unknown error"))]
          (throw (ex-info (str "HTTP " code ": " error-body)
                          {:url url :code code})))))))

(defn fetch-translation-config-model
  "Fetch the translation model from the Knoxx-owned config boundary,
  `GET /api/translations/config`.

  The response is the wire config itself — `{\"model\" ... \"source-locale\" ...
  \"default-review\" ...}` — not wrapped under a `config` key, and `model` is a
  catalog model id spelled exactly as the Knoxx model catalog spells it.

  Returns the model string, or throws. It deliberately does NOT swallow the
  error into `nil`: silently returning nil is what previously let the worker
  fall through to an unrelated env default that could disagree with the resource
  graph."
  []
  (let [result (fetch-json (knoxx-url "/api/translations/config") (knoxx-headers))
        model (str/trim (str (or (:model result) "")))]
    (when (str/blank? model)
      (throw (ex-info "Knoxx translation config returned no model"
                      {:response result})))
    model))

(defn resolve-translation-model
  "Resolve the model used by the translation agent.

  Knoxx resources are the ONLY authority. There is no OpenPlanner read and no
  env/default fallback: a config lookup failure is surfaced as a configuration
  failure, because a worker that quietly translates with a different model than
  the resource graph declares produces output nobody can account for.

  `TRANSLATION_MODEL` is no longer consulted. If a fallback is ever wanted it
  belongs in Knoxx-owned configuration, so that UI and worker consumers get the
  same answer from the same place.

  Successful lookups are cached briefly; failures are not cached, so a transient
  outage does not pin a stale answer."
  []
  (let [now (System/currentTimeMillis)
        {:keys [model fetched-at]} @translation-model-cache*]
    (if (and model (< (- now fetched-at) translation-config-ttl-ms))
      model
      (let [fresh (try
                    (fetch-translation-config-model)
                    (catch Exception e
                      (throw (ex-info
                              (str "Translation configuration unavailable from Knoxx: "
                                   (.getMessage e))
                              {:kind ::translation-config-unavailable}
                              e))))]
        (reset! translation-model-cache* {:model fresh :fetched-at now})
        fresh))))

(defn- url-encode [s]
  (URLEncoder/encode (str s) "UTF-8"))

;; ─── Fetch helpers ────────────────────────────────────────────────────

(defn- fetch-oldest-queued-batch
  "Fetch the single oldest queued batch, whatever its state."
  []
  (try
    (:batch (fetch-json (openplanner-url "/translations/batches/next")
                        (openplanner-headers)))
    (catch Exception e
      (println "[translation-worker] Failed to fetch next batch:" (.getMessage e))
      nil)))

(defn- fetch-queued-batches
  "List queued batches, oldest first.

  `/translations/batches/next` only ever returns the oldest queued batch and
  does not claim it, so a batch the worker cannot run would otherwise block
  every batch behind it. This list is the fallback used to look past it."
  []
  (try
    (->> (:batches (fetch-json (openplanner-url "/translations/batches?status=queued")
                               (openplanner-headers)))
         (sort-by #(str (:created_at %)))
         vec)
    (catch Exception e
      (println "[translation-worker] Failed to list queued batches:" (.getMessage e))
      [])))

(defn- mark-batch-status
  [batch-id status & {:keys [error agent-session-id agent-conversation-id
                             agent-run-id completed-document failed-document]}]
  (try
    (let [payload (cond-> {:status status}
                    error (assoc :error error)
                    agent-session-id (assoc :agent_session_id agent-session-id)
                    agent-conversation-id (assoc :agent_conversation_id agent-conversation-id)
                    agent-run-id (assoc :agent_run_id agent-run-id)
                    completed-document (assoc :completed_document completed-document)
                    failed-document (assoc :failed_document failed-document))]
      (post-json
       (openplanner-url
        (str "/translations/batches/" (url-encode batch-id) "/status"))
       (openplanner-headers)
       payload))
    (catch Exception e
      (println "[translation-worker] Failed to mark batch" batch-id status ":"
               (.getMessage e)))))

(defn- fetch-document
  [document-id]
  (let [result (fetch-json (openplanner-url (str "/documents/" document-id))
                           (openplanner-headers))]
    (:document result)))

(defn- fetch-knoxx-segments
  [membership-id org-id project document-id source-lang target-lang]
  (fetch-json (str (knoxx-url "/api/translations/segments")
                   "?project=" (url-encode project)
                   (when-not (str/blank? (str org-id))
                     (str "&org_id=" (url-encode org-id)))
                   "&document_id=" (url-encode document-id)
                   "&source_lang=" (url-encode source-lang)
                   "&target_lang=" (url-encode target-lang)
                   "&limit=100")
              (knoxx-headers membership-id)))

(defn- wait-for-new-segments
  "Wait until segment count increases past initial-total, or timeout."
  [membership-id org-id project document-id source-lang target-lang initial-total timeout-ms]
  (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (let [result (try
                     (fetch-knoxx-segments membership-id org-id project document-id
                                           source-lang target-lang)
                     (catch Exception _ {:total initial-total}))
            total (long (or (:total result) 0))]
        (cond
          (> total initial-total)
          total

          (> (System/currentTimeMillis) deadline)
          (do
            (println "[translation-worker] Timeout waiting for segments for"
                     document-id "->" target-lang
                     "(got" total "expected >" initial-total ")")
            total)

          :else
          (do
            (Thread/sleep 2000)
            (recur)))))))

(defn- context-org-id
  [context]
  (or (get-in context [:org :id])
      (:orgId context)
      (:org_id context)))

(defn- context-membership-id
  [context]
  (or (get-in context [:membership :id])
      (:membershipId context)
      (:membership_id context)))

(defn- system-admin-context?
  [context]
  (or (true? (:isSystemAdmin context))
      (boolean (some system-admin-role-slugs
                     (map str (or (:roleSlugs context)
                                  (:role_slugs context)
                                  []))))))

(defn- legacy-batch-principal
  "Resolve the configured worker identity for a pre-membership batch.

  System admins may safely carry the explicit batch org in agent resource
  policy. Non-system principals are accepted only when their current
  membership already belongs to that same org.

  Returns a principal map, `:unresolved` when the identity is known but unsafe
  for this batch, or `:transient` when the identity could not be read at all.
  The two failures are distinct: only `:unresolved` earns a cooldown, so a brief
  `/api/auth/context` outage does not stall the batch for the full window."
  [org-id]
  (try
    (let [context (fetch-json (knoxx-url "/api/auth/context")
                              (knoxx-headers))
          context-org (some-> (context-org-id context) str)
          membership-id (some-> (context-membership-id context) str)]
      (cond
        (system-admin-context? context)
        ;; Authenticate as the system admin's own membership. Dropping it forced
        ;; email-only resolution, which picks the default membership and gives
        ;; the reads a tenant unrelated to the batch.
        {:membership-id (not-empty membership-id) :mode :system-admin}

        (and (= (str org-id) context-org)
             (not (str/blank? membership-id)))
        {:membership-id membership-id :mode :same-org-membership}

        :else
        :unresolved))
    (catch Exception e
      (println "[translation-worker] Could not read Knoxx identity:"
               (.getMessage e))
      :transient)))

(defn- batch-principal
  "Principal map for a batch, or `:unresolved` / `:transient` per `legacy-batch-principal`."
  [batch]
  (let [membership-id (some-> (:membership_id batch) str str/trim not-empty)]
    (if membership-id
      {:membership-id membership-id :mode :batch-membership}
      (legacy-batch-principal (:org_id batch)))))

;; ─── Batch processing ─────────────────────────────────────────────────

(defn- build-batch-prompt
  "Build the system prompt for a batch translation session."
  [source-lang target-lang project garden-id]
  (str "You are the Knoxx translator agent. Translate ALL supplied documents from "
       source-lang " to " target-lang ".\n\n"
       "Rules:\n"
       "1. Preserve meaning, tone, markdown structure, links, and list structure where possible.\n"
       "2. For each document, split the source into logical segments and call save_translation for every translated segment.\n"
       "3. Every save_translation call must include source_text, translated_text, source_lang, target_lang, document_id, garden_id, project, and segment_index.\n"
       "4. Set project to '" project "' and garden_id to '" garden-id "'.\n"
       "5. translated_text must be in " target-lang
       "; do not copy source text for normal prose.\n"
       "6. Maintain CONSISTENT TERMINOLOGY across all documents within this batch. If you translate a technical term one way in the first document, use the same translation in subsequent documents.\n"
       "7. After completing all documents, provide a brief summary of translations done.\n"
       "8. Do not skip any documents. Translate ALL of them."))

(defn- build-batch-message
  "Build the user message containing all document content for the batch."
  [documents source-lang target-lang]
  (let [inventory (->> documents
                       (map-indexed
                        (fn [idx document]
                          (str (inc idx) ". " (:title document "Untitled")
                               " (id: " (:id document) ")")))
                       (str/join "\n"))
        contents (->> documents
                      (map (fn [document]
                             (str "\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                                  "DOCUMENT: " (:title document "Untitled") "\n"
                                  "DOCUMENT_ID: " (:id document) "\n"
                                  "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
                                  (:content document ""))))
                      (str/join "\n"))]
    (str "Translate the following " (count documents) " documents from "
         source-lang " to " target-lang ".\n\n"
         "DOCUMENT INVENTORY:\n" inventory "\n\n"
         "FULL DOCUMENT CONTENT:\n" contents)))

(defn- process-batch-with-principal
  [batch principal]
  (let [batch-id (or (:batch_id batch) (:id batch) (str (:_id batch)))
        garden-id (:garden_id batch)
        project (:project batch)
        org-id (:org_id batch)
        membership-id (:membership-id principal)
        source-lang (:source_lang batch "en")
        target-lang (:target_lang batch)
        document-ids (:document_ids batch)]
    (println "[translation-worker] Processing batch" batch-id
             "garden" garden-id "->" target-lang
             (count document-ids) "documents"
             "principal" (name (:mode principal)))
    (try
      (when (str/blank? (str org-id))
        (throw (ex-info "Translation batch is missing org_id"
                        {:batch-id batch-id})))

      (mark-batch-status batch-id "processing")

      (let [documents (doall
                       (for [doc-id document-ids]
                         (try
                           (let [document (fetch-document doc-id)]
                             {:id doc-id
                              :title (:title document "Untitled")
                              :content (or (:content document)
                                           (:text document)
                                           "")})
                           (catch Exception e
                             (println "[translation-worker] Failed to fetch document"
                                      doc-id ":" (.getMessage e))
                             nil))))
            valid-docs (remove nil? documents)]

        (if (empty? valid-docs)
          (do
            (println "[translation-worker] No valid documents in batch" batch-id)
            (mark-batch-status batch-id "failed"
                               :error "No valid documents to translate"))

          (let [run-id (str (UUID/randomUUID))
                conversation-id (str "translation-batch-" batch-id "-" run-id)
                session-id (str (UUID/randomUUID))
                system-prompt (build-batch-prompt source-lang target-lang
                                                  project garden-id)
                batch-message (build-batch-message valid-docs source-lang target-lang)
                translation-model (resolve-translation-model)
                agent-request
                {:conversation_id conversation-id
                 :session_id session-id
                 :run_id run-id
                 :message batch-message
                 :agent_spec
                 {:role "translator"
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
                                      :org_id org-id
                                      :source_lang source-lang
                                      :target_lang target-lang}}
                 :model translation-model}
                _ (println "[translation-worker] Calling Knoxx agent with"
                           (count valid-docs) "documents...")
                result (post-json (knoxx-url "/api/knoxx/direct/start")
                                  (knoxx-headers membership-id)
                                  agent-request)
                _ (println "[translation-worker] Agent started:"
                           (:conversation_id result) "run:" (:run_id result))]

            (mark-batch-status batch-id "processing"
                               :agent-session-id session-id
                               :agent-conversation-id conversation-id
                               :agent-run-id run-id)

            (let [completed (atom [])
                  failed (atom [])]
              (doseq [document valid-docs]
                (try
                  (let [doc-id (:id document)
                        initial-total
                        (long
                         (or (:total
                              (fetch-knoxx-segments membership-id org-id project doc-id
                                                    source-lang target-lang))
                             0))
                        _ (println "[translation-worker] Waiting for segments:"
                                   doc-id "initial:" initial-total)
                        final-total
                        (wait-for-new-segments membership-id org-id project doc-id
                                               source-lang target-lang
                                               initial-total
                                               (* 180000 (count valid-docs)))]
                    (if (> final-total initial-total)
                      (do
                        (println "[translation-worker] Document" doc-id
                                 "translated:" final-total "segments")
                        (swap! completed conj doc-id)
                        (mark-batch-status batch-id "processing"
                                           :completed-document doc-id))
                      (do
                        (println "[translation-worker] Document" doc-id "timed out")
                        (swap! failed conj
                               {:document_id doc-id
                                :error "Timed out waiting for segments"}))))
                  (catch Exception e
                    (println "[translation-worker] Document" (:id document)
                             "error:" (.getMessage e))
                    (swap! failed conj
                           {:document_id (:id document)
                            :error (.getMessage e)}))))

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
        (println "[translation-worker] Batch" batch-id "failed:"
                 (.getMessage e))
        (mark-batch-status batch-id "failed" :error (.getMessage e))))))

(defn- batch-identity
  [batch]
  (or (:batch_id batch) (:id batch) (str (:_id batch))))

(defn- batch-cooling-down?
  "True when this batch already failed principal resolution inside the cooldown window.

  Expired entries are dropped so the map stays bounded by the number of batches
  seen within one cooldown window."
  [batch-id now]
  (-> (swap! unresolved-principals*
             (fn [entries]
               (into {}
                     (remove (fn [[_ at]] (>= (- now at) unresolved-principal-cooldown-ms)))
                     entries)))
      (contains? batch-id)))

(defn- fetch-next-batch
  "Return the oldest queued batch the worker is not currently cooling down on.

  `/translations/batches/next` returns the oldest queued batch without claiming
  it, so an unrunnable batch would sit at the head of the queue forever. When
  the head is cooling down, look past it through the queued list instead of
  stalling every batch behind it."
  []
  (let [now (System/currentTimeMillis)
        head (fetch-oldest-queued-batch)]
    (cond
      (nil? head) nil

      (not (batch-cooling-down? (batch-identity head) now)) head

      :else
      (let [eligible (remove #(batch-cooling-down? (batch-identity %) now)
                             (fetch-queued-batches))]
        (when-let [batch (first eligible)]
          (println "[translation-worker] Head batch" (batch-identity head)
                   "is cooling down; taking queued batch" (batch-identity batch)
                   "instead")
          batch)))))

(defn- process-batch
  "Process a batch, or cool a legacy batch down when no safe principal exists.

  A batch with no resolvable principal keeps its queue position rather than
  being failed, but it is skipped for `unresolved-principal-cooldown-ms` so it
  neither spins the poll loop on identity resolution nor blocks the batches
  behind it. A transient identity-service failure is not cooled down, so it
  retries on the next poll."
  [batch]
  (let [batch-id (batch-identity batch)
        now (System/currentTimeMillis)
        principal (batch-principal batch)]
    (case principal
      :transient
      (println "[translation-worker] Deferring batch" batch-id
               "- Knoxx identity unavailable; retrying next poll")

      :unresolved
      (do (swap! unresolved-principals* assoc batch-id now)
          (println "[translation-worker] Leaving legacy batch queued; configured Knoxx identity"
                   "is neither system admin nor a member of org" (:org_id batch)
                   "batch" batch-id
                   "- retrying in" (quot unresolved-principal-cooldown-ms 1000) "s"))

      (do (swap! unresolved-principals* dissoc batch-id)
          (process-batch-with-principal batch principal)))))

;; ─── Legacy single-job support (backward compat) ─────────────────────

(defn- fetch-next-job
  "Poll for next queued single translation job (legacy)."
  []
  (try
    (let [result (fetch-json (openplanner-url "/translations/jobs/next")
                             (openplanner-headers))]
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
      (println "[translation-worker] Failed to mark job" job-id status ":"
               (.getMessage e)))))

(defn- process-job
  "Process a single translation job (legacy mode)."
  [job]
  (let [job-id (or (:id job) (str (:_id job)))
        document-id (:document_id job)
        garden-id (:garden_id job)
        project (:project job)
        source-lang (:source_lang job)
        target-lang (:target_language job)]
    (println "[translation-worker] Processing job" job-id "document"
             document-id "->" target-lang)
    (try
      (mark-job-status job-id "processing")
      (let [translation-model (resolve-translation-model)
            document (fetch-document document-id)
            doc-title (or (:title document) "Untitled")
            doc-content (or (:content document) (:text document) "")
            run-id (str (UUID/randomUUID))
            conversation-id (str "translation-" job-id "-" run-id)
            session-id (str (UUID/randomUUID))
            initial-total
            (long
             (or (:total
                  (fetch-knoxx-segments nil nil project document-id
                                        source-lang target-lang))
                 0))
            system-prompt
            (str "You are the Knoxx translator agent. Translate the supplied document from "
                 source-lang " to " target-lang ".\n\n"
                 "Rules:\n"
                 "1. Preserve meaning, tone, markdown structure, links, and list structure where possible.\n"
                 "2. Split the source into logical segments and call save_translation for every translated segment.\n"
                 "3. Every save_translation call must include source_text, translated_text, source_lang, target_lang, document_id, garden_id, project, and segment_index.\n"
                 "4. Set project to '" project "' and garden_id to '" garden-id "'.\n"
                 "5. translated_text must be in " target-lang
                 "; do not copy source text for normal prose.\n"
                 "6. Do not output commentary or alternatives; a brief completion acknowledgment is enough after all save_translation calls succeed.")
            agent-request
            {:conversation_id conversation-id
             :session_id session-id
             :run_id run-id
             :message (str "Translate document " document-id " (title: "
                           doc-title ") from " source-lang " to " target-lang
                           ".\n\nSOURCE DOCUMENT:\n" doc-content)
             :agent_spec
             {:role "translator"
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
            _ (println "[translation-worker] Agent started:"
                       (:conversation_id result) "run:" (:run_id result))
            final-total (wait-for-new-segments nil nil project document-id
                                               source-lang target-lang
                                               initial-total 120000)]
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
      (if-let [batch (fetch-next-batch)]
        (process-batch batch)
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
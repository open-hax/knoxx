(ns knoxx.backend.translation-agent
  "Translation Agent - processes translation jobs using the agent runtime.
  
  The translation agent is a specialized agent with restricted tools:
  - read: Read source documents
  - memory_search/memory_session: Access prior translation context
  - graph_query: Query translation examples from knowledge graph
  - save_translation: Save translated content to database
  
  When a translation job is queued, the agent processes it and saves results.
  If more jobs remain, a new session is spawned to continue processing."
  (:require [clojure.string :as str]
            [knoxx.backend.agent-runtime :refer [create-agent-session!]]
            [knoxx.backend.runtime-config :refer [cfg]]))

(defonce poll-interval* (atom 5000))
(defonce running* (atom false))
(defonce current-session* (atom nil))

(defn- openplanner-url
  [config path]
  (str (:openplanner-base-url config) "/v1" path))

(defn- openplanner-headers
  [config]
  (let [headers #js {"Content-Type" "application/json"}]
    (when-not (str/blank? (:openplanner-api-key config))
      (aset headers "X-API-Key" (:openplanner-api-key config)))
    headers))

(defn- fetch-json
  [url headers]
  (-> (js/fetch url #js {:headers headers})
      (.then (fn [resp]
               (if (.-ok resp)
                 (.json resp)
                 (-> (.text resp)
                     (.then (fn [text]
                              (throw (js/Error. (str "HTTP " (.-status resp) ": " text)))))))))))

(defn- post-json
  [url headers body]
  (-> (js/fetch url #js {:method "POST"
                         :headers headers
                         :body (.stringify js/JSON (clj->js body))})
      (.then (fn [resp]
               (if (.-ok resp)
                 (.json resp)
                 (-> (.text resp)
                     (.then (fn [text]
                              (throw (js/Error. (str "HTTP " (.-status resp) ": " text)))))))))))

(defn- format-translation-job
  [job]
  (let [job-id (str (aget job "_id"))
        document-id (str (aget job "document_id"))
        garden-id (str (aget job "garden_id"))
        source-lang (str (aget job "source_lang"))
        target-lang (str (aget job "target_language"))]
    {:job-id job-id
     :document-id document-id
     :garden-id garden-id
     :source-lang source-lang
     :target-lang target-lang}))

(defn- fetch-next-job
  [config]
  (let [url (openplanner-url config "/translations/jobs/next")]
    (-> (fetch-json url (openplanner-headers config))
        (.then (fn [result]
                 (when-let [job (aget result "job")]
                   (format-translation-job job)))))))

(defn- fetch-document-content
  [config document-id]
  (let [url (openplanner-url config (str "/documents/" document-id))]
    (-> (fetch-json url (openplanner-headers config))
        (.then (fn [doc]
                 (when doc
                   {:text (or (aget doc "text") (aget doc "extra" "content") "")
                    :title (or (aget doc "extra" "title") "Untitled")
                    :domain (aget doc "extra" "domain")}))))))

(defn- fetch-translation-examples
  [config source-lang target-lang limit]
  (let [url (openplanner-url config "/translations/examples")
        params (str "?source_lang=" source-lang "&target_lang=" target-lang "&limit=" (or limit 3))]
    (-> (fetch-json (str url params) (openplanner-headers config))
        (.then (fn [result]
                 (let [examples (aget result "examples")]
                   (mapv (fn [node]
                           {:source-text (aget node "source_text")
                            :target-text (aget node "target_text")})
                         (js-array-seq examples))))))))

(defn- mark-job-processing
  [config job-id]
  (let [url (openplanner-url config (str "/translations/jobs/" job-id "/status"))]
    (post-json url (openplanner-headers config) {:status "processing"})))

(defn- mark-job-complete
  [config job-id]
  (let [url (openplanner-url config (str "/translations/jobs/" job-id "/status"))]
    (post-json url (openplanner-headers config) {:status "complete"})))

(defn- mark-job-failed
  [config job-id error-message]
  (let [url (openplanner-url config (str "/translations/jobs/" job-id "/status"))]
    (post-json url (openplanner-headers config) {:status "failed" :error error-message})))

(defn- save-translation-segment
  [config segment]
  (let [url (openplanner-url config "/translations/segments")]
    (post-json url (openplanner-headers config) segment)))

(defn create-translation-tools
  "Create translation-specific tools for the agent runtime."
  [runtime config job document examples]
  (let [sdk (aget runtime "sdk")
        cwd (:workspace-root config)
        read-tool (aget sdk "createReadTool")]
    (vec
     (remove nil?
             [(when read-tool (read-tool cwd))
              ;; Custom tool: save_translation
              #js {:name "save_translation"
                   :description "Save a translated segment to the database. Use this after translating each segment."
                   :parameters #js {:type "object"
                                    :properties #js {:source_text #js {:type "string"
                                                                       :description "Original source text"}
                                                     :translated_text #js {:type "string"
                                                                           :description "Translated text"}
                                                     :segment_index #js {:type "number"
                                                                         :description "0-based index of this segment"}}
                                    :required #js ["source_text" "translated_text" "segment_index"]}
                   :execute (fn [params]
                              (let [source-text (aget params "source_text")
                                    translated-text (aget params "translated_text")
                                    segment-index (aget params "segment_index")
                                    segment {:source_text source-text
                                             :translated_text translated-text
                                             :source_lang (:source-lang job)
                                             :target_lang (:target-lang job)
                                             :document_id (:document-id job)
                                             :garden_id (:garden-id job)
                                             :segment_index segment-index
                                             :status "pending"
                                             :mt_model "agent"}]
                                (-> (save-translation-segment config segment)
                                    (.then (fn [_]
                                             #js {:success true
                                                  :message (str "Saved segment " segment-index)}))
                                    (.catch (fn [err]
                                              #js {:success false
                                                   :error (str err)})))))}
              ;; Custom tool: get_translation_context
              #js {:name "get_translation_context"
                   :description "Get the source document and any translation examples for context."
                   :parameters #js {:type "object"
                                    :properties #js {}
                                    :required #js []}
                   :execute (fn [_params]
                              (js/Promise.resolve
                               (clj->js {:document document
                                         :examples examples
                                         :job job})))}]))))

(defn build-translation-prompt
  "Build the system prompt for the translation agent."
  [job document examples]
  (let [source-lang (:source-lang job)
        target-lang (:target-lang job)
        title (:title document)
        example-str (when (seq examples)
                      (str "\n\nTranslation examples:\n"
                           (->> examples
                                (map (fn [{:keys [source-text target-text]}]
                                       (str "Source: " source-text "\n"
                                            "Target: " target-text)))
                                (str/join "\n\n"))))]
    (str "You are a professional translator. Translate the given document from " source-lang " to " target-lang ".\n\n"
         "Document: " title "\n"
         "Source language: " source-lang "\n"
         "Target language: " target-lang "\n\n"
         "INSTRUCTIONS:\n"
         "1. Read the source document using the read tool or get_translation_context\n"
         "2. Split the document into logical segments (respecting sentence boundaries)\n"
         "3. For each segment, translate naturally and accurately\n"
         "4. Use save_translation to save each translated segment with its index\n"
         "5. Output ONLY the translation - no explanations or alternatives\n\n"
         "When you have translated all segments, confirm completion."
         example-str)))

(defn process-translation-job
  "Process a single translation job using an agent session."
  [runtime config job]
  (println "[translation-agent] Processing job" (:job-id job) "for document" (:document-id job))
  (-> (mark-job-processing config (:job-id job))
      (.then (fn [] (fetch-document-content config (:document-id job))))
      (.then (fn [document]
               (if (nil? document)
                 (throw (js/Error. (str "Document " (:document-id job) " not found")))
                 document)))
      (.then (fn [document]
               (-> (fetch-translation-examples config (:source-lang job) (:target-lang job) 3)
                   (.then (fn [examples]
                            {:document document :examples examples}))))
      (.then (fn [{:keys [document examples]}]
               (let [conversation-id (str "translation-" (:job-id job))]
                 ;; Create agent session with translator role
                 (create-agent-session! runtime config conversation-id "glm-5"
                                        #js {:role "translator"
                                             :toolPolicies (clj->js
                                                            (mapv (fn [tool-id]
                                                                    {:toolId tool-id :effect "allow"})
                                                                  ["read" "save_translation" "get_translation_context"
                                                                   "memory_search" "memory_session" "graph_query"]))}
                                        "off"))))
      (.then (fn [session]
               (reset! current-session* session)
               ;; Send the translation request
               (let [session-manager (aget session "sessionManager")
                     user-message #js {:role "user"
                                       :content #js [#js {:type "text"
                                                          :text (str "Translate the document to " (:target-lang job)
                                                                     ". Use get_translation_context to see the source, "
                                                                     "then save each segment with save_translation.")}]
                                       :timestamp (.now js/Date)}]
                 (.appendMessage session-manager user-message)
                 ;; TODO: Trigger agent turn execution via agent_turns namespace
                 session)))
      (.then (fn [_session]
               ;; Mark job complete
               (mark-job-complete config (:job-id job))))
      (.catch (fn [err]
                (println "[translation-agent] Job failed:" (str err))
                (mark-job-failed config (:job-id job) (str err)))))))

(defn poll-and-process
  "Poll for jobs and process them. Spawn new session if more jobs remain."
  [runtime config]
  (when @running*
    (-> (fetch-next-job config)
        (.then (fn [job]
                 (if job
                   (-> (process-translation-job runtime config job)
                       (.then (fn []
                                ;; Check for more jobs - if found, continue processing
                                (fetch-next-job config)))
                       (.then (fn [next-job]
                                (when (and next-job @running*)
                                  ;; Recursively spawn new session for next job
                                  (js/setTimeout #(poll-and-process runtime config) 100)))))
                   ;; No job found, wait and poll again
                   (js/setTimeout #(poll-and-process runtime config) @poll-interval*))))
        (.catch (fn [err]
                  (println "[translation-agent] Poll error:" (str err))
                  (js/setTimeout #(poll-and-process runtime config) @poll-interval*))))))

(defn start-translation-agent!
  "Start the translation agent loop."
  [runtime config]
  (reset! running* true)
  (println "[translation-agent] Starting translation agent")
  (println "[translation-agent] OpenPlanner:" (:openplanner-base-url config))
  (poll-and-process runtime config))

(defn stop-translation-agent!
  "Stop the translation agent loop."
  []
  (reset! running* false)
  (reset! current-session* nil)
  (println "[translation-agent] Stopped"))


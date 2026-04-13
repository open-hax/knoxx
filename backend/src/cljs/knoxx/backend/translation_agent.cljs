(ns knoxx.backend.translation-agent
  "Translation agent that processes queued OpenPlanner translation jobs through the
   same Knoxx agent runtime used by chat/CMS, but with the restricted translator toolset."
  (:require [clojure.string :as str]
            [knoxx.backend.agent-turns :refer [send-agent-turn!]]))

(defonce poll-interval* (atom 5000))
(defonce running* (atom false))
(defonce current-run* (atom nil))

(def ^:private translator-tool-ids
  ["read" "memory_search" "memory_session" "graph_query" "save_translation"])

(defn- openplanner-url
  [config path]
  (str (:openplanner-base-url config) "/v1" path))

(defn- openplanner-headers
  [config]
  (let [headers #js {"Content-Type" "application/json"}]
    (when-not (str/blank? (:openplanner-api-key config))
      (aset headers "Authorization" (str "Bearer " (:openplanner-api-key config))))
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
  {:job-id (str (or (aget job "_id") (aget job "id")))
   :document-id (str (aget job "document_id"))
   :garden-id (some-> (aget job "garden_id") str)
   :source-lang (str (or (aget job "source_lang") "en"))
   :target-lang (str (or (aget job "target_language") (aget job "target_lang")))})

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
        (.then (fn [payload]
                 (let [doc (or (aget payload "document") payload)
                       extra (or (aget doc "extra") (js-obj))]
                   (when doc
                     {:text (or (aget doc "content")
                                (aget doc "text")
                                (aget extra "content")
                                (aget payload "content")
                                "")
                      :title (or (aget extra "title")
                                 (aget doc "title")
                                 "Untitled")
                      :domain (or (aget extra "domain")
                                  (aget doc "domain"))})))))))

(defn- fetch-translation-examples
  [config source-lang target-lang limit]
  (let [url (openplanner-url config "/translations/examples")
        params (str "?source_lang=" source-lang "&target_lang=" target-lang "&limit=" (or limit 3))]
    (-> (fetch-json (str url params) (openplanner-headers config))
        (.then (fn [result]
                 (let [examples (or (aget result "examples") #js [])]
                   (mapv (fn [node]
                           {:source-text (aget node "source_text")
                            :target-text (aget node "target_text")})
                         (array-seq examples))))))))

(defn- fetch-segments
  [config {:keys [document-id source-lang target-lang]}]
  (let [url (openplanner-url config "/translations/segments")
        params (str "?project=" (:project-name config)
                    "&document_id=" document-id
                    "&source_lang=" source-lang
                    "&target_lang=" target-lang
                    "&limit=100")]
    (-> (fetch-json (str url params) (openplanner-headers config))
        (.then (fn [result]
                 (mapv #(js->clj % :keywordize-keys true)
                       (array-seq (or (aget result "segments") #js []))))))))

(defn- fetch-segment-count
  [config job]
  (-> (fetch-segments config job)
      (.then count)))

(defn- identical-translation?
  [{:keys [source_text translated_text]}]
  (= (str/trim (or source_text ""))
     (str/trim (or translated_text ""))))

(defn- mark-job-status
  [config job-id payload]
  (let [url (openplanner-url config (str "/translations/jobs/" job-id "/status"))]
    (post-json url (openplanner-headers config) payload)))

(defn- mark-job-processing
  [config job-id]
  (mark-job-status config job-id {:status "processing"}))

(defn- mark-job-complete
  [config job-id]
  (mark-job-status config job-id {:status "complete"}))

(defn- mark-job-failed
  [config job-id error-message]
  (mark-job-status config job-id {:status "failed" :error error-message}))

(defn- translator-auth-context
  []
  {:roleSlugs ["translator"]
   :permissions []
   :membershipToolPolicies []
   :toolPolicies (mapv (fn [tool-id]
                         {:toolId tool-id :effect "allow"})
                       translator-tool-ids)
   :isSystemAdmin false})

(defn build-translation-prompt
  [config job document examples]
  (let [example-str (when (seq examples)
                      (str "\n\nReference examples:\n"
                           (->> examples
                                (map (fn [{:keys [source-text target-text]}]
                                       (str "- Source: " source-text "\n  Target: " target-text)))
                                (str/join "\n"))))]
    (str "You are the Knoxx translator agent. Translate the supplied source document from " (:source-lang job)
         " to " (:target-lang job) ".\n\n"
         "Rules:\n"
         "1. Preserve meaning, tone, markdown structure, links, and list structure where possible.\n"
         "2. Split the source into logical segments and call save_translation for every translated segment.\n"
         "3. Every save_translation call must include source_text, translated_text, source_lang, target_lang, document_id, project, and segment_index.\n"
         "4. Set project to '" (:project-name config) "'.\n"
         "5. The translated_text MUST be written in " (:target-lang job) "; do not copy the English source text into translated_text unless the segment is only a URL, code fence, or invariant proper noun.\n"
         "6. If target_lang differs from source_lang, normal prose segments should differ from source_text.\n"
         "7. Use memory_search, memory_session, or graph_query only if they help consistency; do not invent facts.\n"
         "8. Do not emit commentary, notes, or alternatives as the final answer. A short completion confirmation is enough after all segments are saved.\n\n"
         "Document metadata:\n"
         "- document_id: " (:document-id job) "\n"
         "- garden_id: " (or (:garden-id job) "") "\n"
         "- source_lang: " (:source-lang job) "\n"
         "- target_lang: " (:target-lang job) "\n"
         "- title: " (:title document) "\n"
         "- domain: " (or (:domain document) "") "\n\n"
         "Source document follows between the markers.\n"
         "<<<SOURCE_DOCUMENT\n"
         (:text document)
         "\n>>>SOURCE_DOCUMENT"
         example-str)))

(defn process-translation-job
  "Process one queued translation job through the Knoxx agent runtime."
  [runtime config job]
  (let [node-crypto (aget runtime "crypto")
        conversation-id (str "translation-" (:job-id job))
        session-id (.randomUUID node-crypto)
        run-id (.randomUUID node-crypto)]
    (println "[translation-agent] Processing job" (:job-id job) "document" (:document-id job) "target" (:target-lang job))
    (-> (mark-job-processing config (:job-id job))
        (.then (fn []
                 (fetch-document-content config (:document-id job))))
        (.then (fn [document]
                 (if (or (nil? document)
                         (str/blank? (:text document)))
                   (throw (js/Error. (str "Document " (:document-id job) " is missing translation content")))
                   document)))
        (.then (fn [document]
                 (-> (fetch-translation-examples config (:source-lang job) (:target-lang job) 3)
                     (.then (fn [examples]
                              {:document document
                               :examples examples})))))
        (.then (fn [{:keys [document examples]}]
                 (-> (fetch-segment-count config job)
                     (.then (fn [existing-count]
                              {:document document
                               :examples examples
                               :existing-count existing-count})))))
        (.then (fn [{:keys [document examples existing-count]}]
                 (reset! current-run* {:conversation-id conversation-id
                                       :session-id session-id
                                       :run-id run-id
                                       :job-id (:job-id job)})
                 (-> (send-agent-turn! runtime config {:conversation-id conversation-id
                                                       :session-id session-id
                                                       :run-id run-id
                                                       :message (build-translation-prompt config job document examples)
                                                       :mode "direct"
                                                       :thinking-level "off"
                                                       :auth-context (translator-auth-context)})
                     (.then (fn [_response]
                              {:existing-count existing-count})))))
        (.then (fn [{:keys [existing-count]}]
                 (-> (fetch-segments config job)
                     (.then (fn [segments]
                              {:existing-count existing-count
                               :segments segments})))))
        (.then (fn [{:keys [existing-count segments]}]
                 (let [saved-count (count segments)
                       created-count (- saved-count existing-count)
                       new-segments (if (pos? created-count)
                                      (take-last created-count segments)
                                      [])
                       identical-count (count (filter identical-translation? new-segments))]
                   (cond
                     (not (pos? created-count))
                     (throw (js/Error. (str "Translation run completed without saving new segments for job " (:job-id job))))

                     (= identical-count created-count)
                     (throw (js/Error. (str "Translation run saved only untranslated source text for job " (:job-id job))))

                     :else
                     (do
                       (println "[translation-agent] Saved" created-count "segments for job" (:job-id job)
                                "with" identical-count "unchanged segments")
                       (mark-job-complete config (:job-id job)))))))
        (.catch (fn [err]
                  (println "[translation-agent] Job failed:" (str err))
                  (mark-job-failed config (:job-id job) (str err))))
        (.finally (fn []
                    (reset! current-run* nil))))))

(defn poll-and-process
  [runtime config]
  (when @running*
    (-> (fetch-next-job config)
        (.then (fn [job]
                 (if job
                   (-> (process-translation-job runtime config job)
                       (.then (fn []
                                (when @running*
                                  (js/setTimeout #(poll-and-process runtime config) 100)))))
                   (js/setTimeout #(poll-and-process runtime config) @poll-interval*))))
        (.catch (fn [err]
                  (println "[translation-agent] Poll error:" (str err))
                  (js/setTimeout #(poll-and-process runtime config) @poll-interval*))))))

(defn start-translation-agent!
  [runtime config]
  (reset! running* true)
  (println "[translation-agent] Starting translation agent")
  (println "[translation-agent] OpenPlanner:" (:openplanner-base-url config))
  (poll-and-process runtime config))

(defn stop-translation-agent!
  []
  (reset! running* false)
  (reset! current-run* nil)
  (println "[translation-agent] Stopped"))

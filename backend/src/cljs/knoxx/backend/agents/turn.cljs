(ns knoxx.backend.agents.turn
  "Main turn orchestrator: send-agent-turn! and supporting lifecycle functions."
  (:require [clojure.string :as str]
            [knoxx.backend.agent-hydration :refer [settings-state* ensure-settings!
                                                   passive-hydration! passive-memory-hydration!
                                                   build-agent-user-message
                                                   hydration-sources]]
            [knoxx.backend.agent-runtime :refer [ensure-agent-session! remove-agent-session!]]
            [knoxx.backend.agents.content :as content :refer [model-ready-content-parts merge-content-parts]]
            [knoxx.backend.agents.policy :as policy]
            [knoxx.backend.agents.stream :as stream]
            [knoxx.backend.agents.transcript :as transcript]
            [knoxx.backend.authz :as authz :refer [auth-snapshot]]
            [knoxx.backend.core-memory :refer [extract-mentioned-devel-paths extract-mentioned-urls]]
            [knoxx.backend.extension-runtime :as ext-runtime]
            [knoxx.backend.openplanner-memory :as openplanner-memory]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.realtime :refer [broadcast-ws-session!]]
            [knoxx.backend.run-state :refer [store-run! append-run-event! update-run!
                                             finalize-run-trace-blocks! tool-event-payload
                                             record-retrieval-sample! latest-assistant-message]]
            [knoxx.backend.runtime.models :refer [effective-thinking-level normalize-thinking-level model-supports-input?]]
            [knoxx.backend.session-store :as session-store]
            [knoxx.backend.session-titles :refer [maybe-prime-session-title!]]
            [knoxx.backend.text :refer [assistant-message-text assistant-message-reasoning-text]]
            [knoxx.backend.turn-control :as turn-control]
            [knoxx.backend.util.time :refer [now-iso]]))

(defonce conversation-access* (atom {}))
(defonce lounge-messages* (atom []))

(defn ensure-conversation-access!
  [ctx conversation-id]
  (authz/ensure-conversation-access! conversation-access* ctx conversation-id))

(defn remember-conversation-access!
  [ctx conversation-id]
  (authz/remember-conversation-access! conversation-access* ctx conversation-id))

(defn ensure-session-id
  [node-crypto session-id]
  (or (content/nonblank session-id)
      (.randomUUID node-crypto)))

(defn- split-think-tags
  "Extract a leading <think>...</think> block from assistant text.

   Some Gemma-family models emit thinking traces inline instead of as structured
   reasoning parts. This keeps the assistant answer clean while preserving
   the trace in :reasoning." 
  [text]
  (let [text (str (or text ""))
        open-idx (.indexOf text "<think>")
        close-idx (.indexOf text "</think>")]
    (if (and (>= open-idx 0)
             (>= close-idx 0)
             (< open-idx 64)
             (> close-idx open-idx))
      (let [thinking (subs text (+ open-idx (count "<think>")) close-idx)
            after (subs text (+ close-idx (count "</think>")))
            before (subs text 0 open-idx)
            answer (str (or before "") (or after ""))]
        {:reasoning (str/trim thinking)
         :answer (str/trim answer)
         :hadThinkTags true})
      {:reasoning ""
       :answer text
       :hadThinkTags false})))

(defn- agent-spec-summary
  [agent-spec]
  (when agent-spec
    (cond-> {}
      (:contract-id agent-spec) (assoc :contractId (:contract-id agent-spec))
      (:actor-id agent-spec) (assoc :actorId (:actor-id agent-spec))
      (seq (:contract-actors agent-spec)) (assoc :contractActors (vec (:contract-actors agent-spec)))
      (:role agent-spec) (assoc :role (:role agent-spec))
      (:model agent-spec) (assoc :model (:model agent-spec))
      (:thinking-level agent-spec) (assoc :thinkingLevel (:thinking-level agent-spec))
      (:system-prompt agent-spec) (assoc :hasSystemPrompt true)
      (:task-prompt agent-spec) (assoc :hasTaskPrompt true)
      (seq (:tool-policies agent-spec)) (assoc :toolPolicies (vec (:tool-policies agent-spec)))
      (:resource-policies agent-spec) (assoc :resourcePolicies (:resource-policies agent-spec)))))

(defn- create-initial-run!
  [run-id session-id conversation-id started-at model-id mode thinking-level
   agent-spec auth-extra request-messages config]
  (let [base-run (merge {:run_id run-id
                         :session_id session-id
                         :conversation_id conversation-id
                         :created_at started-at
                         :updated_at started-at
                         :status "running"
                         :model model-id
                         :ttft_ms nil
                         :total_time_ms nil
                         :input_tokens nil
                         :output_tokens nil
                         :tokens_per_s nil
                         :error nil
                         :answer nil
                         :content_parts []
                         :events []
                         :trace_blocks []
                         :tool_receipts []
                         :request_messages request-messages
                         :settings (cond-> {:sessionId session-id
                                            :conversationId conversation-id
                                            :mode mode
                                            :thinkingLevel thinking-level
                                            :workspaceRoot (:workspace-root config)}
                                     agent-spec (assoc :agentSpec (agent-spec-summary agent-spec)))
                         :resources (cond-> {:provider "proxx"
                                             :collection (:collection-name config)}
                                      (get agent-spec :resource-policies) (assoc :agentResourcePolicies (get agent-spec :resource-policies)))}
                        auth-extra)]
    (store-run! run-id base-run)
    (session-store/put-session! (redis/get-client)
                                (merge (cond-> {:session_id session-id
                                                :conversation_id conversation-id
                                                :run_id run-id
                                                :status "running"
                                                :model model-id
                                                :mode mode
                                                :thinking_level thinking-level
                                                :created_at started-at
                                                :updated_at started-at
                                                :has_active_stream false
                                                :messages request-messages}
                                         agent-spec (assoc :agent_spec (agent-spec-summary agent-spec)))
                                       auth-extra))
    (let [initial-event (tool-event-payload run-id conversation-id session-id "run_started"
                                            {:status "running"
                                             :mode mode
                                             :model model-id
                                             :thinking_level thinking-level})]
      (append-run-event! run-id initial-event)
      (broadcast-ws-session! session-id "events" initial-event))))

(defn- finalize-turn-success!
  [config state session run-id conversation-id session-id started-ms model-id mode
   hydration memory-hydration persisted-request-messages]
  (let [assistant-message (latest-assistant-message session)
        answer (let [chunked (apply str @(:chunks state))]
                 (if (str/blank? chunked)
                   (assistant-message-text assistant-message)
                   chunked))
        assistant-content-parts (content/assistant-content-parts assistant-message)
        usage (or (aget assistant-message "usage") #js {})
        reasoning-text (let [streamed (apply str @(:reasoning-chunks state))
                             final-reasoning (assistant-message-reasoning-text assistant-message)]
                         (cond
                           (and (str/blank? streamed) (not (str/blank? final-reasoning))) final-reasoning
                           (and (not (str/blank? final-reasoning)) (> (count final-reasoning) (count streamed))) final-reasoning
                           :else streamed))
        ;; If the provider didn't supply structured reasoning, try to extract a
        ;; leading <think>...</think> block from the assistant message.
        think-split (when (str/blank? (str reasoning-text))
                      (split-think-tags answer))
        answer (if (and think-split (:hadThinkTags think-split))
                 (:answer think-split)
                 answer)
        reasoning-text (if (and think-split (:hadThinkTags think-split))
                         (:reasoning think-split)
                         reasoning-text)
        elapsed (- (.now js/Date) started-ms)
        output-tokens (or (aget usage "output") 0)
        tokens-per-second (when (and (pos? output-tokens) (pos? elapsed))
                            (* 1000 (/ output-tokens elapsed)))
        sources (hydration-sources hydration)
        message-parts (cond-> []
                        (not (str/blank? reasoning-text))
                        (conj {:role "thinking"
                               :content reasoning-text
                               :reasoningType "reasoning_summary"})
                        (not (str/blank? answer))
                        (conj {:role "assistant"
                               :content answer}))
        completed-event (tool-event-payload run-id conversation-id session-id "run_completed"
                                            {:status "completed"
                                             :model model-id
                                             :sources_count (count sources)})]
    (when (= mode "rag")
      (record-retrieval-sample! (:retrievalMode @settings-state*) elapsed))
    (finalize-run-trace-blocks! run-id "done")
    (let [completed-run (update-run! run-id
                                     (fn [run]
                                       (let [resource-patch (cond-> {:sources sources}
                                                               hydration (assoc :passiveHydration (select-keys hydration [:query :tokens :database :elapsedMs :results]))
                                                               memory-hydration (assoc :memoryHydration (select-keys memory-hydration [:query :mode :hits :elapsedMs :conversationId])))
                                             merged-content-parts (merge-content-parts assistant-content-parts
                                                                                      (content/reply-attachment-content-parts (:tool_receipts run)))]
                                         (-> run
                                             (assoc :updated_at (now-iso)
                                                    :status "completed"
                                                    :total_time_ms elapsed
                                                    :input_tokens (or (aget usage "input") 0)
                                                    :output_tokens output-tokens
                                                    :tokens_per_s tokens-per-second
                                                    :answer answer
                                                    :content_parts merged-content-parts
                                                    :reasoning reasoning-text
                                                    :sources sources)
                                             (update :resources merge resource-patch)))))
          merged-content-parts (vec (or (:content_parts completed-run) assistant-content-parts))
          response {:answer answer
                    :run_id run-id
                    :runId run-id
                    :conversation_id conversation-id
                    :conversationId conversation-id
                    :session_id session-id
                    :model model-id
                    :content_parts merged-content-parts
                    :sources sources
                    :message_parts message-parts
                    :compare nil}
          _ (when completed-run
              (openplanner-memory/index-run-memory! config completed-run extract-mentioned-devel-paths extract-mentioned-urls))]
      (append-run-event! run-id completed-event)
      (broadcast-ws-session! session-id "events" completed-event)
      (let [final-messages (transcript/transcript-after-turn session
                                                             (conj persisted-request-messages
                                                                   (cond-> {:role "assistant"
                                                                            :content answer}
                                                                     (seq merged-content-parts) (assoc :content-parts merged-content-parts))))]
        (session-store/complete-session! (redis/get-client)
                                         session-id
                                         conversation-id
                                         {:status "completed"
                                          :answer answer
                                          :messages final-messages}))
      (remove-agent-session! conversation-id)
      response)))

(defn- finalize-turn-failure!
  [config state session run-id conversation-id session-id started-ms
   hydration memory-hydration persisted-request-messages err]
  (let [err-text (or @(:abort-reason* state) (str err))
        error-event (tool-event-payload run-id conversation-id session-id "run_failed"
                                        {:status "failed"
                                         :error err-text})]
    (finalize-run-trace-blocks! run-id "error")
    (let [failed-run (update-run! run-id
                                  (fn [run]
                                    (let [resource-patch (cond-> {}
                                                           hydration (assoc :passiveHydration (select-keys hydration [:query :tokens :database :elapsedMs :results]))
                                                           memory-hydration (assoc :memoryHydration (select-keys memory-hydration [:query :mode :hits :elapsedMs :conversationId])))]
                                      (-> run
                                          (assoc :updated_at (now-iso)
                                                 :status "failed"
                                                 :total_time_ms (- (.now js/Date) started-ms)
                                                 :reasoning (apply str @(:reasoning-chunks state))
                                                 :error err-text)
                                          (update :resources merge resource-patch)))))
          _ (when failed-run
              (openplanner-memory/index-run-memory! config failed-run extract-mentioned-devel-paths extract-mentioned-urls))]
      (append-run-event! run-id error-event)
      (broadcast-ws-session! session-id "events" error-event)
      (let [final-messages (transcript/transcript-after-turn session persisted-request-messages)]
        (session-store/complete-session! (redis/get-client)
                                         session-id
                                         conversation-id
                                         {:status "failed"
                                          :error err-text
                                          :messages final-messages}))
      (remove-agent-session! conversation-id))
    (throw err)))

(defn- prompt-and-await!
  [runtime config session-id run-id conversation-id started-ms model-id mode
   session message prompt-content-parts hydration memory-hydration
   persisted-request-messages agent-spec]
  (letfn [(content-part-type [part]
            (cond
              (keyword? (:type part)) (name (:type part))
              (string? (:type part)) (:type part)
              :else nil))

          (data-url->image-attachment [raw]
            (when (and (string? raw) (str/starts-with? raw "data:"))
              (let [[meta b64] (str/split raw #"," 2)
                    meta (or meta "")
                    mime (some-> meta
                                 (str/replace-first #"^data:" "")
                                 (str/split #";" 2)
                                 first
                                 str/trim
                                 content/nonblank)
                    b64 (content/nonblank b64)]
                (when b64
                  {:data b64
                   :mimeType mime}))))

          (image-part->pi-attachment [part]
            (when (= "image" (content-part-type part))
              (let [raw-data (content/nonblank (:data part))
                    parsed (data-url->image-attachment raw-data)
                    data (or (:data parsed) raw-data)
                    ;; NOTE: pi-coding-agent expects raw base64 in :data (not a data: URL)
                    mime-type (or (content/nonblank (:mimeType part))
                                  (:mimeType parsed))]
                (when data
                  (cond-> {:type "image"
                           :data data}
                    (content/nonblank mime-type) (assoc :mimeType mime-type))))))]

    (let [state (stream/make-stream-state run-id conversation-id session-id (now-iso) started-ms (aget runtime "crypto"))
          abort! (fn [reason] (stream/request-abort! state session reason))
          _registered (stream/register-active-turn! state abort!)
          unsubscribe (.subscribe session (stream/build-subscribe-handler state session))
          parts (or prompt-content-parts [])
          images (->> parts (keep image-part->pi-attachment) vec)
          omitted-count (max 0 (- (count parts) (count images)))
          base-text (build-agent-user-message message hydration memory-hydration)
          final-text (cond-> base-text
                       (pos? omitted-count)
                       (str "\n\n" "[Note: " omitted-count " non-image attachment(s) were omitted because the current pi AgentSession API only supports image attachments.]"))
          content (if (seq images)
                    (clj->js (into [{:type "text" :text final-text}] images))
                    final-text)]
      (-> (.sendUserMessage session content)
          (.then (fn []
                   (unsubscribe)
                   (turn-control/unregister-active-turn! conversation-id run-id)
                   (finalize-turn-success! config state session run-id conversation-id session-id started-ms model-id mode
                                           hydration memory-hydration persisted-request-messages)))
          (.catch (fn [err]
                    (unsubscribe)
                    (turn-control/unregister-active-turn! conversation-id run-id)
                    (finalize-turn-failure! config state session run-id conversation-id session-id started-ms
                                            hydration memory-hydration persisted-request-messages err)))))))

(defn send-agent-turn!
  [runtime config {:keys [conversation-id session-id message content-parts model mode run-id auth-context thinking-level agent-spec]}]
  (let [node-crypto (aget runtime "crypto")
        conversation-id (or conversation-id (.randomUUID node-crypto))
        session-id (ensure-session-id node-crypto session-id)
        _ (ensure-conversation-access! auth-context conversation-id)
        _ (remember-conversation-access! auth-context conversation-id)
        mode (or mode "direct")
        requested-model (or model (:model agent-spec))
        model-id (or requested-model (:llmModel (ensure-settings! config)) (:proxx-default-model config))
        thinking-level-raw (or thinking-level (:thinking-level agent-spec))
        parsed-thinking-level (when thinking-level-raw
                                (normalize-thinking-level thinking-level-raw))
        thinking-level (effective-thinking-level config model-id (or parsed-thinking-level
                                                                    thinking-level-raw
                                                                    (:agent-thinking-level config)
                                                                    "off"))
        run-id (or run-id (.randomUUID node-crypto))
        started-at (now-iso)
        started-ms (.now js/Date)
        existing-messages (vec (or (:messages (session-store/get-session-sync session-id)) []))
seeded-messages (->> (transcript/ensure-system-message existing-messages agent-spec)
                     ;; Strip historical :content-parts (base64 images) from seeded context.
                     ;; Rationale: accumulated base64 from prior turns bloats the
                     ;; LLM request payload — especially in sticky event-agent sessions
                     ;; that fire on every Discord message. Only the current turn needs
                     ;; image data; history is adequately represented by text content.
                     (mapv #(dissoc % :content-parts)))
        auth-extra (auth-snapshot auth-context)]
    (cond
      (and thinking-level-raw (nil? parsed-thinking-level))
      (js/Promise.reject (js/Error. (str "Unsupported thinking level: " thinking-level-raw ". Expected one of off, minimal, low, medium, high, xhigh.")))

      :else
      (-> (policy/enforce-chat-policy! auth-context model-id)
          (.then (fn [_]
                   (maybe-prime-session-title! runtime config conversation-id message)

                   ;; -------------------------------------------------------------------
                   ;; Multimodal correctness: if content parts include remote image URLs,
                   ;; download them and embed as data: URLs.
                   ;;
                   ;; Why:
                   ;; - Some upstream providers (notably Ollama-compatible endpoints) reject
                   ;;   remote URLs and require base64 image input.
                   ;; - Storing only URLs breaks the "OpenPlanner is truth" invariant because
                   ;;   restored sessions can no longer replay the exact multimodal context.
                   ;; -------------------------------------------------------------------
                   (let [max-bytes 8000000
                         looks-like-url? (fn [value]
                                           (and (string? value)
                                                (or (str/starts-with? value "http://")
                                                    (str/starts-with? value "https://"))))
                         data-url? (fn [value]
                                     (and (string? value) (str/starts-with? value "data:")))
                         content-part-type (fn [part]
                                             (cond
                                               (keyword? (:type part)) (name (:type part))
                                               (string? (:type part)) (:type part)
                                               :else nil))
                         fetch-image-data-url! (fn [url]
                                                (-> (js/fetch url #js {:method "GET"})
                                                    (.then (fn [resp]
                                                             (when-not (.-ok resp)
                                                               (throw (js/Error. (str "Failed to fetch image: HTTP " (.-status resp)))))
                                                             (let [len-h (some-> resp (.-headers) (.get "content-length"))
                                                                   len (when len-h (js/parseInt len-h 10))]
                                                               (when (and (number? len) (pos? len) (> len max-bytes))
                                                                 (throw (js/Error. (str "Remote image exceeds max bytes: " len))))
                                                             (-> (.arrayBuffer resp)
                                                                 (.then (fn [ab]
                                                                          (let [buf (js/Buffer.from ab)
                                                                                size (.-length buf)
                                                                                _ (when (> size max-bytes)
                                                                                    (throw (js/Error. (str "Remote image exceeds max bytes: " size))))
                                                                                mime (or (some-> resp (.-headers) (.get "content-type")) "image/png")
                                                                                b64 (.toString buf "base64")]
                                                                            (str "data:" mime ";base64," b64))))))))))
                         materialize-part! (fn [part]
                                            (let [part-type (content-part-type part)]
                                              (cond
                                                (not= part-type "image") (js/Promise.resolve part)

                                                ;; Already data: URL.
                                                (data-url? (:data part)) (js/Promise.resolve part)

                                                ;; If :data is a URL (legacy / buggy), treat it as :url.
                                                (looks-like-url? (:data part))
                                                (-> (fetch-image-data-url! (:data part))
                                                    (.then (fn [data-url]
                                                             (-> part
                                                                 (dissoc :url)
                                                                 (assoc :data data-url)))))

                                                (looks-like-url? (:url part))
                                                (-> (fetch-image-data-url! (:url part))
                                                    (.then (fn [data-url]
                                                             (-> part
                                                                 (dissoc :url)
                                                                 (assoc :data data-url)))))

                                                :else (js/Promise.resolve part))))
                         materialize-content-parts! (fn [parts]
                                                      (let [parts (vec (or parts []))]
                                                        (if-not (and (seq parts)
                                                                     (model-supports-input? config model-id "image"))
                                                          (js/Promise.resolve parts)
                                                          (-> (js/Promise.all (clj->js (map materialize-part! parts)))
                                                              (.then (fn [items]
                                                                       (vec (array-seq items))))))))]

                     (-> (.all js/Promise
                               #js [(passive-hydration! runtime config mode message auth-context)
                                    (passive-memory-hydration! config conversation-id message auth-context)
                                    (materialize-content-parts! content-parts)])
                         (.then (fn [results]
                                  (let [hydration (aget results 0)
                                        memory-hydration (aget results 1)
                                        materialized-content-parts (vec (or (aget results 2) []))
                                        user-message (if (seq materialized-content-parts)
                                                       {:role "user" :content message :content-parts materialized-content-parts}
                                                       {:role "user" :content message})
                                        prompt-content-parts (model-ready-content-parts config model-id materialized-content-parts)
                                        request-messages (conj seeded-messages user-message)]

                                    (create-initial-run! run-id session-id conversation-id started-at model-id mode thinking-level
                                                         agent-spec auth-extra request-messages config)
                                  (when hydration
                                    (let [hydration-event (tool-event-payload run-id conversation-id session-id "passive_hydration"
                                                                              {:status "ok"
                                                                               :hits (count (:results hydration))
                                                                               :elapsed_ms (:elapsedMs hydration)})]
                                      (update-run! run-id
                                                   (fn [run]
                                                     (-> run
                                                         (update :resources merge {:passiveHydration (select-keys hydration [:query :tokens :database :elapsedMs :results])})
                                                         (assoc :updated_at (now-iso)))))
                                      (append-run-event! run-id hydration-event)
                                      (broadcast-ws-session! session-id "events" hydration-event)))
                                  (when (seq (:hits memory-hydration))
                                    (let [memory-event (tool-event-payload run-id conversation-id session-id "memory_hydration"
                                                                           {:status "ok"
                                                                            :hits (count (:hits memory-hydration))
                                                                            :elapsed_ms (:elapsedMs memory-hydration)})]
                                      (update-run! run-id
                                                   (fn [run]
                                                     (-> run
                                                         (update :resources merge {:memoryHydration (select-keys memory-hydration [:query :mode :hits :elapsedMs :conversationId])})
                                                         (assoc :updated_at (now-iso)))))
                                      (append-run-event! run-id memory-event)
                                      (broadcast-ws-session! session-id "events" memory-event)))
                                  (-> (ensure-agent-session! runtime config conversation-id model-id auth-context thinking-level session-id agent-spec)
                                      (.then (fn [session]
                                               (let [persisted-request-messages (transcript/transcript-before-prompt session user-message agent-spec)]
                                                 (session-store/update-session! (redis/get-client)
                                                                                session-id
                                                                                {:status "running"
                                                                                 :has_active_stream false
                                                                                 :messages persisted-request-messages
                                                                                 :conversation_id conversation-id
                                                                                 :run_id run-id})
                                                 (prompt-and-await! runtime config session-id run-id conversation-id started-ms model-id mode
                                                                    session message prompt-content-parts hydration memory-hydration
                                                                    persisted-request-messages agent-spec)))))))))))))))
)
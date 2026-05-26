(ns knoxx.backend.infra.agent.turn
  "Main turn orchestrator: send-agent-turn! and supporting lifecycle functions."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.agent.hydration :refer [settings-state* ensure-settings!
                                                                passive-hydration! passive-memory-hydration!
                                                                build-agent-user-message
                                                                hydration-sources]]
            [knoxx.backend.infra.agent.session :refer [ensure-agent-session! remove-agent-session! prune-session-messages]]
            [knoxx.backend.infra.agent.message :as msg]
            [knoxx.backend.extern.agent-turn-media :as xturn-media]
            [knoxx.backend.extern.agent-turn-node :as xturn-node]
            [knoxx.backend.extern.agent-turn-prompt :as xturn-prompt]
            [knoxx.backend.extern.agent-turn-result :as xturn-result]
            [knoxx.backend.extern.promise :as xpromise]
            [knoxx.backend.domain.agent.agent-templates :as templates]
            [knoxx.backend.domain.agent.content :as content :refer [model-ready-content-parts merge-content-parts]]
            [knoxx.backend.infra.agent.policy :as policy]
            [knoxx.backend.infra.agent.stream :as stream]
            [knoxx.backend.infra.agent.transcript :as transcript]
            [knoxx.backend.infra.auth.authz :as authz :refer [auth-snapshot]]
            [knoxx.backend.infra.core-memory :refer [extract-mentioned-devel-paths extract-mentioned-urls]]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.openplanner.memory :as openplanner-memory]
            [knoxx.backend.domain.media :as media]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.domain.realtime :refer [broadcast-ws-session!]]
            [knoxx.backend.domain.action.run-state :refer [store-run! append-run-event! update-run!
                                                           finalize-run-trace-blocks! tool-event-payload
                                                           record-retrieval-sample! latest-assistant-message
                                                           set-event-stream-sink! clear-event-stream-sink!]]
            [knoxx.backend.domain.models :refer [effective-thinking-level normalize-thinking-level model-supports-input?]]
            [knoxx.backend.shape.agent :refer [send-user-message! subscribe!]]
            [knoxx.backend.infra.stores.session-store :as session-store]
            [knoxx.backend.infra.stores.session-store-registry :as store-registry]
            [knoxx.backend.shape.session-persistence :refer [put-run!]]
            [knoxx.backend.infra.stores.session-titles :refer [maybe-prime-session-title!]]
            [knoxx.backend.domain.text :refer [assistant-message-text assistant-message-reasoning-text]]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.domain.agent.agent-context :as agent-ctx]
            [knoxx.backend.domain.time :refer [now-iso]]))

(defonce conversation-access* (atom {}))
(defonce lounge-messages* (atom []))


(defn ensure-conversation-access!
  [ctx conversation-id]
  (authz/ensure-conversation-access! conversation-access* ctx conversation-id))

(defn remember-conversation-access!
  [ctx conversation-id]
  (authz/remember-conversation-access! conversation-access* ctx conversation-id))

(defn- auth-context-for-agent-turn
  [auth-context agent-spec]
  (let [agent-actor-id (some-> (:actor-id agent-spec) str str/trim not-empty)
        needs-context? (or auth-context
                           agent-actor-id
                           (seq (:tool-policies agent-spec))
                           (:role agent-spec))]
    (when needs-context?
      (cond-> (or auth-context {})
        agent-actor-id (assoc :actorId agent-actor-id)
        (and (nil? auth-context) (seq (:tool-policies agent-spec)))
        (assoc :toolPolicies (vec (:tool-policies agent-spec)))
        (and (nil? auth-context) (:role agent-spec))
        (assoc :roleSlugs [(:role agent-spec)])))))

(defn ensure-session-id
  [session-id]
  (or (content/nonblank session-id)
      (xturn-node/random-uuid!)))



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
      (seq (:tool-policies agent-spec)) (assoc :toolPolicies (vec (:tool-policies agent-spec)))
      (:resource-policies agent-spec) (assoc :resourcePolicies (:resource-policies agent-spec))
      (:context-policy agent-spec) (assoc :contextPolicy (:context-policy agent-spec))
      (:sub-agent-id agent-spec) (assoc :subAgentId (:sub-agent-id agent-spec))
      (:parent-agent-id agent-spec) (assoc :parentAgentId (:parent-agent-id agent-spec))
      (:parent-run-id agent-spec) (assoc :parentRunId (:parent-run-id agent-spec))
      (:spawn-kind agent-spec) (assoc :spawnKind (:spawn-kind agent-spec))
      (:trigger-id agent-spec) (assoc :triggerId (:trigger-id agent-spec))
      (:event-type agent-spec) (assoc :eventType (:event-type agent-spec))
      (seq (:event-types agent-spec)) (assoc :eventTypes (vec (:event-types agent-spec)))
      (:event-id agent-spec) (assoc :eventId (:event-id agent-spec))
      (:event-scope-id agent-spec) (assoc :eventScopeId (:event-scope-id agent-spec))
      (:schedule-id agent-spec) (assoc :scheduleId (:schedule-id agent-spec)))))

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
    (when-let [store @store-registry/session-store*]
      (-> (put-run! store base-run)
          (.catch (fn [err]
                    (.warn js/console "[turn] failed to persist initial run"
                           (clj->js {:run-id run-id
                                     :error (ex-message err)
                                     :error-data (clj->js (or (ex-data err) {}))}))))))
    (set-event-stream-sink!
     (fn [event]
       (let [client (openplanner-client/client config)]
         (when (openplanner-client/enabled? client)
           (-> (openplanner-client/events!
                client
                [(openplanner-memory/openplanner-event
                  config
                  {:id        (str (:run_id event) ":"
                                   (:type event) ":"
                                   (:at event))
                   :ts        (:at event)
                   :kind      (str "knoxx." (:type event))
                   :session   (:conversation_id event)
                   :message   (:run_id event)
                   :role      "system"
                   :text      (str (:type event)
                                   (when (:tool_name event)
                                     (str ": " (:tool_name event)))
                                   (when (:preview event)
                                     (str "\n" (:preview event))))
                   :extra     event})])
               (.catch (fn [_] nil)))))))
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
   hydration memory-hydration persisted-request-messages agent-spec]
  (let [assistant-message (latest-assistant-message session)
        answer (let [chunked (apply str @(:chunks state))]
                 (if (str/blank? chunked)
                   (assistant-message-text assistant-message)
                   chunked))
        assistant-content-parts (content/assistant-content-parts assistant-message)
        usage-tokens (xturn-result/usage-tokens assistant-message)
        reasoning-text (let [streamed (apply str @(:reasoning-chunks state))
                             final-reasoning (assistant-message-reasoning-text assistant-message)]
                         (cond
                           (and (str/blank? streamed) (not (str/blank? final-reasoning))) final-reasoning
                           (and (not (str/blank? final-reasoning)) (> (count final-reasoning) (count streamed))) final-reasoning
                           :else streamed))
        ;; If the provider didn't supply structured reasoning, try to extract a
        ;; leading <think>...</think> block from the assistant message.
        think-split (when (str/blank? (str reasoning-text))
                      (msg/split-think-tags answer))
        answer (if (and think-split (:hadThinkTags think-split))
                 (:answer think-split)
                 answer)
        reasoning-text (if (and think-split (:hadThinkTags think-split))
                         (:reasoning think-split)
                         reasoning-text)
        elapsed (- (.now js/Date) started-ms)
        output-tokens (:output-tokens usage-tokens)
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
    (record-retrieval-sample! (:retrievalMode @settings-state*) elapsed)
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
                                                    :input_tokens (:input-tokens usage-tokens)
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
      (let [final-messages (prune-session-messages
                            agent-spec
                            (transcript/transcript-after-turn session
                                                              (conj persisted-request-messages
                                                                    (cond-> {:role "assistant"
                                                                             :content answer}
                                                                      (seq merged-content-parts) (assoc :content-parts merged-content-parts)))))]
        (session-store/complete-session! (redis/get-client)
                                         session-id
                                         conversation-id
                                         {:status "completed"
                                          :answer answer
                                          :messages final-messages}))
      (clear-event-stream-sink!)
      (remove-agent-session! conversation-id)
      response)))

(defn- finalize-turn-failure!
  [config state session run-id conversation-id session-id started-ms
   hydration memory-hydration persisted-request-messages agent-spec err]
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
      (let [final-messages (prune-session-messages agent-spec (transcript/transcript-after-turn session persisted-request-messages))]
        (session-store/complete-session! (redis/get-client)
                                         session-id
                                         conversation-id
                                         {:status "failed"
                                          :error err-text
                                          :messages final-messages}))
      (clear-event-stream-sink!)
      (remove-agent-session! conversation-id))
    (throw err)))
(defn content-part-type [part]
  (cond
    (keyword? (:type part)) (name (:type part))
    (string? (:type part)) (:type part)
    :else nil))
(defn data-url->image-attachment [raw]
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

(defn base64-bytes
  [b64]
  (when-let [b64 (content/nonblank b64)]
    (let [len (count b64)
          padding (cond
                    (str/ends-with? b64 "==") 2
                    (str/ends-with? b64 "=") 1
                    :else 0)]
      (max 0 (- (js/Math.floor (* 3 (/ len 4))) padding)))))
(defn media-part->eta-mu-attachment [part]
  (let [part-type (content-part-type part)]
    (when (contains? #{"image" "audio" "video" "document"} part-type)
      (let [raw-data (content/nonblank (:data part))
            parsed (when (= "image" part-type)
                     (data-url->image-attachment raw-data))
            data (or (:data parsed) (xturn-media/strip-data-url raw-data))
            mime-type (content/nonblank
                       (some-> (or (:mimeType part) (:mimeType parsed))
                               name))
            filename (content/nonblank (:filename part))]
        (when data
          (cond-> {:type part-type
                   :data data}
            mime-type (assoc :mimeType mime-type)
            (and filename (not= part-type "audio")) (assoc :filename filename)))))))
(defn file-processor-style-marker
  [media-part]
  (let [t (content-part-type media-part)
        mime (or (content/nonblank (:mimeType media-part))
                 (when (= t "audio") "audio/mpeg")
                 (when (= t "image") "image/png"))
        name (case t
               "audio" (if (= mime "audio/wav") "attached-audio.wav" "attached-audio.mp3")
               "image" "attached-image"
               "video" "attached-video"
               "document" "attached-document"
               (str "attached-" t))
        bytes (or (:size media-part) (base64-bytes (:data media-part)))]
    (cond
      (= t "audio")
      (str "<file name=\"" name "\">[Audio attached: " mime ", " (or bytes "?") " bytes.]</file>\n")

      (= t "image")
      (str "<file name=\"" name "\"></file>\n")

      (= t "video")
      (str "<file name=\"" name "\">[Video attached: " mime ", " (or bytes "?") " bytes.]</file>\n")

      (= t "document")
      (str "<file name=\"" name "\">[Document attached: " mime ", " (or bytes "?") " bytes.]</file>\n")

      :else "")))
(defn- prompt-and-await!
  [config session-id run-id conversation-id started-ms model-id mode
   session message prompt-content-parts hydration memory-hydration
   persisted-request-messages agent-spec]
  (let [state (stream/make-stream-state run-id conversation-id session-id (now-iso) started-ms xturn-node/random-uuid!)
        abort! (fn [reason] (stream/request-abort! state session reason))
        _registered (stream/register-active-turn! state abort! agent-spec)
        unsubscribe (subscribe! session (stream/build-subscribe-handler state session))
        parts (or prompt-content-parts [])
        media-parts (->> parts (keep media-part->eta-mu-attachment) vec)
        omitted-count (max 0 (- (count parts) (count media-parts)))
        turn-message (or (content/nonblank message)
                         (content/nonblank (:task-prompt agent-spec))
                         "")
        attachment-markers (when (seq media-parts)
                             (apply str (map file-processor-style-marker media-parts)))
        base-text (str (or attachment-markers "")
                       (build-agent-user-message turn-message hydration memory-hydration))
        final-text (cond-> base-text
                     (pos? omitted-count)
                     (str "\n\n" "[Note: " omitted-count " unsupported attachment(s) were omitted for this model/runtime.]"))
        content (xturn-prompt/prompt-content media-parts final-text)]
    (xturn-prompt/log-prompt! {:run-id run-id
                               :session-id session-id
                               :conversation-id conversation-id
                               :model-id model-id
                               :mode mode
                               :parts-count (count parts)
                               :media-parts-count (count media-parts)
                               :omitted-count omitted-count
                               :content content})
    (agent-ctx/set-context! {:session-id session-id
                             :conversation-id conversation-id
                             :run-id run-id
                             :agent-spec agent-spec})
    (-> (send-user-message! session content)
        (.then (fn [_]
                 (agent-ctx/clear-context!)
                 (unsubscribe)
                 (finalize-turn-success!
                  config state
                  session run-id conversation-id session-id started-ms model-id mode
                  hydration memory-hydration persisted-request-messages agent-spec)))
        (.catch (fn [err]
                  (agent-ctx/clear-context!)
                  (unsubscribe)
                  (turn-control/unregister-active-turn! conversation-id run-id)
                  (finalize-turn-failure! config state session run-id conversation-id session-id started-ms
                                          hydration memory-hydration persisted-request-messages agent-spec err))))))



(defn studio-stream-path
  [value]
  (xturn-media/studio-stream-path value))

(defn read-workspace-media-data-url!
  [runtime config max-bytes raw-path fallback-mime label]
  (let [normalized (media/normalize-tool-path-arg raw-path)
        {:keys [absolute relative]} (media/resolve-workspace-media-path runtime config normalized)
        mime (or (media/workspace-media-mime-type relative) fallback-mime)]
    (xturn-node/file-data-url! absolute mime label max-bytes)))


(defn fetch-media-data-url!
  [runtime config auth-context max-bytes url fallback-mime label]
  (if-let [stream-path (studio-stream-path url)]
    (read-workspace-media-data-url! runtime config max-bytes stream-path fallback-mime label)
    (xturn-media/fetch-data-url! url fallback-mime label max-bytes auth-context)))

(defn materialize-part!
  [runtime config auth-context max-bytes part]
  (let [part-type (content-part-type part)]
    (cond
      (not (#{"image" "audio"} part-type)) (js/Promise.resolve part)

      (xturn-media/data-url? (:data part)) (js/Promise.resolve part)

      (xturn-media/media-url? (:data part))
      (-> (fetch-media-data-url! runtime config auth-context max-bytes (:data part)
                                 (if (= part-type "audio") "audio/mpeg" "image/png")
                                 part-type)
          (.then (fn [data-url]
                   (-> part
                       (dissoc :url)
                       (assoc :data data-url)))))

      (xturn-media/media-url? (:url part))
      (-> (fetch-media-data-url! runtime config auth-context max-bytes (:url part)
                                 (if (= part-type "audio") "audio/mpeg" "image/png")
                                 part-type)
          (.then (fn [data-url]
                   (-> part
                       (dissoc :url)
                       (assoc :data data-url)))))

      :else (js/Promise.resolve part))))
(defn materialize-content-parts!
  [runtime config model-id auth-context max-bytes parts]
  (let [parts (vec (or parts []))
        should-materialize? (some (fn [part]
                                    (let [part-type (content-part-type part)]
                                      (and (#{"image" "audio"} part-type)
                                           (model-supports-input? config model-id part-type))))
                                  parts)]
    (if-not (and (seq parts) should-materialize?)
      (js/Promise.resolve parts)
      (xpromise/all-vec
       (mapv #(materialize-part! runtime config auth-context max-bytes %) parts)))))
(defn send-agent-turn!
  [runtime config {:keys [conversation-id session-id message content-parts template-context model mode run-id auth-context thinking-level agent-spec]}]
  (js/Promise.
   (fn [resolve reject]
     (try
       (resolve
        (let [conversation-id (or conversation-id (xturn-node/random-uuid!))
              session-id (ensure-session-id session-id)
              auth-context (auth-context-for-agent-turn auth-context agent-spec)
              agent-spec (templates/render-agent-prompts agent-spec auth-context template-context)
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
              run-id (or run-id (xturn-node/random-uuid!))
              started-at (now-iso)
              started-ms (.now js/Date)
              existing-messages (vec (or (:messages (session-store/get-session-sync session-id)) []))
              seeded-messages (transcript/ensure-system-message existing-messages agent-spec)
              auth-extra (auth-snapshot auth-context)]
          (cond
            (and thinking-level-raw (nil? parsed-thinking-level))
            (js/Promise.reject (js/Error. (str "Unsupported thinking level: " thinking-level-raw ". Expected one of off, minimal, low, medium, high, xhigh.")))

            :else
            (-> (policy/enforce-chat-policy! auth-context model-id)
                (.then (fn [_]
                         (maybe-prime-session-title! runtime config conversation-id message)

                         ;; -------------------------------------------------------------------
                         ;; Multimodal correctness: if content parts include remote image/audio URLs,
                         ;; download them and embed as data: URLs so the upstream model receives
                         ;; the actual attachment payload instead of a brittle reference.
                         ;; -------------------------------------------------------------------
                         (let [max-bytes 32000000]
                           (-> (xpromise/all-vec
                                [(passive-hydration! runtime config mode message auth-context)
                                 (passive-memory-hydration! config conversation-id message auth-context agent-spec)
                                 (materialize-content-parts! runtime config model-id auth-context max-bytes content-parts)
                                 (ensure-agent-session! runtime config conversation-id model-id auth-context thinking-level session-id agent-spec)])
                               (.then (fn [[hydration memory-hydration materialized-content-parts session]]
                                        (let [materialized-content-parts (vec (or materialized-content-parts []))
                                              turn-message (content/nonblank message)
                                              user-message (if (seq materialized-content-parts)
                                                             {:role "user" :content turn-message :content-parts materialized-content-parts}
                                                             {:role "user" :content turn-message})
                                              prompt-content-parts (model-ready-content-parts config model-id materialized-content-parts)
                                              request-messages (prune-session-messages agent-spec (conj seeded-messages user-message))]

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
                                          (let [persisted-request-messages (prune-session-messages
                                                                            agent-spec
                                                                            (transcript/transcript-before-prompt session user-message agent-spec))]
                                            (session-store/update-session! (redis/get-client)
                                                                           session-id
                                                                           {:status "running"
                                                                            :has_active_stream false
                                                                            :messages persisted-request-messages
                                                                            :conversation_id conversation-id
                                                                            :run_id run-id})
                                            (prompt-and-await! config session-id run-id conversation-id started-ms model-id mode
                                                               session turn-message prompt-content-parts hydration memory-hydration
                                                               persisted-request-messages agent-spec))))))))))))
        )
       (catch :default e# (reject e#))))))

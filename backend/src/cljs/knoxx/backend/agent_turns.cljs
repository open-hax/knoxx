(ns knoxx.backend.agent-turns
  (:require [clojure.string :as str]
            [knoxx.backend.agent-hydration :refer [settings-state* ensure-settings! passive-hydration! passive-memory-hydration! build-agent-user-message build-agent-multimodal-message hydration-sources]]
            [knoxx.backend.agent-runtime :refer [ensure-agent-session! remove-agent-session!]]
            [knoxx.backend.authz :as authz :refer [auth-snapshot auth-snapshot-has-principal?]]
            [knoxx.backend.core-memory :refer [extract-mentioned-devel-paths extract-mentioned-urls]]
            [knoxx.backend.openplanner-memory :as openplanner-memory]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.realtime :refer [broadcast-ws-session!]]
            [knoxx.backend.run-state :refer [store-run! append-run-event! update-run! update-run-tool-receipt! append-limited latest-assistant-message record-retrieval-sample! tool-event-payload append-run-trace-text! apply-run-tool-trace-event! finalize-run-trace-blocks!]]
            [knoxx.backend.runtime-config :refer [model-supports-reasoning? normalize-thinking-level now-iso]]
            [knoxx.backend.session-store :as session-store]
            [knoxx.backend.session-titles :refer [maybe-prime-session-title!]]
            [knoxx.backend.turn-control :as turn-control]
            [knoxx.backend.text :refer [value->preview-text assistant-message-text assistant-message-reasoning-text]]))

;; Death-spiral guardrails: if the agent repeatedly calls the same tool with the same
;; input signature, abort the turn to prevent infinite loops.
(def ^:private DEATH_SPIRAL_STREAK_LIMIT 6)
(def ^:private DEATH_SPIRAL_TOTAL_LIMIT 12)

(defonce conversation-access* (atom {}))
(defonce lounge-messages* (atom []))

(defn ensure-conversation-access!
  [ctx conversation-id]
  (authz/ensure-conversation-access! conversation-access* ctx conversation-id))

(defn remember-conversation-access!
  [ctx conversation-id]
  (authz/remember-conversation-access! conversation-access* ctx conversation-id))

(defn index-run-memory!
  [config run]
  (openplanner-memory/index-run-memory! config run extract-mentioned-devel-paths extract-mentioned-urls))

(defn- requested-system-prompt
  [agent-spec]
  (some-> (:system-prompt agent-spec) str not-empty))

(defn- ensure-system-message
  [messages agent-spec]
  (let [system-prompt (requested-system-prompt agent-spec)
        has-system? (boolean (some #(= "system" (some-> (:role %) str str/lower-case)) messages))]
    (if (or (str/blank? system-prompt) has-system?)
      (vec messages)
      (into [{:role "system" :content system-prompt}] messages))))

(defn- agent-spec-summary
  [agent-spec]
  (when agent-spec
    (cond-> {}
      (:role agent-spec) (assoc :role (:role agent-spec))
      (:model agent-spec) (assoc :model (:model agent-spec))
      (:thinking-level agent-spec) (assoc :thinkingLevel (:thinking-level agent-spec))
      (:system-prompt agent-spec) (assoc :hasSystemPrompt true)
      (seq (:tool-policies agent-spec)) (assoc :toolPolicies (vec (:tool-policies agent-spec)))
      (:resource-policies agent-spec) (assoc :resourcePolicies (:resource-policies agent-spec)))))

(defn send-agent-turn!
  [runtime config {:keys [conversation-id session-id message content-parts model mode run-id auth-context thinking-level agent-spec]}]
  (let [node-crypto (aget runtime "crypto")
        conversation-id (or conversation-id (.randomUUID node-crypto))
        _ (ensure-conversation-access! auth-context conversation-id)
        _ (remember-conversation-access! auth-context conversation-id)
        mode (or mode "direct")
        requested-model (or model (:model agent-spec))
        model-id (or requested-model (:llmModel (ensure-settings! config)) (:proxx-default-model config))
        thinking-level-raw (or thinking-level (:thinking-level agent-spec))
        parsed-thinking-level (when thinking-level-raw
                                (normalize-thinking-level thinking-level-raw))
        thinking-level (or parsed-thinking-level
                           (:agent-thinking-level config)
                           "off")
        run-id (or run-id (.randomUUID node-crypto))
        started-at (now-iso)
        started-ms (.now js/Date)
        existing-messages (vec (or (:messages (session-store/get-session-sync session-id)) []))
        seeded-messages (ensure-system-message existing-messages agent-spec)
        ;; Build user message - support both text and multimodal content
        user-message (if (seq content-parts)
                       {:role "user" :content message :content-parts content-parts}
                       {:role "user" :content message})
        request-messages (conj seeded-messages user-message)
        _title-prime (maybe-prime-session-title! runtime config conversation-id message)
        auth-extra (auth-snapshot auth-context)
        base-run (merge {:run_id run-id
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
                                      (get agent-spec :resource-policies) (assoc :agentResourcePolicies (get agent-spec :resource-policies))) }
                        auth-extra)
        _ (store-run! run-id base-run)
        _ (session-store/put-session! (redis/get-client)
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
        initial-event (tool-event-payload run-id conversation-id session-id "run_started"
                                          {:status "running"
                                           :mode mode
                                           :model model-id
                                           :thinking_level thinking-level})
        _ (append-run-event! run-id initial-event)
        _ (broadcast-ws-session! session-id "events" initial-event)
        chunks (atom [])
        reasoning-chunks (atom [])]
    (cond
      (and thinking-level-raw (nil? parsed-thinking-level))
      (js/Promise.reject (js/Error. (str "Unsupported thinking level: " thinking-level-raw ". Expected one of off, minimal, low, medium, high, xhigh.")))

      (and (not= thinking-level "off")
           (not (model-supports-reasoning? config model-id)))
      (js/Promise.reject (js/Error. (str "Model " model-id " is not marked reasoning-capable in Knoxx/Proxx configuration. Set thinkingLevel to off or extend KNOXX_REASONING_MODEL_PREFIXES if this model truly supports reasoning.")))

      :else
      (-> (.all js/Promise
              #js [(passive-hydration! runtime config mode message auth-context)
                   (passive-memory-hydration! config conversation-id message auth-context)])
        (.then (fn [results]
                 (let [hydration (aget results 0)
                       memory-hydration (aget results 1)]
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
                   (-> (ensure-agent-session! runtime config conversation-id model-id auth-context thinking-level)
                     (.then (fn [session]
                              (let [ttft-recorded? (atom false)
                                    aborting? (atom false)
                                    abort-reason* (atom nil)
                                    tool-loop* (atom {:last nil :streak 0 :counts {}})
                                    request-abort! (fn [reason]
                                                     (let [reason (str (or reason "aborted"))]
                                                       (if @aborting?
                                                         (js/Promise.resolve nil)
                                                         (do
                                                           (reset! aborting? true)
                                                           (reset! abort-reason* reason)
                                                           ;; Drop streaming flag immediately so the UI can re-enable the composer.
                                                           (session-store/mark-session-streaming! (redis/get-client) session-id false)
                                                           (let [abort-event (tool-event-payload run-id conversation-id session-id "abort_requested"
                                                                                                 {:status "aborting"
                                                                                                  :reason reason})]
                                                             (append-run-event! run-id abort-event)
                                                             (broadcast-ws-session! session-id "events" abort-event))
                                                           (.abort session)))))
                                    _registered (turn-control/register-active-turn!
                                                 conversation-id
                                                 {:run_id run-id
                                                  :session_id session-id
                                                  :started_at started-at
                                                  :abort! request-abort!})
                                    unsubscribe (.subscribe session
                                                          (fn [event]
                                                            (let [event-type (aget event "type")]
                                                              (cond
                                                                (= event-type "message_update")
                                                                (let [assistant-event (aget event "assistantMessageEvent")
                                                                      assistant-event-type (aget assistant-event "type")]
                                                                  (cond
                                                                    (= assistant-event-type "text_delta")
                                                                    (let [delta (or (aget assistant-event "delta") "")]
                                                                      (when-not @ttft-recorded?
                                                                        (reset! ttft-recorded? true)
                                                                        (let [ttft-ms (- (.now js/Date) started-ms)
                                                                              ttft-event (tool-event-payload run-id conversation-id session-id "assistant_first_token"
                                                                                                             {:status "streaming"
                                                                                                              :ttft_ms ttft-ms})]
                                                                          (update-run! run-id #(assoc % :ttft_ms ttft-ms))
                                                                          (append-run-event! run-id ttft-event)
                                                                          (broadcast-ws-session! session-id "events" ttft-event)
                                                                          ;; Mark session as actively streaming in Redis
                                                                          (session-store/mark-session-streaming! (redis/get-client) session-id true)))
                                                                      (swap! chunks conj delta)
                                                                      (when (seq delta)
                                                                        (append-run-trace-text! run-id :agent_message delta (now-iso))
                                                                        (broadcast-ws-session! session-id "tokens"
                                                                                               {:run_id run-id
                                                                                                :conversation_id conversation-id
                                                                                                :session_id session-id
                                                                                                :kind "assistant_message"
                                                                                                :token delta})))

                                                                    (contains? #{"reasoning_delta" "reasoning" "reasoning_content_delta" "thinking_delta" "thinking"} assistant-event-type)
                                                                    (let [delta (or (aget assistant-event "delta")
                                                                                    (aget assistant-event "text")
                                                                                    (aget assistant-event "reasoning")
                                                                                    (aget assistant-event "thinking")
                                                                                    "")]
                                                                      (when (seq delta)
                                                                        (swap! reasoning-chunks conj delta)
                                                                        (append-run-trace-text! run-id :reasoning delta (now-iso))
                                                                        (broadcast-ws-session! session-id "tokens"
                                                                                               {:run_id run-id
                                                                                                :conversation_id conversation-id
                                                                                                :session_id session-id
                                                                                                :kind "reasoning"
                                                                                                :token delta})))

                                                                    :else nil))

                                                                (= event-type "tool_execution_start")
                                                                (let [tool-name (or (aget event "toolName") "tool")
                                                                      tool-call-id (or (aget event "toolCallId") (.randomUUID node-crypto))
                                                                      input-preview (or (value->preview-text (aget event "params") 600)
                                                                                        (value->preview-text (aget event "toolArgs") 600)
                                                                                        (value->preview-text (aget event "args") 600))
                                                                      signature (str tool-name "::" (or input-preview ""))
                                                                      _death-spiral
                                                                      (let [{:keys [last streak counts]} @tool-loop*
                                                                            next-total (inc (get counts signature 0))
                                                                            next-counts (assoc counts signature next-total)
                                                                            next-streak (if (= signature last) (inc streak) 1)]
                                                                        (reset! tool-loop* {:last signature
                                                                                            :streak next-streak
                                                                                            :counts next-counts})
                                                                        (when (and (not @aborting?)
                                                                                   (or (>= next-streak DEATH_SPIRAL_STREAK_LIMIT)
                                                                                       (>= next-total DEATH_SPIRAL_TOTAL_LIMIT)))
                                                                          (let [reason (str "death_spiral_detected: tool '" tool-name "' repeated " next-total "x (streak " next-streak ")")
                                                                                spiral-event (tool-event-payload run-id conversation-id session-id "death_spiral_detected"
                                                                                                                 {:status "failed"
                                                                                                                  :tool_name tool-name
                                                                                                                  :tool_call_id tool-call-id
                                                                                                                  :count next-total
                                                                                                                  :streak next-streak})]
                                                                            (append-run-event! run-id spiral-event)
                                                                            (broadcast-ws-session! session-id "events" spiral-event)
                                                                            (request-abort! reason))))
                                                                      tool-event (tool-event-payload run-id conversation-id session-id "tool_start"
                                                                                                     {:status "running"
                                                                                                      :tool_name tool-name
                                                                                                      :tool_call_id tool-call-id
                                                                                                      :preview input-preview})]
                                                                  (update-run-tool-receipt! run-id tool-call-id {:tool_name tool-name}
                                                                                            (fn [receipt]
                                                                                              (cond-> (merge receipt {:tool_name tool-name
                                                                                                                      :status "running"
                                                                                                                      :started_at (or (:started_at receipt) (now-iso))})
                                                                                                input-preview (assoc :input_preview input-preview))))
                                                                  (apply-run-tool-trace-event! run-id {:type "tool_start"
                                                                                                       :tool_name tool-name
                                                                                                       :tool_call_id tool-call-id
                                                                                                       :preview input-preview
                                                                                                       :at (now-iso)})
                                                                  (append-run-event! run-id tool-event)
                                                                  (broadcast-ws-session! session-id "events" tool-event))

                                                                (= event-type "tool_execution_update")
                                                                (let [tool-name (or (aget event "toolName") "tool")
                                                                      tool-call-id (or (aget event "toolCallId") (str tool-name "-update"))
                                                                      preview (or (value->preview-text (aget event "delta") 400)
                                                                                  (value->preview-text (aget event "update") 400)
                                                                                  (value->preview-text (aget event "message") 400)
                                                                                  (value->preview-text (aget event "statusMessage") 400))]
                                                                  (update-run-tool-receipt! run-id tool-call-id {:tool_name tool-name}
                                                                                            (fn [receipt]
                                                                                              (cond-> (merge receipt {:tool_name tool-name
                                                                                                                      :status "running"})
                                                                                                preview (update :updates #(append-limited % preview 8)))))
                                                                  (apply-run-tool-trace-event! run-id {:type "tool_update"
                                                                                                       :tool_name tool-name
                                                                                                       :tool_call_id tool-call-id
                                                                                                       :preview preview
                                                                                                       :at (now-iso)})
                                                                  (when preview
                                                                    (let [tool-event (tool-event-payload run-id conversation-id session-id "tool_update"
                                                                                                         {:status "running"
                                                                                                          :tool_name tool-name
                                                                                                          :tool_call_id tool-call-id
                                                                                                          :preview preview})]
                                                                      (append-run-event! run-id tool-event)
                                                                      (broadcast-ws-session! session-id "events" tool-event))))

                                                                (= event-type "tool_execution_end")
                                                                (let [tool-name (or (aget event "toolName") "tool")
                                                                      tool-call-id (or (aget event "toolCallId") (.randomUUID node-crypto))
                                                                      is-error (boolean (aget event "isError"))
                                                                      result-preview (or (value->preview-text (aget event "result") 800)
                                                                                         (value->preview-text (aget event "toolResult") 800)
                                                                                         (value->preview-text (aget event "output") 800))
                                                                      tool-event (tool-event-payload run-id conversation-id session-id "tool_end"
                                                                                                     {:status (if is-error "failed" "completed")
                                                                                                      :tool_name tool-name
                                                                                                      :tool_call_id tool-call-id
                                                                                                      :is_error is-error
                                                                                                      :preview result-preview})]
                                                                  (update-run-tool-receipt! run-id tool-call-id {:tool_name tool-name}
                                                                                            (fn [receipt]
                                                                                              (cond-> (merge receipt {:tool_name tool-name
                                                                                                                      :status (if is-error "failed" "completed")
                                                                                                                      :ended_at (now-iso)
                                                                                                                      :is_error is-error})
                                                                                                result-preview (assoc :result_preview result-preview))))
                                                                  (apply-run-tool-trace-event! run-id {:type "tool_end"
                                                                                                       :tool_name tool-name
                                                                                                       :tool_call_id tool-call-id
                                                                                                       :preview result-preview
                                                                                                       :is_error is-error
                                                                                                       :at (now-iso)})
                                                                  (append-run-event! run-id tool-event)
                                                                  (broadcast-ws-session! session-id "events" tool-event))

                                                                (= event-type "turn_end")
                                                                (let [tool-results (or (aget event "toolResults") #js [])
                                                                      turn-event (tool-event-payload run-id conversation-id session-id "turn_end"
                                                                                                     {:status "completed"
                                                                                                      :tool_result_count (or (.-length tool-results) 0)})]
                                                                  (append-run-event! run-id turn-event)
                                                                  (broadcast-ws-session! session-id "events" turn-event))

                                                                (= event-type "agent_end")
                                                                (broadcast-ws-session! session-id "events"
                                                                                       (tool-event-payload run-id conversation-id session-id "agent_end"
                                                                                                           {:status "completed"}))))))]
                                ;; Use multimodal message builder if content-parts are present
                                (let [prompt-args (if (seq content-parts)
                                                    (build-agent-multimodal-message message content-parts hydration memory-hydration)
                                                    (build-agent-user-message message hydration memory-hydration))
                                      prompt-promise (.prompt session prompt-args)]
                                  (.catch
                                   (.then prompt-promise
                                          (fn []
                                            (unsubscribe)
                                            (turn-control/unregister-active-turn! conversation-id run-id)
                                            (let [assistant-message (latest-assistant-message session)
                                                  answer (let [chunked (apply str @chunks)]
                                                           (if (str/blank? chunked)
                                                             (assistant-message-text assistant-message)
                                                             chunked))
                                                  usage (or (aget assistant-message "usage") #js {})
                                                  reasoning-text (let [streamed (apply str @reasoning-chunks)
                                                                       final-reasoning (assistant-message-reasoning-text assistant-message)]
                                                                   (cond
                                                                     (and (str/blank? streamed) (not (str/blank? final-reasoning))) final-reasoning
                                                                     (and (not (str/blank? final-reasoning)) (> (count final-reasoning) (count streamed))) final-reasoning
                                                                     :else streamed))
                                                  elapsed (- (.now js/Date) started-ms)
                                                  output-tokens (or (aget usage "output") 0)
                                                  tokens-per-second (if (and (pos? output-tokens) (pos? elapsed))
                                                                      (* 1000 (/ output-tokens elapsed))
                                                                      nil)
                                                  sources (hydration-sources hydration)
                                                  message-parts (cond-> []
                                                               (not (str/blank? reasoning-text))
                                                               (conj {:role "thinking"
                                                                      :content reasoning-text
                                                                      :reasoningType "reasoning_summary"})
                                                               (not (str/blank? answer))
                                                               (conj {:role "assistant"
                                                                      :content answer}))
                                                  response {:answer answer
                                                            :run_id run-id
                                                            :runId run-id
                                                            :conversation_id conversation-id
                                                            :conversationId conversation-id
                                                            :session_id session-id
                                                            :model model-id
                                                            :sources sources
                                                            :message_parts message-parts
                                                            :compare nil}
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
                                                                                                        memory-hydration (assoc :memoryHydration (select-keys memory-hydration [:query :mode :hits :elapsedMs :conversationId])))]
                                                                                   (-> run
                                                                                       (assoc :updated_at (now-iso)
                                                                                              :status "completed"
                                                                                              :total_time_ms elapsed
                                                                                              :input_tokens (or (aget usage "input") 0)
                                                                                              :output_tokens output-tokens
                                                                                              :tokens_per_s tokens-per-second
                                                                                              :answer answer
                                                                                              :reasoning reasoning-text
                                                                                              :sources sources)
                                                                                       (update :resources merge resource-patch)))))
                                                    _ (when completed-run
                                                        (index-run-memory! config completed-run))]
                                                (append-run-event! run-id completed-event)
                                                (broadcast-ws-session! session-id "events" completed-event)
                                                ;; Mark session as completed in Redis
                                                (session-store/complete-session! (redis/get-client)
                                                                                  session-id
                                                                                  conversation-id
                                                                                  {:status "completed"
                                                                                   :answer answer
                                                                                   :messages (conj request-messages {:role "assistant" :content answer})})
                                                ;; Remove from in-memory cache to prevent stale isStreaming
                                                (remove-agent-session! conversation-id)
                                                response)))
                                   (fn [err]
                                     (unsubscribe)
                                     (turn-control/unregister-active-turn! conversation-id run-id)
                                     (let [err-text (or @abort-reason* (str err))
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
                                                                                    :reasoning (apply str @reasoning-chunks)
                                                                                    :error err-text)
                                                                             (update :resources merge resource-patch)))))
                                             _ (when failed-run
                                                 (index-run-memory! config failed-run))]
                                         (append-run-event! run-id error-event)
                                         (broadcast-ws-session! session-id "events" error-event)
                                         ;; Mark session as failed in Redis
                                         (session-store/complete-session! (redis/get-client)
                                                                           session-id
                                                                           conversation-id
                                                                           {:status "failed"
                                                                            :error err-text
                                                                            :messages request-messages})
                                         ;; Remove from in-memory cache to prevent stale isStreaming
                                         (remove-agent-session! conversation-id))
                                     (throw err)))))))))))))))))

(defn recovered-auth-context
  [session]
  {:orgId (:org_id session)
   :orgSlug (:org_slug session)
   :userId (:user_id session)
   :userEmail (:user_email session)
   :membershipId (:membership_id session)
   :roleSlugs (vec (or (:role_slugs session) []))
   :permissions (vec (or (:permissions session) []))
   :toolPolicies (vec (or (:tool_policies session) []))
   :membershipToolPolicies (vec (or (:membership_tool_policies session) []))
   :isSystemAdmin (boolean (:is_system_admin session))})

(defn recovered-agent-spec
  [session]
  (:agent_spec session))

(defn restore-recovered-conversation-access!
  [session]
  (let [conversation-id (str (or (:conversation_id session) ""))
        snapshot (select-keys session [:org_id
                                       :org_slug
                                       :user_id
                                       :user_email
                                       :membership_id
                                       :role_slugs
                                       :permissions
                                       :tool_policies
                                       :membership_tool_policies
                                       :is_system_admin])]
    (when (and (not (str/blank? conversation-id))
               (auth-snapshot-has-principal? snapshot))
      (swap! conversation-access* assoc conversation-id snapshot))))

(defn last-session-user-message
  [session]
  (some (fn [message]
          (let [role (some-> (:role message) str str/lower-case)
                content (some-> (:content message) str)]
            (when (and (= role "user")
                       (not (str/blank? content)))
              content)))
        (reverse (vec (or (:messages session) [])))))

(defn resume-recovered-session!
  [runtime config session]
  (let [conversation-id (str (or (:conversation_id session) ""))
        session-id (str (or (:session_id session) ""))
        run-id (or (:run_id session) nil)
        model-id (or (:model session) nil)
        mode (or (:mode session) "direct")
        thinking-level (or (:thinking_level session)
                           (:agent-thinking-level config)
                           "off")
        auth-context (recovered-auth-context session)
        agent-spec (recovered-agent-spec session)
        message (last-session-user-message session)]
    (restore-recovered-conversation-access! session)
    (cond
      (or (str/blank? conversation-id)
          (str/blank? session-id))
      (js/Promise.resolve {:session_id session-id
                           :conversation_id conversation-id
                           :resumed false
                           :reason "missing session or conversation id"})

      (str/blank? message)
      (-> (ensure-agent-session! runtime config conversation-id model-id auth-context thinking-level)
          (.then (fn [_]
                   (-> (session-store/update-session! (redis/get-client) session-id
                                                     {:status "waiting_input"
                                                      :has_active_stream false
                                                      :recovered_at (now-iso)})
                       (.then (fn [_]
                                {:session_id session-id
                                 :conversation_id conversation-id
                                 :resumed false
                                 :reason "no pending user message to resume"}))))))

      :else
      (-> (session-store/update-session! (redis/get-client) session-id
                                         {:status "running"
                                          :has_active_stream false
                                          :recovered_at (now-iso)})
          (.then (fn [_]
                   (send-agent-turn! runtime config {:conversation-id conversation-id
                                                     :session-id session-id
                                                     :run-id run-id
                                                     :message message
                                                     :model model-id
                                                     :mode mode
                                                     :thinking-level thinking-level
                                                     :auth-context auth-context
                                                     :agent-spec agent-spec})))
          (.then (fn [_]
                   {:session_id session-id
                    :conversation_id conversation-id
                    :resumed true}))
          (.catch (fn [err]
                    (js/console.error "[knoxx] failed to resume recovered session"
                                      #js {:sessionId session-id
                                           :conversationId conversation-id
                                           :error (str err)})
                    (-> (session-store/complete-session! (redis/get-client)
                                                         session-id
                                                         conversation-id
                                                         {:status "failed"
                                                          :error (str "Session recovery failed: " err)
                                                          :messages (:messages session)})
                        (.then (fn [_]
                                 {:session_id session-id
                                  :conversation_id conversation-id
                                  :resumed false
                                  :error (str err)})))))))))

(defn recover-active-agent-sessions!
  [runtime config redis-client]
  (-> (session-store/recover-sessions! redis-client)
      (.then (fn [sessions]
               (let [items (vec sessions)]
                 (if (seq items)
                   (-> (.all js/Promise (clj->js (mapv #(resume-recovered-session! runtime config %) items)))
                       (.then (fn [results]
                                (vec (js->clj results :keywordize-keys true)))))
                   (js/Promise.resolve [])))))))

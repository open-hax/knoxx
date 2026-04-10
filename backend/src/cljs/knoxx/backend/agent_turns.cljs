(ns knoxx.backend.agent-turns
  (:require [clojure.string :as str]
            [knoxx.backend.agent-events :as agent-events]
            [knoxx.backend.agent-hydration :refer [settings-state* ensure-settings! passive-hydration! passive-memory-hydration! build-agent-user-message hydration-sources]]
            [knoxx.backend.agent-runtime :refer [ensure-agent-session! remove-agent-session!]]
            [knoxx.backend.authz :as authz :refer [auth-snapshot auth-snapshot-has-principal?]]
            [knoxx.backend.core-memory :refer [extract-mentioned-devel-paths extract-mentioned-urls]]
            [knoxx.backend.loop-detection :as loop-detection]
            [knoxx.backend.openplanner-memory :as openplanner-memory]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.realtime :refer [broadcast-ws-session!]]
            [knoxx.backend.run-state :refer [store-run! append-run-event! update-run! latest-assistant-message record-retrieval-sample! tool-event-payload]]
            [knoxx.backend.runtime-config :refer [model-supports-reasoning? normalize-thinking-level now-iso]]
            [knoxx.backend.session-store :as session-store]
            [knoxx.backend.session-titles :refer [maybe-prime-session-title!]]
            [knoxx.backend.text :refer [assistant-message-text assistant-message-reasoning-text]]))

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


(defn send-agent-turn!
  [runtime config {:keys [conversation-id session-id message model mode run-id auth-context thinking-level]}]
  (let [node-crypto (aget runtime "crypto")
        conversation-id (or conversation-id (.randomUUID node-crypto))
        _ (ensure-conversation-access! auth-context conversation-id)
        _ (remember-conversation-access! auth-context conversation-id)
        mode (or mode "direct")
        model-id (or model (:llmModel (ensure-settings! config)) (:proxx-default-model config))
        thinking-level-raw thinking-level
        parsed-thinking-level (when thinking-level-raw
                                (normalize-thinking-level thinking-level-raw))
        thinking-level (or parsed-thinking-level
                           (:agent-thinking-level config)
                           "off")
        run-id (or run-id (.randomUUID node-crypto))
        started-at (now-iso)
        started-ms (.now js/Date)
        existing-messages (vec (or (:messages (session-store/get-session-sync session-id)) []))
        request-messages (conj existing-messages {:role "user" :content message})
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
                         :tool_receipts []
                         :request_messages request-messages
                         :settings {:sessionId session-id
                                    :conversationId conversation-id
                                    :mode mode
                                    :thinkingLevel thinking-level
                                    :workspaceRoot (:workspace-root config)}
                         :resources {:provider "proxx"
                                     :collection (:collection-name config)}}
                        auth-extra)
        _ (store-run! run-id base-run)
        _ (session-store/put-session! (redis/get-client)
                                      (merge {:session_id session-id
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
                                             auth-extra))
        initial-event (tool-event-payload run-id conversation-id session-id "run_started"
                                          {:status "running"
                                           :mode mode
                                           :model model-id
                                           :thinking_level thinking-level})
        _ (append-run-event! run-id initial-event)
        _ (loop-detection/start-turn! session-id)
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
                                    ctx (agent-events/make-context run-id session-id conversation-id started-ms node-crypto chunks reasoning-chunks ttft-recorded?)
                                    unsubscribe (.subscribe session (agent-events/make-subscription-callback ctx))]
                                (let [prompt-promise (.prompt session (build-agent-user-message message hydration memory-hydration))]
                                  (.catch
                                   (.then prompt-promise
                                          (fn []
                                            (unsubscribe)
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
                                                (loop-detection/end-turn! session-id)
                                                (remove-agent-session! conversation-id)
                                                response)))
                                   (fn [err]
                                     (unsubscribe)
                                     (let [error-event (tool-event-payload run-id conversation-id session-id "run_failed"
                                                                           {:status "failed"
                                                                            :error (str err)})]
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
                                                                                    :error (str err))
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
                                                                            :error (str err)
                                                                            :messages request-messages})
                                         ;; Remove from in-memory cache to prevent stale isStreaming
                                         (loop-detection/end-turn! session-id)
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
                                                     :auth-context auth-context})))
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

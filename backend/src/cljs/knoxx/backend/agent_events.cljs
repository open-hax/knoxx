(ns knoxx.backend.agent-events
  "Agent event subscription handlers.
   
   Extracted from agent_turns.cljs to reduce nesting depth and improve
   maintainability. Each event type has its own handler function.
   
   The main entry point is `make-subscription-callback` which dispatches
   to the appropriate handler based on event type."
  (:require [knoxx.backend.loop-detection :as loop-detection]
            [knoxx.backend.realtime :refer [broadcast-ws-session!]]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.run-state :refer [update-run! update-run-tool-receipt! append-limited tool-event-payload]]
            [knoxx.backend.runtime-config :refer [now-iso]]
            [knoxx.backend.session-store :as session-store]
            [knoxx.backend.text :refer [value->preview-text]]))

;; ━━━ Handler Context ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

(defn make-context
  "Create a context map passed to every event handler."
  [run-id session-id conversation-id started-ms node-crypto chunks reasoning-chunks ttft-recorded?]
  {:run-id run-id
   :session-id session-id
   :conversation-id conversation-id
   :started-ms started-ms
   :node-crypto node-crypto
   :chunks chunks
   :reasoning-chunks reasoning-chunks
   :ttft-recorded? ttft-recorded?})

;; ━━━ Broadcasting Helpers ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

(defn broadcast-tokens!
  [{:keys [run-id session-id conversation-id]} kind token]
  (when (seq token)
    (broadcast-ws-session! session-id "tokens"
                           {:run_id run-id
                            :conversation_id conversation-id
                            :session_id session-id
                            :kind kind
                            :token token})))

(defn broadcast-event!
  [{:keys [session-id]} event]
  (broadcast-ws-session! session-id "events" event))

;; ━━━ Text Delta Handler ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

(defn handle-text-delta!
  "Handle a text_delta event: record TTFT, accumulate chunks, broadcast tokens."
  [ctx assistant-event]
  (let [delta (or (aget assistant-event "delta") "")]
    ;; Record TTFT on first token
    (when-not @(:ttft-recorded? ctx)
      (reset! (:ttft-recorded? ctx) true)
      (let [ttft-ms (- (.now js/Date) (:started-ms ctx))
            ttft-event (tool-event-payload (:run-id ctx) (:conversation-id ctx) (:session-id ctx)
                                           "assistant_first_token"
                                           {:status "streaming" :ttft_ms ttft-ms})]
        (update-run! (:run-id ctx) #(assoc % :ttft_ms ttft-ms))
        (broadcast-event! ctx ttft-event)
        (session-store/mark-session-streaming! (redis/get-client) (:session-id ctx) true)))
    ;; Accumulate chunks
    (swap! (:chunks ctx) conj delta)
    ;; Record progress for loop detection
    (loop-detection/record-progress! (:session-id ctx))
    ;; Check for message loop
    (when (seq delta)
      (let [loop-result (loop-detection/check-and-handle-loop
                         (:session-id ctx) (:conversation-id ctx)
                         {:message-chunk delta})]
        (when (= (:status loop-result) :abort)
          (js/console.warn "[loop] Aborting due to message loop:"
                           (pr-str (:reason loop-result))))))
    ;; Broadcast tokens
    (broadcast-tokens! ctx "assistant_message" delta)))

;; ━━━ Reasoning Delta Handler ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

(defn handle-reasoning-delta!
  "Handle a reasoning/thinking delta event: accumulate reasoning chunks, broadcast tokens."
  [ctx assistant-event]
  (let [delta (or (aget assistant-event "delta")
                  (aget assistant-event "text")
                  (aget assistant-event "reasoning")
                  (aget assistant-event "thinking")
                  "")]
    (when (seq delta)
      (swap! (:reasoning-chunks ctx) conj delta)
      (broadcast-tokens! ctx "reasoning" delta))))

;; ━━━ Tool Event Handlers ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

(defn handle-tool-start!
  "Handle tool_execution_start event."
  [ctx event]
  (let [tool-name (or (aget event "toolName") "tool")
        tool-call-id (or (aget event "toolCallId") (.randomUUID (:node-crypto ctx)))
        input-preview (or (value->preview-text (aget event "params") 600)
                          (value->preview-text (aget event "toolArgs") 600)
                          (value->preview-text (aget event "args") 600))
        tool-event (tool-event-payload (:run-id ctx) (:conversation-id ctx) (:session-id ctx)
                                       "tool_start"
                                       {:status "running"
                                        :tool_name tool-name
                                        :tool_call_id tool-call-id
                                        :preview input-preview})]
    (update-run-tool-receipt! (:run-id ctx) tool-call-id {:tool_name tool-name}
                              (fn [receipt]
                                (cond-> (merge receipt {:tool_name tool-name
                                                        :status "running"
                                                        :started_at (or (:started_at receipt) (now-iso))})
                                  input-preview (assoc :input_preview input-preview))))
    (broadcast-event! ctx tool-event)))

(defn handle-tool-update!
  "Handle tool_execution_update event."
  [ctx event]
  (let [tool-name (or (aget event "toolName") "tool")
        tool-call-id (or (aget event "toolCallId") (str tool-name "-update"))
        preview (or (value->preview-text (aget event "delta") 400)
                    (value->preview-text (aget event "update") 400)
                    (value->preview-text (aget event "message") 400)
                    (value->preview-text (aget event "statusMessage") 400))]
    (update-run-tool-receipt! (:run-id ctx) tool-call-id {:tool_name tool-name}
                              (fn [receipt]
                                (cond-> (merge receipt {:tool_name tool-name :status "running"})
                                  preview (update :updates #(append-limited % preview 8)))))
    (when preview
      (let [tool-event (tool-event-payload (:run-id ctx) (:conversation-id ctx) (:session-id ctx)
                                           "tool_update"
                                           {:status "running"
                                            :tool_name tool-name
                                            :tool_call_id tool-call-id
                                            :preview preview})]
        (broadcast-event! ctx tool-event)))))

(defn handle-tool-end!
  "Handle tool_execution_end event."
  [ctx event]
  (let [tool-name (or (aget event "toolName") "tool")
        tool-call-id (or (aget event "toolCallId") (.randomUUID (:node-crypto ctx)))
        is-error (boolean (aget event "isError"))
        result-preview (or (value->preview-text (aget event "result") 800)
                           (value->preview-text (aget event "toolResult") 800)
                           (value->preview-text (aget event "output") 800))
        tool-event (tool-event-payload (:run-id ctx) (:conversation-id ctx) (:session-id ctx)
                                       "tool_end"
                                       {:status (if is-error "failed" "completed")
                                        :tool_name tool-name
                                        :tool_call_id tool-call-id
                                        :is_error is-error
                                        :preview result-preview})]
    (update-run-tool-receipt! (:run-id ctx) tool-call-id {:tool_name tool-name}
                              (fn [receipt]
                                (cond-> (merge receipt {:tool_name tool-name
                                                        :status (if is-error "failed" "completed")
                                                        :ended_at (now-iso)
                                                        :is_error is-error})
                                  result-preview (assoc :result_preview result-preview))))
    (broadcast-event! ctx tool-event)))

;; ━━━ Turn/Agent End Handlers ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

(defn handle-turn-end!
  "Handle turn_end event."
  [ctx event]
  (let [tool-results (or (aget event "toolResults") #js [])
        turn-event (tool-event-payload (:run-id ctx) (:conversation-id ctx) (:session-id ctx)
                                       "turn_end"
                                       {:status "completed"
                                        :tool_result_count (or (.-length tool-results) 0)})]
    (broadcast-event! ctx turn-event)))

(defn handle-agent-end!
  "Handle agent_end event."
  [ctx]
  (broadcast-event! ctx
                    (tool-event-payload (:run-id ctx) (:conversation-id ctx) (:session-id ctx)
                                       "agent_end" {:status "completed"})))

;; ━━━ Message Update Dispatcher ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

(defn handle-message-update!
  "Dispatch message_update sub-events to text_delta or reasoning handlers."
  [ctx event]
  (let [assistant-event (aget event "assistantMessageEvent")
        assistant-event-type (aget assistant-event "type")]
    (cond
      (= assistant-event-type "text_delta")
      (handle-text-delta! ctx assistant-event)

      (contains? #{"reasoning_delta" "reasoning" "reasoning_content_delta"
                   "thinking_delta" "thinking"}
                 assistant-event-type)
      (handle-reasoning-delta! ctx assistant-event)

      :else nil)))

;; ━━━ Main Subscription Callback ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

(defn make-subscription-callback
  "Create the event callback for session.subscribe.
   Replaces the massive inline cond in agent_turns.cljs.
   
   Parameters:
   - ctx: context map from make-context
   - run-id, session-id, conversation-id: identifiers
   - node-crypto: runtime crypto for UUID generation
   
   Returns a function suitable for .subscribe."
  [ctx]
  (fn [event]
    (let [event-type (aget event "type")]
      (cond
        (= event-type "message_update")
        (handle-message-update! ctx event)

        (= event-type "tool_execution_start")
        (handle-tool-start! ctx event)

        (= event-type "tool_execution_update")
        (handle-tool-update! ctx event)

        (= event-type "tool_execution_end")
        (handle-tool-end! ctx event)

        (= event-type "turn_end")
        (handle-turn-end! ctx event)

        (= event-type "agent_end")
        (handle-agent-end! ctx)

        :else nil))))

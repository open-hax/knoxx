(ns knoxx.backend.infra.agent.stream.reducer
  "Pure stream reducer used to test stream semantics independently from Redis,
   run-state, WebSocket, and provider subscription side effects."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.agent.reasoning :as reasoning]
            [knoxx.backend.domain.agent.text-delta :as text-delta]
            [knoxx.backend.domain.agent.tool-lifecycle :as tool-lifecycle]
            [knoxx.backend.domain.agent.turn-guards :as turn-guards]))

(defn initial-state
  []
  {:assistant-text ""
   :reasoning-text ""
   :replay-offsets {}
   :think-tag-mode :off
   :tool-loop turn-guards/empty-tool-loop-state
   :aborting? false
   :seen-lifecycle-events #{}})

(defn- suppress-replay
  [state kind delta]
  (let [previous (if (= kind :agent_message)
                   (:assistant-text state)
                   (:reasoning-text state))
        offset (get-in state [:replay-offsets kind])
        {:keys [delta replay-offset]} (text-delta/suppress-replayed-prefix-delta previous offset delta)]
    {:delta delta
     :state (if replay-offset
              (assoc-in state [:replay-offsets kind] replay-offset)
              (update state :replay-offsets dissoc kind))}))

(defn- append-text
  [state kind delta]
  (let [{state :state delta :delta} (suppress-replay state kind delta)
        key (if (= kind :agent_message) :assistant-text :reasoning-text)
        safe-delta (text-delta/diff-appended-text (get state key "") delta)]
    (if (str/blank? safe-delta)
      {:state state :effects []}
      {:state (update state key str safe-delta)
       :effects [{:effect :token
                  :kind kind
                  :delta safe-delta}]})))

(defn- append-routed-text
  [state delta]
  (let [{:keys [mode emissions]} (reasoning/route-think-delta {:mode (:think-tag-mode state)
                                                               :last-assistant-text (:assistant-text state)
                                                               :delta delta})]
    (reduce (fn [{:keys [state effects]} emission]
              (let [result (append-text state (:kind emission) (:delta emission))]
                {:state (:state result)
                 :effects (into effects (:effects result))}))
            {:state (assoc state :think-tag-mode mode)
             :effects []}
            emissions)))

(defn- handle-message-update
  [state event]
  (let [assistant-event-type (:assistant-event-type event)]
    (cond
      (= assistant-event-type "text_delta")
      (let [delta (str (or (:delta event) ""))]
        (if (and (not (str/blank? (:assistant-text state)))
                 (str/starts-with? delta (:assistant-text state)))
          (append-text state :agent_message delta)
          (append-routed-text state delta)))

      (contains? #{"reasoning_delta" "reasoning" "reasoning_content_delta"
                   "thinking_delta" "thinking"}
                 assistant-event-type)
      (let [delta (str (or (:delta event) ""))]
        (if (and (not (str/blank? (:reasoning-text state)))
                 (str/starts-with? delta (:reasoning-text state)))
          (append-text state :reasoning delta)
          (append-text state :reasoning delta)))

      (contains? #{"toolcall_delta" "tool_call_delta" "toolcall_end" "tool_call_end"}
                 assistant-event-type)
      {:state state :effects [{:effect :sync-message
                               :message (:partial-message event)}]}

      :else
      {:state state :effects [{:effect :sync-message
                               :message (:message event)}]})))

(defn- handle-tool-start
  [state event]
  (let [tool-call-id (:tool-call-id event)
        guard (turn-guards/observe-tool-call (:tool-loop state)
                                             {:tool-name (:tool-name event)
                                              :tool-call-id tool-call-id
                                              :input-preview (:input-preview event)
                                              :aborting? (:aborting? state)})
        base-effect {:effect :tool-start
                     :tool-name (:tool-name event)
                     :tool-call-id tool-call-id
                     :receipt (tool-lifecycle/start-receipt {} event)
                     :run-event (tool-lifecycle/run-event-extra :start event)}
        abort-effect (when (:abort? guard)
                       {:effect :abort
                        :reason (:reason guard)
                        :run-event (tool-lifecycle/run-event-extra
                                    :death-spiral
                                    (merge event
                                           {:count (:count guard)
                                            :streak (:streak guard)}))})]
    {:state (assoc state :tool-loop (:state guard))
     :effects (cond-> [base-effect]
                abort-effect (conj abort-effect))}))

(defn- handle-tool-update
  [state event]
  {:state state
   :effects [{:effect :tool-update
              :tool-name (:tool-name event)
              :tool-call-id (:tool-call-id event)
              :receipt (tool-lifecycle/update-receipt {} event)
              :run-event (tool-lifecycle/run-event-extra :update event)}]})

(defn- handle-tool-end
  [state event]
  {:state state
   :effects [{:effect :tool-end
              :tool-name (:tool-name event)
              :tool-call-id (:tool-call-id event)
              :receipt (tool-lifecycle/end-receipt {} event)
              :run-event (tool-lifecycle/run-event-extra :end event)}]})

(defn reduce-event
  [state event]
  (let [state (merge (initial-state) state)]
    (case (:type event)
      "message_update" (handle-message-update state event)
      "message_end" {:state state :effects [{:effect :sync-message :message (:message event)}]}
      "tool_execution_start" (handle-tool-start state event)
      "tool_execution_update" (handle-tool-update state event)
      "tool_execution_end" (handle-tool-end state event)
      "turn_end" {:state state
                  :effects [{:effect :turn-end
                             :status "completed"
                             :tool-result-count (:tool-result-count event)}]}
      "agent_end" {:state state
                   :effects [{:effect :agent-end
                              :status "completed"}]}
      {:state state :effects []})))

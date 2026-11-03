(ns knoxx.backend.infra.agent.stream.reducer
  "Pure stream reducer used to test stream semantics independently from the session store,
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
   :tool-call-ids tool-lifecycle/empty-tool-call-id-state
   :aborting? false
   :seen-lifecycle-events #{}})

(defn reconcile-text
  "Reconcile an explicit cumulative snapshot without guessing overlap in tokens.
   A changed prefix is a correction, not an incremental append."
  [previous snapshot]
  (let [previous (str (or previous ""))
        snapshot (str (or snapshot ""))
        appended? (str/starts-with? snapshot previous)]
    {:text snapshot
     :delta (if appended? (subs snapshot (count previous)) "")
     :corrected? (not appended?)}))

(defn route-incremental-text
  "Route literal token bytes, including whitespace-only reasoning/answer deltas.
   The pinned compatibility router otherwise discards blank fragments."
  [{:keys [mode delta] :as input}]
  (if (and (seq delta) (str/blank? delta))
    {:mode (or mode :off)
     :emissions [{:kind (if (= mode :thinking) :reasoning :agent_message)
                  :delta delta}]}
    (reasoning/route-think-delta input)))

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
  (let [key (if (= kind :agent_message) :assistant-text :reasoning-text)]
    (if (seq delta)
      {:state (update state key str delta)
       :effects [{:effect :token :kind kind :delta delta}]}
      {:state state :effects []})))

(defn- append-replayed-text
  [state kind delta]
  (let [{state :state delta :delta} (suppress-replay state kind delta)
        key (if (= kind :agent_message) :assistant-text :reasoning-text)
        safe-delta (text-delta/diff-appended-text (get state key "") delta)]
    (if-not (seq safe-delta)
      {:state state :effects []}
      {:state (update state key str safe-delta)
       :effects [{:effect :token
                  :kind kind
                  :delta safe-delta}]})))

(defn- append-snapshot
  [state kind snapshot]
  (let [key (if (= kind :agent_message) :assistant-text :reasoning-text)
        {:keys [text delta corrected?]} (reconcile-text (get state key) snapshot)]
    {:state (assoc state key text)
     :effects (cond
                corrected? [{:effect :replace-text :kind kind :text text}]
                (seq delta) [{:effect :token :kind kind :delta delta}]
                :else [])}))

(defn- append-routed-text
  [state delta]
  (let [{:keys [mode emissions]} (route-incremental-text {:mode (:think-tag-mode state)
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
        (cond
          (some? (:text-snapshot event)) (append-snapshot state :agent_message (:text-snapshot event))
          (= :replay (:text-semantics event)) (append-replayed-text state :agent_message delta)
          :else (append-routed-text state delta)))

      (contains? #{"reasoning_delta" "reasoning" "reasoning_content_delta"
                   "thinking_delta" "thinking"}
                 assistant-event-type)
      (let [delta (str (or (:delta event) ""))]
        (cond
          (some? (:reasoning-snapshot event)) (append-snapshot state :reasoning (:reasoning-snapshot event))
          (= :replay (:text-semantics event)) (append-replayed-text state :reasoning delta)
          :else (append-text state :reasoning delta)))

      (contains? #{"toolcall_delta" "tool_call_delta" "toolcall_end" "tool_call_end"}
                 assistant-event-type)
      {:state state :effects [{:effect :sync-message
                               :message (:partial-message event)}]}

      :else
      {:state state :effects [{:effect :sync-message
                               :message (:message event)}]})))

(defn- handle-tool-start
  [state event]
  (let [resolved (tool-lifecycle/resolve-tool-call-start-id (:tool-call-ids state)
                                                            {:tool-name (:tool-name event)
                                                             :tool-call-id (:tool-call-id event)})
        tool-call-id (:tool-call-id resolved)
        event (assoc event :tool-call-id tool-call-id)
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
    {:state (assoc state
                   :tool-loop (:state guard)
                   :tool-call-ids (:state resolved))
     :effects (cond-> [base-effect]
                abort-effect (conj abort-effect))}))

(defn- resolved-tool-call-id
  [state event]
  (or (tool-lifecycle/active-tool-call-id (:tool-call-ids state)
                                          {:tool-name (:tool-name event)
                                           :tool-call-id (:tool-call-id event)})
      (:tool-call-id event)))

(defn- handle-tool-update
  [state event]
  (let [event (assoc event :tool-call-id (resolved-tool-call-id state event))]
    {:state state
     :effects [{:effect :tool-update
                :tool-name (:tool-name event)
                :tool-call-id (:tool-call-id event)
                :receipt (tool-lifecycle/update-receipt {} event)
                :run-event (tool-lifecycle/run-event-extra :update event)}]}))

(defn- handle-tool-end
  [state event]
  (let [event (assoc event :tool-call-id (resolved-tool-call-id state event))]
    {:state state
     :effects [{:effect :tool-end
                :tool-name (:tool-name event)
                :tool-call-id (:tool-call-id event)
                :receipt (tool-lifecycle/end-receipt {} event)
                :run-event (tool-lifecycle/run-event-extra :end event)}]}))

(defn reduce-event
  [state event]
  (let [state (merge (initial-state) state)]
    (case (:type event)
      "message_start" {:state (if (= "assistant" (:message-role event))
                                 (assoc state :assistant-text "" :reasoning-text ""
                                              :replay-offsets {} :think-tag-mode :off)
                                 state)
                       :effects []}
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

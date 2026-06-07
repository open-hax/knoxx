(ns knoxx.backend.agents.stream-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.agent.stream :as stream]
            [knoxx.backend.infra.agent.stream.sinks :as sinks]
            [knoxx.backend.domain.action.run-state :as run-state]
            [knoxx.backend.domain.realtime :as realtime]
            [knoxx.backend.infra.stores.mongo-session-store :as session-store]
            [knoxx.backend.shape.agent :as agent-shape]))

(defn- assistant-message
  [{:keys [content reasoning tool-previews]}]
  (let [m #js {:role "assistant"
               :content (or content "")}]
    (when (some? reasoning)
      (aset m "reasoning" reasoning))
    (when (some? tool-previews)
      (aset m "tool_calls" tool-previews))
    m))

(deftype RecordingSink [events*]
  sinks/IRunEventSink
  (emit-run-event! [_ run-event]
    (swap! events* conj {:op :run-event :event run-event}))
  (emit-token-event! [_ token-event]
    (swap! events* conj {:op :token-event :event token-event}))
  (update-run-state! [_ run-id update-fn]
    (swap! events* conj {:op :update-run :run-id run-id :result (update-fn {})}))
  (update-session-record! [_ session-id update]
    (swap! events* conj {:op :update-session :session-id session-id :update update}))
  (finalize-run! [_ result]
    (swap! events* conj {:op :finalize-run :result result}))
  (record-run-error! [_ error-event]
    (swap! events* conj {:op :run-error :event error-event}))
  (append-trace-text! [_ run-id kind delta at]
    (swap! events* conj {:op :trace-text :run-id run-id :kind kind :delta delta :at at}))
  (update-tool-receipt! [_ run-id receipt-id default-receipt update-fn]
    (swap! events* conj {:op :tool-receipt
                         :run-id run-id
                         :receipt-id receipt-id
                         :receipt (update-fn default-receipt)}))
  (apply-tool-trace-event! [_ run-id trace-event]
    (swap! events* conj {:op :tool-trace :run-id run-id :event trace-event}))
  (backfill-tool-input-preview! [_ run-id receipt-id tool-name input-preview]
    (swap! events* conj {:op :tool-input-preview
                         :run-id run-id
                         :receipt-id receipt-id
                         :tool-name tool-name
                         :input-preview input-preview})))

(deftype ThrowingTokenSink []
  sinks/IRunEventSink
  (emit-run-event! [_ _] nil)
  (emit-token-event! [_ _] (throw (js/Error. "token sink failed")))
  (update-run-state! [_ _ _] nil)
  (update-session-record! [_ _ _] nil)
  (finalize-run! [_ result] result)
  (record-run-error! [_ _] nil)
  (append-trace-text! [_ _ _ _ _] nil)
  (update-tool-receipt! [_ _ _ _ _] nil)
  (apply-tool-trace-event! [_ _ _] nil)
  (backfill-tool-input-preview! [_ _ _ _ _] nil))

(defn- recording-abort-session
  [aborts*]
  (reify agent-shape/IAgentSession
    (streaming? [_] true)
    (current-turn [_] nil)
    (messages [_] [])
    (subscribe! [_ _handler] (fn [] nil))
    (send-user-message! [_ _content] (js/Promise. (fn [_resolve _reject] nil)))
    (follow-up! [_ _message] (js/Promise.resolve nil))
    (steer! [_ _message] (js/Promise.resolve nil))
    (set-thinking-level! [_ _level] nil)
    (abort! [_] (swap! aborts* inc) (js/Promise.resolve :aborted))))

(deftest request-abort-reaches-the-provider-session
  (testing "death-spiral / explicit abort routes through IAgentSession abort! to the raw provider"
    ;; Regression: request-abort! previously called (.abort session) on the wrapper
    ;; record, which has no such method — it threw and was swallowed, so runaway
    ;; turns were never actually stopped (only the now-removed timeout caught them).
    (let [events* (atom [])
          aborts* (atom 0)
          state (assoc (stream/make-stream-state "run" "conv" "sess" "now" 0 (fn [] "uuid"))
                       :run-event-sink (RecordingSink. events*))
          session (recording-abort-session aborts*)]
      (stream/request-abort! state session "death_spiral_detected")
      (is (= 1 @aborts*) "abort! must be invoked exactly once on the provider session")
      (is (some #(= "abort_requested" (get-in % [:event :type])) @events*)
          "an abort_requested run event should be emitted"))))

(deftest emit-streaming-delta-can-use-fake-run-event-sink
  (testing "stream token side effects are routed through IRunEventSink"
    (let [events* (atom [])
          state (assoc (stream/make-stream-state "run" "conv" "sess" "now" 0 #js {:randomUUID (fn [] "uuid")})
                       :run-event-sink (RecordingSink. events*))]
      (stream/emit-streaming-delta! state :agent_message "Hello")
      (is (= [:update-run :run-event :update-session :trace-text :token-event]
             (mapv :op @events*)))
      (is (= {:op :update-session
              :session-id "sess"
              :update {:op :mark-streaming :active? true}}
             (nth @events* 2)))
      (is (= {:run_id "run"
              :conversation_id "conv"
              :session_id "sess"
              :kind "assistant_message"
              :token "Hello"}
             (get-in @events* [4 :event]))))))

(deftest subscribe-handler-uniquifies-reused-provider-tool-call-ids
  (testing "rounds reusing toolCallId call_0 keep distinct receipts and emit every lifecycle event"
    (let [events* (atom [])
          state (assoc (stream/make-stream-state "run" "conv" "sess" "now" 0 (fn [] "uuid"))
                       :run-event-sink (RecordingSink. events*))
          handle! (stream/build-subscribe-handler state nil)]
      (handle! #js {:type "tool_execution_start" :toolName "discord_read" :toolCallId "call_0"
                    :params #js {:limit 20}})
      (handle! #js {:type "tool_execution_end" :toolName "discord_read" :toolCallId "call_0"
                    :result "ok"})
      (handle! #js {:type "tool_execution_start" :toolName "bluesky_timeline" :toolCallId "call_0"
                    :params #js {:limit 10}})
      (handle! #js {:type "tool_execution_end" :toolName "bluesky_timeline" :toolCallId "call_0"
                    :result "ok"})
      (let [receipts (filterv #(= :tool-receipt (:op %)) @events*)
            lifecycle-events (->> @events*
                                  (filter #(= :run-event (:op %)))
                                  (mapv (fn [{:keys [event]}] [(:type event) (:tool_call_id event)])))]
        (is (= ["call_0" "call_0" "call_0#2" "call_0#2"]
               (mapv :receipt-id receipts)))
        (is (= ["discord_read" "discord_read" "bluesky_timeline" "bluesky_timeline"]
               (mapv (comp :tool_name :receipt) receipts)))
        (is (= [["tool_start" "call_0"] ["tool_end" "call_0"]
                ["tool_start" "call_0#2"] ["tool_end" "call_0#2"]]
               lifecycle-events))))))

(deftest stream-sink-synchronous-failures-propagate
  (testing "sink wiring bugs are fatal instead of silently hiding stream corruption"
    (let [state (assoc (stream/make-stream-state "run" "conv" "sess" "now" 0 #js {:randomUUID (fn [] "uuid")})
                       :run-event-sink (ThrowingTokenSink.))]
      (is (thrown-with-msg? js/Error #"token sink failed"
                            (stream/emit-streaming-delta! state :agent_message "Hello"))))))

(deftest emit-streaming-delta-collapses-overlapping-provider-chunks
  (testing "defensively diffs cumulative or overlapping chunks before broadcasting/persisting"
    (let [events* (atom [])
          tokens* (atom [])
          state (stream/make-stream-state "run" "conv" "sess" "now" 0 #js {:randomUUID (fn [] "uuid")})]
      (with-redefs [run-state/update-run! (fn [_ f] (f {}) {})
                    run-state/append-run-event! (fn [& _] nil)
                    run-state/append-run-trace-text! (fn [_ kind delta _at]
                                                       (swap! tokens* conj {:kind kind :delta delta}))
                    run-state/backfill-run-tool-input-preview! (fn [& _] nil)
                    realtime/broadcast-ws-session! (fn [& args] (swap! events* conj args))
                    session-store/mark-session-streaming! (fn ([_ _] nil) ([_ _ _] nil))]
        (stream/emit-streaming-delta! state :reasoning "The")
        (stream/emit-streaming-delta! state :reasoning "TheThe model should reason once.")
        (is (= "The model should reason once." @(:last-reasoning-text* state)))
        (is (= [{:kind :reasoning :delta "The"}
                {:kind :reasoning :delta " model should reason once."}]
               @tokens*))))))

(deftest emit-streaming-delta-suppresses-incremental-prefix-replay
  (testing "drops Gemma answer prefix replay before appending new suffix"
    (let [events* (atom [])
          tokens* (atom [])
          state (stream/make-stream-state "run" "conv" "sess" "now" 0 #js {:randomUUID (fn [] "uuid")})]
      (with-redefs [run-state/update-run! (fn [_ f] (f {}) {})
                    run-state/append-run-event! (fn [& _] nil)
                    run-state/append-run-trace-text! (fn [_ kind delta _at]
                                                       (swap! tokens* conj {:kind kind :delta delta}))
                    run-state/backfill-run-tool-input-preview! (fn [& _] nil)
                    realtime/broadcast-ws-session! (fn [& args] (swap! events* conj args))
                    session-store/mark-session-streaming! (fn ([_ _] nil) ([_ _ _] nil))]
        (doseq [delta ["Ready." " How" "Ready" "." " How" " can" " I" " help" "?"]]
          (stream/emit-streaming-delta! state :agent_message delta))
        (is (= "Ready. How can I help?" @(:last-assistant-text* state)))
        (is (= [{:kind :agent_message :delta "Ready."}
                {:kind :agent_message :delta " How"}
                {:kind :agent_message :delta " can"}
                {:kind :agent_message :delta " I"}
                {:kind :agent_message :delta " help"}
                {:kind :agent_message :delta "?"}]
               @tokens*))))))

(deftest sync-assistant-message-does-not-duplicate-reasoning-delta
  (testing "terminal sync emits only appended delta and leaves last-reasoning-text* == full reasoning"
    (let [events* (atom [])
          tokens* (atom [])
          state (stream/make-stream-state "run" "conv" "sess" "now" 0 #js {:randomUUID (fn [] "uuid")})]
      (with-redefs [run-state/update-run! (fn [_ f] (f {}) {})
                    run-state/append-run-event! (fn [& _] nil)
                    run-state/append-run-trace-text! (fn [_ kind delta _at]
                                                       (swap! tokens* conj {:kind kind :delta delta}))
                    run-state/backfill-run-tool-input-preview! (fn [& _] nil)
                    realtime/broadcast-ws-session! (fn [& args] (swap! events* conj args))
                    session-store/mark-session-streaming! (fn ([_ _] nil) ([_ _ _] nil))]
        (stream/emit-streaming-delta! state :reasoning "The")
        (stream/sync-assistant-message! state (assistant-message {:reasoning "The reply has been sent."}))
        (is (= "The reply has been sent." @(:last-reasoning-text* state)))
        (is (not (.includes @(:last-reasoning-text* state) "sent.sent")))
        (is (= [{:kind :reasoning :delta "The"}
                {:kind :reasoning :delta " reply has been sent."}]
               @tokens*))))))

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
        (stream/emit-streaming-delta! state :reasoning "The" :replay)
        (stream/emit-streaming-delta! state :reasoning "TheThe model should reason once." :replay)
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
          (stream/emit-streaming-delta! state :agent_message delta :replay))
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

(deftest native-text-deltas-preserve-every-byte-through-terminal-sync
  (testing "literal tokens retain repeated characters, paragraph breaks, and repeated words"
    (let [deltas ["# CMS Demo" "\n" "\n" "Knox" "x" " generates a draft." "\n\n" "ha" "ha"]
          expected (apply str deltas)]
      (doseq [fragments [deltas [expected] ["# CMS Demo\n\nKnox" "x generates a draft.\n\nhaha"]]]
        (let [events* (atom [])
              state (assoc (stream/make-stream-state "run" "conv" "sess" "now" 0 (fn [] "uuid"))
                           :run-event-sink (RecordingSink. events*))
              handle! (stream/build-subscribe-handler state nil)]
          (handle! #js {:type "message_start" :message (assistant-message {})})
          (doseq [delta fragments]
            (handle! #js {:type "message_update"
                         :assistantMessageEvent #js {:type "text_delta" :delta delta}}))
          (is (= expected @(:last-assistant-text* state)))
          (handle! #js {:type "message_end" :message (assistant-message {:content expected})})
          (handle! #js {:type "message_end" :message (assistant-message {:content expected})})
          (is (= expected (apply str @(:chunks state))))
          (is (= expected (->> @events* (filter #(= :token-event (:op %)))
                               (map #(get-in % [:event :token])) (apply str))))
          (is (= expected (->> @events* (filter #(= :trace-text (:op %)))
                               (map :delta) (apply str)))))))))

(deftest native-partial-snapshots-are-cumulative-without-token-overlap-guesses
  (testing "the partial message supplies explicit cumulative context even when delta replays a prefix"
    (let [events* (atom [])
          state (assoc (stream/make-stream-state "run" "conv" "sess" "now" 0 (fn [] "uuid"))
                       :run-event-sink (RecordingSink. events*))
          handle! (stream/build-subscribe-handler state nil)
          expected "Knoxx\n\nhaha"]
      (doseq [[delta snapshot] [["Knox" "Knox"] ["x" "Knoxx"]
                                ["Knoxx\n\n" "Knoxx\n\n"]
                                ["ha" "Knoxx\n\nha"] ["ha" expected]
                                [expected expected]]]
        (handle! #js {:type "message_update"
                     :assistantMessageEvent #js {:type "text_delta" :delta delta
                                                 :partial (assistant-message {:content snapshot})}}))
      (stream/sync-assistant-message! state (assistant-message {:content expected}))
      (is (= expected (apply str @(:chunks state))))
      (is (= ["Knox" "x" "\n\n" "ha" "ha"]
             (->> @events* (filter #(= :token-event (:op %))) (mapv #(get-in % [:event :token]))))))))

(deftest native-array-snapshots-preserve-distinct-text-blocks
  (testing "partial and terminal content blocks preserve overlap, repeats, and blank lines"
    (doseq [fragments [["Knox" "x"] ["ha" "ha"] ["Title" "\n\n" "Body"]]]
      (let [events* (atom [])
            state (assoc (stream/make-stream-state "run" "conv" "sess" "now" 0 (fn [] "uuid"))
                         :run-event-sink (RecordingSink. events*))
            handle! (stream/build-subscribe-handler state nil)
            expected (apply str fragments)
            snapshot (assistant-message {:content (clj->js (mapv #(hash-map :type "text" :text %) fragments))})]
        (doseq [delta fragments]
          (handle! #js {:type "message_update" :assistantMessageEvent #js {:type "text_delta" :delta delta}}))
        (handle! #js {:type "message_update"
                     :assistantMessageEvent #js {:type "text_delta" :delta expected :partial snapshot}})
        (is (= expected @(:last-assistant-text* state)) "a partial array must not truncate streamed bytes")
        (is (= expected (apply str @(:chunks state))))
        (handle! #js {:type "message_end" :message snapshot})
        (is (= expected @(:last-assistant-text* state)) "a terminal array must preserve those same bytes")
        (is (= expected (apply str @(:chunks state))))
        (is (= expected (->> @events* (filter #(= :token-event (:op %)))
                             (map #(get-in % [:event :token])) (apply str))))))))

(deftest native-array-snapshots-preserve-distinct-reasoning-blocks
  (testing "reasoning partials and terminal snapshots retain literal thinking and reasoning blocks"
    (doseq [[part-type field fragments] [["thinking" :thinking ["ha" "ha"]]
                                       ["reasoning" :text ["Check" "\n\n" "Done"]]
                                       ["reasoning_text" :text ["Knox" "x"]]]]
      (let [events* (atom [])
            state (assoc (stream/make-stream-state "run" "conv" "sess" "now" 0 (fn [] "uuid"))
                         :run-event-sink (RecordingSink. events*))
            handle! (stream/build-subscribe-handler state nil)
            expected (apply str fragments)
            snapshot (assistant-message {:content (clj->js (conj (mapv #(hash-map :type part-type field %) fragments)
                                                                  {:type "text" :text "Answer"}))})]
        (doseq [delta fragments]
          (handle! #js {:type "message_update" :assistantMessageEvent #js {:type "reasoning_delta" :delta delta}}))
        (handle! #js {:type "message_update"
                     :assistantMessageEvent #js {:type "reasoning_delta" :delta expected :partial snapshot}})
        (is (= expected @(:last-reasoning-text* state)))
        (is (= expected (apply str @(:reasoning-chunks state))))
        (handle! #js {:type "message_end" :message snapshot})
        (is (= expected @(:last-reasoning-text* state)))
        (is (= expected (apply str @(:reasoning-chunks state))))
        (is (= "Answer" (apply str @(:chunks state))))))))

(deftest terminal-corrections-replace-only-the-current-provider-message
  (testing "an incompatible terminal snapshot never appends the full response as another token"
    (let [events* (atom [])
          state (assoc (stream/make-stream-state "run" "conv" "sess" "now" 0 (fn [] "uuid"))
                       :run-event-sink (RecordingSink. events*))
          handle! (stream/build-subscribe-handler state nil)
          first-text "Checking a tool.\n\n"
          corrected "Knoxx\n\nReview it."]
      (handle! #js {:type "message_start" :message (assistant-message {})})
      (stream/emit-streaming-delta! state :agent_message first-text)
      (stream/sync-assistant-message! state (assistant-message {:content first-text}))
      (handle! #js {:type "message_start" :message (assistant-message {})})
      (stream/emit-streaming-delta! state :agent_message "Knox draft")
      (stream/sync-assistant-message! state (assistant-message {:content corrected}))
      (stream/sync-assistant-message! state (assistant-message {:content corrected}))
      (is (= (str first-text corrected) (apply str @(:chunks state))))
      (is (= corrected @(:last-assistant-text* state)))
      (is (= [first-text "Knox draft"]
             (->> @events* (filter #(= :token-event (:op %))) (mapv #(get-in % [:event :token])))))
      (handle! #js {:type "message_start" :message #js {:role "tool"}})
      (is (= corrected @(:last-assistant-text* state)) "tool messages must not reset assistant boundaries"))))

(deftest reasoning-deltas-preserve-whitespace-and-repeated-characters
  (testing "both separate reasoning fields and think-tag routing preserve literal bytes"
    (doseq [think-tags? [false true]]
      (let [events* (atom [])
            state (assoc (stream/make-stream-state "run" "conv" "sess" "now" 0 (fn [] "uuid"))
                         :run-event-sink (RecordingSink. events*))
            handle! (stream/build-subscribe-handler state nil)]
        (when think-tags?
          (handle! #js {:type "message_update" :assistantMessageEvent #js {:type "text_delta" :delta "<think>"}}))
        (doseq [delta ["Knox" "x" "\n" "\n" "ha" "ha"]]
          (handle! #js {:type "message_update"
                       :assistantMessageEvent #js {:type (if think-tags? "text_delta" "reasoning_delta")
                                                   :delta delta}}))
        (when think-tags?
          (handle! #js {:type "message_update" :assistantMessageEvent #js {:type "text_delta" :delta "</think>Answer"}}))
        (stream/sync-assistant-message! state (assistant-message {:content (if think-tags? "Answer" "")
                                                               :reasoning "Knoxx\n\nhaha"}))
        (is (= "Knoxx\n\nhaha" (apply str @(:reasoning-chunks state))))
        (is (= (if think-tags? "Answer" "") (apply str @(:chunks state))))))))

(deftest omitted-terminal-reasoning-keeps-the-observed-stream
  (testing "an answer-only terminal snapshot does not erase streamed reasoning"
    (let [events* (atom [])
          state (assoc (stream/make-stream-state "run" "conv" "sess" "now" 0 (fn [] "uuid"))
                       :run-event-sink (RecordingSink. events*))]
      (stream/emit-streaming-delta! state :reasoning "Knoxx\n\nReasoning")
      (stream/sync-assistant-message! state (assistant-message {:content "Answer"}))
      (is (= "Knoxx\n\nReasoning" @(:last-reasoning-text* state)))
      (is (= "Knoxx\n\nReasoning" (apply str @(:reasoning-chunks state))))
      (is (= "Answer" (apply str @(:chunks state)))))))

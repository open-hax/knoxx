(ns knoxx.backend.agents.stream-correction-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.action.run-state :as run-state]
            [knoxx.backend.domain.realtime :as realtime]
            [knoxx.backend.infra.agent.stream :as stream]
            [knoxx.backend.infra.agent.stream.sinks :as sinks]
            [knoxx.backend.infra.stores.mongo-session-store :as session-store]))

(defn- with-live-stream [check!]
  (let [previous-runs @run-state/runs*
        packets* (atom [])
        state (stream/make-stream-state "correction" "conv" "sess" "now" 0 (fn [] "uuid"))]
    (try
      (swap! run-state/runs* assoc "correction" {:trace_blocks [] :events []})
      (with-redefs [realtime/broadcast-ws-session! (fn [_ topic packet]
                                                   (when (= topic "tokens") (swap! packets* conj packet)))
                    session-store/mark-session-streaming! (fn ([_ _] nil) ([_ _ _] nil))
                    run-state/append-run-event! (fn [_ _] nil)]
        (check! state packets* #(get @run-state/runs* "correction")))
      (finally (reset! run-state/runs* previous-runs)))))

(deftest deltas-after-correction-extend-the-corrected-native-trace
  (with-live-stream
    (fn [state packets* run]
      (stream/emit-streaming-delta! state :agent_message "Wrong draft")
      (stream/sync-assistant-message! state #js {:role "assistant" :content "Knoxx\n\n"})
      (stream/emit-streaming-delta! state :agent_message "ha")
      (stream/emit-streaming-delta! state :agent_message "ha")
      (stream/sync-assistant-message! state #js {:role "assistant" :content "Knoxx\n\nhaha"})
      (is (= "Knoxx\n\nhaha" (apply str @(:chunks state))))
      (is (= "Knoxx\n\nhaha" (apply str (map :content (:trace_blocks (run))))))
      (is (= [{:token "Wrong draft"} {:token "Knoxx\n\n" :operation "replace" :offset 0}
              {:token "ha"} {:token "ha"}]
             (mapv #(select-keys % [:token :operation :offset]) @packets*))))))

(deftest empty-answer-correction-preserves-only-earlier-provider-messages
  (with-live-stream
    (fn [state packets* run]
      (let [handle! (stream/build-subscribe-handler state nil)
            prefix "Earlier 😀.\n\n"]
        (stream/emit-streaming-delta! state :agent_message prefix)
        (handle! #js {:type "message_start" :message #js {:role "assistant" :content ""}})
        (stream/emit-streaming-delta! state :agent_message "Remove this")
        (stream/sync-assistant-message! state #js {:role "assistant" :content ""})
        (stream/sync-assistant-message! state #js {:role "assistant" :content ""})
        (is (= prefix (apply str @(:chunks state))))
        (is (= prefix (apply str (map :content (:trace_blocks (run))))))
        (is (= [{:token prefix :operation "replace" :offset (count prefix)}]
               (mapv #(select-keys % [:token :operation :offset])
                     (filter :operation @packets*))))))))

(deftest native-delta-after-other-channel-deletion-has-a-unique-trace-id
  (with-live-stream
    (fn [state _packets* run]
      (stream/emit-streaming-delta! state :reasoning "R1")
      (stream/emit-streaming-delta! state :agent_message "A1")
      (stream/emit-streaming-delta! state :reasoning "R2")
      (stream/emit-streaming-delta! state :agent_message "A2")
      (run-state/apply-run-tool-trace-event! "correction" {:type "tool_end" :tool_call_id "t" :at "tool"})
      (sinks/replace-stream-text! state :reasoning 0 "")
      (stream/emit-streaming-delta! state :agent_message "Next")
      (let [blocks (:trace_blocks (run))]
        (is (= ["agent_message:1" "agent_message:3" "tool:t" "agent_message:4"] (mapv :id blocks)))
        (is (= "A1A2Next" (apply str (map :content (filter #(= :agent_message (:kind %)) blocks)))))
        (is (= (count blocks) (count (set (map :id blocks)))))))))

(deftest explicit-empty-reasoning-corrects-while-omission-preserves-observations
  (doseq [explicit? [false true] partial? [false true]]
    (with-live-stream
      (fn [state packets* run]
        (let [handle! (stream/build-subscribe-handler state nil)
              prefix "Earlier 🔬.\n\n"]
          (stream/emit-streaming-delta! state :reasoning prefix)
          (handle! #js {:type "message_start" :message #js {:role "assistant" :content ""}})
          (stream/emit-streaming-delta! state :reasoning "Current draft")
          (let [snapshot (if explicit? #js {:role "assistant" :content "Answer" :reasoning ""}
                                      #js {:role "assistant" :content "Answer"})]
            (handle! (if partial?
                       #js {:type "message_update" :assistantMessageEvent #js {:type "reasoning_delta"
                                                                              :delta "" :partial snapshot}}
                       #js {:type "message_end" :message snapshot})))
          (let [expected (str prefix (when-not explicit? "Current draft"))
                replacements (filter #(= "reasoning" (:kind %)) (filter :operation @packets*))]
            (is (= expected (apply str @(:reasoning-chunks state))))
            (is (= expected (apply str (map :content (filter #(= :reasoning (:kind %)) (:trace_blocks (run)))))))
            (is (= (if explicit? [{:token prefix :operation "replace" :offset (count prefix)}] [])
                   (mapv #(select-keys % [:token :operation :offset]) replacements)))))))))

(deftest native-replacement-converges-incomplete-retained-channel-text
  (doseq [retained [nil "Earlier " "Stale prior text"]]
    (with-live-stream
      (fn [state packets* run]
        (let [prefix "Earlier 😀.\n\n"
              snapshot (str prefix "Corrected\n\nhaha")
              tool {:id "tool:t" :kind :tool_call :status "done"}
              other {:id "reasoning:1" :kind :reasoning :content "Keep reasoning" :status "done"}
              blocks (cond-> [tool other] retained (conj {:id "agent_message:2" :kind :agent_message
                                                         :content retained :status "streaming"}))]
          (swap! run-state/runs* assoc-in ["correction" :trace_blocks] blocks)
          (sinks/replace-stream-text! state :agent_message (count prefix) snapshot)
          (let [projected (:trace_blocks (run))]
            (is (= snapshot (apply str (map :content (filter #(= :agent_message (:kind %)) projected)))))
            (is (= [tool other] (filterv #(not= :agent_message (:kind %)) projected)))
            (is (= [{:token snapshot :operation "replace" :offset (count prefix)}]
                   (mapv #(select-keys % [:token :operation :offset]) @packets*)))))))))

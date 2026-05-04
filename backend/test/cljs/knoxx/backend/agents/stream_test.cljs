(ns knoxx.backend.agents.stream-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.agents.stream :as stream]
            [knoxx.backend.run-state :as run-state]
            [knoxx.backend.realtime :as realtime]
            [knoxx.backend.session-store :as session-store]
            [knoxx.backend.redis-client :as redis]))

(defn- assistant-message
  [{:keys [content reasoning tool-previews]}]
  (let [m #js {:role "assistant"
               :content (or content "")}] 
    (when (some? reasoning)
      (aset m "reasoning" reasoning))
    (when (some? tool-previews)
      (aset m "tool_calls" tool-previews))
    m))

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
                    session-store/mark-session-streaming! (fn [& _] nil)
                    redis/get-client (fn [] nil)]
        ;; Streamed first token.
        (stream/emit-streaming-delta! state :reasoning "The")

        ;; Terminal message arrives with cumulative reasoning.
        (stream/sync-assistant-message! state (assistant-message {:reasoning "The reply has been sent."}))

        (is (= "The reply has been sent." @(:last-reasoning-text* state)))
        ;; Ensure we did not append the delta twice onto last-reasoning-text*.
        (is (not (.includes @(:last-reasoning-text* state) "sent.sent")))
        ;; Ensure emitted delta is just the novel suffix, not full cumulative.
        (is (= [{:kind :reasoning :delta "The"}
                {:kind :reasoning :delta " reply has been sent."}]
               @tokens*))))))

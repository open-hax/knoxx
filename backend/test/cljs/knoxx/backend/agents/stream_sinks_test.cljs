(ns knoxx.backend.agents.stream-sinks-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.action.run-state :as run-state]
            [knoxx.backend.domain.realtime :as realtime]
            [knoxx.backend.infra.agent.stream.sinks :as sinks]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.infra.stores.session-store :as session-store]))

(deftest live-run-event-sink-forwards-observability-effects
  (let [calls* (atom [])
        redis-client #js {:kind "redis"}
        sink (sinks/live-run-event-sink)]
    (with-redefs [run-state/append-run-event! (fn [run-id event]
                                                (swap! calls* conj [:append-run-event run-id event]))
                  realtime/broadcast-ws-session! (fn [session-id topic event]
                                                   (swap! calls* conj [:broadcast session-id topic event]))
                  run-state/update-run! (fn [run-id update-fn]
                                          (swap! calls* conj [:update-run run-id (update-fn {:n 1})]))
                  redis/get-client (fn [] redis-client)
                  session-store/mark-session-streaming! (fn [client session-id active?]
                                                          (swap! calls* conj [:mark-streaming client session-id active?]))
                  run-state/append-run-trace-text! (fn [run-id kind delta at]
                                                     (swap! calls* conj [:append-trace run-id kind delta at]))
                  run-state/update-run-tool-receipt! (fn [run-id receipt-id default-receipt update-fn]
                                                       (swap! calls* conj [:tool-receipt run-id receipt-id (update-fn default-receipt)]))
                  run-state/apply-run-tool-trace-event! (fn [run-id trace-event]
                                                          (swap! calls* conj [:tool-trace run-id trace-event]))
                  run-state/backfill-run-tool-input-preview! (fn [run-id receipt-id tool-name input-preview]
                                                               (swap! calls* conj [:backfill run-id receipt-id tool-name input-preview]))]
      (sinks/emit-run-event! sink {:run_id "run-1" :session_id "session-1" :status "running"})
      (sinks/emit-token-event! sink {:session_id "session-1" :delta "hi"})
      (sinks/update-run-state! sink "run-1" #(assoc % :done true))
      (sinks/update-session-record! sink "session-1" {:op :mark-streaming :active? true})
      (is (= {:ok true} (sinks/finalize-run! sink {:ok true})))
      (sinks/record-run-error! sink {:run_id "run-1" :session_id "session-1" :status "failed"})
      (sinks/append-trace-text! sink "run-1" :agent_message "hello" "t0")
      (sinks/update-tool-receipt! sink "run-1" "receipt-1" {:updates []} #(update % :updates conj "done"))
      (sinks/apply-tool-trace-event! sink "run-1" {:type "tool_end"})
      (sinks/backfill-tool-input-preview! sink "run-1" "receipt-1" "memory.search" "query")
      (is (= [[:append-run-event "run-1" {:run_id "run-1" :session_id "session-1" :status "running"}]
              [:broadcast "session-1" "events" {:run_id "run-1" :session_id "session-1" :status "running"}]
              [:broadcast "session-1" "tokens" {:session_id "session-1" :delta "hi"}]
              [:update-run "run-1" {:n 1 :done true}]
              [:mark-streaming redis-client "session-1" true]
              [:append-run-event "run-1" {:run_id "run-1" :session_id "session-1" :status "failed"}]
              [:broadcast "session-1" "events" {:run_id "run-1" :session_id "session-1" :status "failed"}]
              [:append-trace "run-1" :agent_message "hello" "t0"]
              [:tool-receipt "run-1" "receipt-1" {:updates ["done"]}]
              [:tool-trace "run-1" {:type "tool_end"}]
              [:backfill "run-1" "receipt-1" "memory.search" "query"]]
             @calls*)))))

(deftest sink-or-default-prefers-injected-sink
  (let [custom-sink {:fake true}]
    (is (= custom-sink (sinks/sink-or-default {:run-event-sink custom-sink})))
    (is (satisfies? sinks/IRunEventSink (sinks/sink-or-default {})))))

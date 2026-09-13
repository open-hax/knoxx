(ns knoxx.backend.extern.session-reclaim-admission-test
  "A failed durable orphan reclaim cannot grant a new turn ownership."
  (:require [cljs.test :as test]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.infra.agent.session :as session]
            [knoxx.backend.infra.agent.session-gate :as gate]
            [knoxx.backend.infra.stores.mongo-session-store :as sessions]
            [knoxx.backend.infra.system-instance :as instance]))

(test/deftest ^:async durable-reclaim-failure-keeps-both-orphan-branches-busy
  (doseq [current-owner? [false true]]
    (let [dispatched* (atom 0) attempted* (atom 0)]
      (with-redefs [sessions/get-session (fn ([_id] (js/Promise.resolve {:status "running" :updated_at 0}))
                                              ([_db _id] (throw (ex-info "Unexpected fixture arity" {}))))
                    sessions/complete-session! (fn ([_id _conversation _update]
                                                    (swap! attempted* inc)
                                                    (js/Promise.reject (ex-info "durable store refused" {:code "write_refused"})))
                                                   ([_db _id _conversation _update]
                                                    (throw (ex-info "Unexpected fixture arity" {}))))
                    session/active-agent-session (constantly nil)
                    turn-control/active-turn (constantly nil)
                    instance/owned-by-current-instance? (constantly current-owner?)]
        (let [error (try
                      (await (gate/dispatch-with-session-gate!
                              {} {} {:session-id "orphan" :conversation-id "orphan-conversation"}
                              (^:async fn [_runtime _config _body] (swap! dispatched* inc))))
                      nil
                      (catch :default error error))]
          (test/is (= 1 @attempted*) "the durable reclaim was attempted")
          (test/is (zero? @dispatched*) "failed persistence must never authorize a second turn")
          (test/is (= "agent_already_processing: orphaned session reclaim failed"
                      (ex-message error))))))))

(test/deftest ^:async successful-durable-reclaim-precedes-dispatch
  (let [observed* (atom [])]
    (with-redefs [sessions/get-session (fn ([_id] (js/Promise.resolve {:status "running"}))
                                            ([_db _id] (throw (ex-info "Unexpected fixture arity" {}))))
                  sessions/complete-session! (fn ([_id _conversation status-update]
                                                  (swap! observed* conj (:status status-update)) (js/Promise.resolve true))
                                                 ([_db _id _conversation _update]
                                                  (throw (ex-info "Unexpected fixture arity" {}))))
                  session/active-agent-session (constantly nil)
                  instance/owned-by-current-instance? (constantly false)]
      (await (gate/dispatch-with-session-gate!
              {} {} {:session-id "orphan" :conversation-id "orphan-conversation"}
              (^:async fn [_runtime _config _body] (swap! observed* conj "dispatch"))))
      (test/is (= ["failed" "dispatch"] @observed*)))))

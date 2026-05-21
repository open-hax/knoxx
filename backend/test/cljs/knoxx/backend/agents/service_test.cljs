(ns knoxx.backend.agents.service-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.agent.service :as service]))

(deftest ^:async agent-service-delegates-start-queue-control-resume-active
  (let [calls* (atom [])
        svc (service/agent-service
             :runtime
             {:cfg true}
             {:start-turn! (fn [runtime config request]
                             (swap! calls* conj [:start runtime config request])
                             (js/Promise.resolve {:started (:run-id request)}))
              :queue-turn! (fn [runtime config request]
                             (swap! calls* conj [:queue runtime config request])
                             (js/Promise.resolve {:queued true :run_id (:run-id request)}))
              :control-turn! (fn [runtime config request]
                               (swap! calls* conj [:control runtime config request])
                               (js/Promise.resolve {:ok true :kind (:kind request)}))
              :resume-turn! (fn [runtime config session opts]
                              (swap! calls* conj [:resume runtime config session opts])
                              (js/Promise.resolve {:resumed true :session_id (:session_id session)}))
              :active-turn (fn [conversation-id]
                             (swap! calls* conj [:active conversation-id])
                             {:conversation-id conversation-id})})]
    (testing "facade methods delegate to configured implementation"
      (is (= {:started "run-1"}
             (await (service/start-turn! svc {:run-id "run-1"}))))
      (is (= {:queued true :run_id "run-2"}
             (await (service/queue-turn! svc {:run-id "run-2"}))))
      (is (= {:ok true :kind "follow_up"}
             (await (service/control-turn! svc {:kind "follow_up"}))))
      (is (= {:resumed true :session_id "sess-1"}
             (await (service/resume-turn! svc {:session {:session_id "sess-1"}
                                               :opts {:wait-for :kickoff}}))))
      (is (= {:conversation-id "conv-1"}
             (service/active-turn svc "conv-1")))
      (is (= [[:start :runtime {:cfg true} {:run-id "run-1"}]
              [:queue :runtime {:cfg true} {:run-id "run-2"}]
              [:control :runtime {:cfg true} {:kind "follow_up"}]
              [:resume :runtime {:cfg true} {:session_id "sess-1"} {:wait-for :kickoff}]
              [:active "conv-1"]]
             @calls*)))))

(deftest ^:async compatibility-wrappers-preserve-response-shapes
  (let [calls* (atom [])
        svc (service/agent-service
             :runtime
             {:cfg true}
             {:start-turn! (fn [_runtime _config request]
                             (swap! calls* conj [:start request])
                             (js/Promise.resolve {:done true}))
              :control-turn! (fn [_runtime _config request]
                               (swap! calls* conj [:control request])
                               (js/Promise.resolve {:ok true :run_id (:run-id request)}))
              :resume-turn! (fn [_runtime _config session opts]
                              (swap! calls* conj [:resume session opts])
                              (js/Promise.resolve {:resumed true}))})]
    (testing "queue-turn defaults to accepted async response while launching start-turn"
      (is (= {:ok true
              :queued true
              :run_id "run-3"
              :runId "run-3"
              :conversation_id "conv-3"
              :conversationId "conv-3"
              :session_id "sess-3"
              :sessionId "sess-3"
              :model "model-3"}
             (await (service/queue-turn! svc {:run-id "run-3"
                                              :conversation-id "conv-3"
                                              :session-id "sess-3"
                                              :model "model-3"})))))
    (testing "old names remain transitional wrappers"
      (with-redefs [service/start-turn-runtime! (fn [runtime config request]
                                                  (is (= :runtime runtime))
                                                  (is (= {:cfg true} config))
                                                  (js/Promise.resolve {:started (:run-id request)}))
                    service/control-turn-runtime! (fn [_runtime _config request]
                                                    (js/Promise.resolve {:controlled (:run-id request)}))
                    service/resume-turn-runtime! (fn [_runtime _config request]
                                                   (js/Promise.resolve {:resumed (get-in request [:session :session_id])
                                                                        :opts (:opts request)}))]
        (is (= {:started "run-4"}
               (await (service/send-agent-turn! :runtime {:cfg true} {:run-id "run-4"}))))
        (is (= {:controlled "run-5"}
               (await (service/queue-agent-control! :runtime {:cfg true} {:run-id "run-5"}))))
        (is (= {:resumed "sess-4" :opts {:wait-for :kickoff}}
               (await (service/resume-recovered-session! :runtime {:cfg true} {:session_id "sess-4"} {:wait-for :kickoff}))))))))

(deftest default-service-can-be-bound-for-simple-namespace-functions
  (let [previous @service/default-service*
        svc (service/agent-service :runtime {:cfg true} {:active-turn (fn [conversation-id]
                                                                        {:id conversation-id})})]
    (try
      (service/set-default-service! svc)
      (is (= {:id "conv-default"} (service/active-turn "conv-default")))
      (finally
        (reset! service/default-service* previous)))))

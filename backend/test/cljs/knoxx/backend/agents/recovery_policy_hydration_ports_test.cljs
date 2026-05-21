(ns knoxx.backend.agents.recovery-policy-hydration-ports-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.agent.hydration-sources :as hydration-sources]
            [knoxx.backend.infra.agent.policy :as policy]
            [knoxx.backend.infra.agent.recovery-coordinator :as recovery-coordinator]))

(deftest ^:async policy-engine-can-be-faked-and-default-preserves-model-check
  (testing "fake policy engine supports turn orchestration tests without Redis/authz"
    (let [calls* (atom [])
          fake (reify policy/IPolicyEngine
                 (authorize-turn [_ turn-request]
                   (swap! calls* conj [:authorize turn-request])
                   (js/Promise.resolve {:ok true}))
                 (resolve-model-policy [_ auth-context requested-model]
                   (swap! calls* conj [:model auth-context requested-model])
                   (js/Promise.resolve {:model requested-model :allowed true}))
                 (resolve-tool-policy [_ auth-context agent-spec]
                   (swap! calls* conj [:tools auth-context agent-spec])
                   (js/Promise.resolve {:tools []}))
                 (resolve-resource-policy [_ auth-context agent-spec]
                   (swap! calls* conj [:resources auth-context agent-spec])
                   (js/Promise.resolve {:resources []})))]
      (is (= {:ok true}
             (await (policy/authorize-turn fake {:model "model-a" :auth-context {:userId "u"}}))))
      (is (= [[:authorize {:model "model-a" :auth-context {:userId "u"}}]] @calls*))))
  (testing "default model policy delegates to existing chat policy enforcement"
    (with-redefs [policy/enforce-chat-policy! (fn [auth-context model-id]
                                                (is (= {:userId "u"} auth-context))
                                                (is (= "model-b" model-id))
                                                (js/Promise.resolve nil))]
      (is (= {:model "model-b" :allowed true}
             (await (policy/resolve-model-policy policy/default-policy-engine
                                                  {:userId "u"}
                                                  "model-b")))))))

(deftest ^:async hydration-source-ports-preserve-semantic-and-memory-payloads
  (testing "fake hydration source gives orchestration tests a common hydrate method"
    (let [fake (reify hydration-sources/IHydrationSource
                 (hydrate [_ request]
                   (js/Promise.resolve {:request request :elapsedMs 1})))]
      (is (= {:request {:message "hello"} :elapsedMs 1}
             (await (hydration-sources/hydrate fake {:message "hello"}))))))
  (testing "semantic and memory adapters delegate to existing hydration functions"
    (with-redefs [hydration-sources/semantic-hydrate! (fn [runtime config {:keys [mode message auth-context]}]
                                                        (is (= :runtime runtime))
                                                        (is (= {:cfg true} config))
                                                        (js/Promise.resolve {:results [{:path "doc.md"}]
                                                                             :mode mode
                                                                             :query message
                                                                             :auth auth-context}))
                  hydration-sources/memory-hydrate! (fn [config {:keys [conversation-id message auth-context agent-spec]}]
                                                      (is (= {:cfg true} config))
                                                      (js/Promise.resolve {:hits [{:text "memory"}]
                                                                           :conversationId conversation-id
                                                                           :query message
                                                                           :auth auth-context
                                                                           :agent agent-spec}))]
      (let [semantic (hydration-sources/semantic-source :runtime {:cfg true})
            memory (hydration-sources/memory-source {:cfg true})]
        (is (= {:results [{:path "doc.md"}]
                :mode "rag"
                :query "find docs"
                :auth {:userId "u"}}
               (await (hydration-sources/hydrate semantic {:mode "rag"
                                                           :message "find docs"
                                                           :auth-context {:userId "u"}}))))
        (is (= {:hits [{:text "memory"}]
                :conversationId "conv"
                :query "remember"
                :auth {:userId "u"}
                :agent {:contract-id "agent"}}
               (await (hydration-sources/hydrate memory {:conversation-id "conv"
                                                         :message "remember"
                                                         :auth-context {:userId "u"}
                                                         :agent-spec {:contract-id "agent"}}))))))))

(deftest ^:async recovery-coordinator-can-be-faked-and-default-delegates
  (testing "fake coordinator supports lifecycle tests"
    (let [calls* (atom [])
          fake (reify recovery-coordinator/IRecoveryCoordinator
                 (recover-startup! [_]
                   (swap! calls* conj :startup)
                   (js/Promise.resolve {:startup true}))
                 (recover-session! [_ request]
                   (swap! calls* conj [:recover request])
                   (js/Promise.resolve {:recovered true}))
                 (resume-turn! [_ request]
                   (swap! calls* conj [:resume request])
                   (js/Promise.resolve {:resumed true}))
                 (shutdown-recovery! [_]
                   (swap! calls* conj :shutdown)
                   (js/Promise.resolve {:stopped true})))]
      (is (= {:startup true} (await (recovery-coordinator/recover-startup! fake))))
      (is (= {:resumed true} (await (recovery-coordinator/resume-turn! fake {:session {:session_id "s"}}))))
      (is (= [:startup [:resume {:session {:session_id "s"}}]] @calls*))))
  (testing "default coordinator delegates to existing resume/recovery functions"
    (with-redefs [recovery-coordinator/startup-recover! (fn [runtime app config]
                                                          (is (= :runtime runtime))
                                                          (is (= :app app))
                                                          (is (= {:cfg true} config))
                                                          (js/Promise.resolve {:startup :ok}))
                  recovery-coordinator/resume-recovered! (fn [runtime config {:keys [session opts]}]
                                                           (is (= :runtime runtime))
                                                           (is (= {:cfg true} config))
                                                           (js/Promise.resolve {:session session :opts opts :resumed true}))
                  recovery-coordinator/stop-recovery! (fn [] (js/Promise.resolve {:stopped true}))]
      (let [coordinator (recovery-coordinator/recovery-coordinator :runtime :app {:cfg true})]
        (is (= {:startup :ok}
               (await (recovery-coordinator/recover-startup! coordinator))))
        (is (= {:session {:session_id "s"}
                :opts {:wait-for :kickoff}
                :resumed true}
               (await (recovery-coordinator/resume-turn! coordinator
                                                         {:session {:session_id "s"}
                                                          :opts {:wait-for :kickoff}}))))
        (is (= {:stopped true}
               (await (recovery-coordinator/shutdown-recovery! coordinator))))))))

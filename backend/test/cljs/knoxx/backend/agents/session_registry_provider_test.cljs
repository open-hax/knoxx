(ns knoxx.backend.agents.session-registry-provider-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.eta-mu :as eta-mu-extern]
            [knoxx.backend.infra.agent.provider :as agent-provider]
            [knoxx.backend.infra.agent.provider.eta-mu :as eta-mu-provider]
            [knoxx.backend.infra.agent.session-registry :as registry]))

(deftest atom-session-registry-supports-active-touch-remove-and-sweep
  (testing "active session registry behavior is testable without eta-mu runtime"
    (let [now* (atom 1000)
          sessions* (atom {})
          reg (registry/atom-registry sessions* {:max-sessions 2
                                                 :inactive-ttl-ms 100
                                                 :now-ms (fn [] @now*)})]
      (registry/put-active-session! reg "conv-1" {:session :s1 :model-id "m1"})
      (is (= :s1 (registry/get-active-session reg "conv-1")))
      (is (= 1000 (:last-accessed (registry/get-active-session-entry reg "conv-1"))))

      (reset! now* 1050)
      (registry/touch-active-session! reg "conv-1")
      (is (= 1050 (:last-accessed (registry/get-active-session-entry reg "conv-1"))))

      (registry/put-active-session! reg "conv-2" {:session :s2 :last-accessed 1060})
      (registry/put-active-session! reg "conv-3" {:session :s3 :last-accessed 1070})
      (is (nil? (registry/get-active-session reg "conv-1")) "oldest entry is evicted past max size")
      (is (= #{"conv-2" "conv-3"} (set (keys @sessions*))))

      (reset! now* 1161)
      (is (= ["conv-2"] (registry/sweep-expired-sessions! reg @now*)))
      (is (= #{"conv-3"} (set (keys @sessions*))))

      (registry/remove-active-session! reg "conv-3")
      (is (empty? @sessions*)))))

(deftest provider-adapter-port-can-be-faked
  (testing "agent provider behavior can be mocked without eta-mu runtime startup"
    (let [calls* (atom [])
          fake-provider (reify eta-mu-provider/IAgentProviderAdapter
                          (ensure-runtime! [_]
                            (swap! calls* conj :ensure-runtime)
                            (js/Promise.resolve {:model-registry :registry}))
                          (resolve-model [_ model-registry model-provider-id model-id fallback-model-id]
                            (swap! calls* conj [:resolve model-registry model-provider-id model-id fallback-model-id])
                            {:id model-id})
                          (create-session! [_ session-request]
                            (swap! calls* conj [:create (:model session-request)])
                            {:session-id (:session-id session-request)})
                          (send-message! [_ provider-session message-request]
                            (swap! calls* conj [:send provider-session message-request])
                            (js/Promise.resolve {:ok true}))
                          (subscribe-stream! [_ provider-session handlers]
                            (swap! calls* conj [:subscribe provider-session handlers])
                            (js/Promise.resolve :subscribed)))]
      (is (= {:id "model-1"}
             (eta-mu-provider/resolve-model fake-provider :registry "proxx" "model-1" "fallback")))
      (is (= {:session-id "sess-1"}
             (eta-mu-provider/create-session! fake-provider {:session-id "sess-1" :model {:id "model-1"}})))
      (is (= [[:resolve :registry "proxx" "model-1" "fallback"]
              [:create {:id "model-1"}]]
             @calls*)))))

(deftest ^:async eta-mu-provider-adapter-delegates-to-runtime-boundaries
  (let [calls* (atom [])
        provider (eta-mu-provider/eta-mu-provider :runtime {:agent-compaction-enabled? true
                                                            :agent-compaction-reserve-tokens 10
                                                            :agent-compaction-keep-recent-tokens 20})]
    (with-redefs [agent-provider/fetch-proxx-model-ids! (fn [config]
                                                          (swap! calls* conj [:fetch-models config])
                                                          (js/Promise.resolve ["glm-5"]))
                  eta-mu-extern/setup-runtime! (fn [config models-config compaction]
                                                 (swap! calls* conj [:setup config (set (keys (:providers models-config))) compaction])
                                                 (js/Promise.resolve {:registry :ready}))
                  eta-mu-extern/find-model (fn [registry provider-id model-id fallback]
                                             (swap! calls* conj [:find registry provider-id model-id fallback])
                                             {:id model-id :provider provider-id})
                  eta-mu-extern/create-session! (fn [request]
                                                  (swap! calls* conj [:create request])
                                                  {:session-id (:session-id request)})]
      (is (= {:registry :ready} (await (eta-mu-provider/ensure-runtime! provider))))
      (is (= {:id "glm-5" :provider "proxx"}
             (eta-mu-provider/resolve-model provider :registry "proxx" "glm-5" "fallback")))
      (is (= {:session-id "session-1"}
             (eta-mu-provider/create-session! provider {:session-id "session-1"})))
      (is (= [[:fetch-models {:agent-compaction-enabled? true
                              :agent-compaction-reserve-tokens 10
                              :agent-compaction-keep-recent-tokens 20}]
              [:setup {:agent-compaction-enabled? true
                       :agent-compaction-reserve-tokens 10
                       :agent-compaction-keep-recent-tokens 20}
               #{:proxx}
               {:enabled true :reserveTokens 10 :keepRecentTokens 20}]
              [:find :registry "proxx" "glm-5" "fallback"]
              [:create {:session-id "session-1"}]]
             @calls*)))))

(deftest ^:async eta-mu-provider-adapter-requires-injected-send-and-subscribe-functions
  (let [provider (eta-mu-provider/eta-mu-provider :runtime {})
        send! (fn [session request]
                (js/Promise.resolve {:session session
                                     :request-id (:id request)}))
        subscribe! (fn [session handlers]
                     (js/Promise.resolve {:session session
                                          :handler-keys (set (keys handlers))}))]
    (is (= {:session :provider-session :request-id "message-1"}
           (await (eta-mu-provider/send-message! provider
                                                 :provider-session
                                                 {:id "message-1"
                                                  :send-message! send!}))))
    (try
      (await (eta-mu-provider/send-message! provider :provider-session {}))
      (is false "expected send-message rejection")
      (catch :default err
        (is (re-find #"requires :send-message!" (.-message err)))))
    (is (= {:session :provider-session :handler-keys #{:subscribe! :on-token}}
           (await (eta-mu-provider/subscribe-stream! provider
                                                     :provider-session
                                                     {:subscribe! subscribe!
                                                      :on-token identity}))))
    (try
      (await (eta-mu-provider/subscribe-stream! provider :provider-session {}))
      (is false "expected subscribe rejection")
      (catch :default err
        (is (re-find #"requires :subscribe!" (.-message err)))))))

(ns knoxx.backend.agents.session-registry-provider-test
  (:require [cljs.test :refer [deftest is testing]]
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

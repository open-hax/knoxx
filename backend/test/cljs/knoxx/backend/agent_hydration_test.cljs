(ns knoxx.backend.agent-hydration-test
  (:require [cljs.test :refer [async deftest is testing]]
            [knoxx.backend.infra.agent.hydration :as agent-hydration]
            [knoxx.backend.infra.http :as backend-http]))

(deftest agent-custom-tool-suite-selects-contract-librarian-runtime
  (testing "contract librarian sessions use the dedicated contract-oriented tool suite"
    (is (= :contract-librarian
           (agent-hydration/agent-custom-tool-suite {:contract-id "contract_librarian"
                                                     :role "contract_librarian"})))
    (is (= :contract-librarian
           (agent-hydration/agent-custom-tool-suite {:contract-id "contract_librarian"})))
    (is (= :knoxx
           (agent-hydration/agent-custom-tool-suite {:contract-id "knoxx_default"
                                                     :role "knowledge_worker"})))))

(deftest passive-memory-hydration-failure-is-non-fatal
  (testing "OpenPlanner outage must not abort an agent turn before tools can run"
    (async done
      (with-redefs [backend-http/openplanner-enabled? (fn [_] true)
                    agent-hydration/openplanner-memory-search! (fn [_ _]
                                                                 (js/Promise.reject (js/Error. "OpenPlanner 502")))]
        (-> (agent-hydration/passive-memory-hydration!
             {:openplanner-base-url "http://openplanner.local"
              :openplanner-api-key "test"}
             "conversation-a"
             "remember prior context"
             nil
             {:memory-hydration {:enabled? true :mode :always :k 4}})
            (.then (fn [result]
                     (is (nil? result))
                     (done)))
            (.catch (fn [err]
                      (is false (str "memory hydration should not reject: " (.-message err)))
                      (done))))))))

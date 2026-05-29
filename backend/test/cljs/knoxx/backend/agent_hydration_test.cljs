(ns knoxx.backend.agent-hydration-test
  (:require [cljs.test :refer [async deftest is testing]]
            [knoxx.backend.extern.eta-mu :as eta-mu-extern]
            [knoxx.backend.infra.agent.hydration :as agent-hydration]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.openplanner.memory :as openplanner-memory]))

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

(deftest knoxx-tool-suite-includes-sandbox-container-tools
  (testing "signup/basic chat users can receive the ephemeral sandbox tool slice"
    (let [tools (agent-hydration/create-knoxx-custom-tools
                 #js {}
                 {}
                 {:toolPolicies [{:toolId "sandbox_container.create" :effect "allow"}
                                 {:toolId "sandbox_container.status" :effect "allow"}
                                 {:toolId "sandbox_container.exec" :effect "allow"}
                                 {:toolId "sandbox_container.read" :effect "allow"}
                                 {:toolId "sandbox_container.write" :effect "allow"}
                                 {:toolId "sandbox_container.destroy" :effect "allow"}]}
                 #{"sandbox_container.create"
                   "sandbox_container.status"
                   "sandbox_container.exec"
                   "sandbox_container.read"
                   "sandbox_container.write"
                   "sandbox_container.destroy"})
          canonical-ids (set (keep #(or (aget % "originalName")
                                        (eta-mu-extern/tool-runtime-name %))
                                   (eta-mu-extern/tool-seq tools)))]
      (is (contains? canonical-ids "sandbox_container.create"))
      (is (contains? canonical-ids "sandbox_container.exec"))
      (is (contains? canonical-ids "sandbox_container.read"))
      (is (contains? canonical-ids "sandbox_container.write"))
      (is (contains? canonical-ids "sandbox_container.destroy"))
      (is (not (contains? canonical-ids "sandbox_container.commit"))))))

(deftest ^:async passive-memory-hydration-failure-is-non-fatal
  (testing "OpenPlanner outage must not abort an agent turn before tools can run"
    (with-redefs [openplanner-client/enabled? (fn [_] true)
                  openplanner-memory/openplanner-memory-search! (fn [_ _]
                                                                  (js/Promise.reject (js/Error. "OpenPlanner 502")))]
      (let [result (await (agent-hydration/passive-memory-hydration!
                           {:openplanner-base-url "http://openplanner.local"
                            :openplanner-api-key "test"}
                           "conversation-a"
                           "remember prior context"
                           nil
                           {:memory-hydration {:enabled? true :mode :always :k 4}}))]
        (is (nil? result))))))

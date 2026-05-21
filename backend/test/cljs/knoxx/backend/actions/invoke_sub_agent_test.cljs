(ns knoxx.backend.actions.invoke-sub-agent-test
  (:require [cljs.test :refer [async deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.domain.action.invoke-sub-agent]
            [knoxx.backend.domain.action.registry :as registry]
            [knoxx.backend.domain.contracts.loader :as loader]
            [knoxx.backend.law.contracts :as validator]))

;; ── Sub-agent contract validation ──────────────────────────────────────────

(deftest sub-agent-contract-validates
  (testing "A well-formed sub-agent contract passes validation"
    (let [contract {:contract/id "test_sub_agent"
                    :contract/kind :sub-agent
                    :contract/version 1
                    :enabled true
                    :sub-agent/parent-capabilities :restrict
                    :sub-agent/capabilities [:read :semantic_query]
                    :sub-agent/model "gpt-4o"
                    :sub-agent/thinking "medium"
                    :sub-agent/role "researcher"
                    :sub-agent/timeout-ms 30000
                    :sub-agent/mode :await
                    :agent {:role "researcher"
                            :model "gpt-4o"
                            :thinking "medium"}
                    :prompts {:system "You are a test sub-agent."
                              :task "Do test sub-agent things."}
                    :context {:max-messages 10
                              :max-chars 20000
                              :preserve-system true}}
          result (validator/validate "sub_agents" contract)]
      (is (:ok result) (str "Sub-agent contract should validate: "
                            (pr-str (:errors result)))))))

(deftest sub-agent-contract-minimal-validates
  (testing "A minimal sub-agent contract (just id + kind) passes validation"
    (let [contract {:contract/id "minimal_sub"
                    :contract/kind :sub-agent}
          result (validator/validate "sub_agents" contract)]
      (is (:ok result) (str "Minimal sub-agent should validate: "
                            (pr-str (:errors result)))))))

(deftest sub-agent-contract-rejects-wrong-kind
  (testing "A contract with wrong kind is rejected for sub_agents class"
    (let [contract {:contract/id "wrong_kind"
                    :contract/kind :agent}
          result (validator/validate "sub_agents" contract)]
      (is (not (:ok result))
          "Agent contract should fail sub_agents validation"))))

;; ── Contract class normalization ───────────────────────────────────────────

(deftest normalize-sub-agent-class
  (testing "Various sub-agent class strings normalize correctly"
    (is (= "sub_agents" (loader/normalize-contract-class "sub-agent")))
    (is (= "sub_agents" (loader/normalize-contract-class "sub-agents")))
    (is (= "sub_agents" (loader/normalize-contract-class "sub_agent")))
    (is (= "sub_agents" (loader/normalize-contract-class "sub_agents")))))

;; ── Action contract validation ─────────────────────────────────────────────

(deftest invoke-sub-agent-action-kind-accepted
  (testing "invoke/sub-agent is a valid action kind"
    (let [action-kinds [:invoke/agent :invoke/sub-agent :invoke/http
                        :invoke/discord-post :invoke/noop]]
      (doseq [kind action-kinds]
        (is (some? kind) (str "Action kind " kind " should be recognized"))))))

(deftest invoke-sub-agent-loads-contract-and-spawns
  (async done
    (let [calls (atom [])
          ctx {:config {:contracts-dir "test/fixtures/contracts"}
               :event {:sourceKind "unit"
                       :eventKind "unit.event"
                       :payload {:content "Parent event content"}}
               :job {:id "parent-agent"
                     :agentSpec {:model "parent-model"
                                 :role "knowledge_worker"
                                 :thinkingLevel "off"
                                 :toolPolicies [{:toolId "read" :effect "allow"}]}
                     :data {:run-id "parent-run"
                            :conversation-id "parent-conversation"
                            :session-id "parent-session"}}
               :run-agent! (fn [_config job event]
                             (swap! calls conj {:job job :event event})
                             (js/Promise.resolve {:queued true}))}
          action {:action/id "invoke-sub-agent"
                  :action/kind :invoke/sub-agent
                  :action/with {:sub-agent-id "test_sub_agent"}}]
      (.catch
       (.then (registry/run-action! ctx action)
              (fn [result]
                (let [{:keys [job event]} (first @calls)
                      agent-spec (:agentSpec job)
                      content (get-in event [:payload :content])]
                  (is (:ok result))
                  (is (= :await (:mode result)))
                  (is (= "test_sub_agent" (:sub-agent-id result)))
                  (is (= "gpt-4o" (:model agent-spec)))
                  (is (= "researcher" (:role agent-spec)))
                  (is (= "medium" (:thinkingLevel agent-spec)))
                  (is (str/includes? (:systemPrompt agent-spec) "Sub-Agent Context"))
                  (is (str/includes? (:systemPrompt agent-spec) "You are a test sub-agent."))
                  (is (= "Do test sub-agent things." (:taskPrompt agent-spec)))
                  (is (= "test_sub_agent" (get-in job [:data :sub-agent-id])))
                  (is (= "parent-conversation" (:parentConversationId agent-spec)))
                  (is (= "parent-session" (:parentSessionId agent-spec)))
                  (is (= "parent-run" (:parentRunId agent-spec)))
                  (is (str/includes? content "Do test sub-agent things."))
                  (is (str/includes? content "Parent event content"))
                  (done))))
       (fn [err]
         (is false (or (.-stack err) (.-message err) (str err)))
         (done))))))

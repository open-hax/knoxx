(ns knoxx.backend.triggers.control-config-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.triggers.control-config :as control-config]))

(def base-default-job
  {:id "discord_patrol"
   :name "discord_patrol"
   :enabled true
   :trigger {:kind "cron" :cadenceMinutes 5 :eventKinds []}
   :source {:kind "discord" :mode "patrol" :config {:maxMessages 25}}
   :filters {:channels ["alpha"]}
   :agentSpec {:role "knowledge_worker"
               :model "gemma4:31b"
               :thinkingLevel "off"
               :systemPrompt "contract system"
               :taskPrompt "contract task"
               :toolPolicies [{:toolId "discord.read" :effect "allow"}]}
   :description "contract description"
   :contractSourceId "discord_patrol"
   :contractSourceKind "agent"
   :contractSourceKey "agent:discord_patrol"
   :contractHash 222
   :actorId "discord_automation"})

(def base-default-control
  {:sources {:discord {:botUserId ""
                       :defaultChannels ["alpha"]
                       :targetKeywords ["knoxx"]}
             :github {:webhookSecretConfigured false}
             :cron {}}
   :jobs [base-default-job]})

(deftest event-agent-control-config-uses-saved-overrides-when-contract-hash-matches
  (with-redefs [control-config/default-event-agent-control (fn [_] base-default-control)
                control-config/event-agent-role-options (fn [_] ["knowledge_worker" "system_admin"])]
    (let [saved-job (assoc base-default-job
                           :description "saved description"
                           :agentSpec (assoc (:agentSpec base-default-job)
                                             :systemPrompt "saved system"
                                             :taskPrompt "saved task"))
          result (control-config/event-agent-control-config
                  {:knoxx-default-role "knowledge_worker"
                   :event-agent-control {:jobs [saved-job]}})
          job (first (:jobs result))]
      (testing "matching hashes keep persisted overrides"
        (is (= "saved description" (:description job)))
        (is (= "saved system" (get-in job [:agentSpec :systemPrompt])))
        (is (= "saved task" (get-in job [:agentSpec :taskPrompt])))
        (is (= "agent" (:contractSourceKind job)))
        (is (= "agent:discord_patrol" (:contractSourceKey job)))
        (is (= 222 (:contractHash job)))))))

(deftest event-agent-control-config-resets-contract-job-when-hash-drifts
  (with-redefs [control-config/default-event-agent-control (fn [_] base-default-control)
                control-config/event-agent-role-options (fn [_] ["knowledge_worker" "system_admin"])]
    (let [saved-job (assoc base-default-job
                           :contractHash 111
                           :description "stale description"
                           :agentSpec (assoc (:agentSpec base-default-job)
                                             :systemPrompt "stale system"
                                             :taskPrompt "stale task"))
          result (control-config/event-agent-control-config
                  {:knoxx-default-role "knowledge_worker"
                   :event-agent-control {:jobs [saved-job]}})
          job (first (:jobs result))]
      (testing "changed contract hash makes contract file win"
        (is (= "contract description" (:description job)))
        (is (= "contract system" (get-in job [:agentSpec :systemPrompt])))
        (is (= "contract task" (get-in job [:agentSpec :taskPrompt])))
        (is (= "agent" (:contractSourceKind job)))
        (is (= "agent:discord_patrol" (:contractSourceKey job)))
        (is (= 222 (:contractHash job)))))))

(deftest event-agent-control-config-resets-legacy-contract-job-without-hash
  (with-redefs [control-config/default-event-agent-control (fn [_] base-default-control)
                control-config/event-agent-role-options (fn [_] ["knowledge_worker" "system_admin"])]
    (let [legacy-job (-> base-default-job
                         (dissoc :contractHash)
                         (assoc :description "legacy stale description"
                                :agentSpec (assoc (:agentSpec base-default-job)
                                                  :systemPrompt "legacy stale system")))
          result (control-config/event-agent-control-config
                  {:knoxx-default-role "knowledge_worker"
                   :event-agent-control {:jobs [legacy-job]}})
          job (first (:jobs result))]
      (testing "old persisted rows without contract hashes are treated as drifted"
        (is (= "contract description" (:description job)))
        (is (= "contract system" (get-in job [:agentSpec :systemPrompt])))
        (is (= "agent" (:contractSourceKind job)))
        (is (= "agent:discord_patrol" (:contractSourceKey job)))
        (is (= 222 (:contractHash job)))))))

(deftest event-agent-control-config-keeps-custom-jobs
  (with-redefs [control-config/default-event-agent-control (fn [_] base-default-control)
                control-config/event-agent-role-options (fn [_] ["knowledge_worker" "system_admin"])]
    (let [custom-job {:id "custom-job"
                      :name "custom-job"
                      :enabled true
                      :trigger {:kind "event" :cadenceMinutes 5 :eventKinds ["issues.opened"]}
                      :source {:kind "github" :mode "respond" :config {}}
                      :filters {:repositories ["open-hax/openplanner"]}
                      :agentSpec {:role "knowledge_worker"
                                  :model "gemma4:31b"
                                  :thinkingLevel "off"
                                  :systemPrompt "custom system"
                                  :taskPrompt "custom task"
                                  :toolPolicies [{:toolId "websearch" :effect "allow"}]}
                      :description "custom job"}
          result (control-config/event-agent-control-config
                  {:knoxx-default-role "knowledge_worker"
                   :event-agent-control {:jobs [custom-job]}})
          jobs (:jobs result)]
      (testing "non-contract custom jobs still come from persisted control"
        (is (= #{"discord_patrol" "custom-job"}
               (into #{} (map :id) jobs)))
        (is (= "custom system"
               (get-in (some #(when (= "custom-job" (:id %)) %) jobs)
                       [:agentSpec :systemPrompt])))))))

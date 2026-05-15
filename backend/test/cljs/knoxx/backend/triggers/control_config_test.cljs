(ns knoxx.backend.triggers.control-config-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.runtime.contract-loader :as contract-loader]
            [knoxx.backend.tooling :as tooling]
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
               :toolPolicies [{:toolId "discord.read" :effect "allow"}]
               :sources [:source/openplanner-memory]
               :memoryHydration {:enabled? true :provider :openplanner :mode :triggered :k 6}
               :contextPolicy {:max-messages 240 :max-chars 60000}}
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
        (is (= [:source/openplanner-memory] (get-in job [:agentSpec :sources])))
        (is (= {:enabled? true :provider :openplanner :mode :triggered :k 6}
               (get-in job [:agentSpec :memoryHydration])))
        (is (= {:max-messages 240 :max-chars 60000}
               (get-in job [:agentSpec :contextPolicy])))
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

(deftest contract-job-resolves-source-mode-contract-and-cumulative-task-prompt
  (let [source-mode {:contractClass "source_modes"
                     :id "discord-synthesis"
                     :contract {:contract/id "discord-synthesis"
                                :contract/kind :source-mode
                                :source-mode/id :source-mode/discord-synthesis
                                :source/kind :discord
                                :source/mode :template-synthesize
                                :prompts {:task "source mode task"}
                                :data {:trusted-role-ids ["1494109844737753220"]}}}
        contract {:contract/id "synth-agent"
                  :contract/kind :agent
                  :enabled true
                  :trigger-kind :cron
                  :source-kind :discord
                  :source-mode :source-mode/discord-synthesis
                  :agent {:role :role/test}
                  :prompts {:task "agent task"}
                  :data {:filters {:channels ["explicit-channel"]}
                         :source {:max-messages 9
                                  :max-total-messages 30
                                  :max-message-chars 900}}}]
    (with-redefs [contract-loader/load-all-contracts-sync (fn [_] [source-mode])
                  tooling/resolve-agent-contract (fn
                                                   ([_ _]
                                                    {:role "test"
                                                     :system-prompt "system"
                                                     :task-prompt "resolved role+agent task"
                                                     :tool-policies []})
                                                   ([_ _ _]
                                                    {:role "test"
                                                     :system-prompt "system"
                                                     :task-prompt "resolved role+agent task"
                                                     :tool-policies []}))]
      (let [job (#'control-config/contract->event-agent-job {} "synth-agent" contract 123)
            task (get-in job [:agentSpec :taskPrompt])]
        (is (= "discord" (get-in job [:source :kind])))
        (is (= "template-synthesize" (get-in job [:source :mode])))
        (is (= 9 (get-in job [:source :config :maxMessages])))
        (is (= 30 (get-in job [:source :config :maxTotalMessages])))
        (is (= 900 (get-in job [:source :config :maxMessageChars])))
        (is (= ["1494109844737753220"] (get-in job [:source :config :trustedRoleIds])))
        (is (= ["explicit-channel"] (get-in job [:filters :channels])))
        (is (sequential? task))
        (is (= ["source mode task" "resolved role+agent task"] (nth task 2)))))))

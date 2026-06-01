(ns knoxx.backend.agents.runner-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [knoxx.backend.extern.js :as xjs]
            [knoxx.backend.infra.agent.runner :as runner]))

(deftest direct-start-payload->turn-params-normalizes-direct-start-shape
  (testing "snake_case direct-start payload becomes send-agent-turn! params"
    (is (= {:conversation-id "conversation-1"
            :session-id "session-1"
            :run-id "run-1"
            :message "hello"
            :content-parts [{:type "image" :url "https://example.com/demo.png"}]
            :model "gemma4:31b"
            :mode "direct"
            :agent-spec {:role "knowledge_worker"
                         :system-prompt "sys"}}
           (runner/direct-start-payload->turn-params
            {:conversation_id "conversation-1"
             :session_id "session-1"
             :run_id "run-1"
             :message "hello"
             :content_parts [{:type "image" :url "https://example.com/demo.png"}]
             :model "gemma4:31b"
             :agent_spec {:role "knowledge_worker"
                          :system_prompt "sys"}})))))

(deftest direct-start-payload->turn-params-supports-js-payloads
  (testing "JS direct-start payloads normalize through the runner extern boundary"
    (is (= {:conversation-id "conversation-js"
            :session-id "session-js"
            :run-id "run-js"
            :message "hello from js"
            :content-parts []
            :model "model-js"
            :mode "direct"
            :agent-spec {:role "developer"
                         :tool-policies [{:toolId "discord.read" :effect "allow"}]}}
           (runner/direct-start-payload->turn-params
            (xjs/object
             {:conversation_id "conversation-js"
              :session_id "session-js"
              :run_id "run-js"
              :message "hello from js"
              :model "model-js"
              :agent_spec {:role "developer"
                           :tool_policies [{:toolId "discord.read" :effect "allow"}]}}))))))

(deftest direct-start-payload->turn-params-supports-kebab-keys-too
  (testing "existing CLJ payloads also normalize"
    (is (= {:conversation-id "conversation-2"
            :session-id "session-2"
            :run-id "run-2"
            :message "hi"
            :content-parts []
            :model nil
            :mode "direct"
            :agent-spec {:role "developer"}}
           (runner/direct-start-payload->turn-params
            {:conversation-id "conversation-2"
             :session-id "session-2"
             :run-id "run-2"
             :message "hi"
             :agent-spec {:role "developer"}})))))

(deftest direct-start-payload->turn-params-normalizes-contract-and-actor-ids
  (testing "snake_case agent_spec preserves contract and actor identity"
    (is (= {:contract-id "discord_mention_response"
            :actor-id "discord_automation"
            :role "knowledge_worker"
            :thinking-level "medium"
            :tool-policies [{:toolId "discord.read" :effect "allow"}]}
           (:agent-spec
            (runner/direct-start-payload->turn-params
             {:message "hi"
              :agent_spec {:contract_id "discord_mention_response"
                           :actor_id "discord_automation"
                           :role "knowledge_worker"
                           :thinking_level "medium"
                           :tool_policies [{:toolId "discord.read" :effect "allow"}]}}))))))

(deftest direct-start-payload->turn-params-normalizes-trigger-audit-metadata
  (testing "triggered agent runs preserve audit metadata through the runner boundary"
    (is (= {:contract-id "ussyverse_social_creative"
            :actor-id "discord_automation"
            :trigger-id "ussyverse_social_creative_cron"
            :event-type "schedule/ussyverse-social-creative"
            :event-types ["schedule/ussyverse-social-creative"]
            :event-id "evt-1"
            :event-scope-id "ussyverse_social_creative"
            :schedule-id "ussyverse_social_creative"}
           (:agent-spec
            (runner/direct-start-payload->turn-params
             {:message "hi"
              :agent_spec {:contract_id "ussyverse_social_creative"
                           :actor_id "discord_automation"
                           :trigger_id "ussyverse_social_creative_cron"
                           :event_type "schedule/ussyverse-social-creative"
                           :event_types ["schedule/ussyverse-social-creative"]
                           :event_id "evt-1"
                           :event_scope_id "ussyverse_social_creative"
                           :schedule_id "ussyverse_social_creative"}}))))))

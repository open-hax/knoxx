(ns knoxx.backend.event-agents-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.event-agents :as ea]))

;; -----------------------------------------------------------------------------
;; Target 1: Event Matching Logic
;; (event-agents.cljs -> job-matches-event?)
;; -----------------------------------------------------------------------------

(deftest test-job-matches-event
  (let [control {:sources {:discord {:defaultChannels ["123"]}
                      :github {}}
                 :jobs [{:id "job-1"
                        :enabled true
                        :trigger {:kind "event" :eventKinds ["discord.message.mention" "discord.message.created"]}
                        :source {:kind "discord"}
                        :filters {:channels ["123"] :keywords ["urgent"]}
                        :agentSpec {}}]}
        job (first (:jobs control))
        
        mention-event {:sourceKind "discord"
                       :eventKind "discord.message.mention"
                       :payload {:channelId "123" :content "Hello bot!"}}
        
        keyword-event {:sourceKind "discord"
                      :eventKind "discord.message.created"
                      :payload {:channelId "123" :content "This is urgent!"}}
        
        wrong-channel-event {:sourceKind "discord"
                             :eventKind "discord.message.mention"
                             :payload {:channelId "456" :content "Hello bot!"}}
        
        wrong-kind-event {:sourceKind "github"
                          :eventKind "github.issue.created"
                          :payload {:repository "open-hax/openplanner"}}]

    (testing "Mention events should match regardless of keywords"
      (is (ea/job-matches-event? control job mention-event)))

    (testing "Keyword events should match if keywords are present"
      (is (ea/job-matches-event? control job keyword-event)))

    (testing "Events in wrong channels should not match"
      (is (not (ea/job-matches-event? control job wrong-channel-event))))

    (testing "Events from wrong source should not match"
      (is (not (ea/job-matches-event? control job wrong-kind-event))))))

;; -----------------------------------------------------------------------------
;; Target 2: Event Summary Generation
;; (event-agents.cljs -> event-summary-text)
;; -----------------------------------------------------------------------------

(deftest test-event-summary-text
  (let [event {:sourceKind "discord"
               :eventKind "discord.message.created"
               :id "evt-123"
               :timestamp "2026-04-19T10:00:00Z"
               :payload {:channelId "123"
                        :authorUsername "Alice"
                        :content "Hello world!"}}]
    
    (testing "The summary should contain key event details"
      (let [summary (ea/event-summary-text event)]
        (is (.includes summary "Event source: discord"))
        (is (.includes summary "Event kind: discord.message.created"))
        (is (.includes summary "Author: Alice"))
        (is (.includes summary "Content: Hello world!"))))))

;; -----------------------------------------------------------------------------
;; Target 3: Agent Run Payload Construction
;; (event-agents.cljs -> start-agent-run!)
;; -----------------------------------------------------------------------------

(deftest test-build-agent-run-payload
  (let [config {:knoxx-base-url "http://localhost"
                :proxx-default-model "glm-5"}
        job {:id "job-1"
             :agentSpec {:role "executive"
                        :systemPrompt "You are a robot."
                        :model "glm-5"
                        :thinkingLevel "off"
                        :toolPolicies [{:toolId "discord.send" :effect "allow"}]}}
        event {:sourceKind "discord"
              :eventKind "discord.message.mention"
              :payload {:content "Hi"}}]
    
    (testing "The payload should be correctly mapped for the direct/start API"
      (let [payload (ea/build-agent-run-payload config job event)]
        (is (= (:model payload) "glm-5"))
        (is (= (get-in payload [:agent_spec :role]) "executive"))
        (is (= (get-in payload [:agent_spec :system_prompt]) "You are a robot."))
        (is (= (js->clj (get-in payload [:agent_spec :tool_policies]) :keywordize-keys true)
               [{:toolId "discord.send" :effect "allow"}]))))))

;; -----------------------------------------------------------------------------
;; Target 4: Discord Message Normalization
;; (event-agents.cljs -> map-discord-message)
;; -----------------------------------------------------------------------------

(deftest test-map-discord-message
  (let [raw-msg #js {"id" "msg-1" 
                     "channel_id" "chan-1" 
                     "content" "Hello!" 
                     "author" #js {"id" "u-1" "username" "Alice" "bot" false}
                     "timestamp" "2026-04-19T10:00:00Z"}]
    
    (testing "Raw JS Discord message should map to Clojure keyword map"
      (let [mapped (ea/map-discord-message raw-msg)]
        (is (= (:id mapped) "msg-1"))
        (is (= (:channelId mapped) "chan-1"))
        (is (= (:content mapped) "Hello!"))
        (is (= (:authorUsername mapped) "Alice"))
        (is (false? (:authorIsBot mapped)))))))

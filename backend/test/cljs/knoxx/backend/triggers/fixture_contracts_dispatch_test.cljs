(ns knoxx.backend.triggers.fixture-contracts-dispatch-test
  "End-to-end test: load fixture trigger/source contracts from disk and verify
   the full dispatch pipeline matches Discord events. Uses test/fixtures so the
   live contracts/ folder stays free for operators to edit."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.condition.builtin :as condition-builtins]
            [knoxx.backend.domain.condition.registry :as condition-registry]
            [knoxx.backend.domain.driver.builtin :as driver-builtin]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.event.normalize :as event-normalize]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.domain.source.runtime :as source-runtime]
            [knoxx.backend.domain.trigger.normalize :as trigger-normalize]))

(def fixture-config
  {:contracts-dir "test/fixtures/trigger-contracts"})

(deftest fixture-trigger-resource-exists-and-is-valid
  (testing "the ussyverse trigger fixture is discoverable on disk"
    (let [trigger (resources/resource-sync fixture-config :trigger "ussyverse_social_replies_event")]
      (is (some? trigger) "Trigger resource should be found on disk")
      (when trigger
        (is (= :trigger (:contract/kind trigger)))
        (is (= "ussyverse_social_replies_event" (:contract/id trigger)))
        (is (true? (:enabled trigger)))
        (is (= :event (:trigger/kind trigger)))
        (is (= [:discord.message] (:trigger/events trigger)))
        (is (= "discord_automation" (:trigger/actor trigger)))
        (is (= :actions/start-agent-session (:trigger/action trigger)))
        (is (= "ussyverse_social_replies" (:trigger/agent trigger)))
        (is (some? (:trigger/condition trigger)) "Trigger should have a condition")))))

(deftest fixture-source-resource-exists-and-is-valid
  (testing "the discord gateway source fixture is discoverable on disk"
    (let [source (resources/resource-sync fixture-config :source "discord_gateway")]
      (is (some? source) "Source resource should be found on disk")
      (when source
        (is (= :source (:contract/kind source)))
        (is (= "discord_gateway" (:contract/id source)))
        (is (true? (:enabled source)))
        (is (= :event-generator (:source/type source)))
        (is (= :driver/discord (:source/driver source)))
        (is (= "discord_automation" (:source/actor source)))
        (is (vector? (:source/listens source)))
        (is (some #{:discord.message} (:source/listens source)))))))

(deftest normalized-trigger-has-correct-runtime-shape
  (testing "the fixture trigger folds legacy arguments into :trigger/with"
    (let [trigger (resources/resource-sync fixture-config :trigger "ussyverse_social_replies_event")
          normalized (trigger-normalize/normalize-trigger trigger)]
      (is (= "ussyverse_social_replies_event" (:trigger/id normalized)))
      (is (= :event (:trigger/kind normalized)))
      (is (true? (:trigger/enabled? normalized)))
      (is (= ["discord_automation"] (mapv str [(:trigger/actor normalized)])))
      (is (= ["discord_automation"] (mapv str [(:trigger/emitter normalized)])))
      (is (= ["discord_automation"] (mapv str [(:trigger/listener normalized)])))
      (is (= [:discord.message] (:trigger/events normalized)))
      (is (= :actions/start-agent-session (:trigger/action normalized)))
      (is (= "ussyverse_social_replies" (get-in normalized [:trigger/with :agent-id])))
      (is (string? (get-in normalized [:trigger/with :task])))
      (is (nil? (:trigger/agent normalized)))
      (is (some? (:trigger/condition normalized)) "Normalized trigger should preserve condition"))))

(deftest trigger-matches-keyword-event-from-disk
  (testing "a Discord keyword event matches the fixture trigger loaded from disk"
    (condition-builtins/register-builtins!)
    (let [trigger (resources/resource-sync fixture-config :trigger "ussyverse_social_replies_event")
          normalized (trigger-normalize/normalize-trigger trigger)
          event (event-normalize/normalize-event
                 {:event/type :discord.message
                  :event/actor "discord_automation"
                  :event/payload {:content "hey frankie what's up"
                                  :gatewayBotUserId "12345"
                                  :gatewayActorId "discord_automation"
                                  :channelId "123"
                                  :id "msg-1"}})]
      (is (true? (condition-registry/evaluate (:trigger/condition normalized) event nil normalized nil))
          "Condition should match keyword"))))

(deftest trigger-matches-mention-event-from-disk
  (testing "a Discord mention event matches the fixture trigger loaded from disk"
    (condition-builtins/register-builtins!)
    (let [trigger (resources/resource-sync fixture-config :trigger "ussyverse_social_replies_event")
          normalized (trigger-normalize/normalize-trigger trigger)
          event (event-normalize/normalize-event
                 {:event/type :discord.message
                  :event/actor "discord_automation"
                  :event/payload {:content "hello <@12345>"
                                  :gatewayBotUserId "12345"
                                  :gatewayActorId "discord_automation"
                                  :channelId "123"
                                  :id "msg-2"}})]
      (is (true? (condition-registry/evaluate (:trigger/condition normalized) event nil normalized nil))
          "Condition should match mention"))))

(deftest trigger-rejects-unrelated-event-from-disk
  (testing "an unrelated Discord message does not match the trigger"
    (condition-builtins/register-builtins!)
    (let [trigger (resources/resource-sync fixture-config :trigger "ussyverse_social_replies_event")
          normalized (trigger-normalize/normalize-trigger trigger)
          event (event-normalize/normalize-event
                 {:event/type :discord.message
                  :event/actor "discord_automation"
                  :event/payload {:content "hello world"
                                  :gatewayBotUserId "12345"
                                  :gatewayActorId "discord_automation"
                                  :channelId "123"
                                  :id "msg-3"}})]
      (is (false? (condition-registry/evaluate (:trigger/condition normalized) event nil normalized nil))
          "Condition should not match unrelated message"))))

(deftest ^:async full-dispatch-pipeline-with-fixture-contracts
  (testing "the full source dispatch pipeline matches trigger, then action errors visibly"
    (driver-builtin/register-built-in-drivers!)
    (condition-builtins/register-builtins!)
    (event-dispatch/reset-dedup!)
    (try
      (await (source-runtime/dispatch-driver-event!
              fixture-config
              :driver/discord
              "discord_automation"
              {:event/type :discord.message
               :event/payload {:content "hey frankie what's up"
                               :gatewayBotUserId "12345"
                               :gatewayActorId "discord_automation"
                               :channelId "123"
                               :id "msg-4"}}))
      (is false "Should have thrown runtime unavailable error")
      (catch :default err
        (is (re-find #"runtime unavailable" (str (.-message err)))
            "Error should be visible and mention runtime")))))

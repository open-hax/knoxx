(ns knoxx.backend.triggers.production-scenario-test
  "Verify gateway-shaped Discord messages through source and trigger dispatch, with captured action effects."
  (:require [cljs.test :refer [deftest is testing use-fixtures]]
            [knoxx.backend.domain.condition.builtin :as condition-builtins]
            [knoxx.backend.domain.driver.builtin :as driver-builtin]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.source.runtime :as source-runtime]
            [knoxx.backend.domain.trigger.runtime :as trigger-runtime]
            [knoxx.backend.triggers.action-fixture :as action-fixture]))

(def action-calls (atom []))
(use-fixtures :each (action-fixture/recording-fixture action-calls))

(def fixture-config
  {:contracts-dir "test/fixtures/trigger-contracts"})

(deftest trigger-runtime-sees-trigger
  (testing "trigger runtime can list the ussyverse trigger"
    (let [status (trigger-runtime/status fixture-config)
          trigger-ids (->> status :triggers (map :contract/id) set)]
      (is (contains? trigger-ids "ussyverse_social_replies_event")
          (str "Trigger should be visible in status. Got triggers: " (pr-str trigger-ids))))))

(deftest source-runtime-sees-discord-source
  (testing "source runtime can find the discord gateway source"
    (driver-builtin/register-built-in-drivers!)
    (let [source (source-runtime/matching-source fixture-config :driver/discord "discord_automation" :discord.message)]
      (is (some? source)
          "Discord source should be found for discord_automation actor"))))

(deftest ^:async exact-production-message-flow
  (testing "simulating the exact message flow from discord gateway through to dispatch"
    (driver-builtin/register-built-in-drivers!)
    (condition-builtins/register-builtins!)
    (event-dispatch/reset-dedup!)
    ;; This is the exact payload shape produced by discord.gateway/map-message
    ;; plus the gatewayActorId/gatewayBotUserId injected by discord.source/bind-gateways!
    (let [msg {:id "msg-real-1"
               :channelId "1494137016303095828"
               :guildId "some-guild"
               :content "hey @ussyverse what do you think about this beat?"
               :authorId "user-123"
               :authorUsername "testuser"
               :authorIsBot false
               :authorRoleIds []
               :timestamp "2026-06-03T20:00:00.000Z"
               :attachments []
               :embeds []
               ;; These are injected by bind-gateways!
               :gatewayActorId "discord_automation"
               :gatewayBotUserId "bot-123"}
          ;; This is the exact event shape built by core.cljs on-message! handler
          event {:event/type :discord.message
                 :event/payload msg}
          result (await (source-runtime/dispatch-driver-event!
                          fixture-config :driver/discord "discord_automation" event))]
        (is (= ["ussyverse_social_replies_event"] (:matchedTriggers result)))
        (is (= 1 (count @action-calls)))
        (is (= msg (get-in @action-calls [0 :ctx :event :event/payload])))
        (is (= "ussyverse_social_replies" (get-in @action-calls [0 :action :action/with :agent-id]))))))

(deftest ^:async keyword-only-message-flow
  (testing "a keyword-only message (no mention) still dispatches through the fixture trigger"
    (driver-builtin/register-built-in-drivers!)
    (condition-builtins/register-builtins!)
    (event-dispatch/reset-dedup!)
    (let [msg {:id "msg-real-2"
               :channelId "1494137016303095828"
               :content "hey frankie"
               :gatewayActorId "discord_automation"
               :gatewayBotUserId "bot-123"}
          event {:event/type :discord.message
                 :event/payload msg}
          result (await (source-runtime/dispatch-driver-event!
                          fixture-config :driver/discord "discord_automation" event))]
        (is (= ["ussyverse_social_replies_event"] (:matchedTriggers result)))
        (is (= 1 (count @action-calls)))
        (is (= msg (get-in @action-calls [0 :ctx :event :event/payload])))
        (is (= "ussyverse_social_replies" (get-in @action-calls [0 :action :action/with :agent-id]))))))

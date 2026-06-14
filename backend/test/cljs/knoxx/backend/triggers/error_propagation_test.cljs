(ns knoxx.backend.triggers.error-propagation-test
  "Test that errors in the trigger pipeline are visible, not swallowed.
   Uses test/fixtures snapshots — the live contracts/ folder is operator-owned."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.condition.builtin :as condition-builtins]
            [knoxx.backend.domain.driver.builtin :as driver-builtin]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.domain.source.runtime :as source-runtime]))

(def fixture-config
  {:contracts-dir "test/fixtures/trigger-contracts"})

(deftest ^:async action-error-is-visible
  (testing "when an action throws, the error propagates instead of being swallowed"
    (driver-builtin/register-built-in-drivers!)
    (condition-builtins/register-builtins!)
    (event-dispatch/reset-dedup!)
    (with-redefs [resources/load-all-resources-sync
                  (fn [_]
                    [(resources/resource-record-sync fixture-config :trigger "ussyverse_social_replies_event")])]
      (try
        (await (event-dispatch/dispatch!
                fixture-config
                {:event/type :discord.message
                 :event/actor "discord_automation"
                 :event/payload {:content "hey frankie"
                                 :gatewayBotUserId "12345"
                                 :gatewayActorId "discord_automation"
                                 :channelId "123"}}))
        (is false "Should have thrown an error")
        (catch :default err
          (is (some? err) "Error should be thrown"))))))

(deftest ^:async source-dispatch-error-is-visible
  (testing "when source dispatch fails, the error is not silently swallowed"
    (driver-builtin/register-built-in-drivers!)
    (condition-builtins/register-builtins!)
    (event-dispatch/reset-dedup!)
    (try
      (await (source-runtime/dispatch-driver-event!
              fixture-config
              :driver/discord
              "discord_automation"
              {:event/type :discord.message
               :event/payload {:content "hey frankie"
                               :gatewayBotUserId "12345"
                               :gatewayActorId "discord_automation"
                               :channelId "123"}}))
      (is false "Should have thrown an error")
      (catch :default err
        (is (some? err) "Error should be thrown")))))

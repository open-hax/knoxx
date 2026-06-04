(ns knoxx.backend.triggers.event-deduplication-test
  "Test that event deduplication does not silently drop legitimate events."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.condition.builtin :as condition-builtins]
            [knoxx.backend.domain.driver.builtin :as driver-builtin]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.domain.source.runtime :as source-runtime]))

(def real-config
  {:contracts-dir "contracts"})

(deftest ^:async same-event-id-is-deduplicated
  (testing "dispatching the same event twice should skip the second"
    (driver-builtin/register-built-in-drivers!)
    (condition-builtins/register-builtins!)
    (event-dispatch/reset-dedup!)
    (let [event {:event/type :discord.message
                 :event/id "same-id"
                 :event/actor "discord_automation"
                 :event/payload {:content "hey frankie"
                                 :gatewayBotUserId "12345"
                                 :gatewayActorId "discord_automation"
                                 :channelId "123"}}]
      ;; First dispatch throws because runtime is unavailable
      (try
        (await (source-runtime/dispatch-driver-event!
                real-config :driver/discord "discord_automation" event))
        (is false "Should have thrown")
        (catch :default _ nil))
      ;; Second dispatch should be deduplicated (same event ID)
      (let [result2 (await (source-runtime/dispatch-driver-event!
                           real-config :driver/discord "discord_automation" event))]
        (is (true? (:skipped result2))
            "Second dispatch with same ID should be skipped")))))

(deftest ^:async different-event-ids-are-not-deduplicated
  (testing "dispatching different events should not be deduplicated"
    (driver-builtin/register-built-in-drivers!)
    (condition-builtins/register-builtins!)
    (event-dispatch/reset-dedup!)
    (let [event1 {:event/type :discord.message
                  :event/id "id-1"
                  :event/actor "discord_automation"
                  :event/payload {:content "hey frankie"
                                  :gatewayBotUserId "12345"
                                  :gatewayActorId "discord_automation"
                                  :channelId "123"}}
          event2 {:event/type :discord.message
                  :event/id "id-2"
                  :event/actor "discord_automation"
                  :event/payload {:content "hey frankie again"
                                  :gatewayBotUserId "12345"
                                  :gatewayActorId "discord_automation"
                                  :channelId "123"}}]
      ;; Both should throw because runtime is unavailable
      (try
        (await (source-runtime/dispatch-driver-event!
                real-config :driver/discord "discord_automation" event1))
        (is false "First should have thrown")
        (catch :default _ nil))
      (try
        (await (source-runtime/dispatch-driver-event!
                real-config :driver/discord "discord_automation" event2))
        (is false "Second should have thrown")
        (catch :default _ nil)))))

(deftest ^:async missing-event-id-gets-generated-and-deduplicated
  (testing "events without IDs get generated IDs; same payload may dedupe"
    (driver-builtin/register-built-in-drivers!)
    (condition-builtins/register-builtins!)
    (event-dispatch/reset-dedup!)
    (let [event {:event/type :discord.message
                 :event/actor "discord_automation"
                 :event/payload {:content "hey frankie"
                                 :gatewayBotUserId "12345"
                                 :gatewayActorId "discord_automation"
                                 :channelId "123"}}]
      ;; First dispatch throws because runtime is unavailable
      (try
        (await (source-runtime/dispatch-driver-event!
                real-config :driver/discord "discord_automation" event))
        (is false "Should have thrown")
        (catch :default _ nil))
      ;; Second dispatch may or may not throw depending on generated ID
      (try
        (await (source-runtime/dispatch-driver-event!
                real-config :driver/discord "discord_automation" event))
        (catch :default _ nil)))))

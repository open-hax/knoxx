(ns knoxx.backend.triggers.event-deduplication-test
  "Test that event deduplication does not silently drop legitimate events."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.condition.builtin :as condition-builtins]
            [knoxx.backend.domain.driver.builtin :as driver-builtin]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.domain.source.runtime :as source-runtime]))

(def fixture-config
  {:contracts-dir "test/fixtures/trigger-contracts"})

(defn- trigger-record
  [action]
  {:resource/id "retryable-event"
   :resource/kind :trigger
   :resource/class "triggers"
   :resource/definition
   {:contract/id "retryable-event"
    :trigger/kind :event
    :trigger/events [:test/retryable]
    :trigger/action :test/retryable-action
    :action/fn action
    :enabled true}})

(defn- retryable-event
  []
  {:event/type :test/retryable
   :event/id "same-id"
   :event/payload {:content "one deterministic event"}})

(deftest ^:async failed-event-id-is-released-but-a-success-remains-deduplicated
  (event-dispatch/reset-dedup!)
  (let [attempts (atom 0)]
    (with-redefs [resources/load-all-resources-sync
                  (fn [_]
                    [(trigger-record
                      (fn [_ctx _action]
                        (if (= 1 (swap! attempts inc))
                          (js/Promise.reject
                           (js/Error. "transient action failure"))
                          (js/Promise.resolve {:ok true}))))])]
      (testing "the failed owner releases the id for a real retry"
        (try
          (await (event-dispatch/dispatch! fixture-config (retryable-event)))
          (is false "the first action should fail")
          (catch :default err
            (is (= "transient action failure" (ex-message err)))))
        (let [retry (await (event-dispatch/dispatch!
                            fixture-config (retryable-event)))]
          (is (= ["retryable-event"] (:matchedTriggers retry)))
          (is (= 2 @attempts))))

      (testing "the equal event is deduplicated after successful completion"
        (let [duplicate (await (event-dispatch/dispatch!
                                fixture-config (retryable-event)))]
          (is (true? (:skipped duplicate)))
          (is (= 2 @attempts)))))))

(deftest ^:async concurrent-equal-successful-events-have-one-owner
  (event-dispatch/reset-dedup!)
  (let [attempts (atom 0)
        release! (atom nil)]
    (with-redefs [resources/load-all-resources-sync
                  (fn [_]
                    [(trigger-record
                      (fn [_ctx _action]
                        (swap! attempts inc)
                        (js/Promise. (fn [resolve _reject]
                                       (reset! release! resolve)))))])]
      (let [owner (event-dispatch/dispatch! fixture-config (retryable-event))
            duplicate (await (event-dispatch/dispatch!
                              fixture-config (retryable-event)))]
        (is (true? (:skipped duplicate)))
        (is (= 1 @attempts))
        (@release! {:ok true})
        (is (= ["retryable-event"]
               (:matchedTriggers (await owner))))
        (is (true? (:skipped (await (event-dispatch/dispatch!
                                     fixture-config (retryable-event))))))
        (is (= 1 @attempts))))))

(deftest ^:async an-unmatched-event-can-be-retried-after-a-trigger-is-enabled
  (event-dispatch/reset-dedup!)
  (let [trigger-enabled? (atom false)
        attempts (atom 0)]
    (with-redefs [resources/load-all-resources-sync
                  (fn [_]
                    (if @trigger-enabled?
                      [(trigger-record
                        (fn [_ctx _action]
                          (swap! attempts inc)
                          (js/Promise.resolve {:ok true})))]
                      []))]
      (is (empty? (:matchedTriggers
                   (await (event-dispatch/dispatch!
                           fixture-config (retryable-event))))))
      (reset! trigger-enabled? true)
      (let [retry (await (event-dispatch/dispatch!
                          fixture-config (retryable-event)))]
        (is (= ["retryable-event"] (:matchedTriggers retry)))
        (is (= 1 @attempts))))))

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
                fixture-config :driver/discord "discord_automation" event1))
        (is false "First should have thrown")
        (catch :default _ nil))
      (try
        (await (source-runtime/dispatch-driver-event!
                fixture-config :driver/discord "discord_automation" event2))
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
                fixture-config :driver/discord "discord_automation" event))
        (is false "Should have thrown")
        (catch :default _ nil))
      ;; Second dispatch may or may not throw depending on generated ID
      (try
        (await (source-runtime/dispatch-driver-event!
                fixture-config :driver/discord "discord_automation" event))
        (catch :default _ nil)))))

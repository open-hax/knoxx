(ns knoxx.backend.triggers.event-deduplication-test
  "Test that event deduplication does not silently drop legitimate events."
  (:require [cljs.test :refer [deftest is testing use-fixtures]]
            [knoxx.backend.domain.condition.builtin :as condition-builtins]
            [knoxx.backend.domain.driver.builtin :as driver-builtin]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.domain.source.runtime :as source-runtime]
            [knoxx.backend.triggers.action-fixture :as action-fixture]))

(def action-calls (atom []))
(use-fixtures :each (action-fixture/recording-fixture action-calls))

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
          (is (= :completed (:dedup/status retry)))
          (is (= 2 @attempts))))

      (testing "the equal event is deduplicated after successful completion"
        (let [duplicate (await (event-dispatch/dispatch!
                                fixture-config (retryable-event)))]
          (is (true? (:skipped duplicate)))
          (is (= :completed (:dedup/status duplicate)))
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
        (is (= :in-flight (:dedup/status duplicate)))
        (is (= 1 @attempts))
        (@release! {:ok true})
        (is (= ["retryable-event"]
               (:matchedTriggers (await owner))))
        (let [completed-duplicate
              (await (event-dispatch/dispatch!
                      fixture-config (retryable-event)))]
          (is (true? (:skipped completed-duplicate)))
          (is (= :completed (:dedup/status completed-duplicate))))
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

(defn- discord-event
  [id content]
  {:event/type :discord.message :event/id id :event/actor "discord_automation"
   :event/payload {:content content :gatewayBotUserId "12345"
                   :gatewayActorId "discord_automation" :channelId "123"}})

(deftest ^:async different-event-ids-are-not-deduplicated
  (testing "distinct ids each reach the action, even through the same source"
    (driver-builtin/register-built-in-drivers!)
    (condition-builtins/register-builtins!)
    (event-dispatch/reset-dedup!)
    (doseq [event [(discord-event "id-1" "hey frankie")
                   (discord-event "id-2" "hey frankie again")]]
      (let [result (await (source-runtime/dispatch-driver-event!
                           fixture-config :driver/discord "discord_automation" event))]
        (is (= ["ussyverse_social_replies_event"] (:matchedTriggers result)))
        (is (= :completed (:dedup/status result)))))
    (is (= ["id-1" "id-2"] (mapv #(get-in % [:ctx :event :event/id]) @action-calls)))))

(deftest ^:async missing-event-id-is-generated-and-can-be-replayed
  (testing "a generated id can be retained for an exact retry without duplicating effects"
    (driver-builtin/register-built-in-drivers!)
    (condition-builtins/register-builtins!)
    (event-dispatch/reset-dedup!)
    (let [event (discord-event nil "hey frankie")
          result (await (source-runtime/dispatch-driver-event!
                          fixture-config :driver/discord "discord_automation" event))
          generated-id (get-in result [:event :event/id])
          replay (await (source-runtime/dispatch-driver-event!
                          fixture-config :driver/discord "discord_automation"
                          (assoc event :event/id generated-id)))]
      (is (seq generated-id))
      (is (= ["ussyverse_social_replies_event"] (:matchedTriggers result)))
      (is (true? (:skipped replay)))
      (is (= :completed (:dedup/status replay)))
      (is (= 1 (count @action-calls))))))

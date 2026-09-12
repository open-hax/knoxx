(ns knoxx.backend.triggers.error-propagation-test
  "Test that errors in the trigger pipeline are visible, not swallowed.
   Uses test/fixtures snapshots — the live contracts/ folder is operator-owned."
  (:require [cljs.test :refer [deftest is testing use-fixtures]]
            [knoxx.backend.domain.condition.builtin :as condition-builtins]
            [knoxx.backend.domain.driver.builtin :as driver-builtin]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.domain.source.runtime :as source-runtime]
            [knoxx.backend.triggers.action-fixture :as action-fixture]))

(def action-calls (atom []))
(def action-failure (ex-info "Deliberate trigger fixture rejection" {:fixture/error :action-failed}))
(use-fixtures :each
  (action-fixture/recording-fixture action-calls
    (fn [_ctx _action] (js/Promise.reject action-failure))))

(def fixture-config
  {:contracts-dir "test/fixtures/trigger-contracts"})

(def event
  {:event/type :discord.message
   :event/actor "discord_automation"
   :event/payload {:content "hey frankie" :gatewayBotUserId "12345"
                   :gatewayActorId "discord_automation" :channelId "123"}})

(deftest ^:async action-error-is-visible
  (testing "the exact deliberate action rejection survives event dispatch"
    (driver-builtin/register-built-in-drivers!)
    (condition-builtins/register-builtins!)
    (event-dispatch/reset-dedup!)
    (let [trigger (resources/resource-record-sync fixture-config :trigger "ussyverse_social_replies_event")]
      (is (some? trigger))
      (with-redefs [resources/load-all-resources-sync (fn [_] [trigger])]
        (let [caught (try (await (event-dispatch/dispatch! fixture-config event)) nil
                          (catch :default err err))]
          (is (identical? action-failure caught) "An unrelated loader/runtime failure must not satisfy this test")
          (is (= 1 (count @action-calls))))))))

(deftest ^:async source-dispatch-error-is-visible
  (testing "the exact deliberate action rejection survives source dispatch"
    (driver-builtin/register-built-in-drivers!)
    (condition-builtins/register-builtins!)
    (event-dispatch/reset-dedup!)
    (let [caught (try (await (source-runtime/dispatch-driver-event!
                              fixture-config :driver/discord "discord_automation" event)) nil
                      (catch :default err err))]
      (is (identical? action-failure caught))
      (is (= 1 (count @action-calls))))))

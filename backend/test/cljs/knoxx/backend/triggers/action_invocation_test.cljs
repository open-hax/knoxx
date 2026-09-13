(ns knoxx.backend.triggers.action-invocation-test
  "Verify that when a trigger matches, the action is actually invoked."
  (:require [cljs.test :refer [deftest is testing use-fixtures]]
            [knoxx.backend.domain.condition.builtin :as condition-builtins]
            [knoxx.backend.domain.driver.builtin :as driver-builtin]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.triggers.action-fixture :as action-fixture]))

(def action-calls (atom []))
(use-fixtures :each (action-fixture/recording-fixture action-calls))

(def fixture-config
  {:contracts-dir "test/fixtures/trigger-contracts"})

(deftest ^:async action-is-invoked-when-trigger-matches
  (testing "the real trigger dispatch invokes the expected action exactly once"
    (driver-builtin/register-built-in-drivers!)
    (condition-builtins/register-builtins!)
    (event-dispatch/reset-dedup!)
    ;; Resource lookup itself reads the inventory, so finish it before narrowing that reader.
    (let [trigger (resources/resource-record-sync fixture-config :trigger "ussyverse_social_replies_event")]
      (is (some? trigger))
      (with-redefs [resources/load-all-resources-sync (fn [_] [trigger])]
        (let [result (await (event-dispatch/dispatch!
                             fixture-config
                             {:event/type :discord.message
                              :event/actor "discord_automation"
                              :event/payload {:content "hey frankie"
                                              :gatewayBotUserId "12345"
                                              :gatewayActorId "discord_automation"
                                              :channelId "123"}}))
              invocation (first @action-calls)]
          (is (= ["ussyverse_social_replies_event"] (:matchedTriggers result)))
          (is (= 1 (count @action-calls)))
          (is (= :actions/start-agent-session (get-in invocation [:action :action/kind])))
          (is (= "ussyverse_social_replies" (get-in invocation [:action :action/with :agent-id])))
          (is (string? (get-in invocation [:action :action/with :task])))
          (is (= "discord_automation" (get-in invocation [:ctx :actor/id]))))))))

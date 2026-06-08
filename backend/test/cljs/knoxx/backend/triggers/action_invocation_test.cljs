(ns knoxx.backend.triggers.action-invocation-test
  "Verify that when a trigger matches, the action is actually invoked."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.action.registry :as action-registry]
            [knoxx.backend.domain.condition.builtin :as condition-builtins]
            [knoxx.backend.domain.driver.builtin :as driver-builtin]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.domain.source.runtime :as source-runtime]))

(def fixture-config
  {:contracts-dir "test/fixtures/trigger-contracts"})

(deftest ^:async action-is-invoked-when-trigger-matches
  (testing "when a trigger matches, the action registry receives the action map"
    (driver-builtin/register-built-in-drivers!)
    (condition-builtins/register-builtins!)
    (event-dispatch/reset-dedup!)
    (let [action-invocations (atom [])
          original-run-action! action-registry/run-action!]
      (with-redefs [action-registry/run-action!
                     (fn [ctx action]
                       (swap! action-invocations conj {:ctx-keys (keys ctx)
                                                        :action action})
                       (js/Promise.resolve {:ok true :test true}))
                     resources/load-all-resources-sync
                     (fn [_]
                       ;; Load only the trigger we care about
                       [(resources/resource-record-sync fixture-config :trigger "ussyverse_social_replies_event")])]
        (let [result (await (event-dispatch/dispatch!
                             fixture-config
                             {:event/type :discord.message
                              :event/actor "discord_automation"
                              :event/payload {:content "hey frankie"
                                              :gatewayBotUserId "12345"
                                              :gatewayActorId "discord_automation"
                                              :channelId "123"}}))]
          (is (= ["ussyverse_social_replies_event"] (:matchedTriggers result))
              "Trigger should match")
          (is (= 1 (count @action-invocations))
              "Action should be invoked exactly once")
          (when (= 1 (count @action-invocations))
            (let [invocation (first @action-invocations)]
              (is (= :actions/start-agent-session (get-in invocation [:action :action/kind]))
                  "Action kind should be start-agent-session")
              (is (= "ussyverse_social_replies" (get-in invocation [:action :action/with :agent-id]))
                  "Action should reference the correct agent")
              (is (some? (get-in invocation [:action :action/with :task]))
                  "Action should include the task prompt"))))))))

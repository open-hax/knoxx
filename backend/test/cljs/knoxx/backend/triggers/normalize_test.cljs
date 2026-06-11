(ns knoxx.backend.triggers.normalize-test
  "Tests for trigger normalization: :trigger/with is the sole argument
   mechanism, with legacy :trigger/agent and :trigger/task folding into it."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.action.registry :as registry]
            [knoxx.backend.domain.trigger.normalize :as normalize]))

(deftest trigger-with-passes-through
  (testing "explicit :trigger/with is the argument map"
    (let [trigger (normalize/normalize-trigger
                   {:contract/id "t1"
                    :trigger/kind :event
                    :trigger/events [:x]
                    :trigger/with {:agent-id "a1" :task "do it" :anything 7}})]
      (is (= {:agent-id "a1" :task "do it" :anything 7} (:trigger/with trigger))))))

(deftest legacy-agent-and-task-fold-into-with
  (let [trigger (normalize/normalize-trigger
                 {:contract/id "t2"
                  :trigger/kind :event
                  :trigger/events [:x]
                  :trigger/agent "legacy_agent"
                  :trigger/task "legacy task"})]
    (is (= {:agent-id "legacy_agent" :task "legacy task"} (:trigger/with trigger)))
    (is (not (contains? trigger :trigger/agent)) "normalized shape drops :trigger/agent")
    (is (not (contains? trigger :trigger/task)) "normalized shape drops :trigger/task")))

(deftest explicit-with-wins-over-legacy-fields
  (let [trigger (normalize/normalize-trigger
                 {:contract/id "t3"
                  :trigger/kind :event
                  :trigger/events [:x]
                  :trigger/agent "legacy_agent"
                  :trigger/task "legacy task"
                  :trigger/with {:agent-id "with_agent" :task "with task"}})]
    (is (= {:agent-id "with_agent" :task "with task"} (:trigger/with trigger)))))

(deftest data-task-fallback-folds-into-with
  (let [trigger (normalize/normalize-trigger
                 {:contract/id "t4"
                  :trigger/kind :event
                  :trigger/events [:x]
                  :data {:task "data task"}})]
    (is (= "data task" (get-in trigger [:trigger/with :task])))))

(deftest target-defaults-action-and-agent
  (let [trigger (normalize/normalize-trigger
                 {:contract/id "t5"
                  :trigger/kind :event
                  :trigger/events [:x]
                  :trigger/target "target_agent"})]
    (is (= :actions/start-agent-session (:trigger/action trigger)))
    (is (= "target_agent" (get-in trigger [:trigger/with :agent-id])))))

;; ── action-map: trigger/with becomes action/with ─────────────────────

(deftest action-map-uses-trigger-with-as-action-with
  (let [trigger (normalize/normalize-trigger
                 {:contract/id "t6"
                  :trigger/kind :event
                  :trigger/events [:x]
                  :trigger/action :actions/start-agent-session
                  :trigger/with {:agent-id "a1" :task "t"}})
        action (registry/action-map trigger)]
    (is (= :actions/start-agent-session (:action/kind action)))
    (is (= {:agent-id "a1" :task "t"} (:action/with action)))))

(deftest action-map-defaults-empty-with
  (let [action (registry/action-map
                (normalize/normalize-trigger
                 {:contract/id "t7" :trigger/kind :event :trigger/events [:x]
                  :trigger/action :actions/noop}))]
    (is (= {} (:action/with action)))))

(deftest action-map-carries-composite-action-keys
  (testing "composite resources pass :action/fn and :action/scope through"
    (let [raw {:contract/id "ussyverse/social-replies"
               :trigger/kind :event
               :trigger/events [:discord.message]
               :trigger/with {:agent-id "a1"}
               :action/fn '(fn [ctx action] {:ok true})
               :action/scope {:actions [:actions/noop]
                              :stores [:ussyverse/observed-messages]}}
          action (registry/action-map (normalize/normalize-trigger raw))]
      (is (some? (:action/fn action)))
      (is (= {:actions [:actions/noop] :stores [:ussyverse/observed-messages]}
             (:action/scope action))))))

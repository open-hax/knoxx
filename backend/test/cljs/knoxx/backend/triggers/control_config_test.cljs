(ns knoxx.backend.triggers.control-config-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.resources.loader :as resource-loader]
            [knoxx.backend.infra.control-config :as control-config]))

(defn- resource-record
  [resource-class resource-id resource]
  {:resource/class resource-class
   :resource/id resource-id
   :resource/definition resource
   :id resource-id
   :resourceClass resource-class})

(def agent-with-runtime-fields-resource
  (resource-record
   "agents"
   "legacy_mixed_agent"
   {:contract/id "legacy_mixed_agent"
    :contract/kind :agent
    :enabled true
    :trigger-kind :cron
    :source-kind :discord
    :agent {:role :role/test}
    :prompts {:system "system" :task "task"}}))

(def plain-agent-resource
  (resource-record
   "agents"
   "plain_agent"
   {:contract/id "plain_agent"
    :contract/kind :agent
    :enabled true
    :contract/actors ["discord_automation"]
    :agent {:role :role/test}
    :prompts {:system "system" :task "task"}
    :actor {:capabilities [:cap/discord]}}))

(def trigger-resource
  (resource-record
   "triggers"
   "message_reply_trigger"
   {:contract/id "message_reply_trigger"
    :contract/kind :trigger
    :enabled true
    :trigger/kind :event
    :trigger/events [:discord.message]
    :trigger/actor "discord_automation"
    :trigger/action :actions/start-agent-session
    :trigger/agent "plain_agent"}))

(def action-resource
  (resource-record
   "actions"
   "start_agent_session"
   {:contract/id "start_agent_session"
    :contract/kind :action
    :enabled true
    :action/handler "knoxx.backend.domain.action.start-agent-session"}))

(def schedule-resource
  (resource-record
   "schedules"
   "half_hour_tick"
   {:contract/id "half_hour_tick"
    :contract/kind :schedule
    :enabled true
    :schedule/rule "*/30 * * * *"
    :schedule/event {:event/type :schedule.tick
                     :event/payload {:reason "half hour"}}}))

(def generator-resource
  (resource-record
   "generators"
   "cron_generator"
   {:contract/id "cron_generator"
    :contract/kind :generator
    :enabled true
    :generator/kind "cron"
    :generator/emits [:schedule.tick]}))

(deftest event-control-config-keeps-runtime-resources-separate
  (with-redefs [resource-loader/load-all-resources-sync
                (fn [_]
                  [plain-agent-resource trigger-resource action-resource schedule-resource generator-resource])]
    (let [control (control-config/event-control-config {})]
      (testing "runtime concepts are flat resource tables, not synthesized jobs"
        (is (nil? (:jobs control)))
        (is (= ["plain_agent"] (mapv :id (get-in control [:resources :agent]))))
        (is (= ["message_reply_trigger"] (mapv :id (get-in control [:resources :trigger]))))
        (is (= ["start_agent_session"] (mapv :id (get-in control [:resources :action]))))
        (is (= ["half_hour_tick"] (mapv :id (get-in control [:resources :schedule]))))
        (is (= ["cron_generator"] (mapv :id (get-in control [:resources :generator]))))
        (is (= {:agent ["plain_agent"]
                :actor []
                :action ["start_agent_session"]
                :trigger ["message_reply_trigger"]
                :schedule ["half_hour_tick"]
                :generator ["cron_generator"]}
               (get-in control [:catalog :catalog/resources]))))
      (testing "separate resource tables are admissible when they keep their responsibilities"
        (is (true? (get-in control [:admissibility :ok?])))
        (is (empty? (get-in control [:admissibility :violations])))))))

(deftest event-control-config-reports-agent-runtime-field-violations
  (with-redefs [resource-loader/load-all-resources-sync
                (fn [_]
                  [agent-with-runtime-fields-resource trigger-resource action-resource])]
    (let [control (control-config/event-control-config {})
          violations (get-in control [:admissibility :violations])]
      (testing "agent resources do not absorb triggers, generators, schedules, or sources"
        (is (false? (get-in control [:admissibility :ok?])))
        (is (= #{:agent/contains-runtime-agreement}
               (into #{} (map :violation/kind) violations)))
        (is (= #{:source-kind :trigger-kind}
               (->> violations first :data :keys set)))
        (is (= "legacy_mixed_agent" (-> violations first :resource/id)))))))

(deftest event-control-config-reports-trigger-schedule-violations
  (let [cron-trigger (resource-record
                      "triggers"
                      "old_cron_trigger"
                      {:contract/id "old_cron_trigger"
                       :contract/kind :trigger
                       :enabled true
                       :trigger/kind :cron
                       :trigger/schedule "*/5 * * * *"
                       :trigger/action :actions/start-agent-session
                       :trigger/agent "plain_agent"})]
    (with-redefs [resource-loader/load-all-resources-sync (fn [_] [cron-trigger])]
      (let [violations (get-in (control-config/event-control-config {}) [:admissibility :violations])]
        (testing "schedules emit synthetic events instead of being trigger fields"
          (is (= #{:trigger/missing-events :trigger/contains-schedule}
                 (into #{} (map :violation/kind) violations))))))))

(deftest event-control-options-use-generator-vocabulary
  (with-redefs [resource-loader/load-all-resources-sync (fn [_] [generator-resource])]
    (is (= ["cron"] (control-config/event-generator-kind-options {})))
    (is (= ["event"] (control-config/event-trigger-kind-options)))))

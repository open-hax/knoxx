(ns knoxx.backend.openplanner-session-store-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.infra.stores.openplanner-session-store :as op-store]))

(deftest archived-run-events-carry-agent-trigger-and-event-scope
  (let [run {:run_id "run-1"
             :session_id "session-1"
             :conversation_id "conversation-1"
             :status "completed"
             :created_at "2026-05-25T00:00:00.000Z"
             :updated_at "2026-05-25T00:01:00.000Z"
             :model "gemma4:31b"
             :settings {:agentSpec {:contractId "ussyverse_social_creative"
                                     :actorId "discord_automation"
                                     :triggerId "ussyverse_social_creative_cron"
                                     :eventType "schedule/ussyverse-social-creative"
                                     :eventTypes ["schedule/ussyverse-social-creative"]
                                     :eventId "evt-1"
                                     :eventScopeId "ussyverse_social_creative"
                                     :scheduleId "ussyverse_social_creative"}}
             :messages [{:role "user" :content "hello"}]
             :answer "done"}
        events (#'op-store/run->events {:session-project-name "knoxx-session"} run)
        extras (map :extra events)]
    (is (seq events))
    (doseq [extra extras]
      (is (= "ussyverse_social_creative" (:contract_id extra)))
      (is (= "discord_automation" (:actor_id extra)))
      (is (= "ussyverse_social_creative_cron" (:trigger_id extra)))
      (is (= "schedule/ussyverse-social-creative" (:event_type extra)))
      (is (= ["schedule/ussyverse-social-creative"] (:event_types extra)))
      (is (= "evt-1" (:event_id extra)))
      (is (= "ussyverse_social_creative" (:event_scope_id extra)))
      (is (= "ussyverse_social_creative" (:schedule_id extra))))))

(ns knoxx.backend.actions.start-agent-session-test
  (:require [clojure.test :refer [deftest is testing]]
            [knoxx.backend.domain.action.start-agent-session :as start-agent-session]))

(deftest triggered-session-identifiers-include-trigger-scope-and-timestamp
  (testing "normal trigger launches get unique conversation ids per run"
    (is (= {:trigger-id "ussyverse_social_creative_cron"
            :event-scope-id "1494137016303095828"
            :run-id "trigger-ussyverse_social_creative_cron-12345"
            :conversation-id "trigger-ussyverse_social_creative_cron-12345-1494137016303095828"
            :session-id "trigger-session-ussyverse_social_creative_cron-1494137016303095828-12345"}
           (start-agent-session/triggered-session-identifiers
            {:trigger/id "ussyverse_social_creative_cron"}
            {:event/id "evt_1"
             :event/payload {:channelId "1494137016303095828"}}
            "ussyverse_social_creative"
            12345)))))

(deftest triggered-session-identifiers-never-collapse-to-trigger-event
  (testing "missing trigger ids fall back to agent/schedule context instead of trigger--event"
    (let [ids (start-agent-session/triggered-session-identifiers
               {}
               {:event/id "evt_2"
                :event/generator {:schedule/id "ussyverse_social_creative"}
                :event/payload {:schedule/id "ussyverse_social_creative"}}
               "ussyverse_social_creative"
               67890)]
      (is (= "ussyverse_social_creative" (:trigger-id ids)))
      (is (= "ussyverse_social_creative" (:event-scope-id ids)))
      (is (= "trigger-ussyverse_social_creative-67890-ussyverse_social_creative"
             (:conversation-id ids)))
      (is (not (re-find #"trigger--event" (:conversation-id ids)))))))

(deftest triggered-audit-metadata-keeps-trigger-and-event-names
  (let [event {:event/id "evt_schedule_1"
               :event/type :schedule/ussyverse-social-creative
               :event/types [:schedule/ussyverse-social-creative]
               :event/generator {:schedule/id "ussyverse_social_creative"}
               :event/payload {:schedule/id "ussyverse_social_creative"}}
        ids (start-agent-session/triggered-session-identifiers
             {:trigger/id "ussyverse_social_creative_cron"}
             event
             "ussyverse_social_creative"
             12345)]
    (is (= {:trigger_id "ussyverse_social_creative_cron"
            :event_scope_id "ussyverse_social_creative"
            :event_id "evt_schedule_1"
            :event_type "schedule/ussyverse-social-creative"
            :event_types ["schedule/ussyverse-social-creative"]
            :schedule_id "ussyverse_social_creative"}
           (start-agent-session/triggered-audit-metadata
            {:trigger/id "ussyverse_social_creative_cron"}
            event
            ids)))))

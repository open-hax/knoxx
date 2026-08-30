(ns knoxx.backend.actions.start-agent-session-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [knoxx.backend.domain.action.start-agent-session :as start-agent-session]))

(def ^:private resolved-execution
  {:model "model-a"
   :thinking-level :medium
   :system-prompt "Translate every admitted split."
   :tool-ids ["save_translation"]})

(defn- execution-event
  [overrides]
  {:event/payload
   {:execution
    (merge {:translation-execution/agent-id "publication_translator"
            :translation-execution/model "model-a"
            :translation-execution/thinking "medium"
            :translation-execution/system-prompt
            "Translate every admitted split."
            :translation-execution/tool-ids ["save_translation"]
            :translation-execution/digest "digest-a"}
           overrides)}})

(deftest execution-snapshot-pins-what-the-trigger-may-spawn
  (testing "the exact resolved policy agrees with its pre-provider event"
    (is (nil? (start-agent-session/event-execution-refusal
               (execution-event {}) "publication_translator"
               resolved-execution))))

  (testing "model, prompt, tools, and agent drift are each refused"
    (doseq [[field changed]
            [[:translation-execution/agent-id "some-other-agent"]
             [:translation-execution/model "model-b"]
             [:translation-execution/thinking "high"]
             [:translation-execution/system-prompt "Different prompt"]
             [:translation-execution/tool-ids ["save_translation" "other"]]]]
      (let [refusal (start-agent-session/event-execution-refusal
                     (execution-event {field changed})
                     "publication_translator" resolved-execution)]
        (is (= :agent-execution-drift (:refusal/type refusal))
            (str field " drift was allowed")))))

  (testing "missing execution authority is refused rather than defaulted"
    (is (= :agent-execution-drift
           (:refusal/type
            (start-agent-session/event-execution-refusal
             {:event/payload {}} "publication_translator"
             resolved-execution))))))

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

(deftest triggered-session-identifiers-honor-sticky-sessions
  (testing "sticky sources reuse one conversation/session per trigger+scope while runs stay unique"
    (let [ids-1 (start-agent-session/triggered-session-identifiers
                 {:trigger/id "ussyverse_social_replies_event"}
                 {:event/id "evt_1" :event/payload {:channelId "1494137016303095828"}}
                 "ussyverse_social_replies"
                 12345
                 {:sticky? true})
          ids-2 (start-agent-session/triggered-session-identifiers
                 {:trigger/id "ussyverse_social_replies_event"}
                 {:event/id "evt_2" :event/payload {:channelId "1494137016303095828"}}
                 "ussyverse_social_replies"
                 67890
                 {:sticky? true})]
      (is (= "trigger-ussyverse_social_replies_event-sticky-1494137016303095828"
             (:conversation-id ids-1)
             (:conversation-id ids-2)))
      (is (= "trigger-session-ussyverse_social_replies_event-1494137016303095828"
             (:session-id ids-1)
             (:session-id ids-2)))
      (is (not= (:run-id ids-1) (:run-id ids-2)))))
  (testing "different channels still get distinct sticky sessions"
    (let [ids-a (start-agent-session/triggered-session-identifiers
                 {:trigger/id "t"} {:event/payload {:channelId "chan-a"}} "agent" 1 {:sticky? true})
          ids-b (start-agent-session/triggered-session-identifiers
                 {:trigger/id "t"} {:event/payload {:channelId "chan-b"}} "agent" 1 {:sticky? true})]
      (is (not= (:session-id ids-a) (:session-id ids-b)))
      (is (not= (:conversation-id ids-a) (:conversation-id ids-b))))))

(deftest sticky-session-source-config-resolution
  (testing "stickySession and sessionMaxMessages come from the agent contract :data :source"
    (let [resolved {:contract {:data {:source {:max-messages 100
                                               :stickySession true
                                               :sessionMaxMessages 1000}}}
                    :context-policy nil}
          source (start-agent-session/agent-source-config resolved)]
      (is (start-agent-session/sticky-session-source? source))
      (is (= {:max-messages 1000}
             (start-agent-session/sticky-context-policy resolved source)))))
  (testing "an explicit contract context policy keeps its own message cap"
    (let [resolved {:contract {:data {:source {:stickySession true
                                               :sessionMaxMessages 1000}}}
                    :context-policy {:max-messages 40}}
          source (start-agent-session/agent-source-config resolved)]
      (is (= {:max-messages 40}
             (start-agent-session/sticky-context-policy resolved source)))))
  (testing "non-sticky contracts are unchanged"
    (let [source (start-agent-session/agent-source-config {:contract {}})]
      (is (not (start-agent-session/sticky-session-source? source)))
      (is (nil? (start-agent-session/sticky-context-policy {:context-policy nil} source))))))

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

(deftest action-task-input-prefers-action-or-trigger-over-agent-task-prompts
  (testing "trigger/action task text is the primary source for triggered user messages"
    (is (= {:task "Act from the trigger."
            :task-source :action/task
            :deprecated-agent-task-fallback? false}
           (start-agent-session/action-task-input
            {:action/with {:task "Act from the trigger."}}
            {:trigger/task "Act from the trigger resource."}
            {:task-prompt "Legacy agent task."}))))
  (testing "legacy agent task prompts are only a deprecated fallback"
    (is (= {:task "Legacy agent task."
            :task-source :agent/task-prompt
            :deprecated-agent-task-fallback? true}
           (start-agent-session/action-task-input
            {:action/with {}}
            {}
            {:task-prompt "Legacy agent task."})))))

(deftest rendered-start-message-labels-action-task-source
  (let [event {:event/type :discord.message
               :event/payload {:channelId "chan-1"
                               :authorUsername "err"
                               :content "ping"}}
        message (start-agent-session/render-start-message
                 {:trigger/id "ussyverse_social_replies_event"
                  :trigger/context {:reason "contract event: ussyverse social replies"}}
                 event
                 {:task "Use the action-owned task."
                  :task-source :trigger/task}
                 "ussyverse_social_replies_event")]
    (is (str/includes? message "Event: discord.message"))
    (is (str/includes? message "Reason: contract event: ussyverse social replies"))
    (is (str/includes? message "Action task prompt:"))
    (is (str/includes? message "Use the action-owned task."))
    (is (not (str/includes? message "Agent task prompt:")))))

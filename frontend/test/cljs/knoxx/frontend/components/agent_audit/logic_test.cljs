(ns knoxx.frontend.components.agent-audit.logic-test
  "Written FIRST (TDD) — pure-logic contract for the Helix port of
  src/components/agent-audit/AgentAuditSessionList.tsx (merge/scoring/
  matching utilities). Mirrors the utility tests in
  AgentAuditSessionList.test.tsx plus deeper coverage."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.frontend.components.agent-audit.logic :as logic]))

(defn active-run
  ([] (active-run {}))
  ([overrides]
   (merge {:run_id "run-1"
           :session_id "sid-1"
           :conversation_id "conv-1"
           :status "running"
           :model "gemma4:31b"
           :created_at "2026-05-14T00:00:00.000Z"
           :updated_at "2026-05-14T00:01:00.000Z"
           :agent_spec {:contractId "fork_tales_creative_director"
                        :actorId "discord_automation"
                        :subAgentId "fork_tales_creative_director"
                        :triggerId "fork_tales_creative_director_cron"
                        :eventType "schedule/fork-tales-creative-director"
                        :eventTypes ["schedule/fork-tales-creative-director"]
                        :eventId "evt-fork-tales"
                        :eventScopeId "fork_tales_creative_director"
                        :scheduleId "fork_tales_creative_director"}}
          overrides)))

(deftest contract-matching
  (is (logic/session-matches-contract? {:session "a" :contract_id "agent_a"} "agent_a"))
  (is (logic/session-matches-contract? {:session "b" :contract_actors ["agent_b"]} "agent_b"))
  (is (not (logic/session-matches-contract? {:session "c" :contract_id "agent_c"} "agent_b")))
  (is (logic/session-matches-contract? {:session "d"} nil) "no target matches all")
  (is (logic/session-matches-contract? {:session "e"} "new-agent") "new-agent matches all")
  (is (logic/run-matches-contract? (active-run) "fork_tales_creative_director"))
  (is (not (logic/run-matches-contract? (active-run) "other_agent"))))

(deftest active-run-becomes-live-session
  (let [session (logic/active-run->session (active-run))]
    (is (= "conv-1" (:session session)))
    (is (= "active" (:auditSource session)))
    (is (= "fork_tales_creative_director" (:contract_id session)))
    (is (= "sid-1" (:active_session_id session)))
    (is (= "fork_tales_creative_director_cron" (:trigger_id session)))
    (is (= "schedule/fork-tales-creative-director" (:event_type session)))
    (is (= "fork_tales_creative_director" (:schedule_id session)))
    (is (true? (:is_active session)))
    (is (= "sub-agent fork_tales_creative_director" (:title session))))
  (testing "falls back conversation→session→run id; nil when none"
    (is (= "sid-9" (:session (logic/active-run->session
                              (active-run {:conversation_id nil :session_id "sid-9"})))))
    (is (nil? (logic/active-run->session
               (active-run {:conversation_id nil :session_id nil :run_id nil}))))))

(deftest merge-prefers-active-and-keeps-history-event-count
  (let [memory [{:session "conv-1" :title "Archived" :contract_id "fork_tales_creative_director"
                 :event_count 7 :last_ts "2026-05-13T00:00:00.000Z"}
                {:session "conv-2" :title "Other" :contract_id "other_agent"
                 :event_count 2 :last_ts "2026-05-14T00:00:00.000Z"}]
        merged (logic/merge-sessions memory [(active-run)] "fork_tales_creative_director")]
    (is (= 1 (count merged)) "other-agent session filtered out")
    (is (= "conv-1" (:session (first merged))))
    (is (= "active" (:auditSource (first merged))))
    (is (= 7 (:event_count (first merged))) "history event count survives the merge")
    (is (= "Active" (:label (logic/session-status (first merged)))))))

(deftest sessions-sort-active-first-then-recent
  (let [merged (logic/merge-sessions
                [{:session "old-history" :title "A" :event_count 1 :last_ts "2026-05-01T00:00:00.000Z"}
                 {:session "new-history" :title "B" :event_count 1 :last_ts "2026-05-20T00:00:00.000Z"}]
                [(active-run {:conversation_id "live-1"})]
                nil)]
    (is (= ["live-1" "new-history" "old-history"] (mapv :session merged)))))

(deftest status-mapping
  (is (= {:label "Live" :variant :warning} (logic/session-status {:has_active_stream true})))
  (is (= {:label "Active" :variant :success} (logic/session-status {:active_status "running"})))
  (is (= {:label "Waiting" :variant :info} (logic/session-status {:active_status "waiting_input"})))
  (is (= {:label "Failed" :variant :error} (logic/session-status {:active_status "failed"})))
  (is (= {:label "Active" :variant :success} (logic/session-status {:is_active true})))
  (is (= {:label "History" :variant :default} (logic/session-status {}))))

(deftest page-merging-dedupes-on-session
  (let [first-page [{:session "s1" :title "One" :event_count 1 :last_ts "2026-05-02T00:00:00.000Z"}]
        second-page [{:session "s2" :title "Two" :event_count 1 :last_ts "2026-05-01T00:00:00.000Z"}
                     {:session "s1" :title "One" :event_count 3 :last_ts "2026-05-02T00:00:00.000Z"}]
        merged (logic/merge-session-pages first-page second-page)]
    (is (= ["s1" "s2"] (mapv :session merged)))
    (is (= 3 (:event_count (first merged))) "duplicate rows merge, keeping max event count")))

(deftest search-text-includes-identifiers
  (let [text (logic/session-search-text
              {:session "conv-1" :title "Fork history" :model "gemma4:31b"
               :trigger_id "trig-1" :event_types ["schedule/x"] :contract_actors ["actor-9"]})]
    (is (str/includes? text "fork history"))
    (is (str/includes? text "gemma4:31b"))
    (is (str/includes? text "trig-1"))
    (is (str/includes? text "schedule/x"))
    (is (str/includes? text "actor-9"))))

(deftest format-maybe-date-contract
  (is (nil? (logic/format-maybe-date nil)))
  (is (= "not-a-date" (logic/format-maybe-date "not-a-date")))
  (is (str/includes? (logic/format-maybe-date "2026-05-14T00:00:00.000Z") "2026")))

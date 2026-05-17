(ns knoxx.backend.event-agents-test
  (:require-macros [knoxx.backend.macros :refer [with-redis-nil]])
  (:require [cljs.test :refer-macros [deftest is async testing]]
            [knoxx.backend.event-agents :as ea]
            [knoxx.backend.session-store :as session-store]))

(defn- reset-runtime! []
  (ea/stop!)
  (reset! ea/job-state* {})
  (reset! ea/job-specs* {})
  (reset! ea/source-state* {:discord {:last-seen {}}})
  (reset! ea/recent-events* []))

(deftest dedup-atom-exists
  (testing "dispatched-event-ids* is a set atom"
    (is (set? @ea/dispatched-event-ids*))))

(deftest contract-sourced-jobs-do-not-inherit-global-discord-default-channels
  (let [control {:sources {:discord {:defaultChannels ["leaky-default"]}}}]
    (is (= [] (#'ea/job-channels control {:contractSourceId "agent-a" :filters {}})))
    (is (= ["explicit"] (#'ea/job-channels control {:contractSourceId "agent-a"
                                                     :filters {:channels ["explicit"]}})))
    (is (= ["leaky-default"] (#'ea/job-channels control {:filters {}})))))

(deftest discord-template-source-messages-truncate-untrusted-message-text
  (let [long-text (apply str (repeat 200 "x"))
        rows [{:channelId "channel-a"
               :messages [{:id "message-a"
                           :channelId "channel-a"
                           :authorUsername "alice"
                           :authorId "user-a"
                           :timestamp "2026-05-13T00:00:00Z"
                           :content long-text}]}]
        [message] (#'ea/discord-template-source-messages rows 50)]
    (is (= "message-a" (:id message)))
    (is (false? (:trusted? message)))
    (is (<= (count (:text message)) 110))
    (is (re-find #"source message truncated at 50 chars" (:text message)))))

(deftest discord-template-source-messages-pin-trusted-then-good
  (let [rows [{:channelId "channel-a"
               :messages [{:id "neutral"
                           :channelId "channel-a"
                           :authorUsername "alice"
                           :authorId "user-a"
                           :content "neutral"}
                          {:id "good"
                           :channelId "channel-a"
                           :authorUsername "bob"
                           :authorId "user-b"
                           :openplannerLabels {:quality "good"}
                           :content "good"}
                          {:id "trusted"
                           :channelId "channel-a"
                           :authorUsername "carol"
                           :authorId "user-c"
                           :authorRoleIds ["1494109844737753220"]
                           :content "trusted"}]}]
        messages (#'ea/discord-template-source-messages rows 1200)]
    (is (= ["trusted" "good" "neutral"] (mapv :id messages)))
    (is (true? (:trusted? (first messages))))
    (is (= [:discord-role/trust] (:trust/roles (first messages))))
    (is (= ["1494109844737753220"] (:author-role-ids (first messages))))))

;; TEST 1
(deftest concurrent-starts-are-idempotent
  (testing "Three concurrent start! calls admit exactly one start! body"
    (async done
      (reset-runtime!)
      (with-redis-nil
        (let [p1 (ea/start! nil)
              p2 (ea/start! nil)
              p3 (ea/start! nil)]
          (-> (js/Promise.all #js [p1 p2 p3])
              (.then (fn [_]
                       (is (true? @ea/running?*))
                       (is (contains? @ea/scheduled-tasks* :flush))
                       (let [task-count (count @ea/scheduled-tasks*)]
                         (ea/stop!)
                         (-> (ea/start! nil)
                             (.then (fn [_]
                                      (is (= task-count (count @ea/scheduled-tasks*))
                                          (str "stable: "
                                               task-count " vs "
                                               (count @ea/scheduled-tasks*)))
                                      (ea/stop!)
                                      (done)))))))
              (.catch (fn [e]
                        (ea/stop!)
                        (is false (str "rejected: " (.-message e)))
                        (done)))))))))

;; TEST 2
(deftest reload-during-startup-does-not-double-register
  (testing "reload! after start! leaves exactly one set of registered tasks"
    (async done
      (reset-runtime!)
      (with-redis-nil
        (let [p1 (ea/start! nil)]
          (ea/reload!)
          (-> p1
              (.finally (fn []
                          (js/setTimeout
                           (fn []
                             (is (contains? @ea/scheduled-tasks* :flush))
                             (is (= 1 (count (filter #(= :flush %) (keys @ea/scheduled-tasks*)))))
                             (ea/stop!)
                             (done))
                           200)))))))))

;; TEST 3
(deftest dispatch-event-is-idempotent-on-same-event-id
  (testing "Three identical dispatch-event! calls short-circuit on 2nd and 3rd"
    (async done
      (reset-runtime!)
      (let [ev {:sourceKind "discord"
               :eventKind  "discord.snapshot.summary"
               :id         "evt-stable-abc-123"
               :timestamp  "2026-04-28T01:00:00Z"
               :payload    {:channelId "ch1" :summary "hi"}}]
        (with-redis-nil
          (-> (js/Promise.all #js [(ea/dispatch-event! ev)
                                   (ea/dispatch-event! ev)
                                   (ea/dispatch-event! ev)])
              (.then (fn [rs]
                       (let [arr (vec (array-seq rs))
                             sk  (filter :skipped arr)]
                         (is (= 1 (count @ea/dispatched-event-ids*)))
                         (is (= 2 (count sk)) (str "skipped: " (count sk)))
                         (done))))
              (.catch (fn [e]
                        (is false (str "rejected: " (.-message e)))
                        (done)))))))))

;; TEST 4
(deftest sticky-session-ids-are-scoped-by-discord-owner
  (testing "sticky event agents do not collapse different Discord channels into one session"
    (async done
      (reset-runtime!)
      (reset! session-store/session-cache* {})
      (with-redis-nil
        (let [job {:id "sticky-job"
                   :source {:kind "discord"
                            :config {:stickySession true
                                     :sessionMaxMessages 10}}
                   :agentSpec {:systemPrompt "system"
                               :taskPrompt "task"
                               :model "gemma4:31b"
                               :toolPolicies []}}
              event-a {:sourceKind "discord"
                       :eventKind "discord.message"
                       :id "event-a"
                       :timestamp "2026-05-11T00:00:00Z"
                       :payload {:channelId "channel-a"
                                 :authorId "user-1"
                                 :content "hello a"}}
              event-b (assoc-in event-a [:payload :channelId] "channel-b")]
          (-> (js/Promise.all #js [(ea/build-agent-run-payload {} job event-a)
                                   (ea/build-agent-run-payload {} job event-b)])
              (.then (fn [results]
                       (let [payload-a (aget results 0)
                             payload-b (aget results 1)]
                         (is (= "event-agent-session-sticky-job-channel-a-sticky" (:session_id payload-a)))
                         (is (= "event-agent-session-sticky-job-channel-b-sticky" (:session_id payload-b)))
                         (is (not= (:conversation_id payload-a) (:conversation_id payload-b)))
                         (done))))
              (.catch (fn [e]
                        (is false (str "rejected: " (.-message e)))
                        (done)))))))))

;; TEST 5
(deftest upsert-job-skips-reload-when-spec-unchanged
  (testing "Upserting the same spec twice triggers at most 1 reload!"
    (async done
      (reset-runtime!)
      (let [cnt  (atom 0)
            orig ea/reload!]
        (set! ea/reload! (fn [] (swap! cnt inc)))
        (with-redis-nil
          (let [spec {:id "disco_spawner" :enabled true}]
            (-> (ea/upsert-job! "disco_spawner" spec)
                (.then (fn [_] (ea/upsert-job! "disco_spawner" spec)))
                (.then (fn [_]
                         (set! ea/reload! orig)
                         (is (<= @cnt 1) (str "reload! count: " @cnt))
                         (done)))
                (.catch (fn [e]
                          (set! ea/reload! orig)
                          (is false (str "rejected: " (.-message e)))
                          (done))))))))))

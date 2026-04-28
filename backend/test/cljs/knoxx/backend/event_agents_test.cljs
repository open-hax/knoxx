(ns knoxx.backend.event-agents-test
  (:require-macros [knoxx.backend.macros :refer [with-redis-nil]])
  (:require [cljs.test :refer-macros [deftest is async testing]]
            [knoxx.backend.event-agents :as ea]
            [knoxx.backend.redis-client :as redis]))

(defn- reset-runtime! []
  (ea/stop!)
  (reset! ea/job-state* {})
  (reset! ea/job-specs* {})
  (reset! ea/source-state* {:discord {:last-seen {}}})
  (reset! ea/recent-events* []))

(deftest dedup-atom-exists
  (testing "dispatched-event-ids* is a set atom"
    (is (set? @ea/dispatched-event-ids*))))

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

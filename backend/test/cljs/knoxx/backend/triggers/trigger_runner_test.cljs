(ns knoxx.backend.triggers.trigger-runner-test
  (:require [knoxx.backend.domain.action.registry :as action-registry]
            [knoxx.backend.domain.schedule.runtime :as schedule-runtime]
            [clojure.test :refer [deftest is testing]]))

;; ── schedule rules ─────────────────────────────

(deftest schedule-rule-star-slash-n
  (testing "*/5 * * * * → 5 minutes in ms"
    (is (= (* 5 60 1000) (schedule-runtime/rule->ms "*/5 * * * *"))))
  (testing "*/1 * * * * → 1 minute in ms"
    (is (= (* 1 60 1000) (schedule-runtime/rule->ms "*/1 * * * *")))))

(deftest schedule-rule-n-minutes
  (testing "5 * * * * → 5 minutes in ms"
    (is (= (* 5 60 1000) (schedule-runtime/rule->ms "5 * * * *"))))
  (testing "30 * * * * → 30 minutes in ms"
    (is (= (* 30 60 1000) (schedule-runtime/rule->ms "30 * * * *")))))

(deftest schedule-rule-default
  (testing "unrecognized falls back to 5 minutes"
    (is (= (* 5 60 1000) (schedule-runtime/rule->ms "junk"))))
  (testing "daily minute/hour cron forms tick at most daily in the naive scheduler"
    (is (= (* 24 60 60 1000) (schedule-runtime/rule->ms "0 2 * * *"))))
  (testing "never returns < 1 minute"
    (is (= (* 1 60 1000) (schedule-runtime/rule->ms "0 * * * *")))))

;; ── schedule event emission ───────────────────────

(deftest schedule-event-carries-generator-and-payload
  (let [event (schedule-runtime/schedule->event {:contract/id "game_tick"
                                                 :schedule/generator :game-clock
                                                 :schedule/event {:event/type :game.tick
                                                                  :event/payload {:turn 7}}})]
    (is (= :game.tick (:event/type event)))
    (is (= {:kind :game-clock :schedule/id "game_tick"} (:event/generator event)))
    (is (= {:schedule/id "game_tick" :turn 7} (:event/payload event)))))


;; ── action registry sanity ───────────────────────

(deftest noop-action-returns-promise
  (let [result (action-registry/run-action! {} {:action/kind :actions/noop})]
    (is (instance? js/Promise result)
        (str "Expected Promise, got: " (pr-str (type result))))))

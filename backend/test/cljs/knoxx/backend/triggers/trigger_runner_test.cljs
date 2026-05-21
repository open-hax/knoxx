(ns knoxx.backend.triggers.trigger-runner-test
  (:require [knoxx.backend.infra.trigger-runner :as tr]
            [knoxx.backend.domain.action.registry :as action-registry]
            [clojure.test :refer [deftest is testing async]]
            [clojure.string :as str]))

;; ── parse-cron-to-ms ─────────────────────────────

(deftest parse-cron-star-slash-n
  (testing "*/5 * * * * → 5 minutes in ms"
    (is (= (* 5 60 1000) (#'tr/parse-cron-to-ms "*/5 * * * *"))))
  (testing "*/1 * * * * → 1 minute in ms"
    (is (= (* 1 60 1000) (#'tr/parse-cron-to-ms "*/1 * * * *")))))

(deftest parse-cron-n-minutes
  (testing "5 * * * * → 5 minutes in ms"
    (is (= (* 5 60 1000) (#'tr/parse-cron-to-ms "5 * * * *"))))
  (testing "30 * * * * → 30 minutes in ms"
    (is (= (* 30 60 1000) (#'tr/parse-cron-to-ms "30 * * * *")))))

(deftest parse-cron-default
  (testing "unrecognized falls back to 5 minutes"
    (is (= (* 5 60 1000) (#'tr/parse-cron-to-ms "junk"))))
  (testing "0 2 * * * (starts with 0 but not N * * * *) falls back to 5 minutes"
    (is (= (* 5 60 1000) (#'tr/parse-cron-to-ms "0 2 * * *"))))
  (testing "never returns < 1 minute"
    (is (= (* 1 60 1000) (#'tr/parse-cron-to-ms "0 * * * *")))))

;; ── action registry sanity ───────────────────────

(deftest noop-action-returns-promise
  (let [result (action-registry/run-action! {} {:action/kind :actions/noop})]
    (is (instance? js/Promise result)
        (str "Expected Promise, got: " (pr-str (type result))))))

;; ── dispatch-trigger-action! ─────────────────────

(deftest ^:async dispatch-trigger-action-rejects-when-no-action
  (let [result (await (#'tr/dispatch-trigger-action! {} {:contract/id "bad_trigger"
                                                          :trigger/kind :cron} {}))]
    (is (= false (:ok result)))
    (is (str/includes? (:error result) "no :trigger/action"))
    ))

(deftest ^:async dispatch-trigger-action-runs-noop-action
  (let [trigger {:contract/id "test_trigger"
                 :trigger/action :actions/noop
                 :trigger/actor "discord_automation"}
        result (await (#'tr/dispatch-trigger-action! {} trigger {:reason "test"}))]
    (is (= true (:ok result)))
    (is (= :actions/noop (:action/kind result)))
    ))

(ns knoxx.backend.triggers.trigger-runner-test
  (:require [knoxx.backend.triggers.trigger-runner :as tr]
            [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]))

;; ── parse-cron-to-ms ─────────────────────────────

(deftest parse-cron-star-slash-n
  (testing "*/5 * * * * → 5 minutes in ms"
    (is (= (* 5 60 1000) (tr/parse-cron-to-ms "*/5 * * * *"))))
  (testing "*/1 * * * * → 1 minute in ms"
    (is (= (* 1 60 1000) (tr/parse-cron-to-ms "*/1 * * * *")))))

(deftest parse-cron-n-minutes
  (testing "5 * * * * → 5 minutes in ms"
    (is (= (* 5 60 1000) (tr/parse-cron-to-ms "5 * * * *"))))
  (testing "30 * * * * → 30 minutes in ms"
    (is (= (* 30 60 1000) (tr/parse-cron-to-ms "30 * * * *")))))

(deftest parse-cron-default
  (testing "unrecognized falls back to 5 minutes"
    (is (= (* 5 60 1000) (tr/parse-cron-to-ms "junk")))))

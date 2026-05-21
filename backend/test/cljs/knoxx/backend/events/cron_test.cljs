(ns knoxx.backend.events.cron-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.event.cron :as cron]))

(deftest cadence-label-renders-human-readable-schedules
  (testing "minute and hourly labels"
    (is (= "Every minute" (cron/cadence-label 1)))
    (is (= "Every 5 minutes" (cron/cadence-label 5)))
    (is (= "Every 2 hours" (cron/cadence-label 120)))))

(deftest cron-job-predicate-only-matches-enabled-cron-jobs
  (testing "cron trigger kind must be enabled"
    (is (true? (cron/cron-job? {:enabled true :trigger {:kind "cron"}})))
    (is (false? (cron/cron-job? {:enabled false :trigger {:kind "cron"}})))
    (is (false? (cron/cron-job? {:enabled true :trigger {:kind "event"}})))))

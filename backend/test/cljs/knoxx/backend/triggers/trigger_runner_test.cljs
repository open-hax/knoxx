(ns knoxx.backend.triggers.trigger-runner-test
  (:require [knoxx.backend.triggers.trigger-runner :as tr]
            [clojure.test :refer [deftest is testing]]
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
    (is (= (* 5 60 1000) (#'tr/parse-cron-to-ms "junk")))))

(deftest target-contract-id-strips-optional-type-prefix
  (testing "plain target ids are preserved"
    (is (= "mentions_pipeline" (#'tr/target-contract-id "mentions_pipeline"))))
  (testing "typed target prefixes are stripped before lookup"
    (is (= "mentions_pipeline" (#'tr/target-contract-id "pipeline:mentions_pipeline")))
    (is (= "ussyverse_social_creative" (#'tr/target-contract-id "agent:ussyverse_social_creative")))))

(deftest dispatch-target-resolves-pipeline-by-contract-class-not-prefix
  (let [calls (atom [])]
    (with-redefs [tr/load-contract-sync (fn [_config klass contract-id]
                                          (when (and (= klass "pipelines")
                                                     (= contract-id "mentions_pipeline"))
                                            {:contract/id contract-id
                                             :contract/kind :pipeline}))
                  tr/fire-pipeline! (fn [_config trigger-ctx pipeline-id]
                                      (swap! calls conj [:pipeline pipeline-id trigger-ctx])
                                      (js/Promise.resolve {:ok true}))
                  tr/fire-agent! (fn [_config _trigger-ctx agent-id]
                                   (swap! calls conj [:agent agent-id])
                                   (js/Promise.resolve {:ok true}))]
      (#'tr/dispatch-target! {} {:reason "test"} "mentions_pipeline")
      (is (= [[:pipeline "mentions_pipeline" {:reason "test"}]] @calls)))))

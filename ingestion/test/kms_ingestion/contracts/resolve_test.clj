(ns kms-ingestion.contracts.resolve-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [kms-ingestion.contracts.resolve :as cr]))

(def ^:private full-contract
  {:source/discovery   {:skip-dirs         #{"node_modules"}
                        :file-types        [".md"]
                        :include-patterns  ["docs/*"]
                        :exclude-patterns  ["archive/*"]
                        :hidden-policy     :include
                        :follow-symlinks?  true}
   :source/schedule    {:mode                     :poll
                        :sync-interval-minutes    10
                        :scheduler-poll-ms        30000
                        :passive-watch-enabled?   false
                        :passive-watch-poll-ms    15000
                        :passive-watch-debounce-ms 2000
                        :bootstrap?               false}
   :source/ingest      {:batch-size         3
                        :batch-parallelism  2
                        :batch-delay-ms     50
                        :throttle-enabled?  false
                        :max-load-per-core  0.5
                        :throttle-sleep-ms  500
                        :retry-failed?      false}
   :source/sink        {:type          :ragussy
                        :collections   ["test-col"]
                        :lake          "test-lake"
                        :visibility    "public"
                        :language      "fr"
                        :source-label  "test-label"
                        :created-by    "test-agent"
                        :model         "claude-4"}
   :source/semantic    {:enabled?            false
                        :build-mode          :off
                        :k                   4
                        :min-similarity      0.7
                        :emit-graph-events?  false}
   :source/translation {:enabled?  true
                        :model     "some-model"
                        :poll-ms   1000}
   :source/projection  {:domain-strategy        :fixed
                        :visibility             "public"
                        :language               "fr"
                        :created-by             "test-agent"
                        :source                 "test"}
   :source/backpressure {:strategy        :fixed
                         :base-delay-ms   500
                         :max-delay-ms    5000
                         :failure-window  3
                         :respect-remote? false}})

(deftest reads-contract-values
  (testing "all resolvers read from contract when present"
    (is (= #{"node_modules"} (cr/skip-dirs full-contract)))
    (is (= [".md"]           (cr/file-types full-contract)))
    (is (= ["docs/*"]        (cr/include-patterns full-contract)))
    (is (= ["archive/*"]     (cr/exclude-patterns full-contract)))
    (is (= :include          (cr/hidden-policy full-contract)))
    (is (true?               (cr/follow-symlinks? full-contract)))

    (is (= :poll             (cr/schedule-mode full-contract)))
    (is (= 10                (cr/sync-interval-minutes full-contract)))
    (is (= false             (cr/passive-watch-enabled? full-contract)))
    (is (= 2000              (cr/passive-watch-debounce-ms full-contract)))

    (is (= 3                 (cr/batch-size full-contract)))
    (is (= 2                 (cr/batch-parallelism full-contract)))
    (is (= false             (cr/throttle-enabled? full-contract)))
    (is (= 0.5               (cr/max-load-per-core full-contract)))

    (is (= :ragussy          (cr/sink-type full-contract)))
    (is (= ["test-col"]      (cr/sink-collections full-contract)))
    (is (= "fr"              (cr/sink-language full-contract)))
    (is (= "claude-4"        (cr/sink-model full-contract)))

    (is (= false             (cr/semantic-enabled? full-contract)))
    (is (= 4                 (cr/semantic-k full-contract)))
    (is (= 0.7               (cr/semantic-min-similarity full-contract)))

    (is (= true              (cr/translation-enabled? full-contract)))
    (is (= "some-model"      (cr/translation-model full-contract)))

    (is (= :fixed            (cr/domain-strategy full-contract)))
    (is (= "fr"              (cr/projection-language full-contract)))

    (is (= :fixed            (cr/backpressure-strategy full-contract)))
    (is (= 500               (cr/backpressure-base-ms full-contract)))
    (is (= 3                 (cr/backpressure-window full-contract)))))

(deftest falls-back-for-nil-contract
  (testing "all resolvers return non-nil when contract is nil (env/floor fallback)"
    ;; We can't assert exact values here because they come from env,
    ;; but we can assert they are non-nil and the right type.
    (is (some? (cr/batch-size nil)))
    (is (some? (cr/batch-parallelism nil)))
    (is (some? (cr/passive-watch-enabled? nil)))
    (is (some? (cr/scheduler-poll-ms nil)))
    (is (some? (cr/sink-type nil)))
    (is (some? (cr/semantic-k nil)))
    (is (some? (cr/translation-model nil)))))

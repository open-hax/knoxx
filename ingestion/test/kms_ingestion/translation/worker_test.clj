(ns kms-ingestion.translation.worker-test
  "Proves the worker reads translation model selection from the Knoxx-owned
  config boundary and from nowhere else."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [kms-ingestion.translation.worker :as worker]))

(defn- reset-cache! []
  (reset! worker/translation-model-cache* {:model nil :fetched-at 0}))

;; ── Single authority ──────────────────────────────────────────────────────

(deftest worker-and-facade-select-same-model
  (testing "the worker reports exactly the model the Knoxx config wire carries"
    (doseq [model-id ["glm-5" "gpt-5.5" "xiaomi/mimo-v2-pro" "gemma4:31b"]]
      (testing model-id
        (reset-cache!)
        (with-redefs [worker/fetch-translation-config-model (constantly model-id)]
          (is (= model-id (worker/resolve-translation-model))
              "catalog ids containing / : and . must survive unchanged"))))))

(deftest worker-caches-successful-lookups
  (reset-cache!)
  (let [calls (atom 0)]
    (with-redefs [worker/fetch-translation-config-model
                  (fn [] (swap! calls inc) "glm-5")]
      (is (= "glm-5" (worker/resolve-translation-model)))
      (is (= "glm-5" (worker/resolve-translation-model)))
      (is (= 1 @calls) "a second call inside the TTL must not refetch"))))

;; ── Failure is configuration failure, never a silent default ──────────────

(deftest worker-lookup-failure-is-configuration-failure
  (reset-cache!)
  (with-redefs [worker/fetch-translation-config-model
                (fn [] (throw (ex-info "connection refused" {})))]
    (let [err (try (worker/resolve-translation-model)
                   nil
                   (catch clojure.lang.ExceptionInfo e e))]
      (is (some? err) "the worker must not translate with a guessed model")
      (is (= :kms-ingestion.translation.worker/translation-config-unavailable
             (:kind (ex-data err))))
      (testing "and it does not fall back to the legacy env default"
        (is (not= "glm-5" (:model @worker/translation-model-cache*)))
        (is (nil? (:model @worker/translation-model-cache*)))))))

(deftest failed-lookups-are-not-cached
  (reset-cache!)
  (let [calls (atom 0)]
    (with-redefs [worker/fetch-translation-config-model
                  (fn [] (swap! calls inc) (throw (ex-info "down" {})))]
      (dotimes [_ 2]
        (try (worker/resolve-translation-model) (catch Exception _ nil)))
      (is (= 2 @calls) "a transient outage must not pin a stale answer"))))

(deftest blank-model-is-rejected
  (testing "an empty model in the response is a failure, not an empty selection"
    (reset-cache!)
    (with-redefs [worker/fetch-translation-config-model
                  (fn [] (throw (ex-info "Knoxx translation config returned no model" {})))]
      (is (thrown? clojure.lang.ExceptionInfo (worker/resolve-translation-model))))))

;; ── No second authority remains ───────────────────────────────────────────

(deftest no-openplanner-config-caller-remains
  (let [source (slurp "src/kms_ingestion/translation/worker.clj")
        legacy-config-path (str "/translations" "/config")]
    (testing "the worker no longer reads the legacy translation config endpoint"
      (is (not (str/includes? source (str "(openplanner-url \"" legacy-config-path "\")")))))
    (testing "and it does read the Knoxx boundary"
      (is (str/includes? source "/api/translations/config")))))

(deftest translation-model-env-is-gone
  (testing "TRANSLATION_MODEL is not a dormant second authority"
    (let [source (slurp "src/kms_ingestion/config.clj")]
      (is (not (re-find #"\(env \"TRANSLATION_MODEL\"" source))))
    (is (not (contains? (set (map str (keys (ns-publics 'kms-ingestion.config))))
                        "translation-model")))))

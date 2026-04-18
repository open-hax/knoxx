(ns kms-ingestion.db-epistemic-test
  "Integration tests for the epistemic db layer.
   These tests run against a real Postgres DB and are skipped when
   DATABASE_URL is not set (CI without a db service will skip gracefully)."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [kms-ingestion.db :as db]
            [kms-ingestion.epistemic :as ep]))

;; Only run when a real DB is available
(def ^:private db-available?
  (delay
    (try
      (when (System/getenv "DATABASE_URL")
        (db/init!)
        (db/ensure-tenant! "test-tenant" "Test Tenant")
        true)
      (catch Exception _ false))))

(defn- db-fixture [f]
  (if @db-available?
    (f)
    (println "[SKIP] DATABASE_URL not set — skipping DB integration tests")))

(use-fixtures :once db-fixture)

;; ---------------------------------------------------------------------------

(deftest insert-and-retrieve-fact
  (when @db-available?
    (testing "insert a fact and retrieve it by kind"
      (let [rec {:kind  :fact
                 :ctx   :己
                 :claim "integration test claim"
                 :src   "db-test"
                 :p     0.95
                 :time  (java.util.Date.)}
            _   (db/insert-epistemic-record! "test-tenant" rec)
            rows (db/query-epistemic "test-tenant" :kind :fact :limit 5)]
        (is (seq rows))
        (is (every? #(= "fact" (:kind %)) rows))))))

(deftest insert-and-retrieve-attestation
  (when @db-available?
    (testing "attestation carries actor provenance"
      (let [run-id (random-uuid)
            caused (random-uuid)
            rec {:kind     :attestation
                 :actor    :actor/test
                 :did      "sent-test-message"
                 :run-id   run-id
                 :causedby caused
                 :p        0.9}
            _   (db/insert-epistemic-record! "test-tenant" rec
                                             :actor-id    "actor/test"
                                             :contract-id "discord-patrol"
                                             :causedby    (str caused))
            row (first (db/query-epistemic "test-tenant" :kind :attestation :actor-id "actor/test" :limit 1))]
        (is row)
        (is (= "actor/test"    (:actor_id row)))
        (is (= "discord-patrol" (:contract_id row)))))))

(deftest batch-insert
  (when @db-available?
    (testing "batch insert returns one row per record"
      (let [records [{:kind :obs :ctx :己 :about "x" :signal {:raw 1} :p 0.8}
                     {:kind :obs :ctx :己 :about "y" :signal {:raw 2} :p 0.7}]
            rows    (db/insert-epistemic-records! "test-tenant" records)]
        (is (= 2 (count rows)))
        (is (every? #(= "obs" (:kind %)) rows))))))

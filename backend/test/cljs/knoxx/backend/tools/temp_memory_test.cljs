(ns knoxx.backend.tools.temp-memory-test
  (:require [knoxx.backend.infra.temp-memory :as temp]
            [promesa.core :as p]
            [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]))

;; ── parse-ttl-ms ─────────────────────────────

(deftest parse-iso-duration
  (testing "PT1H parses to 3600000"
    (is (= 3600000 (temp/parse-ttl-ms "PT1H"))))
  (testing "PT30M parses to 1800000"
    (is (= 1800000 (temp/parse-ttl-ms "PT30M"))))
  (testing "PT45S parses to 45000"
    (is (= 45000 (temp/parse-ttl-ms "PT45S"))))
  (testing "integer seconds works"
    (is (= 5000 (temp/parse-ttl-ms 5)))))

(deftest parse-iso-invalid
  (testing "blank string falls back to default"
    (is (= (* 60 60 1000) (temp/parse-ttl-ms ""))))
  (testing "nil falls back to default"
    (is (= (* 60 60 1000) (temp/parse-ttl-ms nil)))))

;; ── local fallback (no Redis) ─────────────────────────────

(deftest local-set-and-get
  (testing "set then get returns value"
    (p/let [_ (temp/mem-set! "test-key" "hello" {:ttl 60})
            v (temp/mem-get "test-key")]
      (is (= "hello" v))))
  (testing "get missing key returns nil"
    (p/let [v (temp/mem-get "missing-key")]
      (is (nil? v)))))

(deftest local-expiry
  (testing "expired key returns nil"
    (p/let [_ (temp/mem-set! "exp-key" "value" {:ttl 0.001})]
      (p/then (p/delay 10)
              (fn [_]
                (p/let [v (temp/mem-get "exp-key")]
                  (is (nil? v))))))))

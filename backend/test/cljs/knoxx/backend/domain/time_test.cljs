(ns knoxx.backend.domain.time-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.domain.time :as time]))

(deftest now-iso-returns-valid-iso-string
  (testing "returns a string matching ISO 8601 format"
    (let [result (time/now-iso)]
      (is (string? result))
      (is (re-matches #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z" result))
      (is (not (str/blank? result))))))

(ns knoxx.backend.agents.content-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.agents.content :refer [diff-appended-text]]))

(deftest diff-appended-text-test
  (testing "returns only novel suffix for cumulative and overlapping streams"
    (is (= "hello" (diff-appended-text "" "hello")))
    (is (= "" (diff-appended-text "hello" "hello")))
    (is (= " world" (diff-appended-text "hello" "hello world")))
    (is (= " again" (diff-appended-text "hello world" "world again"))))
  (testing "collapses Gemma/Ollama hosted duplicated first reasoning token"
    (is (= " model should reason once."
           (diff-appended-text "The" "TheThe model should reason once.")))
    (is (= " need one token."
           (diff-appended-text "We" "WeWe need one token."))))
  (testing "does not collapse intentional adjacent token continuation without a boundary"
    (is (= "ha" (diff-appended-text "ha" "haha")))))
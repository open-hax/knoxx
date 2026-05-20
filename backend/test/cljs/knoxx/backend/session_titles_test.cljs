(ns knoxx.backend.session-titles-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.stores.session-titles :as titles]))

(deftest sanitize-session-title
  (is (= "this is a very long text that exceeds the maximum allowed length for a session title so should be truncated"
         (titles/sanitize-session-title "this is a very long text that exceeds the maximum allowed length for a session title so should be truncated")))
  (is (nil? (titles/sanitize-session-title nil)))
  (is (nil? (titles/sanitize-session-title "   "))))

(deftest heuristic-session-title
  (is (= "test query" (titles/heuristic-session-title "# Test\n* query\n- here"))))

(deftest normalize-session-title
  (is (= "Valid Title" (titles/normalize-session-title "Valid Title" "fallback")))
  (is (nil? (titles/normalize-session-title "title" "chat"))))

(deftest session-title-seed-text
  (is (= "a b c d"
         (titles/session-title-seed-text [{:role "user" :text "a b"} {:role "user" :text "c d"}]))))

(deftest session-title-row-entry
  (let [result (titles/session-title-row-entry {:kind "knoxx.session_title" :text "Valid Title"})]
    (is (= "Valid Title" (:title result)))
    (is (nil? (:title_model result)))
    (is (string? (:updated_at result)))))

(deftest session-title-row-entry-parses-extra-json-at-extern-boundary
  (let [result (titles/session-title-row-entry
                {:extra "{\"kind\":\"knoxx.session_title\",\"title\":\"Extern Boundary\",\"title_model\":\"model-x\"}"})]
    (is (= "Extern Boundary" (:title result)))
    (is (= "model-x" (:title_model result)))))

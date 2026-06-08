(ns knoxx.frontend.admin.event-agent-utils-test
  "Real unit coverage for the pure logic already migrated out of DiscordSection.tsx.
   These functions had zero tests after migration — the loader-shim vitest tests
   only exercise the TS wrapper, not the CLJS logic. This is the first namespace
   under the new frontend :node-test build."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.frontend.admin.event-agent-utils :as sut]))

(deftest split-csv-trims-and-drops-empties
  (is (= ["a" "b" "c"] (sut/split-csv "a, b ,,c")))
  (is (= [] (sut/split-csv "")))
  (is (= ["x"] (sut/split-csv " x "))))

(deftest join-csv-roundtrips
  (is (= "a, b, c" (sut/join-csv ["a" "b" "c"])))
  (is (= "" (sut/join-csv nil)))
  (is (= ["a" "b"] (sut/split-csv (sut/join-csv ["a" "b"])))))

(deftest pretty-json-handles-nil
  (is (= "{}" (sut/pretty-json nil)))
  (is (= "{\n  \"a\": 1\n}" (sut/pretty-json {:a 1}))))

(deftest to-local-date-time-guards-invalid
  (is (= "—" (sut/to-local-date-time nil)))
  (is (= "—" (sut/to-local-date-time js/NaN)))
  (is (string? (sut/to-local-date-time 0)))
  (is (not= "—" (sut/to-local-date-time 1700000000000))))

(deftest runtime-for-job-matches-by-id
  (let [jobs [{:id "a" :status "ok"} {:id "b" :status "error"}]]
    (is (= {:id "b" :status "error"} (sut/runtime-for-job jobs "b")))
    (is (nil? (sut/runtime-for-job jobs "missing")))))

(deftest seed-json-drafts-shapes-each-job
  (let [drafts (sut/seed-json-drafts [{:id "j1"
                                       :source {:config {:x 1}}
                                       :filters {:k "v"}
                                       :agentSpec {:toolPolicies [{:toolId "t" :effect "allow"}]}}])]
    (is (= #{"j1"} (set (keys drafts))))
    (is (= #{:source-config :filters :tool-policies} (set (keys (drafts "j1")))))
    (is (= "{\n  \"x\": 1\n}" (get-in drafts ["j1" :source-config])))))

(deftest compact-text-normalizes-and-truncates
  (is (= "No description" (sut/compact-text "   ")))
  (is (= "No description" (sut/compact-text nil)))
  (is (= "a b c" (sut/compact-text "  a   b\n c ")))
  (testing "truncation appends an ellipsis at the max boundary"
    (let [out (sut/compact-text (apply str (repeat 200 "x")) 10)]
      (is (= 10 (count out)))
      (is (str/ends-with? out "…")))))

(deftest normalize-search-lowercases-and-trims
  (is (= "frankie" (sut/normalize-search "  FrAnKie "))))

(deftest job-search-text-blob-is-lowercased-and-skips-nils
  (let [text (sut/job-search-text {:id "J1"
                                   :name "Replies"
                                   :description nil
                                   :source {:kind "Discord" :mode "Event"}
                                   :trigger {:kind "Cron" :eventKinds ["discord.message"]}
                                   :agentSpec {:role "Creative" :model "Gemma"}
                                   :contractSourceId "src-1"})]
    (is (= text (str/lower-case text)))
    (is (str/includes? text "replies"))
    (is (str/includes? text "discord.message"))
    (is (str/includes? text "gemma"))
    (is (not (str/includes? text "null")))))

(deftest runtime-status-tone-maps-known-and-default
  (is (= :success (sut/runtime-status-tone "ok")))
  (is (= :danger (sut/runtime-status-tone "error")))
  (is (= :info (sut/runtime-status-tone "running")))
  (is (= :default (sut/runtime-status-tone "anything-else")))
  (is (= :default (sut/runtime-status-tone nil))))

(ns knoxx.frontend.admin.event-agent-utils-test
  "Real unit coverage for the pure logic already migrated out of DiscordSection.tsx.
   These functions had zero tests after migration — the loader-shim vitest tests
   only exercise the TS wrapper, not the CLJS logic. This is the first namespace
   under the new frontend :node-test build."
  (:require [cljs.test :as t]
            [clojure.string :as str]
            [knoxx.frontend.admin.event-agent-utils :as sut]))

(t/deftest split-csv-trims-and-drops-empties
  (t/is (= ["a" "b" "c"] (sut/split-csv "a, b ,,c")))
  (t/is (= [] (sut/split-csv "")))
  (t/is (= ["x"] (sut/split-csv " x "))))

(t/deftest join-csv-roundtrips
  (t/is (= "a, b, c" (sut/join-csv ["a" "b" "c"])))
  (t/is (= "" (sut/join-csv nil)))
  (t/is (= ["a" "b"] (sut/split-csv (sut/join-csv ["a" "b"])))))

(t/deftest pretty-json-handles-nil
  (t/is (= "{}" (sut/pretty-json nil)))
  (t/is (= "{\n  \"a\": 1\n}" (sut/pretty-json {:a 1}))))

(t/deftest to-local-date-time-guards-invalid
  (t/is (= "—" (sut/to-local-date-time nil)))
  (t/is (= "—" (sut/to-local-date-time js/NaN)))
  (t/is (string? (sut/to-local-date-time 0)))
  (t/is (not= "—" (sut/to-local-date-time 1700000000000))))

(t/deftest runtime-for-job-matches-by-id
  (let [jobs [{:id "a" :status "ok"} {:id "b" :status "error"}]]
    (t/is (= {:id "b" :status "error"} (sut/runtime-for-job jobs "b")))
    (t/is (nil? (sut/runtime-for-job jobs "missing")))))

(t/deftest seed-json-drafts-shapes-each-job
  (let [drafts (sut/seed-json-drafts [{:id "j1"
                                       :source {:config {:x 1}}
                                       :filters {:k "v"}
                                       :agentSpec {:toolPolicies [{:toolId "t" :effect "allow"}]}}])]
    (t/is (= #{"j1"} (set (keys drafts))))
    (t/is (= #{:source-config :filters :tool-policies} (set (keys (drafts "j1")))))
    (t/is (= "{\n  \"x\": 1\n}" (get-in drafts ["j1" :source-config])))))

(t/deftest compact-text-normalizes-and-truncates
  (t/is (= "No description" (sut/compact-text "   ")))
  (t/is (= "No description" (sut/compact-text nil)))
  (t/is (= "a b c" (sut/compact-text "  a   b\n c ")))
  (t/testing "truncation appends an ellipsis at the max boundary"
    (let [out (sut/compact-text (apply str (repeat 200 "x")) 10)]
      (t/is (= 10 (count out)))
      (t/is (str/ends-with? out "…")))))

(t/deftest normalize-search-lowercases-and-trims
  (t/is (= "frankie" (sut/normalize-search "  FrAnKie "))))

(t/deftest job-search-text-blob-is-lowercased-and-skips-nils
  (let [text (sut/job-search-text {:id "J1"
                                   :name "Replies"
                                   :description nil
                                   :source {:kind "Discord" :mode "Event"}
                                   :trigger {:kind "Cron" :eventKinds ["discord.message"]}
                                   :agentSpec {:role "Creative" :model "Gemma"}
                                   :contractSourceId "src-1"})]
    (t/is (= text (str/lower-case text)))
    (t/is (str/includes? text "replies"))
    (t/is (str/includes? text "discord.message"))
    (t/is (str/includes? text "gemma"))
    (t/is (not (str/includes? text "null")))))

(t/deftest runtime-status-tone-maps-known-and-default
  (t/is (= :success (sut/runtime-status-tone "ok")))
  (t/is (= :danger (sut/runtime-status-tone "error")))
  (t/is (= :info (sut/runtime-status-tone "running")))
  (t/is (= :default (sut/runtime-status-tone "anything-else")))
  (t/is (= :default (sut/runtime-status-tone nil))))

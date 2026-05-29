(ns knoxx.backend.extern-pg-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.pg :as pg]))

(defn- mock-pool [resolved-value]
  #js {:query (fn [_sql _params] (js/Promise.resolve resolved-value))})

(deftest ^:async query-keywordizes-single-statement-result
  (testing "single-statement query produces keywordized rows and row-count"
    (let [result (await (pg/query! (mock-pool #js {:rows #js [#js {"id" "abc" "name" "foo"}]
                                                    :rowCount 1})
                                   "SELECT id, name FROM things" nil))]
      (is (= [{:id "abc" :name "foo"}] (:rows result)))
      (is (= 1 (:row-count result))))))

(deftest ^:async query-handles-multi-statement-array-result
  ;; Regression: pg returns Array<QueryResult> when multiple statements are
  ;; submitted in one call (e.g. ensure-schema! DDL block). keywordize-rows
  ;; must not crash on result.rows being undefined on an Array.
  (testing "multi-statement result array resolves without TypeError"
    (let [ddl-result #js {:rows #js [] :rowCount nil}
          result (await (pg/query! (mock-pool #js [ddl-result ddl-result ddl-result])
                                   "CREATE TABLE IF NOT EXISTS a (id INT); CREATE TABLE IF NOT EXISTS b (id INT);" nil))]
      (is (map? result) "result is a CLJS map")
      (is (vector? (:rows result)) "rows is a vector")
      (is (empty? (:rows result)) "rows is empty for DDL"))))

(deftest ^:async query-one-returns-first-row
  (testing "query-one! extracts first keywordized row"
    (let [row (await (pg/query-one! (mock-pool #js {:rows #js [#js {"slug" "open-hax"}]
                                                     :rowCount 1})
                                    "SELECT slug FROM orgs LIMIT 1" nil))]
      (is (= {:slug "open-hax"} row)))))

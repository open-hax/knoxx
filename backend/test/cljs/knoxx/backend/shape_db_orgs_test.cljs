(ns knoxx.backend.shape-db-orgs-test
  (:require [cljs.test :refer [deftest is testing]]
            [honey.sql :as sql]
            [knoxx.backend.shape.db.orgs :as q-orgs]))

(deftest upsert-primary-excluded-references-are-qualified
  ;; Regression: [:excluded :name] generates EXCLUDED(name) (function-call
  ;; syntax), which makes bare `name` ambiguous in PostgreSQL's ON CONFLICT
  ;; DO UPDATE SET context. The fix uses [:raw "EXCLUDED.name"] so PostgreSQL
  ;; sees a proper qualified column reference.
  (testing "upsert-primary SQL uses EXCLUDED.col dot-syntax, not EXCLUDED(col)"
    (let [[sql-str] (sql/format (q-orgs/upsert-primary {:slug "open-hax"
                                                         :name "Open Hax"
                                                         :kind "org"})
                                {:numbered true})]
      (is (re-find #"EXCLUDED\.name" sql-str)
          (str "Expected EXCLUDED.name but got: " sql-str))
      (is (re-find #"EXCLUDED\.kind" sql-str)
          (str "Expected EXCLUDED.kind but got: " sql-str))
      (is (not (re-find #"EXCLUDED\(name\)" sql-str))
          "Must not generate function-call syntax EXCLUDED(name)")
      (is (not (re-find #"EXCLUDED\(kind\)" sql-str))
          "Must not generate function-call syntax EXCLUDED(kind)"))))

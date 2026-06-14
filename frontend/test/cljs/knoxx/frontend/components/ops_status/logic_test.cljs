(ns knoxx.frontend.components.ops-status.logic-test
  "Written FIRST (TDD) — sparkline path math, stat-sample extraction and
  windowing for the sidebar ops status (port of SidebarOpsStatus.tsx)."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.frontend.components.ops-status.logic :as logic]))

(deftest sparkline-path-contract
  (is (= "" (logic/sparkline-path [] 220 56)) "no values → empty path")
  (testing "single point starts with M"
    (is (re-find #"^M 0\.0" (logic/sparkline-path [50] 220 56))))
  (testing "two points span the width, M then L"
    (let [path (logic/sparkline-path [0 100] 200 50)]
      (is (re-find #"^M 0\.0 50\.0 L 200\.0 0\.0$" path)
        "min at bottom-left, max at top-right")))
  (testing "flat non-zero series stays within bounds"
    (let [path (logic/sparkline-path [50 50 50] 220 56)]
      (is (not (re-find #"NaN" path))))))

(deftest stats-payload->sample
  (let [sample (logic/stats->sample
                (clj->js {:cpu_percent 12.5 :memory_percent 40.25
                          :gpu [{:util_gpu 77} {:util_gpu 10}]})
                123456)]
    (is (= {:t 123456 :cpu 12.5 :ram 40.25 :gpu 77} sample)))
  (testing "missing fields default to zero"
    (is (= {:t 1 :cpu 0 :ram 0 :gpu 0}
           (logic/stats->sample #js {} 1)))
    (is (= 0 (:gpu (logic/stats->sample (clj->js {:gpu []}) 1))))))

(deftest sample-window-caps-at-50
  (let [many (vec (for [i (range 60)] {:t i :cpu i :ram 0 :gpu 0}))
        next-samples (logic/push-sample many {:t 60 :cpu 60 :ram 0 :gpu 0})]
    (is (= 50 (count next-samples)))
    (is (= 60 (:t (peek next-samples))))))

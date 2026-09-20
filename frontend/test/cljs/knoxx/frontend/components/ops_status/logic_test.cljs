(ns knoxx.frontend.components.ops-status.logic-test
  "Written FIRST (TDD) — sparkline path math, stat-sample extraction and
  windowing for the sidebar ops status (port of SidebarOpsStatus.tsx)."
  (:require [cljs.test :as t]
            [knoxx.frontend.components.ops-status.logic :as logic]))

(t/deftest sparkline-path-contract
  (t/is (= "" (logic/sparkline-path [] 220 56)) "no values → empty path")
  (t/testing "single point starts with M"
    (t/is (re-find #"^M 0\.0" (logic/sparkline-path [50] 220 56))))
  (t/testing "two points span the width, M then L"
    (let [path (logic/sparkline-path [0 100] 200 50)]
      (t/is (re-find #"^M 0\.0 50\.0 L 200\.0 0\.0$" path)
        "min at bottom-left, max at top-right")))
  (t/testing "flat non-zero series stays within bounds"
    (let [path (logic/sparkline-path [50 50 50] 220 56)]
      (t/is (not (re-find #"NaN" path))))))

(t/deftest stats-payload->sample
  (let [sample (logic/stats->sample
                (clj->js {:cpu_percent 12.5 :memory_percent 40.25
                          :gpu [{:util_gpu 77} {:util_gpu 10}]})
                123456)]
    (t/is (= {:t 123456 :cpu 12.5 :ram 40.25 :gpu 77} sample)))
  (t/testing "missing fields default to zero"
    (t/is (= {:t 1 :cpu 0 :ram 0 :gpu 0}
           (logic/stats->sample #js {} 1)))
    (t/is (= 0 (:gpu (logic/stats->sample (clj->js {:gpu []}) 1))))))

(t/deftest sample-window-caps-at-50
  (let [many (vec (for [i (range 60)] {:t i :cpu i :ram 0 :gpu 0}))
        next-samples (logic/push-sample many {:t 60 :cpu 60 :ram 0 :gpu 0})]
    (t/is (= 50 (count next-samples)))
    (t/is (= 60 (:t (peek next-samples))))))

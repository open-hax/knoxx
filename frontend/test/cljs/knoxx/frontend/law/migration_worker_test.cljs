(ns knoxx.frontend.law.migration-worker-test
  (:require [cljs.test :as t]
            [knoxx.frontend.law.migration-worker :as law]))

(t/deftest cljs-worker-references-need-explicit-inventory-support
  (let [facts {:path "frontend/src/cljs/fixture.cljs" :references []}]
    (t/is (= facts (law/assert-cljs-source! facts)))
    (t/is (thrown-with-msg? js/Error #"CLJS browser workers require explicit migration inventory support"
                           (law/assert-cljs-source! (assoc facts :references ["js/Worker"]))))))

(t/deftest browser-worker-values-require-an-inspected-use
  (doseq [usage [:constructor-call :registration-call :query-call :type-query]]
    (let [facts {:path "frontend/src/bridge/app.ts" :kind :worker :usage usage}]
      (t/is (= facts (law/assert-reference! facts)))))
  (t/is (thrown-with-msg? js/Error #"Browser worker reference is outside the supported migration grammar"
                         (law/assert-reference! {:path "frontend/src/bridge/app.ts" :kind :worker}))))

(t/deftest worker-sources-require-the-inspected-dependency-grammar
  (let [facts {:path "frontend/src/bridge/app.ts" :kind :service-worker :governed-url? true}]
    (t/is (= facts (law/assert-source! facts)))
    (t/is (thrown-with-msg? js/Error #"Worker source is outside the supported migration URL grammar"
                           (law/assert-source! (assoc facts :governed-url? false))))))

(ns knoxx.frontend.law.migration-html-test
  (:require [cljs.test :as t]
            [knoxx.frontend.law.migration-html :as law]))

(t/deftest only-the-existing-shadow-bootstrap-is-admitted
  (let [canonical {:source "/cljs/app.js" :html? true :inline-code? false}
        facts {:path "frontend/index.html" :scripts [canonical]}]
    (t/is (= facts (law/assert-entrypoint! facts)))
    (doseq [script [(assoc canonical :source "/legacy.js")
                    (assoc canonical :inline-code? true)
                    (assoc canonical :html? false)]]
      (t/is (thrown? js/Error (law/assert-entrypoint! (assoc facts :scripts [script])))))))

(t/deftest executable-html-attributes-need-inventory-support
  (doseq [field [:handlers :script-urls :base-hrefs]]
    (t/is (thrown? js/Error (law/assert-entrypoint! {:path "frontend/index.html" field [{}]})))))

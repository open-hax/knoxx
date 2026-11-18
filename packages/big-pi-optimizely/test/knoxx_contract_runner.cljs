(ns knoxx-contract-runner
  (:require [big-pi.optimizely.knoxx-contract-test]
            [cljs.test :as test]))
(defmethod test/report [::test/default :end-run-tests] [result]
  (when-not (test/successful? result) (set! (.-exitCode js/process) 1)))
(defn -main [& _]
  (test/run-tests 'big-pi.optimizely.knoxx-contract-test))

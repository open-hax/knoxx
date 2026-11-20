(ns runner
  "Run the standalone local CMS and bridge behavior contracts."
  (:require [big-pi.optimizely.bridge-test]
            [big-pi.optimizely.domain-test]
            [cljs.test :as test]))

(defmethod test/report [::test/default :end-run-tests] [result]
  (when-not (test/successful? result)
    (set! (.-exitCode js/process) 1)))

(defn -main
  "Execute all package behavior contracts and propagate failure to the shell."
  [& _arguments]
  (test/run-tests 'big-pi.optimizely.domain-test 'big-pi.optimizely.bridge-test))

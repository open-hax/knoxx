(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])
(shadow/with-runtime
  (let [config (edn/read-string (slurp "shadow-cljs.edn"))
        native? (= "1" (System/getenv "KNOXX_RECOVERY_NATIVE"))]
    (shadow/compile* (-> (get-in config [:builds :test-ci])
                         (assoc :build-id :recovery-handoff-proof
                                :output-to "target/recovery-handoff-proof/tests.cjs"
                                :ns-regexp (if native? "knoxx\\.backend\\.mongo-recovery-handoff-e2e$"
                                                       "knoxx\\.backend\\.extern\\.recovery-handoff-test$"))) {}))
  nil)

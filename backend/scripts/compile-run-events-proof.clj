(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])
(shadow/with-runtime
  (let [config (edn/read-string (slurp "shadow-cljs.edn"))
        native? (= "1" (System/getenv "KNOXX_RUN_EVENTS_NATIVE"))
        build (-> (get-in config [:builds :test-ci])
                  (assoc :build-id (if native? :run-events-native-proof :run-events-proof)
                         :output-to (if native? "target/run-events-native-proof/tests.cjs"
                                        "target/run-events-proof/tests.cjs")
                         :ns-regexp (if native? "knoxx\\.backend\\.mongo-run-events-e2e$"
                                      "knoxx\\.backend\\.((run-event-provider|run-queries-recovery|openplanner-session-store)-test|infra\\.stores\\.mongo-run-store-test|extern\\.models-run-routes-test)$")))]
    (shadow/compile* build {}))
  nil)

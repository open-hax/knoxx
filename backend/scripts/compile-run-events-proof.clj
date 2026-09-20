(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])
(shadow/with-runtime
  (let [config (edn/read-string (slurp "shadow-cljs.edn"))
        native? (= "1" (System/getenv "KNOXX_RUN_EVENTS_NATIVE"))
        build (-> (get-in config [:builds :test-ci])
                  (assoc :build-id (if native? :run-events-native-proof :run-events-proof)
                         :output-to (if native? "target/run-events-native-proof/tests.cjs"
                                        "target/run-events-proof/tests.cjs")
                         :ns-regexp (if native? "knoxx\\.backend\\.mongo-(run-events|thread-atomicity|event-chunks)-e2e$"
                                      "knoxx\\.backend\\.((mongo-event-chunks|run-event-provider|run-queries-recovery|openplanner-session-store|mongo-thread-cache|agent-run-persistence)-test|infra\\.stores\\.mongo-(run|session)-store-test|extern\\.(initial-admission-cleanup|startup-publication|async-spawn-durability|agent-control-persistence|cache-startup|turn-finalization|turn-sink-ownership|bootstrap-readiness|run-provider-startup|event-queue-admission|event-queue-retention)-test)$")))]
    (shadow/compile* build {}))
  nil)

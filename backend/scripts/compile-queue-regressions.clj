(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])
(try
  (shadow/with-runtime
    (let [config (edn/read-string (slurp "shadow-cljs.edn"))
          build (-> (get-in config [:builds :test-ci])
                    (assoc :build-id :queue-regressions
                           :output-to "target/queue-regressions/tests.cjs"
                           :ns-regexp "knoxx\\.backend\\.(extern\\.(event-queue|session-reclaim)-admission|agents\\.runner|infra\\.(translation-agent-dispatch|routes\\.document-admission-settlement))-test$"))]
      (shadow/compile* build {}))
    nil)
  (catch Throwable error
    (binding [*out* *err*] (println (ex-message error)))
    (binding [*warn-on-reflection* true] (System/exit 1))))

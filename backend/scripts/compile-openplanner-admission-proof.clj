(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])
(try
  (shadow/with-runtime
    (let [config (edn/read-string (slurp "shadow-cljs.edn"))
          build (-> (get-in config [:builds :test-ci])
                    (assoc :build-id :openplanner-admission-proof
                           :output-to "target/openplanner-admission-proof/tests.cjs"
                           :ns-regexp "knoxx\\.backend\\.(extern\\.openplanner-admission|infra\\.clients\\.openplanner-clio|openplanner-scope-recovery)-test$"))]
      (shadow/compile* build {}))
    nil)
  (catch Throwable error
    (binding [*out* *err*] (println (ex-message error)))
    (binding [*warn-on-reflection* true] (System/exit 1))))

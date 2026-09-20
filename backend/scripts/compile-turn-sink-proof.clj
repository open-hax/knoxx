(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])
(shadow/with-runtime
  (let [config (edn/read-string (slurp "shadow-cljs.edn"))
        build (-> (get-in config [:builds :test-ci])
                  (assoc :build-id :turn-sink-proof
                         :output-to "target/turn-sink-proof/tests.cjs"
                         :ns-regexp "knoxx\\.backend\\.(extern\\.(turn-sink-ownership|turn-finalization|hydration-publication|initial-admission-cleanup)-test|agent-(turn-timeout|turn-admission|run-persistence)-test)$"))]
    (shadow/compile* build {}))
  nil)

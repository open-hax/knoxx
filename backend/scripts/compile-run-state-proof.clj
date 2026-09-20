(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])
(shadow/with-runtime
  (let [config (edn/read-string (slurp "shadow-cljs.edn"))
        build (-> (get-in config [:builds :test-ci])
                  (assoc :build-id :run-state-proof
                         :output-to "target/run-state-proof/tests.cjs"
                         :ns-regexp "knoxx\\.backend\\.run-state(?:-retention)?-test$"))]
    (shadow/compile* build {}))
  nil)

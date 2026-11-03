(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])
(shadow/with-runtime
  (let [config (edn/read-string (slurp "shadow-cljs.edn"))
        build (-> (get-in config [:builds :test-ci])
                  (assoc :build-id :resources-recovery
                         :output-to "target/resources-recovery/tests.cjs"
                         :ns-regexp "knoxx\\.backend\\.(contracts-routes-test|extern\\.resource-watcher-test)$"))]
    (shadow/compile* build {}))
  nil)

(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])
(shadow/with-runtime
  (let [config (edn/read-string (slurp "shadow-cljs.edn"))
        build (-> (get-in config [:builds :test-ci])
                  (assoc :build-id :memory-proof
                         :output-to "target/memory-proof/tests.cjs"
                         :ns-regexp "knoxx\\.backend\\.((extern\\.)?memory-routes|extern\\.discord-tools|extern-discord)-test$"))]
    (shadow/compile* build {}))
  nil)

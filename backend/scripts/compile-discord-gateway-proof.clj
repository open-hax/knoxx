(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])
(shadow/with-runtime
  (let [config (edn/read-string (slurp "shadow-cljs.edn"))
        build (-> (get-in config [:builds :test-ci])
                  (assoc :build-id :discord-gateway-proof
                         :output-to "target/discord-gateway-proof/tests.cjs"
                         :ns-regexp "knoxx\\.backend\\.extern\\.discord-gateway-test$"))]
    (shadow/compile* build {}))
  nil)

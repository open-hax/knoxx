(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])
(try
  (shadow/with-runtime
    (let [config (edn/read-string (slurp "shadow-cljs.edn"))
          build (-> (get-in config [:builds :test-ci])
                    (assoc :build-id :mcp-http-provider-proof
                           :output-to "target/mcp-http-provider-proof/tests.cjs"
                           :ns-regexp "knoxx\\.backend\\.mcp-http-test$"))]
      (shadow/compile* build {}))
    nil)
  (catch Throwable error
    (binding [*out* *err*] (println (ex-message error)))
    (binding [*warn-on-reflection* true] (System/exit 1))))

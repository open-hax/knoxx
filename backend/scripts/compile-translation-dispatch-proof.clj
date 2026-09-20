(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])
(shadow/with-runtime
  (let [config (edn/read-string (slurp "shadow-cljs.edn"))
        build (-> (get-in config [:builds :test-ci])
                  (assoc :build-id :translation-dispatch-proof
                         :output-to "target/translation-dispatch-proof/tests.cjs"
                         :ns-regexp "knoxx\\.backend\\.infra\\.translation-dispatch(?:-(?:completion|observation|recovery))?-test$"))]
    (shadow/compile* build {}))
  nil)

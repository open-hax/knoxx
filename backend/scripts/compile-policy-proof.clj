(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])
(shadow/with-runtime
  (let [config (edn/read-string (slurp "shadow-cljs.edn"))
        build (-> (get-in config [:builds :test-ci])
                  (assoc :build-id :policy-proof
                         :output-to "target/policy-proof/tests.cjs"
                         :ns-regexp "knoxx\\.backend\\.((admin-credential-routes|policy-db-credentials|policy-db-sync|provider-recovery|local-policy-api-recovery)-test|law\\.policy-values-test)$"))]
    (shadow/compile* build {}))
  nil)

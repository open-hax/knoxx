(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])

(shadow/with-runtime
  (let [config (edn/read-string (slurp "shadow-cljs.edn"))
        build (-> (get-in config [:builds :test-ci])
                  (assoc :build-id :startup-admission-native-proof
                         :output-to "target/startup-admission-native-proof/tests.cjs"
                         :ns-regexp "knoxx\\.backend\\.mongo-startup-admission-e2e$"))]
    (shadow/compile* build {}))
  nil)

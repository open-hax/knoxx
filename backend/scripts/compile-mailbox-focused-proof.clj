(require '[clojure.edn :as edn] '[shadow.cljs.devtools.api :as shadow])
(shadow/with-runtime
  (let [config (edn/read-string (slurp "shadow-cljs.edn"))
        build (-> (get-in config [:builds :test-ci])
                  (assoc :build-id :mailbox-focused-proof
                         :output-to "target/mailbox-focused-proof/tests.cjs"
                         :ns-regexp "knoxx\\.backend\\.infra\\.stores\\.clio-mailbox-store-test$"))]
    (shadow/compile* build {})) nil)

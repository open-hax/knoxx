(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])

(try
  (shadow/with-runtime
    (let [config (edn/read-string (slurp "shadow-cljs.edn"))
          build (-> (get-in config [:builds :test-ci])
                    (assoc :build-id :source-authority-proof
                           :output-to "target/source-authority-proof/tests.cjs"
                           :ns-regexp "^knoxx\\.backend\\.(domain\\.source-review-test|domain\\.source-authoring-test|infra\\.clio-application-store-test|source-recovery-test|source-acceptance-test)$"))]
      (shadow/compile* build {}))
    nil)
  (catch Throwable error
    (binding [*out* *err*] (println (ex-message error)))
    (System/exit 1)))

(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])
(try
 (shadow/with-runtime
  (let [laws? (= ["--laws"] (vec *command-line-args*))
        _ (when (and (seq *command-line-args*) (not laws?))
            (throw (ex-info "Only --laws or no arguments are supported" {})))
        config (edn/read-string (slurp "shadow-cljs.edn"))
        build (-> (get-in config [:builds :test-ci])
                  (assoc :build-id (if laws? :translation-scope-laws :translation-scope-proof)
                         :output-to (if laws? "target/translation-scope-laws/tests.cjs"
                                        "target/translation-scope-proof/tests.cjs")
                         :ns-regexp (if laws? "knoxx\\.backend\\.law\\.(event-scope|translation-agent|translation-dispatch)-test$"
                                    "knoxx\\.backend\\.(extern\\.translation-trigger-scope-test|law\\.(event-scope|translation-agent|translation-dispatch)-test|actions\\.start-agent-session-test|triggers\\.scope-status-test)$")))]
    (shadow/compile* build {}))
  nil)
 (catch Throwable error
   (binding [*out* *err*] (println (ex-message error)))
   (binding [*warn-on-reflection* true] (System/exit 1)))
 (finally (shutdown-agents)))

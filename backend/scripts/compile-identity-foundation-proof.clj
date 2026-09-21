(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])

(try
  (shadow/with-runtime
    (let [config (edn/read-string (slurp "shadow-cljs.edn"))
          build (-> (get-in config [:builds :test-ci])
                    (assoc :build-id :identity-foundation-proof
                           :output-to "target/identity-foundation-proof/tests.cjs"
                           :ns-regexp "^knoxx\\.backend\\.(domain\\.local-policy-profile-test|infra\\.(identity-membership-bindings|clio-policy-foundation|local-policy-foundation)-test)$"))]
      (shadow/compile* build {}))
    nil)
  (catch Throwable error
    (binding [*out* *err*] (println (ex-message error)))
    (System/exit 1)))

(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])
(try
  (shadow/with-runtime
    (let [config (edn/read-string (slurp "shadow-cljs.edn"))
          native? (= "1" (System/getenv "KNOXX_POLICY_NATIVE_PROOF"))
          selector (str "^knoxx\\.backend\\.("
                        "(admin-credential-routes|policy-db-credentials|policy-db-sync|mongo-policy-mutations|mongo-policy-invites)-test"
                        "|law\\.policy-values-test|domain\\.local-policy-profile-test"
                        "|infra\\.(identity-membership-bindings|clio-policy-foundation|local-policy-foundation)-test"
                        "|infra\\.stores\\.mongo-policy-directory-test"
                        (when native? "|(mongo-policy-route-compatibility|mongo-policy-invites|mongo-actor-coordinate|mongo-legacy-auth)-e2e") ")$")
          build (assoc (get-in config [:builds :test-ci]) :build-id :policy-proof
                       :output-to "target/policy-proof/tests.cjs" :ns-regexp selector)]
      (shadow/compile* build {}))
    nil)
  (catch Throwable error
    (binding [*out* *err*] (println (ex-message error)))
    (System/exit 1)))

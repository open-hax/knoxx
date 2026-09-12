(ns knoxx.backend.auth-session-test
  (:require [axxium.domain.identity :as identity-domain]
            [axxium.infra.identity :as axxium]
            [axxium.infra.identity-store :as axxium-store]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.axxium :as transport]
            [knoxx.backend.extern.identity-fixture :as fixture]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.auth.session :as session]
            [knoxx.backend.infra.identity :as identity]
            [knoxx.backend.infra.identity-bootstrap :as bootstrap]
            [knoxx.backend.infra.identity-bindings :as bindings]))

(defn- ^:async open-context! [directory options]
  (with-redefs [transport/options (fn [_] {:provider :edn :directory (str directory "/identity")
                                           :public-base-url "http://localhost"})
                transport/bootstrap-options (fn [options]
                                              {:username "admin" :email (:bootstrapSystemAdminEmail options)
                                               :password (:bootstrapSystemAdminPassword options)
                                               :principal-id "fixed-test-administrator"})]
    (await (bootstrap/create-context! {:policy-provider :edn :wiki-directory directory} options))))

(defn- ^:async refused [operation]
  (try (await (operation)) nil
       (catch :default error (or (:status (ex-data error)) (:code (ex-data error))))))

(defn- signup! [context username]
  (axxium/signup! (:axxium-service context) {:username username :email (str username "@example.test")
                                            :password "correct horse battery staple"}))

(deftest ^:async active-identity-binds-by-id-and-replays-without-email-authority
  (let [directory (fixture/directory!)]
    (try
      (let [context (await (open-context! directory {}))
            created (await (signup! context "alice"))
            token (:token created)
            request (fixture/request token {"x-knoxx-user-email" "admin@example.test"
                                            "x-knoxx-membership-id" "admin-member"} "GET")
            first-context (await (session/resolve-auth-context request context))
            reopened (await (open-context! directory {}))
            next-context (await (session/resolve-auth-context request reopened))]
        (is (= "alice@example.test" (get-in first-context [:user :email])))
        (is (= (:principal created) (:axxium-principal first-context)))
        (is (= (:identity-binding first-context) (:identity-binding next-context)))
        (is (= ["basic-user"] (:role-slugs first-context)))
        (is (not (authz/system-admin? first-context)))
        (is (= 401 (await (refused #(session/resolve-auth-context
                                     (fixture/request nil {"x-knoxx-user-email" "alice@example.test"} "GET") reopened)))))
        (is (= 401 (await (refused #(session/resolve-auth-context
                                     (fixture/request nil {"x-api-key" "pretend-admin"} "GET") reopened)))))
        (is (= 401 (await (refused #(session/ensure-user-membership! reopened nil "alice@example.test")))))
        (is (= 409 (await (refused #(bindings/bind! (:identity-bindings reopened)
                                                    (assoc (:identity-binding first-context) :org-id "another-org"))))))
        (axxium/logout! (:axxium-service reopened) token)
        (is (= 401 (await (refused #(identity/current-context! reopened first-context)))))
        (is (= 401 (await (refused #(session/resolve-auth-context request reopened))))))
      (finally (fixture/remove! directory)))))

(deftest ^:async mixed-malformed-and-foreign-origin-credentials-fail-closed
  (let [directory (fixture/directory!)]
    (try
      (let [context (await (open-context! directory {}))
            token (:token (await (signup! context "alice")))]
        (doseq [header ["Basic malformed" "Bearer " (str "Bearer " token) "Bearer someone-else"]]
          (is (= 401 (await (refused #(session/resolve-auth-context
                                       (fixture/request token {"authorization" header} "GET") context))))))
        (doseq [headers [{} {"origin" "https://attacker.example"}]]
          (is (= 403 (await (refused #(session/resolve-auth-context
                                       (fixture/request token headers "POST") context))))))
        (is (= "alice" (get-in (await (session/resolve-auth-context
                                       (fixture/request token {"origin" "http://localhost"} "POST") context))
                                [:user :username]))))
      (finally (fixture/remove! directory)))))

(deftest ^:async principal-and-session-revocation-during-policy-hydration-is-rechecked
  (let [directory (fixture/directory!)]
    (try
      (let [context (await (open-context! directory {}))
            created (await (signup! context "alice"))
            token (:token created)
            hydrate identity/principal-context!]
        (with-redefs [identity/principal-context!
                      (fn ^:async revoke-after-hydration [ctx principal]
                        (let [result (await (hydrate ctx principal))]
                          (axxium/logout! (:axxium-service ctx) token)
                          result))]
          (is (= 401 (await (refused #(session/resolve-auth-context (fixture/request token) context))))))
        (let [token (:token (await (axxium/login! (:axxium-service context)
                                                 {:identifier "alice" :password "correct horse battery staple"})))
              principal (:principal created)]
          (axxium-store/transact! (get-in context [:axxium-service :store])
                                  (fn [_] {:operation :fixture-suspend
                                           :changes [(identity-domain/put :principals (:principal/id principal)
                                                                          (assoc principal :principal/status :suspended))]}))
          (is (= 401 (await (refused #(session/resolve-auth-context (fixture/request token) context)))))))
      (finally (fixture/remove! directory)))))

(deftest ^:async explicit-bootstrap-is-durable-and-never-promotes-a-signup-collision
  (let [directory (fixture/directory!)
        options {:bootstrapSystemAdminEmail "admin@example.test"
                 :bootstrapSystemAdminPassword "correct administrator fixture password"}]
    (try
      (let [context (await (open-context! directory options))
            login (await (axxium/login! (:axxium-service context)
                                        {:identifier "admin" :password (:bootstrapSystemAdminPassword options)}))
            ctx (await (session/resolve-auth-context (fixture/request (:token login)) context))]
        (is (authz/system-admin? ctx))
        (is (= "fixed-test-administrator" (get-in ctx [:axxium-principal :principal/id])))
        (is (:axxium-service (await (open-context! directory options))))
        (is (= :bootstrap-mismatch (await (refused #(open-context! directory
                                                    (assoc options :bootstrapSystemAdminPassword "changed administrator fixture password")))))))
      (finally (fixture/remove! directory))))
  (let [directory (fixture/directory!)]
    (try
      (let [context (await (open-context! directory {}))]
        (await (signup! context "admin"))
        (is (= :bootstrap-collision (await (refused #(open-context! directory
                                                     {:bootstrapSystemAdminEmail "admin@example.test"
                                                      :bootstrapSystemAdminPassword "correct administrator fixture password"}))))))
      (finally (fixture/remove! directory)))))

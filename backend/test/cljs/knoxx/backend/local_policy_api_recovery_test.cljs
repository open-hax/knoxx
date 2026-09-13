(ns knoxx.backend.local-policy-api-recovery-test
  "Real Axxium and Clio facade proofs with no Mongo connection or fixture identities."
  (:require [axxium.domain.identity :as identity-domain]
            [axxium.infra.identity :as axxium]
            [axxium.infra.identity-store :as axxium-store]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.axxium :as transport]
            [knoxx.backend.extern.identity-fixture :as fixture]
            [knoxx.backend.infra.auth.session :as session]
            [knoxx.backend.infra.db.policy :as policy]
            [knoxx.backend.infra.identity-bootstrap :as bootstrap]
            [knoxx.backend.infra.identity-bindings :as bindings]
            [knoxx.backend.infra.mongo-client :as mongo-client]
            [knoxx.backend.infra.routes.users.admin :as routes]))

(def ^:private password "correct administrator fixture password")

(defn- ^:async open! [directory]
  (with-redefs [transport/options (fn [_] {:provider :edn :directory (str directory "/identity")
                                          :public-base-url "http://localhost"})
                transport/bootstrap-options (fn [_] {:username "operator" :email "operator@example.test"
                                                     :password password :principal-id "policy-test-administrator"})]
    (let [context (await (bootstrap/create-context! {:policy-provider :edn :wiki-directory directory} {}))
          login (await (axxium/login! (:axxium-service context) {:identifier "operator" :password password}))
          verified (await (session/resolve-auth-context (fixture/request (:token login)) context))]
      {:context context :acting (policy/acting-context context verified) :token (:token login)})))

(defn- ^:async refused [f]
  (try (await (f)) nil (catch :default error (:status (ex-data error)))))

(defn- ^:async signup! [context username]
  (await (axxium/signup! (:axxium-service context)
                         {:username username :email (str username "@example.test") :password password})))

(defn- suspend! [context principal]
  (axxium-store/transact! (get-in context [:axxium-service :store])
                          (fn [_] {:operation :fixture-suspend
                                   :changes [(identity-domain/put :principals (:principal/id principal)
                                                                  (assoc principal :principal/status :suspended))]})))

(deftest ^:async directory-facade-runs-without-mongo-and-replays-profile-state
  (let [directory (fixture/directory!)]
    (try
      (let [{:keys [acting]} (await (open! directory))
            pool (policy/context-pool acting)]
        (is (identical? acting pool))
        (is (policy/configured? acting))
        (with-redefs [policy/db! (fn [] (throw (ex-info "Mongo must never be reached" {})))
                      mongo-client/init-mongo! (fn [] (throw (ex-info "Mongo connection must never be reached" {})))]
          (let [org (:org (await (policy/create-org-for-context! acting {:name "Editorial"})))
                created (await (policy/create-user-for-context! acting {:org-id (:id org) :display-name "Research agent"}))
                id (get-in created [:user :id])]
            (is (false? (get-in created [:user :identityBound])))
            (is (true? (get-in created [:user :identityEnrollmentRequired])))
            (is (nil? (get-in created [:user :principalId])))
            (await (policy/update-user-actor-for-context! acting id {:org-id (:id org) :display-name "Editor"}))
            (is (= "Editor" (get-in (await (policy/list-users! pool {:org-id (:id org)})) [:users 0 :displayName])))
            (is (= 1 (count (:memberships (await (policy/list-memberships! pool {:org-id (:id org)}))))))
            (is (seq (:orgs (await (policy/list-orgs! pool)))))
            (is (map? (await (policy/bootstrap-context! acting))))
            (is (vector? (:permissions (await (policy/list-permissions! pool)))))
            (is (vector? (:tools (await (policy/list-tools! pool)))))
            (is (vector? (:roles (await (policy/list-roles! pool {:org-id (:id org)})))) )
            (is (empty? (:data-lakes (await (policy/list-data-lakes! pool {:org-id (:id org)})))))
            (let [reopened (:acting (await (open! directory)))]
              (is (= "Editor" (get-in (await (policy/list-users! (policy/context-pool reopened) {:org-id (:id org)}))
                                        [:users 0 :displayName])))))))
      (finally (fixture/remove! directory)))))

(deftest ^:async explicit-principal-membership-preserves-canonical-login-and-rejects-alias-mutation
  (let [directory (fixture/directory!)]
    (try
      (let [{:keys [context acting]} (await (open! directory))
            created (await (signup! context "alice"))
            verified (await (session/resolve-auth-context (fixture/request (:token created)) context))
            canonical (:identity-binding verified)
            org (:org (await (policy/create-org-for-context! acting {:name "Second organization"})))
            payload {:org-id (:id org) :axxium-principal-id (get-in created [:principal :principal/id])}
            result (await (policy/create-user-for-context! acting payload))
            id (get-in result [:user :id])]
        (is (= (:user-id canonical) id))
        (is (true? (get-in result [:user :identityBound])))
        (is (false? (get-in result [:user :identityEnrollmentRequired])))
        (is (= "alice@example.test" (get-in result [:user :email])))
        (is (not= (:membership-id canonical) (get-in result [:membership :id])))
        (is (= canonical (await (bindings/read! (:identity-bindings context) (:principal-id canonical)))))
        (is (= canonical (:identity-binding (await (session/resolve-auth-context (fixture/request (:token created)) context)))))
        (is (= 409 (await (refused #(policy/create-user-for-context! acting payload)))))
        (is (= 403 (await (refused #(policy/update-user-actor-for-context! acting id {:org-id (:id org) :actor-id "another-principal"})))) )
        (is (= 400 (await (refused #(policy/update-user-actor-for-context! acting id
                                    (#'routes/actor-update-payload {:orgId (:id org) :email "replacement@example.test"}))))))
        (await (policy/update-user-actor-for-context! acting id {:org-id (:id org) :display-name "Project editor"}))
        (let [updated (first (:users (await (policy/list-users! (policy/context-pool acting) {:org-id (:id org)}))))]
          (is (= "Project editor" (:displayName updated)))
          (is (= "alice@example.test" (:email updated)))) )
      (finally (fixture/remove! directory)))))

(deftest ^:async caller-injected-principal-maps-cannot-bind-an-identity
  (let [directory (fixture/directory!)]
    (try
      (let [{:keys [context acting]} (await (open! directory))
            created (await (signup! context "alice"))
            org-id (get-in context [:primary-org :id])
            result (await (policy/create-user-for-context! acting {:org-id org-id :display-name "Unbound"
                                                                   :verified-principal (:principal created)
                                                                   :verified-binding {:user-id "operator"}}))]
        (is (false? (get-in result [:user :identityBound])))
        (is (= 404 (await (refused #(policy/create-user-for-context! acting
                                    {:org-id org-id :axxium-principal-id "missing-principal"})))) )
        (suspend! context (:principal created))
        (is (= 404 (await (refused #(policy/create-user-for-context! acting
                                    {:org-id org-id :axxium-principal-id (get-in created [:principal :principal/id])}))))))
      (finally (fixture/remove! directory)))))

(deftest ^:async unbound-server-context-and-revoked-session-never-inherit-bootstrap-authority
  (let [directory (fixture/directory!)]
    (try
      (let [{:keys [context acting token]} (await (open! directory))]
        (is (nil? (policy/context-actor-user-id context)))
        (is (= 401 (await (refused #(policy/list-orgs! (policy/context-pool context))))))
        (is (= 401 (await (refused #(policy/create-org-for-context! context {:name "Forbidden"})))) )
        (axxium/logout! (:axxium-service context) token)
        (is (= 401 (await (refused #(policy/list-orgs! (policy/context-pool acting))))))
        (is (= 401 (await (refused #(policy/create-org-for-context! acting {:name "Still forbidden"}))))))
      (finally (fixture/remove! directory)))))

(deftest ^:async removing-current-membership-authority-blocks-the-next-facade-operation
  (let [directory (fixture/directory!)]
    (try
      (let [{:keys [acting]} (await (open! directory))
            member-id (policy/context-actor-membership-id acting)
            org-id (get-in acting [:acting-context :org :id])]
        (await (policy/set-membership-roles-for-context! acting member-id {:org-id org-id :role-ids [] :role-slugs [] :replace true}))
        (is (= 403 (await (refused #(policy/list-orgs! (policy/context-pool acting))))))
        (is (= 403 (await (refused #(policy/create-org-for-context! acting {:name "Removed authority"}))))))
      (finally (fixture/remove! directory)))))

(deftest ^:async service-credential-lookup-rechecks-active-owner-and-exact-membership
  (let [directory (fixture/directory!)]
    (try
      (let [{:keys [context acting]} (await (open! directory))
            created (await (signup! context "alice"))
            verified (await (session/resolve-auth-context (fixture/request (:token created)) context))
            org-id (get-in verified [:org :id])
            id (get-in verified [:user :id])
            scope {:org-id org-id :membership-id (get-in verified [:membership :id])}
            actor-id (get-in verified [:actor :id])
            result (await (policy/upsert-actor-credential-for-context! acting id
                            {:org-id org-id :provider "test-service" :secret-json {:token "local-test-secret"}}))]
        (is (nil? (get-in result [:credential :secretJson])))
        (is (= "local-test-secret" (get-in (await (policy/get-actor-credential! context actor-id "test-service" scope))
                                          [:credential :secretJson :token])))
        (is (nil? (:credential (await (policy/get-actor-credential! context actor-id "test-service" (assoc scope :membership-id "foreign"))))))
        (await (policy/update-user-actor-for-context! acting id {:org-id org-id :membership-status "disabled"}))
        (is (nil? (:credential (await (policy/get-actor-credential! context actor-id "test-service" scope)))))
        (await (policy/update-user-actor-for-context! acting id {:org-id org-id :membership-status "active"}))
        (suspend! context (:principal created))
        (is (nil? (:credential (await (policy/get-actor-credential! context actor-id "test-service" scope)))))
        (is (empty? (:credentials (await (policy/list-actor-credentials! (policy/context-pool context) "test-service"))))))
      (finally (fixture/remove! directory)))))

(deftest profile-payload-preserves-omitted-fields-and-explicit-removals
  (is (= {:org-id "org" :display-name "Name"}
         (#'routes/actor-update-payload {:orgId "org" :displayName "Name"})))
  (is (= {:org-id "org" :role-slugs [] :tool-policies [] :status "disabled"}
         (#'routes/actor-update-payload {:orgId "org" :roleSlugs [] :toolPolicies [] :status "disabled"})))
  (is (= [:authProvider] (:identity-fields (#'routes/actor-update-payload {:authProvider "forged"})))) )

(ns knoxx.backend.infra.local-policy-foundation-test
  "Fresh selected-provider authorization using the pinned Axxium service directly."
  (:require [axxium.infra.identity :as axxium]
            [clio.extern.js.fs :as fs]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.clio-store-fixture :as fixture]
            [knoxx.backend.infra.clio-application-store :as application]
            [knoxx.backend.infra.clio-policy-store :as policy]
            [knoxx.backend.infra.identity-bindings :as bindings]
            [knoxx.backend.infra.local-policy-api :as api]))

(def ^:private password "foundation fixture administrator password")

(defn- binding-for [principal]
  {:principal-id (:principal/id principal) :entity-id (:principal/entity-id principal)
   :kind (:principal/kind principal) :user-id "operator-user" :membership-id "operator-member"
   :org-id "home" :actor-id (:principal/id principal)})

(defn- refreshed [service token]
  (when-let [principal (axxium/resolve-principal service token)]
    {:axxium-principal principal :user {:id "operator-user"} :membership {:id "operator-member"}}))

(defn- ^:async with-context! [run!]
  (let [directory (fixture/temp-directory!)]
    (try
      (let [service (axxium/open! {:provider :edn :directory (str directory "/identity")
                                   :public-base-url "http://localhost"})
            principal (await (axxium/bootstrap! service {:username "operator" :email "operator@example.test"
                                                         :password password :principal-id "foundation-operator"}))
            token (:token (await (axxium/login! service {:identifier "operator" :password password})))
            store (policy/open! {:directory (str directory "/policy")})
            binding-store (bindings/open! {:directory (str directory "/bindings")})
            binding (binding-for principal)
            verified (assoc (refreshed service token) :identity/refresh! #(refreshed service token))
            context {:axxium-service service :clio-policy-store store :identity-bindings binding-store}]
        (await (policy/initialize! store {:org {:id "home" :slug "home" :name "Home" :status "active"}
                                         :roles {"system-admin" {:permissions [] :tool-policies []}} :tools []}))
        (await (bindings/bind! binding-store binding))
        (await (policy/observe! store binding principal))
        (await (run! {:context context :acting (api/acting-context context verified)
                     :service service :token token :store store})))
      (finally (fs/remove-tree! directory)))))

(defn- ^:async status [run!]
  (try (await (run!)) nil (catch :default cause (:status (ex-data cause)))))

(deftest ^:async revoked-session-refuses-the-next-read-and-command
  (await (with-context!
          (^:async fn [{:keys [context acting service token store]}]
            (is (api/selected? context))
            (is (= 1 (count (:orgs (await (api/read! acting :policy/orgs []))))))
            (is (= "Editorial" (get-in (await (api/command! acting :policy/create-org [{:name "Editorial"}])) [:org :name])))
            (let [before (application/history (:engine store))]
              (is (= 401 (await (status #(api/read! context :policy/orgs [])))))
              (axxium/logout! service token)
              (is (= 401 (await (status #(api/read! acting :policy/orgs [])))))
              (is (= 401 (await (status #(api/command! acting :policy/create-org [{:name "Revoked"}])))))
              (is (= before (application/history (:engine store)))))))))

(deftest ^:async current-directory-revocation-and-delegation-cannot-use-cached-authority
  (await (with-context!
          (^:async fn [{:keys [acting store]}]
            (let [refresh (get-in acting [:acting-context :identity/refresh!])
                  delegated (assoc-in acting [:acting-context :identity/refresh!]
                                      #(assoc (refresh) :identity/delegated? true))]
              (is (= 403 (await (status #(api/read! delegated :policy/orgs []))))))
            (await (api/command! acting :policy/membership-roles
                                 ["operator-member" {:org-id "home" :role-ids [] :replace true}]))
            (let [before (application/history (:engine store))]
              (is (= 403 (await (status #(api/read! acting :policy/orgs [])))))
              (is (= 403 (await (status #(api/command! acting :policy/create-org [{:name "Forbidden"}])))))
              (is (= before (application/history (:engine store)))))))))

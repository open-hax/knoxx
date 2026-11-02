(ns knoxx.backend.infra.db.policy.role-queries
  "Provider-selected role and permission directory queries and creation."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.local-policy-api :as local-api]
            [knoxx.backend.infra.stores.mongo-policy-roles :as mongo-roles]
            [knoxx.backend.infra.stores.mongo-policy-tools :as mongo-tools]
            [knoxx.backend.domain.contracts.roles :as contracts-roles]
            [knoxx.backend.infra.db.policy.support :as policy-support]
            [knoxx.backend.law.policy-values :as values]
            [knoxx.backend.infra.db.policy.connection :as policy-connection]
            [knoxx.backend.infra.db.policy.roles :as policy-roles]
            [knoxx.backend.infra.db.policy.hydration :as policy-hydration]))

(defn list-permissions!
  [pool]
  (if (local-api/selected? pool)
    (local-api/read! pool :policy/permissions [])
    (let [codes (->> (contracts-roles/list-role-slugs (policy-support/contracts-config))
                   (mapcat #(contracts-roles/role-permissions (policy-support/contracts-config) %))
                   distinct sort vec)]
    (js/Promise.resolve
     {:permissions (mapv (fn [c]
                           {:id           c
                            :code         c
                            :resourceKind (first (str/split c #"\."))
                            :description  ""})
                         codes)}))))

(defn- tool-row->map
  [{:keys [id label description risk_level]}]
  {:id id :label label :description description :risk-level risk_level})

(defn ^:async list-tools!
  [pool]
  (if (local-api/selected? pool)
    (local-api/read! pool :policy/tools [])
    (let [rows (await (mongo-tools/list-tools! (await (policy-connection/db!))))]
    {:tools (mapv tool-row->map rows)})))

(defn ^:async list-roles!
  [pool {:keys [org-id]}]
  (if (local-api/selected? pool)
    (local-api/read! pool :policy/roles [{:org-id org-id}])
    (let [rows (await (mongo-roles/list-roles! (await (policy-connection/db!)) {:org-id org-id}))]
    {:roles (await (policy-hydration/hydrate-role-maps pool rows))})))

(defn ^:async get-role!
  [pool role-id]
  (if (local-api/selected? pool)
    (local-api/read! pool :policy/role [role-id])
    (if-let [row (await (mongo-roles/get-role-by-id! (await (policy-connection/db!)) role-id))]
    {:role (first (await (policy-hydration/hydrate-role-maps pool [row])))}
    {:role nil})))

(defn ^:async create-role!
  [pool uid mid {:keys [org-id name slug permission-codes tool-policies] :as payload}]
  (if (local-api/selected? pool)
    (local-api/command! pool :policy/create-role [payload])
    (cond
    (str/blank? org-id) (throw (js/Error. "org-id is required"))
    (str/blank? name)   (throw (js/Error. "name is required"))
    :else
    (let [s (values/slugify (or slug name) "role")
          role (await (policy-roles/ensure-role! pool {:org-id org-id :name name :slug s
                                          :scope-kind "org" :built-in false
                                          :system-managed false}))]
      (await (policy-roles/set-role-permissions! pool (:id role) (or permission-codes [])))
      (await (policy-roles/set-role-tool-policies! pool (:id role) (or tool-policies [])))
      (await (policy-connection/append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                  :org-id org-id :action "role.create"
                                  :resource-kind "role" :resource-id (:id role)}))
      {:role (first (await (policy-hydration/hydrate-role-maps pool [role])))}))))

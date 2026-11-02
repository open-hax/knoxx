(ns knoxx.backend.infra.db.policy.organizations
  "Provider-selected organization and data-lake directory operations."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.local-policy-api :as local-api]
            [knoxx.backend.infra.stores.mongo-policy-directory :as mongo-directory]
            [knoxx.backend.infra.stores.mongo-policy-roles :as mongo-roles]
            [knoxx.backend.infra.stores.mongo-policy-data-lakes :as mongo-data-lakes]
            [knoxx.backend.infra.db.policy.support :as policy-support]
            [knoxx.backend.law.policy-values :as values]
            [knoxx.backend.infra.db.policy.connection :as policy-connection]
            [knoxx.backend.infra.db.policy.roles :as policy-roles]
            [knoxx.backend.infra.db.policy.hydration :as policy-hydration]))

(defn- org-row->map
  [{:keys [id slug name kind is_primary status member_count role_count data_lake_count created_at updated_at]}]
  {:id id :slug slug :name name :kind kind
   :is-primary is_primary :status status
   :member-count (js/Number (or member_count 0))
   :role-count (js/Number (or role_count 0))
   :data-lake-count (js/Number (or data_lake_count 0))
   :created-at created_at :updated-at updated_at})

(defn- ^:async accumulate-org-lake-count!
  [db acc o]
  (let [lakes (await (mongo-data-lakes/list-data-lakes-by-org! db (:id o)))]
    (swap! acc assoc (:id o) (count lakes))
    nil))

(defn ^:async org-data-lake-counts
  "Resolve {org-id -> data-lake-count} for the given org rows (sequential
   awaits; the directory twin's list-orgs! zeroes data_lake_count)."
  [db rows]
  (let [acc (atom {})]
    (await (policy-support/promise-each rows (partial accumulate-org-lake-count! db acc)))
    @acc))

(defn ^:async list-orgs!
  [pool]
  (if (local-api/selected? pool)
    (local-api/read! pool :policy/orgs [])
    (let [db (await (policy-connection/db!))
        rows (await (mongo-directory/list-orgs! db))
        ;; The directory twin owns member_count but zeroes role_count /
        ;; data_lake_count (later slices). Recompute them here, preserving the
        ;; PG list-with-counts semantics, before org-row->map coerces them.
        all-roles (await (mongo-roles/list-roles! db {:org-id nil}))
        role-counts (frequencies (keep :org_id all-roles))
        lake-counts (await (org-data-lake-counts db rows))]
    {:orgs (mapv (fn [o]
                   (org-row->map (assoc o
                                        :role_count (get role-counts (:id o) 0)
                                        :data_lake_count (get lake-counts (:id o) 0))))
                 rows)})))

(defn- org-response
  [org]
  {:org {:id (:id org) :slug (:slug org) :name (:name org)
         :kind (:kind org) :is-primary (:is_primary org) :status (:status org)}})

(defn ^:async create-org!
  [pool uid mid {:keys [name slug kind status]
                  :or {kind "customer" status "active"} :as payload}]
  (if (local-api/selected? pool)
    (local-api/command! pool :policy/create-org [payload])
    (if (str/blank? name)
    (throw (js/Error. "name is required"))
    (let [s (values/slugify (or slug name) "org")
          org (await (mongo-directory/create-org! (await (policy-connection/db!)) {:slug s :name name
                                                                 :kind kind :status status}))]
      (await (policy-roles/sync-contract-role-projections! pool))
      (await (policy-connection/append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                  :org-id (:id org) :action "org.create"
                                  :resource-kind "org" :resource-id (:id org)}))
      (org-response org)))))

(defn ^:async ensure-self-org!
  [_pool email display-name]
  (let [db (await (policy-connection/db!))
        slug (values/self-org-slug email)
        label (or (some-> display-name str str/trim not-empty)
                  (values/normalize-email email)
                  "User")
        name (str label " Self")
        existing (await (mongo-directory/find-org-by-slug db slug))]
    (if existing
      existing
      (await (mongo-directory/create-org! db {:slug slug :name name
                                              :kind "self" :status "active"})))))

(defn- data-lake-row->map
  [{:keys [id org_id name slug kind config_json status created_at updated_at]}]
  {:id id :org-id org_id :name name :slug slug :kind kind
   :config (policy-hydration/constraints-json->clj config_json)
   :status status :created-at created_at :updated-at updated_at})

(defn ^:async list-data-lakes!
  [pool {:keys [org-id]}]
  (if (local-api/selected? pool)
    (local-api/read! pool :policy/data-lakes [{:org-id org-id}])
    (if (str/blank? org-id)
    (throw (js/Error. "org-id is required"))
    (when-let [db (await (policy-connection/ensure-mongo-policy-db!))]
      {:data-lakes (mapv data-lake-row->map
                         (await (mongo-data-lakes/list-data-lakes-by-org! db org-id)))}))))

(defn- data-lake-response
  [lake]
  {:data-lake {:id (:id lake) :org-id (:org_id lake) :name (:name lake)
               :slug (:slug lake) :kind (:kind lake) :status (:status lake)}})

(defn ^:async create-data-lake!
  [pool uid mid {:keys [org-id name slug kind config status]
                  :or {kind "workspace_docs" status "active"} :as payload}]
  (if (local-api/selected? pool)
    (local-api/command! pool :policy/create-data-lake [payload])
    (cond
    (str/blank? org-id) (throw (js/Error. "org-id is required"))
    (str/blank? name)   (throw (js/Error. "name is required"))
    :else
    (when-let [db (await (policy-connection/ensure-mongo-policy-db!))]
      (let [s    (values/slugify (or slug name) "lake")
            lake (await (mongo-data-lakes/create-data-lake!
                         db org-id {:name name :slug s :kind kind
                                    :config-json (clj->js (or config {}))
                                    :status status}))]
        (await (policy-connection/append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                    :org-id org-id :action "data_lake.create"
                                    :resource-kind "data_lake" :resource-id (:id lake)}))
        (data-lake-response lake))))))

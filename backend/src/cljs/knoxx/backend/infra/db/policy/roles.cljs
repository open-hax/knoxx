(ns knoxx.backend.infra.db.policy.roles
  "Policy role assignments and contract role projection."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.local-policy-api :as local-api]
            [knoxx.backend.infra.stores.mongo-policy-directory :as mongo-directory]
            [knoxx.backend.infra.stores.mongo-policy-roles :as mongo-roles]
            [knoxx.backend.infra.stores.mongo-policy-tools :as mongo-tools]
            [knoxx.backend.domain.contracts.loader :as contracts-loader]
            [knoxx.backend.infra.registry.tools :as tool-registry]
            [knoxx.backend.infra.db.policy.support :as policy-support]
            [knoxx.backend.law.policy-values :as values]
            [knoxx.backend.infra.db.policy.connection :as policy-connection]))

(defn ^:async find-org-by-id [pool org-id]
  (if (local-api/selected? pool)
    (local-api/trusted-read! pool :policy/org-by-id [org-id])
    (await (mongo-directory/find-org-by-id! (await (policy-connection/db!)) org-id))))

(defn ^:async find-org-by-slug [pool slug]
  (if (local-api/selected? pool)
    (local-api/trusted-read! pool :policy/org-by-slug [slug])
    (await (mongo-directory/find-org-by-slug (await (policy-connection/db!)) slug))))

(defn ^:async find-role [pool {:keys [org-id slug]}]
  (if (local-api/selected? pool)
    (local-api/trusted-read! pool :policy/role-by-slug [{:org-id org-id :slug slug}])
    (await (mongo-roles/find-role (await (policy-connection/db!)) {:org-id org-id :slug slug}))))

(defn ^:async ensure-role!
  [_pool {:keys [org-id name slug scope-kind built-in system-managed]}]
  (await (mongo-roles/ensure-role! (await (policy-connection/db!))
                                   {:org-id org-id :name name :slug slug
                                    :scope-kind scope-kind :built-in built-in
                                    :system-managed system-managed})))

(defn ^:async set-role-permissions!
  [_pool role-id permission-codes]
  (await (mongo-roles/set-role-permissions! (await (policy-connection/db!)) role-id (values/unique permission-codes))))

(defn normalize-tool-policy [p]
  (if (string? p)
    {:tool-id p :effect "allow" :constraints {}}
    (let [tool-id (or (:tool-id p) (:tool_id p) (:id p))]
      (when-not tool-id (throw (js/Error. "toolId is required for tool policy")))
      {:tool-id (tool-registry/normalize-tool-id tool-id)
       :effect  (if (= (:effect p) "deny") "deny" "allow")
       :constraints (or (:constraints p) {})})))

values/keywordish-id(defn- role-tool-policies
  [caps-by-id contract]
  (let [cap-tool-policies
        (->> (or (:role/capabilities contract) [])
             (keep values/keywordish-id)
             (keep caps-by-id)
             (mapcat #(or (:cap/tools %) []))
             (keep tool-registry/normalize-tool-id)
             distinct sort
             (mapv (fn [tid] {:tool-id tid :effect "allow" :constraints {}})))
        declared-tool-policies
        (->> (or (:role/tool-policies contract)
                 (:role/tool_policies contract)
                 (:tool-policies contract)
                 (:toolPolicies contract)
                 [])
             (mapv normalize-tool-policy))]
    (->> (concat cap-tool-policies declared-tool-policies)
         (reduce (fn [acc policy]
                   (assoc acc (:tool-id policy) policy))
                 {})
         vals
         (sort-by :tool-id)
         vec)))

(defn- policy-with-constraints-json
  [p]
  (assoc p :constraints-json (js/JSON.stringify (clj->js (:constraints p)))))

(defn ^:async set-role-tool-policies!
  [pool role-id tool-policies]
  (if (local-api/selected? pool)
    (local-api/command! pool :policy/role-tools [role-id tool-policies])
    (let [db (await (policy-connection/db!))
        normalized (mapv (comp policy-with-constraints-json normalize-tool-policy) tool-policies)]
    (await (mongo-tools/ensure-tool-definitions! db (mapv :tool-id normalized)))
    (await (mongo-tools/set-role-tool-policies! db role-id normalized)))))

(defn- resolve-role-slugs
  [base-ids requested rows alias-map]
  (let [found (into {} (map (fn [r] [(:slug r) (str (:id r))]) rows))
        resolved (keep (fn [slug] (some #(get found %) (get alias-map slug))) requested)
        missing (filter (fn [slug] (not-any? #(contains? found %) (get alias-map slug))) requested)]
    (when (seq missing)
      (throw (js/Error. (str "Role not found for slug(s): " (str/join ", " missing)))))
    (into (vec base-ids) resolved)))

(defn ^:async resolve-role-ids
  [_pool {:keys [org-id role-ids role-slugs]}]
  (let [base-ids (set (map str (or role-ids [])))]
    (if (empty? role-slugs)
      (vec base-ids)
      (let [requested (values/requested-role-slugs role-slugs)
            alias-map (into {} (map (fn [slug] [slug (values/role-slug-aliases slug)])) requested)
            query-slugs (->> (vals alias-map) (mapcat identity) distinct vec)
            rows (await (mongo-roles/list-roles-by-slugs! (await (policy-connection/db!)) query-slugs org-id))]
        (resolve-role-slugs base-ids requested rows alias-map)))))

(defn- canonical-contract-role-slug
  [known raw]
  (let [s (some-> raw str str/trim not-empty)]
    (cond
      (and s (contains? known s)) s
      (and s (contains? known (values/slugify s s))) (values/slugify s s)
      s (do (.warn js/console "[policy-db] unknown role slug, skipping:" s) nil)
      :else nil)))

(defn ^:async canonicalize-contract-role-slugs!
  [role-slugs]
  (let [records (await (contracts-loader/load-all-contracts! (policy-support/contracts-config)))
        known (values/known-contract-role-slugs records)]
    (->> (or role-slugs [])
         (keep #(canonical-contract-role-slug known %))
         distinct
         vec)))

(defn ^:async resolved-membership-role-slugs!
  [role-slugs contract-projection]
  (if contract-projection
    (await (canonicalize-contract-role-slugs! role-slugs))
    (or role-slugs [])))

(defn ^:async set-membership-roles!
  [pool membership-id {:keys [org-id role-ids role-slugs replace contract-projection]}]
  (let [resolved-slugs (await (resolved-membership-role-slugs! role-slugs contract-projection))
        resolved-ids (await (resolve-role-ids pool {:org-id org-id
                                                    :role-ids (or role-ids [])
                                                    :role-slugs resolved-slugs}))]
    (await (mongo-roles/set-membership-roles! (await (policy-connection/db!)) membership-id (boolean replace) resolved-ids))))

(defn ^:async set-membership-tool-policies!
  [pool membership-id tool-policies]
  (if (local-api/selected? pool)
    (local-api/command! pool :policy/membership-tools [membership-id tool-policies])
    (let [db (await (policy-connection/db!))
        normalized (mapv (comp policy-with-constraints-json normalize-tool-policy) tool-policies)]
    (await (mongo-tools/ensure-tool-definitions! db (mapv :tool-id normalized)))
    (await (mongo-tools/set-membership-tool-policies! db membership-id normalized)))))

(defn ^:async set-membership-actor-id!
  [_pool membership-id actor-id]
  (await (mongo-directory/set-membership-actor-id! (await (policy-connection/db!)) membership-id actor-id)))

(defn ^:async sync-contract-role-record!
  [pool caps-by-id rec]
  (when-let [slug (values/role-record-slug rec)]
    (let [contract (:contract rec)
          perms (->> (or (:role/permissions contract) []) (map str) distinct sort vec)
          role (await (ensure-role! pool {:org-id nil
                                          :name (values/role-display-name slug contract)
                                          :slug slug :scope-kind "platform"
                                          :built-in false :system-managed true}))]
      (await (set-role-permissions! pool (:id role) perms))
      (await (set-role-tool-policies! pool (:id role) (role-tool-policies caps-by-id contract)))))
  nil)

(defn ^:async sync-contract-role-projections!
  [pool]
  (let [records (await (contracts-loader/load-all-contracts! (policy-support/contracts-config)))
        caps-by-id (into {} (map (fn [r] [(:id r) (:contract r)]))
                         (values/contract-records-by-class records "capabilities"))]
    (await (policy-support/promise-each (values/contract-records-by-class records "roles")
                         #(sync-contract-role-record! pool caps-by-id %))))
  nil)

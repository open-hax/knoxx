(ns knoxx.backend.infra.db.policy.hydration
  "Hydrate policy rows and derive request authority from current memberships."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.stores.mongo-policy-directory :as mongo-directory]
            [knoxx.backend.infra.stores.mongo-policy-roles :as mongo-roles]
            [knoxx.backend.infra.stores.mongo-policy-tools :as mongo-tools]
            [knoxx.backend.infra.db.policy.support :as policy-support]
            [knoxx.backend.law.policy-values :as values]
            [knoxx.backend.infra.db.policy.connection :as policy-connection]
            [knoxx.backend.infra.db.policy.roles :as policy-roles]))

(defn constraints-json->clj
  [value]
  (js->clj (or value (js-obj)) :keywordize-keys true))

(defn- grouped-role-permissions
  [rows]
  (reduce (fn [acc {:keys [role_id code]}]
            (update acc role_id (fnil conj []) code))
          {}
          rows))

(defn- grouped-role-tool-policies
  [rows]
  (reduce (fn [acc {:keys [role_id tool_id effect constraints_json]}]
            (update acc role_id (fnil conj [])
                    {:tool-id tool_id :effect effect
                     :constraints (constraints-json->clj constraints_json)}))
          {}
          rows))

(defn- hydrate-role-row
  [perm-map tool-map {:keys [id org_id name slug scope_kind built_in system_managed created_at updated_at]}]
  {:id id :org-id org_id :name name :slug slug :scope-kind scope_kind
   :built-in built_in :system-managed system_managed
   :created-at created_at :updated-at updated_at
   :permissions (or (get perm-map id) [])
   :tool-policies (or (get tool-map id) [])})

(defn ^:async hydrate-role-maps
  [_pool roles]
  (if (empty? roles)
    []
    (let [db (await (policy-connection/db!))
          role-ids (mapv :id roles)
          perm-rows (await (mongo-roles/permissions-for-roles! db role-ids))
          tool-rows (await (mongo-tools/tool-policies-for-roles! db role-ids))
          perm-map (grouped-role-permissions perm-rows)
          tool-map (grouped-role-tool-policies tool-rows)]
      (mapv #(hydrate-role-row perm-map tool-map %) roles))))

(defn- grouped-membership-roles
  [rows]
  (reduce (fn [acc {:keys [membership_id role_id slug name scope_kind org_id]}]
            (update acc membership_id (fnil conj [])
                    {:id role_id :slug slug :name name :scope-kind scope_kind :org-id org_id}))
          {}
          rows))

(defn- grouped-membership-tool-policies
  [rows]
  (reduce (fn [acc {:keys [membership_id tool_id effect constraints_json]}]
            (update acc membership_id (fnil conj [])
                    {:tool-id tool_id :effect effect
                     :constraints (constraints-json->clj constraints_json)}))
          {}
          rows))

(defn- hydrate-membership-row
  [roles-by-m tools-by-m {:keys [id user_id org_id actor_id org_name org_slug status is_default created_at updated_at]}]
  (let [roles (or (get roles-by-m id) [])]
    {:id id :userId user_id :orgId org_id
     :actorId (or (values/normalize-actor-id actor_id)
                  (policy-support/default-membership-actor-id (map :slug roles)))
     :orgName org_name :orgSlug org_slug :status status :isDefault is_default
     :createdAt created_at :updatedAt updated_at
     :roles roles :toolPolicies (or (get tools-by-m id) [])}))

(defn ^:async hydrate-memberships
  [_pool memberships]
  (if (empty? memberships)
    []
    (let [db (await (policy-connection/db!))
          membership-ids (mapv :id memberships)
          role-rows (await (mongo-roles/roles-for-memberships! db membership-ids))
          tool-rows (await (mongo-tools/tool-policies-for-memberships! db membership-ids))
          roles-by-m (grouped-membership-roles role-rows)
          tools-by-m (grouped-membership-tool-policies tool-rows)]
      (mapv #(hydrate-membership-row roles-by-m tools-by-m %) memberships))))

(defn- header-value [headers-like name]
  (when headers-like
    (if (fn? (aget headers-like "get"))
      (str/trim (or (.get headers-like name)
                    (.get headers-like (str/lower-case name)) ""))
      (str/trim (str (or (aget headers-like name)
                         (aget headers-like (str/lower-case name)) ""))))))

(defn- ^:async find-request-membership-row [_pool headers-like]
  (let [membership-id (header-value headers-like "x-knoxx-membership-id")
        user-email    (some-> (header-value headers-like "x-knoxx-user-email") str/lower-case)
        org-id        (header-value headers-like "x-knoxx-org-id")
        org-slug      (some-> (header-value headers-like "x-knoxx-org-slug") str/lower-case)]
    (cond
      (and (str/blank? membership-id) (str/blank? user-email))
      (throw (policy-support/http-error 401 "Missing x-knoxx-user-email or x-knoxx-membership-id"
                         "request_context_missing"))

      (not (str/blank? membership-id))
      (await (mongo-directory/find-membership-row-with-user-org! (await (policy-connection/db!)) membership-id))

      :else
      (await (mongo-directory/find-membership-row-by-email-and-org!
              (await (policy-connection/db!))
              {:user-email user-email :org-id org-id :org-slug org-slug})))))

(defn- merge-tool-policies [role-policies membership-policies]
  (let [merged (atom {})]
    (doseq [p role-policies]
      (let [n (policy-roles/normalize-tool-policy p) tid (:tool-id n)]
        (when (or (nil? (get @merged tid))
                  (= (:effect n) "deny")
                  (not= (:effect (get @merged tid)) "deny"))
          (swap! merged assoc tid n))))
    (doseq [p membership-policies]
      (let [n (policy-roles/normalize-tool-policy p)]
        (swap! merged assoc (:tool-id n) n)))
    (->> (vals @merged) (sort-by :tool-id) vec)))

(defn- validate-membership-row!
  [membership-row]
  (cond
    (not membership-row)
    (throw (policy-support/http-error 401 "Request context did not resolve to a membership"
                       "request_context_unresolved"))
    (not= (:user_status membership-row) "active")
    (throw (policy-support/http-error 403 "User is not active" "user_inactive"))
    (not= (:status membership-row) "active")
    (throw (policy-support/http-error 403 "Membership is not active" "membership_inactive"))
    (not= (:org_status membership-row) "active")
    (throw (policy-support/http-error 403 "Org is not active" "org_inactive"))))

(defn ^:async detailed-membership-roles
  [pool membership]
  (let [role-ids (mapv :id (:roles membership))]
    (if (empty? role-ids)
      []
      (let [rows (await (mongo-roles/list-roles-by-ids! (await (policy-connection/db!)) role-ids))]
        (await (hydrate-role-maps pool rows))))))

(defn- request-user-map
  [membership-row]
  {:id (:user_id membership-row) :email (:email membership-row)
   :username (:email membership-row) :display-name (:display_name membership-row)
   :status (:user_status membership-row)})

(defn- request-org-map
  [membership-row]
  {:id (:org_id membership-row) :slug (:org_slug membership-row)
   :name (:org_name membership-row) :status (:org_status membership-row)
   :is-primary (:is_primary membership-row) :kind (:org_kind membership-row)})

(defn- request-membership-map
  [membership actor-id]
  {:id (:id membership) :actor-id actor-id :status (:status membership)
   :is-default (:is-default membership) :created-at (:created-at membership)
   :updated-at (:updated-at membership)})

(defn- request-policy-summary
  [membership detailed-roles]
  {:permissions (sort (values/unique (mapcat :permissions detailed-roles)))
   :tool-policies (merge-tool-policies (mapcat :tool-policies detailed-roles)
                                       (:tool-policies membership))
   :role-slugs (sort-by #(- (values/rolePriority %)) (map :slug detailed-roles))})

(defn- request-context-map
  [membership-row membership detailed-roles]
  (let [{:keys [permissions tool-policies role-slugs]} (request-policy-summary membership detailed-roles)
        ;; The membership's *stored* actor binding, or nil. Kept separate from
        ;; actor-id below, which falls back to a role-derived default and is
        ;; therefore never nil — so it cannot answer "was an actor assigned?".
        ;; Anything deciding authority (which credentials a token may read) must
        ;; use the binding; the default is a display and role convenience.
        actor-binding (values/normalize-actor-id (:actor_id membership-row))
        actor-id (or actor-binding
                     (policy-support/default-membership-actor-id role-slugs))]
    {:user (request-user-map membership-row)
     :org (request-org-map membership-row)
     :membership (request-membership-map membership actor-id)
     :actor {:id actor-id :binding actor-binding}
     :roles detailed-roles
     :role-slugs role-slugs
     :permissions permissions
     :tool-policies tool-policies
     :membership-tool-policies (:tool-policies membership)
     :is-system-admin (boolean (some #{"system_admin" "system-admin"} role-slugs))}))

(defn ^:async build-request-context
  [pool membership-row]
  (validate-membership-row! membership-row)
  (let [membership (first (await (hydrate-memberships pool [membership-row])))
        detailed-roles (await (detailed-membership-roles pool membership))]
    (request-context-map membership-row membership detailed-roles)))

(defn ^:async resolve-request-context!
  "Resolve a Knoxx auth context from headers-like (Fastify headers or CLJS map).
   Returns Promise<CLJS ctx map>."
  [pool headers-like]
  (await (build-request-context pool (await (find-request-membership-row pool headers-like)))))

(defn ^:async evaluate-tool-access!
  [pool headers-like tool-id]
  (let [ctx (await (resolve-request-context! pool headers-like))
        match (some #(when (= (:tool-id %) tool-id) %) (:tool-policies ctx))]
    {:context ctx
     :tool-id tool-id
     :allowed (boolean (and match (= (:effect match) "allow")))}))

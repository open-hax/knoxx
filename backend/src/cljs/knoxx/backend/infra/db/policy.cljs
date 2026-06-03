(ns knoxx.backend.infra.db.policy
  "Policy DB public API.

   All queries are HoneySQL maps built in shape.db.*; this namespace
   executes them via extern.pg and exposes a CLJS-typed public surface.

   create-policy-db returns a Promise<CLJS policy context | nil>. The context
   is plain CLJS data whose :pool value is an opaque pg Pool handle owned by
   extern.pg. Public functions take that pool/context and return CLJS maps."
  (:require [clojure.string :as str]
            [honey.sql :as sql]
            [knoxx.backend.extern.pg :as pg]
            [knoxx.backend.shape.db.audit :as q-audit]
            [knoxx.backend.shape.db.invites :as q-invites]
            [knoxx.backend.shape.db.memberships :as q-memberships]
            [knoxx.backend.shape.db.orgs :as q-orgs]
            [knoxx.backend.shape.db.roles :as q-roles]
            [knoxx.backend.shape.db.sessions :as q-sessions]
            [knoxx.backend.shape.db.users :as q-users]
            [knoxx.backend.infra.db.policy.schema :as db-schema]
            [knoxx.backend.domain.actor.scope :as actor-scope]
            [knoxx.backend.domain.contracts.loader :as contracts-loader]
            [knoxx.backend.domain.contracts.roles :as contracts-roles]
            [knoxx.backend.infra.db.actors :as policy-actors]
            [knoxx.backend.domain.policy.protocol :as policy]
            [knoxx.backend.domain.policy.sql-adapter :as sql-policy]
            [knoxx.backend.infra.registry.tools :as tool-registry]
            ["node:path" :as path]
            ["node:fs" :as fs]
            ["node:crypto" :as crypto]))

(declare set-membership-roles! sync-contract-role-projections! find-org-by-slug)

;; ---------------------------------------------------------------------------
;; SQL execution helpers
;; ---------------------------------------------------------------------------

(defn- honey->sql [honey-map]
  (sql/format honey-map {:numbered true}))

(defn- honey-query! [conn honey-map]
  (let [[sql-str & params] (honey->sql honey-map)]
    (pg/query! conn sql-str params)))

(defn- honey-query-one! [conn honey-map]
  (let [[sql-str & params] (honey->sql honey-map)]
    (pg/query-one! conn sql-str params)))

(defn ^:async promise-each
  [items f]
  (doseq [item (or items [])]
    (await (f item)))
  nil)

;; ---------------------------------------------------------------------------
;; Utility helpers
;; ---------------------------------------------------------------------------

(defn- slugify [value fallback]
  (let [s (-> (str value "")
              str/trim str/lower-case
              (str/replace #"[^a-z0-9]+" "-")
              (str/replace #"^[-]+|[-]+$" ""))]
    (if (str/blank? s) fallback s)))

(defn- normalize-email [v]
  (some-> v str str/trim str/lower-case not-empty))

(defn- normalize-actor-id [v]
  (some-> v str str/trim not-empty))

(defn- unique [vs]
  (vec (distinct (filter some? vs))))

(defn- http-error [status message code]
  (doto (js/Error. message)
    (aset "statusCode" status)
    (aset "code" code)))

(defn- env-positive-int [key default]
  (let [raw (aget js/process.env key)
        n (js/Number raw)]
    (if (and raw (not (js/Number.isNaN n)) (pos? n)) (js/Math.floor n) default)))

(defn- env-truthy? [key]
  (contains? #{"1" "true" "yes" "on" "y"}
             (-> (or (aget js/process.env key) "") str str/trim str/lower-case)))

(defn- default-contracts-dir []
  (let [configured (some-> (aget js/process.env "CONTRACTS_DIR") str str/trim not-empty)
        cwd (.cwd js/process)]
    (or (some (fn [c] (when (.existsSync fs c) c))
              (map #(.resolve path cwd %)
                   (keep identity [configured "contracts" "../contracts"
                                   "packages/agents/knoxx/contracts"
                                   "orgs/open-hax/openplanner/packages/agents/knoxx/contracts"])))
        (.resolve path cwd (or configured "contracts")))))

(defn- contracts-dir [] (default-contracts-dir))
(defn- contracts-config [] {:contracts-dir (default-contracts-dir)})

;; ---------------------------------------------------------------------------
;; Actor contract helpers (delegate to policy-actors)
;; ---------------------------------------------------------------------------

(defn- upsert-actor-contract! [payload]
  (policy-actors/upsert-actor-contract! (contracts-dir) payload))

(defn- read-only-contract-write-error?
  [err]
  (let [code (some-> (.-code err) str str/trim)
        message (some-> (.-message err) str)]
    (or (#{"EROFS" "EACCES" "EPERM"} code)
        (and message (boolean (re-find #"read-only file system|permission denied|operation not permitted" message))))))

(defn- handle-read-only-actor-contract-error!
  [err]
  (if (read-only-contract-write-error? err)
    (do
      (.warn js/console
             "[knoxx-policy] actor contract write skipped; contracts dir is read-only/unwritable"
             (.-message err))
      nil)
    (throw err)))

(defn ^:async upsert-actor-contract-best-effort!
  [payload]
  (try
    (await (upsert-actor-contract! payload))
    (catch :default err
      (handle-read-only-actor-contract-error! err))))

(defn- find-actor-contract-by-id [actor-id]
  (policy-actors/find-actor-contract-by-id (contracts-dir) actor-id))

(defn- find-user-actor-contract-by-email [email]
  (policy-actors/find-user-actor-contract-by-email (contracts-dir) email))

(defn- list-actor-contracts []
  (policy-actors/list-actor-contracts (contracts-dir)))


(defn- user-actor-id-from-email [email]
  (policy-actors/user-actor-id-from-email email))

;; ---------------------------------------------------------------------------
;; Basic lookups
;; ---------------------------------------------------------------------------

(defn- find-org-by-id [pool org-id]
  (when-not (str/blank? (str org-id))
    (honey-query-one! pool (q-orgs/by-id org-id))))

(defn- find-org-by-slug [pool slug]
  (if (str/blank? (str slug))
    (js/Promise.resolve nil)
    (honey-query-one! pool (q-orgs/by-slug slug))))

(defn- find-role [pool {:keys [org-id slug]}]
  (honey-query-one! pool (q-roles/by-slug {:slug slug :org-id org-id})))

;; ---------------------------------------------------------------------------
;; Role management
;; ---------------------------------------------------------------------------

(defn ^:async ensure-role!
  [pool {:keys [org-id name slug scope-kind built-in system-managed]}]
  (let [existing (await (find-role pool {:org-id org-id :slug slug}))
        attrs {:name name :scope-kind scope-kind
               :built-in built-in :system-managed system-managed}]
    (if existing
      (await (honey-query-one! pool (q-roles/update-role (:id existing) attrs)))
      (await (honey-query-one! pool (q-roles/insert (assoc attrs :org-id org-id :slug slug)))))))

(defn ^:async role-permissions-uses-legacy-ids?
  [pool]
  (let [{:keys [rows]} (await (pg/query! pool
                               "SELECT column_name FROM information_schema.columns
                                WHERE table_schema = 'public' AND table_name = 'role_permissions'"
                               nil))]
    (contains? (set (map :column_name rows)) "permission_id")))

(defn- insert-role-permission!
  [client role-id legacy? code]
  (if legacy?
    (pg/query! client
      "INSERT INTO role_permissions (role_id, permission_id, effect)
       SELECT $1, id, 'allow' FROM permissions WHERE code = $2
       ON CONFLICT (role_id, permission_id) DO UPDATE SET effect = EXCLUDED.effect"
      [role-id code])
    (honey-query! client (q-roles/insert-permission-modern role-id code))))

(defn ^:async set-role-permissions-tx!
  [client role-id codes]
  (await (honey-query! client (q-roles/delete-permissions role-id)))
  (let [legacy? (await (role-permissions-uses-legacy-ids? client))]
    (await (promise-each codes #(insert-role-permission! client role-id legacy? %))))
  nil)

(defn- set-role-permissions!
  [pool role-id permission-codes]
  (pg/with-transaction!
   pool
   (fn [client]
     (set-role-permissions-tx! client role-id (unique permission-codes)))))

(defn- normalize-tool-policy [p]
  (if (string? p)
    {:tool-id p :effect "allow" :constraints {}}
    (let [tool-id (or (:tool-id p) (:tool_id p) (:id p))]
      (when-not tool-id (throw (js/Error. "toolId is required for tool policy")))
      {:tool-id (tool-registry/normalize-tool-id tool-id)
       :effect  (if (= (:effect p) "deny") "deny" "allow")
       :constraints (or (:constraints p) {})})))

(defn- keywordish-id
  [value]
  (cond
    (keyword? value) (some-> value name str/trim not-empty)
    (string? value) (some-> value str str/trim not-empty)
    (nil? value) nil
    :else (some-> value str str/trim not-empty)))

(defn- role-slug-aliases
  [slug]
  (let [s (some-> slug str str/trim not-empty)]
    (->> [s
          (some-> s (str/replace #"_" "-"))
          (some-> s (str/replace #"-" "_"))
          (when s (slugify s s))]
         (remove str/blank?)
         distinct
         vec)))

(defn- role-tool-policies
  [caps-by-id contract]
  (let [cap-tool-policies
        (->> (or (:role/capabilities contract) [])
             (keep keywordish-id)
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

(defn- ensure-tool-definitions! [conn tool-ids]
  (let [ids (->> tool-ids (keep tool-registry/normalize-tool-id) distinct vec)]
    (if (empty? ids)
      (js/Promise.resolve nil)
      (promise-each
       ids
       (fn [tid]
         (let [{:keys [label description risk-level]} (tool-registry/get-tool tid)]
           (pg/query! conn
             "INSERT INTO tool_definitions (id, label, description, risk_level)
              VALUES ($1, $2, $3, $4)
              ON CONFLICT (id) DO UPDATE
              SET label = EXCLUDED.label, description = EXCLUDED.description,
                  risk_level = EXCLUDED.risk_level"
             [tid (or label tid) (or description "") (or risk-level "low")])))))))

(defn- policy-with-constraints-json
  [p]
  (assoc p :constraints-json (js/JSON.stringify (clj->js (:constraints p)))))

(defn ^:async set-role-tool-policies-tx!
  [client role-id normalized]
  (await (honey-query! client (q-roles/delete-tool-policies role-id)))
  (await (ensure-tool-definitions! client (mapv :tool-id normalized)))
  (await (promise-each normalized
                       #(honey-query! client
                                      (q-roles/insert-tool-policy
                                       role-id (policy-with-constraints-json %)))))
  nil)

(defn set-role-tool-policies!
  [pool role-id tool-policies]
  (let [normalized (mapv normalize-tool-policy tool-policies)]
    (pg/with-transaction!
     pool
     (fn [client]
       (set-role-tool-policies-tx! client role-id normalized)))))

;; ---------------------------------------------------------------------------
;; Membership roles
;; ---------------------------------------------------------------------------

(defn- requested-role-slugs
  [role-slugs]
  (->> role-slugs
       (map #(some-> % str str/trim not-empty))
       (remove nil?)
       distinct
       vec))

(defn- resolve-role-slugs
  [base-ids requested rows alias-map]
  (let [found (into {} (map (fn [r] [(:slug r) (str (:id r))]) rows))
        resolved (keep (fn [slug] (some #(get found %) (get alias-map slug))) requested)
        missing (filter (fn [slug] (not-any? #(contains? found %) (get alias-map slug))) requested)]
    (when (seq missing)
      (throw (js/Error. (str "Role not found for slug(s): " (str/join ", " missing)))))
    (into (vec base-ids) resolved)))

(defn ^:async resolve-role-ids
  [pool {:keys [org-id role-ids role-slugs]}]
  (let [base-ids (set (map str (or role-ids [])))]
    (if (empty? role-slugs)
      (vec base-ids)
      (let [requested (requested-role-slugs role-slugs)
            alias-map (into {} (map (fn [slug] [slug (role-slug-aliases slug)])) requested)
            query-slugs (->> (vals alias-map) (mapcat identity) distinct vec)
            {:keys [rows]} (await (honey-query! pool (q-roles/by-slugs-and-org query-slugs org-id)))]
        (resolve-role-slugs base-ids requested rows alias-map)))))

(defn- known-contract-role-slugs
  [records]
  (->> records
       (filter #(= "roles" (:contractClass %)))
       (keep #(some-> (or (:id %) (get-in % [:contract :role/id])) str str/trim not-empty))
       set))

(defn- canonical-contract-role-slug
  [known raw]
  (let [s (some-> raw str str/trim not-empty)]
    (cond
      (and s (contains? known s)) s
      (and s (contains? known (slugify s s))) (slugify s s)
      s (do (.warn js/console "[policy-db] unknown role slug, skipping:" s) nil)
      :else nil)))

(defn ^:async canonicalize-contract-role-slugs!
  [role-slugs]
  (let [records (await (contracts-loader/load-all-contracts! (contracts-config)))
        known (known-contract-role-slugs records)]
    (->> (or role-slugs [])
         (keep #(canonical-contract-role-slug known %))
         distinct
         vec)))

(defn ^:async set-membership-roles-tx!
  [client membership-id replace resolved-ids]
  (when replace
    (await (honey-query! client (q-memberships/delete-roles membership-id))))
  (await (promise-each resolved-ids
                       #(honey-query! client (q-memberships/insert-role membership-id %))))
  resolved-ids)

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
    (await (pg/with-transaction!
            pool
            (fn [client]
              (set-membership-roles-tx! client membership-id replace resolved-ids))))))

(defn ^:async set-membership-tool-policies-tx!
  [client membership-id normalized]
  (await (honey-query! client (q-memberships/delete-tool-policies membership-id)))
  (await (ensure-tool-definitions! client (mapv :tool-id normalized)))
  (await (promise-each normalized
                       #(honey-query! client
                                      (q-memberships/insert-tool-policy
                                       membership-id (policy-with-constraints-json %)))))
  nil)

(defn set-membership-tool-policies!
  [pool membership-id tool-policies]
  (let [normalized (mapv normalize-tool-policy tool-policies)]
    (pg/with-transaction!
     pool
     (fn [client]
       (set-membership-tool-policies-tx! client membership-id normalized)))))

(defn ^:async set-membership-actor-id!
  [pool membership-id actor-id]
  (let [resolved (or (normalize-actor-id actor-id) "workspace_user")]
    (await (honey-query! pool (q-memberships/set-actor-id membership-id resolved)))
    resolved))

;; ---------------------------------------------------------------------------
;; Hydration
;; ---------------------------------------------------------------------------

(defn- default-membership-actor-id [role-slugs]
  (actor-scope/default-membership-actor-id role-slugs))

(defn- constraints-json->clj
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
  [pool roles]
  (if (empty? roles)
    []
    (let [role-ids (mapv :id roles)
          legacy? (await (role-permissions-uses-legacy-ids? pool))
          perm-query (if legacy? q-roles/permissions-for-roles-legacy q-roles/permissions-for-roles)
          perm-result (await (honey-query! pool (perm-query role-ids)))
          tool-result (await (honey-query! pool (q-roles/tool-policies-for-roles role-ids)))
          perm-map (grouped-role-permissions (:rows perm-result))
          tool-map (grouped-role-tool-policies (:rows tool-result))]
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
    {:id id :user-id user_id :org-id org_id
     :actor-id (or (normalize-actor-id actor_id)
                   (default-membership-actor-id (map :slug roles)))
     :org-name org_name :org-slug org_slug :status status :is-default is_default
     :created-at created_at :updated-at updated_at
     :roles roles :tool-policies (or (get tools-by-m id) [])}))

(defn ^:async hydrate-memberships
  [pool memberships]
  (if (empty? memberships)
    []
    (let [membership-ids (mapv :id memberships)
          role-result (await (honey-query! pool (q-roles/roles-for-memberships membership-ids)))
          tool-result (await (honey-query! pool (q-memberships/tool-policies-for-ids membership-ids)))
          roles-by-m (grouped-membership-roles (:rows role-result))
          tools-by-m (grouped-membership-tool-policies (:rows tool-result))]
      (mapv #(hydrate-membership-row roles-by-m tools-by-m %) memberships))))

;; ---------------------------------------------------------------------------
;; Request context
;; ---------------------------------------------------------------------------

(defn- header-value [headers-like name]
  (when headers-like
    (if (fn? (aget headers-like "get"))
      (str/trim (or (.get headers-like name)
                    (.get headers-like (str/lower-case name)) ""))
      (str/trim (str (or (aget headers-like name)
                         (aget headers-like (str/lower-case name)) ""))))))

(defn- find-request-membership-row [pool headers-like]
  (let [membership-id (header-value headers-like "x-knoxx-membership-id")
        user-email    (some-> (header-value headers-like "x-knoxx-user-email") str/lower-case)
        org-id        (header-value headers-like "x-knoxx-org-id")
        org-slug      (some-> (header-value headers-like "x-knoxx-org-slug") str/lower-case)]
    (cond
      (and (str/blank? membership-id) (str/blank? user-email))
      (js/Promise.reject
       (http-error 401 "Missing x-knoxx-user-email or x-knoxx-membership-id"
                   "request_context_missing"))

      (not (str/blank? membership-id))
      (honey-query-one! pool (q-memberships/by-id membership-id))

      :else
      (honey-query-one! pool (q-memberships/by-email-and-org
                               {:user-email user-email
                                :org-id     org-id
                                :org-slug   org-slug})))))

(defn- rolePriority [slug]
  (case slug "system_admin" 100 "system-admin" 100 "org_admin" 90 "org-admin" 90
    "developer" 80 "data_analyst" 70 "data-analyst" 70 "knowledge_worker" 60 "knowledge-worker" 60 0))

(defn- merge-tool-policies [role-policies membership-policies]
  (let [merged (atom {})]
    (doseq [p role-policies]
      (let [n (normalize-tool-policy p) tid (:tool-id n)]
        (when (or (nil? (get @merged tid))
                  (= (:effect n) "deny")
                  (not= (:effect (get @merged tid)) "deny"))
          (swap! merged assoc tid n))))
    (doseq [p membership-policies]
      (let [n (normalize-tool-policy p)]
        (swap! merged assoc (:tool-id n) n)))
    (->> (vals @merged) (sort-by :tool-id) vec)))

(defn- validate-membership-row!
  [membership-row]
  (cond
    (not membership-row)
    (throw (http-error 401 "Request context did not resolve to a membership"
                       "request_context_unresolved"))
    (not= (:user_status membership-row) "active")
    (throw (http-error 403 "User is not active" "user_inactive"))
    (not= (:status membership-row) "active")
    (throw (http-error 403 "Membership is not active" "membership_inactive"))
    (not= (:org_status membership-row) "active")
    (throw (http-error 403 "Org is not active" "org_inactive"))))

(defn ^:async detailed-membership-roles
  [pool membership]
  (let [role-ids (mapv :id (:roles membership))]
    (if (empty? role-ids)
      []
      (let [{:keys [rows]} (await (honey-query! pool (q-roles/by-ids role-ids)))]
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
  {:permissions (sort (unique (mapcat :permissions detailed-roles)))
   :tool-policies (merge-tool-policies (mapcat :tool-policies detailed-roles)
                                       (:tool-policies membership))
   :role-slugs (sort-by #(- (rolePriority %)) (map :slug detailed-roles))})

(defn- request-context-map
  [membership-row membership detailed-roles]
  (let [{:keys [permissions tool-policies role-slugs]} (request-policy-summary membership detailed-roles)
        actor-id (or (normalize-actor-id (:actor_id membership-row))
                     (default-membership-actor-id role-slugs))]
    {:user (request-user-map membership-row)
     :org (request-org-map membership-row)
     :membership (request-membership-map membership actor-id)
     :actor {:id actor-id}
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

;; ---------------------------------------------------------------------------
;; Bootstrap & contract sync
;; ---------------------------------------------------------------------------

(defn- sql-policy-store [pool primary-org]
  (sql-policy/create-store
   {:query-one!          (fn [s p] (pg/query-one! pool s p))
    :query!              (fn [s p] (pg/query! pool s p))
    :find-org-by-slug!   (fn [slug] (find-org-by-slug pool slug))
    :set-membership-roles! (fn [mid opts] (set-membership-roles! pool mid opts))
    :primary-org         primary-org}))

(defn- sync-user-from-actor-contract!* [pool primary-org payload]
  (let [actor-id (normalize-actor-id (or (:actor-id payload) (:actor_id payload)))
        email    (normalize-email (:email payload))]
    (if-not (or email actor-id)
      (js/Promise.resolve nil)
      (if-let [contract (or (find-actor-contract-by-id actor-id)
                            (find-user-actor-contract-by-email email))]
        (policy/sync-actor-projections!
         (sql-policy-store pool primary-org)
         [(:actor contract)])
        (js/Promise.resolve nil)))))

(defn- contract-records-by-class
  [records contract-class]
  (->> records (filter #(= contract-class (:contractClass %))) vec))

(defn- role-record-slug
  [rec]
  (some-> (or (:id rec) (get-in rec [:contract :role/id])) str str/trim not-empty))

(defn- role-display-name
  [slug contract]
  (or (some-> (:role/label contract) str str/trim not-empty)
      (some-> (:role/name contract) str str/trim not-empty)
      (->> (str/split slug #"[-_]+")
           (remove str/blank?)
           (map str/capitalize)
           (str/join " "))))

(defn ^:async sync-contract-role-record!
  [pool caps-by-id rec]
  (when-let [slug (role-record-slug rec)]
    (let [contract (:contract rec)
          perms (->> (or (:role/permissions contract) []) (map str) distinct sort vec)
          role (await (ensure-role! pool {:org-id nil
                                          :name (role-display-name slug contract)
                                          :slug slug :scope-kind "platform"
                                          :built-in false :system-managed true}))]
      (await (set-role-permissions! pool (:id role) perms))
      (await (set-role-tool-policies! pool (:id role) (role-tool-policies caps-by-id contract)))))
  nil)

(defn ^:async sync-contract-role-projections!
  [pool]
  (let [records (await (contracts-loader/load-all-contracts! (contracts-config)))
        caps-by-id (into {} (map (fn [r] [(:id r) (:contract r)]))
                         (contract-records-by-class records "capabilities"))]
    (await (promise-each (contract-records-by-class records "roles")
                         #(sync-contract-role-record! pool caps-by-id %))))
  nil)

(defn ^:async ensure-primary-org!
  [pool opts]
  (let [primary-org-slug (or (:primaryOrgSlug opts) (:primary-org-slug opts) "open-hax")
        primary-org-name (or (:primaryOrgName opts) (:primary-org-name opts) "Open Hax")
        primary-org-kind (or (:primaryOrgKind opts) (:primary-org-kind opts) "platform_owner")
        slug (slugify primary-org-slug "open-hax")
        org (await (honey-query-one! pool (q-orgs/upsert-primary {:slug slug
                                                                  :name (str primary-org-name)
                                                                  :kind (str primary-org-kind)})))]
    (await (honey-query! pool (q-orgs/clear-primary-except slug)))
    org))

(defn ^:async ensure-bootstrap-user!
  [pool primary-org opts]
  (let [email (str/lower-case (str (or (:bootstrapSystemAdminEmail opts)
                                       (:bootstrap-system-admin-email opts)
                                       "system-admin@open-hax.local")))
        dn (str (or (:bootstrapSystemAdminName opts)
                    (:bootstrap-system-admin-name opts)
                    "Knoxx System Admin"))
        user (await (honey-query-one! pool (q-users/upsert {:email email :display-name dn
                                                            :auth-provider "bootstrap"
                                                            :external-subject nil :status "active"})))
        membership (await (honey-query-one! pool (q-memberships/upsert {:user-id (:id user)
                                                                        :org-id (:id primary-org)
                                                                        :status "active"
                                                                        :is-default true})))]
    (await (set-membership-roles! pool (:id membership) {:org-id (:id primary-org)
                                                         :role-slugs ["system-admin"]
                                                         :replace true}))
    (await (set-membership-actor-id! pool (:id membership) "system_admin"))
    {:user user :membership membership}))

;; ---------------------------------------------------------------------------
;; Audit
;; ---------------------------------------------------------------------------

(defn- append-audit! [pool {:keys [before after] :as opts}]
  (honey-query! pool
    (q-audit/insert-event
     (assoc opts
            :before-json (when before (js/JSON.stringify (clj->js before)))
            :after-json  (when after  (js/JSON.stringify (clj->js after)))))))

;; ---------------------------------------------------------------------------
;; Session persistence
;; ---------------------------------------------------------------------------

(defn- hash-token [token salt]
  (let [h (.createHash crypto "sha256")]
    (.update h (str salt ":" token) "utf8")
    (.digest h "hex")))

(defn- token-prefix [token]
  (let [h (.createHash crypto "sha256")]
    (.update h (str token) "utf8")
    (subs (.digest h "hex") 0 12)))

(defn- generate-salt []
  (.toString (.randomBytes crypto 16) "hex"))

(defn ^:async touch-session-best-effort!
  [pool session-id]
  (try
    (await (honey-query! pool (q-sessions/touch session-id)))
    (catch :default _ nil)))

(defn- find-session-in-rows [pool token rows]
  (loop [[row & rest] rows]
    (when row
      (if (= (:token_hash row) (hash-token token (:salt row)))
        (do
          (touch-session-best-effort! pool (:id row))
          {:session {:id            (:id row)
                     :user-id       (:user_id row)
                     :membership-id (:membership_id row)
                     :org-id        (:org_id row)
                     :email         (:email row)
                     :display-name  (:display_name row)
                     :auth-provider (:auth_provider row)
                     :expires-at    (:expires_at row)
                     :created-at    (:created_at row)}})
        (recur rest)))))

;; ---------------------------------------------------------------------------
;; Public API
;; ---------------------------------------------------------------------------

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

(defn ^:async list-actor-credentials!
  "Return active actor credential rows for provider as {:credentials [...]}."
  [pool provider]
  {:credentials (await (policy/list-actor-credentials (sql-policy-store pool nil) provider))})

(defn list-permissions!
  [_pool]
  (let [codes (->> (contracts-roles/list-role-slugs (contracts-config))
                   (mapcat #(contracts-roles/role-permissions (contracts-config) %))
                   distinct sort vec)]
    (js/Promise.resolve
     {:permissions (mapv (fn [c]
                           {:id           c
                            :code         c
                            :resourceKind (first (str/split c #"\."))
                            :description  ""})
                         codes)})))

(defn- tool-row->map
  [{:keys [id label description risk_level]}]
  {:id id :label label :description description :risk-level risk_level})

(defn ^:async list-tools!
  [pool]
  (let [{:keys [rows]} (await (pg/query! pool
                               "SELECT id, label, description, risk_level FROM tool_definitions ORDER BY id ASC"
                               nil))]
    {:tools (mapv tool-row->map rows)}))

(defn get-bootstrap-context!
  [_pool primary-org bootstrap]
  (js/Promise.resolve
   {"primaryOrg"    {"id"        (:id primary-org)
                     "slug"      (:slug primary-org)
                     "name"      (:name primary-org)
                     "kind"      (:kind primary-org)
                     "isPrimary" (:is_primary primary-org)
                     "status"    (:status primary-org)}
    "bootstrapUser" {"id"           (get-in bootstrap [:user :id])
                     "email"        (get-in bootstrap [:user :email])
                     "displayName"  (get-in bootstrap [:user :display_name])
                     "membershipId" (get-in bootstrap [:membership :id])}}))

(defn- org-row->map
  [{:keys [id slug name kind is_primary status member_count role_count data_lake_count created_at updated_at]}]
  {:id id :slug slug :name name :kind kind
   :is-primary is_primary :status status
   :member-count (js/Number (or member_count 0))
   :role-count (js/Number (or role_count 0))
   :data-lake-count (js/Number (or data_lake_count 0))
   :created-at created_at :updated-at updated_at})

(defn ^:async list-orgs!
  [pool]
  (let [{:keys [rows]} (await (honey-query! pool (q-orgs/list-with-counts)))]
    {:orgs (mapv org-row->map rows)}))

(defn- org-response
  [org]
  {:org {:id (:id org) :slug (:slug org) :name (:name org)
         :kind (:kind org) :is-primary (:is_primary org) :status (:status org)}})

(defn ^:async create-org!
  [pool uid mid {:keys [name slug kind status]
                  :or {kind "customer" status "active"}}]
  (if (str/blank? name)
    (throw (js/Error. "name is required"))
    (let [s (slugify (or slug name) "org")
          org (await (honey-query-one! pool (q-orgs/insert {:slug s :name name
                                                            :kind kind :status status})))]
      (await (sync-contract-role-projections! pool))
      (await (append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                  :org-id (:id org) :action "org.create"
                                  :resource-kind "org" :resource-id (:id org)}))
      (org-response org))))

(defn self-org-slug
  [email]
  (let [normalized (or (normalize-email email) "user")]
    (slugify (str "self-" (str/replace normalized #"@" "-at-")) "self-user")))

(defn ^:async ensure-self-org!
  [pool email display-name]
  (let [slug (self-org-slug email)
        label (or (some-> display-name str str/trim not-empty)
                  (normalize-email email)
                  "User")
        name (str label " Self")
        existing (await (find-org-by-slug pool slug))]
    (if existing
      existing
      (await (honey-query-one! pool (q-orgs/insert {:slug slug :name name
                                                   :kind "self" :status "active"}))))))

(defn ^:async list-roles!
  [pool {:keys [org-id]}]
  (let [{:keys [rows]} (await (if org-id
                                (honey-query! pool (q-roles/list-by-org org-id))
                                (honey-query! pool (q-roles/list-all))))]
    {:roles (await (hydrate-role-maps pool rows))}))

(defn ^:async get-role!
  [pool role-id]
  (if-let [row (await (honey-query-one! pool (q-roles/by-id role-id)))]
    {:role (first (await (hydrate-role-maps pool [row])))}
    {:role nil}))

(defn ^:async create-role!
  [pool uid mid {:keys [org-id name slug permission-codes tool-policies]}]
  (cond
    (str/blank? org-id) (throw (js/Error. "org-id is required"))
    (str/blank? name)   (throw (js/Error. "name is required"))
    :else
    (let [s (slugify (or slug name) "role")
          role (await (ensure-role! pool {:org-id org-id :name name :slug s
                                          :scope-kind "org" :built-in false
                                          :system-managed false}))]
      (await (set-role-permissions! pool (:id role) (or permission-codes [])))
      (await (set-role-tool-policies! pool (:id role) (or tool-policies [])))
      (await (append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                  :org-id org-id :action "role.create"
                                  :resource-kind "role" :resource-id (:id role)}))
      {:role (first (await (hydrate-role-maps pool [role])))})))

(defn- user-row->map
  [memberships-by-user {:keys [id email display_name auth_provider external_subject status created_at updated_at]}]
  {:id id :email email :display-name display_name
   :auth-provider auth_provider :external-subject external_subject
   :status status :created-at created_at :updated-at updated_at
   :memberships (or (get memberships-by-user id) [])})

(defn- memberships-by-user
  [memberships]
  (reduce (fn [acc m]
            (update acc (:user-id m) (fnil conj []) m))
          {}
          memberships))

(defn ^:async list-users!
  [pool {:keys [org-id]}]
  (let [{:keys [rows]} (await (if org-id
                                (honey-query! pool (q-users/list-by-org org-id))
                                (honey-query! pool (q-users/list-all))))
        users rows
        user-ids (mapv :id users)
        {mem-rows :rows} (await (if org-id
                                  (honey-query! pool (q-users/memberships-for-users user-ids org-id))
                                  (honey-query! pool (q-users/all-memberships-for-users user-ids))))
        by-user (memberships-by-user (await (hydrate-memberships pool mem-rows)))]
    {:users (mapv #(user-row->map by-user %) users)}))

(defn- require-not-blank!
  [value message]
  (when (str/blank? value)
    (throw (js/Error. message))))

(defn ^:async upsert-user-actor-contract-for-membership!
  [pool org-id resolved-actor email display-name role-slugs]
  (let [org-row (await (find-org-by-id pool org-id))]
    (await (upsert-actor-contract-best-effort!
            {:actor-id resolved-actor
             :email email
             :display-name display-name
             :org-slug (:slug org-row)
             :role-slugs role-slugs
             :kind :agent}))))

(defn ^:async create-user!
  [pool uid mid {:keys [email display-name auth-provider external-subject status
                         membership-status org-id role-slugs role-ids is-default actor-id]
                  :or {auth-provider "local" status "active"
                       membership-status "active" is-default true}}]
  (require-not-blank! email "email is required")
  (require-not-blank! org-id "org-id is required")
  (let [dn (or display-name email)
        resolved-slugs (or role-slugs ["knowledge-worker"])
        actor-contract (find-user-actor-contract-by-email email)
        resolved-actor (or (normalize-actor-id actor-id) (:id actor-contract)
                           (user-actor-id-from-email email)
                           (default-membership-actor-id resolved-slugs))
        user (await (honey-query-one! pool (q-users/upsert {:email email :display-name dn
                                                            :auth-provider auth-provider
                                                            :external-subject external-subject
                                                            :status status})))
        ms (await (honey-query-one! pool (q-memberships/upsert {:user-id (:id user)
                                                                :org-id org-id
                                                                :status membership-status
                                                                :is-default is-default})))]
    (await (set-membership-roles! pool (:id ms) {:org-id org-id :role-ids (or role-ids [])
                                                 :role-slugs resolved-slugs :replace true}))
    (await (set-membership-actor-id! pool (:id ms) resolved-actor))
    (await (upsert-user-actor-contract-for-membership! pool org-id resolved-actor email dn resolved-slugs))
    (await (append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                :org-id org-id :action "user.create_or_update"
                                :resource-kind "user" :resource-id (:id user)}))
    {:user user :membership ms}))

(defn- secret-json->clj
  [value]
  (cond
    (nil? value) {}
    (map? value) value
    (string? value) (js->clj (js/JSON.parse value) :keywordize-keys true)
    :else (js->clj value :keywordize-keys true)))

(defn ^:async local-password-auth-record!
  [pool email]
  (when-let [normalized (normalize-email email)]
    (when-let [row (await (pg/query-one! pool
                                         "SELECT u.id AS user_id,
                                                 u.email,
                                                 u.display_name,
                                                 m.id AS membership_id,
                                                 m.org_id,
                                                 m.actor_id,
                                                 o.slug AS org_slug,
                                                 ac.secret_json
                                          FROM users u
                                          JOIN memberships m ON m.user_id = u.id
                                          JOIN orgs o ON o.id = m.org_id
                                          LEFT JOIN actor_credentials ac
                                            ON ac.user_id = u.id
                                           AND ac.org_id = m.org_id
                                           AND ac.provider = 'local'
                                           AND ac.kind = 'password'
                                           AND ac.status = 'active'
                                          WHERE lower(u.email) = lower($1)
                                            AND u.status = 'active'
                                            AND m.status = 'active'
                                          ORDER BY m.is_default DESC, m.created_at ASC
                                          LIMIT 1"
                                         [normalized]))]
      {:user-id (:user_id row)
       :email (:email row)
       :display-name (:display_name row)
       :membership-id (:membership_id row)
       :org-id (:org_id row)
       :org-slug (:org_slug row)
       :actor-id (:actor_id row)
       :secret-json (secret-json->clj (:secret_json row))})))

(defn ^:async list-memberships!
  [pool {:keys [org-id]}]
  (if (str/blank? org-id)
    (throw (js/Error. "org-id is required"))
    (let [{:keys [rows]} (await (honey-query! pool (q-memberships/list-by-org org-id)))]
      {:memberships (await (hydrate-memberships pool rows))})))

(defn ^:async get-membership!
  [pool membership-id]
  (if-let [row (await (honey-query-one! pool (q-memberships/by-id membership-id)))]
    {:membership (first (await (hydrate-memberships pool [row])))}
    {:membership nil}))

(defn ^:async set-membership-roles-public!
  [pool uid mid membership-id {:keys [org-id role-ids role-slugs actor-id replace]
                                 :or {replace true}}]
  (let [ms (await (honey-query-one! pool (q-memberships/bare-by-id membership-id)))]
    (when-not ms (throw (js/Error. "membership not found")))
    (let [resolved-actor (or (normalize-actor-id actor-id)
                             (normalize-actor-id (:actor_id ms))
                             (default-membership-actor-id (or role-slugs [])))]
      (await (set-membership-roles! pool membership-id {:org-id (or org-id (:org_id ms))
                                                        :role-ids (or role-ids [])
                                                        :role-slugs (or role-slugs [])
                                                        :replace replace}))
      (await (set-membership-actor-id! pool membership-id resolved-actor))
      (let [row (await (honey-query-one! pool (q-memberships/with-user-and-org membership-id)))]
        (await (upsert-actor-contract-best-effort!
                {:actor-id resolved-actor :email (:email row)
                 :display-name (:display_name row) :org-slug (:org_slug row)
                 :role-slugs (or role-slugs [])})))
      (await (append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                  :org-id (:org_id ms) :action "membership.roles.update"
                                  :resource-kind "membership" :resource-id membership-id}))
      {:membership nil})))

(defn- data-lake-row->map
  [{:keys [id org_id name slug kind config_json status created_at updated_at]}]
  {:id id :org-id org_id :name name :slug slug :kind kind
   :config (constraints-json->clj config_json)
   :status status :created-at created_at :updated-at updated_at})

(defn ^:async list-data-lakes!
  [pool {:keys [org-id]}]
  (if (str/blank? org-id)
    (throw (js/Error. "org-id is required"))
    (let [{:keys [rows]} (await (honey-query! pool (q-orgs/data-lake-by-org org-id)))]
      {:data-lakes (mapv data-lake-row->map rows)})))

(defn- data-lake-response
  [lake]
  {:data-lake {:id (:id lake) :org-id (:org_id lake) :name (:name lake)
               :slug (:slug lake) :kind (:kind lake) :status (:status lake)}})

(defn ^:async create-data-lake!
  [pool uid mid {:keys [org-id name slug kind config status]
                  :or {kind "workspace_docs" status "active"}}]
  (cond
    (str/blank? org-id) (throw (js/Error. "org-id is required"))
    (str/blank? name)   (throw (js/Error. "name is required"))
    :else
    (let [s          (slugify (or slug name) "lake")
          config-json (js/JSON.stringify (clj->js (or config {})))
          lake (await (honey-query-one! pool (q-orgs/insert-data-lake {:org-id org-id
                                                                       :name name :slug s
                                                                       :kind kind
                                                                       :config-json config-json
                                                                       :status status})))]
      (await (append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                  :org-id org-id :action "data_lake.create"
                                  :resource-kind "data_lake" :resource-id (:id lake)}))
      (data-lake-response lake))))

;; ---------------------------------------------------------------------------
;; Sessions
;; ---------------------------------------------------------------------------

(defn- session-row-response
  [row]
  {:session {:id            (:id row)
             :user-id       (:user_id row)
             :membership-id (:membership_id row)
             :org-id        (:org_id row)
             :email         (:email row)
             :display-name  (:display_name row)
             :auth-provider (:auth_provider row)
             :expires-at    (:expires_at row)
             :created-at    (:created_at row)}})

(defn ^:async create-session!
  [pool {:keys [token user-id membership-id org-id email display-name
                 auth-provider external-subject ip-address user-agent]}]
  (if (str/blank? token)
    (throw (js/Error. "token is required"))
    (let [ttl        (js/parseInt (or (aget js/process.env "KNOXX_SESSION_TTL_SECONDS") "86400") 10)
          salt       (generate-salt)
          token-hash (hash-token token salt)
          prefix     (token-prefix token)
          expires-at (js/Date. (+ (js/Date.now) (* ttl 1000)))
          row (await (honey-query-one! pool
                                        (q-sessions/insert {:user-id       user-id
                                                            :membership-id membership-id
                                                            :org-id        org-id
                                                            :token-hash    token-hash
                                                            :token-prefix  prefix
                                                            :salt          salt
                                                            :email         email
                                                            :display-name  display-name
                                                            :auth-provider (or auth-provider "github")
                                                            :external-subject external-subject
                                                            :ip-address    ip-address
                                                            :user-agent    user-agent
                                                            :expires-at    (.toISOString expires-at)})))]
      (session-row-response row))))

(defn ^:async get-session-by-token!
  [pool token]
  (when-not (str/blank? token)
    (try
      (let [prefix (token-prefix token)
            {:keys [rows]} (await (honey-query! pool (q-sessions/by-prefix prefix)))]
        (or (find-session-in-rows pool token rows)
            (let [{:keys [rows]} (await (honey-query! pool (q-sessions/all-active)))]
              (find-session-in-rows pool token rows))))
      (catch :default _
        nil))))

(defn ^:async delete-session-by-token!
  [pool token]
  (let [result (await (get-session-by-token! pool token))]
    (when-let [sid (get-in result [:session :id])]
      (try
        (await (honey-query! pool (q-sessions/delete-by-id sid)))
        (catch :default _ nil)))
    result))

(defn ^:async cleanup-expired-sessions!
  [pool]
  (try
    (let [{:keys [row-count]} (await (honey-query! pool (q-sessions/delete-expired)))]
      (when (> (or row-count 0) 0)
        (.log js/console "[policy-db] Cleaned up" row-count "expired sessions"))
      (or row-count 0))
    (catch :default _
      0)))

;; ---------------------------------------------------------------------------
;; Invites
;; ---------------------------------------------------------------------------

(defn- invite-response
  [row code]
  {:invite {:id         (:id row)
            :org-id     (:org_id row)
            :code       code
            :email      (:email row)
            :status     (:status row)
            :expires-at (:expires_at row)
            :created-at (:created_at row)}})

(defn ^:async create-invite!
  [pool uid mid {:keys [org-id email role-slugs inviter-membership-id]}]
  (cond
    (str/blank? org-id) (throw (js/Error. "org-id is required"))
    (str/blank? email)  (throw (js/Error. "email is required"))
    :else
    (let [slugs      (or role-slugs ["basic-user"])
          code       (.toString (.randomBytes crypto 8) "hex")
          expires-at (js/Date. (+ (js/Date.now) (* 7 24 3600 1000)))
          row (await (honey-query-one! pool
                                        (q-invites/insert {:org-id               org-id
                                                           :code                 code
                                                           :email                email
                                                           :inviter-membership-id (or inviter-membership-id mid)
                                                           :role-slugs-json      (js/JSON.stringify (clj->js slugs))
                                                           :expires-at           (.toISOString expires-at)})))]
      (await (append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                  :org-id org-id :action "invite.create"
                                  :resource-kind "invite" :resource-id (:id row)}))
      (invite-response row code))))

(defn- parse-role-slugs-json
  [value]
  (try
    (let [parsed (cond
                   (nil? value) []
                   (string? value) (js->clj (js/JSON.parse value))
                   :else (js->clj value))]
      (if (sequential? parsed) (vec parsed) []))
    (catch :default _ [])))

(defn- invite-error [message status]
  (doto (js/Error. message)
    (aset "status" status)))

(defn- redeemed-invite-response [updated code]
  {:invite {:id          (:id updated)
            :org-id      (:org_id updated)
            :code        code
            :email       (:email updated)
            :status      (:status updated)
            :redeemed-at (:redeemed_at updated)
            :created-at  (:created_at updated)}})

(defn ^:async redeem-invite!
  [pool code email]
  (if (or (str/blank? code) (str/blank? email))
    (throw (js/Error. "code and email are required"))
    (let [invite (await (honey-query-one! pool (q-invites/pending-by-code code)))]
      (if-not invite
        (throw (invite-error "Invalid or expired invite code" 400))
        (let [invite-email (str/lower-case (str (:email invite)))
              req-email    (str/lower-case (str email))
              role-slugs   (or (seq (parse-role-slugs-json (:role_slugs invite)))
                               ["basic-user"])]
          (when-not (= invite-email req-email)
            (throw (invite-error "Invite email does not match" 403)))
          (let [updated (await (honey-query-one! pool (q-invites/redeem (:id invite))))]
            (await (create-user! pool nil nil
                                 {:email (:email updated)
                                  :display-name (:email updated)
                                  :auth-provider "invite"
                                  :status "active"
                                  :membership-status "active"
                                  :org-id (:org_id updated)
                                  :role-slugs (vec role-slugs)
                                  :is-default true}))
            (redeemed-invite-response updated code)))))))

(defn- invite-row->map
  [{:keys [id org_id code email status role_slugs expires_at redeemed_at created_at]}]
  {:id id
   :org-id org_id
   :code code
   :email email
   :status status
   :role-slugs (parse-role-slugs-json role_slugs)
   :expires-at expires_at
   :redeemed-at redeemed_at
   :created-at created_at})

(defn ^:async list-invites!
  [pool {:keys [org-id status]}]
  (if (str/blank? org-id)
    (throw (js/Error. "org-id is required"))
    (let [{:keys [rows]} (await (honey-query! pool (if status
                                                     (q-invites/list-by-org-and-status org-id status)
                                                     (q-invites/list-by-org org-id))))]
      {:invites (mapv invite-row->map rows)})))

(defn sync-actor-contracts!
  [pool primary-org]
  (policy/sync-actor-projections!
   (sql-policy-store pool primary-org)
   (mapv :actor (list-actor-contracts))))

(defn sync-user-from-actor-contract!
  [pool primary-org opts]
  (sync-user-from-actor-contract!* pool primary-org opts))

(defn ^:async recover-session-secret!
  "Load the session secret from knoxx_config, generating and persisting one if absent.
   Returns Promise<string>."
  [pool]
  (let [{:keys [rows]} (await (pg/query! pool "SELECT value FROM knoxx_config WHERE key = 'session_secret'" nil))]
    (if-let [stored (:value (first rows))]
      (do
        (.log js/console "[knoxx-session] Recovered session secret from database")
        stored)
      (let [new-secret (.toString (.randomBytes crypto 32) "hex")]
        (await (pg/query! pool
                          "INSERT INTO knoxx_config (key, value) VALUES ('session_secret', $1)
                          ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value"
                          [new-secret]))
        (.log js/console "[knoxx-session] Generated and persisted session secret")
        new-secret))))

(defn ^:async update-user-actor!
  "Update a membership's actor-id and optionally its roles."
  [pool uid mid user-id {:keys [org-id actor-id role-slugs]}]
  (let [ms (await (honey-query-one! pool (q-memberships/by-user-and-org user-id org-id)))]
    (if-not ms
      (throw (js/Error. "membership not found"))
      (let [membership-id (:id ms)
            resolved-actor (or (normalize-actor-id actor-id)
                               (normalize-actor-id (:actor_id ms))
                               (default-membership-actor-id (or role-slugs [])))]
        (when (seq role-slugs)
          (await (set-membership-roles! pool membership-id
                                        {:org-id org-id :role-slugs role-slugs :replace true})))
        (await (set-membership-actor-id! pool membership-id resolved-actor))
        (await (append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                    :org-id org-id :action "user.update_actor"
                                    :resource-kind "user" :resource-id user-id}))
        {:ok true}))))

(defn- actor-credential-response [row]
  {:credential (when row
                 {:id                 (:id row)
                  :user-id            (:user_id row)
                  :org-id             (:org_id row)
                  :provider           (:provider row)
                  :kind               (:kind row)
                  :account-identifier (:account_identifier row)
                  :status             (:status row)})})

(defn ^:async upsert-actor-credential!
  "Upsert an actor credential by user-id + org-id + provider."
  [pool _uid _mid user-id {:keys [org-id provider kind account-identifier secret-json status]}]
  (let [ms (await (honey-query-one! pool (q-memberships/by-user-and-org user-id org-id)))]
    (if-not ms
      (throw (js/Error. "actor membership not found"))
      (let [row (await (pg/query-one! pool
                                      "INSERT INTO actor_credentials
                      (user_id, org_id, provider, kind, account_identifier, secret_json, status)
                    VALUES ($1::uuid, $2::uuid, $3, $4, $5, $6::jsonb, $7)
                    ON CONFLICT (user_id, org_id, provider, kind) DO UPDATE SET
                      account_identifier = COALESCE(EXCLUDED.account_identifier,
                                                    actor_credentials.account_identifier),
                      secret_json        = actor_credentials.secret_json || EXCLUDED.secret_json,
                      status             = EXCLUDED.status,
                      updated_at         = NOW()
                    RETURNING *"
                                      [user-id org-id provider (or kind "credential") account-identifier
                                       (js/JSON.stringify (clj->js (or secret-json {})))
                                       (or status "active")]))]
        (actor-credential-response row)))))

;; ---------------------------------------------------------------------------
;; Policy context helpers
;; ---------------------------------------------------------------------------

(defn context-pool [policy-context] (:pool policy-context))
(defn configured? [policy-context] (boolean (or (:pool policy-context) (:query! policy-context))))
(defn context-primary-org [policy-context] (:primary-org policy-context))
(defn context-bootstrap [policy-context] (:bootstrap policy-context))
(defn context-actor-user-id [policy-context] (:bootstrap-user-id policy-context))
(defn context-actor-membership-id [policy-context] (:bootstrap-membership-id policy-context))

(defn close!
  [policy-context]
  (when-let [pool (context-pool policy-context)]
    (pg/end-pool! pool)))

(defn query!
  "Transitional raw-query entrypoint for legacy routes. Prefer named policy DB
   functions backed by shape.db query builders."
  [policy-context sql-str params]
  (cond
    (:query! policy-context)
    ((:query! policy-context) sql-str params)

    (context-pool policy-context)
    (pg/query! (context-pool policy-context) sql-str params)

    :else
    (js/Promise.resolve nil)))

(defn bootstrap-context!
  [policy-context]
  (if-let [f (:bootstrap-context! policy-context)]
    (f)
    (get-bootstrap-context! (context-pool policy-context)
                            (context-primary-org policy-context)
                            (context-bootstrap policy-context))))

(defn resolve-context!
  [policy-context headers-like]
  (if-let [f (:resolve-context! policy-context)]
    (f headers-like)
    (resolve-request-context! (context-pool policy-context) headers-like)))

(defn sync-actor-contracts-for-context!
  [policy-context]
  (if-let [f (:sync-actor-contracts! policy-context)]
    (f)
    (sync-actor-contracts! (context-pool policy-context)
                           (context-primary-org policy-context))))

(defn sync-user-from-actor-contract-for-context!
  [policy-context opts]
  (if-let [f (:sync-user-from-actor-contract! policy-context)]
    (f opts)
    (sync-user-from-actor-contract! (context-pool policy-context)
                                    (context-primary-org policy-context)
                                    opts)))

(defn create-user-for-context!
  [policy-context payload]
  (create-user! (context-pool policy-context)
                (context-actor-user-id policy-context)
                (context-actor-membership-id policy-context)
                payload))

(defn local-password-auth-record-for-context!
  [policy-context email]
  (local-password-auth-record! (context-pool policy-context) email))

(defn create-invite-for-context!
  [policy-context payload]
  (create-invite! (context-pool policy-context)
                  (context-actor-user-id policy-context)
                  (context-actor-membership-id policy-context)
                  payload))

(defn create-org-for-context!
  [policy-context payload]
  (create-org! (context-pool policy-context)
               (context-actor-user-id policy-context)
               (context-actor-membership-id policy-context)
               payload))

(defn create-role-for-context!
  [policy-context payload]
  (create-role! (context-pool policy-context)
                (context-actor-user-id policy-context)
                (context-actor-membership-id policy-context)
                payload))

(defn create-data-lake-for-context!
  [policy-context payload]
  (create-data-lake! (context-pool policy-context)
                     (context-actor-user-id policy-context)
                     (context-actor-membership-id policy-context)
                     payload))

(defn set-membership-roles-for-context!
  [policy-context membership-id payload]
  (set-membership-roles-public! (context-pool policy-context)
                                (context-actor-user-id policy-context)
                                (context-actor-membership-id policy-context)
                                membership-id
                                payload))

(defn update-user-actor-for-context!
  [policy-context user-id payload]
  (update-user-actor! (context-pool policy-context)
                      (context-actor-user-id policy-context)
                      (context-actor-membership-id policy-context)
                      user-id
                      payload))

(defn upsert-actor-credential-for-context!
  [policy-context user-id payload]
  (upsert-actor-credential! (context-pool policy-context)
                            (context-actor-user-id policy-context)
                            (context-actor-membership-id policy-context)
                            user-id
                            payload))

(defn ^:async get-actor-credential!
  [policy-context actor-id provider]
  (let [credential (await (policy/get-actor-credential (sql-policy-store (context-pool policy-context)
                                                                         (context-primary-org policy-context))
                                                       actor-id
                                                       provider))]
    {:credential credential}))

;; ---------------------------------------------------------------------------
;; Initialisation
;; ---------------------------------------------------------------------------

(declare ensure-bootstrap-allowlist-users!)

(defn- create-policy-db-pool! [conn-str]
  (let [pool (pg/create-pool! {:connection-string  conn-str
                               :max                (env-positive-int "KNOXX_POLICY_DB_POOL_MAX" 6)
                               :idle-timeout-ms    (env-positive-int "KNOXX_POLICY_DB_IDLE_TIMEOUT_MS" 30000)
                               :connect-timeout-ms (env-positive-int "KNOXX_POLICY_DB_CONNECT_TIMEOUT_MS" 15000)})]
    (pg/on-pool-error! pool
      (fn [err _] (.error js/console "[policy-db] PG pool error:" (.-message err))))
    (pg/on-pool-connect! pool
      (fn [_] (when (env-truthy? "KNOXX_POLICY_DB_LOG_CONNECTS")
                (.log js/console "[policy-db] New PG client connected"))))
    pool))

(defn ^:async allowlist-best-effort! [pool primary-org opts]
  (when (seq (or (:bootstrapAllowlistEmails opts) (:bootstrap-allowlist-emails opts)))
    (try
      (await (ensure-bootstrap-allowlist-users! pool primary-org opts))
      (catch :default err
        (.warn js/console "[policy-db] allowlist failed:" (.-message err))))))

(defn ^:async sync-actor-contracts-best-effort! [pool primary-org]
  (try
    (await (sync-actor-contracts! pool primary-org))
    (catch :default err
      (.warn js/console "[policy-db] actor sync failed:" (.-message err)))))

(defn ^:async cleanup-expired-sessions-best-effort! [pool]
  (try
    (await (cleanup-expired-sessions! pool))
    (catch :default _ nil)))

(defn- policy-context-map [pool primary-org bootstrap]
  {:pool pool
   :primary-org primary-org
   :bootstrap bootstrap
   :bootstrap-user-id (get-in bootstrap [:user :id])
   :bootstrap-membership-id (get-in bootstrap [:membership :id])})

(defn ^:async initialise-policy-db! [pool opts]
  (await (db-schema/ensure-schema! pool))
  (await (db-schema/insert-permission-seeds! pool))
  (await (db-schema/insert-tool-seeds! pool))
  (let [primary-org (await (ensure-primary-org! pool opts))]
    (await (sync-contract-role-projections! pool))
    (let [bootstrap (await (ensure-bootstrap-user! pool primary-org opts))]
      (await (allowlist-best-effort! pool primary-org opts))
      (await (sync-actor-contracts-best-effort! pool primary-org))
      (await (honey-query! pool q-memberships/backfill-actor-ids))
      (cleanup-expired-sessions-best-effort! pool)
      (policy-context-map pool primary-org bootstrap))))

(defn ^:async create-policy-db
  "Initialise the policy DB. Returns Promise<CLJS policy context | nil>."
  [options]
  (let [opts (if (map? options)
               options
               (js->clj options :keywordize-keys true))
        conn-str (or (:connection-string opts) (:connectionString opts) "")]
    (when-not (str/blank? conn-str)
      (await (initialise-policy-db! (create-policy-db-pool! conn-str) opts)))))

(defn- split-bootstrap-values [value]
  (->> (str/split (str value) #"[\s,]+")
       (map str/trim)
       (remove str/blank?)
       distinct
       vec))

(defn- bootstrap-allowlist-emails [opts]
  (->> (split-bootstrap-values (or (:bootstrapAllowlistEmails opts) (:bootstrap-allowlist-emails opts) ""))
       (map str/lower-case)
       vec))

(defn- bootstrap-allowlist-role-slugs [opts]
  (let [role-slugs (split-bootstrap-values (or (:bootstrapAllowlistRoleSlugs opts)
                                               (:bootstrap-allowlist-role-slugs opts)
                                               ""))]
    (if (seq role-slugs) role-slugs ["knowledge-worker"])))

(defn ^:async ensure-bootstrap-allowlist-role! [pool org-id membership-id slug]
  (let [role (or (await (find-role pool {:slug slug :org-id org-id}))
                 (await (find-role pool {:slug slug :org-id nil})))]
    (when role
      (await (honey-query! pool (q-roles/insert-membership-role membership-id (:id role)))))))

(defn ^:async ensure-bootstrap-allowlist-user! [pool org-id role-slugs email]
  (let [user (await (honey-query-one! pool
                                      (q-users/upsert {:email email :display-name email
                                                       :auth-provider "bootstrap"
                                                       :external-subject nil :status "active"})))
        ms (await (honey-query-one! pool
                                    (q-memberships/upsert {:user-id (:id user) :org-id org-id
                                                           :status "active" :is-default false})))]
    (await (js/Promise.all
            (into-array
             (mapv (partial ensure-bootstrap-allowlist-role! pool org-id (:id ms))
                   role-slugs))))
    (await (set-membership-actor-id!
            pool (:id ms) (default-membership-actor-id role-slugs)))))

(defn ^:async ensure-bootstrap-allowlist-users! [pool primary-org opts]
  (let [emails (bootstrap-allowlist-emails opts)
        role-slugs (bootstrap-allowlist-role-slugs opts)
        org-id (:id primary-org)]
    (when (seq emails)
      (await (js/Promise.all
              (into-array
               (mapv (partial ensure-bootstrap-allowlist-user! pool org-id role-slugs)
                     emails)))))))

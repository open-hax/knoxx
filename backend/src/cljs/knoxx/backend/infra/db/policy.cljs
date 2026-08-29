(ns knoxx.backend.infra.db.policy
  "Policy DB public API — Mongo-backed (kanban 14-05 cutover complete).

   All durable policy state lives in MongoDB. This namespace is a thin
   CLJS-typed public surface that routes every slice (directory, roles, tools,
   sessions, config, audit, actor credentials, data lakes, invites) to its
   Mongo twin under infra.stores.mongo-policy-*. The twins own collection
   layout + indexes (setup-indexes!/ensure-indexes!); this layer keeps the
   row->map / response-wrapping layers and the public function signatures.

   create-policy-db returns a Promise<CLJS policy context | nil>. The context
   is plain CLJS data; its :pool value is now always nil — it is retained only
   so existing callers that pass (context-pool ctx) into policy functions need
   not change. The pool first arg on policy functions is ignored."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.password :as password]
            [knoxx.backend.infra.mongo-client :as mongo-client]
            [knoxx.backend.infra.stores.mongo-policy-store :as mongo-policy]
            [knoxx.backend.infra.stores.mongo-policy-directory :as mongo-directory]
            [knoxx.backend.infra.stores.mongo-policy-roles :as mongo-roles]
            [knoxx.backend.infra.stores.mongo-policy-tools :as mongo-tools]
            [knoxx.backend.infra.stores.mongo-policy-actor-credentials :as mongo-actor-creds]
            [knoxx.backend.infra.stores.mongo-policy-audit-events :as mongo-audit]
            [knoxx.backend.infra.stores.mongo-policy-data-lakes :as mongo-data-lakes]
            [knoxx.backend.infra.stores.mongo-policy-invites :as mongo-invites]
            [knoxx.backend.domain.actor.scope :as actor-scope]
            [knoxx.backend.domain.contracts.loader :as contracts-loader]
            [knoxx.backend.domain.contracts.roles :as contracts-roles]
            [knoxx.backend.infra.db.actors :as policy-actors]
            [knoxx.backend.domain.policy.protocol :as policy]
            [knoxx.backend.infra.registry.tools :as tool-registry]
            ["node:path" :as path]
            ["node:fs" :as fs]
            ["node:crypto" :as crypto]))

(declare set-membership-roles! sync-contract-role-projections! find-org-by-slug
         ensure-mongo-policy-db!)

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
;; Mongo db resolution — the pool arg on policy fns is ignored; every call
;; resolves the connected db (idempotent) and ensures twin indexes exist.
;; ---------------------------------------------------------------------------

(defn ^:async db!
  "Resolve the connected Mongo db, ensuring twin indexes once. Throws when
   Mongo is unavailable so write paths surface the failure rather than
   silently no-op."
  []
  (if-let [db (await (ensure-mongo-policy-db!))]
    db
    (throw (js/Error. "Mongo policy store unavailable"))))

;; ---------------------------------------------------------------------------
;; Basic lookups
;; ---------------------------------------------------------------------------

(defn ^:async find-org-by-id [_pool org-id]
  (await (mongo-directory/find-org-by-id! (await (db!)) org-id)))

(defn ^:async find-org-by-slug [_pool slug]
  (await (mongo-directory/find-org-by-slug (await (db!)) slug)))

(defn ^:async find-role [_pool {:keys [org-id slug]}]
  (await (mongo-roles/find-role (await (db!)) {:org-id org-id :slug slug})))

;; ---------------------------------------------------------------------------
;; Role management
;; ---------------------------------------------------------------------------

(defn ^:async ensure-role!
  [_pool {:keys [org-id name slug scope-kind built-in system-managed]}]
  (await (mongo-roles/ensure-role! (await (db!))
                                   {:org-id org-id :name name :slug slug
                                    :scope-kind scope-kind :built-in built-in
                                    :system-managed system-managed})))

(defn- ^:async set-role-permissions!
  [_pool role-id permission-codes]
  (await (mongo-roles/set-role-permissions! (await (db!)) role-id (unique permission-codes))))

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

(defn- policy-with-constraints-json
  [p]
  (assoc p :constraints-json (js/JSON.stringify (clj->js (:constraints p)))))

(defn ^:async set-role-tool-policies!
  [_pool role-id tool-policies]
  (let [db (await (db!))
        normalized (mapv (comp policy-with-constraints-json normalize-tool-policy) tool-policies)]
    (await (mongo-tools/ensure-tool-definitions! db (mapv :tool-id normalized)))
    (await (mongo-tools/set-role-tool-policies! db role-id normalized))))

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
  [_pool {:keys [org-id role-ids role-slugs]}]
  (let [base-ids (set (map str (or role-ids [])))]
    (if (empty? role-slugs)
      (vec base-ids)
      (let [requested (requested-role-slugs role-slugs)
            alias-map (into {} (map (fn [slug] [slug (role-slug-aliases slug)])) requested)
            query-slugs (->> (vals alias-map) (mapcat identity) distinct vec)
            rows (await (mongo-roles/list-roles-by-slugs! (await (db!)) query-slugs org-id))]
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
    (await (mongo-roles/set-membership-roles! (await (db!)) membership-id (boolean replace) resolved-ids))))

(defn ^:async set-membership-tool-policies!
  [_pool membership-id tool-policies]
  (let [db (await (db!))
        normalized (mapv (comp policy-with-constraints-json normalize-tool-policy) tool-policies)]
    (await (mongo-tools/ensure-tool-definitions! db (mapv :tool-id normalized)))
    (await (mongo-tools/set-membership-tool-policies! db membership-id normalized))))

(defn ^:async set-membership-actor-id!
  [_pool membership-id actor-id]
  (await (mongo-directory/set-membership-actor-id! (await (db!)) membership-id actor-id)))

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
  [_pool roles]
  (if (empty? roles)
    []
    (let [db (await (db!))
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
     :actorId (or (normalize-actor-id actor_id)
                  (default-membership-actor-id (map :slug roles)))
     :orgName org_name :orgSlug org_slug :status status :isDefault is_default
     :createdAt created_at :updatedAt updated_at
     :roles roles :toolPolicies (or (get tools-by-m id) [])}))

(defn ^:async hydrate-memberships
  [_pool memberships]
  (if (empty? memberships)
    []
    (let [db (await (db!))
          membership-ids (mapv :id memberships)
          role-rows (await (mongo-roles/roles-for-memberships! db membership-ids))
          tool-rows (await (mongo-tools/tool-policies-for-memberships! db membership-ids))
          roles-by-m (grouped-membership-roles role-rows)
          tools-by-m (grouped-membership-tool-policies tool-rows)]
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

(defn- ^:async find-request-membership-row [_pool headers-like]
  (let [membership-id (header-value headers-like "x-knoxx-membership-id")
        user-email    (some-> (header-value headers-like "x-knoxx-user-email") str/lower-case)
        org-id        (header-value headers-like "x-knoxx-org-id")
        org-slug      (some-> (header-value headers-like "x-knoxx-org-slug") str/lower-case)]
    (cond
      (and (str/blank? membership-id) (str/blank? user-email))
      (throw (http-error 401 "Missing x-knoxx-user-email or x-knoxx-membership-id"
                         "request_context_missing"))

      (not (str/blank? membership-id))
      (await (mongo-directory/find-membership-row-with-user-org! (await (db!)) membership-id))

      :else
      (await (mongo-directory/find-membership-row-by-email-and-org!
              (await (db!))
              {:user-email user-email :org-id org-id :org-slug org-slug})))))

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
      (let [rows (await (mongo-roles/list-roles-by-ids! (await (db!)) role-ids))]
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
        ;; The membership's *stored* actor binding, or nil. Kept separate from
        ;; actor-id below, which falls back to a role-derived default and is
        ;; therefore never nil — so it cannot answer "was an actor assigned?".
        ;; Anything deciding authority (which credentials a token may read) must
        ;; use the binding; the default is a display and role convenience.
        actor-binding (normalize-actor-id (:actor_id membership-row))
        actor-id (or actor-binding
                     (default-membership-actor-id role-slugs))]
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

;; ---------------------------------------------------------------------------
;; Bootstrap & contract sync
;; ---------------------------------------------------------------------------

;; Actor projection sync: reimplemented against the Mongo twins, replacing the
;; SqlPolicyStore ActorProjectionStore (domain.policy.sql-adapter, deleted in
;; the PG cutover). Mirrors sql-adapter/project-actor!: validate actor →
;; upsert user → resolve target org → upsert membership w/ actor-id → set roles
;; (contract-projection true) best-effort.

(defn- actor-projection-role-slugs
  [actor]
  (->> (or (:actor/roles actor) [])
       (map (fn [role]
              (cond
                (keyword? role) (-> role name (str/replace #"_" "-"))
                (string? role) (-> role str/trim (str/replace #"_" "-"))
                :else nil)))
       (remove str/blank?)
       distinct
       vec))

(defn- ^:async project-actor-role-slugs!
  "Best-effort set the projected membership's roles, keeping the membership
   when role projection fails (mirrors sql-adapter's .catch behavior)."
  [membership actor-id role-slugs]
  (try
    (await (set-membership-roles! nil (:id membership)
                                  {:org-id (:org_id membership)
                                   :role-slugs role-slugs :role-ids []
                                   :replace true :contract-projection true}))
    (catch :default err
      (.warn js/console
             "[policy-mongo] actor role projection failed; keeping actor membership"
             actor-id (.-message err)))))

(defn ^:async project-actor-via-store!
  "Upsert the user/membership/roles projection for one actor contract against
   the Mongo twins. Returns the membership row."
  [_pool primary-org actor]
  (let [validated (policy/validate-actor! actor)
        actor-id (:actor/id validated)
        db (await (db!))
        email (or (normalize-email (:actor/email validated))
                  (policy-actors/actor-email-from-id actor-id))
        display-name (or (some-> (:actor/label validated) str str/trim not-empty)
                         actor-id email)
        role-slugs (actor-projection-role-slugs validated)
        user (await (mongo-directory/create-user!
                     db {:email email :display-name display-name
                         :auth-provider "actor-contract" :external-subject nil
                         :status "active"}))
        org (or (when-let [org-slug (some-> (:actor/org validated) str str/trim not-empty)]
                  (await (mongo-directory/find-org-by-slug db org-slug)))
                primary-org)]
    (when-not org
      (throw (js/Error. "primary org is required for actor projection sync")))
    (let [membership (await (mongo-directory/upsert-membership!
                             db {:user-id (:id user) :org-id (:id org)
                                 :status "active" :is-default true}))]
      (await (mongo-directory/set-membership-actor-id! db (:id membership) actor-id))
      (await (project-actor-role-slugs! membership actor-id role-slugs))
      membership)))

(defn ^:async sync-actor-projections!
  [pool primary-org actors]
  (await (promise-each actors #(project-actor-via-store! pool primary-org %)))
  nil)

(defn- sync-user-from-actor-contract!* [pool primary-org payload]
  (let [actor-id (normalize-actor-id (or (:actor-id payload) (:actor_id payload)))
        email    (normalize-email (:email payload))]
    (if-not (or email actor-id)
      (js/Promise.resolve nil)
      (if-let [contract (or (find-actor-contract-by-id actor-id)
                            (find-user-actor-contract-by-email email))]
        (sync-actor-projections! pool primary-org [(:actor contract)])
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
  [_pool opts]
  (let [primary-org-slug (or (:primaryOrgSlug opts) (:primary-org-slug opts) "open-hax")
        primary-org-name (or (:primaryOrgName opts) (:primary-org-name opts) "Open Hax")
        primary-org-kind (or (:primaryOrgKind opts) (:primary-org-kind opts) "platform_owner")
        slug (slugify primary-org-slug "open-hax")]
    (await (mongo-directory/ensure-primary-org! (await (db!))
                                                {:slug slug
                                                 :name (str primary-org-name)
                                                 :kind (str primary-org-kind)}))))

(defn ^:async ensure-bootstrap-user!
  [pool primary-org opts]
  (let [db (await (db!))
        email (str/lower-case (str (or (:bootstrapSystemAdminEmail opts)
                                       (:bootstrap-system-admin-email opts)
                                       "system-admin@open-hax.local")))
        dn (str (or (:bootstrapSystemAdminName opts)
                    (:bootstrap-system-admin-name opts)
                    "Knoxx System Admin"))
        user (await (mongo-directory/create-user! db {:email email :display-name dn
                                                      :auth-provider "bootstrap"
                                                      :external-subject nil :status "active"}))
        membership (await (mongo-directory/upsert-membership! db {:user-id (:id user)
                                                                  :org-id (:id primary-org)
                                                                  :status "active"
                                                                  :is-default true}))]
    (await (set-membership-roles! pool (:id membership) {:org-id (:id primary-org)
                                                         :role-slugs ["system-admin"]
                                                         :replace true}))
    (await (set-membership-actor-id! pool (:id membership) "system_admin"))
    {:user user :membership membership}))

(defn ^:async ensure-bootstrap-local-password!
  "Idempotently project the environment-owned bootstrap password into the
   local credential store. Blank passwords revoke any previously provisioned
   local bootstrap credential."
  ([db primary-org bootstrap opts]
   (ensure-bootstrap-local-password!
    db primary-org bootstrap opts
    {:deactivate-credential! mongo-actor-creds/deactivate-actor-credential!
     :encode-password password/hash-password
     :upsert-credential! mongo-actor-creds/upsert-actor-credential!}))
  ([db primary-org bootstrap opts
    {:keys [deactivate-credential! encode-password upsert-credential!]}]
   (let [configured-password (some-> (or (:bootstrapSystemAdminPassword opts)
                                         (:bootstrap-system-admin-password opts))
                                     str not-empty)
         user-id (get-in bootstrap [:user :id])
         org-id (:id primary-org)]
     (if configured-password
       (await (upsert-credential!
               db user-id org-id "local"
               {:kind "password"
                :account-identifier (get-in bootstrap [:user :email])
                :secret-json (encode-password configured-password)
                :status "active"}))
       (await (deactivate-credential! db user-id org-id "local" "password")))
     nil)))

;; ---------------------------------------------------------------------------
;; Audit
;; ---------------------------------------------------------------------------

(defn- ^:async append-audit! [_pool {:keys [before after] :as opts}]
  (when-let [db (await (mongo-client/init-mongo!))]
    (await (mongo-audit/insert-event!
            db (assoc opts
                      :before-json (when before (js/JSON.stringify (clj->js before)))
                      :after-json  (when after  (js/JSON.stringify (clj->js after))))))))

;; ---------------------------------------------------------------------------
;; Session persistence
;; ---------------------------------------------------------------------------

(defn- ^:async ensure-mongo-policy-db!
  "Connect to Mongo (idempotent) and ensure the policy-store indexes exist.
   Returns the db, or nil when Mongo is unavailable. Index setup is guarded by
   ensure-indexes! so a bad spec never crash-loops startup."
  []
  (let [db (await (mongo-client/init-mongo!))]
    (when db
      (await (mongo-policy/ensure-indexes! db)))
    db))

(defn ^:async touch-session-best-effort!
  [_pool session-id]
  (await (mongo-policy/touch-session-best-effort! session-id)))

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
  [_pool provider]
  (when (str/blank? provider)
    (throw (js/Error. "provider is required")))
  (if-let [db (await (ensure-mongo-policy-db!))]
    {:credentials (mapv mongo-actor-creds/credential-row->response
                        (await (mongo-actor-creds/list-actor-credentials-by-provider! db provider)))}
    {:credentials []}))

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
  [_pool]
  (let [rows (await (mongo-tools/list-tools! (await (db!))))]
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
    (await (promise-each rows (partial accumulate-org-lake-count! db acc)))
    @acc))

(defn ^:async list-orgs!
  [_pool]
  (let [db (await (db!))
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
                 rows)}))

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
          org (await (mongo-directory/create-org! (await (db!)) {:slug s :name name
                                                                 :kind kind :status status}))]
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
  [_pool email display-name]
  (let [db (await (db!))
        slug (self-org-slug email)
        label (or (some-> display-name str str/trim not-empty)
                  (normalize-email email)
                  "User")
        name (str label " Self")
        existing (await (mongo-directory/find-org-by-slug db slug))]
    (if existing
      existing
      (await (mongo-directory/create-org! db {:slug slug :name name
                                              :kind "self" :status "active"})))))

(defn ^:async list-roles!
  [pool {:keys [org-id]}]
  (let [rows (await (mongo-roles/list-roles! (await (db!)) {:org-id org-id}))]
    {:roles (await (hydrate-role-maps pool rows))}))

(defn ^:async get-role!
  [pool role-id]
  (if-let [row (await (mongo-roles/get-role-by-id! (await (db!)) role-id))]
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

(defn- credential-row->map
  [{:keys [id provider kind account_identifier status secret_json created_at updated_at]}]
  {:id                 id
   :provider           provider
   :kind               kind
   :accountIdentifier  account_identifier
   :status             status
   :configuredFields   (vec (remove str/blank? (map str (keys (js->clj (or secret_json {}) :keywordize-keys true)))))
   :createdAt          created_at
   :updatedAt          updated_at})

(defn- user-row->map
  [memberships-by-user credentials-by-user {:keys [id email display_name auth_provider external_subject status created_at updated_at]}]
  {:id id :email email :displayName display_name
   :authProvider auth_provider :externalSubject external_subject
   :status status :createdAt created_at :updatedAt updated_at
   :credentials (or (get credentials-by-user id) [])
   :memberships (or (get memberships-by-user id) [])})

(defn- memberships-by-user
  [memberships]
  (reduce (fn [acc m]
            (update acc (:userId m) (fnil conj []) m))
          {}
          memberships))

(defn- credentials-by-user
  [credentials]
  (reduce (fn [acc c]
            (update acc (:user_id c) (fnil conj []) (credential-row->map c)))
          {}
          credentials))

(defn ^:async list-users!
  [pool {:keys [org-id]}]
  (let [db (await (db!))
        users (await (mongo-directory/list-users! db {:org-id org-id}))
        user-ids (mapv :id users)
        mem-rows (await (mongo-directory/memberships-for-users-with-org! db user-ids org-id))
        by-user (memberships-by-user (await (hydrate-memberships pool mem-rows)))
        cred-rows (if org-id
                    (await (mongo-actor-creds/list-credentials-for-users-org! db user-ids org-id))
                    [])
        by-cred-user (credentials-by-user cred-rows)]
    {:users (mapv #(user-row->map by-user by-cred-user %) users)}))

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
        db (await (db!))
        user (await (mongo-directory/create-user! db {:email email :display-name dn
                                                      :auth-provider auth-provider
                                                      :external-subject external-subject
                                                      :status status}))
        ms (await (mongo-directory/upsert-membership! db {:user-id (:id user)
                                                          :org-id org-id
                                                          :status membership-status
                                                          :is-default is-default}))]
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
  "Resolve the active user + default-first active membership for a local
   password login, plus the active local/password credential. Composes the
   directory + actor-credentials twins; preserves the PG return shape. Returns
   nil when no active user/membership matches."
  [_pool email]
  (when-let [normalized (normalize-email email)]
    (let [db (await (db!))
          user (await (mongo-directory/find-user-by-email! db normalized))]
      (when (and user (= "active" (:status user)))
        (when-let [row (await (mongo-directory/find-membership-row-by-email-and-org!
                               db {:user-email normalized :active-only true}))]
          (when (and (= "active" (:user_status row)) (= "active" (:status row)))
            (let [cred (await (mongo-actor-creds/get-credential-by-user-org-provider-kind!
                               db (:user_id row) (:org_id row) "local" "password"))]
              {:user-id (:user_id row)
               :email (:email row)
               :display-name (:display_name row)
               :membership-id (:id row)
               :org-id (:org_id row)
               :org-slug (:org_slug row)
               :actor-id (:actor_id row)
               :secret-json (secret-json->clj (:secret_json cred))})))))))

(defn ^:async list-memberships!
  [pool {:keys [org-id]}]
  (if (str/blank? org-id)
    (throw (js/Error. "org-id is required"))
    (let [rows (await (mongo-directory/list-memberships-with-org! (await (db!)) {:org-id org-id}))]
      {:memberships (await (hydrate-memberships pool rows))})))

(defn ^:async get-membership!
  [pool membership-id]
  (if-let [row (await (mongo-directory/find-membership-row-with-user-org! (await (db!)) membership-id))]
    {:membership (first (await (hydrate-memberships pool [row])))}
    {:membership nil}))

(defn ^:async set-membership-roles-public!
  [pool uid mid membership-id {:keys [org-id role-ids role-slugs actor-id replace]
                                 :or {replace true}}]
  (let [ms (await (mongo-directory/get-membership! (await (db!)) membership-id))]
    (when-not ms (throw (js/Error. "membership not found")))
    (let [resolved-actor (or (normalize-actor-id actor-id)
                             (normalize-actor-id (:actor_id ms))
                             (default-membership-actor-id (or role-slugs [])))]
      (await (set-membership-roles! pool membership-id {:org-id (or org-id (:org_id ms))
                                                        :role-ids (or role-ids [])
                                                        :role-slugs (or role-slugs [])
                                                        :replace replace}))
      (await (set-membership-actor-id! pool membership-id resolved-actor))
      (let [row (await (mongo-directory/find-membership-row-with-user-org! (await (db!)) membership-id))]
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
  [_pool {:keys [org-id]}]
  (if (str/blank? org-id)
    (throw (js/Error. "org-id is required"))
    (when-let [db (await (ensure-mongo-policy-db!))]
      {:data-lakes (mapv data-lake-row->map
                         (await (mongo-data-lakes/list-data-lakes-by-org! db org-id)))})))

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
    (when-let [db (await (ensure-mongo-policy-db!))]
      (let [s    (slugify (or slug name) "lake")
            lake (await (mongo-data-lakes/create-data-lake!
                         db org-id {:name name :slug s :kind kind
                                    :config-json (clj->js (or config {}))
                                    :status status}))]
        (await (append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                    :org-id org-id :action "data_lake.create"
                                    :resource-kind "data_lake" :resource-id (:id lake)}))
        (data-lake-response lake)))))

;; ---------------------------------------------------------------------------
;; Sessions
;; ---------------------------------------------------------------------------

(defn ^:async create-session!
  [_pool {:keys [token] :as opts}]
  (if (str/blank? token)
    (throw (js/Error. "token is required"))
    (await (mongo-policy/create-session! (await (db!)) opts))))

(defn ^:async get-session-by-token!
  [_pool token]
  (when-let [db (await (ensure-mongo-policy-db!))]
    (await (mongo-policy/get-session-by-token! db token))))

(defn ^:async delete-session-by-token!
  [_pool token]
  (when-let [db (await (ensure-mongo-policy-db!))]
    (await (mongo-policy/delete-session-by-token! db token))))

(defn ^:async cleanup-expired-sessions!
  [_pool]
  (if-let [db (await (ensure-mongo-policy-db!))]
    (await (mongo-policy/cleanup-expired-sessions! db))
    0))

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
    (when-let [db (await (ensure-mongo-policy-db!))]
      (let [slugs      (or role-slugs ["basic-user"])
            code       (.toString (.randomBytes crypto 8) "hex")
            expires-at (js/Date. (+ (js/Date.now) (* 7 24 3600 1000)))
            row (await (mongo-invites/insert-invite!
                        db {:org-id               org-id
                            :code                 code
                            :email                email
                            :inviter-membership-id (or inviter-membership-id mid)
                            :role-slugs-json      (js/JSON.stringify (clj->js slugs))
                            :expires-at           (.toISOString expires-at)}))]
        (await (append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                   :org-id org-id :action "invite.create"
                                   :resource-kind "invite" :resource-id (:id row)}))
      (invite-response row code)))))

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
    (let [db (await (db!))
          invite (await (mongo-invites/pending-by-code! db code))]
      (if-not invite
        (throw (invite-error "Invalid or expired invite code" 400))
        (let [invite-email (str/lower-case (str (:email invite)))
              req-email    (str/lower-case (str email))
              role-slugs   (or (seq (parse-role-slugs-json (:role_slugs invite)))
                               ["basic-user"])]
          (when-not (= invite-email req-email)
            (throw (invite-error "Invite email does not match" 403)))
          (let [updated (await (mongo-invites/redeem-invite! db (:id invite)))]
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
  [_pool {:keys [org-id status]}]
  (if (str/blank? org-id)
    (throw (js/Error. "org-id is required"))
    (when-let [db (await (ensure-mongo-policy-db!))]
      {:invites (mapv invite-row->map
                      (await (mongo-invites/list-invites-by-org! db org-id status)))})))

(defn ^:async sync-actor-contracts!
  [pool primary-org]
  (await (sync-actor-projections! pool primary-org (mapv :actor (list-actor-contracts)))))

(defn sync-user-from-actor-contract!
  [pool primary-org opts]
  (sync-user-from-actor-contract!* pool primary-org opts))

(defn ^:async recover-session-secret!
  "Load the session secret from the Mongo config collection, generating and
   persisting one if absent. Returns Promise<string>."
  [_pool]
  (if-let [db (await (ensure-mongo-policy-db!))]
    (await (mongo-policy/recover-session-secret! db nil))
    (throw (js/Error. "Mongo policy store unavailable"))))

(defn ^:async update-user-actor!
  "Update a membership's actor-id and optionally its roles."
  [pool uid mid user-id {:keys [org-id actor-id role-slugs]}]
  (let [ms (await (mongo-directory/find-membership-by-user-and-org! (await (db!)) user-id org-id))]
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
                 (let [secret (js->clj (or (:secret_json row) {}) :keywordize-keys true)]
                   {:id                 (:id row)
                    :userId             (:user_id row)
                    :orgId              (:org_id row)
                    :provider           (:provider row)
                    :kind               (:kind row)
                    :accountIdentifier  (:account_identifier row)
                    :status             (:status row)
                    :secretJson         secret
                    :configuredFields   (vec (remove str/blank? (map name (keys secret))))}))})

(defn ^:async upsert-actor-credential!
  "Upsert an actor credential by user-id + org-id + provider."
  [_pool _uid _mid user-id {:keys [org-id provider kind account-identifier secret-json status]}]
  (if-let [db (await (ensure-mongo-policy-db!))]
    (let [row (await (mongo-actor-creds/upsert-actor-credential!
                      db user-id org-id provider
                      {:kind kind
                       :account-identifier account-identifier
                       :secret-json (js->clj (or secret-json {}) :keywordize-keys true)
                       :status status}))]
      (actor-credential-response row))
    (throw (js/Error. "Mongo policy store unavailable"))))

;; ---------------------------------------------------------------------------
;; Policy context helpers
;; ---------------------------------------------------------------------------

(defn context-pool [policy-context] (:pool policy-context))
(defn configured? [policy-context] (boolean (or (:mongo? policy-context) (:query! policy-context))))
(defn context-primary-org [policy-context] (:primary-org policy-context))
(defn context-bootstrap [policy-context] (:bootstrap policy-context))
(defn context-actor-user-id [policy-context] (:bootstrap-user-id policy-context))
(defn context-actor-membership-id [policy-context] (:bootstrap-membership-id policy-context))

(defn close!
  "No-op for the Mongo policy store: the shared Mongo client is owned by
   infra.mongo-client and closed by the global shutdown path, not per
   policy-context. Retained so graceful-shutdown's call site is unchanged."
  [_policy-context]
  (js/Promise.resolve nil))

(defn query!
  "Deprecated raw-SQL entrypoint. The Mongo policy store no longer executes
   SQL, so a Mongo policy-context resolves nil here and legacy callers degrade
   to their empty-result fallbacks. A context that injects its own :query! fn
   (e.g. mailbox routes wiring a custom executor) still has it honored. Use the
   named policy DB functions instead."
  [policy-context sql-str params]
  (if-let [f (:query! policy-context)]
    (f sql-str params)
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
  "An actor's active credential for a provider.

   scope narrows the membership lookup: {:org-id, :membership-id}. actor_id is
   unique nowhere — not even within an org — so an unnarrowed lookup refuses an
   ambiguous actor rather than returning some member's secret. A caller holding a
   request context should pass its membership id, which is exact.

   Dispatches through the context's :get-actor-credential! when it carries one,
   the same seam :resolve-context! and :query! already use. This is the query
   seam mongo-policy-actor-credentials names as the correct dispatch point, and
   it is what lets a harness supply credentials without a database: until it
   existed, every credential-backed tool was unreachable from a test, which is
   most of the Discord and Bluesky surface."
  ([policy-context actor-id provider] (get-actor-credential! policy-context actor-id provider nil))
  ([policy-context actor-id provider scope]
   (if-let [f (:get-actor-credential! policy-context)]
     (await (f actor-id provider scope))
     (when-let [db (await (ensure-mongo-policy-db!))]
       {:credential (mongo-actor-creds/credential-row->response
                     (await (mongo-actor-creds/get-actor-credential-by-actor-and-provider!
                             db actor-id provider scope)))}))))

;; ---------------------------------------------------------------------------
;; Initialisation
;; ---------------------------------------------------------------------------

(declare ensure-bootstrap-allowlist-users!)

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

(defn- policy-context-map [primary-org bootstrap]
  {:pool nil
   :mongo? true
   :primary-org primary-org
   :bootstrap bootstrap
   :bootstrap-user-id (get-in bootstrap [:user :id])
   :bootstrap-membership-id (get-in bootstrap [:membership :id])})

(defn ^:async initialise-policy-db!
  "Mongo-backed initialisation: connect + ensure twin indexes (guarded so a
   bad spec never crash-loops startup), seed the primary org + contract role
   projections + bootstrap user, run best-effort allowlist/actor-sync/cleanup,
   then return the policy context map."
  [opts]
  (let [db (await (ensure-mongo-policy-db!))]
    (when-not db
      (throw (js/Error. "Mongo policy store unavailable")))
    (let [primary-org (await (ensure-primary-org! nil opts))]
      (await (sync-contract-role-projections! nil))
      (let [bootstrap (await (ensure-bootstrap-user! nil primary-org opts))]
        (await (ensure-bootstrap-local-password! db primary-org bootstrap opts))
        (await (allowlist-best-effort! nil primary-org opts))
        (await (sync-actor-contracts-best-effort! nil primary-org))
        (await (cleanup-expired-sessions-best-effort! nil))
        (policy-context-map primary-org bootstrap)))))

(defn ^:async create-policy-db
  "Initialise the Mongo-backed policy DB. Returns Promise<CLJS policy context |
   nil>; nil only when Mongo is unavailable."
  [options]
  (let [opts (if (map? options)
               options
               (js->clj options :keywordize-keys true))]
    (try
      (await (initialise-policy-db! opts))
      (catch :default err
        (.error js/console "[policy-db] Mongo policy DB init failed:" (.-message err))
        nil))))

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
  (let [db (await (db!))
        role (or (await (find-role pool {:slug slug :org-id org-id}))
                 (await (find-role pool {:slug slug :org-id nil})))]
    (when role
      ;; replace? false: additive link, mirroring the PG insert-membership-role
      ;; (ON CONFLICT DO NOTHING) used during allowlist bootstrap.
      (await (mongo-roles/set-membership-roles! db membership-id false [(:id role)])))))

(defn ^:async ensure-bootstrap-allowlist-user! [pool org-id role-slugs email]
  (let [db (await (db!))
        user (await (mongo-directory/create-user! db {:email email :display-name email
                                                      :auth-provider "bootstrap"
                                                      :external-subject nil :status "active"}))
        ms (await (mongo-directory/upsert-membership! db {:user-id (:id user) :org-id org-id
                                                          :status "active" :is-default false}))]
    (await (promise-each role-slugs
                         (partial ensure-bootstrap-allowlist-role! pool org-id (:id ms))))
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

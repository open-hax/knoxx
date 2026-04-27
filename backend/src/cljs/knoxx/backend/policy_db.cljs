(ns knoxx.backend.policy-db
  "Policy database — CLJS port of policy-db.mjs.

   Manages orgs, users, memberships, roles, permissions, and tool policies
   via PostgreSQL. Uses HoneySQL for query building with numbered params.

   The factory function `create-policy-db` returns a JS object with async
   methods for use from the CLJS runtime via (aget runtime \"policyDb\").

   The `pg` npm package is imported directly via `:keep-as-import #{\"pg\"}`
   in shadow-cljs.edn, which tells shadow-cljs to skip dependency analysis
   and generate a bare import statement. Node.js resolves the transitive
   Node.js built-in deps (dns, events, net, tls, buffer, stream) at runtime.

   See: https://github.com/thheller/shadow-cljs/issues/1219"
  (:require [clojure.string :as str]
            [cljs.reader :as reader]
            [honey.sql :as sql]
            [knoxx.backend.contracts.roles :as contracts-roles]
            [knoxx.backend.tools.registry :as tool-registry]
            ["pg" :as pg]
            ["node:fs" :as fs]
            ["node:path" :as path]
            ["node:crypto" :as crypto]))

(declare contracts-config
         query!
         query-one!
         find-org-by-slug
         normalize-actor-id
         set-membership-roles!)

;; ---------------------------------------------------------------------------
;; Data Constants
;; ---------------------------------------------------------------------------

(defn- default-contracts-dir
  []
  (let [configured (some-> (aget js/process.env "CONTRACTS_DIR") str str/trim not-empty)
        cwd (.cwd js/process)
        candidates (->> [configured
                         "contracts"
                         "../contracts"
                         "packages/agents/knoxx/contracts"
                         "orgs/open-hax/openplanner/packages/agents/knoxx/contracts"]
                        (keep identity)
                        (map #(.resolve path cwd %))
                        distinct
                        vec)]
    (or (some (fn [candidate]
                (when (.existsSync fs candidate)
                  candidate))
              candidates)
        (.resolve path cwd (or configured "contracts")))))

(defn- contracts-dir
  []
  (default-contracts-dir))

(defn- safe-read-edn
  [file-path]
  (try
    (reader/read-string (str (.readFileSync fs file-path "utf8")))
    (catch :default _
      nil)))

(defn- normalize-email
  [value]
  (some-> value str str/trim str/lower-case not-empty))

(defn- role-slug->contract-keyword
  [value]
  (when-let [slug (some-> value str str/trim not-empty)]
    (keyword "role" (str/replace slug #"_" "-"))))

(defn- user-actor-id-from-email
  [email]
  (some-> email
          normalize-email
          (str/replace #"[^a-z0-9]+" "_")
          (str/replace #"^_+|_+$" "")
          not-empty))

(defn- user-actor-file-path
  [email]
  (when-let [actor-id (user-actor-id-from-email email)]
    (.join path (contracts-dir) "actors" (str actor-id ".edn"))))

(defn- ensure-contract-dir!
  [dir]
  (.mkdirSync fs dir #js {:recursive true}))

(defn- upsert-user-actor-contract!
  [{:keys [actor-id email display-name org-slug role-slugs]}]
  (let [normalized-email (normalize-email email)
        resolved-actor-id (or (normalize-actor-id actor-id)
                              (user-actor-id-from-email normalized-email))
        file-path (user-actor-file-path normalized-email)]
    (if (or (str/blank? (str (or normalized-email "")))
            (str/blank? (str (or resolved-actor-id "")))
            (str/blank? (str (or file-path ""))))
      (js/Promise.resolve nil)
      (try
        (let [existing (when (.existsSync fs file-path)
                         (safe-read-edn file-path))
              contract (merge
                        {:actor/id resolved-actor-id
                         :actor/kind :user
                         :actor/email normalized-email
                         :actor/username normalized-email
                         :actor/org (or (some-> org-slug str str/trim not-empty) "open-hax")
                         :actor/label (or (some-> display-name str str/trim not-empty)
                                          normalized-email)
                         :actor/roles (->> (or role-slugs ["knowledge_worker"])
                                           (keep role-slug->contract-keyword)
                                           distinct
                                           vec)
                         :actor/policy {:principal :actor
                                        :source-of-truth :contract
                                        :notes (str "Canonical human actor contract for " normalized-email ". Created or updated by Knoxx onboarding flows.")}}
                        (when (map? existing)
                          (select-keys existing [:actor/contract :actor/policy])))]
          (ensure-contract-dir! (.dirname path file-path))
          (.writeFileSync fs file-path (str (pr-str contract) "\n") "utf8")
          (js/Promise.resolve {:actor-id resolved-actor-id
                               :path file-path}))
        (catch :default err
          (js/Promise.reject err))))))

(defn- find-org-by-id
  [pool org-id]
  (if (str/blank? (str org-id))
    (js/Promise.resolve nil)
    (query-one! pool "SELECT * FROM orgs WHERE id = $1::uuid" [org-id])))

(defn- actor-role-slugs
  [actor]
  (->> (or (:actor/roles actor) [])
       (map (fn [value]
              (let [raw (cond
                          (keyword? value) (name value)
                          (string? value) (some-> value str str/trim not-empty)
                          :else (some-> value str str/trim not-empty))]
                (when raw
                  (contracts-roles/normalize-role (contracts-config) raw)))))
       (remove nil?)
       distinct
       vec))

(defn- find-user-actor-contract-by-email
  [email]
  (when-let [normalized-email (normalize-email email)]
    (try
      (let [actor-dir (.join path (contracts-dir) "actors")
            names (.readdirSync fs actor-dir)]
        (->> (array-seq names)
             (keep (fn [name]
                     (when (and (string? name) (str/ends-with? name ".edn"))
                       (let [actor (safe-read-edn (.join path actor-dir name))
                             actor-email (normalize-email (or (:actor/email actor)
                                                              (:actor/username actor)))]
                         (when (and (= :user (:actor/kind actor))
                                    (= normalized-email actor-email))
                           {:id (or (:actor/id actor)
                                    (subs name 0 (- (count name) 4)))
                            :email actor-email
                            :org (some-> (:actor/org actor) str str/trim not-empty)
                            :role-slugs (actor-role-slugs actor)
                            :actor actor})))))
             first))
      (catch :default _
        nil))))

(defn- list-user-actor-contracts
  []
  (try
    (let [actor-dir (.join path (contracts-dir) "actors")
          names (.readdirSync fs actor-dir)]
      (->> (array-seq names)
           (keep (fn [name]
                   (when (and (string? name) (str/ends-with? name ".edn"))
                     (let [actor (safe-read-edn (.join path actor-dir name))]
                       (when (= :user (:actor/kind actor))
                         {:id (or (:actor/id actor)
                                  (subs name 0 (- (count name) 4)))
                          :email (normalize-email (or (:actor/email actor)
                                                      (:actor/username actor)))
                          :org (some-> (:actor/org actor) str str/trim not-empty)
                          :label (some-> (:actor/label actor) str str/trim not-empty)
                          :role-slugs (actor-role-slugs actor)
                          :actor actor})))))
           vec))
    (catch :default _
      [])))

(defn- contract-tool-ids
  "Return a set of tool ids found in contracts/capabilities/*.edn."
  []
  (try
    (let [cap-dir (.join path (contracts-dir) "capabilities")
          names (.readdirSync fs cap-dir)
          tool-ids (atom #{})]
      (doseq [name (array-seq names)]
        (when (and (string? name) (str/ends-with? name ".edn"))
          (when-let [cap (safe-read-edn (.join path cap-dir name))]
            (doseq [tool (or (:cap/tools cap) [])]
              (when-let [id (tool-registry/normalize-tool-id tool)]
                (swap! tool-ids conj id))))))
      @tool-ids)
    (catch :default _
      #{})))

(defn- contracts-config
  []
  {:contracts-dir (default-contracts-dir)})

(defn- role-tool-policies-from-contracts
  "Return [{:toolId <id> :effect \"allow\"} ...] for the given role slug.

   Uses contracts/roles/<role>.edn → contracts/capabilities/* to derive tools."
  [role-slug]
  (-> (contracts-roles/role-tool-ids (contracts-config) role-slug)
       (.then (fn [tool-ids]
                (when (seq tool-ids)
                  (mapv (fn [tool-id] {:toolId tool-id :effect "allow"}) tool-ids))))))

(defn- role-permissions-from-contracts
  "Return a vector of permission code strings for the given role slug.

   Reads :role/permissions from contracts/roles/<role>.edn."
  [role-slug]
  (contracts-roles/role-permissions (contracts-config) role-slug))

(def ^:private PLATFORM-ROLE-SEEDS
  [{:slug "system_admin"
    :name "System Admin"}])

(defn- basic-user-chat-policy-constraints
  []
  {:allowedModels ["gemma4:31b"]
   :maxRequests 20
   :windowSeconds 600})

(def ^:private ORG-ROLE-SEEDS
  [{:slug "org_admin"
    :name "Org Admin"
    :tool-policies (mapv (fn [tool-id] {:toolId tool-id :effect "allow"}) (tool-registry/known-tool-ids))}
   {:slug "knowledge_worker"
    :name "Knowledge Worker"}
   {:slug "basic_user"
    :name "Basic User"}
   {:slug "data_analyst"
    :name "Data Analyst"}
   {:slug "developer"
    :name "Developer"}
   {:slug "translator"
    :name "Translator"}])

;; ---------------------------------------------------------------------------
;; Helper Functions
;; ---------------------------------------------------------------------------

(defn- slugify
  [value fallback]
  (let [slug (-> (str value "")
                 str/trim
                 str/lower-case
                 (str/replace #"[^a-z0-9]+" "-")
                 (str/replace #"^[-]+|[-]+$" ""))]
    (if (str/blank? slug) fallback slug)))

(defn- unique
  [values]
  (vec (distinct (filter some? values))))

(defn- normalize-actor-id
  [value]
  (some-> value str str/trim not-empty))

(defn default-membership-actor-id
  [role-slugs]
  (let [normalized (into #{}
                         (comp (map #(some-> % str str/trim not-empty))
                               (map #(when % (contracts-roles/normalize-role (contracts-config) %)))
                               (remove nil?))
                         (or role-slugs []))]
    (if (contains? normalized "system_admin")
      "system_admin"
      "workspace_user")))

(defn- set-membership-actor-id!
  [pool membership-id actor-id]
  (let [resolved (or (normalize-actor-id actor-id) "workspace_user")]
    (-> (query! pool
                "UPDATE memberships SET actor_id = $2, updated_at = NOW() WHERE id = $1::uuid"
                [membership-id resolved])
        (.then (fn [_] resolved)))))

(defn- backfill-membership-actors!
  [pool]
  (-> (query! pool
              "UPDATE memberships m
               SET actor_id = CASE
                 WHEN EXISTS (
                   SELECT 1
                   FROM membership_roles mr
                   JOIN roles r ON r.id = mr.role_id
                   WHERE mr.membership_id = m.id
                     AND r.slug = 'system_admin'
                 ) THEN 'system_admin'
                 ELSE 'workspace_user'
               END,
               updated_at = NOW()
               WHERE COALESCE(NULLIF(trim(actor_id), ''), '') = ''"
              [])
      (.then (fn [_] nil))))

(defn- sync-user-from-actor-contract!
  [pool primary-org payload]
  (let [email (normalize-email (aget payload "email"))
        display-name (str/trim (str (or (aget payload "displayName")
                                        (aget payload "display_name")
                                        email)))
        auth-provider (str (or (aget payload "authProvider") "github"))
        external-subject (or (aget payload "externalSubject") nil)]
    (if-not email
      (js/Promise.resolve nil)
      (if-let [actor-contract (find-user-actor-contract-by-email email)]
        (let [role-slugs (vec (or (:role-slugs actor-contract) ["knowledge_worker"]))
              effective-name (or (:label actor-contract) display-name email)]
          (-> (query-one! pool
                          "INSERT INTO users (email, display_name, auth_provider, external_subject, status) VALUES ($1, $2, $3, $4, 'active') ON CONFLICT (email) DO UPDATE SET display_name = EXCLUDED.display_name, auth_provider = EXCLUDED.auth_provider, external_subject = EXCLUDED.external_subject, status = 'active', updated_at = NOW() RETURNING *"
                          [email effective-name auth-provider external-subject])
              (.then
               (fn [user]
                 (-> (if-let [actor-org (:org actor-contract)]
                       (-> (find-org-by-slug pool actor-org)
                           (.then (fn [resolved-org]
                                    (or resolved-org primary-org))))
                       (js/Promise.resolve primary-org))
                     (.then
                      (fn [target-org]
                        (-> (query-one! pool
                                        "INSERT INTO memberships (user_id, org_id, actor_id, status, is_default) VALUES ($1, $2, $3, 'active', TRUE) ON CONFLICT (user_id, org_id) DO UPDATE SET actor_id = EXCLUDED.actor_id, status = 'active', is_default = TRUE, updated_at = NOW() RETURNING *"
                                        [(aget user "id") (aget target-org "id") (:id actor-contract)])
                            (.then
                             (fn [membership]
                               (-> (set-membership-roles! pool (aget membership "id")
                                                          {:org-id (aget target-org "id")
                                                           :role-slugs role-slugs
                                                           :role-ids #js []
                                                           :replace true})
                                   (.then (fn [_]
                                            (set-membership-actor-id! pool (aget membership "id") (:id actor-contract))))))))))))))
        (js/Promise.resolve nil))))))

(defn- sync-user-actors-from-contracts!
  [pool primary-org]
  (-> (js/Promise.all
       (clj->js
        (mapv (fn [actor-contract]
                (if-let [email (:email actor-contract)]
                  (sync-user-from-actor-contract! pool primary-org
                                                 #js {:email email
                                                      :displayName (or (:label actor-contract) email)
                                                      :authProvider "contract"
                                                      :externalSubject nil})
                  (js/Promise.resolve nil)))
              (list-user-actor-contracts))))
      (.then (fn [_] nil))))

(defn- normalize-tool-policy
  [policy]
  (cond
    (string? policy)
    {:toolId policy :effect "allow" :constraints {}}

    :else
    (let [tool-id (or (:toolId policy) (:tool_id policy) (:id policy))]
      (when-not tool-id
        (throw (js/Error. "toolId is required for tool policy")))
      {:toolId (tool-registry/normalize-tool-id tool-id)
       :effect (if (= (:effect policy) "deny") "deny" "allow")
       :constraints (or (:constraints policy) (:constraints_json policy) {})})))

(defn- seed-tool-ids
  "Return a stable, sorted list of tool ids that should exist in tool_definitions.

   We seed from the runtime registry AND from contract capabilities so that
   contract-driven role policies can't fail on FK constraints when a tool id
   appears in contracts before it is added to the runtime tool registry (or
   when different nodes deploy slightly different registries)."
  []
  (let [runtime-ids (tool-registry/known-tool-ids)
        contract-result (contract-tool-ids)
        contract-ids (when (and (not (nil? contract-result))
                             (sequential? contract-result))
                     (seq contract-result))]
    (when (and runtime-ids (sequential? runtime-ids))
      (->> (concat runtime-ids contract-ids)
           (keep (fn [x]
                   (when (some? x)
                     (let [id (tool-registry/normalize-tool-id x)]
                       (when (string? id) id)))))
           distinct
           sort
           vec))))

(defn- ensure-tool-definitions!
  "Upsert tool definitions so FK constraints on *_tool_policies can't fail.

   Accepts a seq of tool-id strings (or keywords/symbols).
   This intentionally tolerates unknown tools by inserting a placeholder.
   Unknown tool ids may originate from contracts, UI experiments, or mixed-version
   deployments."
  [pool tool-ids]
  (let [ids (->> tool-ids
                 (keep tool-registry/normalize-tool-id)
                 distinct
                 vec)]
    (if (empty? ids)
      (js/Promise.resolve nil)
      (js/Promise.all
       (into-array
        (for [tool-id ids]
          (let [{:keys [label description risk-level]} (tool-registry/get-tool tool-id)]
            (query! pool
                    "INSERT INTO tool_definitions (id, label, description, risk_level)
                         VALUES ($1, $2, $3, $4)
                         ON CONFLICT (id) DO UPDATE
                         SET label = EXCLUDED.label,
                             description = EXCLUDED.description,
                             risk_level = EXCLUDED.risk_level"
                    [tool-id (or label tool-id) (or description "") (or risk-level "low")]))))))))

(defn- normalize-lake-config
  [config]
  (if (or (nil? config) (not (object? config)) (array? config))
    {}
    (js->clj config :keywordize-keys true)))

(defn- http-error
  [status-code message code]
  (let [err (js/Error. message)]
    (aset err "statusCode" status-code)
    (aset err "code" code)
    err))

(defn- header-value
  [headers-like name]
  (when headers-like
    (cond
      (fn? (aget headers-like "get"))
      (str/trim (or (.get headers-like name)
                     (.get headers-like (str/lower-case name))
                     ""))

      :else
      (str/trim (str (or (aget headers-like name)
                          (aget headers-like (str/lower-case name))
                          ""))))))

(defn- merge-toolPolicies
  [role-policies membership-policies]
  (let [merged (atom {})]
    (doseq [policy role-policies]
      (let [normalized (normalize-tool-policy policy)
            tool-id (:toolId normalized)
            existing (get @merged tool-id)]
        (when (or (nil? existing)
                  (= (:effect normalized) "deny")
                  (not= (:effect existing) "deny"))
          (swap! merged assoc tool-id normalized))))
    (doseq [policy membership-policies]
      (let [normalized (normalize-tool-policy policy)]
        (swap! merged assoc (:toolId normalized) normalized)))
    (->> (vals @merged)
         (sort-by :toolId)
         vec)))

(defn- rolePriority
  [slug]
  (case slug
    "system_admin" 100
    "org_admin" 90
    "developer" 80
    "data_analyst" 70
    "knowledge_worker" 60
    0))

(defn- permission-row-shape
  [code]
  (let [parts (->> (str/split (str code) #"\.")
                   (remove str/blank?)
                   vec)
        action (or (peek parts) "use")
        resource-kind (or (some->> (butlast parts) seq (str/join "_")) "permission")]
    {:code (str code)
     :resource-kind resource-kind
     :action action
     :description (str code)}))

;; ---------------------------------------------------------------------------
;; Database Query Helpers
;; ---------------------------------------------------------------------------

(defn- query!
  "Execute a parameterized SQL query. Returns Promise resolving to {:rows [...]}."
  [pool sql-str params]
  (let [params-arr (if (seq params) (into-array params) js/undefined)]
    (.query pool sql-str params-arr)))

(defn- query-one!
  "Execute query and return first row."
  [pool sql-str params]
  (-> (query! pool sql-str params)
      (.then (fn [result]
               (let [rows (aget result "rows")]
                 (when (and rows (> (.-length rows) 0))
                   (aget rows 0)))))))

(defn- role-permissions-uses-legacy-ids?
  [pool]
  (-> (query! pool
              "SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'role_permissions'"
              [])
      (.then (fn [result]
               (let [rows (array-seq (aget result "rows"))
                     columns (set (map #(aget % "column_name") rows))]
                 (contains? columns "permission_id"))))))

(defn- ensure-permission-records!
  [pool permission-codes]
  (let [codes (unique permission-codes)]
    (if (empty? codes)
      (js/Promise.resolve nil)
      (-> (js/Promise.all
           (into-array
            (for [code codes]
              (let [{:keys [resource-kind action description]} (permission-row-shape code)]
                (query! pool
                        "INSERT INTO permissions (code, resource_kind, action, description)
                         VALUES ($1, $2, $3, $4)
                         ON CONFLICT (code) DO UPDATE
                         SET resource_kind = EXCLUDED.resource_kind,
                             action = EXCLUDED.action,
                             description = EXCLUDED.description"
                        [code resource-kind action description])))))
          (.then (fn [_] nil))))))

(defn- fetch-role-permission-rows!
  [pool role-ids]
  (-> (role-permissions-uses-legacy-ids? pool)
      (.then
       (fn [legacy?]
         (if legacy?
           (query! pool
                   "SELECT rp.role_id, p.code AS code FROM role_permissions rp JOIN permissions p ON p.id = rp.permission_id WHERE rp.role_id = ANY($1::uuid[]) ORDER BY p.code ASC"
                   [(into-array role-ids)])
           (query! pool
                   "SELECT role_id, permission_code AS code FROM role_permissions WHERE role_id = ANY($1::uuid[]) ORDER BY permission_code ASC"
                   [(into-array role-ids)]))))))

(defn- honey->sql
  "Convert HoneySQL map to [sql-string & params]."
  [honey-map]
  (sql/format honey-map {:numbered true}))

(defn- honey-query!
  "Execute HoneySQL query map."
  [pool honey-map]
  (let [[sql-str & params] (honey->sql honey-map)]
    (query! pool sql-str params)))

(defn- honey-query-one!
  "Execute HoneySQL query and return first row."
  [pool honey-map]
  (let [[sql-str & params] (honey->sql honey-map)]
    (query-one! pool sql-str params)))

;; ---------------------------------------------------------------------------
;; Schema Management
;; ---------------------------------------------------------------------------

(defn- ensure-schema!
  [pool]
  (query! pool "
    CREATE EXTENSION IF NOT EXISTS pgcrypto;

    CREATE TABLE IF NOT EXISTS orgs (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      slug TEXT NOT NULL UNIQUE,
      name TEXT NOT NULL,
      kind TEXT NOT NULL DEFAULT 'customer',
      is_primary BOOLEAN NOT NULL DEFAULT FALSE,
      status TEXT NOT NULL DEFAULT 'active',
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS users (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      email TEXT NOT NULL UNIQUE,
      display_name TEXT NOT NULL,
      auth_provider TEXT NOT NULL DEFAULT 'bootstrap',
      external_subject TEXT,
      status TEXT NOT NULL DEFAULT 'active',
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS memberships (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
      actor_id TEXT,
      status TEXT NOT NULL DEFAULT 'active',
      is_default BOOLEAN NOT NULL DEFAULT FALSE,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      UNIQUE (user_id, org_id)
    );

    CREATE TABLE IF NOT EXISTS roles (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      org_id UUID REFERENCES orgs(id) ON DELETE CASCADE,
      name TEXT NOT NULL,
      slug TEXT NOT NULL,
      scope_kind TEXT NOT NULL DEFAULT 'org',
      built_in BOOLEAN NOT NULL DEFAULT FALSE,
      system_managed BOOLEAN NOT NULL DEFAULT FALSE,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      CHECK (scope_kind IN ('platform', 'org'))
    );

    CREATE UNIQUE INDEX IF NOT EXISTS roles_platform_slug_uniq
      ON roles (slug)
      WHERE org_id IS NULL;

    CREATE UNIQUE INDEX IF NOT EXISTS roles_org_slug_uniq
      ON roles (org_id, slug)
      WHERE org_id IS NOT NULL;

    CREATE TABLE IF NOT EXISTS role_permissions (
      role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
      permission_code TEXT NOT NULL,
      effect TEXT NOT NULL DEFAULT 'allow',
      PRIMARY KEY (role_id, permission_code),
      CHECK (effect IN ('allow', 'deny'))
    );

    -- Legacy compatibility: older Knoxx builds stored role permissions via
    -- permission_id -> permissions(id). Newer builds store permission_code
    -- directly so contracts can drive the catalog without a separate table.
    ALTER TABLE role_permissions
      ADD COLUMN IF NOT EXISTS permission_code TEXT;

    DO $$
    BEGIN
      IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'permissions'
      ) AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'role_permissions'
          AND column_name = 'permission_id'
      ) THEN
        UPDATE role_permissions rp
        SET permission_code = p.code
        FROM permissions p
        WHERE rp.permission_code IS NULL
          AND rp.permission_id = p.id;
      END IF;
    END $$;

    CREATE UNIQUE INDEX IF NOT EXISTS role_permissions_role_code_uniq
      ON role_permissions (role_id, permission_code)
      WHERE permission_code IS NOT NULL;

    CREATE TABLE IF NOT EXISTS membership_roles (
      membership_id UUID NOT NULL REFERENCES memberships(id) ON DELETE CASCADE,
      role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
      PRIMARY KEY (membership_id, role_id)
    );

    CREATE TABLE IF NOT EXISTS tool_definitions (
      id TEXT PRIMARY KEY,
      label TEXT NOT NULL,
      description TEXT NOT NULL,
      risk_level TEXT NOT NULL DEFAULT 'medium'
    );

    CREATE TABLE IF NOT EXISTS role_tool_policies (
      role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
      tool_id TEXT NOT NULL REFERENCES tool_definitions(id) ON DELETE CASCADE,
      effect TEXT NOT NULL DEFAULT 'allow',
      constraints_json JSONB NOT NULL DEFAULT '{}'::jsonb,
      PRIMARY KEY (role_id, tool_id),
      CHECK (effect IN ('allow', 'deny'))
    );

    CREATE TABLE IF NOT EXISTS user_tool_policies (
      membership_id UUID NOT NULL REFERENCES memberships(id) ON DELETE CASCADE,
      tool_id TEXT NOT NULL REFERENCES tool_definitions(id) ON DELETE CASCADE,
      effect TEXT NOT NULL DEFAULT 'allow',
      constraints_json JSONB NOT NULL DEFAULT '{}'::jsonb,
      PRIMARY KEY (membership_id, tool_id),
      CHECK (effect IN ('allow', 'deny'))
    );

    CREATE TABLE IF NOT EXISTS data_lakes (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
      name TEXT NOT NULL,
      slug TEXT NOT NULL,
      kind TEXT NOT NULL DEFAULT 'workspace_docs',
      config_json JSONB NOT NULL DEFAULT '{}'::jsonb,
      status TEXT NOT NULL DEFAULT 'active',
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      UNIQUE (org_id, slug)
    );

    CREATE TABLE IF NOT EXISTS audit_events (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
      actor_membership_id UUID REFERENCES memberships(id) ON DELETE SET NULL,
      org_id UUID REFERENCES orgs(id) ON DELETE SET NULL,
      action TEXT NOT NULL,
      resource_kind TEXT NOT NULL,
      resource_id TEXT,
      before_json JSONB,
      after_json JSONB,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS sessions (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      membership_id UUID NOT NULL REFERENCES memberships(id) ON DELETE CASCADE,
      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
      token_hash TEXT NOT NULL,
      token_prefix TEXT NOT NULL DEFAULT '',
      salt TEXT NOT NULL,
      email TEXT NOT NULL,
      display_name TEXT NOT NULL,
      auth_provider TEXT NOT NULL DEFAULT 'github',
      external_subject TEXT,
      ip_address TEXT,
      user_agent TEXT,
      expires_at TIMESTAMPTZ NOT NULL,
      last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    -- Backfill for installations that created `sessions` before `token_prefix` existed.
    ALTER TABLE sessions
      ADD COLUMN IF NOT EXISTS token_prefix TEXT NOT NULL DEFAULT '';

    ALTER TABLE memberships
      ADD COLUMN IF NOT EXISTS actor_id TEXT;

    CREATE INDEX IF NOT EXISTS sessions_user_idx ON sessions (user_id);
    CREATE INDEX IF NOT EXISTS sessions_membership_idx ON sessions (membership_id);
    CREATE INDEX IF NOT EXISTS sessions_token_prefix_idx ON sessions (token_prefix);
    CREATE INDEX IF NOT EXISTS sessions_expires_at_idx ON sessions (expires_at);

    CREATE TABLE IF NOT EXISTS invites (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
      code TEXT NOT NULL UNIQUE,
      email TEXT NOT NULL,
      inviter_membership_id UUID REFERENCES memberships(id) ON DELETE SET NULL,
      role_slugs JSONB NOT NULL DEFAULT '[]'::jsonb,
      status TEXT NOT NULL DEFAULT 'pending',
      redeemed_by UUID REFERENCES users(id) ON DELETE SET NULL,
      redeemed_at TIMESTAMPTZ,
      expires_at TIMESTAMPTZ NOT NULL,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE INDEX IF NOT EXISTS invites_org_idx ON invites (org_id);
    CREATE INDEX IF NOT EXISTS invites_code_idx ON invites (code);
    CREATE INDEX IF NOT EXISTS invites_status_idx ON invites (status);

    CREATE TABLE IF NOT EXISTS knoxx_config (
      key TEXT PRIMARY KEY,
      value TEXT NOT NULL,
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
  " nil))

(defn- insert-permission-seeds!
  "Permissions are now contract-driven via role EDN files. No-op for backwards compat."
  [pool]
  (js/Promise.resolve nil))

(defn- insert-tool-seeds!
  [pool]
  (-> (js/Promise.all
       (into-array
        (for [tool-id (seed-tool-ids)]
          (let [{:keys [label description risk-level]} (tool-registry/get-tool tool-id)]
            (query! pool
                    "INSERT INTO tool_definitions (id, label, description, risk_level)
                     VALUES ($1, $2, $3, $4)
                     ON CONFLICT (id) DO UPDATE
                     SET label = EXCLUDED.label,
                         description = EXCLUDED.description,
                         risk_level = EXCLUDED.risk_level"
                    [tool-id (or label tool-id) (or description "") (or risk-level "low")])))))
      (.then (fn [_] nil))))

;; ---------------------------------------------------------------------------
;; Org Management
;; ---------------------------------------------------------------------------

(defn- ensure-primary-org!
  [pool options]
  (let [slug (slugify (or (:primaryOrgSlug options) "open-hax") "open-hax")
        name (str (or (:primaryOrgName options) "Open Hax"))
        kind (str (or (:primaryOrgKind options) "platform_owner"))]
(-> (query-one! pool
                    "INSERT INTO orgs (slug, name, kind, is_primary, status)
                     VALUES ($1, $2, $3, TRUE, 'active')
                     ON CONFLICT (slug) DO UPDATE
                     SET name = EXCLUDED.name,
                         kind = EXCLUDED.kind,
                         is_primary = TRUE,
                         updated_at = NOW()
                     RETURNING *"
                    [slug name kind])
         (.then (fn [org]
                  (query! pool "UPDATE orgs SET is_primary = CASE WHEN slug = $1 THEN TRUE ELSE FALSE END" [slug])
                  (fn [_] org))))))

(defn- find-org-by-slug
  [pool slug]
  (if (str/blank? (str slug))
    (js/Promise.resolve nil)
    (query-one! pool
                "SELECT * FROM orgs WHERE lower(slug) = lower($1) LIMIT 1"
                [slug])))

;; ---------------------------------------------------------------------------
;; Role Management
;; ---------------------------------------------------------------------------

(defn- find-role
  [pool {:keys [org-id slug]}]
  (query-one! pool
              "SELECT * FROM roles WHERE slug = $1 AND (($2::uuid IS NULL AND org_id IS NULL) OR org_id = $2::uuid) LIMIT 1"
              [slug org-id]))

(defn- ensure-role!
  [pool {:keys [org-id name slug scope-kind built-in system-managed]}]
  (-> (find-role pool {:org-id org-id :slug slug})
      (.then (fn [existing]
               (if existing
                 (query-one! pool
                             "UPDATE roles SET name = $2, scope_kind = $3, built_in = $4, system_managed = $5, updated_at = NOW() WHERE id = $1 RETURNING *"
                             [(aget existing "id") name scope-kind built-in system-managed])
                 (query-one! pool
                             "INSERT INTO roles (org_id, name, slug, scope_kind, built_in, system_managed) VALUES ($1, $2, $3, $4, $5, $6) RETURNING *"
                             [org-id name slug scope-kind built-in system-managed]))))))


;; ---------------------------------------------------------------------------
;; Role & Permission Mutations
;; ---------------------------------------------------------------------------

(defn- set-role-permissions!
  [pool role-id permission-codes]
  (let [codes (unique permission-codes)]
    (-> (query! pool "DELETE FROM role_permissions WHERE role_id = $1" [role-id])
        (.then (fn [_] (role-permissions-uses-legacy-ids? pool)))
        (.then
         (fn [legacy?]
           (if (empty? codes)
             nil
             (if legacy?
               (-> (ensure-permission-records! pool codes)
                   (.then
                    (fn [_]
                      (js/Promise.all
                       (into-array
                        (for [code codes]
                          (query! pool
                                  "INSERT INTO role_permissions (role_id, permission_id, effect) SELECT $1, id, 'allow' FROM permissions WHERE code = $2 ON CONFLICT (role_id, permission_id) DO UPDATE SET effect = EXCLUDED.effect"
                                  [role-id code])))))))
               (js/Promise.all
                (into-array
                 (for [code codes]
                   (query! pool
                           "INSERT INTO role_permissions (role_id, permission_code, effect) VALUES ($1, $2, 'allow') ON CONFLICT (role_id, permission_code) DO UPDATE SET effect = EXCLUDED.effect"
                           [role-id code])))))))))))


(defn- set-role-tool-policies!
  [pool role-id tool-policies]
  (let [normalized (mapv normalize-tool-policy tool-policies)
        tool-ids (mapv :toolId normalized)]
    (-> (query! pool "DELETE FROM role_tool_policies WHERE role_id = $1" [role-id])
        (.then (fn [_] (ensure-tool-definitions! pool tool-ids)))
        (.then
         (fn [_]
           (js/Promise.all
            (into-array
             (for [policy normalized]
               (query! pool
                       "INSERT INTO role_tool_policies (role_id, tool_id, effect, constraints_json) VALUES ($1, $2, $3, $4::jsonb) ON CONFLICT (role_id, tool_id) DO UPDATE SET effect = EXCLUDED.effect, constraints_json = EXCLUDED.constraints_json"
                       [role-id (:toolId policy) (:effect policy)
                        (js/JSON.stringify (clj->js (:constraints policy)))]))))))
        (.then (fn [_] nil)))))

(defn- set-membership-tool-policies!
  [pool membership-id tool-policies]
  (let [normalized (mapv normalize-tool-policy tool-policies)
        tool-ids (mapv :toolId normalized)]
    (-> (query! pool "DELETE FROM user_tool_policies WHERE membership_id = $1" [membership-id])
        (.then (fn [_] (ensure-tool-definitions! pool tool-ids)))
        (.then
         (fn [_]
           (js/Promise.all
            (into-array
             (for [policy normalized]
               (query! pool
                       "INSERT INTO user_tool_policies (membership_id, tool_id, effect, constraints_json) VALUES ($1, $2, $3, $4::jsonb) ON CONFLICT (membership_id, tool_id) DO UPDATE SET effect = EXCLUDED.effect, constraints_json = EXCLUDED.constraints_json"
                       [membership-id (:toolId policy) (:effect policy)
                        (js/JSON.stringify (clj->js (:constraints policy)))]))))))
        (.then (fn [_] nil)))))

(defn- ensure-builtin-org-roles!
  [pool org]
  (-> (js/Promise.all
       (into-array
        (for [seed ORG-ROLE-SEEDS]
          (-> (ensure-role! pool {:org-id (aget org "id")
                                  :name (:name seed)
                                  :slug (:slug seed)
                                  :scope-kind "org"
                                  :built-in true
                                  :system-managed true})
              (.then
               (fn [role]
                 (let [perms (or (role-permissions-from-contracts (:slug seed))
                                 (:permissions seed)
                                 [])]
                   (-> (set-role-permissions! pool (aget role "id") perms)
                       (.then (fn [_]
                                (let [policies (or (role-tool-policies-from-contracts (:slug seed))
                                                   (:tool-policies seed)
                                                   (mapv (fn [tool-id] {:toolId tool-id :effect "allow"}) (tool-registry/known-tool-ids)))]
                                  (set-role-tool-policies! pool (aget role "id")
                                                           policies))))))))))))
      (.then (fn [_] nil))))

(defn- ensure-builtin-platform-roles!
  [pool]
  (-> (js/Promise.all
       (into-array
        (for [seed PLATFORM-ROLE-SEEDS]
          (-> (ensure-role! pool {:org-id nil
                                  :name (:name seed)
                                  :slug (:slug seed)
                                  :scope-kind "platform"
                                  :built-in true
                                  :system-managed true})
              (.then
               (fn [role]
                 (-> (role-permissions-from-contracts (:slug seed))
                     (.then (fn [perms]
                              (let [perms (or perms (:permissions seed) [])]
                                (-> (set-role-permissions! pool (aget role "id") perms)
                                    (.then (fn [_]
                                             (-> (role-tool-policies-from-contracts (:slug seed))
                                                 (.then (fn [policies]
                                                          (let [policies (or policies (:tool-policies seed))]
                                                            (set-role-tool-policies! pool (aget role "id") policies)))))))))))))
       )))))))

(defn- tool-allowed
  [context tool-id]
  (let [policies (or (some-> context (aget "toolPolicies")) #js [])
        match (some (fn [entry]
                      (when (= (aget entry "toolId") tool-id) entry))
                    policies)]
    (some? (and match (= (aget match "effect") "allow")))))

(defn- resolve-role-ids
  [pool {:keys [org-id role-ids role-slugs]}]
  (let [resolved-ids (atom (set (map str (or role-ids #js []))))]
    (-> (js/Promise.all
         (into-array
          (for [slug (filter some? (or role-slugs #js []))]
            (let [raw-slug (str/trim (str slug))
                  normalized (slugify raw-slug raw-slug)]
              (-> (query-one! pool
                              "SELECT id FROM roles WHERE (slug = $1 OR slug = $2) AND (org_id = $3::uuid OR org_id IS NULL) ORDER BY CASE WHEN org_id IS NULL THEN 1 ELSE 0 END, created_at ASC LIMIT 1"
                              [raw-slug normalized org-id])
                  (.then
                   (fn [row]
                     (when-not row
                       (throw (js/Error. (str "Role not found for slug '" raw-slug "'"))))
                     (swap! resolved-ids conj (str (aget row "id"))))))))))
        (.then (fn [_] (into-array @resolved-ids))))))

(defn- set-membership-roles!
  [pool membership-id {:keys [org-id role-ids role-slugs replace]}]
  (-> (resolve-role-ids pool {:org-id org-id
                               :role-ids (or role-ids #js [])
                               :role-slugs (or role-slugs #js [])})
      (.then
       (fn [resolved-ids]
         (-> (if replace
               (query! pool "DELETE FROM membership_roles WHERE membership_id = $1"
                       [membership-id])
               (js/Promise.resolve nil))
             (.then
              (fn [_]
                (js/Promise.all
                 (into-array
                  (for [role-id resolved-ids]
                    (query! pool
                            "INSERT INTO membership_roles (membership_id, role_id) VALUES ($1, $2) ON CONFLICT (membership_id, role_id) DO NOTHING"
                            [membership-id role-id]))))))
             (.then (fn [_] resolved-ids)))))))

;; ---------------------------------------------------------------------------
;; Hydration Helpers
;; ---------------------------------------------------------------------------

(defn- hydrate-role-maps
  [pool roles]
  (if (empty? roles)
    (js/Promise.resolve [])
    (let [role-ids (mapv #(aget % "id") roles)]
      (-> (js/Promise.all
           [(fetch-role-permission-rows! pool role-ids)
            (query! pool
                    "SELECT rtp.role_id, rtp.tool_id, rtp.effect, rtp.constraints_json FROM role_tool_policies rtp WHERE rtp.role_id = ANY($1::uuid[]) ORDER BY rtp.tool_id ASC"
                    [(into-array role-ids)])])
          (.then
           (fn [[perm-result tool-result]]
             (let [perm-rows (aget perm-result "rows")
                   tool-rows (aget tool-result "rows")
                   perm-map (atom {})
                   tool-map (atom {})]
               (doseq [i (range (.-length perm-rows))]
                 (let [row (aget perm-rows i)
                       rid (aget row "role_id")
                       code (aget row "code")]
                   (swap! perm-map update rid (fnil conj []) code)))
               (doseq [i (range (.-length tool-rows))]
                 (let [row (aget tool-rows i)
                       rid (aget row "role_id")
                       policy {:toolId (aget row "tool_id")
                               :effect (aget row "effect")
                               :constraints (js->clj (or (aget row "constraints_json") {})
                                                     :keywordize-keys true)}]
                   (swap! tool-map update rid (fnil conj []) policy)))
               (vec
                (for [role roles]
                  {:id (aget role "id")
                   :orgId (aget role "org_id")
                   :name (aget role "name")
                   :slug (aget role "slug")
                   :scopeKind (aget role "scope_kind")
                   :builtIn (aget role "built_in")
                   :systemManaged (aget role "system_managed")
                   :createdAt (aget role "created_at")
                   :updatedAt (aget role "updated_at")
                   :permissions (or (get @perm-map (aget role "id")) [])
                   :toolPolicies (or (get @tool-map (aget role "id")) [])})))))))))

(defn- hydrate-memberships
  [pool memberships]
  (if (empty? memberships)
    (js/Promise.resolve [])
    (let [membership-ids (mapv #(aget % "id") memberships)]
      (-> (js/Promise.all
           [(query! pool
                    "SELECT mr.membership_id, r.id AS role_id, r.slug, r.name, r.scope_kind, r.org_id FROM membership_roles mr JOIN roles r ON r.id = mr.role_id WHERE mr.membership_id = ANY($1::uuid[]) ORDER BY r.name ASC"
                    [(into-array membership-ids)])
            (query! pool
                    "SELECT membership_id, tool_id, effect, constraints_json FROM user_tool_policies WHERE membership_id = ANY($1::uuid[]) ORDER BY tool_id ASC"
                    [(into-array membership-ids)])])
          (.then
           (fn [[role-result tool-result]]
             (let [role-rows (aget role-result "rows")
                   tool-rows (aget tool-result "rows")
                   roles-by-m (atom {})
                   tools-by-m (atom {})]
               (doseq [i (range (.-length role-rows))]
                 (let [row (aget role-rows i)
                       m-id (aget row "membership_id")
                       role {:id (aget row "role_id")
                             :slug (aget row "slug")
                             :name (aget row "name")
                             :scopeKind (aget row "scope_kind")
                             :orgId (aget row "org_id")}]
                   (swap! roles-by-m update m-id (fnil conj []) role)))
               (doseq [i (range (.-length tool-rows))]
                 (let [row (aget tool-rows i)
                       m-id (aget row "membership_id")
                       policy {:toolId (aget row "tool_id")
                               :effect (aget row "effect")
                               :constraints (js->clj (or (aget row "constraints_json") {})
                                                     :keywordize-keys true)}]
                   (swap! tools-by-m update m-id (fnil conj []) policy)))
               (vec
                (for [membership memberships]
                  {:id (aget membership "id")
                   :orgId (aget membership "org_id")
                   :actorId (or (normalize-actor-id (aget membership "actor_id"))
                                (default-membership-actor-id (map :slug (or (get @roles-by-m (aget membership "id")) []))))
                   :orgName (aget membership "org_name")
                   :orgSlug (aget membership "org_slug")
                   :status (aget membership "status")
                   :isDefault (aget membership "is_default")
                   :createdAt (aget membership "created_at")
                   :updatedAt (aget membership "updated_at")
                   :roles (or (get @roles-by-m (aget membership "id")) [])
                   :toolPolicies (or (get @tools-by-m (aget membership "id")) [])})))))))))

;; ---------------------------------------------------------------------------
;; Request Context
;; ---------------------------------------------------------------------------

(defn- load-detailed-roles
  [pool role-ids]
  (if (empty? role-ids)
    (js/Promise.resolve [])
    (-> (query! pool
                "SELECT * FROM roles WHERE id = ANY($1::uuid[]) ORDER BY name ASC"
                [(into-array role-ids)])
        (.then (fn [result]
                 (hydrate-role-maps pool (aget result "rows")))))))

(defn- find-request-membership-row
  [pool headers-like]
  (let [membership-id (header-value headers-like "x-knoxx-membership-id")
        user-email (str/lower-case (header-value headers-like "x-knoxx-user-email"))
        org-id (header-value headers-like "x-knoxx-org-id")
        org-slug (str/lower-case (header-value headers-like "x-knoxx-org-slug"))]
    (cond
      (and (str/blank? membership-id) (str/blank? user-email))
      (js/Promise.reject
       (http-error 401
                   "Knoxx request context is missing x-knoxx-user-email or x-knoxx-membership-id"
                   "request_context_missing"))

      (not (str/blank? membership-id))
      (query-one! pool
                  "SELECT m.*, u.email, u.display_name, u.status AS user_status, o.slug AS org_slug, o.name AS org_name, o.status AS org_status, o.is_primary, o.kind AS org_kind FROM memberships m JOIN users u ON u.id = m.user_id JOIN orgs o ON o.id = m.org_id WHERE m.id = $1::uuid"
                  [membership-id])

      (and (not (str/blank? user-email))
           (or (not (str/blank? org-id)) (not (str/blank? org-slug))))
      (query-one! pool
                  "SELECT m.*, u.email, u.display_name, u.status AS user_status, o.slug AS org_slug, o.name AS org_name, o.status AS org_status, o.is_primary, o.kind AS org_kind FROM memberships m JOIN users u ON u.id = m.user_id JOIN orgs o ON o.id = m.org_id WHERE lower(u.email) = $1 AND (($2 <> '' AND o.id = $2::uuid) OR ($3 <> '' AND lower(o.slug) = $3)) ORDER BY m.is_default DESC, m.created_at ASC LIMIT 1"
                  [user-email org-id org-slug])

      :else
      (query-one! pool
                  "SELECT m.*, u.email, u.display_name, u.status AS user_status, o.slug AS org_slug, o.name AS org_name, o.status AS org_status, o.is_primary, o.kind AS org_kind FROM memberships m JOIN users u ON u.id = m.user_id JOIN orgs o ON o.id = m.org_id WHERE lower(u.email) = $1 ORDER BY m.is_default DESC, o.is_primary DESC, m.created_at ASC LIMIT 1"
                  [user-email]))))

(defn- build-request-context
  [pool membership-row]
  (cond
    (not membership-row)
    (js/Promise.reject
     (http-error 401 "Knoxx request context did not resolve to a membership"
                 "request_context_unresolved"))

    (not= (aget membership-row "user_status") "active")
    (js/Promise.reject (http-error 403 "Knoxx user is not active" "user_inactive"))

    (not= (aget membership-row "status") "active")
    (js/Promise.reject (http-error 403 "Knoxx membership is not active"
                                   "membership_inactive"))

    (not= (aget membership-row "org_status") "active")
    (js/Promise.reject (http-error 403 "Knoxx org is not active" "org_inactive"))

    :else
    (-> (hydrate-memberships pool [membership-row])
        (.then
         (fn [memberships]
           (let [membership (first memberships)
                 role-ids (mapv :id (:roles membership))]
             (-> (load-detailed-roles pool role-ids)
                 (.then
                  (fn [detailed-roles]
                   (let [permissions (sort (unique (mapcat :permissions detailed-roles)))
                          effective-tool-policies
                          (merge-toolPolicies
                           (mapcat :toolPolicies detailed-roles)
                           (:toolPolicies membership))
                          role-slugs (sort-by #(- (rolePriority %))
                                              (map :slug detailed-roles))
                          actor-id (or (normalize-actor-id (aget membership-row "actor_id"))
                                       (default-membership-actor-id role-slugs))]
                      (clj->js
                       {:user {:id (aget membership-row "user_id")
                               :email (aget membership-row "email")
                               :username (aget membership-row "email")
                               :displayName (aget membership-row "display_name")
                               :status (aget membership-row "user_status")}
                        :org {:id (aget membership-row "org_id")
                              :slug (aget membership-row "org_slug")
                              :name (aget membership-row "org_name")
                              :status (aget membership-row "org_status")
                              :isPrimary (aget membership-row "is_primary")
                              :kind (aget membership-row "org_kind")}
                        :membership {:id (:id membership)
                                     :actorId actor-id
                                     :status (:status membership)
                                     :isDefault (:isDefault membership)
                                     :createdAt (:createdAt membership)
                                     :updatedAt (:updatedAt membership)}
                        :actor {:id actor-id}
                        :roles detailed-roles
                        :roleSlugs role-slugs
                        :permissions permissions
                        :toolPolicies effective-tool-policies
                        :membershipToolPolicies (:toolPolicies membership)
                        :isSystemAdmin (contains? (set role-slugs)
                                                  "system_admin")})))))))))))

;; ---------------------------------------------------------------------------
;; Bootstrap User
;; ---------------------------------------------------------------------------

(defn- ensure-bootstrap-user!
  [pool primary-org options]
  (let [email (str/lower-case
               (str (or (:bootstrapSystemAdminEmail options)
                        "system-admin@open-hax.local")))
        display-name (str (or (:bootstrapSystemAdminName options)
                              "Knoxx System Admin"))]
    (-> (query-one! pool
                    "INSERT INTO users (email, display_name, auth_provider, status) VALUES ($1, $2, 'bootstrap', 'active') ON CONFLICT (email) DO UPDATE SET display_name = EXCLUDED.display_name, updated_at = NOW() RETURNING *"
                    [email display-name])
        (.then
         (fn [user]
           (-> (query-one! pool
                           "INSERT INTO memberships (user_id, org_id, status, is_default) VALUES ($1, $2, 'active', TRUE) ON CONFLICT (user_id, org_id) DO UPDATE SET is_default = TRUE, updated_at = NOW() RETURNING *"
                           [(aget user "id") (aget primary-org "id")])
               (.then
                (fn [membership]
                  (-> (find-role pool {:slug "system_admin" :org-id nil})
                      (.then
                       (fn [system-admin]
                         (let [insert-role-promise (if system-admin
                                                     (query! pool
                                                             "INSERT INTO membership_roles (membership_id, role_id) VALUES ($1, $2) ON CONFLICT (membership_id, role_id) DO NOTHING"
                                                             [(aget membership "id") (aget system-admin "id")])
                                                     (js/Promise.resolve nil))]
                           (-> insert-role-promise
                               (.then (fn [_]
                                        (set-membership-actor-id! pool (aget membership "id") "system_admin")))
                               (.then (fn [_]
                                        #js {:user user :membership membership})))))))))))))))

(defn- parse-bootstrap-allowlist-emails
  [options]
  (let [raw (or (aget ^js options "bootstrapAllowlistEmails")
                (:bootstrapAllowlistEmails options)
                "")
        raw (str raw)
        parts (->> (clojure.string/split raw #"[\s,]+")
                   (map str/trim)
                   (remove str/blank?)
                   (map str/lower-case)
                   distinct
                   vec)]
    parts))

(defn- parse-bootstrap-allowlist-role-slugs
  [options]
  (let [raw (or (aget ^js options "bootstrapAllowlistRoleSlugs")
                (:bootstrapAllowlistRoleSlugs options)
                "")
        raw (str raw)
        parts (->> (clojure.string/split raw #"[\s,]+")
                   (map str/trim)
                   (remove str/blank?)
                   distinct
                   vec)]
    (if (seq parts) parts ["knowledge_worker"])))

(defn- ensure-bootstrap-allowlist-users!
  [pool primary-org options]
  (let [emails (parse-bootstrap-allowlist-emails options)
        role-slugs (parse-bootstrap-allowlist-role-slugs options)
        org-id (aget primary-org "id")]
    (if (empty? emails)
      (js/Promise.resolve nil)
      (-> (js/Promise.all
           (clj->js
            (mapv
             (fn [email]
               (-> (query-one! pool
                               "INSERT INTO users (email, display_name, auth_provider, status) VALUES ($1, $2, 'bootstrap', 'active') ON CONFLICT (email) DO UPDATE SET updated_at = NOW() RETURNING *"
                               [email email])
                   (.then
                    (fn [user]
                      (query-one! pool
                                 "INSERT INTO memberships (user_id, org_id, status, is_default) VALUES ($1, $2, 'active', FALSE) ON CONFLICT (user_id, org_id) DO UPDATE SET updated_at = NOW() RETURNING *"
                                 [(aget user "id") org-id])))
                   (.then
                    (fn [membership]
                      (-> (js/Promise.all
                           (clj->js
                            (mapv
                             (fn [slug]
                               (-> (find-role pool {:slug slug :org-id org-id})
                                   (.then (fn [role]
                                            (if role
                                              role
                                              (find-role pool {:slug slug :org-id nil}))))
                                   (.then
                                    (fn [role]
                                      (when role
                                        (query! pool
                                                "INSERT INTO membership_roles (membership_id, role_id) VALUES ($1, $2) ON CONFLICT (membership_id, role_id) DO NOTHING"
                                                [(aget membership "id") (aget role "id")]))))))
                             role-slugs)))
                          (.then (fn [_]
                                   (set-membership-actor-id! pool
                                                             (aget membership "id")
                                                             (default-membership-actor-id role-slugs))))
                          (.then (fn [_] #js {:email email :ok true})))))))
             emails)))
          (.then (fn [_] nil))))))
;; ---------------------------------------------------------------------------
;; Audit
;; ---------------------------------------------------------------------------

(defn- append-audit!
  [pool {:keys [actor-user-id actor-membership-id org-id action
                resource-kind resource-id before after]}]
  (query! pool
          "INSERT INTO audit_events (actor_user_id, actor_membership_id, org_id, action, resource_kind, resource_id, before_json, after_json) VALUES ($1, $2, $3, $4, $5, $6, $7::jsonb, $8::jsonb)"
          [actor-user-id actor-membership-id org-id action resource-kind resource-id
           (when before (js/JSON.stringify (clj->js before)))
           (when after (js/JSON.stringify (clj->js after)))]))

;; ---------------------------------------------------------------------------
;; JS Conversion Helpers
;; ---------------------------------------------------------------------------

(defn- ->js-permission [code]
  #js {:code code})

(defn- ->js-tool [row]
  #js {:id (aget row "id")
       :label (aget row "label")
       :description (aget row "description")
       :riskLevel (aget row "risk_level")})

(defn- ->js-org [row]
  #js {:id (aget row "id")
       :slug (aget row "slug")
       :name (aget row "name")
       :kind (aget row "kind")
       :isPrimary (aget row "is_primary")
       :status (aget row "status")})

(defn- ->js-org-with-counts [row]
  #js {:id (aget row "id")
       :slug (aget row "slug")
       :name (aget row "name")
       :kind (aget row "kind")
       :isPrimary (aget row "is_primary")
       :status (aget row "status")
       :memberCount (js/Number (or (aget row "member_count") 0))
       :roleCount (js/Number (or (aget row "role_count") 0))
       :dataLakeCount (js/Number (or (aget row "data_lake_count") 0))
       :createdAt (aget row "created_at")
       :updatedAt (aget row "updated_at")})

(defn- ->js-data-lake [row]
  #js {:id (aget row "id")
       :orgId (aget row "org_id")
       :name (aget row "name")
       :slug (aget row "slug")
       :kind (aget row "kind")
       :config (or (aget row "config_json") {})
       :status (aget row "status")
       :createdAt (aget row "created_at")
       :updatedAt (aget row "updated_at")})

;; ---------------------------------------------------------------------------
;; Factory Method Helpers
;; ---------------------------------------------------------------------------

(defn- factory-resolve-request-context
  [pool headers-like]
  (-> (find-request-membership-row pool headers-like)
      (.then (fn [row] (build-request-context pool row)))))

(defn- factory-evaluate-tool-access
  [pool headers-like tool-id]
  (-> (find-request-membership-row pool headers-like)
      (.then (fn [row] (build-request-context pool row)))
      (.then (fn [ctx]
               #js {:context ctx
                    :toolId tool-id
                    :allowed (tool-allowed ctx tool-id)}))))

(defn- factory-list-permissions
  [_pool]
  (let [all-codes (->> (contracts-roles/list-role-slugs (contracts-config))
                       (mapcat #(contracts-roles/role-permissions (contracts-config) %))
                       distinct
                       sort
                       vec)]
    (js/Promise.resolve
     #js {:permissions (into-array (map ->js-permission all-codes))})))

(defn- factory-list-tools
  [pool]
  (-> (query! pool "SELECT id, label, description, risk_level FROM tool_definitions ORDER BY id ASC" [])
      (.then (fn [r]
               #js {:tools (into-array (map ->js-tool (aget r "rows")))}))))

(defn- factory-get-bootstrap-context
  [pool primary-org bootstrap]
  (let [uid (aget ^js bootstrap "user" "id")
        mid (aget ^js bootstrap "membership" "id")]
    (js/Promise.resolve
      #js {:primaryOrg (->js-org primary-org)
           :bootstrapUser #js {:id uid
                               :email (aget ^js bootstrap "user" "email")
                               :displayName (aget ^js bootstrap "user" "display_name")
                               :membershipId mid}})))

(defn- factory-list-orgs
  [pool]
  (-> (query! pool "SELECT o.*, COUNT(DISTINCT m.id) AS member_count, COUNT(DISTINCT r.id) FILTER (WHERE r.org_id = o.id) AS role_count, COUNT(DISTINCT d.id) AS data_lake_count FROM orgs o LEFT JOIN memberships m ON m.org_id = o.id LEFT JOIN roles r ON r.org_id = o.id LEFT JOIN data_lakes d ON d.org_id = o.id GROUP BY o.id ORDER BY o.is_primary DESC, o.name ASC" [])
      (.then (fn [r]
               #js {:orgs (into-array (map ->js-org-with-counts (aget r "rows")))}))))

(defn- factory-create-org
  [pool uid mid payload]
  (let [name (str/trim (str (or (aget payload "name") "")))]
    (if (str/blank? name)
      (js/Promise.reject (js/Error. "name is required"))
      (let [slug (slugify (or (aget payload "slug") name) "org")
            kind (str (or (aget payload "kind") "customer"))
            status (str (or (aget payload "status") "active"))]
        (-> (query-one! pool "INSERT INTO orgs (slug, name, kind, is_primary, status) VALUES ($1, $2, $3, FALSE, $4) RETURNING *" [slug name kind status])
            (.then
             (fn [org]
               (-> (ensure-builtin-org-roles! pool org)
                   (.then (fn [_]
                            (append-audit! pool {:actor-user-id uid
                                                 :actor-membership-id mid
                                                 :org-id (aget org "id")
                                                 :action "org.create"
                                                 :resource-kind "org"
                                                 :resource-id (aget org "id")})))
                   (.then (fn [_] #js {:org (->js-org org)}))))))))))

(defn- factory-list-roles
  [pool opts]
  (let [org-id (some-> opts (aget "orgId"))]
    (-> (if org-id
          (query! pool "SELECT * FROM roles WHERE org_id = $1 ORDER BY built_in DESC, name ASC" [org-id])
          (query! pool "SELECT * FROM roles ORDER BY built_in DESC, name ASC" []))
        (.then (fn [r] (hydrate-role-maps pool (aget r "rows"))))
        (.then (fn [roles] #js {:roles (into-array roles)})))))

(defn- factory-get-role
  [pool role-id]
  (-> (query-one! pool "SELECT * FROM roles WHERE id = $1::uuid" [role-id])
      (.then (fn [row]
               (if row
                 (-> (hydrate-role-maps pool [row])
                     (.then (fn [h] #js {:role (first h)})))
                 #js {:role nil})))))

(defn- factory-create-role
  [pool uid mid payload]
  (let [org-id (str/trim (str (or (aget payload "orgId") "")))
        name (str/trim (str (or (aget payload "name") "")))]
    (cond
      (str/blank? org-id) (js/Promise.reject (js/Error. "orgId is required"))
      (str/blank? name) (js/Promise.reject (js/Error. "name is required"))
      :else
      (let [slug (slugify (or (aget payload "slug") name) "role")]
        (-> (ensure-role! pool {:org-id org-id :name name :slug slug
                                :scope-kind "org" :built-in false
                                :system-managed false})
            (.then
             (fn [role]
               (let [rid (aget role "id")]
                 (-> (set-role-permissions! pool rid (or (aget payload "permissionCodes") #js []))
                     (.then (fn [_]
                              (set-role-tool-policies! pool rid
                                                       (or (aget payload "toolPolicies") #js []))))
                     (.then (fn [_]
                              (append-audit! pool {:actor-user-id uid
                                                   :actor-membership-id mid
                                                   :org-id org-id
                                                   :action "role.create"
                                                   :resource-kind "role"
                                                   :resource-id rid})))
                     (.then (fn [_] (hydrate-role-maps pool [role])))
                     (.then (fn [h] #js {:role (first h)})))))))))))

(defn- factory-set-role-tool-policies
  [pool uid mid role-id payload]
  (-> (query-one! pool "SELECT * FROM roles WHERE id = $1" [role-id])
      (.then
       (fn [role]
         (if-not role
           (js/Promise.reject (js/Error. "role not found"))
           (-> (set-role-tool-policies! pool role-id
                                        (or (aget payload "toolPolicies") #js []))
               (.then (fn [_]
                        (append-audit! pool {:actor-user-id uid
                                             :actor-membership-id mid
                                             :org-id (aget role "org_id")
                                             :action "role.tool_policy.update"
                                             :resource-kind "role"
                                             :resource-id role-id})))
               (.then (fn [_] (hydrate-role-maps pool [role])))
               (.then (fn [h] #js {:role (first h)}))))))))

(defn- factory-list-users
  [pool opts]
  (let [org-id (some-> opts (aget "orgId"))]
    (-> (if org-id
          (query! pool "SELECT DISTINCT u.* FROM users u JOIN memberships m ON m.user_id = u.id WHERE m.org_id = $1::uuid ORDER BY u.display_name ASC, u.email ASC" [org-id])
          (query! pool "SELECT * FROM users ORDER BY display_name ASC, email ASC" []))
        (.then
         (fn [user-result]
           (let [users (aget user-result "rows")]
             #js {:users (into-array
                          (for [i (range (.-length users))]
                            (let [u (aget users i)]
                              #js {:id (aget u "id")
                                   :email (aget u "email")
                                   :displayName (aget u "display_name")
                                   :authProvider (aget u "auth_provider")
                                   :externalSubject (aget u "external_subject")
                                   :status (aget u "status")
                                   :createdAt (aget u "created_at")
                                   :updatedAt (aget u "updated_at")
                                   :memberships []})))}))))))

(defn- factory-create-user
  [pool uid mid payload]
  (let [email (str/lower-case (str/trim (str (or (aget payload "email") ""))))
        org-id (str/trim (str (or (aget payload "orgId")
                                  (aget payload "org_id") "")))]
    (cond
      (str/blank? email) (js/Promise.reject (js/Error. "email is required"))
      (str/blank? org-id) (js/Promise.reject (js/Error. "orgId is required"))
      :else
      (let [dn (str/trim (str (or (aget payload "displayName")
                                  (aget payload "display_name") email)))
            requested-role-slugs (vec (or (aget payload "roleSlugs") #js ["knowledge_worker"]))
            requested-actor-id (or (normalize-actor-id (aget payload "actorId"))
                                   (some-> (find-user-actor-contract-by-email email) :id)
                                   (user-actor-id-from-email email)
                                   (default-membership-actor-id requested-role-slugs))]
        (-> (query-one! pool
                        "INSERT INTO users (email, display_name, auth_provider, external_subject, status) VALUES ($1, $2, $3, $4, $5) ON CONFLICT (email) DO UPDATE SET display_name = EXCLUDED.display_name, auth_provider = EXCLUDED.auth_provider, external_subject = EXCLUDED.external_subject, status = EXCLUDED.status, updated_at = NOW() RETURNING *"
                        [email (or dn email)
                         (str (or (aget payload "authProvider") "local"))
                         (or (aget payload "externalSubject") nil)
                         (str (or (aget payload "status") "active"))])
            (.then
             (fn [user]
               (-> (query-one! pool
                               "INSERT INTO memberships (user_id, org_id, status, is_default) VALUES ($1, $2, $3, $4) ON CONFLICT (user_id, org_id) DO UPDATE SET status = EXCLUDED.status, is_default = EXCLUDED.is_default, updated_at = NOW() RETURNING *"
                               [(aget user "id") org-id
                                (str (or (aget payload "membershipStatus") "active"))
                                (not= (aget payload "isDefault") false)])
                   (.then
                    (fn [ms]
                      (set-membership-roles! pool (aget ms "id")
                                             {:org-id org-id
                                              :role-ids (or (aget payload "roleIds") #js [])
                                              :role-slugs requested-role-slugs
                                              :replace true})))
                   (.then
                    (fn [_]
                      (query-one! pool
                                  "SELECT id FROM memberships WHERE user_id = $1::uuid AND org_id = $2::uuid"
                                  [(aget user "id") org-id])))
                   (.then
                    (fn [membership-row]
                      (-> (set-membership-actor-id! pool
                                                    (aget membership-row "id")
                                                    requested-actor-id)
                          (.then (fn [_]
                                   (find-org-by-id pool org-id)))
                          (.then (fn [org-row]
                                   (upsert-user-actor-contract!
                                    {:actor-id requested-actor-id
                                     :email email
                                     :display-name dn
                                     :org-slug (aget org-row "slug")
                                     :role-slugs requested-role-slugs}))))))
                   (.then
                    (fn [_]
                      (append-audit! pool {:actor-user-id uid
                                           :actor-membership-id mid
                                           :org-id org-id
                                           :action "user.create_or_update"
                                           :resource-kind "user"
                                           :resource-id (aget user "id")})))
                   (.then (fn [_] #js {:user nil}))))))))))

(defn- factory-list-memberships
  [pool opts]
  (let [org-id (aget opts "orgId")]
    (if (str/blank? org-id)
      (js/Promise.reject (js/Error. "orgId is required"))
      (-> (query! pool "SELECT m.*, o.name AS org_name, o.slug AS org_slug FROM memberships m JOIN orgs o ON o.id = m.org_id WHERE m.org_id = $1::uuid ORDER BY m.created_at ASC" [org-id])
          (.then (fn [r] (hydrate-memberships pool (aget r "rows"))))
          (.then (fn [ms] #js {:memberships (into-array ms)}))))))

(defn- factory-get-membership
  [pool membership-id]
  (-> (query-one! pool "SELECT m.*, o.name AS org_name, o.slug AS org_slug FROM memberships m JOIN orgs o ON o.id = m.org_id WHERE m.id = $1::uuid" [membership-id])
      (.then (fn [row]
               (if row
                 (-> (hydrate-memberships pool [row])
                     (.then (fn [h] #js {:membership (first h)})))
                 #js {:membership nil})))))

(defn- factory-set-membership-roles
  [pool uid mid membership-id payload]
  (-> (query-one! pool "SELECT * FROM memberships WHERE id = $1" [membership-id])
      (.then
       (fn [ms]
         (if-not ms
           (js/Promise.reject (js/Error. "membership not found"))
           (-> (set-membership-roles! pool membership-id
                                      {:org-id (aget ms "org_id")
                                       :role-ids (or (aget payload "roleIds") #js [])
                                       :role-slugs (or (aget payload "roleSlugs") #js [])
                                       :replace (not= (aget payload "replace") false)})
               (.then
               (fn [_]
                  (let [requested-role-slugs (vec (or (aget payload "roleSlugs") #js []))
                        requested-actor-id (or (normalize-actor-id (aget payload "actorId"))
                                               (normalize-actor-id (aget ms "actor_id"))
                                               (if (seq requested-role-slugs)
                                                 (default-membership-actor-id requested-role-slugs)
                                                 "workspace_user"))]
                    (-> (set-membership-actor-id! pool membership-id requested-actor-id)
                        (.then (fn [_]
                                 (query-one! pool
                                             "SELECT m.id, u.email, u.display_name, o.slug AS org_slug FROM memberships m JOIN users u ON u.id = m.user_id JOIN orgs o ON o.id = m.org_id WHERE m.id = $1::uuid"
                                             [membership-id])))
                        (.then (fn [membership-row]
                                 (upsert-user-actor-contract!
                                  {:actor-id requested-actor-id
                                   :email (aget membership-row "email")
                                   :display-name (aget membership-row "display_name")
                                   :org-slug (aget membership-row "org_slug")
                                   :role-slugs requested-role-slugs}))))))
               (.then
                (fn [_]
                  (append-audit! pool {:actor-user-id uid
                                       :actor-membership-id mid
                                       :org-id (aget ms "org_id")
                                       :action "membership.roles.update"
                                       :resource-kind "membership"
                                       :resource-id membership-id})))
               (.then (fn [_] #js {:membership nil})))))))))

(defn- factory-set-membership-tool-policies
  [pool uid mid membership-id payload]
  (-> (query-one! pool "SELECT * FROM memberships WHERE id = $1" [membership-id])
      (.then
       (fn [ms]
         (if-not ms
           (js/Promise.reject (js/Error. "membership not found"))
           (-> (set-membership-tool-policies! pool membership-id
                                              (or (aget payload "toolPolicies") #js []))
               (.then
                (fn [_]
                  (append-audit! pool {:actor-user-id uid
                                       :actor-membership-id mid
                                       :org-id (aget ms "org_id")
                                       :action "membership.tool_policy.update"
                                       :resource-kind "membership"
                                       :resource-id membership-id})))
               (.then (fn [_] #js {:membership nil}))))))))

(defn- factory-list-data-lakes
  [pool opts]
  (let [org-id (aget opts "orgId")]
    (if (str/blank? org-id)
      (js/Promise.reject (js/Error. "orgId is required"))
      (-> (query! pool "SELECT * FROM data_lakes WHERE org_id = $1::uuid ORDER BY name ASC" [org-id])
          (.then (fn [r]
                   #js {:dataLakes (into-array (map ->js-data-lake (aget r "rows")))}))))))

(defn- factory-create-data-lake
  [pool uid mid payload]
  (let [org-id (str/trim (str (or (aget payload "orgId")
                                  (aget payload "org_id") "")))
        name (str/trim (str (or (aget payload "name") "")))]
    (cond
      (str/blank? org-id) (js/Promise.reject (js/Error. "orgId is required"))
      (str/blank? name) (js/Promise.reject (js/Error. "name is required"))
      :else
      (let [slug (slugify (or (aget payload "slug") name) "lake")
            kind (str (or (aget payload "kind") "workspace_docs"))
            status (str (or (aget payload "status") "active"))
            config (normalize-lake-config
                    (or (aget payload "config") (aget payload "config_json")))]
        (-> (query-one! pool
                        "INSERT INTO data_lakes (org_id, name, slug, kind, config_json, status) VALUES ($1, $2, $3, $4, $5::jsonb, $6) RETURNING *"
                        [org-id name slug kind
                         (js/JSON.stringify (clj->js config)) status])
            (.then
             (fn [lake]
               (-> (append-audit! pool {:actor-user-id uid
                                        :actor-membership-id mid
                                        :org-id org-id
                                        :action "data_lake.create"
                                        :resource-kind "data_lake"
                                        :resource-id (aget lake "id")})
                   (.then (fn [_] #js {:dataLake (->js-data-lake lake)}))))))))))

;; ---------------------------------------------------------------------------
;; Session persistence (Postgres)
;; ---------------------------------------------------------------------------

(defn- hash-token-with-salt
  [token salt]
  (let [h (.createHash crypto "sha256")]
    (.update h (str salt ":" token) "utf8")
    (.digest h "hex")))

;; Deterministic prefix used to narrow the candidate session set quickly.
;; We still verify the salted token_hash for correctness.
(defn- token-prefix
  [token]
  (let [h (.createHash crypto "sha256")]
    (.update h (str token) "utf8")
    (subs (.digest h "hex") 0 12)))

(defn- generate-salt
  []
  (.toString (.randomBytes crypto 16) "hex"))

(defn- factory-create-session
  [pool session-data]
  (let [token (or (aget session-data "token") "")
        ttl-secs (js/parseInt (or (aget (.-env js/process) "KNOXX_SESSION_TTL_SECONDS") "86400") 10)
        salt (generate-salt)
        token-hash (hash-token-with-salt token salt)
        prefix (token-prefix token)
        expires-at (js/Date. (+ (js/Date.now) (* ttl-secs 1000)))]
    (if (str/blank? token)
      (js/Promise.reject (js/Error. "token is required for session creation"))
      (-> (query-one! pool
                      "INSERT INTO sessions (user_id, membership_id, org_id, token_hash, token_prefix, salt, email, display_name, auth_provider, external_subject, ip_address, user_agent, expires_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13) RETURNING *"
                      [(or (aget session-data "userId") "")
                       (or (aget session-data "membershipId") "")
                       (or (aget session-data "orgId") "")
                       token-hash
                       prefix
                       salt
                       (or (aget session-data "email") "")
                       (or (aget session-data "displayName") "")
                       (or (aget session-data "authProvider") "github")
                       (or (aget session-data "externalSubject") nil)
                       (or (aget session-data "ipAddress") nil)
                       (or (aget session-data "userAgent") nil)
                       (.toISOString expires-at)])
          (.then
           (fn [row]
             #js {:session #js {:id (aget row "id")
                                :userId (aget row "user_id")
                                :membershipId (aget row "membership_id")
                                :orgId (aget row "org_id")
                                :email (aget row "email")
                                :displayName (aget row "display_name")
                                :authProvider (aget row "auth_provider")
                                :expiresAt (aget row "expires_at")
                                :createdAt (aget row "created_at")}}))))))
(defn- find-session-in-rows
  [pool token rows]
  (loop [i 0]
    (if (>= i (.-length rows))
      nil
      (let [row (aget rows i)
            h  (aget row "token_hash")
            s  (aget row "salt")
            c  (hash-token-with-salt token s)]
        (if (= h c)
          (do
            (.catch (query! pool "UPDATE sessions SET last_seen_at = NOW() WHERE id = $1" [(aget row "id")])
                    (fn [_err] nil))
            #js {:session #js {:id            (aget row "id")
                               :userId        (aget row "user_id")
                               :membershipId  (aget row "membership_id")
                               :orgId         (aget row "org_id")
                               :email         (aget row "email")
                               :displayName   (aget row "display_name")
                               :authProvider  (aget row "auth_provider")
                               :expiresAt     (aget row "expires_at")
                               :createdAt     (aget row "created_at")}})
          (recur (inc i)))))))

(defn- factory-get-session-by-token
  [pool token]
  (if (str/blank? token)
    (js/Promise.resolve nil)
    (let [prefix (token-prefix token)]
      (-> (query! pool
                  "SELECT * FROM sessions WHERE token_prefix = $1 AND expires_at > NOW() ORDER BY created_at DESC LIMIT 50"
                  [prefix])
          (.then
           (fn [result]
             (let [rows (aget result "rows")
                   found (find-session-in-rows pool token rows)]
               (if found
                 found
                 ;; Back-compat for legacy sessions created before token_prefix existed.
                 (-> (query! pool
                             "SELECT * FROM sessions WHERE expires_at > NOW() ORDER BY created_at DESC LIMIT 200"
                             [])
                     (.then (fn [fallback-result]
                              (find-session-in-rows pool token (aget fallback-result "rows")))))))))
          (.catch (fn [_err] nil))))))

(defn- factory-delete-session-by-token
  [pool token]
  (-> (factory-get-session-by-token pool token)
      (.then
       (fn [result]
         (when (and result (aget result "session") (aget result "session" "id"))
           (.catch (query! pool "DELETE FROM sessions WHERE id = $1" [(aget result "session" "id")])
                   (fn [_] nil)))
         result))))

(defn- factory-cleanup-expired-sessions
  [pool]
  (-> (query! pool "DELETE FROM sessions WHERE expires_at < NOW()" [])
      (.then (fn [result]
               (let [count (or (aget result "rowCount") 0)]
                 (when (> count 0)
                   (.log js/console (str "[knoxx-policy-db] Cleaned up " count " expired sessions")))
               count)))
      (.catch (fn [_err] 0))))

(defn- factory-create-invite
  [pool uid mid payload]
  (let [org-id (str/trim (str (or (aget payload "orgId") "")))
        email (str/lower-case (str/trim (str (or (aget payload "email") ""))))
        role-slugs (or (aget payload "roleSlugs") #js ["knowledge_worker"])
        role-slugs-array (cond
                           (js/Array.isArray role-slugs) role-slugs
                           (sequential? role-slugs) (into-array role-slugs)
                           :else #js ["knowledge_worker"])
        inviter-mid (or (aget payload "inviterMembershipId") mid)
        code (.toString (.randomBytes crypto 8) "hex")
        ttl-secs (* 7 24 3600)
        expires-at (js/Date. (+ (js/Date.now) (* ttl-secs 1000)))]
    (cond
      (str/blank? org-id) (js/Promise.reject (js/Error. "orgId is required"))
      (str/blank? email) (js/Promise.reject (js/Error. "email is required"))
      :else
      (-> (query-one! pool
                      "INSERT INTO invites (org_id, code, email, inviter_membership_id, role_slugs, status, expires_at) VALUES ($1, $2, $3, $4, $5::jsonb, 'pending', $6) RETURNING *"
                      [org-id code email inviter-mid
                       (js/JSON.stringify role-slugs-array)
                       (.toISOString expires-at)])
          (.then
           (fn [row]
             (-> (append-audit! pool {:actor-user-id uid
                                      :actor-membership-id mid
                                      :org-id org-id
                                      :action "invite.create"
                                      :resource-kind "invite"
                                      :resource-id (aget row "id")})
                 (.then (fn [_]
                          #js {:invite #js {:id (aget row "id")
                                            :orgId (aget row "org_id")
                                            :code code
                                            :email email
                                            :status (aget row "status")
                                            :expiresAt (aget row "expires_at")
                                            :createdAt (aget row "created_at")}})))))))))

(defn- factory-redeem-invite
  [pool code email]
  (if (or (str/blank? code) (str/blank? email))
    (js/Promise.reject (js/Error. "code and email are required"))
    (-> (query-one! pool
                    "SELECT * FROM invites WHERE code = $1 AND status = 'pending' AND expires_at > NOW()"
                    [code])
        (.then
         (fn [invite]
           (if-not invite
             (js/Promise.reject (let [err (js/Error. "Invalid or expired invite code")]
                                  (set! (.-status err) 400)
                                  err))
             (let [invite-id (aget invite "id")
                   invite-email (str/lower-case (str (aget invite "email")))
                   normalized-email (str/lower-case (str email))]
               (when-not (= invite-email normalized-email)
                 (throw (let [err (js/Error. "Invite email does not match")]
                          (set! (.-status err) 403)
                          err)))
               (-> (query-one! pool
                               "UPDATE invites SET status = 'redeemed', redeemed_at = NOW() WHERE id = $1 RETURNING *"
                               [invite-id])
                   (.then
                    (fn [updated]
                      #js {:invite #js {:id (aget updated "id")
                                        :orgId (aget updated "org_id")
                                        :code code
                                        :email (aget updated "email")
                                        :status (aget updated "status")
                                        :redeemedAt (aget updated "redeemed_at")
                                        :createdAt (aget updated "created_at")}
                           ;; For now we don't auto-provision a user here.
                           :user nil}))))))))))

(defn- factory-list-invites
  [pool opts]
  (let [org-id (aget opts "orgId")
        status-filter (aget opts "status")]
    (if (str/blank? org-id)
      (js/Promise.reject (js/Error. "orgId is required"))
      (-> (if status-filter
            (query! pool
                    "SELECT * FROM invites WHERE org_id = $1::uuid AND status = $2 ORDER BY created_at DESC"
                    [org-id status-filter])
            (query! pool
                    "SELECT * FROM invites WHERE org_id = $1::uuid ORDER BY created_at DESC"
                    [org-id]))
          (.then
           (fn [result]
             (let [rows (aget result "rows")
                   invites (array)]
               (dotimes [i (.-length rows)]
                 (let [row (aget rows i)
                       role-slugs (try
                                    (let [v (aget row "role_slugs")]
                                      (cond
                                        (nil? v) []
                                        (string? v) (js->clj (js/JSON.parse v))
                                        :else (js->clj v)))
                                    (catch :default _ []))]
                   (.push invites
                          #js {:id (aget row "id")
                               :orgId (aget row "org_id")
                               :code (aget row "code")
                               :email (aget row "email")
                               :status (aget row "status")
                               :roleSlugs (into-array role-slugs)
                               :expiresAt (aget row "expires_at")
                               :redeemedAt (aget row "redeemed_at")
                               :createdAt (aget row "created_at")})))
               #js {:invites invites})))))))

;; ---------------------------------------------------------------------------
;; Factory
;; ---------------------------------------------------------------------------

(defn create-policy-db
  [options]
  (let [conn-str (or (aget options "connectionString")
                     (:connectionString options)
                     "")]
    (if (str/blank? conn-str)
      (js/Promise.resolve nil)
      (js/Promise.
       (fn [resolve reject]
         (let [pool (new (.-Pool pg) (clj->js {:connectionString conn-str}))]
           (-> (ensure-schema! pool)
               (.then (fn [_] (insert-permission-seeds! pool)))
               (.then (fn [_] (insert-tool-seeds! pool)))
               (.then (fn [_] (ensure-primary-org! pool options)))
               (.then
                (fn [primary-org]
                  (-> (ensure-builtin-platform-roles! pool)
                      (.then (fn [_] (ensure-builtin-org-roles! pool primary-org)))
                      (.then (fn [_] (ensure-bootstrap-user! pool primary-org options)))
                      ;; Optional: additional allowlisted emails to auto-create
                      ;; as active users in the primary org.
                      (.then (fn [bootstrap]
                               (-> (ensure-bootstrap-allowlist-users! pool primary-org options)
                                   (.catch (fn [err]
                                             (.warn js/console "[policy-db] bootstrap allowlist failed:" (.-message err))
                                             nil))
                                   (.then (fn [_] bootstrap)))))
                      (.then (fn [bootstrap]
                               (-> (sync-user-actors-from-contracts! pool primary-org)
                                   (.catch (fn [err]
                                             (.warn js/console "[policy-db] user actor sync failed:" (.-message err))
                                             nil))
                                   (.then (fn [_] bootstrap)))))
                      (.then (fn [bootstrap]
                               (-> (backfill-membership-actors! pool)
                                   (.then (fn [_] bootstrap)))))
                      (.then
                       (fn [bootstrap]
                         ;; Cleanup expired sessions on startup
                         (.catch (factory-cleanup-expired-sessions pool) (fn [_] nil))
                         (let [uid (aget ^js bootstrap "user" "id")
                               mid (aget ^js bootstrap "membership" "id")]
                           (resolve
                            #js {:close (fn [] (.end pool))

                                 :resolveRequestContext
                                 (fn [headers-like]
                                   (factory-resolve-request-context pool headers-like))

                                 :evaluateToolAccess
                                 (fn [headers-like tool-id]
                                   (factory-evaluate-tool-access pool headers-like tool-id))

                                 :listPermissions
                                 (fn [] (factory-list-permissions pool))

                                 :listTools
                                 (fn [] (factory-list-tools pool))

                                 :getBootstrapContext
                                 (fn []
                                   (factory-get-bootstrap-context pool primary-org bootstrap))

                                 :listOrgs
                                 (fn [] (factory-list-orgs pool))

                                 :createOrg
                                 (fn [payload]
                                   (factory-create-org pool uid mid payload))

                                 :listRoles
                                 (fn [opts]
                                   (factory-list-roles pool opts))

                                 :getRole
                                 (fn [role-id]
                                   (factory-get-role pool role-id))

                                 :createRole
                                 (fn [payload]
                                   (factory-create-role pool uid mid payload))

                                 :setRoleToolPolicies
                                 (fn [role-id payload]
                                   (factory-set-role-tool-policies pool uid mid role-id payload))

                                 :listUsers
                                 (fn [opts]
                                   (factory-list-users pool opts))

                                 :createUser
                                 (fn [payload]
                                   (factory-create-user pool uid mid payload))

                                 :listMemberships
                                 (fn [opts]
                                   (factory-list-memberships pool opts))

                                 :getMembership
                                 (fn [membership-id]
                                   (factory-get-membership pool membership-id))

                                 :setMembershipRoles
                                 (fn [membership-id payload]
                                   (factory-set-membership-roles pool uid mid membership-id payload))

                                 :setMembershipToolPolicies
                                 (fn [membership-id payload]
                                   (factory-set-membership-tool-policies pool uid mid membership-id payload))

                                 :listDataLakes
                                 (fn [opts]
                                   (factory-list-data-lakes pool opts))

                                 :createDataLake
                                 (fn [payload]
                                   (factory-create-data-lake pool uid mid payload))

                                 :createSession
                                 (fn [session-data]
                                   (factory-create-session pool session-data))

                                 :getSessionByToken
                                 (fn [token]
                                   (factory-get-session-by-token pool token))

                                 :deleteSessionByToken
                                 (fn [token]
                                   (factory-delete-session-by-token pool token))

                                 :cleanupExpiredSessions
                                 (fn []
                                   (factory-cleanup-expired-sessions pool))

                                 :createInvite
                                 (fn [payload]
                                   (factory-create-invite pool uid mid payload))

                                 :redeemInvite
                                 (fn [code email]
                                   (factory-redeem-invite pool code email))

                                 :listInvites
                                 (fn [opts]
                                   (factory-list-invites pool opts))

                                 :syncUserFromActorContract
                                 (fn [payload]
                                   (sync-user-from-actor-contract! pool primary-org payload))

                                 :query
                                 (fn [sql params]
                                   (query! pool sql params))})))))))
               (.catch reject))))))))

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
            [honey.sql :as sql]
            ["pg" :as pg]))

;; ---------------------------------------------------------------------------
;; Data Constants
;; ---------------------------------------------------------------------------

(def ^:private PERMISSIONS
  [["platform.org.create" "platform" "create" "Create orgs across the Knoxx platform"]
   ["platform.org.read" "platform" "read" "Read orgs across the Knoxx platform"]
   ["platform.org.update" "platform" "update" "Update orgs across the Knoxx platform"]
   ["platform.org.delete" "platform" "delete" "Delete orgs across the Knoxx platform"]
   ["platform.roles.manage" "platform_roles" "manage" "Manage platform-scoped roles"]
   ["platform.audit.read" "platform_audit" "read" "Read platform-wide audit events"]
   ["org.settings.read" "org_settings" "read" "Read org settings"]
   ["org.settings.update" "org_settings" "update" "Update org settings"]
   ["org.members.read" "org_members" "read" "Read org memberships"]
   ["org.members.create" "org_members" "create" "Create org memberships"]
   ["org.members.update" "org_members" "update" "Update org memberships"]
   ["org.members.delete" "org_members" "delete" "Delete org memberships"]
   ["org.users.invite" "org_users" "invite" "Invite users into an org"]
   ["org.users.create" "org_users" "create" "Create users inside an org"]
   ["org.users.read" "org_users" "read" "Read users inside an org"]
   ["org.users.update" "org_users" "update" "Update users inside an org"]
   ["org.users.disable" "org_users" "disable" "Disable users inside an org"]
   ["org.roles.read" "org_roles" "read" "Read org roles"]
   ["org.roles.create" "org_roles" "create" "Create org roles"]
   ["org.roles.update" "org_roles" "update" "Update org roles"]
   ["org.roles.delete" "org_roles" "delete" "Delete org roles"]
   ["org.tool_policy.read" "org_tool_policy" "read" "Read org tool policies"]
   ["org.tool_policy.update" "org_tool_policy" "update" "Update org tool policies"]
   ["org.user_policy.read" "org_user_policy" "read" "Read per-user policy overrides"]
   ["org.user_policy.update" "org_user_policy" "update" "Update per-user policy overrides"]
   ["org.datalakes.read" "org_datalakes" "read" "Read org data lakes"]
   ["org.datalakes.create" "org_datalakes" "create" "Create org data lakes"]
   ["org.datalakes.update" "org_datalakes" "update" "Update org data lakes"]
   ["org.datalakes.delete" "org_datalakes" "delete" "Delete org data lakes"]
   ["datalake.read" "datalake" "read" "Read a data lake"]
   ["datalake.query" "datalake" "query" "Query a data lake"]
   ["datalake.write" "datalake" "write" "Write to a data lake"]
   ["datalake.ingest" "datalake" "ingest" "Ingest into a data lake"]
   ["datalake.admin" "datalake" "admin" "Administer a data lake"]
   ["agent.chat.use" "agent_chat" "use" "Use the Knoxx agent chat runtime"]
   ["agent.memory.read" "agent_memory" "read" "Read prior Knoxx memory"]
   ["agent.memory.cross_session" "agent_memory" "cross_session" "Search prior Knoxx sessions"]
   ["agent.runs.read_own" "agent_runs" "read_own" "Read own Knoxx runs"]
   ["agent.runs.read_org" "agent_runs" "read_org" "Read org Knoxx runs"]
   ["agent.runs.read_all" "agent_runs" "read_all" "Read all Knoxx runs"]
   ["agent.controls.steer" "agent_controls" "steer" "Steer a live Knoxx run"]
   ["agent.controls.follow_up" "agent_controls" "follow_up" "Queue follow-up on a Knoxx run"]
   ["tool.read.use" "tool" "read" "Use read tool"]
   ["tool.write.use" "tool" "write" "Use write tool"]
   ["tool.edit.use" "tool" "edit" "Use edit tool"]
   ["tool.bash.use" "tool" "bash" "Use bash tool"]
   ["tool.email.send" "tool" "email_send" "Send email from Knoxx"]
   ["tool.discord.publish" "tool" "discord_publish" "Publish to Discord from Knoxx"]
   ["tool.discord.send" "tool" "discord_send" "Send Discord messages and replies from Knoxx"]
   ["tool.discord.read" "tool" "discord_read" "Read Discord channel messages from Knoxx"]
   ["tool.discord.channel.messages" "tool" "discord_channel_messages" "Fetch Discord channel messages with cursors from Knoxx"]
   ["tool.discord.channel.scroll" "tool" "discord_channel_scroll" "Scroll older Discord channel messages from Knoxx"]
   ["tool.discord.dm.messages" "tool" "discord_dm_messages" "Fetch Discord DM messages from Knoxx"]
   ["tool.discord.search" "tool" "discord_search" "Search Discord messages from Knoxx"]
   ["tool.discord.guilds" "tool" "discord_guilds" "List Discord guilds from Knoxx"]
   ["tool.discord.channels" "tool" "discord_channels" "List Discord channels from Knoxx"]
   ["tool.discord.list.servers" "tool" "discord_list_servers" "List all Discord servers from Knoxx"]
   ["tool.discord.list.channels" "tool" "discord_list_channels" "List Discord channels across guilds from Knoxx"]
   ["tool.event_agents.status" "tool" "event_agents_status" "Inspect event-agent runtime state from Knoxx"]
   ["tool.event_agents.dispatch" "tool" "event_agents_dispatch" "Dispatch event-agent events from Knoxx"]
   ["tool.event_agents.run_job" "tool" "event_agents_run_job" "Trigger event-agent jobs from Knoxx"]
   ["tool.event_agents.upsert_job" "tool" "event_agents_upsert_job" "Create/update event-agent jobs from Knoxx"]
   ["tool.schedule_event_agent" "tool" "schedule_event_agent" "Schedule event-agent jobs from Knoxx"]
   ["tool.bluesky.publish" "tool" "bluesky_publish" "Publish to Bluesky from Knoxx"]
   ["tool.semantic_query.use" "tool" "semantic_query" "Use semantic query tool"]
   ["tool.memory_search.use" "tool" "memory_search" "Use memory search tool"]
   ["tool.memory_session.use" "tool" "memory_session" "Use memory session tool"]
   ["tool.websearch.use" "tool" "websearch" "Use websearch tool"]
   ["tool.graph_query.use" "tool" "graph_query" "Use graph query tool"]
   ["org.translations.read" "org_translations" "read" "Read translation segments"]
   ["org.translations.review" "org_translations" "review" "Review and label translations"]
   ["org.translations.export" "org_translations" "export" "Export translation training data"]
   ["org.translations.manage" "org_translations" "manage" "Manage translation pipeline config"]
   ["org.proxx.observability.read" "org_proxx_observability" "read" "Read Proxx analytics and request logs"]])

(def ^:private TOOL-DEFINITIONS
  [["read" "Read" "Read files and retrieved context" "low"]
   ["write" "Write" "Create new markdown drafts and artifacts" "medium"]
   ["edit" "Edit" "Revise existing documents and drafts" "medium"]
   ["bash" "Shell" "Run controlled shell commands" "high"]
   ["canvas" "Canvas" "Open long-form markdown drafting canvas" "low"]
   ["email.send" "Email" "Send drafts through configured email account" "medium"]
   ["discord.publish" "Discord Publish" "Publish updates to Discord" "medium"]
   ["discord.send" "Discord Send" "Send Discord messages and threaded replies" "medium"]
   ["discord.read" "Discord Read" "Read messages from Discord channels" "low"]
   ["discord.channel.messages" "Discord Channel Messages" "Fetch messages from a Discord channel with before/after/around cursors" "low"]
   ["discord.channel.scroll" "Discord Channel Scroll" "Scroll older messages in a Discord channel" "low"]
   ["discord.dm.messages" "Discord DM Messages" "Fetch messages from a Discord DM channel" "low"]
   ["discord.search" "Discord Search" "Search messages in Discord channels" "low"]
   ["discord.guilds" "Discord Guilds" "List Discord servers the bot is in" "low"]
   ["discord.channels" "Discord Channels" "List channels in a Discord server" "low"]
   ["discord.list.servers" "Discord List Servers" "List all Discord servers the bot can access" "low"]
   ["discord.list.channels" "Discord List Channels" "List channels across one or all Discord servers" "low"]
   ["event_agents.status" "Event Agent Status" "Inspect scheduled event-agent runtime state and configuration" "low"]
   ["event_agents.dispatch" "Event Agent Dispatch" "Dispatch a structured event into the event-agent runtime" "medium"]
   ["event_agents.run_job" "Event Agent Run Job" "Trigger a configured event-agent job immediately" "medium"]
   ["event_agents.upsert_job" "Event Agent Upsert Job" "Create or update a scheduled event-agent job" "high"]
   ["schedule_event_agent" "Schedule Event Agent" "Create or update a scheduled event-agent job with prompts, tools, triggers, and source config" "high"]
   ["bluesky.publish" "Bluesky" "Publish updates to Bluesky" "medium"]
   ["semantic_query" "Semantic Query" "Query semantic context in the active corpus" "low"]
   ["memory_search" "Memory Search" "Search prior Knoxx sessions in OpenPlanner" "low"]
   ["memory_session" "Memory Session" "Load a specific Knoxx session from OpenPlanner" "low"]
   ["websearch" "Web Search" "Search the live web through Proxx websearch" "low"]
   ["graph_query" "Graph Query" "Query the canonical knowledge graph" "low"]])

(def ^:private ALL-PERMISSION-CODES (map first PERMISSIONS))

(def ^:private ALL-TOOL-IDS (map first TOOL-DEFINITIONS))

(def ^:private PLATFORM-ROLE-SEEDS
  [{:slug "system_admin"
    :name "System Admin"
    :permissions ALL-PERMISSION-CODES
    :tool-policies (mapv (fn [tool-id] {:toolId tool-id :effect "allow"}) ALL-TOOL-IDS)}])

(def ^:private ORG-ROLE-SEEDS
  [{:slug "org_admin"
    :name "Org Admin"
    :permissions ["org.settings.read" "org.settings.update"
                  "org.members.read" "org.members.create" "org.members.update" "org.members.delete"
                  "org.users.invite" "org.users.create" "org.users.read" "org.users.update" "org.users.disable"
                  "org.roles.read" "org.roles.create" "org.roles.update" "org.roles.delete"
                  "org.tool_policy.read" "org.tool_policy.update"
                  "org.user_policy.read" "org.user_policy.update"
                  "org.datalakes.read" "org.datalakes.create" "org.datalakes.update" "org.datalakes.delete"
                  "datalake.read" "datalake.query" "datalake.write" "datalake.ingest" "datalake.admin"
                  "agent.chat.use" "agent.memory.read" "agent.memory.cross_session"
                  "agent.runs.read_org" "agent.controls.steer" "agent.controls.follow_up"
                  "tool.read.use" "tool.write.use" "tool.edit.use" "tool.bash.use"
                  "tool.email.send" "tool.discord.publish" "tool.discord.send" "tool.discord.read"
                  "tool.discord.channel.messages" "tool.discord.channel.scroll" "tool.discord.dm.messages"
                  "tool.discord.search" "tool.discord.guilds" "tool.discord.channels"
                  "tool.discord.list.servers" "tool.discord.list.channels"
                  "tool.event_agents.status" "tool.event_agents.dispatch"
                  "tool.event_agents.run_job" "tool.event_agents.upsert_job" "tool.schedule_event_agent"
                  "tool.bluesky.publish" "tool.semantic_query.use"
                  "tool.memory_search.use" "tool.memory_session.use" "tool.websearch.use" "tool.graph_query.use"
                  "org.translations.read" "org.translations.review" "org.translations.export" "org.translations.manage"
                  "org.proxx.observability.read"]
    :tool-policies (mapv (fn [tool-id] {:toolId tool-id :effect "allow"}) ALL-TOOL-IDS)}
   {:slug "knowledge_worker"
    :name "Knowledge Worker"
    :permissions ["org.datalakes.read" "datalake.read" "datalake.query"
                  "agent.chat.use" "agent.memory.read"
                  "agent.runs.read_own" "agent.controls.steer" "agent.controls.follow_up"
                  "tool.read.use" "tool.semantic_query.use"
                  "tool.memory_search.use" "tool.memory_session.use"]
    :tool-policies [{:toolId "read" :effect "allow"}
                    {:toolId "canvas" :effect "allow"}
                    {:toolId "semantic_query" :effect "allow"}
                    {:toolId "memory_search" :effect "allow"}
                    {:toolId "memory_session" :effect "allow"}]}
   {:slug "data_analyst"
    :name "Data Analyst"
    :permissions ["org.datalakes.read" "datalake.read" "datalake.query"
                  "agent.chat.use" "agent.memory.read" "agent.memory.cross_session"
                  "agent.runs.read_own"
                  "tool.read.use" "tool.write.use" "tool.edit.use"
                  "tool.semantic_query.use" "tool.memory_search.use" "tool.memory_session.use"]
    :tool-policies [{:toolId "read" :effect "allow"}
                    {:toolId "write" :effect "allow"}
                    {:toolId "edit" :effect "allow"}
                    {:toolId "canvas" :effect "allow"}
                    {:toolId "semantic_query" :effect "allow"}
                    {:toolId "memory_search" :effect "allow"}
                    {:toolId "memory_session" :effect "allow"}]}
   {:slug "developer"
    :name "Developer"
    :permissions ["org.datalakes.read" "datalake.read" "datalake.query" "datalake.write" "datalake.ingest"
                  "agent.chat.use" "agent.memory.read" "agent.memory.cross_session"
                  "agent.runs.read_own"
                  "tool.read.use" "tool.write.use" "tool.edit.use" "tool.bash.use"
                  "tool.semantic_query.use" "tool.memory_search.use" "tool.memory_session.use"]
    :tool-policies [{:toolId "read" :effect "allow"}
                    {:toolId "write" :effect "allow"}
                    {:toolId "edit" :effect "allow"}
                    {:toolId "bash" :effect "allow"}
                    {:toolId "canvas" :effect "allow"}
                    {:toolId "semantic_query" :effect "allow"}
                    {:toolId "memory_search" :effect "allow"}
                    {:toolId "memory_session" :effect "allow"}]}
   {:slug "translator"
    :name "Translator"
    :permissions ["org.datalakes.read" "datalake.read"
                  "agent.chat.use"
                  "org.translations.read" "org.translations.review"]
    :tool-policies [{:toolId "read" :effect "allow"}
                    {:toolId "canvas" :effect "allow"}
                    {:toolId "semantic_query" :effect "allow"}]}])

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

(defn- normalize-tool-policy
  [policy]
  (cond
    (string? policy)
    {:toolId policy :effect "allow" :constraints {}}

    :else
    (let [tool-id (or (:toolId policy) (:tool_id policy) (:id policy))]
      (when-not tool-id
        (throw (js/Error. "toolId is required for tool policy")))
      {:toolId (str tool-id)
       :effect (if (= (:effect policy) "deny") "deny" "allow")
       :constraints (or (:constraints policy) (:constraints_json policy) {})})))

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

    CREATE TABLE IF NOT EXISTS permissions (
      id BIGSERIAL PRIMARY KEY,
      code TEXT NOT NULL UNIQUE,
      resource_kind TEXT NOT NULL,
      action TEXT NOT NULL,
      description TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS role_permissions (
      role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
      permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
      effect TEXT NOT NULL DEFAULT 'allow',
      PRIMARY KEY (role_id, permission_id),
      CHECK (effect IN ('allow', 'deny'))
    );

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
  " nil))

(defn- insert-permission-seeds!
  [pool]
  (-> (js/Promise.all
       (into-array
        (for [[code resource-kind action description] PERMISSIONS]
          (query! pool
                  "INSERT INTO permissions (code, resource_kind, action, description)
                   VALUES ($1, $2, $3, $4)
                   ON CONFLICT (code) DO UPDATE
                   SET resource_kind = EXCLUDED.resource_kind,
                       action = EXCLUDED.action,
                       description = EXCLUDED.description"
                  [code resource-kind action description]))))
      (.then (fn [_] nil))))

(defn- insert-tool-seeds!
  [pool]
  (-> (js/Promise.all
       (into-array
        (for [[id label description risk-level] TOOL-DEFINITIONS]
          (query! pool
                  "INSERT INTO tool_definitions (id, label, description, risk_level)
                   VALUES ($1, $2, $3, $4)
                   ON CONFLICT (id) DO UPDATE
                   SET label = EXCLUDED.label,
                       description = EXCLUDED.description,
                       risk_level = EXCLUDED.risk_level"
                  [id label description risk-level]))))
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
                 (.then (query! pool "UPDATE orgs SET is_primary = CASE WHEN slug = $1 THEN TRUE ELSE FALSE END" [slug])
                        (fn [_] org)))))))

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
        (.then
         (fn [_]
           (if (empty? codes)
             nil
             (-> (query! pool
                         "SELECT id, code FROM permissions WHERE code = ANY($1::text[])"
                         [(into-array codes)])
                 (.then
                  (fn [result]
                    (let [rows (aget result "rows")
                          found (set (for [i (range (.-length rows))]
                                       (aget (aget rows i) "code")))
                          missing (filter #(not (contains? found %)) codes)]
                      (when (seq missing)
                        (throw (js/Error.
                                (str "Unknown permission codes: "
                                     (str/join ", " missing)))))
                      (js/Promise.all
                       (into-array
                        (for [i (range (.-length rows))]
                          (let [row (aget rows i)
                                perm-id (aget row "id")]
                            (query! pool
                                    "INSERT INTO role_permissions (role_id, permission_id, effect) VALUES ($1, $2, 'allow') ON CONFLICT (role_id, permission_id) DO UPDATE SET effect = EXCLUDED.effect"
                                    [role-id perm-id])))))))))))))))

(defn- set-role-tool-policies!
  [pool role-id tool-policies]
  (let [normalized (mapv normalize-tool-policy tool-policies)]
    (-> (query! pool "DELETE FROM role_tool_policies WHERE role_id = $1" [role-id])
        (.then
         (fn [_]
           (js/Promise.all
            (into-array
             (for [policy normalized]
               (query! pool
                       "INSERT INTO role_tool_policies (role_id, tool_id, effect, constraints_json) VALUES ($1, $2, $3, $4::jsonb) ON CONFLICT (role_id, tool_id) DO UPDATE SET effect = EXCLUDED.effect, constraints_json = EXCLUDED.constraints_json"
                       [role-id (:toolId policy) (:effect policy)
                        (js/JSON.stringify (clj->js (:constraints policy)))])))))))))

(defn- set-membership-tool-policies!
  [pool membership-id tool-policies]
  (let [normalized (mapv normalize-tool-policy tool-policies)]
    (-> (query! pool "DELETE FROM user_tool_policies WHERE membership_id = $1" [membership-id])
        (.then
         (fn [_]
           (js/Promise.all
            (into-array
             (for [policy normalized]
               (query! pool
                       "INSERT INTO user_tool_policies (membership_id, tool_id, effect, constraints_json) VALUES ($1, $2, $3, $4::jsonb) ON CONFLICT (membership_id, tool_id) DO UPDATE SET effect = EXCLUDED.effect, constraints_json = EXCLUDED.constraints_json"
                       [membership-id (:toolId policy) (:effect policy)
                        (js/JSON.stringify (clj->js (:constraints policy)))])))))))))

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
                 (-> (set-role-permissions! pool (aget role "id") (:permissions seed))
                     (.then (fn [_]
                              (set-role-tool-policies! pool (aget role "id")
                                                       (:tool-policies seed)))))))))))
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
                 (-> (set-role-permissions! pool (aget role "id") (:permissions seed))
                     (.then (fn [_]
                              (set-role-tool-policies! pool (aget role "id")
                                                       (:tool-policies seed)))))))))))
      (.then (fn [_] nil))))

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
                  normalized (slugify raw-slug)]
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
           [(query! pool
                    "SELECT rp.role_id, p.code FROM role_permissions rp JOIN permissions p ON p.id = rp.permission_id WHERE rp.role_id = ANY($1::uuid[]) ORDER BY p.code ASC"
                    [(into-array role-ids)])
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
                          (mergeToolPolicies
                           (mapcat :toolPolicies detailed-roles)
                           (:toolPolicies membership))
                          role-slugs (sort-by #(- (rolePriority %))
                                              (map :slug detailed-roles))]
                      (clj->js
                       {:user {:id (aget membership-row "user_id")
                               :email (aget membership-row "email")
                               :displayName (aget membership-row "display_name")
                               :status (aget membership-row "user_status")}
                        :org {:id (aget membership-row "org_id")
                              :slug (aget membership-row "org_slug")
                              :name (aget membership-row "org_name")
                              :status (aget membership-row "org_status")
                              :isPrimary (aget membership-row "is_primary")
                              :kind (aget membership-row "org_kind")}
                        :membership {:id (:id membership)
                                     :status (:status membership)
                                     :isDefault (:isDefault membership)
                                     :createdAt (:createdAt membership)
                                     :updatedAt (:updatedAt membership)}
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
                         (when system-admin
                           (query! pool
                                   "INSERT INTO membership_roles (membership_id, role_id) VALUES ($1, $2) ON CONFLICT (membership_id, role_id) DO NOTHING"
                                   [(aget membership "id") (aget system-admin "id")]))
                         {:user user :membership membership})))))))))))

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

(defn- ->js-permission [row]
  #js {:id (aget row "id")
       :code (aget row "code")
       :resourceKind (aget row "resource_kind")
       :action (aget row "action")
       :description (aget row "description")})

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
  [pool]
  (-> (query! pool "SELECT id, code, resource_kind, action, description FROM permissions ORDER BY code ASC" [])
      (.then (fn [r]
               #js {:permissions (into-array (map ->js-permission (aget r "rows")))}))))

(defn- factory-list-tools
  [pool]
  (-> (query! pool "SELECT id, label, description, risk_level FROM tool_definitions ORDER BY id ASC" [])
      (.then (fn [r]
               #js {:tools (into-array (map ->js-tool (aget r "rows")))}))))

(defn- factory-get-bootstrap-context
  [pool primary-org bootstrap]
  (let [uid (aget ^js bootstrap "user" "id")
        mid (aget ^js bootstrap "membership" "id")]
    #js {:primaryOrg (->js-org primary-org)
         :bootstrapUser #js {:id uid
                             :email (aget ^js bootstrap "user" "email")
                             :displayName (aget ^js bootstrap "user" "display_name")
                             :membershipId mid}}))

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
                                  (aget payload "display_name") email)))]
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
                                              :role-slugs (or (aget payload "roleSlugs") #js ["knowledge_worker"])
                                              :replace true})))
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
                  (append-audit! pool {:actor-user-id uid
                                       :actor-membership-id mid
                                       :org-id (aget ms "org_id")
                                       :action "membership.roles.update"
                                       :resource-kind "membership"
                                       :resource-id membership-id})))
               (.then (fn [_] #js {:membership nil}))))))))

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
;; Factory
;; ---------------------------------------------------------------------------

(defn create-policy-db
  [options]
  (let [conn-str (or (aget options "connectionString")
                     (:connectionString options)
                     "")]
    (when-not (str/blank? conn-str)
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
                      (.then
                       (fn [bootstrap]
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
                                   (factory-create-data-lake pool uid mid payload))})))))))
               (.catch reject))))))))

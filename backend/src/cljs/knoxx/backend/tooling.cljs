(ns knoxx.backend.tooling
  (:require [clojure.string :as str]
            [knoxx.backend.authz :as authz]
            [knoxx.backend.contracts.resolve :as contract-resolve]
            [knoxx.backend.contracts.roles :as roles]
            [knoxx.backend.http :as backend-http]
            [knoxx.backend.mcp-bridge :as mcp]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.state :as state]
            [knoxx.backend.tools.registry :as tool-registry]))

(defn- current-config
  []
  (or @state/config*
      (runtime-config/cfg)))

(defn normalize-role
  [role]
  (roles/normalize-role (current-config) role))

(defn email-enabled?
  [config]
  (and (not (str/blank? (:gmail-app-email config)))
       (not (str/blank? (:gmail-app-password config)))))

(defn discord-enabled?
  [config]
  (not (str/blank? (:discord-bot-token config))))

(defn auth-tool-ids
  [auth-context]
  (into #{}
        (comp (filter #(= "allow" (:effect %)))
              (map :toolId))
        (:toolPolicies auth-context)))

;; ---------------------------------------------------------------------------
;; Contract resolution (delegated)
;; ---------------------------------------------------------------------------

(defn resolve-actor
  [config actor-id]
  (contract-resolve/resolve-actor config actor-id))

(defn actor-catalog
  [config]
  (contract-resolve/actor-catalog config))

(defn default-actor-id
  [config]
  (contract-resolve/default-actor-id config))

(defn resolve-agent-contract
  ([config contract-id]
   (contract-resolve/resolve-agent-contract config contract-id nil))
  ([config contract-id actor-id]
   (contract-resolve/resolve-agent-contract config contract-id actor-id)))

(defn agent-contract-catalog
  ([config]
   (agent-contract-catalog config nil))
  ([config actor-id]
   (contract-resolve/agent-contract-catalog config actor-id)))

(defn default-agent-contract-id
  ([config]
   (default-agent-contract-id config nil))
  ([config actor-id]
   (contract-resolve/default-agent-contract-id config actor-id)))

(defn effective-agent-contract
  ([config requested-contract-id]
   (effective-agent-contract config requested-contract-id nil))
  ([config requested-contract-id actor-id]
   (contract-resolve/effective-agent-contract config requested-contract-id actor-id)))

;; ---------------------------------------------------------------------------
;; Enforcement + tool catalog
;; ---------------------------------------------------------------------------

(defn ensure-role-can-use!
  ([role tool-id]
   (ensure-role-can-use! nil role tool-id nil nil))
  ([auth-context role tool-id]
   (ensure-role-can-use! auth-context role tool-id nil nil))
  ([auth-context role tool-id agent-contract-id]
   (ensure-role-can-use! auth-context role tool-id agent-contract-id nil))
  ([auth-context role tool-id agent-contract-id actor-id]
   (let [config (current-config)
         contract-spec (effective-agent-contract config agent-contract-id actor-id)
         normalized (roles/normalize-role config (or (:role contract-spec) role))
         contract-tool-ids (some-> contract-spec :tool-ids set)
         role-tool-ids (set (roles/role-tool-ids config normalized))
         allowed (cond
                   contract-tool-ids contract-tool-ids
                   auth-context (auth-tool-ids auth-context)
                   :else role-tool-ids)]
     (when-not (contains? allowed tool-id)
       (if auth-context
         (throw (backend-http/http-error 403 "tool_denied" (str "Current Knoxx policy does not allow tool '" tool-id "'")))
         (throw (js/Error. (str "Role '" normalized "' cannot use tool '" tool-id "'")))))
     normalized)))

(defn- resolve-tool-context
  [config role auth-context agent-contract-id actor-id]
  (let [contract-spec (effective-agent-contract config agent-contract-id actor-id)
        actor-spec (or (when actor-id (resolve-actor config actor-id))
                       (when-let [resolved-actor-id (:actor-id contract-spec)]
                         (resolve-actor config resolved-actor-id)))
        normalized (roles/normalize-role config (or (:role contract-spec) role))
        role-tool-ids (set (roles/role-tool-ids config normalized))
        allowed-tool-ids (cond
                           contract-spec (set (:tool-ids contract-spec))
                           auth-context (auth-tool-ids auth-context)
                           :else role-tool-ids)]
    {:contract-spec contract-spec
     :actor-spec actor-spec
     :normalized-role normalized
     :allowed-tool-ids allowed-tool-ids}))

(defn allowed-tool-id-set
  ([config role]
   (allowed-tool-id-set config role nil nil nil))
  ([config role auth-context]
   (allowed-tool-id-set config role auth-context nil nil))
  ([config role auth-context agent-contract-id]
   (allowed-tool-id-set config role auth-context agent-contract-id nil))
  ([config role auth-context agent-contract-id actor-id]
   (:allowed-tool-ids (resolve-tool-context config role auth-context agent-contract-id actor-id))))

(defn tool-catalog
  ([config role]
   (tool-catalog config role nil nil nil))
  ([config role auth-context]
   (tool-catalog config role auth-context nil nil))
  ([config role auth-context agent-contract-id]
   (tool-catalog config role auth-context agent-contract-id nil))
  ([config role auth-context agent-contract-id actor-id]
   (let [email? (email-enabled? config)
         discord? (discord-enabled? config)
         live-mcp-tool-ids (if (and (:mcp-enabled config) (mcp/available?) (mcp/enabled?))
                             (into #{} (map :id) (mcp/catalog))
                             #{})
         {:keys [contract-spec actor-spec normalized-role allowed-tool-ids]}
         (resolve-tool-context config role auth-context agent-contract-id actor-id)
         base-tools (->> allowed-tool-ids
                         sort
                         (mapv (fn [tool-id]
                                 (let [{:keys [label description]} (tool-registry/get-tool tool-id)]
                                   {:id tool-id
                                    :label label
                                    :description description
                                    :enabled (cond
                                               (= tool-id "email.send") email?
                                               (str/starts-with? tool-id "discord.") discord?
                                               (str/starts-with? tool-id "mcp.") (contains? live-mcp-tool-ids tool-id)
                                               :else true)}))))
         tools (cond-> base-tools
                 (contains? allowed-tool-ids "semantic_query")
                 (conj {:id "graph_query"
                        :label "Graph Query"
                        :description "Query the canonical knowledge graph across devel/web/bluesky/knoxx-session lakes"
                        :enabled true}))]
     {:role (if auth-context
              (or (:role contract-spec) (authz/primary-context-role auth-context))
              normalized-role)
      :actor_id (:id actor-spec)
      :agent_id (:id contract-spec)
      :agent_label (:id contract-spec)
      :agent_trigger_kind (:trigger-kind contract-spec)
      :role_slugs (vec (or (:role-slugs contract-spec) []))
      :capability_ids (vec (or (:capability-ids contract-spec) []))
      :system_prompt (when (or (nil? auth-context)
                               (authz/system-admin? auth-context))
                       (:system-prompt contract-spec))
      :actor_system_prompt (when (or (nil? auth-context)
                                     (authz/system-admin? auth-context))
                             (:actor-system-prompt contract-spec))
      :agent_system_prompt (when (or (nil? auth-context)
                                     (authz/system-admin? auth-context))
                             (:agent-system-prompt contract-spec))
      :task_prompt (when (or (nil? auth-context)
                             (authz/system-admin? auth-context))
                     (:task-prompt contract-spec))
      :email_enabled email?
      :tools tools})))

(defn create-runtime-tools
  ([runtime config auth-context]
   (create-runtime-tools runtime config auth-context nil nil nil))
  ([runtime config auth-context role agent-contract-id actor-id]
   (let [sdk (aget runtime "sdk")
         cwd (:workspace-root config)
         read-tool (aget sdk "createReadTool")
         write-tool (aget sdk "createWriteTool")
         edit-tool (aget sdk "createEditTool")
         bash-tool (aget sdk "createBashTool")
         allowed-tool-ids (allowed-tool-id-set config role auth-context agent-contract-id actor-id)
         allowed? (fn [tool-id]
                    (contains? allowed-tool-ids tool-id))]
     (vec
      (remove nil?
              [(when (and read-tool (allowed? "read")) (read-tool cwd))
               (when (and write-tool (allowed? "write")) (write-tool cwd))
               (when (and edit-tool (allowed? "edit")) (edit-tool cwd))
               (when (and bash-tool (allowed? "bash")) (bash-tool cwd))])))))

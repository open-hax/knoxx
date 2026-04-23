(ns knoxx.backend.tooling
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.authz :as authz]
            [knoxx.backend.http :as backend-http]
            [knoxx.backend.mcp-bridge :as mcp]
            [knoxx.backend.runtime.actor-scope :as actor-scope]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.contract-loader :as contract-loader]
            [knoxx.backend.runtime.roles :as roles]
            [knoxx.backend.runtime.state :as state]
            [knoxx.backend.tools.registry :as tool-registry]
            ["node:fs" :as fs]
            ["node:path" :as path]))

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

(defn- keywordish->role-slug
  [value]
  (let [raw (cond
              (keyword? value) (name value)
              (string? value) value
              (nil? value) nil
              :else (str value))]
    (some-> raw str str/trim not-empty)))

(defn- keywordish->capability-ref
  [value]
  (cond
    (keyword? value) (str (namespace value) "/" (name value))
    (string? value) (some-> value str str/trim not-empty)
    (nil? value) nil
    :else (some-> value str str/trim not-empty)))

(defn- read-edn-sync
  [file-path]
  (try
    (some-> (.readFileSync fs file-path "utf8") str reader/read-string)
    (catch :default _
      nil)))

(defn resolve-actor
  [config actor-id]
  (when-let [id (some-> actor-id str str/trim not-empty)]
    (let [file-path (contract-loader/actor-file-path config id)
          actor (read-edn-sync file-path)]
      (when actor
        {:id id
         :actor actor
         :kind (some-> (:actor/kind actor) name)
         :org (some-> (:actor/org actor) str str/trim not-empty)
         :default-agent (some-> (:actor/default-agent actor) str str/trim not-empty)
         :role-slugs (->> (or (:actor/roles actor) [])
                          (map keywordish->role-slug)
                          (remove nil?)
                          distinct
                          vec)
         :capability-ids (->> (or (:actor/capabilities actor) [])
                              (map keywordish->capability-ref)
                              (remove nil?)
                              distinct
                              vec)
         :system-prompt (some-> (get-in actor [:prompts :system]) str str/trim not-empty)}))))

(defn- combine-system-prompts
  [& parts]
  (let [segments (->> parts
                      (map (fn [part]
                             (some-> part str str/trim not-empty)))
                      (remove nil?))]
    (when (seq segments)
      (str/join "\n\n" segments))))

(defn- contract-edn-file?
  [name]
  (and (string? name)
       (str/ends-with? name ".edn")
       (not (str/starts-with? name "."))))

(defn actor-catalog
  [config]
  (->> (contract-loader/list-contract-ids-sync config "actors")
       (map (fn [actor-id]
              (resolve-actor config actor-id)))
       (remove nil?)
       (sort-by :id)
       vec))

(defn default-actor-id
  [config]
  (let [configured (some-> (:knoxx-default-actor-id config) str str/trim not-empty)
        configured-actor (when configured
                           (resolve-actor config configured))]
    (cond
      configured-actor configured
      :else (or (some-> (actor-catalog config) first :id)
                "chat_primary"))))

(defn- collect-role-tool-ids
  [config role-slugs]
  (->> role-slugs
       (map keywordish->role-slug)
       (remove nil?)
       distinct
       (mapcat (fn [role-slug]
                 (roles/role-tool-ids config role-slug)))
       distinct
       sort
       vec))

(defn- collect-capability-tool-ids
  [config capability-ids]
  (->> capability-ids
       (map keywordish->capability-ref)
       (remove nil?)
       distinct
       (mapcat (fn [cap-id]
                 (roles/capability-tool-ids config cap-id)))
       distinct
       sort
       vec))

(defn- legacy-explicit-tool-ids
  [contract]
  (->> (or (get-in contract [:data :tools]) [])
       (map tool-registry/normalize-tool-id)
       (remove str/blank?)
       distinct
       sort
       vec))

(defn- contract-actor-capability-claims
  [contract]
  (->> (concat (or (:actor/capabilities contract) [])
               (or (get-in contract [:actor :capabilities]) []))
       distinct
       vec))

(defn resolve-agent-contract
  ([config contract-id]
   (resolve-agent-contract config contract-id nil))
  ([config contract-id actor-id]
   (when-let [id (some-> contract-id str str/trim not-empty)]
     (let [file-path (contract-loader/contract-file-path config "agents" id)
           contract0 (read-edn-sync file-path)
           contract (some-> contract0 actor-scope/normalize-agent-contract)]
       (when contract
         (let [enabled? (not (false? (:enabled contract)))
               contract-actors (actor-scope/normalized-contract-actors contract)
               requested-actor-id (some-> actor-id str str/trim not-empty)
               allowed-for-request? (or (nil? requested-actor-id)
                                        (actor-scope/actor-allowed? contract-actors requested-actor-id))
               effective-actor-id (actor-scope/effective-actor-id contract-actors requested-actor-id (default-actor-id config))]
           (when allowed-for-request?
             (let [actor-spec (or (when effective-actor-id (resolve-actor config effective-actor-id))
                                  (when-let [default-id (default-actor-id config)]
                                    (resolve-actor config default-id)))
                   contract-role-slugs (->> (actor-scope/agent-role-claims contract)
                                            (map keywordish->role-slug)
                                            (remove nil?)
                                            distinct
                                            vec)
                   actor-role-slugs (vec (or (:role-slugs actor-spec) []))
                   role-slugs (vec (distinct (concat actor-role-slugs contract-role-slugs)))
                   actor-capability-ids (vec (or (:capability-ids actor-spec) []))
                   contract-capability-ids (->> (contract-actor-capability-claims contract)
                                                (map keywordish->capability-ref)
                                                (remove nil?)
                                                distinct
                                                vec)
                   capability-ids (vec (distinct (concat actor-capability-ids contract-capability-ids)))
                   role-tool-ids (collect-role-tool-ids config role-slugs)
                   capability-tool-ids (collect-capability-tool-ids config capability-ids)
                   explicit-tool-ids (legacy-explicit-tool-ids contract)
                   tool-ids (->> (concat role-tool-ids capability-tool-ids explicit-tool-ids)
                                 distinct
                                 sort
                                 vec)
                   tool-policies (mapv (fn [tool-id]
                                         {:toolId tool-id :effect "allow"})
                                       tool-ids)]
               {:id id
                :enabled enabled?
                :contract contract
                :contract-actors contract-actors
                :contract-actor-ids (actor-scope/actor-claims->wire contract-actors)
                :actor-id (:id actor-spec)
                :actor-kind (:kind actor-spec)
                :actor-role-slugs actor-role-slugs
                :role-slugs role-slugs
                :capability-ids capability-ids
                :role (or (first contract-role-slugs)
                          (first actor-role-slugs)
                          (:knoxx-default-role config))
                :model (some-> (get-in contract [:agent :model]) str str/trim not-empty)
                :thinking-level (some-> (get-in contract [:agent :thinking]) keywordish->role-slug)
                :actor-system-prompt (:system-prompt actor-spec)
                :agent-system-prompt (some-> (get-in contract [:prompts :system]) str str/trim not-empty)
                :system-prompt (combine-system-prompts
                                (:system-prompt actor-spec)
                                (some-> (get-in contract [:prompts :system]) str str/trim not-empty))
                :task-prompt (some-> (get-in contract [:prompts :task]) str str/trim not-empty)
                :trigger-kind (some-> (:trigger-kind contract) keywordish->role-slug)
                :tool-ids tool-ids
                :tool-policies tool-policies}))))))))
(defn- manual-agent-contract?
  [entry]
  (= "manual" (some-> (:trigger-kind entry) str str/trim str/lower-case)))

(defn agent-contract-catalog
  ([config]
   (agent-contract-catalog config nil))
  ([config actor-id]
   (let [ids (contract-loader/list-contract-ids-sync config "agents")
         wanted-actor-id (some-> actor-id str str/trim not-empty)]
     (->> ids
          (map (fn [id]
                 (resolve-agent-contract config id wanted-actor-id)))
          (remove nil?)
          (filter :enabled)
          (filter manual-agent-contract?)
          (sort-by :id)
          vec))))

(defn default-agent-contract-id
  ([config]
   (default-agent-contract-id config nil))
  ([config actor-id]
   (let [actor-spec (or (when actor-id (resolve-actor config actor-id))
                        (when-let [default-id (default-actor-id config)]
                          (resolve-actor config default-id)))
         actor-default (some-> (:default-agent actor-spec) str str/trim not-empty)
         configured (some-> (:knoxx-default-agent-contract config) str str/trim not-empty)
         configured-manual (when configured (resolve-agent-contract config configured (:id actor-spec)))
         actor-default-manual (when actor-default (resolve-agent-contract config actor-default (:id actor-spec)))
         actor-catalog (agent-contract-catalog config (:id actor-spec))]
     (cond
       (and actor-default-manual (manual-agent-contract? actor-default-manual)) actor-default
       (and configured-manual
            (manual-agent-contract? configured-manual)
            (or (nil? actor-spec) (= (:id actor-spec) (:actor-id configured-manual)))) configured
       :else (some-> actor-catalog first :id)))))

(defn effective-agent-contract
  ([config requested-contract-id]
   (effective-agent-contract config requested-contract-id nil))
  ([config requested-contract-id actor-id]
   (or (when requested-contract-id (resolve-agent-contract config requested-contract-id actor-id))
       (when-let [actor-default-id (default-agent-contract-id config actor-id)]
         (resolve-agent-contract config actor-default-id actor-id))
       (when-let [global-default-id (default-agent-contract-id config nil)]
         (resolve-agent-contract config global-default-id actor-id)))))

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

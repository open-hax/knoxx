(ns knoxx.backend.tooling
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.authz :as authz]
            [knoxx.backend.http :as backend-http]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.contract-loader :as contract-loader]
            [knoxx.backend.runtime.roles :as roles]
            [knoxx.backend.runtime.state :as state]
            [knoxx.backend.tools.registry :as tool-registry]
            ["node:fs" :as fs]))

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

(defn- keywordish->slug
  [value]
  (let [raw (cond
              (keyword? value) (name value)
              (string? value) value
              (nil? value) nil
              :else (str value))]
    (some-> raw str str/trim not-empty)))

(defn- normalize-contract-tool-ids
  [contract role-tool-ids]
  (let [explicit-tool-ids (->> (or (get-in contract [:data :tools]) [])
                               (map tool-registry/normalize-tool-id)
                               (remove str/blank?)
                               set)]
    (cond
      (and (seq role-tool-ids) (seq explicit-tool-ids))
      (vec (sort (set/intersection (set role-tool-ids) explicit-tool-ids)))

      (seq explicit-tool-ids)
      (vec (sort explicit-tool-ids))

      :else
      (vec (sort role-tool-ids)))))

(defn resolve-agent-contract
  [config contract-id]
  (when-let [id (some-> contract-id str str/trim not-empty)]
    (try
      (let [file-path (contract-loader/contract-file-path config "agents" id)
            text (.readFileSync fs file-path "utf8")
            contract (reader/read-string (str text))
            enabled? (not (false? (:enabled contract)))
            role (some-> (get-in contract [:agent :role]) keywordish->slug)
            role-tool-ids (if role
                            (roles/role-tool-ids config role)
                            [])
            tool-ids (normalize-contract-tool-ids contract role-tool-ids)
            tool-policies (mapv (fn [tool-id]
                                  {:toolId tool-id :effect "allow"})
                                tool-ids)]
        {:id id
         :enabled enabled?
         :contract contract
         :role (or role (:knoxx-default-role config))
         :model (some-> (get-in contract [:agent :model]) str str/trim not-empty)
         :thinking-level (some-> (get-in contract [:agent :thinking]) keywordish->slug)
         :system-prompt (some-> (or (get-in contract [:prompts :system])
                                    (get-in contract [:prompts :task]))
                                str
                                not-empty)
         :trigger-kind (some-> (:trigger-kind contract) keywordish->slug)
         :actor-id (some-> (:actor/id contract) str str/trim not-empty)
         :tool-ids tool-ids
         :tool-policies tool-policies})
      (catch :default _
        nil))))

(defn- manual-agent-contract?
  [entry]
  (= "manual" (some-> (:trigger-kind entry) str str/trim str/lower-case)))

(defn agent-contract-catalog
  [config]
  (let [agent-ids (try
                    (->> (.readdirSync fs (.join (contract-loader/contracts-dir-path config) "agents"))
                         (filter (fn [name]
                                   (and (string? name)
                                        (str/ends-with? name ".edn"))))
                         (map (fn [name] (subs name 0 (- (count name) 4))))
                         sort
                         vec)
                    (catch :default _ []))]
    (->> agent-ids
         (keep (partial resolve-agent-contract config))
         (filter :enabled)
         (filter manual-agent-contract?)
         (sort-by :id)
         vec)))

(defn default-agent-contract-id
  [config]
  (let [configured (some-> (:knoxx-default-agent-contract config) str str/trim not-empty)
        configured-manual (some-> configured (resolve-agent-contract config))]
    (cond
      (and configured-manual (manual-agent-contract? configured-manual)) configured
      :else (some-> (agent-contract-catalog config) first :id))))

(defn effective-agent-contract
  [config requested-contract-id]
  (or (some-> requested-contract-id (resolve-agent-contract config))
      (some-> (default-agent-contract-id config) (resolve-agent-contract config))))

(defn ensure-role-can-use!
  ([role tool-id]
   (ensure-role-can-use! nil role tool-id nil))
  ([auth-context role tool-id]
   (ensure-role-can-use! auth-context role tool-id nil))
  ([auth-context role tool-id agent-contract-id]
   (let [config (current-config)
         contract-spec (effective-agent-contract config agent-contract-id)
         normalized (roles/normalize-role config (or (:role contract-spec) role))
         contract-tool-ids (some-> contract-spec :tool-ids set)
         role-tool-ids (set (roles/role-tool-ids config normalized))
         allowed (cond
                   auth-context (cond-> (auth-tool-ids auth-context)
                                  contract-tool-ids (set/intersection contract-tool-ids))
                   contract-tool-ids contract-tool-ids
                   :else role-tool-ids)]
     (when-not (contains? allowed tool-id)
       (if auth-context
         (throw (backend-http/http-error 403 "tool_denied" (str "Current Knoxx policy does not allow tool '" tool-id "'")))
         (throw (js/Error. (str "Role '" normalized "' cannot use tool '" tool-id "'")))))
     normalized)))

(defn tool-catalog
  ([config role]
   (tool-catalog config role nil nil))
  ([config role auth-context]
   (tool-catalog config role auth-context nil))
  ([config role auth-context agent-contract-id]
   (let [email? (email-enabled? config)
         discord? (discord-enabled? config)
         contract-spec (effective-agent-contract config agent-contract-id)
         normalized (roles/normalize-role config (or (:role contract-spec) role))
         allowed-tool-ids (cond
                            auth-context (let [base (auth-tool-ids auth-context)]
                                           (if-let [contract-ids (some-> contract-spec :tool-ids set)]
                                             (set/intersection base contract-ids)
                                             base))
                            contract-spec (set (:tool-ids contract-spec))
                            :else (set (roles/role-tool-ids config normalized)))
         tools (cond->> allowed-tool-ids
                 true sort
                 true (mapv (fn [tool-id]
                              (let [{:keys [label description]} (tool-registry/get-tool tool-id)]
                                {:id tool-id
                                 :label label
                                 :description description
                                 :enabled (cond
                                            (= tool-id "email.send") email?
                                            (str/starts-with? tool-id "discord.") discord?
                                            :else true)})))
                 (contains? allowed-tool-ids "semantic_query")
                 (conj {:id "graph_query"
                        :label "Graph Query"
                        :description "Query the canonical knowledge graph across devel/web/bluesky/knoxx-session lakes"
                        :enabled true}))]
     {:role (if auth-context
              (or (:role contract-spec) (authz/primary-context-role auth-context))
              normalized)
      :agent_id (:id contract-spec)
      :agent_label (:id contract-spec)
      :agent_trigger_kind (:trigger-kind contract-spec)
      :email_enabled email?
      :tools tools})))

(defn create-runtime-tools
  [runtime config auth-context]
  (let [sdk (aget runtime "sdk")
        cwd (:workspace-root config)
        read-tool (aget sdk "createReadTool")
        write-tool (aget sdk "createWriteTool")
        edit-tool (aget sdk "createEditTool")
        bash-tool (aget sdk "createBashTool")
        allowed? (fn [tool-id]
                   (or (nil? auth-context)
                       (authz/ctx-tool-allowed? auth-context tool-id)))]
    (vec
     (remove nil?
             [(when (and read-tool (allowed? "read")) (read-tool cwd))
              (when (and write-tool (allowed? "write")) (write-tool cwd))
              (when (and edit-tool (allowed? "edit")) (edit-tool cwd))
              (when (and bash-tool (allowed? "bash")) (bash-tool cwd))]))))

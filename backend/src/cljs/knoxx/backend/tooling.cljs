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
                          (map keywordish->slug)
                          (remove nil?)
                          distinct
                          vec)
         :capability-ids (->> (or (:actor/capabilities actor) [])
                              (map keywordish->slug)
                              (remove nil?)
                              distinct
                              vec)
         :system-prompt (some-> (get-in actor [:prompts :system]) str str/trim not-empty)}))))

(defn actor-catalog
  [config]
  (->> (try
         (.readdirSync fs (.join (contract-loader/contracts-dir-path config) "actors"))
         (catch :default _ #js []))
       (filter (fn [name]
                 (and (string? name) (str/ends-with? name ".edn"))))
       (map (fn [name]
              (let [actor-id (subs name 0 (- (count name) 4))]
                (resolve-actor config actor-id))))
       (remove nil?)
       (sort-by :id)
       vec))

(defn default-actor-id
  [config]
  (let [configured (some-> (:knoxx-default-actor-id config) str str/trim not-empty)
        configured-actor (some-> configured (resolve-actor config))]
    (cond
      configured-actor configured
      :else (or (some-> (actor-catalog config) first :id)
                "chat_primary"))))

(defn- collect-role-tool-ids
  [config role-slugs]
  (->> role-slugs
       (map keywordish->slug)
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
       (map keywordish->slug)
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

(defn resolve-agent-contract
  [config contract-id]
  (when-let [id (some-> contract-id str str/trim not-empty)]
    (let [file-path (contract-loader/contract-file-path config "agents" id)
          contract (read-edn-sync file-path)]
      (when contract
        (let [enabled? (not (false? (:enabled contract)))
              actor-id (or (some-> (:contract/actor contract) str str/trim not-empty)
                           (some-> (:actor/id contract) str str/trim not-empty))
              actor-spec (or (some-> actor-id (resolve-actor config))
                             (some-> (default-actor-id config) (resolve-actor config)))
              contract-role-slugs (->> (concat (or (:actor/roles contract) [])
                                               [(get-in contract [:agent :role])])
                                       (map keywordish->slug)
                                       (remove nil?)
                                       distinct
                                       vec)
              actor-role-slugs (vec (or (:role-slugs actor-spec) []))
              role-slugs (vec (distinct (concat actor-role-slugs contract-role-slugs)))
              actor-capability-ids (vec (or (:capability-ids actor-spec) []))
              contract-capability-ids (->> (or (:actor/capabilities contract) [])
                                           (map keywordish->slug)
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
           :actor-id (:id actor-spec)
           :actor-kind (:kind actor-spec)
           :actor-role-slugs actor-role-slugs
           :role-slugs role-slugs
           :capability-ids capability-ids
           :role (or (first contract-role-slugs)
                     (first actor-role-slugs)
                     (:knoxx-default-role config))
           :model (some-> (get-in contract [:agent :model]) str str/trim not-empty)
           :thinking-level (some-> (get-in contract [:agent :thinking]) keywordish->slug)
           :system-prompt (or (some-> (get-in contract [:prompts :system]) str str/trim not-empty)
                              (:system-prompt actor-spec)
                              (some-> (get-in contract [:prompts :task]) str str/trim not-empty))
           :trigger-kind (some-> (:trigger-kind contract) keywordish->slug)
           :tool-ids tool-ids
           :tool-policies tool-policies})))))

(defn- manual-agent-contract?
  [entry]
  (= "manual" (some-> (:trigger-kind entry) str str/trim str/lower-case)))

(defn agent-contract-catalog
  ([config]
   (agent-contract-catalog config nil))
  ([config actor-id]
   (let [ids (try
               (.readdirSync fs (.join (contract-loader/contracts-dir-path config) "agents"))
               (catch :default _ #js []))
         wanted-actor-id (some-> actor-id str str/trim not-empty)]
     (->> ids
          (filter (fn [name]
                    (and (string? name)
                         (str/ends-with? name ".edn"))))
          (map (fn [name]
                 (resolve-agent-contract config (subs name 0 (- (count name) 4)))))
          (remove nil?)
          (filter :enabled)
          (filter manual-agent-contract?)
          (filter (fn [entry]
                    (if wanted-actor-id
                      (= wanted-actor-id (:actor-id entry))
                      true)))
          (sort-by :id)
          vec))))

(defn default-agent-contract-id
  ([config]
   (default-agent-contract-id config nil))
  ([config actor-id]
   (let [actor-spec (or (some-> actor-id (resolve-actor config))
                        (some-> (default-actor-id config) (resolve-actor config)))
         actor-default (some-> (:default-agent actor-spec) str str/trim not-empty)
         configured (some-> (:knoxx-default-agent-contract config) str str/trim not-empty)
         configured-manual (some-> configured (resolve-agent-contract config))
         actor-default-manual (some-> actor-default (resolve-agent-contract config))
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
   (or (some-> requested-contract-id (resolve-agent-contract config))
       (some-> (default-agent-contract-id config actor-id) (resolve-agent-contract config))
       (some-> (default-agent-contract-id config nil) (resolve-agent-contract config)))))

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
   (tool-catalog config role nil nil nil))
  ([config role auth-context]
   (tool-catalog config role auth-context nil nil))
  ([config role auth-context agent-contract-id]
   (tool-catalog config role auth-context agent-contract-id nil))
  ([config role auth-context agent-contract-id actor-id]
   (let [email? (email-enabled? config)
         discord? (discord-enabled? config)
         contract-spec (effective-agent-contract config agent-contract-id actor-id)
         actor-spec (or (some-> actor-id (resolve-actor config))
                        (some-> (:actor-id contract-spec) (resolve-actor config)))
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
      :actor_id (:id actor-spec)
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

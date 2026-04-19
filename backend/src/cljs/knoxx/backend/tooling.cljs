(ns knoxx.backend.tooling
  (:require [clojure.string :as str]
            [knoxx.backend.authz :as authz]
            [knoxx.backend.http :as backend-http]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.roles :as roles]
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

(defn ensure-role-can-use!
  ([role tool-id]
   (let [config (current-config)
         normalized (roles/normalize-role config role)
         allowed (set (roles/role-tool-ids config normalized))]
     (when-not (contains? allowed tool-id)
       (throw (js/Error. (str "Role '" normalized "' cannot use tool '" tool-id "'"))))
     normalized))
  ([auth-context role tool-id]
   (if auth-context
     (let [allowed (auth-tool-ids auth-context)]
       (when-not (contains? allowed tool-id)
         (throw (backend-http/http-error 403 "tool_denied" (str "Current Knoxx policy does not allow tool '" tool-id "'"))))
       (authz/primary-context-role auth-context))
     (ensure-role-can-use! role tool-id))))

(defn tool-catalog
  ([config role]
   (let [normalized (roles/normalize-role config role)
         email? (email-enabled? config)
         discord? (discord-enabled? config)]
     {:role normalized
      :email_enabled email?
      :tools (mapv (fn [{:keys [id label description]}]
                     {:id id
                      :label label
                      :description description
                      :enabled (cond
                                 (= id "email.send") email?
                                 (str/starts-with? id "discord.") discord?
                                 :else true)})
                   (roles/role-tools config normalized))}))
  ([config role auth-context]
   (if auth-context
     (let [email? (email-enabled? config)
           discord? (discord-enabled? config)
           allowed-tool-ids (->> (:toolPolicies auth-context)
                                 (filter #(= "allow" (:effect %)))
                                 (map :toolId)
                                 set)
           tools (cond-> (->> allowed-tool-ids
                              (map (fn [tool-id]
                                     (let [{:keys [label description]} (tool-registry/get-tool tool-id)]
                                       {:id tool-id
                                        :label label
                                        :description description
                                        :enabled (cond
                                                 (= tool-id "email.send") email?
                                                 (str/starts-with? tool-id "discord.") discord?
                                                 :else true)})))
                              vec)
                   (contains? allowed-tool-ids "semantic_query")
                   (conj {:id "graph_query"
                          :label "Graph Query"
                          :description "Query the canonical knowledge graph across devel/web/bluesky/knoxx-session lakes"
                          :enabled true}))]
       {:role (authz/primary-context-role auth-context)
        :email_enabled email?
        :tools tools})
     (tool-catalog config role))))

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

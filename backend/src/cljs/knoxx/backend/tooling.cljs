(ns knoxx.backend.tooling
  (:require [clojure.string :as str]
            [knoxx.backend.authz :as authz]
            [knoxx.backend.http :as backend-http]
            [knoxx.backend.runtime-config :as runtime-config]))

(defn normalize-role
  [role]
  (let [role (str (or role ""))
        canonical (or (get runtime-config/role-aliases role) role)]
    (if (contains? runtime-config/role-tools canonical) canonical "knowledge_worker")))

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
   (let [normalized (normalize-role role)
         allowed (into #{} (map first) (get runtime-config/role-tools normalized))]
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
   (let [normalized (normalize-role role)
         email? (email-enabled? config)
         discord? (discord-enabled? config)]
     {:role normalized
      :email_enabled email?
      :tools (mapv (fn [[tool-id label description]]
                     {:id tool-id
                      :label label
                      :description description
                      :enabled (cond
                                 (= tool-id "email.send") email?
                                 (str/starts-with? tool-id "discord.") discord?
                                 :else true)})
                   (get runtime-config/role-tools normalized))}))
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
                                     {:id tool-id
                                       :label (case tool-id
                                               "read" "Read"
                                               "write" "Write"
                                               "edit" "Edit"
                                               "bash" "Shell"
                                               "websearch" "Web Search"
                                               "canvas" "Canvas"
                                               "email.send" "Email"
                                               "discord.publish" "Discord Publish"
                                               "discord.send" "Discord Send"
                                               "discord.read" "Discord Read"
                                               "discord.channel.messages" "Discord Channel Messages"
                                               "discord.channel.scroll" "Discord Channel Scroll"
                                               "discord.dm.messages" "Discord DM Messages"
                                               "discord.search" "Discord Search"
                                               "discord.guilds" "Discord Guilds"
                                               "discord.channels" "Discord Channels"
                                               "discord.list.servers" "Discord List Servers"
                                               "discord.list.channels" "Discord List Channels"
                                               "event_agents.status" "Event Agent Status"
                                               "event_agents.dispatch" "Event Agent Dispatch"
                                               "event_agents.run_job" "Event Agent Run Job"
                                               "event_agents.upsert_job" "Event Agent Upsert Job"
                                               "schedule_event_agent" "Schedule Event Agent"
                                               "bluesky.publish" "Bluesky"
                                               "semantic_query" "Semantic Query"
                                               "graph_query" "Graph Query"
                                               "memory_search" "Memory Search"
                                               "memory_session" "Memory Session"
                                               "save_translation" "Save Translation"
                                               tool-id)
                                      :description (case tool-id
                                                     "read" "Read files and retrieved context"
                                                     "write" "Create new markdown drafts and artifacts"
                                                     "edit" "Revise existing documents and drafts"
                                                     "bash" "Run controlled shell commands"
                                                     "websearch" "Search the live web through Proxx websearch"
                                                     "canvas" "Open long-form markdown drafting canvas"
                                                     "email.send" "Send drafts through configured email account"
                                                     "discord.publish" "Publish updates to Discord"
                                                     "discord.send" "Send Discord messages and threaded replies"
                                                     "discord.read" "Read messages from Discord channels"
                                                     "discord.channel.messages" "Fetch messages from a Discord channel with before/after/around cursors"
                                                     "discord.channel.scroll" "Scroll older messages in a Discord channel"
                                                     "discord.dm.messages" "Fetch messages from a Discord DM channel"
                                                     "discord.search" "Search messages in Discord channels"
                                                     "discord.guilds" "List Discord servers the bot is in"
                                                     "discord.channels" "List channels in a Discord server"
                                                     "discord.list.servers" "List all Discord servers the bot can access"
                                                     "discord.list.channels" "List channels in one or all Discord servers"
                                                     "event_agents.status" "Inspect scheduled event-agent runtime state and configuration"
                                                     "event_agents.dispatch" "Dispatch a structured event into the event-agent runtime"
                                                     "event_agents.run_job" "Trigger a configured event-agent job immediately"
                                                     "event_agents.upsert_job" "Create or update a scheduled event-agent job"
                                                     "schedule_event_agent" "Create or update a scheduled event-agent job with prompts, tools, triggers, and source config"
                                                     "bluesky.publish" "Publish updates to Bluesky"
                                                     "semantic_query" "Query semantic context in the active corpus"
                                                     "graph_query" "Query the canonical knowledge graph across devel/web/bluesky/knoxx-session lakes"
                                                     "memory_search" "Search prior Knoxx sessions in OpenPlanner"
                                                     "memory_session" "Load a specific Knoxx session from OpenPlanner"
                                                     "save_translation" "Save translated segments to the translation database"
                                                     tool-id)
                                      :enabled (cond
                                                 (= tool-id "email.send") email?
                                                 (str/starts-with? tool-id "discord.") discord?
                                                 :else true)}))
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

(ns knoxx.backend.domain.discord.discord-io
  "Discord I/O helpers. Pure API wrappers consumed by trigger-runner,
   pipeline-runner, and agent tools. No scheduling or job logic here."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.discord.rest-client :as discord-rest]
            [knoxx.backend.infra.agent.runner :as agents-runner]
            [knoxx.backend.infra.config :as runtime-config]))

(defn- discord-token
  []
  (or (some-> (knoxx.backend.infra.config/cfg) :discord-bot-token)
      (throw (js/Error. "Discord bot token not configured"))))

(defn- discord-client
  []
  (discord-rest/client (discord-token)))

(defn- map-message
  [msg]
  (let [author (or (:author msg) {})]
    {:id (:id msg)
     :channelId (or (:channel_id msg) "")
     :content (or (:content msg) "")
     :authorId (or (:id author) "")
     :authorUsername (or (:username author) "unknown")
     :authorIsBot (boolean (:bot author))
     :timestamp (or (:timestamp msg) "")}))

(defn- sort-newest-first
  [messages]
  (sort-by :timestamp #(compare %2 %1) messages))

(defn read-channel!
  "Fetch up to `limit` messages from `channel-id` (max 100).
   Returns Promise<[{:id :channelId :content :authorId :authorUsername :authorIsBot :timestamp}]>."
  [channel-id & [limit]]
  (-> (discord-rest/channel-messages! (discord-client) channel-id {:limit (max 1 (min 100 (or limit 25)))})
      (.then (fn [payload]
               (->> (or payload [])
                    (map map-message)
                    sort-newest-first
                    vec)))))

(defn search-channel!
  "Return messages in `channel-id` whose content contains `query`
   (case-insensitive). Scans up to 100 messages, returns up to `limit`."
  [channel-id query & [limit]]
  (-> (read-channel! channel-id 100)
      (.then (fn [messages]
               (let [needle (str/lower-case (str (or query "")))]
                 (->> messages
                      (filter (fn [message]
                                (str/includes? (str/lower-case (:content message)) needle)))
                      (take (or limit 25))
                      vec))))))

(defn list-guilds!
  "List guilds the bot is a member of."
  []
  (-> (discord-rest/current-user-guilds! (discord-client))
      (.then (fn [payload]
               (->> (or payload [])
                    (mapv (fn [guild]
                            {:id (:id guild)
                             :name (:name guild)})))))))

(defn list-channels!
  "List channels in `guild-id` (types 0, 5, 11, 12 only)."
  [guild-id]
  (-> (discord-rest/guild-channels! (discord-client) guild-id)
      (.then (fn [payload]
               (->> (or payload [])
                    (filter (fn [channel]
                              (contains? #{0 5 11 12} (:type channel))))
                    (mapv (fn [channel]
                            {:id (:id channel)
                             :guildId guild-id
                             :name (or (:name channel) "")
                             :type (:type channel)})))))))

(def ^:private default-discord-tool-policies
  [{:toolId "discord.read" :effect "allow"}
   {:toolId "discord.search" :effect "allow"}
   {:toolId "discord.publish" :effect "allow"}
   {:toolId "discord.guilds" :effect "allow"}
   {:toolId "memory_search" :effect "allow"}
   {:toolId "graph_query" :effect "allow"}])

(defn start-agent-session!
  "Launch a normal Knoxx direct-mode turn for a Discord-triggered payload.
   `opts` map accepts :channelId :channelName :authorUsername :content :reason."
  [config job {:keys [channelId channelName authorUsername content reason]}]
  (let [now (.now js/Date)
        job-agent-spec (or (:agentSpec job) {})
        run-id (str "discord-" (:id job) "-" now)
        conversation-id (str "discord-" (:id job) "-" channelId "-" now)
        session-id (str "discord-session-" (:id job) "-" now)
        task-prompt (or (:taskPrompt job) (:taskPrompt job-agent-spec) "")
        tool-policies (or (:toolPolicies job)
                          (:toolPolicies job-agent-spec)
                          default-discord-tool-policies)
        user-message (str "Discord job: " (:name job) "\n"
                          "Reason: " reason "\n"
                          "Channel ID: " channelId "\n"
                          "Channel Name: " (or channelName channelId) "\n"
                          "Author: " authorUsername "\n"
                          "Message: " content "\n\n"
                          (when-not (str/blank? task-prompt)
                            (str "Job task prompt:\n" task-prompt "\n\n"))
                          "Use discord.read, discord.search, discord.channels, and discord.guilds when they improve confidence. "
                          "If a response is warranted, send it with discord.publish to the target channel. "
                          "If not, stay silent.")
        body {:conversation_id conversation-id
              :session_id session-id
              :run_id run-id
              :message user-message
              :agent_spec {:contract_id (or (:contractId job) (:contractId job-agent-spec))
                           :actor_id (or (:actorId job) (:actorId job-agent-spec))
                           :role (or (:role job) (:role job-agent-spec) "system_admin")
                           :system_prompt (or (:systemPrompt job)
                                              (:systemPrompt job-agent-spec)
                                              "You are Knoxx's Discord agent.")
                           :task_prompt task-prompt
                           :model (or (:model job)
                                      (:model job-agent-spec)
                                      (:proxx-default-model config)
                                      "glm-5")
                           :thinking_level (or (:thinkingLevel job)
                                               (:thinkingLevel job-agent-spec)
                                               "off")
                           :tool_policies tool-policies
                           :sources (or (:sources job) (:sources job-agent-spec))
                           :memory_hydration (or (:memoryHydration job) (:memoryHydration job-agent-spec))
                           :context_policy (or (:contextPolicy job) (:contextPolicy job-agent-spec))}
              :model (or (:model job)
                         (:model job-agent-spec)
                         (:proxx-default-model config)
                         "glm-5")}]
    (-> (agents-runner/spawn-direct! config body)
        (.then (fn [result]
                 (println "[discord-io] queued agent run" run-id "for job" (:id job))
                 result))
        (.catch (fn [err]
                  (println "[discord-io] failed to queue agent run for job" (:id job)
                           ":" (.-message err))
                  nil)))))

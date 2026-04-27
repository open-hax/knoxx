(ns knoxx.backend.discord-io
  "Discord I/O helpers. Pure API wrappers consumed by trigger-runner,
   pipeline-runner, and agent tools. No scheduling or job logic here."
  (:require [clojure.string :as str]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.models :as runtime-models]))

(defn- discord-token
  []
  (or (some-> (knoxx.backend.runtime.config/cfg) :discord-bot-token)
      (throw (js/Error. "Discord bot token not configured"))))

(defn- discord-headers
  [token]
  #js {"Authorization" (str "Bot " token)
       "Content-Type" "application/json"})

(defn- fetch-json!
  [url options]
  (-> (js/fetch url options)
      (.then (fn [resp]
               (if (.-ok resp)
                 (.json resp)
                 (-> (.text resp)
                     (.then (fn [text]
                              (throw (js/Error. (str "HTTP " (.-status resp) ": " text)))))))))))

(defn- map-message
  [msg]
  {:id (aget msg "id")
   :channelId (or (aget msg "channel_id") "")
   :content (or (aget msg "content") "")
   :authorId (or (aget msg "author" "id") "")
   :authorUsername (or (aget msg "author" "username") "unknown")
   :authorIsBot (boolean (aget msg "author" "bot"))
   :timestamp (or (aget msg "timestamp") "")})

(defn- sort-newest-first
  [messages]
  (sort-by :timestamp #(compare %2 %1) messages))

(defn read-channel!
  "Fetch up to `limit` messages from `channel-id` (max 100).
   Returns Promise<[{:id :channelId :content :authorId :authorUsername :authorIsBot :timestamp}]>."
  [channel-id & [limit]]
  (let [token (discord-token)]
    (-> (fetch-json!
         (str "https://discord.com/api/v10/channels/" channel-id "/messages?limit="
              (max 1 (min 100 (or limit 25))))
         #js {:method "GET"
              :headers (discord-headers token)})
        (.then (fn [payload]
                 (->> (if (array? payload) (array-seq payload) [])
                      (map map-message)
                      sort-newest-first
                      vec))))))

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
  (let [token (discord-token)]
    (-> (fetch-json! "https://discord.com/api/v10/users/@me/guilds"
                     #js {:method "GET"
                          :headers (discord-headers token)})
        (.then (fn [payload]
                 (->> (if (array? payload) (array-seq payload) [])
                      (mapv (fn [guild]
                              {:id (aget guild "id")
                               :name (aget guild "name")}))))))))

(defn list-channels!
  "List channels in `guild-id` (types 0, 5, 11, 12 only)."
  [guild-id]
  (let [token (discord-token)]
    (-> (fetch-json! (str "https://discord.com/api/v10/guilds/" guild-id "/channels")
                     #js {:method "GET"
                          :headers (discord-headers token)})
        (.then (fn [payload]
                 (->> (if (array? payload) (array-seq payload) [])
                      (filter (fn [channel]
                                (contains? #{0 5 11 12} (aget channel "type"))))
                      (mapv (fn [channel]
                              {:id (aget channel "id")
                               :guildId guild-id
                               :name (or (aget channel "name") "")
                               :type (aget channel "type")}))))))))

(defn start-agent-session!
  "POST /api/knoxx/direct/start with a Discord-triggered payload.
   `opts` map accepts :channelId :channelName :authorUsername :content :reason."
  [config job {:keys [channelId channelName authorUsername content reason]}]
  (let [now (.now js/Date)
        run-id (str "discord-" (:id job) "-" now)
        conversation-id (str "discord-" (:id job) "-" channelId "-" now)
        session-id (str "discord-session-" (:id job) "-" now)
        task-prompt (or (:taskPrompt job) "")
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
        headers (let [api-key (:knoxx-api-key config)]
                  (cond-> #js {"Content-Type" "application/json"
                               "x-knoxx-user-email" "discord-cron@knoxx"}
                    (not (str/blank? api-key))
                    (aset "X-API-Key" api-key)))
        body #js {:conversation_id conversation-id
                  :session_id session-id
                  :run_id run-id
                  :message user-message
                  :agent_spec #js {:role (or (:role job) "system_admin")
                                   :system_prompt (or (:systemPrompt job)
                                                       "You are Knoxx's Discord agent.")
                                   :model (or (:model job)
                                              (:proxx-default-model config)
                                              "glm-5")
                                   :thinking_level (or (:thinkingLevel job) "off")
                                   :tool_policies #js [#js {:toolId "discord.read" :effect "allow"}
                                                       #js {:toolId "discord.search" :effect "allow"}
                                                       #js {:toolId "discord.publish" :effect "allow"}
                                                       #js {:toolId "discord.guilds" :effect "allow"}
                                                       #js {:toolId "memory_search" :effect "allow"}
                                                       #js {:toolId "graph_query" :effect "allow"}]}
                  :model (or (:model job)
                             (:proxx-default-model config)
                             "glm-5")}]
    (-> (fetch-json! (str (:knoxx-base-url config) "/api/knoxx/direct/start")
                     #js {:method "POST"
                          :headers headers
                          :body (.stringify js/JSON body)})
        (.then (fn [result]
                 (println "[discord-io] queued agent run" run-id "for job" (:id job))
                 result))
        (.catch (fn [err]
                  (println "[discord-io] failed to queue agent run for job" (:id job)
                           ":" (.-message err))
                  nil)))))

(ns knoxx.backend.discord-cron
  "Cron-driven Discord worker for Knoxx.

   This follows the translation-worker pattern conceptually:
   - poll on a named schedule
   - detect concrete work
   - start a Knoxx agent run only when there is signal

   It deliberately avoids cephalon's old infinite setTimeout tick loop."
  (:require [clojure.string :as str]
            [knoxx.backend.runtime-config :as runtime-config]))

(defonce running?* (atom false))
(defonce cron-tasks* (atom {}))
(defonce last-seen-messages* (atom {}))
(defonce mention-queue* (atom []))

(defn- cfg []
  (runtime-config/cfg))

(defn monitored-channels
  []
  (->> (str/split (or (runtime-config/env "DISCORD_CHANNEL_IDS" "") "") #",")
       (map str/trim)
       (remove str/blank?)
       vec))

(defn- target-keywords
  []
  (->> (str/split (or (runtime-config/env "DISCORD_TARGET_KEYWORDS" "knoxx,cephalon") "") #",")
       (map (comp str/lower-case str/trim))
       (remove str/blank?)
       vec))

(defn- discord-token
  []
  (:discord-bot-token (cfg)))

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
  [_config channel-id limit]
  (let [token (discord-token)]
    (if (str/blank? token)
      (js/Promise.reject (js/Error. "Discord bot token not configured"))
      (-> (fetch-json! (str "https://discord.com/api/v10/channels/" channel-id "/messages?limit=" (max 1 (min 100 (or limit 25))))
                       #js {:method "GET"
                            :headers (discord-headers token)})
          (.then (fn [payload]
                   (->> (if (array? payload) (array-seq payload) [])
                        (map map-message)
                        sort-newest-first
                        vec)))))))

(defn search-channel!
  [_config channel-id query limit]
  (-> (read-channel! nil channel-id 100)
      (.then (fn [messages]
               (let [needle (str/lower-case (str (or query "")))]
                 (->> messages
                      (filter (fn [message]
                                (str/includes? (str/lower-case (:content message)) needle)))
                      (take (or limit 25))
                      vec))))))

(defn list-guilds!
  [_config]
  (let [token (discord-token)]
    (if (str/blank? token)
      (js/Promise.reject (js/Error. "Discord bot token not configured"))
      (-> (fetch-json! "https://discord.com/api/v10/users/@me/guilds"
                       #js {:method "GET"
                            :headers (discord-headers token)})
          (.then (fn [payload]
                   (->> (if (array? payload) (array-seq payload) [])
                        (mapv (fn [guild]
                                {:id (aget guild "id")
                                 :name (aget guild "name")})))))))))

(defn list-channels!
  [_config guild-id]
  (let [token (discord-token)]
    (if (str/blank? token)
      (js/Promise.reject (js/Error. "Discord bot token not configured"))
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
                                 :type (aget channel "type")})))))))))

(defn- seen-id
  [channel-id]
  (get @last-seen-messages* channel-id))

(defn- unseen-messages
  [channel-id messages]
  (let [known-id (seen-id channel-id)]
    (if (str/blank? known-id)
      messages
      (->> messages
           (take-while (fn [message]
                         (not= known-id (:id message))))
           vec))))

(defn- remember-latest!
  [channel-id messages]
  (when-let [latest-id (:id (first messages))]
    (swap! last-seen-messages* assoc channel-id latest-id)))

(defn- message-mentions-bot?
  [message]
  (let [bot-user-id (str/trim (or (runtime-config/env "DISCORD_BOT_USER_ID" "") ""))
        content (str/lower-case (:content message))]
    (or (and (not (str/blank? bot-user-id))
             (or (str/includes? content (str "<@" bot-user-id ">"))
                 (str/includes? content (str "<@!" bot-user-id ">"))))
        (some (fn [keyword]
                (str/includes? content keyword))
              (target-keywords)))))

(defn- message-worthy?
  [message]
  (and (not (:authorIsBot message))
       (not (str/blank? (:content message)))
       (message-mentions-bot? message)))

(defn- queue-message!
  [message]
  (swap! mention-queue*
         (fn [queue]
           (if (some #(= (:id %) (:id message)) queue)
             queue
             (conj queue message)))))

(defn- direct-start-headers
  [config]
  (let [api-key (:knoxx-api-key config)]
    (cond-> #js {"Content-Type" "application/json"
                 "x-knoxx-user-email" "discord-cron@knoxx"}
      (not (str/blank? api-key))
      (aset "X-API-Key" api-key))))

(defn- start-discord-agent-session!
  [config {:keys [channelId channelName authorUsername content reason]}]
  (let [now (.now js/Date)
        run-id (str "discord-cron-" now)
        conversation-id (str "discord-cron-" channelId "-" now)
        session-id (str "discord-cron-session-" now)
        user-message (str "Discord signal detected.\n"
                          "Reason: " reason "\n"
                          "Channel ID: " channelId "\n"
                          "Channel Name: " (or channelName channelId) "\n"
                          "Author: " authorUsername "\n"
                          "Message: " content "\n\n"
                          "Read more context if needed using discord.read or discord.search. "
                          "If a response is useful, send it with discord.publish to channelId=" channelId ". "
                          "If no response is warranted, stay silent.")
        body #js {:conversation_id conversation-id
                  :session_id session-id
                  :run_id run-id
                  :message user-message
                  :agent_spec #js {:role "system_admin"
                                   :system_prompt (str "You are Knoxx's cron-driven Discord agent. "
                                                       "Be targeted. Read context before replying when needed. "
                                                       "Do not post internal status memos. "
                                                       "Keep useful responses short, native to the room, and optional.")
                                   :model (or (:proxx-default-model config) "glm-5")
                                   :thinking_level "off"
                                   :tool_policies #js [#js {:toolId "discord.read" :effect "allow"}
                                                       #js {:toolId "discord.search" :effect "allow"}
                                                       #js {:toolId "discord.publish" :effect "allow"}
                                                       #js {:toolId "memory_search" :effect "allow"}
                                                       #js {:toolId "graph_query" :effect "allow"}]}
                  :model (or (:proxx-default-model config) "glm-5")}]
    (-> (fetch-json! (str (:knoxx-base-url config) "/api/knoxx/direct/start")
                     #js {:method "POST"
                          :headers (direct-start-headers config)
                          :body (.stringify js/JSON body)})
        (.then (fn [result]
                 (println "[discord-cron] queued agent run" run-id "for channel" channelId)
                 result))
        (.catch (fn [err]
                  (println "[discord-cron] failed to queue agent run:" (.-message err))
                  nil)))))

(defn- patrol-channel!
  [config channel-id]
  (-> (read-channel! config channel-id 25)
      (.then (fn [messages]
               (let [fresh (unseen-messages channel-id messages)]
                 (doseq [message fresh]
                   (when (message-worthy? message)
                     (queue-message! message)))
                 (remember-latest! channel-id messages)
                 {:channelId channel-id
                  :fetched (count messages)
                  :fresh (count fresh)
                  :queued (count (filter message-worthy? fresh))})))
      (.catch (fn [err]
                (println "[discord-cron] patrol failed for" channel-id ":" (.-message err))
                {:channelId channel-id
                 :error true}))))

(defn- patrol-job!
  [config]
  (let [channel-ids (monitored-channels)]
    (when (seq channel-ids)
      (println "[discord-cron] patrol-channels start" (count channel-ids) "channels")
      (js/Promise.all
       (clj->js (mapv (fn [channel-id]
                        (patrol-channel! config channel-id))
                      channel-ids))))))

(defn- drain-mention-queue!
  []
  (let [queued @mention-queue*]
    (reset! mention-queue* [])
    queued))

(defn- mention-check-job!
  [config]
  (let [queued (drain-mention-queue!)]
    (when (seq queued)
      (println "[discord-cron] mention-check processing" (count queued) "queued messages")
      (js/Promise.all
       (clj->js
        (mapv (fn [message]
                (start-discord-agent-session!
                 config
                 {:channelId (:channelId message)
                  :channelName (:channelId message)
                  :authorUsername (:authorUsername message)
                  :content (:content message)
                  :reason "mention-or-keyword"}))
              queued))))))

(defn- summarize-channel
  [channel-id messages]
  (->> messages
       (remove :authorIsBot)
       (take 8)
       (map (fn [message]
              (str "[" channel-id "] <" (:authorUsername message) "> "
                   (subs (:content message) 0 (min 180 (count (:content message)))))))
       (str/join "\n")))

(defn- deep-synthesis-job!
  [config]
  (let [channel-ids (monitored-channels)]
    (when (seq channel-ids)
      (println "[discord-cron] deep-synthesis start")
      (-> (js/Promise.all
           (clj->js
            (mapv (fn [channel-id]
                    (-> (read-channel! config channel-id 12)
                        (.then (fn [messages]
                                 {:channelId channel-id
                                  :messages messages}))
                        (.catch (fn [_]
                                  {:channelId channel-id
                                   :messages []}))))
                  channel-ids)))
          (.then (fn [results]
                   (let [channels (js->clj results :keywordize-keys true)
                         summary (->> channels
                                      (map (fn [{:keys [channelId messages]}]
                                             (summarize-channel channelId messages)))
                                      (remove str/blank?)
                                      (str/join "\n\n"))]
                     (when-not (str/blank? summary)
                       (start-discord-agent-session!
                        config
                        {:channelId (first channel-ids)
                         :channelName (first channel-ids)
                         :authorUsername "synthesis"
                         :content (str "Cross-channel synthesis checkpoint:\n\n" summary)
                         :reason "scheduled-deep-synthesis"})))))))))

(defn- clear-interval-task!
  [task]
  (when-let [id (:id task)]
    (js/clearInterval id)))

(defn stop!
  []
  (when @running?*
    (doseq [[_ task] @cron-tasks*]
      (cond
        (and task (fn? (aget task "stop"))) (.stop task)
        (and task (map? task) (= :interval (:type task))) (clear-interval-task! task)
        :else nil))
    (reset! cron-tasks* {})
    (reset! running?* false)
    (println "[discord-cron] stopped")))

(defn- schedule-interval-task!
  [name schedule-expr every-ms f]
  (let [id (js/setInterval f every-ms)
        task {:type :interval
              :id id
              :schedule schedule-expr
              :every-ms every-ms}]
    (swap! cron-tasks* assoc name task)
    task))

(defn start!
  [_config]
  (when-not @running?*
    (let [config (cfg)
          token (:discord-bot-token config)
          channel-ids (monitored-channels)]
      (when-not (str/blank? token)
        (reset! running?* true)
        (println "[discord-cron] starting; channels=" (pr-str channel-ids))
        ;; cron-like named schedules, but implemented with bounded intervals so we avoid
        ;; cephalon's unstructured recursive tick loop.
        (schedule-interval-task! :patrol "*/5 * * * *" (* 5 60 1000) #(patrol-job! config))
        (schedule-interval-task! :mentions "* * * * *" (* 60 1000) #(mention-check-job! config))
        (schedule-interval-task! :deep-synthesis "0 */2 * * *" (* 2 60 60 1000) #(deep-synthesis-job! config))
        ;; warm initial patrol so the worker becomes useful immediately after boot
        (patrol-job! config)))))
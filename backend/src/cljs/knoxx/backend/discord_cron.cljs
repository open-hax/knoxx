(ns knoxx.backend.discord-cron
  "Discord worker control plane for Knoxx.

   The worker runs named scheduled jobs on bounded intervals instead of using
   cephalon's recursive tick loop. Job behavior is controlled by runtime config
   so the admin dashboard can tune cadence, channels, roles, prompts, and model
   selection per job."
  (:require [clojure.string :as str]
            [knoxx.backend.runtime-config :as runtime-config]))

(defonce running?* (atom false))
(defonce cron-tasks* (atom {}))
(defonce last-seen-messages* (atom {}))
(defonce mention-queue* (atom []))
(defonce job-state* (atom {}))

(defn- cfg []
  (runtime-config/cfg))

(defn- control-config
  [config]
  (runtime-config/discord-agent-control-config config))

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
      (-> (fetch-json!
           (str "https://discord.com/api/v10/channels/" channel-id "/messages?limit=" (max 1 (min 100 (or limit 25))))
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

(defn- find-job
  [control job-id]
  (some (fn [job]
          (when (= (:id job) job-id)
            job))
        (:jobs control)))

(defn- job-channels
  [control job]
  (let [channels (vec (or (:channels job) []))]
    (if (seq channels)
      channels
      (vec (or (:defaultChannels control) [])))))

(defn- job-keywords
  [control job]
  (let [keywords (vec (or (:keywords job) []))]
    (if (seq keywords)
      (mapv str/lower-case keywords)
      (mapv str/lower-case (or (:targetKeywords control) [])))))

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
  [control job message]
  (let [bot-user-id (some-> (:botUserId control) str str/trim)
        content (str/lower-case (:content message))]
    (or (and (not (str/blank? bot-user-id))
             (or (str/includes? content (str "<@" bot-user-id ">"))
                 (str/includes? content (str "<@!" bot-user-id ">"))))
        (some (fn [keyword]
                (str/includes? content keyword))
              (job-keywords control job)))))

(defn- message-worthy?
  [control job message]
  (and (not (:authorIsBot message))
       (not (str/blank? (:content message)))
       (message-mentions-bot? control job message)))

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
        body #js {:conversation_id conversation-id
                  :session_id session-id
                  :run_id run-id
                  :message user-message
                  :agent_spec #js {:role (or (:role job) "system_admin")
                                   :system_prompt (or (:systemPrompt job) "You are Knoxx's Discord agent.")
                                   :model (or (:model job) (:proxx-default-model config) "glm-5")
                                   :thinking_level (or (:thinkingLevel job) "off")
                                   :tool_policies #js [#js {:toolId "discord.read" :effect "allow"}
                                                       #js {:toolId "discord.search" :effect "allow"}
                                                       #js {:toolId "discord.publish" :effect "allow"}
                                                       #js {:toolId "discord.guilds" :effect "allow"}
                                                       #js {:toolId "discord.channels" :effect "allow"}
                                                       #js {:toolId "memory_search" :effect "allow"}
                                                       #js {:toolId "graph_query" :effect "allow"}]}
                  :model (or (:model job) (:proxx-default-model config) "glm-5")}]
    (-> (fetch-json! (str (:knoxx-base-url config) "/api/knoxx/direct/start")
                     #js {:method "POST"
                          :headers (direct-start-headers config)
                          :body (.stringify js/JSON body)})
        (.then (fn [result]
                 (println "[discord-cron] queued agent run" run-id "for job" (:id job) "channel" channelId)
                 result))
        (.catch (fn [err]
                  (println "[discord-cron] failed to queue agent run for job" (:id job) ":" (.-message err))
                  nil)))))

(defn- update-job-state!
  [job-id f]
  (swap! job-state* update job-id (fn [current]
                                    (f (or current {})))))

(defn- run-job-instrumented!
  [config job job-fn]
  (let [job-id (:id job)
        started-ms (.now js/Date)
        cadence-ms (* 60 1000 (max 1 (:cadenceMinutes job)))]
    (update-job-state! job-id
                       (fn [state]
                         (assoc state
                                :running true
                                :lastStartedAt started-ms
                                :lastStatus "running"
                                :nextRunAt (+ started-ms cadence-ms))))
    (-> (js/Promise.resolve (job-fn config job))
        (.then (fn [result]
                 (let [finished-ms (.now js/Date)]
                   (update-job-state! job-id
                                      (fn [state]
                                        (-> state
                                            (assoc :running false
                                                   :lastFinishedAt finished-ms
                                                   :lastDurationMs (- finished-ms started-ms)
                                                   :lastStatus "ok")
                                            (update :runCount (fnil inc 0)))))
                   result)))
        (.catch (fn [err]
                  (let [finished-ms (.now js/Date)]
                    (update-job-state! job-id
                                       (fn [state]
                                         (-> state
                                             (assoc :running false
                                                    :lastFinishedAt finished-ms
                                                    :lastDurationMs (- finished-ms started-ms)
                                                    :lastStatus "error"
                                                    :lastError (.-message err))
                                             (update :runCount (fnil inc 0))))))
                  (println "[discord-cron] job failed" job-id ":" (.-message err))
                  nil)))))

(defn- patrol-channel!
  [config control job channel-id]
  (-> (read-channel! config channel-id (:maxMessages job))
      (.then (fn [messages]
               (let [fresh (unseen-messages channel-id messages)
                     queued (filter #(message-worthy? control job %) fresh)]
                 (doseq [message queued]
                   (queue-message! message))
                 (remember-latest! channel-id messages)
                 {:channelId channel-id
                  :fetched (count messages)
                  :fresh (count fresh)
                  :queued (count queued)})))
      (.catch (fn [err]
                (println "[discord-cron] patrol failed for" channel-id ":" (.-message err))
                {:channelId channel-id
                 :error true}))))

(defn- patrol-job!
  [config job]
  (let [control (control-config config)
        channel-ids (job-channels control job)]
    (when (seq channel-ids)
      (println "[discord-cron] patrol start" (count channel-ids) "channels")
      (js/Promise.all
       (clj->js
        (mapv (fn [channel-id]
                (patrol-channel! config control job channel-id))
              channel-ids))))))

(defn- drain-mention-queue!
  []
  (let [queued @mention-queue*]
    (reset! mention-queue* [])
    queued))

(defn- mention-check-job!
  [config job]
  (let [queued (drain-mention-queue!)]
    (when (seq queued)
      (println "[discord-cron] mention-check processing" (count queued) "queued messages")
      (js/Promise.all
       (clj->js
        (mapv (fn [message]
                (start-discord-agent-session!
                 config
                 job
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
  [config job]
  (let [control (control-config config)
        channel-ids (job-channels control job)]
    (when (seq channel-ids)
      (println "[discord-cron] deep-synthesis start")
      (-> (js/Promise.all
           (clj->js
            (mapv (fn [channel-id]
                    (-> (read-channel! config channel-id (:maxMessages job))
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
                        job
                        {:channelId (first channel-ids)
                         :channelName (first channel-ids)
                         :authorUsername "synthesis"
                         :content (str "Cross-channel synthesis checkpoint:\n\n" summary)
                         :reason "scheduled-deep-synthesis"})))))))))

(defn- execute-job!
  [config job]
  (case (:id job)
    "patrol" (run-job-instrumented! config job patrol-job!)
    "mentions" (run-job-instrumented! config job mention-check-job!)
    "deep-synthesis" (run-job-instrumented! config job deep-synthesis-job!)
    (do
      (println "[discord-cron] unknown job id" (:id job))
      (js/Promise.resolve nil))))

(defn- clear-interval-task!
  [task]
  (when-let [id (:id task)]
    (js/clearInterval id)))

(defn stop!
  []
  (doseq [[_ task] @cron-tasks*]
    (when (and task (map? task) (= :interval (:type task)))
      (clear-interval-task! task)))
  (reset! cron-tasks* {})
  (reset! running?* false)
  (println "[discord-cron] stopped"))

(defn reload!
  []
  (stop!)
  (start! nil))

(defn- cadence->schedule-label
  [minutes]
  (cond
    (= minutes 1) "Every minute"
    (< minutes 60) (str "Every " minutes " minutes")
    (= (mod minutes 60) 0) (str "Every " (/ minutes 60) " hours")
    :else (str "Every " minutes " minutes")))

(defn status-snapshot
  [config]
  (let [control (control-config config)]
    {:running @running?*
     :configured (not (str/blank? (:discord-bot-token config)))
     :channelCount (count (:defaultChannels control))
     :channels (:defaultChannels control)
     :lastSeenChannels (-> @last-seen-messages* keys vec)
     :mentionQueueCount (count @mention-queue*)
     :jobs (mapv (fn [job]
                   (let [state (get @job-state* (:id job) {})]
                     (merge {:id (:id job)
                             :name (:name job)
                             :enabled (:enabled job)
                             :scheduleLabel (cadence->schedule-label (:cadenceMinutes job))}
                            state)))
                 (:jobs control))}))

(defn run-job!
  [job-id]
  (let [config (cfg)
        control (control-config config)
        job (find-job control job-id)]
    (if job
      (execute-job! config job)
      (js/Promise.reject (js/Error. (str "Unknown Discord job: " job-id))))))

(defn- schedule-interval-task!
  [config job]
  (let [every-ms (* 60 1000 (max 1 (:cadenceMinutes job)))
        wrapped (fn []
                  (when @running?*
                    (execute-job! config job)
                    nil))
        id (js/setInterval wrapped every-ms)
        task {:type :interval
              :id id
              :everyMs every-ms
              :scheduleLabel (cadence->schedule-label (:cadenceMinutes job))}]
    (swap! cron-tasks* assoc (:id job) task)
    (update-job-state! (:id job)
                       (fn [state]
                         (merge state
                                {:configured true
                                 :enabled (:enabled job)
                                 :cadenceMinutes (:cadenceMinutes job)
                                 :nextRunAt (+ (.now js/Date) every-ms)})))
    task))

(defn start!
  [_config]
  (when-not @running?*
    (let [config (cfg)
          token (:discord-bot-token config)
          control (control-config config)]
      (when-not (str/blank? token)
        (reset! running?* true)
        (println "[discord-cron] starting; channels=" (pr-str (:defaultChannels control)))
        (doseq [job (:jobs control)]
          (when (:enabled job)
            (schedule-interval-task! config job)))
        (when-let [patrol-job (find-job control "patrol")]
          (when (:enabled patrol-job)
            (execute-job! config patrol-job)
            nil))))))
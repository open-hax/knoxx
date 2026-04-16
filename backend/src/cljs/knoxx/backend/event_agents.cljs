(ns knoxx.backend.event-agents
  "Generic event-agent runtime for Knoxx.

   Adapters emit normalized events.
   Jobs describe triggers + source filters + arbitrary agent specs.
   The runtime matches events/jobs and launches Knoxx runs through direct/start."
  (:require [clojure.string :as str]
            [knoxx.backend.runtime-config :as runtime-config]
            [knoxx.backend.redis-client :as redis]))

(declare start!)

(declare start!)

(defonce running?* (atom false))
(defonce scheduled-tasks* (atom {}))
(defonce job-state* (atom {}))
(defonce source-state* (atom {:discord {:last-seen {}}}))
(defonce recent-events* (atom []))
(defonce discord-gateway-unsubscribe* (atom nil))

(defn- cfg []
  (runtime-config/cfg))

(defn- control-config
  [config]
  (runtime-config/event-agent-control-config config))

(defn- discord-token
  []
  (:discord-bot-token (cfg)))

(defn- discord-gateway-manager
  []
  (aget js/globalThis "knoxxDiscordGateway"))

(defn- discord-gateway-active?
  []
  (when-let [manager (discord-gateway-manager)]
    (let [status (.status manager)]
      (boolean (or (aget status "ready")
                   (aget status "started"))))))

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

(defn- map-discord-message
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

(defn- read-discord-channel!
  [channel-id limit]
  (if (discord-gateway-active?)
    (-> (.fetchChannelMessages (discord-gateway-manager) channel-id (clj->js {:limit (max 1 (min 100 (or limit 25)))}))
        (.then (fn [messages]
                 (->> (js->clj messages :keywordize-keys true)
                      sort-newest-first
                      vec))))
    (let [token (discord-token)]
      (if (str/blank? token)
        (js/Promise.reject (js/Error. "Discord bot token not configured"))
        (-> (fetch-json!
             (str "https://discord.com/api/v10/channels/" channel-id "/messages?limit=" (max 1 (min 100 (or limit 25))))
             #js {:method "GET"
                  :headers (discord-headers token)})
            (.then (fn [payload]
                     (->> (if (array? payload) (array-seq payload) [])
                          (map map-discord-message)
                          sort-newest-first
                          vec))))))))

(defn- discord-source-config
  [control]
  (or (get-in control [:sources :discord]) {}))

(defn- job-channels
  [control job]
  (let [channels (->> (or (get-in job [:filters :channels]) [])
                      (map (fn [value] (some-> value str str/trim not-empty)))
                      (remove nil?)
                      vec)]
    (if (seq channels)
      channels
      (vec (or (:defaultChannels (discord-source-config control)) [])))))

(defn- job-keywords
  [control job]
  (let [keywords (->> (or (get-in job [:filters :keywords]) [])
                      (map (fn [value] (some-> value str str/trim str/lower-case not-empty)))
                      (remove nil?)
                      distinct
                      vec)]
    (if (seq keywords)
      keywords
      (vec (or (:targetKeywords (discord-source-config control)) [])))))

(defn- job-max-messages
  [job fallback]
  (or (runtime-config/parse-positive-int (get-in job [:source :config :maxMessages]))
      (runtime-config/parse-positive-int fallback)
      25))

(defn- discord-last-seen
  [channel-id]
  (get-in @source-state* [:discord :last-seen channel-id]))

(defn- remember-discord-latest!
  [channel-id messages]
  (when-let [latest-id (:id (first messages))]
    (swap! source-state* assoc-in [:discord :last-seen channel-id] latest-id)
    ;; Persistence: Mirror to Redis
    (when-let [client (redis/get-client)]
      (redis/set-key client (str "event-agent:discord-last-seen:" channel-id) latest-id))))

(defn- unseen-discord-messages
  [channel-id messages]
  (let [known-id (discord-last-seen channel-id)]
    (if (str/blank? known-id)
      messages
      (->> messages
           (take-while (fn [message]
                         (not= known-id (:id message))))
           vec))))

(defn- append-recent-event!
  [event]
  (swap! recent-events*
         (fn [events]
           (->> (conj (vec events) event)
                (take-last 30)
                vec))))

(defn- update-job-state!
  [job-id f]
  (let [new-state (swap! job-state* update job-id (fn [current] (f (or current {}))))]
    ;; Persistence: Mirror to Redis
    (when-let [client (redis/get-client)]
      (redis/set-json client (str "event-agent:job-state:" job-id) new-state))
    new-state))

(defn- record-job-run-start!
  [job]
  (let [job-id (:id job)
        started-at (.now js/Date)
        cadence-ms (* 60 1000 (max 1 (or (get-in job [:trigger :cadenceMinutes]) 1)))]
    (update-job-state! job-id
                       (fn [state]
                         (assoc state
                                :id job-id
                                :name (:name job)
                                :enabled (:enabled job)
                                :running true
                                :lastStartedAt started-at
                                :lastStatus "running"
                                :nextRunAt (+ started-at cadence-ms))))
    started-at))

(defn- record-job-run-finish!
  [job started-at status error-message]
  (let [finished-at (.now js/Date)
        job-id (:id job)]
    (update-job-state! job-id
                       (fn [state]
                         (-> state
                             (assoc :id job-id
                                    :name (:name job)
                                    :enabled (:enabled job)
                                    :running false
                                    :lastFinishedAt finished-at
                                    :lastDurationMs (- finished-at started-at)
                                    :lastStatus status)
                             (update :runCount (fnil inc 0))
                             ((fn [next-state]
                                (if error-message
                                  (assoc next-state :lastError error-message)
                                  (dissoc next-state :lastError)))))))
    ;; Telemetry: write result to stdout (captured by PM2/container logs).
    (let [log-line (str "ts=" (.toISOString (js/Date.))
                        " | kind=:agent-job-result | job=" job-id
                        " | status=" status
                        (when error-message (str " | error=" error-message)))]
      (println "[event-agents-telemetry]" log-line)
      nil)))

(defn- direct-start-headers
  [config]
  (let [api-key (:knoxx-api-key config)]
    (cond-> #js {"Content-Type" "application/json"
                 "x-knoxx-user-email" "event-agents@knoxx"}
      (not (str/blank? api-key))
      (aset "X-API-Key" api-key))))

(defn- tool-policies->js
  [policies]
  (clj->js
   (vec
    (for [policy (or policies [])]
      {:toolId (:toolId policy)
       :effect (:effect policy)}))))

(defn- event-summary-text
  [event]
  (let [payload (or (:payload event) {})]
    (str "Event source: " (:sourceKind event) "\n"
         "Event kind: " (:eventKind event) "\n"
         "Event id: " (:id event) "\n"
         "Occurred at: " (:timestamp event) "\n\n"
         (when-let [channel-id (:channelId payload)]
           (str "Channel ID: " channel-id "\n"))
         (when-let [author (:authorUsername payload)]
           (str "Author: " author "\n"))
         (when-let [repository (:repository payload)]
           (str "Repository: " repository "\n"))
         (when-let [content (:content payload)]
           (str "Content: " content "\n"))
         (when-let [summary (:summary payload)]
           (str "Summary: " summary "\n"))
         (when-let [payload-preview (:payloadPreview payload)]
           (str "Payload preview: " payload-preview "\n")))))

(defn- start-agent-run!
  [config job event]
  (let [agent-spec (:agentSpec job)
        now (.now js/Date)
        run-id (str "event-agent-" (:id job) "-" now)
        conversation-id (str "event-agent-" (:id job) "-" (str/lower-case (str (:sourceKind event))) "-" now)
        session-id (str "event-agent-session-" (:id job) "-" now)
        user-message (str "An event matched this job.\n\n"
                          (or (:taskPrompt agent-spec) "")
                          (when-not (str/blank? (or (:taskPrompt agent-spec) "")) "\n\n")
                          (event-summary-text event))
        body #js {:conversation_id conversation-id
                  :session_id session-id
                  :run_id run-id
                  :message user-message
                  :agent_spec #js {:role (or (:role agent-spec) "knowledge_worker")
                                   :system_prompt (or (:systemPrompt agent-spec) "You are a Knoxx event agent.")
                                   :model (or (:model agent-spec) (:proxx-default-model config) "glm-5")
                                   :thinking_level (or (:thinkingLevel agent-spec) "off")
                                   :tool_policies (tool-policies->js (:toolPolicies agent-spec))}
                  :model (or (:model agent-spec) (:proxx-default-model config) "glm-5")}]
    (-> (fetch-json! (str (:knoxx-base-url config) "/api/knoxx/direct/start")
                     #js {:method "POST"
                          :headers (direct-start-headers config)
                          :body (.stringify js/JSON body)})
        (.then (fn [result]
                 (println "[event-agents] queued run" run-id "for job" (:id job) "event" (:eventKind event))
                 result))
        (.catch (fn [err]
                  (println "[event-agents] failed to queue run for job" (:id job) ":" (.-message err))
                  nil)))))

(defn- matches-event-kind?
  [job event-kind]
  (let [configured (vec (or (get-in job [:trigger :eventKinds]) []))]
    (or (empty? configured)
        (some #(= (str %) (str event-kind)) configured))))

(defn- matches-repository?
  [job repository]
  (let [allowlist (->> (or (get-in job [:filters :repositories]) [])
                       (map (fn [value] (some-> value str str/trim not-empty)))
                       (remove nil?)
                       vec)]
    (or (empty? allowlist)
        (some #(= % repository) allowlist))))

(defn- matches-channel?
  [control job channel-id]
  (let [channels (job-channels control job)]
    (or (empty? channels)
        (some #(= % channel-id) channels))))

(defn- matches-keywords?
  [control job content]
  (let [keywords (job-keywords control job)
        lowered (str/lower-case (str (or content "")))]
    (or (empty? keywords)
        (some #(str/includes? lowered %) keywords))))

(defn- job-matches-event?
  [control job event]
  (let [payload (or (:payload event) {})]
    (and (:enabled job)
         (= "event" (get-in job [:trigger :kind]))
         (= (str (get-in job [:source :kind])) (str (:sourceKind event)))
         (matches-event-kind? job (:eventKind event))
         (matches-channel? control job (:channelId payload))
         (matches-keywords? control job (:content payload))
         (matches-repository? job (:repository payload)))))

(defn dispatch-event!
  [event]
  (let [config (cfg)
        control (control-config config)
        normalized-event (merge {:id (str "event-" (.now js/Date))
                                 :timestamp (.toISOString (js/Date.))
                                 :sourceKind "manual"
                                 :eventKind "manual.event"
                                 :payload {}}
                                (or event {}))
        matching-jobs (->> (:jobs control)
                           (filter #(job-matches-event? control % normalized-event))
                           vec)]
    (append-recent-event! normalized-event)
    (if (empty? matching-jobs)
      (js/Promise.resolve {:matchedJobs []
                           :event normalized-event})
      (-> (js/Promise.all
           (clj->js
            (mapv (fn [job]
                    (let [started-at (record-job-run-start! job)]
                      (-> (start-agent-run! config job normalized-event)
                          (.then (fn [result]
                                   (record-job-run-finish! job started-at "ok" nil)
                                   result))
                          (.catch (fn [err]
                                    (record-job-run-finish! job started-at "error" (.-message err))
                                    nil)))))
                  matching-jobs)))
          (.then (fn [_]
                   {:matchedJobs (mapv :id matching-jobs)
                    :event normalized-event}))))))

(defn- discord-bot-user-id
  [control]
  (some-> (get-in control [:sources :discord :botUserId]) str str/trim not-empty))

(defn- discord-event-jobs
  [control]
  (->> (:jobs control)
       (filter (fn [job]
                 (and (:enabled job)
                      (= "event" (get-in job [:trigger :kind]))
                      (= "discord" (get-in job [:source :kind])))))
       vec))

(defn- discord-union-keywords
  [control]
  (->> (discord-event-jobs control)
       (mapcat #(job-keywords control %))
       distinct
       vec))

(defn- dispatch-discord-gateway-message!
  [message]
  (let [config (cfg)
        control (control-config config)
        content (str/lower-case (str (:content message) ""))
        bot-user-id (discord-bot-user-id control)
        mention? (and bot-user-id
                      (or (str/includes? content (str "<@" bot-user-id ">"))
                          (str/includes? content (str "<@!" bot-user-id ">"))))
        keyword? (some #(str/includes? content %) (discord-union-keywords control))
        payload {:channelId (:channelId message)
                 :authorId (:authorId message)
                 :authorUsername (:authorUsername message)
                 :authorIsBot (:authorIsBot message)
                 :content (:content message)
                 :messageId (:id message)}]
    (when-not (:authorIsBot message)
      (remember-discord-latest! (:channelId message) [message])
      (dispatch-event! {:sourceKind "discord"
                        :eventKind "discord.message.created"
                        :payload payload})
      (when mention?
        (dispatch-event! {:sourceKind "discord"
                          :eventKind "discord.message.mention"
                          :payload payload}))
      (when keyword?
        (dispatch-event! {:sourceKind "discord"
                          :eventKind "discord.message.keyword"
                          :payload payload})))))

(defn- bind-discord-gateway!
  [config]
  (when-let [manager (discord-gateway-manager)]
    (let [token (some-> (:discord-bot-token config) str str/trim)]
      (when-not (str/blank? token)
        (-> (.start manager token)
            (.catch (fn [err]
                      (println "[event-agents] discord gateway start failed:" (.-message err))
                      nil))))
      (when-let [unsubscribe @discord-gateway-unsubscribe*]
        (unsubscribe)
        (reset! discord-gateway-unsubscribe* nil))
      (reset! discord-gateway-unsubscribe*
              (.onMessage manager (fn [mapped _raw]
                                    (dispatch-discord-gateway-message! (js->clj mapped :keywordize-keys true))))))))

(defn- dispatch-discord-message-event!
  [control job message match-kind]
  (dispatch-event! {:sourceKind "discord"
                    :eventKind match-kind
                    :payload {:channelId (:channelId message)
                              :authorId (:authorId message)
                              :authorUsername (:authorUsername message)
                              :authorIsBot (:authorIsBot message)
                              :content (:content message)
                              :messageId (:id message)}}))

(defn- discord-message-match-kind
  [control job message]
  (let [bot-user-id (discord-bot-user-id control)
        content (str/lower-case (str (:content message) ""))
        mention? (and bot-user-id
                      (or (str/includes? content (str "<@" bot-user-id ">"))
                          (str/includes? content (str "<@!" bot-user-id ">"))))
        keyword? (matches-keywords? control job (:content message))]
    (cond
      mention? "discord.message.mention"
      keyword? "discord.message.keyword"
      :else nil)))

(defn- execute-discord-patrol!
  [config control job]
  (let [channels (job-channels control job)
        limit (job-max-messages job 25)]
    (if (seq channels)
      (js/Promise.all
       (clj->js
        (mapv (fn [channel-id]
                (-> (read-discord-channel! channel-id limit)
                    (.then (fn [messages]
                             (let [fresh (unseen-discord-messages channel-id messages)]
                               (doseq [message fresh]
                                 (when-let [match-kind (discord-message-match-kind control job message)]
                                   (dispatch-discord-message-event! control job message match-kind)))
                               (remember-discord-latest! channel-id messages)
                               {:channelId channel-id
                                :fetched (count messages)
                                :fresh (count fresh)})))
                    (.catch (fn [err]
                              (println "[event-agents] discord patrol failed for" channel-id ":" (.-message err))
                              {:channelId channel-id
                               :error true}))))
              channels)))
      (js/Promise.resolve nil))))

(defn- summarize-discord-channel
  [channel-id messages]
  (->> messages
       (remove :authorIsBot)
       (take 8)
       (map (fn [message]
              (str "[" channel-id "] <" (:authorUsername message) "> "
                   (subs (:content message) 0 (min 180 (count (:content message)))))))
       (str/join "\n")))

(defn- execute-discord-synthesis!
  [config control job]
  (let [channels (job-channels control job)
        limit (job-max-messages job 12)]
    (if (seq channels)
      (-> (js/Promise.all
           (clj->js
            (mapv (fn [channel-id]
                    (-> (read-discord-channel! channel-id limit)
                        (.then (fn [messages]
                                 {:channelId channel-id
                                  :messages messages}))
                        (.catch (fn [_]
                                  {:channelId channel-id
                                   :messages []}))))
                  channels)))
          (.then (fn [results]
                   (let [rows (js->clj results :keywordize-keys true)
                         summary (->> rows
                                      (map (fn [{:keys [channelId messages]}]
                                             (summarize-discord-channel channelId messages)))
                                      (remove str/blank?)
                                      (str/join "\n\n"))]
                     (if (str/blank? summary)
                       (js/Promise.resolve nil)
                       (start-agent-run!
                        config
                        job
                        {:sourceKind "discord"
                         :eventKind "discord.snapshot.summary"
                         :timestamp (.toISOString (js/Date.))
                         :payload {:summary summary
                                   :channelId (first channels)}}))))))
      (js/Promise.resolve nil))))

(defn- execute-direct-job!
  [config job source-kind event-kind]
  (start-agent-run!
   config
   job
   {:sourceKind source-kind
    :eventKind event-kind
    :timestamp (.toISOString (js/Date.))
    :payload {:payloadPreview (str "Synthetic trigger for job " (:id job))}}))

(defn- execute-cron-job!
  [config job]
  (let [control (control-config config)
        source-kind (get-in job [:source :kind])
        mode (get-in job [:source :mode])]
    (cond
      (and (= source-kind "discord") (= mode "patrol"))
      (execute-discord-patrol! config control job)

      (and (= source-kind "discord") (= mode "synthesize"))
      (execute-discord-synthesis! config control job)

      :else
      (execute-direct-job! config job source-kind "cron.tick"))))

(defn run-job!
  [job-id]
  (let [config (cfg)
        control (control-config config)
        job (some (fn [candidate] (when (= (:id candidate) job-id) candidate)) (:jobs control))]
    (if-not job
      (js/Promise.reject (js/Error. (str "Unknown event-agent job: " job-id)))
      (let [started-at (record-job-run-start! job)]
        (-> (js/Promise.resolve
             (if (= "cron" (get-in job [:trigger :kind]))
               (execute-cron-job! config job)
               (execute-direct-job! config job (get-in job [:source :kind]) "manual.run")))
            (.then (fn [result]
                     (record-job-run-finish! job started-at "ok" nil)
                     result))
            (.catch (fn [err]
                      (record-job-run-finish! job started-at "error" (.-message err))
                      nil)))))))

(defn- clear-interval-task!
  [task]
  (when-let [id (:id task)]
    (js/clearInterval id)))

(defn stop!
  []
  (when-let [unsubscribe @discord-gateway-unsubscribe*]
    (unsubscribe)
    (reset! discord-gateway-unsubscribe* nil))
  (doseq [[_ task] @scheduled-tasks*]
    (when (and task (map? task) (= :interval (:type task)))
      (clear-interval-task! task)))
  (reset! scheduled-tasks* {})
  (reset! running?* false)
  (println "[event-agents] stopped"))

(defn- cadence-label
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
     :configured true
     :sources {:discord {:lastSeenChannels (-> @source-state* :discord :last-seen keys vec)}
               :recentEvents @recent-events*}
     :jobs (mapv (fn [job]
                   (merge {:id (:id job)
                           :name (:name job)
                           :enabled (:enabled job)
                           :trigger (:trigger job)
                           :source (:source job)
                           :scheduleLabel (cadence-label (get-in job [:trigger :cadenceMinutes]))}
                          (get @job-state* (:id job) {:runCount 0 :lastStatus "none"})))
                 (:jobs control))}))

(defn- schedule-job!
  [config job]
  (let [every-ms (* 60 1000 (max 1 (get-in job [:trigger :cadenceMinutes])))
        wrapped (fn []
                  (when @running?*
                    (run-job! (:id job))
                    nil))
        id (js/setInterval wrapped every-ms)]
    (swap! scheduled-tasks* assoc (:id job) {:type :interval
                                             :id id
                                             :everyMs every-ms})
    (update-job-state! (:id job)
                       (fn [state]
                         (merge state
                                {:id (:id job)
                                 :name (:name job)
                                 :enabled (:enabled job)
                                 :nextRunAt (+ (.now js/Date) every-ms)})))))

(defn reload!
  []
  (stop!)
  (start! nil))

(defn start!
  [_config]
  (when-not @running?*
    (let [config (cfg)
          control (control-config config)]
      (reset! running?* true)
      (println "[event-agents] starting with" (count (:jobs control)) "jobs")

      ;; Persistence: recover states from Redis
      (when-let [client (redis/get-client)]
        (println "[event-agents] recovering state from Redis...")
        (let [job-ids (map :id (:jobs control))]
          (doseq [id job-ids]
            (-> (redis/get-json client (str "event-agent:job-state:" id))
                (.then (fn [state]
                         (when state
                           (swap! job-state* assoc id state))))))
          (doseq [channel-id (or (:defaultChannels (discord-source-config control)) [])]
            (-> (redis/get-key client (str "event-agent:discord-last-seen:" channel-id))
                (.then (fn [last-id]
                         (when last-id
                           (swap! source-state* assoc-in [:discord :last-seen channel-id] last-id))))))))

      (bind-discord-gateway! config)

      ;; Schedule cron jobs
      (doseq [job (:jobs control)]
        (when (and (:enabled job)
                   (= "cron" (get-in job [:trigger :kind])))
          (schedule-job! config job)))

      ;; Kick one job immediately so boot doesn't wait for the first cron tick.
      (when-let [first-cron-job (some (fn [job]
                                        (when (and (:enabled job)
                                                   (= "cron" (get-in job [:trigger :kind])))
                                          job))
                                      (:jobs control))]
        (run-job! (:id first-cron-job))))))

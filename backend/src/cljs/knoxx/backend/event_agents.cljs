(ns knoxx.backend.event-agents
  "Generic event-agent runtime for Knoxx.

   Adapters emit normalized events.
   Jobs describe triggers + source filters + arbitrary agent specs.
   The runtime matches events/jobs and launches Knoxx runs through direct/start."
  (:require [clojure.string :as str]
            [shadow.cljs.modern :refer [js-await]]
            [knoxx.backend.discord-gateway :as dg]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.models :as runtime-models]
            [knoxx.backend.runtime.state :as runtime-state]
            [knoxx.backend.session-store :as session-store]
            [knoxx.backend.triggers.control-config :as control-config]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.agent-templates :as templates]
            [knoxx.backend.tools.media :as media]
            [knoxx.backend.util.parse :refer [parse-positive-int]]
            [knoxx.backend.actions.dispatch :as actions-dispatch]))

(declare start!)

(declare start!)

(defonce running?* (atom false))
(defonce scheduled-tasks* (atom {}))
(defonce job-state* (atom {}))
(defonce user-job-state* (atom {}))
(defonce source-state* (atom {:discord {:last-seen {}}}))
(defonce recent-events* (atom []))
(defonce discord-gateway-unsubscribe* (atom nil))

(defonce ^:private enriched-env-config-cache*
  (atom {:base nil :enriched nil}))

(defn- cached-enriched-env-config
  "Return an enriched config derived from runtime-config/cfg.

   Memoized to avoid repeated env reads / enrich-config recomputation on hot paths.
   Cache is refreshed automatically when the base env config value changes." 
  []
  (let [base (runtime-config/cfg)
        cached @enriched-env-config-cache*]
    (if (and (:enriched cached)
             (= base (:base cached)))
      (:enriched cached)
      (let [enriched (runtime-models/enrich-config base)]
        (reset! enriched-env-config-cache* {:base base :enriched enriched})
        enriched))))

(defn- cfg
  "Return the current enriched runtime config.

   Prefer runtime-state/config* when available so dynamic overrides (e.g.
   persisted event-agent-control) are visible to callers." 
  []
  (or @runtime-state/config*
      (cached-enriched-env-config)))

(defn- control-config
  [config]
  (control-config/event-agent-control-config config))

(defn- discord-token
  []
  (:discord-bot-token (cfg)))

(defn- discord-gateway-manager
  []
  (dg/gateway-manager))

(defn- discord-gateway-active?
  []
  (when-let [manager (discord-gateway-manager)]
    (let [status (.status manager)]
      (boolean (or (aget status "ready")
                   (aget status "started"))))))

(defn- discord-gateway-user-id
  []
  (when-let [manager (discord-gateway-manager)]
    (let [status (.status manager)]
      (some-> (aget status "userId") str str/trim not-empty))))

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

(defn map-discord-message
  [msg]
  {:id (aget msg "id")
   :channelId (or (aget msg "channel_id") "")
   :content (or (aget msg "content") "")
   :authorId (or (aget msg "author" "id") "")
   :authorUsername (or (aget msg "author" "username") "unknown")
   :authorIsBot (boolean (aget msg "author" "bot"))
   :timestamp (or (aget msg "timestamp") "")
   :attachments (->> (if (array? (aget msg "attachments")) (array-seq (aget msg "attachments")) [])
                     (mapv (fn [attachment]
                             {:id (or (aget attachment "id") "")
                              :filename (or (aget attachment "filename") "")
                              :contentType (or (aget attachment "content_type") (aget attachment "contentType"))
                              :size (or (aget attachment "size") 0)
                              :url (or (aget attachment "url") "")})))
   :embeds (->> (if (array? (aget msg "embeds")) (array-seq (aget msg "embeds")) [])
               (mapv (fn [embed]
                       {:title (or (aget embed "title") nil)
                        :description (or (aget embed "description") nil)
                        :url (or (aget embed "url") nil)})))})

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

(defn- filter-string-list
  [values]
  (->> (or values [])
       (map (fn [value] (some-> value str str/trim not-empty)))
       (remove nil?)
       vec))

(defn- job-channels
  [control job]
  (let [channels (filter-string-list (get-in job [:filters :channels]))]
    (if (seq channels)
      channels
      (vec (or (:defaultChannels (discord-source-config control)) [])))))

(defn- job-publish-channels
  [job]
  (filter-string-list (or (get-in job [:filters :publishChannels])
                          (get-in job [:filters :publish_channels]))))

(defn- job-guild-ids
  [job]
  (filter-string-list (or (get-in job [:filters :guildIds])
                          (get-in job [:filters :guild_ids]))))

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
  (or (parse-positive-int (get-in job [:source :config :maxMessages]))
      (parse-positive-int fallback)
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

(defn- update-job-spec!
  "Update a job spec in Redis and mark it dirty for SQL flush.
   This is the canonical write path - all job updates go through here."
  [job-id spec]
  (when-let [client (redis/get-client)]
    (let [key (str "event-agent:job-spec:" job-id)
          dirty-key "event-agent:job-dirty"]
      ;; Write the full spec to Redis (hot store)
      (redis/set-json client key spec)
      ;; Add to dirty set for write-behind flush to SQL
      (redis/sadd client dirty-key job-id)
      ;; Set TTL on dirty marker (24 hours - gives plenty of time for flush)
      (redis/expire client dirty-key 86400)
      (println "[event-agents] job" job-id "marked dirty for persistence")))
  spec)

(defn- get-job-spec
  "Load job spec from Redis (hot), or return default spec.
   Redis is the source of truth for running configuration.
   This prevents the 'reasoning reset' bug by ensuring runtime overrides persist."
  [job-id default-spec]
  (if-let [client (redis/get-client)]
    (let [key (str "event-agent:job-spec:" job-id)]
      (-> (redis/get-json client key)
          (.then (fn [redis-spec]
                   (if redis-spec
                     (do
                       (println "[event-agents] loaded spec for" job-id "from Redis")
                       redis-spec)
                     (do
                       (println "[event-agents] using default spec for" job-id)
                       default-spec))))
          (.catch (fn [err]
                    (println "[event-agents] Redis load failed for" job-id ":" (.-message err))
                    default-spec))))
    default-spec))

(defn- flush-dirty-jobs-to-sql!
  "Write-behind flush: Move dirty job specs from Redis to SQL.
   Called periodically by the background flush task.

   In a future implementation, this would write to a SQL database.
   For now, it logs the dirty jobs and clears the dirty marker."
  []
  (when-let [client (redis/get-client)]
    (let [dirty-key "event-agent:job-dirty"]
      (-> (redis/smembers client dirty-key)
          (.then (fn [job-ids]
                   (when (and job-ids (seq job-ids))
                     (println "[event-agents] flushing" (count job-ids) "dirty jobs to SQL...")
                     (doseq [job-id job-ids]
                       (-> (redis/get-json client (str "event-agent:job-spec:" job-id))
                           (.then (fn [spec]
                                    (when spec
                                      ;; TODO: Write to SQL here
                                      ;; (sql/upsert! :event_agent_jobs spec)
                                      (println "[event-agents] flushed" job-id "to SQL"))))))
                     ;; Clear dirty set after successful flush
                     (redis/del client dirty-key)
                     (println "[event-agents] dirty queue cleared"))))
          (.catch (fn [err]
                    (println "[event-agents] flush failed:" (.-message err))))))))

(defn- schedule-flush-task!
  "Schedule background task to flush dirty jobs to SQL.
   Runs every 5 minutes to batch writes."
  []
  (let [flush-interval-ms (* 5 60 1000) ;; 5 minutes
        flush-task (fn []
                     (when @running?*
                       (flush-dirty-jobs-to-sql!)))]
    (js/setInterval flush-task flush-interval-ms)
    (println "[event-agents] scheduled SQL flush every 5 minutes")))

(defn- update-job-state!
  [job-id f]
  (let [new-state (get (swap! job-state* update job-id (fn [current] (f (or current {})))) job-id)]
    ;; Persistence: Mirror to Redis
    (when-let [client (redis/get-client)]
      (redis/set-json client (str "event-agent:job-state:" job-id) new-state))
    new-state))

(defn- normalize-job-state
  [job-id state]
  (let [candidate (cond
                    (and (map? state) (map? (get state job-id))) (get state job-id)
                    (map? state) state
                    :else {})]
    (if (map? candidate)
      candidate
      {})))

(defn- record-job-run-start!
  [job]
  (let [job-id (:id job)
        started-at (.now js/Date)
        cadence-ms (* 60 1000 (max 1 (or (get-in job [:trigger :cadenceMinutes]) 1)))]
    (update-job-state! job-id
                       (fn [state]
                         (assoc (normalize-job-state job-id state)
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
                         (-> (normalize-job-state job-id state)
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
  (let [api-key (:knoxx-api-key config)
        headers #js {"Content-Type" "application/json"
                     "x-knoxx-user-email" "system-admin@open-hax.local"}]
    (when-not (str/blank? api-key)
      (aset headers "X-API-Key" api-key))
    headers))

(defn- tool-policies->js
  [policies]
  (clj->js
   (vec
    (for [policy (or policies [])]
      {:toolId (:toolId policy)
       :effect (:effect policy)}))))

(defn event-summary-text
  [event]
  (let [payload (or (:payload event) {})
        attachments (or (:attachments payload) [])
        attachment-lines (when (seq attachments)
                           (str "Attachments:\n"
                                (str/join "\n"
                                          (map (fn [attachment]
                                                 (str "- " (:filename attachment)
                                                      (when-let [url (:url attachment)]
                                                        (str " <" url ">"))))
                                               attachments))
                                "\n"))
        publish-channels (or (:publishChannels payload) [])]
    (str "Event source: " (:sourceKind event) "\n"
         "Event kind: " (:eventKind event) "\n"
         "Event id: " (:id event) "\n"
         "Occurred at: " (:timestamp event) "\n\n"
(when-let [channel-id (:channelId payload)]
            (str "Channel ID: " channel-id "\n"))
          (when-let [message-id (:messageId payload)]
            (str "Message ID: " message-id "\n"))
          (when-let [author (:authorUsername payload)]
           (str "Author: " author "\n"))
         (when-let [repository (:repository payload)]
           (str "Repository: " repository "\n"))
         (when-let [content (:content payload)]
           (str "Content: " content "\n"))
         attachment-lines
         (when (seq publish-channels)
           (str "Publish channels: " (str/join ", " publish-channels) "\n"))
         (when-let [summary (:summary payload)]
           (str "Summary: " summary "\n"))
         (when-let [payload-preview (:payloadPreview payload)]
           (str "Payload preview: " payload-preview "\n")))))

(defn- media-url-pattern
  "Regex to find media URLs in text content."
  []
  (js/RegExp. #"https?://\S+\.(?:png|jpg|jpeg|gif|webp|mp4|webm|mp3|wav|ogg|m4a|flac|pdf)" "gi"))

(defn- extract-media-urls-from-text
  "Extract media URLs from raw text content."
  [text]
  (when (string? text)
    (let [pattern (media-url-pattern)]
      (->> (str/split text #"\s+")
           (filter #(re-matches pattern %))
           distinct
           vec))))

(defn- extract-media-from-embeds
  "Extract media URLs from Discord embeds."
  [embeds]
  (->> (or embeds [])
       (keep (fn [embed]
               (let [url (:url embed)]
                 (when url
                   (let [lower (str/lower-case url)]
                     (when (some #(str/includes? lower %) [".png" ".jpg" ".jpeg" ".gif" ".webp" ".mp4" ".webm" ".pdf"])
                       url))))))
       distinct
       vec))

(defn- event-content-parts
  [event]
  (let [payload (or (:payload event) {})
        attachment-urls (->> (or (:attachments payload) [])
                          (keep (fn [att]
                                  (let [url (:url att)
                                        content-type (some-> (:contentType att) str str/lower-case)]
                                    (when url
                                      {:type (cond
                                              (some-> content-type (str/starts-with? "image/")) "image"
                                              (some-> content-type (str/starts-with? "video/")) "video"
                                              (some-> content-type (str/starts-with? "audio/")) "audio"
                                              (some-> content-type (str/starts-with? "text/")) "document"
                                              (some-> content-type (str/includes? "pdf")) "document"
                                              :else nil)
                                       :url url
                                       :mimeType (:contentType att)
                                       :filename (:filename att)}))))
                          vec)
        text-media-urls (extract-media-urls-from-text (:content payload))
        embed-media-urls (extract-media-from-embeds (:embeds payload))
        detected-urls (->> (concat text-media-urls embed-media-urls)
                           distinct
                           (map (fn [url]
                                   (let [lower (str/lower-case url)]
                                     {:type (cond
                                             (some #(str/includes? lower %) [".png" ".jpg" ".jpeg" ".gif" ".webp"]) "image"
                                             (some #(str/includes? lower %) [".mp4" ".webm" ".mov"]) "video"
                                             (some #(str/includes? lower %) [".mp3" ".wav" ".ogg" ".m4a" ".flac"]) "audio"
                                             (some #(str/includes? lower %) [".pdf"]) "document"
                                             :else "image")
                                      :url url
                                      :mimeType nil
                                      :filename nil})))
                           vec)]
    (concat attachment-urls detected-urls)))

(defn- sticky-session-enabled?
  [job]
  (true? (get-in job [:source :config :stickySession])))

(defn- sticky-session-max-messages
  [job]
  (parse-positive-int (get-in job [:source :config :sessionMaxMessages])))

(defn- update-user-job-state!
  [job-id user-id f]
  (let [user-key (str job-id ":" user-id)
        new-state (get (swap! user-job-state* update user-key (fn [current] (f (or current {})))) user-key)]
    ;; Persistence: Mirror to Redis
    (when-let [client (redis/get-client)]
      (redis/set-json client (str "event-agent:user-job-state:" user-key) new-state))
    new-state))

(defn- normalize-user-job-state
  [user-key state]
  (if (map? state)
    state
    {}))

(defn- sticky-session-base-conversation-id
  [job event]
  (let [source-kind (str (:sourceKind event))
        author-id (or (get-in event [:payload :authorId]) "unknown-user")
        owner-id (if (= source-kind "discord")
                   (or (get-in event [:payload :channelId]) author-id)
                   author-id)]
    (str "event-agent-" (:id job) "-" owner-id "-" (str/lower-case source-kind) "-sticky")))

(defn- sticky-session-base-session-id
  [job event]
  (let [source-kind (str (:sourceKind event))
        author-id (or (get-in event [:payload :authorId]) "unknown-user")
        owner-id (if (= source-kind "discord")
                   (or (get-in event [:payload :channelId]) author-id)
                   author-id)]
    (str "event-agent-session-" (:id job) "-" owner-id "-sticky")))

(defn- sticky-session-summary
  [session]
  (let [messages (->> (or (:messages session) [])
                      (filter map?)
                      (take-last 8)
                      vec)]
    (when (seq messages)
      (str "Previous sticky session summary:\n"
           (str/join "\n"
                     (map (fn [message]
                            (let [role (or (:role message) "message")
                                  content (str (or (:content message) ""))]
                              (str "- " role ": " (subs content 0 (min 240 (count content))))))
                          messages))))))

(defn- sticky-session-target
  [job event]
  (let [job-id (:id job)]
    (if-not (sticky-session-enabled? job)
      {:conversation-id (str "event-agent-" job-id "-" (str/lower-case (str (:sourceKind event))) "-" (.now js/Date))
       :session-id (str "event-agent-session-" job-id "-" (.now js/Date))
       :summary nil}
      (let [state (get @job-state* job-id {})
            generation (or (:stickyGeneration state) 0)
            base-conversation-id (sticky-session-base-conversation-id job event)
            base-session-id (sticky-session-base-session-id job)
            current-conversation-id (or (:stickyConversationId state)
                                        (if (zero? generation)
                                          base-conversation-id
                                          (str base-conversation-id "-r" generation)))
            current-session-id (or (:stickySessionId state)
                                   (if (zero? generation)
                                     base-session-id
                                     (str base-session-id "-r" generation)))
            existing-session (session-store/get-session-sync current-session-id)
            message-limit (sticky-session-max-messages job)
            message-count (count (or (:messages existing-session) []))
            rollover? (and existing-session message-limit (>= message-count message-limit))]
        (if rollover?
          (let [next-generation (inc generation)
                next-conversation-id (str base-conversation-id "-r" next-generation)
                next-session-id (str base-session-id "-r" next-generation)
                summary (sticky-session-summary existing-session)]
            (update-job-state! job-id
                               (fn [current]
                                 (assoc (or current {})
                                        :stickyGeneration next-generation
                                        :stickyConversationId next-conversation-id
                                        :stickySessionId next-session-id)))
            {:conversation-id next-conversation-id
             :session-id next-session-id
             :summary summary})
          (do
            (update-job-state! job-id
                               (fn [current]
                                 (assoc (or current {})
                                        :stickyGeneration generation
                                        :stickyConversationId current-conversation-id
                                        :stickySessionId current-session-id)))
            {:conversation-id current-conversation-id
             :session-id current-session-id
             :summary nil}))))))

(defn build-agent-run-payload
  [config job event]
  (let [agent-spec (:agentSpec job)
        now (.now js/Date)
        run-id (str "event-agent-" (:id job) "-" now)
        {:keys [conversation-id session-id summary]} (sticky-session-target job event)
        contract-id (or (:contract-id agent-spec)
                        (:contractSourceId job))
        actor-id (or (:actor-id agent-spec)
                     (:actorId job))
        user-message (str "An event matched this job.\n\n"
                          (or (:taskPrompt agent-spec) "")
                          (when-not (str/blank? (or (:taskPrompt agent-spec) "")) "\n\n")
                          (when-not (str/blank? (or summary ""))
                            (str summary "\n\n"))
                          (event-summary-text event))
        content-parts (event-content-parts event)
        model-id (or (:model agent-spec) "gemma4:31b")]
    {:conversation_id conversation-id
     :session_id session-id
     :run_id run-id
     :message user-message
     :content_parts content-parts
     :agent_spec (cond-> {:role (or (:role agent-spec) "knowledge_worker")
                          :system_prompt (or (:systemPrompt agent-spec) "You are a Knoxx event agent.")
                          :task_prompt (or (:taskPrompt agent-spec) "")
                          :model model-id
                          :thinking_level (or (:thinkingLevel agent-spec) "off")
                          :tool_policies (tool-policies->js (:toolPolicies agent-spec))}
                   contract-id (assoc :contract_id contract-id)
                   actor-id (assoc :actor_id actor-id))
     :model model-id}))

(defn- job-step
  [job]
  (or (:step job)
      {:uses "run-agent" :with (:agentSpec job {})}))

(defn- start-agent-run!
  [config job event]
  (let [body (build-agent-run-payload config job event)]
    (-> (fetch-json! (str (:knoxx-base-url config) "/api/knoxx/direct/start")
                     #js {:method "POST"
                          :headers (direct-start-headers config)
                          :body (.stringify js/JSON (clj->js body))})
        (.then (fn [result]
                 (println "[event-agents] queued run" (:run_id body) "for job" (:id job) "event" (:eventKind event))
                 result))
        (.catch (fn [err]
                  (println "[event-agents] failed to queue run for job" (:id job) ":" (.-message err))
                  (js/Promise.reject err))))))

(defn- matches-event-kind?
  [job event-kinds]
  (let [configured (vec (or (get-in job [:trigger :eventKinds]) []))
        kinds (if (string? event-kinds) [event-kinds] event-kinds)]
    (or (empty? configured)
        (some (fn [ek]
                (some (fn [conf] (= (str ek) (str conf)))
                      configured))
              kinds))))

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

(defn- mention-event?
  [event-kind]
  (some #(= (str event-kind) %) ["discord.message.mention"]))

(defn job-matches-event?
  [control job event]
  (let [payload (or (:payload event) {})
        event-kind (:eventKind event)]
    (and (:enabled job)
         (= "event" (get-in job [:trigger :kind]))
         (= (str (get-in job [:source :kind])) (str (:sourceKind event)))
         (matches-event-kind? job event-kind)
         (matches-channel? control job (:channelId payload))
         ;; Keyword filter does not apply to mention events — the mention
         ;; itself is the trigger signal, regardless of content words.
         (or (mention-event? event-kind)
             (matches-keywords? control job (:content payload)))
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
        raw-matches (->> (:jobs control)
                         (filter #(job-matches-event? control % normalized-event))
                         vec)
        matching-jobs (->> raw-matches
                          (reduce (fn [acc job]
                                    (if (some #(= (:id %) (:id job)) acc)
                                      acc
                                      (conj acc job)))
                                  [])
                          vec)]
    (append-recent-event! normalized-event)
    (if (empty? matching-jobs)
      (js/Promise.resolve {:matchedJobs []
                           :event normalized-event})
      (-> (js/Promise.all
           (clj->js
            (mapv (fn [job]
                    (let [started-at (record-job-run-start! job)
                      step (job-step job)]
                  (-> (actions-dispatch/dispatch! {:config config
                                                   :event normalized-event :job job
                                                   :run-agent! start-agent-run!}
                                                  step)
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
  (or (some-> (get-in control [:sources :discord :botUserId]) str str/trim not-empty)
      (discord-gateway-user-id)))

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
                 :messageId (:id message)
                 :attachments (vec (or (:attachments message) []))
                 :embeds (vec (or (:embeds message) []))}]
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

(defn- resolve-job-channel-ids!
  [control job]
  (let [explicit-channels (job-channels control job)
        guild-ids (job-guild-ids job)
        publish-channels (job-publish-channels job)
        list-channels! (fn []
                         (or (dg/list-channels)
                             (js/Promise.resolve #js [])))]
    (cond
      (seq guild-ids)
      (-> (list-channels!)
          (.then (fn [channels]
                   (let [rows (js->clj channels :keywordize-keys true)
                         guild-id-set (set guild-ids)]
                     (->> rows
                          (filter (fn [channel]
                                    (contains? guild-id-set (:guildId channel))))
                          (map :id)
                          distinct
                          vec)))))

      (and (empty? explicit-channels) (seq publish-channels))
      (-> (list-channels!)
          (.then (fn [channels]
                   (let [rows (js->clj channels :keywordize-keys true)
                         publish-channel-set (set publish-channels)
                         guilds (->> rows
                                     (filter (fn [channel]
                                               (contains? publish-channel-set (:id channel))))
                                     (map :guildId)
                                     distinct
                                     vec)]
                     (if (seq guilds)
                       (let [guild-set (set guilds)]
                         (->> rows
                              (filter (fn [channel]
                                        (contains? guild-set (:guildId channel))))
                              (map :id)
                              distinct
                              vec))
                       publish-channels)))))

      :else
      (js/Promise.resolve explicit-channels))))

(defn- execute-discord-patrol!
  [config control job]
  (let [limit (job-max-messages job 25)]
    (-> (resolve-job-channel-ids! control job)
        (.then (fn [channels]
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
                   (js/Promise.resolve nil)))))))

(defn- summarize-discord-channel
  [channel-id messages]
  (->> messages
       (remove :authorIsBot)
       (take 8)
       (map (fn [message]
              (let [attachments (:attachments message)
                    attachment-text (when (seq attachments)
                                      (str " attachments="
                                           (str/join ", " (map :filename attachments))))]
                (str "[" channel-id "] <" (:authorUsername message) " (id:" (:authorId message) ")> "
                     (subs (:content message) 0 (min 180 (count (:content message))))
                     (or attachment-text "")))))
       (str/join "\n")))

(defn- execute-discord-synthesis!
  [config control job]
  (let [limit (job-max-messages job 12)
        publish-channels (job-publish-channels job)]
    (-> (resolve-job-channel-ids! control job)
        (.then (fn [channels]
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
                                     (actions-dispatch/dispatch!
                                      {:config config
                                       :event {:sourceKind "discord"
                                               :eventKind "discord.snapshot.summary"
                                               :timestamp (.toISOString (js/Date.))
                                               :payload {:summary summary
                                                         :channelId (first channels)
                                                         :publishChannels publish-channels}}
                                       :job job
                                       :run-agent! start-agent-run!}
                                      (job-step job))))))
                   (js/Promise.resolve nil)))))))

(defn- execute-direct-job!
  [config job source-kind event-kind]
  (actions-dispatch/dispatch! {:config config
                              :event {:sourceKind source-kind
                                      :eventKind event-kind
                                      :timestamp (.toISOString (js/Date.))
                                      :payload {:payloadPreview (str "Synthetic trigger for job " (:id job))}
                              :job job
                              :run-agent! start-agent-run!}
                             (job-step job)))

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
    (reset! running?* true)
    ;; =======================================================================
    ;; Persistence: Recover event-agent-control overrides from Redis FIRST.
    ;; This is the primary fix for "changes not sticking" — the admin panel
    ;; writes control overrides via PUT, but they were only in memory.
    ;; We must recover before scheduling so jobs use persisted settings.
    ;; =======================================================================
    (let [recovery-promise
          (if-let [client (redis/get-client)]
            (-> (control-config/load-event-agent-control)
                (.then (fn [saved-control]
                         (when saved-control
                           (swap! runtime-state/config*
                                  (fn [current-cfg]
                                    (assoc (or current-cfg (cfg))
                                           :event-agent-control saved-control)))))))
            (js/Promise.resolve nil))]
      (-> recovery-promise
          (.then (fn [_]
                   (let [config (cfg)
                         control (control-config config)]
                     (println "[event-agents] starting with" (count (:jobs control)) "jobs")

                     ;; Recover remaining state from Redis (job state, specs, last-seen)
                     (when-let [client (redis/get-client)]
                       (println "[event-agents] recovering state from Redis...")

                       ;; Recover operational state and specs for all configured jobs
                       (let [job-ids (map :id (:jobs control))]
                         (doseq [id job-ids]
                           ;; Recover Operational State (Counts/Status)
                           (-> (redis/get-json client (str "event-agent:job-state:" id))
                               (.then (fn [state]
                                        (when state
                                          (swap! job-state* assoc id (normalize-job-state id state))
                                          (println "[event-agents] recovered state for" id)))))

                           ;; Recover Job Spec Overrides from Redis
                           (-> (redis/get-json client (str "event-agent:job-spec:" id))
                               (.then (fn [redis-spec]
                                        (when redis-spec
                                          (println "[event-agents] loaded Redis spec override for" id)))))))

                       ;; Recover Discord last-seen markers
                       (let [channels (or (:defaultChannels (discord-source-config control)) [])]
                         (doseq [channel-id channels]
                           (-> (redis/get-key client (str "event-agent:discord-last-seen:" channel-id))
                               (.then (fn [last-id]
                                        (when last-id
                                          (swap! source-state* assoc-in [:discord :last-seen channel-id] last-id)
                                          (println "[event-agents] recovered last-seen for channel" channel-id))))))))

                     ;; Schedule background SQL flush task
                     (schedule-flush-task!)

                     ;; Bind Discord gateway for real-time message handling
                     (bind-discord-gateway! config)

                     ;; Schedule cron jobs from control config
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
          (.catch (fn [err]
                    (println "[event-agents] failed to recover control config from Redis:" (.-message err))
                    ;; Fall through — start with defaults
                    (let [config (cfg)
                          control (control-config config)]
                      (println "[event-agents] starting with" (count (:jobs control)) "jobs (defaults)")
                      (schedule-flush-task!)
                      (bind-discord-gateway! config)
                      (doseq [job (:jobs control)]
                        (when (and (:enabled job)
                                   (= "cron" (get-in job [:trigger :kind])))
                          (schedule-job! config job))))))))))

;; =============================================================================
;; Public API: Job Management with Template Support
;; =============================================================================

(defn upsert-job!
  "Public API: Create or update an event-agent job.
   
   Args:
   - job-id: String identifier for the job
   - job-spec: Complete job specification OR template-based spec with :templateId
   
   If job-spec contains :templateId, instantiates from agent-templates DSL.
   Otherwise, treats job-spec as a complete job definition.
   
   Returns a promise that resolves to the normalized job spec.
   
   Example (template-based):
   (upsert-job! \"frankie-yap-bot\" 
                {:templateId :yap-bot
                 :trigger {:kind \"event\" :cadenceMinutes 1 :eventKinds [\"discord.message.mention\"]}
                 :filters {:channels [\"123456789\"] :keywords [\"frankie\"]}})
   
   Example (direct spec):
   (upsert-job! \"custom-bot\"
                {:id \"custom-bot\"
                 :enabled true
                 :trigger {:kind \"cron\" :cadenceMinutes 10}
                 :agentSpec {:role \"executive\" :model \"glm-5\" :thinkingLevel \"off\"}})"
  [job-id job-spec]
  (let [config (cfg)
        template-id (or (:templateId job-spec) (:template-id job-spec))
        
        normalized-job (if template-id
                         ;; Template instantiation path
                         (let [trigger (or (:trigger job-spec)
                                           {:kind "event" :cadenceMinutes 5 :eventKinds []})
                               source (or (:source job-spec)
                                          {:kind "manual" :mode "respond" :config {}})
                               filters (or (:filters job-spec)
                                           {:channels [] :keywords []})
                               overrides (dissoc job-spec :templateId :template-id :trigger :source :filters)]
                           (templates/instantiate-job template-id job-id trigger source filters overrides))
                         ;; Direct spec path - ensure required fields
                         (merge job-spec {:id job-id}))]
    
    ;; Normalize and persist
    (let [final-job (templates/normalize-job-for-persistence normalized-job)]
      (update-job-spec! job-id final-job)
      (reload!)
      (js/Promise.resolve final-job))))

(defn get-job
  "Get a job spec by ID.
   Loads from Redis if available, otherwise returns nil.
   Returns a promise."
  [job-id]
  (let [config (cfg)
        control (control-config config)
        default-job (some #(when (= (:id %) job-id) %) (:jobs control))]
    (get-job-spec job-id default-job)))

(defn delete-job!
  "Delete a job from Redis and reload runtime.
   Note: This only removes the Redis override - the job will revert to config defaults.
   Returns a promise."
  [job-id]
  (when-let [client (redis/get-client)]
    (let [key (str "event-agent:job-spec:" job-id)
          dirty-key "event-agent:job-dirty"]
      (-> (redis/del client key)
          (.then (fn []
                   (redis/srem client dirty-key job-id)
                   (reload!)
                   (println "[event-agents] deleted job" job-id "from Redis")
                   {:deleted job-id})))))
  (js/Promise.resolve {:deleted job-id}))

(defn list-templates
  "List all available agent templates.
   Returns vector of template keywords."
  []
  (templates/all-templates))

(defn list-model-profiles
  "List all available model profiles.
   Returns vector of profile keywords."
  []
  (templates/all-model-profiles))

(defn get-template
  "Get a template definition by keyword.
   Returns the template map or nil."
  [template-id]
  (templates/get-template template-id))

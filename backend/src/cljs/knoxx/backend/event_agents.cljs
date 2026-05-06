(ns knoxx.backend.event-agents
  "Generic event-agent runtime for Knoxx.

   Adapters emit normalized events.
   Jobs describe triggers + source filters + arbitrary agent specs.
   The runtime matches events/jobs and launches Knoxx runs through direct/start."
  (:require [clojure.string :as str]
            [shadow.cljs.modern :refer [js-await]]
            ["node:child_process" :as child-process]
             ["node:fs/promises" :as fs-promises]
             [knoxx.backend.discord-gateway :as dg]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.models :as runtime-models]
            [knoxx.backend.runtime.state :as runtime-state]
            [knoxx.backend.session-store :as session-store]
            [knoxx.backend.triggers.control-config :as control-config]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.agent-templates :as templates]
            [knoxx.backend.tools.media :as media]
            [knoxx.backend.text :refer [sanitize-svg-content]]
            [knoxx.backend.util.parse :refer [parse-positive-int]]
            [knoxx.backend.actions.dispatch :as actions-dispatch]))

(declare start! reload! execute-discord-synthesis!)

(defonce running?* (atom false))
(defonce scheduled-tasks* (atom {}))
(defonce job-state* (atom {}))
(defonce user-job-state* (atom {}))
(defonce source-state* (atom {:discord {:last-seen {}}}))
(defonce recent-events* (atom []))

;; In-process dispatch dedup. Guards against N concurrent scheduler instances
;; firing the same event-id N times. Swept to #{} on stop!.
(defonce dispatched-event-ids* (atom #{}))
;; In-process cache of the last written spec per job-id.
;; Used by upsert-job! to skip reload! when spec is unchanged.
(defonce job-specs* (atom {}))

;; Debounce reload! so rapid contract-watcher bursts (agent writing N files)
;; collapse into a single restart instead of N restarts each boot-kicking
;; discordideaspawner repeatedly.
(defonce ^:private reload-timer* (atom nil))

;; Lock to prevent overlapping reload executions. start! is async; without
;; this, rapid successive reload! calls spawn multiple concurrent start!
;; recoveries, each finding the same Redis sessions and resuming them —
;; the root cause of zombie job accumulation.
(defonce ^:private reload-lock* (atom nil))

(defn debounced-reload!
  "Coalesce rapid reload! calls into one, firing 2 s after the last call.
   Public so contract watchers and admin routes can use it."
  []
  (when-let [t @reload-timer*] (js/clearTimeout t))
  (reset! reload-timer*
          (js/setTimeout (fn []
                           (reset! reload-timer* nil)
                           (reload!))
                          2000)))


(defn- mark-event-dispatched!
  [event-id]
  (let [[before] (swap-vals! dispatched-event-ids* conj event-id)]
    (not (contains? before event-id))))
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

(defn- job-author-ids
  [job]
  (filter-string-list (or (get-in job [:filters :authorIds])
                          (get-in job [:filters :author_ids])
                          (get-in job [:filters :authors]))))

(defn- job-match-all?
  [job]
  (or (true? (get-in job [:filters :matchAll]))
      (true? (get-in job [:filters :match_all]))))

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
  (swap! job-specs* assoc job-id spec)
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
   Runs every 5 minutes to batch writes.
   Idempotent: no-op if the flush task is already registered."
  []
  (when-not (contains? @scheduled-tasks* :flush)
    (let [flush-interval-ms (* 5 60 1000)
          flush-task        (fn []
                              (when @running?*
                                (flush-dirty-jobs-to-sql!)))
          flush-id          (doto (js/setInterval flush-task flush-interval-ms) (.unref))]
      (swap! scheduled-tasks* assoc :flush {:type :interval :id flush-id})
      (println "[event-agents] scheduled SQL flush every 5 minutes")

      ;; Sliding-window sweep: cap dispatched-event-ids* at 500 entries so the
      ;; set doesn't grow unbounded across long uptimes.
      ;;
      ;; IMPORTANT: this interval must be tracked in scheduled-tasks* so stop!/reload!
      ;; clears it. Otherwise every reload leaks another interval which all wake up
      ;; again on the next start! (running?* becomes true), multiplying work.
      (let [sweep-interval-ms (* 10 60 1000)
            sweep-fn (fn []
                       (when @running?*
                         (swap! dispatched-event-ids*
                                (fn [ids]
                                  (if (> (count ids) 500)
                                    (set (take-last 500 (vec ids)))
                                    ids)))))
            sweep-id (doto (js/setInterval sweep-fn sweep-interval-ms) (.unref))]
        (swap! scheduled-tasks* assoc :dispatch-sweep {:type :interval
                                                      :id sweep-id
                                                      :everyMs sweep-interval-ms})))))

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

(defn- synthesis-source-mode?
  [mode]
  (contains? #{"synthesize" "synthesis"}
             (some-> mode str str/trim str/lower-case)))

(defn- exec-async
  "Run a shell command asynchronously, returning a Promise that resolves to
   the stdout string or rejects on error/timeout."
  [cmd opts]
  (js/Promise.
   (fn [resolve reject]
     (.exec child-process cmd opts
            (fn [err stdout _stderr]
              (if err
                (reject err)
                (resolve stdout)))))))

(defn- sanitize-svg-file!
  "Read an SVG file, repair corruption if present, and write it back.
   Returns a Promise resolving to true if repaired, false otherwise."
  [local-path]
  (if (str/ends-with? (str/lower-case local-path) ".svg")
    (-> (.readFile fs-promises local-path "utf8")
        (.then (fn [content]
                 (if-let [repaired (sanitize-svg-content content)]
                   (-> (.writeFile fs-promises local-path repaired "utf8")
                       (.then (fn [_]
                                (println "[event-agents] repaired corrupted SVG:" local-path)
                                true)))
                   false)))
        (.catch (fn [err]
                  (println "[event-agents] SVG sanitization failed for" local-path ":" (.-message err))
                  false)))
    (js/Promise.resolve false)))

(defn- download-attachment-to-tmp!
  "Download an attachment to /tmp asynchronously. Returns a Promise<string|nil>."
  [{:keys [filename url]}]
  (if (and filename url)
    (let [local-path (str "/tmp/" filename)]
      (-> (if (media/source-discord-cdn-url? url)
            (if-let [token (discord-token)]
              (-> (exec-async
                   (str "curl -sL -H " (pr-str (str "Authorization: Bot " token))
                        " -o " (pr-str local-path) " " (pr-str url))
                   #js {:timeout 10000})
                  (.then (fn [_] local-path))
                  (.catch (fn [err]
                            (println "[event-agents] attachment download failed:" filename (.-message err))
                            nil)))
              (js/Promise.resolve nil))
            (-> (exec-async
                 (str "curl -sL -o " (pr-str local-path) " " (pr-str url))
                 #js {:timeout 10000})
                (.then (fn [_] local-path))
                (.catch (fn [err]
                          (println "[event-agents] attachment download failed:" filename (.-message err))
                          nil))))
          (.then (fn [path]
                   (when path
                     (sanitize-svg-file! path))
                   path))))
    (js/Promise.resolve nil)))

(defn event-summary-text
  "Build an event summary string. Returns a Promise<string> because attachment
   downloads are async to avoid blocking the event loop."
  [event]
  (let [payload (or (:payload event) {})
        attachments (or (:attachments payload) [])
        publish-channels (or (:publishChannels payload) [])
        publish-text (when (seq publish-channels)
                       (str "\nPublish to channels:\n"
                            (str/join "\n" (map #(str "- " %) publish-channels))
                            "\n"))
        base-text (str "Event source: " (:sourceKind event) "\n"
                       "Event kind: " (:eventKind event) "\n"
                       "Event id: " (:id event) "\n"
                       "Occurred at: " (:timestamp event) "\n"
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
                       (when-let [summary (:summary payload)]
                         (str "Summary: " summary "\n"))
                       (when-let [payload-preview (:payloadPreview payload)]
                         (str "Payload preview: " payload-preview "\n")))]
    (if (seq attachments)
      (-> (js/Promise.all
           (clj->js
            (map (fn [attachment]
                   (-> (download-attachment-to-tmp! attachment)
                       (.then (fn [local-path]
                                (let [filename (:filename attachment)
                                      url (:url attachment)]
                                  (if local-path
                                    (str "- " filename " (saved to " local-path " — use the read tool to view it)")
                                    (str "- " filename
                                         (when url
                                           (str " <" url "> (download failed, use url directly)")))))))))
                 attachments)))
          (.then (fn [lines]
                   (str base-text
                        "\nAttachments (downloaded for reading):\n"
                        (str/join "\n" (js->clj lines))
                        "\n"
                        (or publish-text "")))))
      (js/Promise.resolve
       (str base-text
            (or publish-text ""))))))

;; Legacy sync version kept for callers that truly need a string (e.g. inline
;; log formatting).  Does NOT download attachments — just lists them as links.
(defn- event-summary-text-sync
  [event]
  (let [payload (or (:payload event) {})
        attachments (or (:attachments payload) [])
        attachment-lines (when (seq attachments)
                           (str "Attachments:\n"
                                (str/join "\n"
                                          (map (fn [attachment]
                                                 (str "- " (:filename attachment)
                                                      (when (:url attachment)
                                                        (str " <" (:url attachment) ">"))))
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

(defn- fetch-image-part!
  "Download an image content-part's :url, embed as data URI in :data."
  [part]
  (-> (js/fetch (:url part) #js {:method "GET"})
      (.then (fn [resp]
               (if (.-ok resp)
                 (-> (.arrayBuffer resp)
                     (.then (fn [buf]
                              (let [b64  (.toString (.from js/Buffer (js/Uint8Array. buf)) "base64")
                                    mime (or (:mimeType part) "image/jpeg")]
                                (-> part
                                    (assoc :data b64))))))
                 (do (js/console.warn "[event-agents] fetch-image-part! HTTP" (.-status resp) (:url part))
                     (js/Promise.resolve nil)))))
      (.catch (fn [err]
                (js/console.warn "[event-agents] fetch-image-part! failed" (.-message err) (:url part))
                (js/Promise.resolve nil)))))

(defn- materialize-content-parts!
  "Returns Promise<vec-of-parts> with image :url replaced by data URI :data."
  [parts]
  (-> (js/Promise.all
        (clj->js (mapv (fn [part]
                         (if (= "image" (:type part))
                           (fetch-image-part! part)
                           (js/Promise.resolve part)))
                       parts)))
      (.then (fn [arr] (vec (remove nil? (array-seq arr)))))))
(defn- event-content-parts [_event] [])

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
  "Build the agent run payload. Returns a Promise<map> because event-summary-text
   is async (attachment downloads)."
  [config job event]
  (let [agent-spec (:agentSpec job)
        now (.now js/Date)
        run-id (str "event-agent-" (:id job) "-" now)
        {:keys [conversation-id session-id summary]} (sticky-session-target job event)
        contract-id (or (:contract-id agent-spec)
                        (:contractSourceId job))
        actor-id (or (:actor-id agent-spec)
                     (:actorId job))
        content-parts (event-content-parts event)
        model-id (or (:model agent-spec) "gemma4:31b")
        memory-hydration (or (:memoryHydration agent-spec)
                             (:memory-hydration agent-spec))
        preamble (str "An event matched this job.\n\n"
                      (or (:taskPrompt agent-spec) "")
                      (when-not (str/blank? (or (:taskPrompt agent-spec) "")) "\n\n")
                      (when-not (str/blank? (or summary ""))
                        (str summary "\n\n")))]
    (-> (event-summary-text event)
        (.then (fn [summary-text]
                 {:conversation_id conversation-id
                  :session_id session-id
                  :run_id run-id
                  :message (str preamble summary-text)
                  :content_parts content-parts
                  :agent_spec (cond-> {:role (or (:role agent-spec) "knowledge_worker")
                                       :system_prompt (or (:systemPrompt agent-spec) "You are a Knoxx event agent.")
                                       :task_prompt (or (:taskPrompt agent-spec) "")
                                       :model model-id
                                       :thinking_level (or (:thinkingLevel agent-spec) "off")
                                       :tool_policies (tool-policies->js (:toolPolicies agent-spec))}
                                contract-id (assoc :contract_id contract-id)
                                actor-id (assoc :actor_id actor-id)
                                memory-hydration (assoc :memory_hydration memory-hydration))
                  :model model-id})))))

(defn- job-step
  [job]
  (or (:step job)
      {:uses "run-agent" :with (:agentSpec job {})}))

(defn- streaming-behavior
  "Return the streaming behavior for a job when the session is already active.
   :steer     — send the message as a steering directive to the running session
   :follow-up — queue the message for when the current turn finishes (default)"
  [job]
  (let [raw (or (get-in job [:data :streamingBehavior])
               (get-in job [:data :source :streamingBehavior])
               (get-in job [:source :config :streamingBehavior])
               (get-in job [:source :streamingBehavior])
               "follow-up")]
    (case (str/lower-case (str raw))
      "steer" :steer
      "follow-up" :follow-up
      "followup" :follow-up
      "queue" :follow-up
      :follow-up)))

(defn- steer-running-session!
  "Send a steering message to a running agent session via /api/knoxx/steer."
  [config job event session-id conversation-id]
  (-> (event-summary-text event)
      (.then (fn [summary-text]
               (let [message (str "[Steer] New event for this agent:\n\n" summary-text)
                     body {:message message
                           :conversation_id conversation-id
                           :session_id session-id}]
                 (-> (fetch-json! (str (:knoxx-base-url config) "/api/knoxx/steer")
                                  #js {:method "POST"
                                       :headers (direct-start-headers config)
                                       :body (.stringify js/JSON (clj->js body))})
                     (.then (fn [result]
                              (println "[event-agents] steered session" session-id "for job" (:id job))
                              result))
                     (.catch (fn [err]
                               (println "[event-agents] failed to steer session for job" (:id job) ":" (.-message err))
                               (js/Promise.reject err)))))))))

(defn- follow-up-running-session!
  "Send a follow-up message to a running agent session via /api/knoxx/follow-up."
  [config job event session-id conversation-id]
  (-> (event-summary-text event)
      (.then (fn [summary-text]
               (let [message (str "[Follow-up] New event for this agent:\n\n" summary-text)
                     body {:message message
                           :conversation_id conversation-id
                           :session_id session-id}]
                 (-> (fetch-json! (str (:knoxx-base-url config) "/api/knoxx/follow-up")
                                  #js {:method "POST"
                                       :headers (direct-start-headers config)
                                       :body (.stringify js/JSON (clj->js body))})
                     (.then (fn [result]
                              (println "[event-agents] follow-up queued" session-id "for job" (:id job))
                              result))
                     (.catch (fn [err]
                               (println "[event-agents] failed to follow-up for job" (:id job) ":" (.-message err))
                               (js/Promise.reject err)))))))))

(defn- start-agent-run!
  [config job event]
  (-> (build-agent-run-payload config job event)
      (.then (fn [raw-body]
               (-> (materialize-content-parts! (:content_parts raw-body))
                   (.then (fn [materialized-parts]
                            (let [body (assoc raw-body :content_parts materialized-parts)]
                              (-> (fetch-json! (str (:knoxx-base-url config) "/api/knoxx/direct/start")
                                               #js {:method "POST"
                                                    :headers (direct-start-headers config)
                                                    :body (.stringify js/JSON (clj->js body))})
                                  (.then (fn [result]
                                           (println "[event-agents] queued run" (:run_id body)
                                                    "for job" (:id job) "event" (:eventKind event))
                                           result))
                                  (.catch (fn [err]
                                            (let [msg (.-message err)
                                                  session-id (:session_id body)
                                                  conversation-id (:conversation_id body)
                                                  behavior (streaming-behavior job)]
                                              (if (and (str/includes? msg "agent_already_processing")
                                                       (= behavior :steer))
                                                (steer-running-session! config job event session-id conversation-id)
                                                (if (and (str/includes? msg "agent_already_processing")
                                                         (= behavior :follow-up))
                                                  (do (println "[event-agents] session active, queuing follow-up for job" (:id job))
                                                      (follow-up-running-session! config job event session-id conversation-id))
                                                  (do (println "[event-agents] failed to queue run for job" (:id job) ":" msg)
                                                      (js/Promise.reject err))))))))))))))))
(defn- matches-event-kind?
  "Check if event kinds match the job's configured events.

   LEGACY MODE (default): :always and :maybe are treated as a simple union.
   Any matching event kind triggers the job.

   SCORING MODE (opt-in): When :threshold or :weights are present, :always kinds
   are required (all must match) and :maybe kinds contribute to a score that
   must meet or exceed :eventThreshold."
  [job event-kinds]
  (let [configured (vec (or (get-in job [:trigger :eventKinds]) []))
        kinds (if (string? event-kinds) [event-kinds] event-kinds)
        always-kinds (vec (or (get-in job [:trigger :alwaysKinds]) []))
        maybe-kinds (vec (or (get-in job [:trigger :maybeKinds]) []))
        weights (or (get-in job [:trigger :eventWeights]) {})
        threshold (or (get-in job [:trigger :eventThreshold]) 1)
        kind-strs (set (map str kinds))
        ;; Scoring mode is active if contract explicitly sets threshold or weights
        scoring-mode? (or (seq weights) (> threshold 1))]
    (if scoring-mode?
      ;; Scoring mode: :always = required, :maybe = scored
      (let [always-strs (set (map str always-kinds))
            maybe-strs (set (map str maybe-kinds))
            ;; All always kinds must be present
            always-met? (or (empty? always-strs)
                           (every? #(contains? kind-strs %) always-strs))
            ;; Calculate score from matching maybe kinds
            score (if (empty? maybe-strs)
                   0
                   (reduce (fn [acc mk]
                             (if (contains? kind-strs mk)
                               (+ acc (get weights mk 1))
                               acc))
                           0
                           maybe-strs))
            ;; Score must meet threshold
            score-met? (>= score threshold)]
        (and always-met? score-met?))
      ;; Legacy mode: simple intersection across all configured kinds
      (or (empty? configured)
          (some (fn [ek]
                  (some (fn [conf] (= (str ek) (str conf)))
                        configured))
                kinds)))))

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
    (or (job-match-all? job)
        (empty? keywords)
        (some #(str/includes? lowered %) keywords))))

(defn- matches-author?
  [job author-id]
  (let [author-ids (job-author-ids job)
        normalized-author-id (some-> author-id str str/trim not-empty)]
    (or (empty? author-ids)
        (and normalized-author-id
             (some #(= % normalized-author-id) author-ids)))))

(defn- mention-event?
  [event-kind]
  (some #(= (str event-kind) %) ["discord.message.mention"]))

(defn job-matches-event?
  [control job event]
  (let [payload (or (:payload event) {})
        event-kind (:eventKind event)
        ;; For scoring, consider all event kinds present in this event
        event-kinds (or (seq (:eventKinds event)) [event-kind])]
    (and (:enabled job)
         (= "event" (get-in job [:trigger :kind]))
         (= (str (get-in job [:source :kind])) (str (:sourceKind event)))
         (matches-event-kind? job event-kinds)
         ;; Voice-state events carry voice-channel IDs in :channelId.
         ;; A job's default :channels are text channels for message monitoring;
         ;; applying those to voice channels would never match, so skip it.
         (or (str/starts-with? (str event-kind) "discord.voice")
             (matches-channel? control job (:channelId payload)))
         (matches-author? job (:authorId payload))
         ;; Keyword filter does not apply to mention events — the mention
         ;; itself is the trigger signal, regardless of content words.
         (or (mention-event? event-kind)
             (matches-keywords? control job (:content payload)))
         (matches-repository? job (:repository payload)))))

(defn dispatch-event!
  [event]
  (let [config (cfg)
        control (control-config config)
        normalized-base (merge {:id (str "event-" (.now js/Date))
                                :timestamp (.toISOString (js/Date.))
                                :sourceKind "manual"
                                :eventKind "manual.event"
                                :payload {}}
                               (or event {}))
        ;; Fan matching across all event-kinds, dedup jobs by :id.
        ;; Preserves which specific eventKind triggered each job so
        ;; downstream context is accurate.
        all-kinds (or (seq (:eventKinds normalized-base))
                      [(:eventKind normalized-base)])
        matching-jobs (->> all-kinds
                           (mapcat (fn [kind]
                                     (let [e (assoc normalized-base :eventKind kind)]
                                       (->> (:jobs control)
                                            (filter #(job-matches-event? control % e))
                                            (map #(assoc % ::matched-kind kind))))))
                           (reduce (fn [acc job]
                                     (if (some #(= (:id %) (:id job)) acc)
                                       acc
                                       (conj acc job)))
                                   [])
                           vec)
        normalized-event normalized-base]
    (append-recent-event! normalized-event)
    (if (and (not (str/blank? (:id normalized-event)))
             (not (mark-event-dispatched! (:id normalized-event))))
      (js/Promise.resolve {:matchedJobs [] :event normalized-event :skipped true})
    (if (empty? matching-jobs)
      (js/Promise.resolve {:matchedJobs []
                           :event normalized-event})
      (-> (js/Promise.all
           (clj->js
            (mapv (fn [job]
                    (let [started-at (record-job-run-start! job)
                          step (job-step job)]
                      (-> (if (and (= "discord" (get-in job [:source :kind]))
                                    (synthesis-source-mode? (get-in job [:source :mode])))
                            (execute-discord-synthesis! config control job normalized-event)
                            (actions-dispatch/dispatch! {:config config
                                                         :event normalized-event :job job
                                                         :run-agent! start-agent-run!}
                                                        step))
                          (.then (fn [result]
                                   (record-job-run-finish! job started-at "ok" nil)
                                   result))
                          (.catch (fn [err]
                                    (record-job-run-finish! job started-at "error" (.-message err))
                                    nil)))))
                  matching-jobs)))
          (.then (fn [_]
                   {:matchedJobs (mapv :id matching-jobs)
                    :event normalized-event})))))))  ;; closes idempotency if

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
         guild-id (:guildId message)
         author-id (:authorId message)
         author-voice-channel (when-not (str/blank? (str guild-id ""))
                                (get-in @source-state* [:discord :voice-states guild-id author-id]))
         payload {:channelId (:channelId message)
                  :guildId guild-id
                  :authorId author-id
                  :authorUsername (:authorUsername message)
                  :authorIsBot (:authorIsBot message)
                  :authorVoiceChannelId author-voice-channel
                  :content (:content message)
                  :messageId (:id message)
                  :attachments (vec (or (:attachments message) []))
                  :embeds (vec (or (:embeds message) []))}]
     (when-not (:authorIsBot message)
       (remember-discord-latest! (:channelId message) [message])
       (dispatch-event! {:sourceKind  "discord"
                         :eventKind   "discord.message.created"
                         :eventKinds  (cond-> ["discord.message.created"]
                                        mention? (conj "discord.message.mention")
                                        keyword? (conj "discord.message.keyword"))
                         :payload     payload}))))

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
      (let [msg-unsub (.onMessage manager (fn [mapped _raw]
                                             (dispatch-discord-gateway-message! (js->clj mapped :keywordize-keys true))))
            voice-unsub (.onVoiceStateUpdate manager (fn [mapped _old _new]
                                                        (let [data (js->clj mapped :keywordize-keys true)]
                                                          (dispatch-voice-state-update! data))))]
        (reset! discord-gateway-unsubscribe*
                (fn [] (msg-unsub) (voice-unsub)))))))

(defn- dispatch-voice-state-update!
  [{:keys [action userId username guildId channelId oldChannelId newChannelId]}]
  (when (not (str/blank? (str userId "")))
    ;; Track user voice states so message events can include the author's voice channel
    (when-not (str/blank? (str guildId ""))
      (if (str/blank? (str newChannelId ""))
        ;; User left/moved out - remove their voice state
        (swap! source-state* update-in [:discord :voice-states guildId] dissoc userId)
        ;; User joined/moved in - record their voice channel
        (swap! source-state* assoc-in [:discord :voice-states guildId userId] newChannelId)))
    (dispatch-event! {:sourceKind "discord"
                      :eventKind "discord.voice.state_update"
                      :eventKinds ["discord.voice.state_update"]
                      :payload {:action action
                                :userId userId
                                :username username
                                :guildId guildId
                                :channelId channelId
                                :oldChannelId oldChannelId
                                :newChannelId newChannelId}})))

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
        keyword? (and (not (job-match-all? job))
                      (matches-keywords? control job (:content message)))
        created? (matches-event-kind? job "discord.message.created")]
    (cond
      mention? "discord.message.mention"
      keyword? "discord.message.keyword"
      (and created? (job-match-all? job)) "discord.message.created"
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
  ;; Skip patrol dispatch entirely when the gateway is live — the gateway fires
  ;; dispatch-discord-gateway-message! in real time, so patrol would double-fire.
  (if (discord-gateway-active?)
    (js/Promise.resolve nil)
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
                   (js/Promise.resolve nil))))))))

(defn- summarize-discord-channel
  [channel-id messages]
  (->> messages
       (remove :authorIsBot)
       (take 8)
       (map (fn [message]
              (let [attachments (:attachments message)
                    attachment-text (when (seq attachments)
                                      (str " attachments="
                                           (str/join ", " (map (fn [a] (str (:filename a) (when (:url a) (str " <" (:url a) ">"))))
                                          attachments))))]
                (str "[" channel-id "] <" (:authorUsername message) " (id:" (:authorId message) ")> "
                     (subs (:content message) 0 (min 180 (count (:content message))))
                     (or attachment-text "")))))
       (str/join "\n")))

(defn- synthesis-channel-ids!
  [control job trigger-event]
  (-> (resolve-job-channel-ids! control job)
      (.then (fn [channels]
               (let [trigger-channel (some-> (get-in trigger-event [:payload :channelId]) str str/trim not-empty)]
                 (->> (concat channels (when trigger-channel [trigger-channel]))
                      distinct
                      vec))))))

(defn- fetch-discord-synthesis-row!
  [limit channel-id]
  (-> (read-discord-channel! channel-id limit)
      (.then (fn [messages]
               {:channelId channel-id
                :messages messages}))
      (.catch (fn [_]
                {:channelId channel-id
                 :messages []}))))

(defn- discord-synthesis-trigger-summary!
  [trigger-event]
  (if trigger-event
    (-> (event-summary-text trigger-event)
        (.then (fn [txt]
                 (str "Triggering event:\n" txt))))
    (js/Promise.resolve nil)))

(defn- discord-synthesis-image-attachments
  [rows]
  (->> rows
       (mapcat (fn [{:keys [messages]}]
                 (mapcat :attachments messages)))
       (filter :url)
       (filter (fn [attachment]
                 (some-> (:contentType attachment)
                         str
                         str/lower-case
                         (str/starts-with? "image/"))))
       (take 8)
       vec))

(defn- discord-synthesis-dispatch-summary!
  [config job publish-channels trigger-event channels rows]
  (let [channel-summary (->> rows
                             (map (fn [{:keys [channelId messages]}]
                                    (summarize-discord-channel channelId messages)))
                             (remove str/blank?)
                             (str/join "\n\n"))
        base-summary (when-not (str/blank? channel-summary)
                       (str "Recent Discord context:\n" channel-summary))]
    (-> (discord-synthesis-trigger-summary! trigger-event)
        (.then (fn [trigger-summary]
                 (let [summary (str/join "\n\n"
                                         (remove str/blank?
                                                 [trigger-summary base-summary]))]
                   (if (str/blank? summary)
                     (js/Promise.resolve nil)
                     (actions-dispatch/dispatch!
                      {:config config
                       :event {:sourceKind "discord"
                               :eventKind "discord.snapshot.summary"
                               :timestamp (.toISOString (js/Date.))
                               :payload {:summary summary
                                         :channelId (first channels)
                                         :publishChannels publish-channels
                                         :attachments (discord-synthesis-image-attachments rows)}}
                       :job job
                       :run-agent! start-agent-run!}
                      (job-step job)))))))))

(defn- execute-discord-synthesis!
  ([config control job] (execute-discord-synthesis! config control job nil))
  ([config control job trigger-event]
   (let [limit (job-max-messages job 12)
         publish-channels (job-publish-channels job)
         fetch-row! (partial fetch-discord-synthesis-row! limit)
         dispatch-summary! (partial discord-synthesis-dispatch-summary! config job publish-channels trigger-event)]
     (-> (synthesis-channel-ids! control job trigger-event)
         (.then (fn [channels]
                  (if-not (seq channels)
                    (js/Promise.resolve nil)
                    (-> (js/Promise.all (clj->js (mapv fetch-row! channels)))
                        (.then (fn [results]
                                 (dispatch-summary! channels (js->clj results :keywordize-keys true))))))))))))

(defn- execute-direct-job!
  [config job source-kind event-kind]
  (actions-dispatch/dispatch! {:config config
                               :event {:sourceKind source-kind
                                       :eventKind event-kind
                                       :timestamp (.toISOString (js/Date.))
                                       :payload {:payloadPreview (str "Synthetic trigger for job " (:id job))}}
                               :job job
                               :run-agent! start-agent-run!}
                              (job-step job))
  )

(defn- execute-cron-job!
  [config job]
  (let [control (control-config config)
        source-kind (get-in job [:source :kind])
        mode (get-in job [:source :mode])]
    (cond
      (and (= source-kind "discord") (= mode "patrol"))
      (execute-discord-patrol! config control job)

      (and (= source-kind "discord") (synthesis-source-mode? mode))
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
        (js-await [result
                   (if (= "cron" (get-in job [:trigger :kind]))
                     (execute-cron-job! config job)
                     (execute-direct-job! config job (get-in job [:source :kind]) "manual.run"))]
                  (record-job-run-finish! job started-at "ok" nil)
                  result
                  (catch err
                      (record-job-run-finish! job started-at "error" (.-message err))
                    nil))))))

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
  ;; NOTE: dispatched-event-ids* is intentionally NOT reset on stop!/reload!
  ;; Clearing it was the root cause of re-dispatch fan-out: in-flight event ids
  ;; from the triggering run would be re-dispatched on every contract-file reload.
  ;; The set is capped at 500 entries by the periodic sweep in schedule-flush-task!.
  (reset! job-specs* {})
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

(def ^:private cron-ticker-ms 15000)

(defn- job-cadence-ms
  [job]
  (* 60 1000 (max 1 (or (get-in job [:trigger :cadenceMinutes]) 1))))

(defn- cron-job?
  [job]
  (and (:enabled job)
       (= "cron" (get-in job [:trigger :kind]))))

(defn- initialize-cron-job-state!
  [job]
  (let [job-id (:id job)
        cadence-ms (job-cadence-ms job)
        now (.now js/Date)]
    (update-job-state! job-id
                       (fn [state]
                         (let [state (normalize-job-state job-id state)
                               running? (boolean (:running state))
                               last-finished (:lastFinishedAt state)
                               next-run (or (:nextRunAt state)
                                            (when last-finished (+ last-finished cadence-ms))
                                            now)]
                           (assoc state
                                  :id job-id
                                  :name (:name job)
                                  :enabled (:enabled job)
                                  :running running?
                                  :nextRunAt next-run))))))

(defn- due-cron-job?
  [now job]
  (let [job-id (:id job)
        state (normalize-job-state job-id (get @job-state* job-id))]
    (and (cron-job? job)
         (not (:running state))
         (<= (or (:nextRunAt state) 0) now))))

(defn- trigger-due-cron-jobs!
  [config]
  (when @running?*
    (let [control (control-config config)
          now (.now js/Date)
          cron-jobs (filter cron-job? (:jobs control))]
      (doseq [job cron-jobs]
        (initialize-cron-job-state! job))
      (doseq [job cron-jobs]
        (when (due-cron-job? now job)
          (-> (run-job! (:id job))
              (.catch (fn [err]
                        (println "[event-agents] cron ticker job failed for" (:id job) ":" (.-message err))))))))))

(defn- schedule-cron-ticker!
  [config]
  ;; One ticker owns all cron evaluation. Individual jobs never register their
  ;; own timers/timeouts, which prevents timer fan-out across reload/restart
  ;; churn and makes "next activation time" visible in job-state.
  (when-not (contains? @scheduled-tasks* :cron-ticker)
    (let [tick! (fn [] (trigger-due-cron-jobs! config))
          id (doto (js/setInterval tick! cron-ticker-ms) (.unref))]
      (swap! scheduled-tasks* assoc :cron-ticker {:type :interval
                                                  :id id
                                                  :everyMs cron-ticker-ms})
      (println "[event-agents] scheduled single cron ticker every" cron-ticker-ms "ms")
      (tick!))))

(defn reload!
  "Stop then start the event-agent runtime. Returns a promise.
   Prevents concurrent reloads — if a reload is already in progress,
   returns the existing promise."
  []
  ;; Cancel any pending debounced reload
  (when-let [t @reload-timer*]
    (js/clearTimeout t)
    (reset! reload-timer* nil))

  ;; If already reloading, return existing promise
  (or @reload-lock*
      (let [p (-> (js/Promise.resolve (stop!))
                  (.then (fn []
                           ;; Clear sticky session state to prevent cross-reload leaks.
                           ;; Old sessions are orphaned; agent-resume cooldown keeps
                           ;; recovery from immediately resuming them.
                           (reset! job-state* {})
                           (reset! user-job-state* {})
                           (start! nil)))
                  (.finally (fn []
                              (reset! reload-lock* nil))))]
        (reset! reload-lock* p)
        p)))

(defn start!
  [_config]
  (if (compare-and-set! running?* false true)
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

                     ;; One scheduler ticker evaluates all cron jobs from
                     ;; their persisted :nextRunAt timestamps. No per-job
                     ;; intervals or boot-kick timeouts are registered.
                     (schedule-cron-ticker! config))))
          (.catch (fn [err]
                    (println "[event-agents] failed to recover control config from Redis:" (.-message err))
                    ;; Fall through — start with defaults
                    (let [config (cfg)
                          control (control-config config)]
                      (println "[event-agents] starting with" (count (:jobs control)) "jobs (defaults)")
                      (schedule-flush-task!)
                      (bind-discord-gateway! config)
                      (schedule-cron-ticker! config))))))
    ;; Already running — return resolved promise so callers can chain uniformly
    (js/Promise.resolve nil)))

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

    ;; Normalize and persist; only reload if spec changed
    (let [final-job  (templates/normalize-job-for-persistence normalized-job)
          prev-spec  (or (get-in @runtime-state/config* [:event-agent-control :jobs])
                          (when-let [s (get @job-specs* job-id)] [s]))
          spec-sig   #(dissoc % :updatedAt :createdAt)
          unchanged? (some #(= (spec-sig %) (spec-sig final-job)) (or prev-spec []))]
      (update-job-spec! job-id final-job)
      (when-not unchanged?
        ;; Debounced: agents writing many contracts in quick succession
        ;; will collapse to a single reload instead of N.
        (debounced-reload!))
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

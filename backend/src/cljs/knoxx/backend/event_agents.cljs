(ns knoxx.backend.event-agents
  "Generic event-agent runtime for Knoxx.

   Adapters emit normalized events.
   Jobs describe triggers + source filters + arbitrary agent specs.
   The runtime matches events/jobs and launches Knoxx runs through direct/start."
  (:require [clojure.string :as str]
            [shadow.cljs.modern :refer [js-await]]
             ["node:fs/promises" :as fs-promises]
            [knoxx.backend.events.sources.discord :as discord-source]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.models :as runtime-models]
            [knoxx.backend.runtime.state :as runtime-state]
            [knoxx.backend.session-store :as session-store]
            [knoxx.backend.agents.runner :as agents-runner]
            [knoxx.backend.events.cron :as events-cron]
            [knoxx.backend.triggers.control-config :as control-config]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.agent-templates :as templates]
            [knoxx.backend.tools.media :as media]
            [knoxx.backend.text :refer [sanitize-svg-content]]
            [knoxx.backend.util.parse :refer [parse-positive-int]]
            [knoxx.backend.actions.dispatch :as actions-dispatch]
            [knoxx.backend.contracts.actor-scope :as actor-scope]))

(declare start! reload! execute-discord-synthesis! normalize-job-state run-job! dispatch-voice-state-update!)

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

(def ^:private event-agent-job-dirty-redis-key "event-agent:job-dirty")

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

(defn- schedule-events-cron-ticker!
  [config]
  (events-cron/schedule-cron-ticker!
   {:scheduled-tasks* scheduled-tasks*
    :job-state* job-state*
    :running?* running?*
    :control-config-fn control-config
    :run-job! run-job!
    :update-job-state! update-job-state!
    :normalize-job-state normalize-job-state}
   config))

(defn- discord-token
  []
  (discord-source/bot-token (cfg)))

(defn- policy-db
  []
  (runtime-state/current-policy-db))

(defn- discord-gateway-user-id
  []
  (discord-source/gateway-user-id))

(defn- fetch-json!
  [url options]
  (-> (js/fetch url options)
      (.then (fn [resp]
               (if (.-ok resp)
                 (.json resp)
                 (-> (.text resp)
                     (.then (fn [text]
                              (throw (js/Error. (str "HTTP " (.-status resp) ": " text)))))))))))

(defn- discord-source-config
  [control]
  (or (get-in control [:sources :discord]) {}))

(defn- nonblank-str
  [value]
  (some-> value str str/trim not-empty))

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

(defn- redis-keys!
  [client pattern]
  (-> (.keys client pattern)
      (.then (fn [keys]
               (if (array? keys)
                 (vec (array-seq keys))
                 [])))
      (.catch (fn [err]
                (println "[event-agents] Redis KEYS failed for" pattern ":" (.-message err))
                []))))

(defn- delete-redis-keys!
  [client keys]
  (let [keys' (->> (or keys [])
                   (map #(some-> % str str/trim not-empty))
                   (remove nil?)
                   distinct
                   vec)]
    (if (seq keys')
      (-> (js/Promise.all (clj->js (map #(redis/del client %) keys')))
          (.then (fn [_] (count keys'))))
      (js/Promise.resolve 0))))

(defn- clear-runtime-state!
  []
  (reset! job-state* {})
  (reset! user-job-state* {})
  (reset! source-state* {:discord {:last-seen {}}})
  (reset! recent-events* [])
  (reset! job-specs* {})
  (reset! dispatched-event-ids* #{}))

(defn- disable-cron-jobs
  [control]
  (update control :jobs
          (fn [jobs]
            (mapv (fn [job]
                    (if (= "cron" (get-in job [:trigger :kind]))
                      (assoc job :enabled false)
                      job))
                  (or jobs [])))))

(defn- recover-runtime-state!
  [control]
  (if-let [client (redis/get-client)]
    (let [job-ids (map :id (:jobs control))
          channels (or (:defaultChannels (discord-source-config control)) [])
          recovery-promises
          (concat
           (for [job-id job-ids]
             (-> (redis/get-json client (str "event-agent:job-state:" job-id))
                 (.then (fn [state]
                          (when state
                            (swap! job-state* assoc job-id (normalize-job-state job-id state))
                            (println "[event-agents] recovered state for" job-id))
                          nil))))
           (for [job-id job-ids]
             (-> (redis/get-json client (str "event-agent:job-spec:" job-id))
                 (.then (fn [redis-spec]
                          (when redis-spec
                            (swap! job-specs* assoc job-id redis-spec)
                            (println "[event-agents] recovered spec for" job-id))
                          nil))))
           (for [channel-id channels]
             (-> (redis/get-key client (str "event-agent:discord-last-seen:" channel-id))
                 (.then (fn [last-id]
                          (when last-id
                            (swap! source-state* assoc-in [:discord :last-seen channel-id] last-id)
                            (println "[event-agents] recovered last-seen for channel" channel-id))
                          nil)))))]
      (-> (js/Promise.all (clj->js recovery-promises))
          (.then (fn [_] nil))))
    (js/Promise.resolve nil)))

(defn- update-job-spec!
  "Update a job spec in Redis and mark it dirty for SQL flush.
   This is the canonical write path - all job updates go through here."
  [job-id spec]
  (when-let [client (redis/get-client)]
    (let [key (str "event-agent:job-spec:" job-id)
          dirty-key event-agent-job-dirty-redis-key]
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
    (let [dirty-key event-agent-job-dirty-redis-key]
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

(defn- fetch-with-timeout
  "Fetch a URL with an AbortController timeout. Returns a Promise<Response>."
  [url opts timeout-ms]
  (let [controller (js/AbortController.)
        timeout-id (js/setTimeout #(.abort controller) timeout-ms)]
    (-> (js/fetch url (js/Object.assign #js {:signal (.-signal controller)} opts))
        (.finally (fn [] (js/clearTimeout timeout-id))))))

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
  "Download an attachment to /tmp asynchronously using secure fetch.
   Returns a Promise<string|nil> with the local file path."
  [{:keys [filename url]}]
  (if (and filename url)
    (let [local-path (str "/tmp/" filename)]
      (-> (fetch-with-timeout
           url
           (if (and (media/source-discord-cdn-url? url) (discord-token))
             #js {:headers #js {"Authorization" (str "Bot " (discord-token))}}
             #js {})
           10000)
          (.then (fn [resp]
                   (if (.-ok resp)
                     (.-arrayBuffer resp)
                     (throw (js/Error. (str "HTTP " (.-status resp)))))))
          (.then (fn [buffer]
                   (.writeFile fs-promises local-path (js/Buffer. buffer))
                   local-path))
          (.then (fn [path]
                   (sanitize-svg-file! path)
                   path))
          (.catch (fn [err]
                    (println "[event-agents] attachment download failed:" filename (.-message err))
                    nil))))
    (js/Promise.resolve nil)))

(defn- cleanup-tmp-file!
  "Delete a temporary file, logging any errors."
  [path]
  (when path
    (-> (.unlink fs-promises path)
        (.catch (fn [err]
                  (println "[event-agents] failed to clean up temp file" path ":" (.-message err)))))))

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
                                  ;; Clean up temp file after processing
                                  (cleanup-tmp-file! local-path)
                                  (if local-path
                                    (str "- " filename " (saved to " local-path " — use the read tool to view it)")
                                    (str "- " filename
                                         (when url
                                           (str " <" url "> (download failed, use url directly)"))))))))
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
(defn- event-content-parts [event]
  (let [payload (or (:payload event) {})]
    (vec (or (:content_parts payload)
             (:contentParts payload)
             []))))

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
        contract-id (or (nonblank-str (:contract-id agent-spec))
                        (nonblank-str (:contractId agent-spec))
                        (nonblank-str (:contract_id agent-spec))
                        (nonblank-str (:contractSourceId job)))
        event-actor (event-actor-id event)
        actor-id (or (when (and event-actor
                                 (job-permitted-for-event-actor? job event))
                        event-actor)
                     (nonblank-str (:actor-id agent-spec))
                     (nonblank-str (:actorId agent-spec))
                     (nonblank-str (:actor_id agent-spec))
                     (nonblank-str (:actorId job))
                     (nonblank-str (:actor-id job))
                     (nonblank-str (:actor_id job)))
        content-parts (event-content-parts event)
        model-id (or (:model agent-spec) "gemma4:31b")
        task-prompt (or (:taskPrompt agent-spec)
                        (:task-prompt agent-spec)
                        "")
        system-prompt (or (:systemPrompt agent-spec)
                          (:system-prompt agent-spec)
                          "You are a Knoxx event agent.")
        thinking-level (or (:thinkingLevel agent-spec)
                           (:thinking-level agent-spec)
                           "off")
        tool-policies (or (:toolPolicies agent-spec)
                          (:tool-policies agent-spec))
        memory-hydration (or (:memoryHydration agent-spec)
                             (:memory-hydration agent-spec))
        context-policy (or (:contextPolicy agent-spec)
                           (:context-policy agent-spec)
                           (:context agent-spec))
        preamble (str "An event matched this job.\n\n"
                      task-prompt
                      (when-not (str/blank? task-prompt) "\n\n")
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
                                       :system_prompt system-prompt
                                       :task_prompt task-prompt
                                       :model model-id
                                       :thinking_level thinking-level
                                       :tool_policies (tool-policies->js tool-policies)}
                                contract-id (assoc :contract_id contract-id)
                                actor-id (assoc :actor_id actor-id)
                                memory-hydration (assoc :memory_hydration memory-hydration)
                                context-policy (assoc :context_policy context-policy))
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
      "idle-only" :idle-only
      "idle_only" :idle-only
      "drop-if-busy" :idle-only
      "drop_if_busy" :idle-only
      "ignore-if-busy" :idle-only
      "ignore_if_busy" :idle-only
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
                              (-> (agents-runner/spawn-direct! config body)
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
                                                  (if (and (str/includes? msg "agent_already_processing")
                                                           (= behavior :idle-only))
                                                    (do (println "[event-agents] session busy, dropping idle-only event for job" (:id job))
                                                        (js/Promise.resolve {:skipped true :reason "session_busy"}))
                                                    (do (println "[event-agents] failed to queue run for job" (:id job) ":" msg)
                                                        (js/Promise.reject err)))))))))))))))))
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

(defn- event-actor-id
  [event]
  (let [payload (or (:payload event) {})]
    (or (nonblank-str (:gatewayActorId payload))
        (nonblank-str (:sourceActorId payload))
        (nonblank-str (:actorId payload))
        (nonblank-str (:actor-id payload))
        (nonblank-str (:actor_id payload))
        (nonblank-str (:gatewayActorId event))
        (nonblank-str (:sourceActorId event))
        (nonblank-str (:actorId event))
        (nonblank-str (:actor-id event))
        (nonblank-str (:actor_id event)))))

(defn- job-actor-claims
  [job]
  (let [agent-spec (:agentSpec job)
        claims (actor-scope/normalize-actor-claims
                (or (:contractActors agent-spec)
                    (:contract-actors agent-spec)
                    (:contract_actor_ids agent-spec)
                    (:contractActorIds job)
                    (:contract-actor-ids job)
                    (:contractActors job)))]
    (if (seq claims)
      claims
      (actor-scope/normalize-actor-claims
       (or (:actorId agent-spec)
           (:actor-id agent-spec)
           (:actor_id agent-spec)
           (:actorId job)
           (:actor-id job)
           (:actor_id job))))))

(defn- job-permitted-for-event-actor?
  [job event]
  (if-let [actor-id (event-actor-id event)]
    (actor-scope/actor-allowed? (job-actor-claims job) actor-id)
    true))

(defn job-matches-event?
  [control job event]
  (let [payload (or (:payload event) {})
        event-kind (:eventKind event)
        ;; For scoring, consider all event kinds present in this event
        event-kinds (or (seq (:eventKinds event)) [event-kind])]
    (and (:enabled job)
         (= "event" (get-in job [:trigger :kind]))
         (= (str (get-in job [:source :kind])) (str (:sourceKind event)))
         (job-permitted-for-event-actor? job event)
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
        bot-user-id (or (:gatewayBotUserId message)
                        (discord-bot-user-id control))
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
                  :gatewayActorId (:gatewayActorId message)
                  :gatewayBotUserId (:gatewayBotUserId message)
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
  [_config]
  (discord-source/bind-gateways!
   {:policy-db (policy-db)
    :on-message! dispatch-discord-gateway-message!
    :on-voice-state! dispatch-voice-state-update!}))

(defn- dispatch-voice-state-update!
  [{:keys [action userId username guildId channelId oldChannelId newChannelId] :as data}]
  (when (not (str/blank? (str userId "")))
    ;; Track user voice states so message events can include the author's voice channel
    (when-not (str/blank? (str guildId ""))
      (if (str/blank? (str newChannelId ""))
        ;; User left/moved out - remove their voice state; clean up empty guild map
        (swap! source-state* (fn [state]
                               (let [updated (update-in state [:discord :voice-states guildId] dissoc userId)]
                                 (if (empty? (get-in updated [:discord :voice-states guildId]))
                                   (update-in updated [:discord :voice-states] dissoc guildId)
                                   updated))))
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
                                :newChannelId newChannelId
                                :gatewayActorId (:gatewayActorId data)
                                :gatewayBotUserId (:gatewayBotUserId data)}})))

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
  (discord-source/message-match-kind
   {:bot-user-id (discord-bot-user-id control)
    :content (:content message)
    :keyword? (and (not (job-match-all? job))
                   (matches-keywords? control job (:content message)))
    :created? (matches-event-kind? job "discord.message.created")
    :match-all? (job-match-all? job)}))

(defn- resolve-job-channel-ids!
  [control job]
  (discord-source/resolve-channel-ids!
   {:explicit-channels (job-channels control job)
    :guild-ids (job-guild-ids job)
    :publish-channels (job-publish-channels job)}))

(defn- execute-discord-patrol!
  [config control job]
  (let [limit (job-max-messages job 25)]
    (-> (resolve-job-channel-ids! control job)
        (.then (fn [channels]
                 (discord-source/execute-patrol!
                  {:config config
                   :channel-ids channels
                   :limit limit
                   :unseen-messages unseen-discord-messages
                   :remember-latest! remember-discord-latest!
                   :match-kind (partial discord-message-match-kind control job)
                   :dispatch-message! (partial dispatch-discord-message-event! control job)}))))))

(defn- summarize-discord-channel
  [channel-id messages]
  (discord-source/summarize-channel channel-id messages))

(defn- synthesis-channel-ids!
  [control job trigger-event]
  (-> (resolve-job-channel-ids! control job)
      (.then (fn [channels]
               (let [trigger-channel (some-> (get-in trigger-event [:payload :channelId]) str str/trim not-empty)]
                 (->> (concat channels (when trigger-channel [trigger-channel]))
                      distinct
                      vec))))))

(defn- discord-synthesis-trigger-summary!
  [trigger-event]
  (if trigger-event
    (-> (event-summary-text trigger-event)
        (.then (fn [txt]
                 (str "Triggering event:\n" txt))))
    (js/Promise.resolve nil)))

(defn- discord-synthesis-image-attachments
  [rows]
  (discord-source/image-attachments rows))

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
         publish-channels (job-publish-channels job)]
     (-> (synthesis-channel-ids! control job trigger-event)
         (.then (fn [channels]
                  (discord-source/execute-synthesis!
                   {:config config
                    :channel-ids channels
                    :limit limit
                    :dispatch-summary! (fn [rows]
                                         (discord-synthesis-dispatch-summary!
                                          config job publish-channels trigger-event channels rows))})))))))

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
  (discord-source/stop!)
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

(defn reset-runtime!
  [config]
  (let [live-config (or @runtime-state/config* config)
        pending-reload (or @reload-lock* (js/Promise.resolve nil))
        reset-control (-> (control-config/default-event-agent-control live-config)
                          disable-cron-jobs)
        redis-patterns ["event-agent:job-state:*"
                        "event-agent:user-job-state:*"
                        "event-agent:job-spec:*"
                        "event-agent:discord-last-seen:*"]]
    (-> pending-reload
        (.then (fn []
                 (when-let [t @reload-timer*]
                   (js/clearTimeout t)
                   (reset! reload-timer* nil))
                 (stop!)))
        (.then (fn []
                 (clear-runtime-state!)
                 (if-let [client (redis/get-client)]
                   (-> (js/Promise.all (clj->js (map #(redis-keys! client %) redis-patterns)))
                       (.then (fn [results]
                                (let [matched (->> (js/Array.from results)
                                                   (mapcat (fn [result]
                                                             (if (array? result)
                                                               (array-seq result)
                                                               result)))
                                                   distinct
                                                   vec)]
                                  (-> (delete-redis-keys! client matched)
                                      (.then (fn [deleted-count]
                                               (-> (delete-redis-keys! client [event-agent-job-dirty-redis-key])
                                                   (.then (fn [_]
                                                            {:deletedCount deleted-count}))))))))))
                   (js/Promise.resolve {:deletedCount 0}))))
        (.then (fn [{:keys [deletedCount]}]
                 (swap! runtime-state/config*
                        (fn [current]
                          (assoc (or current live-config) :event-agent-control reset-control)))
                 (-> (control-config/persist-event-agent-control! reset-control)
                     (.then (fn [_]
                              {:ok true
                               :deletedCount deletedCount
                               :disabledCronJobCount (count (filter #(= "cron" (get-in % [:trigger :kind])) (:jobs reset-control)))})))))
        (.catch (fn [err]
                  (clear-runtime-state!)
                  (js/Promise.reject err))))))

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
                           :contractSourceId (:contractSourceId job)
                           :contractSourceKind (:contractSourceKind job)
                           :contractSourceKey (:contractSourceKey job)
                           :trigger (:trigger job)
                           :source (:source job)
                           :scheduleLabel (events-cron/cadence-label (get-in job [:trigger :cadenceMinutes]))}
                          (get @job-state* (:id job) {:runCount 0 :lastStatus "none"})))
                 (:jobs control))}))

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

                     ;; Recover remaining state from Redis (job state, specs,
                     ;; last-seen) BEFORE the cron ticker starts. Otherwise a
                     ;; reload can boot with empty in-memory state, mark every
                     ;; cron job due "now", and duplicate scheduled runs.
                     (-> (recover-runtime-state! control)
                         (.then (fn [_]
                                  ;; Schedule background SQL flush task
                                  (schedule-flush-task!)

                                  ;; Bind Discord gateway for real-time message handling
                                  (bind-discord-gateway! config)

                                  ;; One scheduler ticker evaluates all cron jobs from
                                  ;; their persisted :nextRunAt timestamps. No per-job
                                  ;; intervals or boot-kick timeouts are registered.
                                  (schedule-events-cron-ticker! config)))))))
          (.catch (fn [err]
                    (println "[event-agents] failed to recover control config from Redis:" (.-message err))
                    ;; Fall through — start with defaults
                    (let [config (cfg)
                          control (control-config config)]
                      (println "[event-agents] starting with" (count (:jobs control)) "jobs (defaults)")
                      (schedule-flush-task!)
                      (bind-discord-gateway! config)
                      (schedule-events-cron-ticker! config))))))
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

(ns knoxx.backend.infra.routes.studio.discord-scan
  (:require-macros [knoxx.backend.macros :refer [defroute]])
  (:require [clojure.string :as str]
            [knoxx.backend.tools.media :as media]
            [knoxx.backend.tools.shared :refer [live-config]]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]))

;; -- Discord audio import helpers --

(def discord-scan-audio-extensions
  #{".mp3" ".wav" ".ogg" ".m4a" ".flac" ".aac" ".opus" ".wma" ".aiff" ".aif" ".mp4" ".webm"})

(def discord-scan-image-extensions
  #{".png" ".jpg" ".jpeg" ".gif" ".webp" ".bmp" ".svg" ".tif" ".tiff"})

(defn- discord-bot-token
  [config]
  (some-> (live-config config) :discord-bot-token str str/trim not-empty))

(defn- sleep!
  [ms]
  (js/Promise. (fn [resolve _reject]
                 (js/setTimeout resolve ms))))

(defn- discord-fetch-json!
  ([url token]
   (discord-fetch-json! url token 0))
  ([url token attempt]
   (-> (js/fetch url
                 #js {:method "GET"
                      :headers #js {"Authorization" (str "Bot " token)
                                    "Accept" "application/json"
                                    "User-Agent" "Knoxx-Studio/1.0"}})
       (.then (fn [resp]
                (if (.-ok resp)
                  (.json resp)
                  (-> (.text resp)
                      (.then (fn [text]
                               (if (and (= 429 (.-status resp)) (< attempt 3))
                                 (let [retry-after-ms (try
                                                        (let [parsed (js/JSON.parse text)
                                                              retry-after (or (aget parsed "retry_after") 1)]
                                                          (long (Math/ceil (* 1000 retry-after))))
                                                        (catch :default _ 1000))]
                                   (-> (sleep! retry-after-ms)
                                       (.then (fn [] (discord-fetch-json! url token (inc attempt))))))
                                 (throw (js/Error. (str "Discord API error " (.-status resp) ": " text)))))))))))))

(defn- safe-path-segment
  [value]
  (let [cleaned (some-> (str (or value ""))
                        str/trim
                        (str/replace #"[^A-Za-z0-9._-]+" "_")
                        (str/replace #"_+" "_")
                        (str/replace #"^_+|_+$" "")
                        not-empty)]
    (or cleaned "unknown")))

(defn- timestamp-token
  [value]
  (let [cleaned (some-> (str (or value ""))
                        (str/replace #"[^0-9T]+" "")
                        not-empty)]
    (or cleaned "unknown-time")))

(defn- timestamp-ms
  [value]
  (let [ms (.parse js/Date (str (or value "")))]
    (when-not (js/isNaN ms) ms)))

(defn- recent-enough?
  [cutoff-ms message]
  (if-let [ms (timestamp-ms (:timestamp message))]
    (>= ms cutoff-ms)
    true))

(defn- discord-audio-attachment?
  [attachment]
  (let [filename (some-> (:filename attachment) str str/lower-case)
        ext (when filename
              (some-> (re-find #"\.[^.]+$" filename) str/lower-case))
        content-type (some-> (:contentType attachment) str str/lower-case)]
    (or (some-> content-type (str/starts-with? "audio/"))
        (contains? discord-scan-audio-extensions ext)
        (and (some-> content-type (str/starts-with? "video/"))
             (contains? #{".mp4" ".webm"} ext)))))

(defn- discord-image-attachment?
  [attachment]
  (let [filename (some-> (:filename attachment) str str/lower-case)
        ext (when filename
              (some-> (re-find #"\.[^.]+$" filename) str/lower-case))
        content-type (some-> (:contentType attachment) str str/lower-case)]
    (or (some-> content-type (str/starts-with? "image/"))
        (contains? discord-scan-image-extensions ext))))

(defn- discord-list-guilds!
  [token]
  (-> (discord-fetch-json! "https://discord.com/api/v10/users/@me/guilds" token)
      (.then (fn [payload]
               (->> (if (array? payload) (array-seq payload) [])
                    (mapv (fn [guild]
                            {:id (or (aget guild "id") "")
                             :name (or (aget guild "name") "unknown-guild")})))))))

(defn- discord-list-channels!
  [token guild]
  (-> (discord-fetch-json! (str "https://discord.com/api/v10/guilds/" (:id guild) "/channels") token)
      (.then (fn [payload]
               (->> (if (array? payload) (array-seq payload) [])
                    (filter (fn [channel]
                              (contains? #{0 5 11 12} (aget channel "type"))))
                    (mapv (fn [channel]
                            {:id (or (aget channel "id") "")
                             :name (or (aget channel "name") "unknown-channel")
                             :guildId (:id guild)
                             :guildName (:name guild)})))))))

(defn- discord-fetch-channel-messages!
  [token channel-id before limit]
  (let [params (js/URLSearchParams.)]
    (.set params "limit" (str (max 1 (min 100 (or limit 100)))))
    (when (some? before)
      (.set params "before" (str before)))
    (-> (discord-fetch-json! (str "https://discord.com/api/v10/channels/" channel-id "/messages?" (.toString params)) token)
        (.then (fn [payload]
                 (->> (if (array? payload) (array-seq payload) [])
                      (mapv (fn [message]
                              {:id (or (aget message "id") "")
                               :channelId (or (aget message "channel_id") channel-id)
                               :content (or (aget message "content") "")
                               :authorId (or (aget message "author" "id") "")
                               :authorUsername (or (aget message "author" "username") "unknown")
                               :timestamp (or (aget message "timestamp") "")
                               :attachments (->> (if (array? (aget message "attachments"))
                                                   (array-seq (aget message "attachments"))
                                                   [])
                                                 (mapv (fn [attachment]
                                                         {:id (or (aget attachment "id") "")
                                                          :filename (or (aget attachment "filename") "attachment.bin")
                                                          :contentType (or (aget attachment "content_type") nil)
                                                          :size (or (aget attachment "size") 0)
                                                          :url (or (aget attachment "url") "")})))}))))))))

(defn- promise-reduce
  [items init step-fn]
  (reduce (fn [promise item]
            (.then promise (fn [state] (step-fn state item))))
          (js/Promise.resolve init)
          items))

(defn- collect-discord-scan-channels!
  [token channel-ids max-channels]
  (if (seq channel-ids)
    (js/Promise.resolve
     (vec (map (fn [channel-id]
                 {:id (str channel-id)
                  :name (str channel-id)
                  :guildId nil
                  :guildName "manual"})
               channel-ids)))
    (-> (discord-list-guilds! token)
        (.then (fn [guilds]
                 (-> (promise-reduce guilds []
                                     (fn [acc guild]
                                       (-> (discord-list-channels! token guild)
                                           (.then (fn [channels]
                                                    (into acc channels))))))
                     (.then (fn [channels]
                              (vec (take max-channels channels))))))))))

(defn- scan-channel-audio!
  [token channel {:keys [cutoff-ms pages-per-channel limit-per-page]}]
  (letfn [(step [before page messages-scanned attachments]
            (if (>= page pages-per-channel)
              (js/Promise.resolve {:messages-scanned messages-scanned
                                   :attachments attachments})
              (-> (discord-fetch-channel-messages! token (:id channel) before limit-per-page)
                  (.then (fn [messages]
                           (let [message-vec (vec messages)
                                 matching (->> message-vec
                                               (filter #(recent-enough? cutoff-ms %))
                                               (mapcat (fn [message]
                                                         (->> (:attachments message)
                                                              (filter discord-audio-attachment?)
                                                              (map (fn [attachment]
                                                                     (assoc attachment
                                                                            :guildId (:guildId channel)
                                                                            :guildName (:guildName channel)
                                                                            :channelId (:id channel)
                                                                            :channelName (:name channel)
                                                                            :messageId (:id message)
                                                                            :messageUrl (when (:guildId channel)
                                                                                          (str "https://discord.com/channels/"
                                                                                               (:guildId channel) "/" (:id channel) "/" (:id message)))
                                                                            :authorId (:authorId message)
                                                                            :authorUsername (:authorUsername message)
                                                                            :timestamp (:timestamp message)
                                                                            :content (:content message)))))))
                                               vec)
                                 next-attachments (into attachments matching)
                                 total-scanned (+ messages-scanned (count message-vec))
                                 oldest-id (:id (last message-vec))
                                 stop? (or (empty? message-vec)
                                           (< (count message-vec) limit-per-page)
                                           (every? #(not (recent-enough? cutoff-ms %)) message-vec))]
                             (if (or stop? (str/blank? (str oldest-id)))
                               {:messages-scanned total-scanned
                                :attachments next-attachments}
                               (step oldest-id (inc page) total-scanned next-attachments))))))))]
    (step nil 0 0 [])))

(defn- scan-channel-images!
  [token channel {:keys [cutoff-ms limit-per-page]}]
  (-> (discord-fetch-channel-messages! token (:id channel) nil limit-per-page)
      (.then (fn [messages]
               (let [message-vec (vec messages)
                     matching (->> message-vec
                                   (filter #(recent-enough? cutoff-ms %))
                                   (mapcat (fn [message]
                                             (->> (:attachments message)
                                                  (filter discord-image-attachment?)
                                                  (map (fn [attachment]
                                                         (assoc attachment
                                                                :guildId (:guildId channel)
                                                                :guildName (:guildName channel)
                                                                :channelId (:id channel)
                                                                :channelName (:name channel)
                                                                :messageId (:id message)
                                                                :messageUrl (when (:guildId channel)
                                                                              (str "https://discord.com/channels/"
                                                                                   (:guildId channel) "/" (:id channel) "/" (:id message)))
                                                                :authorId (:authorId message)
                                                                :authorUsername (:authorUsername message)
                                                                :timestamp (:timestamp message)
                                                                :content (:content message)))))))
                                   vec)]
                 {:messages-scanned (count message-vec)
                  :attachments matching})))))

(defn- fs-path-exists!
  [node-fs path]
  (-> (.stat node-fs path)
      (.then (fn [_] true))
      (.catch (fn [_] false))))

(defn- discord-audio-import-result
  ([status file-relative meta-relative attachment]
   (discord-audio-import-result status file-relative meta-relative attachment nil))
  ([status file-relative meta-relative attachment error]
   (cond-> {:status status
            :path file-relative
            :metadata_path meta-relative
            :message_id (:messageId attachment)
            :channel_id (:channelId attachment)
            :source_url (:url attachment)}
     error (assoc :error error))))

(defn- discord-audio-import-metadata
  [attachment loaded file-relative]
  {:kind "discord-audio-import"
   :imported_at (.toISOString (js/Date.))
   :source_url (:url attachment)
   :guild_id (:guildId attachment)
   :guild_name (:guildName attachment)
   :channel_id (:channelId attachment)
   :channel_name (:channelName attachment)
   :message_id (:messageId attachment)
   :message_url (:messageUrl attachment)
   :timestamp (:timestamp attachment)
   :author_id (:authorId attachment)
   :author_username (:authorUsername attachment)
   :content (:content attachment)
   :attachment_id (:id attachment)
   :filename (:filename attachment)
   :content_type (or (:contentType attachment) (:mime-type loaded))
   :size (:size loaded)
   :saved_path file-relative})

(defn- discord-image-import-metadata
  [attachment loaded file-relative]
  {:kind "discord-image-import"
   :imported_at (.toISOString (js/Date.))
   :source_url (:url attachment)
   :guild_id (:guildId attachment)
   :guild_name (:guildName attachment)
   :channel_id (:channelId attachment)
   :channel_name (:channelName attachment)
   :message_id (:messageId attachment)
   :message_url (:messageUrl attachment)
   :timestamp (:timestamp attachment)
   :author_id (:authorId attachment)
   :author_username (:authorUsername attachment)
   :content (:content attachment)
   :attachment_id (:id attachment)
   :filename (:filename attachment)
   :content_type (or (:contentType attachment) (:mime-type loaded))
   :size (:size loaded)
   :saved_path file-relative})

(defn- import-audio-attachment!
  [runtime config import-root attachment]
  (let [workspace-root (:workspace-root config)
        day (let [ts (str (or (:timestamp attachment) ""))]
              (if (>= (count ts) 10) (subs ts 0 10) "unknown-date"))
        guild-segment (safe-path-segment (or (:guildName attachment) (:guildId attachment) "discord"))
        channel-segment (safe-path-segment (or (:channelName attachment) (:channelId attachment) "channel"))
        filename (safe-path-segment (:filename attachment))
        file-token (timestamp-token (:timestamp attachment))
        target-name (str file-token "__" (safe-path-segment (:messageId attachment)) "__" (safe-path-segment (:id attachment)) "__" filename)
        dir-absolute (.join path workspace-root import-root guild-segment channel-segment day)
        file-absolute (.join path dir-absolute target-name)
        file-relative (str import-root "/" guild-segment "/" channel-segment "/" day "/" target-name)
        meta-absolute (str file-absolute ".json")
        meta-relative (str file-relative ".json")]
    (-> (fs-path-exists! fs file-absolute)
        (.then
         (fn [exists?]
           (if exists?
             (discord-audio-import-result "skipped" file-relative meta-relative attachment)
             (-> (media/load-media-source! runtime (live-config config) (:url attachment) media/audio-render-max-bytes)
                 (.then
                  (fn [loaded]
                    (let [metadata (discord-audio-import-metadata attachment loaded file-relative)]
                      (-> (media/fs-mkdir! fs dir-absolute #js {:recursive true})
                          (.then (fn [] (media/fs-write-file! fs file-absolute (:buffer loaded))))
                          (.then (fn []
                                   (media/fs-write-file! fs meta-absolute (.stringify js/JSON (clj->js metadata) nil 2) "utf8")))
                          (.then (fn []
                                   (discord-audio-import-result "imported" file-relative meta-relative attachment))))))))))))))

(defn- import-image-attachment!
  [runtime config import-root attachment]
  (let [workspace-root (:workspace-root config)
        day (let [ts (str (or (:timestamp attachment) ""))]
              (if (>= (count ts) 10) (subs ts 0 10) "unknown-date"))
        guild-segment (safe-path-segment (or (:guildName attachment) (:guildId attachment) "discord"))
        channel-segment (safe-path-segment (or (:channelName attachment) (:channelId attachment) "channel"))
        filename (safe-path-segment (:filename attachment))
        file-token (timestamp-token (:timestamp attachment))
        target-name (str file-token "__" (safe-path-segment (:messageId attachment)) "__" (safe-path-segment (:id attachment)) "__" filename)
        dir-absolute (.join path workspace-root import-root guild-segment channel-segment day)
        file-absolute (.join path dir-absolute target-name)
        file-relative (str import-root "/" guild-segment "/" channel-segment "/" day "/" target-name)
        meta-absolute (str file-absolute ".json")
        meta-relative (str file-relative ".json")]
    (-> (fs-path-exists! fs file-absolute)
        (.then
         (fn [exists?]
           (if exists?
             (discord-audio-import-result "skipped" file-relative meta-relative attachment)
             (-> (media/load-media-source! runtime (live-config config) (:url attachment) media/multimodal-upload-max-bytes)
                 (.then
                  (fn [loaded]
                    (let [metadata (discord-image-import-metadata attachment loaded file-relative)]
                      (-> (media/fs-mkdir! fs dir-absolute #js {:recursive true})
                          (.then (fn [] (media/fs-write-file! fs file-absolute (:buffer loaded))))
                          (.then (fn []
                                   (media/fs-write-file! fs meta-absolute (.stringify js/JSON (clj->js metadata) nil 2) "utf8")))
                          (.then (fn []
                                   (discord-audio-import-result "imported" file-relative meta-relative attachment))))))))))))))

(defn- write-scan-manifest!
  [_runtime config import-root manifest]
  (let [workspace-root (:workspace-root config)
        stamp (timestamp-token (:scanned_at manifest))
        dir-absolute (.join path workspace-root import-root "_scan_logs")
        file-name (str "scan-" stamp ".json")
        file-absolute (.join path dir-absolute file-name)
        file-relative (str import-root "/_scan_logs/" file-name)]
    (-> (media/fs-mkdir! fs dir-absolute #js {:recursive true})
        (.then (fn []
                 (media/fs-write-file! fs file-absolute (.stringify js/JSON (clj->js manifest) nil 2) "utf8")))
        (.then (fn [] file-relative)))))

(defn- bounded-body-int
  [body key-name default min-value max-value]
  (let [value (js/parseInt (str (or (aget body key-name) default)) 10)]
    (if (js/isNaN value)
      default
      (max min-value (min value max-value)))))

(defn- scan-request-options
  [body default-import-root]
  (let [channel-ids (vec (remove str/blank? (map str (js->clj (or (aget body "channel_ids") #js [])))))
        since-hours (bounded-body-int body "since_hours" 336 1 8760)
        pages-per-channel (bounded-body-int body "pages_per_channel" 2 1 20)
        limit-per-page (bounded-body-int body "limit_per_page" 100 1 100)
        max-channels (bounded-body-int body "max_channels" 50 1 500)]
    {:channel-ids channel-ids
     :max-channels max-channels
     :import-root (or (media/normalize-tool-path-arg (aget body "import_root")) default-import-root)
     :scan-options {:cutoff-ms (- (.now js/Date) (* since-hours 60 60 1000))
                    :pages-per-channel pages-per-channel
                    :limit-per-page limit-per-page}}))

(defn- collect-attachments!
  [token channels scan-channel! scan-options]
  (promise-reduce channels
                  {:messages-scanned 0 :attachments []}
                  (fn [state channel]
                    (-> (scan-channel! token channel scan-options)
                        (.then (fn [result]
                                 {:messages-scanned (+ (:messages-scanned state) (:messages-scanned result))
                                  :attachments (into (:attachments state) (:attachments result))}))))))

(defn- import-attachment-results!
  [runtime config import-root attachments import-attachment!]
  (promise-reduce attachments []
                  (fn [results attachment]
                    (-> (import-attachment! runtime config import-root attachment)
                        (.then (fn [result]
                                 (conj results result)))
                        (.catch (fn [error]
                                  (conj results (discord-audio-import-result "failed"
                                                                             ""
                                                                             ""
                                                                             attachment
                                                                             (or (.-message error) (str error))))))))))

(defn- scan-summary
  [import-root channels messages-scanned attachments results]
  {:ok true
   :scanned_at (.toISOString (js/Date.))
   :import_root import-root
   :channels_scanned (count channels)
   :messages_scanned messages-scanned
   :attachments_found (count attachments)
   :imported_count (count (filter #(= "imported" (:status %)) results))
   :skipped_count (count (filter #(= "skipped" (:status %)) results))
   :failed_count (count (filter #(= "failed" (:status %)) results))
   :channels (mapv (fn [channel]
                     (select-keys channel [:id :name :guildId :guildName]))
                   (take 50 channels))
   :results (vec results)})

(defn- run-discord-media-scan!
  [runtime config {:keys [token channel-ids max-channels import-root scan-options]} scan-channel! import-attachment!]
  (-> (collect-discord-scan-channels! token channel-ids max-channels)
      (.then (fn [channels]
               (-> (collect-attachments! token channels scan-channel! scan-options)
                   (.then (fn [{:keys [messages-scanned attachments]}]
                            (-> (import-attachment-results! runtime config import-root attachments import-attachment!)
                                (.then (fn [results]
                                         {:channels channels
                                          :messages-scanned messages-scanned
                                          :attachments attachments
                                          :results results}))))))))))

(defn- handle-discord-media-scan!
  [runtime config reply body default-import-root scan-channel! import-attachment! failure-message]
  (let [token (discord-bot-token config)
        options (assoc (scan-request-options body default-import-root) :token token)]
    (if-not token
      (json-response! reply 503 {:detail "Discord bot token is not configured"})
      (-> (run-discord-media-scan! runtime config options scan-channel! import-attachment!)
          (.then (fn [{:keys [channels messages-scanned attachments results]}]
                   (let [summary (scan-summary (:import-root options) channels messages-scanned attachments results)]
                     (-> (write-scan-manifest! runtime config (:import-root options) summary)
                         (.then (fn [manifest-path]
                                  (json-response! reply 200 (assoc summary :manifest_path manifest-path))))))))
          (.catch (fn [err]
                    (json-response! reply 500 {:detail (str failure-message err)})))))))

(defroute studio-discord-audio-scan! []
  "POST" "/api/studio/discord-audio-scan"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (handle-discord-media-scan! runtime
                              config
                              reply
                              (or (aget request "body") #js {})
                              "Audio/discord-imports"
                              scan-channel-audio!
                              import-audio-attachment!
                              "Discord audio scan failed: "))

(defroute studio-discord-image-scan! []
  "POST" "/api/studio/discord-image-scan"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (handle-discord-media-scan! runtime
                              config
                              reply
                              (or (aget request "body") #js {})
                              "discord/images"
                              scan-channel-images!
                              import-image-attachment!
                              "Discord image scan failed: "))

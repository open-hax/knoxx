(ns knoxx.backend.studio-routes
  (:require-macros [knoxx.backend.macros :refer [defroute]])
  (:require [clojure.string :as str]
            [knoxx.backend.tools.media :as media]
            [knoxx.backend.tools.shared :refer [live-config]]
            [knoxx.backend.audio-labels :as labels]))

;; -- Audio file discovery helpers --

(defn- audio-extensions [] #{".mp3" ".wav" ".ogg" ".m4a" ".flac" ".aac" ".opus" ".wma"})

(defn- audio-mime-type [ext]
  (case ext ".mp3" "audio/mpeg" ".wav" "audio/wav" ".ogg" "audio/ogg"
            ".m4a" "audio/mp4" ".flac" "audio/flac" ".aac" "audio/aac"
            ".opus" "audio/opus" "audio/mpeg"))

(declare walk-audio-files!)

(defn- process-entry [node-fs node-path root-dir base-relative depth max-depth entry]
  (let [nm (.-name entry)
        abs (.join node-path root-dir nm)
        rel (if (str/blank? base-relative) nm (str base-relative "/" nm))]
    (cond
      (str/starts-with? nm ".")
      (.resolve js/Promise [])

      (.isDirectory entry)
      (walk-audio-files! node-fs node-path abs rel (inc depth) max-depth)

      :else
      (let [ext (str/lower-case (or (some-> (.extname node-path nm) str/trim) ""))]
        (if (contains? (audio-extensions) ext)
          (-> (.stat node-fs abs)
              (.then (fn [s] [{:name nm :path rel :ext ext :size (.-size s) :modified (-> (.-mtime s) (.getTime)) :mime (audio-mime-type ext)}]))
              (.catch (fn [_] [])))
          (.resolve js/Promise []))))))

(defn- walk-audio-files! [node-fs node-path root-dir base-relative depth max-depth]
  (if (> depth max-depth)
    (.resolve js/Promise [])
    (-> (.readdir node-fs root-dir #js {:withFileTypes true})
        (.then (fn [entries]
                 (let [promises (mapv #(process-entry node-fs node-path root-dir base-relative depth max-depth %) (vec (array-seq entries)))]
                   (-> (.all js/Promise (into-array promises))
                       (.then (fn [r] (vec (mapcat identity r))))))))
        (.catch (fn [_] [])))))

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
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        workspace-root (:workspace-root config)
        day (let [ts (str (or (:timestamp attachment) ""))]
              (if (>= (count ts) 10) (subs ts 0 10) "unknown-date"))
        guild-segment (safe-path-segment (or (:guildName attachment) (:guildId attachment) "discord"))
        channel-segment (safe-path-segment (or (:channelName attachment) (:channelId attachment) "channel"))
        filename (safe-path-segment (:filename attachment))
        file-token (timestamp-token (:timestamp attachment))
        target-name (str file-token "__" (safe-path-segment (:messageId attachment)) "__" (safe-path-segment (:id attachment)) "__" filename)
        dir-absolute (.join node-path workspace-root import-root guild-segment channel-segment day)
        file-absolute (.join node-path dir-absolute target-name)
        file-relative (str import-root "/" guild-segment "/" channel-segment "/" day "/" target-name)
        meta-absolute (str file-absolute ".json")
        meta-relative (str file-relative ".json")]
    (-> (fs-path-exists! node-fs file-absolute)
        (.then
         (fn [exists?]
           (if exists?
             (discord-audio-import-result "skipped" file-relative meta-relative attachment)
             (-> (media/load-media-source! runtime (live-config config) (:url attachment) media/audio-render-max-bytes)
                 (.then
                  (fn [loaded]
                    (let [metadata (discord-audio-import-metadata attachment loaded file-relative)]
                      (-> (media/fs-mkdir! node-fs dir-absolute #js {:recursive true})
                          (.then (fn [] (media/fs-write-file! node-fs file-absolute (:buffer loaded))))
                          (.then (fn []
                                   (media/fs-write-file! node-fs meta-absolute (.stringify js/JSON (clj->js metadata) nil 2) "utf8")))
                          (.then (fn []
                                   (discord-audio-import-result "imported" file-relative meta-relative attachment))))))))))))))

(defn- import-image-attachment!
  [runtime config import-root attachment]
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        workspace-root (:workspace-root config)
        day (let [ts (str (or (:timestamp attachment) ""))]
              (if (>= (count ts) 10) (subs ts 0 10) "unknown-date"))
        guild-segment (safe-path-segment (or (:guildName attachment) (:guildId attachment) "discord"))
        channel-segment (safe-path-segment (or (:channelName attachment) (:channelId attachment) "channel"))
        filename (safe-path-segment (:filename attachment))
        file-token (timestamp-token (:timestamp attachment))
        target-name (str file-token "__" (safe-path-segment (:messageId attachment)) "__" (safe-path-segment (:id attachment)) "__" filename)
        dir-absolute (.join node-path workspace-root import-root guild-segment channel-segment day)
        file-absolute (.join node-path dir-absolute target-name)
        file-relative (str import-root "/" guild-segment "/" channel-segment "/" day "/" target-name)
        meta-absolute (str file-absolute ".json")
        meta-relative (str file-relative ".json")]
    (-> (fs-path-exists! node-fs file-absolute)
        (.then
         (fn [exists?]
           (if exists?
             (discord-audio-import-result "skipped" file-relative meta-relative attachment)
             (-> (media/load-media-source! runtime (live-config config) (:url attachment) media/multimodal-upload-max-bytes)
                 (.then
                  (fn [loaded]
                    (let [metadata (discord-image-import-metadata attachment loaded file-relative)]
                      (-> (media/fs-mkdir! node-fs dir-absolute #js {:recursive true})
                          (.then (fn [] (media/fs-write-file! node-fs file-absolute (:buffer loaded))))
                          (.then (fn []
                                   (media/fs-write-file! node-fs meta-absolute (.stringify js/JSON (clj->js metadata) nil 2) "utf8")))
                          (.then (fn []
                                   (discord-audio-import-result "imported" file-relative meta-relative attachment))))))))))))))

(defn- write-scan-manifest!
  [runtime config import-root manifest]
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        workspace-root (:workspace-root config)
        stamp (timestamp-token (:scanned_at manifest))
        dir-absolute (.join node-path workspace-root import-root "_scan_logs")
        file-name (str "scan-" stamp ".json")
        file-absolute (.join node-path dir-absolute file-name)
        file-relative (str import-root "/_scan_logs/" file-name)]
    (-> (media/fs-mkdir! node-fs dir-absolute #js {:recursive true})
        (.then (fn []
                 (media/fs-write-file! node-fs file-absolute (.stringify js/JSON (clj->js manifest) nil 2) "utf8")))
        (.then (fn [] file-relative)))))

;; -- Routes --

(defroute studio-audio-library! [policy-db]
  "GET" "/api/studio/audio-library"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        subpath (or (aget request "query" "path") ".")
        max-depth (let [d (js/parseInt (or (aget request "query" "depth") "3") 10)]
                    (if (js/isNaN d) 3 (min d 8)))
        ;; For root path, use workspace root directly to avoid path validation issues
        is-root (or (= subpath ".") (= subpath "") (= subpath "/"))
        absolute (if is-root
                   (:workspace-root config)
                   (let [normalized (media/normalize-tool-path-arg subpath)
                         {:keys [absolute]} (media/resolve-workspace-media-path runtime config normalized)]
                     absolute))
        base-relative (if is-root "" subpath)]
    (-> (walk-audio-files! node-fs node-path absolute base-relative 0 max-depth)
        (.then (fn [files]
                 (let [sorted (vec (sort-by :modified > files))]
                   (json-response! reply 200 {:ok true :root subpath :count (count sorted) :files sorted}))))
        (.catch (fn [err] (json-response! reply 500 {:detail (str "Scan failed: " err)}))))))

(defroute studio-state-get! [policy-db]
  "GET" "/api/studio/state"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [db (policy-db runtime)]
    (if db
      (let [user-id (or (:user-id ctx) (some-> ctx :user :id))
            org-id (or (:org-id ctx) (some-> ctx :org :id))
            kind (or (aget request "query" "kind") "player")]
        (if (and user-id org-id)
          (-> (.query db "SELECT state_json FROM studio_state WHERE user_id = $1 AND org_id = $2 AND kind = $3" #js [user-id org-id kind])
              (.then (fn [res]
                       (let [row (some-> res .-rows (aget 0))]
                         (json-response! reply 200 {:ok true :state (if row (js->clj (.-state_json row) :keywordize-keys true) {})}))))
              (.catch (fn [err] (json-response! reply 500 {:detail (str "Load failed: " err)}))))
          (json-response! reply 200 {:ok true :state {}})))
      (json-response! reply 200 {:ok true :state {}}))))

(defroute studio-state-put! [policy-db]
  "PUT" "/api/studio/state"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [db (policy-db runtime)]
    (if db
      (let [user-id (or (:user-id ctx) (some-> ctx :user :id))
            org-id (or (:org-id ctx) (some-> ctx :org :id))
            body (or (aget request "body") #js {})
            kind (or (aget body "kind") "player")
            state (js->clj (or (aget body "state") #js {}) :keywordize-keys true)]
        (if (and user-id org-id)
          (-> (.query db "INSERT INTO studio_state (user_id,org_id,kind,state_json) VALUES ($1,$2,$3,$4::jsonb) ON CONFLICT (user_id,org_id,kind) DO UPDATE SET state_json=EXCLUDED.state_json, updated_at=NOW() RETURNING *" #js [user-id org-id kind (.stringify js/JSON (clj->js state))])
              (.then (fn [_] (json-response! reply 200 {:ok true :saved true})))
              (.catch (fn [err] (json-response! reply 500 {:detail (str "Save failed: " err)}))))
          (json-response! reply 400 {:detail "User context required"})))
      (json-response! reply 503 {:detail "Database not configured"}))))

(defroute studio-playlist-get! [policy-db]
  "GET" "/api/studio/playlist"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [db (policy-db runtime)]
    (if db
      (let [user-id (or (:user-id ctx) (some-> ctx :user :id))
            org-id (or (:org-id ctx) (some-> ctx :org :id))]
        (if (and user-id org-id)
          (-> (.query db "SELECT state_json FROM studio_state WHERE user_id=$1 AND org_id=$2 AND kind='playlist'" #js [user-id org-id])
              (.then (fn [res]
                       (let [row (some-> res .-rows (aget 0))
                             state (if row (js->clj (.-state_json row) :keywordize-keys true) {})]
                         (json-response! reply 200 {:ok true :playlist (or (:items state) [])}))))
              (.catch (fn [err] (json-response! reply 500 {:detail (str "Load failed: " err)}))))
          (json-response! reply 200 {:ok true :playlist []})))
      (json-response! reply 200 {:ok true :playlist []}))))

(defroute studio-playlist-put! [policy-db]
  "PUT" "/api/studio/playlist"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [db (policy-db runtime)]
    (if db
      (let [user-id (or (:user-id ctx) (some-> ctx :user :id))
            org-id (or (:org-id ctx) (some-> ctx :org :id))
            body (or (aget request "body") #js {})
            items (js->clj (or (aget body "items") #js []) :keywordize-keys true)]
        (if (and user-id org-id)
          (-> (.query db "INSERT INTO studio_state (user_id,org_id,kind,state_json) VALUES ($1,$2,'playlist',$3::jsonb) ON CONFLICT (user_id,org_id,kind) DO UPDATE SET state_json=EXCLUDED.state_json, updated_at=NOW()" #js [user-id org-id (.stringify js/JSON (clj->js {:items items}))])
              (.then (fn [_] (json-response! reply 200 {:ok true :saved true :count (count items)})))
              (.catch (fn [err] (json-response! reply 500 {:detail (str "Save failed: " err)}))))
          (json-response! reply 400 {:detail "User context required"})))
      (json-response! reply 503 {:detail "Database not configured"}))))

(defroute studio-stream! []
  "GET" "/api/studio/stream"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        raw-path (or (aget request "query" "path") "")
        normalized (media/normalize-tool-path-arg raw-path)
        {:keys [absolute relative]} (media/resolve-workspace-media-path runtime config normalized)
        mime-type (or (media/workspace-media-mime-type relative) "audio/mpeg")]
    (-> (media/fs-stat! node-fs absolute)
        (.then (fn [stat]
                 (when-not (.isFile stat) (throw (js/Error. (str relative " is not a file"))))
                 (let [total-size (.-size stat)
                       filename (media/path-basename node-path absolute)
                       safe? (every? (fn [c] (let [n (.charCodeAt c 0)] (and (>= n 32) (<= n 126)))) (str filename))
                       disp (if safe? (str "inline; filename=\"" filename "\"") (str "inline; filename*=UTF-8''" (js/encodeURIComponent filename)))]
                   (.header reply "Content-Type" mime-type)
                   (.header reply "Accept-Ranges" "bytes")
                   (.header reply "Cache-Control" "private, max-age=0")
                   (.header reply "Content-Disposition" disp)
                   (.header reply "Content-Length" (str total-size))
                   (.send reply (.createReadStream node-fs absolute)))))
        (.catch (fn [err] (json-response! reply 404 {:detail (str err)}))))))

(defroute studio-save-m3u! []
  "POST" "/api/studio/save-m3u"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [body (or (aget request "body") #js {})
        items (js->clj (or (aget body "items") #js []) :keywordize-keys true)
        name (or (aget body "name") (str "playlist-" (.toISOString (js/Date.))))
        node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        ;; Build M3U content
        m3u-lines (concat
                   ["#EXTM3U"]
                   (mapcat (fn [item]
                             [(str "#EXTINF:-1," (:name item))
                              (:path item)])
                           items))
        m3u-content (str/join "\n" m3u-lines)
        ;; Save to Music/playlists/
        normalized (media/normalize-tool-path-arg "Music/playlists")
        {:keys [absolute]} (media/resolve-workspace-media-path runtime config normalized)
        safe-name (str/replace name #"[^a-zA-Z0-9_-]" "_")
        file-path (.join node-path absolute (str safe-name ".m3u"))]
    (-> (.mkdir node-fs absolute #js {:recursive true})
        (.then (fn [_] (.writeFile node-fs file-path m3u-content "utf8")))
        (.then (fn [_] (json-response! reply 200 {:ok true :path (str "Music/playlists/" safe-name ".m3u") :count (count items)})))
        (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed to save playlist: " err)}))))))

(defroute studio-save-m3u-download! []
  "POST" "/api/studio/download-m3u"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [body (or (aget request "body") #js {})
        items (js->clj (or (aget body "items") #js []) :keywordize-keys true)
        name (or (aget body "name") "playlist")
        ;; Build M3U content
        m3u-lines (concat
                   ["#EXTM3U"]
                   (mapcat (fn [item]
                             [(str "#EXTINF:-1," (:name item))
                              (:path item)])
                           items))
        m3u-content (str/join "\n" m3u-lines)
        safe-name (str/replace name #"[^a-zA-Z0-9_-]" "_")]
    (.header reply "Content-Type" "audio/x-mpegurl")
    (.header reply "Content-Disposition" (str "attachment; filename=\"" safe-name ".m3u\""))
    (.send reply m3u-content)))

(defroute studio-load-m3u! []
  "GET" "/api/studio/load-m3u"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        file-path (aget request "query" "path")]
    (if-not file-path
      (json-response! reply 400 {:detail "Missing path parameter"})
      (-> (.readFile node-fs file-path "utf8")
          (.then (fn [content]
                   (let [lines (str/split-lines content)
                         ;; Parse M3U format
                         items (loop [remaining lines
                                      result []
                                      current-name nil]
                                 (if (empty? remaining)
                                   result
                                   (let [line (str/trim (first remaining))
                                         rest-lines (rest remaining)]
                                     (cond
                                       ;; Skip empty lines and comments
                                       (or (str/blank? line)
                                           (str/starts-with? line "#EXTM3U"))
                                       (recur rest-lines result current-name)
                                       ;; Parse EXTINF
                                       (str/starts-with? line "#EXTINF:")
                                       (let [name-part (second (str/split line #"," 2))]
                                         (recur rest-lines result (or name-part "Unknown")))
                                       ;; This is a file path
                                       :else
                                       (recur rest-lines
                                              (conj result {:path line
                                                            :name (or current-name
                                                                      (.basename node-path line))})
                                              nil)))))
                         playlist-name (.basename node-path file-path)
                         clean-name (str/replace playlist-name #"\.m3u$" "")]
                     (json-response! reply 200 {:ok true :name clean-name :items (vec items)}))))
          (.catch (fn [err]
                    (json-response! reply 500 {:detail (str "Failed to load M3U: " err)})))))))

(defroute studio-list-playlists! []
  "GET" "/api/studio/playlists"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        workspace-root (:workspace-root config)
        playlists-dir (.join node-path workspace-root "Music" "playlists")]
    (-> (.readdir node-fs playlists-dir)
        (.then (fn [files]
                 (let [m3u-files (->> (js->clj files)
                                      (filter #(str/ends-with? % ".m3u"))
                                      (map (fn [filename]
                                             {:name (str/replace filename #"\.m3u$" "")
                                              :path (.join node-path playlists-dir filename)
                                              :filename filename})))]
                   (json-response! reply 200 {:ok true :playlists (vec m3u-files)}))))
        (.catch (fn [_err]
                  ;; Directory doesn't exist or other error - return empty list
                  (json-response! reply 200 {:ok true :playlists []}))))))

;; -- Label routes --

(defroute studio-labels-get! []
  "GET" "/api/studio/labels"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [node-fs (aget runtime "fs")
        workspace-root (:workspace-root config)
        file-path (aget request "query" "path")
        all? (aget request "query" "all")]
    (cond
      all?
      (-> (labels/get-all-labels node-fs workspace-root)
          (.then (fn [all-labels] (json-response! reply 200 {:ok true :labels all-labels})))
          (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed: " err)}))))
      file-path
      (-> (labels/get-labels node-fs workspace-root file-path)
          (.then (fn [file-labels] (json-response! reply 200 {:ok true :path file-path :labels file-labels})))
          (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed: " err)}))))
      :else
      (json-response! reply 400 {:detail "Missing path or all parameter"}))))

(defroute studio-labels-add! []
  "POST" "/api/studio/labels/add"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [node-fs (aget runtime "fs")
        workspace-root (:workspace-root config)
        body (or (aget request "body") #js {})
        file-path (aget body "path")
        label (aget body "label")]
    (if (and file-path label)
      (-> (labels/add-label! node-fs workspace-root file-path label)
          (.then (fn [updated] (json-response! reply 200 {:ok true :path file-path :labels updated})))
          (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed: " err)}))))
      (json-response! reply 400 {:detail "Missing path or label"}))))

(defroute studio-labels-remove! []
  "POST" "/api/studio/labels/remove"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [node-fs (aget runtime "fs")
        workspace-root (:workspace-root config)
        body (or (aget request "body") #js {})
        file-path (aget body "path")
        label (aget body "label")]
    (if (and file-path label)
      (-> (labels/remove-label! node-fs workspace-root file-path label)
          (.then (fn [updated] (json-response! reply 200 {:ok true :path file-path :labels updated})))
          (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed: " err)}))))
      (json-response! reply 400 {:detail "Missing path or label"}))))

(defroute studio-labels-by-label! []
  "GET" "/api/studio/labels/by-label"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [node-fs (aget runtime "fs")
        workspace-root (:workspace-root config)
        label (aget request "query" "label")]
    (if label
      (-> (labels/get-files-by-label node-fs workspace-root label)
          (.then (fn [files] (json-response! reply 200 {:ok true :label label :files files})))
          (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed: " err)}))))
      (json-response! reply 400 {:detail "Missing label parameter"}))))

(defroute studio-sync-symlinks! []
  "POST" "/api/studio/sync-symlinks"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        workspace-root (:workspace-root config)]
    (-> (labels/sync-symlinks! node-fs node-path workspace-root)
        (.then (fn [count] (json-response! reply 200 {:ok true :symlinks count})))
        (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed to sync symlinks: " err)}))))))

;; -- Audio asset routes (waveform/spectrogram) --

(defroute studio-audio-asset-get! [policy-db]
  "GET" "/api/studio/audio-asset"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [db (policy-db runtime)
        audio-path (aget request "query" "path")
        asset-type (aget request "query" "type")]
    (if (and audio-path asset-type)
      (-> (.query db "SELECT image_data, mime_type, width, height FROM studio_audio_assets WHERE audio_path = $1 AND asset_type = $2" #js [audio-path asset-type])
          (.then (fn [res]
                   (if (.-rows res)
                     (let [row (first (js->clj (.-rows res)))]
                       (if row
                         (do
                           (.header reply "Content-Type" (get row "mime_type" "image/png"))
                           (.header reply "Cache-Control" "public, max-age=86400")
                           (.send reply (aget row "image_data")))
                         (json-response! reply 404 {:detail "Asset not found"})))
                     (json-response! reply 404 {:detail "Asset not found"}))))
          (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed: " err)}))))
      (json-response! reply 400 {:detail "Missing path or type"}))))

(defroute studio-audio-asset-save! [policy-db]
  "POST" "/api/studio/audio-asset"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [db (policy-db runtime)
        body (or (aget request "body") #js {})
        audio-path (aget body "path")
        asset-type (aget body "type")
        image-data (aget body "imageData")
        mime-type (or (aget body "mimeType") "image/png")
        width (aget body "width")
        height (aget body "height")]
    (if (and audio-path asset-type image-data)
      (let [buffer (js/Buffer.from image-data "base64")]
        (-> (.query db "INSERT INTO studio_audio_assets (audio_path, asset_type, image_data, mime_type, width, height) VALUES ($1, $2, $3, $4, $5, $6) ON CONFLICT (audio_path, asset_type) DO UPDATE SET image_data = $3, mime_type = $4, width = $5, height = $6, created_at = NOW()"
                    #js [audio-path asset-type buffer mime-type width height])
            (.then (fn [_] (json-response! reply 200 {:ok true :path audio-path :type asset-type})))
            (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed: " err)})))))
      (json-response! reply 400 {:detail "Missing path, type, or imageData"}))))

(defroute studio-discord-audio-scan! []
  "POST" "/api/studio/discord-audio-scan"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [body (or (aget request "body") #js {})
        channel-ids (vec (remove str/blank? (map str (js->clj (or (aget body "channel_ids") #js [])))))
        since-hours (let [value (js/parseInt (str (or (aget body "since_hours") 336)) 10)]
                      (if (js/isNaN value) 336 (max 1 (min value 8760))))
        pages-per-channel (let [value (js/parseInt (str (or (aget body "pages_per_channel") 2)) 10)]
                            (if (js/isNaN value) 2 (max 1 (min value 20))))
        limit-per-page (let [value (js/parseInt (str (or (aget body "limit_per_page") 100)) 10)]
                         (if (js/isNaN value) 100 (max 1 (min value 100))))
        max-channels (let [value (js/parseInt (str (or (aget body "max_channels") 50)) 10)]
                       (if (js/isNaN value) 50 (max 1 (min value 500))))
         import-root (or (media/normalize-tool-path-arg (aget body "import_root")) "Audio/discord-imports")
        token (discord-bot-token config)
        cutoff-ms (- (.now js/Date) (* since-hours 60 60 1000))]
    (if-not token
      (json-response! reply 503 {:detail "Discord bot token is not configured"})
      (-> (collect-discord-scan-channels! token channel-ids max-channels)
          (.then (fn [channels]
                   (-> (promise-reduce channels
                                       {:messages-scanned 0 :attachments []}
                                       (fn [state channel]
                                         (-> (scan-channel-audio! token channel {:cutoff-ms cutoff-ms
                                                                                 :pages-per-channel pages-per-channel
                                                                                 :limit-per-page limit-per-page})
                                             (.then (fn [result]
                                                      {:messages-scanned (+ (:messages-scanned state) (:messages-scanned result))
                                                       :attachments (into (:attachments state) (:attachments result))})))))
                       (.then (fn [{:keys [messages-scanned attachments]}]
                                (-> (promise-reduce attachments []
                                                    (fn [results attachment]
                                                      (-> (import-audio-attachment! runtime config import-root attachment)
                                                          (.then (fn [result]
                                                                   (conj results result)))
                                                          (.catch (fn [error]
                                                                    (conj results (discord-audio-import-result "failed"
                                                                                                               ""
                                                                                                               ""
                                                                                                               attachment
                                                                                                               (.-message error))))))))
                                    (.then (fn [results]
                                             (let [summary {:ok true
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
                                                            :results (vec results)}]
                                               (-> (write-scan-manifest! runtime config import-root summary)
                                                   (.then (fn [manifest-path]
                                                            (json-response! reply 200 (assoc summary :manifest_path manifest-path))))))))))))))
          (.catch (fn [err]
                    (json-response! reply 500 {:detail (str "Discord audio scan failed: " err)})))))))

(defroute studio-discord-image-scan! []
  "POST" "/api/studio/discord-image-scan"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [body (or (aget request "body") #js {})
        channel-ids (vec (remove str/blank? (map str (js->clj (or (aget body "channel_ids") #js [])))))
        since-hours (let [value (js/parseInt (str (or (aget body "since_hours") 336)) 10)]
                      (if (js/isNaN value) 336 (max 1 (min value 8760))))
        pages-per-channel (let [value (js/parseInt (str (or (aget body "pages_per_channel") 2)) 10)]
                            (if (js/isNaN value) 2 (max 1 (min value 20))))
        limit-per-page (let [value (js/parseInt (str (or (aget body "limit_per_page") 100)) 10)]
                         (if (js/isNaN value) 100 (max 1 (min value 100))))
        max-channels (let [value (js/parseInt (str (or (aget body "max_channels") 50)) 10)]
                       (if (js/isNaN value) 50 (max 1 (min value 500))))
        import-root (or (media/normalize-tool-path-arg (aget body "import_root")) "discord/images")
        token (discord-bot-token config)
        cutoff-ms (- (.now js/Date) (* since-hours 60 60 1000))]
    (if-not token
      (json-response! reply 503 {:detail "Discord bot token is not configured"})
      (-> (collect-discord-scan-channels! token channel-ids max-channels)
          (.then (fn [channels]
                   (-> (promise-reduce channels
                                       {:messages-scanned 0 :attachments []}
                                       (fn [state channel]
                                         (-> (scan-channel-images! token channel {:cutoff-ms cutoff-ms
                                                                                  :pages-per-channel pages-per-channel
                                                                                  :limit-per-page limit-per-page})
                                             (.then (fn [result]
                                                      {:messages-scanned (+ (:messages-scanned state) (:messages-scanned result))
                                                       :attachments (into (:attachments state) (:attachments result))})))))
                       (.then (fn [{:keys [messages-scanned attachments]}]
                                (-> (promise-reduce attachments []
                                                    (fn [results attachment]
                                                      (-> (import-image-attachment! runtime config import-root attachment)
                                                          (.then (fn [result]
                                                                   (conj results result)))
                                                          (.catch (fn [error]
                                                                    (conj results (discord-audio-import-result "failed"
                                                                                                               ""
                                                                                                               ""
                                                                                                               attachment
                                                                                                               (.-message error))))))))
                                    (.then (fn [results]
                                             (let [summary {:ok true
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
                                                            :results (vec results)}]
                                               (-> (write-scan-manifest! runtime config import-root summary)
                                                   (.then (fn [manifest-path]
                                                            (json-response! reply 200 (assoc summary :manifest_path manifest-path))))))))))))))
          (.catch (fn [err]
                    (json-response! reply 500 {:detail (str "Discord image scan failed: " err)})))))))

(defn register-studio-routes! [app runtime config deps]
  (studio-audio-library! app runtime config deps)
  (studio-state-get! app runtime config deps)
  (studio-state-put! app runtime config deps)
  (studio-playlist-get! app runtime config deps)
  (studio-playlist-put! app runtime config deps)
  (studio-stream! app runtime config deps)
  (studio-save-m3u! app runtime config deps)
  (studio-save-m3u-download! app runtime config deps)
  (studio-load-m3u! app runtime config deps)
  (studio-list-playlists! app runtime config deps)
  (studio-labels-get! app runtime config deps)
  (studio-labels-add! app runtime config deps)
  (studio-labels-remove! app runtime config deps)
  (studio-labels-by-label! app runtime config deps)
  (studio-sync-symlinks! app runtime config deps)
  (studio-audio-asset-get! app runtime config deps)
  (studio-audio-asset-save! app runtime config deps)
  (studio-discord-audio-scan! app runtime config deps)
  (studio-discord-image-scan! app runtime config deps))

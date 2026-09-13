(ns knoxx.backend.extern.discord-tool-execution
  "Decode Discord SDK arguments and emit tool results and progress."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.discord.messages :as messages]
            [knoxx.backend.domain.text :as text]
            [knoxx.backend.domain.tools :as tools]
            [knoxx.backend.extern.discord :as xdiscord]
            [knoxx.backend.extern.discord-message-selection :as selection]
            [knoxx.backend.extern.discord-tool-operations :as operations]))

(defn- strip-path-delims [s]
  (xdiscord/trim-path-delims s))

(defn- tool-params
  [params]
  (xdiscord/normalize-tool-params params))

(defn- pget
  ([params k]
   (get params k))
  ([params k fallback-k]
   (or (get params k) (get params fallback-k))))

(defn ^:async discord-send-execute
  "SDK execution for discord-send-execute." [runtime config _tool-call-id params a b c]
  (let [params (tool-params params)
        on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        channel-id (or (pget params :channel_id :channelId) "")
        text (or (pget params :text :content) "")
        reply-to (pget params :reply_to :replyTo)
        attachment-urls (->> (or (pget params :attachment_urls :attachmentUrls) [])
                             (map strip-path-delims)
                             (remove str/blank?)
                             vec)]
    (tools/maybe-tool-update! on-update (str "Sending Discord message to " channel-id "…"))
    (let [result (await (operations/discord-send-message! runtime config channel-id text reply-to attachment-urls))]
      (text/tool-text-result (str "Sent Discord message " (:messageId result) " to channel " channel-id)
                        result))))
(defn ^:async channel-messages-execute
  "SDK execution for channel-messages-execute." [runtime config _tool-call-id params a b c]
  (let [params (tool-params params)
        on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        channel-id (or (pget params :channel_id :channelId) "")]
    (tools/maybe-tool-update! on-update (str "Fetching Discord messages from channel " channel-id "…"))
    (let [result (await (operations/discord-fetch-channel-messages! runtime config channel-id {:limit (pget params :limit)
                                                                                   :before (pget params :before)
                                                                                   :after (pget params :after)
                                                                                   :around (pget params :around)}))
          messages (await (selection/attach-openplanner-labels! config (:messages result)))
          filtered (assoc result :messages messages :count (count messages))]
      (text/tool-text-result (messages/discord-messages-text (str "Fetched " (:count filtered) " non-bad messages from channel " channel-id ".") messages)
                        filtered))))

(defn ^:async channel-scroll-execute
  "SDK execution for channel-scroll-execute." [runtime config _tool-call-id params a b c]
  (let [params (tool-params params)
        on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        channel-id (or (pget params :channel_id :channelId) "")
        oldest-seen-id (or (pget params :oldest_seen_id :oldestSeenId) "")]
    (tools/maybe-tool-update! on-update (str "Scrolling older Discord messages in channel " channel-id "…"))
    (let [result (await (operations/discord-scroll-channel-messages! runtime config channel-id oldest-seen-id (pget params :limit)))
          messages (await (selection/attach-openplanner-labels! config (:messages result)))
          filtered (assoc result :messages messages :count (count messages))]
      (text/tool-text-result (messages/discord-messages-text (str "Fetched older non-bad messages before " oldest-seen-id ".") messages)
                        filtered))))

(defn ^:async dm-messages-execute
  "SDK execution for dm-messages-execute." [runtime config _tool-call-id params a b c]
  (let [params (tool-params params)
        on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        user-id (or (pget params :user_id :userId) "")]
    (tools/maybe-tool-update! on-update (str "Fetching Discord DM messages for user " user-id "…"))
    (let [result (await (operations/discord-fetch-dm-messages! runtime config user-id {:limit (pget params :limit)
                                                                            :before (pget params :before)}))
          messages (await (selection/attach-openplanner-labels! config (:messages result)))
          filtered (assoc result :messages messages :count (count messages))]
      (text/tool-text-result (messages/discord-messages-text (str "Fetched " (:count filtered) " non-bad DM messages for user " user-id ".") messages)
                        filtered))))

(defn ^:async search-execute
  "SDK execution for search-execute." [runtime config _tool-call-id params a b c]
  (let [params (tool-params params)
        on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        scope (or (pget params :scope) "channel")
        channel-id (pget params :channel_id :channelId)
        user-id (pget params :user_id :userId)
        query (or (pget params :query) "")]
    (tools/maybe-tool-update! on-update (str "Searching Discord messages in scope " scope "…"))
    (let [result (await (operations/discord-search-messages! runtime config scope {:channel-id channel-id
                                                                        :user-id user-id
                                                                        :query query
                                                                        :limit (pget params :limit)
                                                                        :before (pget params :before)
                                                                        :after (pget params :after)
                                                                        :since-hours (pget params :since_hours :sinceHours)}))]
      (text/tool-text-result (messages/discord-messages-text (str "Found " (:count result) " matching Discord messages.") (:messages result))
                        result))))

(defn ^:async list-servers-execute
  "SDK execution for list-servers-execute." [runtime _config _tool-call-id _params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))]
    (tools/maybe-tool-update! on-update "Listing Discord servers…")
    (let [result (await (operations/discord-list-guilds! runtime))
          lines (->> (:servers result)
                     (map (fn [server] (str (:name server) " (" (:id server) ")")))
                     (str/join "\n"))]
      (text/tool-text-result (str "Discord servers:\n" lines) result))))

(defn ^:async list-channels-execute
  "SDK execution for list-channels-execute." [runtime _config _tool-call-id params a b c]
  (let [params (tool-params params)
        on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        guild-id (pget params :guild_id :guildId)]
    (tools/maybe-tool-update! on-update "Listing Discord channels…")
    (let [result (await (operations/discord-list-channels! runtime guild-id))
          lines (->> (:channels result)
                     (map (fn [channel] (str "#" (:name channel) " (" (:id channel) ") guild=" (:guildId channel))))
                     (str/join "\n"))]
      (text/tool-text-result (str "Discord channels:\n" lines) result))))

(defn channels-execute
  "SDK execution for channels-execute." [runtime config tool-call-id params a b c]
  (let [params (tool-params params)]
    (list-channels-execute runtime config tool-call-id
                           {:guild_id (pget params :guildId :guild_id)}
                           a b c)))

(defn ^:async react-execute
  "SDK execution for react-execute." [runtime _config _tool-call-id params a b c]
  (let [params (tool-params params)
        on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        channel-id (or (pget params :channel_id :channelId) "")
        message-id (or (pget params :message_id :messageId) "")
        emoji (or (pget params :emoji) "")]
    (tools/maybe-tool-update! on-update (str "Reacting to message " message-id " with " emoji "…"))
    (let [result (await (operations/discord-react! runtime channel-id message-id emoji))]
      (text/tool-text-result (str "Reacted with " emoji " to message " message-id) result))))

(defn ^:async thread-create-execute
  "SDK execution for thread-create-execute." [runtime _config _tool-call-id params a b c]
  (let [params (tool-params params)
        on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        channel-id (or (pget params :channel_id :channelId) "")
        message-id (pget params :message_id :messageId)
        thread-name (or (pget params :name) "")
        auto-archive (pget params :auto_archive_duration :autoArchiveDuration)]
    (tools/maybe-tool-update! on-update (str "Creating thread '" thread-name "' in channel " channel-id "…"))
    (let [result (await (operations/discord-thread-create! runtime channel-id message-id thread-name auto-archive))]
      (text/tool-text-result (str "Created thread " (:threadId result) " named '" thread-name "'")
                        result))))

(defn publish-execute
  "SDK execution for publish-execute." [runtime config tool-call-id params a b c]
  (let [params (tool-params params)]
    (discord-send-execute runtime config tool-call-id
                          {:channel_id (or (pget params :channel_id :channelId) "")
                           :text (or (pget params :content :text) "")
                           :attachment_urls (or (pget params :attachment_urls :attachmentUrls) [])}
                          a b c)))

(defn read-execute
  "SDK execution for read-execute." [runtime config tool-call-id params a b c]
  (let [params (tool-params params)]
    (channel-messages-execute runtime config tool-call-id
                              {:channel_id (or (pget params :channel_id :channelId) "")
                               :limit (pget params :limit)}
                              a b c)))


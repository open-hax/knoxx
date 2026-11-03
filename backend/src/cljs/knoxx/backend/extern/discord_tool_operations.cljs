(ns knoxx.backend.extern.discord-tool-operations
  "Discord remote operations and native message chunk submission."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.discord.messages :as messages]
            [knoxx.backend.domain.discord.rest-client :as discord-rest]
            [knoxx.backend.extern.discord :as xdiscord]
            [knoxx.backend.extern.discord-message-selection :as selection]
            [knoxx.backend.extern.discord-upload :as upload]
            [knoxx.backend.infra.actor.credentials :as actor-credentials]))

(defn ^:async discord-token!
  "Discord boundary operation: discord-token!."
  [runtime]
  (let [credential (await (actor-credentials/get-credential! runtime "discord_bot"))
        token (actor-credentials/secret-value credential :botToken :bot-token :token)]
    (when (str/blank? (str token))
      (throw (js/Error. "Discord bot actor credential must include botToken.")))
    token))

(defn ^:async discord-client!
  "Discord boundary operation: discord-client!."
  [runtime]
  (discord-rest/client (await (discord-token! runtime))))

(defn ^:async discord-fetch-channel-messages!
  "Discord boundary operation: discord-fetch-channel-messages!."
  [runtime _config channel-id {:keys [limit before after around]}]
  (when (str/blank? channel-id)
    (throw (js/Error. "channel_id is required")))
  (let [client (await (discord-client! runtime))
        payload (await (discord-rest/channel-messages! client channel-id {:limit limit
                                                                          :before before
                                                                          :after after
                                                                          :around around}))
        messages (->> (or payload [])
                      (map messages/discord-message->map)
                      vec)]
    {:messages messages
     :count (count messages)
     :channelId channel-id}))

(defn ^:async discord-scroll-channel-messages!
  "Discord boundary operation: discord-scroll-channel-messages!."
  [runtime config channel-id oldest-seen-id limit]
  (assoc (await (discord-fetch-channel-messages! runtime config channel-id {:limit limit :before oldest-seen-id}))
         :oldestSeenId oldest-seen-id))

(defn ^:async discord-open-dm-channel!
  "Discord boundary operation: discord-open-dm-channel!."
  [runtime user-id]
  (when (str/blank? user-id)
    (throw (js/Error. "user_id is required")))
  (let [client (await (discord-client! runtime))]
    (await (discord-rest/open-dm-channel! client user-id))))

(defn ^:async discord-fetch-dm-messages!
  "Discord boundary operation: discord-fetch-dm-messages!."
  [runtime config user-id {:keys [limit before]}]
  (let [channel (await (discord-open-dm-channel! runtime user-id))
        channel-id (or (:id channel) "")
        result (await (discord-fetch-channel-messages! runtime config channel-id {:limit limit :before before}))]
    (assoc result :dmChannelId channel-id :userId user-id)))

(defn ^:async discord-search-messages!
  "Discord boundary operation: discord-search-messages!."
  [runtime config scope {:keys [channel-id user-id query limit before after since-hours]}]
  (let [scope (str/lower-case (str (or scope "channel")))
        timeframe-hours (selection/parse-hours since-hours 168)
        result (if (= scope "dm")
                 (await (discord-fetch-dm-messages! runtime config user-id {:limit 100 :before before}))
                 (await (discord-fetch-channel-messages! runtime config channel-id {:limit 100 :before before :after after})))
        labelled (await (selection/attach-openplanner-labels! config (:messages result)))]
    (selection/discord-search-result scope timeframe-hours user-id query limit result labelled)))

(defn- discord-message-chunks
  [normalized]
  (let [chunk-size 2000
        base-text (if (str/blank? normalized) "[attachment]" normalized)]
    (loop [remaining base-text
           acc []]
      (if (<= (count remaining) chunk-size)
        (conj acc remaining)
        (let [slice (.lastIndexOf remaining "\n\n" chunk-size)
              split-index (if (> slice (int (* chunk-size 0.5))) slice chunk-size)]
          (recur (str/trim (subs remaining split-index))
                 (conj acc (str/trim (subs remaining 0 split-index)))))))))

(defn- post-discord-message-chunk!
  [client channel-id reply-to file-list text-chunk state]
  (discord-rest/create-channel-message-form!
   client
   channel-id
   (xdiscord/message-form-data {:payload (messages/discord-message-payload text-chunk reply-to state)
                                :files file-list})))

(defn ^:async post-discord-message-chunks!
  "Discord boundary operation: post-discord-message-chunks!."
  [client channel-id reply-to file-list chunks]
  (loop [[text-chunk & remaining] (seq chunks)
         state nil]
    (if text-chunk
      (recur remaining
             (await (post-discord-message-chunk! client channel-id reply-to file-list text-chunk state)))
      state)))

(defn ^:async discord-send-message!
  "Discord boundary operation: discord-send-message!."
  [runtime config channel-id text reply-to attachment-urls]
  (let [raw-text (str/trim (str (or text "")))
        {:keys [text attachmentUrls]} (upload/extract-inline-svg-code-blocks raw-text)
        extracted-urls (when (and (not (str/blank? text)) (str/includes? text "data:image/"))
                         (vec (re-seq #"data:image/[^;]+;base64,[A-Za-z0-9+/=]+" text)))
        text-for-discord (if (seq extracted-urls)
                           (reduce (fn [txt url] (str/replace txt url "[image]")) text extracted-urls)
                           text)
        normalized (str/trim text-for-discord)
        all-urls (vec (concat (or attachment-urls []) (or attachmentUrls []) extracted-urls))]
    (when (str/blank? channel-id)
      (throw (js/Error. "channel_id is required")))
    (when (and (str/blank? normalized) (empty? all-urls))
      (throw (js/Error. "text or attachment_urls is required")))
    (let [file-list (await (xdiscord/promise-all-vector
                            (map-indexed (partial upload/resolve-discord-upload-attachment! runtime config)
                                         all-urls)))
          client (await (discord-client! runtime))
          chunks (discord-message-chunks normalized)
          result (await (post-discord-message-chunks! client channel-id reply-to file-list chunks))]
      {:channelId channel-id
       :messageId (or (:id result) "")
       :sent true
       :timestamp (or (:timestamp result) "")
       :chunkCount (count chunks)
       :attachmentCount (count file-list)})))


(defn ^:async discord-react!
  "Add an emoji reaction to a Discord message."
  [runtime channel-id message-id emoji]
  (when (str/blank? channel-id)
    (throw (js/Error. "channel_id is required")))
  (when (str/blank? message-id)
    (throw (js/Error. "message_id is required")))
  (when (str/blank? emoji)
    (throw (js/Error. "emoji is required")))
  (let [client (await (discord-client! runtime))]
    (await (discord-rest/add-reaction! client channel-id message-id emoji))
    {:channelId channel-id
     :messageId message-id
     :emoji emoji
     :reacted true}))

(defn ^:async discord-thread-create!
  "Create a thread in a channel or from a message."
  [runtime channel-id message-id thread-name auto-archive-duration]
  (when (str/blank? channel-id)
    (throw (js/Error. "channel_id is required")))
  (when (str/blank? thread-name)
    (throw (js/Error. "name is required")))
  (let [body {:name thread-name
              :auto_archive_duration (or auto-archive-duration 1440)
              :type 11}
        client (await (discord-client! runtime))
        result (await (discord-rest/create-thread! client channel-id message-id body))]
    {:threadId (or (:id result) "")
     :channelId channel-id
     :messageId (or message-id "")
     :name thread-name
     :created true}))

(defn ^:async discord-list-guilds!
  "Discord boundary operation: discord-list-guilds!."
  [runtime]
  (let [client (await (discord-client! runtime))
        payload (await (discord-rest/current-user-guilds! client))
        servers (->> (or payload [])
                     (mapv (fn [guild]
                             {:id (or (:id guild) "")
                              :name (or (:name guild) "")
                              :memberCount (:approximate_member_count guild)})))]
    {:servers servers
     :count (count servers)}))

(defn ^:async discord-list-guild-channels!
  "Discord boundary operation: discord-list-guild-channels!."
  [runtime guild-id]
  (let [client (await (discord-client! runtime))
        payload (await (discord-rest/guild-channels! client guild-id))
        channels (->> (or payload [])
                      (filter messages/text-channel-type?)
                      (mapv (fn [channel]
                              {:id (or (:id channel) "")
                               :name (or (:name channel) "")
                               :guildId guild-id
                               :type (str (or (:type channel) ""))})))]
    {:channels channels
     :count (count channels)}))

(defn ^:async discord-list-channels!
  "Discord boundary operation: discord-list-channels!."
  [runtime guild-id]
  (if (str/blank? (str (or guild-id "")))
    (let [result (await (discord-list-guilds! runtime))
          payloads (await (xdiscord/promise-all-vector
                           (mapv (fn [server]
                                   (discord-list-guild-channels! runtime (:id server)))
                                 (:servers result))))
          channels (->> payloads
                        (mapcat :channels)
                        vec)]
      {:channels channels
       :count (count channels)})
    (await (discord-list-guild-channels! runtime guild-id))))


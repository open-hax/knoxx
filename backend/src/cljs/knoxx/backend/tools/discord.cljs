(ns knoxx.backend.tools.discord
  "Discord API wrappers and tool factories."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.discord-gateway :as dg]
            [knoxx.backend.http :as backend-http :refer [http-error js-array-seq]]
            [knoxx.backend.text :refer [tool-text-result]]
            [knoxx.backend.tools.media :as media]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! type-optional live-config]]))

(defn- discord-gateway-manager
  []
  (dg/gateway-manager))

(defn- discord-gateway-started?
  []
  (let [manager (discord-gateway-manager)]
    (when (some? manager)
      (let [status (.status manager)]
        (boolean (or (aget status "ready")
                     (aget status "started")))))))

(defn- discord-token
  [config]
  (let [token (:discord-bot-token (live-config config))]
    (when (str/blank? token)
      (throw (js/Error. "Discord bot token not configured. Set DISCORD_BOT_TOKEN in the admin panel.")))
    token))

(defn- discord-rest-headers
  [token]
  #js {"Authorization" (str "Bot " token)
       "Content-Type" "application/json"})

(defn- discord-query-url
  [base params]
  (let [search (js/URLSearchParams.)]
    (doseq [[k v] params]
      (when-not (or (nil? v) (and (string? v) (str/blank? v)))
        (.append search (name k) (str v))))
    (let [query (.toString search)]
      (if (str/blank? query)
        base
        (str base "?" query)))))

(defn- discord-fetch-json!
  [url options]
  (-> (js/fetch url options)
      (.then (fn [resp]
               (if (.-ok resp)
                 (.json resp)
                 (-> (.text resp)
                     (.then (fn [text]
                              (throw (js/Error. (str "Discord API error " (.-status resp) ": " text)))))))))))


(defn- discord-attachments
  [message]
  (->> (if (array? (aget message "attachments")) (array-seq (aget message "attachments")) [])
       (mapv (fn [attachment]
               {:id (or (aget attachment "id") "")
                :filename (or (aget attachment "filename") "")
                :contentType (aget attachment "content_type")
                :size (or (aget attachment "size") 0)
                :url (or (aget attachment "url") "")}))))

(defn- discord-embeds
  [message]
  (->> (if (array? (aget message "embeds")) (array-seq (aget message "embeds")) [])
       (mapv (fn [embed]
               {:title (aget embed "title")
                :description (aget embed "description")
                :url (aget embed "url")}))))

(defn- discord-message->map
  [message]
  {:id (or (aget message "id") "")
   :channelId (or (aget message "channel_id") "")
   :content (or (aget message "content") "")
   :authorId (or (aget message "author" "id") "")
   :authorUsername (or (aget message "author" "username") "unknown")
   :authorIsBot (boolean (aget message "author" "bot"))
   :timestamp (or (aget message "timestamp") "")
   :attachments (discord-attachments message)
   :embeds (discord-embeds message)})

(defn- discord-message-line
  [message]
  (let [content (str/trim (str (or (:content message) "")))
        attachments (:attachments message)
        attachment-text (when (seq attachments)
                          (str " attachments="
                               (str/join ", "
                                         (map (fn [attachment]
                                                (str (:filename attachment)
                                                     (when-let [url (:url attachment)]
                                                       (str " <" url ">"))))
                                              attachments))))
        embeds (:embeds message)
        embed-text (when (seq embeds)
                     (str " embeds="
                          (str/join ", "
                                    (map (fn [embed]
                                           (str (or (:title embed) "embed")
                                                (when-let [url (:url embed)]
                                                  (str " <" url ">"))))
                                         embeds))))]
    (str "<" (:authorUsername message) "> "
         (if (str/blank? content) "[no text]" content)
         (or attachment-text "")
         (or embed-text ""))))

(defn- discord-messages-text
  [heading messages]
  (str heading
       (if (seq messages)
         (str "\n\n" (str/join "\n\n" (map discord-message-line messages)))
         "\n\nNo messages found.")))

(defn- discord-fetch-channel-messages!
  [config channel-id {:keys [limit before after around]}]
  (let [manager (discord-gateway-manager)]
    (when (str/blank? channel-id)
      (throw (js/Error. "channel_id is required")))
    (if (discord-gateway-started?)
      (-> (.fetchChannelMessages manager channel-id (clj->js {:limit (max 1 (min 100 (or limit 50)))
                                                              :before before
                                                              :after after
                                                              :around around}))
          (.then (fn [messages]
                   {:messages (js->clj messages :keywordize-keys true)
                    :count (count (js->clj messages))
                    :channelId channel-id})))
      (let [token (discord-token config)]
        (-> (discord-fetch-json!
             (discord-query-url (str "https://discord.com/api/v10/channels/" channel-id "/messages")
                                {:limit (max 1 (min 100 (or limit 50)))
                                 :before before
                                 :after after
                                 :around around})
             #js {:method "GET"
                  :headers (discord-rest-headers token)})
            (.then (fn [payload]
                     (let [messages (->> (if (array? payload) (array-seq payload) [])
                                         (map discord-message->map)
                                         vec)]
                       {:messages messages
                        :count (count messages)
                        :channelId channel-id}))))))))

(defn- discord-scroll-channel-messages!
  [config channel-id oldest-seen-id limit]
  (-> (discord-fetch-channel-messages! config channel-id {:limit limit :before oldest-seen-id})
      (.then (fn [result]
               (assoc result :oldestSeenId oldest-seen-id)))))

(defn- discord-open-dm-channel!
  [config user-id]
  (let [token (discord-token config)]
    (when (str/blank? user-id)
      (throw (js/Error. "user_id is required")))
    (discord-fetch-json!
     "https://discord.com/api/v10/users/@me/channels"
     #js {:method "POST"
          :headers (discord-rest-headers token)
          :body (.stringify js/JSON #js {:recipient_id user-id})})))

(defn- discord-fetch-dm-messages!
  [config user-id {:keys [limit before]}]
  (if (discord-gateway-started?)
    (-> (.fetchDmMessages (discord-gateway-manager) user-id (clj->js {:limit (or limit 50)
                                                                      :before before}))
        (.then (fn [result]
                 (let [mapped (js->clj result :keywordize-keys true)]
                   {:messages (:messages mapped)
                    :count (count (:messages mapped))
                    :dmChannelId (:dmChannelId mapped)
                    :userId user-id}))))
    (-> (discord-open-dm-channel! config user-id)
        (.then (fn [channel]
                 (let [channel-id (or (aget channel "id") "")]
                   (-> (discord-fetch-channel-messages! config channel-id {:limit limit :before before})
                       (.then (fn [result]
                                (assoc result :dmChannelId channel-id :userId user-id))))))))))

(defn- discord-search-messages!
  [config scope {:keys [channel-id user-id query limit before after]}]
  (let [scope (str/lower-case (str (or scope "channel")))]
    (-> (if (= scope "dm")
          (discord-fetch-dm-messages! config user-id {:limit 100 :before before})
          (discord-fetch-channel-messages! config channel-id {:limit 100 :before before :after after}))
        (.then (fn [result]
                 (let [needle (some-> query str str/lower-case)
                       author-id (some-> user-id str not-empty)
                       filtered (->> (:messages result)
                                     (filter (fn [message]
                                               (and (or (str/blank? (str (or needle "")))
                                                        (str/includes? (str/lower-case (str (:content message))) needle))
                                                    (or (nil? author-id)
                                                        (= author-id (:authorId message))))))
                                     (take (or limit 50))
                                     vec)]
                   {:messages filtered
                    :count (count filtered)
                    :scope scope
                    :channelId (:channelId result)
                    :dmChannelId (:dmChannelId result)
                    :source "client_side_filter"}))))))

(defn- infer-upload-filename
  [url idx]
  (let [pathname (try (.-pathname (js/URL. (str url))) (catch :default _ ""))
        candidate (some-> pathname (str/split #"/") last str/trim not-empty)]
    (or candidate (str "attachment-" idx ".bin"))))

(defn- fetch-discord-upload-attachment!
  [url idx]
  (-> (js/fetch (str url))
      (.then (fn [resp]
               (if (.-ok resp)
                 (.then (.arrayBuffer resp)
                        (fn [buf]
                          {:name (infer-upload-filename url idx)
                           :mimeType (or (.get (.-headers resp) "content-type") "application/octet-stream")
                           :buffer (.from js/Buffer buf)}))
                 (-> (.text resp)
                     (.then (fn [text]
                              (throw (js/Error. (str "Attachment fetch failed " (.-status resp) ": " text)))))))))))

(defn- discord-send-message!
  [config channel-id text reply-to attachment-urls]
  (let [manager (discord-gateway-manager)
        normalized (str/trim (str (or text "")))
        attachment-urls (->> (or attachment-urls [])
                             (map (fn [value] (some-> value str str/trim not-empty)))
                             (remove nil?)
                             vec)]
    (when (str/blank? channel-id)
      (throw (js/Error. "channel_id is required")))
    (when (and (str/blank? normalized) (empty? attachment-urls))
      (throw (js/Error. "text or attachment_urls is required")))
    (-> (js/Promise.all
         (clj->js (map-indexed (fn [idx url]
                                 (fetch-discord-upload-attachment! url idx))
                               attachment-urls)))
        (.then (fn [files]
                 (let [file-list (vec (js->clj files :keywordize-keys true))]
                   (if (discord-gateway-started?)
                     (-> (.sendMessage manager channel-id normalized reply-to (clj->js file-list))
                         (.then (fn [result]
                                  (js->clj result :keywordize-keys true))))
                     (let [token (discord-token config)
                           chunk-size 2000
                           base-text (if (str/blank? normalized) "[attachment]" normalized)
                           chunks (loop [remaining base-text acc []]
                                    (if (<= (count remaining) chunk-size)
                                      (conj acc remaining)
                                      (let [slice (.lastIndexOf remaining "\n\n" chunk-size)
                                            split-at (cond
                                                       (> slice (int (* chunk-size 0.5))) slice
                                                       :else chunk-size)]
                                        (recur (str/trim (subs remaining split-at))
                                               (conj acc (str/trim (subs remaining 0 split-at)))))))]
                       (-> (reduce (fn [promise chunk]
                                     (.then promise
                                            (fn [state]
                                              (let [form (js/FormData.)
                                                    payload (cond-> {:content chunk}
                                                              (and reply-to (nil? (:messageId state)))
                                                              (assoc :message_reference {:message_id reply-to}))]
                                                (.append form "payload_json" (.stringify js/JSON (clj->js payload)))
                                                (doseq [[idx file] (map-indexed vector file-list)]
                                                  (.append form (str "files[" idx "]")
                                                           (js/Blob. #js [(:buffer file)] #js {:type (:mimeType file)})
                                                           (:name file)))
                                                (-> (js/fetch (str "https://discord.com/api/v10/channels/" channel-id "/messages")
                                                              #js {:method "POST"
                                                                   :headers #js {"Authorization" (str "Bot " token)}
                                                                   :body form})
                                                    (.then (fn [resp]
                                                             (if (.-ok resp)
                                                               (.json resp)
                                                               (-> (.text resp)
                                                                   (.then (fn [text]
                                                                            (throw (js/Error. (str "Discord API error " (.-status resp) ": " text)))))))))))))
                                   (js/Promise.resolve nil)
                                   chunks)
                           (.then (fn [result]
                                    {:channelId channel-id
                                     :messageId (or (aget result "id") "")
                                     :sent true
                                     :timestamp (or (aget result "timestamp") "")
                                     :chunkCount (count chunks)
                                     :attachmentCount (count file-list)}))))))))))))

(defn- discord-list-guilds!
  [config]
  (if (discord-gateway-started?)
    (-> (.listServers (discord-gateway-manager))
        (.then (fn [servers]
                 (let [mapped (js->clj servers :keywordize-keys true)]
                   {:servers mapped
                    :count (count mapped)}))))
    (let [token (discord-token config)]
      (-> (discord-fetch-json! "https://discord.com/api/v10/users/@me/guilds"
                               #js {:method "GET"
                                    :headers (discord-rest-headers token)})
          (.then (fn [payload]
                   (let [servers (->> (if (array? payload) (array-seq payload) [])
                                      (mapv (fn [guild]
                                              {:id (or (aget guild "id") "")
                                               :name (or (aget guild "name") "")
                                               :memberCount (aget guild "approximate_member_count")})))]
                     {:servers servers
                      :count (count servers)})))))))

(defn- text-channel-type?
  [channel]
  (contains? #{0 5 11 12} (aget channel "type")))

(defn- discord-list-guild-channels!
  [config guild-id]
  (if (discord-gateway-started?)
    (-> (.listChannels (discord-gateway-manager) guild-id)
        (.then (fn [channels]
                 (let [mapped (js->clj channels :keywordize-keys true)]
                   {:channels mapped
                    :count (count mapped)}))))
    (let [token (discord-token config)]
      (-> (discord-fetch-json! (str "https://discord.com/api/v10/guilds/" guild-id "/channels")
                               #js {:method "GET"
                                    :headers (discord-rest-headers token)})
          (.then (fn [payload]
                   (let [channels (->> (if (array? payload) (array-seq payload) [])
                                       (filter text-channel-type?)
                                       (mapv (fn [channel]
                                               {:id (or (aget channel "id") "")
                                                :name (or (aget channel "name") "")
                                                :guildId guild-id
                                                :type (str (or (aget channel "type") ""))})))]
                     {:channels channels
                      :count (count channels)})))))))

(defn- discord-list-channels!
  [config guild-id]
  (if (str/blank? (str (or guild-id "")))
    (-> (discord-list-guilds! config)
        (.then (fn [result]
                 (-> (js/Promise.all
                      (clj->js (mapv (fn [server]
                                       (discord-list-guild-channels! config (:id server)))
                                     (:servers result))))
                     (.then (fn [payloads]
                              (let [channels (->> (js-array-seq payloads)
                                                  (mapcat :channels)
                                                  vec)]
                                {:channels channels
                                 :count (count channels)})))))))
    (discord-list-guild-channels! config guild-id)))


(defn create-discord-custom-tools
  ([runtime config] (create-discord-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [Type (aget runtime "Type")
         allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))
         send-params (.Object Type
                              #js {:channel_id (.String Type #js {:description "Discord channel ID to send the message to. Use discord.list.channels to discover IDs."})
                                   :text (.String Type #js {:description "Message content to send. Long messages will be chunked automatically."})
                                   :reply_to (type-optional Type (.String Type #js {:description "Optional message ID to reply to."}))
                                   :attachment_urls (type-optional Type (.Array Type (.String Type) #js {:description "Optional attachment URLs to fetch and upload into the Discord message."}))})
         channel-messages-params (.Object Type
                                          #js {:channel_id (.String Type #js {:description "Discord channel ID to fetch messages from."})
                                               :limit (type-optional Type (.Number Type #js {:description "Maximum number of messages to fetch (default 50, max 100)." :minimum 1 :maximum 100}))
                                               :before (type-optional Type (.String Type #js {:description "Fetch messages before this message ID."}))
                                               :after (type-optional Type (.String Type #js {:description "Fetch messages after this message ID."}))
                                               :around (type-optional Type (.String Type #js {:description "Fetch messages around this message ID."}))})
         channel-scroll-params (.Object Type
                                        #js {:channel_id (.String Type #js {:description "Discord channel ID to fetch older messages from."})
                                             :oldest_seen_id (.String Type #js {:description "Oldest message ID already seen; fetch messages before this."})
                                             :limit (type-optional Type (.Number Type #js {:description "Maximum number of older messages to fetch." :minimum 1 :maximum 100}))})
         dm-messages-params (.Object Type
                                     #js {:user_id (.String Type #js {:description "Discord user ID whose DM channel should be read."})
                                          :limit (type-optional Type (.Number Type #js {:description "Maximum number of DM messages to fetch." :minimum 1 :maximum 100}))
                                          :before (type-optional Type (.String Type #js {:description "Fetch DM messages before this message ID."}))})
         search-params (.Object Type
                                #js {:scope (.String Type #js {:description "Search scope: channel or dm."})
                                     :channel_id (type-optional Type (.String Type #js {:description "Discord channel ID to search when scope=channel."}))
                                     :user_id (type-optional Type (.String Type #js {:description "Discord user ID to search against when scope=dm or to filter author."}))
                                     :query (type-optional Type (.String Type #js {:description "Optional substring query to filter messages by content."}))
                                     :limit (type-optional Type (.Number Type #js {:description "Maximum number of matching messages to return." :minimum 1 :maximum 100}))
                                     :before (type-optional Type (.String Type #js {:description "Fetch messages before this message ID."}))
                                     :after (type-optional Type (.String Type #js {:description "Fetch messages after this message ID."}))})
         list-channels-params (.Object Type
                                       #js {:guild_id (type-optional Type (.String Type #js {:description "Optional guild/server ID. If omitted, returns channels across all visible guilds."}))})
         empty-params (.Object Type #js {})

         discord-send-execute (fn [_tool-call-id params a b c]
                                (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                      channel-id (or (aget params "channel_id") (aget params "channelId") "")
                                      text (or (aget params "text") (aget params "content") "")
                                      reply-to (or (aget params "reply_to") (aget params "replyTo"))
                                      attachment-urls (or (aget params "attachment_urls") (aget params "attachmentUrls") #js [])]
                                  (maybe-tool-update! on-update (str "Sending Discord message to " channel-id "…"))
                                  (-> (discord-send-message! config channel-id text reply-to attachment-urls)
                                      (.then (fn [result]
                                               (tool-text-result (str "Sent Discord message " (:messageId result) " to channel " channel-id)
                                                                 result))))))

         discord-channel-messages-execute (fn [_tool-call-id params a b c]
                                            (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                                  channel-id (or (aget params "channel_id") (aget params "channelId") "")]
                                              (maybe-tool-update! on-update (str "Fetching Discord messages from channel " channel-id "…"))
                                              (-> (discord-fetch-channel-messages! config channel-id {:limit (aget params "limit")
                                                                                                    :before (aget params "before")
                                                                                                    :after (aget params "after")
                                                                                                    :around (aget params "around")})
                                                  (.then (fn [result]
                                                           (tool-text-result (discord-messages-text (str "Fetched " (:count result) " messages from channel " channel-id ".") (:messages result))
                                                                             result))))))

         discord-channel-scroll-execute (fn [_tool-call-id params a b c]
                                          (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                                channel-id (or (aget params "channel_id") (aget params "channelId") "")
                                                oldest-seen-id (or (aget params "oldest_seen_id") (aget params "oldestSeenId") "")]
                                            (maybe-tool-update! on-update (str "Scrolling older Discord messages in channel " channel-id "…"))
                                            (-> (discord-scroll-channel-messages! config channel-id oldest-seen-id (aget params "limit"))
                                                (.then (fn [result]
                                                         (tool-text-result (discord-messages-text (str "Fetched older messages before " oldest-seen-id ".") (:messages result))
                                                                           result))))))

         discord-dm-messages-execute (fn [_tool-call-id params a b c]
                                       (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                             user-id (or (aget params "user_id") (aget params "userId") "")]
                                         (maybe-tool-update! on-update (str "Fetching Discord DM messages for user " user-id "…"))
                                         (-> (discord-fetch-dm-messages! config user-id {:limit (aget params "limit")
                                                                                         :before (aget params "before")})
                                             (.then (fn [result]
                                                      (tool-text-result (discord-messages-text (str "Fetched " (:count result) " DM messages for user " user-id ".") (:messages result))
                                                                        result))))))

         discord-search-execute (fn [_tool-call-id params a b c]
                                  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                        scope (or (aget params "scope") "channel")
                                        channel-id (or (aget params "channel_id") (aget params "channelId"))
                                        user-id (or (aget params "user_id") (aget params "userId"))
                                        query (or (aget params "query") "")]
                                    (maybe-tool-update! on-update (str "Searching Discord messages in scope " scope "…"))
                                    (-> (discord-search-messages! config scope {:channel-id channel-id
                                                                                :user-id user-id
                                                                                :query query
                                                                                :limit (aget params "limit")
                                                                                :before (aget params "before")
                                                                                :after (aget params "after")})
                                        (.then (fn [result]
                                                 (tool-text-result (discord-messages-text (str "Found " (:count result) " matching Discord messages.") (:messages result))
                                                                   result))))))

         discord-list-servers-execute (fn [_tool-call-id _params a b c]
                                        (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))]
                                          (maybe-tool-update! on-update "Listing Discord servers…")
                                          (-> (discord-list-guilds! config)
                                              (.then (fn [result]
                                                       (let [lines (->> (:servers result)
                                                                        (map (fn [server] (str (:name server) " (" (:id server) ")")))
                                                                        (str/join "\n"))]
                                                         (tool-text-result (str "Discord servers:\n" lines) result)))))))

         discord-list-channels-execute (fn [_tool-call-id params a b c]
                                         (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                               guild-id (or (aget params "guild_id") (aget params "guildId"))]
                                           (maybe-tool-update! on-update "Listing Discord channels…")
                                           (-> (discord-list-channels! config guild-id)
                                               (.then (fn [result]
                                                        (let [lines (->> (:channels result)
                                                                         (map (fn [channel] (str "#" (:name channel) " (" (:id channel) ") guild=" (:guildId channel))))
                                                                         (str/join "\n"))]
                                                          (tool-text-result (str "Discord channels:\n" lines) result)))))))


         ]

     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "discord.publish")
                  (doto (js-obj)
                    (aset "name" "discord.publish")
                    (aset "label" "Discord Publish")
                    (aset "description" "Post a message to a Discord channel using the configured Knoxx Discord bot.")
                    (aset "promptSnippet" "Post updates, summaries, or notifications to Discord channels.")
                    (aset "promptGuidelines" (clj->js ["Use discord.publish or discord.send to share results in a Discord channel."
                                                       "Provide channelId/channel_id and content/text."
                                                       "If replying in-thread, prefer discord.send with reply_to."]))
                    (aset "parameters" (.Object Type #js {:channelId (.String Type #js {:description "Discord channel ID to post the message to."})
                                                           :content (.String Type #js {:description "Message content to post to the Discord channel."})
                                                           :attachmentUrls (type-optional Type (.Array Type (.String Type) #js {:description "Optional attachment URLs to upload with the message."}))}))
                    (aset "execute" (fn [tool-call-id params a b c]
                                       (discord-send-execute tool-call-id #js {:channel_id (aget params "channelId")
                                                                               :text (aget params "content")
                                                                               :attachment_urls (aget params "attachmentUrls")}
                                                             a b c)))))
                (when (allowed? "discord.send")
                  (doto (js-obj)
                    (aset "name" "discord.send")
                    (aset "label" "Discord Send")
                    (aset "description" "Send a message to a Discord channel, optionally as a reply to an existing message.")
                    (aset "promptSnippet" "Send a Discord message or reply to a specific message id.")
                    (aset "promptGuidelines" (clj->js ["Use discord.send to post or reply in channels once you know the channel id."
                                                       "Use discord.list.servers and discord.list.channels first if you need discovery."]))
                    (aset "parameters" send-params)
                    (aset "execute" discord-send-execute)))
                (when (allowed? "discord.read")
                  (doto (js-obj)
                    (aset "name" "discord.read")
                    (aset "label" "Discord Read")
                    (aset "description" "Read recent messages from a Discord channel.")
                    (aset "promptSnippet" "Read recent messages from a Discord channel to understand context.")
                    (aset "promptGuidelines" (clj->js ["Use discord.read as a simple alias for discord.channel.messages."
                                                       "For pagination or cursors, use discord.channel.messages or discord.channel.scroll directly."]))
                    (aset "parameters" (.Object Type #js {:channelId (.String Type #js {:description "Discord channel ID to read messages from."})
                                                           :limit (type-optional Type (.Number Type #js {:description "Maximum number of messages to return." :minimum 1 :maximum 100}))}))
                    (aset "execute" (fn [tool-call-id params a b c]
                                       (discord-channel-messages-execute tool-call-id #js {:channel_id (aget params "channelId")
                                                                                           :limit (aget params "limit")}
                                                                         a b c)))))
                (when (allowed? "discord.channel.messages")
                  (doto (js-obj)
                    (aset "name" "discord.channel.messages")
                    (aset "label" "Discord Channel Messages")
                    (aset "description" "Fetch messages from a Discord channel with before/after/around cursors.")
                    (aset "promptSnippet" "Fetch channel messages from Discord with pagination cursors when you need exact transcript context.")
                    (aset "promptGuidelines" (clj->js ["Use this when you know the channel id and need exact message history."
                                                       "Use before/after/around for precise pagination."]))
                    (aset "parameters" channel-messages-params)
                    (aset "execute" discord-channel-messages-execute)))
                (when (allowed? "discord.channel.scroll")
                  (doto (js-obj)
                    (aset "name" "discord.channel.scroll")
                    (aset "label" "Discord Channel Scroll")
                    (aset "description" "Scroll older channel messages by fetching messages before the oldest already-seen message id.")
                    (aset "promptSnippet" "Scroll backwards in a Discord channel once you already know the oldest seen id.")
                    (aset "promptGuidelines" (clj->js ["Use discord.channel.scroll as sugar over discord.channel.messages before=oldest_seen_id."
                                                       "Useful for paging backward through long histories."]))
                    (aset "parameters" channel-scroll-params)
                    (aset "execute" discord-channel-scroll-execute)))
                (when (allowed? "discord.dm.messages")
                  (doto (js-obj)
                    (aset "name" "discord.dm.messages")
                    (aset "label" "Discord DM Messages")
                    (aset "description" "Fetch messages from the DM channel shared with a Discord user.")
                    (aset "promptSnippet" "Read DM history with a Discord user by user id.")
                    (aset "promptGuidelines" (clj->js ["Use this when the relevant conversation is in DMs rather than a guild channel."
                                                       "Provide the target user id."]))
                    (aset "parameters" dm-messages-params)
                    (aset "execute" discord-dm-messages-execute)))
                (when (allowed? "discord.search")
                  (doto (js-obj)
                    (aset "name" "discord.search")
                    (aset "label" "Discord Search")
                    (aset "description" "Search channel or DM messages by content and/or author using client-side filtering.")
                    (aset "promptSnippet" "Search Discord messages by text and scope to find relevant discussion quickly.")
                    (aset "promptGuidelines" (clj->js ["Use scope=channel with channel_id for guild channels or scope=dm with user_id for DMs."
                                                       "This falls back to client-side filtering when native search is unavailable."]))
                    (aset "parameters" search-params)
                    (aset "execute" discord-search-execute)))
                (when (allowed? "discord.guilds")
                  (doto (js-obj)
                    (aset "name" "discord.guilds")
                    (aset "label" "Discord Guilds")
                    (aset "description" "List Discord guilds/servers the bot is in.")
                    (aset "promptSnippet" "List Discord guilds to discover available servers.")
                    (aset "promptGuidelines" (clj->js ["Alias for discord.list.servers."
                                                       "Use before listing channels or posting to a specific server."]))
                    (aset "parameters" empty-params)
                    (aset "execute" discord-list-servers-execute)))
                (when (allowed? "discord.list.servers")
                  (doto (js-obj)
                    (aset "name" "discord.list.servers")
                    (aset "label" "Discord List Servers")
                    (aset "description" "List all Discord servers/guilds the bot can access.")
                    (aset "promptSnippet" "List Discord servers before choosing channels or replying into a guild.")
                    (aset "promptGuidelines" (clj->js ["Use this before discord.list.channels when you need discovery."
                                                       "Do not guess guild ids."]))
                    (aset "parameters" empty-params)
                    (aset "execute" discord-list-servers-execute)))
                (when (allowed? "discord.channels")
                  (doto (js-obj)
                    (aset "name" "discord.channels")
                    (aset "label" "Discord Channels")
                    (aset "description" "List channels in a Discord guild.")
                    (aset "promptSnippet" "List channels in a Discord guild to find the right channel for reading or posting.")
                    (aset "promptGuidelines" (clj->js ["Alias for discord.list.channels."
                                                       "Use guildId/guild_id when you already know the server."]))
                    (aset "parameters" (.Object Type #js {:guildId (.String Type #js {:description "Discord guild ID to list channels for."})}))
                    (aset "execute" (fn [tool-call-id params a b c]
                                       (discord-list-channels-execute tool-call-id #js {:guild_id (aget params "guildId")}
                                                                      a b c)))))
                (when (allowed? "discord.list.channels")
                  (doto (js-obj)
                    (aset "name" "discord.list.channels")
                    (aset "label" "Discord List Channels")
                    (aset "description" "List channels in one Discord guild or across all visible guilds.")
                    (aset "promptSnippet" "List Discord channels to discover readable/postable targets.")
                    (aset "promptGuidelines" (clj->js ["If guild_id is omitted, returns channels across all visible guilds."
                                                       "Use returned channel ids with discord.channel.messages or discord.send."]))
                    (aset "parameters" list-channels-params)
                    (aset "execute" discord-list-channels-execute)))
                    ]))))))

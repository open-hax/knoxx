(ns knoxx.backend.domain.discord.tools
  "Discord API wrappers and tool factories."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.domain.discord.gateway :as dg]
            [knoxx.backend.domain.discord.rest-client :as discord-rest]
            [knoxx.backend.extern.discord :as xdiscord]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.domain.label.quality :as quality-labels]
            [knoxx.backend.infra.svg-render :as svg-render]
            [knoxx.backend.domain.text :refer [sanitize-svg-content tool-text-result]]

            [knoxx.backend.domain.actor.credentials :as actor-credentials]
            [knoxx.backend.domain.media :as media]
            [knoxx.backend.domain.tools :refer [maybe-tool-update! create-tool-obj]]))

(defn- discord-gateway-manager
  []
  (dg/gateway-manager))

(defn- discord-gateway-started?
  []
  (xdiscord/gateway-started? (discord-gateway-manager)))

(defn- discord-token!
  [runtime]
  (-> (actor-credentials/get-credential! runtime "discord_bot")
      (.then (fn [credential]
               (let [token (actor-credentials/secret-value credential :botToken :bot-token :token)]
                 (when (str/blank? (str token))
                   (throw (js/Error. "Discord bot actor credential must include botToken.")))
                 token)))))

(defn- discord-client!
  [runtime]
  (-> (discord-token! runtime)
      (.then (fn [token]
               (discord-rest/client token)))))

(defn- discord-attachments
  [message]
  (->> (or (:attachments message) [])
       (mapv (fn [attachment]
               {:id (or (:id attachment) "")
                :filename (or (:filename attachment) "")
                :contentType (or (:content_type attachment) (:contentType attachment))
                :size (or (:size attachment) 0)
                :url (or (:url attachment) "")}))))

(defn- discord-embeds
  [message]
  (->> (or (:embeds message) [])
       (mapv (fn [embed]
               {:title (:title embed)
                :description (:description embed)
                :url (:url embed)}))))

(defn- discord-message->map
  [message]
  (let [author (or (:author message) {})]
    {:id (or (:id message) "")
     :channelId (or (:channel_id message) (:channelId message) "")
     :content (or (:content message) "")
     :authorId (or (:id author) "")
     :authorUsername (or (:username author) "unknown")
     :authorIsBot (boolean (:bot author))
     :timestamp (or (:timestamp message) "")
   :attachments (discord-attachments message)
     :embeds (discord-embeds message)}))

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
    (str "<" (:authorUsername message) " (id:" (:authorId message) ")> "
         (if (str/blank? content) "[no text]" content)
         (or attachment-text "")
         (or embed-text ""))))

(defn- discord-messages-text
  [heading messages]
  (str heading
       (if (seq messages)
         (str "\n\n" (str/join "\n\n" (map discord-message-line messages)))
         "\n\nNo messages found.")))

(defn- discord-record-id
  [message]
  (str "discord:message:" (:channelId message) ":" (:id message)))

(defn- discord-message-quality
  [message]
  (quality-labels/quality-label message))

(defn- drop-bad-discord-messages
  [messages]
  (quality-labels/drop-bad messages))

(defn- parse-hours
  [value default-hours]
  (let [n (js/Number value)]
    (if (and (not (js/isNaN n)) (pos? n)) n default-hours)))

(defn- timestamp-ms
  [value]
  (let [ms (.parse js/Date (str (or value "")))]
    (when-not (js/isNaN ms) ms)))

(defn- within-hours?
  [hours message]
  (if-let [ms (timestamp-ms (:timestamp message))]
    (>= ms (- (.now js/Date) (* hours 60 60 1000)))
    true))

(defn- chronological-discord-messages
  [messages]
  (vec (sort-by (fn [message] (or (timestamp-ms (:timestamp message)) 0)) messages)))

(defn- good-first-then-not-bad
  [messages]
  (let [non-bad (drop-bad-discord-messages messages)
        good (chronological-discord-messages (filter #(= "good" (discord-message-quality %)) non-bad))
        not-bad (chronological-discord-messages (remove #(= "good" (discord-message-quality %)) non-bad))]
    (vec (concat good not-bad))))

(defn- label-for-record-id
  [labels record-id]
  (or (get labels record-id)
      (get labels (keyword record-id))
      {}))

(defn- attach-openplanner-labels!
  [config messages]
  (let [client (openplanner-client/client config)]
    (if (or (empty? messages) (not (openplanner-client/enabled? client)))
      (js/Promise.resolve (good-first-then-not-bad messages))
      (let [ids (mapv discord-record-id messages)]
        (-> (openplanner-client/record-labels! client ids)
            (.then (fn [response]
                     (let [labels (:labels response)]
                       (->> messages
                            (mapv (fn [message]
                                    (assoc message :openplannerLabels (label-for-record-id labels (discord-record-id message)))))
                            good-first-then-not-bad))))
            (.catch (fn [error]
                      (.warn js/console "[discord-tools] OpenPlanner label lookup failed; failing closed to avoid surfacing crossed/bad messages" error)
                      [])))))))
(defn- discord-fetch-channel-messages!
  [runtime config channel-id {:keys [limit before after around]}]
  (do
    (when (str/blank? channel-id)
      (throw (js/Error. "channel_id is required")))
    (-> (discord-client! runtime)
        (.then (fn [client]
                 (-> (discord-rest/channel-messages! client channel-id {:limit limit
                                                                        :before before
                                                                        :after after
                                                                        :around around})
                     (.then (fn [payload]
                              (let [messages (->> (or payload [])
                                                  (map discord-message->map)
                                                  vec)]
                                {:messages messages
                                 :count (count messages)
                                 :channelId channel-id})))))))))



(defn- discord-scroll-channel-messages!
  [runtime config channel-id oldest-seen-id limit]
  (-> (discord-fetch-channel-messages! runtime config channel-id {:limit limit :before oldest-seen-id})
      (.then (fn [result]
               (assoc result :oldestSeenId oldest-seen-id)))))

(defn- discord-open-dm-channel!
  [runtime user-id]
  (when (str/blank? user-id)
    (throw (js/Error. "user_id is required")))
  (-> (discord-client! runtime)
      (.then (fn [client]
               (discord-rest/open-dm-channel! client user-id)))))

(defn- discord-fetch-dm-messages!
  [runtime config user-id {:keys [limit before]}]
  (-> (discord-open-dm-channel! runtime user-id)
        (.then (fn [channel]
                 (let [channel-id (or (:id channel) "")]
                   (-> (discord-fetch-channel-messages! runtime config channel-id {:limit limit :before before})
                       (.then (fn [result]
                                (assoc result :dmChannelId channel-id :userId user-id)))))))))

(defn- discord-search-messages!
  [runtime config scope {:keys [channel-id user-id query limit before after since-hours]}]
  (let [scope (str/lower-case (str (or scope "channel")))
        timeframe-hours (parse-hours since-hours 168)]
    (-> (if (= scope "dm")
          (discord-fetch-dm-messages! runtime config user-id {:limit 100 :before before})
          (discord-fetch-channel-messages! runtime config channel-id {:limit 100 :before before :after after}))
        (.then (fn [result]
                 (-> (attach-openplanner-labels! config (:messages result))
                     (.then (fn [labelled]
                              (let [needle (some-> query str str/lower-case)
                                    author-id (some-> user-id str not-empty)
                                    filtered (->> labelled
                                                  (filter #(within-hours? timeframe-hours %))
                                                  (filter (fn [message]
                                                            (and (or (str/blank? (str (or needle "")))
                                                                     (str/includes? (str/lower-case (str (:content message))) needle))
                                                                 (or (nil? author-id)
                                                                     (= author-id (:authorId message))))))
                                                  good-first-then-not-bad
                                                  (take (or limit 50))
                                                  vec)]
                                {:messages filtered
                                 :count (count filtered)
                                 :scope scope
                                 :channelId (:channelId result)
                                 :dmChannelId (:dmChannelId result)
                                 :source "client_side_filter_openplanner_labels"
                                 :qualityOrder "good_chronological_then_not_bad_chronological"
                                 :sinceHours timeframe-hours})))))))))

(defn- infer-upload-filename
[url idx]
(let [pathname (try (.-pathname (js/URL. (str url))) (catch :default _ ""))
      candidate (some-> pathname (str/split #"/") last str/trim not-empty)]
  (or candidate (str "attachment-" idx ".bin"))))

(defn- file-url->path
[source]
(try
  (js/decodeURIComponent (.-pathname (js/URL. source)))
  (catch :default _
    (subs source (count "file://")))))

(defn- svg-buffer->png-buffer!
"Render an SVG buffer to PNG using headless Chromium. Returns a promise."
[svg-buffer]
(let [svg-str (sanitize-svg-content (.toString svg-buffer "utf8"))]
  (svg-render/svg->png svg-str {:width 600 :height 300})))

(def svg-code-block-pattern
#"(?is)```(?:svg|image/svg\+xml)\s*\n([\s\S]*?)\n```")

(defn- extract-inline-svg-code-blocks
"Pull fenced ```svg code blocks out of message text so they can be rendered
   and attached as PNGs instead of being sent as raw code."
[text]
(let [raw-text (str (or text ""))
      matches (re-seq svg-code-block-pattern raw-text)
      svg-blocks (->> matches
                      (map second)
                      (map #(str/trim (str %)))
                      (filter #(str/includes? (str/lower-case %) "<svg"))
                      vec)
      cleaned-text (reduce (fn [acc [full-match _svg]]
                             (str/replace acc full-match "[image]"))
                           raw-text
                           matches)
      attachment-urls (mapv (fn [svg]
                              (str "data:image/svg+xml;charset=utf-8,"
                                   (js/encodeURIComponent svg)))
                            svg-blocks)]
  {:text cleaned-text
   :attachmentUrls attachment-urls}))

(defn- maybe-render-svg!
"If the resolved attachment is an SVG, render it to PNG transparently.
   On render failure, returns original attachment."
  [{:keys [name mimeType buffer] :as attachment}]
  (if (or (= mimeType "image/svg+xml")
          (some-> name str/lower-case (str/ends-with? ".svg")))
    (-> (svg-buffer->png-buffer! buffer)
        (.then (fn [png-buf]
                 {:name (if (some-> name str/lower-case (str/ends-with? ".svg"))
                          (str/replace name #"(?i)\.svg$" ".png")
                          (str (or name "attachment") ".png"))
                  :mimeType "image/png"
                  :buffer png-buf}))
        (.catch (fn [error]
                  (.warn js/console "[discord-tools] SVG render failed; uploading original SVG" error)
                  attachment)))
    (js/Promise.resolve attachment)))

(defn- fetch-discord-upload-attachment!
  "Fetch an attachment from a URL, data URL, or local file path.
   Returns a promise resolving to {:name :mimeType :buffer}.
   SVG files are automatically rendered to PNG before upload."
  [runtime config url idx]
  (let [source (str (or url ""))]
    (cond
      (str/blank? source)
      (js/Promise.reject (js/Error. "Empty attachment source"))

      (media/source-data-url? source)
      (let [data-start (.indexOf source ",")
            metadata (when (>= data-start 0) (subs source 5 data-start))
            payload (when (>= data-start 0) (subs source (inc data-start)))]
        (if (or (nil? metadata) (nil? payload))
          (js/Promise.reject (js/Error. "Invalid data URL attachment"))
          (let [metadata-parts (str/split metadata #";")
                mime-type (media/sanitize-mime-type (first metadata-parts) "application/octet-stream")
                base64? (boolean (some #{"base64"} (rest metadata-parts)))
                buffer (if base64?
                         (.from js/Buffer payload "base64")
                         (.from js/Buffer (js/decodeURIComponent payload) "utf8"))]
            (media/ensure-source-size! (.-length buffer) media/workspace-media-max-bytes "Discord attachment")
            (js/Promise.resolve
             {:name (str "attachment-" idx (media/mime-type->extension mime-type))
              :mimeType mime-type
              :buffer buffer}))))

      (media/source-http-url? source)
      (-> (discord-rest/fetch-attachment! (discord-rest/client nil) source)
          (.then (fn [{:keys [ok status headers body]}]
                   (if ok
                     (let [buffer (.from js/Buffer body)
                           mime-type (media/sanitize-mime-type (get headers "content-type")
                                                               (media/workspace-media-mime-type source))]
                       (media/ensure-source-size! (.-length buffer) media/workspace-media-max-bytes "Discord attachment")
                       {:name (infer-upload-filename source idx)
                        :mimeType mime-type
                        :buffer buffer})
                     (throw (js/Error. (str "Attachment fetch failed " status)))))))

      :else
      ;; Local file path — resolve through shared workspace media rules so
      ;; @-prefixed, workspace-relative, and allowed absolute paths work, while
      ;; paths outside allowed media roots are rejected before Discord sees them.
      (let [raw-source (if (media/source-file-url? source)
                         (file-url->path source)
                         source)]
        (-> (media/load-media-source! runtime config raw-source media/workspace-media-max-bytes)
            (.then (fn [loaded]
                     {:name (or (:filename loaded)
                                (str "attachment-" idx (media/mime-type->extension (:mime-type loaded))))
                      :mimeType (media/sanitize-mime-type (:mime-type loaded) "application/octet-stream")
                      :buffer (:buffer loaded)})))))))

(defn- discord-message-chunks
  [normalized]
  (let [chunk-size 2000
        base-text (if (str/blank? normalized) "[attachment]" normalized)]
    (loop [remaining base-text
           acc []]
      (if (<= (count remaining) chunk-size)
        (conj acc remaining)
        (let [slice (.lastIndexOf remaining "\n\n" chunk-size)
              split-at (if (> slice (int (* chunk-size 0.5))) slice chunk-size)]
          (recur (str/trim (subs remaining split-at))
                 (conj acc (str/trim (subs remaining 0 split-at)))))))))

(defn- discord-message-payload
  [chunk reply-to state]
  (cond-> {:content chunk}
    (and reply-to (nil? (:messageId state)))
    (assoc :message_reference {:message_id reply-to})))

(defn- post-discord-message-chunk!
  [client channel-id reply-to file-list chunk state]
  (discord-rest/create-channel-message-form!
   client
   channel-id
   (xdiscord/message-form-data {:payload (discord-message-payload chunk reply-to state)
                                :files file-list})))

(defn- post-discord-message-chunks!
  [client channel-id reply-to file-list chunks]
  (reduce (fn [promise chunk]
            (.then promise
                   (fn [state]
                     (post-discord-message-chunk! client channel-id reply-to file-list chunk state))))
          (js/Promise.resolve nil)
          chunks))

(defn- discord-send-message!
  [runtime config channel-id text reply-to attachment-urls]
  (let [raw-text (str/trim (str (or text "")))
        {:keys [text attachmentUrls]} (extract-inline-svg-code-blocks raw-text)
        extracted-urls (when (and (not (str/blank? text)) (str/includes? text "data:image/"))
                         (vec (re-seq #"data:image/[^;]+;base64,[A-Za-z0-9+/=]+" text)))
        text-for-discord (if (seq extracted-urls)
                           (reduce (fn [txt url] (str/replace txt url "[image]"))
                                   text
                                   extracted-urls)
                           text)
        normalized (str/trim text-for-discord)
        all-urls (vec (concat (or attachment-urls [])
                              (or attachmentUrls [])
                              extracted-urls))]
    (when (str/blank? channel-id)
      (throw (js/Error. "channel_id is required")))
    (when (and (str/blank? normalized) (empty? all-urls))
      (throw (js/Error. "text or attachment_urls is required")))
    (-> (xdiscord/promise-all-vector
         (map-indexed (fn [idx url]
                        (-> (fetch-discord-upload-attachment! runtime config url idx)
                            (.then maybe-render-svg!)))
                      all-urls))
        (.then (fn [file-list]
                 (-> (discord-client! runtime)
                     (.then (fn [client]
                              (let [chunks (discord-message-chunks normalized)]
                                (-> (post-discord-message-chunks! client channel-id reply-to file-list chunks)
                                    (.then (fn [result]
                                             {:channelId channel-id
                                              :messageId (or (:id result) "")
                                              :sent true
                                              :timestamp (or (:timestamp result) "")
                                              :chunkCount (count chunks)
                                              :attachmentCount (count file-list)}))))))))))))


(defn- discord-react!
  "Add an emoji reaction to a Discord message."
  [runtime channel-id message-id emoji]
  (when (str/blank? channel-id)
    (throw (js/Error. "channel_id is required")))
  (when (str/blank? message-id)
    (throw (js/Error. "message_id is required")))
  (when (str/blank? emoji)
    (throw (js/Error. "emoji is required")))
  (-> (discord-client! runtime)
      (.then (fn [client]
               (-> (discord-rest/add-reaction! client channel-id message-id emoji)
                   (.then (fn [_]
                            {:channelId channel-id
                             :messageId message-id
                             :emoji emoji
                             :reacted true})))))))

(defn- discord-thread-create!
  "Create a thread in a channel or from a message."
  [runtime channel-id message-id name auto-archive-duration]
  (when (str/blank? channel-id)
    (throw (js/Error. "channel_id is required")))
  (when (str/blank? name)
    (throw (js/Error. "name is required")))
  (let [body {:name name
              :auto_archive_duration (or auto-archive-duration 1440)
              :type 11}]
    (-> (discord-client! runtime)
        (.then (fn [client]
                 (discord-rest/create-thread! client channel-id message-id body)))
        (.then (fn [result]
                 {:threadId (or (:id result) "")
                  :channelId channel-id
                  :messageId (or message-id "")
                  :name name
                  :created true})))))

(defn- discord-list-guilds!
  [runtime]
  (-> (discord-client! runtime)
      (.then (fn [client]
               (discord-rest/current-user-guilds! client)))
      (.then (fn [payload]
               (let [servers (->> (or payload [])
                                  (mapv (fn [guild]
                                          {:id (or (:id guild) "")
                                           :name (or (:name guild) "")
                                           :memberCount (:approximate_member_count guild)})))]
                 {:servers servers
                  :count (count servers)})))))

(defn- text-channel-type?
  [channel]
  (contains? #{0 5 11 12} (:type channel)))

(defn- discord-list-guild-channels!
  [runtime guild-id]
  (-> (discord-client! runtime)
      (.then (fn [client]
               (discord-rest/guild-channels! client guild-id)))
      (.then (fn [payload]
               (let [channels (->> (or payload [])
                                   (filter text-channel-type?)
                                   (mapv (fn [channel]
                                           {:id (or (:id channel) "")
                                            :name (or (:name channel) "")
                                            :guildId guild-id
                                            :type (str (or (:type channel) ""))})))]
                 {:channels channels
                  :count (count channels)})))))

(defn- discord-list-channels!
  [runtime guild-id]
  (if (str/blank? (str (or guild-id "")))
    (-> (discord-list-guilds! runtime)
        (.then (fn [result]
                 (-> (xdiscord/promise-all-vector
                      (mapv (fn [server]
                              (discord-list-guild-channels! runtime (:id server)))
                            (:servers result)))
                     (.then (fn [payloads]
                              (let [channels (->> payloads
                                                  (mapcat :channels)
                                                  vec)]
                                {:channels channels
                                 :count (count channels)})))))))
    (discord-list-guild-channels! runtime guild-id)))

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

(defn discord-send-execute [runtime config _tool-call-id params a b c]
  (let [params (tool-params params)
        on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        channel-id (or (pget params :channel_id :channelId) "")
        text (or (pget params :text :content) "")
        reply-to (pget params :reply_to :replyTo)
        attachment-urls (->> (or (pget params :attachment_urls :attachmentUrls) [])
                             (map strip-path-delims)
                             (remove str/blank?)
                             vec)]
    (maybe-tool-update! on-update (str "Sending Discord message to " channel-id "…"))
    (-> (discord-send-message! runtime config channel-id text reply-to attachment-urls)
        (.then (fn [result]
                 (tool-text-result (str "Sent Discord message " (:messageId result) " to channel " channel-id)
                                   result))))))
(def send-params
  [:map
   [:channel_id {:description "Discord channel ID to send the message to. Use discord.list.channels to discover IDs."} :string]
   [:text {:description "Message content to send. Long messages will be chunked automatically."} :string]
   [:reply_to {:optional true :description "Optional message ID to reply to."} :string]
   [:attachment_urls {:optional true
                      :description "Optional attachment sources to upload: HTTP(S) URLs, data URLs, absolute file paths, or workspace-relative paths (e.g. sandbox output files, generated images)."}
    [:vector :string]]])
(def discord-send-tool (partial create-tool-obj "discord.send" "Discord Send" "Send a message to a Discord channel, optionally as a reply to an existing message."
                          "Send a Discord message or reply to a specific message id."
                          ["Use discord.publish or discord.send to share results in a Discord channel."
                                    "Provide channelId/channel_id and content/text."
                                    "Include attachmentUrls/attachment_urls to upload files, images, or generated assets."
                                    "Pass file paths as plain strings (e.g. Graphics/seal.svg or Voice/clip.mp3). Do NOT wrap them in <|\"| delimiters."
                                    "To mention a user, use <@user_id> in the text. Do NOT use @username — it will not ping."]
                          send-params
                          discord-send-execute))


;; ---------------------------------------------------------------------------
;; Hoisted tool definitions: params, execute, tool-obj per tool
;; ---------------------------------------------------------------------------

(def channel-messages-params
  [:map
   [:channel_id {:description "Discord channel ID to fetch messages from."} :string]
   [:limit {:optional true :description "Maximum number of messages to fetch (default 50, max 100)."} [:int {:min 1 :max 100}]]
   [:before {:optional true :description "Fetch messages before this message ID."} :string]
   [:after {:optional true :description "Fetch messages after this message ID."} :string]
   [:around {:optional true :description "Fetch messages around this message ID."} :string]])

(defn channel-messages-execute [runtime config _tool-call-id params a b c]
  (let [params (tool-params params)
        on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        channel-id (or (pget params :channel_id :channelId) "")]
    (maybe-tool-update! on-update (str "Fetching Discord messages from channel " channel-id "…"))
    (-> (discord-fetch-channel-messages! runtime config channel-id {:limit (pget params :limit)
                                                            :before (pget params :before)
                                                            :after (pget params :after)
                                                            :around (pget params :around)})
        (.then (fn [result]
                 (-> (attach-openplanner-labels! config (:messages result))
                     (.then (fn [messages]
                              (let [filtered (assoc result :messages messages :count (count messages))]
                                (tool-text-result (discord-messages-text (str "Fetched " (:count filtered) " non-bad messages from channel " channel-id ".") messages)
                                                  filtered))))))))))

(def channel-messages-tool (partial create-tool-obj "discord.channel.messages" "Discord Channel Messages"
                                    "Fetch messages from a Discord channel with before/after/around cursors."
                                    "Fetch channel messages from Discord with pagination cursors when you need exact transcript context."
                                    ["Use this when you know the channel id and need exact message history."
                                              "Use before/after/around for precise pagination."]
                                    channel-messages-params
                                    channel-messages-execute))

(def channel-scroll-params
  [:map
   [:channel_id {:description "Discord channel ID to fetch older messages from."} :string]
   [:oldest_seen_id {:description "Oldest message ID already seen; fetch messages before this."} :string]
   [:limit {:optional true :description "Maximum number of older messages to fetch."} [:int {:min 1 :max 100}]]])

(defn channel-scroll-execute [runtime config _tool-call-id params a b c]
  (let [params (tool-params params)
        on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        channel-id (or (pget params :channel_id :channelId) "")
        oldest-seen-id (or (pget params :oldest_seen_id :oldestSeenId) "")]
    (maybe-tool-update! on-update (str "Scrolling older Discord messages in channel " channel-id "…"))
    (-> (discord-scroll-channel-messages! runtime config channel-id oldest-seen-id (pget params :limit))
        (.then (fn [result]
                 (-> (attach-openplanner-labels! config (:messages result))
                     (.then (fn [messages]
                              (let [filtered (assoc result :messages messages :count (count messages))]
                                (tool-text-result (discord-messages-text (str "Fetched older non-bad messages before " oldest-seen-id ".") messages)
                                                  filtered))))))))))

(def channel-scroll-tool (partial create-tool-obj "discord.channel.scroll" "Discord Channel Scroll"
                                  "Scroll older channel messages by fetching messages before the oldest already-seen message id."
                                  "Scroll backwards in a Discord channel once you already know the oldest seen id."
                                  ["Use discord.channel.scroll as sugar over discord.channel.messages before=oldest_seen_id."
                                            "Useful for paging backward through long histories."]
                                  channel-scroll-params
                                  channel-scroll-execute))

(def dm-messages-params
  [:map
   [:user_id {:description "Discord user ID whose DM channel should be read."} :string]
   [:limit {:optional true :description "Maximum number of DM messages to fetch."} [:int {:min 1 :max 100}]]
   [:before {:optional true :description "Fetch DM messages before this message ID."} :string]])

(defn dm-messages-execute [runtime config _tool-call-id params a b c]
  (let [params (tool-params params)
        on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        user-id (or (pget params :user_id :userId) "")]
    (maybe-tool-update! on-update (str "Fetching Discord DM messages for user " user-id "…"))
    (-> (discord-fetch-dm-messages! runtime config user-id {:limit (pget params :limit)
                                                    :before (pget params :before)})
        (.then (fn [result]
                 (-> (attach-openplanner-labels! config (:messages result))
                     (.then (fn [messages]
                              (let [filtered (assoc result :messages messages :count (count messages))]
                                (tool-text-result (discord-messages-text (str "Fetched " (:count filtered) " non-bad DM messages for user " user-id ".") messages)
                                                  filtered))))))))))

(def dm-messages-tool (partial create-tool-obj "discord.dm.messages" "Discord DM Messages"
                               "Fetch messages from the DM channel shared with a Discord user."
                               "Read DM history with a Discord user by user id."
                               ["Use this when the relevant conversation is in DMs rather than a guild channel."
                                         "Provide the target user id."]
                               dm-messages-params
                               dm-messages-execute))

(def search-params
  [:map
   [:scope {:description "Search scope: channel or dm."} :string]
   [:channel_id {:optional true :description "Discord channel ID to search when scope=channel."} :string]
   [:user_id {:optional true :description "Discord user ID to search against when scope=dm or to filter author."} :string]
   [:query {:optional true :description "Optional substring query to filter messages by content."} :string]
   [:limit {:optional true :description "Maximum number of matching messages to return."} [:int {:min 1 :max 100}]]
   [:before {:optional true :description "Fetch messages before this message ID."} :string]
   [:after {:optional true :description "Fetch messages after this message ID."} :string]
   [:since_hours {:optional true :description "Prefer matching messages within this many hours (default 168); pass a larger value to override the timeframe."} [:int {:min 1}]]])

(defn search-execute [runtime config _tool-call-id params a b c]
  (let [params (tool-params params)
        on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        scope (or (pget params :scope) "channel")
        channel-id (pget params :channel_id :channelId)
        user-id (pget params :user_id :userId)
        query (or (pget params :query) "")]
    (maybe-tool-update! on-update (str "Searching Discord messages in scope " scope "…"))
    (-> (discord-search-messages! runtime config scope {:channel-id channel-id
                                                :user-id user-id
                                                :query query
                                                :limit (pget params :limit)
                                                :before (pget params :before)
                                                :after (pget params :after)
                                                :since-hours (pget params :since_hours :sinceHours)})
        (.then (fn [result]
                 (tool-text-result (discord-messages-text (str "Found " (:count result) " matching Discord messages.") (:messages result))
                                   result))))))

(def search-tool (partial create-tool-obj "discord.search" "Discord Search"
                          "Search channel or DM messages by content and/or author using client-side filtering."
                          "Search Discord messages by text and scope to find relevant discussion quickly."
                          ["Use scope=channel with channel_id for guild channels or scope=dm with user_id for DMs."
                                    "Messages marked bad in OpenPlanner labels are never shown."
                                    "Matching good-marked messages are returned first in chronological order, then unbad messages chronologically."
                                    "The default timeframe is 168 hours; pass since_hours to override when needed."
                                    "This falls back to client-side filtering when native search is unavailable."]
                          search-params
                          search-execute))

(def list-servers-params
  [:map])

(defn list-servers-execute [runtime config _tool-call-id _params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))]
    (maybe-tool-update! on-update "Listing Discord servers…")
    (-> (discord-list-guilds! runtime)
        (.then (fn [result]
                 (let [lines (->> (:servers result)
                                  (map (fn [server] (str (:name server) " (" (:id server) ")")))
                                  (str/join "\n"))]
                   (tool-text-result (str "Discord servers:\n" lines) result)))))))

(def list-servers-tool (partial create-tool-obj "discord.list.servers" "Discord List Servers"
                                "List all Discord servers/guilds the bot can access."
                                "List Discord servers before choosing channels or replying into a guild."
                                ["Use this before discord.list.channels when you need discovery."
                                          "Do not guess guild ids."]
                                list-servers-params
                                list-servers-execute))

(def guilds-tool (partial create-tool-obj "discord.guilds" "Discord Guilds"
                          "List Discord guilds/servers the bot is in."
                          "List Discord guilds to discover available servers."
                          ["Alias for discord.list.servers."
                                    "Use before listing channels or posting to a specific server."]
                          list-servers-params
                          list-servers-execute))

(def list-channels-params
  [:map
   [:guild_id {:optional true :description "Optional guild/server ID. If omitted, returns channels across all visible guilds."} :string]])

(defn list-channels-execute [runtime config _tool-call-id params a b c]
  (let [params (tool-params params)
        on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        guild-id (pget params :guild_id :guildId)]
    (maybe-tool-update! on-update "Listing Discord channels…")
    (-> (discord-list-channels! runtime guild-id)
        (.then (fn [result]
                 (let [lines (->> (:channels result)
                                  (map (fn [channel] (str "#" (:name channel) " (" (:id channel) ") guild=" (:guildId channel))))
                                  (str/join "\n"))]
                   (tool-text-result (str "Discord channels:\n" lines) result)))))))

(def list-channels-tool (partial create-tool-obj "discord.list.channels" "Discord List Channels"
                                 "List channels in one Discord guild or across all visible guilds."
                                 "List Discord channels to discover readable/postable targets."
                                 ["If guild_id is omitted, returns channels across all visible guilds."
                                           "Use returned channel ids with discord.channel.messages or discord.send."]
                                 list-channels-params
                                 list-channels-execute))

(def channels-params
  [:map
   [:guild_id {:description "Discord guild ID to list channels for."} :string]])

(defn channels-execute [runtime config tool-call-id params a b c]
  (let [params (tool-params params)]
    (list-channels-execute runtime config tool-call-id
                           {:guild_id (pget params :guildId :guild_id)}
                           a b c)))

(def channels-tool (partial create-tool-obj "discord.channels" "Discord Channels"
                            "List channels in a Discord guild."
                            "List channels in a Discord guild to find the right channel for reading or posting."
                            ["Alias for discord.list.channels."
                                      "Use guildId/guild_id when you already know the server."]
                            channels-params
                            channels-execute))

(def react-params
  [:map
   [:channel_id {:description "Discord channel ID containing the message to react to."} :string]
   [:message_id {:description "Discord message ID to react to."} :string]
   [:emoji {:description "Emoji to react with (e.g. 👍, 🎉, 💀)."} :string]])

(defn react-execute [runtime config _tool-call-id params a b c]
  (let [params (tool-params params)
        on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        channel-id (or (pget params :channel_id :channelId) "")
        message-id (or (pget params :message_id :messageId) "")
        emoji (or (pget params :emoji) "")]
    (maybe-tool-update! on-update (str "Reacting to message " message-id " with " emoji "…"))
    (-> (discord-react! runtime channel-id message-id emoji)
        (.then (fn [result]
                 (tool-text-result (str "Reacted with " emoji " to message " message-id) result))))))

(def react-tool (partial create-tool-obj "discord.react" "Discord React"
                         "Add an emoji reaction to a Discord message."
                         "React to a Discord message with an emoji."
                         ["Use discord.react to add emoji reactions to messages."
                                   "Provide channel_id, message_id, and an emoji (e.g. 👍, 🎉, 💀)."]
                         react-params
                         react-execute))

(def thread-create-params
  [:map
   [:channel_id {:description "Discord channel ID to create the thread in."} :string]
   [:message_id {:optional true :description "Optional message ID to create a thread from. If omitted, creates a standalone thread in the channel."} :string]
   [:name {:description "Name of the thread (max 100 chars)."} :string]
   [:auto_archive_duration {:optional true :description "Auto-archive duration in minutes: 60, 1440 (default), 4320, or 10080."} :int]])

(defn thread-create-execute [runtime config _tool-call-id params a b c]
  (let [params (tool-params params)
        on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        channel-id (or (pget params :channel_id :channelId) "")
        message-id (pget params :message_id :messageId)
        name (or (pget params :name) "")
        auto-archive (pget params :auto_archive_duration :autoArchiveDuration)]
    (maybe-tool-update! on-update (str "Creating thread '" name "' in channel " channel-id "…"))
    (-> (discord-thread-create! runtime channel-id message-id name auto-archive)
        (.then (fn [result]
                 (tool-text-result (str "Created thread " (:threadId result) " named '" name "'" )
                                   result))))))

(def thread-create-tool (partial create-tool-obj "discord.thread.create" "Discord Thread Create"
                                 "Create a Discord thread from a message or in a channel."
                                 "Create a thread to spin off a conversation."
                                 ["Use discord.thread.create to start a thread from a message or in a channel."
                                           "Provide channel_id and a name. Optionally pass message_id to create a thread from that message."
                                           "After creating a thread, use the returned threadId as channel_id with discord.send to post in it."]
                                 thread-create-params
                                 thread-create-execute))

;; ---------------------------------------------------------------------------
;; Wrapper tools: remap params then delegate to an existing execute
;; ---------------------------------------------------------------------------

(def publish-params
  [:map
   [:channel_id {:description "Discord channel ID to post the message to."} :string]
   [:content {:description "Message content to post to the Discord channel."} :string]
   [:attachment_urls {:optional true
                      :description "Optional attachment sources to upload: HTTP(S) URLs, data URLs, absolute file paths, or workspace-relative paths (e.g. sandbox output files, generated images)."}
    [:vector :string]]])

(defn publish-execute [runtime config tool-call-id params a b c]
  (let [params (tool-params params)]
    (discord-send-execute runtime config tool-call-id
                          {:channel_id (or (pget params :channel_id :channelId) "")
                           :text (or (pget params :content :text) "")
                           :attachment_urls (or (pget params :attachment_urls :attachmentUrls) [])}
                          a b c)))

(def publish-tool (partial create-tool-obj "discord.publish" "Discord Publish"
                           "Post a message to a Discord channel using the configured Knoxx Discord bot."
                           "Post updates, summaries, or notifications to Discord channels."
                           ["Use discord.publish or discord.send to share results in a Discord channel."
                                     "Provide channelId/channel_id and content/text."
                                     "Include attachmentUrls/attachment_urls to upload files, images, or generated assets."
                                     "To mention a user, use <@user_id> in the text. Do NOT use @username — it will not ping."]
                           publish-params
                           publish-execute))

(def read-params
  [:map
   [:channel_id {:description "Discord channel ID to read messages from."} :string]
   [:limit {:optional true :description "Maximum number of messages to return."} [:int {:min 1 :max 100}]]])

(defn read-execute [runtime config tool-call-id params a b c]
  (let [params (tool-params params)]
    (channel-messages-execute runtime config tool-call-id
                              {:channel_id (or (pget params :channel_id :channelId) "")
                               :limit (pget params :limit)}
                              a b c)))

(def read-tool (partial create-tool-obj "discord.read" "Discord Read"
                        "Read recent messages from a Discord channel."
                        "Read recent messages from a Discord channel to understand context."
                        ["Use discord.read as a simple alias for discord.channel.messages."
                                  "For pagination or cursors, use discord.channel.messages or discord.channel.scroll directly."]
                        read-params
                        read-execute))

;; ---------------------------------------------------------------------------
;; create-discord-custom-tools: gate + collect
;; ---------------------------------------------------------------------------

(defn create-discord-custom-tools
  ([runtime config] (create-discord-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))]
     (xdiscord/tool-array
      (remove nil?
              [(when (allowed? "discord.publish") (publish-tool runtime config))
               (when (allowed? "discord.send") (discord-send-tool runtime config))
               (when (allowed? "discord.react") (react-tool runtime config))
               (when (allowed? "discord.thread.create") (thread-create-tool runtime config))
               (when (allowed? "discord.read") (read-tool runtime config))
               (when (allowed? "discord.channel.messages") (channel-messages-tool runtime config))
               (when (allowed? "discord.channel.scroll") (channel-scroll-tool runtime config))
               (when (allowed? "discord.dm.messages") (dm-messages-tool runtime config))
               (when (allowed? "discord.search") (search-tool runtime config))
               (when (allowed? "discord.guilds") (guilds-tool runtime config))
               (when (allowed? "discord.list.servers") (list-servers-tool runtime config))
               (when (allowed? "discord.channels") (channels-tool runtime config))
               (when (allowed? "discord.list.channels") (list-channels-tool runtime config))])))))

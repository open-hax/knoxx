(ns knoxx.backend.discord-gateway
  "Discord gateway manager — native CLJS implementation using discord.js.

   Uses direct discord.js import via shadow-cljs :keep-as-import.
   The :keep-as-import #{\"discord.js\"} in shadow-cljs.edn tells shadow-cljs
   to skip dependency analysis for discord.js, generating a bare import statement.
   Node.js resolves transitive Node.js built-in deps (events, buffer, etc.) at runtime.

   Exported: createDiscordGatewayManager — factory function returning a JS object
   with async methods. Also provides a CLJS convenience API via set-manager!."
  (:require [clojure.string :as str]
            ["discord.js" :as discord]
            ["@discordjs/voice" :as voice]
            ["libsodium-wrappers" :as sodium]
            ["node:stream" :refer [Readable]]))

;; sodium is imported for @discordjs/voice crypto support — no explicit use needed

(declare set-manager!)

;; ---------------------------------------------------------------------------
;; discord.js imports
;; ---------------------------------------------------------------------------

(defn- intent-bits [] (aget discord "GatewayIntentBits"))
(defn- partials-enum [] (aget discord "Partials"))
(defn- events-enum [] (aget discord "Events"))
(defn- channel-type-enum [] (aget discord "ChannelType"))
(defn- Client-class [] (aget discord "Client"))

;; ---------------------------------------------------------------------------
;; Internal helpers
;; ---------------------------------------------------------------------------

(defn- map-message
  "Convert a discord.js Message to a plain JS map."
  [message]
  (let [author (.-author message)]
    #js {:id (.-id message)
         :channelId (.-channelId message)
         :content (or (.-content message) "")
         :authorId (or (when author (.-id author)) "")
         :authorUsername (or (when author (.-username author)) "unknown")
         :authorIsBot (boolean (when author (.-bot author)))
         :timestamp (try (.toISOString (.-createdAt message))
                         (catch js/Error _ (.toISOString (js/Date.))))
         :attachments (into-array
                       (for [[_id att] (.-attachments message)]
                         #js {:id (.-id att)
                              :filename (or (.-name att) "")
                              :contentType (or (.-contentType att) nil)
                              :size (or (.-size att) 0)
                              :url (or (.-url att) "")}))
         :embeds (into-array
                  (for [embed (.-embeds message)]
                    #js {:title (or (.-title embed) nil)
                         :description (or (.-description embed) nil)
                         :url (or (.-url embed) nil)}))}))

(defn- readable-text-channel?
  "Check if a channel is a text-based channel we can read."
  [channel]
  (and channel
       (fn? (.-isTextBased channel))
       (.isTextBased channel)))

(defn- sort-newest-first
  "Sort an array of message maps by timestamp, newest first."
  [messages]
  (js/Array.from (.sort (into-array messages)
                        (fn [a b]
                          (.localeCompare (str (aget b "timestamp"))
                                          (str (aget a "timestamp")))))))

(defn- split-message
  "Split text into chunks of ≤2000 chars, preferring paragraph/line/word breaks."
  [text]
  (let [normalized (.trim (str (or text "")))]
    (if (<= (.-length normalized) 2000)
      #js [normalized]
      (let [parts (atom #js [])
            remaining (atom normalized)]
        (while (> (.-length @remaining) 2000)
          (let [r @remaining
                split-at-para (.lastIndexOf r "\n\n" 2000)
                split-at-line (.lastIndexOf r "\n" 2000)
                split-at-space (.lastIndexOf r " " 2000)
                split-at (cond
                           (> split-at-para 1000) split-at-para
                           (> split-at-line 1000) split-at-line
                           (> split-at-space 1000) split-at-space
                           :else 2000)]
            (swap! parts (fn [p] (.concat p #js [(.trimEnd (.slice r 0 split-at))])))
            (reset! remaining (.trimStart (.slice r split-at)))))
        (when (> (.-length @remaining) 0)
          (swap! parts (fn [p] (.concat p #js [@remaining]))))
        @parts))))

(defn- attachment-value
  "Read an attachment field from either a CLJS map or a plain JS object."
  [attachment k js-key]
  (or (when (map? attachment) (get attachment k))
      (when (object? attachment) (aget attachment js-key))))

(defn- discord-file-payload
  [attachment]
  (let [buffer (or (attachment-value attachment :buffer "buffer")
                   (attachment-value attachment :attachment "attachment"))
        name (or (attachment-value attachment :name "name")
                 (attachment-value attachment :filename "filename")
                 "attachment.bin")]
    (when-not buffer
      (throw (js/Error. "Discord attachment is missing file data")))
    #js {:attachment buffer
         :name name}))

;; ---------------------------------------------------------------------------
;; Gateway method implementations (extracted for readability)
;; ---------------------------------------------------------------------------

(defn- gw-start
  "Start the gateway client with a bot token."
  [client-state ready-promise current-token listeners log this-stop build-client token]
  (let [next-token (.trim (str (or token "")))]
    (if (= next-token "")
      (.then (this-stop) (fn [_] nil))
      (if (and @client-state (= @current-token next-token))
        (if @ready-promise @ready-promise (js/Promise.resolve @client-state))
        (.then (this-stop)
               (fn [_]
                 (reset! current-token next-token)
                 (let [new-client (build-client)]
                   (reset! client-state new-client)
                   (let [login-promise
                         (-> (.login new-client next-token)
                             (.then (fn [_] new-client))
                             (.catch (fn [error]
                                       (when (.-error? log)
                                         (.error log "[discord-gateway] login failed" error))
                                       (try (.destroy new-client) (catch js/Error _))
                                       (reset! client-state nil)
                                       (reset! ready-promise nil)
                                       (reset! current-token nil)
                                       (js/Promise.reject error))))]
                     (reset! ready-promise login-promise)
                     login-promise))))))))

(defn- gw-stop
  "Stop the gateway client."
  [client-state ready-promise current-token]
  (let [result (if @client-state
                 (try (.then (.destroy @client-state) (fn [_] nil))
                      (catch js/Error _ (js/Promise.resolve nil)))
                 (js/Promise.resolve nil))]
    (reset! client-state nil)
    (reset! ready-promise nil)
    (reset! current-token nil)
    result))

(defn- gw-status
  "Get gateway status."
  [client-state]
  (let [c @client-state]
    (cond-> #js {:started (some? c)
                 :ready false
                 :userId nil
                 :userTag nil
                 :guildCount 0}
      c (doto
            (aset "ready" (try (.isReady c) (catch js/Error _ false)))
          (aset "userId" (try (.-id (.-user c)) (catch js/Error _ nil)))
          (aset "userTag" (try (.-tag (.-user c)) (catch js/Error _ nil)))
          (aset "guildCount" (try (.. c -guilds -cache -size) (catch js/Error _ 0)))))))
(defn- gw-list-servers
  "List all guilds the bot is in."
  [ensure-client]
  (.then (ensure-client)
         (fn [active-client]
           (into-array
            (for [[_id guild] (.. active-client -guilds -cache)]
              #js {:id (.-id guild)
                   :name (.-name guild)
                   :memberCount (or (.-memberCount guild) nil)})))))

(defn- gw-list-channels
  "List channels in a guild or all guilds."
  [ensure-client log guild-id]
  (.then (ensure-client)
         (fn [active-client]
           (let [ChannelType (channel-type-enum)
                 collect (fn [guild]
                           (-> (.fetch (.. guild -channels))
                               (.then (fn [fetched]
                                        (into-array
                                         (for [[_id ch] fetched
                                               :when (and ch
                                                          (readable-text-channel? ch)
                                                          (not= (.-type ch) (.-DM ChannelType)))]
                                           #js {:id (.-id ch)
                                                :name (or (.-name ch) "")
                                                :guildId (.-id guild)
                                                :type (str (.-type ch))}))))))]
             (if guild-id
               (let [guild (.. active-client -guilds -cache (get guild-id))]
                 (if-not guild
                   (js/Promise.reject (js/Error. (str "Guild not found: " guild-id)))
                   (collect guild)))
               ;; All guilds — collect from each, suppress per-guild errors
               (let [promises (atom #js [])]
                 (doseq [[_id guild] (.. active-client -guilds -cache)]
                   (swap! promises (fn [ps]
                                     (.concat ps #js [(-> (collect guild)
                                                          (.catch (fn [err]
                                                                    (when (.-warn? log)
                                                                      (.warn log "[discord-gateway] listChannels guild failed" (.-id guild) err))
                                                                    #js [])))]))))
                 (.then (js/Promise.all @promises)
                        (fn [results]
                          (let [flat (atom #js [])]
                            (doseq [r results]
                              (swap! flat (fn [f] (.concat f r))))
                            @flat)))))))))

(defn- gw-fetch-channel-messages
  "Fetch messages from a channel."
  [ensure-client channel-id opts]
  (.then (ensure-client)
         (fn [active-client]
           (-> (.fetch (.. active-client -channels) channel-id)
               (.then (fn [channel]
                        (if (or (not channel) (not (readable-text-channel? channel)))
                          (js/Promise.reject (js/Error. (str "Channel not found or not text-based: " channel-id)))
                          (-> (.fetch (.. channel -messages)
                                      (clj->js {:limit (max 1 (min 100 (or (aget opts "limit") 50)))
                                                :before (aget opts "before")
                                                :after (aget opts "after")
                                                :around (aget opts "around")}))
                              (.then (fn [fetched]
                                       (sort-newest-first
                                        (map map-message (for [[_id msg] fetched] msg)))))))))))))

(defn- gw-fetch-dm-messages
  "Fetch DM messages with a user."
  [ensure-client user-id opts]
  (.then (ensure-client)
         (fn [active-client]
           (-> (.fetch (.. active-client -users) user-id)
               (.then (fn [user]
                        (-> (.createDM user)
                            (.then (fn [dm]
                                     (-> (.fetch (.. dm -messages)
                                                 (clj->js {:limit (max 1 (min 100 (or (aget opts "limit") 50)))
                                                           :before (aget opts "before")}))
                                         (.then (fn [fetched]
                                                  #js {:dmChannelId (.-id dm)
                                                       :messages (sort-newest-first
                                                                  (map map-message (for [[_id msg] fetched] msg)))}))))))))))))

(defn- search-filter-fn
  "Create a filter function for message search."
  [opts]
  (let [needle (.toLowerCase (str (or (aget opts "query") "")))
        target-user-id (aget opts "userId")]
    (fn [message]
      (let [content-ok (or (= needle "")
                           (.includes (.toLowerCase (or (aget message "content") "")) needle))
            author-ok (or (not target-user-id)
                          (= (aget message "authorId") target-user-id))]
        (and content-ok author-ok)))))

(defn- gw-search-messages
  "Search messages in a channel or DM."
  [this-fn scope opts]
  (let [normalized-scope (.toLowerCase (str (or scope "channel")))]
    (if (= normalized-scope "dm")
      (-> (.fetchDmMessages this-fn (aget opts "userId")
                            (clj->js {:limit 100 :before (aget opts "before")}))
          (.then (fn [result]
                   (let [filtered (.filter (aget result "messages") (search-filter-fn opts))]
                     #js {:dmChannelId (aget result "dmChannelId")
                          :messages (.slice filtered 0 (or (aget opts "limit") 50))
                          :count (min (.-length filtered) (or (aget opts "limit") 50))
                          :source "gateway-cache"}))))
      (-> (.fetchChannelMessages this-fn (aget opts "channelId")
                                 (clj->js {:limit 100
                                           :before (aget opts "before")
                                           :after (aget opts "after")}))
          (.then (fn [messages]
                   (let [filtered (.filter messages (search-filter-fn opts))]
                     #js {:channelId (aget opts "channelId")
                          :messages (.slice filtered 0 (or (aget opts "limit") 50))
                          :count (min (.-length filtered) (or (aget opts "limit") 50))
                          :source "gateway-cache"})))))))

(defn- gw-send-message
  "Send a message to a channel, splitting into chunks if needed."
  [ensure-client channel-id text reply-to attachments]
  (.then (ensure-client)
         (fn [active-client]
           (-> (.fetch (.. active-client -channels) channel-id)
               (.then (fn [channel]
                        (if (or (not channel) (not (readable-text-channel? channel)))
                          (js/Promise.reject (js/Error. (str "Channel not found or not text-based: " channel-id)))
                          (let [base-text (str (or text ""))
                                chunks (split-message (if (and (str/blank? base-text)
                                                               (seq attachments))
                                                        "[attachment]"
                                                        base-text))]
                            (-> (.reduce chunks
                                         (fn [promise chunk index]
                                           (.then (or promise (js/Promise.resolve nil))
                                                  (fn [_]
                                                    (let [payload (clj->js {:content chunk})]
                                                      (when (and (= index 0) reply-to)
                                                        (aset payload "reply" (clj->js {:messageReference reply-to})))
                                                      (when (and (= index 0) (seq attachments))
                                                        (aset payload "files"
                                                              (into-array (map discord-file-payload attachments))))
                                                      (.send channel payload)))))
                                         nil)
                                (.then (fn [_]
                                         #js {:channelId channel-id
                                              :messageId ""
                                              :sent true
                                              :timestamp (.toISOString (js/Date.))
                                              :chunkCount (.-length chunks)
                                              :attachmentCount (count attachments)})))))))))))

;; ---------------------------------------------------------------------------
;; Voice helpers
;; ---------------------------------------------------------------------------

(defn- gw-join-voice
  "Join a voice channel. Returns a VoiceConnection."
  [ensure-client channel-id]
  (js/console.log "[voice:gw] joining channel:" channel-id)
  (.then (ensure-client)
         (fn [active-client]
           (-> (.fetch (.. active-client -channels) channel-id)
               (.then (fn [channel]
                        (if-not channel
                          (do (js/console.error "[voice:gw] channel not found:" channel-id)
                              (js/Promise.reject (js/Error. (str "Channel not found: " channel-id))))
                          (let [guild-id (.-guildId channel)]
                            (js/console.log "[voice:gw] channel found:" channel-id "guild:" guild-id "selfDeaf:false")
                            (let [conn (voice/joinVoiceChannel
                                        #js {:channelId channel-id
                                             :guildId guild-id
                                             :adapterCreator (.-voiceAdapterCreator (.-guild channel))
                                             :selfDeaf false
                                             :selfMute false})]
                              (aset conn "__guildId" guild-id)
                              (js/console.log "[voice:gw] joinVoiceChannel returned, waiting for ready state…")
                               (-> (voice/entersState conn (.-Ready voice/VoiceConnectionStatus) 15000)
                                  (.then (fn [_]
                                           (js/console.log "[voice:gw] voice connection ready for guild:" (.-guildId conn))
                                           conn))
                                  (.catch (fn [err]
                                            (js/console.error "[voice:gw] voice connection failed to ready:" (.-message err))
                                            (js/Promise.reject err)))))))))))))

(defn- gw-leave-voice
  "Leave a voice channel for a guild."
  [connections guild-id]
  (when-let [conn (.get connections guild-id)]
    (.destroy conn)
    (.delete connections guild-id))
  (js/Promise.resolve true))

(defn- gw-play-audio
  "Play an audio buffer (PCM s16le 48kHz stereo or any ffmpeg-decodable) in the voice connection."
  [connections guild-id audio-buffer]
  (let [conn (.get connections guild-id)]
    (if-not conn
      (js/Promise.reject (js/Error. (str "No voice connection for guild: " guild-id)))
      (let [player (or (.-__audioPlayer conn)
                       (let [p (voice/createAudioPlayer)]
                         (aset conn "__audioPlayer" p)
                         (.subscribe conn p)
                         p))
            stream (new Readable #js {:read (fn [])})
            _ (do (.push stream audio-buffer) (.push stream nil))
            resource (voice/createAudioResource
                      stream
                      #js {:inputType (.-Arbitrary (aget voice "StreamType"))})]
        (.play player resource)
        (js/Promise.resolve true)))))

(defn- gw-subscribe-voice
  "Subscribe to audio from a specific user. Returns an unsubscribe function."
  [connections guild-id user-id callback]
  (let [conn (.get connections guild-id)]
    (if-not conn
      (js/Promise.reject (js/Error. (str "No voice connection for guild: " guild-id)))
      (let [receiver (.-receiver conn)
            opus-stream (.subscribe receiver user-id #js {:mode "opus"})]
        (.on opus-stream "data" (fn [chunk] (callback user-id chunk)))
        (js/Promise.resolve
         (fn [] (.destroy opus-stream)))))))

(defn- gw-list-voice-members
  "List members in a voice channel."
  [ensure-client guild-id channel-id]
  (.then (ensure-client)
         (fn [active-client]
           (-> (.fetch (.. active-client -guilds) guild-id)
               (.then (fn [guild]
                        (if-not guild
                          (js/Promise.reject (js/Error. (str "Guild not found: " guild-id)))
                          (-> (.fetch (.. guild -channels) channel-id)
                              (.then (fn [channel]
                                       (if-not channel
                                         (js/Promise.reject (js/Error. (str "Channel not found: " channel-id)))
                                         (let [members (.-members channel)]
                                           (into-array
                                            (for [[_ member] members]
                                              (let [user (.-user member)]
                                                #js {:userId (.-id user)
                                                     :username (.-username user)
                                                     :displayName (or (.-displayName member) (.-username user))
                                                     :isBot (boolean (.-bot user))
                                                     :isMuted (boolean (.-mute member))
                                                     :isDeaf (boolean (.-deaf member))
                                                     :isSpeaking false})))))))))))))))

(defn- gw-start-voice-listener
  "Start listening for all speaking users in a voice channel.
   Calls (on-start user-id) when a user starts speaking.
   Calls (on-audio user-id opus-buffer) when a user stops speaking with accumulated audio.
   Returns a stop function."
  [connections guild-id on-start on-audio]
  (js/console.log "[voice:listener] starting for guild:" guild-id "connections:" (.-size connections))
  (let [conn (.get connections guild-id)]
    (if-not conn
      (do (js/console.error "[voice:listener] no connection for guild:" guild-id)
          (js/Promise.reject (js/Error. (str "No voice connection for guild: " guild-id))))
      (let [receiver (.-receiver conn)
            speaking-map (.-speaking receiver)
            buffers (atom {})
            streams (atom {})
            chunk-counts (atom {})
            active-users (atom #{})
            silence-timers (atom {})
            on-start-speaking
            (fn [user-id]
              (let [uid (str user-id)]
                (when-not (contains? @active-users uid)
                  (js/console.log "[voice:listener] >>> SPEAKING START:" uid)
                  (swap! active-users conj uid)
                  (when on-start (on-start uid))
                  (swap! buffers assoc uid #js [])
                  (swap! chunk-counts assoc uid 0)
                  (let [audio-stream (.subscribe receiver uid)]
                    (js/console.log "[voice:listener] subscribed audio for:" uid)
                    (.on audio-stream "data"
                         (fn [chunk]
                           (when-let [buf (get @buffers uid)]
                             (.push buf chunk)
                             (swap! chunk-counts update uid inc))))
                    (.on audio-stream "error"
                         (fn [err]
                           (js/console.error "[voice:listener] audio stream error for" uid ":" (.-message err))))
                    (.on audio-stream "end"
                         (fn []
                           (js/console.log "[voice:listener] audio stream ended for" uid)))
                    (swap! streams assoc uid audio-stream)))))
            on-opus-packet
            (fn [uid packet]
              (let [user-id (str uid)]
                (when-not (contains? @active-users user-id)
                  (when-not (get @buffers user-id)
                    (js/console.log "[voice:listener] >>> OPUS START (fallback):" user-id)
                    (swap! active-users conj user-id)
                    (when on-start (on-start user-id))
                    (swap! buffers assoc user-id #js [])
                    (swap! chunk-counts assoc user-id 0))
                  (when-let [buf (get @buffers user-id)]
                    (.push buf packet)
                    (swap! chunk-counts update user-id inc)))))
            on-end-speaking
            (fn [user-id]
              (let [uid (str user-id)]
                (js/console.log "[voice:listener] >>> SPEAKING END:" uid)
                (swap! active-users disj uid)
                (when-let [audio-stream (get @streams uid)]
                  (try (.destroy audio-stream) (catch js/Error e
                                                 (js/console.error "[voice:listener] error destroying stream for" uid ":" (.-message e))))
                  (swap! streams dissoc uid))
                (when-let [buf (get @buffers uid)]
                  (let [len (.-length buf)]
                    (js/console.log "[voice:listener] stopped speaking:" uid "chunks:" len)
                    (when (pos? len)
                      (try
                        (let [flat (js/Array.from (.concat #js [] buf))
                              combined (.from js/Buffer flat)]
                          (js/console.log "[voice:listener] calling on-audio for" uid "bytes:" (.-length combined))
                          (swap! buffers dissoc uid)
                          (swap! chunk-counts dissoc uid)
                          (on-audio uid combined))
                        (catch js/Error e
                          (js/console.error "[voice:listener] error combining audio for" uid ":" (.-message e))
                          (swap! buffers dissoc uid)
                          (swap! chunk-counts dissoc uid))))))))]
        (js/console.log "[voice:listener] attaching listeners")
        (.on speaking-map "start" on-start-speaking)
        (.on speaking-map "end" on-end-speaking)
        (.on receiver "opus" on-opus-packet)
        (js/Promise.resolve
         (fn []
           (js/console.log "[voice:listener] stopping for guild:" guild-id)
           (.removeListener speaking-map "start" on-start-speaking)
           (.removeListener speaking-map "end" on-end-speaking)
           (.removeListener receiver "opus" on-opus-packet)
           (doseq [[_ s] @streams]
             (try (.destroy s) (catch js/Error _)))
           (doseq [[_ t] @silence-timers]
             (js/clearTimeout t))
           (reset! buffers {})
           (reset! streams {})
           (reset! chunk-counts {})
           (reset! active-users #{})
           (reset! silence-timers {})))))))

;; ---------------------------------------------------------------------------
;; Factory
;; ---------------------------------------------------------------------------

(defn createDiscordGatewayManager
  "Create a Discord gateway manager. Returns a JS object with async methods.

   Options (CLJS map or JS object):
     - :log / \"log\": logger object (default: console)

   Methods: start, stop, restart, onMessage, status, listServers,
            listChannels, fetchChannelMessages, fetchDmMessages,
            searchMessages, sendMessage"
  [opts]
  (let [log (or (when (map? opts) (:log opts))
                (when (object? opts) (aget opts "log"))
                js/console)
        client-state (atom nil)
        ready-promise (atom nil)
        current-token (atom nil)
        listeners (atom (js/Set.))
        reaction-listeners (atom (js/Set.))
        voice-state-listeners (atom (js/Set.))]

    (letfn [(notify-message [message]
              (let [mapped (map-message message)]
                (.forEach @listeners
                          (fn [listener]
                            (try
                              (listener mapped message)
                              (catch js/Error error
                                (when (.-error? log)
                                  (.error log "[discord-gateway] listener failed" error))))))))

            (notify-reaction [reaction user]
              (let [message (.-message reaction)
                    emoji (.-emoji reaction)
                    mapped #js {:emoji (or (.-name emoji) "")
                                :message (when message (map-message message))
                                :messageId (or (when message (.-id message)) "")
                                :channelId (or (when message (.-channelId message)) "")
                                :userId (or (when user (.-id user)) "")
                                :userUsername (or (when user (.-username user)) "unknown")}]
                (.forEach @reaction-listeners
                          (fn [listener]
                            (try
                              (listener mapped reaction user)
                              (catch js/Error error
                                (when (.-error? log)
                                  (.error log "[discord-gateway] reaction listener failed" error))))))))

            (notify-voice-state [old-state new-state]
              (let [old-channel-id (when old-state (.-channelId old-state))
                    new-channel-id (when new-state (.-channelId new-state))
                    user (when new-state (.-member new-state))
                    user-id (when user (.-id user))
                    guild-id (when new-state (.-guild new-state) (.-id (.-guild new-state)))
                    action (cond
                             (and (nil? old-channel-id) new-channel-id) "join"
                             (and old-channel-id (nil? new-channel-id)) "leave"
                             (and old-channel-id new-channel-id
                                  (not= old-channel-id new-channel-id)) "move"
                             :else nil)
                    mapped #js {:action action
                                :userId user-id
                                :username (or (when user (.-user user)) (when user (.-username user)) "unknown")
                                :guildId guild-id
                                :channelId (or new-channel-id old-channel-id)
                                :oldChannelId old-channel-id
                                :newChannelId new-channel-id}]
                (when action
                  (.forEach @voice-state-listeners
                            (fn [listener]
                              (try
                                (listener mapped old-state new-state)
                                (catch js/Error error
                                  (when (.-error? log)
                                    (.error log "[discord-gateway] voice state listener failed" error))))))))

              (build-client []
                            (let [Client (Client-class)
                                  GatewayIntentBits (intent-bits)
                                  Partials (partials-enum)
                                  Events (events-enum)
                                  next-client (new Client
                                                   (clj->js {:intents [(.-Guilds GatewayIntentBits)
                                                                       (.-GuildMessages GatewayIntentBits)
                                                                       (.-DirectMessages GatewayIntentBits)
                                                                       (.-GuildMessageReactions GatewayIntentBits)
                                                                       (.-DirectMessageReactions GatewayIntentBits)
                                                                       (.-GuildVoiceStates GatewayIntentBits)
                                                                       (.-MessageContent GatewayIntentBits)]
                                                             :partials [(.-Channel Partials)
                                                                        (.-Message Partials)
                                                                        (.-Reaction Partials)]}))]
                              (.on next-client (.-ClientReady Events)
                                   (fn [ready-client]
                                     (when (.-info? log)
                                       (.info log (str "[discord-gateway] ready as "
                                                       (or (when (.-user ready-client) (.-tag (.-user ready-client))) "unknown")
                                                       " in " (.. ready-client -guilds -cache -size) " guilds")))))
                              (.on next-client (.-MessageCreate Events)
                                   (fn [message] (notify-message message)))
                              (.on next-client (.-MessageReactionAdd Events)
                                   (fn [reaction user]
                                     (-> (if (.-partial reaction) (.fetch reaction) (js/Promise.resolve reaction))
                                         (.then (fn [full-reaction]
                                                  (let [message (.-message full-reaction)]
                                                    (-> (if (and message (.-partial message)) (.fetch message) (js/Promise.resolve message))
                                                        (.then (fn [_] (notify-reaction full-reaction user)))))))
                                         (.catch (fn [error]
                                                   (when (.-warn? log)
                                                     (.warn log "[discord-gateway] reaction ingest failed" error))))))))
                            (.on next-client (.-Error Events)
                                 (fn [error]
                                   (when (.-error? log)
                                     (.error log "[discord-gateway] client error" error))))
                            (.on next-client (.-VoiceStateUpdate Events)
                                 (fn [old-state new-state]
                                   (notify-voice-state old-state new-state)))
                            next-client))

            (ensure-client []
              (if-not @client-state
                (js/Promise.reject (js/Error. "Discord gateway client is not started"))
                (if @ready-promise
                  (.then @ready-promise (fn [_] @client-state))
                  (js/Promise.resolve @client-state))))]

      (let [this-stop (fn [] (gw-stop client-state ready-promise current-token))
            voice-connections (js/Map.)
            this-obj (atom nil)]

        (letfn [(this-fn [] @this-obj)]

          (reset! this-obj
                  #js {:start (fn [token] (gw-start client-state ready-promise current-token listeners log this-stop build-client token))
                       :stop (fn []
                               ;; Destroy all voice connections on stop
                               (.forEach voice-connections
                                         (fn [conn _key] (try (.destroy conn) (catch js/Error _))))
                               (.clear voice-connections)
                               (this-stop))
                       :restart (fn [token] (.then (this-stop) (fn [_] (.start (this-fn) token))))
                       :onMessage (fn [listener] (.add @listeners listener) (fn [] (.delete @listeners listener)))
                       :onReaction (fn [listener] (.add @reaction-listeners listener) (fn [] (.delete @reaction-listeners listener)))
                       :onVoiceStateUpdate (fn [listener] (.add @voice-state-listeners listener) (fn [] (.delete @voice-state-listeners listener)))
                       :status (fn [] (gw-status client-state))
                       :listServers (fn [] (gw-list-servers ensure-client))
                       :listChannels (fn [guild-id] (gw-list-channels ensure-client log guild-id))
                       :fetchChannelMessages (fn [channel-id opts] (gw-fetch-channel-messages ensure-client channel-id opts))
                       :fetchDmMessages (fn [user-id opts] (gw-fetch-dm-messages ensure-client user-id opts))
                       :searchMessages (fn [scope opts] (gw-search-messages (this-fn) scope opts))
                       :sendMessage (fn [channel-id text reply-to attachments] (gw-send-message ensure-client channel-id text reply-to attachments))
                       ;; Voice methods
                        :joinVoice (fn [channel-id]
                                     (.then (gw-join-voice ensure-client channel-id)
                                            (fn [conn]
                                              (let [guild-id (or (.-__guildId conn) (.-guildId conn))]
                                                (.set voice-connections guild-id conn)
                                                #js {:guildId guild-id :channelId channel-id :joined true}))))
                       :leaveVoice (fn [guild-id]
                                     (gw-leave-voice voice-connections guild-id)
                                     #js {:guildId guild-id :left true})
                       :playAudio (fn [guild-id audio-buffer]
                                    (gw-play-audio voice-connections guild-id audio-buffer))
                       :subscribeVoice (fn [guild-id user-id callback]
                                         (gw-subscribe-voice voice-connections guild-id user-id callback))
                       :startVoiceListener (fn [guild-id on-start on-audio]
                                             (gw-start-voice-listener voice-connections guild-id on-start on-audio))
                        :getVoiceConnection (fn [guild-id]
                                              (if guild-id
                                                (.get voice-connections guild-id)
                                                (when (> (.-size voice-connections) 0)
                                                  (let [entries (.entries voice-connections)]
                                                    (.-value (.next entries))))))
                       :listVoiceMembers (fn [guild-id channel-id]
                                           (gw-list-voice-members ensure-client guild-id channel-id))})

          (set-manager! @this-obj)
          @this-obj)))))

;; ---------------------------------------------------------------------------
;; Convenience CLJS API
;; ---------------------------------------------------------------------------

(defonce ^:private manager* (atom nil))

(defn set-manager!
  "Store the gateway manager instance for CLJS API access."
  [m]
  (reset! manager* m))

(defn gateway-manager
  "Returns the current gateway manager instance (or nil)."
  []
  @manager*)

(defn started?
  "Returns true if the gateway client exists."
  []
  (some? @manager*))

(defn ready?
  "Returns true if the gateway client is connected and ready."
  []
  (when-let [manager @manager*]
    (let [s (.status manager)]
      (boolean (aget s "ready")))))

(defn status
  "Get gateway status as a JS object."
  []
  (when-let [manager @manager*]
    (.status manager)))

(defn start!
  "Start the Discord gateway with the given token."
  [token]
  (when-let [manager @manager*]
    (.start manager token)))

(defn stop!
  "Stop the Discord gateway client."
  []
  (when-let [manager @manager*]
    (.stop manager)))

(defn restart!
  "Stop and restart with the given token."
  [token]
  (when-let [manager @manager*]
    (.restart manager token)))

(defn on-message!
  "Register a message listener. Returns an unsubscribe function."
  [listener]
  (when-let [manager @manager*]
    (.onMessage manager listener)))

(defn on-reaction!
  "Register a reaction listener. Returns an unsubscribe function."
  [listener]
  (when-let [manager @manager*]
    (.onReaction manager listener)))

(defn on-voice-state-update!
  "Register a voice state update listener. Returns an unsubscribe function."
  [listener]
  (when-let [manager @manager*]
    (.onVoiceStateUpdate manager listener)))

(defn list-servers
  "List all guilds the bot is in. Returns a Promise."
  []
  (when-let [manager @manager*]
    (.listServers manager)))

(defn list-channels
  "List channels in a guild (or all guilds if guild-id is nil). Returns a Promise."
  ([]
   (when-let [manager @manager*]
     (.listChannels manager)))
  ([guild-id]
   (when-let [manager @manager*]
     (.listChannels manager guild-id))))

(defn fetch-channel-messages
  "Fetch messages from a channel. Returns a Promise."
  [channel-id opts]
  (when-let [manager @manager*]
    (.fetchChannelMessages manager channel-id opts)))

(defn fetch-dm-messages
  "Fetch DM messages with a user. Returns a Promise."
  [user-id opts]
  (when-let [manager @manager*]
    (.fetchDmMessages manager user-id opts)))

(defn search-messages
  "Search messages in a channel or DM. Returns a Promise."
  [scope opts]
  (when-let [manager @manager*]
    (.searchMessages manager scope opts)))

(defn send-message
  "Send a message to a channel. Returns a Promise."
  ([channel-id text reply-to]
   (send-message channel-id text reply-to nil))
  ([channel-id text reply-to attachments]
   (when-let [manager @manager*]
     (.sendMessage manager channel-id text reply-to attachments))))

;; Voice convenience API

(defn join-voice
  "Join a voice channel. Returns a Promise."
  [channel-id]
  (when-let [manager @manager*]
    (.joinVoice manager channel-id)))

(defn leave-voice
  "Leave a voice channel for a guild. Returns a Promise."
  [guild-id]
  (when-let [manager @manager*]
    (.leaveVoice manager guild-id)))

(defn play-audio
  "Play an audio buffer in a voice channel. Returns a Promise."
  [guild-id audio-buffer]
  (when-let [manager @manager*]
    (.playAudio manager guild-id audio-buffer)))

(defn start-voice-listener
  "Start listening for voice input. Returns a Promise of a stop function."
  [guild-id on-start on-audio]
  (when-let [manager @manager*]
    (.startVoiceListener manager guild-id on-start on-audio)))

(defn get-voice-connection
  "Get the current voice connection for a guild."
  [guild-id]
  (when-let [manager @manager*]
    (.getVoiceConnection manager guild-id)))

(defn list-voice-members
  "List members in a voice channel. Returns a Promise."
  [guild-id channel-id]
  (when-let [manager @manager*]
    (.listVoiceMembers manager guild-id channel-id)))

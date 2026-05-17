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
            ["prism-media" :as prism]
            ["node:module" :refer [createRequire]]
            ["node:stream" :refer [Readable]]))

;; libsodium-wrappers is needed for @discordjs/voice crypto support.
;; We *must* load it via CommonJS `require` because libsodium-wrappers@0.7.16
;; publishes an ESM export that imports a missing ./libsodium.mjs.
(defonce ^:private libsodium-wrappers-loaded?
  (let [req (createRequire (str (.cwd js/process) "/"))]
    (req "libsodium-wrappers")
    true))

(declare set-manager!)

(def ^:private voice-listener-sample-rate 48000)
(def ^:private voice-listener-channels 2)
(def ^:private voice-listener-bytes-per-sample 2)
(def ^:private voice-listener-min-duration-s 0.8)
(def ^:private voice-listener-silence-debounce-ms 900)
;; Chunk audio into ~25s segments with ~5s overlap so the NPU STT never
;; receives more than it can handle.  When the buffer hits the threshold we
;; flush everything except the overlap (which becomes the head of the next
;; chunk).  This prevents both OOM and mid-word cuts at chunk boundaries.
;;
;; 48000 Hz × 2 channels × 2 bytes/sample × 25 s = 4 800 000 bytes
(def ^:private voice-listener-chunk-threshold-s 25)
;; Overlap must be ≥ the NPU model’s receptive context (≈ 1–2 s) plus a
;; safety margin so words spanning the boundary appear in both chunks.
(def ^:private voice-listener-chunk-overlap-s 5)

(def ^:private voice-listener-chunk-threshold-bytes
  (* voice-listener-sample-rate
     voice-listener-channels
     voice-listener-bytes-per-sample
     voice-listener-chunk-threshold-s))

(def ^:private voice-listener-chunk-overlap-bytes
  (* voice-listener-sample-rate
     voice-listener-channels
     voice-listener-bytes-per-sample
     voice-listener-chunk-overlap-s))

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

(defn- pcm16le->wav-buffer
  "Wrap raw PCM16LE bytes in a WAV container so ffmpeg (and thus STT) can decode it.

   pcm: Node Buffer of signed 16-bit little-endian samples.
   rate: sample rate in Hz (Discord voice is typically 48000)
   channels: 1 or 2 (Discord voice is typically 2)

   Returns a Node Buffer containing a complete .wav file."
  [pcm rate channels]
  (let [rate (max 1 (long (or rate 48000)))
        channels (max 1 (long (or channels 2)))
        data-size (.-length pcm)
        byte-rate (* rate channels 2)
        block-align (* channels 2)
        wav (js/Buffer.alloc (+ 44 data-size))]
    (.write wav "RIFF" 0)
    (.writeUInt32LE wav (+ 36 data-size) 4)
    (.write wav "WAVE" 8)
    (.write wav "fmt " 12)
    (.writeUInt32LE wav 16 16)
    (.writeUInt16LE wav 1 20)
    (.writeUInt16LE wav channels 22)
    (.writeUInt32LE wav rate 24)
    (.writeUInt32LE wav byte-rate 28)
    (.writeUInt16LE wav block-align 32)
    (.writeUInt16LE wav 16 34)
    (.write wav "data" 36)
    (.writeUInt32LE wav data-size 40)
    (.copy pcm wav 44)
    wav))

(defn- member-role-ids
  [member]
  (try
    (let [roles (when member (.-roles member))
          cache (when roles (.-cache roles))]
      (if cache
        (into-array (for [[role-id _role] cache] role-id))
        #js []))
    (catch js/Error _
      #js [])))

(defn- map-message
  "Convert a discord.js Message to a plain JS map."
  [message]
  (let [author (.-author message)
        guild (.-guild message)
        member (.-member message)]
    #js {:id (.-id message)
         :channelId (.-channelId message)
         :guildId (or (when guild (.-id guild)) "")
         :content (or (.-content message) "")
         :authorId (or (when author (.-id author)) "")
         :authorUsername (or (when author (.-username author)) "unknown")
         :authorIsBot (boolean (when author (.-bot author)))
         :authorRoleIds (member-role-ids member)
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

(defn- log-fn
  "Return a logger function (or nil) for a given level keyword.

   We avoid the old (.-info? log) style because js/console doesn't expose
   predicate fields; it only exposes methods like .info/.warn/.error."
  [log level]
  (let [candidate (case level
                    :info  (aget log "info")
                    :warn  (aget log "warn")
                    :error (aget log "error")
                    :debug (aget log "debug")
                    nil)]
    (when (fn? candidate)
      (fn [& args]
        (try
          (.apply candidate log (to-array args))
          (catch js/Error _ nil))))))

(defn- notify-message!
  [listeners log message]
  (let [mapped (map-message message)
        log-error (log-fn log :error)]
    (.forEach @listeners
              (fn [listener]
                (try
                  (listener mapped message)
                  (catch js/Error error
                    (when log-error
                      (log-error "[discord-gateway] listener failed" error))))))))

(defn- notify-reaction!
  [reaction-listeners log reaction user]
  (let [message (.-message reaction)
        emoji (.-emoji reaction)
        mapped #js {:emoji (or (.-name emoji) "")
                    :message (when message (map-message message))
                    :messageId (or (when message (.-id message)) "")
                    :channelId (or (when message (.-channelId message)) "")
                    :userId (or (when user (.-id user)) "")
                    :userUsername (or (when user (.-username user)) "unknown")}
        log-error (log-fn log :error)]
    (.forEach @reaction-listeners
              (fn [listener]
                (try
                  (listener mapped reaction user)
                  (catch js/Error error
                    (when log-error
                      (log-error "[discord-gateway] reaction listener failed" error))))))))

(defn- notify-voice-state!
  [voice-state-listeners log old-state new-state]
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
                    :newChannelId new-channel-id}
        log-error (log-fn log :error)]
    (when action
      (.forEach @voice-state-listeners
                (fn [listener]
                  (try
                    (listener mapped old-state new-state)
                    (catch js/Error error
                      (when log-error
                        (log-error "[discord-gateway] voice state listener failed" error)))))))))

(defn- handle-client-ready
  [log-info ready-client]
  (when log-info
    (log-info (str "[discord-gateway] ready as "
                   (or (when (.-user ready-client) (.-tag (.-user ready-client))) "unknown")
                   " in " (.. ready-client -guilds -cache -size) " guilds"))))

(defn- handle-message-create
  [notify-message message]
  (notify-message message))

(defn- handle-reaction-add
  [log-warn notify-reaction reaction user]
  (-> (if (.-partial reaction) (.fetch reaction) (js/Promise.resolve reaction))
      (.then (fn [full-reaction]
               (let [message (.-message full-reaction)]
                 (-> (if (and message (.-partial message)) (.fetch message) (js/Promise.resolve message))
                     (.then (fn [_] (notify-reaction full-reaction user)))))))
      (.catch (fn [error]
                (when log-warn
                  (log-warn "[discord-gateway] reaction ingest failed" error))))))

(defn- handle-client-error
  [log-error error]
  (when log-error
    (log-error "[discord-gateway] client error" error)))

(defn- handle-voice-state-update
  [notify-voice-state old-state new-state]
  (notify-voice-state old-state new-state))

(defn- build-discord-client
  "Create a new discord.js Client and attach event listeners."
  [log notify-message notify-reaction notify-voice-state]
  (let [Client (Client-class)
        GatewayIntentBits (intent-bits)
        Partials (partials-enum)
        Events (events-enum)
        log-info (log-fn log :info)
        log-warn (log-fn log :warn)
        log-error (log-fn log :error)
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
    (.on next-client (.-ClientReady Events) (partial handle-client-ready log-info))
    (.on next-client (.-MessageCreate Events) (partial handle-message-create notify-message))
    (.on next-client (.-MessageReactionAdd Events) (partial handle-reaction-add log-warn notify-reaction))
    (.on next-client (.-Error Events) (partial handle-client-error log-error))
    (.on next-client (.-VoiceStateUpdate Events) (partial handle-voice-state-update notify-voice-state))
    next-client))

(defn- ensure-client!
  [client-state ready-promise]
  (if-not @client-state
    (js/Promise.reject (js/Error. "Discord gateway client is not started"))
    (if @ready-promise
      (.then @ready-promise (fn [_] @client-state))
      (js/Promise.resolve @client-state))))

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
                                           (js/console.log "[voice:gw] voice connection ready for guild:" (or (.-__guildId conn)
                                                                                                                    (some-> conn (.-joinConfig) (.-guildId))
                                                                                                                    (.-guildId conn)))
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
   Calls (on-audio user-id wav-buffer) when a user stops speaking with accumulated audio.

   NOTE: We decode Opus -> PCM -> WAV here so downstream STT can simply POST bytes
   to /transcribe (the STT service uses ffmpeg).

   Returns a stop function."
  [connections guild-id on-start on-audio]
  (js/console.log "[voice:listener] starting for guild:" guild-id "connections:" (.-size connections))
  (let [conn (.get connections guild-id)]
    (if-not conn
      (do
        (js/console.error "[voice:listener] no connection for guild:" guild-id)
        (js/Promise.reject (js/Error. (str "No voice connection for guild: " guild-id))))
      (let [receiver (.-receiver conn)
            speaking-map (.-speaking receiver)
            pcm-buffers (atom {})
            streams (atom {})
            decoders (atom {})
            active-users (atom #{})
            silence-timers (atom {})

            create-decoder
            (fn []
              (let [OpusDecoder (some-> prism (aget "opus") (aget "Decoder"))]
                (when-not (fn? OpusDecoder)
                  (throw (js/Error. "prism-media Opus decoder unavailable")))
                (new OpusDecoder #js {:rate 48000 :channels 2 :frameSize 960})))

            ;; Debounce duration in ms — if a user pauses briefly we don’t
            ;; immediately flush; we wait to see if they resume. This needs to
            ;; be long enough for natural speech pauses; otherwise Whisper sees
            ;; tiny fragments and loses context.
            silence-debounce-ms voice-listener-silence-debounce-ms

            flush-audio!
            (fn [uid]
              (when-let [buf (get @pcm-buffers uid)]
                (let [chunks (js/Array.from buf)
                      pcm (js/Buffer.concat chunks)
                      duration-s (/ (.-length pcm)
                                    voice-listener-sample-rate
                                    voice-listener-bytes-per-sample
                                    voice-listener-channels)
                      wav (pcm16le->wav-buffer pcm voice-listener-sample-rate voice-listener-channels)]
                  (swap! pcm-buffers dissoc uid)
                  (swap! silence-timers dissoc uid)
                  (if (< duration-s voice-listener-min-duration-s)
                    (js/console.log "[voice:listener] skipping very short audio for" uid "duration:" duration-s "s")
                    (do
                      (js/console.log "[voice:listener] calling on-audio for" uid "wav bytes:" (.-length wav) "duration:" duration-s "s")
                      (on-audio uid wav))))))

            ;; Eagerly chunk long utterances so the NPU STT never receives > 25 s.
            ;; We keep the last 5 s as overlap so words at the boundary are not cut.
            chunk-and-flush!
            (fn [uid]
              (when-let [buf (get @pcm-buffers uid)]
                (let [total-pcm (js/Buffer.concat (js/Array.from buf))
                      total-len (.-length total-pcm)]
                  (when (> total-len voice-listener-chunk-overlap-bytes)
                    (let [flush-len (- total-len voice-listener-chunk-overlap-bytes)
                          flush-pcm (.slice total-pcm 0 flush-len)
                          keep-pcm (.slice total-pcm flush-len)
                          duration-s (/ flush-len
                                        voice-listener-sample-rate
                                        voice-listener-bytes-per-sample
                                        voice-listener-channels)
                          overlap-s (/ voice-listener-chunk-overlap-bytes
                                       voice-listener-sample-rate
                                       voice-listener-bytes-per-sample
                                       voice-listener-channels)
                          wav (pcm16le->wav-buffer flush-pcm
                                                   voice-listener-sample-rate
                                                   voice-listener-channels)]
                      ;; Retain overlap as the head of the next chunk.
                      (swap! pcm-buffers assoc uid #js [keep-pcm])
                      (js/console.log "[voice:listener] chunk-flush for" uid
                                      "flushed:" duration-s "s"
                                      "overlap-kept:" overlap-s "s")
                      (on-audio uid wav))))))

            on-start-speaking
            (fn [user-id]
              (let [uid (str user-id)]
                ;; Cancel any pending silence debounce for this user
                (when-let [t (get @silence-timers uid)]
                  (js/clearTimeout t)
                  (swap! silence-timers dissoc uid))

                (when-not (contains? @active-users uid)
                  (js/console.log "[voice:listener] >>> SPEAKING START:" uid)
                  (swap! active-users conj uid)
                  (when on-start (on-start uid))
                  ;; If the user resumed within the silence debounce window,
                  ;; keep the previous PCM chunks so one sentence with short
                  ;; pauses becomes one Whisper request instead of many lossy
                  ;; fragments.
                  (when-not (get @pcm-buffers uid)
                    (swap! pcm-buffers assoc uid #js []))

                  (let [audio-stream (.subscribe receiver uid)
                        decoder (create-decoder)]
                    ;; audio-stream emits Opus packets; decoder emits PCM16LE.
                    (.pipe audio-stream decoder)
                     (.on decoder "data"
                          (fn [pcm-chunk]
                            (when-let [buf (get @pcm-buffers uid)]
                              (.push buf pcm-chunk)
                              ;; Eagerly chunk when we approach the NPU limit
                              ;; so we never drop live audio.
                              (let [current-size (reduce (fn [acc b] (+ acc (.-length b))) 0 buf)]
                                (when (> current-size voice-listener-chunk-threshold-bytes)
                                  (chunk-and-flush! uid))))))
                    (.on decoder "error"
                         (fn [err]
                           (js/console.error "[voice:listener] decoder error for" uid ":" (.-message err))))
                    (.on audio-stream "error"
                         (fn [err]
                           (js/console.error "[voice:listener] audio stream error for" uid ":" (.-message err))))
                    (.on audio-stream "end"
                         (fn []
                           (js/console.log "[voice:listener] audio stream ended for" uid)))

                    (swap! streams assoc uid audio-stream)
                    (swap! decoders assoc uid decoder)))))

            on-end-speaking
            (fn [user-id]
              (let [uid (str user-id)]
                (js/console.log "[voice:listener] >>> SPEAKING END:" uid)
                (swap! active-users disj uid)

                (when-let [audio-stream (get @streams uid)]
                  (try (.destroy audio-stream) (catch js/Error _))
                  (swap! streams dissoc uid))

                (when-let [decoder (get @decoders uid)]
                  (try (.destroy decoder) (catch js/Error _))
                  (swap! decoders dissoc uid))

                ;; Debounce: wait a bit before flushing in case the user resumes
                (let [t (js/setTimeout #(flush-audio! uid) silence-debounce-ms)]
                  (swap! silence-timers assoc uid t))))
            ]

        (js/console.log "[voice:listener] attaching listeners")
        (.on speaking-map "start" on-start-speaking)
        (.on speaking-map "end" on-end-speaking)

        (js/Promise.resolve
         (fn []
           (js/console.log "[voice:listener] stopping for guild:" guild-id)
            (.removeListener speaking-map "start" on-start-speaking)
            (.removeListener speaking-map "end" on-end-speaking)

           (doseq [[_ s] @streams]
             (try (.destroy s) (catch js/Error _)))
           (doseq [[_ d] @decoders]
             (try (.destroy d) (catch js/Error _)))
           (doseq [[uid t] @silence-timers]
             (js/clearTimeout t)
             ;; Flush any pending audio immediately on stop
             (flush-audio! uid))

           (reset! pcm-buffers {})
           (reset! streams {})
           (reset! decoders {})
           (reset! active-users #{})
           (reset! silence-timers {})))))))

;; ---------------------------------------------------------------------------
;; Factory
;; ---------------------------------------------------------------------------

(defn createDiscordGatewayManager
  "Create a Discord gateway manager. Returns a JS object with async methods.

   Options (CLJS map or JS object):
     - :log / \"log\": logger object (default: console)
     - :set-default? / \"setDefault\": when false, do not replace the legacy
       singleton manager.

   Methods: start, stop, restart, onMessage, status, listServers,
            listChannels, fetchChannelMessages, fetchDmMessages,
            searchMessages, sendMessage"
  [opts]
  (let [log (or (when (map? opts) (:log opts))
                (when (object? opts) (aget opts "log"))
                js/console)
        set-default? (not= false (or (when (map? opts) (:set-default? opts))
                                     (when (object? opts) (aget opts "setDefault"))
                                     true))
        client-state (atom nil)
        ready-promise (atom nil)
        current-token (atom nil)
        listeners (atom (js/Set.))
        reaction-listeners (atom (js/Set.))
        voice-state-listeners (atom (js/Set.))]

    (let [notify-message (partial notify-message! listeners log)
          notify-reaction (partial notify-reaction! reaction-listeners log)
          notify-voice-state (partial notify-voice-state! voice-state-listeners log)
          build-client (partial build-discord-client log notify-message notify-reaction notify-voice-state)
          ensure-client (partial ensure-client! client-state ready-promise)]

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

          (when set-default?
            (set-manager! @this-obj))
          @this-obj)))))

;; ---------------------------------------------------------------------------
;; Convenience CLJS API
;; ---------------------------------------------------------------------------

(defonce ^:private manager* (atom nil))
(defonce ^:private actor-managers* (atom {}))

(defn set-manager!
  "Store the gateway manager instance for CLJS API access."
  [m]
  (reset! manager* m))

(defn gateway-manager
  "Returns the legacy/default gateway manager instance, or an actor-owned manager."
  ([] @manager*)
  ([actor-id]
   (if-let [id (some-> actor-id str str/trim not-empty)]
     (get @actor-managers* id)
     @manager*)))

(defn gateway-managers
  "Returns a map of actor-id to actor-owned Discord gateway managers."
  []
  @actor-managers*)

(defn- credential-value
  [credential k]
  (or (when (map? credential) (get credential k))
      (when (map? credential) (get credential (keyword k)))
      (when (map? credential) (get credential (name k)))
      (when (object? credential) (aget credential (name k)))))

(defn- credential-secret-value
  [credential & ks]
  (let [secrets (credential-value credential :secretJson)]
    (some (fn [k]
            (some-> (or (credential-value secrets k)
                        (credential-value secrets (keyword k))
                        (credential-value secrets (name k)))
                    str
                    str/trim
                    not-empty))
          ks)))

(defn- credential-actor-id
  [credential]
  (some-> (or (credential-value credential :actorId)
              (credential-value credential :actor-id)
              (credential-value credential :actor_id))
          str
          str/trim
          not-empty))

(defn- credential-bot-token
  [credential]
  (credential-secret-value credential :botToken :bot-token :token))

(defn ensure-actor-manager!
  [actor-id]
  (let [actor-id (some-> actor-id str str/trim not-empty)]
    (when-not actor-id
      (throw (js/Error. "actor id is required for Discord actor gateway")))
    (or (get @actor-managers* actor-id)
        (let [manager (createDiscordGatewayManager #js {:log js/console :setDefault false})]
          (swap! actor-managers* assoc actor-id manager)
          manager))))

(defn start-actor-gateway!
  [actor-id token]
  (let [manager (ensure-actor-manager! actor-id)]
    (-> (.start manager token)
        (.then (fn [_]
                 {:actorId actor-id
                  :status (js->clj (.status manager) :keywordize-keys true)})))))

(defn start-actor-gateways!
  [credentials]
  (let [rows (js->clj (or credentials #js []) :keywordize-keys true)
        valid (->> rows
                   (keep (fn [credential]
                           (let [actor-id (credential-actor-id credential)
                                 token (credential-bot-token credential)]
                             (when (and actor-id token)
                               {:actorId actor-id :token token}))))
                   vec)
        active-actor-ids (set (map :actorId valid))]
     (doseq [[actor-id manager] @actor-managers*]
       (when-not (contains? active-actor-ids actor-id)
         (try
           (.stop manager)
           (catch js/Error _)
           (finally
             (swap! actor-managers* dissoc actor-id)))))
    (-> (js/Promise.all
         (clj->js
          (mapv (fn [{:keys [actorId token]}]
                  (-> (start-actor-gateway! actorId token)
                      (.catch (fn [err]
                                (.warn js/console "[discord-gateway] actor gateway start failed" actorId (.-message err))
                                {:actorId actorId :error (.-message err)}))))
                valid)))
        (.then (fn [results]
                 (js->clj results :keywordize-keys true))))))

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

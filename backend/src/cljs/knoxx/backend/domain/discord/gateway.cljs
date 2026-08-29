(ns knoxx.backend.domain.discord.gateway
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
(defonce libsodium-wrappers-loaded?
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

(defn- ^:async fetch-partial!
  [x]
  (if (.-partial x)
    (await (.fetch x))
    x))

(defn- ^:async fetch-reaction-message!
  [reaction]
  (let [message (.-message reaction)]
    (when (and message (.-partial message))
      (await (.fetch message)))
    message))

(defn- ^:async handle-reaction-add
  [log-warn notify-reaction reaction user]
  (try
    (let [full-reaction (await (fetch-partial! reaction))]
      (await (fetch-reaction-message! full-reaction))
      (notify-reaction full-reaction user))
    (catch js/Error error
      (when log-warn
        (log-warn "[discord-gateway] reaction ingest failed" error)))))

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

(defn- ^:async ensure-client!
  [client-state ready-promise]
  (when-not @client-state
    (throw (js/Error. "Discord gateway client is not started")))
  (when @ready-promise
    (await @ready-promise))
  @client-state)

(defn- reset-client-state!
  [client-state ready-promise current-token]
  (reset! client-state nil)
  (reset! ready-promise nil)
  (reset! current-token nil))

(defn- log-login-failed!
  [log error]
  (when-let [log-error (log-fn log :error)]
    (log-error "[discord-gateway] login failed" error)))

(defn- ^:async login-client!
  [client-state ready-promise current-token log new-client token]
  (try
    (await (.login new-client token))
    new-client
    (catch js/Error error
      (log-login-failed! log error)
      (try (.destroy new-client) (catch js/Error _))
      (reset-client-state! client-state ready-promise current-token)
      (throw error))))

(defn- ^:async gw-start
  "Start the gateway client with a bot token."
  [client-state ready-promise current-token _listeners log this-stop build-client token]
  (let [next-token (.trim (str (or token "")))]
    (cond
      (= next-token "") (do (await (this-stop)) nil)
      (and @client-state (= @current-token next-token)) (if @ready-promise
                                                           (await @ready-promise)
                                                           @client-state)
      :else (do
              (await (this-stop))
              (reset! current-token next-token)
              (let [new-client (build-client)
                    login-promise (login-client! client-state ready-promise current-token log new-client next-token)]
                (reset! client-state new-client)
                (reset! ready-promise login-promise)
                (await login-promise))))))

(defn- ^:async gw-stop
  "Stop the gateway client."
  [client-state ready-promise current-token]
  (when-let [client @client-state]
    (try
      (await (.destroy client))
      (catch js/Error _ nil)))
  (reset-client-state! client-state ready-promise current-token)
  nil)

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
(defn- guild->server
  [guild]
  #js {:id (.-id guild)
       :name (.-name guild)
       :memberCount (or (.-memberCount guild) nil)})

(defn- ^:async gw-list-servers
  "List all guilds the bot is in."
  [ensure-client]
  (let [active-client (await (ensure-client))]
    (into-array
     (for [[_id guild] (.. active-client -guilds -cache)]
       (guild->server guild)))))

(defn- guild-channel-entry
  [guild channel]
  #js {:id (.-id channel)
       :name (or (.-name channel) "")
       :guildId (.-id guild)
       :type (str (.-type channel))})

(defn- listing-channel?
  [ChannelType channel]
  (and channel
       (readable-text-channel? channel)
       (not= (.-type channel) (.-DM ChannelType))))

(defn- ^:async collect-guild-channels!
  [ChannelType guild]
  (let [fetched (await (.fetch (.. guild -channels)))]
    (into-array
     (for [[_id channel] fetched
           :when (listing-channel? ChannelType channel)]
       (guild-channel-entry guild channel)))))

(defn- concat-js-arrays
  [arrays]
  (let [flat (atom #js [])]
    (doseq [arr arrays]
      (swap! flat (fn [acc] (.concat acc arr))))
    @flat))

(defn- ^:async safe-collect-guild-channels!
  [log ChannelType guild]
  (try
    (await (collect-guild-channels! ChannelType guild))
    (catch js/Error err
      (when-let [log-warn (log-fn log :warn)]
        (log-warn "[discord-gateway] listChannels guild failed" (.-id guild) err))
      #js [])))

(defn- ^:async list-all-guild-channels!
  [active-client log ChannelType]
  (let [promises (clj->js
                  (mapv (fn [[_id guild]]
                          (safe-collect-guild-channels! log ChannelType guild))
                        (.. active-client -guilds -cache)))
        results (await (js/Promise.all promises))]
    (concat-js-arrays results)))

(defn- ^:async gw-list-channels
  "List channels in a guild or all guilds."
  [ensure-client log guild-id]
  (let [active-client (await (ensure-client))
        ChannelType (channel-type-enum)]
    (if guild-id
      (let [guild (.. active-client -guilds -cache (get guild-id))]
        (when-not guild
          (throw (js/Error. (str "Guild not found: " guild-id))))
        (await (collect-guild-channels! ChannelType guild)))
      (await (list-all-guild-channels! active-client log ChannelType)))))

(defn- js-opt
  [opts k]
  (when opts
    (aget opts k)))

(defn- bounded-limit
  [opts default-limit]
  (max 1 (min 100 (or (js-opt opts "limit") default-limit))))

(defn- message-fetch-options
  [opts]
  (clj->js {:limit (bounded-limit opts 50)
            :before (js-opt opts "before")
            :after (js-opt opts "after")
            :around (js-opt opts "around")}))

(defn- messages-array
  [fetched]
  (sort-newest-first
   (map map-message (for [[_id msg] fetched] msg))))

(defn- ^:async fetch-readable-channel!
  [active-client channel-id]
  (let [channel (await (.fetch (.. active-client -channels) channel-id))]
    (when (or (not channel) (not (readable-text-channel? channel)))
      (throw (js/Error. (str "Channel not found or not text-based: " channel-id))))
    channel))

(defn- ^:async gw-fetch-channel-messages
  "Fetch messages from a channel."
  [ensure-client channel-id opts]
  (let [active-client (await (ensure-client))
        channel (await (fetch-readable-channel! active-client channel-id))
        fetched (await (.fetch (.. channel -messages) (message-fetch-options opts)))]
    (messages-array fetched)))

(defn- dm-message-options
  [opts]
  (clj->js {:limit (bounded-limit opts 50)
            :before (js-opt opts "before")}))

(defn- dm-messages-response
  [dm fetched]
  #js {:dmChannelId (.-id dm)
       :messages (messages-array fetched)})

(defn- ^:async gw-fetch-dm-messages
  "Fetch DM messages with a user."
  [ensure-client user-id opts]
  (let [active-client (await (ensure-client))
        user (await (.fetch (.. active-client -users) user-id))
        dm (await (.createDM user))
        fetched (await (.fetch (.. dm -messages) (dm-message-options opts)))]
    (dm-messages-response dm fetched)))

(defn- search-filter-fn
  "Create a filter function for message search."
  [opts]
  (let [needle (.toLowerCase (str (or (js-opt opts "query") "")))
        target-user-id (js-opt opts "userId")]
    (fn [message]
      (let [content-ok (or (= needle "")
                           (.includes (.toLowerCase (or (aget message "content") "")) needle))
            author-ok (or (not target-user-id)
                          (= (aget message "authorId") target-user-id))]
        (and content-ok author-ok)))))

(defn- search-limit
  [opts]
  (or (js-opt opts "limit") 50))

(defn- search-result
  [source key-name key-value messages opts]
  (let [filtered (.filter messages (search-filter-fn opts))
        limit (search-limit opts)
        result #js {:messages (.slice filtered 0 limit)
                    :count (min (.-length filtered) limit)
                    :source source}]
    (aset result key-name key-value)
    result))

(defn- ^:async gw-search-messages
  "Search messages in a channel or DM."
  [this-fn scope opts]
  (let [normalized-scope (.toLowerCase (str (or scope "channel")))]
    (if (= normalized-scope "dm")
      (let [result (await (.fetchDmMessages this-fn (js-opt opts "userId")
                                            (clj->js {:limit 100 :before (js-opt opts "before")})))]
        (search-result "gateway-cache" "dmChannelId" (aget result "dmChannelId") (aget result "messages") opts))
      (let [messages (await (.fetchChannelMessages this-fn (js-opt opts "channelId")
                                                   (clj->js {:limit 100
                                                             :before (js-opt opts "before")
                                                             :after (js-opt opts "after")})))]
        (search-result "gateway-cache" "channelId" (js-opt opts "channelId") messages opts)))))

(defn- attachment-count
  [attachments]
  (cond
    (nil? attachments) 0
    (array? attachments) (.-length attachments)
    :else (count attachments)))

(defn- message-body-text
  [text attachments]
  (let [base-text (str (or text ""))]
    (if (and (str/blank? base-text) (seq attachments))
      "[attachment]"
      base-text)))

(defn- send-message-payload
  [chunk index reply-to attachments]
  (let [payload (clj->js {:content chunk})]
    (when (and (zero? index) reply-to)
      (aset payload "reply" (clj->js {:messageReference reply-to})))
    (when (and (zero? index) (seq attachments))
      (aset payload "files" (into-array (map discord-file-payload attachments))))
    payload))

(defn- ^:async send-message-chunks!
  [channel chunks reply-to attachments]
  (doseq [[index chunk] (map-indexed vector (array-seq chunks))]
    (await (.send channel (send-message-payload chunk index reply-to attachments)))))

(defn- send-message-result
  [channel-id chunks attachments]
  #js {:channelId channel-id
       :messageId ""
       :sent true
       :timestamp (.toISOString (js/Date.))
       :chunkCount (.-length chunks)
       :attachmentCount (attachment-count attachments)})

(defn- ^:async gw-send-message
  "Send a message to a channel, splitting into chunks if needed."
  [ensure-client channel-id text reply-to attachments]
  (let [active-client (await (ensure-client))
        channel (await (fetch-readable-channel! active-client channel-id))
        chunks (split-message (message-body-text text attachments))]
    (await (send-message-chunks! channel chunks reply-to attachments))
    (send-message-result channel-id chunks attachments)))

;; ---------------------------------------------------------------------------
;; Voice helpers
;; ---------------------------------------------------------------------------

(defn- voice-connection-guild-id
  [conn]
  (or (.-__guildId conn)
      (some-> conn (.-joinConfig) (.-guildId))
      (.-guildId conn)))

(defn- join-voice-connection!
  [channel-id channel guild-id]
  (let [conn (voice/joinVoiceChannel
              #js {:channelId channel-id
                   :guildId guild-id
                   :adapterCreator (.-voiceAdapterCreator (.-guild channel))
                   :selfDeaf false
                   :selfMute false})]
    (aset conn "__guildId" guild-id)
    conn))

(defn- ^:async wait-for-voice-ready!
  [conn]
  (try
    (await (voice/entersState conn (.-Ready voice/VoiceConnectionStatus) 15000))
    (js/console.log "[voice:gw] voice connection ready for guild:" (voice-connection-guild-id conn))
    conn
    (catch js/Error err
      (js/console.error "[voice:gw] voice connection failed to ready:" (.-message err))
      (throw err))))

(defn- ^:async gw-join-voice
  "Join a voice channel. Returns a VoiceConnection."
  [ensure-client channel-id]
  (js/console.log "[voice:gw] joining channel:" channel-id)
  (let [active-client (await (ensure-client))
        channel (await (.fetch (.. active-client -channels) channel-id))]
    (when-not channel
      (js/console.error "[voice:gw] channel not found:" channel-id)
      (throw (js/Error. (str "Channel not found: " channel-id))))
    (let [guild-id (.-guildId channel)
          conn (join-voice-connection! channel-id channel guild-id)]
      (js/console.log "[voice:gw] channel found:" channel-id "guild:" guild-id "selfDeaf:false")
      (js/console.log "[voice:gw] joinVoiceChannel returned, waiting for ready state…")
      (await (wait-for-voice-ready! conn)))))

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

(defn- voice-member-entry
  [member]
  (let [user (.-user member)]
    #js {:userId (.-id user)
         :username (.-username user)
         :displayName (or (.-displayName member) (.-username user))
         :isBot (boolean (.-bot user))
         :isMuted (boolean (.-mute member))
         :isDeaf (boolean (.-deaf member))
         :isSpeaking false}))

(defn- ^:async fetch-guild!
  [active-client guild-id]
  (let [guild (await (.fetch (.. active-client -guilds) guild-id))]
    (when-not guild
      (throw (js/Error. (str "Guild not found: " guild-id))))
    guild))

(defn- ^:async fetch-guild-channel!
  [guild channel-id]
  (let [channel (await (.fetch (.. guild -channels) channel-id))]
    (when-not channel
      (throw (js/Error. (str "Channel not found: " channel-id))))
    channel))

(defn- ^:async gw-list-voice-members
  "List members in a voice channel."
  [ensure-client guild-id channel-id]
  (let [active-client (await (ensure-client))
        guild (await (fetch-guild! active-client guild-id))
        channel (await (fetch-guild-channel! guild channel-id))]
    (into-array (for [[_ member] (.-members channel)]
                  (voice-member-entry member)))))

(defn- voice-listener-create-decoder
  []
  (let [OpusDecoder (some-> prism (aget "opus") (aget "Decoder"))]
    (when-not (fn? OpusDecoder)
      (throw (js/Error. "prism-media Opus decoder unavailable")))
    (new OpusDecoder #js {:rate 48000 :channels 2 :frameSize 960})))

(defn- voice-listener-flush-audio!
  [pcm-buffers silence-timers on-audio uid]
  (when-let [buf (get @pcm-buffers uid)]
    (let [pcm (js/Buffer.concat (js/Array.from buf))
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

(defn- voice-listener-chunk-and-flush!
  [pcm-buffers on-audio uid]
  (when-let [buf (get @pcm-buffers uid)]
    (let [total-pcm (js/Buffer.concat (js/Array.from buf))
          total-len (.-length total-pcm)]
      (when (> total-len voice-listener-chunk-overlap-bytes)
        (let [flush-len (- total-len voice-listener-chunk-overlap-bytes)
              flush-pcm (.slice total-pcm 0 flush-len)
              keep-pcm (.slice total-pcm flush-len)
              duration-s (/ flush-len voice-listener-sample-rate voice-listener-bytes-per-sample voice-listener-channels)
              overlap-s (/ voice-listener-chunk-overlap-bytes voice-listener-sample-rate voice-listener-bytes-per-sample voice-listener-channels)
              wav (pcm16le->wav-buffer flush-pcm voice-listener-sample-rate voice-listener-channels)]
          (swap! pcm-buffers assoc uid #js [keep-pcm])
          (js/console.log "[voice:listener] chunk-flush for" uid
                          "flushed:" duration-s "s"
                          "overlap-kept:" overlap-s "s")
          (on-audio uid wav))))))

(defn- voice-listener-destroy-user-streams!
  [streams decoders uid]
  (when-let [audio-stream (get @streams uid)]
    (try (.destroy audio-stream) (catch js/Error _))
    (swap! streams dissoc uid))
  (when-let [decoder (get @decoders uid)]
    (try (.destroy decoder) (catch js/Error _))
    (swap! decoders dissoc uid)))

(defn- voice-listener-on-start-speaking
  [receiver pcm-buffers streams decoders active-users silence-timers on-start on-audio]
  (fn [user-id]
    (let [uid (str user-id)]
      (when-let [t (get @silence-timers uid)]
        (js/clearTimeout t)
        (swap! silence-timers dissoc uid))
      (when-not (contains? @active-users uid)
        (js/console.log "[voice:listener] >>> SPEAKING START:" uid)
        (swap! active-users conj uid)
        (when on-start (on-start uid))
        (when-not (get @pcm-buffers uid)
          (swap! pcm-buffers assoc uid #js []))
        (let [audio-stream (.subscribe receiver uid)
              decoder (voice-listener-create-decoder)]
          (.pipe audio-stream decoder)
          (.on decoder "data"
               (fn [pcm-chunk]
                 (when-let [buf (get @pcm-buffers uid)]
                   (.push buf pcm-chunk)
                   (let [current-size (reduce (fn [acc b] (+ acc (.-length b))) 0 buf)]
                     (when (> current-size voice-listener-chunk-threshold-bytes)
                       (voice-listener-chunk-and-flush! pcm-buffers on-audio uid))))))
          (.on decoder "error" #(js/console.error "[voice:listener] decoder error for" uid ":" (.-message %)))
          (.on audio-stream "error" #(js/console.error "[voice:listener] audio stream error for" uid ":" (.-message %)))
          (.on audio-stream "end" #(js/console.log "[voice:listener] audio stream ended for" uid))
          (swap! streams assoc uid audio-stream)
          (swap! decoders assoc uid decoder))))))

(defn- voice-listener-on-end-speaking
  [streams decoders active-users silence-timers flush-audio!]
  (fn [user-id]
    (let [uid (str user-id)]
      (js/console.log "[voice:listener] >>> SPEAKING END:" uid)
      (swap! active-users disj uid)
      (voice-listener-destroy-user-streams! streams decoders uid)
      (let [t (js/setTimeout #(flush-audio! uid) voice-listener-silence-debounce-ms)]
        (swap! silence-timers assoc uid t)))))

(defn- voice-listener-stop!
  [guild-id speaking-map on-start-speaking on-end-speaking pcm-buffers streams decoders active-users silence-timers flush-audio!]
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
      (flush-audio! uid))
    (reset! pcm-buffers {})
    (reset! streams {})
    (reset! decoders {})
    (reset! active-users #{})
    (reset! silence-timers {})))

(defn- gw-start-voice-listener
  "Start voice capture and return a Promise of a stop function."
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
            flush-audio! #(voice-listener-flush-audio! pcm-buffers silence-timers on-audio %)
            on-start-speaking (voice-listener-on-start-speaking receiver pcm-buffers streams decoders active-users silence-timers on-start on-audio)
            on-end-speaking (voice-listener-on-end-speaking streams decoders active-users silence-timers flush-audio!)]
        (js/console.log "[voice:listener] attaching listeners")
        (.on speaking-map "start" on-start-speaking)
        (.on speaking-map "end" on-end-speaking)
        (js/Promise.resolve
         (voice-listener-stop! guild-id speaking-map on-start-speaking on-end-speaking
                               pcm-buffers streams decoders active-users silence-timers flush-audio!))))))


;; ---------------------------------------------------------------------------
;; Factory
;; ---------------------------------------------------------------------------

(defn- destroy-voice-connections!
  [voice-connections]
  (.forEach voice-connections (fn [conn _key] (try (.destroy conn) (catch js/Error _))))
  (.clear voice-connections))

(defn- gateway-stop!
  [voice-connections this-stop]
  (destroy-voice-connections! voice-connections)
  (this-stop))

(defn- ^:async gateway-restart!
  [this-stop this-fn token]
  (await (this-stop))
  (await (.start (this-fn) token)))

(defn- register-gateway-listener!
  [listener-set listener]
  (.add @listener-set listener)
  (fn [] (.delete @listener-set listener)))

(defn- ^:async gateway-join-voice!
  [ensure-client voice-connections channel-id]
  (let [conn (await (gw-join-voice ensure-client channel-id))
        guild-id (voice-connection-guild-id conn)]
    (.set voice-connections guild-id conn)
    #js {:guildId guild-id :channelId channel-id :joined true}))

(defn- gateway-active-voice-connection
  [voice-connections guild-id]
  (if guild-id
    (.get voice-connections guild-id)
    (when (> (.-size voice-connections) 0)
      (let [entries (.entries voice-connections)]
        (.-value (.next entries))))))

(defn- gateway-lifecycle-methods
  [client-state ready-promise current-token listeners log this-stop build-client this-fn voice-connections]
  #js {:start (fn [token] (gw-start client-state ready-promise current-token listeners log this-stop build-client token))
       :stop (fn [] (gateway-stop! voice-connections this-stop))
       :restart (fn [token] (gateway-restart! this-stop this-fn token))
       :status (fn [] (gw-status client-state))})

(defn- gateway-listener-methods
  [listeners reaction-listeners voice-state-listeners]
  #js {:onMessage (fn [listener] (register-gateway-listener! listeners listener))
       :onReaction (fn [listener] (register-gateway-listener! reaction-listeners listener))
       :onVoiceStateUpdate (fn [listener] (register-gateway-listener! voice-state-listeners listener))})

(defn- gateway-message-methods
  [ensure-client log this-fn]
  #js {:listServers (fn [] (gw-list-servers ensure-client))
       :listChannels (fn [guild-id] (gw-list-channels ensure-client log guild-id))
       :fetchChannelMessages (fn [channel-id opts] (gw-fetch-channel-messages ensure-client channel-id opts))
       :fetchDmMessages (fn [user-id opts] (gw-fetch-dm-messages ensure-client user-id opts))
       :searchMessages (fn [scope opts] (gw-search-messages (this-fn) scope opts))
       :sendMessage (fn [channel-id text reply-to attachments] (gw-send-message ensure-client channel-id text reply-to attachments))})

(defn- gateway-voice-methods
  [ensure-client voice-connections]
  #js {:joinVoice (fn [channel-id] (gateway-join-voice! ensure-client voice-connections channel-id))
       :leaveVoice (fn [guild-id] (gw-leave-voice voice-connections guild-id) #js {:guildId guild-id :left true})
       :playAudio (fn [guild-id audio-buffer] (gw-play-audio voice-connections guild-id audio-buffer))
       :subscribeVoice (fn [guild-id user-id callback] (gw-subscribe-voice voice-connections guild-id user-id callback))
       :startVoiceListener (fn [guild-id on-start on-audio] (gw-start-voice-listener voice-connections guild-id on-start on-audio))
       :getVoiceConnection (fn [guild-id] (gateway-active-voice-connection voice-connections guild-id))
       :listVoiceMembers (fn [guild-id channel-id] (gw-list-voice-members ensure-client guild-id channel-id))})

(defn- build-gateway-manager-methods
  [client-state ready-promise current-token listeners reaction-listeners voice-state-listeners
   log this-stop build-client ensure-client voice-connections this-fn]
  (js/Object.assign
   #js {}
   (gateway-lifecycle-methods client-state ready-promise current-token listeners log this-stop build-client this-fn voice-connections)
   (gateway-listener-methods listeners reaction-listeners voice-state-listeners)
   (gateway-message-methods ensure-client log this-fn)
   (gateway-voice-methods ensure-client voice-connections)))

(defn- parse-gateway-manager-opts
  "Parse gateway manager options from a CLJS map or JS object."
  [opts]
  {:log (or (when (map? opts) (:log opts))
            (when (object? opts) (aget opts "log"))
            js/console)
   :set-default? (not= false (or (when (map? opts) (:set-default? opts))
                                 (when (object? opts) (aget opts "setDefault"))
                                 true))})

(defn- gateway-manager-state
  []
  {:client-state (atom nil)
   :ready-promise (atom nil)
   :current-token (atom nil)
   :listeners (atom (js/Set.))
   :reaction-listeners (atom (js/Set.))
   :voice-state-listeners (atom (js/Set.))
   :voice-connections (js/Map.)})

(defn- gateway-manager-deps
  [log {:keys [client-state ready-promise listeners reaction-listeners voice-state-listeners]}]
  (let [notify-message (partial notify-message! listeners log)
        notify-reaction (partial notify-reaction! reaction-listeners log)
        notify-voice-state (partial notify-voice-state! voice-state-listeners log)]
    {:build-client (partial build-discord-client log notify-message notify-reaction notify-voice-state)
     :ensure-client (partial ensure-client! client-state ready-promise)}))

(defn- create-gateway-manager-object!
  [log {:keys [client-state ready-promise current-token listeners reaction-listeners
               voice-state-listeners voice-connections] :as state}]
  (let [{:keys [build-client ensure-client]} (gateway-manager-deps log state)
        this-stop (fn [] (gw-stop client-state ready-promise current-token))
        this-obj (atom nil)]
    (letfn [(this-fn [] @this-obj)]
      (reset! this-obj
              (build-gateway-manager-methods
               client-state ready-promise current-token listeners reaction-listeners voice-state-listeners
               log this-stop build-client ensure-client voice-connections this-fn))
      @this-obj)))

(defn createDiscordGatewayManager
  "Create a Discord gateway manager. Returns a JS object with async methods."
  [opts]
  (let [{:keys [log set-default?]} (parse-gateway-manager-opts opts)
        manager (create-gateway-manager-object! log (gateway-manager-state))]
    (when set-default?
      (set-manager! manager))
    manager))

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

(defn set-actor-manager!
  "Store `manager` for exactly one actor, or remove it when manager is nil.

   The actor id is mandatory even for removal. That keeps a blank actor from
   becoming an alias for the legacy/default manager, which would turn an
   actor-scoped caller into a process-scoped one."
  [actor-id manager]
  (let [actor-id (some-> actor-id str str/trim not-empty)]
    (when-not actor-id
      (throw (js/Error. "actor id is required for Discord actor gateway")))
    (if (nil? manager)
      (swap! actor-managers* dissoc actor-id)
      (swap! actor-managers* assoc actor-id manager))
    manager))

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
          (set-actor-manager! actor-id manager)
          manager))))

(defn ^:async start-actor-gateway!
  [actor-id token]
  (let [manager (ensure-actor-manager! actor-id)]
    (await (.start manager token))
    {:actorId actor-id
     :status (js->clj (.status manager) :keywordize-keys true)}))

(defn- credential->actor-gateway-start
  [credential]
  (let [actor-id (credential-actor-id credential)
        token (credential-bot-token credential)]
    (when (and actor-id token)
      {:actorId actor-id :token token})))

(defn- actor-gateway-starts
  [credentials]
  (->> (js->clj (or credentials #js []) :keywordize-keys true)
       (keep credential->actor-gateway-start)
       vec))

(defn- stop-inactive-actor-gateways!
  [active-actor-ids]
  (doseq [[actor-id manager] @actor-managers*]
    (when-not (contains? active-actor-ids actor-id)
      (try
        (.stop manager)
        (catch js/Error _)
        (finally
          (swap! actor-managers* dissoc actor-id))))))

(defn- ^:async start-actor-gateway-best-effort!
  [{:keys [actorId token]}]
  (try
    (await (start-actor-gateway! actorId token))
    (catch js/Error err
      (.warn js/console "[discord-gateway] actor gateway start failed" actorId (.-message err))
      {:actorId actorId :error (.-message err)})))

(defn ^:async start-actor-gateways!
  [credentials]
  (let [valid (actor-gateway-starts credentials)
        active-actor-ids (set (map :actorId valid))
        starts (clj->js (mapv start-actor-gateway-best-effort! valid))]
    (stop-inactive-actor-gateways! active-actor-ids)
    (js->clj (await (js/Promise.all starts)) :keywordize-keys true)))

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

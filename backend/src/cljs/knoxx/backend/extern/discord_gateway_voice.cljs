(ns knoxx.backend.extern.discord-gateway-voice
  "Discord native voice connection, playback and member boundary."
  (:require ["@discordjs/voice" :as voice]
            ["node:stream" :as stream]))

(defn voice-connection-guild-id
  "Discord gateway operation: voice-connection-guild-id."
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

(defn ^:async gw-join-voice
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

(defn gw-leave-voice
  "Leave a voice channel for a guild."
  [connections guild-id]
  (when-let [conn (.get connections guild-id)]
    (.destroy conn)
    (.delete connections guild-id))
  (js/Promise.resolve true))

(defn gw-play-audio
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
            stream (new stream/Readable #js {:read (fn [])})
            _ (do (.push stream audio-buffer) (.push stream nil))
            resource (voice/createAudioResource
                      stream
                      #js {:inputType (.-Arbitrary (aget voice "StreamType"))})]
        (.play player resource)
        (js/Promise.resolve true)))))

(defn gw-subscribe-voice
  "Subscribe to audio from a specific user. Returns an unsubscribe function."
  [connections guild-id user-id callback]
  (let [conn (.get connections guild-id)]
    (if-not conn
      (js/Promise.reject (js/Error. (str "No voice connection for guild: " guild-id)))
      (let [receiver (.-receiver conn)
            opus-stream (.subscribe receiver user-id #js {:mode "opus"})]
        (.on opus-stream "data" (fn [audio-chunk] (callback user-id audio-chunk)))
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

(defn ^:async gw-list-voice-members
  "List members in a voice channel."
  [ensure-client guild-id channel-id]
  (let [active-client (await (ensure-client))
        guild (await (fetch-guild! active-client guild-id))
        channel (await (fetch-guild-channel! guild channel-id))]
    (into-array (for [[_ member] (.-members channel)]
                  (voice-member-entry member)))))


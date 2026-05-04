(ns knoxx.backend.tools.discord-voice
  "Discord voice channel tools: join, leave, say, listen."
  (:require [clojure.string :as str]
            [knoxx.backend.agent-context :as agent-ctx]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.discord-gateway :as dg]
            [knoxx.backend.text :refer [tool-text-result]]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! create-tool-obj]]))

(defn- gw [] (dg/gateway-manager))

(defn- resolve-voice-key [config]
  (or (when (map? config) (get config :voxx-api-key))
      (some-> js/process .-env (aget "VOICE_GATEWAY_API_KEY"))
      (some-> js/process .-env (aget "KNOXX_VOICE_GATEWAY_API_KEY"))))

(defn- voice-gateway-url [config]
  (or (when (map? config) (get config :voxx-url))
      (some-> js/process .-env (aget "VOXX_URL"))
      "http://127.0.0.1:8787"))

(defn- tts-url [config]
  (let [base (str/replace (voice-gateway-url config) #"/+$" "")]
    (cond
      (str/ends-with? base "/v1/audio/speech") base
      (str/ends-with? base "/v1") (str base "/audio/speech")
      :else (str base "/v1/audio/speech"))))

(defn- stt-url [config]
  (or (when (map? config) (get config :stt-url))
      (some-> js/process .-env (aget "KNOXX_STT_URL"))
      "http://127.0.0.1:8010"))

(defn- knoxx-base [config]
  (or (when (map? config) (get config :knoxx-base-url))
      (some-> js/process .-env (aget "KNOXX_BASE_URL"))
      "http://127.0.0.1:8000"))

(defn- knoxx-key [config]
  (or (when (map? config) (get config :knoxx-api-key))
      (some-> js/process .-env (aget "KNOXX_API_KEY"))))

(defn- fetch-tts! [config text voice-id model-id]
  (let [api-key (or (resolve-voice-key config) (throw (js/Error. "VOICE_GATEWAY_API_KEY not configured")))
        body {:input text :voice (or voice-id "alloy") :model (or model-id "kokoro")
              :response_format "mp3" :postprocess_enabled false}]
    (-> (js/fetch (tts-url config)
                  #js {:method "POST"
                       :headers #js {"Authorization" (str "Bearer " api-key) "Content-Type" "application/json" "Accept" "audio/mpeg"}
                       :body (.stringify js/JSON (clj->js body))})
        (.then (fn [r] (if (.-ok r) (.arrayBuffer r) (throw (js/Error. (str "TTS " (.-status r)))))))
        (.then (fn [b] (.from js/Buffer (js/Uint8Array. b)))))))

(defn- transcribe! [config audio-buffer]
  ;; Knoxx STT service expects raw audio bytes (not multipart). It runs ffmpeg
  ;; server-side, so WAV is the safest on-the-wire format.
  (js/console.log "[voice:stt] === TRANSCRIBE START ===" (.-length audio-buffer) "bytes from" (stt-url config))
  (js/console.log "[voice:stt] sending POST to" (str (stt-url config) "/transcribe"))
  (-> (js/fetch (str (stt-url config) "/transcribe")
                #js {:method "POST"
                     :headers #js {"Content-Type" "audio/wav"}
                     :body audio-buffer})
      (.then (fn [r]
               (js/console.log "[voice:stt] response received, status:" (.-status r) "ok:" (.-ok r))
               (if (.-ok r)
                 (do (js/console.log "[voice:stt] parsing JSON response") (.json r))
                 (do (js/console.error "[voice:stt] HTTP FAILED:" (.-status r))
                     (throw (js/Error. (str "STT " (.-status r))))))))
      (.then (fn [j]
               (js/console.log "[voice:stt] JSON parsed:" (js/JSON.stringify j))
               (let [text (or (.-text j) (.-transcription j) "")]
                 (js/console.log "[voice:stt] extracted text:" (if (str/blank? text) "[EMPTY]" text))
                 text)))
      (.catch (fn [err]
                (js/console.error "[voice:stt] === TRANSCRIBE ERROR ===" (.-message err))
                (throw err)))))

(defn- steer! [config session-id conversation-id text]
  (js/console.log "[voice:steer] injecting into session:" session-id "conv:" conversation-id "text:" (.slice text 0 60))
  (let [body {:message (str "[Voice] " text) :conversation_id conversation-id :session_id session-id}]
    (-> (js/fetch (str (knoxx-base config) "/api/knoxx/steer")
                  #js {:method "POST"
                       :headers #js {"Content-Type" "application/json" "Authorization" (str "Bearer " (or (knoxx-key config) ""))}
                       :body (.stringify js/JSON (clj->js body))})
        (.then (fn [r]
                 (if (.-ok r)
                   (js/console.log "[voice:steer] ok")
                   (do (js/console.error "[voice:steer] failed:" (.-status r))
                       (throw (js/Error. (str "Steer " (.-status r)))))))))))

;; ---------------------------------------------------------------------------
;; Hoisted tool definitions: params, execute, tool-obj per tool
;; ---------------------------------------------------------------------------

(def voice-join-params
  [:map
   [:channel_id {:description "Discord voice channel ID to join."} :string]
   [:guild_id {:optional true :description "Guild ID with an active voice connection."} :string]])

(defn voice-join-execute [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        m (gw)
        ch (or (aget params "channel_id") (aget params "channelId") "")]
    (when-not m (throw (js/Error. "Gateway not started")))
    (when (str/blank? ch) (throw (js/Error. "channel_id required")))
    (js/console.log "[voice:tool] discord.voice.join channel:" ch)
    (maybe-tool-update! on-update (str "Joining voice " ch "…"))
    (-> (.joinVoice m ch)
        (.then (fn [r]
                 (js/console.log "[voice:tool] joined voice, result:" (js/JSON.stringify r))
                 (tool-text-result (str "Joined voice " ch " in guild " (aget r "guildId")) (js->clj r :keywordize-keys true)))))))

(def voice-join-tool (partial create-tool-obj "discord.voice.join" "Join Voice"
                              "Join a Discord voice channel."
                              "Join a voice channel to enable voice features."
                              (clj->js ["Use discord.voice.join to connect to a voice channel."
                                        "Provide channel_id from discord.list.channels."])
                              voice-join-params
                              voice-join-execute))

(def voice-leave-params
  [:map
   [:guild_id {:description "Guild ID with an active voice connection."} :string]])

(defn voice-leave-execute [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        m (gw)
        g (or (aget params "guild_id") (aget params "guildId") "")]
    (when-not m (throw (js/Error. "Gateway not started")))
    (when (str/blank? g) (throw (js/Error. "guild_id required")))
    (maybe-tool-update! on-update (str "Leaving voice " g "…"))
    (when-let [sf (aget m "__voiceListener")] (try (sf) (catch js/Error _)))
    (-> (.leaveVoice m g)
        (.then (fn [r] (tool-text-result (str "Left voice in guild " g) (js->clj r :keywordize-keys true)))))))

(def voice-leave-tool (partial create-tool-obj "discord.voice.leave" "Leave Voice"
                               "Leave a Discord voice channel."
                               "Disconnect from a voice channel."
                               (clj->js ["Use discord.voice.leave to disconnect from a voice channel."
                                         "Provide the guild_id of the active connection."])
                               voice-leave-params
                               voice-leave-execute))

(def voice-say-params
  [:map
   [:guild_id {:description "Guild ID with an active voice connection."} :string]
   [:text {:description "Text to synthesize and play in the voice channel."} :string]
   [:voice_id {:optional true :description "Voxx voice ID. Default: alloy."} :string]
   [:model_id {:optional true :description "Voxx model ID. Default: kokoro."} :string]])

(defn voice-say-execute [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        m (gw)
        g (or (aget params "guild_id") (aget params "guildId") "")
        text (or (aget params "text") "")
        vi (aget params "voice_id")
        mi (aget params "model_id")
        listening? (boolean (when m (aget m "__voiceListener")))]
    (when-not m (throw (js/Error. "Gateway not started")))
    (when (str/blank? g) (throw (js/Error. "guild_id required")))
    (when (str/blank? text) (throw (js/Error. "text required")))
    ;; Contract invariant: voice agents should not speak unless listening.
    ;; We enforce that at runtime to avoid confusing partial-voice behavior.
    (when-not listening?
      (throw (js/Error. "Voice listener is not running. Use discord.voice.connect (preferred) before discord.voice.say.")))
    (maybe-tool-update! on-update (str "TTS: \"" (.slice text 0 40) "\"…"))
    (-> (fetch-tts! config text vi mi)
        (.then (fn [buf] (.playAudio m g buf)))
        (.then (fn [_]
                 (tool-text-result (str "Playing in guild " g ": \"" (.slice text 0 60) "\"")
                                   {:guildId g :text text :played true :listening true}))))))

(def voice-say-tool (partial create-tool-obj "discord.voice.say" "Voice Say"
                             "Synthesize speech and play in a voice channel."
                             "Speak text aloud in a connected voice channel."
                             (clj->js ["Use discord.voice.say to speak in a voice channel."
                                       "Must be connected via discord.voice.join first."
                                       "Provide guild_id and text. Optionally set voice_id and model_id."])
                             voice-say-params
                             voice-say-execute))

(def voice-status-params
  [:map])

(defn voice-status-execute [runtime config _tool-call-id _params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        m (gw)]
    (when-not m (throw (js/Error. "Gateway not started")))
    (maybe-tool-update! on-update "Checking voice…")
    (let [c (.getVoiceConnection m)]
      (tool-text-result (if c (str "Connected to guild " (or (.-__guildId c) (.-guildId c))) "Not connected") {:connected (some? c)}))))

(def voice-status-tool (partial create-tool-obj "discord.voice.status" "Voice Status"
                                "Check voice connection status."
                                "Check whether the bot is connected to a voice channel."
                                (clj->js ["Use discord.voice.status to check if the bot is in a voice channel."
                                          "No parameters required."])
                                voice-status-params
                                voice-status-execute))

(def voice-connect-params
  [:map
   [:channel_id {:description "Discord voice channel ID to join."} :string]
   [:session_id {:optional true :description "Agent session ID to inject transcriptions into. Auto-detected if omitted."} :string]
   [:conversation_id {:optional true :description "Agent conversation ID for the session. Auto-detected if omitted."} :string]])

(def voice-listen-params
  [:map
   [:guild_id {:description "Guild ID with an active voice connection."} :string]
   [:session_id {:optional true :description "Agent session ID to inject transcriptions into. Auto-detected if omitted."} :string]
   [:conversation_id {:optional true :description "Agent conversation ID for the session. Auto-detected if omitted."} :string]])

(defn- resolve-session-context!
  "Resolve (session-id, conversation-id) either from explicit params or the current agent context." 
  [params]
  (let [explicit-sid (or (aget params "session_id") (aget params "sessionId") "")
        explicit-cid (or (aget params "conversation_id") (aget params "conversationId") "")
        agent-context (agent-ctx/get-context)
        sid (or explicit-sid (:session-id agent-context) "")
        cid (or explicit-cid (:conversation-id agent-context) "")]
    {:sid sid
     :cid cid
     :auto? (and (str/blank? explicit-sid) (str/blank? explicit-cid))}))

(defn- ensure-session-context!
  [sid cid]
  (when (str/blank? sid)
    (throw (js/Error.
            (str "session_id required (auto-detect failed; no active agent turn context). "
                 "If calling manually, provide session_id and conversation_id explicitly."))))
  (when (str/blank? cid)
    (throw (js/Error.
            (str "conversation_id required (auto-detect failed; no active agent turn context). "
                 "If calling manually, provide session_id and conversation_id explicitly.")))))

(defn voice-listen-execute
  [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        m (gw)
        g (or (aget params "guild_id") (aget params "guildId") "")
        {:keys [sid cid auto?]} (resolve-session-context! params)]
    (when-not m (throw (js/Error. "Gateway not started")))
    (when (str/blank? g) (throw (js/Error. "guild_id required")))
    (ensure-session-context! sid cid)
    (maybe-tool-update! on-update (str "Listening in guild " g "…"))
    (js/console.log "[voice:tool] discord.voice.listen guild:" g "session:" sid "conv:" cid "auto-detect?" auto?)
    (-> (.startVoiceListener
         m g
         (fn [uid] (js/console.log "[voice:tool] >>> on-start callback fired for user:" uid))
         (fn [uid buf]
           (js/console.log "[voice:tool] >>> on-audio callback fired for user:" uid "buffer length:" (.-length buf) "bytes")
           (-> (transcribe! config buf)
               (.then (fn [t]
                        (js/console.log "[voice:tool] transcription result for" uid ":" (if (str/blank? t) "[EMPTY]" t))
                        (when-not (str/blank? t)
                          (steer! config sid cid t))))
               (.catch (fn [e]
                         (js/console.error "[voice:tool] transcription/steering pipeline FAILED for" uid ":" (.-message e)))))))
        (.then (fn [stop]
                 (aset m "__voiceListener" stop)
                 (tool-text-result (str "Listening in guild " g ". Transcriptions → session " sid)
                                   {:guildId g :listening true}))))))

(defn voice-connect-execute
  [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        m (gw)
        ch (or (aget params "channel_id") (aget params "channelId") "")
        {:keys [sid cid auto?]} (resolve-session-context! params)]
    (when-not m (throw (js/Error. "Gateway not started")))
    (when (str/blank? ch) (throw (js/Error. "channel_id required")))
    (ensure-session-context! sid cid)
    (maybe-tool-update! on-update (str "Connecting voice + listener for channel " ch "…"))
    (-> (.joinVoice m ch)
        (.then (fn [r]
                 (let [guild-id (or (aget r "guildId") "")]
                   (when (str/blank? guild-id)
                     (throw (js/Error. "joinVoice did not return guildId")))
                   (js/console.log "[voice:tool] discord.voice.connect joined" ch "guild" guild-id "auto-detect?" auto?)
                   (voice-listen-execute runtime config _tool-call-id (clj->js {:guild_id guild-id
                                                                              :session_id sid
                                                                              :conversation_id cid}) a b c))))
        (.then (fn [_]
                 (tool-text-result (str "Connected to voice " ch " and listening")
                                   {:channelId ch :listening true :sessionId sid :conversationId cid}))))))

(def voice-connect-tool
  (partial create-tool-obj
           "discord.voice.connect" "Voice Connect"
           "Join a Discord voice channel and start listening/transcription."
           "Join voice + enable voice-to-text transcription in one operation."
           (clj->js ["Use discord.voice.connect as the default voice entrypoint."
                     "Provide channel_id. session_id and conversation_id are auto-detected when called during an agent run."
                     "This will join the channel, then start listening in the resulting guild."])
           voice-connect-params
           voice-connect-execute))

(def voice-listen-tool (partial create-tool-obj "discord.voice.listen" "Voice Listen"
                                "Listen for user speech and transcribe into agent session."
                                "Start listening for voice input and transcribe speech to text."
                                (clj->js ["Use discord.voice.listen only when already connected via discord.voice.join."
                                          "Provide guild_id. session_id and conversation_id are auto-detected."
                                          "Transcriptions are steered into the agent session automatically."])
                                voice-listen-params
                                voice-listen-execute))

(def voice-stop-listen-params
  [:map
   [:guild_id {:description "Guild ID with an active voice connection."} :string]])

(defn voice-stop-listen-execute [runtime config _tool-call-id params a b c]
  (let [m (gw)
        g (or (aget params "guild_id") (aget params "guildId") "")]
    (when-let [sf (when m (aget m "__voiceListener"))] (sf))
    (when m (aset m "__voiceListener" nil))
    (tool-text-result (str "Stopped listening in guild " g) {:guildId g :listening false})))

(def voice-stop-listen-tool (partial create-tool-obj "discord.voice.stop_listen" "Stop Voice Listen"
                                     "Stop listening for voice input."
                                     "Stop the active voice listener in a guild."
                                     (clj->js ["Use discord.voice.stop_listen to stop voice transcription."
                                               "Provide the guild_id of the active listener."])
                                     voice-stop-listen-params
                                     voice-stop-listen-execute))

(def voice-list-members-params
  [:map
   [:guild_id {:description "Guild ID with an active voice connection."} :string]
   [:channel_id {:description "Voice channel ID to list members of."} :string]])

(defn voice-list-members-execute [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        m (gw)
        g (or (aget params "guild_id") (aget params "guildId") "")
        ch (or (aget params "channel_id") (aget params "channelId") "")]
    (when-not m (throw (js/Error. "Gateway not started")))
    (when (str/blank? g) (throw (js/Error. "guild_id required")))
    (when (str/blank? ch) (throw (js/Error. "channel_id required")))
    (maybe-tool-update! on-update (str "Listing voice members in " ch "…"))
    (-> (.listVoiceMembers m g ch)
        (.then (fn [members]
                 (let [ms (js->clj members :keywordize-keys true)
                       lines (map (fn [m] (str (if (:isBot m) "[bot] " "") (:displayName m) " (" (:userId m) ")")) ms)]
                   (tool-text-result
                    (str "Voice members in " ch ":\n" (str/join "\n" lines))
                    {:channelId ch :members ms :count (count ms)})))))))

(def voice-list-members-tool (partial create-tool-obj "discord.voice.list_members" "List Voice Members"
                                      "List members currently in a voice channel."
                                      "List who is in a voice channel."
                                      (clj->js ["Use discord.voice.list_members to see who is in a voice channel."
                                                "Provide guild_id and channel_id."])
                                      voice-list-members-params
                                      voice-list-members-execute))

;; ---------------------------------------------------------------------------
;; create-discord-voice-custom-tools: gate + collect
;; ---------------------------------------------------------------------------

(defn create-discord-voice-custom-tools
  ([runtime config] (create-discord-voice-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [ok? (fn [id] (or (nil? auth-context) (ctx-tool-allowed? auth-context id)))]
     (clj->js
      (vec
       (remove nil?
               [(when (ok? "discord.voice.join") (voice-join-tool runtime config))
                (when (ok? "discord.voice.leave") (voice-leave-tool runtime config))
                (when (ok? "discord.voice.say") (voice-say-tool runtime config))
                (when (ok? "discord.voice.status") (voice-status-tool runtime config))
                (when (ok? "discord.voice.connect") (voice-connect-tool runtime config))
                (when (ok? "discord.voice.listen") (voice-listen-tool runtime config))
                (when (ok? "discord.voice.stop_listen") (voice-stop-listen-tool runtime config))
                (when (ok? "discord.voice.list_members") (voice-list-members-tool runtime config))]))))))

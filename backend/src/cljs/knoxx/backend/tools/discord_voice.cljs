(ns knoxx.backend.tools.discord-voice
  "Discord voice channel tools: join, leave, say, listen."
  (:require [clojure.string :as str]
            [knoxx.backend.agent-context :as agent-ctx]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.discord-gateway :as dg]
            [knoxx.backend.http :as backend-http]
            [knoxx.backend.openplanner-memory :as op-mem]
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

(defn- proxx-chat-completions-url [config]
  (str (str/replace (or (when (map? config) (get config :proxx-base-url))
                        (some-> js/process .-env (aget "PROXX_BASE_URL"))
                        "http://proxx:8789")
                    #"/+$" "")
       "/v1/chat/completions"))

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

;; ---------------------------------------------------------------------------
;; Hoisted tool definitions: params, execute, tool-obj per tool
;; ---------------------------------------------------------------------------

(declare resolve-session-context! stop-existing-voice-loop!)

(def voice-join-params
  [:map
   [:channel_id {:description "Discord voice channel ID to join."} :string]
   [:guild_id {:optional true :description "Guild ID with an active voice connection."} :string]])

(defn voice-join-execute [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        m (gw)
        ch (or (aget params "channel_id") (aget params "channelId") "")
        {:keys [sid cid]} (resolve-session-context! params)]
    (when-not m (throw (js/Error. "Gateway not started")))
    (when (str/blank? ch) (throw (js/Error. "channel_id required")))
    (js/console.log "[voice:tool] discord.voice.join channel:" ch)
    ;; Persist session context so a subsequent discord.voice.listen can find it
    ;; even if the global agent context has been cleared.
    (when (and m (seq sid) (seq cid))
      (aset m "__voiceSessionContext" #js {:sessionId sid :conversationId cid}))
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
    (when m
      (stop-existing-voice-loop! m)
      (aset m "__voiceSessionContext" nil))
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
                                       "Must be in voice mode via discord.voice.connect first."
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
    (let [c (.getVoiceConnection m)
          loop-state (aget m "__voiceGemmaLoop")]
      (tool-text-result (if c (str "Connected to guild " (or (.-__guildId c) (.-guildId c))) "Not connected")
                        {:connected (some? c)
                         :gemmaLoop (some? loop-state)
                         :modelId (when loop-state (aget loop-state "modelId"))
                         :queuedAudioWindows (when loop-state (.-length (or (aget loop-state "audioWindows") #js [])))}))))

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
   [:session_id {:optional true :description "Optional session ID to attach voice memory events to. Auto-detected if omitted."} :string]
   [:conversation_id {:optional true :description "Optional conversation ID to attach voice memory events to. Auto-detected if omitted."} :string]
   [:model_id {:optional true :description "Multimodal cognitive core model. Default gemma4:e4b."} :string]
   [:voice_id {:optional true :description "Voxx voice ID for say actions. Default alloy."} :string]
   [:tts_model_id {:optional true :description "Voxx TTS model for say actions. Default kokoro."} :string]
   [:tick_ms {:optional true :description "Mechanical loop cadence in ms. Default 1000."} :int]
   [:heartbeat_ms {:optional true :description "Run an idle state tick at least this often even without new audio. Default 10000; 0 disables idle ticks."} :int]
   [:min_say_interval_ms {:optional true :description "Host safety valve: minimum ms between spoken actions. Default 10000."} :int]])

(def voice-listen-params
  [:map
   [:guild_id {:description "Guild ID with an active voice connection."} :string]
   [:session_id {:optional true :description "Optional session ID to attach voice memory events to. Auto-detected if omitted."} :string]
   [:conversation_id {:optional true :description "Optional conversation ID to attach voice memory events to. Auto-detected if omitted."} :string]
   [:model_id {:optional true :description "Multimodal cognitive core model. Default gemma4:e4b."} :string]
   [:voice_id {:optional true :description "Voxx voice ID for say actions. Default alloy."} :string]
   [:tts_model_id {:optional true :description "Voxx TTS model for say actions. Default kokoro."} :string]
   [:tick_ms {:optional true :description "Mechanical loop cadence in ms. Default 1000."} :int]
   [:heartbeat_ms {:optional true :description "Run an idle state tick at least this often even without new audio. Default 10000; 0 disables idle ticks."} :int]
   [:min_say_interval_ms {:optional true :description "Host safety valve: minimum ms between spoken actions. Default 10000."} :int]])

(defn- resolve-session-context!
  "Resolve (session-id, conversation-id) either from explicit params, the current agent context,
   or the last known voice session context stored on the gateway manager."
  [params]
  (let [explicit-sid (or (when-not (str/blank? (aget params "session_id")) (aget params "session_id"))
                         (when-not (str/blank? (aget params "sessionId")) (aget params "sessionId")))
        explicit-cid (or (when-not (str/blank? (aget params "conversation_id")) (aget params "conversation_id"))
                         (when-not (str/blank? (aget params "conversationId")) (aget params "conversationId")))
        agent-context (agent-ctx/get-context)
        ;; Fallback to stored voice session context if agent context is gone
        ;; (e.g. async listener callback fires after turn completes).
        stored-ctx (some-> (gw) (aget "__voiceSessionContext"))
        sid (or explicit-sid
                (:session-id agent-context)
                (when stored-ctx (aget stored-ctx "sessionId"))
                "")
        cid (or explicit-cid
                (:conversation-id agent-context)
                (when stored-ctx (aget stored-ctx "conversationId"))
                "")]
    (js/console.log "[voice:tool] resolve-session-context explicit-sid:" explicit-sid
                    "agent-context:" (when agent-context (js/JSON.stringify (clj->js agent-context)))
                    "stored-sid:" (when stored-ctx (aget stored-ctx "sessionId"))
                    "resolved-sid:" sid)
    {:sid sid
     :cid cid
     :auto? (and (str/blank? explicit-sid) (str/blank? explicit-cid))}))

(defn- param-int [params key camel default]
  (let [raw (or (aget params key) (aget params camel))
        n (js/Number raw)]
    (if (or (nil? raw) (js/isNaN n)) default n)))

(defn- buffer->audio-window [uid audio-buffer]
  #js {:speaker_id uid
       :mimeType "audio/wav"
       :bytes (.-length audio-buffer)
       :received_at (.toISOString (js/Date.))
       :data (.toString audio-buffer "base64")})

(defn- voice-loop-system-prompt []
  (str "You are Knoxx's single continuous audio cognition core inside a Discord voice channel. "
       "There is no ASR layer and no steering agent. Treat audio as primary perception. "
       "Each tick: listen to the latest audio windows, update compact internal state, "
       "decide whether speaking is useful, and optionally write a short memory log. "
       "Stay silent when nothing meaningful changed. Return strict JSON only with keys: "
       "state_next (object), actions (array). Actions may be "
       "{\"type\":\"say\",\"text\":\"...\"}, "
       "{\"type\":\"memory_log\",\"text\":\"...\"}, or {\"type\":\"none\"}."))

(defn- step-user-content [state audio-windows]
  (let [summary {:state state
                 :tick_ts (.toISOString (js/Date.))
                 :audio_window_count (.-length audio-windows)
                 :instruction "Process these audio windows directly. Do not transcribe as a prerequisite; only write memory if useful."}
        content #js [#js {:type "text" :text (.stringify js/JSON (clj->js summary))}]]
    (doseq [w (array-seq audio-windows)]
      (.push content #js {:type "audio"
                          :data (aget w "data")
                          :mimeType (or (aget w "mimeType") "audio/wav")}))
    content))

(defn- parse-json-object-text [text]
  (let [s (str/trim (str (or text "")))
        unfenced (str/replace s #"(?s)^```(?:json)?\s*|\s*```$" "")]
    (try
      (js/JSON.parse unfenced)
      (catch js/Error _
        (let [start (.indexOf unfenced "{")
              end (.lastIndexOf unfenced "}")]
          (when (and (>= start 0) (> end start))
            (js/JSON.parse (.slice unfenced start (inc end)))))))))

(defn- gemma-audio-step! [config model-id state audio-windows]
  (let [body {:model model-id
              :temperature 0.2
              :response_format {:type "json_object"}
              :messages [{:role "system" :content (voice-loop-system-prompt)}
                         {:role "user" :content (step-user-content state audio-windows)}]}]
    (-> (js/fetch (proxx-chat-completions-url config)
                  #js {:method "POST"
                       :headers #js {"Content-Type" "application/json"}
                       :body (.stringify js/JSON (clj->js body))})
        (.then (fn [r]
                 (-> (.text r)
                     (.then (fn [text]
                              (when-not (.-ok r)
                                (throw (js/Error. (str "Gemma audio step " (.-status r) ": " text))))
                              (let [payload (js/JSON.parse text)
                                    content (or (aget payload "choices" 0 "message" "content") "")]
                                (or (parse-json-object-text content)
                                    #js {:state_next state :actions #js []}))))))))))

(defn- append-voice-memory! [config sid model-id text extra]
  (if (str/blank? (str text))
    (js/Promise.resolve nil)
    (let [event (op-mem/openplanner-event config {:id (str "voice:" (.randomUUID js/crypto))
                                                  :kind "knoxx.voice.memory"
                                                  :session sid
                                                  :role "assistant"
                                                  :model model-id
                                                  :text text
                                                  :extra extra})]
      (-> (backend-http/openplanner-request! config "POST" "/v1/events" {:events [event]})
          (.catch (fn [e]
                    (js/console.warn "[voice:gemma-loop] memory_log not persisted:" (.-message e))))))))

(defn- say-action! [config m guild-id text voice-id tts-model-id]
  (-> (fetch-tts! config text voice-id tts-model-id)
      (.then (fn [buf] (.playAudio m guild-id buf)))))

(defn- handle-loop-action! [config m loop-state action]
  (let [typ (or (aget action "type") "none")
        text (str/trim (str (or (aget action "text") "")))
        now (.now js/Date)
        guild-id (aget loop-state "guildId")]
    (cond
      (and (= typ "say") (not (str/blank? text)))
      (if (< (- now (or (aget loop-state "lastSayAt") 0)) (aget loop-state "minSayIntervalMs"))
        (do (js/console.log "[voice:gemma-loop] say suppressed by min_say_interval_ms")
            (js/Promise.resolve nil))
        (do (aset loop-state "lastSayAt" now)
            (say-action! config m guild-id text (aget loop-state "voiceId") (aget loop-state "ttsModelId"))))

      (and (contains? #{"memory_log" "log"} typ) (not (str/blank? text)))
      (append-voice-memory! config (aget loop-state "sessionId") (aget loop-state "modelId") text
                            {:guildId guild-id :conversationId (aget loop-state "conversationId")})

      :else (js/Promise.resolve nil))))

(defn- handle-loop-actions! [config m loop-state actions]
  (let [items (if (array? actions) (array-seq actions) [])]
    (.reduce (clj->js items)
             (fn [p action]
               (.then p #(handle-loop-action! config m loop-state action)))
             (js/Promise.resolve nil))))

(defn- run-loop-step! [config m loop-state]
  (let [queued (or (aget loop-state "audioWindows") #js [])
        heartbeat-ms (aget loop-state "heartbeatMs")
        idle? (zero? (.-length queued))
        last-step (or (aget loop-state "lastStepAt") 0)
        now (.now js/Date)]
    (if (and idle? (or (zero? heartbeat-ms) (< (- now last-step) heartbeat-ms)))
      (js/Promise.resolve nil)
      (let [windows (.splice queued 0 (.-length queued))
            state (or (aget loop-state "state") #js {})
            model-id (aget loop-state "modelId")]
        (aset loop-state "lastStepAt" now)
        (-> (gemma-audio-step! config model-id state windows)
            (.then (fn [result]
                     (when-let [next-state (aget result "state_next")]
                       (aset loop-state "state" next-state))
                     (handle-loop-actions! config m loop-state (aget result "actions")))))))))

(defn- schedule-loop-tick! [config m loop-state]
  (when-not (aget loop-state "stopped")
    (aset loop-state "timer"
          (js/setTimeout
           (fn []
             (if (aget loop-state "running")
               (schedule-loop-tick! config m loop-state)
               (do
                 (aset loop-state "running" true)
                 (-> (run-loop-step! config m loop-state)
                     (.catch (fn [e]
                               (js/console.error "[voice:gemma-loop] step failed:" (.-message e))))
                     (.finally (fn []
                                 (aset loop-state "running" false)
                                 (schedule-loop-tick! config m loop-state)))))))
           (aget loop-state "tickMs")))))

(defn- stop-existing-voice-loop! [m]
  (when-let [loop-state (when m (aget m "__voiceGemmaLoop"))]
    (aset loop-state "stopped" true)
    (when-let [timer (aget loop-state "timer")]
      (js/clearTimeout timer))
    (when-let [stop (aget loop-state "listenerStop")]
      (try (stop) (catch js/Error _)))
    (aset m "__voiceGemmaLoop" nil)
    (aset m "__voiceListener" nil)))

(defn voice-listen-execute
  [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        m (gw)
        g (or (aget params "guild_id") (aget params "guildId") "")
        {:keys [sid cid auto?]} (resolve-session-context! params)
        model-id (or (aget params "model_id") (aget params "modelId") "gemma4:e4b")
        tick-ms (max 250 (param-int params "tick_ms" "tickMs" 1000))
        heartbeat-ms (max 0 (param-int params "heartbeat_ms" "heartbeatMs" 10000))
        min-say-ms (max 0 (param-int params "min_say_interval_ms" "minSayIntervalMs" 10000))]
    (when-not m (throw (js/Error. "Gateway not started")))
    (when (str/blank? g) (throw (js/Error. "guild_id required")))
    ;; Persist optional session context for memory events; no steer endpoint is used.
    (when m
      (aset m "__voiceSessionContext" #js {:sessionId sid :conversationId cid}))
    (stop-existing-voice-loop! m)
    (maybe-tool-update! on-update (str "Starting continuous Gemma audio loop in guild " g "…"))
    (js/console.log "[voice:tool] discord.voice.listen/gemma-loop guild:" g "session:" sid "conv:" cid
                    "auto-detect?" auto? "model:" model-id "tick_ms:" tick-ms)
    (-> (.startVoiceListener
         m g
         (fn [uid]
           (js/console.log "[voice:gemma-loop] speaker start:" uid))
         (fn [uid buf]
           (js/console.log "[voice:gemma-loop] audio window:" uid "bytes:" (.-length buf))
           (when-let [loop-state (aget m "__voiceGemmaLoop")]
             (.push (aget loop-state "audioWindows") (buffer->audio-window uid buf)))))
        (.then (fn [stop]
                 (let [loop-state #js {:guildId g
                                        :sessionId sid
                                        :conversationId cid
                                        :modelId model-id
                                        :tickMs tick-ms
                                        :heartbeatMs heartbeat-ms
                                        :minSayIntervalMs min-say-ms
                                        :voiceId (aget params "voice_id")
                                        :ttsModelId (aget params "tts_model_id")
                                        :audioWindows #js []
                                        :state #js {:mode "continuous_audio_loop"}
                                        :lastSayAt 0
                                        :lastStepAt 0
                                        :running false
                                        :stopped false
                                        :listenerStop stop}]
                   (aset m "__voiceGemmaLoop" loop-state)
                   ;; Keep __voiceListener truthy so discord.voice.say remains a voice-mode action.
                   (aset m "__voiceListener" stop)
                   (schedule-loop-tick! config m loop-state)
                   (tool-text-result (str "Continuous Gemma audio loop active in guild " g " via " model-id)
                                     {:guildId g :listening true :mode "gemma-audio-loop"
                                      :modelId model-id :tickMs tick-ms :heartbeatMs heartbeat-ms
                                      :sessionId sid :conversationId cid})))))))

(defn voice-connect-execute
  [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        m (gw)
        ch (or (aget params "channel_id") (aget params "channelId") "")
        {:keys [sid cid auto?]} (resolve-session-context! params)]
    (when-not m (throw (js/Error. "Gateway not started")))
    (when (str/blank? ch) (throw (js/Error. "channel_id required")))
    ;; Persist optional session context for memory events; no steer endpoint is used.
    (when m
      (aset m "__voiceSessionContext" #js {:sessionId sid :conversationId cid}))
    (maybe-tool-update! on-update (str "Connecting voice + continuous Gemma loop for channel " ch "…"))
    (-> (.joinVoice m ch)
        (.then (fn [r]
                 (let [guild-id (or (aget r "guildId") "")]
                   (when (str/blank? guild-id)
                     (throw (js/Error. "joinVoice did not return guildId")))
                   (js/console.log "[voice:tool] discord.voice.connect joined" ch "guild" guild-id "auto-detect?" auto?)
                   (voice-listen-execute runtime config _tool-call-id
                                         (clj->js {:guild_id guild-id
                                                   :session_id sid
                                                   :conversation_id cid
                                                   :model_id (or (aget params "model_id") (aget params "modelId"))
                                                   :voice_id (or (aget params "voice_id") (aget params "voiceId"))
                                                   :tts_model_id (or (aget params "tts_model_id") (aget params "ttsModelId"))
                                                   :tick_ms (or (aget params "tick_ms") (aget params "tickMs"))
                                                   :heartbeat_ms (or (aget params "heartbeat_ms") (aget params "heartbeatMs"))
                                                   :min_say_interval_ms (or (aget params "min_say_interval_ms")
                                                                            (aget params "minSayIntervalMs"))})
                                         a b c))))
        (.then (fn [_]
                 (tool-text-result (str "Connected to voice " ch " and listening")
                                   {:channelId ch :listening true :sessionId sid :conversationId cid}))))))

(def voice-connect-tool
  (partial create-tool-obj
           "discord.voice.connect" "Voice Connect"
           "Join a Discord voice channel and start the continuous Gemma audio loop."
           "Join voice + enable one-model audio cognition; no ASR or steer endpoint is used."
           (clj->js ["Use discord.voice.connect as the default voice entrypoint."
                     "Provide channel_id. session_id and conversation_id are optional and only tag memory events."
                     "This will join the channel, then start the continuous Gemma audio loop in the resulting guild."])
           voice-connect-params
           voice-connect-execute))

(def voice-listen-tool (partial create-tool-obj "discord.voice.listen" "Voice Listen"
                                "Start the continuous Gemma audio loop in an existing voice connection."
                                "Listen directly with the multimodal cognitive core; no ASR or steering pass."
                                (clj->js ["Use discord.voice.listen only when already connected via discord.voice.join."
                                          "Provide guild_id. session_id and conversation_id are optional memory tags."
                                          "Audio windows are sent directly to Gemma; Gemma chooses say/memory/none actions."])
                                voice-listen-params
                                voice-listen-execute))

(def voice-stop-listen-params
  [:map
   [:guild_id {:description "Guild ID with an active voice connection."} :string]])

(defn voice-stop-listen-execute [runtime config _tool-call-id params a b c]
  (let [m (gw)
        g (or (aget params "guild_id") (aget params "guildId") "")]
    (when m
      (stop-existing-voice-loop! m)
      (aset m "__voiceSessionContext" nil))
    (tool-text-result (str "Stopped listening in guild " g) {:guildId g :listening false})))

(def voice-stop-listen-tool (partial create-tool-obj "discord.voice.stop_listen" "Stop Voice Listen"
                                     "Stop the continuous Gemma audio loop."
                                     "Stop the active voice listener/loop in a guild."
                                     (clj->js ["Use discord.voice.stop_listen to stop the continuous Gemma audio loop."
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

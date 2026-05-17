(ns knoxx.backend.tools.discord-voice
  "Discord voice channel tools: join, leave, say, listen."
  (:require [clojure.string :as str]
            [knoxx.backend.agent-context :as agent-ctx]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.discord-gateway :as dg]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.session-store :as session-store]
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

(defn- default-tts-speed [config]
  (or (when (map? config) (get config :voxx-default-speed))
      (some-> js/process .-env (aget "KNOXX_VOXX_DEFAULT_SPEED"))
      (some-> js/process .-env (aget "VOICE_GATEWAY_TTS_DEFAULT_SPEED"))
      "1.15"))

(defn- stt-url [config]
  (or (when (map? config) (or (get config :stt-url)
                              (get config :stt-base-url)))
      (some-> js/process .-env (aget "KNOXX_STT_URL"))
      (some-> js/process .-env (aget "KNOXX_STT_BASE_URL"))
      "http://127.0.0.1:8010"))

(defn- knoxx-base [config]
  (or (when (map? config) (get config :knoxx-base-url))
      (some-> js/process .-env (aget "KNOXX_BASE_URL"))
      "http://127.0.0.1:8000"))

(defn- knoxx-key [config]
  (or (when (map? config) (get config :knoxx-api-key))
      (some-> js/process .-env (aget "KNOXX_API_KEY"))))

(defn- knoxx-control-headers [config]
  (let [api-key (knoxx-key config)
        headers #js {"Content-Type" "application/json"
                     "x-knoxx-user-email" "system-admin@open-hax.local"}]
    (when-not (str/blank? (str api-key))
      (aset headers "X-API-Key" api-key))
    headers))

(defn- param-int [params key camel default]
  (let [raw (or (aget params key) (aget params camel))
        n (js/Number raw)]
    (if (or (nil? raw) (js/isNaN n)) default n)))

(defn- fetch-tts! [config text voice-id model-id]
  (let [api-key (or (resolve-voice-key config) (throw (js/Error. "VOICE_GATEWAY_API_KEY not configured")))
        body {:input text :voice (or voice-id "af_jessica") :model (or model-id "kokoro")
              :response_format "mp3" :speed (default-tts-speed config) :postprocess_enabled false}]
    (-> (js/fetch (tts-url config)
                  #js {:method "POST"
                       :headers #js {"Authorization" (str "Bearer " api-key) "Content-Type" "application/json" "Accept" "audio/mpeg"}
                       :body (.stringify js/JSON (clj->js body))})
        (.then (fn [r] (if (.-ok r) (.arrayBuffer r) (throw (js/Error. (str "TTS " (.-status r)))))))
        (.then (fn [b] (.from js/Buffer (js/Uint8Array. b)))))))

(defn- parse-stt-json-text [raw]
  ;; Some STT deployments respond as SSE-ish text where the JSON is prefixed with
  ;; "data: ", e.g. "data: {\"text\":...}". Accept both plain JSON and SSE.
  ;; For chunked (streaming) responses, concatenate all text segments.
  (let [s (str/trim (str raw))]
    (if (str/includes? s "data:")
      ;; SSE format — collect all data: lines and merge text fields
      (let [lines (->> (str/split-lines s)
                       (keep (fn [line]
                               (let [t (str/trim line)]
                                 (when (str/starts-with? t "data:")
                                   (str/trim (subs t 5)))))))
            ;; Parse each line, collect non-empty text segments
            segments (keep (fn [text]
                             (when (seq text)
                               (try
                                 (let [j (js/JSON.parse text)
                                       txt (or (.-text j) (.-transcription j) "")]
                                   (when (seq txt) txt))
                                 (catch js/Error _ nil))))
                           lines)
            final-segment (last (keep (fn [text]
                                        (when (seq text)
                                          (try
                                            (js/JSON.parse text)
                                            (catch js/Error _ nil))))
                                      lines))
            merged-text (str/trim (str/join " " segments))]
        ;; Return a synthetic JS object with merged text and final flag
        #js {:text merged-text
             :final (or (when final-segment (.-final final-segment)) true)})
      ;; Plain JSON
      (js/JSON.parse s))))

(defn- stt-text-garbage?
  "Detect repetitive/garbage STT output (e.g. NPU KV-cache stuck)."
  [text]
  (when (seq text)
    (let [t (str/trim text)]
      (and (> (count t) 10)
           ;; Fewer than 3 unique non-space characters = repetitive garbage
           (let [chars (set (remove #(or (= % " ") (= % "\n")) t))]
             (<= (count chars) 2))
           ;; Contains no ASCII letters or digits
           (not (re-find #"[a-zA-Z0-9]" t))))))

(defn- transcribe! [config audio-buffer]
  ;; Knoxx STT service expects raw audio bytes (not multipart). It runs ffmpeg
  ;; server-side, so WAV is the safest on-the-wire format.
  (js/console.log "[voice:stt] === TRANSCRIBE START ===" (.-length audio-buffer) "bytes from" (stt-url config))
  (js/console.log "[voice:stt] sending POST to" (str (stt-url config) "/transcribe"))
  (-> (js/fetch (str (stt-url config) "/transcribe")
                #js {:method "POST"
                     :headers #js {"Content-Type" "audio/wav"
                                   "Accept" "application/json, text/plain, text/event-stream"}
                     :body audio-buffer})
      (.then (fn [r]
               (js/console.log "[voice:stt] response received, status:" (.-status r) "ok:" (.-ok r))
               (if (.-ok r)
                 (do (js/console.log "[voice:stt] parsing response body") (.text r))
                 (do (js/console.error "[voice:stt] HTTP FAILED:" (.-status r))
                     (throw (js/Error. (str "STT " (.-status r))))))))
      (.then (fn [raw]
               (js/console.log "[voice:stt] raw body prefix:" (.slice (str raw) 0 80))
               (let [j (parse-stt-json-text raw)]
                 (js/console.log "[voice:stt] JSON parsed:" (js/JSON.stringify j))
                 (let [text (or (.-text j) (.-transcription j) "")]
                   (if (stt-text-garbage? text)
                     (do (js/console.warn "[voice:stt] GARBAGE detected, discarding:" (.slice text 0 60))
                         "")
                     (do (js/console.log "[voice:stt] extracted text:" (if (str/blank? text) "[EMPTY]" text))
                         text))))))
      (.catch (fn [err]
                (js/console.error "[voice:stt] === TRANSCRIBE ERROR ===" (.-message err))
                (throw err)))))

(defn- steer! [config session-id conversation-id text]
  (js/console.log "[voice:steer] injecting into session:" session-id "conv:" conversation-id "text:" (.slice text 0 60))
  (let [body {:message (str "[Voice] " text) :conversation_id conversation-id :session_id session-id}]
    (-> (js/fetch (str (knoxx-base config) "/api/knoxx/steer")
                  #js {:method "POST"
                       :headers (knoxx-control-headers config)
                       :body (.stringify js/JSON (clj->js body))})
        (.then (fn [r]
                 (-> (.text r)
                     (.then (fn [raw]
                              (if (.-ok r)
                                (do (js/console.log "[voice:steer] ok") raw)
                                (do (js/console.error "[voice:steer] failed:" (.-status r) raw)
                                    (throw (js/Error. (str "Steer " (.-status r) ": " raw)))))))))))))

(defn- session-agent-spec [session]
  (or (:agent_spec session)
      (:agentSpec session)))

(defn- direct-start-voice-turn! [config body]
  (-> (js/fetch (str (knoxx-base config) "/api/knoxx/direct/start")
                #js {:method "POST"
                     :headers (knoxx-control-headers config)
                     :body (.stringify js/JSON (clj->js body))})
      (.then (fn [r]
               (-> (.text r)
                   (.then (fn [raw]
                            (if (.-ok r)
                              (do (js/console.log "[voice:direct-start] ok") raw)
                              (do (js/console.error "[voice:direct-start] failed:" (.-status r) raw)
                                  (throw (js/Error. (str "Direct start " (.-status r) ": " raw))))))))))))

(defn- start-voice-turn! [config session-id conversation-id text]
  (js/console.log "[voice:direct-start] starting idle session:" session-id "conv:" conversation-id)
  (-> (session-store/get-session (redis/get-client) session-id)
      (.then (fn [session]
               (let [agent-spec (session-agent-spec session)
                     body (cond-> {:message (str "[Voice] " text)
                                   :conversation_id conversation-id
                                   :session_id session-id}
                            agent-spec (assoc :agent_spec agent-spec))]
                 (when agent-spec
                   (js/console.log "[voice:direct-start] resuming agent spec:"
                                   (js/JSON.stringify (clj->js (select-keys agent-spec [:contractId :contract-id :actorId :actor-id :role :model])))))
                 (direct-start-voice-turn! config body))))))

(defn- inactive-steer-error? [err]
  (let [message (str/lower-case (str (.-message err)))]
    (or (str/includes? message "no active running turn")
        (str/includes? message "conversation is not active")
        (str/includes? message "not active in the agent runtime"))))

(defn- deliver-voice-text! [config sid cid text]
  (-> (steer! config sid cid text)
      (.catch (fn [err]
                (if (inactive-steer-error? err)
                  (do (js/console.log "[voice:deliver] steer target idle; starting a normal voice turn")
                      (start-voice-turn! config sid cid text))
                  (throw err))))))

;; ---------------------------------------------------------------------------
;; Hoisted tool definitions: params, execute, tool-obj per tool
;; ---------------------------------------------------------------------------

(declare resolve-session-context! stop-agent-event-loop!)

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
      (stop-agent-event-loop! m)
      (when-let [sf (aget m "__voiceListener")] (try (sf) (catch js/Error _)))
      (aset m "__voiceListener" nil)
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
   [:voice_id {:optional true :description "Voxx/Kokoro voice ID. Default: af_jessica."} :string]
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
    (let [c (.getVoiceConnection m)
          agent-loop (aget m "__voiceAgentEventLoop")]
      (tool-text-result (if c (str "Connected to guild " (or (.-__guildId c) (.-guildId c))) "Not connected")
                        {:connected (some? c)
                         :agentEventLoop (some? agent-loop)
                         :queuedAudioWindows (when agent-loop
                                               (.-length (or (aget agent-loop "audioWindows") #js [])))}))))

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

(defn- flush-voice-buffer!
  "Send accumulated transcription text for a user as a single steer."
  [config sid cid uid]
  (let [m (gw)
        buf-obj (when m (aget m "__voiceTranscriptionBuffer"))
        user-buf (when buf-obj (aget buf-obj uid))]
    (when user-buf
      (let [texts (aget user-buf "texts")]
        (when (and texts (.-length texts))
          (let [merged (str/trim (str/join " " (js/Array.from texts)))]
            (js/console.log "[voice:tool] >>> FLUSHING buffer for" uid "concatenated:" (if (str/blank? merged) "[EMPTY]" merged))
            (when-not (str/blank? merged)
              (-> (deliver-voice-text! config sid cid merged)
                  (.catch (fn [e]
                            (js/console.error "[voice:tool] voice delivery FAILED for" uid ":" (.-message e))))))))
        ;; Clear the buffer
        (aset user-buf "texts" #js [])
        (aset user-buf "timer" nil)))))

(defn- audio-window-content-part [uid audio-buffer]
  {:type "audio"
   :data (.toString audio-buffer "base64")
   :mimeType "audio/wav"
   :filename (str "discord-voice-" uid "-" (.now js/Date) ".wav")
   :bytes (.-length audio-buffer)})

(defn- requeue-front! [queue windows]
  (doseq [window (reverse windows)]
    (.unshift queue window)))

(defn- trigger-agent-voice-event! [config loop-state windows]
  (let [guild-id (aget loop-state "guildId")
        channel-id (aget loop-state "channelId")
        event-id (str "discord-voice-audio-" guild-id "-" (.now js/Date))
        body {:id event-id
              :sourceKind "discord"
              :eventKind "discord.voice.audio.window"
              :eventKinds ["discord.voice.audio.window"]
              :payload {:guildId guild-id
                        :channelId channel-id
                        :authorId "discord-voice-room"
                        :content "Raw Discord voice audio window(s) are attached. Do not require ASR; perceive the audio directly if the model supports it."
                        :summary (str "Discord voice audio event with " (count windows) " window(s).")
                        :content_parts windows}}]
    (-> (js/fetch (str (knoxx-base config) "/api/admin/config/events/dispatch")
                  #js {:method "POST"
                       :headers (knoxx-control-headers config)
                       :body (.stringify js/JSON (clj->js body))})
        (.then (fn [resp]
                 (if (.-ok resp)
                   (.json resp)
                   (-> (.text resp)
                       (.then (fn [text]
                                (throw (js/Error. (str "HTTP " (.-status resp) ": " text)))))))))
        (.then (fn [_]
                 {:event_id event-id :windows (count windows)})))))

(defn- run-agent-event-loop-step! [config loop-state]
  (let [queue (or (aget loop-state "audioWindows") #js [])]
    (if (or (aget loop-state "running") (zero? (.-length queue)))
      (js/Promise.resolve nil)
      (let [max-windows (aget loop-state "maxWindowsPerTurn")
            n (min (.-length queue) max-windows)
            windows (vec (array-seq (.splice queue 0 n)))]
        (aset loop-state "running" true)
        (-> (trigger-agent-voice-event! config loop-state windows)
            (.catch (fn [err]
                      ;; This is the trigger condition: only fire when the agent's own
                      ;; session is idle. If direct/start says it is not idle, preserve
                      ;; the audio windows and let the next cadence retry. Other errors
                      ;; are real failures and should not create an infinite retry loop.
                      (let [message (str (.-message err))
                            busy? (or (str/includes? message "agent_already_processing")
                                      (str/includes? message "already processing"))]
                        (when busy?
                          (requeue-front! queue windows))
                        (js/console.log "[voice:agent-event] trigger not accepted:" message))))
            (.finally (fn []
                        (aset loop-state "running" false))))))))

(defn- schedule-agent-event-loop! [config m loop-state]
  (when-not (aget loop-state "stopped")
    (aset loop-state "timer"
          (js/setTimeout
           (fn []
             (-> (run-agent-event-loop-step! config loop-state)
                 (.finally (fn []
                             (schedule-agent-event-loop! config m loop-state)))))
           (aget loop-state "tickMs")))))

(defn- stop-agent-event-loop! [m]
  (when-let [loop-state (when m (aget m "__voiceAgentEventLoop"))]
    (aset loop-state "stopped" true)
    (when-let [timer (aget loop-state "timer")]
      (js/clearTimeout timer))
    (when-let [stop (aget loop-state "listenerStop")]
      (try (stop) (catch js/Error _)))
    (aset m "__voiceAgentEventLoop" nil)
    (aset m "__voiceListener" nil)))

(defn- start-agent-event-voice-listener!
  [config m g ch sid cid auto? params on-update]
  (let [tick-ms (max 250 (param-int params "tick_ms" "tickMs" 1000))
        max-windows (max 1 (or (param-int params "max_windows_per_event" "maxWindowsPerEvent" nil)
                               (param-int params "max_windows_per_turn" "maxWindowsPerTurn" 3)))
        model-id (or (aget params "model_id") (aget params "modelId") "")]
    (stop-agent-event-loop! m)
    (maybe-tool-update! on-update (str "Listening in guild " g " as agent-event voice trigger…"))
    (js/console.log "[voice:tool] discord.voice.listen agent-event guild:" g "session:" sid "conv:" cid "auto-detect?" auto?)
    (-> (.startVoiceListener
         m g
         (fn [uid]
           (js/console.log "[voice:agent-event] speaker start:" uid))
         (fn [uid buf]
           (js/console.log "[voice:agent-event] audio window:" uid "bytes:" (.-length buf))
           (when-let [loop-state (aget m "__voiceAgentEventLoop")]
             (.push (aget loop-state "audioWindows") (audio-window-content-part uid buf)))))
        (.then (fn [stop]
                 (let [loop-state #js {:guildId g
                                        :channelId ch
                                        :sessionId sid
                                        :conversationId cid
                                        :modelId model-id
                                        :tickMs tick-ms
                                        :maxWindowsPerTurn max-windows
                                        :audioWindows #js []
                                        :running false
                                        :stopped false
                                        :listenerStop stop}]
                   (aset m "__voiceAgentEventLoop" loop-state)
                   (aset m "__voiceListener" stop)
                   (schedule-agent-event-loop! config m loop-state)
                   (tool-text-result (str "Listening in guild " g " as agent-event trigger for session " sid)
                                     {:guildId g :listening true :mode "agent_event"
                                      :sessionId sid :conversationId cid :tickMs tick-ms})))))))

(defn- start-asr-steer-voice-listener!
  [config m g sid auto? steer-debounce-ms on-update]
  (stop-agent-event-loop! m)
  (maybe-tool-update! on-update (str "Listening in guild " g "…"))
  (js/console.log "[voice:tool] discord.voice.listen guild:" g "session:" sid "auto-detect?" auto?)
  (-> (.startVoiceListener
       m g
       (fn [uid]
         (js/console.log "[voice:tool] >>> on-start callback fired for user:" uid)
         (let [buf-obj (when m (aget m "__voiceTranscriptionBuffer"))
               user-buf (when buf-obj (aget buf-obj uid))]
           (when user-buf
             (when-let [t (aget user-buf "timer")]
               (js/clearTimeout t)
               (aset user-buf "timer" nil)))))
       (fn [uid buf]
         (js/console.log "[voice:tool] >>> on-audio callback fired for user:" uid "buffer length:" (.-length buf) "bytes")
         (-> (transcribe! config buf)
             (.then (fn [t]
                      (js/console.log "[voice:tool] transcription result for" uid ":" (if (str/blank? t) "[EMPTY]" t))
                      (when-not (str/blank? t)
                        (let [buf-obj (when m (aget m "__voiceTranscriptionBuffer"))
                              _ (when (and m (not buf-obj))
                                  (aset m "__voiceTranscriptionBuffer" #js {}))
                              buf-obj (or buf-obj (aget m "__voiceTranscriptionBuffer"))
                              user-buf (or (aget buf-obj uid) #js {:texts #js [] :timer nil})]
                          (.push (aget user-buf "texts") t)
                          (aset buf-obj uid user-buf)
                          (when-let [old-timer (aget user-buf "timer")]
                            (js/clearTimeout old-timer))
                          (let [new-timer (js/setTimeout
                                           #(flush-voice-buffer! config sid (aget (aget m "__voiceSessionContext") "conversationId") uid)
                                           steer-debounce-ms)]
                            (aset user-buf "timer" new-timer))))))
             (.catch (fn [e]
                       (js/console.error "[voice:tool] transcription/steering pipeline FAILED for" uid ":" (.-message e)))))))
      (.then (fn [stop]
               (aset m "__voiceListener" stop)
               (tool-text-result (str "Listening in guild " g ". Transcriptions → session " sid)
                                 {:guildId g :listening true :mode "asr_steer"})))))

(defn voice-listen-execute
  [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        m (gw)
        g (or (aget params "guild_id") (aget params "guildId") "")
        {:keys [sid cid auto?]} (resolve-session-context! params)
        steer-debounce-ms 1500]
    (when-not m (throw (js/Error. "Gateway not started")))
    (when (str/blank? g) (throw (js/Error. "guild_id required")))
    (ensure-session-context! sid cid)
    (aset m "__voiceSessionContext" #js {:sessionId sid :conversationId cid})
    (when-not (aget m "__voiceTranscriptionBuffer")
      (aset m "__voiceTranscriptionBuffer" #js {}))
    (start-asr-steer-voice-listener! config m g sid auto? steer-debounce-ms on-update)))

(defn voice-connect-execute
  [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        m (gw)
        ch (or (aget params "channel_id") (aget params "channelId") "")
        {:keys [sid cid auto?]} (resolve-session-context! params)]
    (when-not m (throw (js/Error. "Gateway not started")))
    (when (str/blank? ch) (throw (js/Error. "channel_id required")))
    (ensure-session-context! sid cid)
    ;; Persist session context on the gateway manager so async listener callbacks
    ;; can still resolve it even if the global agent context has been cleared.
    (when m
      (aset m "__voiceSessionContext" #js {:sessionId sid :conversationId cid}))
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

(def voice-agent-event-connect-params
  [:map
   [:channel_id {:description "Discord voice channel ID to join and emit raw-audio events from."} :string]
   [:tick_ms {:optional true :description "Mechanical event-dispatch cadence in ms. Default 1000."} :int]
   [:max_windows_per_event {:optional true :description "Maximum queued WAV windows per emitted event. Default 3."} :int]])

(def voice-agent-event-listen-params
  [:map
   [:guild_id {:description "Guild ID with an active voice connection."} :string]
   [:channel_id {:optional true :description "Voice channel ID used as the event owner/filter key."} :string]
   [:tick_ms {:optional true :description "Mechanical event-dispatch cadence in ms. Default 1000."} :int]
   [:max_windows_per_event {:optional true :description "Maximum queued WAV windows per emitted event. Default 3."} :int]])

(defn voice-agent-event-listen-execute
  [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        m (gw)
        g (or (aget params "guild_id") (aget params "guildId") "")
        ch (or (aget params "channel_id") (aget params "channelId") "")
        {:keys [sid cid auto?]} (resolve-session-context! params)]
    (when-not m (throw (js/Error. "Gateway not started")))
    (when (str/blank? g) (throw (js/Error. "guild_id required")))
    (start-agent-event-voice-listener! config m g ch sid cid auto? params on-update)))

(defn voice-agent-event-connect-execute
  [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        m (gw)
        ch (or (aget params "channel_id") (aget params "channelId") "")]
    (when-not m (throw (js/Error. "Gateway not started")))
    (when (str/blank? ch) (throw (js/Error. "channel_id required")))
    (maybe-tool-update! on-update (str "Connecting voice event source for channel " ch "…"))
    (-> (.joinVoice m ch)
        (.then (fn [r]
                 (let [guild-id (or (aget r "guildId") "")]
                   (when (str/blank? guild-id)
                     (throw (js/Error. "joinVoice did not return guildId")))
                   (voice-agent-event-listen-execute runtime config _tool-call-id
                                                     (clj->js {:guild_id guild-id
                                                               :channel_id ch
                                                               :tick_ms (or (aget params "tick_ms")
                                                                            (aget params "tickMs"))
                                                               :max_windows_per_event (or (aget params "max_windows_per_event")
                                                                                          (aget params "maxWindowsPerEvent"))})
                                                     a b c)))))))

(def voice-agent-event-connect-tool
  (partial create-tool-obj
           "discord.voice.agent_event.connect" "Voice Agent Event Connect"
           "Join voice and emit raw Discord audio as real Knoxx event-agent events."
           "Contract-only voice event source for agents that perceive raw audio through event-agent runs; no ASR and no direct provider loop."
           (clj->js ["This is for contracts that explicitly grant :cap/voice-audio-event."
                     "It emits discord.voice.audio.window events; matching event-agent contracts decide what to do."
                     "Do not use this as a general replacement for discord.voice.connect."])
           voice-agent-event-connect-params
           voice-agent-event-connect-execute))

(def voice-agent-event-listen-tool
  (partial create-tool-obj
           "discord.voice.agent_event.listen" "Voice Agent Event Listen"
           "Emit raw Discord audio as real Knoxx event-agent events for an existing voice connection."
           "Contract-only voice event source; no ASR and no direct provider loop."
           (clj->js ["Use only after joining voice."
                     "Emits discord.voice.audio.window events for matching event-agent contracts."])
           voice-agent-event-listen-params
           voice-agent-event-listen-execute))

(def voice-stop-listen-params
  [:map
   [:guild_id {:description "Guild ID with an active voice connection."} :string]])

(defn voice-stop-listen-execute [runtime config _tool-call-id params a b c]
  (let [m (gw)
        g (or (aget params "guild_id") (aget params "guildId") "")]
    (when m
      (stop-agent-event-loop! m)
      (when-let [sf (aget m "__voiceListener")] (sf))
      (aset m "__voiceListener" nil)
      (aset m "__voiceSessionContext" nil))
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
                (when (ok? "discord.voice.agent_event.connect") (voice-agent-event-connect-tool runtime config))
                (when (ok? "discord.voice.agent_event.listen") (voice-agent-event-listen-tool runtime config))
                (when (ok? "discord.voice.stop_listen") (voice-stop-listen-tool runtime config))
                (when (ok? "discord.voice.list_members") (voice-list-members-tool runtime config))]))))))

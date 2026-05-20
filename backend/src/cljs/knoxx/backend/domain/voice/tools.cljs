(ns knoxx.backend.domain.voice.tools
  "Voice synthesis: OpenUtau (sung) + Voxx Gateway TTS (spoken)."
  (:require [clojure.string :as str]
            [promesa.core :as p]
            [knoxx.backend.infra.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.domain.text :refer [tool-text-result]]
            ["node:fs/promises" :as fs-promises]
            ["node:path" :as path]
            [knoxx.backend.domain.media :as media :refer [normalize-tool-path-arg]]
            [knoxx.backend.domain.openutau.tools :as openutau]
            [knoxx.backend.domain.tools :refer [maybe-tool-update! create-tool-obj]]
            [knoxx.backend.infra.document-state :refer [normalize-relative-path]]))

;; --- shared helpers ---

(defn- blank->nil [v]
  (let [s (str/trim (str (or v "")))] (when-not (str/blank? s) s)))

(defn- config-value
  [config keyword-key js-key camel-key]
  (or (when (map? config) (get config keyword-key))
      (when-not (map? config) (aget config js-key))
      (when-not (map? config) (aget config camel-key))))

(defn- false-like? [v]
  (or (= false v)
      (contains? #{"0" "false" "no" "off"}
                 (str/lower-case (str/trim (str v))))))

(defn- bool-value [v default]
  (if (nil? v) default (not (false-like? v))))

(defn- config-bool-value
  [config keyword-key js-key camel-key default]
  (let [v (if (map? config)
            (get config keyword-key ::missing)
            (let [kebab (aget config js-key)
                  camel (aget config camel-key)]
              (cond
                (some? kebab) kebab
                (some? camel) camel
                :else ::missing)))]
    (if (= ::missing v) default (bool-value v default))))

(defn- default-tts-speed [config]
  (or (blank->nil (config-value config :voxx-default-speed "voxx-default-speed" "voxxDefaultSpeed"))
      (some-> js/process .-env (aget "KNOXX_VOXX_DEFAULT_SPEED") blank->nil)
      (some-> js/process .-env (aget "VOICE_GATEWAY_TTS_DEFAULT_SPEED") blank->nil)
      "1.15"))

(defn- resolve-voice-key [config]
  (or (blank->nil (config-value config :voxx-api-key "voxx-api-key" "voxxApiKey"))
      (some-> js/process .-env (aget "VOICE_GATEWAY_API_KEY") blank->nil)
      (some-> js/process .-env (aget "KNOXX_VOICE_GATEWAY_API_KEY") blank->nil)))

(defn- voice-gateway-url [config]
  (or (blank->nil (config-value config :voxx-url "voxx-url" "voxxUrl"))
      (some-> js/process .-env (aget "VOXX_URL") blank->nil)
      "http://127.0.0.1:8787"))

(defn- tts-url [config]
  (let [base (str/replace (voice-gateway-url config) #"/+$" "")]
    (cond
      (str/ends-with? base "/v1/audio/speech") base
      (str/ends-with? base "/v1") (str base "/audio/speech")
      :else (str base "/v1/audio/speech"))))

(defn- voice-settings-payload [params]
  (cond-> {}
    (aget params "stability")        (assoc :stability        (aget params "stability"))
    (aget params "similarity_boost") (assoc :similarity_boost (aget params "similarity_boost"))))

(defn- tts-body [text voice-id model-id output-format params options]
  (let [vs (voice-settings-payload params)
        {:keys [speed postprocess-profile postprocess-enabled prompt-aware prompt-aware-style]} options]
    (cond-> {:input text
             :voice voice-id
             :model model-id
             :response_format output-format
             :speed speed
             :postprocess_enabled postprocess-enabled
             :prompt_aware prompt-aware}
      postprocess-profile (assoc :postprocess_profile postprocess-profile)
      prompt-aware-style  (assoc :prompt_aware_style prompt-aware-style)
      (seq vs)            (assoc :voice_settings vs))))

;; --- voice.tts ---

(defn- tts-default-output-path
  "Generate a default output path in Voice/ when none is provided."
  []
  (let [ts (.toISOString (js/Date.))
        safe-ts (str/replace ts #"[:.]" "-")]
    (str "Voice/tts-" safe-ts ".mp3")))

(def tts-rest-params
  [:map
   [:text {:description "Plain text. Strip markdown first."} :string]
   [:output_path {:optional true :description "Workspace-relative output path. Defaults to Voice/tts-<timestamp>.mp3. Use Voice/ for spoken output, Audio/ for clips and effects, Music/ for musical content."} :string]
   [:voice_id {:optional true :description "Voxx/Kokoro voice ID. Default: af_jessica."} :string]
   [:model_id {:optional true :description "Voxx backend hint/model. Default: kokoro. Voxx may fall back by VOICE_GATEWAY_TTS_BACKEND_ORDER: xiaomi_mimo, kokoro; eSpeak is opt-in only."} :string]
   [:output_format {:optional true :description "Audio format. Default mp3."} :string]
   [:speed {:optional true :description "Speech speed multiplier. Default 1.15 for the af_jessica workspace voice."} [:double {:min 0.25 :max 4.0}]]
   [:postprocess_profile {:optional true :description "Final Voxx mastering profile. Default sports-commentator-v1. Aliases: sports/commentator, broadcast/warm, narrator/polish, radio/crisp, soft/studio; off/none disables."} :string]
   [:postprocess_enabled {:optional true :description "Enable final Voxx postprocess. Default true; set false for dry capture."} :boolean]
   [:prompt_aware {:optional true :description "Prompt-aware performance mode. Default true. Voxx consumes tags like [excited], [whisper], [pause], [dramatic], [laugh], and <break time=\"500ms\" /> as segment-level postprocessing directions, not spoken words."} :boolean]
   [:prompt_aware_style {:optional true :description "Optional custom instruction for how Voxx should interpret performance tags."} :string]
   [:stability {:optional true :description "Stability 0-1 for compatible providers."} [:double {:min 0 :max 1}]]
   [:similarity_boost {:optional true :description "Similarity boost 0-1 for compatible providers."} [:double {:min 0 :max 1}]]])

(defn- fetch-tts-audio! [url api-key body]
  (p/let [res (js/fetch url #js {:method  "POST"
                                :headers #js {"Authorization" (str "Bearer " api-key)
                                              "Content-Type" "application/json"
                                              "Accept"       "audio/mpeg"}
                                :body    (.stringify js/JSON (clj->js body))})
         _   (when-not (.-ok res)
               (p/let [msg (.text res)]
                 (throw (js/Error. (str "Voice Gateway " (.-status res) ": " msg)))))
         arr (.arrayBuffer res)]
    (.from js/Buffer (js/Uint8Array. arr))))

(defn- write-audio-file! [node-path buf absolute relative voice-id model-id fmt]
  (p/do
    (.mkdir fs-promises (.dirname node-path absolute) #js {:recursive true})
    (.writeFile fs-promises absolute buf)
    (tool-text-result
     (str "Wrote " relative " (" (.-length buf) " bytes). Use workspace_media.attach to embed.")
     {:path relative :bytes (.-length buf) :voice_id voice-id :model_id model-id :format fmt})))

(defn tts-rest-execute [runtime config]
  (fn [_call-id params on-update & _]
    (let [text      (or (blank->nil (aget params "text")) (throw (js/Error. "voice.tts: text required")))
          api-key   (or (resolve-voice-key config) (throw (js/Error. "voice.tts: VOICE_GATEWAY_API_KEY not configured")))
          voice-id  (or (blank->nil (aget params "voice_id"))
                        (blank->nil (config-value config :voxx-voice-id "voxx-voice-id" "voxxVoiceId"))
                        "af_jessica")
          model-id  (or (blank->nil (aget params "model_id"))
                        (blank->nil (config-value config :voxx-model-id "voxx-model-id" "voxxModelId"))
                        "kokoro")
          speed     (or (blank->nil (aget params "speed")) (default-tts-speed config))
          out-fmt   (or (blank->nil (aget params "output_format")) "mp3")
          postprocess-profile (or (blank->nil (aget params "postprocess_profile"))
                                  (blank->nil (config-value config :voxx-postprocess-profile "voxx-postprocess-profile" "voxxPostprocessProfile"))
                                  "sports-commentator-v1")
          postprocess-enabled (if (some? (aget params "postprocess_enabled"))
                                (bool-value (aget params "postprocess_enabled") true)
                                (config-bool-value config :voxx-postprocess-enabled "voxx-postprocess-enabled" "voxxPostprocessEnabled" true))
          prompt-aware (if (some? (aget params "prompt_aware"))
                         (bool-value (aget params "prompt_aware") true)
                         (config-bool-value config :voxx-prompt-aware "voxx-prompt-aware" "voxxPromptAware" true))
           prompt-aware-style (or (blank->nil (aget params "prompt_aware_style"))
                                  (blank->nil (config-value config :voxx-prompt-aware-style "voxx-prompt-aware-style" "voxxPromptAwareStyle")))
           out-path  (or (blank->nil (aget params "output_path"))
                         (tts-default-output-path))
           {:keys [absolute relative]} (media/resolve-workspace-media-path runtime config out-path)
           options   {:speed speed
                      :postprocess-profile postprocess-profile
                      :postprocess-enabled postprocess-enabled
                      :prompt-aware prompt-aware
                      :prompt-aware-style prompt-aware-style}]
      (maybe-tool-update! on-update (str "TTS: " (count text) " chars -> " relative
                                         " via " model-id ", voice=" voice-id ", speed=" speed
                                         ", postprocess=" (if postprocess-enabled postprocess-profile "off")
                                         ", prompt-aware=" prompt-aware "..."))
      (p/let [buf (fetch-tts-audio! (tts-url config) api-key (tts-body text voice-id model-id out-fmt params options))]
        (write-audio-file! path buf absolute relative voice-id model-id out-fmt)))))

;; --- voice.tts_stream ---

(def tts-stream-params
  [:map
   [:text {:description "Text to synthesize via /ws/voice/tts."} :string]
   [:voice_id {:optional true :description "Voxx/Kokoro voice ID. Default: af_jessica."} :string]
   [:model_id {:optional true :description "Voxx backend hint/model. Default: kokoro; fallback order is controlled by Voxx. eSpeak is opt-in only in the workspace default."} :string]
   [:output_format {:optional true :description "Format. Default: mp3."} :string]
   [:speed {:optional true :description "Speech speed multiplier. Default 1.15."} [:double {:min 0.25 :max 4.0}]]
   [:postprocess_profile {:optional true :description "Final Voxx mastering profile. Default sports-commentator-v1. Aliases: sports, broadcast, narrator, radio, soft; off disables."} :string]
   [:postprocess_enabled {:optional true :description "Enable final Voxx postprocess. Default true."} :boolean]
   [:prompt_aware {:optional true :description "Prompt-aware tag mode. Default true; Voxx consumes tags as segment-level postprocessing directions."} :boolean]
   [:prompt_aware_style {:optional true :description "Optional custom instruction for tag interpretation."} :string]
   [:auto_mode {:optional true :description "auto_mode. Default true."} :boolean]])

(def openutau-project-params
  [:map
   [:project_name {:description "Project name."} :string]
   [:notes {:description "Ordered note plan."}
    [:vector
     [:map
      [:lyric {:optional true :description "Lyric. Use + or +~ for slurs."} :string]
      [:phonetic_hint {:optional true :description "Phonetic hint without brackets."} :string]
      [:tone {:description "MIDI note number. C4 = 60."} :int]
      [:duration {:description "Duration in ticks. 480 = 1 quarter note."} :int]
      [:position {:optional true :description "Start tick. Sequential if omitted."} :int]]]]
   [:tempo {:optional true :description "BPM. Default 120."} :int]
   [:time_signature {:optional true}
    [:map
     [:beat_per_bar {:optional true :description "Numerator."} :int]
     [:beat_unit {:optional true :description "Denominator."} :int]]]
    [:singer_id {:optional true :description "Singer ID. Options: teto (Kasane Teto JA), ritsu (Namine Ritsu JA), teto-en (Kasane Teto EN). Default: teto."} :string]
    [:phonemizer {:optional true :description "Phonemizer class. Usually auto-selected by singer."} :string]
   [:track_name {:optional true :description "Vocal track name."} :string]
   [:part_name {:optional true :description "Voice part name."} :string]
   [:output_path {:optional true :description "Output .ustx path."} :string]
   [:comment {:optional true :description "Project comment."} :string]])

(defn tts-stream-execute [config]
  (fn [_call-id params on-update & _]
    (let [voice-id  (or (blank->nil (aget params "voice_id"))
                        (blank->nil (config-value config :voxx-voice-id "voxx-voice-id" "voxxVoiceId"))
                        "af_jessica")
          model-id  (or (blank->nil (aget params "model_id"))
                        (blank->nil (config-value config :voxx-model-id "voxx-model-id" "voxxModelId"))
                        "kokoro")
          speed     (or (blank->nil (aget params "speed")) (default-tts-speed config))
          out-fmt   (or (blank->nil (aget params "output_format")) "mp3")
          postprocess-profile (or (blank->nil (aget params "postprocess_profile"))
                                  (blank->nil (config-value config :voxx-postprocess-profile "voxx-postprocess-profile" "voxxPostprocessProfile"))
                                  "sports-commentator-v1")
          postprocess-enabled (if (some? (aget params "postprocess_enabled"))
                                (bool-value (aget params "postprocess_enabled") true)
                                (config-bool-value config :voxx-postprocess-enabled "voxx-postprocess-enabled" "voxxPostprocessEnabled" true))
          prompt-aware (if (some? (aget params "prompt_aware"))
                         (bool-value (aget params "prompt_aware") true)
                         (config-bool-value config :voxx-prompt-aware "voxx-prompt-aware" "voxxPromptAware" true))
          prompt-aware-style (or (blank->nil (aget params "prompt_aware_style"))
                                 (blank->nil (config-value config :voxx-prompt-aware-style "voxx-prompt-aware-style" "voxxPromptAwareStyle")))
          auto-mode (not= false (aget params "auto_mode"))
          key-ok?   (boolean (resolve-voice-key config))]
      (maybe-tool-update! on-update "voice.tts_stream: returning WS params...")
      (p/resolved
       (tool-text-result
        (cond-> "Connect to /ws/voice/tts. Send {type:start,...}, then {type:text,text:...} chunks, then {type:flush}. Include postprocess_profile/postprocess_enabled/prompt_aware in the start message or query. Receive {type:audio,audio:<base64>} chunks."
          (not key-ok?) (str " WARNING: VOICE_GATEWAY_API_KEY is not configured."))
        {:ws_endpoint "/ws/voice/tts" :voice_id voice-id :model_id model-id
         :speed speed
         :output_format out-fmt :auto-mode auto-mode :api_key_configured key-ok?
         :postprocess_profile (if postprocess-enabled postprocess-profile "none")
         :postprocess_enabled postprocess-enabled
         :prompt_aware prompt-aware
         :prompt_aware_style prompt-aware-style})))))

;; --- voice.openutau_project ---

(defn openutau-project-execute [runtime config _call-id params on-update & _]
  (let [project-name (or (normalize-tool-path-arg (aget params "project_name")) "Knoxx OpenUtau Project")
        out-path     (or (normalize-tool-path-arg (aget params "output_path"))
                         (openutau/default-project-relative-path project-name))
        {:keys [workspace-root absolute relative]} (media/resolve-workspace-media-path runtime config out-path)
        output-dir   (.dirname path absolute)
        filename     (media/path-basename path absolute)
        readme-abs   (.join path output-dir "README.md")
        readme-rel   (normalize-relative-path (media/path-relative path workspace-root readme-abs))
        notes        (openutau/normalize-notes (js->clj (or (aget params "notes") #js []) :keywordize-keys true))
        project      (openutau/build-project {:project_name   (aget params "project_name")
                                              :tempo          (aget params "tempo")
                                              :time_signature (js->clj (or (aget params "time_signature") #js {}) :keywordize-keys true)
                                              :singer_id      (aget params "singer_id")
                                              :phonemizer     (aget params "phonemizer")
                                              :track_name     (aget params "track_name")
                                              :part_name      (aget params "part_name")
                                              :comment        (aget params "comment")}
                                             notes)
        ustx-yaml    (openutau/project->ustx-yaml project)
        readme-text  (openutau/readme-markdown {:project-name project-name :ustx-path relative
                                                :readme-path readme-rel    :note-count (count notes)
                                                :tempo (or (aget params "tempo") 120)
                                                :singer-id (or (aget params "singer_id") "")
                                                :phonemizer (or (aget params "phonemizer") "")})
        data-url     (str "data:text/yaml;base64," (.toString (.from js/Buffer ustx-yaml "utf8") "base64"))]
    (when-not (seq notes) (throw (js/Error. "notes must contain at least one note")))
    (maybe-tool-update! on-update (str "Writing OpenUtau project " relative "..."))
    (p/do
      (.mkdir fs-promises output-dir #js {:recursive true})
      (.writeFile fs-promises absolute ustx-yaml "utf8")
      (.writeFile fs-promises readme-abs readme-text "utf8")
      (tool-text-result
       (str "Created OpenUtau project at " relative " with " (count notes) " notes.")
       {:path relative :readme_path readme-rel :project_name project-name
        :note_count (count notes) :tempo (or (aget params "tempo") 120)
        :renderer openutau/default-renderer :headless_render_supported true
       :content_parts [{:type "document" :data data-url :mimeType "text/yaml" :filename filename}]}))))

(defn voice-openutau-project-execute [runtime config call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))]
    (openutau-project-execute runtime config call-id params on-update)))

(defn voice-tts-execute [runtime config & args]
  (apply (tts-rest-execute runtime config) args))

(defn voice-tts-stream-execute [runtime config & args]
  (apply (tts-stream-execute config) args))

(def voice-openutau-project-tool
  (partial create-tool-obj
           "voice.openutau_project"
           "OpenUtau Project"
           "Create an OpenUtau .ustx singing project."
           "Use for lyric-timed vocal synthesis via OpenUtau."
           ["Provide notes with lyric, tone, duration."
            "Default singer: Kasane Teto (重音テト). Default phonemizer: JapaneseCV."
            "Use voice.openutau_render to headlessly render .ustx to .wav."]
           openutau-project-params
           voice-openutau-project-execute))

(def openutau-render-params
  [:map
   [:ustx_path {:description "Path to the .ustx file to render."} :string]
   [:output_path {:optional true :description "Output .wav path. Defaults to same dir as .ustx."} :string]])

(defn voice-openutau-render-execute [runtime config _call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        ustx-path (normalize-tool-path-arg (aget params "ustx_path"))
        output-path (or (when-let [p (aget params "output_path")]
                          (normalize-tool-path-arg p))
                        (str (media/path-basename path ustx-path) ".wav"))
        {:keys [absolute]} (media/resolve-workspace-media-path runtime config output-path)]
    (when-not ustx-path (throw (js/Error. "ustx_path is required")))
    (maybe-tool-update! on-update (str "Rendering " ustx-path " to WAV..."))
    (-> (openutau/render-ustx-to-wav ustx-path absolute)
        (.then (fn [result]
                 (tool-text-result
                  (str "Rendered OpenUTAU project to " output-path)
                  {:wav_path output-path
                   :stdout (:stdout result)})))
        (.catch (fn [err]
                  (throw (js/Error. (str "Render failed: " (.-message err)))))))))

(def voice-openutau-render-tool
  (partial create-tool-obj
           "voice.openutau_render"
           "OpenUtau Render"
           "Headlessly render an OpenUtau .ustx project to WAV."
           "Requires the OpenUTAU headless pipeline (Xvfb + WORLDLINE-R + voicebank)."
           ["Pass the path to a .ustx file generated by voice.openutau_project."
            "Renders using the singer and phonemizer configured in the project."
            "Outputs 16-bit mono 44100Hz PCM WAV."]
           openutau-render-params
           voice-openutau-render-execute))

(def voice-tts-tool
  (partial create-tool-obj
           "voice.tts"
           "Text-to-Speech"
           "Synthesize spoken audio via Voxx Gateway. Defaults to prompt-aware mode plus lively final postprocess, then writes MP3 to workspace."
           "Use voice.tts for spoken audio. Default: prompt_aware=true, postprocess_profile=sports-commentator-v1, model_id=kokoro, voice_id=af_jessica, speed=1.15."
           ["Pass clean spoken copy; strip markdown formatting, but keep intentional performance tags."
            "Default mode is prompt-aware: [excited], [whisper], [laugh], [pause], [dramatic], and <break time=\"500ms\" /> are Voxx-owned performance directions, not words to speak and not markup to pass through to the provider."
            "Use tags sparingly at phrase boundaries. Bracket tags set Voxx segment-level emotion/energy filters, [pause] and <break time=\"...ms\" /> insert silence, and [laugh] inserts a short nonverbal effect."
            "Voxx consumes known performance tags, sends clean segment text to the chosen backend, stitches the segments together, then applies tag-driven inflection postprocessing plus the final mastering profile."
            "Use postprocess_profile to choose Voxx's final mastering: sports/commentator (default high energy), broadcast/warm, narrator/polish, radio/crisp, soft/studio, or off/none for dry capture."
            "eSpeak is not in the default Voxx backend order; if a voice sounds robotic, inspect x-openhax-tts-backend before assuming postprocess is the cause."
            "Use model_id as a backend hint: kokoro, xiaomi_mimo, requesty, openai, melo, or espeak; Voxx may fall back by VOICE_GATEWAY_TTS_BACKEND_ORDER."
            "Default output_format is mp3. When output_path is omitted, files save to Voice/tts-<timestamp>.mp3 automatically."
            "Use Voice/ for spoken TTS output, Audio/ for sound clips and effects, Music/ for musical or sung content."
            "Follow with workspace_media.attach to embed audio."
            "If debugging, inspect Voxx headers/logs: x-openhax-tts-backend, x-openhax-tts-postprocess-profile, and x-openhax-tts-prompt-aware."]
           tts-rest-params
           voice-tts-execute))

(def voice-tts-stream-tool
  (partial create-tool-obj
           "voice.tts_stream"
           "TTS Stream"
           "WS streaming TTS session params for /ws/voice/tts with Voxx prompt-aware and postprocess defaults."
           "Use voice.tts_stream for WS TTS connection params. Default: prompt_aware=true, postprocess_profile=sports-commentator-v1, model_id=kokoro, voice_id=af_jessica, speed=1.15."
           ["Returns WS protocol spec, default postprocess/prompt-aware settings, and API key status."
            "Send prompt_aware, prompt_aware_style, postprocess_profile, and postprocess_enabled in the start message or query when overriding defaults."
            "Use the same tag rules as voice.tts: bracket/XML-like tags are Voxx-owned postprocessing directions, not spoken text."
            "Use voice.tts when you need a persisted MP3 file."]
           tts-stream-params
           voice-tts-stream-execute))

;; --- factory ---

(defn create-voice-synth-custom-tools
  ([runtime config] (create-voice-synth-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [allowed? (fn [id] (or (nil? auth-context) (ctx-tool-allowed? auth-context id)))]
     (clj->js
      (vec
       (remove nil?
                [(when (allowed? "voice.openutau_project")
                   (voice-openutau-project-tool runtime config))
                 (when (allowed? "voice.openutau_render")
                   (voice-openutau-render-tool runtime config))
                 (when (allowed? "voice.tts")
                   (voice-tts-tool runtime config))
                 (when (allowed? "voice.tts_stream")
                   (voice-tts-stream-tool runtime config))]))))))

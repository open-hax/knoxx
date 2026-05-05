(ns knoxx.backend.tools.voice
  "Voice synthesis: OpenUtau (sung) + Voxx Gateway TTS (spoken)."
  (:require [clojure.string :as str]
            [promesa.core :as p]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.text :refer [tool-text-result]]
            ["node:fs/promises" :as fs-promises]
            [knoxx.backend.tools.media :as media :refer [normalize-tool-path-arg]]
            [knoxx.backend.tools.openutau :as openutau]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! type-optional]]
            [knoxx.backend.document-state :refer [normalize-relative-path]]))

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
        {:keys [postprocess-profile postprocess-enabled prompt-aware prompt-aware-style]} options]
    (cond-> {:input text
             :voice voice-id
             :model model-id
             :response_format output-format
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

(defn- tts-rest-params [Type]
  (.Object Type
    #js {:text          (.String Type #js {:description "Plain text. Strip markdown first."})
         :output_path   (type-optional Type (.String Type #js {:description "Workspace-relative output path. Defaults to Voice/tts-<timestamp>.mp3. Use Voice/ for spoken output, Audio/ for clips and effects, Music/ for musical content."}))
         :voice_id      (type-optional Type (.String  Type #js {:description "Voxx voice ID. Default: alloy."}))
         :model_id      (type-optional Type (.String  Type #js {:description "Voxx backend hint/model. Default: kokoro. Voxx may fall back by VOICE_GATEWAY_TTS_BACKEND_ORDER: kokoro, xiaomi_mimo, requesty, openai, melo, espeak."}))
         :output_format (type-optional Type (.String  Type #js {:description "Audio format. Default mp3."}))
         :postprocess_profile (type-optional Type (.String Type #js {:description "Final Voxx mastering profile. Default sports-commentator-v1. Aliases: sports/commentator, broadcast/warm, narrator/polish, radio/crisp, soft/studio; off/none disables."}))
         :postprocess_enabled (type-optional Type (.Boolean Type #js {:description "Enable final Voxx postprocess. Default true; set false for dry capture."}))
         :prompt_aware  (type-optional Type (.Boolean Type #js {:description "Prompt-aware performance mode. Default true. Voxx consumes tags like [excited], [whisper], [pause], [dramatic], [laugh], and <break time=\"500ms\" /> as segment-level postprocessing directions, not spoken words."}))
         :prompt_aware_style (type-optional Type (.String Type #js {:description "Optional custom instruction for how Voxx should interpret performance tags."}))
         :stability     (type-optional Type (.Number  Type #js {:description "Stability 0-1 for compatible providers."}))
         :similarity_boost (type-optional Type (.Number Type #js {:description "Similarity boost 0-1 for compatible providers."}))}))

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
                        "alloy")
          model-id  (or (blank->nil (aget params "model_id"))
                        (blank->nil (config-value config :voxx-model-id "voxx-model-id" "voxxModelId"))
                        "xiaomi_mimo")
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
           node-path (aget runtime "path")
           options   {:postprocess-profile postprocess-profile
                      :postprocess-enabled postprocess-enabled
                      :prompt-aware prompt-aware
                      :prompt-aware-style prompt-aware-style}]
      (maybe-tool-update! on-update (str "TTS: " (count text) " chars -> " relative
                                         " via " model-id ", postprocess=" (if postprocess-enabled postprocess-profile "off")
                                         ", prompt-aware=" prompt-aware "..."))
      (p/let [buf (fetch-tts-audio! (tts-url config) api-key (tts-body text voice-id model-id out-fmt params options))]
        (write-audio-file! node-path buf absolute relative voice-id model-id out-fmt)))))

;; --- voice.tts_stream ---

(defn- tts-stream-params [Type]
  (.Object Type
    #js {:text          (.String  Type #js {:description "Text to synthesize via /ws/voice/tts."})
         :voice_id      (type-optional Type (.String  Type #js {:description "Voxx voice ID. Default: alloy."}))
         :model_id      (type-optional Type (.String  Type #js {:description "Voxx backend hint/model. Default: kokoro; fallback order is controlled by Voxx."}))
         :output_format (type-optional Type (.String  Type #js {:description "Format. Default: mp3."}))
         :postprocess_profile (type-optional Type (.String Type #js {:description "Final Voxx mastering profile. Default sports-commentator-v1. Aliases: sports, broadcast, narrator, radio, soft; off disables."}))
         :postprocess_enabled (type-optional Type (.Boolean Type #js {:description "Enable final Voxx postprocess. Default true."}))
         :prompt_aware  (type-optional Type (.Boolean Type #js {:description "Prompt-aware tag mode. Default true; Voxx consumes tags as segment-level postprocessing directions."}))
         :prompt_aware_style (type-optional Type (.String Type #js {:description "Optional custom instruction for tag interpretation."}))
         :auto_mode     (type-optional Type (.Boolean Type #js {:description "auto_mode. Default true."}))}))

(defn tts-stream-execute [config]
  (fn [_call-id params on-update & _]
    (let [voice-id  (or (blank->nil (aget params "voice_id"))
                        (blank->nil (config-value config :voxx-voice-id "voxx-voice-id" "voxxVoiceId"))
                        "alloy")
          model-id  (or (blank->nil (aget params "model_id"))
                        (blank->nil (config-value config :voxx-model-id "voxx-model-id" "voxxModelId"))
                        "xiaomi_mimo")
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
         :output_format out-fmt :auto-mode auto-mode :api_key_configured key-ok?
         :postprocess_profile (if postprocess-enabled postprocess-profile "none")
         :postprocess_enabled postprocess-enabled
         :prompt_aware prompt-aware
         :prompt_aware_style prompt-aware-style})))))

;; --- voice.openutau_project ---

(defn openutau-project-execute [runtime config _call-id params on-update & _]
  (let [node-path    (aget runtime "path")
        project-name (or (normalize-tool-path-arg (aget params "project_name")) "Knoxx OpenUtau Project")
        out-path     (or (normalize-tool-path-arg (aget params "output_path"))
                         (openutau/default-project-relative-path project-name))
        {:keys [workspace-root absolute relative]} (media/resolve-workspace-media-path runtime config out-path)
        output-dir   (.dirname node-path absolute)
        filename     (media/path-basename node-path absolute)
        readme-abs   (.join node-path output-dir "README.md")
        readme-rel   (normalize-relative-path (media/path-relative node-path workspace-root readme-abs))
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
        :renderer openutau/default-renderer :headless_render_supported false
        :content_parts [{:type "document" :data data-url :mimeType "text/yaml" :filename filename}]}))))

;; --- factory ---

(defn create-voice-synth-custom-tools
  ([runtime config] (create-voice-synth-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [Type     (aget runtime "Type")
         allowed? (fn [id] (or (nil? auth-context) (ctx-tool-allowed? auth-context id)))
         note-p   (.Object Type
                    #js {:lyric         (type-optional Type (.String Type #js {:description "Lyric. Use + or +~ for slurs."}))
                         :phonetic_hint (type-optional Type (.String Type #js {:description "Phonetic hint without brackets."}))
                         :tone          (.Number Type #js {:description "MIDI note number. C4 = 60."})
                         :duration      (.Number Type #js {:description "Duration in ticks. 480 = 1 quarter note."})
                         :position      (type-optional Type (.Number Type #js {:description "Start tick. Sequential if omitted."}))})
         time-sig (.Object Type
                    #js {:beat_per_bar (type-optional Type (.Number Type #js {:description "Numerator."}))
                         :beat_unit    (type-optional Type (.Number Type #js {:description "Denominator."}))})
         ustx-p   (.Object Type
                    #js {:project_name   (.String Type #js {:description "Project name."})
                         :notes          (.Array  Type note-p #js {:description "Ordered note plan."})
                         :tempo          (type-optional Type (.Number Type #js {:description "BPM. Default 120."}))
                         :time_signature (type-optional Type time-sig)
                         :singer_id      (type-optional Type (.String Type #js {:description "Singer/voicebank folder."}))
                         :phonemizer     (type-optional Type (.String Type #js {:description "Phonemizer class/tag."}))
                         :track_name     (type-optional Type (.String Type #js {:description "Vocal track name."}))
                         :part_name      (type-optional Type (.String Type #js {:description "Voice part name."}))
                         :output_path    (type-optional Type (.String Type #js {:description "Output .ustx path."}))
                         :comment        (type-optional Type (.String Type #js {:description "Project comment."}))})
         tools    [(when (allowed? "voice.openutau_project")
                     (doto (js-obj)
                       (aset "name"            "voice.openutau_project")
                       (aset "label"           "OpenUtau Project")
                       (aset "description"     "Create an OpenUtau .ustx singing project.")
                       (aset "promptSnippet"   "Use for lyric-timed vocal synthesis via OpenUtau.")
                       (aset "promptGuidelines" (clj->js ["Provide notes with lyric, tone, duration."
                                                             "Export is UI-driven; do not claim audio rendered without a real file."
                                                             "WORLDLINE-R is the default safe renderer."]))
                       (aset "parameters" ustx-p)
                       (aset "execute"     (fn [_call-id params on-update & _] (openutau-project-execute runtime config _call-id params on-update)))))
                   (when (allowed? "voice.tts")
                     (doto (js-obj)
                       (aset "name"            "voice.tts")
                       (aset "label"           "Text-to-Speech")
                       (aset "description"     "Synthesize spoken audio via Voxx Gateway. Defaults to prompt-aware mode plus lively final postprocess, then writes MP3 to workspace.")
                       (aset "promptSnippet"   "Use voice.tts for spoken audio. Default: prompt_aware=true, postprocess_profile=sports-commentator-v1, model_id=xiaomi_mimo, voice_id=alloy.")
                       (aset "promptGuidelines" (clj->js ["Pass clean spoken copy; strip markdown formatting, but keep intentional performance tags."
                                                             "Default mode is prompt-aware: [excited], [whisper], [laugh], [pause], [dramatic], and <break time=\"500ms\" /> are Voxx-owned performance directions, not words to speak and not markup to pass through to the provider."
                                                             "Use tags sparingly at phrase boundaries. Bracket tags set Voxx segment-level emotion/energy filters, [pause] and <break time=\"...ms\" /> insert silence, and [laugh] inserts a short nonverbal effect."
                                                             "Voxx consumes known performance tags, sends clean segment text to the chosen backend, stitches the segments together, then applies tag-driven inflection postprocessing plus the final mastering profile."
                                                             "Use postprocess_profile to choose Voxx's final mastering: sports/commentator (default high energy), broadcast/warm, narrator/polish, radio/crisp, soft/studio, or off/none for dry capture."
                                                             "The inherited Melo narrator-unifier remains a local Melo stage; the Voxx final postprocess stage is backend-agnostic and gives Kokoro/remote/Melo/eSpeak livelier leveling, EQ, compression, limiting, and gain."
                                                             "Use model_id as a backend hint: kokoro, xiaomi_mimo, requesty, openai, melo, or espeak; Voxx may fall back by VOICE_GATEWAY_TTS_BACKEND_ORDER."
                                                              "Default output_format is mp3. When output_path is omitted, files save to Voice/tts-<timestamp>.mp3 automatically."
                                                              "Use Voice/ for spoken TTS output, Audio/ for sound clips and effects, Music/ for musical or sung content."
                                                              "Follow with workspace_media.attach to embed audio."
                                                              "If debugging, inspect Voxx headers/logs: x-openhax-tts-backend, x-openhax-tts-postprocess-profile, and x-openhax-tts-prompt-aware."]))
                       (aset "parameters" (tts-rest-params Type))
                       (aset "execute"     (tts-rest-execute runtime config))))
                   (when (allowed? "voice.tts_stream")
                     (doto (js-obj)
                       (aset "name"            "voice.tts_stream")
                       (aset "label"           "TTS Stream")
                       (aset "description"     "WS streaming TTS session params for /ws/voice/tts with Voxx prompt-aware and postprocess defaults.")
                       (aset "promptSnippet"   "Use voice.tts_stream for WS TTS connection params. Default: prompt_aware=true, postprocess_profile=sports-commentator-v1, model_id=xiaomi_mimo.")
                       (aset "promptGuidelines" (clj->js ["Returns WS protocol spec, default postprocess/prompt-aware settings, and API key status."
                                                             "Send prompt_aware, prompt_aware_style, postprocess_profile, and postprocess_enabled in the start message or query when overriding defaults."
                                                             "Use the same tag rules as voice.tts: bracket/XML-like tags are Voxx-owned postprocessing directions, not spoken text."
                                                             "Use voice.tts when you need a persisted MP3 file."]))
                       (aset "parameters" (tts-stream-params Type))
                       (aset "execute"     (tts-stream-execute config))))]]
     (clj->js (vec (remove nil? tools))))))

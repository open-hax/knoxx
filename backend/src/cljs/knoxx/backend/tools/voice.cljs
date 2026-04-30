(ns knoxx.backend.tools.voice
  "Voice synthesis: OpenUtau (sung) + Voxx Gateway TTS (spoken)."
  (:require [clojure.string :as str]
            [promesa.core :as p]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.text :refer [tool-text-result]]
            ["node:fs" :as node-fs]
            [knoxx.backend.tools.media :as media :refer [normalize-tool-path-arg]]
            [knoxx.backend.tools.openutau :as openutau]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! type-optional]]
            [knoxx.backend.document-state :refer [normalize-relative-path]]))

;; --- shared helpers ---

(defn- blank->nil [v]
  (let [s (str/trim (str (or v "")))] (when-not (str/blank? s) s)))

(defn- resolve-voice-key [config]
  (or (blank->nil (aget config "voxx-api-key"))
      (blank->nil (aget config "voxxApiKey"))
      (some-> js/process .-env (aget "VOICE_GATEWAY_API_KEY") blank->nil)
      (some-> js/process .-env (aget "KNOXX_VOICE_GATEWAY_API_KEY") blank->nil)))

(defn- voice-gateway-url [config]
  (or (blank->nil (aget config "voxx-url"))
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

(defn- tts-body [text voice-id model-id output-format params]
  (let [vs (voice-settings-payload params)]
    (cond-> {:input text
             :voice voice-id
             :model model-id
             :response_format output-format}
      (seq vs) (assoc :voice_settings vs))))

;; --- voice.tts ---

(defn- tts-rest-params [Type]
  (.Object Type
    #js {:text          (.String Type #js {:description "Plain text. Strip markdown first."})
         :output_path   (.String Type #js {:description "Workspace-relative MP3 path, e.g. Music/out.mp3."})
         :voice_id      (type-optional Type (.String  Type #js {:description "Voxx voice ID. Default: alloy."}))
         :model_id      (type-optional Type (.String  Type #js {:description "Voxx provider/model. Default: kokoro. Examples: kokoro, melo, espeak, xiaomi_mimo, requesty, openai."}))
         :output_format (type-optional Type (.String  Type #js {:description "Audio format. Default mp3."}))
         :stability     (type-optional Type (.Number  Type #js {:description "Stability 0-1."}))
         :similarity_boost (type-optional Type (.Number Type #js {:description "Similarity boost 0-1."}))}))

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
    (.mkdir node-fs (.dirname node-path absolute) #js {:recursive true})
    (.writeFile node-fs absolute buf)
    (tool-text-result
     (str "Wrote " relative " (" (.-length buf) " bytes). Use workspace_media.attach to embed.")
     {:path relative :bytes (.-length buf) :voice_id voice-id :model_id model-id :format fmt})))

(defn tts-rest-execute [runtime config]
  (fn [_call-id params on-update & _]
    (let [text      (or (blank->nil (aget params "text")) (throw (js/Error. "voice.tts: text required")))
          api-key   (or (resolve-voice-key config) (throw (js/Error. "voice.tts: VOICE_GATEWAY_API_KEY not configured")))
          voice-id  (or (blank->nil (aget params "voice_id"))     "alloy")
          model-id  (or (blank->nil (aget params "model_id"))     "kokoro")
          out-fmt   (or (blank->nil (aget params "output_format")) "mp3")
          out-path  (blank->nil (aget params "output_path"))
          {:keys [absolute relative]} (when out-path (media/resolve-workspace-media-path runtime config out-path))
          node-path (aget runtime "path")]
      (maybe-tool-update! on-update (str "TTS: " (count text) " chars -> " (or relative "buffer") "..."))
      (p/let [buf (fetch-tts-audio! (tts-url config) api-key (tts-body text voice-id model-id out-fmt params))]
        (if absolute
          (write-audio-file! node-path buf absolute relative voice-id model-id out-fmt)
          (tool-text-result (str "TTS done (" (.-length buf) " bytes). Provide output_path to save.")
                            {:bytes (.-length buf) :voice_id voice-id :model_id model-id :format out-fmt}))))))

;; --- voice.tts_stream ---

(defn- tts-stream-params [Type]
  (.Object Type
    #js {:text          (.String  Type #js {:description "Text to synthesize via /ws/voice/tts."})
         :voice_id      (type-optional Type (.String  Type #js {:description "Voxx voice ID. Default: alloy."}))
         :model_id      (type-optional Type (.String  Type #js {:description "Voxx provider/model. Default: kokoro."}))
         :output_format (type-optional Type (.String  Type #js {:description "Format. Default: mp3."}))
         :auto_mode     (type-optional Type (.Boolean Type #js {:description "auto_mode. Default true."}))}))

(defn tts-stream-execute [config]
  (fn [_call-id params on-update & _]
    (let [voice-id  (or (blank->nil (aget params "voice_id"))     "alloy")
          model-id  (or (blank->nil (aget params "model_id"))     "kokoro")
          out-fmt   (or (blank->nil (aget params "output_format")) "mp3")
          auto-mode (not= false (aget params "auto_mode"))
          key-ok?   (boolean (resolve-voice-key config))]
      (maybe-tool-update! on-update "voice.tts_stream: returning WS params...")
      (p/resolved
       (tool-text-result
        (cond-> "Connect to /ws/voice/tts. Send {type:start,...}, then {type:text,text:...} chunks, then {type:flush}. Receive {type:audio,audio:<base64>} chunks."
          (not key-ok?) (str " WARNING: VOICE_GATEWAY_API_KEY is not configured."))
        {:ws_endpoint "/ws/voice/tts" :voice_id voice-id :model_id model-id
         :output_format out-fmt :auto-mode auto-mode :api_key_configured key-ok?})))))

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
      (.mkdir node-fs output-dir #js {:recursive true})
      (.writeFile node-fs absolute ustx-yaml "utf8")
      (.writeFile node-fs readme-abs readme-text "utf8")
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
                       (aset "description"     "Synthesize speech via Voxx Gateway. Writes MP3 to workspace.")
                       (aset "promptSnippet"   "Use voice.tts for spoken audio from plain text.")
                       (aset "promptGuidelines" (clj->js ["Pass plain text; strip markdown first."
                                                             "Use model_id to select a Voxx provider per request: kokoro, melo, espeak, xiaomi_mimo, requesty, or openai."
                                                             "Default model_id is kokoro; default voice_id is alloy; default output_format is mp3."
                                                             "Follow with workspace_media.attach to embed audio."
                                                             "If debugging provider choice, inspect Voxx response metadata/logs for x-openhax-tts-backend."]))
                       (aset "parameters" (tts-rest-params Type))
                       (aset "execute"     (tts-rest-execute runtime config))))
                   (when (allowed? "voice.tts_stream")
                     (doto (js-obj)
                       (aset "name"            "voice.tts_stream")
                       (aset "label"           "TTS Stream")
                       (aset "description"     "WS streaming TTS session params for /ws/voice/tts.")
                       (aset "promptSnippet"   "Use voice.tts_stream for WS TTS connection params.")
                       (aset "promptGuidelines" (clj->js ["Returns WS protocol spec and API key status."
                                                             "Use voice.tts when you need a persisted MP3 file."]))
                       (aset "parameters" (tts-stream-params Type))
                       (aset "execute"     (tts-stream-execute config))))]]
     (clj->js (vec (remove nil? tools))))))

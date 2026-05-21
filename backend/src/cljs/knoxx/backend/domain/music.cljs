(ns knoxx.backend.domain.music
  "Music/audio identification and visualization tools."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.domain.text :refer [tool-text-result]]
            [knoxx.backend.domain.media :as media :refer [normalize-tool-path-arg]]
            [knoxx.backend.domain.music.audd-client :as audd-client]
            [knoxx.backend.domain.tools :refer [maybe-tool-update! create-tool-obj]]
            ["node:child_process" :refer [execFile]]
            ["node:crypto" :as crypto]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]
            ["node:util" :refer [promisify]]))

(def ^:private exec-file-async (promisify execFile))

(defn- music-audd-lookup!
  "Identify a song from an audio file using AudD API."
  [runtime config source]
  (if (str/blank? (:audd-api-token config))
    (js/Promise.resolve {:error "AUDD_API_TOKEN not configured"
                         :hint "Set AUDD_API_TOKEN to enable music identification"})
    (-> (media/materialize-media-source! runtime config source media/audio-render-max-bytes)
        (.then (fn [media]
                 (-> (audd-client/recognize! (audd-client/client config) media)
                     (.then (fn [payload]
                              {:status (or (:status payload) "unknown")
                               :source source
                               :filename (:filename media)
                               :result (:result payload)}))))))))

(defn- music-acoustid-lookup!
  "Look up audio fingerprint via AcoustID API."
  [config fingerprint duration]
  (-> (audd-client/acoustid-lookup! (audd-client/client config) fingerprint duration)
      (.then (fn [result]
               (if (:error result)
                 result
                 {:status "ok"
                  :result result})))))

(defn- music-musicbrainz-recording!
  "Look up MusicBrainz recording by MBID."
  [config mbid]
  (-> (audd-client/musicbrainz-recording! (audd-client/client config) mbid)
      (.then (fn [result]
               {:status "ok"
                :mbid mbid
                :result result}))))

(defn- music-copyright-check!
  "Check if audio is likely copyrighted based on ISRC presence."
  [_config audio-data]
  (let [isrc (get audio-data :isrc)
        apple-music-isrc (get-in audio-data [:apple_music :isrc])
        spotify-isrc (get-in audio-data [:spotify :external_ids :isrc])
        has-isrc (or (not (str/blank? isrc))
                     (not (str/blank? apple-music-isrc))
                     (not (str/blank? spotify-isrc)))]
    {:decision (if has-isrc "BLOCK" "UNKNOWN")
     :reason (if has-isrc
               "ISRC found - recording is commercially released"
               "No ISRC found - copyright status unclear")
     :isrcs {:primary isrc
             :apple_music apple-music-isrc
             :spotify spotify-isrc}}))

(defn- music-generate!
  "Generate a WAV file from a JSON music spec using the native Node.js synthesis engine."
  [runtime config spec-json output-path]
  (let [script-path (media/path-resolve path (or (.cwd js/process) "/") "scripts" "synthesize-music.mjs")]
    (-> (media/temp-file-path! runtime "music-specs" ".json")
        (.then (fn [spec-path]
                 (-> (media/fs-write-file! fs spec-path spec-json)
                     (.then (fn []
                              (let [out-path (or output-path
                                                 (str "Music/generated/" (.randomUUID crypto) ".wav"))
                                    {:keys [absolute relative]} (media/resolve-workspace-media-path runtime config out-path)]
                                (-> (media/fs-mkdir! fs (media/path-resolve path absolute "..") #js {:recursive true})
                                    (.then (fn []
                                             (-> (exec-file-async "node" #js [script-path spec-path absolute]
                                                                 #js {:timeout 120000 :maxBuffer 1048576})
                                                 (.then (fn [stdout _stderr]
                                                          (let [result (js->clj (.parse js/JSON stdout) :keywordize-keys true)]
                                                            (assoc result :workspace-path relative :absolute-path absolute)))))))))))))))))

(defn- json-object-type?
  [value]
  (and value
       (not= value js/undefined)
       (= "object" (goog/typeOf value))))

(defn- ensure-json-string!
  [field-name value]
  (cond
    (or (nil? value) (= value js/undefined))
    (throw (js/Error. (str field-name " is required")))

    (string? value)
    (let [trimmed (str/trim value)]
      (when (str/blank? trimmed)
        (throw (js/Error. (str field-name " must not be blank"))))
      (try
        (.parse js/JSON trimmed)
        trimmed
        (catch :default err
          (throw (js/Error. (str "Invalid " field-name ": expected JSON string or JSON object; parse failed: " (.-message err)))))))

    (or (map? value) (vector? value) (array? value) (json-object-type? value))
    (let [json-text (try
                      (.stringify js/JSON (if (or (array? value) (json-object-type? value))
                                            value
                                            (clj->js value)))
                      (catch :default err
                        (throw (js/Error. (str "Invalid " field-name ": could not serialize JSON object: " (.-message err))))))]
      (when (or (nil? json-text)
                (= json-text js/undefined)
                (str/blank? (str json-text)))
        (throw (js/Error. (str "Invalid " field-name ": expected JSON object or array"))))
      json-text)

    :else
    (throw (js/Error. (str "Invalid " field-name ": expected JSON string or JSON object, got " (goog/typeOf value))))))

(def identify-file-params
  [:map
   [:file_path {:description "Workspace path, URL, or data URL for the audio file to identify."} :string]])

(def acoustid-params
  [:map
   [:fingerprint {:description "AcoustID fingerprint string."} :string]
   [:duration {:optional true :description "Duration in seconds (default 25)."} :int]])

(def musicbrainz-params
  [:map
   [:mbid {:description "MusicBrainz recording ID (MBID)."} :string]])

(def copyright-params
  [:map
   [:audio_data_json {:description "JSON object containing audio metadata with ISRC fields from AudD or similar."} :string]])

(def audio-params
  [:map
   [:source {:description "Path or URL to the audio file."} :string]
   [:width {:optional true :description "Output width in pixels."} :int]
   [:height {:optional true :description "Output height in pixels."} :int]
   [:title {:optional true :description "Optional filename/title for the rendered image."} :string]])

(def generate-params
  [:map
   [:spec_json {:description "JSON music specification as an object or JSON string. Supports bpm, tracks, instruments, notes, patterns, and nested patterns[].notes with Tone-style times like 0:1:2 and durations like 4n/8n."} :any]
   [:output_path {:optional true :description "Optional workspace-relative output path for the WAV file. Defaults to Music/generated/<uuid>.wav"} :string]])

(defn identify-file-execute [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        file-path (aget params "file_path")]
    (maybe-tool-update! on-update (str "Identifying song from file: " file-path "…"))
    (-> (music-audd-lookup! runtime config file-path)
        (.then (fn [result]
                 (tool-text-result
                  (str "Music identification result: " (:status result)
                       (when-let [song (get-in result [:result :title])]
                         (str " - " song " by " (get-in result [:result :artist]))))
                  result))))))

(defn acoustid-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        fingerprint (aget params "fingerprint")
        duration (aget params "duration")]
    (maybe-tool-update! on-update "Looking up AcoustID fingerprint…")
    (-> (music-acoustid-lookup! config fingerprint duration)
        (.then (fn [result]
                 (tool-text-result
                  (str "AcoustID lookup: " (:status result)
                       (when-let [results (get-in result [:result :results])]
                         (str " - found " (count results) " matches")))
                  result))))))

(defn musicbrainz-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        mbid (aget params "mbid")]
    (maybe-tool-update! on-update (str "Looking up MusicBrainz recording " mbid "…"))
    (-> (music-musicbrainz-recording! config mbid)
        (.then (fn [result]
                 (tool-text-result
                  (str "MusicBrainz recording: " mbid
                       (when-let [title (get-in result [:result :title])]
                         (str " - " title)))
                  result))))))

(defn copyright-check-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        audio-data-json (aget params "audio_data_json")
        audio-data (try
                     (js->clj (.parse js/JSON audio-data-json) :keywordize-keys true)
                     (catch :default _ nil))]
    (if (nil? audio-data)
      (tool-text-result "Invalid audio_data_json" {:error "Invalid JSON"})
      (let [result (music-copyright-check! config audio-data)]
        (maybe-tool-update! on-update "Checking copyright status…")
        (tool-text-result
         (str "Copyright decision: " (:decision result) " - " (:reason result))
         result)))))

(defn spectrogram-execute [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        source (aget params "source")
        width (aget params "width")
        height (aget params "height")
        title (normalize-tool-path-arg (aget params "title"))]
    (maybe-tool-update! on-update "Audio spectrogram generation…")
    (media/audio-visualization-result! runtime config source {:kind :spectrogram
                                                              :width width
                                                              :height height
                                                              :title title})))

(defn waveform-execute [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        source (aget params "source")
        width (aget params "width")
        height (aget params "height")
        title (normalize-tool-path-arg (aget params "title"))]
    (maybe-tool-update! on-update "Audio waveform generation…")
    (media/audio-visualization-result! runtime config source {:kind :waveform
                                                              :width width
                                                              :height height
                                                              :title title})))

(defn generate-execute [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        raw-spec-json (or (aget params "spec_json")
                          (aget params "specJson")
                          (aget params "spec"))
        spec-json (ensure-json-string! "spec_json" raw-spec-json)
        output-path (media/normalize-tool-path-arg (or (aget params "output_path")
                                                       (aget params "outputPath")))]
    (maybe-tool-update! on-update "Generating music from spec…")
    (-> (music-generate! runtime config spec-json output-path)
        (.then (fn [result]
                 (tool-text-result
                  (str "Generated WAV: " (:workspace-path result)
                       " (" (:durationSec result) "s, " (:sampleRate result) "Hz)")
                  result))))))

(def identify-file-tool
  (partial create-tool-obj
           "music.identify_file" "Music Identify File"
           "Identify a song from an audio file using AudD API."
           "Identify songs from audio files when you need to know what music is playing."
           ["Use music.identify_file when you have an audio file and need to identify the song."
            "Returns song title, artist, album, and ISRC when found."]
           identify-file-params
           identify-file-execute))

(def acoustid-tool
  (partial create-tool-obj
           "music.acoustid_lookup" "AcoustID Lookup"
           "Look up audio fingerprint via AcoustID API to identify recordings."
           "Look up AcoustID fingerprints to identify music by its acoustic signature."
           ["Use music.acoustid_lookup when you have a fingerprint from chromaprint or similar."
            "Requires AcoustID API key in config."]
           acoustid-params
           acoustid-execute))

(def musicbrainz-tool
  (partial create-tool-obj
           "music.musicbrainz_recording" "MusicBrainz Recording"
           "Look up MusicBrainz recording metadata by MBID."
           "Fetch detailed recording metadata from MusicBrainz."
           ["Use music.musicbrainz_recording when you have a MusicBrainz ID (MBID)."
            "Returns ISRCs, releases, release groups, and other metadata."
            "Rate-limited to 1 request per second."]
           musicbrainz-params
           musicbrainz-execute))

(def copyright-check-tool
  (partial create-tool-obj
           "music.copyright_check" "Music Copyright Check"
           "Check if audio is likely copyrighted based on ISRC presence."
           "Check copyright status of identified music before using it."
           ["Use music.copyright_check after music identification to assess copyright risk."
            "Returns BLOCK if ISRC found (commercially released), UNKNOWN otherwise."
            "Pass audio_data_json from AudD or similar identification result."]
           copyright-params
           copyright-check-execute))

(def spectrogram-tool
  (partial create-tool-obj
           "audio.spectrogram" "Audio Spectrogram"
           "Generate a spectrogram image from an audio file using ffmpeg."
           "Visualize audio as a spectrogram to see frequencies over time."
           ["Use audio.spectrogram to visualize audio frequency content."
            "This is especially useful when the active model can see images but cannot directly accept audio input."]
           audio-params
           spectrogram-execute))

(def waveform-tool
  (partial create-tool-obj
           "audio.waveform" "Audio Waveform"
           "Generate a waveform image from an audio file using ffmpeg."
           "Visualize audio as a waveform to see amplitude over time."
           ["Use audio.waveform to visualize audio amplitude."
            "Pair with audio.spectrogram when you want both amplitude and frequency views."]
           audio-params
           waveform-execute))

(def generate-tool
  (partial create-tool-obj
           "music.generate" "Generate Music"
           "Synthesize a WAV file from a JSON music spec using the native Node.js audio engine."
           "Generate original music and render it to a WAV file directly on the server."
           ["Use music.generate when the user wants original synthesized music, beats, loops, or melodies."
            "Construct a JSON object spec with bpm, tracks, instruments, and notes; spec_json may be an object or a JSON-encoded string."
            "Supported instruments: synth, bass, lead, pad, drum (kick, snare, hihat, clap, tom)."
            "Timing supports numeric beats plus strings like 0:1:2; durations support numeric beats plus 4n, 8n, 2n, and dotted forms."
            "After generation, use workspace_media.attach to embed the WAV in the reply with a player."
            "Default output path is Music/generated/<uuid>.wav."]
           generate-params
           generate-execute))

(defn create-music-custom-tools
  "Create music/audio identification and analysis tools."
  ([runtime config] (create-music-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "music.identify_file")
                  (identify-file-tool runtime config))
                (when (allowed? "music.acoustid_lookup")
                  (acoustid-tool runtime config))
                (when (allowed? "music.musicbrainz_recording")
                  (musicbrainz-tool runtime config))
                (when (allowed? "music.copyright_check")
                  (copyright-check-tool runtime config))
                (when (allowed? "audio.spectrogram")
                  (spectrogram-tool runtime config))
                (when (allowed? "audio.waveform")
                  (waveform-tool runtime config))
                (when (allowed? "music.generate")
                  (generate-tool runtime config))]))))))

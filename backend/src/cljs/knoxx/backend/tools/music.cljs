(ns knoxx.backend.tools.music
  "Music/audio identification and visualization tools."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.text :refer [tool-text-result]]
            [knoxx.backend.tools.media :as media]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! type-optional]]))

(defn- music-audd-lookup!
  "Identify a song from an audio file using AudD API."
  [runtime config source]
  (let [audd-token (:audd-api-token config)]
    (if (str/blank? audd-token)
      (js/Promise.resolve {:error "AUDD_API_TOKEN not configured"
                           :hint "Set AUDD_API_TOKEN to enable music identification"})
      (-> (media/materialize-media-source! runtime config source media/audio-render-max-bytes)
          (.then (fn [media]
                   (let [form (js/FormData.)]
                     (.append form "api_token" audd-token)
                     (.append form "return" "apple_music,spotify,deezer")
                     (.append form "file"
                              (js/Blob. #js [(:buffer media)] #js {:type (:mime-type media)})
                              (:filename media))
                     (-> (js/fetch "https://api.audd.io/" #js {:method "POST" :body form})
                         (.then (fn [resp]
                                  (if-not (.-ok resp)
                                    (-> (.text resp)
                                        (.then (fn [text]
                                                 (throw (js/Error. (str "AudD HTTP " (.-status resp) ": " text))))))
                                    (.json resp))))
                         (.then (fn [payload]
                                  (let [status (or (aget payload "status") "unknown")
                                        result (aget payload "result")]
                                    {:status status
                                     :source source
                                     :filename (:filename media)
                                     :result (when result (js->clj result :keywordize-keys true))})))))))))))

(defn- music-acoustid-lookup!
  "Look up audio fingerprint via AcoustID API."
  [config fingerprint duration]
  (let [acoustid-key (:acoustid-api-key config)]
    (if (str/blank? acoustid-key)
      (js/Promise.resolve {:error "ACOUSTID_API_KEY not configured"
                          :hint "Set acoustid-api-key in Knoxx config to enable AcoustID lookups"})
      (let [url (str "https://api.acoustid.org/v2/lookup?client=" acoustid-key
                    "&duration=" (or duration 25)
                    "&fingerprint=" fingerprint
                    "&meta=recordings+recordingids+releasegroups")]
        (-> (js/fetch url)
            (.then (fn [resp]
                     (if (.-ok resp)
                       (.json resp)
                       (-> (.text resp)
                           (.then (fn [text]
                                    (throw (js/Error. (str "AcoustID HTTP " (.-status resp) ": " text))))))))
            (.then (fn [result]
                     {:status "ok"
                      :result (js->clj result :keywordize-keys true)}))))))))

(defn- music-musicbrainz-recording!
  "Look up MusicBrainz recording by MBID."
  [mbid]
  (let [url (str "https://musicbrainz.org/ws/2/recording/" mbid
                "?inc=isrcs+releases+release-groups&fmt=json")]
    (-> (js/Promise.resolve nil)
        (.then (fn [_]
                 ;; Rate limiting: wait 1.1s before MusicBrainz requests
                 (js/Promise. (fn [resolve]
                                (js/setTimeout resolve 1100)))))
        (.then (fn [_]
                 (js/fetch url #js {:headers #js {"User-Agent" "Knoxx-Agent/1.0 (discord bot)"}})))
        (.then (fn [resp]
                 (if (.-ok resp)
                   (.json resp)
                   {:error (str "MusicBrainz HTTP " (.-status resp))})))
        (.then (fn [result]
                 {:status "ok"
                  :mbid mbid
                  :result (js->clj result :keywordize-keys true)})))))

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
  (let [exec-file-async (aget runtime "execFileAsync")
        node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        script-path (media/path-resolve node-path (or (.cwd js/process) "/") "scripts" "synthesize-music.mjs")]
    (when-not exec-file-async
      (throw (js/Error. "execFileAsync runtime dependency is not available")))
    (-> (media/temp-file-path! runtime "music-specs" ".json")
        (.then (fn [spec-path]
                 (-> (media/fs-write-file! node-fs spec-path spec-json)
                     (.then (fn []
                              (let [out-path (or output-path
                                                 (str "Music/generated/" (.randomUUID (aget runtime "crypto")) ".wav"))
                                    {:keys [absolute relative]} (media/resolve-workspace-media-path runtime config out-path)]
                                (-> (media/fs-mkdir! node-fs (media/path-resolve node-path absolute "..") #js {:recursive true})
                                    (.then (fn []
                                             (-> (exec-file-async "node" #js [script-path spec-path absolute]
                                                                 #js {:timeout 120000 :maxBuffer 1048576})
                                                 (.then (fn [stdout _stderr]
                                                          (let [result (js->clj (.parse js/JSON stdout) :keywordize-keys true)]
                                                            (assoc result :workspace-path relative :absolute-path absolute)))))))))))))))))

(defn create-music-custom-tools
  "Create music/audio identification and analysis tools."
  ([runtime config] (create-music-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [Type (aget runtime "Type")
         allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))

         ;; Tool parameter schemas
         identify-file-params (.Object Type
                                       #js {:file_path (.String Type #js {:description "Workspace path, URL, or data URL for the audio file to identify."})})
         acoustid-params (.Object Type
                                  #js {:fingerprint (.String Type #js {:description "AcoustID fingerprint string."})
                                       :duration (type-optional Type (.Number Type #js {:description "Duration in seconds (default 25)."}))})
         musicbrainz-params (.Object Type
                                     #js {:mbid (.String Type #js {:description "MusicBrainz recording ID (MBID)."})})
         copyright-params (.Object Type
                                   #js {:audio_data_json (.String Type #js {:description "JSON object containing audio metadata with ISRC fields from AudD or similar."})})

         ;; Execute functions
         identify-file-execute (fn [_tool-call-id params a b c]
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

         acoustid-execute (fn [_tool-call-id params a b c]
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

         musicbrainz-execute (fn [_tool-call-id params a b c]
                               (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                     mbid (aget params "mbid")]
                                 (maybe-tool-update! on-update (str "Looking up MusicBrainz recording " mbid "…"))
                                 (-> (music-musicbrainz-recording! mbid)
                                     (.then (fn [result]
                                              (tool-text-result
                                               (str "MusicBrainz recording: " mbid
                                                    (when-let [title (get-in result [:result :title])]
                                                      (str " - " title)))
                                               result))))))

         copyright-check-execute (fn [_tool-call-id params a b c]
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

         spectrogram-execute (fn [_tool-call-id params a b c]
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

         waveform-execute (fn [_tool-call-id params a b c]
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

         audio-params (.Object Type
                               #js {:source (.String Type #js {:description "Path or URL to the audio file."})
                                    :width (type-optional Type (.Number Type #js {:description "Output width in pixels."}))
                                    :height (type-optional Type (.Number Type #js {:description "Output height in pixels."}))
                                    :title (type-optional Type (.String Type #js {:description "Optional filename/title for the rendered image."}))})

         generate-params (.Object Type
                                  #js {:spec_json (.String Type #js {:description "JSON music specification describing BPM, tracks, instruments, patterns, and notes."})
                                       :output_path (type-optional Type (.String Type #js {:description "Optional workspace-relative output path for the WAV file. Defaults to Music/generated/<uuid>.wav"}))})

         generate-execute (fn [_tool-call-id params a b c]
                            (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                  spec-json (aget params "spec_json")
                                  output-path (media/normalize-tool-path-arg (aget params "output_path"))]
                              (maybe-tool-update! on-update "Generating music from spec…")
                              (-> (music-generate! runtime config spec-json output-path)
                                  (.then (fn [result]
                                           (tool-text-result
                                            (str "Generated WAV: " (:workspace-path result)
                                                 " (" (:durationSec result) "s, " (:sampleRate result) "Hz)")
                                            result))))))]

     (clj->js
      (vec
       (remove nil?
                [(when (allowed? "music.identify_file")
                   (doto (js-obj)
                     (aset "name" "music.identify_file")
                     (aset "label" "Music Identify File")
                     (aset "description" "Identify a song from an audio file using AudD API.")
                     (aset "promptSnippet" "Identify songs from audio files when you need to know what music is playing.")
                     (aset "promptGuidelines" (clj->js ["Use music.identify_file when you have an audio file and need to identify the song."
                                                        "Returns song title, artist, album, and ISRC when found."]))
                     (aset "parameters" identify-file-params)
                     (aset "execute" identify-file-execute)))
                 (when (allowed? "music.acoustid_lookup")
                   (doto (js-obj)
                     (aset "name" "music.acoustid_lookup")
                     (aset "label" "AcoustID Lookup")
                     (aset "description" "Look up audio fingerprint via AcoustID API to identify recordings.")
                     (aset "promptSnippet" "Look up AcoustID fingerprints to identify music by its acoustic signature.")
                     (aset "promptGuidelines" (clj->js ["Use music.acoustid_lookup when you have a fingerprint from chromaprint or similar."
                                                        "Requires AcoustID API key in config."]))
                     (aset "parameters" acoustid-params)
                     (aset "execute" acoustid-execute)))
                 (when (allowed? "music.musicbrainz_recording")
                   (doto (js-obj)
                     (aset "name" "music.musicbrainz_recording")
                     (aset "label" "MusicBrainz Recording")
                     (aset "description" "Look up MusicBrainz recording metadata by MBID.")
                     (aset "promptSnippet" "Fetch detailed recording metadata from MusicBrainz.")
                     (aset "promptGuidelines" (clj->js ["Use music.musicbrainz_recording when you have a MusicBrainz ID (MBID)."
                                                        "Returns ISRCs, releases, release groups, and other metadata."
                                                        "Rate-limited to 1 request per second."]))
                     (aset "parameters" musicbrainz-params)
                     (aset "execute" musicbrainz-execute)))
                 (when (allowed? "music.copyright_check")
                   (doto (js-obj)
                     (aset "name" "music.copyright_check")
                     (aset "label" "Music Copyright Check")
                     (aset "description" "Check if audio is likely copyrighted based on ISRC presence.")
                     (aset "promptSnippet" "Check copyright status of identified music before using it.")
                     (aset "promptGuidelines" (clj->js ["Use music.copyright_check after music identification to assess copyright risk."
                                                        "Returns BLOCK if ISRC found (commercially released), UNKNOWN otherwise."
                                                        "Pass audio_data_json from AudD or similar identification result."]))
                     (aset "parameters" copyright-params)
                     (aset "execute" copyright-check-execute)))
                 (when (allowed? "audio.spectrogram")
                   (doto (js-obj)
                     (aset "name" "audio.spectrogram")
                     (aset "label" "Audio Spectrogram")
                     (aset "description" "Generate a spectrogram image from an audio file using ffmpeg.")
                     (aset "promptSnippet" "Visualize audio as a spectrogram to see frequencies over time.")
                     (aset "promptGuidelines" (clj->js ["Use audio.spectrogram to visualize audio frequency content."
                                                        "This is especially useful when the active model can see images but cannot directly accept audio input."]))
                     (aset "parameters" audio-params)
                     (aset "execute" spectrogram-execute)))
                 (when (allowed? "audio.waveform")
                   (doto (js-obj)
                     (aset "name" "audio.waveform")
                     (aset "label" "Audio Waveform")
                     (aset "description" "Generate a waveform image from an audio file using ffmpeg.")
                     (aset "promptSnippet" "Visualize audio as a waveform to see amplitude over time.")
                     (aset "promptGuidelines" (clj->js ["Use audio.waveform to visualize audio amplitude."
                                                        "Pair with audio.spectrogram when you want both amplitude and frequency views."]))
                     (aset "parameters" audio-params)
                     (aset "execute" waveform-execute)))
                 (when (allowed? "music.generate")
                   (doto (js-obj)
                     (aset "name" "music.generate")
                     (aset "label" "Generate Music")
                     (aset "description" "Synthesize a WAV file from a JSON music spec using the native Node.js audio engine.")
                     (aset "promptSnippet" "Generate original music and render it to a WAV file directly on the server.")
                     (aset "promptGuidelines" (clj->js ["Use music.generate when the user wants original synthesized music, beats, loops, or melodies."
                                                        "Construct a JSON spec with bpm, tracks, instruments, and patterns."
                                                        "Supported instruments: synth, bass, lead, pad, drum (kick, snare, hihat, clap, tom)."
                                                        "After generation, use workspace_media.attach to embed the WAV in the reply with a player."
                                                        "Default output path is Music/generated/<uuid>.wav."]))
                     (aset "parameters" generate-params)
                     (aset "execute" generate-execute)))]))))))


(ns knoxx.backend.tools.blaze
  "BlazeAPI-backed media and chat generation tools."
  (:require [clojure.string :as str]
            [promesa.core :as p]
            [knoxx.backend.domain.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.domain.text :refer [tool-text-result]]
            [knoxx.backend.tools.media :as media]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! create-tool-obj]]
            ["node:crypto" :as crypto]
            ["node:fs/promises" :as fs-promises]
            ["node:path" :as path]))

(defn- blank->nil
  [v]
  (let [s (str/trim (str (or v "")))]
    (when-not (str/blank? s) s)))

(defn- config-value
  [config keyword-key js-key camel-key]
  (or (when (map? config) (get config keyword-key))
      (when-not (map? config) (aget config js-key))
      (when-not (map? config) (aget config camel-key))))

(defn- env-value
  [& names]
  (some (fn [name]
          (some-> js/process .-env (aget name) blank->nil))
        names))

(defn- proxx-api-key
  [config]
  (or (blank->nil (config-value config :proxx-auth-token "proxx-auth-token" "proxxAuthToken"))
      (env-value "PROXX_AUTH_TOKEN" "PROXY_AUTH_TOKEN")))

(defn- now-ms
  []
  (.now js/Date))

(defn- safe-json
  [value]
  (try
    (.stringify js/JSON (clj->js value))
    (catch :default err
      (str "{\"log_error\":\"" (.-message err) "\"}"))))

(defn- log-info!
  [event data]
  (.log js/console "[blaze.generate]" event (safe-json data)))

(defn- log-warn!
  [event data]
  (.warn js/console "[blaze.generate]" event (safe-json data)))

(defn- log-error!
  [event data]
  (.error js/console "[blaze.generate]" event (safe-json data)))

(defn- summarize-body
  [body]
  {:model (:model body)
   :body_keys (sort (map name (keys body)))
   :prompt_chars (count (str (or (:prompt body) "")))
   :input_chars (count (str (or (:input body) "")))
   :lyrics_chars (count (str (or (:lyrics body) "")))
   :is_instrumental (:is_instrumental body)
   :lyrics_optimizer (:lyrics_optimizer body)
   :sample_rate (:sample_rate body)
   :bitrate (:bitrate body)
   :audio_format (:audio_format body)})

(defn- summarize-payload
  [payload]
  (let [data (:data payload)
        data-audio (when (map? data) (:audio data))
        data-video (when (map? data) (:video data))
        first-data (when (sequential? data) (first data))]
    (cond-> {:response_keys (when (map? payload) (sort (map name (keys payload))))
             :id (:id payload)
             :object (:object payload)
             :model (:model payload)
             :status (:status payload)
             :error_present (boolean (blank->nil (:error payload)))
             :message_present (boolean (blank->nil (:message payload)))}
      (map? data) (assoc :data_keys (sort (map name (keys data))))
      (map? first-data) (assoc :first_data_keys (sort (map name (keys first-data))))
      (string? data-audio) (assoc :data_audio_chars (count data-audio))
      (string? data-video) (assoc :data_video_chars (count data-video)))))

(defn- proxx-base-url
  [config]
  (str/replace
   (or (blank->nil (config-value config :proxx-base-url "proxx-base-url" "proxxBaseUrl"))
       (env-value "PROXX_BASE_URL")
       "http://proxx:8789")
   #"/+$" ""))

(defn- generation-url
  [config modality]
  (str (proxx-base-url config)
       (case modality
         "image" "/v1/images/generations"
         "video" "/v1/videos/generations"
         "music" "/v1/music/generations"
         "tts" "/v1/audio/speech"
         "/v1/chat/completions")))

(defn- normalize-modality
  [value]
  (let [m (-> (or value "chat") str str/trim str/lower-case)]
    (if (contains? #{"chat" "text" "image" "images" "video" "music" "tts" "speech"} m)
      (case m
        "text" "chat"
        "images" "image"
        "speech" "tts"
        m)
      "chat")))

(defn- default-model
  [modality]
  (case modality
    "music" "musicgen-small"
    "image" "MiniMax-image-01-highspeed"
    "video" "qwen3.5-omni-flash-thinking-search"
    "tts" "MiniMax-speech-2.8-hd-highspeed"
    "fast-general"))

;; Blaze media generation queue — limit concurrency to 1 so each request gets
;; Blaze's full attention and avoids Cloudflare 120s timeout under contention.
(def ^:private blaze-queue (atom {:in-flight false :waiting []}))


(defn- run-queued!
  "Enqueue fn; if nothing in flight, run immediately. Otherwise queue."
  [f]
  (js/Promise.
   (fn [resolve reject]
     (letfn [(try-run! []
               (let [current @blaze-queue]
                 (if (:in-flight current)
                   (swap! blaze-queue update :waiting conj [resolve reject f])
                   (do
                     (swap! blaze-queue assoc :in-flight true)
                     (-> (f)
                         (.then (fn [result]
                                  (resolve result)
                                  (drain!)))
                         (.catch (fn [err]
                                   (reject err)
                                   (drain!))))))))
             (drain! []
               (let [current (swap! blaze-queue
                                    (fn [q]
                                      (let [rest (vec (rest (:waiting q)))]
                                        (if (seq rest)
                                          {:in-flight true :waiting rest}
                                          {:in-flight false :waiting []}))))
                     next (first (:waiting current))]
                 (when next
                   (let [[rslv rjct fn-] next]
                     (-> (fn-)
                         (.then (fn [result]
                                  (rslv result)
                                  (drain!)))
                         (.catch (fn [err]
                                   (rjct err)
                                   (drain!))))))))]
       (try-run!)))))

(defn- candidate-models
  [modality requested-model]
  (if-let [model (blank->nil requested-model)]
    [model]
    (case modality
       ;; Local musicgen is free and fast; fall back to MiniMax if it fails.
       "music" ["musicgen-small"
                "MiniMax-music-2.6-free"
                "MiniMax-music-2.5-free"
                "MiniMax-music-2.6-highspeed"
                "MiniMax-music-2.5-highspeed"]
      "image" ["MiniMax-image-01-highspeed"]
      "video" ["qwen3.5-omni-flash-thinking-search"]
      "tts" ["MiniMax-speech-2.8-hd-highspeed"]
      [(default-model modality)])))

(declare response-text first-data-url first-media-url hex-asset-string base64-asset-string)

(defn- payload-has-output?
  "Best-effort detection of whether a Blaze payload includes any usable output.

   Some upstream/proxy responses may report status=terminated/failed while still
   including an asset URL or embedded audio/image/video bytes. The known-good
   Python script treats these as success when an asset can be saved, so Knoxx
   should only treat logical statuses as fatal when no output is present."
  [payload]
  (let [data (:data payload)
        data-maps (cond
                    (map? data) [data]
                    (sequential? data) (filter map? data)
                    :else [])
        has-direct? (some (fn [m]
                            (or (blank->nil (:url m))
                                (blank->nil (:audio m))
                                (blank->nil (:video m))))
                          data-maps)
        text (response-text payload)
        asset-text (str text "\n" (pr-str payload))]
    (boolean
     (or has-direct?
         (first-data-url asset-text)
         (first-media-url asset-text)
         (hex-asset-string payload)
         (base64-asset-string payload)))))

(defn- default-system-prompt
  [modality]
  (case modality
    "music" "You are a music generation endpoint. Return generated audio asset URLs or data URLs when available, plus concise metadata."
    "image" "You are an image generation endpoint. Return generated image asset URLs or data URLs when available, plus concise metadata."
    "video" "You are a video generation endpoint. Return generated video asset URLs or data URLs when available, plus concise metadata."
    "tts" "You are a text-to-speech generation endpoint. Return generated audio asset URLs or data URLs when available, plus concise metadata."
    "You are BlazeAPI. Produce the requested result concisely."))

(defn- parse-json-object
  [raw label]
  (if-let [text (blank->nil raw)]
    (try
      (let [parsed (js->clj (.parse js/JSON text) :keywordize-keys true)]
        (if (map? parsed)
          parsed
          (throw (js/Error. (str label " must be a JSON object")))))
      (catch :default err
        (throw (js/Error. (str "Invalid " label ": " (.-message err))))))
    {}))

(defn- bool-param
  [params key]
  (let [v (aget params key)]
    (cond
      (true? v) true
      (false? v) false
      (number? v) (not (zero? v))
      (string? v) (case (str/lower-case (str/trim v))
                    ("true" "yes" "1" "on") true
                    ("false" "no" "0" "off") false
                    nil)
      :else nil)))

(defn- prompt-implies-vocals?
  [prompt]
  (boolean
   (re-find #"(?i)\b(vocal|vocals|voice|singer|singing|sing|song|lyrics|lyric|rap|rapper|spoken|verse|chorus|hook)\b"
            (str prompt))))

(defn- output-path-audio-format
  [params]
  (when-let [raw (blank->nil (aget params "output_path"))]
    (let [lower (str/lower-case raw)]
      (cond
        (str/ends-with? lower ".wav") "wav"
        (str/ends-with? lower ".pcm") "pcm"
        (str/ends-with? lower ".mp3") "mp3"
        :else nil))))

(def ^:private lyric-section-pattern
  #"^\s*\((Intro|Verse\s*\d*|Pre[- ]?Chorus|Chorus|Final\s+Chorus|Bridge|Hook|Refrain|Interlude|Breakdown|Outro)\)\s*$")

(defn- normalize-lyric-section-line
  [line]
  (if-let [[_ section] (re-matches lyric-section-pattern (str line))]
    (str "[" section "]")
    line))

(defn- normalize-music-lyrics
  [lyrics]
  (when-let [text (blank->nil lyrics)]
    (->> (str/split-lines text)
         (map normalize-lyric-section-line)
         (str/join "\n"))))

(defn- ensure-vocal-music-prompt
  [prompt lyrics]
  (let [text (str prompt)]
    (if (and (blank->nil lyrics)
             (not (prompt-implies-vocals? text)))
      (str text " Vocal song arrangement with clear sung or spoken vocal delivery; fit the provided lyrics naturally into verses, choruses, bridge, and outro.")
      text)))

(defn- build-body
  [params modality model prompt]
  (let [system-prompt (or (blank->nil (aget params "system_prompt"))
                          (default-system-prompt modality))
        payload (parse-json-object (aget params "payload_json") "payload_json")
        music-lyrics (normalize-music-lyrics (aget params "lyrics"))
        music-prompt (ensure-vocal-music-prompt prompt music-lyrics)
      explicit-instrumental (bool-param params "is_instrumental")
      instrumental? (if (nil? explicit-instrumental)
                      (nil? music-lyrics)
                      explicit-instrumental)
      explicit-lyrics-optimizer (bool-param params "lyrics_optimizer")
      lyrics-optimizer? (if (nil? explicit-lyrics-optimizer)
                          false
                          explicit-lyrics-optimizer)
        music-format (or (blank->nil (aget params "audio_format"))
                         (output-path-audio-format params)
                         "mp3")
        base (case modality
               "image" {:model model :prompt prompt}
               "video" {:model model :prompt prompt}
                 "music" (cond-> {:model model
                                   :prompt music-prompt
                                   :lyrics_optimizer lyrics-optimizer?
                                   :is_instrumental instrumental?
                                   :sample_rate (let [v (aget params "sample_rate")]
                                                  (if (number? v) v 44100))
                                   :bitrate (let [v (aget params "bitrate")]
                                              (if (number? v) v 256000))
                                   :audio_format music-format}
                            music-lyrics (assoc :lyrics music-lyrics)
                            (number? (aget params "duration")) (assoc :duration (aget params "duration")))
                "tts" {:model model
                      :input prompt
                      :voice "default"
                      :response_format "mp3"}
               (cond-> {:model model
                         :stream false
                         :messages [{:role "system" :content system-prompt}
                                    {:role "user" :content prompt}]}
                 (some? (aget params "temperature")) (assoc :temperature (aget params "temperature"))
                 (some? (aget params "max_tokens")) (assoc :max_tokens (aget params "max_tokens"))
                 (blank->nil (aget params "response_format")) (assoc :response_format {:type (blank->nil (aget params "response_format"))})))]
    (merge base payload)))

(defn- failed-payload-message
  [payload]
  (let [status (some-> (:status payload) str str/lower-case)
        error (blank->nil (:error payload))
        message (blank->nil (:message payload))
        status-msg (blank->nil (get-in payload [:analysis_info :status_msg]))
        no-output? (true? (:_blaze_no_output payload))
        has-output? (payload-has-output? payload)]
    (cond
      (and (contains? #{"failed" "error" "terminated" "canceled" "cancelled"} status)
           (not has-output?))
      (str "Blaze returned " status
           (when-let [detail (or error message)]
             (str ": " detail)))

      (or no-output?
          (and status-msg (re-find #"(?i)usage limit exceeded|daily usage limit" status-msg)))
      (str "Blaze returned no output"
           (when-let [detail (or status-msg error message)]
             (str ": " detail)))

      :else nil)))

(defn- fetch-json!
  [url api-key body log-context]
  (let [start-ms (now-ms)
        headers #js {"Authorization" (str "Bearer " api-key)
                     "Content-Type" "application/json"
                     "Accept" "application/json"}]
    (when-let [tool-call-id (:tool_call_id log-context)]
      (aset headers "X-Open-Hax-Tool-Call-Id" tool-call-id))
    (log-info! "request-start" (assoc log-context
                                      :url url
                                      :body (summarize-body body)))
    (p/let [res (js/fetch url #js {:method "POST"
                                   :headers headers
                                   :signal (js/AbortSignal.timeout 1200000)
                                   :body (.stringify js/JSON (clj->js body))})
            elapsed (- (now-ms) start-ms)
            _ (log-info! "response-status" (assoc log-context
                                                   :http_status (.-status res)
                                                   :ok (.-ok res)
                                                   :elapsed_ms elapsed))
            _ (when-not (.-ok res)
                (p/let [msg (.text res)]
                  (log-warn! "http-error" (assoc log-context
                                                 :http_status (.-status res)
                                                 :elapsed_ms elapsed
                                                 :body_preview (.slice (str msg) 0 500)))
                  (throw (js/Error. (str "Proxx Blaze proxy HTTP " (.-status res) ": " msg)))))
            payload (.json res)
            payload-clj (js->clj payload :keywordize-keys true)
            _ (log-info! "payload" (assoc log-context
                                           :elapsed_ms (- (now-ms) start-ms)
                                           :payload (summarize-payload payload-clj)))
            _ (when-let [failure (failed-payload-message payload-clj)]
                (log-warn! "logical-failure" (assoc log-context
                                                    :elapsed_ms (- (now-ms) start-ms)
                                                    :failure failure
                                                    :payload (summarize-payload payload-clj)))
                (throw (js/Error. failure)))]
      payload-clj)))

(defn- content-part-text
  [part]
  (cond
    (string? part) part
    (map? part) (or (:text part)
                    (:url part)
                    (:content part)
                    (when-let [nested (:image_url part)]
                      (if (map? nested) (:url nested) nested))
                    (when-let [nested (:audio_url part)]
                      (if (map? nested) (:url nested) nested))
                    (when-let [nested (:video_url part)]
                      (if (map? nested) (:url nested) nested))
                    (pr-str part))
    (sequential? part) (str/join "\n" (keep content-part-text part))
    (nil? part) nil
    :else (str part)))

(defn- response-text
  [payload]
  (let [choice (first (:choices payload))
        message (:message choice)
        content (or (:content message) (:text choice) (:output_text payload))]
    (or (content-part-text content)
        (some-> payload :data first :url)
        (some-> payload :data :url)
        (some-> payload :data :audio)
        (some-> payload :data :video)
        (pr-str payload))))

(def ^:private data-url-pattern
  #"data:([^;,\s]+);base64,([A-Za-z0-9+/=]+)")

(def ^:private http-url-pattern
  #"https?://[^\s\]\)\}\"'<>]+")

(defn- first-data-url
  [text]
  (when-let [[_ mime b64] (re-find data-url-pattern (str text))]
    {:mime-type mime
     :buffer (.from js/Buffer b64 "base64")}))

      (defn- likely-media-url?
  [url]
  (or (re-find #"(?i)\.(png|jpe?g|webp|gif|svg|mp3|wav|ogg|m4a|flac|aac|mp4|webm|mov)(\?|$)" url)
      (re-find #"(?i)(image|audio|video|music|file|asset|download|output)" url)))

(defn- first-media-url
  [text]
  (first (filter likely-media-url? (re-seq http-url-pattern (str text)))))

(defn- payload-strings
  [value]
  (cond
    (string? value) [value]
    (map? value) (mapcat payload-strings (vals value))
    (sequential? value) (mapcat payload-strings value)
    :else []))

(defn- hex-asset-string
  [payload]
  (first
   (filter (fn [value]
             (and (> (count value) 1000)
                  (even? (count value))
                  (boolean (re-matches #"[0-9A-Fa-f]+" value))))
           (payload-strings payload))))

(defn- base64-asset-string
  [payload]
  (first
   (filter (fn [value]
             (and (> (count value) 1000)
                  (zero? (mod (count value) 4))
                  (not (re-find #"^https?://" value))
                  (boolean (re-matches #"[A-Za-z0-9+/=]+" value))))
           (payload-strings payload))))

(defn- modality-mime-type
  [modality]
  (case modality
    "image" "image/png"
    "video" "video/mp4"
    "music" "audio/mpeg"
    "tts" "audio/mpeg"
    "application/octet-stream"))

(defn- fetch-media-url!
  [url]
  (log-info! "asset-download-start" {:url url})
  (p/let [res (js/fetch url #js {:headers #js {"Accept" "image/*,audio/*,video/*,*/*"}
                                 :signal (js/AbortSignal.timeout 120000000)

                                 })
          _ (when-not (.-ok res)
              (p/let [msg (.text res)]
                (throw (js/Error. (str "Generated asset download HTTP " (.-status res) ": " msg)))))
          arr (.arrayBuffer res)
          mime (media/sanitize-mime-type (.get (.-headers res) "content-type") "application/octet-stream")
          buffer (.from js/Buffer (js/Uint8Array. arr))]
    (log-info! "asset-download-complete" {:url url
                                           :mime_type mime
                                           :bytes (.-length buffer)})
    {:mime-type mime
     :buffer buffer
     :source-url url}))

(defn- default-output-path
  [modality mime-type]
  (let [folder (case modality
                 "music" "Music/blaze"
                 "image" "Images/blaze"
                 "video" "Video/blaze"
                 "tts" "Voice/blaze"
                 "Blaze/chat")
        ext (media/mime-type->extension mime-type)]
    (str folder "/blaze-" (.randomUUID crypto) ext)))

(defn- response-output-path
  []
  (str "Blaze/responses/blaze-" (.randomUUID crypto) ".json"))

(defn- write-buffer!
  [runtime config raw-output-path buffer]
  (let [{:keys [absolute relative]} (media/resolve-workspace-media-path runtime config raw-output-path)]
    (p/do
      (.mkdir fs-promises (.dirname path absolute) #js {:recursive true})
      (.writeFile fs-promises absolute buffer)
      (log-info! "asset-write" {:workspace_path relative
                                 :bytes (.-length buffer)})
      {:absolute-path absolute
       :workspace-path relative
       :bytes (.-length buffer)})))

(defn- write-response-json!
  [runtime config payload]
  (let [raw (.stringify js/JSON (clj->js payload) nil 2)]
    (write-buffer! runtime config (response-output-path) (.from js/Buffer raw "utf8"))))

(defn- maybe-save-asset!
  [runtime config modality output-path text payload]
  (if-let [data (first-data-url text)]
    (write-buffer! runtime config (or output-path (default-output-path modality (:mime-type data))) (:buffer data))
    (if-let [hex (hex-asset-string payload)]
      (write-buffer! runtime config
                     (or output-path (default-output-path modality (modality-mime-type modality)))
                     (.from js/Buffer hex "hex"))
      (if-let [b64 (base64-asset-string payload)]
        (write-buffer! runtime config
                       (or output-path (default-output-path modality (modality-mime-type modality)))
                       (.from js/Buffer b64 "base64"))
        (if-let [url (first-media-url text)]
          (p/let [asset (fetch-media-url! url)
                  saved (write-buffer! runtime config (or output-path (default-output-path modality (:mime-type asset))) (:buffer asset))]
            (assoc saved :source-url (:source-url asset)))
          (p/resolved nil))))))

(defn- error-message
  [err]
  (str (or (.-message err) err)))

(defn- attempt-generation!
  [config api-key params modality prompt models attempts log-context]
  (if-let [model (first models)]
    (let [body (build-body params modality model prompt)
          attempt-context (assoc log-context
                                 :attempt_index (inc (count attempts))
                                 :model model)]
      (log-info! "attempt-start" (assoc attempt-context
                                        :remaining_models (count models)))
      (-> (fetch-json! (generation-url config modality) api-key body attempt-context)
          (.then (fn [payload]
                   (log-info! "attempt-ok" (assoc attempt-context
                                                  :payload (summarize-payload payload)))
                   {:payload payload
                    :model model
                    :attempts (conj attempts {:model model :status "ok"})}))
          (.catch (fn [err]
                    (let [msg (error-message err)
                          next-attempts (conj attempts {:model model
                                                        :status "failed"
                                                        :error msg})]
                      (log-warn! "attempt-failed" (assoc attempt-context
                                                         :error msg
                                                         :attempts next-attempts))
                      (attempt-generation! config api-key params modality prompt (rest models)
                                           next-attempts log-context))))))
    (do
      (log-error! "all-attempts-failed" (assoc log-context :attempts attempts))
      (js/Promise.reject
       (js/Error.
        (str "Proxx Blaze " modality " generation failed for all candidate models: "
             (pr-str attempts)))))))

(def blaze-generate-params
  [:map
   [:prompt {:description "Prompt for BlazeAPI. For media, include style, duration/size, format, language, and any safety/copyright constraints."} :string]
   [:modality {:optional true :description "Generation mode: chat, image, video, music, or tts. Default chat."} :string]
   [:model {:optional true :description "Optional exact Blaze public model ID. If omitted, blaze.generate tries same-modality candidate models only: music 2.6 then 2.5; image MiniMax then Qwen image models; video Qwen video-capable models; TTS MiniMax speech."} :string]
   [:output_path {:optional true :description "Workspace-relative output path for a returned asset. If omitted, uses Images/blaze, Video/blaze, Music/blaze, or Voice/blaze."} :string]
   [:system_prompt {:optional true :description "Optional system prompt for the Blaze chat-completions request."} :string]
   [:payload_json {:optional true :description "Advanced: JSON object merged into the OpenAI-compatible chat/completions payload. Use only for Blaze-specific fields."} :string]
   [:lyrics {:optional true :description "Music only: explicit lyrics. If omitted and the prompt does not imply vocals, blaze.generate defaults to instrumental music."} :string]
   [:is_instrumental {:optional true :description "Music only: force instrumental true/false. Defaults true for ambient/instrumental prompts with no lyrics, false for vocal/song prompts."} :boolean]
   [:lyrics_optimizer {:optional true :description "Music only: force Blaze lyrics_optimizer true/false. Defaults true only for non-instrumental music without explicit lyrics."} :boolean]
   [:audio_format {:optional true :description "Music/TTS output format hint such as mp3, wav, or pcm. For music, output_path .wav/.mp3/.pcm also influences this."} :string]
   [:sample_rate {:optional true :description "Music only: sample rate. Default 44100."} :int]
   [:bitrate {:optional true :description "Music only: bitrate. Default 256000."} :int]
   [:temperature {:optional true :description "Optional sampling temperature."} [:double {:min 0 :max 2}]]
   [:max_tokens {:optional true :description "Optional max token limit."} :int]
   [:response_format {:optional true :description "Optional OpenAI-compatible response_format type, e.g. json_object."} :string]])

(def blaze-music-generate-params
  [:map
   [:prompt {:description "INSTRUMENTAL ONLY — Music style prompt. Describe arrangement, genre, BPM, key, mood, mix notes, and instruments. Do NOT include lyrics here. Current models (musicgen-small) generate instrumentals only."} :string]
   [:model
    {:optional true
     :description "Optional exact music model ID. Defaults through candidate order: musicgen-small (local free, instrumental only). MiniMax models require API key and may support vocals but are not currently available."}
    :string]

   [:output_path {:optional true :description "Workspace-relative output path. Defaults to Music/blaze/<uuid>.wav. The extension .wav or .mp3 sets audio_format unless audio_format is provided. IMPORTANT: musicgen-small returns WAV, so use .wav extension to avoid format confusion."} :string]
   [:is_instrumental {:optional true :description "Always true for current models. musicgen-small is instrumental-only. Leave unset."} :boolean]
   [:audio_format {:optional true :description "Output format hint: wav or mp3. If omitted, inferred from output_path extension or wav. musicgen-small returns WAV."} :string]
   [:sample_rate {:optional true :description "Sample rate. Default 44100."} :int]
   [:bitrate {:optional true :description "Bitrate. Default 256000."} :int]
   [:duration {:optional true :description "Duration in seconds. Default 5 for musicgen-small. ALWAYS set to 30+ for usable music. musicgen-small max is around 30s."} :int]
    [:payload_json {:optional true :description "Advanced: JSON object merged into the Proxx /v1/music/generations payload. Use only for Blaze-specific music fields."} :string]])

(def blaze-image-generate-params
  [:map
   [:prompt {:description "Image prompt for Proxx/Blaze image generation. Include subject, style, composition, aspect/size, and constraints."} :string]
   [:model {:optional true :description "Optional exact Blaze image model ID. Defaults through MiniMax/Qwen image candidates."} :string]
   [:output_path {:optional true :description "Workspace-relative output path. Defaults to Images/blaze/<uuid> with the detected image extension."} :string]
   [:payload_json {:optional true :description "Advanced: JSON object merged into the Proxx /v1/images/generations payload."} :string]])

(def blaze-video-generate-params
  [:map
   [:prompt {:description "Video prompt for Proxx/Blaze video generation. Include subject, action, camera, duration, style, and constraints."} :string]
   [:model {:optional true :description "Optional exact Blaze video model ID. Defaults through Qwen video-capable candidates."} :string]
   [:output_path {:optional true :description "Workspace-relative output path. Defaults to Video/blaze/<uuid> with the detected video extension."} :string]
   [:payload_json {:optional true :description "Advanced: JSON object merged into the Proxx /v1/videos/generations payload."} :string]])

(declare blaze-generate-execute)

(defn- execute-for-modality
  [modality tool-name runtime config tool-call-id params a b c]
  (let [next-params (js/Object.assign #js {} params)]
    (aset next-params "modality" modality)
    (aset next-params "tool_name" tool-name)
    (blaze-generate-execute runtime config tool-call-id next-params a b c)))

(defn blaze-music-generate-execute
  [runtime config tool-call-id params a b c]
  (execute-for-modality
   "music" "music.generate_song"
                        runtime config tool-call-id params a b c))

(defn blaze-image-generate-execute
  [runtime config tool-call-id params a b c]
  (execute-for-modality "image" "image.generate" runtime config tool-call-id params a b c))

(defn blaze-video-generate-execute
  [runtime config tool-call-id params a b c]
  (execute-for-modality "video" "video.generate" runtime config tool-call-id params a b c))

(defn blaze-generate-execute
  [runtime config tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        tool-name (or (blank->nil (aget params "tool_name")) "blaze.generate")
        prompt (or (blank->nil (aget params "prompt"))
                   (throw (js/Error. (str tool-name ": prompt required"))))
        modality (normalize-modality (aget params "modality"))
        requested-model (blank->nil (aget params "model"))
        models (candidate-models modality requested-model)
        output-path (media/normalize-tool-path-arg (aget params "output_path"))
        api-key (or (proxx-api-key config)
                    (throw (js/Error. (str tool-name ": PROXX_AUTH_TOKEN/PROXY_AUTH_TOKEN not configured for Proxx-authenticated Blaze proxying"))))
        log-context {:tool tool-name
                     :tool_call_id (blank->nil tool-call-id)
                     :modality modality
                     :explicit_model (boolean requested-model)
                     :candidate_models models
                     :prompt_chars (count prompt)
                     :output_path output-path
                     :endpoint (case modality
                                 "image" "/v1/images/generations"
                                 "video" "/v1/videos/generations"
                                 "music" "/v1/music/generations"
                                 "tts" "/v1/audio/speech"
                                 "/v1/chat/completions")
                     :proxx_base_url (proxx-base-url config)}]
    (log-info! "execute-start" log-context)
    (maybe-tool-update! on-update (str "Proxx Blaze " modality " generation via " (str/join ", " models) "…"))
    (p/let [result (attempt-generation! config api-key params modality prompt models [] log-context)
            model (:model result)
            payload (:payload result)
            text (response-text payload)
            asset-text (str text "\n" (pr-str payload))
            saved (maybe-save-asset! runtime config modality output-path asset-text payload)
            response-artifact (when-not saved (write-response-json! runtime config payload))
            _ (log-info! "execute-complete" (assoc log-context
                                                   :model model
                                                   :attempts (:attempts result)
                                                   :saved_asset (boolean saved)
                                                   :asset_workspace_path (:workspace-path saved)
                                                   :response_artifact_workspace_path (:workspace-path response-artifact)))]
      (tool-text-result
       (str "Proxx Blaze " modality " response from " model
            (if saved
              (str ". Saved asset: " (:workspace-path saved) " (" (:bytes saved) " bytes). Use workspace_media.attach to embed it.")
              (str ". No media URL/data URL was found; saved JSON response: " (:workspace-path response-artifact) ".")))
       (cond-> {:provider "blaze"
                :via "proxx"
                :endpoint (case modality
                            "image" "/v1/images/generations"
                            "video" "/v1/videos/generations"
                            "music" "/v1/music/generations"
                            "tts" "/v1/audio/speech"
                            "/v1/chat/completions")
                :modality modality
                :model model
                :attempts (:attempts result)
                :response_text text}
         saved (assoc :asset saved)
         response-artifact (assoc :response_artifact response-artifact))))))

(def blaze-generate-tool
  (partial create-tool-obj
           "blaze.generate" "Blaze Generate"
           "Generate chat, image, video, music, or TTS assets through Proxx's authenticated BlazeAPI proxy routes."
           "Use Proxx-backed BlazeAPI for multimodal generation when a Blaze model ID is requested or when the user wants external AI-generated image/video/music/TTS assets."
           ["Use model IDs exactly as shown on BlazeAPI's Models page."
            "This tool authenticates to Proxx with PROXX_AUTH_TOKEN; Knoxx does not need direct BLAZE_API_KEY access."
            "Use modality=image, video, music, or tts so Proxx can call the correct Blaze endpoint."
            "Do not substitute another modality: video failures must remain video failures after all video model candidates are exhausted."
            "After a successful asset save, call workspace_media.attach on the returned path to embed it in the response."]
           blaze-generate-params
           blaze-generate-execute))

(def blaze-music-generate-tool
  (partial create-tool-obj
            "music.generate_song" "Generate Instrumental"
            "Generate INSTRUMENTAL music through Proxx's authenticated BlazeAPI music endpoint. Current default model (musicgen-small) produces instrumentals only — NO VOCALS, NO LYRICS."
            "Use this tool for AI-generated instrumental music, beats, soundscapes, and backing tracks. Do NOT use for vocal songs or lyrics. For vocal synthesis, use voice.openutau_project + voice.openutau_render. Do not use the generic blaze.generate tool for music."
            ["INSTRUMENTAL ONLY: Current models do not generate vocals. musicgen-small is a local instrumental diffusion model."
             "Describe arrangement, genre, BPM, key, mood, mix notes, and instruments in prompt."
             "Use output_path under Music/ with .wav extension. musicgen-small returns WAV format."
             "ALWAYS set duration to 30+ seconds. Default is 5 seconds which is not usable music."
             "After a successful asset save, call workspace_media.attach on the returned path to embed it in the response."]
           blaze-music-generate-params
           blaze-music-generate-execute))

(def blaze-image-generate-tool
  (partial create-tool-obj
           "image.generate" "Generate Image"
           "Generate an image asset through Proxx's authenticated BlazeAPI image endpoint."
           "Use this dedicated Proxx/Blaze image tool when the user wants an AI-generated image. Do not route image requests through music or video tools."
           ["Save generated images under Images/ or Graphics/ with a descriptive output_path when useful."
            "After a successful asset save, call workspace_media.attach on the returned path to embed it in the response."]
           blaze-image-generate-params
           blaze-image-generate-execute))

(def blaze-video-generate-tool
  (partial create-tool-obj
           "video.generate" "Generate Video"
           "Generate a video asset through Proxx's authenticated BlazeAPI video endpoint."
           "Use this dedicated Proxx/Blaze video tool when the user wants an AI-generated video. Do not route video requests through music or image tools."
           ["Save generated videos under Video/ with a descriptive output_path when useful."
            "After a successful asset save, call workspace_media.attach on the returned path to embed it in the response."]
           blaze-video-generate-params
           blaze-video-generate-execute))

(defn create-blaze-custom-tools
  "Create BlazeAPI-backed generation tools."
  ([runtime config] (create-blaze-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "music.generate_song")
                  (blaze-music-generate-tool runtime config))
                (when (allowed? "image.generate")
                  (blaze-image-generate-tool runtime config))
                (when (allowed? "video.generate")
                  (blaze-video-generate-tool runtime config))
                (when (allowed? "blaze.generate")
                  (blaze-generate-tool runtime config))]))))))

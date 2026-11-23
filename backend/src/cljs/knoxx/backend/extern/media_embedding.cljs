(ns knoxx.backend.extern.media-embedding
  "Bounded binary media preprocessing and configured embedding HTTP boundary."
  (:require ["node:child_process" :as child]
            ["node:crypto" :as crypto]
            [knoxx.backend.law.media-memory :as law]))

(defn configured-model
  "Use the same embedding model as the embedded OpenPlanner data plane."
  []
  (let [model (aget js/process.env "EMBED_PROVIDER_MODEL")]
    (when-not (and (string? model) (seq model))
      (throw (ex-info "Embedding model is not configured" {:code "media_embedding_model_missing"})))
    model))

(defn- digest [buffer]
  (-> (.createHash crypto "sha256") (.update buffer) (.digest "hex")))

(defn- ffmpeg!
  [input arguments max-bytes]
  (js/Promise.
   (fn [resolve reject]
     (let [process (.execFile child "ffmpeg" (clj->js (into ["-v" "error" "-threads" "1" "-protocol_whitelist" "pipe" "-i" "pipe:0"] arguments))
                              #js {:timeout 90000 :maxBuffer max-bytes :encoding "buffer"
                                   :env #js {:PATH "/usr/local/bin:/usr/bin:/bin"}}
                              (fn [error stdout _stderr]
                                (if error
                                  (reject (ex-info "Binary media normalization failed or exceeded its bound"
                                                   {:code "media_normalization_failed" :exit-code (.-code error)}))
                                  (resolve stdout))))]
       (.on (.-stdin process) "error" (fn [_] nil))
       (.end (.-stdin process) input)))))

(defn- ^:async source-bytes! [part]
  (law/assert-valid! law/Part part)
  (if (seq (:data part))
    (let [raw (:data part)
          encoded (if (.startsWith raw "data:")
                    (let [match (re-matches #"data:[^;,]+;base64,(.+)" raw)]
                      (or (second match)
                          (throw (ex-info "Media data URI must contain base64" {:code "media_source_encoding"}))))
                    raw)
          bytes (js/Buffer.from encoded "base64")]
      (when (or (zero? (.-length bytes)) (> (.-length bytes) law/max-source-bytes))
        (throw (ex-info "Invalid or oversized binary media" {:code "media_source_size"})))
      bytes)
    (let [url (js/URL. (:url part))]
      (when-not (contains? #{"http:" "https:"} (.-protocol url))
        (throw (ex-info "Media memory cannot read local paths" {:code "media_source_protocol"})))
      (let [response (await (js/fetch (.toString url) #js {:signal (js/AbortSignal.timeout 60000)}))]
        (when-not (.-ok response)
          (throw (ex-info "Media source unavailable" {:code "media_source_unavailable" :status (.-status response)})))
        (let [reader (.getReader (.-body response))]
          (try
            (loop [chunks [] size 0]
              (let [row (await (.read reader))]
                (if (.-done row)
                  (js/Buffer.concat (clj->js chunks))
                  (let [next-size (+ size (.-length (.-value row)))]
                    (when (> next-size law/max-source-bytes)
                      (throw (ex-info "Media source exceeds 25MiB" {:code "media_source_size"})))
                    (recur (conj chunks (js/Buffer.from (.-value row))) next-size)))))
            (finally (await (.cancel reader)))))))))

(defn wav-bytes
  "Encode actual mono16k PCM into a WAV shared by inspection and embedding."
  [pcm]
  (let [header (js/Buffer.alloc 44) size (.-length pcm)]
    (.write header "RIFF" 0) (.writeUInt32LE header (+ size 36) 4)
    (.write header "WAVEfmt " 8) (.writeUInt32LE header 16 16)
    (.writeUInt16LE header 1 20) (.writeUInt16LE header 1 22)
    (.writeUInt32LE header 16000 24) (.writeUInt32LE header 32000 28)
    (.writeUInt16LE header 2 32) (.writeUInt16LE header 16 34)
    (.write header "data" 36) (.writeUInt32LE header size 40)
    (js/Buffer.concat #js [header pcm])))

(defn- source-provenance [part source]
  (cond-> {:source_sha256 (digest source) :source_bytes (.-length source)
           :source_url (or (:source_url part) (:url part)) :source_filename (:filename part)
           :source_mime_type (:mimeType part) :transform_version law/transform-version}
    (:derived_from part) (assoc :derived_from (:derived_from part))))

(defn ^:async prepare!
  "Return byte-derived image or audio windows, preserving complete provenance.
   Audio is never silently truncated: normalization over its bound fails."
  [part]
  (let [source (await (source-bytes! part))
        original (source-provenance part source)]
    (if (= "image" (:type part))
      (let [prefix (.toString (.subarray source 0 16) "hex")
            _ (when-not (re-find #"^(89504e470d0a1a0a|ffd8ff|52494646........57454250|474946383[79]61|424d)" prefix)
                (throw (ex-info "Only materialized raster images can enter binary memory" {:code "media_image_format"})))
            image (await (ffmpeg! source ["-frames:v" "1" "-vf" "scale=512:512:force_original_aspect_ratio=decrease"
                                         "-f" "image2pipe" "-vcodec" "mjpeg" "-q:v" "6" "pipe:1"] 200000))]
        (when (> (.-length image) 150000)
          (throw (ex-info "Normalized image exceeds embedding budget" {:code "media_normalized_size"})))
        [{:part {:type "image" :mimeType "image/jpeg" :data (.toString image "base64")}
          :provenance (assoc original :normalized_sha256 (digest image) :normalized_bytes (.-length image))}])
      (let [pcm (await (ffmpeg! source ["-vn" "-ar" "16000" "-ac" "1" "-f" "s16le" "pipe:1"] law/max-audio-bytes))
            size (.-length pcm)]
        (when (zero? size) (throw (ex-info "Audio has no samples" {:code "media_empty_audio"})))
        (mapv (fn [start]
                (let [end (min size (+ start law/audio-window-bytes))
                      wav (wav-bytes (.subarray pcm start end))]
                  {:part {:type "audio" :mimeType "audio/wav" :data (.toString wav "base64")}
                   :provenance (assoc original :normalized_sha256 (digest wav) :normalized_bytes (.-length wav)
                                      :start_ms (/ start 32) :end_ms (/ end 32)
                                      :duration_ms (/ size 32))}))
              (range 0 size law/audio-window-bytes))))))

(defn ^:async embed!
  "Send actual normalized media, never a caption surrogate, to configured Proxx."
  [part]
  (let [model (configured-model)
        payload (if (= "image" (:type part))
                  {:type "image_url" :image_url {:url (str "data:" (:mimeType part) ";base64," (:data part))}}
                  {:type "input_audio" :input_audio {:data (:data part) :format "wav"}})
        response (await (js/fetch (str (aget js/process.env "EMBED_PROVIDER_BASE_URL") "/v1/embeddings")
                                  #js {:method "POST" :signal (js/AbortSignal.timeout 90000)
                                       :headers #js {:content-type "application/json"
                                                     :authorization (str "Bearer " (aget js/process.env "EMBED_PROVIDER_API_KEY"))}
                                       :body (.stringify js/JSON (clj->js {:model model :input [{:content [payload]}]}))}))]
    (when-not (.-ok response)
      (throw (ex-info "Binary embedding provider rejected media" {:code "media_embedding_failed" :status (.-status response)})))
    (let [body (js->clj (await (.json response)) :keywordize-keys true)
          vector (get-in body [:data 0 :embedding])
          dimensions (js/Number (aget js/process.env "EMBED_PROVIDER_DIMENSIONS"))]
      (when-not (and (vector? vector) (= dimensions (count vector))
                     (every? #(and (number? %) (js/Number.isFinite %)) vector))
        (throw (ex-info "Invalid binary embedding vector" {:code "media_embedding_shape"})))
      {:embedding vector :model model :dimensions dimensions})))

(ns knoxx.backend.domain.media
  "Shared media infrastructure: workspace path resolution, temp files,
   mime-type detection, data-URL decoding, and source loading."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.document-state :refer [normalize-relative-path]]
            [knoxx.backend.domain.text :refer [tool-text-result]]
            [knoxx.backend.domain.actor.credentials :as actor-credentials]
            ["node:child_process" :refer [execFile]]
            ["node:crypto" :as crypto]
            ["node:fs/promises" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]
            ["node:util" :refer [promisify]]))

(def ^:private exec-file-async (promisify execFile))

(def workspace-media-max-bytes (* 20 1024 1024))
(def multimodal-upload-max-bytes (* 25 1024 1024))
(def audio-render-max-bytes (* 50 1024 1024))

(defn source-discord-cdn-url?
  "Detect Discord CDN attachment URLs that require bot token auth."
  [value]
  (some-> (str value) (str/includes? "cdn.discordapp.com/attachments")))

(defn- discord-bot-token!
  "Get Discord bot token from the current actor credential."
  [runtime]
  (-> (actor-credentials/get-credential! runtime "discord_bot")
      (.then (fn [credential]
               (actor-credentials/secret-value credential :botToken :bot-token :token)))))

;; -------------------------------------------------------------------------
;; Path / FS shims
;; -------------------------------------------------------------------------

(defn path-relative
  [^js node-path from to]
  (.relative node-path from to))

(defn path-basename
  [^js node-path path]
  (.basename node-path path))

(defn path-resolve
  [^js node-path & parts]
  (.apply (aget node-path "resolve") node-path (into-array parts)))

(defn path-is-absolute?
  [^js node-path path]
  (.isAbsolute node-path path))

(defn fs-read-file!
  ([^js node-fs path]
   (.readFile node-fs path))
  ([^js node-fs path encoding]
   (.readFile node-fs path encoding)))

(defn fs-write-file!
  ([^js node-fs path content]
   (.writeFile node-fs path content))
  ([^js node-fs path content encoding]
   (.writeFile node-fs path content encoding)))

(defn fs-mkdir!
  [^js node-fs path opts]
  (.mkdir node-fs path opts))

(defn fs-stat!
  [^js node-fs path]
  (.stat node-fs path))

(defn os-tmpdir
  [^js node-os]
  (.tmpdir node-os))

;; -------------------------------------------------------------------------
;; Workspace root resolution
;; -------------------------------------------------------------------------

(defn normalize-tool-path-arg
  [value]
  (some-> (str (or value ""))
          (str/replace #"^@" "")
          str/trim
          not-empty))

(defn normalize-relative-path-arg
  "Normalize a user-provided relative path while keeping the media namespace's
   path helpers qualified for tool call sites. The implementation delegates to
   document-state/normalize-relative-path, which is imported above."
  [value]
  (normalize-relative-path value))

(defn workspace-media-mime-type
  [path]
  (let [lower (str/lower-case (str path))]
    (cond
      (or (str/ends-with? lower ".png")
          (str/ends-with? lower ".apng")) "image/png"
      (or (str/ends-with? lower ".jpg")
          (str/ends-with? lower ".jpeg")) "image/jpeg"
      (str/ends-with? lower ".gif") "image/gif"
      (str/ends-with? lower ".webp") "image/webp"
      (str/ends-with? lower ".svg") "image/svg+xml"
      (or (str/ends-with? lower ".mp3")
          (str/ends-with? lower ".mpeg")) "audio/mpeg"
      (str/ends-with? lower ".wav") "audio/wav"
      (str/ends-with? lower ".ogg") "audio/ogg"
      (str/ends-with? lower ".m4a") "audio/mp4"
      (str/ends-with? lower ".flac") "audio/flac"
      (str/ends-with? lower ".aac") "audio/aac"
      (str/ends-with? lower ".mp4") "video/mp4"
      (str/ends-with? lower ".webm") "video/webm"
      (or (str/ends-with? lower ".mov")
          (str/ends-with? lower ".qt")) "video/quicktime"
      (str/ends-with? lower ".avi") "video/x-msvideo"
      (str/ends-with? lower ".pdf") "application/pdf"
      (str/ends-with? lower ".md") "text/markdown"
      (str/ends-with? lower ".txt") "text/plain"
      (str/ends-with? lower ".csv") "text/csv"
      (str/ends-with? lower ".json") "application/json"
      :else nil)))

(defn workspace-media-type
  [mime-type]
  (cond
    (str/starts-with? mime-type "image/") "image"
    (str/starts-with? mime-type "audio/") "audio"
    (str/starts-with? mime-type "video/") "video"
    :else "document"))

(defn- configured-media-root-records
  [node-path config]
  (let [music-root (some-> (:music-library-root config) str str/trim not-empty)
        extra-roots (->> (or (:extra-workspace-roots config) [])
                         (map (fn [raw]
                                (some-> raw str str/trim not-empty)))
                         (remove nil?))]
    (->> (concat
          (when music-root
            [{:alias "Music"
              :root (path-resolve node-path music-root)}])
          (map (fn [raw-root]
                 {:alias nil
                  :root (path-resolve node-path raw-root)})
               extra-roots))
         (reduce (fn [acc entry]
                   (if (some #(= (:root %) (:root entry)) acc)
                     acc
                     (conj acc entry)))
                 [])
         vec)))

(defn allowed-media-root-records
  [node-path config]
  (vec (cons {:alias nil
              :root (path-resolve node-path (:workspace-root config))}
             (configured-media-root-records node-path config))))

(defn- media-root-relative-path
  [node-path root absolute]
  (let [rel (path-relative node-path root absolute)]
    (when-not (or (str/starts-with? rel "..")
                  (path-is-absolute? node-path rel))
      rel)))

(defn resolve-workspace-media-path
  [_runtime config raw-path]
  (let [node-path path
        normalized (normalize-tool-path-arg raw-path)
        safe-path (or normalized "")
        roots (allowed-media-root-records node-path config)
        music-root (some #(when (= "Music" (:alias %)) %) roots)
        absolute (cond
                   (path-is-absolute? node-path safe-path)
                   (path-resolve node-path safe-path)

                   (and normalized
                        music-root
                        (or (= normalized "Music")
                            (str/starts-with? normalized "Music/")))
                   (let [suffix (subs normalized (min (count normalized) (count "Music/")))]
                     (path-resolve node-path (:root music-root) suffix))

                   :else
                   (path-resolve node-path (:workspace-root config) safe-path))
        matched-root (some (fn [root-record]
                             (when-let [rel (media-root-relative-path node-path (:root root-record) absolute)]
                               {:root-record root-record
                                :rel rel}))
                           roots)
        root-record (:root-record matched-root)
        rel-to-root (:rel matched-root)
        relative (when root-record
                   (if-let [alias (:alias root-record)]
                     (if (str/blank? rel-to-root)
                       alias
                       (str alias "/" rel-to-root))
                     rel-to-root))]
    (when (or (str/blank? normalized)
              (nil? matched-root)
              (str/blank? relative))
      (throw (js/Error. "Path escapes allowed workspace roots")))
    {:workspace-root (:root root-record)
     :absolute absolute
     :relative relative}))

;; -------------------------------------------------------------------------
;; Source loading
;; -------------------------------------------------------------------------

(defn source-http-url?
  [value]
  (boolean (re-matches #"https?://.+" (str (or value "")))))

(defn source-data-url?
  [value]
  (str/starts-with? (str (or value "")) "data:"))

(defn source-file-url?
  [value]
  (str/starts-with? (str (or value "")) "file://"))

(defn sanitize-mime-type
  [value fallback]
  (let [raw (some-> value str str/trim not-empty)
        trimmed (when raw (first (str/split raw #";")))]
    (or trimmed fallback "application/octet-stream")))

(defn mime-type->extension
  [mime-type]
  (case (sanitize-mime-type mime-type nil)
    "image/png" ".png"
    "image/jpeg" ".jpg"
    "image/gif" ".gif"
    "image/webp" ".webp"
    "image/svg+xml" ".svg"
    "audio/mpeg" ".mp3"
    "audio/wav" ".wav"
    "audio/ogg" ".ogg"
    "audio/mp4" ".m4a"
    "audio/flac" ".flac"
    "audio/aac" ".aac"
    "video/mp4" ".mp4"
    "video/webm" ".webm"
    "video/quicktime" ".mov"
    "video/x-msvideo" ".avi"
    "application/pdf" ".pdf"
    "text/plain" ".txt"
    "text/markdown" ".md"
    "text/csv" ".csv"
    "application/json" ".json"
    ".bin"))

(defn ensure-source-size!
  [size max-bytes label]
  (when (> size max-bytes)
    (throw (js/Error. (str label " exceeds " max-bytes " bytes. Choose a smaller file or summarize it instead.")))))

(defn buffer->data-url
  [buffer mime-type]
  (str "data:" (sanitize-mime-type mime-type "application/octet-stream") ";base64," (.toString buffer "base64")))

(defn temp-media-dir!
  [_runtime name]
  (let [dir (.join path (os-tmpdir os) "knoxx-media" name)]
    (-> (fs-mkdir! fs dir #js {:recursive true})
        (.then (fn [] dir)))))

(defn temp-file-path!
  [runtime name ext]
  (-> (temp-media-dir! runtime name)
      (.then (fn [dir]
               (.join path
                      dir
                      (str (.randomUUID crypto) (or ext "")))))))

(defn- decode-data-url-source
  [raw-source]
  (let [match (.match (str raw-source) #"^data:([^;,]+)?(;base64)?,(.*)$")]
    (when-not match
      (throw (js/Error. "Invalid data URL source")))
    (let [mime-type (sanitize-mime-type (aget match 1) "application/octet-stream")
          base64? (= ";base64" (or (aget match 2) ""))
          payload (or (aget match 3) "")
          buffer (if base64?
                   (.from js/Buffer payload "base64")
                   (.from js/Buffer (js/decodeURIComponent payload) "utf8"))]
      {:buffer buffer
       :mime-type mime-type
       :filename (str "upload" (mime-type->extension mime-type))
       :size (.-length buffer)
       :source-kind "data_url"})))

(defn infer-upload-filename
  [url idx]
  (let [pathname (try (.-pathname (js/URL. (str url))) (catch :default _ ""))
        candidate (some-> pathname (str/split #"/") last str/trim not-empty)]
    (or candidate (str "attachment-" idx ".bin"))))

(defn load-media-source!
  [runtime config raw-source max-bytes]
  (let [source (or (normalize-tool-path-arg raw-source) raw-source "")]
    (cond
      (str/blank? (str source))
      (js/Promise.reject (js/Error. "source is required"))

      (source-data-url? source)
      (js/Promise.resolve
       (let [decoded (decode-data-url-source source)]
         (ensure-source-size! (:size decoded) max-bytes "Source media")
         decoded))

      (source-http-url? source)
      (let [token-promise (if (source-discord-cdn-url? source)
                            (discord-bot-token! runtime)
                            (js/Promise.resolve nil))]
        (-> token-promise
            (.then (fn [token]
                     (let [headers #js {"User-Agent" "Knoxx-Agent/1.0"}]
                       (when token
                         (aset headers "Authorization" (str "Bot " token)))
                       (js/fetch source #js {:headers headers}))))
            (.then (fn [resp]
                     (if-not (.-ok resp)
                       (-> (.text resp)
                           (.then (fn [text]
                                    (throw (js/Error. (str "Failed to fetch source " source " (" (.-status resp) "): " text))))))
                       (let [mime-type (sanitize-mime-type (.get (.-headers resp) "content-type")
                                                           (workspace-media-mime-type source))
                             filename (infer-upload-filename source 0)]
                         (-> (.arrayBuffer resp)
                             (.then (fn [buf]
                                      (let [buffer (.from js/Buffer buf)
                                            size (.-length buffer)]
                                        (ensure-source-size! size max-bytes "Source media")
                                        {:buffer buffer
                                         :mime-type mime-type
                                         :filename filename
                                         :size size
                                         :source-kind "url"}))))))))))

      :else
      (let [{:keys [absolute relative]} (resolve-workspace-media-path runtime config source)
            mime-type (sanitize-mime-type (workspace-media-mime-type relative)
                                          (workspace-media-mime-type absolute))]
        (-> (fs-stat! fs absolute)
            (.then (fn [stat]
                     (when-not (.isFile stat)
                       (throw (js/Error. (str relative " is not a file"))))
                     (ensure-source-size! (.-size stat) max-bytes relative)
                     (fs-read-file! fs absolute)))
            (.then (fn [buffer]
                     {:absolute-path absolute
                      :relative relative
                      :buffer buffer
                      :mime-type mime-type
                      :filename (path-basename path absolute)
                      :size (.-length buffer)
                      :source-kind "workspace"})))))))

(defn materialize-media-source!
  [runtime config raw-source max-bytes]
  (-> (load-media-source! runtime config raw-source max-bytes)
      (.then (fn [source]
               (if (:absolute-path source)
                 source
                 (-> (temp-file-path! runtime "inputs" (mime-type->extension (:mime-type source)))
                     (.then (fn [absolute-path]
                              (-> (fs-write-file! fs absolute-path (:buffer source))
                                  (.then (fn []
                                           (assoc source :absolute-path absolute-path))))))))))))

(defn media-source->content-part!
  [runtime config raw-source max-bytes]
  (-> (load-media-source! runtime config raw-source max-bytes)
      (.then (fn [source]
               (let [mime-type (sanitize-mime-type (:mime-type source) "application/octet-stream")
                     part-type (workspace-media-type mime-type)]
                 {:source source
                  :part {:type part-type
                         :data (buffer->data-url (:buffer source) mime-type)
                         :mimeType mime-type
                         :filename (:filename source)
                         :size (:size source)}})))))

;; -------------------------------------------------------------------------
;; Audio visualization
;; -------------------------------------------------------------------------

(defn clamp-dimension
  [value fallback min-value max-value]
  (let [n (if (number? value) value fallback)]
    (-> n (max min-value) (min max-value))))

(defn audio-visualization-result!
  [runtime config raw-source {:keys [kind width height title]}]
  (let [label (case kind
                :waveform "waveform"
                "spectrogram")
        out-width (clamp-dimension width (if (= kind :waveform) 1200 1024) 256 4096)
        out-height (clamp-dimension height (if (= kind :waveform) 320 640) 128 2048)]
    (-> (materialize-media-source! runtime config raw-source audio-render-max-bytes)
        (.then
         (fn [source]
           (let [base-name (or title
                               (some-> (:filename source) (str/replace #"\.[^.]+$" ""))
                               "audio")]
             (-> (temp-file-path! runtime "renders" ".png")
                 (.then
                  (fn [output-path]
                    (let [filter-expr (if (= kind :waveform)
                                        (str "showwavespic=s=" out-width "x" out-height ":colors=0x7dd3fc")
                                        (str "showspectrumpic=s=" out-width "x" out-height ":legend=disabled"))
                          args (clj->js ["-y"
                                         "-i" (:absolute-path source)
                                         "-lavfi" filter-expr
                                         "-frames:v" "1"
                                         output-path])]
                      (-> (exec-file-async "ffmpeg" args #js {:timeout 120000 :maxBuffer 1048576})
                          (.then (fn [_]
                                   (fs-read-file! fs output-path)))
                          (.then
                           (fn [buffer]
                             (let [filename (str base-name "-" label ".png")
                                   part {:type "image"
                                         :data (buffer->data-url buffer "image/png")
                                         :mimeType "image/png"
                                         :filename filename
                                         :size (.-length buffer)}]
                               (tool-text-result
                                (str "Rendered " label " for " (or (:filename source) (:relative source) raw-source) ".")
                                {:source raw-source
                                 :kind label
                                 :content_parts [part]})))))))))))))))

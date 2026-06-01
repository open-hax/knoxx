(ns knoxx.backend.domain.media.remote-client
  "Remote media fetch client protocol.

   Covers arbitrary URL downloads for workspace media, Discord attachments,
   SVG/image/audio/video references, and agent media materialization. The
   implementation owns user-agent headers, size limits, content-type handling,
   and native fetch/Response details."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fetch :as xfetch]
            [promesa.core :as p]))

(defprotocol IRemoteMediaClient
  (fetch-bytes! [client url opts])
  (fetch-text! [client url opts])
  (fetch-data-url! [client url opts]))

(defn- sanitize-mime-type
  [value fallback]
  (let [raw (some-> value str str/trim not-empty)
        trimmed (when raw (first (str/split raw #";")))]
    (or trimmed fallback "application/octet-stream")))

(defn- mime-type->extension
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

(defn- infer-upload-filename
  [url idx mime-type]
  (let [pathname (try (.-pathname (js/URL. (str url))) (catch :default _ ""))
        candidate (some-> pathname (str/split #"/") last str/trim not-empty)]
    (or candidate (str "attachment-" idx (mime-type->extension mime-type)))))

(defn- ensure-source-size!
  [size max-bytes label]
  (when (and max-bytes (> size max-bytes))
    (throw (js/Error. (str label " exceeds " max-bytes " bytes. Choose a smaller file or summarize it instead.")))))

(defn- base-headers
  [opts]
  (cond-> {"User-Agent" (or (:user-agent opts) "Knoxx-Agent/1.0")}
    (:authorization opts) (assoc "Authorization" (:authorization opts))))

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

(defrecord FetchRemoteMediaClient [http-client]
  IRemoteMediaClient
  (fetch-bytes! [_ url opts]
    (p/let [resp (xfetch/array-buffer! (or http-client xfetch/default-client)
                                       {:url url
                                        :opts {:method "GET"
                                               :headers (merge (base-headers opts) (or (:headers opts) {}))}
                                        :timeout-ms (or (:timeout-ms opts) 60000)})]
      (if (:ok resp)
        (let [mime-type (sanitize-mime-type (get (:headers resp) "content-type") (:fallback-mime-type opts))
              buffer (.from js/Buffer (js/Uint8Array. (:body resp)))
              size (.-length buffer)]
          (ensure-source-size! size (:max-bytes opts) (or (:label opts) "Remote media"))
          {:buffer buffer
           :mime-type mime-type
           :filename (or (:filename opts) (infer-upload-filename url 0 mime-type))
           :size size
           :source-kind "url"
           :source-url url})
        (throw (js/Error. (str "Failed to fetch source " url " (" (:status resp) "): " (pr-str (:body resp))))))))
  (fetch-text! [_ url opts]
    (p/let [resp (xfetch/text! (or http-client xfetch/default-client)
                               {:url url
                                :opts {:method "GET"
                                       :headers (merge (base-headers opts) (or (:headers opts) {}))}
                                :timeout-ms (or (:timeout-ms opts) 60000)})]
      (if (:ok resp)
        (:body resp)
        (throw (js/Error. (str "Failed to fetch text " url " (" (:status resp) "): " (:body resp)))))))
  (fetch-data-url! [this url opts]
    (p/let [source (fetch-bytes! this url opts)]
      (str "data:" (:mime-type source) ";base64," (.toString (:buffer source) "base64")))))

(defn client
  ([] (client {}))
  ([{:keys [http-client]}]
   (->FetchRemoteMediaClient (or http-client xfetch/default-client))))

(defn decode-data-url!
  [raw-source opts]
  (let [decoded (decode-data-url-source raw-source)]
    (ensure-source-size! (:size decoded) (:max-bytes opts) (or (:label opts) "Source media"))
    decoded))

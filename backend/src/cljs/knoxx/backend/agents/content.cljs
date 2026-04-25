(ns knoxx.backend.agents.content
  "Content-part, media, and text helpers for agent turns."
  (:require [clojure.string :as str]
            [knoxx.backend.runtime.models :refer [model-supports-input?]]
            [knoxx.backend.text :refer [value->preview-text content-part-text clip-text]]))

(defn nonblank
  "Return s when it is a non-blank string (after trim)."
  [s]
  (when (string? s)
    (let [trimmed (str/trim s)]
      (when-not (str/blank? trimmed)
        trimmed))))

(defn preview-text-nonblank
  "Like value->preview-text, but returns nil for blank previews so OR chains keep searching."
  [value max-chars]
  (let [preview (some-> (value->preview-text value max-chars) nonblank)
        lowered (some-> preview str/lower-case)]
    (when-not (contains? #{"null" "undefined"} lowered)
      preview)))

(defn fenced
  [lang text]
  (str "```" lang "\n" (or text "") "\n```"))

(defn json-preview-nonblank
  [value max-chars]
  (when (and value (not= value js/undefined))
    (try
      (let [json (.stringify js/JSON value nil 2)]
        (when (string? json)
          (preview-text-nonblank json max-chars)))
      (catch :default _ nil))))

(defn diff-appended-text
  [previous current]
  (let [previous (str (or previous ""))
        current (str (or current ""))
        max-overlap (fn [left right]
                      (loop [n (min (count left) (count right))]
                        (cond
                          (zero? n) 0
                          (str/ends-with? left (.slice right 0 n)) n
                          :else (recur (dec n)))))]
    (cond
      (str/blank? current) ""
      (str/blank? previous) current
      (= current previous) ""
      (str/starts-with? current previous) (.slice current (count previous))
      :else (.slice current (max-overlap previous current)))))

(defn media-part-url
  [part]
  (or (nonblank (aget part "url"))
      (nonblank (aget part "file_url"))
      (nonblank (aget part "fileUrl"))
      (let [image-url (aget part "image_url")]
        (cond
          (string? image-url) (nonblank image-url)
          image-url (nonblank (aget image-url "url"))
          :else nil))
      (let [video-url (aget part "video_url")]
        (cond
          (string? video-url) (nonblank video-url)
          video-url (nonblank (aget video-url "url"))
          :else nil))
      (let [audio-url (aget part "audio_url")]
        (cond
          (string? audio-url) (nonblank audio-url)
          audio-url (nonblank (aget audio-url "url"))
          :else nil))
      (let [source (aget part "source")]
        (when (and source
                   (= "url" (some-> (aget source "type") str str/lower-case)))
          (nonblank (aget source "url"))))))

(defn media-part-data
  [part]
  (or (nonblank (aget part "data"))
      (nonblank (aget part "b64_json"))
      (nonblank (aget part "result"))
      (let [input-audio (aget part "input_audio")]
        (when input-audio
          (nonblank (aget input-audio "data"))))
      (let [output-audio (aget part "output_audio")]
        (when output-audio
          (nonblank (aget output-audio "data"))))
      (let [source (aget part "source")]
        (when (and source
                   (= "base64" (some-> (aget source "type") str str/lower-case)))
          (nonblank (aget source "data"))))))

(defn media-part-mime-type
  [part media-kind]
  (or (nonblank (aget part "mimeType"))
      (nonblank (aget part "mime_type"))
      (nonblank (aget part "mediaType"))
      (nonblank (aget part "media_type"))
      (let [source (aget part "source")]
        (when source
          (or (nonblank (aget source "media_type"))
              (nonblank (aget source "mime_type")))))
      (let [input-audio (aget part "input_audio")
            format (when input-audio
                     (nonblank (aget input-audio "format")))]
        (when format
          (str "audio/" format)))
      (let [output-audio (aget part "output_audio")
            format (when output-audio
                     (nonblank (aget output-audio "format")))]
        (when format
          (str "audio/" format)))
      (case media-kind
        "image" "image/png"
        "audio" "audio/wav"
        "video" "video/mp4"
        "document" "application/octet-stream"
        nil)))

(defn media-part-filename
  [part]
  (or (nonblank (aget part "filename"))
      (nonblank (aget part "file_name"))
      (nonblank (aget part "fileName"))
      (nonblank (aget part "name"))))

(defn media-part-size
  [part]
  (let [value (or (aget part "size")
                  (aget part "bytes")
                  (aget part "byte_size")
                  (aget part "byteSize"))]
    (when (number? value)
      value)))

(defn assistant-media-part
  [part]
  (let [raw-type (some-> (aget part "type") str str/lower-case)
        media-kind (cond
                     (contains? #{"image" "image_url" "input_image" "output_image"} raw-type) "image"
                     (contains? #{"audio" "audio_url" "input_audio" "output_audio"} raw-type) "audio"
                     (contains? #{"video" "video_url" "input_video" "output_video"} raw-type) "video"
                     (contains? #{"document" "file" "input_file" "output_file"} raw-type) "document"
                     :else nil)
        url (media-part-url part)
        raw-data (media-part-data part)
        mime-type (media-part-mime-type part media-kind)
        data (when raw-data
               (if (str/starts-with? raw-data "data:")
                 raw-data
                 (str "data:" mime-type ";base64," raw-data)))
        filename (media-part-filename part)
        size (media-part-size part)]
    (when (and media-kind (or url data))
      (cond-> {:type media-kind}
        url (assoc :url url)
        data (assoc :data data)
        mime-type (assoc :mimeType mime-type)
        filename (assoc :filename filename)
        size (assoc :size size)))))

(defn assistant-content-parts
  [assistant-message]
  (let [content (when assistant-message
                  (aget assistant-message "content"))]
    (if (array? content)
      (->> (array-seq content)
           (keep assistant-media-part)
           vec)
      [])))

(defn session-message-text
  [message]
  (let [content (aget message "content")]
    (cond
      (string? content) content
      (array? content) (->> (array-seq content)
                            (map content-part-text)
                            (remove str/blank?)
                            (str/join "\n\n"))
      (string? (aget message "text")) (aget message "text")
      :else "")))

(defn content-part-label
  [part]
  (let [part-type (cond
                    (keyword? (:type part)) (name (:type part))
                    (string? (:type part)) (:type part)
                    :else nil)]
    (case part-type
      "image" "image"
      "audio" "audio file"
      "video" "video"
      "document" "document"
      "attachment")))

(defn content-part-name
  [part]
  (or (:filename part)
      (:url part)
      (content-part-label part)))

(defn tool-result-media-type
  [value]
  (case (some-> value str str/lower-case)
    ("image" "image_url" "output_image") "image"
    ("audio" "audio_url" "output_audio") "audio"
    ("video" "video_url" "output_video") "video"
    ("document" "file" "output_file") "document"
    nil))

(defn tool-result-content-part
  [part]
  (let [media-type (tool-result-media-type (aget part "type"))
        data (nonblank (aget part "data"))
        url (nonblank (aget part "url"))
        mime-type (or (nonblank (aget part "mimeType"))
                      (nonblank (aget part "mime_type"))
                      (nonblank (aget part "mediaType"))
                      (nonblank (aget part "media_type")))
        filename (or (nonblank (aget part "filename"))
                     (nonblank (aget part "fileName"))
                     (nonblank (aget part "name")))
        size (let [value (or (aget part "size")
                             (aget part "bytes")
                             (aget part "byteSize")
                             (aget part "byte_size"))]
               (when (number? value)
                 value))]
    (when (and media-type (or data url))
      (cond-> {:type media-type}
        data (assoc :data data)
        url (assoc :url url)
        mime-type (assoc :mimeType mime-type)
        filename (assoc :filename filename)
        size (assoc :size size)))))

(defn tool-result-content-parts
  [tool-result]
  (let [details (when tool-result (aget tool-result "details"))
        raw-parts (or (when tool-result (aget tool-result "content_parts"))
                      (when tool-result (aget tool-result "contentParts"))
                      (when details (aget details "content_parts"))
                      (when details (aget details "contentParts"))
                      (when details (aget details "attachments")))]
    (if (array? raw-parts)
      (->> (array-seq raw-parts)
           (keep tool-result-content-part)
           vec)
      [])))

(defn merge-content-parts
  [& groups]
  (->> groups
       (mapcat #(or % []))
       (reduce (fn [acc part]
                 (if (some #(= % part) acc)
                   acc
                   (conj acc part)))
               [])
       vec))

(defn reply-attachment-content-parts
  [tool-receipts]
  (->> (or tool-receipts [])
       (filter #(= "workspace_media.attach" (:tool_name %)))
       (mapcat #(or (:content_parts %) (:contentParts %) []))
       vec))

(defn model-ready-content-parts
  [config model-id content-parts]
  (->> (or content-parts [])
       (mapcat (fn [part]
                 (let [part (if (map? part) part (js->clj part :keywordize-keys true))
                       part-type (cond
                                   (keyword? (:type part)) (name (:type part))
                                   (string? (:type part)) (:type part)
                                   :else nil)]
                   (cond
                     (or (nil? part-type)
                         (= part-type "text")
                         (model-supports-input? config model-id part-type))
                     [part]

                     (= part-type "audio")
                     [{:type :text
                       :text (str "Uploaded audio source '" (content-part-name part) "' is available, but model " model-id " does not declare audio input. Use audio.spectrogram if you need an image-friendly audio view.")}]

                     :else
                     [{:type :text
                       :text (str "Uploaded " (content-part-label part) " '" (content-part-name part) "' is available, but model " model-id " does not declare " part-type " input.")}]))))
       vec))

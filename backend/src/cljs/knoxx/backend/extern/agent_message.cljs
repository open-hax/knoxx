(ns knoxx.backend.extern.agent-message
  "JS shape boundary for eta-mu agent messages.
   Callers pass CLJS stored-message maps and receive eta-mu JS message/part
   objects. All #js/clj->js/aset construction for agent messages lives here."
  (:require [clojure.string :as str]))

(defn mime->audio-format
  [mime-type]
  (let [mime (some-> mime-type str str/lower-case)]
    (case mime
      "audio/mpeg" "mp3"
      "audio/mp4" "mp4"
      "audio/wav" "wav"
      "audio/x-wav" "wav"
      "audio/ogg" "ogg"
      "audio/flac" "flac"
      "audio/aac" "aac"
      (some-> mime (str/split #"/") second))))

(defn- data-url-raw
  [data]
  (let [comma (.indexOf data ",")]
    (if (>= comma 0) (.slice data (inc comma)) data)))

(defn- data-url-mime
  [data fallback]
  (or (second (re-find #"data:([^;,]+)" data)) fallback))

(defn- audio-part
  [data mime-type]
  #js {:type "audio"
       :data data
       :mimeType mime-type
       :format (or (mime->audio-format mime-type) "mp3")})

(defn content-part->agent-part
  [part]
  (let [part-type (some-> (:type part) str str/lower-case)
        text (some-> (:text part) str)
        url (some-> (:url part) str)
        data (some-> (:data part) str)
        mime-type (some-> (:mimeType part) str)
        filename (some-> (:filename part) str)]
    (case part-type
      "text" (when (not (str/blank? (str text)))
               #js {:type "text" :text text})

      ;; Image part routing for the eta-mu SDK:
      ;; eta-mu requires {type "image" :data <RAW-base64-no-prefix> :mimeType "image/..."}.
      ;; Never use image_url shape for already-materialized data.
      "image" (cond
                (and (string? data) (not (str/blank? data)) (str/starts-with? data "data:"))
                #js {:type "image"
                     :data (data-url-raw data)
                     :mimeType (data-url-mime data (or mime-type "image/png"))}

                (and (string? data) (not (str/blank? data)))
                #js {:type "image"
                     :data data
                     :mimeType (or mime-type "image/png")}

                (and (string? url) (not (str/blank? url)))
                #js {:type "image_url"
                     :image_url #js {:url url}}

                :else nil)

      "audio" (cond
                (and (string? data) (not (str/blank? data)) (str/starts-with? data "data:"))
                (let [mime (data-url-mime data (or mime-type "audio/mpeg"))]
                  (audio-part (data-url-raw data) mime))

                (and (string? data) (not (str/blank? data)))
                (audio-part data mime-type)

                (and (string? url) (not (str/blank? url)))
                (audio-part url mime-type)

                :else nil)

      "video" (when (not (str/blank? (str (or data url))))
                #js {:type "video"
                     :data (or data url)
                     :mimeType mime-type})

      "document" (when (not (str/blank? (str (or data url))))
                   #js {:type "document"
                        :data (or data url)
                        :mimeType mime-type
                        :filename filename})
      nil)))

(defn- text-payload
  [content]
  #js [#js {:type "text" :text content}])

(defn- default-usage
  []
  {:input 0
   :output 0
   :cacheRead 0
   :cacheWrite 0
   :totalTokens 0})

(defn stored-message->agent-message
  [message]
  (let [role (some-> (:role message) str)
        content (some-> (:content message) str)
        content-parts (vec (keep content-part->agent-part (or (:content-parts message) [])))
        payload (cond
                  (seq content-parts) (clj->js content-parts)
                  (not (str/blank? content)) (text-payload content)
                  :else nil)]
    (cond
      (= "compactionSummary" role)
      (when-let [summary (some-> (or (:summary message) (:content message)) str str/trim not-empty)]
        #js {:role "compactionSummary"
             :summary summary
             :tokensBefore (or (:tokensBefore message) (:tokens-before message) 0)
             :timestamp (.now js/Date)})

      (and (contains? #{"user" "assistant" "system"} role)
           payload)
      (let [agent-message #js {:role role
                               :content payload
                               :timestamp (.now js/Date)}]
        ;; Eta-mu auto-compaction's pre-prompt check assumes assistant messages
        ;; have a fully populated usage map. Rehydrated Knoxx transcripts often
        ;; predate usage persistence, so provide a zero-value sentinel instead
        ;; of crashing on `usage.totalTokens` before the next sticky turn.
        (when (= "assistant" role)
          (aset agent-message "usage" (clj->js (or (:usage message) (default-usage)))))
        agent-message))))

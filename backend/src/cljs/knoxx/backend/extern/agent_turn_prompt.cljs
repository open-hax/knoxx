(ns knoxx.backend.extern.agent-turn-prompt
  "JS boundary helpers for agent turn prompt content and prompt logging.
   Multipart content construction, array checks/conversion, log redaction, JSON
   serialization, console logging, and session.sendUserMessage live here.")

(defn prompt-content
  [media-parts final-text]
  (if (seq media-parts)
    (clj->js (conj (vec media-parts) {:type "text" :text final-text}))
    final-text))

(defn multipart?
  [content]
  (array? content))

(defn content-type
  [content]
  (if (multipart? content) "multipart" "text"))

(defn- redact-data
  [data]
  (let [data (str data)]
    (str "[img:sha="
         (.slice (.toString (.from js/Buffer data "base64") "hex") 0 12)
         " len=" (count data) "]")))

(defn- safe-part
  [part]
  (let [part-map (js->clj part :keywordize-keys true)
        data (:data part-map)]
    (if (and (string? data) (> (count data) 64))
      (clj->js (assoc part-map :data (redact-data data)))
      part)))

(defn safe-content
  [content]
  (if (multipart? content)
    (clj->js (mapv safe-part (array-seq content)))
    content))

(defn log-payload
  [{:keys [run-id session-id conversation-id model-id mode parts-count
           media-parts-count omitted-count content]}]
  #js {:run_id run-id
       :session_id session-id
       :conversation_id conversation-id
       :model_id model-id
       :mode mode
       :parts_count parts-count
       :media_parts_count media-parts-count
       :omitted_count omitted-count
       :content_type (content-type content)
       :content (safe-content content)})

(defn log-prompt!
  [opts]
  (js/console.log "[prompt-and-await!]"
                  (js/JSON.stringify (log-payload opts))))

(defn send-user-message!
  [^js session content]
  (.sendUserMessage session content))

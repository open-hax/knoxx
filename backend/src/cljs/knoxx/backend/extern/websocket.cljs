(ns knoxx.backend.extern.websocket
  "WebSocket boundary helpers. Owns readyState, JSON serialization, close, and events.")

(defn client-socket
  [socket]
  (or (aget socket "socket") socket))

(defn message-data->string
  [value]
  (cond
    (string? value) value
    (instance? js/Buffer value) (.toString value "utf8")
    (instance? js/Uint8Array value) (.toString (.from js/Buffer value) "utf8")
    :else (str value)))

(defn send-json!
  [socket payload]
  (when (= 1 (aget socket "readyState"))
    (.send socket (.stringify js/JSON (clj->js payload)))))

(defn close!
  ([socket] (close! socket 1000 ""))
  ([socket code reason]
   (when socket
     (try
       (.close socket code reason)
       (catch :default _ nil)))))

(defn on!
  [socket event-name handler]
  (.on socket event-name handler))

(defn voice-stream-event
  [payload]
  (let [audio (aget payload "audio")
        is-final (true? (aget payload "isFinal"))]
    (cond
      (string? audio)
      {:type "audio"
       :audio audio
       :alignment (js->clj (aget payload "alignment") :keywordize-keys true)
       :normalized_alignment (js->clj (aget payload "normalizedAlignment") :keywordize-keys true)}

      is-final
      {:type "final" :isFinal true}

      :else
      {:type "event"
       :payload (js->clj payload :keywordize-keys true)})))

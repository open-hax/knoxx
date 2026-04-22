(ns knoxx.backend.voice-routes
  (:require [clojure.string :as str]
            [knoxx.backend.http :as http]))

(def ^:private elevenlabs-base-url "https://api.elevenlabs.io/v1")
(def ^:private elevenlabs-ws-base-url "wss://api.elevenlabs.io/v1")
(def ^:private default-elevenlabs-voice-id "21m00Tcm4TlvDq8ikWAM")
(def ^:private default-elevenlabs-output-format "mp3_44100_128")
(def ^:private default-elevenlabs-stream-chunk-length-schedule #js [80 120 160 220])

(defn- trim-trailing-slashes
  [s]
  (str/replace (str (or s "")) #"/+$" ""))

(defn- stt-base-url
  [config]
  (-> (str (or (:stt-base-url config) ""))
      str/trim
      trim-trailing-slashes))

(defn- fetch-stt-json
  [base-url suffix opts]
  (http/fetch-json (str base-url suffix) opts))

(defn- trim-or-empty
  [value]
  (-> (str (or value "")) str/trim))

(defn- elevenlabs-api-key
  [config]
  (trim-or-empty (:elevenlabs-api-key config)))

(defn- elevenlabs-default-voice-id
  [config]
  (let [configured (trim-or-empty (:elevenlabs-voice-id config))]
    (if (str/blank? configured) default-elevenlabs-voice-id configured)))

(defn- elevenlabs-default-model-id
  [config]
  (let [configured (trim-or-empty (:elevenlabs-model-id config))]
    (if (str/blank? configured) "eleven_multilingual_v2" configured)))

(defn- elevenlabs-headers
  [api-key]
  #js {"Content-Type" "application/json"
       "Accept" "audio/mpeg"
       "xi-api-key" api-key})

(defn- elevenlabs-health-headers
  [api-key]
  #js {"Content-Type" "application/json"
       "xi-api-key" api-key})

(defn- elevenlabs-tts-url
  [voice-id output-format]
  (let [fmt (if (str/blank? output-format) default-elevenlabs-output-format output-format)]
    (str elevenlabs-base-url
         "/text-to-speech/"
         (js/encodeURIComponent (str voice-id))
         "?output_format="
         (js/encodeURIComponent (str fmt)))))

(declare ws-on! app-route!)

(defn- message-data->string
  [value]
  (cond
    (string? value) value
    (instance? js/Buffer value) (.toString value "utf8")
    (instance? js/Uint8Array value) (.toString (.from js/Buffer value) "utf8")
    :else (str value)))

(defn- ws-send-json!
  [socket payload]
  (when (= 1 (aget socket "readyState"))
    (.send socket (.stringify js/JSON (clj->js payload)))))

(defn- ws-close!
  ([socket] (ws-close! socket 1000 ""))
  ([socket code reason]
   (when socket
     (try
       (.close socket code reason)
       (catch :default _ nil)))))

(defn- elevenlabs-stream-model-id
  [config]
  (let [configured (trim-or-empty (:elevenlabs-model-id config))]
    (if (str/blank? configured) "eleven_multilingual_v2" configured)))

(defn- elevenlabs-stream-url
  [{:keys [voice-id model-id output-format auto-mode]}]
  (let [url (js/URL. (str elevenlabs-ws-base-url
                          "/text-to-speech/"
                          (js/encodeURIComponent (str voice-id))
                          "/stream-input"))
        params (.-searchParams url)]
    (.set params "model_id" (str model-id))
    (.set params "output_format" (str output-format))
    (.set params "auto_mode" (if auto-mode "true" "false"))
    (.set params "sync_alignment" "false")
    (.set params "inactivity_timeout" "180")
    (.toString url)))

(defn- normalize-elevenlabs-stream-text
  [value]
  (let [text (-> (str (or value ""))
                 (str/replace #"\s+" " ")
                 str/trim)]
    (cond
      (str/blank? text) ""
      (str/ends-with? text " ") text
      :else (str text " "))))

(defn- relay-elevenlabs-stream!
  [client payload]
  (let [audio (aget payload "audio")
        is-final (true? (aget payload "isFinal"))]
    (cond
      (string? audio)
      (ws-send-json! client {:type "audio"
                             :audio audio
                             :alignment (js->clj (aget payload "alignment") :keywordize-keys true)
                             :normalized_alignment (js->clj (aget payload "normalizedAlignment") :keywordize-keys true)})

      is-final
      (ws-send-json! client {:type "final" :isFinal true})

      :else
      (ws-send-json! client {:type "event"
                             :payload (js->clj payload :keywordize-keys true)}))))

(defn- register-voice-ws-route!
  [app config]
  (app-route! app
          #js {:method "GET"
               :url "/ws/voice/tts"
               :handler (fn [_request reply]
                          (-> (.code reply 426)
                              (.type "application/json")
                              (.send #js {:error "WebSocket upgrade required"})))
               :wsHandler (fn [socket _request]
                            (let [client (or (aget socket "socket") socket)
                                  upstream* (atom nil)
                                  close-upstream! (fn []
                                                    (when-let [upstream @upstream*]
                                                      (reset! upstream* nil)
                                                      (when (= 1 (aget upstream "readyState"))
                                                        (try
                                                          (.send upstream (.stringify js/JSON #js {:text ""}))
                                                          (catch :default _ nil)))
                                                      (ws-close! upstream 1000 "client_reset")))]
                              (ws-on! client "close" (fn [] (close-upstream!)))
                              (ws-on! client "error" (fn [_] (close-upstream!)))
                              (ws-on! client "message"
                                   (fn [data]
                                     (try
                                       (let [msg (.parse js/JSON (message-data->string data))
                                             kind (trim-or-empty (aget msg "type"))]
                                         (case kind
                                           "start"
                                           (let [api-key (elevenlabs-api-key config)
                                                 voice-id-raw (trim-or-empty (or (aget msg "voice_id")
                                                                                (aget msg "voiceId")
                                                                                ""))
                                                 voice-id (if (str/blank? voice-id-raw)
                                                            (elevenlabs-default-voice-id config)
                                                            voice-id-raw)
                                                 model-id-raw (trim-or-empty (or (aget msg "model_id")
                                                                                (aget msg "modelId")
                                                                                ""))
                                                 model-id (if (str/blank? model-id-raw)
                                                            (elevenlabs-stream-model-id config)
                                                            model-id-raw)
                                                 output-format-raw (trim-or-empty (or (aget msg "output_format")
                                                                                     (aget msg "outputFormat")
                                                                                     ""))
                                                 output-format (if (str/blank? output-format-raw)
                                                                 default-elevenlabs-output-format
                                                                 output-format-raw)
                                                 auto-mode (not= false (aget msg "auto_mode"))]
                                             (if (str/blank? api-key)
                                               (ws-send-json! client {:type "error"
                                                                      :detail "KNOXX_ELEVENLABS_API_KEY is not configured"})
                                               (let [url (elevenlabs-stream-url {:voice-id voice-id
                                                                                 :model-id model-id
                                                                                 :output-format output-format
                                                                                 :auto-mode auto-mode})
                                                     upstream (js/WebSocket. url)
                                                     voice-settings (or (aget msg "voice_settings")
                                                                        (aget msg "voiceSettings"))
                                                     generation-config (or (aget msg "generation_config")
                                                                           (aget msg "generationConfig"))]
                                                 (close-upstream!)
                                                 (reset! upstream* upstream)
                                                 (.addEventListener upstream "open"
                                                                    (fn []
                                                                      (let [init-payload #js {:text " "
                                                                                              :xi_api_key api-key
                                                                                              :generation_config (or generation-config
                                                                                                                     #js {:chunk_length_schedule default-elevenlabs-stream-chunk-length-schedule})}]
                                                                        (when voice-settings
                                                                          (aset init-payload "voice_settings" voice-settings))
                                                                        (.send upstream (.stringify js/JSON init-payload))
                                                                        (ws-send-json! client {:type "ready"
                                                                                               :voice_id voice-id
                                                                                               :model_id model-id
                                                                                               :output_format output-format}))))
                                                 (.addEventListener upstream "message"
                                                                    (fn [event]
                                                                      (try
                                                                        (relay-elevenlabs-stream! client (.parse js/JSON (message-data->string (aget event "data"))))
                                                                        (catch :default err
                                                                          (ws-send-json! client {:type "error"
                                                                                                 :detail (str "Failed to parse ElevenLabs stream payload: " err)})))))
                                                 (.addEventListener upstream "error"
                                                                    (fn [event]
                                                                      (ws-send-json! client {:type "error"
                                                                                             :detail (str "ElevenLabs stream error: " event)})))
                                                 (.addEventListener upstream "close"
                                                                    (fn [event]
                                                                      (when (identical? upstream @upstream*)
                                                                        (reset! upstream* nil))
                                                                      (ws-send-json! client {:type "upstream_closed"
                                                                                             :code (aget event "code")
                                                                                             :reason (aget event "reason")}))))))

                                           "text"
                                           (if-let [upstream @upstream*]
                                             (let [text (normalize-elevenlabs-stream-text (or (aget msg "text") ""))]
                                               (when-not (str/blank? text)
                                                 (.send upstream
                                                        (.stringify js/JSON
                                                                    (clj->js
                                                                     (cond-> {:text text}
                                                                       (= true (aget msg "try_trigger_generation")) (assoc :try_trigger_generation true)
                                                                       (= true (aget msg "tryTriggerGeneration")) (assoc :try_trigger_generation true)))))))
                                             (ws-send-json! client {:type "error" :detail "No active ElevenLabs voice stream"}))

                                           "flush"
                                           (if-let [upstream @upstream*]
                                             (.send upstream (.stringify js/JSON #js {:text " " :flush true}))
                                             (ws-send-json! client {:type "error" :detail "No active ElevenLabs voice stream"}))

                                           "close"
                                           (close-upstream!)

                                           (ws-send-json! client {:type "error"
                                                                  :detail (str "Unknown voice stream message type: " kind)})))
                                       (catch :default err
                                         (ws-send-json! client {:type "error"
                                                                :detail (str "Voice stream bridge failure: " err)})))))))}))

(defn- request-parts-promise
  [^js request]
  (.fromAsync js/Array (.parts request)))

(defn- reply-header!
  [^js reply name value]
  (.header reply name value))

(defn- ws-on!
  [^js socket event-name handler]
  (.on socket event-name handler))

(defn- app-route!
  [^js app opts]
  (.route app opts))

(defn register-voice-routes!
  [app runtime config handlers]
  (let [{:keys [route! json-response! with-request-context! ensure-tool!]} handlers]

    (register-voice-ws-route! app config)

    (route! app "GET" "/api/voice/stt/health"
            (fn [request reply]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (when ctx (ensure-tool! ctx "multimodal.upload"))
                  (let [base (stt-base-url config)]
                    (if (str/blank? base)
                      (json-response! reply 503 {:detail "KNOXX_STT_BASE_URL is not configured"})
                      (-> (fetch-stt-json base "/health" #js {:method "GET"})
                          (.then (fn [resp]
                                   (json-response! reply
                                                  (if (aget resp "ok") 200 502)
                                                  (js->clj (aget resp "body") :keywordize-keys true))))
                          (.catch (fn [err]
                                    (json-response! reply 502 {:detail (str "STT health failed: " err)}))))))))))

    (route! app "POST" "/api/voice/stt"
            (fn [request reply]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (when ctx (ensure-tool! ctx "multimodal.upload"))
                  (let [base (stt-base-url config)]
                    (if (str/blank? base)
                      (json-response! reply 503 {:detail "KNOXX_STT_BASE_URL is not configured"})
                      (let [promise
                            (-> (request-parts-promise request)
                                (.then
                                 (fn [parts]
                                   (let [part-seq (http/js-array-seq parts)
                                         file-part (first (filter #(= (aget % "type") "file") part-seq))]
                                     (if-not file-part
                                       #js {:error #js {:status 400
                                                        :detail "No file uploaded. Send multipart/form-data with a file part."}}
                                       (-> (.arrayBuffer (js/Response. (aget file-part "file")))
                                           (.then
                                            (fn [buf]
                                              (let [mime (or (aget file-part "mimetype")
                                                             (aget file-part "type")
                                                             "application/octet-stream")
                                                    headers #js {"Content-Type" (str mime)}
                                                    body (.from js/Buffer buf)]
                                                (fetch-stt-json
                                                 base
                                                 "/transcribe"
                                                 #js {:method "POST"
                                                      :headers headers
                                                      :body body})))))))))
                                (.then
                                 (fn [resp]
                                   (cond
                                     (and resp (aget resp "error"))
                                     (let [err (aget resp "error")]
                                       (json-response! reply (aget err "status") (js->clj err :keywordize-keys true)))

                                     (and resp (aget resp "ok"))
                                     (json-response! reply 200 (js->clj (aget resp "body") :keywordize-keys true))

                                     :else
                                     (json-response! reply 502 {:detail "STT service error"
                                                                :status (aget resp "status")
                                                                :body (js->clj (aget resp "body") :keywordize-keys true)}))))
                                (.catch
                                 (fn [err]
                                   (json-response! reply 500 {:detail (str "STT request failed: " err)}))))]
                        promise)))))))

    ;; ── TTS (ElevenLabs) ───────────────────────────────────────────────
    (route! app "GET" "/api/voice/tts/health"
            (fn [request reply]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (when ctx (ensure-tool! ctx "multimodal.upload"))
                  (let [api-key (elevenlabs-api-key config)]
                    (if (str/blank? api-key)
                      (json-response! reply 503 {:detail "KNOXX_ELEVENLABS_API_KEY is not configured"})
                      (-> (http/fetch-json (str elevenlabs-base-url "/voices")
                                           #js {:method "GET"
                                                :headers (elevenlabs-health-headers api-key)})
                          (.then (fn [resp]
                                   (json-response!
                                    reply
                                    (if (aget resp "ok") 200 502)
                                    {:provider "elevenlabs"
                                     :configured true
                                     :reachable (boolean (aget resp "ok"))
                                     :status_code (aget resp "status")
                                     :default_voice_id (elevenlabs-default-voice-id config)})))
                          (.catch (fn [err]
                                    (json-response! reply 502 {:detail (str "ElevenLabs health failed: " err)}))))))))))

    (route! app "POST" "/api/voice/tts"
            (fn [request reply]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (when ctx (ensure-tool! ctx "multimodal.upload"))
                  (let [api-key (elevenlabs-api-key config)
                        body (or (aget request "body") #js {})
                        text (-> (or (aget body "text") "") str)
                        voice-id-raw (trim-or-empty (or (aget body "voice_id") (aget body "voiceId") ""))
                        voice-id (if (str/blank? voice-id-raw)
                                   (elevenlabs-default-voice-id config)
                                   voice-id-raw)
                        model-id-raw (trim-or-empty (or (aget body "model_id") (aget body "modelId") ""))
                        model-id (if (str/blank? model-id-raw)
                                   (elevenlabs-default-model-id config)
                                   model-id-raw)
                        output-format (trim-or-empty (or (aget body "output_format") (aget body "outputFormat") ""))
                        voice-settings (aget body "voice_settings")]
                    (cond
                      (str/blank? api-key)
                      (json-response! reply 503 {:detail "KNOXX_ELEVENLABS_API_KEY is not configured"})

                      (str/blank? (str/trim text))
                      (json-response! reply 400 {:detail "Missing required field: text"})

                      :else
                      (let [payload (clj->js
                                     (cond-> {:text text
                                              :model_id model-id}
                                       (and voice-settings (not (nil? voice-settings)))
                                       (assoc :voice_settings (js->clj voice-settings))))
                            url (elevenlabs-tts-url voice-id output-format)
                            opts #js {:method "POST"
                                      :headers (elevenlabs-headers api-key)
                                      :body (.stringify js/JSON payload)}]
                        (-> (js/fetch url opts)
                            (.then
                             (fn [resp]
                               (if (.-ok resp)
                                 (do
                                   ;; Ensure client caches are not poisoned by identical text across contexts.
                                   (reply-header! reply "Cache-Control" "no-store")
                                   (http/send-fetch-response! reply resp))
                                 (-> (.text resp)
                                     (.then (fn [detail]
                                              (json-response!
                                               reply
                                               (.-status resp)
                                               {:detail (str "ElevenLabs TTS failed: " detail)
                                                :status_code (.-status resp)})))))))
                            (.catch
                             (fn [err]
                               (json-response! reply 502 {:detail (str "ElevenLabs TTS request failed: " err)})))))))))))

    nil))

(defn register-voice-routes
  [app runtime config handlers]
  (register-voice-routes! app runtime config handlers))

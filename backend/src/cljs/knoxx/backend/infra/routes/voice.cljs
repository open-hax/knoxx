(ns knoxx.backend.infra.routes.voice
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fastify :as xfastify]
            [knoxx.backend.extern.multipart :as xmultipart]
            [knoxx.backend.extern.websocket :as xws]
            [knoxx.backend.infra.http :as http]))

(def ^:private default-voxx-voice-id "af_jessica")
(def ^:private default-voxx-model-id "kokoro")
(def ^:private default-voxx-speed "1.15")
(def ^:private default-voxx-output-format "mp3")
(def ^:private default-voxx-postprocess-profile "sports-commentator-v1")

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

(defn- false-like?
  [value]
  (or (= false value)
      (contains? #{"0" "false" "no" "off" "disabled" "disable" "none"}
                 (-> (str (or value "")) str/trim str/lower-case))))

(defn- bool-value
  [value default]
  (if (nil? value) default (not (false-like? value))))

(defn- first-body-value
  [body names]
  (some (fn [name]
          (let [value (get body (keyword name))]
            (when-not (nil? value) value)))
        names))

(defn- voice-gateway-url
  [config]
  (let [configured (trim-or-empty (:voxx-url config))]
    (if (str/blank? configured)
      "http://127.0.0.1:8787"
      (trim-trailing-slashes configured))))

(defn- voxx-v1-url
  [config suffix]
  (let [base (voice-gateway-url config)]
    (cond
      (str/ends-with? base "/v1/audio/speech")
      (str/replace base #"/audio/speech$" suffix)

      (str/ends-with? base "/v1")
      (str base suffix)

      :else
      (str base "/v1" suffix))))

(defn- voice-gateway-api-key
  [config]
  (trim-or-empty (:voxx-api-key config)))

(defn- voxx-default-voice-id
  [config]
  (let [configured (trim-or-empty (:voxx-voice-id config))]
    (if (str/blank? configured) default-voxx-voice-id configured)))

(defn- voxx-default-model-id
  [config]
  (let [configured (trim-or-empty (:voxx-model-id config))]
    (if (str/blank? configured) default-voxx-model-id configured)))

(defn- voxx-default-speed
  [config]
  (let [configured (trim-or-empty (:voxx-default-speed config))]
    (if (str/blank? configured) default-voxx-speed configured)))

(defn- voxx-headers
  [api-key]
  {"Content-Type" "application/json"
   "Accept" "audio/mpeg"
   "Authorization" (str "Bearer " api-key)})

(defn- voxx-health-headers
  [api-key]
  {"Content-Type" "application/json"
   "Authorization" (str "Bearer " api-key)})

(defn- voxx-tts-url
  [config]
  (voxx-v1-url config "/audio/speech"))

(declare app-route!)

(defn- ws-send-json!
  [socket payload]
  (xws/send-json! socket payload))

(defn- ws-close!
  ([socket] (ws-close! socket 1000 ""))
  ([socket code reason]
   (xws/close! socket code reason)))


(defn- register-voice-ws-route!
  [app _config]
  (app-route! app
              {:method "GET"
               :url "/ws/voice/tts"
               :handler (fn [_request reply]
                          (xfastify/send-json! reply 426 {:error "WebSocket upgrade required"}))
               :wsHandler (fn [socket _request]
                            (let [client (xws/client-socket socket)]
                              (ws-send-json!
                               client
                               {:type "error"
                                :detail "Voxx streaming TTS is not exposed by this Knoxx bridge yet. Use voice.tts or POST /api/voice/tts for Voxx /v1/audio/speech."})
                              (ws-close! client 1000 "voxx_streaming_tts_unavailable")))}))

(defn- request-parts-promise
  [^js request]
  (xmultipart/parts! request))

(defn- reply-header!
  [^js reply name value]
  (xfastify/reply-header! reply name value))

(defn- app-route!
  [^js app opts]
  (xfastify/route! app opts))

(defn ^:async handle-stt-health!
  [config reply ctx json-response! ensure-tool!]
  (when ctx (ensure-tool! ctx "multimodal.upload"))
  (let [base (stt-base-url config)]
    (if (str/blank? base)
      (json-response! reply 503 {:detail "KNOXX_STT_BASE_URL is not configured"})
      (try
        (let [resp (await (fetch-stt-json base "/health" {:method "GET"}))]
          (json-response! reply (if (:ok resp) 200 502) (:body resp)))
        (catch :default err
          (json-response! reply 502 {:detail (str "STT health failed: " err)}))))))

(defn- register-stt-health-route!
  [app runtime config route! json-response! with-request-context! ensure-tool!]
  (route! app "GET" "/api/voice/stt/health"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (handle-stt-health! config reply ctx json-response! ensure-tool!))))))

(defn ^:async transcribe-file-part!
  [base file-part]
  (let [body (await (xmultipart/part-buffer! file-part))
        mime (xmultipart/part-mime-type file-part)]
    (await (fetch-stt-json base
                           "/transcribe"
                           {:method "POST"
                            :headers {"Content-Type" (str mime)}
                            :body body}))))

(defn ^:async stt-transcription-response!
  [base request]
  (let [parts (await (request-parts-promise request))
        file-part (first (xmultipart/file-parts parts))]
    (if-not file-part
      {:error {:status 400
               :detail "No file uploaded. Send multipart/form-data with a file part."}}
      (await (transcribe-file-part! base file-part)))))

(defn- send-stt-response!
  [reply json-response! resp]
  (cond
    (and resp (:error resp))
    (let [err (:error resp)]
      (json-response! reply (:status err) err))

    (and resp (:ok resp))
    (json-response! reply 200 (:body resp))

    :else
    (json-response! reply 502 {:detail "STT service error"
                               :status (:status resp)
                               :body (:body resp)})))

(defn ^:async handle-stt-transcribe!
  [config request reply ctx json-response! ensure-tool!]
  (when ctx (ensure-tool! ctx "multimodal.upload"))
  (let [base (stt-base-url config)]
    (if (str/blank? base)
      (json-response! reply 503 {:detail "KNOXX_STT_BASE_URL is not configured"})
      (try
        (send-stt-response! reply json-response! (await (stt-transcription-response! base request)))
        (catch :default err
          (json-response! reply 500 {:detail (str "STT request failed: " err)}))))))

(defn- register-stt-transcribe-route!
  [app runtime config route! json-response! with-request-context! ensure-tool!]
  (route! app "POST" "/api/voice/stt"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (handle-stt-transcribe! config request reply ctx json-response! ensure-tool!))))))

(defn- voxx-health-body
  [config resp]
  {:provider "voxx"
   :configured true
   :reachable (boolean (:ok resp))
   :status_code (:status resp)
   :default_voice_id (voxx-default-voice-id config)
   :default_model_id (voxx-default-model-id config)
   :default_speed (voxx-default-speed config)
   :default_postprocess_enabled true
   :default_postprocess_profile default-voxx-postprocess-profile
   :default_prompt_aware true})

(defn ^:async handle-tts-health!
  [config reply ctx json-response! ensure-tool!]
  (when ctx (ensure-tool! ctx "multimodal.upload"))
  (let [api-key (voice-gateway-api-key config)]
    (if (str/blank? api-key)
      (json-response! reply 503 {:detail "VOICE_GATEWAY_API_KEY is not configured"})
      (try
        (let [resp (await (http/fetch-json (voxx-v1-url config "/voices")
                                           {:method "GET"
                                            :headers (voxx-health-headers api-key)}))]
          (json-response! reply (if (:ok resp) 200 502) (voxx-health-body config resp)))
        (catch :default err
          (json-response! reply 502 {:detail (str "Voice Gateway health failed: " err)}))))))

(defn- register-tts-health-route!
  [app runtime config route! json-response! with-request-context! ensure-tool!]
  (route! app "GET" "/api/voice/tts/health"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (handle-tts-health! config reply ctx json-response! ensure-tool!))))))

(defn- configured-or-default
  [value default]
  (let [configured (trim-or-empty value)]
    (if (str/blank? configured) default configured)))

(defn- tts-base-payload
  [config body text]
  {:input text
   :voice (configured-or-default (or (:voice_id body) (:voiceId body))
                                 (voxx-default-voice-id config))
   :model (configured-or-default (or (:model_id body) (:modelId body) (:model body))
                                 (voxx-default-model-id config))
   :response_format (configured-or-default (or (:output_format body) (:outputFormat body)
                                               (:response_format body) (:responseFormat body))
                                           default-voxx-output-format)
   :speed (configured-or-default (first-body-value body ["speed"])
                                 (voxx-default-speed config))
   :postprocess_enabled (bool-value (first-body-value body ["postprocess_enabled" "postprocessEnabled"])
                                    true)
   :prompt_aware (bool-value (first-body-value body ["prompt_aware" "promptAware" "prompt-aware"])
                             true)})

(defn- tts-extra-payload
  [body]
  (let [postprocess-profile (configured-or-default (first-body-value body ["postprocess_profile"
                                                                            "postprocessProfile"
                                                                            "postprocess"])
                                                   default-voxx-postprocess-profile)
        prompt-aware-style (trim-or-empty (first-body-value body ["prompt_aware_style" "promptAwareStyle"]))]
    (cond-> {:postprocess_profile postprocess-profile}
      (not (str/blank? prompt-aware-style)) (assoc :prompt_aware_style prompt-aware-style)
      (some? (:voice_settings body)) (assoc :voice_settings (:voice_settings body)))))

(defn- tts-request-payload
  "Build the TTS request payload from the request body and config."
  [config body]
  (let [text (-> (or (:text body) "") str)]
    {:text text
     :payload (merge (tts-base-payload config body text)
                     (tts-extra-payload body))}))

(defn ^:async send-tts-response!
  [reply json-response! resp]
  (if (.-ok resp)
    (do
      (reply-header! reply "Cache-Control" "no-store")
      (http/send-fetch-response! reply resp))
    (let [detail (await (.text resp))]
      (json-response! reply
                      (.-status resp)
                      {:detail (str "Voice Gateway TTS failed: " detail)
                       :status_code (.-status resp)}))))

(defn ^:async handle-tts!
  [config request reply ctx json-response! ensure-tool!]
  (when ctx (ensure-tool! ctx "multimodal.upload"))
  (let [api-key (voice-gateway-api-key config)
        body (http/request-body request)
        {:keys [text payload]} (tts-request-payload config body)]
    (cond
      (str/blank? api-key)
      (json-response! reply 503 {:detail "VOICE_GATEWAY_API_KEY is not configured"})

      (str/blank? (str/trim text))
      (json-response! reply 400 {:detail "Missing required field: text"})

      :else
      (try
        (let [url (voxx-tts-url config)
              opts {:method "POST" :headers (voxx-headers api-key) :json payload}]
          (send-tts-response! reply json-response! (await (http/fetch-with-timeout url opts 30000))))
        (catch :default err
          (json-response! reply 502 {:detail (str "Voice Gateway TTS request failed: " err)}))))))

(defn- register-tts-route!
  [app runtime config route! json-response! with-request-context! ensure-tool!]
  (route! app "POST" "/api/voice/tts"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (handle-tts! config request reply ctx json-response! ensure-tool!))))))

(defn register-voice-routes!
  [app runtime config handlers]
  (let [{:keys [route! json-response! with-request-context! ensure-tool!]} handlers]
    (register-voice-ws-route! app config)
    (register-stt-health-route! app runtime config route! json-response! with-request-context! ensure-tool!)
    (register-stt-transcribe-route! app runtime config route! json-response! with-request-context! ensure-tool!)
    (register-tts-health-route! app runtime config route! json-response! with-request-context! ensure-tool!)
    (register-tts-route! app runtime config route! json-response! with-request-context! ensure-tool!)
    nil))

(defn register-voice-routes
  [app runtime config handlers]
  (register-voice-routes! app runtime config handlers))

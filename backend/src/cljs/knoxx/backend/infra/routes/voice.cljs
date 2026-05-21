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

(defn- voice-gateway-ws-url
  [config]
  (let [url (voice-gateway-url config)]
    (-> (str url)
        (str/replace "^https://" "wss://")
        (str/replace "^http://" "ws://"))))

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

(declare ws-on! app-route!)

(defn- message-data->string
  [value]
  (xws/message-data->string value))

(defn- ws-send-json!
  [socket payload]
  (xws/send-json! socket payload))

(defn- ws-close!
  ([socket] (ws-close! socket 1000 ""))
  ([socket code reason]
   (xws/close! socket code reason)))

(defn- normalize-voice-stream-text
  [value]
  (let [text (-> (str (or value ""))
                 (str/replace #"\s+" " ")
                 str/trim)]
    (cond
      (str/blank? text) ""
      (str/ends-with? text " ") text
      :else (str text " "))))

(defn- relay-voice-stream!
  [client payload]
  (ws-send-json! client (xws/voice-stream-event payload)))

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

(defn- ws-on!
  [^js socket event-name handler]
  (xws/on! socket event-name handler))

(defn- app-route!
  [^js app opts]
  (xfastify/route! app opts))

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
                      (-> (fetch-stt-json base "/health" {:method "GET"})
                          (.then (fn [resp]
                                   (json-response! reply
                                                  (if (:ok resp) 200 502)
                                                  (:body resp))))
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
                                   (let [file-part (first (xmultipart/file-parts parts))]
                                     (if-not file-part
                                       {:error {:status 400
                                               :detail "No file uploaded. Send multipart/form-data with a file part."}}
                                       (-> (xmultipart/part-buffer! file-part)
                                           (.then
                                            (fn [body]
                                              (let [mime (xmultipart/part-mime-type file-part)
                                                    headers {"Content-Type" (str mime)}]
                                                (fetch-stt-json
                                                 base
                                                 "/transcribe"
                                                 {:method "POST"
                                                  :headers headers
                                                  :body body})))))))))
                                (.then
                                 (fn [resp]
                                   (cond
                                     (and resp (:error resp))
                                     (let [err (:error resp)]
                                       (json-response! reply (:status err) err))

                                     (and resp (:ok resp))
                                     (json-response! reply 200 (:body resp))

                                     :else
                                     (json-response! reply 502 {:detail "STT service error"
                                                                :status (:status resp)
                                                                :body (:body resp)}))))
                                (.catch
                                 (fn [err]
                                   (json-response! reply 500 {:detail (str "STT request failed: " err)}))))]
                        promise)))))))

    ;; ── TTS (Voice Gateway) ───────────────────────────────────────────────
    (route! app "GET" "/api/voice/tts/health"
            (fn [request reply]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (when ctx (ensure-tool! ctx "multimodal.upload"))
                  (let [api-key (voice-gateway-api-key config)]
                    (if (str/blank? api-key)
                      (json-response! reply 503 {:detail "VOICE_GATEWAY_API_KEY is not configured"})
                      (-> (http/fetch-json (voxx-v1-url config "/voices")
                                           {:method "GET"
                                            :headers (voxx-health-headers api-key)})
                          (.then (fn [resp]
                                   (json-response!
                                    reply
                                    (if (:ok resp) 200 502)
                                    {:provider "voxx"
                                     :configured true
                                     :reachable (boolean (:ok resp))
                                     :status_code (:status resp)
                                     :default_voice_id (voxx-default-voice-id config)
                                     :default_model_id (voxx-default-model-id config)
                                     :default_speed (voxx-default-speed config)
                                     :default_postprocess_enabled true
                                     :default_postprocess_profile default-voxx-postprocess-profile
                                     :default_prompt_aware true})))
                          (.catch (fn [err]
                                    (json-response! reply 502 {:detail (str "Voice Gateway health failed: " err)}))))))))))

    (route! app "POST" "/api/voice/tts"
            (fn [request reply]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (when ctx (ensure-tool! ctx "multimodal.upload"))
                  (let [api-key (voice-gateway-api-key config)
                        body (http/request-body request)
                        text (-> (or (:text body) "") str)
                        voice-id-raw (trim-or-empty (or (:voice_id body) (:voiceId body) ""))
                        voice-id (if (str/blank? voice-id-raw)
                                   (voxx-default-voice-id config)
                                   voice-id-raw)
                        model-id-raw (trim-or-empty (or (:model_id body)
                                                        (:modelId body)
                                                        (:model body)
                                                        ""))
                        model-id (if (str/blank? model-id-raw)
                                   (voxx-default-model-id config)
                                   model-id-raw)
                        output-format-raw (trim-or-empty (or (:output_format body)
                                                             (:outputFormat body)
                                                             (:response_format body)
                                                             (:responseFormat body)
                                                             ""))
                        output-format (if (str/blank? output-format-raw)
                                        default-voxx-output-format
                                        output-format-raw)
                        speed-raw (trim-or-empty (first-body-value body ["speed"]))
                        speed (if (str/blank? speed-raw)
                                (voxx-default-speed config)
                                speed-raw)
                        postprocess-profile-raw (trim-or-empty (first-body-value body ["postprocess_profile"
                                                                                       "postprocessProfile"
                                                                                       "postprocess"]))
                        postprocess-profile (if (str/blank? postprocess-profile-raw)
                                              default-voxx-postprocess-profile
                                              postprocess-profile-raw)
                        postprocess-enabled (bool-value (first-body-value body ["postprocess_enabled"
                                                                                "postprocessEnabled"])
                                                        true)
                        prompt-aware (bool-value (first-body-value body ["prompt_aware"
                                                                         "promptAware"
                                                                         "prompt-aware"])
                                                 true)
                        prompt-aware-style (trim-or-empty (first-body-value body ["prompt_aware_style"
                                                                                  "promptAwareStyle"]))
                        voice-settings (:voice_settings body)]
                    (cond
                      (str/blank? api-key)
                      (json-response! reply 503 {:detail "VOICE_GATEWAY_API_KEY is not configured"})

                      (str/blank? (str/trim text))
                      (json-response! reply 400 {:detail "Missing required field: text"})

                      :else
                      (let [payload (cond-> {:input text
                                             :voice voice-id
                                             :model model-id
                                             :response_format output-format
                                             :speed speed
                                             :postprocess_enabled postprocess-enabled
                                             :prompt_aware prompt-aware}
                                      postprocess-profile
                                      (assoc :postprocess_profile postprocess-profile)

                                      (not (str/blank? prompt-aware-style))
                                      (assoc :prompt_aware_style prompt-aware-style)

                                      (and voice-settings (not (nil? voice-settings)))
                                      (assoc :voice_settings voice-settings))
                            url (voxx-tts-url config)
                            opts {:method "POST"
                                  :headers (voxx-headers api-key)
                                  :json payload}]
                        (-> (http/fetch-with-timeout url opts 30000)
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
                                               {:detail (str "Voice Gateway TTS failed: " detail)
                                                :status_code (.-status resp)})))))))
                            (.catch
                             (fn [err]
                               (json-response! reply 502 {:detail (str "Voice Gateway TTS request failed: " err)})))))))))))

    nil))

(defn register-voice-routes
  [app runtime config handlers]
  (register-voice-routes! app runtime config handlers))

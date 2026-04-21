(ns knoxx.backend.voice-routes
  (:require [clojure.string :as str]
            [knoxx.backend.http :as http]))

(def ^:private elevenlabs-base-url "https://api.elevenlabs.io/v1")
(def ^:private default-elevenlabs-voice-id "21m00Tcm4TlvDq8ikWAM")
(def ^:private default-elevenlabs-output-format "mp3_44100_128")

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

(defn register-voice-routes!
  [app runtime config handlers]
  (let [{:keys [route! json-response! with-request-context! ensure-tool!]} handlers]

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
                            (-> (.fromAsync js/Array (.parts request))
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
                                   (.header reply "Cache-Control" "no-store")
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

(ns knoxx.backend.domain.voice.client
  "Voice gateway/STT client protocols.

   Covers Voxx/Kokoro TTS and local STT transcription requests. Concrete
   clients own HTTP request construction, response parsing, and native fetch
   access via `extern.fetch`."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fetch :as xfetch]
            [knoxx.backend.extern.json :as xjson]
            [promesa.core :as p]))

(defprotocol ITtsClient
  (synthesize! [client {:keys [text voice-id model-id response-format speed options]}]))

(defprotocol ISttClient
  (transcribe! [client audio-buffer opts]))

(defn- trim-trailing-slashes
  [s]
  (str/replace (str (or s "")) #"/+$" ""))

(defn- blank->nil
  [v]
  (let [s (str/trim (str (or v "")))]
    (when-not (str/blank? s) s)))

(defn- config-value
  [config keyword-key js-key camel-key]
  (or (when (map? config) (get config keyword-key))
      (when-not (map? config) (aget config js-key))
      (when-not (map? config) (aget config camel-key))))

(defn- resolve-voice-key
  [config]
  (or (blank->nil (config-value config :voxx-api-key "voxx-api-key" "voxxApiKey"))
      (some-> js/process .-env (aget "VOICE_GATEWAY_API_KEY") blank->nil)
      (some-> js/process .-env (aget "KNOXX_VOICE_GATEWAY_API_KEY") blank->nil)))

(defn- voice-gateway-url
  [config]
  (or (blank->nil (config-value config :voxx-url "voxx-url" "voxxUrl"))
      (some-> js/process .-env (aget "VOXX_URL") blank->nil)
      "http://127.0.0.1:8787"))

(defn- tts-url
  [config]
  (let [base (trim-trailing-slashes (voice-gateway-url config))]
    (cond
      (str/ends-with? base "/v1/audio/speech") base
      (str/ends-with? base "/v1") (str base "/audio/speech")
      :else (str base "/v1/audio/speech"))))

(defn- default-tts-speed
  [config]
  (or (blank->nil (config-value config :voxx-default-speed "voxx-default-speed" "voxxDefaultSpeed"))
      (some-> js/process .-env (aget "KNOXX_VOXX_DEFAULT_SPEED") blank->nil)
      (some-> js/process .-env (aget "VOICE_GATEWAY_TTS_DEFAULT_SPEED") blank->nil)
      "1.15"))

(defn- stt-url
  [config]
  (or (when (map? config) (or (get config :stt-url)
                              (get config :stt-base-url)))
      (when-not (map? config) (or (aget config "stt-url")
                                  (aget config "sttUrl")
                                  (aget config "stt-base-url")
                                  (aget config "sttBaseUrl")))
      (some-> js/process .-env (aget "KNOXX_STT_URL") blank->nil)
      (some-> js/process .-env (aget "KNOXX_STT_BASE_URL") blank->nil)
      "http://127.0.0.1:8010"))

(defn- tts-body
  [config {:keys [text voice-id model-id response-format speed options]}]
  (let [{:keys [postprocess-profile postprocess-enabled prompt-aware prompt-aware-style voice-settings]} options]
    (cond-> {:input text
             :voice (or voice-id "af_jessica")
             :model (or model-id "kokoro")
             :response_format (or response-format "mp3")
             :speed (or speed (default-tts-speed config))
             :postprocess_enabled (if (some? postprocess-enabled) postprocess-enabled false)}
      postprocess-profile (assoc :postprocess_profile postprocess-profile)
      (some? prompt-aware) (assoc :prompt_aware prompt-aware)
      prompt-aware-style (assoc :prompt_aware_style prompt-aware-style)
      (seq voice-settings) (assoc :voice_settings voice-settings))))

(defn- parse-stt-json-text
  [raw]
  (let [s (str/trim (str raw))]
    (if (str/includes? s "data:")
      (let [lines (->> (str/split-lines s)
                       (keep (fn [line]
                               (let [t (str/trim line)]
                                 (when (str/starts-with? t "data:")
                                   (str/trim (subs t 5)))))))
            parsed-lines (keep xjson/parse-object lines)
            segments (keep (fn [j]
                             (let [txt (or (:text j) (:transcription j) "")]
                               (when (seq txt) txt)))
                           parsed-lines)
            final-segment (last parsed-lines)]
        {:text (str/trim (str/join " " segments))
         :final (or (:final final-segment) true)})
      (or (xjson/parse-object s) {}))))

(defn- stt-text-garbage?
  "Detect repetitive/garbage STT output (e.g. NPU KV-cache stuck)."
  [text]
  (when (seq text)
    (let [t (str/trim text)]
      (and (> (count t) 10)
           (let [chars (set (remove #(or (= % " ") (= % "\n")) t))]
             (<= (count chars) 2))
           (not (re-find #"[a-zA-Z0-9]" t))))))

(defrecord FetchTtsClient [config http-client]
  ITtsClient
  (synthesize! [_ request]
    (let [api-key (or (resolve-voice-key config)
                      (throw (js/Error. "VOICE_GATEWAY_API_KEY not configured")))
          body (tts-body config request)]
      (p/let [resp (xfetch/array-buffer! (or http-client xfetch/default-client)
                                         {:url (tts-url config)
                                          :opts {:method "POST"
                                                 :headers {"Authorization" (str "Bearer " api-key)
                                                           "Content-Type" "application/json"
                                                           "Accept" "audio/mpeg"}
                                                 :body (xjson/stringify body)}
                                          :timeout-ms 60000})]
        (if (:ok resp)
          (.from js/Buffer (js/Uint8Array. (:body resp)))
          (throw (js/Error. (str "TTS " (:status resp)))))))))

(defrecord FetchSttClient [config http-client]
  ISttClient
  (transcribe! [_ audio-buffer _opts]
    (let [url (str (trim-trailing-slashes (stt-url config)) "/transcribe")]
      (js/console.log "[voice:stt] === TRANSCRIBE START ===" (.-length audio-buffer) "bytes from" (stt-url config))
      (js/console.log "[voice:stt] sending POST to" url)
      (p/catch
        (p/let [resp (xfetch/text! (or http-client xfetch/default-client)
                                   {:url url
                                    :opts {:method "POST"
                                           :headers {"Content-Type" "audio/wav"
                                                     "Accept" "application/json, text/plain, text/event-stream"}
                                           :body audio-buffer}
                                    :timeout-ms 60000})]
          (js/console.log "[voice:stt] response received, status:" (:status resp) "ok:" (:ok resp))
          (if (:ok resp)
            (let [raw (:body resp)
                  _ (js/console.log "[voice:stt] raw body prefix:" (.slice (str raw) 0 80))
                  j (parse-stt-json-text raw)
                  _ (js/console.log "[voice:stt] JSON parsed:" (xjson/stringify j))
                  text (or (:text j) (:transcription j) "")]
              (if (stt-text-garbage? text)
                (do (js/console.warn "[voice:stt] GARBAGE detected, discarding:" (.slice text 0 60))
                    "")
                (do (js/console.log "[voice:stt] extracted text:" (if (str/blank? text) "[EMPTY]" text))
                    text)))
            (do (js/console.error "[voice:stt] HTTP FAILED:" (:status resp))
                (throw (js/Error. (str "STT " (:status resp)))))))
        (fn [err]
          (js/console.error "[voice:stt] === TRANSCRIBE ERROR ===" (.-message err))
          (throw err))))))

(defn tts-client
  ([config] (tts-client config {}))
  ([config {:keys [http-client]}]
   (->FetchTtsClient config (or http-client xfetch/default-client))))

(defn stt-client
  ([config] (stt-client config {}))
  ([config {:keys [http-client]}]
   (->FetchSttClient config (or http-client xfetch/default-client))))

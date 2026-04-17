(ns knoxx.backend.voice-routes
  (:require [clojure.string :as str]
            [knoxx.backend.http :as http]))

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

(defn register-voice-routes!
  [app runtime config handlers]
  (let [{:keys [route! json-response! with-request-context! ensure-permission!]} handlers]

    (route! app "GET" "/api/voice/stt/health"
            (fn [request reply]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (when ctx (ensure-permission! ctx "multimodal.upload"))
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
                  (when ctx (ensure-permission! ctx "multimodal.upload"))
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

    nil))

(defn register-voice-routes
  [app runtime config handlers]
  (register-voice-routes! app runtime config handlers))

(ns knoxx.backend.infra.http
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fetch :as xfetch]
            [knoxx.backend.extern.js :as xjs]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [promesa.core :as p]))

(defn reply-already-sent?
  [reply]
  (let [raw (aget reply "raw")]
    (boolean
      (or (aget reply "sent")
          (and raw (aget raw "writableEnded"))))))

(defn json-response!
  [reply status body]
  ;; Fastify throws if we attempt to send twice. Under load, upstream promises
  ;; can race (e.g. a timeout path sends an error while a success path resolves).
  ;; Prefer a safe no-op when the reply is already closed.
  (if (reply-already-sent? reply)
    reply
    (-> (.code reply status)
        (.type "application/json")
        (.send (clj->js body)))))

(defn request-hostname
  [request]
  (let [forwarded (some-> (aget request "headers" "x-forwarded-host") (str/split #",") first str/trim)
        raw-host (or forwarded (aget request "headers" "host") "")]
    (if (str/blank? raw-host)
      (or (aget request "hostname") "localhost")
      (-> raw-host
          (str/replace #":.*$" "")))))

(defn request-scheme
  [request]
  (let [forwarded (some-> (aget request "headers" "x-forwarded-proto") (str/split #",") first str/trim)]
    (if (str/blank? forwarded) "http" forwarded)))

(defn rewrite-localhost-url
  [url request]
  (try
    (let [parsed (js/URL. url)
          host (.-hostname parsed)]
      (if (contains? #{"localhost" "127.0.0.1" "::1"} host)
        (let [req-host (request-hostname request)
              scheme (request-scheme request)]
          (set! (.-protocol parsed) (str scheme ":"))
          (set! (.-hostname parsed) req-host)
          (.toString parsed))
        url))
    (catch :default _
      url)))

(defn with-query-param
  [url key value]
  (try
    (let [parsed (js/URL. url)]
      (.set (.-searchParams parsed) key value)
      (.toString parsed))
    (catch :default _
      url)))

(defn bearer-headers
  [token]
  (cond-> {"Content-Type" "application/json"}
    (not (str/blank? token)) (assoc "Authorization" (str "Bearer " token))))

(defn openai-auth-error
  [reply status-code message code]
  (json-response! reply status-code {:error {:message message
                                             :type "invalid_request_error"
                                             :param nil
                                             :code code}}))

(defn require-openai-key!
  [config request reply]
  (let [expected (:model-lab-openai-api-key config)
        auth-header (str (or (aget request "headers" "authorization") ""))]
    (cond
      (str/blank? expected)
      (do (openai-auth-error reply 503 "MODEL_LAB_OPENAI_API_KEY is not configured" "service_unavailable") false)

      (not (str/starts-with? (str/lower-case auth-header) "bearer "))
      (do (openai-auth-error reply 401 "Invalid API key" "invalid_api_key") false)

      (not= (subs auth-header 7) expected)
      (do (openai-auth-error reply 401 "Invalid API key" "invalid_api_key") false)

      :else true)))

(defn fetch-with-timeout
  "Compatibility wrapper around the extern fetch client. Accepts a CLJS map or
   JS object for opts and returns Promise<Response>. New call sites should
   prefer protocol clients that call `knoxx.backend.extern.fetch/json!`,
   `text!`, or `array-buffer!`."
  ([url opts] (fetch-with-timeout url opts 30000))
  ([url opts timeout-ms]
   (xfetch/response! xfetch/default-client {:url url
                                            :opts opts
                                            :timeout-ms timeout-ms})))

(def ^:private default-fetch-timeout-ms 30000)

(defn fetch-json
  "Compatibility wrapper around the extern fetch client. Fetch url with optional
   CLJS map or JS object opts. Returns Promise<{:ok :status :body :headers}>
   where :body and :headers are CLJS data."
  ([url opts]
   (fetch-json url opts default-fetch-timeout-ms))
  ([url opts timeout-ms]
   (xfetch/json! xfetch/default-client {:url url
                                        :opts opts
                                        :timeout-ms timeout-ms})))

(defn openplanner-enabled?
  [config]
  (openplanner-client/configured? config))

(defn openplanner-url
  [config suffix]
  (openplanner-client/url-for (openplanner-client/client config) suffix))

(defn openplanner-headers
  [config]
  (openplanner-client/headers-for config))

(defn openplanner-request!
  ([config method suffix] (openplanner-request! config method suffix nil))
  ([config method suffix body]
   (openplanner-client/request! (openplanner-client/client config) method suffix body)))

(defn http-error
  [status code message]
  (doto (ex-info (str status " " message) {:status status :code code})
    (aset "statusCode" status)
    (aset "code" code)))

(defn error-status
  [err default-status]
  (or (aget err "statusCode")
      (aget err "status")
      default-status))

(defn error-message
  [err]
  (or (aget err "message") (str err)))

(defn error-response!
  ([reply err] (error-response! reply err 500))
  ([reply err default-status]
   (json-response! reply (error-status err default-status)
                   (cond-> {:detail (error-message err)}
                     (aget err "code") (assoc :error_code (aget err "code"))))))

(defn no-content?
  [x]
  (or (nil? x) (= js/undefined x)))

(defn copy-response-headers!
  [reply headers]
  (.forEach headers
            (fn [value key]
              (when-not (contains? #{"connection" "content-length" "content-encoding" "transfer-encoding"} (str/lower-case key))
                (.header reply key value)))))

(defn send-fetch-response!
  [reply resp]
  (copy-response-headers! reply (.-headers resp))
  (p/let [buf (.arrayBuffer resp)]
    (-> (.code reply (.-status resp))
        (.send (.from js/Buffer buf)))))

(defn request-body
  "Return the parsed Fastify request body as a JS object, or an empty JS object."
  [request]
  (or (aget request "body") (js/Object.)))

(defn request-query-string
  [request]
  (let [params (js/URLSearchParams.)
        query (or (aget request "query") #js {})]
    (doseq [key (xjs/js-array-seq (.keys js/Object query))]
      (let [value (aget query key)]
        (cond
          (no-content? value) nil
          (array? value) (doseq [item (array-seq value)]
                           (.append params key (str item)))
          :else (.append params key (str value)))))
    (let [encoded (.toString params)]
      (if (str/blank? encoded) "" (str "?" encoded)))))

(defn request-forward-headers
  [request extra]
  (let [headers (js/Headers.)
        source (or (aget request "headers") #js {})]
    (doseq [key (xjs/js-array-seq (.keys js/Object source))]
      (let [lower (str/lower-case key)
            value (aget source key)]
        (when (and (not (contains? #{"host" "connection" "content-length" "transfer-encoding"} lower))
                   (not (no-content? value)))
          (.set headers key (str value)))))
    (doseq [[key value] extra]
      (if (nil? value)
        (.delete headers key)
        (.set headers key (str value))))
    headers))

(defn request-forward-body
  [request]
  (let [method (str/upper-case (or (aget request "method") "GET"))
        body (aget request "body")]
    (cond
      (contains? #{"GET" "HEAD"} method) nil
      (or (string? body)
          (instance? js/Uint8Array body)
          (instance? js/ArrayBuffer body)
          (instance? js/Buffer body)) body
      (no-content? body) nil
      :else (.stringify js/JSON body))))
(defn request-stream-body
  [request]
  (let [method (str/upper-case (or (aget request "method") "GET"))
        body (request-forward-body request)
        content-type (str/lower-case (str (or (aget request "headers" "content-type") "")))]
    (cond
      (contains? #{"GET" "HEAD"} method) #js {}
      (some? body) #js {:body body}
      (str/includes? content-type "multipart/form-data") #js {:body (aget request "raw")
                                                              :duplex "half"}
      :else #js {})))

(defn forward-knoxx-request!
  [config request method path extra]
  (let [target-url (str (:knoxx-base-url config) "/api/" path (request-query-string request))
        base #js {:method method
                  :headers (request-forward-headers request {"x-api-key" (when-not (str/blank? (:knoxx-api-key config)) (:knoxx-api-key config))})}
        stream-opts (request-stream-body request)]
    (fetch-with-timeout target-url (.assign js/Object base stream-opts (clj->js extra)))))

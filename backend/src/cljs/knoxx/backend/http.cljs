(ns knoxx.backend.http
  (:require [clojure.string :as str]))

(defn json-response!
  [reply status body]
  (-> (.code reply status)
      (.type "application/json")
      (.send (clj->js body))))

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
  (let [headers #js {"Content-Type" "application/json"}]
    (when-not (str/blank? token)
      (aset headers "Authorization" (str "Bearer " token)))
    headers))

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

(defn fetch-json
  [url opts]
  (-> (js/fetch url opts)
      (.then (fn [resp]
               (-> (.text resp)
                   (.then (fn [text]
                            (let [body (if (str/blank? text)
                                         #js {}
                                         (try
                                           (.parse js/JSON text)
                                           (catch :default _ #js {:raw text})))]
                              #js {:ok (.-ok resp)
                                   :status (.-status resp)
                                   :body body
                                   :headers resp.headers}))))))))

(defn trim-trailing-slashes
  [s]
  (str/replace (str (or s "")) #"/+$" ""))

(defn openplanner-enabled?
  [config]
  (and (not (str/blank? (:openplanner-base-url config)))
       (not (str/blank? (:openplanner-api-key config)))))

(defn openplanner-url
  [config suffix]
  (str (trim-trailing-slashes (:openplanner-base-url config)) suffix))

(defn openplanner-headers
  [config]
  #js {"Content-Type" "application/json"
       "Authorization" (str "Bearer " (:openplanner-api-key config))})

(defn openplanner-request!
  ([config method suffix] (openplanner-request! config method suffix nil))
  ([config method suffix body]
   (if-not (openplanner-enabled? config)
     (js/Promise.reject (js/Error. "OpenPlanner is not configured"))
     (let [opts #js {:method method
                     :headers (openplanner-headers config)}]
       (when body
         (aset opts "body" (.stringify js/JSON (clj->js body))))
       (-> (fetch-json (openplanner-url config suffix) opts)
           (.then (fn [resp]
                    (if (aget resp "ok")
                      (js->clj (aget resp "body") :keywordize-keys true)
                      (throw (js/Error. (str "OpenPlanner request failed ("
                                             (aget resp "status")
                                             "): "
                                             (pr-str (js->clj (aget resp "body") :keywordize-keys true)))))))))))))

(defn http-error
  [status code message]
  (doto (js/Error. message)
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

(defn js-array-seq
  [value]
  (if (array? value) (array-seq value) []))

(defn copy-response-headers!
  [reply headers]
  (.forEach headers
            (fn [value key]
              (when-not (contains? #{"connection" "content-length" "content-encoding" "transfer-encoding"} (str/lower-case key))
                (.header reply key value)))))

(defn send-fetch-response!
  [reply resp]
  (copy-response-headers! reply (.-headers resp))
  (-> (.arrayBuffer resp)
      (.then (fn [buf]
               (-> (.code reply (.-status resp))
                   (.send (.from js/Buffer buf)))))))

(defn request-query-string
  [request]
  (let [params (js/URLSearchParams.)
        query (or (aget request "query") #js {})]
    (doseq [key (js-array-seq (.keys js/Object query))]
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
    (doseq [key (js-array-seq (.keys js/Object source))]
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

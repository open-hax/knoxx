(ns knoxx.backend.extern.fastify
  "Fastify request/reply boundary helpers.

   Owns raw Fastify request/reply object traversal and native RequestInit
   construction. Callers pass/receive CLJS maps where possible and may carry
   opaque handles such as raw request streams or Response buffers."
  (:require [clojure.string :as str]))

(defn no-content?
  [x]
  (or (nil? x) (= js/undefined x)))

(defn reply-already-sent?
  [reply]
  (let [raw (aget reply "raw")]
    (boolean
     (or (aget reply "sent")
         (and raw (aget raw "writableEnded"))))))

(defn send-json!
  [reply status body]
  (if (reply-already-sent? reply)
    reply
    (-> (.code reply status)
        (.type "application/json")
        (.send (clj->js body)))))

(defn request-body
  [request]
  (let [body (aget request "body")]
    (cond
      (no-content? body) {}
      (map? body) body
      :else (js->clj body :keywordize-keys true))))

(defn request-body-raw
  [request]
  (aget request "body"))

(defn request-headers
  [request]
  (js->clj (or (aget request "headers") #js {}) :keywordize-keys true))

(defn request-header
  [request k]
  (aget request "headers" (name k)))

(defn request-query
  [request]
  (js->clj (or (aget request "query") #js {}) :keywordize-keys true))

(defn request-query-string-map
  [request]
  (let [query (or (aget request "query") #js {})]
    (reduce (fn [acc key]
              (let [value (aget query key)]
                (cond
                  (no-content? value) acc
                  (array? value) (assoc acc key (vec (array-seq value)))
                  :else (assoc acc key value))))
            {}
            (array-seq (.keys js/Object query)))))

(defn request-params
  [request]
  (js->clj (or (aget request "params") #js {}) :keywordize-keys true))

(defn request-param
  [request k]
  (aget request "params" (name k)))

(defn request-method
  [request]
  (or (aget request "method") "GET"))

(defn request-hostname
  [request]
  (or (aget request "hostname") "localhost"))

(defn reply-header!
  [reply name value]
  (.header reply name value))

(defn route-options
  [opts]
  (clj->js opts))

(defn route!
  [app opts]
  (.route app (route-options opts)))

(defn copy-response-headers!
  [reply headers]
  (.forEach headers
            (fn [value key]
              (when-not (contains? #{"connection" "content-length" "content-encoding" "transfer-encoding"}
                                   (str/lower-case key))
                (reply-header! reply key value)))))

(defn send-buffer-response!
  [reply resp buffer]
  (-> (.code reply (.-status resp))
      (.send (.from js/Buffer buffer))))

(defn query-string
  [request]
  (let [params (js/URLSearchParams.)
        query (or (aget request "query") #js {})]
    (doseq [key (array-seq (.keys js/Object query))]
      (let [value (aget query key)]
        (cond
          (no-content? value) nil
          (array? value) (doseq [item (array-seq value)]
                           (.append params key (str item)))
          :else (.append params key (str value)))))
    (let [encoded (.toString params)]
      (if (str/blank? encoded) "" (str "?" encoded)))))

(defn forward-headers
  [request extra]
  (let [headers (js/Headers.)
        source (or (aget request "headers") #js {})]
    (doseq [key (array-seq (.keys js/Object source))]
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

(defn forward-body
  [request]
  (let [method (str/upper-case (request-method request))
        body (request-body-raw request)]
    (cond
      (contains? #{"GET" "HEAD"} method) nil
      (or (string? body)
          (instance? js/Uint8Array body)
          (instance? js/ArrayBuffer body)
          (instance? js/Buffer body)) body
      (no-content? body) nil
      :else (.stringify js/JSON body))))

(defn stream-body-options
  [request]
  (let [method (str/upper-case (request-method request))
        body (forward-body request)
        content-type (str/lower-case (str (or (request-header request :content-type) "")))]
    (cond
      (contains? #{"GET" "HEAD"} method) #js {}
      (some? body) #js {:body body}
      (str/includes? content-type "multipart/form-data") #js {:body (aget request "raw")
                                                              :duplex "half"}
      :else #js {})))

(defn forward-request-init
  [request method extra-headers extra]
  (let [base #js {:method method
                  :headers (forward-headers request extra-headers)}]
    (.assign js/Object base (stream-body-options request) (clj->js extra))))

(defn error-status
  [err default-status]
  (or (aget err "statusCode")
      (aget err "status")
      default-status))

(defn error-message
  [err]
  (or (aget err "message") (str err)))

(defn error-code
  [err]
  (aget err "code"))

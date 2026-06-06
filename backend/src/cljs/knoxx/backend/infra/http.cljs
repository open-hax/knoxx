(ns knoxx.backend.infra.http
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fastify :as xfastify]
            [knoxx.backend.extern.fetch :as xfetch]
            [promesa.core :as p]))

(defn reply-already-sent?
  [reply]
  (xfastify/reply-already-sent? reply))

(defn json-response!
  [reply status body]
  ;; Fastify throws if we attempt to send twice. Under load, upstream promises
  ;; can race (e.g. a timeout path sends an error while a success path resolves).
  ;; Prefer a safe no-op when the reply is already closed.
  (xfastify/send-json! reply status body))

(defn request-hostname
  [request]
  (let [forwarded (some-> (xfastify/request-header request :x-forwarded-host) (str/split #",") first str/trim)
        raw-host (or forwarded (xfastify/request-header request :host) "")]
    (if (str/blank? raw-host)
      (xfastify/request-hostname request)
      (-> raw-host
          (str/replace #":.*$" "")))))

(defn request-scheme
  [request]
  (let [forwarded (some-> (xfastify/request-header request :x-forwarded-proto) (str/split #",") first str/trim)]
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
          (set! (.-port parsed) "")
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
        auth-header (str (or (xfastify/request-header request :authorization) ""))]
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

(defn http-error
  [status code message]
  (doto (ex-info (str status " " message) {:status status :code code})
    (aset "statusCode" status)
    (aset "code" code)))

(defn error-status
  [err default-status]
  (xfastify/error-status err default-status))

(defn error-message
  [err]
  (xfastify/error-message err))

(defn error-response!
  ([reply err] (error-response! reply err 500))
  ([reply err default-status]
   (json-response! reply (error-status err default-status)
                   (cond-> {:detail (error-message err)}
                     (xfastify/error-code err) (assoc :error_code (xfastify/error-code err))))))

(defn no-content?
  [x]
  (xfastify/no-content? x))

(defn copy-response-headers!
  [reply headers]
  (xfastify/copy-response-headers! reply headers))

(defn send-fetch-response!
  [reply resp]
  (copy-response-headers! reply (.-headers resp))
  (p/let [buf (.arrayBuffer resp)]
    (xfastify/send-buffer-response! reply resp buf)))

(defn request-body
  "Return the parsed Fastify request body as a CLJS map."
  [request]
  (xfastify/request-body request))

(defn request-query-string
  [request]
  (xfastify/query-string request))

(defn request-forward-headers
  [request extra]
  (xfastify/forward-headers request extra))

(defn request-forward-body
  [request]
  (xfastify/forward-body request))
(defn request-stream-body
  [request]
  (xfastify/stream-body-options request))

(defn forward-knoxx-request!
  [config request method path extra]
  (let [target-url (str (:knoxx-base-url config) "/api/" path (request-query-string request))]
    (fetch-with-timeout target-url
                        (xfastify/forward-request-init
                         request
                         method
                         {"x-api-key" (when-not (str/blank? (:knoxx-api-key config)) (:knoxx-api-key config))}
                         extra))))

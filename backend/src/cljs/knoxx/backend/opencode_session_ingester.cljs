(ns knoxx.backend.opencode-session-ingester
  "Small OpenCode session API client used by Knoxx admin routes.

   The ingestion service owns durable ingestion. This namespace provides the
   lightweight status/list surface analogous to eta-mu-session-ingester.cljs."
  (:require [clojure.string :as str]))

(def ^:private OPENCODE-SERVER-URL
  (str/replace (or (aget (.-env js/process) "OPENCODE_SERVER_URL")
                   "http://127.0.0.1:4096")
               #"/+$" ""))

(defn- obj-get
  [obj key]
  (aget obj key))

(defn- auth-headers
  []
  (let [headers #js {"accept" "application/json"}
        password (aget (.-env js/process) "OPENCODE_SERVER_PASSWORD")
        username (or (aget (.-env js/process) "OPENCODE_SERVER_USERNAME") "opencode")
        Buffer (aget js/globalThis "Buffer")]
    (when (and (not (str/blank? password)) Buffer)
      (let [token (.toString (.from Buffer (str username ":" password) "utf8") "base64")]
        (aset headers "authorization" (str "Basic " token))))
    headers))

(defn- timeout-signal
  [ms]
  (.timeout js/AbortSignal ms))

(defn- parse-json-text
  [text]
  (if (str/blank? text)
    nil
    (js/JSON.parse text)))

(defn- append-query!
  [url params]
  (doseq [[k v] params]
    (when (and (some? v) (not (and (string? v) (str/blank? v))))
      (.set (.-searchParams url) (name k) (str v))))
  url)

(defn- response->json!
  [resp]
  (-> (.text resp)
      (.then
       (fn [text]
         (let [body (try (parse-json-text text)
                         (catch :default _ text))]
           (if (.-ok resp)
             #js {:ok true
                  :status (.-status resp)
                  :body body
                  :nextCursor (.get (.-headers resp) "x-next-cursor")}
             (throw (js/Error. (str "OpenCode HTTP " (.-status resp) ": " text)))))))))

(defn- fetch-json-response
  [api-path params]
  (let [url (append-query! (js/URL. (str OPENCODE-SERVER-URL api-path)) params)]
    (-> (js/fetch (.toString url)
                  #js {:headers (auth-headers)
                       :signal (timeout-signal 60000)})
        (.then response->json!))))

(defn get-opencode-ingest-status
  []
  (-> (js/Promise.all
       #js [(fetch-json-response "/global/health" {})
            (fetch-json-response "/experimental/session" {:limit 20 :archived true})])
      (.then
       (fn [parts]
         (let [health (obj-get (aget parts 0) "body")
               sessions (obj-get (aget parts 1) "body")
               count (if (array? sessions) (.-length sessions) 0)]
           #js {:ok true
                :opencodeServerUrl OPENCODE-SERVER-URL
                :health health
                :recentSessionCount count
                :recentSessions sessions})))
      (.catch
       (fn [err]
         #js {:ok false
              :opencodeServerUrl OPENCODE-SERVER-URL
              :error (.-message err)}))))

(defn list-opencode-sessions
  [{:keys [limit cursor directory search roots archived]
    :or {limit 50 archived true}}]
  (let [limit (min (or limit 50) 200)]
    (-> (fetch-json-response "/experimental/session"
                             (cond-> {:limit limit
                                       :archived archived}
                               cursor (assoc :cursor cursor)
                               directory (assoc :directory directory)
                               search (assoc :search search)
                               (some? roots) (assoc :roots roots)))
        (.then
         (fn [resp]
           #js {:ok true
                :opencodeServerUrl OPENCODE-SERVER-URL
                :sessions (obj-get resp "body")
                :nextCursor (obj-get resp "nextCursor")
                :has_more (boolean (obj-get resp "nextCursor"))})))))

(defn get-opencode-session-messages
  [session-id]
  (fetch-json-response (str "/session/" (js/encodeURIComponent session-id) "/message") {}))

(ns knoxx.backend.infra.clients.opencode
  "OpenCode session API client protocol.

   Covers the local OpenCode ingestion source: `/global/health`,
   `/experimental/session`, and `/session/:id/message`. The client owns base URL,
   auth headers, query-string construction, fetch timeout, and response parsing."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fetch :as xfetch]
            [promesa.core :as p]))

(defprotocol IOpenCodeClient
  (health! [client])
  (sessions! [client opts])
  (session-messages! [client session-id]))

(defn- blank->nil
  [value]
  (let [s (str/trim (str (or value "")))]
    (when-not (str/blank? s) s)))

(defn- env
  [name]
  (some-> js/process .-env (aget name) blank->nil))

(defn- base-url
  [config]
  (str/replace (or (:opencode-server-url config)
                   (env "OPENCODE_SERVER_URL")
                   "http://127.0.0.1:4096")
               #"/+$" ""))

(defn- basic-auth-token
  []
  (let [password (env "OPENCODE_SERVER_PASSWORD")
        username (or (env "OPENCODE_SERVER_USERNAME") "opencode")
        Buffer (aget js/globalThis "Buffer")]
    (when (and password Buffer)
      (.toString (.from Buffer (str username ":" password) "utf8") "base64"))))

(defn- auth-headers
  []
  (cond-> {"accept" "application/json"}
    (basic-auth-token) (assoc "authorization" (str "Basic " (basic-auth-token)))))

(defn- present-query-value?
  [value]
  (and (some? value) (not (and (string? value) (str/blank? value)))))

(defn- append-query!
  [url params]
  (doseq [[k v] (or params {})]
    (when (present-query-value? v)
      (.set (.-searchParams url) (name k) (str v))))
  url)

(defn- response-body!
  [resp label]
  (if (:ok resp)
    {:ok true
     :status (:status resp)
     :body (:body resp)
     :nextCursor (get (:headers resp) "x-next-cursor")}
    (throw (js/Error. (str label " failed (" (:status resp) "): " (pr-str (:body resp)))))))

(defn- request-json!
  [http-client config api-path params]
  (p/let [resp (xfetch/json! (or http-client xfetch/default-client)
                             {:url (.toString (append-query! (js/URL. (str (base-url config) api-path)) params))
                              :opts {:method "GET"
                                     :headers (auth-headers)}
                              :timeout-ms 60000})]
    (response-body! resp "OpenCode request")))

(defrecord FetchOpenCodeClient [config http-client]
  IOpenCodeClient
  (health! [_]
    (request-json! http-client config "/global/health" {}))
  (sessions! [_ opts]
    (request-json! http-client config "/experimental/session" opts))
  (session-messages! [_ session-id]
    (request-json! http-client config (str "/session/" (js/encodeURIComponent (str session-id)) "/message") {})))

(defn client
  ([] (client {}))
  ([config] (client config {}))
  ([config {:keys [http-client]}]
   (->FetchOpenCodeClient config (or http-client xfetch/default-client))))

(defn server-url
  [config]
  (base-url config))

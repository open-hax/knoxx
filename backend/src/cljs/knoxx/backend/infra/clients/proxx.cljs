(ns knoxx.backend.infra.clients.proxx
  "Proxx service client protocol.

   Covers all `/health`, `/v1/models`, `/v1/chat/completions`,
   `/v1/embeddings`, `/api/tools/websearch`, and dashboard/log/analytics calls
   made from route handlers and tool namespaces. Concrete clients own Proxx URL
   construction, auth headers, JSON encoding, and native fetch access via
   `extern.fetch`."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fetch :as xfetch]
            [knoxx.backend.extern.json :as xjson]))

(defprotocol IProxxClient
  (health! [client])
  (models! [client])
  (request-logs! [client query-string])
  (dashboard-overview! [client query-string])
  (provider-model-analytics! [client query-string])
  (websearch! [client payload])
  (chat-completions! [client payload])
  (chat-completions-response! [client payload])
  (embeddings! [client payload])
  (embeddings-response! [client payload]))

(defn configured?
  [config]
  (and (not (str/blank? (:proxx-base-url config)))
       (not (str/blank? (:proxx-auth-token config)))))

(defn- trim-trailing-slashes
  [s]
  (str/replace (str (or s "")) #"/+$" ""))

(defn url-for
  [config suffix]
  (str (trim-trailing-slashes (:proxx-base-url config)) suffix))

(defn headers-for
  [config]
  (cond-> {"Content-Type" "application/json"}
    (not (str/blank? (:proxx-auth-token config)))
    (assoc "Authorization" (str "Bearer " (:proxx-auth-token config)))))

(defn- stringify-body
  [body]
  (cond
    (nil? body) nil
    (or (map? body) (vector? body) (set? body)) (xjson/stringify body)
    :else (.stringify js/JSON body)))

(defn- json-request!
  [http-client config method suffix body timeout-ms]
  (xfetch/json! (or http-client xfetch/default-client)
                {:url (url-for config suffix)
                 :opts (cond-> {:method method
                                :headers (headers-for config)}
                         (some? body) (assoc :body (stringify-body body)))
                 :timeout-ms (or timeout-ms 30000)}))

(defn- response-request!
  [http-client config method suffix body timeout-ms]
  (xfetch/response! (or http-client xfetch/default-client)
                    {:url (url-for config suffix)
                     :opts (cond-> {:method method
                                    :headers (headers-for config)}
                             (some? body) (assoc :body (stringify-body body)))
                     :timeout-ms (or timeout-ms 30000)}))

(defrecord FetchProxxClient [config http-client]
  IProxxClient
  (health! [_]
    (json-request! http-client config "GET" "/health" nil 30000))
  (models! [_]
    (json-request! http-client config "GET" "/v1/models" nil 30000))
  (request-logs! [_ query-string]
    (json-request! http-client config "GET" (str "/api/v1/request-logs" (or query-string "")) nil 30000))
  (dashboard-overview! [_ query-string]
    (json-request! http-client config "GET" (str "/api/v1/dashboard/overview" (or query-string "")) nil 30000))
  (provider-model-analytics! [_ query-string]
    (json-request! http-client config "GET" (str "/api/v1/analytics/provider-model" (or query-string "")) nil 30000))
  (websearch! [_ payload]
    (json-request! http-client config "POST" "/api/tools/websearch" payload 30000))
  (chat-completions! [_ payload]
    (json-request! http-client config "POST" "/v1/chat/completions" payload 30000))
  (chat-completions-response! [_ payload]
    (response-request! http-client config "POST" "/v1/chat/completions" payload 30000))
  (embeddings! [_ payload]
    (json-request! http-client config "POST" "/v1/embeddings" payload 30000))
  (embeddings-response! [_ payload]
    (response-request! http-client config "POST" "/v1/embeddings" payload 30000)))

(defn client
  ([config] (client config {}))
  ([config {:keys [http-client]}]
   (->FetchProxxClient config (or http-client xfetch/default-client))))

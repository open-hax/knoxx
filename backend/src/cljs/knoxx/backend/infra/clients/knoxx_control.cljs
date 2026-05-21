(ns knoxx.backend.infra.clients.knoxx-control
  "Knoxx self-API/control-plane client protocol.

   Covers requests to the local Knoxx backend from tools/voice/routes:
   `/api/knoxx/steer`, `/api/knoxx/direct/start`,
   `/api/admin/config/events`, `/api/admin/config/events/dispatch`,
   `/api/admin/config/events/jobs/:id/run`, generic forwarding, and internal
   ingestion/source proxy endpoints. Implementations own auth headers, JSON
   encoding, and native fetch details."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fetch :as xfetch]
            [promesa.core :as p]))

(defprotocol IKnoxxControlClient
  (request-json! [client method path body])
  (steer! [client payload])
  (direct-start! [client payload])
  (event-config! [client])
  (save-event-config! [client payload])
  (dispatch-event! [client payload])
  (run-event-job! [client job-id])
  (forward-api! [client request method path extra])
  (ingestion-sources! [client])
  (ingestion-file! [client path])
  (ingestion-jobs! [client])
  (ingestion-job! [client job-id])
  (ingestion-job-actions! [client job-id method payload]))

(defn- trim-trailing-slashes
  [s]
  (str/replace (str (or s "")) #"/+$" ""))

(defn- env
  [k]
  (some-> js/process .-env (aget k)))

(defn- base-url
  [config]
  (or (:knoxx-base-url config)
      (env "KNOXX_BASE_URL")
      "http://127.0.0.1:8000"))

(defn- api-key
  [config]
  (or (:knoxx-api-key config)
      (env "KNOXX_API_KEY")))

(defn headers-for
  [config]
  (let [key (api-key config)]
    (cond-> {"Content-Type" "application/json"
             "x-knoxx-user-email" "system-admin@open-hax.local"}
      (not (str/blank? (str key)))
      (assoc "X-API-Key" key))))

(defn- json-request!
  [http-client config method path body timeout-ms]
  (p/let [resp (xfetch/json! http-client
                             {:url (str (trim-trailing-slashes (base-url config)) path)
                              :opts (cond-> {:method method
                                             :headers (headers-for config)}
                                      body (assoc :json body))
                              :timeout-ms timeout-ms})]
    (if (:ok resp)
      (:body resp)
      (throw (js/Error. (str "Knoxx control request failed ("
                             (:status resp)
                             "): "
                             (pr-str (:body resp))))))))

(defrecord FetchKnoxxControlClient [config http-client timeout-ms]
  IKnoxxControlClient
  (request-json! [_ method path body]
    (json-request! (or http-client xfetch/default-client) config method path body (or timeout-ms 30000)))
  (steer! [client payload]
    (request-json! client "POST" "/api/knoxx/steer" payload))
  (direct-start! [client payload]
    (request-json! client "POST" "/api/knoxx/direct/start" payload))
  (event-config! [client]
    (request-json! client "GET" "/api/admin/config/events" nil))
  (save-event-config! [client payload]
    (request-json! client "PUT" "/api/admin/config/events" payload))
  (dispatch-event! [client payload]
    (request-json! client "POST" "/api/admin/config/events/dispatch" payload))
  (run-event-job! [client job-id]
    (request-json! client "POST" (str "/api/admin/config/events/jobs/" (js/encodeURIComponent job-id) "/run") nil))
  (forward-api! [_ _request _method _path _extra]
    (js/Promise.reject (js/Error. "forward-api! requires Fastify request streaming adapter; not implemented in FetchKnoxxControlClient yet")))
  (ingestion-sources! [client]
    (request-json! client "GET" "/api/ingestion/sources" nil))
  (ingestion-file! [client path]
    (request-json! client "GET" (str "/api/ingestion/file?path=" (js/encodeURIComponent path)) nil))
  (ingestion-jobs! [client]
    (request-json! client "GET" "/api/ingestion/jobs" nil))
  (ingestion-job! [client job-id]
    (request-json! client "GET" (str "/api/ingestion/jobs/" (js/encodeURIComponent job-id)) nil))
  (ingestion-job-actions! [client job-id method payload]
    (request-json! client method (str "/api/ingestion/jobs/" (js/encodeURIComponent job-id)) payload)))

(defn client
  ([config] (client config {}))
  ([config {:keys [http-client timeout-ms]}]
   (->FetchKnoxxControlClient config (or http-client xfetch/default-client) (or timeout-ms 30000))))

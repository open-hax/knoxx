(ns knoxx.backend.extern.app-http
  "Native HTTP, ingestion-file and handoff response boundaries."
  (:require ["node:fs/promises" :as fs]
            ["node:path" :as path]
            [clojure.string :as str]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.http :as infra-http]))

(declare mongo-collections-ok)

(defn- proxy-err [reply prefix err]
  (infra-http/json-response! reply 502 {:detail (str prefix err)}))

(defn- fetch-json-ok [reply resp]
  (infra-http/json-response! reply (or (:status resp) 200) (:body resp)))

(defn fetch-json-err
  "Execute fetch json err through its owned application boundary." [reply err]
  (infra-http/json-response! reply 502 {:error (.-message err)}))

(defn- fetch-json-err-detail [reply prefix err]
  (infra-http/json-response! reply 502 {:detail (str prefix err)}))

(defn ^:async send-knoxx-proxy!
  "Execute send knoxx proxy through its owned application boundary."
  [config request reply method path]
  (try
    (infra-http/send-fetch-response! reply (await (infra-http/forward-knoxx-request! config request method path nil)))
    (catch :default err
      (proxy-err reply "Proxy request failed: " err))))

(defn ^:async send-fetch-json!
  "Execute send fetch json through its owned application boundary."
  [reply target-url opts error-handler]
  (try
    (fetch-json-ok reply (await (infra-http/fetch-json target-url opts)))
    (catch :default err
      (error-handler reply err))))

(defn ^:async send-fetch-json-detail!
  "Execute send fetch json detail through its owned application boundary."
  [reply target-url opts prefix]
  (send-fetch-json! reply target-url opts #(fetch-json-err-detail %1 prefix %2)))

(defn ^:async send-openplanner-v1-json!
  "Execute send openplanner v1 json through its owned application boundary."
  [config reply method path body]
  (try
    (infra-http/json-response! reply 200 (await (openplanner-client/v1-json!
                                      (openplanner-client/client config) method path body)))
    (catch :default err
      (fetch-json-err reply err))))

(defn ^:async send-json-promise!
  "Execute send json promise through its owned application boundary."
  [reply request-promise]
  (try
    (infra-http/json-response! reply 200 (await request-promise))
    (catch :default err
      (fetch-json-err reply err))))

(defn ^:async send-mongo-collections!
  "Execute send mongo collections through its owned application boundary."
  [config reply]
  (let [client (openplanner-client/client config)]
    (try
      (let [[docs graph] (await (promise/all-vec
                                 [(openplanner-client/documents-stats! client)
                                  (openplanner-client/graph-monitoring! client)]))]
        (mongo-collections-ok reply [{:ok true :body docs}
                                     {:ok true :body graph}]))
      (catch :default err
        (fetch-json-err reply err)))))

(defn ^:async send-data-browse!
  "Execute send data browse through its owned application boundary."
  [reply target-url]
  (try
    (let [resp (await (infra-http/fetch-json target-url nil))]
      (infra-http/json-response! reply
                      (or (:status resp) (aget resp "status") 200)
                      (or (:body resp) (aget resp "body"))))
    (catch :default err
      (fetch-json-err reply err))))

(defn ^:async write-ingestion-file!
  "Execute write ingestion file through its owned application boundary."
  [reply absolute-path content safe-path]
  (try
    (await (.mkdir fs (.dirname path absolute-path) (clj->js {:recursive true})))
    (await (.writeFile fs absolute-path content "utf8"))
    (infra-http/json-response! reply 200 {:ok true :path safe-path})
    (catch :default err
      (infra-http/json-response! reply 500 {:detail (str "Write failed: " err)}))))

(defn- response-body
  [resp]
  (or (:body resp) (aget resp "body")))

(defn- mongo-collections-ok [reply results]
  (infra-http/json-response! reply 200
                  {:ok true
                   :documents (response-body (nth results 0))
                   :graph (response-body (nth results 1))}))

(defn shibboleth-ok
  "Execute shibboleth ok through its owned application boundary." [config reply request body data]
  (let [session (or (:session data) {})
        session-id (str (or (:id session) ""))
        ui-url (if (and (not (str/blank? session-id))
                        (not (str/blank? (:shibboleth-ui-url config))))
                 (infra-http/with-query-param (infra-http/rewrite-localhost-url (:shibboleth-ui-url config) request)
                   "session"
                   session-id)
                 "")]
    (if (str/blank? session-id)
      (infra-http/json-response! reply 502 {:detail "Shibboleth import did not return a session id"})
      (infra-http/json-response! reply 200 {:ok true
                                 :session_id session-id
                                 :ui_url ui-url
                                 :imported_item_count (count (or (:items body) []))}))))

(defn shibboleth-import-failed
  "Execute shibboleth import failed through its owned application boundary." [reply resp]
  (infra-http/json-response! reply 502 {:detail (str "Shibboleth import failed: "
                                          (or (:raw (:body resp))
                                              (js/JSON.stringify (clj->js (:body resp)))))}))

(defn shibboleth-unreachable
  "Execute shibboleth unreachable through its owned application boundary." [reply err]
  (infra-http/json-response! reply 502 {:detail (str "Shibboleth is unreachable: " err)}))

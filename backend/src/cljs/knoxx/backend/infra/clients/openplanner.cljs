(ns knoxx.backend.infra.clients.openplanner
  "OpenPlanner service client boundary.

   Callers should depend on named IOpenPlannerClient operations instead of
   constructing OpenPlanner URLs, headers, JSON bodies, or native fetch calls
   inline. Generic HTTP semantics stay private to this boundary."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fetch :as xfetch]
            [promesa.core :as p]))

(defprotocol IOpenPlannerClient
  (enabled? [client]
    "True when the client has enough configuration to call OpenPlanner.")
  (health! [client])
  (events! [client events])
  (session! [client session-id opts])
  (sessions! [client opts])
  (vector-search! [client payload])
  (graph-memory! [client payload])
  (graph-export! [client opts])
  (upsert-document! [client document])
  (documents-stats! [client])
  (graph-monitoring! [client])
  (mongo-collections! [client])
  (mongo-query! [client payload])
  (build-semantic-edges! [client payload])
  (record-labels! [client record-ids])
  (record-reaction! [client record-id payload])
  (translation-segments! [client opts])
  (translation-segment! [client segment-id])
  (create-translation-segment! [client segment])
  (label-translation-segment! [client segment-id payload])
  (translation-export-manifest! [client project])
  (translation-export-sft! [client opts])
  (create-translation-segments-batch! [client payload])
  (translation-documents! [client opts])
  (translation-document! [client document-id target-lang])
  (review-translation-document! [client document-id target-lang payload])
  (create-translation-batch! [client payload])
  (translation-batches! [client opts])
  (next-translation-batch! [client])
  (translation-batch! [client batch-id])
  (update-translation-batch-status! [client batch-id payload])
  (v1-json! [client method path body]
    "Compatibility for explicit admin/data proxy routes; prefer named methods.")
  (forward-v1! [client request]
    "Proxy compatibility for the frontend /api/openplanner/v1/* route."))

(defn trim-trailing-slashes
  [s]
  (str/replace (str (or s "")) #"/+$" ""))

(defn headers-for
  [config]
  {"Content-Type" "application/json"
   "Authorization" (str "Bearer " (:openplanner-api-key config))
   "X-Tenant-ID" (or (:session-project-name config) "knoxx-session")})

(defn configured?
  [config]
  (and (not (str/blank? (:openplanner-base-url config)))
       (not (str/blank? (:openplanner-api-key config)))))

(defn- encode
  [value]
  (js/encodeURIComponent (str value)))

(defn- present-query-value?
  [value]
  (not (nil? value)))

(defn- query-key
  [k]
  (if (keyword? k) (name k) (str k)))

(defn query-string
  [params]
  (let [search (js/URLSearchParams.)]
    (doseq [[k value] (or params {})]
      (when (present-query-value? value)
        (if (sequential? value)
          (doseq [item value]
            (when (present-query-value? item)
              (.append search (query-key k) (str item))))
          (.append search (query-key k) (str value)))))
    (let [encoded (.toString search)]
      (if (str/blank? encoded) "" (str "?" encoded)))))

(defn- body-opts
  [method headers body]
  (cond-> {:method method
           :headers headers}
    (some? body) (assoc :json body)))

(defn- ensure-enabled!
  [client]
  (when-not (enabled? client)
    (throw (js/Error. "OpenPlanner is not configured"))))

(defn- ensure-configured!
  [config]
  (when-not (configured? config)
    (throw (js/Error. "OpenPlanner is not configured"))))

(defn- checked-body!
  [resp label]
  (if (:ok resp)
    (:body resp)
    (throw (js/Error. (str label " failed (" (:status resp) "): " (pr-str (:body resp)))))))

(defn- checked-text!
  [resp label]
  (if (:ok resp)
    (:body resp)
    (throw (js/Error. (str label " failed (" (:status resp) "): " (:body resp))))))

(defn- request-json!
  [http-client config timeout-ms method suffix body]
  (ensure-configured! config)
  (p/let [resp (xfetch/json! (or http-client xfetch/default-client)
                             {:url (str (trim-trailing-slashes (:openplanner-base-url config)) suffix)
                              :opts (body-opts method (headers-for config) body)
                              :timeout-ms (or timeout-ms 60000)})]
    (checked-body! resp "OpenPlanner request")))

(defn- request-response!
  [http-client config timeout-ms {:keys [method path query-string body headers]}]
  (ensure-configured! config)
  (let [suffix (str "/" (str/replace (str (or path "")) #"^/+" "") (or query-string ""))]
    (xfetch/response! (or http-client xfetch/default-client)
                      {:url (str (trim-trailing-slashes (:openplanner-base-url config)) suffix)
                       :opts (cond-> {:method (or method "GET")
                                      :headers (merge (headers-for config) (or headers {}))}
                               (some? body) (assoc :body body))
                       :timeout-ms (or timeout-ms 60000)})))

(defrecord FetchOpenPlannerClient [config http-client timeout-ms]
  IOpenPlannerClient
  (enabled? [_]
    (configured? config))
  (health! [client]
    (if-not (enabled? client)
      (js/Promise.resolve {:ok false
                           :status 503
                           :body {:detail "OpenPlanner is not configured"}})
      (xfetch/json! (or http-client xfetch/default-client)
                    {:url (str (trim-trailing-slashes (:openplanner-base-url config)) "/v1/health")
                     :opts {:method "GET"
                            :headers (headers-for config)}
                     :timeout-ms (or timeout-ms 60000)})))
  (events! [_ events]
    (request-json! http-client config timeout-ms "POST" "/v1/events" {:events events}))
  (session! [_ session-id opts]
    (request-json! http-client config timeout-ms "GET" (str "/v1/sessions/" (encode session-id) (query-string opts)) nil))
  (sessions! [_ opts]
    (request-json! http-client config timeout-ms "GET" (str "/v1/sessions" (query-string opts)) nil))
  (vector-search! [_ payload]
    (request-json! http-client config timeout-ms "POST" "/v1/search/vector" payload))
  (graph-memory! [_ payload]
    (request-json! http-client config timeout-ms "POST" "/v1/graph/memory" payload))
  (graph-export! [_ opts]
    (request-json! http-client config timeout-ms "GET" (str "/v1/graph/export" (query-string opts)) nil))
  (upsert-document! [_ document]
    (request-json! http-client config timeout-ms "POST" "/v1/documents" {:document document}))
  (documents-stats! [_]
    (request-json! http-client config timeout-ms "GET" "/v1/documents/stats" nil))
  (graph-monitoring! [_]
    (request-json! http-client config timeout-ms "GET" "/v1/graph/monitoring" nil))
  (mongo-collections! [_]
    (request-json! http-client config timeout-ms "GET" "/v1/mongo/collections" nil))
  (mongo-query! [_ payload]
    (request-json! http-client config timeout-ms "POST" "/v1/mongo/query" payload))
  (build-semantic-edges! [_ payload]
    (request-json! http-client config timeout-ms "POST" "/v1/jobs/build-semantic-edges" payload))
  (record-labels! [_ record-ids]
    (request-json! http-client config timeout-ms "POST" "/v1/labels/records/lookup" {:ids (vec record-ids)}))
  (record-reaction! [_ record-id payload]
    (request-json! http-client config timeout-ms "POST" (str "/v1/labels/records/" (encode record-id) "/reaction") payload))
  (translation-segments! [_ opts]
    (request-json! http-client config timeout-ms "GET" (str "/v1/translations/segments" (query-string opts)) nil))
  (translation-segment! [_ segment-id]
    (request-json! http-client config timeout-ms "GET" (str "/v1/translations/segments/" (encode segment-id)) nil))
  (create-translation-segment! [_ segment]
    (request-json! http-client config timeout-ms "POST" "/v1/translations/segments" segment))
  (label-translation-segment! [_ segment-id payload]
    (request-json! http-client config timeout-ms "POST" (str "/v1/translations/segments/" (encode segment-id) "/labels") payload))
  (translation-export-manifest! [_ project]
    (request-json! http-client config timeout-ms "GET" (str "/v1/translations/export/manifest" (query-string {:project project})) nil))
  (translation-export-sft! [client opts]
    (ensure-enabled! client)
    (p/let [resp (xfetch/text! (or http-client xfetch/default-client)
                               {:url (str (trim-trailing-slashes (:openplanner-base-url config))
                                          "/v1/translations/export/sft"
                                          (query-string opts))
                                :opts {:method "GET"
                                       :headers (headers-for config)}
                                :timeout-ms (or timeout-ms 60000)})]
      (checked-text! resp "OpenPlanner translation SFT export")))
  (create-translation-segments-batch! [_ payload]
    (request-json! http-client config timeout-ms "POST" "/v1/translations/segments/batch" payload))
  (translation-documents! [_ opts]
    (request-json! http-client config timeout-ms "GET" (str "/v1/translations/documents" (query-string opts)) nil))
  (translation-document! [_ document-id target-lang]
    (request-json! http-client config timeout-ms "GET" (str "/v1/translations/documents/" (encode document-id) "/" (encode target-lang)) nil))
  (review-translation-document! [_ document-id target-lang payload]
    (request-json! http-client config timeout-ms "POST" (str "/v1/translations/documents/" (encode document-id) "/" (encode target-lang) "/review") payload))
  (create-translation-batch! [_ payload]
    (request-json! http-client config timeout-ms "POST" "/v1/translations/batches" payload))
  (translation-batches! [_ opts]
    (request-json! http-client config timeout-ms "GET" (str "/v1/translations/batches" (query-string opts)) nil))
  (next-translation-batch! [_]
    (request-json! http-client config timeout-ms "GET" "/v1/translations/batches/next" nil))
  (translation-batch! [_ batch-id]
    (request-json! http-client config timeout-ms "GET" (str "/v1/translations/batches/" (encode batch-id)) nil))
  (update-translation-batch-status! [_ batch-id payload]
    (request-json! http-client config timeout-ms "POST" (str "/v1/translations/batches/" (encode batch-id) "/status") payload))
  (v1-json! [_ method path body]
    (let [suffix (str "/v1/" (str/replace (str (or path "")) #"^/+" ""))]
      (request-json! http-client config timeout-ms method suffix body)))
  (forward-v1! [_ request]
    (request-response! http-client config timeout-ms request)))

(defn client
  ([config] (client config {}))
  ([config {:keys [http-client timeout-ms]}]
   (->FetchOpenPlannerClient config (or http-client xfetch/default-client) (or timeout-ms 60000))))

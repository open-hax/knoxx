(ns knoxx.backend.infra.routes.app.data
  "Register scoped data browsing and legacy database proxy routes."
  (:require-macros [knoxx.backend.macros :as backend-macros])
  (:require [clojure.string :as str]
            [knoxx.backend.extern.app-health :as app-health]
            [knoxx.backend.extern.app-http :as app-http]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.infra.auth.authz :as auth-authz]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.http :as infra-http]
            [knoxx.backend.infra.routes.app.dependencies :as app-dependencies]))

(backend-macros/defroute ^{:doc "Register GET /api/data/op/* with the shared route dependencies."} api-data-op-get! []
  "GET" "/api/data/op/*"
  (let [path (aget request "params" "*")
        raw-url (aget request "raw" "url")
        query-idx (.indexOf raw-url "?")
        qs (if (>= query-idx 0) (subs raw-url query-idx) "")]
    (app-http/send-openplanner-v1-json! config reply "GET" (str path qs) nil)))

(defn- canonical-openplanner-proxy-path
  [path]
  (let [decoded (loop [value (str path)
                       attempts 0]
                  (if (or (not (str/includes? value "%"))
                          (= attempts 16))
                    value
                    (let [next-value (try
                                       (fastify/decode-uri-component value)
                                       (catch :default _ value))]
                      (if (= value next-value)
                        value
                        (recur next-value (inc attempts))))))
        segments (->> (str/split (-> decoded
                                      str/lower-case
                                      (str/replace "\\" "/"))
                                  #"/+")
                      (reduce (fn [result segment]
                                (case segment
                                  "" result
                                  "." result
                                  ".." (if (seq result) (pop result) result)
                                  (conj result segment)))
                              []))]
    (if (= "v1" (first segments))
      (vec (rest segments))
      segments)))

(defn authorize-openplanner-proxy-post!
  "Refuse authenticated vector search until OpenPlanner can enforce org scope.

  The current SDK only accepts a fixed non-tenant `where` allowlist and drops
  `org_id` from stored vector documents. Response-side filtering would disclose
  rows to this process and truncate top-k results, so the service-credential
  compatibility proxy fails closed before forwarding. Policy-disabled local
  mode remains the existing explicitly trusted workstation boundary."
  [ctx path body]
  (when (and ctx
             (= ["search" "vector"]
                (canonical-openplanner-proxy-path path)))
    (throw (infra-http/http-error 403
                       "openplanner_vector_search_scope_unavailable"
                       "Vector search is unavailable until tenant filtering is supported")))
  body)

(backend-macros/defroute ^{:doc "Register POST /api/data/op/* with the shared route dependencies."} api-data-op-post! []
  "POST" "/api/data/op/*"
  (let [path (aget request "params" "*")
        body (authorize-openplanner-proxy-post!
              ctx path (aget request "body"))]
    (app-http/send-openplanner-v1-json! config reply "POST" path body)))

(backend-macros/defroute ^{:doc "Register DELETE /api/data/op/* with the shared route dependencies."} api-data-op-delete! []
  "DELETE" "/api/data/op/*"
  (let [path (aget request "params" "*")]
    (app-http/send-openplanner-v1-json! config reply "DELETE" path nil)))

(backend-macros/defroute ^{:doc "Register PATCH /api/data/op/* with the shared route dependencies."} api-data-op-patch! []
  "PATCH" "/api/data/op/*"
  (let [path (aget request "params" "*")
        body (aget request "body")]
    (app-http/send-openplanner-v1-json! config reply "PATCH" path body)))

(backend-macros/defroute ^{:doc "Register GET /api/data/health with the shared route dependencies."} api-data-health! []
  "GET" "/api/data/health"
  (app-health/send-data-health! config reply))

(backend-macros/defroute ^{:doc "Register GET /api/data/mongo/collections with the shared route dependencies."} api-data-mongo-collections! []
  "GET" "/api/data/mongo/collections"
  (app-http/send-mongo-collections! config reply))

(backend-macros/defroute ^{:doc "Register GET /api/data/mongo/list with the shared route dependencies."} api-data-mongo-list! []
  "GET" "/api/data/mongo/list"
  (app-http/send-json-promise! reply (openplanner-client/mongo-collections! (openplanner-client/client config))))

(def data-mongo-read-permission
  "Permission required by the legacy Mongo data-read route."
  "org.datalakes.read")

(defn authorize-mongo-query!
  "Authorize raw data exploration and tenant-scope publication source events.

   Policy-disabled local mode is an explicitly trusted workstation boundary.
   Once an authenticated context exists, however, even a caller-supplied
   `extra.org_id` is replaced with the server-derived tenant before the query
   reaches OpenPlanner. Other collections retain their established query shape;
   this predicate closes the source-event disclosure introduced by document
   admission without guessing at unrelated collection schemas."
  [ctx ensure-permission-fn body]
  (if-not ctx
    body
    (do
      (ensure-permission-fn ctx data-mongo-read-permission)
      (if-not (= "events" (:collection body))
        body
        (let [org-id (some-> (auth-authz/ctx-org-id ctx) str str/trim not-empty)]
          (when-not org-id
            (throw (infra-http/http-error 403 "org_scope_denied"
                               "Event queries require an authenticated organization")))
          (assoc body :filter
                 (assoc (or (:filter body) {}) :extra.org_id org-id)))))))

(backend-macros/defroute ^{:doc "Register POST /api/data/mongo/query with the shared route dependencies."} api-data-mongo-query! []
  "POST" "/api/data/mongo/query"
  (let [body (authorize-mongo-query! ctx ensure-permission!
                                     (infra-http/request-body request))]
    (app-http/send-json-promise! reply (openplanner-client/mongo-query! (openplanner-client/client config) body))))

(backend-macros/defroute ^{:doc "Register GET /api/data/pg/tables with the shared route dependencies."} api-data-pg-tables! []
  "GET" "/api/data/pg/tables"
  ;; PostgreSQL was removed in the E14 Mongo migration (kanban 14-05).
  ;; The Mongo explorer at /api/data/mongo/* is the replacement surface.
  (json-response! reply 410 {:error "pg_removed"
                             :detail "PostgreSQL backend removed; use /api/data/mongo/collections"}))

(backend-macros/defroute ^{:doc "Register POST /api/data/jobs/build-semantic-edges with the shared route dependencies."} api-data-jobs-build-semantic-edges! []
  "POST" "/api/data/jobs/build-semantic-edges"
  (let [body (infra-http/request-body request)
        k (or (:k body) 8)
        min-sim (or (:minSimilarity body) 0.3)]
    (app-http/send-json-promise! reply
                        (openplanner-client/build-semantic-edges!
                         (openplanner-client/client config)
                         {:k k :minSimilarity min-sim}))))

(backend-macros/defroute ^{:doc "Register POST /api/data/pg/query with the shared route dependencies."} api-data-pg-query! []
  "POST" "/api/data/pg/query"
  ;; PostgreSQL was removed in the E14 Mongo migration (kanban 14-05).
  (json-response! reply 410 {:error "pg_removed"
                             :detail "PostgreSQL backend removed; use /api/data/mongo/collections"}))

(backend-macros/defroute ^{:doc "Register GET /api/data/browse with the shared route dependencies."} api-data-browse! []
  "GET" "/api/data/browse"
  (let [qs (aget request "query")
        path (or (aget qs "path") "")
        ingestion-base (:ingestion-base-url config)
        target-url (str ingestion-base "/api/ingestion/browse" (if (str/blank? path) "" (str "?path=" (js/encodeURIComponent path))))]
    (app-http/send-data-browse! reply target-url)))

(backend-macros/defroute ^{:doc "Register GET /api/data/file with the shared route dependencies."} api-data-file! []
  "GET" "/api/data/file"
  (let [qs (aget request "query")
        path (or (aget qs "path") "")
        ingestion-base (:ingestion-base-url config)]
    (app-http/send-fetch-json! reply
                      (str ingestion-base "/api/ingestion/file?path=" (js/encodeURIComponent path))
                      nil
                      app-http/fetch-json-err)))

(backend-macros/defroute ^{:doc "Register POST /api/data/graphql with the shared route dependencies."} api-data-graphql! []
  "POST" "/api/data/graphql"
  (let [body (infra-http/request-body request)
        gw-url "http://127.0.0.1:8796/graphql"]
    (app-http/send-fetch-json! reply
                      gw-url
                      {:method "POST"
                       :headers {"Content-Type" "application/json"}
                       :body (or (some-> body js/JSON.stringify) "{}")}
                      app-http/fetch-json-err)))

(backend-macros/defroute ^{:doc "Register GET /api/data/graph/status with the shared route dependencies."} api-data-graph-status! []
  "GET" "/api/data/graph/status"
  (app-http/send-fetch-json! reply "http://127.0.0.1:8796/api/status" {:method "GET"} app-http/fetch-json-err))

(backend-macros/defroute ^{:doc "Register GET /api/data/graph/view-url with the shared route dependencies."} api-data-graph-view-url! []
  "GET" "/api/data/graph/view-url"
  (json-response! reply 200 {:url "http://127.0.0.1:8796"}))

(defn register-data-routes!
  "Compose the data routes with their existing dependencies."
  [app runtime config]
  (api-data-op-get! app runtime config app-dependencies/deps)
  (api-data-op-post! app runtime config app-dependencies/deps)
  (api-data-op-patch! app runtime config app-dependencies/deps)
  (api-data-op-delete! app runtime config app-dependencies/deps)
  (api-data-health! app runtime config app-dependencies/deps)
  (api-data-mongo-collections! app runtime config app-dependencies/deps)
  (api-data-mongo-list! app runtime config app-dependencies/deps)
  (api-data-mongo-query! app runtime config app-dependencies/deps)
  (api-data-pg-tables! app runtime config app-dependencies/deps)
  (api-data-jobs-build-semantic-edges! app runtime config app-dependencies/deps)
  (api-data-pg-query! app runtime config app-dependencies/deps)
  (api-data-browse! app runtime config app-dependencies/deps)
  (api-data-file! app runtime config app-dependencies/deps)
  (api-data-graphql! app runtime config app-dependencies/deps)
  (api-data-graph-status! app runtime config app-dependencies/deps)
  (api-data-graph-view-url! app runtime config app-dependencies/deps))

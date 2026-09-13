(ns knoxx.backend.infra.routes.app.ingestion
  "Register ingestion and compatibility proxy routes."
  (:require-macros [knoxx.backend.macros :as backend-macros])
  (:require ["node:path" :as path]
            [clojure.string :as str]
            [knoxx.backend.extern.app-http :as app-http]
            [knoxx.backend.infra.document-state :as infra-document-state]
            [knoxx.backend.infra.http :as infra-http]
            [knoxx.backend.infra.routes.app.dependencies :as app-dependencies]))

(backend-macros/defroute ^{:doc "Register GET /api/knoxx/proxy/* with the shared route dependencies."} api-knoxx-proxy-get! []
  "GET" "/api/knoxx/proxy/*"
  (app-http/send-knoxx-proxy! config request reply "GET" (aget request "params" "*")))

(backend-macros/defroute ^{:doc "Register POST /api/knoxx/proxy/* with the shared route dependencies."} api-knoxx-proxy-post! []
  "POST" "/api/knoxx/proxy/*"
  (app-http/send-knoxx-proxy! config request reply "POST" (aget request "params" "*")))

(backend-macros/defroute ^{:doc "Register PUT /api/knoxx/proxy/* with the shared route dependencies."} api-knoxx-proxy-put! []
  "PUT" "/api/knoxx/proxy/*"
  (app-http/send-knoxx-proxy! config request reply "PUT" (aget request "params" "*")))

(backend-macros/defroute ^{:doc "Register PATCH /api/knoxx/proxy/* with the shared route dependencies."} api-knoxx-proxy-patch! []
  "PATCH" "/api/knoxx/proxy/*"
  (app-http/send-knoxx-proxy! config request reply "PATCH" (aget request "params" "*")))

(backend-macros/defroute ^{:doc "Register DELETE /api/knoxx/proxy/* with the shared route dependencies."} api-knoxx-proxy-delete! []
  "DELETE" "/api/knoxx/proxy/*"
  (app-http/send-knoxx-proxy! config request reply "DELETE" (aget request "params" "*")))

(backend-macros/defroute ^{:doc "Register GET /api/ingestion/browse with the shared route dependencies."} api-ingestion-browse! []
  "GET" "/api/ingestion/browse"
  (let [target-url (str (:ingestion-base-url config) "/api/ingestion/browse"
                        (request-query-string request))]
    (app-http/send-fetch-json! reply target-url {:method "GET"} app-http/fetch-json-err)))

(backend-macros/defroute ^{:doc "Register GET /api/ingestion/file with the shared route dependencies."} api-ingestion-file! []
  "GET" "/api/ingestion/file"
  (let [target-url (str (:ingestion-base-url config) "/api/ingestion/file"
                        (request-query-string request))]
    (app-http/send-fetch-json! reply target-url {:method "GET"} app-http/fetch-json-err)))

(backend-macros/defroute ^{:doc "Register PUT /api/ingestion/file with the shared route dependencies."} api-ingestion-file-put! []
  "PUT" "/api/ingestion/file"
  (let [workspace-root (:workspace-root config)
        body (infra-http/request-body request)
        file-path (or (:path body) "")
        content (or (:content body) "")
        safe-path (infra-document-state/normalize-relative-path file-path)]
    (if (or (str/blank? safe-path) (str/starts-with? safe-path ".."))
      (json-response! reply 400 {:detail "Invalid or unsafe file path"})
      (let [absolute-path (.resolve path workspace-root safe-path)]
        (if (not (str/starts-with? absolute-path workspace-root))
          (json-response! reply 400 {:detail "Path escapes workspace"})
          (app-http/write-ingestion-file! reply absolute-path content safe-path))))))

(backend-macros/defroute ^{:doc "Register GET /api/ingestion/sources with the shared route dependencies."} api-ingestion-sources! []
  "GET" "/api/ingestion/sources"
  (app-http/send-fetch-json! reply
                    (str (:ingestion-base-url config) "/api/ingestion/sources")
                    {:method "GET"}
                    app-http/fetch-json-err))

(backend-macros/defroute ^{:doc "Register GET /api/ingestion/jobs with the shared route dependencies."} api-ingestion-jobs-get! []
  "GET" "/api/ingestion/jobs"
  (let [target-url (str (:ingestion-base-url config) "/api/ingestion/jobs"
                        (request-query-string request))]
    (app-http/send-fetch-json! reply target-url {:method "GET"} app-http/fetch-json-err)))

(backend-macros/defroute ^{:doc "Register POST /api/ingestion/jobs with the shared route dependencies."} api-ingestion-jobs-post! []
  "POST" "/api/ingestion/jobs"
  (let [body (aget request "body")]
    (app-http/send-fetch-json! reply
                      (str (:ingestion-base-url config) "/api/ingestion/jobs")
                      {:method "POST"
                       :headers {"Content-Type" "application/json"}
                       :body (or (some-> body js/JSON.stringify) "{}")}
                      app-http/fetch-json-err)))

(backend-macros/defroute ^{:doc "Register GET /api/ingestion-proxy/* with the shared route dependencies."} api-ingestion-proxy-get! []
  "GET" "/api/ingestion-proxy/*"
  (let [ingestion-base (:ingestion-base-url config)
        path (aget request "params" "*")
        qs (request-query-string request)
        target-url (str ingestion-base "/api/ingestion/" path qs)]
    (app-http/send-fetch-json-detail! reply target-url {:method "GET"} "Ingestion proxy failed: ")))

(backend-macros/defroute ^{:doc "Register POST /api/ingestion-proxy/* with the shared route dependencies."} api-ingestion-proxy-post! []
  "POST" "/api/ingestion-proxy/*"
  (let [ingestion-base (:ingestion-base-url config)
        path (aget request "params" "*")
        target-url (str ingestion-base "/api/ingestion/" path)
        body (aget request "body")]
    (app-http/send-fetch-json-detail! reply
                             target-url
                             {:method "POST"
                              :headers {"Content-Type" "application/json"}
                              :body (or (some-> body js/JSON.stringify) "{}")}
                             "Ingestion proxy failed: ")))

(backend-macros/defroute ^{:doc "Register DELETE /api/ingestion-proxy/* with the shared route dependencies."} api-ingestion-proxy-delete! []
  "DELETE" "/api/ingestion-proxy/*"
  (let [ingestion-base (:ingestion-base-url config)
        path (aget request "params" "*")
        target-url (str ingestion-base "/api/ingestion/" path)]
    (app-http/send-fetch-json-detail! reply target-url {:method "DELETE"} "Ingestion proxy failed: ")))

(defn register-ingestion-routes!
  "Compose the ingestion routes with their existing dependencies."
  [app runtime config]
  (api-knoxx-proxy-get! app runtime config app-dependencies/deps)
  (api-knoxx-proxy-post! app runtime config app-dependencies/deps)
  (api-knoxx-proxy-put! app runtime config app-dependencies/deps)
  (api-knoxx-proxy-patch! app runtime config app-dependencies/deps)
  (api-knoxx-proxy-delete! app runtime config app-dependencies/deps)
  (api-ingestion-browse! app runtime config app-dependencies/deps)
  (api-ingestion-file! app runtime config app-dependencies/deps)
  (api-ingestion-sources! app runtime config app-dependencies/deps)
  (api-ingestion-jobs-get! app runtime config app-dependencies/deps)
  (api-ingestion-jobs-post! app runtime config app-dependencies/deps)
  (api-ingestion-proxy-get! app runtime config app-dependencies/deps)
  (api-ingestion-proxy-post! app runtime config app-dependencies/deps)
  (api-ingestion-proxy-delete! app runtime config app-dependencies/deps))

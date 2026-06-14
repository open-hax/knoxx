(ns knoxx.backend.infra.routes.tools.proxy
  "Fastify routes that proxy through to other internal services.

   These used to live in src/server.mjs; keeping them in CLJS ensures the Node
   host shim stays a pure dependency injector."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.domain.actor.scope :as actor-scope]
            [knoxx.backend.infra.core-memory :as core-memory]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.http :as backend-http]
            [knoxx.backend.infra.eta-mu-session-ingester :as eta-mu-sessions]
            [knoxx.backend.infra.source.opencode-session-ingester :as opencode-sessions]))

(defn- ^:async enrich-session-summary!
  [config summary]
  (let [session-id (or (:session summary) (get summary :session))]
    (if-not session-id
      summary
      (try
        (let [rows (await (core-memory/fetch-openplanner-session-rows! config session-id))
              contract-id (core-memory/session-contract-id-from-rows rows)
              actor-id (core-memory/session-actor-id-from-rows rows)
              contract-actors (core-memory/session-contract-actors-from-rows rows)
              wire-actors (when (seq contract-actors)
                            (actor-scope/actor-claims->wire contract-actors))]
          (cond-> summary
            contract-id (assoc :contract_id contract-id)
            actor-id (assoc :actor_id actor-id)
            (seq wire-actors) (assoc :contract_actors wire-actors)))
        (catch :default _
          ;; If enrichment fails (e.g. permissions, missing session), return the base summary.
          summary)))))

(defn- now-iso [] (.toISOString (js/Date.)))

(defn- json-content-type
  [resp]
  (or (some-> resp (aget "headers") (.get "content-type")) "application/json"))

(defn- ^:async safe-json
  [resp]
  (try
    (await (.json resp))
    (catch :default _ nil)))

(defn- ^:async safe-text
  [resp]
  (try
    (await (.text resp))
    (catch :default _ "")))

(defn- reply-sent?
  [reply]
  (let [raw (aget reply "raw")]
    (boolean
     (or (aget reply "sent")
         (and raw (aget raw "writableEnded"))))))

(defn- request-query-string
  [req]
  (let [query (or (aget req "query") (js/Object.))
        params (js/URLSearchParams.)]
    (doseq [key (array-seq (.keys js/Object query))]
      (let [value (aget query key)]
        (cond
          (nil? value) nil
          (= value js/undefined) nil
          (array? value) (doseq [item (array-seq value)]
                           (.append params key (str item)))
          :else (.append params key (str value)))))
    (let [encoded (.toString params)]
      (if (str/blank? encoded) "" (str "?" encoded)))))

(defn- reply-send-with-content-type!
  [^js reply status content-type body]
  (when-not (reply-sent? reply)
    (let [reply* (.code reply status)]
      (.header reply* "content-type" content-type)
      (.send reply* body))))

(defn- send-proxy-error!
  [reply prefix err]
  (when-not (reply-sent? reply)
    (backend-http/json-response! reply 502 {:ok false
                                            :error (str prefix ": " (or (aget err "message") (str err)))})))

(defn- request-body
  [req]
  (if (contains? #{"GET" "HEAD"} (aget req "method"))
    js/undefined
    (js/JSON.stringify (aget req "body"))))

(defn- ^:async proxy-fetch!
  [target-url req reply headers error-prefix]
  (try
    (let [resp (await (backend-http/fetch-with-timeout
                        target-url
                        {:method (aget req "method")
                         :headers headers
                         :body (request-body req)}
                        60000))
          content-type (json-content-type resp)
          body (if (str/includes? content-type "application/json")
                 (await (safe-json resp))
                 (await (safe-text resp)))]
      (reply-send-with-content-type! reply (.-status resp) content-type body))
    (catch :default err
      (send-proxy-error! reply error-prefix err))))

(defn- kms-base-url
  [config]
  (or (:ingestion-base-url config) "http://localhost:3003"))

(defn- system-kms-headers
  []
  {"x-knoxx-user-email" "system-admin@open-hax.local"
   "x-knoxx-org-slug" "open-hax"})

(defn- ^:async find-kms-source-jobs!
  [kms-base kms-headers driver-type]
  (try
    (let [sources-r (await (backend-http/fetch-with-timeout
                             (str kms-base "/api/ingestion/sources?tenant_id=knoxx-session")
                             {:headers kms-headers}
                             15000))
          sources (if (.-ok sources-r)
                    (try (await (.json sources-r)) (catch :default _ (js/Array.)))
                    (js/Array.))
          source (.find (if (array? sources) sources (js/Array.))
                        (fn [s] (= (aget s "driver_type") driver-type)))]
      (if-not source
        {:ok false :error (str driver-type " source not found") :sources sources}
        (let [jobs-r (await (backend-http/fetch-with-timeout
                              (str kms-base "/api/ingestion/jobs?tenant_id=knoxx-session&source_id=" (aget source "source_id"))
                              {:headers kms-headers}
                              15000))
              jobs (if (.-ok jobs-r)
                     (try (await (.json jobs-r)) (catch :default _ (js/Array.)))
                     (js/Array.))]
          {:ok true :source source :jobs jobs})))
    (catch :default _
      {:ok false :error "Failed to fetch ingestion sources"})))

(defn- ^:async session-status-handler!
  [config payload-key status! driver-type]
  (try
    (let [kms-base (kms-base-url config)
          kms-headers (system-kms-headers)
          [local kms] (await (promise/all-vec
                              [(try
                                 (await (status!))
                                 (catch :default err
                                   {:ok false :error (.-message err)}))
                               (find-kms-source-jobs! kms-base kms-headers driver-type)]))]
      {:ok true payload-key local :kms_ingestion kms :time (now-iso)})
    (catch :default err
      {:ok false :error (.-message err)})))

(defn- register-session-status-route!
  [^js app config path payload-key status! driver-type]
  (.get app path
        (^:async fn [_req reply]
          (let [result (await (session-status-handler! config payload-key status! driver-type))]
            (backend-http/json-response! reply (if (:ok result) 200 500) result)))))

(defn- ^:async eta-mu-session-list-handler!
  [req]
  (let [q (or (aget req "query") (js/Object.))]
    (await (eta-mu-sessions/list-eta-mu-sessions {:limit (min (js/parseInt (or (aget q "limit") "50") 10) 200)
                                                  :offset (js/parseInt (or (aget q "offset") "0") 10)
                                                  :workspace (aget q "workspace")}))))

(defn- ^:async eta-mu-session-list-responder!
  [req reply]
  (try
    (let [result (await (eta-mu-session-list-handler! req))]
      (.send reply result))
    (catch :default err
      (backend-http/json-response! reply 500 {:ok false :error (str err)}))))

(defn- register-eta-mu-session-list-route!
  [^js app]
  (.get app "/api/admin/eta-mu-sessions"
        (fn [req reply]
          (eta-mu-session-list-responder! req reply))))

(defn- ^:async source-ingest-request!
  [kms-base kms-headers driver-type force? reply]
  (try
    (let [sources-r (await (backend-http/fetch-with-timeout (str kms-base "/api/ingestion/sources?tenant_id=knoxx-session")
                                                             {:headers kms-headers}
                                                             20000))
          sources (if (.-ok sources-r)
                    (try (await (.json sources-r)) (catch :default _ (js/Array.)))
                    (js/Array.))
          source (.find (if (array? sources) sources (js/Array.))
                        (fn [s] (= (aget s "driver_type") driver-type)))]
      (if-not source
        (backend-http/json-response! reply 404 {:ok false :error (str driver-type " source not found in ingestion service")})
        (let [job-r (await (backend-http/fetch-with-timeout
                             (str kms-base "/api/ingestion/jobs")
                             {:method "POST"
                              :headers kms-headers
                              :body (js/JSON.stringify (clj->js {:source_id (aget source "source_id")
                                                                 :full_scan force?}))}
                             20000))
              job (if (.-ok job-r)
                    (try (await (.json job-r)) (catch :default _ (await (safe-json job-r))))
                    (await (safe-json job-r)))]
          (backend-http/json-response! reply 200 {:ok true :job job}))))
    (catch :default err
      (backend-http/json-response! reply 500 {:ok false :error (.-message err)}))))

(defn- register-session-ingest-route!
  [^js app config path driver-type]
  (.post app path
         (fn [req reply]
           (let [kms-headers (assoc (system-kms-headers) "content-type" "application/json")
                 body (or (aget req "body") (js/Object.))]
             (source-ingest-request! (kms-base-url config)
                                     kms-headers
                                     driver-type
                                     (boolean (aget body "force"))
                                     reply)))))

(defn- ^:async opencode-session-list-handler!
  [req]
  (let [q (or (aget req "query") (js/Object.))]
    (await (opencode-sessions/list-opencode-sessions {:limit (min (js/parseInt (or (aget q "limit") "50") 10) 200)
                                                       :cursor (aget q "cursor")
                                                       :directory (aget q "directory")
                                                       :search (aget q "search")
                                                       :roots (when (some? (aget q "roots")) (= "true" (str (aget q "roots"))))
                                                       :archived (if (some? (aget q "archived")) (= "true" (str (aget q "archived"))) true)}))))

(defn- ^:async opencode-session-list-responder!
  [req reply]
  (try
    (let [result (await (opencode-session-list-handler! req))]
      (.send reply result))
    (catch :default err
      (backend-http/json-response! reply 500 {:ok false :error (str err)}))))

(defn- register-opencode-session-list-route!
  [^js app]
  (.get app "/api/admin/opencode-sessions"
        (fn [req reply]
          (opencode-session-list-responder! req reply))))

(defn- register-ingestion-service-proxy-route!
  [^js app config]
  (.all app "/api/ingestion/*"
        (fn [req reply]
          (let [sub-path (aget (aget req "params") "*")
                target-url (str (kms-base-url config) "/api/ingestion/" sub-path (request-query-string req))
                headers (js/Object.assign (js/Object.) (aget req "headers"))]
            (js/Reflect.deleteProperty headers "host")
            (js/Reflect.deleteProperty headers "connection")
            (js/Reflect.deleteProperty headers "content-length")
            (proxy-fetch! target-url req reply headers "Ingestion proxy error")))))

(defn- ^:async openplanner-proxy-handler!
  [config req reply]
  (try
    (let [body (request-body req)
          sub-path (aget (aget req "params") "*")
          fwd-headers {"x-knoxx-user-email" (or (aget (aget req "headers") "x-knoxx-user-email") "")
                       "x-knoxx-org-slug" (or (aget (aget req "headers") "x-knoxx-org-slug") "")}
          request* (cond-> {:method (aget req "method")
                            :path sub-path
                            :query-string (request-query-string req)
                            :headers fwd-headers}
                     (not= body js/undefined) (assoc :body body))
          resp (await (openplanner-client/forward-v1! (openplanner-client/client config) request*))
          content-type (json-content-type resp)
          resp-body (await (if (str/includes? content-type "application/json")
                             (safe-json resp)
                             (safe-text resp)))]
      (reply-send-with-content-type! reply (.-status resp) content-type resp-body))
    (catch :default err
      (send-proxy-error! reply "OpenPlanner proxy error" err))))

(defn- register-openplanner-proxy-routes!
  [^js app config]
  (.get app "/api/openplanner/v1/sessions"
        (^:async fn [req reply]
          (let [body (await (openplanner-client/sessions!
                             (openplanner-client/client config)
                             (js->clj (or (aget req "query") (js/Object.)) :keywordize-keys true)))
                enriched (await (js/Promise.all
                                  (clj->js (map #(enrich-session-summary! config %) (vec (or (:rows body) []))))))]
            (.send reply (clj->js (assoc body :rows (vec (array-seq enriched))))))))
  (.all app "/api/openplanner/*"
        (fn [req reply]
          (openplanner-proxy-handler! config req reply))))

(defn register-proxy-routes!
  "Register all proxy endpoints on the fastify app."
  [^js app config]
  (register-session-status-route! app config "/api/admin/eta-mu-sessions/status" :legacy eta-mu-sessions/get-eta-mu-ingest-status "eta-mu-sessions")
  (register-eta-mu-session-list-route! app)
  (register-session-ingest-route! app config "/api/admin/eta-mu-sessions/ingest" "eta-mu-sessions")
  (register-session-status-route! app config "/api/admin/opencode-sessions/status" :opencode opencode-sessions/get-opencode-ingest-status "opencode-sessions")
  (register-opencode-session-list-route! app)
  (register-session-ingest-route! app config "/api/admin/opencode-sessions/ingest" "opencode-sessions")
  (register-ingestion-service-proxy-route! app config)
  (register-openplanner-proxy-routes! app config))

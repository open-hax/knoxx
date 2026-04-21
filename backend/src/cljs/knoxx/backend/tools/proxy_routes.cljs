(ns knoxx.backend.tools.proxy-routes
  "Fastify routes that proxy through to other internal services.

   These used to live in src/server.mjs; keeping them in CLJS ensures the Node
   host shim stays a pure dependency injector."  
  (:require [clojure.string :as str]
            [knoxx.backend.pi-session-ingester :as pi]))

(defn- now-iso [] (.toISOString (js/Date.)))

(defn- json-content-type
  [resp]
  (or (some-> resp (aget "headers") (.get "content-type")) "application/json"))

(defn- safe-json
  [resp]
  (-> (.json resp)
      (.catch (fn [_] nil))))

(defn- safe-text
  [resp]
  (-> (.text resp)
      (.catch (fn [_] ""))))

(defn register-proxy-routes!
  "Register all proxy endpoints on the fastify app."  
  [app config]

  ;; ---------------------------------------------------------------------------
  ;; Pi Session Ingestion Routes
  ;; ---------------------------------------------------------------------------

  ;; GET /api/admin/pi-sessions/status — ingestion state overview
  ;;
  ;; NOTE: There are *two* ingestion mechanisms:
  ;; - legacy JS ingester state: ~/.knoxx/pi-ingest-state/ingested-sessions.json
  ;; - current kms-ingestion service (pi-sessions driver)
  (.get app "/api/admin/pi-sessions/status"
        (fn [_req reply]
          (let [kms-base (or (:ingestion-base-url config) "http://localhost:3003")
                kms-headers #js {"x-knoxx-user-email" "system-admin@open-hax.local"
                                "x-knoxx-org-slug" "open-hax"}
                legacy-p (-> (pi/get-pi-ingest-status)
                             (.catch (fn [err]
                                       #js {:ok false :error (.-message err)})))
                kms-p (-> (js/fetch (str kms-base "/api/ingestion/sources?tenant_id=knoxx-session")
                                    #js {:headers kms-headers
                                         :signal (AbortSignal.timeout 15000)})
                          (.then (fn [r]
                                   (if (.-ok r) (.json r) (js/Promise.resolve #js []))))
                          (.catch (fn [_] #js []))
                          (.then
                           (fn [sources]
                             (let [sources (if (array? sources) sources #js [])
                                   pi-source (.find sources (fn [s] (= (aget s "driver_type") "pi-sessions")))]
                               (if-not pi-source
                                 #js {:ok false :error "pi-sessions source not found" :sources sources}
                                 (-> (js/fetch (str kms-base "/api/ingestion/jobs?tenant_id=knoxx-session&source_id=" (aget pi-source "source_id"))
                                               #js {:headers kms-headers
                                                    :signal (AbortSignal.timeout 15000)})
                                     (.then (fn [r]
                                              (if (.-ok r) (.json r) (js/Promise.resolve #js []))))
                                     (.catch (fn [_] #js []))
                                     (.then (fn [jobs]
                                              #js {:ok true :source pi-source :jobs jobs}))))))))]
            (-> (js/Promise.all #js [legacy-p kms-p])
                (.then
                 (fn [parts]
                   (let [legacy (aget parts 0)
                         kms (aget parts 1)]
                     (.send reply #js {:ok true
                                       :legacy legacy
                                       :kms_ingestion kms
                                       :time (now-iso)}))))
                (.catch
                 (fn [err]
                   (.code reply 500)
                   (.send reply #js {:ok false :error (.-message err)})))))))

  ;; GET /api/admin/pi-sessions — list available pi sessions
  (.get app "/api/admin/pi-sessions"
        (fn [req reply]
          (try
            (let [q (or (aget req "query") #js {})
                  limit (min (js/parseInt (or (aget q "limit") "50") 10) 200)
                  offset (js/parseInt (or (aget q "offset") "0") 10)
                  workspace (aget q "workspace")]
              (-> (pi/list-pi-sessions {:limit limit :offset offset :workspace workspace})
                  (.then (fn [result] (.send reply result)))
                  (.catch (fn [err]
                            (.code reply 500)
                            (.send reply #js {:ok false :error (.-message err)})))))
            (catch :default err
              (.code reply 500)
              (.send reply #js {:ok false :error (str err)})))))

  ;; POST /api/admin/pi-sessions/ingest — proxy to ingestion service
  (.post app "/api/admin/pi-sessions/ingest"
         (fn [req reply]
           (let [kms-base (or (:ingestion-base-url config) "http://localhost:3003")
                 kms-headers #js {"content-type" "application/json"
                                 "x-knoxx-user-email" "system-admin@open-hax.local"
                                 "x-knoxx-org-slug" "open-hax"}
                 body (or (aget req "body") #js {})
                 force? (boolean (aget body "force"))]
             (-> (js/fetch (str kms-base "/api/ingestion/sources?tenant_id=knoxx-session")
                           #js {:headers kms-headers
                                :signal (AbortSignal.timeout 20000)})
                 (.then (fn [r] (if (.-ok r) (.json r) (js/Promise.resolve #js []))))
                 (.catch (fn [_] #js []))
                 (.then
                  (fn [sources]
                    (let [sources (if (array? sources) sources #js [])
                          pi-source (.find sources (fn [s] (= (aget s "driver_type") "pi-sessions")))]
                      (if-not pi-source
                        (do (.code reply 404)
                            (.send reply #js {:ok false :error "pi-sessions source not found in ingestion service"}))
                        (-> (js/fetch (str kms-base "/api/ingestion/jobs")
                                      #js {:method "POST"
                                           :headers kms-headers
                                           :body (js/JSON.stringify #js {:source_id (aget pi-source "source_id")
                                                                        :full_scan force?})
                                           :signal (AbortSignal.timeout 20000)})
                            (.then (fn [r] (if (.-ok r) (.json r) (safe-json r))))
                            (.then (fn [job]
                                     (.send reply #js {:ok true :job job})))
                            (.catch (fn [err]
                                      (.code reply 500)
                                      (.send reply #js {:ok false :error (.-message err)}))))))))))))

  ;; ---------------------------------------------------------------------------
  ;; Ingestion Service Proxy
  ;; ---------------------------------------------------------------------------

  (.all app "/api/ingestion/*"
        (fn [req reply]
          (let [kms-base (or (:ingestion-base-url config) "http://localhost:3003")
                sub-path (aget (aget req "params") "*")
                target-url (str kms-base "/api/ingestion/" sub-path)
                headers (js/Object.assign #js {} (aget req "headers"))]
            (js/delete headers "host")
            (js/delete headers "connection")
            (js/delete headers "content-length")
            (-> (js/fetch target-url
                          #js {:method (aget req "method")
                               :headers headers
                               :body (if (contains? #{"GET" "HEAD"} (aget req "method"))
                                       js/undefined
                                       (js/JSON.stringify (aget req "body")))
                               :signal (AbortSignal.timeout 60000)})
                (.then (fn [resp]
                         (let [content-type (json-content-type resp)]
                           (-> (if (str/includes? content-type "application/json")
                                 (safe-json resp)
                                 (safe-text resp))
                               (.then
                                (fn [body]
                                  (-> (.code reply (.-status resp))
                                      (.header "content-type" content-type)
                                      (.send body))))))))
                (.catch (fn [err]
                          (.code reply 502)
                          (.send reply #js {:ok false :error (str "Ingestion proxy error: " (.-message err))})))))))

  ;; ---------------------------------------------------------------------------
  ;; OpenPlanner Proxy
  ;; ---------------------------------------------------------------------------

  ;; Frontend calls /api/openplanner/v1/* but OpenPlanner serves /v1/*.
  ;; Proxy: strip /api/openplanner prefix, add Authorization header.
  (.all app "/api/openplanner/*"
        (fn [req reply]
          (let [base (or (:openplanner-base-url config) "http://localhost:7777")
                key (or (:openplanner-api-key config) "change-me")
                sub-path (aget (aget req "params") "*")
                target-url (str base "/" sub-path)
                fwd-headers #js {"content-type" "application/json"
                                 "authorization" (str "Bearer " key)
                                 "x-knoxx-user-email" (or (aget (aget req "headers") "x-knoxx-user-email") "")
                                 "x-knoxx-org-slug" (or (aget (aget req "headers") "x-knoxx-org-slug") "")}]
            (-> (js/fetch target-url
                          #js {:method (aget req "method")
                               :headers fwd-headers
                               :body (if (contains? #{"GET" "HEAD"} (aget req "method"))
                                       js/undefined
                                       (js/JSON.stringify (aget req "body")))
                               :signal (AbortSignal.timeout 60000)})
                (.then (fn [resp]
                         (let [content-type (json-content-type resp)]
                           (-> (if (str/includes? content-type "application/json")
                                 (safe-json resp)
                                 (safe-text resp))
                               (.then
                                (fn [body]
                                  (-> (.code reply (.-status resp))
                                      (.header "content-type" content-type)
                                      (.send body))))))))
                (.catch (fn [err]
                          (.code reply 502)
                          (.send reply #js {:ok false :error (str "OpenPlanner proxy error: " (.-message err))})))))))

  nil)

(ns knoxx.backend.infra.routes.tools.proxy
  "Fastify routes that proxy through to other internal services.

   These used to live in src/server.mjs; keeping them in CLJS ensures the Node
   host shim stays a pure dependency injector."
  (:require [shadow.cljs.modern :refer [js-await]]
            [clojure.string :as str]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.domain.actor.scope :as actor-scope]
            [knoxx.backend.infra.core-memory :as core-memory]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.http :as backend-http]
            [knoxx.backend.infra.eta-mu-session-ingester :as eta-mu-sessions]
            [knoxx.backend.infra.source.opencode-session-ingester :as opencode-sessions]))

(defn- enrich-session-summary!
  [config summary]
  (let [session-id (or (:session summary) (get summary :session))]
    (if-not session-id
      (js/Promise.resolve summary)
      (-> (core-memory/fetch-openplanner-session-rows! config session-id)
          (.then (fn [rows]
                   (let [contract-id (core-memory/session-contract-id-from-rows rows)
                         actor-id (core-memory/session-actor-id-from-rows rows)
                         contract-actors (core-memory/session-contract-actors-from-rows rows)
                         wire-actors (when (seq contract-actors)
                                      (actor-scope/actor-claims->wire contract-actors))]
                     (cond-> summary
                       contract-id (assoc :contract_id contract-id)
                       actor-id (assoc :actor_id actor-id)
                       (seq wire-actors) (assoc :contract_actors wire-actors)))))
          (.catch (fn [_]
                    ;; If enrichment fails (e.g. permissions, missing session), return the base summary.
                    summary))))))

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

(defn- proxy-fetch!
  [target-url req reply headers error-prefix]
  (let [fetch-promise (backend-http/fetch-with-timeout
                        target-url
                        {:method (aget req "method")
                         :headers headers
                         :body (request-body req)}
                        60000)]
    (.then fetch-promise
           (fn [resp]
             (let [content-type (json-content-type resp)
                   body-promise (if (str/includes? content-type "application/json")
                                  (safe-json resp)
                                  (safe-text resp))]
               (.then body-promise
                      (fn [body]
                        (reply-send-with-content-type! reply (.-status resp) content-type body))
                      (fn [err]
                        (send-proxy-error! reply error-prefix err)))))
           (fn [err]
             (send-proxy-error! reply error-prefix err)))))

(defn- kms-base-url
  [config]
  (or (:ingestion-base-url config) "http://localhost:3003"))

(defn- system-kms-headers
  []
  {"x-knoxx-user-email" "system-admin@open-hax.local"
   "x-knoxx-org-slug" "open-hax"})

(defn- find-kms-source-jobs!
  [kms-base kms-headers driver-type]
  (-> (backend-http/fetch-with-timeout
       (str kms-base "/api/ingestion/sources?tenant_id=knoxx-session")
       {:headers kms-headers}
       15000)
      (.then (fn [r] (if (.-ok r) (.json r) (js/Promise.resolve (js/Array.)))))
      (.catch (fn [_] (js/Array.)))
      (.then (fn [sources]
               (let [sources (if (array? sources) sources (js/Array.))
                     source (.find sources (fn [s] (= (aget s "driver_type") driver-type)))]
                 (if-not source
                   {:ok false :error (str driver-type " source not found") :sources sources}
                   (-> (backend-http/fetch-with-timeout
                        (str kms-base "/api/ingestion/jobs?tenant_id=knoxx-session&source_id=" (aget source "source_id"))
                        {:headers kms-headers}
                        15000)
                       (.then (fn [r] (if (.-ok r) (.json r) (js/Promise.resolve (js/Array.)))))
                       (.catch (fn [_] (js/Array.)))
                       (.then (fn [jobs] {:ok true :source source :jobs jobs})))))))))

(defn- register-session-status-route!
  [^js app config path payload-key status! driver-type]
  (.get app path
        (fn [_req reply]
          (let [kms-base (kms-base-url config)
                kms-headers (system-kms-headers)
                local-p (-> (status!)
                            (.catch (fn [err] {:ok false :error (.-message err)})))
                kms-p (find-kms-source-jobs! kms-base kms-headers driver-type)]
            (-> (promise/all-vec [local-p kms-p])
                (.then (fn [[local kms]]
                         (backend-http/json-response! reply 200 {:ok true
                                                                 payload-key local
                                                                 :kms_ingestion kms
                                                                 :time (now-iso)})))
                (.catch (fn [err]
                          (backend-http/json-response! reply 500 {:ok false :error (.-message err)}))))))))

(defn- register-eta-mu-session-list-route!
  [^js app]
  (.get app "/api/admin/eta-mu-sessions"
        (fn [req reply]
          (try
            (let [q (or (aget req "query") (js/Object.))]
              (-> (eta-mu-sessions/list-eta-mu-sessions {:limit (min (js/parseInt (or (aget q "limit") "50") 10) 200)
                                                         :offset (js/parseInt (or (aget q "offset") "0") 10)
                                                         :workspace (aget q "workspace")})
                  (.then (fn [result] (.send reply result)))
                  (.catch (fn [err]
                            (backend-http/json-response! reply 500 {:ok false :error (.-message err)})))))
            (catch :default err
              (backend-http/json-response! reply 500 {:ok false :error (str err)}))))))

(defn- source-ingest-request!
  [kms-base kms-headers driver-type force? reply]
  (-> (backend-http/fetch-with-timeout (str kms-base "/api/ingestion/sources?tenant_id=knoxx-session")
                                       {:headers kms-headers}
                                       20000)
      (.then (fn [r] (if (.-ok r) (.json r) (js/Promise.resolve (js/Array.)))))
      (.catch (fn [_] (js/Array.)))
      (.then (fn [sources]
               (let [sources (if (array? sources) sources (js/Array.))
                     source (.find sources (fn [s] (= (aget s "driver_type") driver-type)))]
                 (if-not source
                   (backend-http/json-response! reply 404 {:ok false :error (str driver-type " source not found in ingestion service")})
                   (-> (backend-http/fetch-with-timeout
                        (str kms-base "/api/ingestion/jobs")
                        {:method "POST"
                         :headers kms-headers
                         :body (js/JSON.stringify (clj->js {:source_id (aget source "source_id")
                                                            :full_scan force?}))}
                        20000)
                       (.then (fn [r] (if (.-ok r) (.json r) (safe-json r))))
                       (.then (fn [job]
                                (backend-http/json-response! reply 200 {:ok true :job job})))
                       (.catch (fn [err]
                                 (backend-http/json-response! reply 500 {:ok false :error (.-message err)}))))))))))

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

(defn- register-opencode-session-list-route!
  [^js app]
  (.get app "/api/admin/opencode-sessions"
        (fn [req reply]
          (try
            (let [q (or (aget req "query") (js/Object.))]
              (-> (opencode-sessions/list-opencode-sessions {:limit (min (js/parseInt (or (aget q "limit") "50") 10) 200)
                                                             :cursor (aget q "cursor")
                                                             :directory (aget q "directory")
                                                             :search (aget q "search")
                                                             :roots (when (some? (aget q "roots")) (= "true" (str (aget q "roots"))))
                                                             :archived (if (some? (aget q "archived")) (= "true" (str (aget q "archived"))) true)})
                  (.then (fn [result] (.send reply result)))
                  (.catch (fn [err]
                            (backend-http/json-response! reply 500 {:ok false :error (.-message err)})))))
            (catch :default err
              (backend-http/json-response! reply 500 {:ok false :error (str err)}))))))

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

(defn- register-openplanner-proxy-routes!
  [^js app config]
  (.get app "/api/openplanner/v1/sessions"
        (fn [req reply]
          (js-await [body (openplanner-client/sessions!
                           (openplanner-client/client config)
                           (js->clj (or (aget req "query") (js/Object.)) :keywordize-keys true))]
            (js-await [enriched (js/Promise.all
                                  (clj->js (map #(enrich-session-summary! config %) (vec (or (:rows body) [])))))]
              (.send reply (clj->js (assoc body :rows (vec (array-seq enriched)))))))))
  (.all app "/api/openplanner/*"
        (fn [req reply]
          (let [body (request-body req)
                sub-path (aget (aget req "params") "*")
                fwd-headers {"x-knoxx-user-email" (or (aget (aget req "headers") "x-knoxx-user-email") "")
                             "x-knoxx-org-slug" (or (aget (aget req "headers") "x-knoxx-org-slug") "")}
                request* (cond-> {:method (aget req "method")
                                  :path sub-path
                                  :query-string (request-query-string req)
                                  :headers fwd-headers}
                           (not= body js/undefined) (assoc :body body))]
            (-> (openplanner-client/forward-v1! (openplanner-client/client config) request*)
                (.then (fn [resp]
                         (let [content-type (json-content-type resp)
                               body-promise (if (str/includes? content-type "application/json")
                                              (safe-json resp)
                                              (safe-text resp))]
                           (.then body-promise
                                  (fn [resp-body]
                                    (reply-send-with-content-type! reply (.-status resp) content-type resp-body))
                                  (fn [err]
                                    (send-proxy-error! reply "OpenPlanner proxy error" err)))))
                (.catch (fn [err]
                          (send-proxy-error! reply "OpenPlanner proxy error" err)))))))))

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

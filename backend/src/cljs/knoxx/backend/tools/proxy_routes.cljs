(ns knoxx.backend.tools.proxy-routes
  "Fastify routes that proxy through to other internal services.

   These used to live in src/server.mjs; keeping them in CLJS ensures the Node
   host shim stays a pure dependency injector."
  (:require [shadow.cljs.modern :refer [js-await]]
            [clojure.string :as str]
            [knoxx.backend.contracts.actor-scope :as actor-scope]
            [knoxx.backend.core-memory :as core-memory]
            [knoxx.backend.http :as backend-http]
            [knoxx.backend.eta-mu-session-ingester :as eta-mu-sessions]))

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

(defn- timeout-signal
  [ms]
  (.timeout js/AbortSignal ms))

(defn- reply-sent?
  [reply]
  (let [raw (aget reply "raw")]
    (boolean
     (or (aget reply "sent")
         (and raw (aget raw "writableEnded"))))))

(defn- request-query-string
  [req]
  (let [query (or (aget req "query") #js {})
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
    (-> (.code reply 502)
        (.send #js {:ok false
                    :error (str prefix ": " (or (aget err "message") (str err)))}))))

(defn- request-body
  [req]
  (if (contains? #{"GET" "HEAD"} (aget req "method"))
    js/undefined
    (js/JSON.stringify (aget req "body"))))

(defn- proxy-fetch!
  [target-url req reply headers error-prefix]
  (let [fetch-promise (js/fetch target-url
                                #js {:method (aget req "method")
                                     :headers headers
                                     :body (request-body req)
                                     :signal (timeout-signal 60000)})]
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

(defn register-proxy-routes!
  "Register all proxy endpoints on the fastify app."
  [^js app config]

  ;; ---------------------------------------------------------------------------
  ;; eta-mu Session Ingestion Routes
  ;; ---------------------------------------------------------------------------

  ;; GET /api/admin/eta-mu-sessions/status — ingestion state overview
  ;;
  ;; NOTE: There are *two* ingestion mechanisms:
  ;; - legacy JS ingester state: ~/.knoxx/eta-mu-ingest-state/ingested-sessions.json
  ;; - current kms-ingestion service (eta-mu-sessions driver)
  (.get app "/api/admin/eta-mu-sessions/status"
        (fn [_req reply]
          (let [kms-base (or (:ingestion-base-url config) "http://localhost:3003")
                kms-headers #js {"x-knoxx-user-email" "system-admin@open-hax.local"
                                "x-knoxx-org-slug" "open-hax"}
                legacy-p (-> (eta-mu-sessions/get-eta-mu-ingest-status)
                             (.catch (fn [err]
                                       #js {:ok false :error (.-message err)})))
                kms-p (-> (js/fetch (str kms-base "/api/ingestion/sources?tenant_id=knoxx-session")
                                    #js {:headers kms-headers
                                         :signal (timeout-signal 15000)})
                          (.then (fn [r]
                                   (if (.-ok r) (.json r) (js/Promise.resolve #js []))))
                          (.catch (fn [_] #js []))
                          (.then
                           (fn [sources]
                             (let [sources (if (array? sources) sources #js [])
                                   eta-mu-source (.find sources (fn [s] (= (aget s "driver_type") "eta-mu-sessions")))]
                               (if-not eta-mu-source
                                 #js {:ok false :error "eta-mu-sessions source not found" :sources sources}
                                 (-> (js/fetch (str kms-base "/api/ingestion/jobs?tenant_id=knoxx-session&source_id=" (aget eta-mu-source "source_id"))
                                               #js {:headers kms-headers
                                                    :signal (timeout-signal 15000)})
                                     (.then (fn [r]
                                              (if (.-ok r) (.json r) (js/Promise.resolve #js []))))
                                     (.catch (fn [_] #js []))
                                     (.then (fn [jobs]
                                              #js {:ok true :source eta-mu-source :jobs jobs}))))))))]
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

  ;; GET /api/admin/eta-mu-sessions — list available eta-mu sessions
  (.get app "/api/admin/eta-mu-sessions"
        (fn [req reply]
          (try
            (let [q (or (aget req "query") #js {})
                  limit (min (js/parseInt (or (aget q "limit") "50") 10) 200)
                  offset (js/parseInt (or (aget q "offset") "0") 10)
                  workspace (aget q "workspace")]
              (-> (eta-mu-sessions/list-eta-mu-sessions {:limit limit :offset offset :workspace workspace})
                  (.then (fn [result] (.send reply result)))
                  (.catch (fn [err]
                            (.code reply 500)
                            (.send reply #js {:ok false :error (.-message err)})))))
            (catch :default err
              (.code reply 500)
              (.send reply #js {:ok false :error (str err)})))))

  ;; POST /api/admin/eta-mu-sessions/ingest — proxy to ingestion service
  (.post app "/api/admin/eta-mu-sessions/ingest"
         (fn [req reply]
           (let [kms-base (or (:ingestion-base-url config) "http://localhost:3003")
                 kms-headers #js {"content-type" "application/json"
                                 "x-knoxx-user-email" "system-admin@open-hax.local"
                                 "x-knoxx-org-slug" "open-hax"}
                 body (or (aget req "body") #js {})
                 force? (boolean (aget body "force"))]
             (-> (js/fetch (str kms-base "/api/ingestion/sources?tenant_id=knoxx-session")
                           #js {:headers kms-headers
                                :signal (timeout-signal 20000)})
                 (.then (fn [r] (if (.-ok r) (.json r) (js/Promise.resolve #js []))))
                 (.catch (fn [_] #js []))
                 (.then
                  (fn [sources]
                    (let [sources (if (array? sources) sources #js [])
                          eta-mu-source (.find sources (fn [s] (= (aget s "driver_type") "eta-mu-sessions")))]
                      (if-not eta-mu-source
                        (do (.code reply 404)
                            (.send reply #js {:ok false :error "eta-mu-sessions source not found in ingestion service"}))
                        (-> (js/fetch (str kms-base "/api/ingestion/jobs")
                                      #js {:method "POST"
                                           :headers kms-headers
                                           :body (js/JSON.stringify #js {:source_id (aget eta-mu-source "source_id")
                                                                        :full_scan force?})
                                           :signal (timeout-signal 20000)})
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
                target-url (str kms-base "/api/ingestion/" sub-path (request-query-string req))
                headers (js/Object.assign #js {} (aget req "headers"))]
            (js/Reflect.deleteProperty headers "host")
            (js/Reflect.deleteProperty headers "connection")
            (js/Reflect.deleteProperty headers "content-length")
            (proxy-fetch! target-url req reply headers "Ingestion proxy error"))))

  ;; ---------------------------------------------------------------------------
  ;; OpenPlanner Proxy
  ;; ---------------------------------------------------------------------------

  ;; Enriched sessions list for Knoxx: OpenPlanner does not currently expose
  ;; actor/agent-contract identity in the session summary payload.
  ;; Knoxx reconstructs it from the archived session rows.
  (.get app "/api/openplanner/v1/sessions"
        (fn [req reply]
          (js-await [body (backend-http/openplanner-request! config "GET" (str "/v1/sessions" (request-query-string req)))]
            (js-await [enriched (js/Promise.all
                                  (clj->js (map #(enrich-session-summary! config %) (vec (or (:rows body) [])))))]
              (.send reply (clj->js (assoc body :rows (vec (array-seq enriched)))))))))

  ;; Frontend calls /api/openplanner/v1/* but OpenPlanner serves /v1/*.
  ;; Proxy: strip /api/openplanner prefix, add Authorization header.
  (.all app "/api/openplanner/*"
        (fn [req reply]
          (let [base (or (:openplanner-base-url config) "http://localhost:7777")
                key (or (:openplanner-api-key config) "change-me")
                sub-path (aget (aget req "params") "*")
                target-url (str base "/" sub-path (request-query-string req))
                fwd-headers #js {"content-type" "application/json"
                                 "authorization" (str "Bearer " key)
                                 "x-knoxx-user-email" (or (aget (aget req "headers") "x-knoxx-user-email") "")
                                 "x-knoxx-org-slug" (or (aget (aget req "headers") "x-knoxx-org-slug") "")}]
            (proxy-fetch! target-url req reply fwd-headers "OpenPlanner proxy error"))))
  )
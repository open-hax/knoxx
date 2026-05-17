(ns knoxx.backend.routes.models
  (:require [clojure.string :as str]
            [knoxx.backend.agent-hydration :refer [settings-state*]]
            [knoxx.backend.app-shapes :refer [route!]]
            [knoxx.backend.authz :refer [with-request-context! run-visible? ensure-permission! ctx-tool-constraints]]
            [knoxx.backend.http :refer [json-response! fetch-json bearer-headers require-openai-key! openai-auth-error send-fetch-response! error-response! http-error js-array-seq request-query-string]]
            [knoxx.backend.run-state :refer [runs* run-order* summarize-run]]
            [knoxx.backend.runtime.models :refer [allowlisted-model-id?]]
            [knoxx.backend.util.time :refer [now-iso]]
            [shadow.cljs.modern :refer [js-await]]))

(defn- proxx-configured?
  [config]
  (and (not (str/blank? (:proxx-base-url config)))
       (not (str/blank? (:proxx-auth-token config)))))

(defn- request-session-key
  "Best-effort extraction of a stable Knoxx session key from request headers.

   Knoxx frontend always sends x-knoxx-session-id. We map that to Proxx's
   prompt_cache_key for provider+account session affinity." 
  [request]
  (let [headers (or (aget request "headers") #js {})
        v (or (aget headers "x-knoxx-session-id")
              (aget headers "x-session-id")
              (aget headers "x-open-hax-session-id"))]
    (when (string? v)
      (let [trimmed (str/trim v)]
        (when-not (str/blank? trimmed)
          trimmed)))))

(defn- ensure-prompt-cache-key!
  "Mutates payload in-place, adding prompt_cache_key when missing.

   Proxx uses prompt_cache_key as the affinity key in routing + analytics." 
  [request payload]
  (let [session-key (request-session-key request)
        existing (or (aget payload "prompt_cache_key") (aget payload "promptCacheKey"))]
    (when (and session-key (or (nil? existing) (str/blank? (str existing))))
      (aset payload "prompt_cache_key" session-key)))
  payload)

(defn- model-policy-allowed-ids
  [ctx]
  (let [constraints (ctx-tool-constraints ctx "agent.chat")
        raw (or (:allowedModels constraints)
                (:allowed-models constraints)
                (:models constraints))]
    (->> (cond
           (sequential? raw) raw
           (array? raw) (array-seq raw)
           :else [])
         (keep (fn [value]
                 (when (string? value)
                   (let [trimmed (str/trim value)]
                     (when-not (str/blank? trimmed)
                       trimmed)))))
         set)))

(defn- filter-model-items-for-ctx
  [ctx items config]
  (let [allowed-by-policy (model-policy-allowed-ids ctx)]
    (filter (fn [item]
              (let [model-id (aget item "id")]
                (and (allowlisted-model-id? config model-id)
                     (or (empty? allowed-by-policy)
                         (contains? allowed-by-policy model-id)))))
            items)))

(defn- proxx-health-ctx
  [config request reply]
  {:config config
   :request request
   :reply reply
   :configured (proxx-configured? config)
   :default-model (:llmModel @settings-state*)})

(defn- send-proxx-health-unconfigured!
  [{:keys [config reply default-model]}]
  (json-response! reply 200 {:reachable false
                             :configured false
                             :base_url (:proxx-base-url config)
                             :status_code 503
                             :default_model default-model})
  reply)

(defn- fetch-proxx-health
  [{:keys [config]}]
  (fetch-json (str (:proxx-base-url config) "/health")
              #js {:headers (bearer-headers (:proxx-auth-token config))}))

(defn- send-proxx-health-success!
  [{:keys [config reply default-model]} resp]
  (let [body (aget resp "body")
        key-pool (aget body "keyPool")]
    (json-response! reply 200 {:reachable (boolean (aget resp "ok"))
                               :configured true
                               :base_url (:proxx-base-url config)
                               :status_code (aget resp "status")
                               :model_count (cond
                                              (number? (aget body "modelCount"))
                                              (aget body "modelCount")

                                              (number? (aget key-pool "totalKeys"))
                                              (aget key-pool "totalKeys")

                                              :else nil)
                               :default_model default-model})
    reply))

(defn- send-proxx-health-failure!
  [{:keys [config reply default-model]}]
  (json-response! reply 200 {:reachable false
                             :configured true
                             :base_url (:proxx-base-url config)
                             :status_code 502
                             :default_model default-model})
  reply)

(defn- send-proxx-health!
  [ctx]
  (if-not (:configured ctx)
    (send-proxx-health-unconfigured! ctx)
    (js-await [resp (fetch-proxx-health ctx)]
      (send-proxx-health-success! ctx resp)
      (catch _err
        (send-proxx-health-failure! ctx)))))

(defn proxx-models-ctx
  [config request reply auth-ctx]
  {:config config
   :request request
   :reply reply
   :auth auth-ctx
   :fetch-json! fetch-json})

(defn- fetch-proxx-models
  [{:keys [config fetch-json!]}]
  (fetch-json! (str (:proxx-base-url config) "/v1/models")
               #js {:headers (bearer-headers (:proxx-auth-token config))}))

(defn- send-proxx-models-success!
  [{:keys [auth config reply]} resp]
  (if (aget resp "ok")
    (let [items (js-array-seq (or (aget (aget resp "body") "data") #js []))
          filtered (into-array (filter-model-items-for-ctx auth items config))]
      (json-response! reply 200 {:models filtered}))
    (json-response! reply 502 {:error "Proxx model list failed"
                               :details (js->clj (aget resp "body") :keywordize-keys true)}))
  reply)

(defn- send-proxx-models-failure!
  [{:keys [reply]} err]
  (json-response! reply 502 {:error (str err)})
  reply)

(defn send-proxx-models!
  [ctx]
  (js-await [resp (fetch-proxx-models ctx)]
    (send-proxx-models-success! ctx resp)
    (catch err
      (send-proxx-models-failure! ctx err))))

(defn register-model-routes!
  [app runtime config]
  (route! app "GET" "/api/proxx/health"
          (fn [request reply]
            (send-proxx-health! (proxx-health-ctx config request reply))))

  ;; ============================================================
  ;; Proxx observability (analytics + request logs)
  ;;
  ;; Knoxx backend proxies Proxx's canonical /api/v1/* observability
  ;; endpoints so the browser never needs the Proxx auth token.
  ;; ============================================================

  (route! app "GET" "/api/proxx/observability/request-logs"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (ensure-permission! ctx "org.proxx.observability.read")
                  (if-not (proxx-configured? config)
                    (json-response! reply 503 {:error "Proxx is not configured"})
                    (-> (fetch-json (str (:proxx-base-url config) "/api/v1/request-logs" (request-query-string request))
                                    #js {:headers (bearer-headers (:proxx-auth-token config))})
                        (.then (fn [resp]
                                 (if (aget resp "ok")
                                   (json-response! reply 200 (js->clj (aget resp "body") :keywordize-keys true))
                                   (json-response! reply 502 {:error "Proxx request logs failed"
                                                              :details (js->clj (aget resp "body") :keywordize-keys true)}))))
                        (.catch (fn [err]
                                  (json-response! reply 502 {:error (str err)})))))
                  (catch :default err
                    (error-response! reply err)))))))

  (route! app "GET" "/api/proxx/observability/dashboard/overview"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (ensure-permission! ctx "org.proxx.observability.read")
                  (if-not (proxx-configured? config)
                    (json-response! reply 503 {:error "Proxx is not configured"})
                    (-> (fetch-json (str (:proxx-base-url config) "/api/v1/dashboard/overview" (request-query-string request))
                                    #js {:headers (bearer-headers (:proxx-auth-token config))})
                        (.then (fn [resp]
                                 (if (aget resp "ok")
                                   (json-response! reply 200 (js->clj (aget resp "body") :keywordize-keys true))
                                   (json-response! reply 502 {:error "Proxx dashboard overview failed"
                                                              :details (js->clj (aget resp "body") :keywordize-keys true)}))))
                        (.catch (fn [err]
                                  (json-response! reply 502 {:error (str err)})))))
                  (catch :default err
                    (error-response! reply err)))))))

  (route! app "GET" "/api/proxx/observability/analytics/provider-model"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (ensure-permission! ctx "org.proxx.observability.read")
                  (if-not (proxx-configured? config)
                    (json-response! reply 503 {:error "Proxx is not configured"})
                    (-> (fetch-json (str (:proxx-base-url config) "/api/v1/analytics/provider-model" (request-query-string request))
                                    #js {:headers (bearer-headers (:proxx-auth-token config))})
                        (.then (fn [resp]
                                 (if (aget resp "ok")
                                   (json-response! reply 200 (js->clj (aget resp "body") :keywordize-keys true))
                                   (json-response! reply 502 {:error "Proxx provider-model analytics failed"
                                                              :details (js->clj (aget resp "body") :keywordize-keys true)}))))
                        (.catch (fn [err]
                                  (json-response! reply 502 {:error (str err)})))))
                  (catch :default err
                    (error-response! reply err)))))))

  (route! app "GET" "/api/proxx/models"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.chat.use"))
                (send-proxx-models! (proxx-models-ctx config request reply ctx))))))

  (route! app "POST" "/api/proxx/chat"
          (fn [request reply]
            (let [body (or (aget request "body") #js {})
                  payload (ensure-prompt-cache-key!
                           request
                           #js {:model (or (aget body "model") (:llmModel @settings-state*))
                                :messages (or (aget body "messages") #js [])
                                :temperature (aget body "temperature")
                                :top_p (aget body "top_p")
                                :max_tokens (aget body "max_tokens")
                                :stop (aget body "stop")
                                :stream false})]
              (-> (fetch-json (str (:proxx-base-url config) "/v1/chat/completions")
                              #js {:method "POST"
                                   :headers (bearer-headers (:proxx-auth-token config))
                                   :body (.stringify js/JSON payload)})
                  (.then (fn [resp]
                           (if (aget resp "ok")
                             (let [data (aget resp "body")
                                   choices (or (aget data "choices") #js [])
                                   first-choice (aget choices 0)
                                   message (or (aget first-choice "message") #js {})
                                   content (or (aget message "content")
                                               (aget first-choice "text")
                                               "")]
                               (json-response! reply 200 {:answer content
                                                          :model (or (aget data "model") (aget payload "model"))
                                                          :rag_context nil}))
                             (json-response! reply 502 {:error "Proxx chat failed"
                                                        :details (js->clj (aget resp "body") :keywordize-keys true)}))))
                  (.catch (fn [err]
                            (json-response! reply 502 {:error (str err)})))))))

  (route! app "GET" "/api/models"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.chat.use"))
                (-> (fetch-json (str (:proxx-base-url config) "/v1/models")
                                #js {:headers (bearer-headers (:proxx-auth-token config))})
                    (.then (fn [resp]
                             (if (aget resp "ok")
                               (let [items (js-array-seq (or (aget (aget resp "body") "data") #js []))
                                     models (mapv (fn [item]
                                                    {:id (str (or (aget item "id") ""))
                                                     :name (str (or (aget item "id") ""))
                                                     :path ""
                                                     :size_bytes 0
                                                     :modified_at (now-iso)
                                                     :hash16mb ""
                                                     :suggested_ctx 128000})
                                                  (filter-model-items-for-ctx ctx items config))]
                                 (json-response! reply 200 {:models models}))
                               (json-response! reply 502 {:detail "Model list failed"}))))
                    (.catch (fn [err]
                              (json-response! reply 502 {:detail (str err)}))))))))

  (route! app "GET" "/api/runs"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (let [limit-raw (aget request "query" "limit")
                      limit (if (string? limit-raw)
                              (js/parseInt limit-raw 10)
                              100)
                      items (->> @run-order*
                                 (map #(get @runs* %))
                                 (filter some?)
                                 (filter #(run-visible? ctx %))
                                 (take (max 1 (or limit 100)))
                                 (map summarize-run)
                                 vec)]
                  (json-response! reply 200 {:runs items}))))))

  (route! app "GET" "/api/runs/:runId"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (let [run-id (aget request "params" "runId")
                      run (get @runs* run-id)]
                  (cond
                    (nil? run) (json-response! reply 404 {:detail "Run not found"})
                    (not (run-visible? ctx run)) (error-response! reply (http-error 403 "run_scope_denied" "Run is outside the current Knoxx scope"))
                    :else (json-response! reply 200 run)))))))

  (route! app "GET" "/v1/models"
          (fn [request reply]
            (when (require-openai-key! config request reply)
              (-> (fetch-json (str (:proxx-base-url config) "/v1/models")
                              #js {:headers (bearer-headers (:proxx-auth-token config))})
                  (.then (fn [resp]
                           (if (aget resp "ok")
                             (json-response! reply 200 (js->clj (aget resp "body") :keywordize-keys true))
                             (openai-auth-error reply 502 "Upstream model list failed" "upstream_error"))))
                  (.catch (fn [err]
                            (openai-auth-error reply 502 (str "Upstream model list failed: " err) "upstream_error")))))))

  (route! app "POST" "/v1/chat/completions"
          (fn [request reply]
            (when (require-openai-key! config request reply)
              (let [payload (ensure-prompt-cache-key! request (or (aget request "body") #js {}))]
                (-> (js/fetch (str (:proxx-base-url config) "/v1/chat/completions")
                              #js {:method "POST"
                                   :headers (bearer-headers (:proxx-auth-token config))
                                   :body (.stringify js/JSON payload)})
                    (.then (fn [resp]
                             (send-fetch-response! reply resp)))
                    (.catch (fn [err]
                              (openai-auth-error reply 502 (str "Upstream chat request failed: " err) "upstream_error"))))))))

  (route! app "POST" "/v1/embeddings"
          (fn [request reply]
            (when (require-openai-key! config request reply)
              (let [body (or (aget request "body") #js {})
                    payload (ensure-prompt-cache-key!
                             request
                             (doto (.assign js/Object #js {} body)
                               (aset "model" (or (aget body "model") (:embedModel @settings-state*) (:proxx-embed-model config)))))]
                (-> (js/fetch (str (:proxx-base-url config) "/v1/embeddings")
                              #js {:method "POST"
                                   :headers (bearer-headers (:proxx-auth-token config))
                                   :body (.stringify js/JSON payload)})
                    (.then (fn [resp]
                             (send-fetch-response! reply resp)))
                    (.catch (fn [err]
                              (openai-auth-error reply 502 (str "Embedding generation failed: " err) "upstream_error"))))))))

  nil)

(ns knoxx.backend.infra.routes.models
  (:require [clojure.string :as str]
            [knoxx.backend.infra.agent.hydration :refer [settings-state*]]
            [knoxx.backend.shape.app-shapes :refer [route!]]
            [knoxx.backend.infra.auth.authz :refer [with-request-context! run-visible? ensure-permission! ctx-tool-constraints]]
            [knoxx.backend.infra.clients.proxx :as proxx-client]
            [knoxx.backend.infra.http :refer [json-response! require-openai-key! openai-auth-error send-fetch-response! error-response! http-error request-body request-query-string]]
            [knoxx.backend.domain.action.run-state :refer [runs* run-order* summarize-run]]
            [knoxx.backend.domain.models :refer [allowlisted-model-id?]]
            [knoxx.backend.domain.time :refer [now-iso]]))

(defn- proxx-configured?
  [config]
  (proxx-client/configured? config))

(defn- request-session-key
  "Best-effort extraction of a stable Knoxx session key from request headers.

   Knoxx frontend always sends x-knoxx-session-id. We map that to Proxx's
   prompt_cache_key for provider+account session affinity." 
  [request]
  (let [headers (or (aget request "headers") (js/Object.))
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
              (let [model-id (:id item)]
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
   :default-model (:llmModel @settings-state*)
   :proxx-client (proxx-client/client config)})

(defn- send-proxx-health-unconfigured!
  [{:keys [config reply default-model]}]
  (json-response! reply 200 {:reachable false
                             :configured false
                             :base_url (:proxx-base-url config)
                             :status_code 503
                             :default_model default-model})
  reply)

(defn- fetch-proxx-health
  [{:keys [proxx-client]}]
  (proxx-client/health! proxx-client))

(defn- send-proxx-health-success!
  [{:keys [config reply default-model]} resp]
  (let [body (:body resp)
        key-pool (:keyPool body)]
    (json-response! reply 200 {:reachable (boolean (:ok resp))
                               :configured true
                               :base_url (:proxx-base-url config)
                               :status_code (:status resp)
                               :model_count (cond
                                              (number? (:modelCount body))
                                              (:modelCount body)

                                              (number? (:totalKeys key-pool))
                                              (:totalKeys key-pool)

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
    (-> (fetch-proxx-health ctx)
        (.then (fn [resp]
                 (send-proxx-health-success! ctx resp)))
        (.catch (fn [_err]
                  (send-proxx-health-failure! ctx))))))

(defn proxx-models-ctx
  [config request reply auth-ctx]
  {:config config
   :request request
   :reply reply
   :auth auth-ctx
   :proxx-client (proxx-client/client config)})

(defn- fetch-proxx-models
  [{:keys [proxx-client]}]
  (proxx-client/models! proxx-client))

(defn- send-proxx-models-success!
  [{:keys [auth config reply]} resp]
  (if (:ok resp)
    (let [items (or (get-in resp [:body :data]) [])
          filtered (vec (filter-model-items-for-ctx auth items config))]
      (json-response! reply 200 {:models filtered}))
    (json-response! reply 502 {:error "Proxx model list failed"
                               :details (:body resp)}))
  reply)

(defn- send-proxx-models-failure!
  [{:keys [reply]} err]
  (json-response! reply 502 {:error (str err)})
  reply)

(defn send-proxx-models!
  [ctx]
  (-> (fetch-proxx-models ctx)
      (.then (fn [resp]
               (send-proxx-models-success! ctx resp)))
      (.catch (fn [err]
                (send-proxx-models-failure! ctx err)))))

(defn- register-proxx-health-route!
  [app config]
  (route! app "GET" "/api/proxx/health"
          (fn [request reply]
            (send-proxx-health! (proxx-health-ctx config request reply)))))

(defn- register-proxx-observability-routes!
  [app runtime config]
  (doseq [[path handler error-label]
          [["/api/proxx/observability/request-logs" proxx-client/request-logs! "Proxx request logs failed"]
           ["/api/proxx/observability/dashboard/overview" proxx-client/dashboard-overview! "Proxx dashboard overview failed"]
           ["/api/proxx/observability/analytics/provider-model" proxx-client/provider-model-analytics! "Proxx provider-model analytics failed"]]]
    (route! app "GET" path
            (fn [request reply]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (try
                    (ensure-permission! ctx "org.proxx.observability.read")
                    (if-not (proxx-configured? config)
                      (json-response! reply 503 {:error "Proxx is not configured"})
                      (-> (handler (proxx-client/client config) (request-query-string request))
                          (.then (fn [resp]
                                   (if (:ok resp)
                                     (json-response! reply 200 (:body resp))
                                     (json-response! reply 502 {:error error-label
                                                                :details (:body resp)}))))
                          (.catch (fn [err]
                                    (json-response! reply 502 {:error (str err)})))))
                    (catch :default err
                      (error-response! reply err)))))))))

(defn- register-proxx-model-and-chat-routes!
  [app runtime config]
  (route! app "GET" "/api/proxx/models"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.chat.use"))
                (send-proxx-models! (proxx-models-ctx config request reply ctx))))))
  (route! app "POST" "/api/proxx/chat"
          (fn [request reply]
            (let [body (request-body request)
                  session-key (request-session-key request)
                  payload (cond-> {:model (or (aget body "model") (:llmModel @settings-state*))
                                   :messages (or (aget body "messages") [])
                                   :temperature (aget body "temperature")
                                   :top_p (aget body "top_p")
                                   :max_tokens (aget body "max_tokens")
                                   :stop (aget body "stop")
                                   :stream false}
                            session-key (assoc :prompt_cache_key session-key))]
              (-> (proxx-client/chat-completions! (proxx-client/client config) payload)
                  (.then (fn [resp]
                           (if (:ok resp)
                             (let [data (:body resp)
                                   first-choice (first (or (:choices data) []))
                                   message (or (:message first-choice) {})]
                               (json-response! reply 200 {:answer (or (:content message) (:text first-choice) "")
                                                          :model (or (:model data) (:model payload))
                                                          :rag_context nil}))
                             (json-response! reply 502 {:error "Proxx chat failed"
                                                        :details (:body resp)}))))
                  (.catch (fn [err]
                            (json-response! reply 502 {:error (str err)}))))))))

(defn- register-local-models-route!
  [app runtime config]
  (route! app "GET" "/api/models"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.chat.use"))
                (-> (proxx-client/models! (proxx-client/client config))
                    (.then (fn [resp]
                             (if (:ok resp)
                               (let [items (or (get-in resp [:body :data]) [])]
                                 (json-response! reply 200
                                                 {:models (mapv (fn [item]
                                                                  {:id (str (or (:id item) ""))
                                                                   :name (str (or (:id item) ""))
                                                                   :path ""
                                                                   :size_bytes 0
                                                                   :modified_at (now-iso)
                                                                   :hash16mb ""
                                                                   :suggested_ctx 128000})
                                                                (filter-model-items-for-ctx ctx items config))}))
                               (json-response! reply 502 {:detail "Model list failed"}))))
                    (.catch (fn [err]
                              (json-response! reply 502 {:detail (str err)})))))))))

(defn- register-run-routes!
  [app runtime]
  (route! app "GET" "/api/runs"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (let [limit-raw (aget request "query" "limit")
                      limit (if (string? limit-raw) (js/parseInt limit-raw 10) 100)
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
                    :else (json-response! reply 200 run))))))))

(defn- register-openai-compatible-routes!
  [app config]
  (route! app "GET" "/v1/models"
          (fn [request reply]
            (when (require-openai-key! config request reply)
              (-> (proxx-client/models! (proxx-client/client config))
                  (.then (fn [resp]
                           (if (:ok resp)
                             (json-response! reply 200 (:body resp))
                             (openai-auth-error reply 502 "Upstream model list failed" "upstream_error"))))
                  (.catch (fn [err]
                            (openai-auth-error reply 502 (str "Upstream model list failed: " err) "upstream_error")))))))
  (route! app "POST" "/v1/chat/completions"
          (fn [request reply]
            (when (require-openai-key! config request reply)
              (let [payload (ensure-prompt-cache-key! request (request-body request))]
                (-> (proxx-client/chat-completions-response! (proxx-client/client config) payload)
                    (.then (fn [resp]
                             (send-fetch-response! reply resp)))
                    (.catch (fn [err]
                              (openai-auth-error reply 502 (str "Upstream chat request failed: " err) "upstream_error"))))))))
  (route! app "POST" "/v1/embeddings"
          (fn [request reply]
            (when (require-openai-key! config request reply)
              (let [body (request-body request)
                    payload (ensure-prompt-cache-key!
                             request
                             (doto (.assign js/Object (js/Object.) body)
                               (aset "model" (or (aget body "model") (:embedModel @settings-state*) (:proxx-embed-model config)))))]
                (-> (proxx-client/embeddings-response! (proxx-client/client config) payload)
                    (.then (fn [resp]
                             (send-fetch-response! reply resp)))
                    (.catch (fn [err]
                              (openai-auth-error reply 502 (str "Embedding generation failed: " err) "upstream_error")))))))))

(defn register-model-routes!
  [app runtime config]
  (register-proxx-health-route! app config)
  (register-proxx-observability-routes! app runtime config)
  (register-proxx-model-and-chat-routes! app runtime config)
  (register-local-models-route! app runtime config)
  (register-run-routes! app runtime)
  (register-openai-compatible-routes! app config)
  nil)


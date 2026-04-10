(ns knoxx.backend.model-routes
  (:require [clojure.string :as str]
            [knoxx.backend.agent-hydration :refer [settings-state*]]
            [knoxx.backend.app-shapes :refer [route!]]
            [knoxx.backend.authz :refer [with-request-context! run-visible?]]
            [knoxx.backend.http :refer [json-response! fetch-json bearer-headers require-openai-key! openai-auth-error send-fetch-response! error-response! http-error js-array-seq]]
            [knoxx.backend.run-state :refer [runs* run-order* summarize-run]]
            [knoxx.backend.runtime-config :refer [now-iso]]))

(defn register-model-routes!
  [app runtime config]
  (route! app "GET" "/api/proxx/health"
          (fn [_request reply]
            (let [configured (and (not (str/blank? (:proxx-base-url config)))
                                  (not (str/blank? (:proxx-auth-token config))))]
              (if-not configured
                (do (json-response! reply 200 {:reachable false
                                           :configured false
                                           :base_url (:proxx-base-url config)
                                           :status_code 503
                                           :default_model (:llmModel @settings-state*)})
                    js/undefined)
                (.then (fetch-json (str (:proxx-base-url config) "/health")
                                   #js {:headers (bearer-headers (:proxx-auth-token config))})
                       (fn [resp]
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
                                                       :default_model (:llmModel @settings-state*)})
                           js/undefined))
                       (fn [_err]
                         (json-response! reply 200 {:reachable false
                                                    :configured true
                                                    :base_url (:proxx-base-url config)
                                                    :status_code 502
                                                    :default_model (:llmModel @settings-state*)})
                          js/undefined))))))

  (route! app "GET" "/api/proxx/models"
          (fn [_request reply]
            (-> (fetch-json (str (:proxx-base-url config) "/v1/models")
                            #js {:headers (bearer-headers (:proxx-auth-token config))})
                (.then (fn [resp]
                         (if (aget resp "ok")
                           (json-response! reply 200 {:models (or (aget (aget resp "body") "data") #js [])})
                           (json-response! reply 502 {:error "Proxx model list failed"
                                                      :details (js->clj (aget resp "body") :keywordize-keys true)}))))
                (.catch (fn [err]
                          (json-response! reply 502 {:error (str err)}))))))

  (route! app "POST" "/api/proxx/chat"
          (fn [request reply]
            (let [body (or (aget request "body") #js {})
                  payload #js {:model (or (aget body "model") (:llmModel @settings-state*))
                               :messages (or (aget body "messages") #js [])
                               :temperature (aget body "temperature")
                               :top_p (aget body "top_p")
                               :max_tokens (aget body "max_tokens")
                               :stop (aget body "stop")
                               :stream false}]
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
          (fn [_request reply]
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
                                              items)]
                             (json-response! reply 200 {:models models}))
                           (json-response! reply 502 {:detail "Model list failed"}))))
                (.catch (fn [err]
                          (json-response! reply 502 {:detail (str err)}))))))

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
              (let [payload (or (aget request "body") #js {})]
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
                    payload (doto (.assign js/Object #js {} body)
                              (aset "model" (or (aget body "model") (:embedModel @settings-state*) (:proxx-embed-model config))))]
                (-> (js/fetch (str (:proxx-base-url config) "/v1/embeddings")
                              #js {:method "POST"
                                   :headers (bearer-headers (:proxx-auth-token config))
                                   :body (.stringify js/JSON payload)})
                    (.then (fn [resp]
                             (send-fetch-response! reply resp)))
                    (.catch (fn [err]
                              (openai-auth-error reply 502 (str "Embedding generation failed: " err) "upstream_error"))))))))

  nil)

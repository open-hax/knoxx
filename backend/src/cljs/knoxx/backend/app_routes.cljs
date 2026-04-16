(ns knoxx.backend.app-routes
  (:require [clojure.string :as str]
            [knoxx.backend.admin-routes :as admin-routes]
            [knoxx.backend.agent-hydration :refer [ensure-settings! settings-state*]]
            [knoxx.backend.agent-runtime :refer [forward-knoxx-request! resolve-workspace-path active-agent-session queue-agent-control!]]
            [knoxx.backend.agent-turns :refer [send-agent-turn! ensure-conversation-access!]]
            [knoxx.backend.app-shapes :refer [normalize-chat-body normalize-control-body route!]]
            [knoxx.backend.authz :refer [policy-db policy-db-enabled? policy-db-promise with-request-context! ensure-permission! ensure-any-permission! ensure-org-scope! primary-context-role ctx-permitted? system-admin? ctx-user-id ctx-user-email ctx-org-id run-visible?]]
            [knoxx.backend.core-memory :refer [fetch-openplanner-session-rows! session-visible? filter-authorized-memory-hits! authorized-session-ids!]]
            [knoxx.backend.document-routes :as document-routes]
            [knoxx.backend.http :refer [json-response! rewrite-localhost-url with-query-param bearer-headers require-openai-key! fetch-json openplanner-enabled? openplanner-request! openplanner-url openplanner-headers openai-auth-error send-fetch-response! request-query-string http-error error-response! js-array-seq]]
            [knoxx.backend.memory-routes :as memory-routes]
            [knoxx.backend.model-routes :as model-routes]
            [knoxx.backend.multimodal-routes :as multimodal-routes]
            [knoxx.backend.openplanner-memory :refer [openplanner-memory-search! openplanner-graph-export!]]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.realtime :refer [broadcast-ws!]]
            [knoxx.backend.run-state :as run-state :refer [runs* run-order* summarize-run]]
            [knoxx.backend.runtime-config :refer [now-iso parse-positive-int truthy-param?]]
            [knoxx.backend.session-store :as session-store]
            [knoxx.backend.session-titles :refer [start-session-title-backfill! session-title-backfill* session-titles* get-cached-session-title! session-title-seed-text heuristic-session-title stored-session-title-entry cache-session-title-entry! resolve-session-title! cache-session-title! normalize-session-title]]
            [knoxx.backend.text :refer [count-occurrences replace-first clip-text]]
            [knoxx.backend.tool-routes :as tool-routes]
            [knoxx.backend.tooling :refer [tool-catalog ensure-role-can-use! email-enabled?]]
            [knoxx.backend.translation-routes :as translation-routes]))

(defn- requested-role
  [parsed]
  (or (get-in parsed [:agent-spec :role])
      (some-> (:auth-context parsed) :role str str/trim not-empty)
      (some->> (get-in parsed [:auth-context :roleSlugs]) seq first str str/trim not-empty)))

(defn- allow-policy?
  [policy]
  (= "allow" (some-> (:effect policy) str str/lower-case)))

(defn- requested-tool-policies
  [parsed]
  (let [from-spec (vec (or (get-in parsed [:agent-spec :tool-policies]) []))
        from-auth (vec (or (get-in parsed [:auth-context :toolPolicies]) []))]
    (cond
      (seq from-spec) from-spec
      (seq from-auth) from-auth
      :else [])))

(defn- effective-tool-policies
  [ctx parsed]
  (let [requested (requested-tool-policies parsed)]
    (cond
      (and (nil? ctx) (seq requested)) requested
      (and (nil? ctx) (:auth-context parsed)) (vec (or (get-in parsed [:auth-context :toolPolicies]) []))
      (empty? requested) (vec (or (:toolPolicies ctx) []))
      (system-admin? ctx) requested
      :else (let [allowed (->> (:toolPolicies ctx)
                               (filter allow-policy?)
                               (map :toolId)
                               set)]
              (->> requested
                   (filter #(contains? allowed (:toolId %)))
                   vec)))))

(defn- effective-auth-context
  [ctx parsed]
  (let [base (or ctx (:auth-context parsed))
        requested-role-slug (requested-role parsed)
        role-slugs (cond
                     (and (nil? base) requested-role-slug) [requested-role-slug]
                     (and requested-role-slug (or (system-admin? ctx)
                                                 (contains? (into #{} (or (:roleSlugs base) [])) requested-role-slug)))
                     [requested-role-slug]
                     :else (vec (or (:roleSlugs base) [])))
        tool-policies (effective-tool-policies ctx parsed)
        resource-policies (or (get-in parsed [:agent-spec :resource-policies])
                              (get-in parsed [:auth-context :resourcePolicies])
                              (:resourcePolicies base))]
    (when (or base requested-role-slug (seq tool-policies) resource-policies)
      (cond-> (or base {})
        (seq role-slugs) (assoc :roleSlugs role-slugs)
        (or (seq tool-policies) (some? base)) (assoc :toolPolicies tool-policies)
        resource-policies (assoc :resourcePolicies resource-policies)))))

(defn- active-run-summary
  [run session]
  {:run_id (:run_id run)
   :session_id (:session_id run)
   :conversation_id (:conversation_id run)
   :status (:status run)
   :model (:model run)
   :created_at (:created_at run)
   :updated_at (:updated_at run)
   :ttft_ms (:ttft_ms run)
   :total_time_ms (:total_time_ms run)
   :input_tokens (:input_tokens run)
   :output_tokens (:output_tokens run)
   :tokens_per_s (:tokens_per_s run)
   :error (:error run)
   :event_count (count (or (:events run) []))
   :tool_receipt_count (count (or (:tool_receipts run) []))
   :has_active_stream (boolean (:has_active_stream session))
   :agent_spec (get-in run [:settings :agentSpec])
   :resource_policies (get-in run [:resources :agentResourcePolicies])
   :latest_user_message (some->> (:request_messages run)
                                 reverse
                                 (some (fn [message]
                                         (when (= "user" (some-> (:role message) str str/lower-case))
                                           (:content message)))))
   :latest_event (some-> (:events run) last (select-keys [:type :status :tool_name :preview :at]))})

(defn register-routes!
  [runtime app config lounge-messages*]
  (ensure-settings! config)

  (route! app "GET" "/health"
          (fn [_request reply]
            (let [proxx-configured (and (not (str/blank? (:proxx-base-url config)))
                                        (not (str/blank? (:proxx-auth-token config))))
                  openplanner-configured (openplanner-enabled? config)
                  proxx-promise (if proxx-configured
                                 (fetch-json (str (:proxx-base-url config) "/health")
                                             #js {:headers (bearer-headers (:proxx-auth-token config))})
                                 (js/Promise.resolve #js {:ok false
                                                         :status 503
                                                         :body #js {:detail "Proxx is not configured"}}))
                  openplanner-promise (if openplanner-configured
                                       (fetch-json (openplanner-url config "/v1/health")
                                                   #js {:headers (openplanner-headers config)})
                                       (js/Promise.resolve #js {:ok false
                                                               :status 503
                                                               :body #js {:detail "OpenPlanner is not configured"}}))]
              (-> (js/Promise.all #js [proxx-promise openplanner-promise])
                  (.then (fn [parts]
                           (let [proxx-res (aget parts 0)
                                 openplanner-res (aget parts 1)
                                 proxx-ok (and proxx-configured (aget proxx-res "ok"))
                                 openplanner-ok (and openplanner-configured (aget openplanner-res "ok"))
                                 healthy (and proxx-ok openplanner-ok)]
                             (json-response!
                              reply
                              (if healthy 200 503)
                              {:status (if healthy "ok" "unhealthy")
                               :service "knoxx-backend-cljs"
                               :dependencies {:proxx {:configured proxx-configured
                                                      :reachable (boolean proxx-ok)
                                                      :status_code (aget proxx-res "status")
                                                      :detail (js->clj (aget proxx-res "body") :keywordize-keys true)}
                                              :openplanner {:configured openplanner-configured
                                                            :reachable (boolean openplanner-ok)
                                                            :status_code (aget openplanner-res "status")
                                                            :detail (js->clj (aget openplanner-res "body") :keywordize-keys true)}}}))))
                  (.catch (fn [err]
                            (json-response! reply 503 {:status "unhealthy"
                                                       :service "knoxx-backend-cljs"
                                                       :error (str err)})))))))

  (route! app "GET" "/api/config"
          (fn [request reply]
            (json-response!
             reply
             200
             {:knoxx_admin_url (rewrite-localhost-url (:knoxx-admin-url config) request)
              :knoxx_base_url (rewrite-localhost-url (:knoxx-base-url config) request)
              :knoxx_enabled true
              :proxx_enabled (and (not (str/blank? (:proxx-base-url config)))
                                  (not (str/blank? (:proxx-auth-token config))))
              :proxx_default_model (:llmModel @settings-state*)
              :shibboleth_ui_url (if (str/blank? (:shibboleth-ui-url config))
                                   ""
                                   (rewrite-localhost-url (:shibboleth-ui-url config) request))
             :shibboleth_enabled (and (not (str/blank? (:shibboleth-base-url config)))
                                      (not (str/blank? (:shibboleth-ui-url config))))
              :default_role (:knoxx-default-role config)
              :email_enabled (email-enabled? config)
              :rbac_enabled (policy-db-enabled? runtime)})))

  (route! app "GET" "/api/auth/context"
          (fn [request reply]
            (if-not (policy-db-enabled? runtime)
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"})
              (with-request-context! runtime request reply
                (fn [ctx]
                  (json-response! reply 200 {:user (:user ctx)
                                             :org (:org ctx)
                                             :membership (:membership ctx)
                                             :roles (vec (or (:roles ctx) []))
                                             :roleSlugs (vec (or (:roleSlugs ctx) []))
                                             :permissions (vec (or (:permissions ctx) []))
                                             :toolPolicies (vec (or (:toolPolicies ctx) []))
                                             :membershipToolPolicies (vec (or (:membershipToolPolicies ctx) []))
                                             :isSystemAdmin (boolean (:isSystemAdmin ctx))
                                             :primaryRole (primary-context-role ctx)}))))))

  (admin-routes/register-admin-routes! app runtime
                                       {:route! route!
                                        :json-response! json-response!
                                        :with-request-context! with-request-context!
                                        :ensure-permission! ensure-permission!
                                        :ensure-any-permission! ensure-any-permission!
                                        :ensure-org-scope! ensure-org-scope!
                                        :policy-db policy-db
                                        :policy-db-promise policy-db-promise
                                        :http-error http-error})

  (memory-routes/register-memory-routes! app runtime config
                                         {:route! route!
                                          :json-response! json-response!
                                          :error-response! error-response!
                                          :with-request-context! with-request-context!
                                          :ensure-permission! ensure-permission!
                                          :parse-positive-int parse-positive-int
                                          :truthy-param? truthy-param?
                                          :start-session-title-backfill! start-session-title-backfill!
                                          :session-title-backfill* session-title-backfill*
                                          :session-titles* session-titles*
                                          :get-cached-session-title! get-cached-session-title!
                                          :openplanner-enabled? openplanner-enabled?
                                          :openplanner-request! openplanner-request!
                                          :fetch-openplanner-session-rows! fetch-openplanner-session-rows!
                                          :session-title-seed-text session-title-seed-text
                                          :heuristic-session-title heuristic-session-title
                                          :stored-session-title-entry stored-session-title-entry
                                          :cache-session-title-entry! cache-session-title-entry!
                                          :resolve-session-title! resolve-session-title!
                                          :cache-session-title! cache-session-title!
                                          :normalize-session-title normalize-session-title
                                          :session-visible? session-visible?
                                          :openplanner-memory-search! openplanner-memory-search!
                                          :filter-authorized-memory-hits! filter-authorized-memory-hits!
                                          :ctx-permitted? ctx-permitted?
                                          :system-admin? system-admin?
                                          :http-error http-error
                                          :now-iso now-iso
                                          :broadcast-ws! broadcast-ws!
                                          :lounge-messages* lounge-messages*
                                          :authorized-session-ids! authorized-session-ids!})

  (tool-routes/register-tool-routes! app runtime config
                                     {:route! route!
                                      :json-response! json-response!
                                      :error-response! error-response!
                                      :with-request-context! with-request-context!
                                      :ensure-permission! ensure-permission!
                                      :tool-catalog tool-catalog
                                      :ensure-role-can-use! ensure-role-can-use!
                                      :resolve-workspace-path resolve-workspace-path
                                      :count-occurrences count-occurrences
                                      :replace-first replace-first
                                      :clip-text clip-text})

  (model-routes/register-model-routes! app runtime config)

  (document-routes/register-document-routes! app runtime config
                                               {:route! route!
                                                :json-response! json-response!
                                                :error-response! error-response!
                                                :with-request-context! with-request-context!
                                                :ensure-permission! ensure-permission!
                                                :clip-text clip-text
                                                :openplanner-graph-export! openplanner-graph-export!
                                                :send-fetch-response! send-fetch-response!
                                                :bearer-headers bearer-headers
                                                :fetch-json fetch-json
                                                :openai-auth-error openai-auth-error
                                                :request-query-string request-query-string})

  (route! app "GET" "/api/knoxx/proxy/*"
          (fn [request reply]
            (let [path (aget request "params" "*")
                  target-url (str (:knoxx-base-url config) "/api/" path (request-query-string request))]
              (-> (forward-knoxx-request! config request "GET" path nil)
                  (.then (fn [resp]
                           (send-fetch-response! reply resp)))
                  (.catch (fn [err]
                            (json-response! reply 502 {:detail (str "Proxy request failed: " err)})))))))

  (route! app "POST" "/api/knoxx/proxy/*"
          (fn [request reply]
            (let [path (aget request "params" "*")]
              (-> (forward-knoxx-request! config request "POST" path nil)
                  (.then (fn [resp]
                           (send-fetch-response! reply resp)))
                  (.catch (fn [err]
                            (json-response! reply 502 {:detail (str "Proxy request failed: " err)})))))))

  (route! app "PUT" "/api/knoxx/proxy/*"
          (fn [request reply]
            (let [path (aget request "params" "*")]
              (-> (forward-knoxx-request! config request "PUT" path nil)
                  (.then (fn [resp]
                           (send-fetch-response! reply resp)))
                  (.catch (fn [err]
                            (json-response! reply 502 {:detail (str "Proxy request failed: " err)})))))))

  (route! app "PATCH" "/api/knoxx/proxy/*"
          (fn [request reply]
            (let [path (aget request "params" "*")]
              (-> (forward-knoxx-request! config request "PATCH" path nil)
                  (.then (fn [resp]
                           (send-fetch-response! reply resp)))
                  (.catch (fn [err]
                            (json-response! reply 502 {:detail (str "Proxy request failed: " err)})))))))

  (route! app "DELETE" "/api/knoxx/proxy/*"
          (fn [request reply]
            (let [path (aget request "params" "*")]
              (-> (forward-knoxx-request! config request "DELETE" path nil)
                  (.then (fn [resp]
                           (send-fetch-response! reply resp)))
                  (.catch (fn [err]
                            (json-response! reply 502 {:detail (str "Proxy request failed: " err)})))))))

  (route! app "GET" "/api/knoxx/health"
          (fn [_request reply]
            (json-response! reply 200 {:reachable true
                                       :configured true
                                       :base_url (:knoxx-base-url config)
                                       :status_code 200
                                       :details {:mode "shadow-cljs-pi-sdk"
                                                 :status "ok"
                                                 :project (:project-name config)
                                                 :collection {:name (:collection-name config)
                                                              :pointsCount nil}}})))

  (route! app "POST" "/api/knoxx/chat"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.chat.use"))
                (let [parsed (normalize-chat-body (or (aget request "body") #js {}))
                      agent-ctx (effective-auth-context ctx parsed)
                      body (assoc parsed
                                  :mode "rag"
                                  :auth-context agent-ctx)]
                  (-> (send-agent-turn! runtime config body)
                      (.then (fn [resp]
                               (json-response! reply 200 resp)))
                      (.catch (fn [err]
                                (error-response! reply err 502)))))))))

  (route! app "POST" "/api/knoxx/chat/start"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.chat.use"))
                (let [node-crypto (aget runtime "crypto")
                      parsed (normalize-chat-body (or (aget request "body") #js {}))
                      agent-ctx (effective-auth-context ctx parsed)
                      session-id (:session-id parsed)
                      conversation-id (or (:conversation-id parsed) (.randomUUID node-crypto))
                      run-id (or (:run-id parsed) (.randomUUID node-crypto))
                      body (assoc parsed :conversation-id conversation-id :run-id run-id :mode "rag" :auth-context agent-ctx)]
                  ;; Guard: check if session can accept a new turn before queueing
                  (if-not session-id
                    ;; No session-id means new session, always allowed
                    (do
                      (-> (send-agent-turn! runtime config body)
                          (.then (fn [_] nil))
                          (.catch (fn [err]
                                    (.error js/console "Async agent chat failed" err))))
                      (json-response! reply 202 {:ok true
                                                 :queued true
                                                 :run_id run-id
                                                 :conversation_id conversation-id
                                                 :session_id (:session-id body)
                                                 :model (or (:model body)
                                                            (get-in body [:agent-spec :model])
                                                            (:llmModel @settings-state*))}))
                    ;; Existing session: check can_send
                    (-> (session-store/get-session (redis/get-client) session-id)
                        (.then (fn [session]
                                 (let [can-send-result (session-store/session-can-send? session)]
                                   (if (:can-send can-send-result)
                                     ;; Also check in-memory agent session for live streaming
                                     (let [agent-session (active-agent-session conversation-id)
                                           actively-streaming? (and agent-session
                                                                    (true? (aget agent-session "isStreaming")))]
                                       (if actively-streaming?
                                         (json-response! reply 409 {:ok false
                                                                    :error "Agent is already processing. Specify streamingBehavior ('steer' or 'followUp') to queue the message."
                                                                    :code "agent_already_processing"
                                                                    :has_active_stream true
                                                                    :can_send false})
                                         (do
                                           (-> (send-agent-turn! runtime config body)
                                               (.then (fn [_] nil))
                                               (.catch (fn [err]
                                                         (.error js/console "Async agent chat failed" err))))
                                           (json-response! reply 202 {:ok true
                                                                      :queued true
                                                                      :run_id run-id
                                                                      :conversation_id conversation-id
                                                                      :session_id (:session-id body)
                                                                      :model (or (:model body)
                                                                 (get-in body [:agent-spec :model])
                                                                 (:llmModel @settings-state*))}))))
                                     (json-response! reply 409 {:ok false
                                                                :error (str "Agent is already processing. " (or (:reason can-send-result) ""))
                                                                :code "agent_already_processing"
                                                                :has_active_stream (boolean (:has_active_stream session))
                                                                :can_send false})))))
                        (.catch (fn [err]
                                  (.error js/console "Session status check failed" err)
                                  ;; On error, allow the send to proceed (fail-open)
                                  (-> (send-agent-turn! runtime config body)
                                      (.then (fn [_] nil))
                                      (.catch (fn [err2]
                                                (.error js/console "Async agent chat failed" err2))))
                                  (json-response! reply 202 {:ok true
                                                             :queued true
                                                             :run_id run-id
                                                             :conversation_id conversation-id
                                                             :session_id (:session-id body)
                                                             :model (or (:model body)
                                                                        (get-in body [:agent-spec :model])
                                                                        (:llmModel @settings-state*))}))))))))))

  (route! app "POST" "/api/knoxx/direct"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.chat.use"))
                (let [parsed (normalize-chat-body (or (aget request "body") #js {}))
                      agent-ctx (effective-auth-context ctx parsed)
                      body (assoc parsed
                                  :mode "direct"
                                  :auth-context agent-ctx)]
                  (-> (send-agent-turn! runtime config body)
                      (.then (fn [resp]
                               (json-response! reply 200 resp)))
                      (.catch (fn [err]
                                (error-response! reply err 502)))))))))

  (route! app "POST" "/api/knoxx/direct/start"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.chat.use"))
                (let [node-crypto (aget runtime "crypto")
                      parsed (normalize-chat-body (or (aget request "body") #js {}))
                      agent-ctx (effective-auth-context ctx parsed)
                      session-id (:session-id parsed)
                      conversation-id (or (:conversation-id parsed) (.randomUUID node-crypto))
                      run-id (or (:run-id parsed) (.randomUUID node-crypto))
                      body (assoc parsed :conversation-id conversation-id :run-id run-id :mode "direct" :auth-context agent-ctx)]
                  ;; Guard: check if session can accept a new turn before queueing
                  (if-not session-id
                    (do
                      (-> (send-agent-turn! runtime config body)
                          (.then (fn [_] nil))
                          (.catch (fn [err]
                                    (.error js/console "Async direct agent chat failed" err))))
                      (json-response! reply 202 {:ok true
                                                 :queued true
                                                 :run_id run-id
                                                 :conversation_id conversation-id
                                                 :session_id (:session-id body)
                                                 :model (or (:model body)
                                                            (get-in body [:agent-spec :model])
                                                            (:llmModel @settings-state*))}))
                    (-> (session-store/get-session (redis/get-client) session-id)
                        (.then (fn [session]
                                 (let [can-send-result (session-store/session-can-send? session)]
                                   (if (:can-send can-send-result)
                                     (let [agent-session (active-agent-session conversation-id)
                                           actively-streaming? (and agent-session
                                                                    (true? (aget agent-session "isStreaming")))]
                                       (if actively-streaming?
                                         (json-response! reply 409 {:ok false
                                                                    :error "Agent is already processing. Specify streamingBehavior ('steer' or 'followUp') to queue the message."
                                                                    :code "agent_already_processing"
                                                                    :has_active_stream true
                                                                    :can_send false})
                                         (do
                                           (-> (send-agent-turn! runtime config body)
                                               (.then (fn [_] nil))
                                               (.catch (fn [err]
                                                         (.error js/console "Async direct agent chat failed" err))))
                                           (json-response! reply 202 {:ok true
                                                                      :queued true
                                                                      :run_id run-id
                                                                      :conversation_id conversation-id
                                                                      :session_id (:session-id body)
                                                                      :model (or (:model body)
                                                                 (get-in body [:agent-spec :model])
                                                                 (:llmModel @settings-state*))}))))
                                     (json-response! reply 409 {:ok false
                                                                :error (str "Agent is already processing. " (or (:reason can-send-result) ""))
                                                                :code "agent_already_processing"
                                                                :has_active_stream (boolean (:has_active_stream session))
                                                                :can_send false})))))
                        (.catch (fn [err]
                                  (.error js/console "Session status check failed" err)
                                  (-> (send-agent-turn! runtime config body)
                                      (.then (fn [_] nil))
                                      (.catch (fn [err2]
                                                (.error js/console "Async direct agent chat failed" err2))))
                                  (json-response! reply 202 {:ok true
                                                             :queued true
                                                             :run_id run-id
                                                             :conversation_id conversation-id
                                                             :session_id (:session-id body)
                                                             :model (or (:model body)
                                                                        (get-in body [:agent-spec :model])
                                                                        (:llmModel @settings-state*))}))))))))))

  (route! app "POST" "/api/knoxx/steer"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.controls.steer"))
                (let [body (assoc (normalize-control-body (or (aget request "body") #js {})) :kind "steer")]
                  (ensure-conversation-access! ctx (:conversation-id body))
                  (-> (queue-agent-control! runtime config body)
                      (.then (fn [resp]
                               (json-response! reply 200 resp)))
                      (.catch (fn [err]
                                (error-response! reply err 409)))))))))

  (route! app "POST" "/api/knoxx/follow-up"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.controls.follow_up"))
                (let [body (assoc (normalize-control-body (or (aget request "body") #js {})) :kind "follow_up")]
                  (ensure-conversation-access! ctx (:conversation-id body))
                  (-> (queue-agent-control! runtime config body)
                      (.then (fn [resp]
                               (json-response! reply 200 resp)))
                      (.catch (fn [err]
                                (error-response! reply err 409)))))))))

  (route! app "GET" "/api/knoxx/agents/active"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.chat.use"))
                (let [limit-raw (aget request "query" "limit")
                      limit (if (string? limit-raw)
                              (max 1 (js/parseInt limit-raw 10))
                              25)
                      sessions-by-id (into {}
                                           (map (fn [session]
                                                  [(:session_id session) session]))
                                           (session-store/active-session-snapshots))
                      items (->> @run-order*
                                 (map #(get @runs* %))
                                 (filter some?)
                                 (filter #(contains? #{"queued" "running" "waiting_input"} (:status %)))
                                 (filter #(run-visible? ctx %))
                                 (map (fn [run]
                                        (active-run-summary run (get sessions-by-id (:session_id run)))))
                                 (take limit)
                                 vec)]
                  (json-response! reply 200 {:runs items
                                             :count (count items)})))))

  ;; Session status endpoint for frontend resume detection
  (route! app "GET" "/api/knoxx/session/status"
          (fn [request reply]
            (let [session-id (or (aget request "query" "session_id")
                                 (aget request "query" "sessionId")
                                 "")
                  conversation-id (or (aget request "query" "conversation_id")
                                       (aget request "query" "conversationId")
                                       "")]
              (cond
                (str/blank? session-id)
                (json-response! reply 400 {:error "session_id is required"})

                :else
                (-> (session-store/get-session (redis/get-client) session-id)
                    (.then (fn [session]
                             (if session
                               (let [can-send (session-store/session-can-send? session)]
                                 (json-response! reply 200
                                                   {:session_id session-id
                                                    :conversation_id (:conversation_id session)
                                                    :status (:status session)
                                                    :has_active_stream (:has_active_stream session)
                                                    :can_send (:can-send can-send)
                                                    :reason (:reason can-send)
                                                    :model (:model session)
                                                    :updated_at (:updated_at session)}))
                               ;; No session in Redis - check if conversation has active agent session
                               ;; Only trust in-memory session if it's actually streaming (not stale)
                               (let [agent-session (active-agent-session conversation-id)]
                                 (if (and agent-session
                                          (true? (aget agent-session "isStreaming"))
                                          ;; Extra safeguard: verify session is actually active
                                          ;; by checking if it has a current turn
                                          (try
                                            (let [current-turn (aget agent-session "currentTurn")]
                                              (some? current-turn))
                                            (catch js/Error _ false)))
                                   (json-response! reply 200
                                                     {:session_id session-id
                                                      :conversation_id conversation-id
                                                      :status "running"
                                                      :has_active_stream true
                                                      :can_send false
                                                      :reason "Session is actively streaming"})
                                   (json-response! reply 200
                                                     {:session_id session-id
                                                      :conversation_id conversation-id
                                                      :status "not_found"
                                                      :has_active_stream false
                                                      :can_send true
                                                      :reason "No session state found. Ready for new turn."}))))))
                    (.catch (fn [err]
                              (js/console.error "Session status check failed" err)
                              (json-response! reply 500 {:error (str err)})))))))

  ;; Run event catch-up endpoint for WS reconnect recovery
  (route! app "GET" "/api/knoxx/run/:runId/events"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.chat.use"))
                (let [run-id (aget request "params" "runId")
                      since (or (aget request "query" "since") "")]
                  (if (str/blank? run-id)
                    (json-response! reply 400 {:error "runId is required"})
                    (-> (run-state/get-run-events-since run-id since)
                        (.then (fn [events]
                                 (json-response! reply 200 {:run_id run-id
                                                            :events events
                                                            :count (count events)})))
                        (.catch (fn [err]
                                  (json-response! reply 500 {:error (str err)})))))))))))

  (route! app "POST" "/api/shibboleth/handoff"
          (fn [request reply]
            (let [body (or (aget request "body") #js {})]
              (if (str/blank? (:shibboleth-base-url config))
                (json-response! reply 503 {:detail "SHIBBOLETH_BASE_URL is not configured"})
                (let [payload #js {:source_app "knoxx"
                                   :model (aget body "model")
                                   :system_prompt (aget body "system_prompt")
                                   :provider (aget body "provider")
                                   :conversation_id (aget body "conversation_id")
                                   :fake_tools_enabled (boolean (aget body "fake_tools_enabled"))
                                   :items (or (aget body "items") #js [])}]
                  (-> (fetch-json (str (:shibboleth-base-url config) "/api/chat/import")
                                  #js {:method "POST"
                                       :headers #js {"Content-Type" "application/json"}
                                       :body (.stringify js/JSON payload)})
                      (.then (fn [resp]
                               (if (aget resp "ok")
                                 (let [data (aget resp "body")
                                       session (or (aget data "session") #js {})
                                       session-id (str (or (aget session "id") ""))
                                       ui-url (if (and (not (str/blank? session-id))
                                                       (not (str/blank? (:shibboleth-ui-url config))))
                                                (with-query-param (rewrite-localhost-url (:shibboleth-ui-url config) request)
                                                                  "session"
                                                                  session-id)
                                                "")]
                                   (if (str/blank? session-id)
                                     (json-response! reply 502 {:detail "Shibboleth import did not return a session id"})
                                     (json-response! reply 200 {:ok true
                                                                :session_id session-id
                                                                :ui_url ui-url
                                                                :imported_item_count (count (js-array-seq (aget body "items")))})))
                                 (json-response! reply 502 {:detail (str "Shibboleth import failed: "
                                                                        (or (aget (aget resp "body") "raw")
                                                                            (js/JSON.stringify (aget resp "body"))))}))))
                      (.catch (fn [err]
                                (json-response! reply 502 {:detail (str "Shibboleth is unreachable: " err)})))))))))

  ;; Translation routes
  (translation-routes/register-translation-routes! app runtime config
                                                    {:json-response! json-response!
                                                     :error-response! error-response!
                                                     :with-request-context! with-request-context!
                                                     :ensure-permission! ensure-permission!
                                                     :openplanner-enabled? openplanner-enabled?
                                                     :openplanner-request! openplanner-request!
                                                     :openplanner-url openplanner-url
                                                     :openplanner-headers openplanner-headers
                                                     :ctx-user-id ctx-user-id
                                                     :ctx-user-email ctx-user-email
                                                     :ctx-org-id ctx-org-id})
  ))

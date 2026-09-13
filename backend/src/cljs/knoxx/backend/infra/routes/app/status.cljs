(ns knoxx.backend.infra.routes.app.status
  "Application health, configuration and authority discovery routes."
  (:require-macros [knoxx.backend.macros :as backend-macros])
  (:require [clojure.string :as str]
            [knoxx.backend.domain.time :as domain-time]
            [knoxx.backend.extern.app-health :as app-health]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.infra.agent.hydration :as agent-hydration]
            [knoxx.backend.infra.auth.authz :as auth-authz]
            [knoxx.backend.infra.http :as infra-http]
            [knoxx.backend.infra.routes.app.dependencies :as app-dependencies]
            [knoxx.backend.infra.tooling :as infra-tooling]))

(defn- dev-hmr-response
  []
  {:ok true
   :version "v2"
   :at (domain-time/now-iso)})

(backend-macros/defroute ^{:doc "Register GET /health with the shared route dependencies."} health! []
  "GET" "/health"
  (let [{:keys [proxx-configured openplanner-configured ollama-configured promises]}
        (app-health/dependency-probes config)]
    (try
      (app-health/health-deps-ok reply proxx-configured openplanner-configured ollama-configured
                      (await (promise/all-vec promises)))
      (catch :default err
        (app-health/health-deps-err reply err)))))

(backend-macros/defroute ^{:doc "Register GET /api/dev/hmr with the shared route dependencies."} dev-hmr! []
  "GET" "/api/dev/hmr" []
  (json-response! reply 200 (dev-hmr-response)))

(defn- speech-configuration
  [config request]
  {    :stt_enabled (not (str/blank? (:stt-base-url config)))
    :stt_base_url (if (str/blank? (:stt-base-url config))
                    ""
                    (infra-http/rewrite-localhost-url (:stt-base-url config) request))
    :tts_enabled (not (str/blank? (str/trim (or (:voxx-api-key config) ""))))
    :tts_provider (if (not (str/blank? (str/trim (or (:voxx-api-key config) ""))))
                    "voxx"
                    "")
    :tts_default_voice_id (or (:voxx-voice-id config) "af_jessica")
    :tts_default_model_id (or (:voxx-model-id config) "kokoro")
    :tts_default_speed (or (:voxx-default-speed config) "1.15")
    :tts_default_postprocess_enabled true
    :tts_default_postprocess_profile "sports-commentator-v1"
})

(backend-macros/defroute ^{:doc "Register GET /api/config with the shared route dependencies."} config! []
  "GET" "/api/config"
  (json-response!
   reply
   200
   (merge (speech-configuration config request)
    {:knoxx_admin_url (infra-http/rewrite-localhost-url (:knoxx-admin-url config) request)
    :knoxx_base_url (infra-http/rewrite-localhost-url (:knoxx-base-url config) request)
    :knoxx_enabled true
    :proxx_enabled (and (not (str/blank? (:proxx-base-url config)))
                        (not (str/blank? (:proxx-auth-token config))))
    :ollama_enabled (not (str/blank? (:ollama-base-url config)))
    :ollama_default_model (:ollama-default-model config)
    :proxx_default_model (:llmModel @agent-hydration/settings-state*)
    :shibboleth_ui_url (if (str/blank? (:shibboleth-ui-url config))
                         ""
                         (infra-http/rewrite-localhost-url (:shibboleth-ui-url config) request))
    :shibboleth_enabled (and (not (str/blank? (:shibboleth-base-url config)))
                             (not (str/blank? (:shibboleth-ui-url config))))
    :default_role (:knoxx-default-role config)
    :default_actor_id (infra-tooling/default-actor-id config)
    :default_agent_contract (infra-tooling/default-agent-contract-id config (infra-tooling/default-actor-id config))
    :email_enabled (infra-tooling/email-enabled? config)
    :rbac_enabled (auth-authz/policy-db-enabled? runtime)})))

(backend-macros/defroute ^{:doc "Register GET /api/knoxx/agents/catalog with the shared route dependencies."} api-knoxx-agents-catalog! []
  "GET" "/api/knoxx/agents/catalog"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [actor-id (some-> (or (aget request "query" "actorId")
                             (aget request "query" "actor"))
                         str
                         str/trim
                         not-empty)
        effective-actor-id (or actor-id (infra-tooling/default-actor-id config))
        agents (infra-tooling/agent-contract-catalog config effective-actor-id)
        default-agent-id (infra-tooling/default-agent-contract-id config effective-actor-id)
        default-agent (when default-agent-id
                        (infra-tooling/effective-agent-contract config default-agent-id effective-actor-id))
        catalog (cond-> agents
                  (and default-agent
                       (not (some #(= (:id %) (:id default-agent)) agents)))
                  (conj default-agent))]
    (json-response! reply 200 {:actor_id effective-actor-id
                               :actors (mapv (fn [actor]
                                               {:id (:id actor)
                                                :kind (:kind actor)
                                                :defaultAgent (:default-agent actor)
                                                :roleSlugs (vec (or (:role-slugs actor) []))})
                                             (infra-tooling/actor-catalog config))
                               :agents (vec (sort-by :id catalog))
                               :default_actor_id (infra-tooling/default-actor-id config)
                               :default_agent_contract default-agent-id})))

(backend-macros/defroute ^{:doc "Register GET /api/auth/context with the shared route dependencies."} api-auth-context! []
  "GET" "/api/auth/context"
  (if-not (auth-authz/policy-db-enabled? runtime)
    (json-response! reply 503 {:detail "Knoxx policy database is not configured"})
    (json-response! reply 200 {:user (:user ctx)
                               :actor (:actor ctx)
                               :org (:org ctx)
                               :membership (:membership ctx)
                               :roles (vec (or (:roles ctx) []))
                               :roleSlugs (vec (auth-authz/ctx-role-slugs ctx))
                               :permissions (vec (or (:permissions ctx) []))
                               :toolPolicies (vec (or (:tool-policies ctx) (:toolPolicies ctx) []))
                               :membershipToolPolicies (vec (or (:membership-tool-policies ctx) (:membershipToolPolicies ctx) []))
                               :isSystemAdmin (auth-authz/system-admin? ctx)
                               :primaryRole (auth-authz/primary-context-role ctx)})))

(defn register-core-routes!
  "Compose the core routes with their existing dependencies."
  [app runtime config]
  (health! app runtime config app-dependencies/deps)
  (dev-hmr! app runtime config app-dependencies/deps)
  (config! app runtime config app-dependencies/deps)
  (api-knoxx-agents-catalog! app runtime config app-dependencies/deps)
  (api-auth-context! app runtime config app-dependencies/deps))

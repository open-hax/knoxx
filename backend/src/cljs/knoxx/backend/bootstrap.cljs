(ns knoxx.backend.bootstrap
  "Single bootstrap entrypoint for hosting the Knoxx CLJS backend inside Node/Fastify.

   Contract:
   - The Node host shim (src/server.mjs) should ONLY import dependencies and call
     this function.
   - All HTTP routes, auth flows, and runtime wiring live in CLJS.
   - Dependencies are passed in as a single JS object (deps) and then threaded
     through request/runtime contexts."  
  (:require [clojure.string :as str]
            [knoxx.backend.auth-session :as auth-session]
            [knoxx.backend.core :as core]
            [knoxx.backend.discord-gateway :as discord-gateway]
            [knoxx.backend.mcp-http :as mcp-http]
            [knoxx.backend.pi-session-ingester :as pi-session-ingester]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.models :as runtime-models]
            [knoxx.backend.tools.proxy-routes :as proxy-routes]
            [knoxx.backend.agent-turns :refer [lounge-messages*]]
            [knoxx.backend.policy-db :as policy-db]))

(defn- env
  [k default]
  (or (aget js/process.env k) default))

(defn- truthy?
  [v]
  (contains? #{"1" "true" "yes" "on" "y"} (-> (str (or v "")) str/trim str/lower-case)))

(defn- ensure-fastify-json-empty-body-parser!
  "Allow Content-Type: application/json with empty bodies.

   Fastify's default parser throws FST_ERR_CTP_EMPTY_JSON_BODY, but some
   endpoints are intentionally POST-without-body."  
  [app]
  (.addContentTypeParser app
                         "application/json"
                         #js {:parseAs "string"}
                         (fn [_req body done]
                           (try
                             (done nil (if (= body "") #js {} (js/JSON.parse body)))
                             (catch :default err
                               (done err))))))

(defn- make-runtime
  "Build the runtime dependency bundle CLJS code expects.

   NOTE: deps is a JS object created by the Node host shim."  
  [deps policyDb]
  #js {:Fastify (aget deps "Fastify")
       :fastifyCors (aget deps "fastifyCors")
       :fastifyWebsocket (aget deps "fastifyWebsocket")
       :fastifyMultipart (aget deps "fastifyMultipart")
       :fastifyCookie (aget deps "fastifyCookie")
       :fastifyFormbody (aget deps "fastifyFormbody")
       :Type (aget deps "Type")
       :sdk (aget deps "sdk")
       :crypto (aget deps "crypto")
       :fs (aget deps "fs")
       :path (aget deps "path")
       :os (aget deps "os")
       :execFileAsync (aget deps "execFileAsync")
       :policyDb policyDb
       :nodemailer (aget deps "nodemailer")
       ;; MCP deps
       :McpServer (aget deps "McpServer")
       :StreamableHTTPServerTransport (aget deps "StreamableHTTPServerTransport")
       :isInitializeRequest (aget deps "isInitializeRequest")
       :z (aget deps "z")})

(defn bootstrap!
  "Main entrypoint called from src/server.mjs.

   deps: JS object containing all imported Node/Fastify/MCP dependencies."  
  [deps]
  (let [cfg (runtime-models/enrich-config (runtime-config/cfg))
        Fastify (aget deps "Fastify")
        app (Fastify #js {:logger true})
        policy-options #js {:connectionString (or (aget js/process.env "KNOXX_POLICY_DATABASE_URL")
                                                 (aget js/process.env "DATABASE_URL")
                                                 "")
                           :primaryOrgSlug (env "KNOXX_PRIMARY_ORG_SLUG" "open-hax")
                           :primaryOrgName (env "KNOXX_PRIMARY_ORG_NAME" "Open Hax")
                           :primaryOrgKind (env "KNOXX_PRIMARY_ORG_KIND" "platform_owner")
                           :bootstrapSystemAdminEmail (env "KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL" "system-admin@open-hax.local")
                           :bootstrapSystemAdminName (env "KNOXX_BOOTSTRAP_SYSTEM_ADMIN_NAME" "Knoxx System Admin")
                           :bootstrapAllowlistEmails (env "KNOXX_BOOTSTRAP_ALLOWLIST_EMAILS" "")
                           :bootstrapAllowlistRoleSlugs (env "KNOXX_BOOTSTRAP_ALLOWLIST_ROLE_SLUGS" "")}
        cookie-hook? (truthy? (aget js/process.env "KNOXX_ENABLE_SESSION_HOOK"))]

    ;; Initialize global discord gateway manager (sets knoxx.backend.discord-gateway/manager*).
    (discord-gateway/createDiscordGatewayManager #js {:log js/console})

    (-> (policy-db/create-policy-db policy-options)
        (.then
         (fn [policyDb]
           (let [runtime (make-runtime deps policyDb)]
             (ensure-fastify-json-empty-body-parser! app)

             ;; Fastify plugins
             (-> (.register app (aget deps "fastifyCors") #js {:origin true})
                 (.then (fn [] (.register app (aget deps "fastifyCookie"))))
                 (.then (fn [] (.register app (aget deps "fastifyFormbody"))))
                 (.then (fn [] (.register app (aget deps "fastifyMultipart"))))
                 (.then (fn [] (.register app (aget deps "fastifyWebsocket"))))

                 ;; WS routes plugin
                 (.then (fn []
                          (.register app
                                     (fn [instance _opts done]
                                       (core/register-ws-routes! runtime instance)
                                       (done)))))

                 ;; Optional legacy session hook
                 (.then (fn []
                          (when cookie-hook?
                            (.addHook app "onRequest" (auth-session/create-session-hook policyDb)))))

                 ;; GitHub OAuth + cookie session auth routes
                 (.then (fn []
                          (auth-session/register-auth-routes app #js {:policyDb policyDb
                                                                      :runtime runtime})))

                 ;; Core CLJS routes (/api/*, etc.)
                 (.then (fn []
                          (core/register-app-routes! runtime app cfg lounge-messages*)))

                 ;; Extra host-level routes previously implemented in server.mjs
                 (.then (fn []
                          (proxy-routes/register-proxy-routes! app cfg)))

                 ;; MCP (server side) + OAuth consent UI
                 (.then (fn []
                          (mcp-http/register-mcp-http-routes! app runtime cfg)))

                 ;; Start listening
                 (.then (fn []
                          (.listen app #js {:host (:host cfg)
                                            :port (:port cfg)})))
                 (.then (fn [_]
                          (.info (.-log app) (str "Knoxx backend CLJS listening on " (:host cfg) ":" (:port cfg)))))
                 (.catch (fn [err]
                           (.error js/console "Knoxx backend CLJS failed to start" err)
                           (js/process.exit 1)))))))
        (.catch (fn [err]
                  (.error js/console "Knoxx policy DB failed to initialize" err)
                  (js/process.exit 1))))))

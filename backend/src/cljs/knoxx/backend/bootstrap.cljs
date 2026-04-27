(ns knoxx.backend.bootstrap
  "Single bootstrap entrypoint for hosting the Knoxx CLJS backend inside Node/Fastify.

   Contract:
   - The Node host shim (src/server.mjs) should ONLY import dependencies and call
     this function.
   - All HTTP routes, auth flows, and runtime wiring live in CLJS.
   - Dependencies are passed in as a single JS object (deps) and then threaded
     through request/runtime contexts."
  (:require [clojure.string :as str]
            [knoxx.backend.agent-resume :as agent-resume]
            [knoxx.backend.auth.session :as auth-session]
            [knoxx.backend.core :as core]
            [knoxx.backend.discord-gateway :as discord-gateway]
            [knoxx.backend.graceful-shutdown :as graceful-shutdown]
            [knoxx.backend.mcp-http :as mcp-http]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.models :as runtime-models]
            [knoxx.backend.routes.auth :as auth-routes]
            [knoxx.backend.tools.proxy-routes :as proxy-routes]
            [knoxx.backend.agent-turns :refer [lounge-messages*]]
            [knoxx.backend.policy-db :as policy-db]))

(defn- env
  [k default]
  (or (aget js/process.env k) default))

(defn- truthy?
  [v]
  (contains? #{"1" "true" "yes" "on" "y"} (-> (str (or v "")) str/trim str/lower-case)))

(defn- notify-ready!
  []
  (let [send-fn (aget js/process "send")
        connected? (aget js/process "connected")]
    (cond
      (fn? send-fn)
      (try
        (.call send-fn js/process "ready")
        (.log js/console (str "[knoxx-bootstrap] sent pm2 ready signal"
                              (when-not connected?
                                " (process.connected was false)")))
        true
        (catch :default err
          (.warn js/console "[knoxx-bootstrap] failed to send pm2 ready signal" err)
          false))

      :else
      (do
        (.log js/console "[knoxx-bootstrap] process.send unavailable; skipping pm2 ready signal")
        false))))

(defn- ensure-fastify-json-empty-body-parser!
  "Allow Content-Type: application/json with empty bodies.

   Fastify's default parser throws FST_ERR_CTP_EMPTY_JSON_BODY, but some
   endpoints are intentionally POST-without-body."  
  [^js app]
  (.addContentTypeParser app
                         "application/json"
                         #js {:parseAs "string"}
                         (fn [_req body done]
                           (try
                             (done nil (if (= body "") #js {} (js/JSON.parse body)))
                             (catch :default err
                              (done err))))))

(defn- app-add-hook!
  [^js app hook-name handler]
  (.addHook app hook-name handler))

(defn- app-listen!
  [^js app host port]
  (.listen app #js {:host host :port port}))

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
        ^js Fastify (aget deps "Fastify")
        app (Fastify #js {:logger true
                        :bodyLimit (* 50 1024 1024)
                        :requestTimeout 600000 ; 10 min
                        :connectionTimeout 600000})
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

              ;; Debug hook: log large requests before they hit 413
              (app-add-hook! app "onRequest"
                (fn [req _reply done]
                  (when-let [len (aget (.-headers req) "content-length")]
                    (when (> (js/parseInt len 10) (* 900 1024))
                      (js/console.warn "[knoxx] large request" (.-url req) len "bytes")))
                  (done)))

              ;; Fastify plugins
             (-> (.register app (aget deps "fastifyCors") #js {:origin true})
                 (.then (fn [] (.register app (aget deps "fastifyCookie"))))
                 (.then (fn [] (.register app (aget deps "fastifyFormbody"))))
                 (.then (fn [] (.register app (aget deps "fastifyMultipart")
                       #js {:limits #js {:fileSize (* 50 1024 1024)
                                         :fieldSize (* 1 1024 1024)
                                         :files 10}})))
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
                            (app-add-hook! app "onRequest" (auth-session/create-session-hook policyDb)))))

                 ;; GitHub OAuth + cookie session auth routes
                 (.then (fn []
                          (auth-routes/register-auth-routes app #js {:policyDb policyDb
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
                          (app-listen! app (:host cfg) (:port cfg))))
                 (.then (fn [_]
                          (graceful-shutdown/install! app cfg)
                          (notify-ready!)
                          (let [^js log (.-log app)]
                            (.info log (str "Knoxx backend CLJS listening on " (:host cfg) ":" (:port cfg)))
                            ;; Redis + session resume (non-blocking)
                            (-> (redis/init-redis! (:redis-url cfg))
                                (.then (fn [client]
                                         (when client
                                           (.info log "Redis connected for session persistence")
                                           ;; Fire-and-forget: must not block startup
                                           (agent-resume/resume-on-startup! runtime app cfg)
                                           (agent-resume/start-periodic-recovery! runtime app cfg))))
                                (.catch (fn [err]
                                          (.warn log "Redis initialization failed" err)))))))
                 (.catch (fn [err]
                           (.error js/console "Knoxx backend CLJS failed to start" err)
                           (js/process.exit 1)))))))
        (.catch (fn [err]
                  (.error js/console "Knoxx policy DB failed to initialize" err)
                  (js/process.exit 1))))))

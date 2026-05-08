(ns knoxx.backend.bootstrap
  "Startup orchestration for the Knoxx CLJS backend.

   Contract:
   - shadow-cljs calls knoxx.backend.entrypoint/init.
   - Node/npm modules are required by the CLJS namespaces that consume them.
   - This namespace orchestrates startup but should not be a dependency-injection
     dump for the whole backend."
  (:require [clojure.string :as str]
            [knoxx.backend.agent-resume :as agent-resume]
            [knoxx.backend.auth.session :as auth-session]
            [knoxx.backend.core :as core]
            [knoxx.backend.discord-gateway :as discord-gateway]
            [knoxx.backend.discord-reaction-labels :as discord-reaction-labels]
            [knoxx.backend.graceful-shutdown :as graceful-shutdown]
            [knoxx.backend.http-server :as http-server]
            [knoxx.backend.lifecycle :as lifecycle]
            [knoxx.backend.policy-db :as policy-db]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.routes.auth :as auth-routes]
            [knoxx.backend.routes.mcp :as mcp-http]
            [knoxx.backend.routes.tools.proxy :as proxy-routes]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.models :as runtime-models]
            [knoxx.backend.runtime.state :as runtime-state]
            [knoxx.backend.agent-turns :refer [lounge-messages*]]))

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

(defn- policy-options
  []
  #js {:connectionString (or (aget js/process.env "KNOXX_POLICY_DATABASE_URL")
                             (aget js/process.env "DATABASE_URL")
                             "")
       :primaryOrgSlug (env "KNOXX_PRIMARY_ORG_SLUG" "open-hax")
       :primaryOrgName (env "KNOXX_PRIMARY_ORG_NAME" "Open Hax")
       :primaryOrgKind (env "KNOXX_PRIMARY_ORG_KIND" "platform_owner")
       :bootstrapSystemAdminEmail (env "KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL" "system-admin@open-hax.local")
       :bootstrapSystemAdminName (env "KNOXX_BOOTSTRAP_SYSTEM_ADMIN_NAME" "Knoxx System Admin")
       :bootstrapAllowlistEmails (env "KNOXX_BOOTSTRAP_ALLOWLIST_EMAILS" "")
       :bootstrapAllowlistRoleSlugs (env "KNOXX_BOOTSTRAP_ALLOWLIST_ROLE_SLUGS" "")})

(defn start-http!
  "Create a fresh Fastify app and bind HTTP routes around durable runtime state."
  [runtime cfg policyDb cookie-hook?]
  (runtime-state/remember-context! runtime cfg policyDb)
  (let [app (http-server/create-app!)]
    (http-server/ensure-json-empty-body-parser! app)
    ;; Debug hook: log large requests before they hit 413.
    (http-server/add-hook! app "onRequest"
      (fn [req reply done]
        (when (= (.-url req) "/api/dev/hmr")
          (.header reply "x-knoxx-hmr-probe" "hmr-probe-2026-05-07-e")
          (js/console.log "[knoxx-hot-reload-probe] hmr-probe-2026-05-07-e"))
        (when-let [len (aget (.-headers req) "content-length")]
          (when (> (js/parseInt len 10) (* 900 1024))
            (js/console.warn "[knoxx] large request" (.-url req) len "bytes")))
        (done)))
    (-> (http-server/register-default-plugins! app)
        ;; WS routes plugin.
        (.then (fn []
                 (.register app
                            (fn [instance _opts done]
                              (core/register-ws-routes! runtime instance)
                              (done)))))
        ;; Optional legacy session hook.
        (.then (fn []
                 (when cookie-hook?
                   (http-server/add-hook! app "onRequest" (auth-session/create-session-hook policyDb)))))
        ;; GitHub OAuth + cookie session auth routes.
        (.then (fn []
                 (auth-routes/register-auth-routes app #js {:policyDb policyDb
                                                            :runtime runtime})))
        ;; Core CLJS routes (/api/*, etc.).
        (.then (fn []
                 (core/register-app-routes! runtime app cfg lounge-messages*)))
        ;; Extra host-level routes previously implemented in server.mjs.
        (.then (fn []
                 (proxy-routes/register-proxy-routes! app cfg)))
        ;; MCP (server side) + OAuth consent UI.
        (.then (fn []
                 (mcp-http/register-mcp-http-routes! app runtime cfg)))
        ;; Start listening.
        (.then (fn []
                 (http-server/listen! app (:host cfg) (:port cfg))))
        (.then (fn [_]
                 (lifecycle/remember-app! app)
                 (graceful-shutdown/install! app cfg)
                 (notify-ready!)
                 (let [^js log (.-log app)]
                   (.info log (str "Knoxx backend CLJS listening on " (:host cfg) ":" (:port cfg)))
                   ;; Redis + session resume (non-blocking).
                   (-> (redis/init-redis! (:redis-url cfg))
                       (.then (fn [client]
                                (when client
                                  (.info log "Redis connected for session persistence")
                                  ;; Fire-and-forget: must not block startup.
                                  (agent-resume/resume-on-startup! runtime app cfg)
                                  (agent-resume/start-periodic-recovery! runtime app cfg))))
                       (.catch (fn [err]
                                 (.warn log "Redis initialization failed" err))))
                   app))))))

(defn bootstrap!
  "Main entrypoint called by shadow-cljs."
  []
  (let [cfg (runtime-models/enrich-config (runtime-config/cfg))
        cookie-hook? (truthy? (aget js/process.env "KNOXX_ENABLE_SESSION_HOOK"))]
    ;; Initialize global durable process state once at process boot. The HTTP app
    ;; can be closed/recreated by shadow-cljs lifecycle hooks without touching
    ;; these durable services.
    (discord-gateway/createDiscordGatewayManager #js {:log js/console})
    (discord-reaction-labels/bind! cfg)

    (-> (policy-db/create-policy-db (policy-options))
        (.then (fn [policyDb]
                 (let [runtime #js {}]
                   (lifecycle/remember-context! runtime cfg policyDb cookie-hook?)
                   (start-http! runtime cfg policyDb cookie-hook?))))
        (.catch (fn [err]
                  (.error js/console "Knoxx policy DB failed to initialize" err)
                  (js/process.exit 1))))))

(defn ^:dev/before-load-async stop-http-before-load!
  [done]
  (.log js/console "[knoxx-hot-reload] before-load: closing HTTP server")
  (-> (lifecycle/close-current-http!)
      (.then (fn [_]
               (.log js/console "[knoxx-hot-reload] before-load: HTTP server closed")))
      (.catch (fn [err]
                (.error js/console "[knoxx-hot-reload] failed to close HTTP server" err)))
      (.finally done)))

(defn ^:dev/after-load-async start-http-after-load!
  [done]
  (.log js/console "[knoxx-hot-reload] after-load: starting HTTP server")
  (let [{:keys [runtime config policyDb cookie-hook?]} (lifecycle/context)]
    (if (and runtime config policyDb)
      (-> (start-http! runtime config policyDb cookie-hook?)
          (.then (fn [_]
                   (.log js/console "[knoxx-hot-reload] after-load: HTTP server started")))
          (.catch (fn [err]
                    (.error js/console "[knoxx-hot-reload] failed to restart HTTP server" err)))
          (.finally done))
      (do
        (.warn js/console "[knoxx-hot-reload] no lifecycle context; skipping HTTP restart")
        (done)))))

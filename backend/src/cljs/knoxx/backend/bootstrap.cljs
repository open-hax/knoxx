(ns knoxx.backend.bootstrap
  "Startup orchestration for the Knoxx CLJS backend.

   Contract:
   - shadow-cljs calls knoxx.backend.entrypoint/init.
   - Node/npm modules are required by the CLJS namespaces that consume them.
   - This namespace orchestrates startup but should not be a dependency-injection
     dump for the whole backend."
  (:require [clojure.string :as str]
            [knoxx.backend.contract-runtime-deps :as contract-runtime-deps]
            [knoxx.backend.infra.agent.resume :as agent-resume]
            [knoxx.backend.infra.auth.session :as auth-session]
            [knoxx.backend.infra.clients.openplanner-mongo]
            [knoxx.backend.infra.core :as core]
            [knoxx.backend.domain.discord.gateway :as discord-gateway]
            [knoxx.backend.domain.discord.discord-reaction-labels :as discord-reaction-labels]
            [knoxx.backend.domain.graph.policy-registry :as graph-policy-registry]
            [knoxx.backend.infra.graceful-shutdown :as graceful-shutdown]
            [knoxx.backend.infra.http-server :as http-server]
            [knoxx.backend.infra.lifecycle :as lifecycle]
            [knoxx.backend.infra.db.policy :as policy-db]
             [knoxx.backend.infra.mongo-client :as mongo-client]
             [knoxx.backend.infra.stores.mongo-policy-store :as mongo-policy-store]
             [knoxx.backend.infra.stores.mongo-run-store :as mongo-run-store]
             [knoxx.backend.infra.stores.mongo-session-store :as mongo-session-store]
             [knoxx.backend.infra.stores.mongo-session-titles :as mongo-session-titles]
             [knoxx.backend.infra.stores.mongo-temp-memory :as mongo-temp-memory]
             [knoxx.backend.infra.stores.mongo-memory-sessions :as mongo-memory-sessions]
             [knoxx.backend.infra.stores.mongo-mcp-oauth :as mongo-mcp-oauth]
             [knoxx.backend.infra.stores.mongo-rate-limits :as mongo-rate-limits]
             [knoxx.backend.infra.stores.mongo-translation-evidence :as mongo-translation-evidence]
             [knoxx.backend.infra.stores.session-store-registry :as store-registry]
             [knoxx.backend.infra.stores.translation-evidence-registry :as translation-evidence-registry]
             [knoxx.backend.infra.stores.session-flush :as session-flush]
            [knoxx.backend.infra.routes.auth :as auth-routes]
            [knoxx.backend.infra.routes.mcp :as mcp-http]
            [knoxx.backend.infra.routes.tools.proxy :as proxy-routes]
            [knoxx.backend.infra.config :as runtime-config]
            [knoxx.backend.domain.models :as runtime-models]
            [knoxx.backend.runtime.state :as runtime-state]
            [knoxx.backend.infra.agent.turn :refer [lounge-messages*]]))

(defn- env
  [k default]
  (or (aget js/process.env k) default))

(defn- truthy?
  [v]
  (contains? #{"1" "true" "yes" "on" "y"} (-> (str (or v "")) str/trim str/lower-case)))

(def hmr-probe-token
  "hmr-probe-2026-05-09-stability-b")

(defn- process-uptime-ms
  []
  (js/Math.round (* 1000 (.uptime js/process))))

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
  #js {:primaryOrgSlug (env "KNOXX_PRIMARY_ORG_SLUG" "open-hax")
       :primaryOrgName (env "KNOXX_PRIMARY_ORG_NAME" "Open Hax")
       :primaryOrgKind (env "KNOXX_PRIMARY_ORG_KIND" "platform_owner")
       :bootstrapSystemAdminEmail (env "KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL" "system-admin@open-hax.local")
       :bootstrapSystemAdminName (env "KNOXX_BOOTSTRAP_SYSTEM_ADMIN_NAME" "Knoxx System Admin")
       :bootstrapSystemAdminPassword (env "KNOXX_BOOTSTRAP_SYSTEM_ADMIN_PASSWORD" "")
       :bootstrapAllowlistEmails (env "KNOXX_BOOTSTRAP_ALLOWLIST_EMAILS" "")
       :bootstrapAllowlistRoleSlugs (env "KNOXX_BOOTSTRAP_ALLOWLIST_ROLE_SLUGS" "")})

(defn- log-hmr-probe!
  [req reply]
  (when (= (.-url req) "/api/dev/hmr")
    (.header ^js reply "x-knoxx-hmr-probe" hmr-probe-token)
    (js/console.log "[knoxx-hot-reload-probe]" hmr-probe-token
                    #js {:pid (.-pid js/process)
                         :uptimeMs (process-uptime-ms)})))

(defn- log-large-request!
  [req]
  (when-let [len (aget (.-headers req) "content-length")]
    (when (> (js/parseInt len 10) (* 900 1024))
      (js/console.warn "[knoxx] large request" (.-url req) len "bytes"))))

(defn- add-request-debug-hook!
  [app]
  (http-server/add-hook! app "onRequest"
    (fn [req reply done]
      (log-hmr-probe! req reply)
      (log-large-request! req)
      (done))))

(defn- register-ws-routes-plugin!
  [runtime app]
  (.register app
             (fn [instance _opts done]
               (core/register-ws-routes! runtime instance)
               (done))))

(defn- add-session-hook!
  [app policy-context cookie-hook?]
  (when cookie-hook?
    (http-server/add-hook! app "onRequest" (auth-session/create-session-hook policy-context))))

(defn- register-http-routes!
  [runtime app cfg policy-context]
  (auth-routes/register-auth-routes app {:policy-context policy-context
                                         :runtime runtime})
  (core/register-app-routes! runtime app cfg lounge-messages*)
  (proxy-routes/register-proxy-routes! app cfg)
  (mcp-http/register-mcp-http-routes! app runtime cfg))

(defn- ^:async start-translation-evidence!
  "Publish the durable translation evidence store, indexes first.

   The unique index on `dispatch_key` IS the atomic claim that stops one revision
   being dispatched to the worker twice, so the store is only published after
   `setup-indexes!` has resolved. Published before the index existed, a
   concurrent pair of dispatches could both insert and both believe they had
   reserved the key."
  [db]
  (await (mongo-translation-evidence/setup-indexes! db))
  (reset! translation-evidence-registry/store*
          (mongo-translation-evidence/create-store db)))

(defn- ^:async start-mongo-indexes!
  "Create every collection's indexes, then publish the stores that need them.

   Extracted from `start-mongo-persistence!` so that function stays about
   lifecycle — connect, index, resume, schedule — rather than growing one line
   per collection."
  [db]
  (mongo-session-store/setup-indexes! db)
  (mongo-run-store/setup-indexes! db)
  ;; Cache stores for session titles, temp memory, memory sessions
  (mongo-session-titles/setup-indexes! db)
  (mongo-temp-memory/setup-indexes! db)
  (mongo-memory-sessions/setup-indexes! db)
  ;; MCP OAuth store
  (mongo-mcp-oauth/setup-indexes! db)
  ;; Rate limits store
  (mongo-rate-limits/setup-indexes! db)
  ;; Translation dispatch bindings and completed-translation evidence.
  (await (start-translation-evidence! db))
  ;; ensure-indexes! (not setup-indexes!): it catches index
  ;; failures so a bad index spec can never crash-loop the
  ;; process from this fire-and-forget bootstrap path.
  (mongo-policy-store/ensure-indexes! db)
  (reset! store-registry/session-store*
          (mongo-run-store/create-mongo-run-store db)))

(defn- ^:async start-mongo-persistence!
  [runtime app cfg log]
  (try
    (let [db (await (mongo-client/init-mongo!))]
      (when db
        (.info log "MongoDB connected for session persistence")
        (await (start-mongo-indexes! db))
        ;; Fire-and-forget: must not block startup.
        ;; Guarded so shadow-cljs hot reload does not spawn
        ;; recovery jobs as if the Node process had restarted.
        (agent-resume/resume-on-process-startup! runtime app cfg)
        (agent-resume/start-periodic-recovery! runtime app cfg)
        (session-flush/start-periodic-flush! (:run-stale-flush-ms cfg))))
    (catch :default err
      (.warn log "MongoDB initialization failed" err))))

(defn- start-session-persistence!
  [runtime app cfg log]
  (start-mongo-persistence! runtime app cfg log))

(defn- handle-app-listening!
  [runtime app cfg]
  (lifecycle/remember-app! app)
  (graceful-shutdown/install! app cfg)
  (notify-ready!)
  (let [^js log (.-log app)]
    (.info log (str "Knoxx backend CLJS listening on " (:host cfg) ":" (:port cfg)))
    (start-session-persistence! runtime app cfg log)
    app))

(defn ^:async start-http!
  "Create a fresh Fastify app and bind HTTP routes around durable runtime state."
  [runtime cfg policy-context cookie-hook?]
  (runtime-state/remember-context! runtime cfg policy-context)
  (let [app (http-server/create-app!)]
    (http-server/ensure-json-empty-body-parser! app)
    (add-request-debug-hook! app)
    (await (http-server/register-default-plugins! app))
    (await (register-ws-routes-plugin! runtime app))
    (await (add-session-hook! app policy-context cookie-hook?))
    (await (register-http-routes! runtime app cfg policy-context))
    (await (http-server/listen! app (:host cfg) (:port cfg)))
    (handle-app-listening! runtime app cfg)))

(defn ^:async bootstrap!
  "Main entrypoint called by shadow-cljs."
  []
  (let [cfg (contract-runtime-deps/inject-deps!
             (runtime-models/enrich-config (runtime-config/cfg)))
        cookie-hook? (truthy? (aget js/process.env "KNOXX_ENABLE_SESSION_HOOK"))]
    ;; Initialize global durable process state once at process boot. The HTTP app
    ;; can be closed/recreated by shadow-cljs lifecycle hooks without touching
    ;; these durable services.
    (discord-gateway/createDiscordGatewayManager #js {:log js/console})
    (discord-reaction-labels/bind! cfg)
    (graph-policy-registry/init!)

    (try
      (let [policy-context (await (policy-db/create-policy-db (policy-options)))
            runtime #js {}]
        (lifecycle/remember-context! runtime cfg policy-context cookie-hook?)
        (await (start-http! runtime cfg policy-context cookie-hook?)))
      (catch :default err
        (.error js/console "Knoxx policy DB failed to initialize" err)
        (js/process.exit 1)))))

(defn ^:async ^:dev/before-load-async stop-http-before-load!
  [done]
  (.log js/console "[knoxx-hot-reload] before-load: closing HTTP server"
        #js {:pid (.-pid js/process)
             :uptimeMs (process-uptime-ms)})
  (session-flush/stop-periodic-flush!)
  (try
    (await (lifecycle/close-current-http!))
    (.log js/console "[knoxx-hot-reload] before-load: HTTP server closed"
          #js {:pid (.-pid js/process)
               :uptimeMs (process-uptime-ms)})
    (catch :default err
      (.error js/console "[knoxx-hot-reload] failed to close HTTP server" err))
    (finally (done))))

(defn ^:async ^:dev/after-load-async start-http-after-load!
  [done]
  (.log js/console "[knoxx-hot-reload] after-load: starting HTTP server"
        #js {:pid (.-pid js/process)
             :uptimeMs (process-uptime-ms)})
  ;; Re-read config from env on every hot reload so changed config defaults and
  ;; env vars take effect without a full process restart — the durable runtime
  ;; and policy-context handles survive, only the (data) config is refreshed.
  ;; This refresh also flows into the stale-run flush, which start-http! restarts
  ;; via start-session-persistence!. Event/cron turns already read (cfg) fresh
  ;; per dispatch, so this closes the gap for the HTTP-served path and the flush.
  (let [{:keys [runtime policy-context cookie-hook?]} (lifecycle/context)
        config (contract-runtime-deps/inject-deps!
                (runtime-models/enrich-config (runtime-config/cfg)))]
    (if (and runtime policy-context)
      (do
        (lifecycle/remember-context! runtime config policy-context cookie-hook?)
        (try
          (await (start-http! runtime config policy-context cookie-hook?))
          (.log js/console "[knoxx-hot-reload] after-load: HTTP server started"
                #js {:pid (.-pid js/process)
                     :uptimeMs (process-uptime-ms)})
          (catch :default err
            (.error js/console "[knoxx-hot-reload] failed to restart HTTP server" err))
          (finally (done))))
      (do
        (.warn js/console "[knoxx-hot-reload] no lifecycle context; skipping HTTP restart")
        (done)))))

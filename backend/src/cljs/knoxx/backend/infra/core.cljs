(ns knoxx.backend.infra.core
  (:require [knoxx.backend.infra.agent.hydration :refer [ensure-settings!]]
            [knoxx.backend.infra.agent.turn :as agent-turns :refer [lounge-messages*]]
            [knoxx.backend.infra.routes.app :as app-routes]
            [knoxx.backend.infra.routes.resources :as resource-routes]
            [knoxx.backend.infra.event-runtime :as event-runtime]
            [knoxx.backend.domain.mcp.mcp-bridge :as mcp]
            [knoxx.backend.domain.realtime :as realtime]
            [knoxx.backend.infra.agent.resume :as agent-resume]
            [knoxx.backend.domain.action.run-state :refer [active-runs-count]]
            [knoxx.backend.infra.config :as runtime-config]
            [knoxx.backend.domain.models :as runtime-models]
            [knoxx.backend.runtime.state :as runtime-state]
            [knoxx.backend.infra.stores.session-titles :refer [load-session-titles!]]
            [knoxx.backend.domain.discord.source :as discord-source]
            [knoxx.backend.domain.driver.builtin :as driver-builtin]
            [knoxx.backend.domain.source.runtime :as source-runtime]
            [knoxx.backend.domain.condition.builtin :as condition-builtins]
            [knoxx.backend.infra.lifecycle :as lifecycle]
            [knoxx.backend.infra.agent.session :as agent-session]
            ["fastify" :default Fastify]
            ["@fastify/cors" :default fastifyCors]
            ["@fastify/websocket" :default fastifyWebsocket]
            ["@fastify/multipart" :default fastifyMultipart]))

(defonce server* (atom nil))

(defn- app-log-info!
  [app message]
  (let [^js log (.-log app)]
    (.info log message)))

(defn- app-log-error!
  [app message err]
  (let [^js log (.-log app)]
    (if err
      (.error log err message)
      (.error log message))))

(defn- app-listen!
  [^js app host port]
  (.listen app #js {:host host :port port}))

(defn register-ws-routes!
  [runtime app]
  (realtime/register-ws-routes! runtime app active-runs-count lounge-messages*))


(defn config-js
  []
  (clj->js (runtime-models/enrich-config (runtime-config/cfg))))

(defn- ^:async initialize-mcp-gateway!
  [app resolved-config]
  (if-not (:mcp-enabled resolved-config)
    nil
    (let [existing-servers (mcp/parse-mcp-servers-env (or (aget js/process.env "MCP_SERVERS") ""))
          openplanner-url (:openplanner-mcp-base-url resolved-config)
          openplanner-name (:openplanner-mcp-tool-name resolved-config "openplanner")
          shoedelussy-url (:shoedelussy-mcp-base-url resolved-config)
          shoedelussy-name (:shoedelussy-mcp-tool-name resolved-config "shoedelussy")
          shoedelussy-secret (:shoedelussy-mcp-shared-secret resolved-config)
          merged-servers (cond-> existing-servers
                           (and (not (contains? existing-servers openplanner-name))
                                (some? openplanner-url)
                                (not= "" openplanner-url))
                           (assoc openplanner-name {:url openplanner-url
                                                    :transport "http"})

                           (and (not (contains? existing-servers shoedelussy-name))
                                (some? shoedelussy-url)
                                (not= "" shoedelussy-url))
                           (assoc shoedelussy-name {:url shoedelussy-url
                                                    :transport "http"
                                                    :shared-secret shoedelussy-secret}))]
      (try
        (await (mcp/initialize! {:servers merged-servers}))
        (app-log-info! app (str "MCP gateway initialized: " (count (mcp/catalog)) " tools available"))
        (catch :default err
          (app-log-error! app "MCP gateway initialization failed" err))))))

(def ^:private event-runtimes-disabled-banner
  ["╔══════════════════════════════════════════════════════════════════════╗"
   "║  EVENT RUNTIMES ARE DISABLED — KNOXX_DISABLE_EVENT_RUNTIMES is set   ║"
   "║                                                                      ║"
   "║  NOT RUNNING: schedules, triggers, Discord actor gateways.           ║"
   "║  This process will not answer Discord messages, fire scheduled       ║"
   "║  jobs, or react to any contract event.                               ║"
   "║                                                                      ║"
   "║  This mode exists ONLY for local human verification. If you are      ║"
   "║  seeing this in production, Knoxx is silently doing nothing.         ║"
   "╚══════════════════════════════════════════════════════════════════════╝"])

(def ^:private event-runtimes-nag-ms 60000)

(defonce ^:private nag-timer* (atom nil))

(defn- warn-event-runtimes-disabled!
  "Say it loudly at boot, then keep saying it.

   A one-line startup notice scrolls away in seconds and a process can then sit
   for hours looking healthy while doing none of its event work. The recurring
   nag is unref'd so it never holds the process open at shutdown.

   Both `start!` and `start-background-services!` announce this, because either
   can be the boot path depending on entry point, and a hot reload can run one
   of them again. The timer is armed once so those paths do not stack nags."
  []
  (doseq [line event-runtimes-disabled-banner]
    (js/console.warn line))
  (when-not @nag-timer*
    (let [timer (js/setInterval
                 #(js/console.warn
                   "[event-runtimes] STILL DISABLED via KNOXX_DISABLE_EVENT_RUNTIMES —"
                   "no schedules, no triggers, no Discord gateways")
                 event-runtimes-nag-ms)]
      (when (fn? (some-> timer .-unref))
        (.unref timer))
      (reset! nag-timer* timer)))
  @nag-timer*)

(defn- ^:async bind-discord-actor-gateways!
  "Connect the Discord actor gateways and route their events into the driver
   runtime.

   Extracted from `start-background-services!` so the boot sequence there reads
   as a list of services rather than burying the two dispatch closures in the
   middle of it."
  [resolved-config policy-context]
  (await (discord-source/bind-gateways!
          {:policy-db policy-context
           :on-message! (fn [msg]
                          (source-runtime/dispatch-driver-event!
                           resolved-config
                           :driver/discord
                           (:gatewayActorId msg)
                           {:event/type :discord.message
                            :event/payload msg}))
           :on-voice-state! (fn [state]
                              (source-runtime/dispatch-driver-event!
                               resolved-config
                               :driver/discord
                               (:gatewayActorId state)
                               {:event/type :discord.voice.state-update
                                :event/payload state}))})))

(defn- ^:async start-background-services!
  [app resolved-config]
  (driver-builtin/register-built-in-drivers!)
  (condition-builtins/register-builtins!)
  ;; Session recovery is awaited separately only until recovered turns are
  ;; kicked off again. The event runtime and MCP discovery remain background work.
  (try
    (let [event-runtimes? (not (:event-runtimes-disabled? resolved-config))
          policy-context (:policy-context (lifecycle/context))]
      (when-not event-runtimes? (warn-event-runtimes-disabled!))
      (event-runtime/start! resolved-config)
      (resource-routes/start-resource-watcher! resolved-config)
      (when (and event-runtimes? policy-context)
        (await (bind-discord-actor-gateways! resolved-config policy-context))))
    (await (initialize-mcp-gateway! app resolved-config))
    (catch :default err
      (app-log-error! app "Background startup services failed" err))))

(defn ^:async prewarm-sdk-runtime!
  [runtime app resolved-config]
  (await (agent-session/ensure-eta-mu-runtime! runtime resolved-config))
  (app-log-info! app "Knoxx SDK runtime prewarmed"))

(defn register-app-routes!
  [runtime app config lounge-messages*]
  (let [resolved-config (runtime-models/enrich-config (if (map? config) config (runtime-config/cfg)))]
    (ensure-settings! resolved-config)
    (reset! runtime-state/config* resolved-config)
    (reset! runtime-state/runtime* runtime)
    (app-routes/register-routes! runtime app resolved-config lounge-messages*)
    ;; Route registration and HTTP listen must not be gated on the SDK runtime
    ;; cache or contract health. Invalid contracts, model-registry misses, and
    ;; upstream model fetch failures should degrade agent turns later; they must
    ;; never prevent the backend from binding /health and admin repair routes.
     (js/setTimeout
      (^:async fn []
        (try
          (await (prewarm-sdk-runtime! runtime app resolved-config))
          (catch :default err
            (app-log-error! app "Knoxx SDK runtime prewarm failed; startup continuing" err))))
      1000)
    (js/setTimeout
     (^:async fn []
       (try
         (await (start-background-services! app resolved-config))
         (catch :default err
           (app-log-error! app "Background startup services promise failed" err))))
     1500)
    (js/Promise.resolve (clj->js resolved-config))))

(defn ^:async start!
  [runtime]
  (when-not @server*
    (let [config (runtime-models/enrich-config (runtime-config/cfg))
          app (Fastify #js {:logger #js {:stream (.-stderr js/process)}})]
      (reset! runtime-state/config* config)
      (reset! runtime-state/runtime* runtime)
      (driver-builtin/register-built-in-drivers!)
      (ensure-settings! config)
      (try
        (await (load-session-titles! runtime config))
        (await (.register app fastifyCors #js {:origin true}))
        (await (.register app fastifyMultipart))
        (await (.register app fastifyWebsocket))
        (await (.register app
                          (fn [instance _opts done]
                            (register-ws-routes! runtime instance)
                            (done))))
        (app-routes/register-routes! runtime app config lounge-messages*)
        ;; Sync the resource index before arming schedule resources.
        (try
          (resource-routes/sync-resource-index! config)
          (catch :default err
            (app-log-error! app "Failed to sync resource index" err)))
        ;; Start the event runtime composition shell. `start!` self-gates on
        ;; KNOXX_DISABLE_EVENT_RUNTIMES; this path never reaches
        ;; start-background-services!, so it announces the mode itself.
        (when (event-runtime/disabled? config) (warn-event-runtimes-disabled!))
        (event-runtime/start! config)
        (resource-routes/start-resource-watcher! config)
        (await (app-listen! app (:host config) (:port config)))
        (reset! server* app)
        (app-log-info! app (str "Knoxx backend CLJS listening on " (:host config) ":" (:port config)))
        ;; Session resume after listen — must not block startup.
        (try
          (await (agent-resume/resume-on-startup! runtime app config))
          (catch :default err
            (app-log-error! app "agent-resume failed" err)))
        (catch :default err
          (.error js/console "Knoxx backend CLJS failed to start" err)
          (js/process.exit 1))))))

;; Handle graceful shutdown

(ns knoxx.backend.infra.core
  (:require [knoxx.backend.infra.agent.hydration :refer [ensure-settings!]]
            [knoxx.backend.infra.agent.turn :as agent-turns :refer [lounge-messages*]]
            [knoxx.backend.infra.routes.app :as app-routes]
            [knoxx.backend.infra.routes.resources :as resource-routes]
            [knoxx.backend.infra.event-runtime :as event-runtime]
            [knoxx.backend.domain.mcp.mcp-bridge :as mcp]
            [knoxx.backend.domain.realtime :as realtime]
            [knoxx.backend.infra.redis-client :as redis]
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

(defn- initialize-mcp-gateway!
  [app resolved-config]
  (if-not (:mcp-enabled resolved-config)
    (js/Promise.resolve nil)
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
      (-> (mcp/initialize! {:servers merged-servers})
          (.then (fn [_]
                   (app-log-info! app (str "MCP gateway initialized: " (count (mcp/catalog)) " tools available"))))
          (.catch (fn [err]
                    (app-log-error! app "MCP gateway initialization failed" err)))))))

(defn- start-background-services!
  [app resolved-config]
  (driver-builtin/register-built-in-drivers!)
  (condition-builtins/register-builtins!)
  ;; Session recovery is awaited separately only until recovered turns are
  ;; kicked off again. The event runtime and MCP discovery remain background work.
  ;;
  ;; IMPORTANT: event-runtime/start! expects redis/get-client to be ready so it
  ;; can load resource state before schedule rules are armed.
  (-> (redis/init-redis! (:redis-url resolved-config))
      (.then (fn [_]
               (event-runtime/start! resolved-config)))
      (.then (fn [_]
               (resource-routes/start-resource-watcher! resolved-config)))
      (.then (fn [_]
               (let [policy-context (:policy-context (lifecycle/context))]
                 (when policy-context
                   (discord-source/bind-gateways!
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
                                          :event/payload state}))})))))
      (.then (fn [_]
               (initialize-mcp-gateway! app resolved-config)))
      (.catch (fn [err]
                (app-log-error! app "Background startup services failed" err)))))

(defn prewarm-sdk-runtime!
  [runtime app resolved-config]
  (-> (agent-session/ensure-eta-mu-runtime! runtime resolved-config)
      (.then (fn [_] (app-log-info! app "Knoxx SDK runtime prewarmed")))))

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
     (fn []
       (-> (prewarm-sdk-runtime! runtime app resolved-config)
           (.catch (fn [err]
                     (app-log-error! app "Knoxx SDK runtime prewarm failed; startup continuing" err)
                     nil))))
     1000)
    (js/setTimeout
     (fn []
       (start-background-services! app resolved-config))
     1500)
    (js/Promise.resolve (clj->js resolved-config))))

(defn start!
  [runtime]
  (when-not @server*
    (let [config (runtime-models/enrich-config (runtime-config/cfg))
          app (Fastify #js {:logger #js {:stream (.-stderr js/process)}})]
      (reset! runtime-state/config* config)
      (reset! runtime-state/runtime* runtime)
      (driver-builtin/register-built-in-drivers!)
      (ensure-settings! config)
      (-> (js/Promise.resolve nil)
          (.then (fn [] (load-session-titles! runtime config)))
          (.then (fn [] (.register app fastifyCors #js {:origin true})))
          (.then (fn [] (.register app fastifyMultipart)))
          (.then (fn [] (.register app fastifyWebsocket)))
          (.then (fn []
                   (.register app
                              (fn [instance _opts done]
                                (register-ws-routes! runtime instance)
                                (done)))))
          (.then (fn []
                   (app-routes/register-routes! runtime app config lounge-messages*)
                   ;; Bring Redis up before arming schedule resources.
                   (-> (redis/init-redis! (:redis-url config))
                       (.then (fn [redis-client]
                                (when redis-client
                                  (app-log-info! app "Redis client initialized")
                                  (resource-routes/sync-resource-index! config))
                                nil))
                       (.catch (fn [err]
                                 (app-log-error! app "Failed to initialize Redis" err)
                                 nil))
                       (.then (fn [_]
                                ;; Start the event runtime composition shell.
                                (event-runtime/start! config)
                                (resource-routes/start-resource-watcher! config)
                                (app-listen! app (:host config) (:port config)))))))
          (.then (fn [_]
                   (reset! server* app)
                   (app-log-info! app (str "Knoxx backend CLJS listening on " (:host config) ":" (:port config)))
                   ;; Session resume after listen — must not block startup.
                   (-> (agent-resume/resume-on-startup! runtime app config)
                       (.catch (fn [err]
                                 (app-log-error! app "agent-resume failed" err))))))
          (.catch (fn [err]
                    (.error js/console "Knoxx backend CLJS failed to start" err)
                    (js/process.exit 1)))))))

;; Handle graceful shutdown

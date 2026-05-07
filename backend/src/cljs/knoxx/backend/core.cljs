(ns knoxx.backend.core
  (:require [knoxx.backend.agent-hydration :refer [ensure-settings!]]
            [knoxx.backend.agent-runtime :as agent-runtime]
            [knoxx.backend.agent-turns :as agent-turns :refer [lounge-messages*]]
            [knoxx.backend.routes.app :as app-routes]
            [knoxx.backend.routes.contracts :as contracts-routes]
            [knoxx.backend.events.runtime :as events-runtime]
            [knoxx.backend.mcp-bridge :as mcp]
            [knoxx.backend.realtime :as realtime]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.agent-resume :as agent-resume]
            [knoxx.backend.run-state :refer [active-runs-count]]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.models :as runtime-models]
            [knoxx.backend.runtime.state :as runtime-state]
            [knoxx.backend.session-titles :refer [load-session-titles!]]
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
    (.error log message err)))

(defn- app-listen!
  [^js app host port]
  (.listen app #js {:host host :port port}))

(defn register-ws-routes!
  [runtime app]
  (realtime/register-ws-routes! runtime app active-runs-count lounge-messages*))

(defn- prewarm-sdk-runtime!
  [runtime app resolved-config]
  (-> (agent-runtime/ensure-sdk-runtime! runtime resolved-config)
      (.then (fn [_]
               (app-log-info! app "Knoxx SDK runtime prewarmed")))
      (.catch (fn [err]
                (app-log-error! app "Knoxx SDK runtime prewarm failed" err)
                (js/Promise.reject err)))))

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
  ;; Session recovery is awaited separately only until recovered turns are
  ;; kicked off again. Event agents and MCP discovery remain background work.
  ;;
  ;; IMPORTANT: events-runtime/start! expects redis/get-client to be ready so it
  ;; can load persisted control config (disable/enable jobs) *before* scheduling.
  ;; If we start event agents before redis/init-redis!, every restart will run
  ;; the default job set, even if the admin panel disabled a crashing job.
  (-> (redis/init-redis! (:redis-url resolved-config))
      (.then (fn [_]
               (events-runtime/start! resolved-config)))
      (.then (fn [_]
               (contracts-routes/start-contract-watcher! resolved-config)))
      (.then (fn [_]
               (initialize-mcp-gateway! app resolved-config)))
      (.catch (fn [err]
                (app-log-error! app "Background startup services failed" err)))))

(defn register-app-routes!
  [runtime app config lounge-messages*]
  (let [resolved-config (runtime-models/enrich-config (if (map? config) config (runtime-config/cfg)))]
    (ensure-settings! resolved-config)
    (reset! runtime-state/config* resolved-config)
    (reset! runtime-state/runtime* runtime)
    (app-routes/register-routes! runtime app resolved-config lounge-messages*)
    (-> (prewarm-sdk-runtime! runtime app resolved-config)
        (.then (fn [_]
                 (start-background-services! app resolved-config)
                 (clj->js resolved-config))))))

(defn start!
  [runtime]
  (when-not @server*
    (let [config (runtime-models/enrich-config (runtime-config/cfg))
          app (Fastify #js {:logger #js {:stream (.-stderr js/process)}})]
      (reset! runtime-state/config* config)
      (reset! runtime-state/runtime* runtime)
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
                   ;; Bring Redis up before any event-agent scheduling so we can
                   ;; honor persisted control config (and avoid crash loops).
                   (-> (redis/init-redis! (:redis-url config))
                       (.then (fn [redis-client]
                                (when redis-client
                                  (app-log-info! app "Redis client initialized")
                                  (contracts-routes/sync-contract-index! config))
                                nil))
                       (.catch (fn [err]
                                 (app-log-error! app "Failed to initialize Redis" err)
                                 nil))
                       (.then (fn [_]
                                ;; Start generic event-agent runtime
                                (events-runtime/start! config)
                                (contracts-routes/start-contract-watcher! config)
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

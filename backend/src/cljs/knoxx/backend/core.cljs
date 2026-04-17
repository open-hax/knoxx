(ns knoxx.backend.core
  (:require [knoxx.backend.agent-hydration :refer [ensure-settings!]]
            [knoxx.backend.agent-turns :as agent-turns :refer [recover-active-agent-sessions! lounge-messages*]]
            [knoxx.backend.app-routes :as app-routes]
            [knoxx.backend.event-agents :as event-agents]
            [knoxx.backend.mcp-bridge :as mcp]
            [knoxx.backend.realtime :as realtime]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.session-recovery :as session-recovery]
            [knoxx.backend.run-state :refer [active-runs-count]]
            [knoxx.backend.runtime-config :as runtime-config :refer [cfg]]
            [knoxx.backend.session-titles :refer [load-session-titles!]]))

(defonce server* (atom nil))

(defn register-ws-routes!
  [runtime app]
  (realtime/register-ws-routes! runtime app active-runs-count lounge-messages*))

(defn config-js
  []
  (clj->js (cfg)))

(defn register-app-routes!
  [runtime app config lounge-messages*]
  (ensure-settings! config)
  (reset! runtime-config/config* config)
  (app-routes/register-routes! runtime app config lounge-messages*)
  (event-agents/start! config)
  ;; Initialize MCP gateway if enabled
  (when (:mcp-enabled config)
    (-> (mcp/initialize!)
        (.then (fn [_]
                 (.log.info app (str "MCP gateway initialized: " (count (mcp/catalog)) " tools available"))))
        (.catch (fn [err]
                  (.log.error app "MCP gateway initialization failed" err)))))
  ;; Async bootstrap: connect Redis (session persistence) and start recovery loop.
  (-> (session-recovery/start! runtime app config)
      (.then (fn [_]
               (clj->js config)))))

(defn start!
  [runtime]
  (when-not @server*
    (let [config (cfg)
          Fastify (aget runtime "Fastify")
          fastify-cors (aget runtime "fastifyCors")
          fastify-multipart (aget runtime "fastifyMultipart")
          app (Fastify #js {:logger true})]
      (reset! runtime-config/config* config)
      (ensure-settings! config)
      (let [redis-startup (-> (redis/init-redis! (:redis-url config))
                              (.then (fn [redis-client]
                                       (if redis-client
                                         (do
                                           (.log.info app "Redis client initialized for session persistence")
                                           (-> (recover-active-agent-sessions! runtime config redis-client)
                                               (.then (fn [results]
                                                        (let [resumed (count (filter :resumed results))]
                                                          (when (seq results)
                                                            (.log.info app (str "Recovered " (count results) " active sessions from Redis; resumed " resumed))))
                                                        nil))))
                                         nil)))
                              (.catch (fn [err]
                                        (.log.error app "Failed to initialize Redis-backed session recovery" err)
                                        nil)))]
        (-> redis-startup
            (.then (fn []
                     (load-session-titles! runtime config)))
            (.then (fn []
                     (.register app fastify-cors #js {:origin true})))
            (.then (fn []
                     (.register app fastify-multipart)))
            (.then (fn []
                     (.register app (aget runtime "fastifyWebsocket"))))
            (.then (fn []
                     (.register app
                                (fn [instance _opts done]
                                  (register-ws-routes! runtime instance)
                                  (done)))))
            (.then (fn []
                     (app-routes/register-routes! runtime app config lounge-messages*)
                     ;; Start generic event-agent runtime
                     (event-agents/start! config)
                     (.listen app #js {:host (:host config)
                                       :port (:port config)})))
            (.then (fn [_]
                     (reset! server* app)
                     (.log.info app (str "Knoxx backend CLJS listening on " (:host config) ":" (:port config)))))
            (.catch (fn [err]
                      (.error js/console "Knoxx backend CLJS failed to start" err)
                      (js/process.exit 1))))))))

;; Handle graceful shutdown
(.on js/process "SIGINT" (fn []
                           (println "\nShutting down...")
                           (event-agents/stop!)
                           (js/process.exit 0)))

(.on js/process "SIGTERM" (fn []
                            (println "\nShutting down...")
                            (event-agents/stop!)
                            (js/process.exit 0)))

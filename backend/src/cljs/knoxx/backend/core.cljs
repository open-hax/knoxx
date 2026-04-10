(ns knoxx.backend.core
  (:require [knoxx.backend.agent-hydration :refer [ensure-settings!]]
            [knoxx.backend.agent-turns :as agent-turns :refer [recover-active-agent-sessions! lounge-messages*]]
            [knoxx.backend.app-routes :as app-routes]
            [knoxx.backend.realtime :as realtime]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.run-state :refer [active-runs-count]]
            [knoxx.backend.runtime-config :refer [cfg]]
            [knoxx.backend.session-titles :refer [load-session-titles!]]))

(defonce server* (atom nil))

(defn register-ws-routes!
  [runtime app]
  (realtime/register-ws-routes! runtime app active-runs-count lounge-messages*))

(defn config-js
  []
  (clj->js (cfg)))

(defn register-app-routes!
  [runtime app]
  (let [config (cfg)]
    (ensure-settings! config)
    (app-routes/register-routes! runtime app config lounge-messages*)
    (clj->js config)))

(defn init-redis-and-recover!
  "Initialize Redis client and kick off background recovery work.
   Call this from server.mjs before starting the HTTP server.
   Recovery and session-title preload run asynchronously so backend startup
   is not blocked on Redis/OpenPlanner availability or long resume work."
  [runtime]
  (let [config (cfg)]
    (-> (redis/init-redis! (:redis-url config))
        (.then (fn [redis-client]
                 (if redis-client
                   (do
                     (js/console.log "[knoxx] Redis client initialized for session persistence")
                     (-> (recover-active-agent-sessions! runtime config redis-client)
                         (.then (fn [results]
                                  (let [resumed (count (filter :resumed results))]
                                    (when (seq results)
                                      (js/console.log
                                       (str "[knoxx] Recovered " (count results) " active sessions from Redis; resumed " resumed))))))
                         (.catch (fn [err]
                                   (js/console.error "[knoxx] Failed to recover active sessions:" err)
                                   nil)))
                     redis-client)
                   (do
                     (js/console.warn "[knoxx] Redis not available, session persistence disabled")
                     nil))))
        (.catch (fn [err]
                  (js/console.error "[knoxx] Failed to initialize Redis:" err)
                  nil))
        (.then (fn [result]
                 (-> (load-session-titles! runtime config)
                     (.catch (fn [err]
                               (.warn js/console "Failed to preload session titles during startup" err)
                               nil)))
                 result)))))

(defn start!
  [runtime]
  (when-not @server*
    (let [config (cfg)
          Fastify (aget runtime "Fastify")
          fastify-cors (aget runtime "fastifyCors")
          app (Fastify #js {:logger true})]
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
                     (.register app (aget runtime "fastifyWebsocket"))))
            (.then (fn []
                     (.register app
                                (fn [instance _opts done]
                                  (register-ws-routes! runtime instance)
                                  (done)))))
            (.then (fn []
                     (app-routes/register-routes! runtime app config lounge-messages*)
                     (.listen app #js {:host (:host config)
                                       :port (:port config)})))
            (.then (fn [_]
                     (reset! server* app)
                     (.log.info app (str "Knoxx backend CLJS listening on " (:host config) ":" (:port config)))))
            (.catch (fn [err]
                      (.error js/console "Knoxx backend CLJS failed to start" err)
                      (js/process.exit 1))))))))

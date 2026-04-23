(ns knoxx.backend.core
  (:require [knoxx.backend.agent-hydration :refer [ensure-settings!]]
            [knoxx.backend.agent-turns :as agent-turns :refer [recover-active-agent-sessions! lounge-messages*]]
            [knoxx.backend.app-routes :as app-routes]
            [knoxx.backend.contracts-routes :as contracts-routes]
            [knoxx.backend.event-agents :as event-agents]
            [knoxx.backend.mcp-bridge :as mcp]
            [knoxx.backend.discord-gateway :as discord-gateway]
            [knoxx.backend.realtime :as realtime]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.session-recovery :as session-recovery]
            [knoxx.backend.run-state :refer [active-runs-count]]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.models :as runtime-models]
            [knoxx.backend.runtime.state :as runtime-state]
            [knoxx.backend.session-titles :refer [load-session-titles!]]))

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
  (-> (js/Promise.resolve nil)
      (.then (fn [_]
               (event-agents/start! resolved-config)))
      (.then (fn [_]
               (initialize-mcp-gateway! app resolved-config)))
      (.catch (fn [err]
                (app-log-error! app "Background startup services failed" err)))))

(defn register-app-routes!
  [runtime app config lounge-messages*]
  (let [resolved-config (runtime-models/enrich-config (if (map? config) config (runtime-config/cfg)))]
    (ensure-settings! resolved-config)
    (reset! runtime-state/config* resolved-config)
    (app-routes/register-routes! runtime app resolved-config lounge-messages*)
    (-> (session-recovery/start! runtime app resolved-config)
        (.then (fn [_]
                 (start-background-services! app resolved-config)
                 (clj->js resolved-config))))))

(defn start!
  [runtime]
  (when-not @server*
    (let [config (runtime-models/enrich-config (runtime-config/cfg))
          Fastify (aget runtime "Fastify")
          fastify-cors (aget runtime "fastifyCors")
          fastify-multipart (aget runtime "fastifyMultipart")
          app (Fastify #js {:logger true})]
      (reset! runtime-state/config* config)
      (ensure-settings! config)
      (let [redis-startup (-> (redis/init-redis! (:redis-url config))
                              (.then (fn [redis-client]
                                       (if redis-client
                                         (do
                                           (app-log-info! app "Redis client initialized for session persistence")
                                           (-> (recover-active-agent-sessions! runtime config redis-client)
                                               (.then (fn [results]
                                                        (let [resumed (count (filter :resumed results))]
                                                          (when (seq results)
                                                            (app-log-info! app (str "Recovered " (count results) " active sessions from Redis; resumed " resumed))))
                                                        nil))))
                                         nil)))
                              (.catch (fn [err]
                                        (app-log-error! app "Failed to initialize Redis-backed session recovery" err)
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
                     ;; Sync filesystem contracts → Redis index (write-through cache).
                     (contracts-routes/sync-contract-index! config)
                     (app-listen! app (:host config) (:port config))))
            (.then (fn [_]
                     (reset! server* app)
                     (app-log-info! app (str "Knoxx backend CLJS listening on " (:host config) ":" (:port config)))))
            (.catch (fn [err]
                      (.error js/console "Knoxx backend CLJS failed to start" err)
                      (js/process.exit 1))))))))

;; Handle graceful shutdown

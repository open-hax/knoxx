(ns knoxx.backend.graceful-shutdown
  "Graceful shutdown orchestration for PM2/system restarts.

   Goals:
   - stop accepting new work quickly
   - allow inflight HTTP requests and active turns a bounded window to settle
   - persist any still-running sessions into a resumable Redis state
   - release timers/sockets so PM2 can restart cleanly"
  (:require [clojure.string :as str]
            [knoxx.backend.agent-resume :as agent-resume]
            [knoxx.backend.discord-gateway :as discord-gateway]
            [knoxx.backend.event-agents :as event-agents]
            [knoxx.backend.realtime :as realtime]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.turn-control :as turn-control]))

(defonce shutdown-state* (atom {:installed? false
                                :in-progress? false
                                :promise nil
                                :signal nil}))

(defn- log-info!
  [app message]
  (if-let [logger (some-> app (.-log))]
    (.info logger message)
    (.log js/console message)))

(defn- log-warn!
  [app message]
  (if-let [logger (some-> app (.-log))]
    (.warn logger message)
    (.warn js/console message)))

(defn- log-error!
  [app message err]
  (if-let [logger (some-> app (.-log))]
    (.error logger message err)
    (.error js/console message err)))

(defn begin-shutdown!
  [app config signal]
  (if-let [existing (:promise @shutdown-state*)]
    existing
    (let [signal (str (or signal "shutdown"))
          close-server! (fn []
                          (try
                            (let [result (.close app)]
                              (if (some? result)
                                result
                                (js/Promise.resolve true)))
                            (catch :default err
                              (log-error! app "[shutdown] failed to close Fastify cleanly" err)
                              (js/Promise.resolve false))))
          shutdown-promise
          (-> (js/Promise.resolve nil)
              (.then (fn [_]
                       (swap! shutdown-state* assoc :in-progress? true :signal signal)
                       (log-info! app (str "[shutdown] received " signal "; draining Knoxx"))
                       (agent-resume/stop-periodic-recovery!)
                       (event-agents/stop!)
                       (discord-gateway/stop!)
                       (realtime/stop!)))
              (.then (fn [_]
                       (.all js/Promise #js [(close-server!)
                                             (agent-resume/wait-for-turns-and-flush! app config)])))
              (.then (fn [parts]
                       (let [drain-result (aget parts 1)]
                         (if (aget drain-result "timed_out")
                           (let [active-turns (turn-control/active-turn-entries)]
                             (-> (agent-resume/mark-sessions-resumable! (redis/get-client) active-turns signal)
                                 (.then (fn [count]
                                          (log-warn! app (str "[shutdown] marked " count " active session(s) resumable for restart"))
                                          #js {:count count}))))
                           (js/Promise.resolve #js {:count 0})))))
              (.then (fn [_]
                       (when-let [client (redis/get-client)]
                         (redis/quit client))))
              (.then (fn [_]
                       (log-info! app "[shutdown] graceful shutdown complete")
                       (js/process.exit 0)))
              (.catch (fn [err]
                        (log-error! app "[shutdown] graceful shutdown failed" err)
                        (js/process.exit 1))))]
      (swap! shutdown-state* assoc :promise shutdown-promise)
      shutdown-promise)))

(defn install!
  [app config]
  (when-not (:installed? @shutdown-state*)
    (swap! shutdown-state* assoc :installed? true)
    (.on js/process "SIGINT" (fn [] (begin-shutdown! app config "SIGINT")))
    (.on js/process "SIGTERM" (fn [] (begin-shutdown! app config "SIGTERM")))
    (.on js/process "message"
         (fn [message]
           (when (= (str message) "shutdown")
             (begin-shutdown! app config "pm2:shutdown"))))
    true))

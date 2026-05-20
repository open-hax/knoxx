(ns knoxx.backend.infra.graceful-shutdown
  "Graceful shutdown orchestration for PM2/system restarts.

   Goals:
   - stop accepting new work quickly
   - allow inflight HTTP requests and active turns a bounded window to settle
   - persist any still-running sessions into a resumable Redis state
   - release timers/sockets so PM2 can restart cleanly"
  (:require [knoxx.backend.infra.agent.resume :as agent-resume]
            [knoxx.backend.domain.discord.gateway :as discord-gateway]
            [knoxx.backend.infra.trigger-runner :as trigger-runtime]
            [knoxx.backend.domain.realtime :as realtime]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.runtime.state :as runtime-state]
            [knoxx.backend.infra.svg-render :as svg-render]
            [knoxx.backend.domain.voice.turn-control :as turn-control]))

(defonce shutdown-state* (atom {:installed? false
                                :in-progress? false
                                :promise nil
                                :signal nil}))

(defonce shutdown-target* (atom {:app nil
                                 :config nil}))

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
                        (trigger-runtime/stop!)
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
                        (.all js/Promise
                              (clj->js
                               [(svg-render/shutdown!)
                                (when-let [client (redis/get-client)]
                                  (redis/quit client))
                                (when-let [policy-db (runtime-state/current-policy-db)]
                                  (when-let [close-fn (aget policy-db "close")]
                                    (close-fn)))]))))
               (.then (fn [_]
                        (log-info! app "[shutdown] graceful shutdown complete")
                        (js/process.exit 0)))
              (.catch (fn [err]
                        (log-error! app "[shutdown] graceful shutdown failed" err)
                        (js/process.exit 1))))]
      (swap! shutdown-state* assoc :promise shutdown-promise)
      shutdown-promise)))

(defn- begin-current-shutdown!
  [signal]
  (let [{:keys [app config]} @shutdown-target*]
    (if app
      (begin-shutdown! app config signal)
      (do
        (.warn js/console "[shutdown] no active HTTP app; exiting")
        (js/process.exit 0)))))

(defn install!
  [app config]
  ;; Hot reload recreates the Fastify app without recreating the process. Keep
  ;; process signal handlers stable, but always point them at the latest app.
  (reset! shutdown-target* {:app app :config config})
  (when-not (:installed? @shutdown-state*)
    (swap! shutdown-state* assoc :installed? true)
    (.on js/process "SIGINT" (fn [] (begin-current-shutdown! "SIGINT")))
    (.on js/process "SIGTERM" (fn [] (begin-current-shutdown! "SIGTERM")))
    (.on js/process "message"
         (fn [message]
           (when (= (str message) "shutdown")
             (begin-current-shutdown! "pm2:shutdown"))))
    true))

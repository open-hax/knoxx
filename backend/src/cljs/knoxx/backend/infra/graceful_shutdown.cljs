(ns knoxx.backend.infra.graceful-shutdown
  "Graceful shutdown orchestration for PM2/system restarts.

   Goals:
   - stop accepting new work quickly
   - allow inflight HTTP requests and active turns a bounded window to settle
   - persist any still-running sessions into a resumable Mongo state
   - release timers/sockets so PM2 can restart cleanly"
  (:require [knoxx.backend.infra.agent.resume :as agent-resume]
            [knoxx.backend.domain.discord.gateway :as discord-gateway]
            [knoxx.backend.infra.event-runtime :as event-runtime]
            [knoxx.backend.domain.realtime :as realtime]
            [knoxx.backend.infra.db.policy :as policy-db]
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

(defn- ^:async close-server!
  [app]
  (try
    (let [result (.close app)]
      (if (some? result)
        (await result)
        true))
    (catch :default err
      (log-error! app "[shutdown] failed to close Fastify cleanly" err)
      false)))

(defn- ^:async run-shutdown!
  [app config signal]
  (try
    (swap! shutdown-state* assoc :in-progress? true :signal signal)
    (log-info! app (str "[shutdown] received " signal "; draining Knoxx"))
    (agent-resume/stop-periodic-recovery!)
    (event-runtime/stop!)
    (discord-gateway/stop!)
    (realtime/stop!)
    (let [parts (await (.all js/Promise #js [(close-server! app)
                                             (agent-resume/wait-for-turns-and-flush! app config)]))
          drain-result (aget parts 1)]
      (when (aget drain-result "timed_out")
        (let [active-turns (turn-control/active-turn-entries)
              count (await (agent-resume/mark-sessions-resumable! active-turns signal))]
          (log-warn! app (str "[shutdown] marked " count " active session(s) resumable for restart")))))
    (await (.all js/Promise
                 (clj->js
                  [(svg-render/shutdown!)
                   (when-let [policy-context (runtime-state/current-policy-db)]
                     (policy-db/close! policy-context))])))
    (log-info! app "[shutdown] graceful shutdown complete")
    (js/process.exit 0)
    (catch :default err
      (log-error! app "[shutdown] graceful shutdown failed" err)
      (js/process.exit 1))))

(defn begin-shutdown!
  [app config signal]
  (if-let [existing (:promise @shutdown-state*)]
    existing
    (let [signal (str (or signal "shutdown"))
          shutdown-promise (run-shutdown! app config signal)]
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

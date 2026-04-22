(ns knoxx.backend.graceful-shutdown
  "Graceful shutdown orchestration for PM2/system restarts.

   Goals:
   - stop accepting new work quickly
   - allow inflight HTTP requests and active turns a bounded window to settle
   - persist any still-running sessions into a resumable Redis state
   - release timers/sockets so PM2 can restart cleanly"
  (:require [clojure.string :as str]
            [knoxx.backend.discord-gateway :as discord-gateway]
            [knoxx.backend.event-agents :as event-agents]
            [knoxx.backend.realtime :as realtime]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.session-recovery :as session-recovery]
            [knoxx.backend.session-store :as session-store]
            [knoxx.backend.turn-control :as turn-control]
            [knoxx.backend.util.time :refer [now-iso]]))

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

(defn- now-ms
  []
  (.now js/Date))

(defn- shutdown-grace-ms
  [config]
  (let [value (:shutdown-grace-ms config)]
    (if (and (number? value) (pos? value))
      value
      25000)))

(defn- shutdown-poll-ms
  [config]
  (let [value (:shutdown-poll-ms config)]
    (if (and (number? value) (pos? value))
      value
      250)))

(defn- wait-for-active-turns!
  [app config]
  (let [deadline (+ (now-ms) (shutdown-grace-ms config))
        poll-ms (shutdown-poll-ms config)]
    (js/Promise.
     (fn [resolve]
       (letfn [(poll []
                 (let [remaining (turn-control/active-turn-count)]
                   (cond
                     (zero? remaining)
                     (resolve #js {:timed_out false :remaining 0})

                     (>= (now-ms) deadline)
                     (resolve #js {:timed_out true :remaining remaining})

                     :else
                     (js/setTimeout poll poll-ms))))]
         (let [initial (turn-control/active-turn-count)]
           (when (pos? initial)
             (log-info! app (str "[shutdown] waiting for " initial " active turn(s) to settle")))
           (poll)))))))

(defn- mark-active-turn-sessions-resumable!
  [app signal]
  (let [client (redis/get-client)
        active-turns (turn-control/active-turn-entries)
        stamp (now-iso)]
    (cond
      (nil? client)
      (js/Promise.resolve #js {:count 0 :skipped true :reason "redis_not_connected"})

      (empty? active-turns)
      (js/Promise.resolve #js {:count 0})

      :else
      (let [updates (mapv (fn [{:keys [session_id conversation_id run_id]}]
                            (if (str/blank? (str (or session_id "")))
                              (js/Promise.resolve nil)
                              (session-store/update-session! client session_id
                                                             {:status "running"
                                                              :conversation_id conversation_id
                                                              :run_id run_id
                                                              :has_active_stream false
                                                              :shutdown_requested_at stamp
                                                              :shutdown_signal signal})))
                          active-turns)]
        (-> (.all js/Promise (clj->js updates))
            (.then (fn [_]
                     (log-warn! app (str "[shutdown] marked " (count active-turns) " active session(s) resumable for restart"))
                     #js {:count (count active-turns)})))))))

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
                       (session-recovery/stop!)
                       (event-agents/stop!)
                       (discord-gateway/stop!)
                       (realtime/stop!)))
              (.then (fn [_]
                       (.all js/Promise #js [(close-server!)
                                             (wait-for-active-turns! app config)])))
              (.then (fn [parts]
                       (let [drain-result (aget parts 1)]
                         (if (aget drain-result "timed_out")
                           (mark-active-turn-sessions-resumable! app signal)
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

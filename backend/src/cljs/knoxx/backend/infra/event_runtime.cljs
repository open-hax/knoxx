(ns knoxx.backend.infra.event-runtime
  "Composition shell for the event runtime.

   Trigger and schedule domains remain separate. This namespace only starts,
   stops, and reloads the two runtimes together for process lifecycle needs."
  (:require [knoxx.backend.domain.error-observatory :as errors]
            [knoxx.backend.domain.models :as models]
            [knoxx.backend.domain.schedule.runtime :as schedule-runtime]
            [knoxx.backend.domain.source.runtime :as source-runtime]
            [knoxx.backend.domain.trigger.runtime :as trigger-runtime]
            [knoxx.backend.infra.config :as runtime-config]))

(defonce running?* (atom false))
(defonce reload-timer* (atom nil))

(defn- cfg
  []
  (models/enrich-config (runtime-config/cfg)))

(defn disabled?
  "True when this process is forbidden from running event runtimes.

   Read here rather than at each call site on purpose. There are five ways to
   reach `start!` — `infra.core/start!`, the background-services boot step, the
   `POST /api/admin/config/events/runtime/start` route, the deprecated
   `trigger-runner` facade, and the hot-reload `after-load` hook — and a guard
   placed at one of them is a guard the other four walk past. The promise the
   flag makes is about the process, so it is enforced where the process state
   actually changes."
  ([] (disabled? (cfg)))
  ([config] (true? (:event-runtimes-disabled? config))))

(defn start!
  ([]
   (start! (cfg)))
  ([config]
   (cond
     (disabled? config)
     (do (js/console.warn
          "[event-runtimes] refusing to start — KNOXX_DISABLE_EVENT_RUNTIMES is set")
         :disabled)

     @running?* :already-running

     :else
     (do (reset! running?* true)
         (trigger-runtime/start! config)
         (schedule-runtime/start! config)
         (errors/observe-promise! :event-runtime/source-start
                                  {}
                                  (source-runtime/start! config))
         :started))))

(defn stop!
  []
  (source-runtime/stop!)
  (schedule-runtime/stop!)
  (trigger-runtime/stop!)
  (reset! running?* false))

(defn reload!
  "Stop and restart the event runtimes, reporting what actually happened.

   Refuses outright when the process is flagged. Returning a hardcoded
   `{:ok true}` here was how the reset route came to report success while
   `start!` had declined to arm anything — the outcome was computed correctly
   one level down and then discarded. It also short-circuits before `stop!`,
   because a reload that cannot start must not tear down first."
  ([]
   (reload! (cfg)))
  ([config]
   (if (disabled? config)
     (do (js/console.warn
          "[event-runtimes] refusing to reload — KNOXX_DISABLE_EVENT_RUNTIMES is set")
         (js/Promise.resolve {:ok false :action "reload" :status :disabled}))
     (do (stop!)
         (js/Promise.resolve {:ok true :action "reload" :status (start! config)})))))

(defn debounced-reload!
  []
  (when-let [timer @reload-timer*]
    (js/clearTimeout timer))
  (reset! reload-timer*
          (js/setTimeout
           (fn []
             (reset! reload-timer* nil)
             (reload!))
           350)))

(defn reset-runtime!
  ([]
   (reset-runtime! (cfg)))
  ([config]
   (reload! config)))

(defn fire-trigger!
  ([trigger-id]
   (trigger-runtime/fire! (cfg) trigger-id))
  ([config trigger-id]
   (trigger-runtime/fire! config trigger-id)))

(defn fire!
  ([trigger-id]
   (fire-trigger! trigger-id))
  ([trigger-id payload]
   (trigger-runtime/fire! (cfg) trigger-id payload)))

(defn status
  ([]
   (status (cfg)))
  ([config]
   {:running @running?*
    :triggers (trigger-runtime/status config)
    :schedules (schedule-runtime/status config)
    :sources (source-runtime/status)}))

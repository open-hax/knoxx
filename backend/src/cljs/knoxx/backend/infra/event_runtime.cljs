(ns knoxx.backend.infra.event-runtime
  "Composition shell for the event runtime.

   Trigger and schedule domains remain separate. This namespace only starts,
   stops, and reloads the two runtimes together for process lifecycle needs."
  (:require [knoxx.backend.domain.models :as models]
            [knoxx.backend.domain.schedule.runtime :as schedule-runtime]
            [knoxx.backend.domain.source.runtime :as source-runtime]
            [knoxx.backend.domain.trigger.runtime :as trigger-runtime]
            [knoxx.backend.infra.config :as runtime-config]))

(defonce running?* (atom false))
(defonce reload-timer* (atom nil))

(defn- cfg
  []
  (models/enrich-config (runtime-config/cfg)))

(defn start!
  ([]
   (start! (cfg)))
  ([config]
   (when-not @running?*
     (reset! running?* true)
     (trigger-runtime/start! config)
     (schedule-runtime/start! config)
     (source-runtime/start! config))))

(defn stop!
  []
  (source-runtime/stop!)
  (schedule-runtime/stop!)
  (trigger-runtime/stop!)
  (reset! running?* false))

(defn reload!
  ([]
   (reload! (cfg)))
  ([config]
   (stop!)
   (start! config)
   (js/Promise.resolve {:ok true :action "reload"})))

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

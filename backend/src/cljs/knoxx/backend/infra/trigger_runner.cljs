(ns knoxx.backend.infra.trigger-runner
  "Deprecated compatibility facade for the event runtime.

   New code should require knoxx.backend.infra.event-runtime plus the specific
   schedule/trigger domain namespace it needs. This file intentionally contains
   no schedule or trigger logic."
  (:require [knoxx.backend.infra.event-runtime :as event-runtime]))

(defn start!
  ([] (event-runtime/start!))
  ([config] (event-runtime/start! config)))

(defn stop!
  []
  (event-runtime/stop!))

(defn reload!
  ([] (event-runtime/reload!))
  ([config] (event-runtime/reload! config)))

(defn debounced-reload!
  []
  (event-runtime/debounced-reload!))

(defn reset-runtime!
  ([] (event-runtime/reset-runtime!))
  ([config] (event-runtime/reset-runtime! config)))

(defn fire-trigger!
  [trigger-id]
  (event-runtime/fire-trigger! trigger-id))

(defn fire!
  ([trigger-id] (event-runtime/fire! trigger-id))
  ([trigger-id payload] (event-runtime/fire! trigger-id payload)))

(defn status
  []
  (event-runtime/status))

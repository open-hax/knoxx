(ns knoxx.backend.events.runtime
  "Preferred public surface for the generic events runtime.

   This namespace is a compatibility extraction layer over the legacy
   knoxx.backend.event-agents implementation while the deeper split proceeds."
  (:require [knoxx.backend.event-agents :as legacy-event-runtime]))

(defn status-snapshot
  [config]
  (legacy-event-runtime/status-snapshot config))

(defn start!
  [config]
  (legacy-event-runtime/start! config))

(defn stop!
  []
  (legacy-event-runtime/stop!))

(defn reload!
  []
  (legacy-event-runtime/reload!))

(defn debounced-reload!
  []
  (legacy-event-runtime/debounced-reload!))

(defn reset-runtime!
  [config]
  (legacy-event-runtime/reset-runtime! config))

(defn run-job!
  [job-id]
  (legacy-event-runtime/run-job! job-id))

(defn upsert-job!
  [job-id job-spec]
  (legacy-event-runtime/upsert-job! job-id job-spec))

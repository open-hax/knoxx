(ns knoxx.backend.events.dispatch
  "Preferred public surface for publishing normalized events.

   This is intentionally thin at first so call sites can stop depending on the
   legacy event-agents namespace before the internals are fully extracted."
  (:require [knoxx.backend.event-agents :as legacy-event-runtime]))

(defn dispatch!
  [event]
  (legacy-event-runtime/dispatch-event! event))

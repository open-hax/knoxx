(ns knoxx.backend.extern.schedule-timers
  "Own native interval handles for the schedule runtime.
   Handles are opaque outside this boundary.")

(defn every!
  "Create an interval after validating the callback and positive millisecond period."
  [tick interval-ms]
  (when-not (and (fn? tick) (number? interval-ms) (js/Number.isFinite interval-ms) (pos? interval-ms))
    (throw (ex-info "Invalid schedule interval" {:code :invalid-schedule-interval})))
  (js/setInterval tick interval-ms))

(defn cancel!
  "Cancel an opaque interval handle."
  [handle]
  (js/clearInterval handle))

(defn now-ms
  "Read the wall clock for inspectable schedule activation provenance."
  []
  (.now js/Date))

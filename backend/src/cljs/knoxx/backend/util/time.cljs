(ns knoxx.backend.util.time)

(defn now-iso
  []
  (.toISOString (js/Date.)))

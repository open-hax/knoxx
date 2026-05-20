(ns knoxx.backend.domain.time)

(defn now-iso
  []
  (.toISOString (js/Date.)))

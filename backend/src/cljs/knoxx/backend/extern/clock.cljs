(ns knoxx.backend.extern.clock
  "Host clock boundary for canonical UTC event timestamps.")

(defn instant-iso
  "Return the current instant in stable UTC ISO-8601 form."
  []
  (.toISOString (js/Date.)))

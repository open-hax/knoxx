(ns knoxx.backend.extern.local-openplanner
  "Host clock, identifiers and export encoding for the local OpenPlanner driver."
  (:require [knoxx.backend.law.local-openplanner :as law]))

(defn now! "Current UTC instant for an accepted operation." [] (.toISOString (js/Date.)))
(defn uuid! "A new stable operation or projection identifier." [] (str (random-uuid)))

(defn instant!
  "Normalize an input instant once, rejecting invalid clocks before admission."
  [value]
  (when-not (or (number? value) (string? value))
    (throw (ex-info "OpenPlanner timestamp must be a number or ISO string"
                    {:status 400 :code "openplanner_timestamp_invalid"})))
  (let [date (js/Date. value)]
    (when-not (js/Number.isFinite (.getTime date))
      (throw (ex-info "OpenPlanner timestamp is invalid"
                      {:status 400 :code "openplanner_timestamp_invalid"})))
    (.toISOString date)))

(defn normalize-event
  "Decode the canonical timestamp without inventing tenant or message content."
  [event]
  (law/assert-valid! law/Event (update event :ts instant!)))

(defn json-lines
  "Encode explicitly shaped SFT export records as newline-delimited JSON."
  [rows]
  (apply str (map #(str (js/JSON.stringify (clj->js %)) "\n") rows)))

(defn query-options
  "Decode query-string pagination exactly; malformed numbers are rejected."
  [opts]
  (reduce (fn [result key]
            (if-let [value (get result key)]
              (let [number (if (string? value) (js/Number value) value)]
                (when-not (and (js/Number.isSafeInteger number) (not (neg? number)))
                  (throw (ex-info "Invalid OpenPlanner query number" {:status 400 :field key})))
                (assoc result key number)) result))
          opts [:limit :offset :k]))

(ns knoxx.frontend.components.ops-status.logic
  "Pure logic for the sidebar ops status (port of the sparkline and
   sample handling in src/components/SidebarOpsStatus.tsx)."
  (:require [clojure.string :as str]))

(defn sparkline-path
  "SVG path string for a sparkline over `values` in a width×height box."
  [values width height]
  (if (empty? values)
    ""
    (let [max-v (apply max 1 values)
          min-v (apply min 0 values)
          value-range (max 1 (- max-v min-v))
          n (max 1 (dec (count values)))]
      (str/join " "
                (map-indexed
                 (fn [i v]
                   (let [x (* (/ i n) width)
                         y (- height (* (/ (- v min-v) value-range) height))]
                     (str (if (zero? i) "M" "L") " " (.toFixed x 1) " " (.toFixed y 1))))
                 values)))))

(defn stats->sample
  "Extracts a {:t :cpu :ram :gpu} sample from a ws stats payload."
  [^js payload t]
  (let [gpu (let [arr (.-gpu payload)]
              (if (and (array? arr) (pos? (.-length arr)))
                (js/Number (or (some-> (aget arr 0) .-util_gpu) 0))
                0))]
    {:t t
     :cpu (js/Number (or (.-cpu_percent payload) 0))
     :ram (js/Number (or (.-memory_percent payload) 0))
     :gpu gpu}))

(defn push-sample
  "Appends a sample, keeping the last 50."
  [samples sample]
  (vec (take-last 50 (conj (vec samples) sample))))

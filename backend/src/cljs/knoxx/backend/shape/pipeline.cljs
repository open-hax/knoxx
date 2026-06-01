(ns knoxx.backend.shape.pipeline
  "Pure shape helpers for pipeline contracts and runtime context maps."
  (:require [clojure.string :as str]))

(defn dependency-order
  "Return pipeline steps in dependency order.

   This preserves the existing contract used by the runner: steps with fewer
   declared dependencies run first, and otherwise retain the sort order provided
   by ClojureScript's stable sort for equal dependency counts."
  [steps]
  (->> steps
       (sort-by #(count (or (:step/depends-on %) [])))
       vec))

(defn interpolate-value
  "Replace {{memory.temp:k}} placeholders in a scalar value from a key/value map."
  [value k->v]
  (if (and (string? value) (str/includes? value "{{memory.temp:"))
    (reduce (fn [s k]
              (if-let [v (get k->v k)]
                (str/replace s (str "{{memory.temp:" k "}}") (str v))
                s))
            value
            (keys k->v))
    value))

(defn interpolate-map
  "Interpolate all {{memory.temp:}} placeholders in a nested map."
  [m k->v]
  (->> m
       (mapv (fn [[k v]]
               [k (if (map? v)
                    (interpolate-map v k->v)
                    (interpolate-value v k->v))]))
       (into {})))

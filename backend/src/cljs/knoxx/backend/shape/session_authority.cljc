(ns knoxx.backend.shape.session-authority
  "Stable data signatures for authority captured by provider tool closures."
  (:require [clojure.walk :as walk]))

(defn- compare-printed [left right]
  (compare (pr-str left) (pr-str right)))

(defn resource-policy-signature
  "Preserve policy values and sequence order while ignoring map/set insertion order."
  [policies]
  (pr-str
   (walk/postwalk
    (fn [value]
      (cond
        (map? value) (into (sorted-map-by compare-printed) value)
        (set? value) (into (sorted-set-by compare-printed) value)
        :else value))
    (or policies {}))))

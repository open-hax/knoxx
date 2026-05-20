(ns knoxx.backend.shape.parse
  (:require [clojure.string :as str]))

(defn parse-positive-int
  [value]
  (let [n (cond
            (string? value) (if (re-find #"\\." value)
                              js/NaN
                              (js/parseInt value 10))
            (number? value) value
            :else js/NaN)]
    (when (and (number? n)
               (not (js/isNaN n))
               (pos? n))
      n)))

(defn truthy-param?
  [value]
  (cond
    (true? value) true
    (number? value) (pos? value)
    (string? value) (contains? #{"1" "true" "yes" "on" "force"}
                                (str/lower-case (str/trim value)))
    :else false))

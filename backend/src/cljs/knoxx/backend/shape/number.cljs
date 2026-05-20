(ns knoxx.backend.shape.number)
(defn- positive-int-value
  [value]
  (let [n (js/Number value)]
    (when (and (not (js/isNaN n)) (pos? n))
      (js/Math.floor n))))

(defn- positive-int
  [value]
  (cond
    (number? value) (let [n (int value)]
                      (when (pos? n) n))
    (string? value) (let [parsed (js/parseInt value 10)]
                      (when (and (not (js/isNaN parsed)) (> parsed 0))
                        parsed))
    :else nil))

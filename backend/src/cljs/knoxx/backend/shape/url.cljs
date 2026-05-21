(defn resolve-media-url  [value]
  (cond
    (not (string? value)) value
    (str/starts-with? value "/") (str "http://127.0.0.1:8000" value)
    :else value))

(defn strip-data-url [raw-data]
  (when-let [data (content/nonblank raw-data)]
    (let [comma (.indexOf data ",")]
      (if (and (str/starts-with? data "data:") (>= comma 0))
        (.slice data (inc comma))
        data))))

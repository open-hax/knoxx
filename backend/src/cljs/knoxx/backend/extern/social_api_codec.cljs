(ns knoxx.backend.extern.social-api-codec
  "Own JSON and opaque tool values for the full platform API boundary.")

(defn json-value
  "Encode maps without prototype setters; keyword envelope keys become names."
  [value]
  (cond
    (keyword? value) (if-let [prefix (namespace value)] (str prefix "/" (name value)) (name value))
    (map? value) (reduce-kv (fn [object key item]
                              (aset object (if (keyword? key) (name key) key) (json-value item)) object)
                            (.create js/Object nil) value)
    (vector? value) (into-array (map json-value value))
    :else value))

(defn json-data
  "Decode actual JSON structure; a constructor field is ordinary data."
  [value]
  (cond
    (array? value) (mapv json-data (array-seq value))
    (or (nil? value) (string? value) (number? value) (boolean? value)) value
    :else (into {} (map (fn [key] [key (json-data (aget value key))]))
                (array-seq (.keys js/Object value)))))

(defn encode
  "Serialize a validated CLJS JSON value without losing arbitrary keys."
  [value]
  (.stringify js/JSON (json-value value)))

(defn decode
  "Decode JSON text with string keys throughout its value."
  [text]
  (json-data (.parse js/JSON text)))

(defn- envelope
  [fields]
  (into {} (map (fn [[key value]] [(if (keyword? key) key (keyword key)) value])) fields))

(defn parameters
  "Convert only tool-envelope and named-parameter row keys. The JSON body and
   scalar/array parameter values retain their original JSON representation."
  [value]
  (let [input (envelope (if (map? value) value (json-data value)))]
    (cond-> input
      (vector? (:parameters input)) (update :parameters #(mapv envelope %)))))

(defn tool-result
  "Preserve every JSON field in structured API results as well as rendered text."
  [text details error?]
  #js {:content #js [#js {:type "text" :text text}]
       :details (json-value details) :isError (boolean error?)})

(defn render
  "Render bounded JSON text; structured result details remain complete data."
  [value]
  (let [json (.stringify js/JSON (json-value value) nil 2) clipped? (> (count json) 20000)]
    {:truncated clipped?
     :text (if clipped? (str (subs json 0 20000) "\n[API view truncated; structured details retain the complete response.]") json)}))

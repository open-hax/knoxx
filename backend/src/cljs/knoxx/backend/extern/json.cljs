(ns knoxx.backend.extern.json
  "JSON/JS conversion boundary helpers.
   Non-extern namespaces should call these instead of js->clj/clj->js or
   js/JSON directly when crossing the JS data boundary.")

(defn to-cljs
  "Convert a JS value tree to CLJS data with keywordized map keys.
   Existing CLJS maps/vectors are returned in equivalent CLJS form."
  [value]
  (cond
    (nil? value) nil
    (map? value) value
    (vector? value) value
    :else (js->clj value :keywordize-keys true)))

(defn parse-object
  "Parse a JSON object string into a CLJS map. Returns nil for invalid JSON or
   non-object values. CLJS maps pass through unchanged."
  [value]
  (cond
    (map? value) value
    (string? value) (try
                      (let [parsed (js->clj (.parse js/JSON value) :keywordize-keys true)]
                        (when (map? parsed) parsed))
                      (catch :default _ nil))
    :else nil))

(defn stringify
  "Stringify a CLJS value for a JSON request body."
  [value]
  (.stringify js/JSON (clj->js value)))

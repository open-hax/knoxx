(ns knoxx.backend.extern.js
  "Tiny JS value-construction boundary.
   Prefer domain-specific extern functions; use this only when a JS object is
   required by an external API and no narrower wrapper exists yet.")

(defn object
  "Convert a CLJS map/vector tree to a plain JS value."
  [value]
  (clj->js value))

(defn empty-object
  "Return a fresh empty JS object."
  []
  #js {})

(defn js-array-seq
  "Return a seq for a native JS array, or an empty vector for non-arrays."
  [value]
  (if (array? value)
    (array-seq value)
    []))

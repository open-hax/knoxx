(ns knoxx.backend.extern.promise
  "JS Promise boundary helpers.
   Non-extern namespaces pass CLJS collections and receive JS Promises/values."
  (:refer-clojure :exclude [all]))

(defn all
  "Promise.all for a CLJS collection of promises. Returns a JS Promise whose
   resolution value is the native JS array produced by Promise.all."
  [promises]
  (.all js/Promise (clj->js (vec promises))))

(defn all-vec
  "Promise.all for a CLJS collection of promises. Resolves to a CLJS vector."
  [promises]
  (-> (all promises)
      (.then (fn [values]
               (if (array? values)
                 (vec (array-seq values))
                 [])))))

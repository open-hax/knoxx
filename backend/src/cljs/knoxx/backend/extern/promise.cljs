(ns knoxx.backend.extern.promise
  "JS Promise boundary helpers.
   Non-extern namespaces pass CLJS collections and receive JS Promises/values."
  (:refer-clojure :exclude [all]))

(defn all
  "Promise.all for a CLJS collection of promises. Returns a JS Promise whose
   resolution value is the native JS array produced by Promise.all."
  [promises]
  (.all js/Promise (clj->js (vec promises))))

(defn ^:async all-vec
  "Promise.all for a CLJS collection of promises. Resolves to a CLJS vector."
  [promises]
  (let [values (await (all promises))]
    (if (array? values)
      (vec (array-seq values))
      [])))

(defn reject-after
  "Return a Promise that rejects with an Error after timeout-ms."
  [timeout-ms message]
  (js/Promise.
   (fn [_resolve reject]
     (js/setTimeout #(reject (js/Error. message)) timeout-ms))))

(defn with-timeout-error
  "Mirror promise, rejecting with error after timeout-ms and clearing the timer
   whenever the provider settles first."
  [promise timeout-ms error]
  (js/Promise.
   (fn [resolve reject]
     (let [timer-id (js/setTimeout #(reject error) timeout-ms)]
       ((^:async fn []
          (try
            (let [value (await promise)]
              (js/clearTimeout timer-id)
              (resolve value))
            (catch :default provider-error
              (js/clearTimeout timer-id)
              (reject provider-error)))))))))

(defn race
  "Promise.race for a CLJS collection of promises."
  [promises]
  (.race js/Promise (clj->js (vec promises))))

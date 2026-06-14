(ns knoxx.frontend.lib.storage
  "Safe localStorage helpers with quota protection.
   CLJS port of src/lib/storage.ts. localStorage is resolved from
   js/globalThis at call time so node tests can inject a mock."
  (:require [clojure.string :as str]))

(defn- local-storage ^js []
  (.-localStorage js/globalThis))

(defn- all-keys [^js ls]
  (into [] (keep #(.key ls %)) (range (.-length ls))))

(defn safe-get-item
  "Returns the stored string, or nil if missing or storage throws."
  [key]
  (try
    (.getItem (local-storage) key)
    (catch :default _ nil)))

(defn safe-set-item
  "Stores `value` under `key`. On quota errors, evicts the oldest half of
   knoxx_-prefixed keys (never `key` itself) and retries once. Returns
   true when the value was stored, false otherwise."
  [key value]
  (let [ls (local-storage)]
    (try
      (.setItem ls key value)
      true
      (catch :default _
        (try
          (let [evictable (filterv #(and (str/starts-with? % "knoxx_")
                                         (not= % key))
                                   (all-keys ls))]
            (doseq [k (take (js/Math.floor (/ (count evictable) 2)) evictable)]
              (try (.removeItem ls k) (catch :default _ nil))))
          (.setItem ls key value)
          true
          (catch :default _ false))))))

(defn safe-remove-item
  "Removes `key`; never throws."
  [key]
  (try
    (.removeItem (local-storage) key)
    (catch :default _ nil))
  nil)

(defn clear-knoxx-storage
  "Removes every knoxx_-prefixed key."
  []
  (let [ls (local-storage)]
    (doseq [k (filterv #(str/starts-with? % "knoxx_") (all-keys ls))]
      (try (.removeItem ls k) (catch :default _ nil)))
    nil))

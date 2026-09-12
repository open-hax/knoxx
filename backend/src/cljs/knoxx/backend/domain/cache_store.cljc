(ns knoxx.backend.domain.cache-store
  "Replay laws for expiring cache values; every clock sample is admitted data."
  (:require [knoxx.backend.law.cache-store :as law]))

(defn visible-entry
  "Read through the recorded expiry, independent of physical cache cleanup."
  [state bucket cache-key at]
  (law/assert-key! bucket cache-key)
  (law/assert-clock! at)
  (let [entry (get state [bucket cache-key])]
    (when (and entry (> (:expires-at entry) at)) entry)))

(defn transition
  "Apply one accepted operation; counter windows retain their first expiry."
  [state {cache-key :key :keys [kind bucket value ttl-ms at] :as operation}]
  (law/assert-operation! operation)
  (let [id [bucket cache-key]]
    (case kind
      :delete {:state (dissoc state id) :result true}
      :put {:state (assoc state id {:value value :expires-at (+ at ttl-ms)}) :result true}
      :increment (let [before (visible-entry state bucket cache-key at)
                       _ (law/require! (or (nil? before) (and (integer? (:value before)) (< 0 (:value before) 9007199254740991)))
                                   "A rate counter must contain an integer")
                       counter (inc (or (:value before) 0))
                       next-entry {:value counter :expires-at (or (:expires-at before) (+ at ttl-ms))}]
                   {:state (assoc state id next-entry) :result counter})
      (throw (ex-info "Unsupported cache operation" {:status 400 :code "cache_operation_unknown"})))))

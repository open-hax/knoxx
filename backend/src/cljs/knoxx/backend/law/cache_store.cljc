(ns knoxx.backend.law.cache-store
  "Admission rules for portable cache keys, clocks and durations."
  (:require [clojure.string :as str]))

(defn require!
  "Refuse an invalid cache invocation before effects."
  [condition message]
  (when-not condition
    (throw (ex-info message {:status 400 :code "cache_store_invalid"}))))

(defn assert-key!
  "Validate one supported cache namespace and exact string cache-key."
  [bucket cache-key]
  (require! (and (contains? #{:titles :temp-memory :memory-sessions :rate-limits} bucket)
                 (string? cache-key) (not (str/blank? cache-key)))
            "A supported cache bucket and nonblank cache-key are required"))

(defn assert-clock!
  "Require an exact, portable epoch-millisecond sample."
  [at]
  (require! (and (integer? at) (<= 0 at 8640000000000000))
            "Cache clock must be nonnegative epoch milliseconds in the portable date range")
  at)

(defn assert-value-key!
  "Keep value reads separate from the atomic counter protocol."
  [bucket cache-key]
  (assert-key! bucket cache-key)
  (require! (not= :rate-limits bucket) "Rate-limit counters admit only atomic increments"))

(defn assert-operation!
  "Validate mutation kind, cache-key and an explicit TTL before admission."
  [{cache-key :key :keys [kind bucket ttl-ms at] :as operation}]
  (assert-key! bucket cache-key)
  (assert-clock! at)
  (require! (contains? #{:put :delete :increment} kind) "Unsupported cache operation")
  (require! (= (= :increment kind) (= :rate-limits bucket))
            "Rate-limit counters admit only atomic increments")
  (when (= :put kind) (require! (contains? operation :value) "Cache writes require a value"))
  (when (contains? #{:put :increment} kind)
    (require! (and (integer? ttl-ms) (pos? ttl-ms) (<= (+ at ttl-ms) 8640000000000000))
              "Cache TTL must produce a positive expiry in the portable date range"))
  operation)

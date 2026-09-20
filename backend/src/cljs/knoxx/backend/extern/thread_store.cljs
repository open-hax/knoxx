(ns knoxx.backend.extern.thread-store
  "Clock and UTC encoding boundary for finite thread admissions."
  (:require [knoxx.backend.extern.local-policy :as locks]
            [knoxx.backend.law.thread-store :as law]))

(defn now-ms "Sample the live view clock." [] (.now js/Date))
(defn cache-live?
  "Respect the provider's actual expiry; a cache read cannot extend its durable lifetime."
  [value now ttl-ms]
  (let [expires (:expiresAt value)
        expiry (cond (instance? js/Date expires) (.getTime expires)
                     (string? expires) (.parse js/Date expires)
                     (number? expires) expires
                     :else (+ (:cached-at value 0) ttl-ms))]
    (and (js/Number.isFinite expiry) (< now expiry))))
(defn with-lock! "Hold a distinct thread lock across the entire awaited transition." [file f]
  (locks/with-lock! file f))

(defn stamp
  "Encode one validated clock sample and the exact declared thread TTL."
  [thread-id at instance-id]
  (law/assert-valid! :thread/id law/NonBlank thread-id)
  (law/assert-valid! :thread/clock law/Milliseconds at)
  (let [expires (+ at (law/ttl-ms thread-id))]
    (law/assert-valid! :thread/stamp law/Stamp
                      {:at (.toISOString (js/Date. at)) :at-ms at
                       :expires-at (.toISOString (js/Date. expires)) :expires-ms expires
                       :instance-id instance-id})))

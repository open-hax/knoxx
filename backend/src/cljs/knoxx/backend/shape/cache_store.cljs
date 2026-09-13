(ns knoxx.backend.shape.cache-store
  "Provider contracts for expiring titles, temporary memory and session lists,
   plus a separate atomic fixed-window counter protocol. Durations are milliseconds.")

(defprotocol ICacheStore
  (read-value! [store bucket key] "Read a live entry, or nil after expiry/deletion.")
  (write-value! [store bucket key value ttl-ms] "Durably replace one value with an explicit TTL.")
  (delete-value! [store bucket key] "Durably delete one value; idempotent."))

(defprotocol IRateLimitStore
  (increment! [store key window-ms] "Increment atomically without extending an existing window."))

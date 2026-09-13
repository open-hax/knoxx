(ns knoxx.backend.infra.stores.cache-registry
  "Explicit composition of the expiring-value and atomic-counter provider."
  (:require [knoxx.backend.shape.cache-store :as protocol]))

(defonce provider (atom nil))

(defn install!
  "Select both cache protocols together, or explicitly clear the test registry."
  [store]
  (when-not (or (nil? store) (and (satisfies? protocol/ICacheStore store)
                                 (satisfies? protocol/IRateLimitStore store)))
    (throw (ex-info "Invalid cache provider" {:status 500 :code "cache_provider_invalid"})))
  (reset! provider store))

(defn current "Return the installed provider; never choose an implicit fallback." [] @provider)

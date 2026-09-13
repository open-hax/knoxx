(ns knoxx.backend.infra.stores.mongo-cache-store
  "Explicit Mongo implementation of the finite cache protocols."
  (:require [knoxx.backend.extern.mongo-cache :as native]
            [knoxx.backend.shape.cache-store :as protocol]))

(defrecord MongoCacheStore [db]
  protocol/ICacheStore
  (read-value! [_ bucket key] (native/read! db bucket key))
  (write-value! [_ bucket key value ttl] (native/write! db bucket key value ttl))
  (delete-value! [_ bucket key] (native/delete! db bucket key))
  protocol/IRateLimitStore
  (increment! [_ key ttl] (native/increment! db key ttl)))

(defn create-store "Wrap an explicit Mongo handle; never infer fallback after failure." [db]
  (when-not db (throw (ex-info "Mongo cache handle is required" {:status 503})))
  (->MongoCacheStore db))

(defn setup-indexes! "Await native cache key and expiry indexes." [db] (native/setup-indexes! db))

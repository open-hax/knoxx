(ns knoxx.backend.infra.cache-services
  "Named application cache selection, preserving explicit Mongo arities."
  (:require [knoxx.backend.infra.mongo-client :as mongo]
            [knoxx.backend.infra.stores.cache-registry :as registry]
            [knoxx.backend.infra.stores.mongo-cache-store :as mongo-cache]
            [knoxx.backend.shape.cache-store :as protocol]))

(defn provider
  "A supplied Mongo handle wins; nil selects the installed provider, with no failure fallback."
  [db]
  (or (when db (mongo-cache/create-store db))
      (registry/current)
      (when-let [handle (mongo/get-db)] (mongo-cache/create-store handle))
      (throw (ex-info "Cache persistence is not initialized" {:status 503 :code "cache_provider_unavailable"}))))

(defn read! "Read one named application cache value." [db bucket key]
  (protocol/read-value! (provider db) bucket key))
(defn write! "Await durable cache admission." [db bucket key value ttl-ms]
  (protocol/write-value! (provider db) bucket key value ttl-ms))
(defn delete! "Delete one named application cache value." [db bucket key]
  (protocol/delete-value! (provider db) bucket key))
(defn increment! "Increment the atomic fixed-window counter." [db key ttl-ms]
  (protocol/increment! (provider db) key ttl-ms))
(defn setup-indexes! "Retain the explicit Mongo cache initialization seam." [db]
  (mongo-cache/setup-indexes! db))

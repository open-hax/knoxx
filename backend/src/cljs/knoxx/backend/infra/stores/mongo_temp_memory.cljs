(ns knoxx.backend.infra.stores.mongo-temp-memory
  "Compatibility facade for the replaceable temp-memory cache."
  (:require [knoxx.backend.infra.cache-services :as cache]))

(def COLLECTION_NAME "knoxx_temp_memory")
(defn setup-indexes! "Initialize explicit Mongo indexes." [db] (cache/setup-indexes! db))

(defn get-memory!
  "Read the selected provider's unexpired value."
  ([key] (get-memory! nil key))
  ([db key] (cache/read! db :temp-memory key)))

(defn ^:async set-memory!
  "Admit temporary memory for an explicit duration in seconds."
  ([key value ttl-seconds] (await (set-memory! nil key value ttl-seconds)))
  ([db key value ttl-seconds]
   (await (cache/write! db :temp-memory key value (* (or ttl-seconds 3600) 1000)))
   {:key key :written true}))

(defn ^:async delete-memory!
  "Remove one entry through the selected provider."
  ([key] (await (delete-memory! nil key)))
  ([db key]
   (await (cache/delete! db :temp-memory key))
   {:key key :deleted true}))

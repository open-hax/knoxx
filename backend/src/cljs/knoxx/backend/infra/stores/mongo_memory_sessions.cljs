(ns knoxx.backend.infra.stores.mongo-memory-sessions
  "Compatibility facade for the replaceable memory-sessions cache."
  (:require [knoxx.backend.infra.cache-services :as cache]))

(def COLLECTION_NAME "knoxx_memory_threads")
(defn setup-indexes! "Initialize explicit Mongo indexes." [db] (cache/setup-indexes! db))

(defn get-cache-entry!
  "Read the selected provider's unexpired value."
  ([cache-key] (get-cache-entry! nil cache-key))
  ([db cache-key] (cache/read! db :memory-sessions cache-key)))

(defn ^:async set-cache-entry!
  "Admit a session-list cache entry without altering its public metadata."
  ([cache-key entry] (await (set-cache-entry! nil cache-key entry)))
  ([db cache-key entry]
   (await (cache/write! db :memory-sessions cache-key entry (* 10 1000)))
   entry))

(defn ^:async delete-cache-entry!
  "Remove one entry through the selected provider."
  ([cache-key] (await (delete-cache-entry! nil cache-key)))
  ([db cache-key]
   (await (cache/delete! db :memory-sessions cache-key))
   true))

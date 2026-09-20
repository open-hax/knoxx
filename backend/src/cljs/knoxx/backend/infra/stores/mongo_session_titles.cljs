(ns knoxx.backend.infra.stores.mongo-session-titles
  "Compatibility facade for the replaceable titles cache."
  (:require [knoxx.backend.infra.cache-services :as cache]))

(def COLLECTION_NAME "knoxx_thread_titles")
(defn setup-indexes! "Initialize explicit Mongo indexes." [db] (cache/setup-indexes! db))

(defn get-title!
  "Read the selected provider's unexpired value."
  ([session-id] (get-title! nil session-id))
  ([db session-id] (cache/read! db :titles session-id)))

(defn ^:async upsert-title!
  "Admit a title before returning its existing response shape."
  ([session-id entry] (await (upsert-title! nil session-id entry)))
  ([db session-id entry]
   (await (cache/write! db :titles session-id entry (* 604800 1000)))
   (assoc entry :session session-id)))

(defn ^:async delete-title!
  "Remove one entry through the selected provider."
  ([session-id] (await (delete-title! nil session-id)))
  ([db session-id]
   (await (cache/delete! db :titles session-id))
   true))

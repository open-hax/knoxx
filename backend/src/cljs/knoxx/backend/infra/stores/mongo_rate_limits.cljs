(ns knoxx.backend.infra.stores.mongo-rate-limits
  "Mongo twin for rate limiting (replaces Redis knoxx:chat-rate: keys).
   Uses TTL docs with $inc for atomic counter operations."
  (:require [knoxx.backend.infra.mongo-client :as mongo-client]
            [knoxx.backend.infra.system-instance :as system-instance]))

(def COLLECTION_NAME "knoxx_rate_limits")

(defonce ^:private increment-fn* (atom nil))

(defn set-increment-fn!
  "Set a custom increment function for testing."
  [f]
  (reset! increment-fn* f))

(defn- coll [db] (.collection db COLLECTION_NAME))

(defn ^:async setup-indexes!
  "Create required indexes. Idempotent."
  [db]
  (let [c (coll db)]
    (await (.createIndex c #js {"key" 1} #js {"unique" true}))
    (await (.createIndex c #js {"expiresAt" 1} #js {"expireAfterSeconds" 0}))
    true))

(defn ^:async increment-rate-limit!
  "Atomically increment the rate limit counter for a key.
   Returns the new count. Creates the document with TTL if it doesn't exist."
  ([key window-seconds] (increment-rate-limit! (mongo-client/get-db) key window-seconds))
  ([db key window-seconds]
   (if-let [f @increment-fn*]
     (f key window-seconds)
     (when (and db key)
       (let [c (coll db)
             now (js/Date.)
             expires-at (js/Date. (+ (.now js/Date) (* window-seconds 1000)))
             update-doc {"$inc" #js {"count" 1}
                         "$setOnInsert" #js {"created_at" now
                                             "system_instance_id" (system-instance/current-id)
                                             "expiresAt" expires-at}}
             opts #js {"upsert" true "returnDocument" "after"}
             result (await (.findOneAndUpdate c #js {"key" (str key)} (clj->js update-doc) opts))]
         (or (aget result "count") 1))))))

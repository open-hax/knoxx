(ns knoxx.backend.infra.stores.mongo-memory-sessions
  "Mongo twin for memory sessions cache (replaces Redis knoxx:memory:sessions:v1: keys).
   TTL-indexed documents with application-managed expiry."
  (:require [knoxx.backend.infra.mongo-client :as mongo-client]
            [knoxx.backend.infra.system-instance :as system-instance]))

(def COLLECTION_NAME "knoxx_memory_sessions")
(def ^:private DEFAULT_TTL_SECONDS 10) ;; 10 seconds, matching existing cache TTL

(defn- coll [db] (.collection db COLLECTION_NAME))

(defn ^:async setup-indexes!
  "Create required indexes. Idempotent."
  [db]
  (let [c (coll db)]
    (await (.createIndex c #js {"cache_key" 1} #js {"unique" true}))
    (await (.createIndex c #js {"expiresAt" 1} #js {"expireAfterSeconds" 0}))
    true))

(defn- keywordize [doc]
  (when doc (js->clj doc :keywordize-keys true)))

(defn ^:async get-cache-entry!
  "Read cached memory sessions entry. Returns keywordized map or nil."
  ([cache-key] (get-cache-entry! (mongo-client/get-db) cache-key))
  ([db cache-key]
   (when (and db cache-key)
     (let [c (coll db)
           result (await (.findOne c #js {"cache_key" (str cache-key)}))]
       (when result
         (let [doc (keywordize result)]
           (when (> (:expires-at doc 0) (.now js/Date))
             doc)))))))

(defn ^:async set-cache-entry!
  "Write memory sessions cache entry. Upserts by cache_key, refreshes TTL."
  ([cache-key entry] (set-cache-entry! (mongo-client/get-db) cache-key entry))
  ([db cache-key entry]
   (when (and db cache-key)
     (let [c (coll db)
           now (js/Date.)
           doc {:cache_key (str cache-key)
                :value (:value entry)
                :cached-at (:cached-at entry now)
                :expires-at (:expires-at entry (+ now DEFAULT_TTL_SECONDS))
                :created_at now
                :system_instance_id (system-instance/current-id)
                :expiresAt (js/Date. (+ (.now js/Date) (* DEFAULT_TTL_SECONDS 1000)))}]
       (await (.updateOne
               c
               #js {"cache_key" (str cache-key)}
               #js {"$set" (clj->js {:value (:value entry)
                                     :cached-at (:cached-at entry now)
                                     :expires-at (:expires-at entry (+ now DEFAULT_TTL_SECONDS))
                                     :expiresAt (:expiresAt doc)})
                    "$setOnInsert" (clj->js {:created_at now
                                            :system_instance_id (system-instance/current-id)})},
               #js {"upsert" true}))
       doc))))

(defn ^:async delete-cache-entry!
  "Remove memory sessions cache entry."
  ([cache-key] (delete-cache-entry! (mongo-client/get-db) cache-key))
  ([db cache-key]
   (when (and db cache-key)
     (let [c (coll db)]
       (await (.deleteOne c #js {"cache_key" (str cache-key)}))
       true))))

(ns knoxx.backend.infra.stores.mongo-temp-memory
  "Mongo twin for short-term agent memory (replaces Redis temp-mem: keys).
   TTL-indexed documents with application-managed expiry."
  (:require [knoxx.backend.infra.mongo-client :as mongo-client]
            [knoxx.backend.infra.system-instance :as system-instance]))

(def COLLECTION_NAME "knoxx_temp_memory")
(def ^:private DEFAULT_TTL_SECONDS (* 60 60)) ;; 1 hour

(defn- coll [db] (.collection db COLLECTION_NAME))

(defn ^:async setup-indexes!
  "Create required indexes. Idempotent."
  [db]
  (let [c (coll db)]
    (await (.createIndex c #js {"key" 1} #js {"unique" true}))
    (await (.createIndex c #js {"expiresAt" 1} #js {"expireAfterSeconds" 0}))
    true))

(defn- keywordize [doc]
  (when doc (js->clj doc :keywordize-keys true)))

(defn ^:async get-memory!
  "Read temp memory value. Returns keywordized map or nil."
  ([key] (get-memory! (mongo-client/get-db) key))
  ([db key]
   (when (and db key)
     (let [c (coll db)
           result (await (.findOne c #js {"key" (str key)}))]
       (when result
         (let [doc (keywordize result)]
           (when (> (:expires-at doc 0) (.now js/Date))
             (:value doc))))))))

(defn ^:async set-memory!
  "Write temp memory value with TTL. Upserts by key, refreshes TTL."
  ([key value ttl-seconds] (set-memory! (mongo-client/get-db) key value ttl-seconds))
  ([db key value ttl-seconds]
   (when (and db key)
     (let [c (coll db)
           now (js/Date.)
           ttl (or ttl-seconds DEFAULT_TTL_SECONDS)
           set-doc {:value value
                    :expiresAt (js/Date. (+ (.now js/Date) (* ttl 1000)))}
           insert-doc {:created_at now
                       :system_instance_id (system-instance/current-id)}]
       (await (.updateOne
               c
               #js {"key" (str key)}
               #js {"$set" (clj->js set-doc)
                    "$setOnInsert" (clj->js insert-doc)}
               #js {"upsert" true}))
       {:key key :written true}))))

(defn ^:async delete-memory!
  "Remove temp memory entry."
  ([key] (delete-memory! (mongo-client/get-db) key))
  ([db key]
   (when (and db key)
     (let [c (coll db)]
       (await (.deleteOne c #js {"key" (str key)}))
       {:key key :deleted true}))))

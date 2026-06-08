(ns knoxx.backend.infra.stores.mongo-session-titles
  "Mongo twin for thread title cache (replaces Redis knoxx:session-title: keys).
   TTL-indexed documents with application-managed expiry."
  (:require [knoxx.backend.infra.mongo-client :as mongo-client]
            [knoxx.backend.infra.system-instance :as system-instance]))

(def COLLECTION_NAME "knoxx_thread_titles")
(def ^:private DEFAULT_TTL_SECONDS (* 60 60 24 7)) ;; 7 days

(defn- coll [db] (.collection db COLLECTION_NAME))

(defn ^:async setup-indexes!
  "Create required indexes. Idempotent."
  [db]
  (let [c (coll db)]
    (await (.createIndex c #js {"session_id" 1} #js {"unique" true}))
    (await (.createIndex c #js {"expiresAt" 1} #js {"expireAfterSeconds" 0}))
    true))

(defn- keywordize [doc]
  (when doc (js->clj doc :keywordize-keys true)))

(defn ^:async get-title!
  "Read cached session title. Returns keywordized map or nil."
  ([session-id] (get-title! (mongo-client/get-db) session-id))
  ([db session-id]
   (when (and db session-id)
     (let [c (coll db)
           result (await (.findOne c #js {"session_id" (str session-id)}))]
       (keywordize result)))))

(defn ^:async upsert-title!
  "Write session title cache entry. Upserts by session_id, refreshes TTL."
  ([session-id entry] (upsert-title! (mongo-client/get-db) session-id entry))
  ([db session-id entry]
   (when (and db session-id)
     (let [c (coll db)
           now (js/Date.)
           doc {:session_id (str session-id)
                :title (:title entry)
                :title_model (:title_model entry)
                :updated_at (:updated_at entry now)
                :created_at now
                :system_instance_id (system-instance/current-id)
                :expiresAt (js/Date. (+ (.now js/Date) (* DEFAULT_TTL_SECONDS 1000)))}]
       (await (.updateOne
               c
               #js {"session_id" (str session-id)}
               #js {"$set" (clj->js (dissoc doc :created_at :system_instance_id))
                    "$setOnInsert" (clj->js {:created_at now
                                            :system_instance_id (system-instance/current-id)})},
               #js {"upsert" true}))
       (assoc entry :session session-id)))))

(defn ^:async delete-title!
  "Remove cached session title."
  ([session-id] (delete-title! (mongo-client/get-db) session-id))
  ([db session-id]
   (when (and db session-id)
     (let [c (coll db)]
       (await (.deleteOne c #js {"session_id" (str session-id)}))
       true))))

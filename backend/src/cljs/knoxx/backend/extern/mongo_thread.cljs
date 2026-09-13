(ns knoxx.backend.extern.mongo-thread
  "Named Mongo native boundary for conversation documents and indexes."
  (:require [clojure.string :as str]))

(def COLLECTION_NAME "knoxx_threads")
(def ACTIVE_STATUS #{"running" "queued" "waiting_input"})
(defn- session-ttl-seconds [session-id]
  (if (str/includes? (str session-id) "-sticky") 86400 3600))

(defn- decode-session [native]
  (when native
    ;; BSON identities and driver-owned Date fields are transport metadata,
    ;; not portable conversation state. Returning them breaks the next patch's
    ;; EDN contract even when the caller only changes an ordinary status.
    (dissoc (js->clj native :keywordize-keys true)
            :_id :expiresAt :createdAt :updatedAt)))

(defn ^:async find-session [db session-id]
  (let [coll (.collection db COLLECTION_NAME)
        result (await (.findOne coll #js {"session_id" session-id}))]
    (decode-session result)))

(defn ^:async find-session-by-conversation [db conversation-id]
  (let [coll (.collection db COLLECTION_NAME)
        result (await (.findOne coll #js {"conversation_id" conversation-id}))]
    (decode-session result)))

(defn ^:async upsert-session! [db session]
  (let [coll (.collection db COLLECTION_NAME)
        ttl (session-ttl-seconds (:session_id session))
        now (js/Date.)
        doc (-> session
                (assoc :expiresAt (js/Date. (+ (.now js/Date) (* ttl 1000)))
                       :updatedAt now)
                (dissoc :createdAt))]
    (await (.findOneAndUpdate
             coll
             #js {"session_id" (:session_id session)}
             #js {"$set" (clj->js doc)
                  "$setOnInsert" (clj->js {:createdAt now})}
             #js {"upsert" true "returnDocument" "after"}))))

(defn ^:async update-session-doc! [db session-id updates]
  (let [coll (.collection db COLLECTION_NAME)
        ttl (session-ttl-seconds session-id)
        set-doc (merge updates
                       {:updatedAt (js/Date.)
                        :expiresAt (js/Date. (+ (.now js/Date) (* ttl 1000)))})]
    (await (.findOneAndUpdate
             coll
             #js {"session_id" session-id}
             #js {"$set" (clj->js set-doc)}
             #js {"returnDocument" "after"}))))

(defn ^:async delete-session! [db session-id]
  (let [coll (.collection db COLLECTION_NAME)]
    (await (.deleteOne coll #js {"session_id" session-id}))
    true))

(defn ^:async fetch-active-sessions [db]
  (let [coll (.collection db COLLECTION_NAME)
        cursor (.find coll #js {"status" #js {"$in" (clj->js (vec ACTIVE_STATUS))}})
        results (await (.toArray cursor))]
    (mapv decode-session (array-seq results))))

(defn ^:async setup-indexes!
  "Create required indexes on knoxx_threads collection."
  [db]
  (let [coll (.collection db COLLECTION_NAME)]
    (await (.createIndex coll #js {"session_id" 1} #js {"unique" true}))
    (await (.createIndex coll #js {"conversation_id" 1} #js {"unique" true "sparse" true}))
    (await (.createIndex coll #js {"user_id" 1}))
    (await (.createIndex coll #js {"org_id" 1}))
    (await (.createIndex coll #js {"status" 1}))
    (await (.createIndex coll #js {"expiresAt" 1} #js {"expireAfterSeconds" 0}))))

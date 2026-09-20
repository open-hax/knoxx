(ns knoxx.backend.extern.mongo-thread
  "Named Mongo native boundary for conversation documents and indexes."
  (:require [clojure.string :as str]))

(def COLLECTION_NAME "knoxx_threads")
(def ACTIVE_STATUS #{"running" "queued" "waiting_input"})
(defn- session-ttl-seconds [session-id]
  (if (str/includes? (str session-id) "-sticky") 86400 3600))

(defn- decode-session [native]
  (when native
    (let [expires (aget native "expiresAt")
          expiry (cond (instance? js/Date expires) (.getTime expires)
                       (string? expires) (.parse js/Date expires)
                       :else js/NaN)]
      ;; A row may expire between the query and decoding. Retain its exact
      ;; portable expiry so caching cannot grant it another full lifetime.
      (when (and (js/Number.isFinite expiry) (> expiry (.now js/Date)))
        (-> (js->clj native :keywordize-keys true)
            (dissoc :_id :createdAt :updatedAt)
            (assoc :expiresAt (.toISOString (js/Date. expiry))))))))

(defn- live-query [fields]
  (clj->js (assoc fields :expiresAt {:$gt (js/Date. (.now js/Date))})))

(defn ^:async find-session [db session-id]
  (let [coll (.collection db COLLECTION_NAME)
        result (await (.findOne coll (live-query {:session_id session-id})))]
    (decode-session result)))

(defn ^:async find-session-by-conversation [db conversation-id]
  (let [coll (.collection db COLLECTION_NAME)
        result (await (.findOne coll (live-query {:conversation_id conversation-id})))]
    (decode-session result)))

(defn ^:async upsert-session! [db session]
  (let [coll (.collection db COLLECTION_NAME)
        ttl (session-ttl-seconds (:session_id session))
        now (js/Date. (.now js/Date))
        doc (-> session
                (assoc :expiresAt (js/Date. (+ (.getTime now) (* ttl 1000)))
                       :updatedAt now)
                (dissoc :createdAt))]
    (decode-session (await (.findOneAndUpdate
             coll
             #js {"session_id" (:session_id session)}
             #js {"$set" (clj->js doc)
                  "$setOnInsert" (clj->js {:createdAt now})}
             #js {"upsert" true "returnDocument" "after"})))))

(defn ^:async update-session-doc! [db session-id updates]
  (let [coll (.collection db COLLECTION_NAME)
        ttl (session-ttl-seconds session-id)
        set-doc (merge updates
                       {:updatedAt (js/Date.)
                        :expiresAt (js/Date. (+ (.now js/Date) (* ttl 1000)))})]
    (decode-session (await (.findOneAndUpdate
             coll
             #js {"session_id" session-id}
             #js {"$set" (clj->js set-doc)}
             #js {"returnDocument" "after"})))))

(defn ^:async delete-session! [db session-id]
  (let [coll (.collection db COLLECTION_NAME)]
    (await (.deleteOne coll #js {"session_id" session-id}))
    true))

(defn ^:async fetch-active-sessions [db]
  (let [coll (.collection db COLLECTION_NAME)
        cursor (.find coll (live-query {:status {:$in (vec ACTIVE_STATUS)}}))
        results (await (.toArray cursor))]
    (into [] (keep decode-session) (array-seq results))))

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

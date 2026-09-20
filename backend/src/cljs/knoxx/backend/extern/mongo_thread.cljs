(ns knoxx.backend.extern.mongo-thread
  "Named Mongo native boundary for conversation documents and indexes."
  (:require [clojure.string :as str]
            [knoxx.backend.law.thread-store :as law]))

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

(defn duplicate-key?
  "Identify only the driver's unique-index conflict classification."
  [error] (= 11000 (.-code ^js error)))

(defn duplicate-conversation?
  "Distinguish the unique conversation binding from a raced session insert."
  [error] (some? (some-> error .-keyPattern (aget "conversation_id"))))

(defn- identity-query [session]
  ;; Each supplied identity field is atomically unbound or exactly equal. The
  ;; shared law requires scalar strings, avoiding Mongo's array-match semantics.
  (reduce (fn [query field]
            (if (contains? session field)
              (assoc query field {:$in [nil (get session field)]}) query))
          {:session_id (:session_id session)} law/identity-fields))

(defn- ^:async retire-expired! [coll session now]
  ;; Match expiry in the delete itself: a competing renewal cannot be removed.
  ;; Re-admission starts empty, never adopting an expired owner's transcript.
  (await (.deleteOne coll (clj->js {:session_id (:session_id session) :expiresAt {:$lte now}})))
  (when-let [conversation (:conversation_id session)]
    (await (.deleteOne coll (clj->js {:conversation_id conversation :expiresAt {:$lte now}})))))

(defn ^:async upsert-session! [db session]
  (let [coll (.collection db COLLECTION_NAME)
        ttl (session-ttl-seconds (:session_id session))
        now (js/Date. (.now js/Date))
        doc (-> session
                (assoc :expiresAt (js/Date. (+ (.getTime now) (* ttl 1000)))
                       :updatedAt now)
                (dissoc :createdAt)
                (cond-> (nil? (:conversation_id session)) (dissoc :conversation_id)))]
    (await (retire-expired! coll session now))
    (decode-session (await (.findOneAndUpdate
             coll
             (clj->js (identity-query session))
             (clj->js (cond-> {:$set doc :$setOnInsert {:createdAt now}}
                        (and (contains? session :conversation_id) (nil? (:conversation_id session)))
                        (assoc :$unset {:conversation_id ""})))
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

(defn ^:async patch-if-messages!
  "Atomically patch a live thread only if its transcript still matches the observed value."
  [db session-id messages updates]
  (let [now (.now js/Date)
        fields (assoc updates :updatedAt (js/Date. now)
                              :expiresAt (js/Date. (+ now (* 1000 (session-ttl-seconds session-id)))))]
    (decode-session
     (await (.findOneAndUpdate (.collection db COLLECTION_NAME)
                              (live-query {:session_id session-id :messages messages})
                              #js {"$set" (clj->js fields)}
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

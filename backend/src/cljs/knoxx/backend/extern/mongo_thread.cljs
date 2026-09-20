(ns knoxx.backend.extern.mongo-thread
  "Named Mongo native boundary for conversation documents and indexes."
  (:require ["mongodb" :refer [BSON]]
            ["node:crypto" :as crypto]
            [clojure.string :as str]
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
            (dissoc :_id :createdAt :updatedAt :startup_cas_token)
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

(defn- identity-query [session observed]
  ;; Preserve every observed binding, including fields omitted by a partial
  ;; write. An unbound field can accept only this write's initial assignment.
  (reduce (fn [query field]
            (assoc query field (or (get observed field) {:$in [nil (get session field)]})))
          {:session_id (:session_id session)} law/identity-fields))

(defn- ^:async retire-expired! [coll session now]
  ;; Match expiry in the delete itself: a competing renewal cannot be removed.
  ;; Re-admission starts empty, never adopting an expired owner's transcript.
  (await (.deleteOne coll (clj->js {:session_id (:session_id session) :expiresAt {:$lte now}})))
  (when-let [conversation (:conversation_id session)]
    (await (.deleteOne coll (clj->js {:conversation_id conversation :expiresAt {:$lte now}})))))

(defn ^:async upsert-session! [db session observed]
  (let [coll (.collection db COLLECTION_NAME)
        ttl (session-ttl-seconds (:session_id session))
        now (js/Date. (.now js/Date))
        ;; Explicitly remove unbound fields: an upsert predicate can synthesize
        ;; null, which would otherwise occupy the sparse conversation index.
        unbound (filter #(and (nil? (get session %)) (nil? (get observed %))) law/identity-fields)
        doc (apply dissoc (-> session
                              (assoc :expiresAt (js/Date. (+ (.getTime now) (* ttl 1000)))
                                     :updatedAt now)
                              (dissoc :createdAt)) unbound)]
    (await (retire-expired! coll session now))
    (decode-session (await (.findOneAndUpdate
             coll
             (clj->js (identity-query session observed))
             (clj->js (cond-> {:$set doc :$setOnInsert {:createdAt now}}
                        (seq unbound) (assoc :$unset (zipmap unbound (repeat "")))))
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
  "Patch only the observed live identity and transcript, including unbound owner fields."
  [db observed updates]
  (let [session-id (:session_id observed)
        query (merge {:session_id session-id :messages (:messages observed)}
                     (zipmap law/identity-fields (map observed law/identity-fields)))
        now (.now js/Date)
        fields (assoc updates :updatedAt (js/Date. now)
                              :expiresAt (js/Date. (+ now (* 1000 (session-ttl-seconds session-id)))))]
    (decode-session
     (await (.findOneAndUpdate (.collection db COLLECTION_NAME)
                              (live-query query)
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

(defn ^:async startup-view!
  "Capture an opaque primary BSON preimage, including metadata and expired rows."
  [db id]
  {:native (await (.findOne (.collection db COLLECTION_NAME) #js {"session_id" id}
                            #js {"readPreference" "primary"}))})

(defn startup-value
  "Expose only the live portable conversation from an opaque startup preimage."
  [view]
  (decode-session (:native view)))

(defn same-startup-view?
  "Compare the exact BSON preimages without promoting driver metadata to domain data."
  [left right]
  (let [a (:native left) b (:native right)]
    (if (and a b) (.equals (.serialize BSON a) (.serialize BSON b)) (= a b))))

(defn- startup-document [view record]
  (let [now (js/Date.) old (:native view)
        visible (decode-session old)
        created (if visible (aget old "createdAt") now)
        expires (if (and visible (= (:startup_token visible) (:startup_token record)))
                  (aget old "expiresAt")
                  (js/Date. (+ (.getTime now) (* 1000 (session-ttl-seconds (:session_id record))))))
        document (clj->js (assoc (dissoc record :_id :createdAt :updatedAt)
                                  :createdAt created :updatedAt now :expiresAt expires
                                  :startup_cas_token (crypto/randomUUID)))]
    (when old (aset document "_id" (aget old "_id")))
    document))

(defn ^:async startup-cas!
  "Journal a full-preimage startup claim/fence; a late old write cannot overwrite it."
  [db view record]
  (let [coll (.collection db COLLECTION_NAME) old (:native view)
        document (startup-document view record)
        options #js {"writeConcern" #js {"w" "majority" "j" true}}]
    (try
      (if old
        (let [query #js {"_id" (aget old "_id")
                        "$expr" #js {"$eq" #js ["$$ROOT" #js {"$literal" old}]}}
              _ (when (> (+ (.calculateObjectSize BSON query) (.calculateObjectSize BSON document) 4096)
                         (* 16 1024 1024))
                  (throw (ex-info "Thread startup CAS exceeds its bounded command size"
                                  {:status 413 :code "thread_startup_too_large"})))
              result (await (.replaceOne coll query document options))]
          (when (= 1 (.-matchedCount result)) (decode-session document)))
        (do
          (when (> (+ (.calculateObjectSize BSON document) 4096) (* 16 1024 1024))
            (throw (ex-info "Thread startup record exceeds its bounded size"
                            {:status 413 :code "thread_startup_too_large"})))
          (await (.insertOne coll document options)) (decode-session document)))
      (catch :default error
        (if (duplicate-key? error) nil (throw error))))))

(ns knoxx.backend.infra.stores.mongo-session-store
  "MongoDB-backed session state for resilient Knoxx sessions.
   Replaces Redis session store with MongoDB + TTL index.
   Active sessions are stored in knoxx_sessions collection.
   In-memory cache preserved for fast access during streaming."
  (:require
    [clojure.string :as str]
    [knoxx.backend.infra.mongo-client :as mongo-client]
    [knoxx.backend.infra.system-instance :as system-instance]))

;; ── Constants ─────────────────────────────────────────────────────────

(def SESSION_TTL_SECONDS 3600) ; 1 hour TTL for active sessions
(def STICKY_SESSION_TTL_SECONDS (* 24 60 60)) ; 24 hours for sticky sessions
(def COLLECTION_NAME "knoxx_sessions")
(def ACTIVE_STATUS #{"running" "queued" "waiting_input"})

;; ── In-memory cache (preserved from Redis store) ──────────────────────

(defonce session-cache* (atom {}))

(def ^:private max-session-cache-size 1000)
(def ^:private sticky-session-ttl-ms (* 24 60 60 1000))
(def ^:private session-cache-sweep-interval-ms 300000)

(defn- evict-oldest-session-cache-entry!
  "Remove the oldest entry when the cache exceeds max size."
  []
  (when (> (count @session-cache*) max-session-cache-size)
    (let [oldest (apply min-key (comp :cached-at val) @session-cache*)]
      (when oldest
        (swap! session-cache* dissoc (key oldest))))))

(defn- start-session-cache-sweep!
  "Periodically evict stale sticky sessions from the cache."
  []
  (js/setInterval
   (fn []
     (let [cutoff (- (js/Date.now) sticky-session-ttl-ms)
           stale (for [[id entry] @session-cache*
                       :when (< (or (:cached-at entry) 0) cutoff)]
                   id)]
       (when (seq stale)
         (swap! session-cache* #(apply dissoc % stale)))))
   session-cache-sweep-interval-ms))

(start-session-cache-sweep!)

(defn- cache-session!
  [session-id session]
  (evict-oldest-session-cache-entry!)
  (swap! session-cache* assoc session-id (assoc session :cached-at (js/Date.now))))

(defn- session-ttl-seconds
  [session-id]
  (if (str/includes? (str session-id) "-sticky")
    STICKY_SESSION_TTL_SECONDS
    SESSION_TTL_SECONDS))

;; ── MongoDB Operations ────────────────────────────────────────────────

(defn- ^:async find-session [db session-id]
  (let [coll (.collection db COLLECTION_NAME)
        result (await (.findOne coll #js {"session_id" session-id}))]
    (when result (js->clj result :keywordize-keys true))))

(defn- ^:async find-session-by-conversation [db conversation-id]
  (let [coll (.collection db COLLECTION_NAME)
        result (await (.findOne coll #js {"conversation_id" conversation-id}))]
    (when result (js->clj result :keywordize-keys true))))

(defn- ^:async upsert-session! [db session]
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

(defn- ^:async update-session-doc! [db session-id updates]
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

(defn- ^:async delete-session! [db session-id]
  (let [coll (.collection db COLLECTION_NAME)]
    (await (.deleteOne coll #js {"session_id" session-id}))
    true))

(defn- ^:async fetch-active-sessions [db]
  (let [coll (.collection db COLLECTION_NAME)
        cursor (.find coll #js {"status" #js {"$in" (clj->js (vec ACTIVE_STATUS))}})
        results (await (.toArray cursor))]
    (js->clj results :keywordize-keys true)))

;; ── Public API ────────────────────────────────────────────────────────

(defn ^:async get-session
  "Get session state, checking cache first then MongoDB."
  ([session-id]
   (get-session (mongo-client/get-db) session-id))
  ([db session-id]
   (if-let [cached (get @session-cache* session-id)]
     cached
     (when db
       (let [session (await (find-session db session-id))]
         (when session
           (evict-oldest-session-cache-entry!)
           (swap! session-cache* assoc session-id (assoc session :cached-at (js/Date.now))))
         session)))))

(defn get-session-sync
  "Synchronous session lookup from cache only."
  [session-id]
  (get @session-cache* session-id))

(defn ^:async get-conversation-active-session
  "Get the active session for a conversation."
  ([conversation-id]
   (get-conversation-active-session (mongo-client/get-db) conversation-id))
  ([db conversation-id]
   (when db
     (let [session (await (find-session-by-conversation db conversation-id))]
       (:session_id session)))))

(defn ^:async put-session!
  "Store session state in cache and MongoDB.
   Stamps :system_instance_id so readers can detect documents orphaned by a
   previous system instance (see knoxx.backend.infra.system-instance)."
  ([session]
   (put-session! (mongo-client/get-db) session))
  ([db session]
   (let [session (assoc session :system_instance_id (system-instance/current-id))
         session-id (str (:session_id session))]
     (cache-session! session-id session)
     (when db
       (await (upsert-session! db session)))
     session)))

(defn ^:async update-session!
  "Update session state, merging with existing."
  ([session-id updates]
   (update-session! (mongo-client/get-db) session-id updates))
  ([db session-id updates]
   (if (str/blank? (str (or session-id "")))
     (do
       (js/console.error "[mongo-session-store] update-session! called with nil/blank session-id")
       nil)
     (let [raw (or (get @session-cache* session-id) {})
           current (if (array? raw) (js->clj raw :keywordize-keys true) raw)
           updated (merge current updates {:session_id session-id
                                           :updated_at (js/Date.now)})]
       (await (put-session! db updated))))))

(defn ^:async remove-session!
  "Remove session from cache and MongoDB."
  ([session-id conversation-id]
   (remove-session! (mongo-client/get-db) session-id conversation-id))
  ([db session-id _conversation-id]
   (swap! session-cache* dissoc session-id)
   (if db
     (try
       (await (delete-session! db session-id))
       true
       (catch :default err
         (js/console.error "Failed to remove session from MongoDB:" err)
         false))
     true)))

(defn ^:async list-active-sessions
  "List all active sessions from MongoDB."
  ([]
   (list-active-sessions (mongo-client/get-db)))
  ([db]
   (if db
     (fetch-active-sessions db)
     [])))

(defn ^:async list-active-session-ids
  "List all active session IDs from MongoDB."
  ([]
   (list-active-session-ids (mongo-client/get-db)))
  ([db]
    (if db
      (let [sessions (await (fetch-active-sessions db))]
        (mapv :session_id sessions))
      [])))

(defn ^:async recover-sessions!
  "Recover active sessions from MongoDB on startup."
  ([]
   (recover-sessions! (mongo-client/get-db)))
  ([db]
   (if-not db
     []
     (let [sessions (await (fetch-active-sessions db))
           running (filterv #(= "running" (:status %)) sessions)]
       (doseq [session sessions]
         (swap! session-cache* assoc (:session_id session) session))
       running))))

(defn mark-session-streaming!
  "Mark session as actively streaming."
  ([session-id is-streaming]
   (mark-session-streaming! (mongo-client/get-db) session-id is-streaming))
  ([db session-id is-streaming]
   (update-session! db session-id {:has_active_stream is-streaming})))

(defn ^:async complete-session!
  "Mark session as completed."
  ([session-id conversation-id opts]
   (complete-session! (mongo-client/get-db) session-id conversation-id opts))
  ([db session-id _conversation-id opts]
   (let [{:keys [status answer error messages]} opts
         session (await (update-session! db session-id
                                         {:status (or status "completed")
                                          :has_active_stream false
                                          :answer answer
                                          :error error
                                          :messages messages}))]
     (js/setTimeout
      #(swap! session-cache* dissoc session-id)
      (if (str/includes? (str session-id) "-sticky")
        sticky-session-ttl-ms
        60000))
     session)))

(defn session-can-send?
  "Check if session can accept new messages."
  [session]
  (cond
    (nil? session)
    {:can-send true :reason "No existing session. Ready for new conversation."}

    (= "running" (:status session))
    {:can-send false
     :reason (if (:has_active_stream session)
               "Session is actively streaming. Use steer or wait."
               "Session is already processing. Use steer, follow-up, abort, or wait.")}

    (= "waiting_input" (:status session))
    {:can-send true :reason nil}

    (= "completed" (:status session))
    {:can-send true :reason "Previous session completed. Starting new turn."}

    (= "failed" (:status session))
    {:can-send true :reason "Previous session failed. Starting new turn."}

    :else
    {:can-send true :reason nil}))

(defn rewind-messages
  "Remove the last N user turns plus everything that followed them.
   Preserves any leading system messages that predate the removed turn(s)."
  [messages turns]
  (loop [remaining (vec (or messages []))
         turns-left (max 1 (or turns 1))]
    (if (or (zero? turns-left) (empty? remaining))
      remaining
      (if-let [last-user-index (->> remaining
                                    (keep-indexed (fn [index message]
                                                    (when (= "user" (:role message))
                                                      index)))
                                    last)]
        (recur (subvec remaining 0 last-user-index) (dec turns-left))
        remaining))))

(defn ^:async undo-session-turns!
  "Rewind the session by removing the last N user turns.
   Resolves nil when no session exists, or the updated session when successful."
  ([session-id turns]
   (undo-session-turns! (mongo-client/get-db) session-id turns))
  ([db session-id turns]
   (when-let [session (await (get-session db session-id))]
     (let [current-messages (vec (or (:messages session) []))
           rewound-messages (rewind-messages current-messages turns)]
       (if (= rewound-messages current-messages)
         session
         (await (put-session! db
                              (-> session
                                  (assoc :messages rewound-messages
                                         :status "waiting_input"
                                         :has_active_stream false
                                         :updated_at (js/Date.)
                                         :answer nil
                                         :error nil)))))))))

;; ── Debug ─────────────────────────────────────────────────────────────

(defn active-session-snapshots
  "Return active sessions from the in-memory cache for monitoring."
  []
  (->> @session-cache*
       vals
       (filter #(contains? #{"running" "queued" "waiting_input"} (:status %)))
       (sort-by :updated_at #(compare %2 %1))
       vec))

;; ── Setup ─────────────────────────────────────────────────────────────

(defn ^:async setup-indexes!
  "Create required indexes on knoxx_sessions collection."
  [db]
  (let [coll (.collection db COLLECTION_NAME)]
    (await (.createIndex coll #js {"session_id" 1} #js {"unique" true}))
    (await (.createIndex coll #js {"conversation_id" 1} #js {"unique" true "sparse" true}))
    (await (.createIndex coll #js {"user_id" 1}))
    (await (.createIndex coll #js {"org_id" 1}))
    (await (.createIndex coll #js {"status" 1}))
    (await (.createIndex coll #js {"expiresAt" 1} #js {"expireAfterSeconds" 0}))))

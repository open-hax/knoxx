(ns knoxx.backend.infra.stores.session-store
  "Redis-backed session state for resilient Knoxx sessions.

   Active sessions are stored in Redis with TTL. When a session completes,
   it's archived to OpenPlanner for long-term memory.

   Key design:
   - Session state is written to Redis on every state change
   - On backend restart, active sessions are recovered from Redis
   - Completed sessions are purged from Redis and indexed in OpenPlanner
   - Frontend can query session status to know if resume is needed"
  (:require
    [clojure.string :as str]
    [knoxx.backend.infra.redis-client :as redis]))

;; Session state schema
;; {
;;   :session_id "uuid"
;;   :conversation_id "uuid"
;;   :run_id "uuid"
;;   :status "running" | "completed" | "failed" | "waiting_input"
;;   :model "model-id"
;;   :mode "rag" | "direct"
;;   :thinking_level "off" | "low" | "medium" | "high"
;;   :created_at "iso-timestamp"
;;   :updated_at "iso-timestamp"
;;   :last_token_count 0
;;   :has_active_stream true | false
;;   :messages [{:role "user" | "assistant" :content "..."}]
;;   :pending_tool_calls [{:tool_name "..." :tool_call_id "..." :status "running"}]
;;   :org_id "..."
;;   :user_id "..."
;;   :membership_id "..."
;;   :permissions ["..."]
;;   :tool_policies [{:toolId "..." :effect "allow"}]
;; }

(def SESSION_TTL_SECONDS 3600) ; 1 hour TTL for active sessions
(def SESSION_KEY_PREFIX "knoxx:session:")
(def CONVERSATION_SESSION_KEY "knoxx:conversation_to_session:")
(def ACTIVE_SESSIONS_SET "knoxx:active_sessions")

(defn session-key
  [session-id]
  (str SESSION_KEY_PREFIX session-id))

(defn conversation-session-key
  [conversation-id]
  (str CONVERSATION_SESSION_KEY conversation-id))

(defn resolved
  [value]
  (js/Promise.resolve value))

(defn- now-iso
  []
  (.toISOString (js/Date.)))

;; In-memory cache for fast access during active streaming
(defonce session-cache* (atom {}))

;; Cache bounds to prevent memory leaks under sustained load.
(def ^:private max-session-cache-size 1000)
(def ^:private sticky-session-ttl-ms (* 24 60 60 1000)) ; 24 hours
(def ^:private sticky-session-ttl-seconds (js/Math.floor (/ sticky-session-ttl-ms 1000)))
(def ^:private session-cache-sweep-interval-ms 300000)   ; 5 minutes

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

(defn ^:async get-session
  "Get session state, checking cache first then Redis.
   Always resolves a promise for call-site consistency."
  [redis-client session-id]
  (if-let [cached (get @session-cache* session-id)]
    cached
    (when redis-client
      (let [session (await (redis/get-json redis-client (session-key session-id)))]
        (when session
          (evict-oldest-session-cache-entry!)
          (swap! session-cache* assoc session-id (assoc session :cached-at (js/Date.now))))
        session))))

(defn get-session-sync
  "Synchronous session lookup from cache only. Use get-session for full lookup."
  [session-id]
  (get @session-cache* session-id))

(defn ^:async get-conversation-active-session
  "Get the active session ID for a conversation."
  [redis-client conversation-id]
  (when redis-client
    (await (redis/get-key redis-client (conversation-session-key conversation-id)))))

(defn- session-ttl-seconds
  [session-id]
  (if (str/includes? (str session-id) "-sticky")
    sticky-session-ttl-seconds
    SESSION_TTL_SECONDS))

(defn- normalize-session-ids
  [session]
  (let [session-id (some-> (:session_id session) str)
        conversation-id (some-> (:conversation_id session) str)
        normalized-session (cond-> session
                             session-id (assoc :session_id session-id)
                             conversation-id (assoc :conversation_id conversation-id))]
    {:session-id session-id
     :conversation-id conversation-id
     :session normalized-session}))

(defn- cache-session!
  [session-id session]
  (evict-oldest-session-cache-entry!)
  (swap! session-cache* assoc session-id (assoc session :cached-at (js/Date.now))))

(defn ^:async persist-session!
  [redis-client session-id conversation-id session ttl-seconds]
  (try
    (await (redis/set-json redis-client (session-key session-id) session ttl-seconds))
    (when conversation-id
      (await (redis/set-key redis-client
                            (conversation-session-key conversation-id)
                            session-id
                            ttl-seconds)))
    (if session-id
      (await (redis/sadd redis-client ACTIVE_SESSIONS_SET session-id))
      (js/console.error "[session-store] put-session! reached sadd with nil session-id; session keys:" (pr-str (keys session))))
    (catch :default err
      (js/console.error "Failed to persist session to Redis:" err))))

(defn ^:async put-session!
  "Store session state in cache and Redis.
   Always resolves a promise with the stored session."
  [redis-client session]
  (let [{:keys [session-id conversation-id session]} (normalize-session-ids session)
        ttl-seconds (session-ttl-seconds session-id)]
    (cache-session! session-id session)
    (when redis-client
      (await (persist-session! redis-client session-id conversation-id session ttl-seconds)))
    session))

(defn ^:async update-session!
  "Update session state, merging with existing. Always resolves the updated session."
  [redis-client session-id updates]
  (if (str/blank? (str (or session-id "")))
    (do
      (js/console.error "[session-store] update-session! called with nil/blank session-id; updates:" (pr-str updates))
      nil)
    (let [raw (or (get @session-cache* session-id) {})
          current (if (array? raw) (js->clj raw :keywordize-keys true) raw)
          updated (merge current updates {:session_id session-id
                                          :updated_at (js/Date.now)})]
      (await (put-session! redis-client updated)))))

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
  [redis-client session-id turns]
  (when-let [session (await (get-session redis-client session-id))]
    (let [current-messages (vec (or (:messages session) []))
          rewound-messages (rewind-messages current-messages turns)]
      (if (= rewound-messages current-messages)
        session
        (await (put-session! redis-client
                             (-> session
                                 (assoc :messages rewound-messages
                                        :status "waiting_input"
                                        :has_active_stream false
                                        :updated_at (now-iso)
                                        :answer nil
                                        :error nil))))))))

(defn ^:async remove-session!
  "Remove session from cache and Redis."
  [redis-client session-id conversation-id]
  (swap! session-cache* dissoc session-id)
  (if redis-client
    (try
      (await (redis/del redis-client (session-key session-id)))
      (when conversation-id
        (await (redis/del redis-client (conversation-session-key conversation-id))))
      (await (redis/srem redis-client ACTIVE_SESSIONS_SET session-id))
      true
      (catch :default err
        (js/console.error "Failed to remove session from Redis:" err)
        false))
    true))

(defn ^:async list-active-sessions
  "List all active session IDs from Redis."
  [redis-client]
  (if redis-client
    (await (redis/smembers redis-client ACTIVE_SESSIONS_SET))
    []))

(defn- recovered-session-summary
  [ids sessions]
  (let [pairs (map vector ids sessions)
        stale-ids (->> pairs
                       (filter (fn [[_ session]] (nil? session)))
                       (map first)
                       vec)
        cacheable (->> pairs
                       (keep (fn [[session-id session]]
                               (when (and session
                                          (or (= "running" (:status session))
                                              (str/includes? (str session-id) "-sticky")))
                                 session)))
                       vec)]
    {:stale-ids stale-ids
     :cacheable cacheable
     :running (->> cacheable
                   (filter #(= "running" (:status %)))
                   vec)}))

(defn ^:async remove-stale-session-ids!
  [redis-client stale-ids]
  (when (seq stale-ids)
    (await (.all js/Promise
                 (clj->js (mapv #(redis/srem redis-client ACTIVE_SESSIONS_SET %) stale-ids))))))

(defn- cache-recovered-sessions!
  [sessions]
  (doseq [session sessions]
    (swap! session-cache* assoc (:session_id session) session)))

(defn ^:async recover-sessions!
  "Recover sessions from Redis on startup. Returns the session records that were still running."
  [redis-client]
  (if-not redis-client
    []
    (let [ids (vec (await (list-active-sessions redis-client)))]
      (if-not (seq ids)
        []
        (let [results (await (.all js/Promise (clj->js (mapv #(get-session redis-client %) ids))))
              sessions (vec (js->clj results :keywordize-keys true))
              {:keys [stale-ids cacheable running]} (recovered-session-summary ids sessions)]
          (await (remove-stale-session-ids! redis-client stale-ids))
          (cache-recovered-sessions! cacheable)
          running)))))

(defn mark-session-streaming!
  "Mark session as actively streaming."
  [redis-client session-id is-streaming]
  (update-session! redis-client session-id {:has_active_stream is-streaming}))

(defn ^:async complete-session!
  "Mark session as completed and remove from active set.
   Optionally archive to OpenPlanner for long-term memory."
  [redis-client session-id _conversation-id opts]
  (let [{:keys [status answer error messages]} opts
        session (await (update-session! redis-client session-id
                                        {:status (or status "completed")
                                         :has_active_stream false
                                         :answer answer
                                         :error error
                                         :messages messages}))]
    ;; Default behavior: keep in Redis briefly for resume, then cleanup.
    ;; Sticky event-triggered sessions are long-lived across scheduled runs, so keep them
    ;; in Redis+cache (and in the active set so they recover after restart).
    ;; Always evict from cache after a brief delay; sticky sessions
    ;; just get a longer delay. The cache sweep will catch any leaks.
    (js/setTimeout
     #(swap! session-cache* dissoc session-id)
     (if (str/includes? (str session-id) "-sticky")
       sticky-session-ttl-ms
       60000))
    session))

(defn session-can-send?
  "Check if session can accept new messages.
   Returns {:can-send true|false :reason <string-or-nil>}."
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

;; Export for REPL debugging
(defn active-session-snapshots
  []
  (->> @session-cache*
       vals
       (filter #(contains? #{"running" "queued" "waiting_input"} (:status %)))
       (sort-by :updated_at #(compare %2 %1))
       vec))

(defn debug-dump-cache []
  (js/console.log "Session cache:" (clj->js @session-cache*)))

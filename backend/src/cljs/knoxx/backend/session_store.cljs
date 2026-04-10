(ns knoxx.backend.session-store
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
    [knoxx.backend.redis-client :as redis]))

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

;; In-memory cache for fast access during active streaming
(defonce session-cache* (atom {}))

(defn get-session
  "Get session state, checking cache first then Redis.
   Always resolves a promise for call-site consistency."
  [redis-client session-id]
  (if-let [cached (get @session-cache* session-id)]
    (resolved cached)
    (if redis-client
      (-> (redis/get-json redis-client (session-key session-id))
          (.then (fn [session]
                   (when session
                     (swap! session-cache* assoc session-id session))
                   session)))
      (resolved nil))))

(defn get-session-sync
  "Synchronous session lookup from cache only. Use get-session for full lookup."
  [session-id]
  (get @session-cache* session-id))

(defn get-conversation-active-session
  "Get the active session ID for a conversation."
  [redis-client conversation-id]
  (if redis-client
    (redis/get redis-client (conversation-session-key conversation-id))
    (resolved nil)))

(defn put-session!
  "Store session state in cache and Redis.
   Always resolves a promise with the stored session."
  [redis-client session]
  (let [session-id (:session_id session)
        conversation-id (:conversation_id session)]
    ;; Update cache immediately
    (swap! session-cache* assoc session-id session)

    (if redis-client
      (-> (redis/set-json redis-client
                          (session-key session-id)
                          session
                          SESSION_TTL_SECONDS)
          (.then (fn []
                   (if conversation-id
                     (redis/set redis-client
                                (conversation-session-key conversation-id)
                                session-id
                                SESSION_TTL_SECONDS)
                     (resolved nil))))
          (.then (fn []
                   (redis/sadd redis-client ACTIVE_SESSIONS_SET session-id)))
          (.then (fn [] session))
          (.catch (fn [err]
                    (js/console.error "Failed to persist session to Redis:" err)
                    session)))
      (resolved session))))

(defn update-session!
  "Update session state, merging with existing. Always resolves the updated session."
  [redis-client session-id updates]
  (let [current (or (get @session-cache* session-id) {})
        updated (merge current updates {:updated_at (js/Date.now)})]
    (put-session! redis-client updated)))

(defn remove-session!
  "Remove session from cache and Redis."
  [redis-client session-id conversation-id]
  (swap! session-cache* dissoc session-id)
  (if redis-client
    (-> (redis/del redis-client (session-key session-id))
        (.then (fn []
                 (if conversation-id
                   (redis/del redis-client (conversation-session-key conversation-id))
                   (resolved nil))))
        (.then (fn []
                 (redis/srem redis-client ACTIVE_SESSIONS_SET session-id)))
        (.then (fn [] true))
        (.catch (fn [err]
                  (js/console.error "Failed to remove session from Redis:" err)
                  false)))
    (resolved true)))

(defn list-active-sessions
  "List all active session IDs from Redis."
  [redis-client]
  (if redis-client
    (redis/smembers redis-client ACTIVE_SESSIONS_SET)
    (resolved [])))

(defn recover-sessions!
  "Recover sessions from Redis on startup. Returns the session records that were still running."
  [redis-client]
  (if-not redis-client
    (resolved [])
    (-> (list-active-sessions redis-client)
        (.then (fn [session-ids]
                 (let [ids (vec session-ids)]
                   (if (seq ids)
                     (-> (.all js/Promise (clj->js (mapv #(get-session redis-client %) ids)))
                         (.then (fn [results]
                                  (let [sessions (vec (js->clj results :keywordize-keys true))
                                        pairs (map vector ids sessions)
                                        stale-ids (->> pairs
                                                       (filter (fn [[_ session]] (nil? session)))
                                                       (map first)
                                                       vec)
                                        running (->> pairs
                                                     (keep (fn [[_ session]]
                                                             (when (= "running" (:status session))
                                                               session)))
                                                     vec)]
                                    (doseq [stale-id stale-ids]
                                      (redis/srem redis-client ACTIVE_SESSIONS_SET stale-id))
                                    (doseq [session running]
                                      (swap! session-cache* assoc (:session_id session) session))
                                    running))))
                     (resolved []))))))))

(defn mark-session-streaming!
  "Mark session as actively streaming."
  [redis-client session-id is-streaming]
  (update-session! redis-client session-id {:has_active_stream is-streaming}))

(def SESSION_EVENTS_KEY_PREFIX "knoxx:session_events:")

(defn session-events-key
  [session-id]
  (str SESSION_EVENTS_KEY_PREFIX session-id))

(defn append-session-event!
  "Append an event to the session's event list in Redis.
   Events are stored as a Redis list for efficient append operations.
   This ensures events persist even if the backend restarts mid-session."
  [redis-client session-id event]
  (if redis-client
    (-> (redis/rpush redis-client (session-events-key session-id) (.stringify js/JSON (clj->js event)))
        (.then (fn []
                 (redis/expire redis-client (session-events-key session-id) SESSION_TTL_SECONDS)))
        (.catch (fn [err]
                  (js/console.error "Failed to append session event to Redis:" err))))
    (resolved nil)))

(defn get-session-events
  "Get all events for a session from Redis.
   Returns an array of events parsed from JSON."
  [redis-client session-id]
  (if redis-client
    (-> (redis/lrange redis-client (session-events-key session-id) 0 -1)
        (.then (fn [events]
                 (mapv (fn [e]
                        (try
                          (js->clj (js/JSON.parse e) :keywordize-keys true)
                          (catch :default _ nil)))
                      events)))
        (.catch (fn [err]
                  (js/console.error "Failed to get session events from Redis:" err)
                  [])))
    (resolved [])))

(defn complete-session!
  "Mark session as completed and remove from active set.
   Optionally archive to OpenPlanner for long-term memory."
  [redis-client session-id conversation-id opts]
  (let [{:keys [status answer error messages]} opts]
    (-> (update-session! redis-client session-id
                         {:status (or status "completed")
                          :has_active_stream false
                          :answer answer
                          :error error
                          :messages messages})
        (.then (fn [session]
                 ;; Keep in Redis briefly for resume, then cleanup
                 (js/setTimeout
                  #(remove-session! redis-client session-id conversation-id)
                  60000)
                 session)))))

(defn session-can-send?
  "Check if session can accept new messages.
   Returns {:can-send true|false :reason <string-or-nil>}."
  [session]
  (cond
    (nil? session)
    {:can-send false :reason "No session found. Start a new conversation."}

    (= "running" (:status session))
    (if (:has_active_stream session)
      {:can-send false :reason "Session is actively streaming. Use steer or wait."}
      {:can-send true :reason nil})

    (= "waiting_input" (:status session))
    {:can-send true :reason nil}

    (= "completed" (:status session))
    {:can-send true :reason "Previous session completed. Starting new turn."}

    (= "failed" (:status session))
    {:can-send true :reason "Previous session failed. Starting new turn."}

    :else
    {:can-send true :reason nil}))

;; Export for REPL debugging
(defn debug-dump-cache []
  (js/console.log "Session cache:" (clj->js @session-cache*)))

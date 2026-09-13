(ns knoxx.backend.domain.thread-store
  "Pure conversation transitions, uniqueness, expiry, and transcript rewind."
  (:require [knoxx.backend.law.thread-store :as law]))

(def empty-state {:threads {} :versions {}})
(def active-statuses law/active-statuses)

(defn visible-thread
  "TTL hides current projections while preserving historical accepted operations."
  [state thread-id now-ms]
  (law/assert-valid! :thread/id law/NonBlank thread-id)
  (law/assert-valid! :thread/clock law/Milliseconds now-ms)
  (let [entry (get-in state [:threads thread-id])]
    (when (and entry (< now-ms (:expires-ms entry))) (:thread entry))))

(defn visible-threads
  "Produce a deterministic snapshot at one supplied instant."
  [state now-ms]
  (law/assert-valid! :thread/clock law/Milliseconds now-ms)
  (->> (keys (:threads state)) sort (keep #(visible-thread state % now-ms)) vec))

(defn conversation-thread
  "Find one visible conversation binding, including its completed state."
  [state conversation-id now-ms]
  (law/assert-valid! :thread/conversation-id law/NonBlank conversation-id)
  (some #(when (= conversation-id (:conversation_id %)) %) (visible-threads state now-ms)))

(defn active-threads
  "Return active visible conversations in stable identity order."
  [state now-ms]
  (filterv #(active-statuses (:status %)) (visible-threads state now-ms)))

(defn rewind-messages
  "Remove the final N user turns and their responses; retain leading system text."
  [messages turns]
  (loop [remaining (vec (or messages [])) turns-left (max 1 (or turns 1))]
    (if (or (zero? turns-left) (empty? remaining)) remaining
      (if-let [index (last (keep-indexed #(when (= "user" (:role %2)) %1) remaining))]
        (recur (subvec remaining 0 index) (dec turns-left)) remaining))))

(defn session-can-send?
  "Explain whether the current conversation admits another user turn."
  [session]
  (cond
    (nil? session) {:can-send true :reason "No existing session. Ready for new conversation."}
    (= "running" (:status session))
    {:can-send false :reason (if (:has_active_stream session)
                              "Session is actively streaming. Use steer or wait."
                              "Session is already processing. Use steer, follow-up, abort, or wait.")}
    (= "completed" (:status session)) {:can-send true :reason "Previous session completed. Starting new turn."}
    (= "failed" (:status session)) {:can-send true :reason "Previous session failed. Starting new turn."}
    :else {:can-send true :reason nil}))

(defn- assert-identity!
  [current proposed thread-id]
  (when-not (= thread-id (:session_id proposed))
    (throw (ex-info "A mutation cannot rename its conversation"
                    {:status 409 :code "thread_store_identity_conflict"})))
  (doseq [field [:conversation_id :org_id :user_id]]
    (when (and (some? (get current field)) (not= (get current field) (get proposed field)))
      (throw (ex-info "An admitted thread cannot change its conversation, tenant or user"
                      {:status 409 :code "thread_store_identity_conflict"})))))

(defn- install
  [state thread-id proposed stamp]
  (let [current (visible-thread state thread-id (:at-ms stamp))
        bound (when (some? (:conversation_id proposed))
                (conversation-thread state (:conversation_id proposed) (:at-ms stamp)))
        thread (-> proposed (dissoc :cached-at)
                   (assoc :createdAt (or (:createdAt current) (:at stamp))
                          :updatedAt (:at stamp) :expiresAt (:expires-at stamp)
                          :system_instance_id (:instance-id stamp)))
        entry {:thread thread :expires-ms (:expires-ms stamp)}]
    (assert-identity! current proposed thread-id)
    (when (and bound (not= thread-id (:session_id bound)))
      (throw (ex-info "Conversation already has a thread"
                      {:status 409 :code "thread_store_conversation_conflict"})))
    (law/assert-valid! :thread/value law/Thread thread)
    (if (= entry (get-in state [:threads thread-id])) state
      (-> state (assoc-in [:threads thread-id] entry)
          (update-in [:versions thread-id] (fnil inc 0))))))

(defn- mutate
  [state {:keys [kind thread-id thread patch stamp turns]}]
  (let [current (visible-thread state thread-id (:at-ms stamp))]
    (case kind
      :put (install state thread-id (merge current thread) stamp)
      :patch (install state thread-id
                      (merge {:session_id thread-id} current patch {:updated_at (:at-ms stamp)}) stamp)
      :rewind (let [messages (rewind-messages (:messages current) turns)]
                (if (or (nil? current) (= messages (vec (or (:messages current) [])))) state
                  (install state thread-id
                           (assoc current :messages messages :status "waiting_input"
                                  :has_active_stream false :answer nil :error nil :updated_at (:at-ms stamp)) stamp)))
      :delete (if (contains? (:threads state) thread-id)
                (-> state (update :threads dissoc thread-id)
                    (update-in [:versions thread-id] (fnil inc 0))) state))))

(defn transition
  "Admit one stamped operation without consulting a clock or filesystem."
  [state operation]
  (law/assert-valid! :thread/operation law/Operation operation)
  (let [{:keys [thread-id stamp]} operation]
    (when-not (= (law/ttl-ms thread-id) (- (:expires-ms stamp) (:at-ms stamp)))
      (throw (ex-info "Thread expiry must match the declared TTL"
                      {:status 400 :code "thread_store_invalid_expiry"})))
    (let [next-state (mutate state operation)]
      {:state next-state :result {:thread-id thread-id
                                  :version (get-in next-state [:versions thread-id] 0)
                                  :present? (contains? (:threads next-state) thread-id)}})))

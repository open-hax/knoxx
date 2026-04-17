(ns knoxx.backend.turn-control
  "In-process control plane for the *currently running* turn per conversation.

   This enables:
   - user-initiated abort/interrupt from the UI
   - internal safety guards (death-spiral detection) to abort the active turn

   Scope: best-effort, per-process. After a pm2 restart this registry is empty
   (but sessions can be recovered from Redis).")

(defonce active-turns* (atom {}))

(defn register-active-turn!
  "Register an active turn control entry.

   entry keys:
   - :run_id
   - :session_id
   - :abort! (fn [reason] => Promise)
   - :started_at
  "
  [conversation-id entry]
  (when (and conversation-id (not (empty? (str conversation-id))))
    (swap! active-turns* assoc (str conversation-id) entry))
  entry)

(defn unregister-active-turn!
  "Remove the active turn entry if it matches the run-id (when provided)."
  ([conversation-id] (unregister-active-turn! conversation-id nil))
  ([conversation-id run-id]
   (when (and conversation-id (not (empty? (str conversation-id))))
     (let [cid (str conversation-id)]
       (swap! active-turns*
              (fn [m]
                (let [entry (get m cid)]
                  (cond
                    (nil? entry) m
                    (and run-id (not= (str run-id) (str (:run_id entry)))) m
                    :else (dissoc m cid)))))))
   true))

(defn active-turn
  [conversation-id]
  (when (and conversation-id (not (empty? (str conversation-id))))
    (get @active-turns* (str conversation-id))))

(defn abort-active-turn!
  "Abort the currently registered turn for conversation-id.

   Returns a Promise resolving to {:ok boolean, ...}."
  [conversation-id reason]
  (let [entry (active-turn conversation-id)
        abort! (:abort! entry)]
    (cond
      (nil? entry)
      (js/Promise.resolve {:ok false :error "no_active_turn"})

      (not (fn? abort!))
      (js/Promise.resolve {:ok false :error "no_abort_handler"})

      :else
      (-> (abort! (or reason "aborted"))
          (.then (fn [_]
                   {:ok true
                    :conversation_id (str conversation-id)
                    :run_id (:run_id entry)
                    :session_id (:session_id entry)}))
          (.catch (fn [err]
                    {:ok false
                     :conversation_id (str conversation-id)
                     :run_id (:run_id entry)
                     :session_id (:session_id entry)
                     :error (str err)}))))))

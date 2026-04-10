(ns knoxx.backend.session-lifecycle
  "Session lifecycle management for Knoxx agent runtime.
   
   Handles session validation, cleanup, and event persistence.
   Extracted from agent_runtime.cljs to reduce complexity.")

(defn session-usable?
  "Check if an in-memory agent session is actually usable.
   Returns false if the session is stale (completed/failed/aborted but still in memory)."
  [session]
  (and session
       (try
         (let [is-streaming (aget session "isStreaming")
               abort-requested (aget session "abortRequested")]
           ;; If streaming, it's usable (active turn in progress)
           ;; If not streaming and no abort requested, it's usable
           ;; If abort was requested, it's not usable
           (and (not (true? abort-requested))
                (or (true? is-streaming)
                    (nil? is-streaming))))
         (catch js/Error _ false))))

(defn remove-session!
  "Remove agent session from in-memory cache atom.
   
   This is called when a turn completes or fails to prevent stale session reuse.
   The next call to ensure-agent-session! will create a fresh session.
   
   Redis/OpenPlanner rehydration remains the fallback path across restarts."
  [agent-sessions-atom conversation-id]
  (when (and conversation-id agent-sessions-atom)
    (swap! agent-sessions-atom dissoc conversation-id)))

(defn validate-or-remove-session!
  "Validate existing session and return it if usable, otherwise remove it.
   Returns the session if usable, nil if removed or invalid."
  [agent-sessions-atom conversation-id]
  (when-let [session (get @agent-sessions-atom conversation-id)]
    (if (session-usable? session)
      session
      (do
        (swap! agent-sessions-atom dissoc conversation-id)
        nil))))

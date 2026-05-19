(ns knoxx.backend.domain.agent.agent-context
  "Global agent execution context for tools that need session/conversation IDs.

   Node.js is single-threaded; we track the currently executing turn's context
   so that tools (e.g. discord.voice.listen) can auto-resolve session_id and
   conversation_id without requiring the LLM to pass them explicitly.

   Scope: best-effort, per-process. Cleared after each turn.")

(defonce ^:private current-context* (atom nil))

(defn set-context!
  "Set the current agent turn context."
  [{:keys [session-id conversation-id run-id agent-spec]}]
  (reset! current-context*
        (when (and session-id conversation-id)
          (cond-> {:session-id session-id
                   :conversation-id conversation-id
                   :run-id run-id}
            agent-spec (assoc :agent-spec agent-spec)))))

(defn clear-context!
  "Clear the current agent turn context."
  []
  (reset! current-context* nil))

(defn get-context
  "Get the current agent turn context, or nil if none is active."
  []
  @current-context*)

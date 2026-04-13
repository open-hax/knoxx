(ns knoxx.backend.app-shapes)

(defn normalize-chat-body
  [body]
  {:message (or (aget body "message") "")
   :conversation-id (or (aget body "conversationId")
                        (aget body "conversation_id"))
   :session-id (or (aget body "sessionId")
                   (aget body "session_id"))
   :run-id (or (aget body "runId")
               (aget body "run_id"))
   :model (or (aget body "model") nil)
   :thinking-level (or (aget body "thinkingLevel")
                       (aget body "thinking_level")
                       (aget body "reasoningEffort")
                       (aget body "reasoning_effort"))
   :mode (or (aget body "mode") "direct")
   :auth-context (some-> (or (aget body "authContext")
                             (aget body "auth_context"))
                         (js->clj :keywordize-keys true))})

(defn normalize-control-body
  [body]
  {:message (or (aget body "message") "")
   :conversation-id (or (aget body "conversationId")
                        (aget body "conversation_id"))
   :session-id (or (aget body "sessionId")
                   (aget body "session_id"))
   :run-id (or (aget body "runId")
               (aget body "run_id"))})

(defn route!
  [app method url handler]
  (.route app #js {:method method
                   :url url
                   :handler handler}))


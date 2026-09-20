(ns knoxx.backend.extern.app-session-requests
  "Decode native request fields for session control and recovery."
  (:require [knoxx.backend.infra.http :as infra-http]))

(defn abort-command
  "Decode supported native session-abort field spellings and the default reason." [request]
  (let [raw (infra-http/request-body request)]
    {:requested-conversation-id (str (or (:conversation_id raw) (:conversationId raw) ""))
     :requested-session-id (str (or (:session_id raw) (:sessionId raw) ""))
     :requested-run-id (str (or (:run_id raw) (:runId raw) ""))
     :reason (str (or (:reason raw) "operator_abort"))}))

(defn status-query
  "Decode the supported native session and conversation query spellings." [request]
  {:session-id (or (aget request "query" "session_id") (aget request "query" "sessionId") "")
   :conversation-id (or (aget request "query" "conversation_id") (aget request "query" "conversationId") "")})

(defn undo-command
  "Decode session rewind fields and normalize the requested positive turn count." [request]
  (let [raw (infra-http/request-body request)
        turns-raw (or (:turns raw) 1)
        parsed (js/parseInt (str turns-raw) 10)]
    {:session-id (str (or (:session_id raw) (:sessionId raw) ""))
     :provided-conversation-id (str (or (:conversation_id raw) (:conversationId raw) ""))
     :actor-id (or (:actor_id raw) (:actorId raw) (:actor-id raw))
     :turns (if (js/isNaN parsed) 1 (max 1 parsed))}))

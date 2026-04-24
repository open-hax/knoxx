(ns knoxx.backend.agent-turns
  "Thin compatibility shim.  All implementations have moved to knoxx.backend.agents.*"
  (:require [knoxx.backend.agents.content :as content]
            [knoxx.backend.agents.policy :as policy]
            [knoxx.backend.agents.recovery :as recovery]
            [knoxx.backend.agents.tools :as tools]
            [knoxx.backend.agents.transcript :as transcript]
            [knoxx.backend.agents.turn :as turn]))

;; Atoms
(def lounge-messages* turn/lounge-messages*)

;; Policy
(defn validate-chat-policy!
  [auth-context model-id]
  (policy/validate-chat-policy! auth-context model-id))

;; Turn orchestration
(defn ensure-session-id
  [node-crypto session-id]
  (turn/ensure-session-id node-crypto session-id))

(defn send-agent-turn!
  [runtime config params]
  (turn/send-agent-turn! runtime config params))

;; Conversation access (retained here for backward compat)
(defn ensure-conversation-access!
  [ctx conversation-id]
  (turn/ensure-conversation-access! ctx conversation-id))

(defn remember-conversation-access!
  [ctx conversation-id]
  (turn/remember-conversation-access! ctx conversation-id))

;; Exports for JS interop / tests
(defn ^:export session->stored-messages
  [session]
  (transcript/session->stored-messages session))

(defn ^:export model-ready-content-parts
  [config model-id content-parts]
  (content/model-ready-content-parts config model-id content-parts))

(defn ^:export assistant-tool-call-previews
  [assistant-message]
  (tools/assistant-tool-call-previews assistant-message))

(defn ^:export reply-attachment-content-parts
  [tool-receipts]
  (content/reply-attachment-content-parts tool-receipts))

(defn ^:export merge-content-parts
  [& groups]
  (apply content/merge-content-parts groups))

;; Recovery helpers
(defn recovered-auth-context
  [session]
  (recovery/recovered-auth-context session))

(defn recovered-agent-spec
  [session]
  (recovery/recovered-agent-spec session))

(defn restore-recovered-conversation-access!
  [session]
  (recovery/restored-conversation-access! session))

(defn last-session-user-message
  [session]
  (recovery/last-session-user-message session))

(defn wait-for-recovered-turn-kickoff!
  [conversation-id launch-promise]
  (recovery/wait-for-recovered-turn-kickoff! conversation-id launch-promise))

(defn resume-recovered-session!
  ([runtime config session]
   (recovery/resume-recovered-session! runtime config session))
  ([runtime config session opts]
   (recovery/resume-recovered-session! runtime config session opts)))

(defn recover-active-agent-sessions!
  [runtime config redis-client]
  (recovery/recover-active-agent-sessions! runtime config redis-client))

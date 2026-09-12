(ns knoxx.backend.infra.routes.app.chat-start
  "Admit and queue chat starts after policy and session checks."
  (:require ["node:crypto" :as crypto]
            [knoxx.backend.infra.agent.hydration :as agent-hydration]
            [knoxx.backend.infra.agent.policy :as agent-policy]
            [knoxx.backend.infra.agent.runner :as agent-runner]
            [knoxx.backend.infra.agent.service :as agent-service]
            [knoxx.backend.infra.agent.turn :as agent-turn]
            [knoxx.backend.infra.http :as infra-http]
            [knoxx.backend.infra.routes.app.chat-context :as app-chat-context]
            [knoxx.backend.infra.routes.app.session-recovery :as app-session-recovery]
            [knoxx.backend.infra.stores.mongo-session-store :as session-store]
            [knoxx.backend.shape.agent :as shape-agent]
            [knoxx.backend.shape.app-shapes :as shape-app-shapes]))

(defn ^:async send-agent-turn-best-effort!
  "Send an admitted turn and record any asynchronous failure."
  [runtime config body log-label]
  (try
    (await (agent-service/send-agent-turn! runtime config body))
    (catch :default err
      (.error js/console log-label err)
      (agent-runner/log-and-record-async-spawn-error! body err))))

(defn ^:async queue-chat-start!
  "Validate chat policy before queuing work and returning acceptance."
  [runtime config reply agent-ctx policy-model body accepted-response]
  (try
    (await (agent-policy/validate-chat-policy! agent-ctx policy-model))
    (send-agent-turn-best-effort! runtime config body "Async agent chat failed")
    (infra-http/json-response! reply 202 accepted-response)
    (catch :default err
      (infra-http/error-response! reply err 429))))

(defn ^:async queue-direct-start!
  "Validate direct-chat policy before queuing work and returning acceptance."
  [runtime config reply agent-ctx policy-model body accepted-response log-label]
  (try
    (await (agent-policy/validate-chat-policy! agent-ctx policy-model))
    (send-agent-turn-best-effort! runtime config body log-label)
    (infra-http/json-response! reply 202 accepted-response)
    (catch :default err
      (infra-http/error-response! reply err 429))))

(defn- prepared-start
  [config ctx request mode]
  (let [node-crypto crypto
        parsed0 (shape-app-shapes/normalize-chat-body (infra-http/request-body request))
        parsed (assoc parsed0 :agent-spec (app-chat-context/merged-agent-spec config parsed0))
        agent-ctx (app-chat-context/effective-auth-context ctx parsed)
        policy-model (or (:model parsed) (get-in parsed [:agent-spec :model])
                         (:llmModel @agent-hydration/settings-state*))
        provided-session-id (:session-id parsed)
        session-id (agent-turn/ensure-session-id provided-session-id)
        conversation-id (or (:conversation-id parsed) (.randomUUID node-crypto))
        run-id (or (:run-id parsed) (.randomUUID node-crypto))]
    {:provided-session-id provided-session-id
     :agent-ctx agent-ctx :policy-model policy-model
     :body (assoc parsed :session-id session-id :conversation-id conversation-id
                  :run-id run-id :mode mode :auth-context agent-ctx)}))

(defn- accepted-start
  [body camel-case?]
  (let [{:keys [session-id conversation-id run-id]} body]
    (cond-> {:ok true :queued true :run_id run-id
             :conversation_id conversation-id :session_id session-id
             :model (or (:model body) (get-in body [:agent-spec :model])
                        (:llmModel @agent-hydration/settings-state*))}
      camel-case? (assoc :runId run-id :conversationId conversation-id
                         :sessionId session-id))))

(defn- ^:async checked-session-start!
  [session-id conversation-id reply queue-turn! stream-error log-label]
  (try
    (let [session (await (session-store/get-session session-id))
          can-send-result (session-store/session-can-send? session)]
      (if (:can-send can-send-result)
        (let [agent-session (agent-service/active-agent-session conversation-id)
              actively-streaming? (and agent-session (shape-agent/streaming? agent-session))]
          (if actively-streaming?
            (infra-http/json-response! reply 409 stream-error)
            (await (queue-turn! log-label))))
        (let [latest-event (when (= "running" (:status session))
                             (await (app-session-recovery/latest-run-event! (:run_id session))))]
          (await (app-session-recovery/detect-zombies
                  conversation-id session session-id queue-turn! can-send-result reply latest-event)))))
    (catch :default err
      (.error js/console "Session status check failed" err)
      (await (queue-turn! log-label)))))

(defn ^:async handle-chat-start
  "Prepare a RAG chat command and check persisted and live session admission."
  [runtime config reply ctx request]
  (let [{:keys [body provided-session-id agent-ctx policy-model]}
        (prepared-start config ctx request "rag")
        accepted-response (accepted-start body true)
        queue-turn! (fn [_log-label]
                      (queue-chat-start! runtime config reply agent-ctx policy-model body accepted-response))]
    (if-not provided-session-id
      (await (queue-turn! "Async agent chat failed"))
      (await (checked-session-start!
              (:session-id body) (:conversation-id body) reply queue-turn!
              {:ok false
               :error "Agent is already processing. Specify streamingBehavior steer or followUp to queue the message."
               :code "agent-already-processing" :has-active-stream true :can-send false}
              "Async agent chat failed")))))

(defn ^:async handle-direct-start
  "Prepare a direct chat command and check persisted and live session admission."
  [runtime config reply ctx request]
  (let [{:keys [body provided-session-id agent-ctx policy-model]}
        (prepared-start config ctx request "direct")
        accepted-response (accepted-start body false)
        queue-turn! (fn [log-label]
                      (queue-direct-start! runtime config reply agent-ctx policy-model
                                           body accepted-response log-label))]
    (if-not provided-session-id
      (await (queue-turn! "Async direct agent chat failed"))
      (await (checked-session-start!
              (:session-id body) (:conversation-id body) reply queue-turn!
              {:ok false
               :error "Agent is already processing. Specify streamingBehavior ('steer' or 'followUp') to queue the message."
               :code "agent_already_processing" :has_active_stream true :can_send false}
              "Async direct agent chat failed")))))

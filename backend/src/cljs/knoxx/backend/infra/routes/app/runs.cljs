(ns knoxx.backend.infra.routes.app.runs
  "Register chat, control, active-run and handoff routes."
  (:require-macros [knoxx.backend.macros :as backend-macros])
  (:require [clojure.string :as str]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.extern.app-health :as app-health]
            [knoxx.backend.extern.app-http :as app-http]
            [knoxx.backend.extern.app-session-requests :as session-requests]
            [knoxx.backend.infra.agent.service :as agent-service]
            [knoxx.backend.infra.agent.turn :as agent-turn]
            [knoxx.backend.infra.http :as infra-http]
            [knoxx.backend.infra.routes.app.chat-context :as app-chat-context]
            [knoxx.backend.infra.routes.app.chat-start :as app-chat-start]
            [knoxx.backend.infra.routes.app.dependencies :as app-dependencies]
            [knoxx.backend.infra.routes.app.session-abort :as app-session-abort]
            [knoxx.backend.infra.routes.app.session-status :as app-session-status]
            [knoxx.backend.infra.routes.app.session-views :as app-session-views]
            [knoxx.backend.infra.run-queries :as run-queries]
            [knoxx.backend.infra.stores.mongo-session-store :as session-store]
            [knoxx.backend.shape.agent-chat-input :as app-input]
            [knoxx.backend.shape.app-shapes :as shape-app-shapes]
            [knoxx.backend.shape.parse :as shape-parse]))

(defn- undo-session-err [reply err]
  (infra-http/json-response! reply 500 {:ok false :error (str err)}))

(defn- agents-active-ok [reply items]
  (infra-http/json-response! reply 200 {:runs items :count (count items)}))

(defn- agents-active-err [reply err]
  (infra-http/error-response! reply err 502))

(defn- run-events-ok [reply run-id events]
  (infra-http/json-response! reply 200 {:run_id run-id :events events :count (count events)}))

(defn- run-events-err [reply err]
  (infra-http/error-response! reply err 500))

(backend-macros/defroute ^{:doc "Register GET /api/knoxx/health with the shared route dependencies."} api-knoxx-health! []
  "GET" "/api/knoxx/health"
  (app-health/send-knoxx-health! config reply))

(defn- chat-turn-ok [reply resp]
  (infra-http/json-response! reply 200 resp))

(defn- chat-turn-err [reply err]
  (infra-http/error-response! reply err 502))

(backend-macros/defroute ^{:doc "Register POST /api/knoxx/chat with the shared route dependencies."} api-knoxx-chat! []
  "POST" "/api/knoxx/chat"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [parsed0 (shape-app-shapes/normalize-chat-body (infra-http/request-body request))
        parsed (assoc parsed0 :agent-spec (app-chat-context/merged-agent-spec config parsed0))
        agent-ctx (app-chat-context/effective-auth-context ctx parsed)
        body (assoc parsed
                    :mode "rag"
                    :auth-context agent-ctx)]
    (try
      (chat-turn-ok reply (await (agent-service/send-agent-turn! runtime config body)))
      (catch :default err
        (chat-turn-err reply err)))))

(backend-macros/defroute ^{:doc "Register POST /api/knoxx/chat/start with the shared route dependencies."} api-knoxx-chat-start! []
  "POST" "/api/knoxx/chat/start"
  (when ctx
    (ensure-permission! ctx "agent.chat.use"))
  (app-chat-start/handle-chat-start runtime config reply ctx request))

(backend-macros/defroute ^{:doc "Register POST /api/knoxx/direct with the shared route dependencies."} api-knoxx-direct! []
  "POST" "/api/knoxx/direct"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [parsed0 (shape-app-shapes/normalize-chat-body (infra-http/request-body request))
        parsed (assoc parsed0 :agent-spec (app-chat-context/merged-agent-spec config parsed0))
        agent-ctx (app-chat-context/effective-auth-context ctx parsed)
        body (assoc parsed
                    :mode "direct"
                    :auth-context agent-ctx)]
    (try
      (chat-turn-ok reply (await (agent-service/send-agent-turn! runtime config body)))
      (catch :default err
        (chat-turn-err reply err)))))

(backend-macros/defroute ^{:doc "Register POST /api/knoxx/direct/start with the shared route dependencies."} api-knoxx-direct-start! []
  "POST" "/api/knoxx/direct/start"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (app-chat-start/handle-direct-start runtime config reply ctx request))

(defn- steer-ok [reply resp]
  (infra-http/json-response! reply 200 resp))

(defn- steer-err [reply err]
  (infra-http/error-response! reply err 409))

(backend-macros/defroute ^{:doc "Register POST /api/knoxx/steer with the shared route dependencies."} api-knoxx-steer! []
  "POST" "/api/knoxx/steer"
  (when ctx (ensure-permission! ctx "agent.controls.steer"))
  (let [body (assoc (shape-app-shapes/normalize-control-body (infra-http/request-body request)) :kind "steer")
        actor-ctx (app-input/auth-context-with-actor ctx (:actor-id body))]
    (agent-turn/ensure-conversation-access! actor-ctx (:conversation-id body))
    (try
      (steer-ok reply (await (agent-service/queue-agent-control! runtime config body)))
      (catch :default err
        (steer-err reply err)))))

(backend-macros/defroute ^{:doc "Register POST /api/knoxx/follow-up with the shared route dependencies."} api-knoxx-follow-up! []
  "POST" "/api/knoxx/follow-up"
  (when ctx (ensure-permission! ctx "agent.controls.follow_up"))
  (let [body (assoc (shape-app-shapes/normalize-control-body (infra-http/request-body request)) :kind "follow_up")
        actor-ctx (app-input/auth-context-with-actor ctx (:actor-id body))]
    (agent-turn/ensure-conversation-access! actor-ctx (:conversation-id body))
    (try
      (steer-ok reply (await (agent-service/queue-agent-control! runtime config body)))
      (catch :default err
        (steer-err reply err)))))

(defn- abort-ok [reply resp]
  (infra-http/json-response! reply (if (:ok resp) 200 409) resp))

(backend-macros/defroute ^{:doc "Register POST /api/knoxx/abort with the shared route dependencies."} api-knoxx-abort! []
  "POST" "/api/knoxx/abort"
  (when ctx (ensure-permission! ctx "agent.controls.steer"))
  (let [raw (infra-http/request-body request)
        conversation-id (or (:conversation_id raw) (:conversationId raw) "")
        actor-id (or (:actor_id raw) (:actorId raw) (:actor-id raw))
        actor-ctx (app-input/auth-context-with-actor ctx actor-id)
        reason (or (:reason raw) "aborted_by_user")]
    (if (str/blank? (str conversation-id))
      (json-response! reply 400 {:ok false :error "conversation_id is required"})
      (do
        (agent-turn/ensure-conversation-access! actor-ctx conversation-id)
        (try
          (abort-ok reply (await (turn-control/abort-active-turn! conversation-id reason)))
          (catch :default err
            (steer-err reply err)))))))

(defn- ^:async rewind-selected-session!
  [reply actor-ctx {:keys [session-id provided-conversation-id turns]} session]
  (let [conversation-id (str (or (:conversation_id session) provided-conversation-id ""))
        current-messages (vec (or (:messages session) []))
        rewound-messages (session-store/rewind-messages current-messages turns)
        removed-count (- (count current-messages) (count rewound-messages))]
    (when (and actor-ctx (not (str/blank? conversation-id)))
      (agent-turn/ensure-conversation-access! actor-ctx conversation-id))
    (if (zero? removed-count)
      (infra-http/json-response! reply 409 {:ok false :error "No user turns available to undo"})
      (do
        (await (session-store/undo-session-turns! session-id turns))
        (infra-http/json-response! reply 200 {:ok true :session_id session-id
                                   :conversation_id conversation-id :removed_count removed-count
                                   :remaining_messages (count rewound-messages)})))))

(defn- ^:async undo-selected-session!
  [reply actor-ctx {:keys [session-id] :as command}]
  (try
    (let [session (await (session-store/get-session session-id))]
      (cond
        (nil? session) (infra-http/json-response! reply 404 {:ok false :error "Session not found or expired"})
        (= "running" (:status session))
        (infra-http/json-response! reply 409 {:ok false :error "Cannot undo while a turn is still running"})
        :else (await (rewind-selected-session! reply actor-ctx command session))))
    (catch :default err (undo-session-err reply err))))

(backend-macros/defroute ^{:doc "Register POST /api/knoxx/session/undo with the shared route dependencies."} api-knoxx-session-undo! []
  "POST" "/api/knoxx/session/undo"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [{:keys [session-id actor-id] :as command} (session-requests/undo-command request)
        actor-ctx (app-input/auth-context-with-actor ctx actor-id)]
    (if (str/blank? session-id)
      (json-response! reply 400 {:ok false :error "session_id is required"})
      (await (undo-selected-session! reply actor-ctx command)))))

(backend-macros/defroute ^{:doc "Register GET /api/knoxx/agents/active with the shared route dependencies."} api-knoxx-agents-active! []
  "GET" "/api/knoxx/agents/active"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [limit-raw (aget request "query" "limit")
        limit (if (string? limit-raw)
                (max 1 (js/parseInt limit-raw 10))
                25)
        items (app-session-views/build-active-runs ctx limit)]
    (agents-active-ok reply items)))

(backend-macros/defroute ^{:doc "Register GET /api/admin/agents/active with the shared route dependencies."} api-admin-agents-active! []
  "GET" "/api/admin/agents/active"
  (ensure-permission! ctx "org.events.control")
  (let [limit-raw (aget request "query" "limit")
        limit (or (shape-parse/parse-positive-int limit-raw) 200)]
    (try
      (agents-active-ok reply (await (app-session-views/live-active-agent-summaries! limit false)))
      (catch :default err
        (agents-active-err reply err)))))

(backend-macros/defroute ^{:doc "Register POST /api/admin/agents/abort with the shared route dependencies."} api-admin-agents-abort! []
  "POST" "/api/admin/agents/abort"
  (ensure-permission! ctx "org.events.control")
  (app-session-abort/handle-admin-abort reply ctx request))

(backend-macros/defroute ^{:doc "Register GET /api/knoxx/session/status with the shared route dependencies."} api-knoxx-session-status! []
  "GET" "/api/knoxx/session/status"
  (app-session-status/handle-session-status runtime config reply request))

(backend-macros/defroute ^{:doc "Register GET /api/knoxx/run/:runId/events with the shared route dependencies."} api-knoxx-run-events! []
  "GET" "/api/knoxx/run/:runId/events"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [run-id (aget request "params" "runId")
        since (or (aget request "query" "since") "")]
    (if (str/blank? run-id)
      (json-response! reply 400 {:error "runId is required"})
      (try
        (run-events-ok reply run-id (await (run-queries/events-since! ctx run-id since)))
        (catch :default err
          (run-events-err reply err))))))

(backend-macros/defroute ^{:doc "Register GET /api/knoxx/runs/:runId with the shared route dependencies."} api-knoxx-run-get! []
  "GET" "/api/knoxx/runs/:runId"
  (let [run-id (str (or (aget request "params" "runId") ""))]
    (if (str/blank? run-id)
      (json-response! reply 400 {:error "runId required"})
      (try
        (json-response! reply 200 {:ok true :source "provider"
                                   :run (await (run-queries/read! ctx run-id))})
        (catch :default err (error-response! reply err 500))))))

(backend-macros/defroute ^{:doc "Register POST /api/shibboleth/handoff with the shared route dependencies."} api-shibboleth-handoff! []
  "POST" "/api/shibboleth/handoff"
  (let [body (infra-http/request-body request)]
    (if (str/blank? (:shibboleth-base-url config))
      (json-response! reply 503 {:detail "SHIBBOLETH_BASE_URL is not configured"})
      (let [payload {:source_app "knoxx"
                     :model (:model body)
                     :system_prompt (:system_prompt body)
                     :provider (:provider body)
                     :conversation_id (:conversation_id body)
                     :fake_tools_enabled (boolean (:fake_tools_enabled body))
                     :items (or (:items body) [])}]
        (try
          (let [resp (await (fetch-json (str (:shibboleth-base-url config) "/api/chat/import")
                                        {:method "POST"
                                         :json payload}))]
            (if (:ok resp)
              (app-http/shibboleth-ok config reply request body (:body resp))
              (app-http/shibboleth-import-failed reply resp)))
          (catch :default err
            (app-http/shibboleth-unreachable reply err)))))))

(defn register-knoxx-run-routes!
  "Compose the knoxx run routes with their existing dependencies."
  [app runtime config]
  (api-knoxx-health! app runtime config app-dependencies/deps)
  (api-knoxx-chat! app runtime config app-dependencies/deps)
  (api-knoxx-chat-start! app runtime config app-dependencies/deps)
  (api-knoxx-direct! app runtime config app-dependencies/deps)
  (api-knoxx-direct-start! app runtime config app-dependencies/deps)
  (api-knoxx-steer! app runtime config app-dependencies/deps)
  (api-knoxx-follow-up! app runtime config app-dependencies/deps)
  (api-knoxx-abort! app runtime config app-dependencies/deps)
  (api-knoxx-session-undo! app runtime config app-dependencies/deps)
  (api-knoxx-agents-active! app runtime config app-dependencies/deps)
  (api-admin-agents-active! app runtime config app-dependencies/deps)
  (api-admin-agents-abort! app runtime config app-dependencies/deps)
  (api-knoxx-session-status! app runtime config app-dependencies/deps)
  (api-knoxx-run-events! app runtime config app-dependencies/deps)
  (api-knoxx-run-get! app runtime config app-dependencies/deps)
  (api-shibboleth-handoff! app runtime config app-dependencies/deps))

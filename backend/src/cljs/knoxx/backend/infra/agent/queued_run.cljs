(ns knoxx.backend.infra.agent.queued-run
  "Durable initial run and event admission for the event-agent FIFO."
  (:require [knoxx.backend.domain.action.run-state :as run-state]
            [knoxx.backend.domain.time :as time]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.run-event-payload :as run-payload]
            [knoxx.backend.infra.run-events :as run-events]
            [knoxx.backend.shape.event-turn-queue :as queue-shape]))

(defn- event-queue-run-context
  [config body queue-result]
  (let [agent-spec (:agent-spec body)]
    {:settings (cond-> {:sessionId (:session-id body)
                        :conversationId (:conversation-id body)
                        :mode "direct"
                        :workspaceRoot (:workspace-root config)
                        :eventQueue (queue-shape/response-queue-metadata queue-result)}
                 agent-spec (assoc :agentSpec (queue-shape/queued-agent-spec-summary agent-spec)))
     :resources (cond-> {:provider "proxx"
                         :collection (:collection-name config)}
                  (:resource-policies agent-spec)
                  (assoc :agentResourcePolicies (:resource-policies agent-spec)))}))

(defn- event-queue-event
  [body queue-result status event-type error]
  (run-payload/tool-event-payload
   (:run-id body) (:conversation-id body) (:session-id body) event-type
   (cond-> {:status status
            :queue_position (:position queue-result)
            :queue_concurrency (get-in queue-result [:snapshot :concurrency])
            :queue_limit (get-in queue-result [:snapshot :queue-limit])
            :restart_aware false}
     error (assoc :error error))))

(defn- event-queue-run
  [config body queue-result status event-type error]
  (let [created-at (time/now-iso)]
    (merge {:run_id (:run-id body)
            :session_id (:session-id body)
            :conversation_id (:conversation-id body)
            :created_at created-at
            :updated_at created-at
            :status status
            :model (or (:model body) (get-in body [:agent-spec :model]) (:llmModel config) (:proxx-default-model config))
            :error error
            :answer nil
            :content_parts []
            :events [(event-queue-event body queue-result status event-type error)]
            :trace_blocks []
            :tool_receipts []
            :request_messages [{:role "user" :content (:message body)}]}
           (event-queue-run-context config body queue-result))))

(defn ^:async record-event-turn!
  "Persist the queue run, then its initial event, before acknowledging admission."
  [config body queue-result status event-type error]
  (let [run-id (:run-id body)
        run (merge (event-queue-run config body queue-result status event-type error)
                   (authz/auth-snapshot (:auth-context body)))
        event (first (:events run))]
    (await (run-events/persist-run! run))
    (run-state/store-run! run-id (assoc run :events []))
    (run-state/append-run-event! run-id event)
    (await (run-events/flush! run-id))))

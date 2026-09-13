(ns knoxx.backend.domain.agent.run-admission
  "Pure initial run and agent-spec projections shared by lifecycle admission.")

(defn agent-spec-summary
  "Project the admitted agent specification into existing persisted wire fields."
  [agent-spec]
  (when agent-spec
    (cond-> {}
      (:contract-id agent-spec) (assoc :contractId (:contract-id agent-spec))
      (:actor-id agent-spec) (assoc :actorId (:actor-id agent-spec))
      (seq (:contract-actors agent-spec)) (assoc :contractActors (vec (:contract-actors agent-spec)))
      (:role agent-spec) (assoc :role (:role agent-spec))
      (:model agent-spec) (assoc :model (:model agent-spec))
      (:thinking-level agent-spec) (assoc :thinkingLevel (:thinking-level agent-spec))
      (:tools-choice agent-spec) (assoc :toolsChoice (:tools-choice agent-spec))
      (:system-prompt agent-spec) (assoc :hasSystemPrompt true)
      (seq (:tool-policies agent-spec)) (assoc :toolPolicies (vec (:tool-policies agent-spec)))
      (:resource-policies agent-spec) (assoc :resourcePolicies (:resource-policies agent-spec))
      (:context-policy agent-spec) (assoc :contextPolicy (:context-policy agent-spec))
      (:sub-agent-id agent-spec) (assoc :subAgentId (:sub-agent-id agent-spec))
      (:parent-agent-id agent-spec) (assoc :parentAgentId (:parent-agent-id agent-spec))
      (:parent-run-id agent-spec) (assoc :parentRunId (:parent-run-id agent-spec))
      (:spawn-kind agent-spec) (assoc :spawnKind (:spawn-kind agent-spec))
      (:trigger-id agent-spec) (assoc :triggerId (:trigger-id agent-spec))
      (:event-type agent-spec) (assoc :eventType (:event-type agent-spec))
      (seq (:event-types agent-spec)) (assoc :eventTypes (vec (:event-types agent-spec)))
      (:event-id agent-spec) (assoc :eventId (:event-id agent-spec))
      (:event-scope-id agent-spec) (assoc :eventScopeId (:event-scope-id agent-spec))
      (:schedule-id agent-spec) (assoc :scheduleId (:schedule-id agent-spec))
      (:task-source agent-spec) (assoc :taskSource (:task-source agent-spec))
      (:rendered-task-prompt agent-spec) (assoc :hasRenderedTaskPrompt true)
      (:deprecated-agent-task-fallback agent-spec) (assoc :deprecatedAgentTaskFallback true))))

(defn- initial-settings [session-id conversation-id mode thinking-level agent-spec config]
  (cond-> {:sessionId session-id :conversationId conversation-id :mode mode
           :thinkingLevel thinking-level :workspaceRoot (:workspace-root config)}
    agent-spec (assoc :agentSpec (agent-spec-summary agent-spec))))

(defn build-initial-run
  "Build the initial run snapshot without publishing or persisting it."
  [run-id session-id conversation-id started-at model-id mode thinking-level
   agent-spec auth-extra request-messages config]
  (merge {:run_id run-id
          :session_id session-id
          :conversation_id conversation-id
          :created_at started-at
          :updated_at started-at
          :status "running"
          :model model-id
          :ttft_ms nil
          :total_time_ms nil
          :input_tokens nil
          :output_tokens nil
          :tokens_per_s nil
          :error nil
          :answer nil
          :content_parts []
          :events []
          :trace_blocks []
          :tool_receipts []
          :request_messages request-messages
          :settings (initial-settings session-id conversation-id mode thinking-level agent-spec config)
          :resources (cond-> {:provider "proxx"
                              :collection (:collection-name config)}
                       (get agent-spec :resource-policies) (assoc :agentResourcePolicies (get agent-spec :resource-policies)))}
         auth-extra))


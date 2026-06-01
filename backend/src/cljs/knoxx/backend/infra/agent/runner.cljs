(ns knoxx.backend.infra.agent.runner
  "Shared entrypoint for launching normal Knoxx agent turns.

   Event-triggered and chat-triggered work should converge on the same Knoxx
   turn runtime. This namespace provides a queue-style direct-start helper so
   non-HTTP callers can use the same semantics as /api/knoxx/direct/start."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.agent.session :refer [active-agent-session]]
            [knoxx.backend.shape.agent :refer [streaming?]]
            [knoxx.backend.infra.agent.policy :as agent-policy]
            [knoxx.backend.infra.agent.turn :as agent-turns]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.runtime.state :as runtime-state]
            [knoxx.backend.infra.stores.session-store :as session-store]
            [knoxx.backend.extern.agent-runner :as xrunner]
            [knoxx.backend.extern.agent-turn-node :as xturn-node]))

(defn current-runtime
  []
  @runtime-state/runtime*)

(defn- normalize-tool-policy
  [policy]
  (let [policy (xrunner/to-cljs-map policy)
        tool-id (some-> (or (:toolId policy)
                            (:tool-id policy)
                            (:tool_id policy))
                        str
                        not-empty)
        effect (some-> (or (:effect policy) "allow") str not-empty)]
    (when tool-id
      {:toolId tool-id :effect effect})))

(defn- spec-value
  "Extract a normalized string value from a spec map given keyword alternatives."
  [spec & keys]
  (some-> (some (fn [k] (get spec k)) keys)
          str
          not-empty))

(defn- normalize-agent-spec
  [value]
  (let [spec (xrunner/to-cljs-map value)
        contract-id (spec-value spec :contract_id :contract-id :contractId :agent_id :agent-id :agentId)
        actor-id (spec-value spec :actor_id :actor-id :actorId)
        role (spec-value spec :role :role_slug :role-slug :roleSlug)
        system-prompt (or (:system_prompt spec) (:system-prompt spec) (:systemPrompt spec))
        task-prompt (or (:task_prompt spec) (:task-prompt spec) (:taskPrompt spec))
        model (spec-value spec :model)
        thinking-level (spec-value spec :thinking_level :thinking-level :thinkingLevel :reasoning_effort :reasoning-effort :reasoningEffort)
        tool-policies (->> (or (:tool_policies spec) (:tool-policies spec) (:toolPolicies spec) [])
                           (keep normalize-tool-policy) vec)
        resource-policies (or (:resource_policies spec) (:resource-policies spec) (:resourcePolicies spec))
        sources (or (:sources spec) (:runtime_sources spec) (:runtime-sources spec) (:runtimeSources spec))
        memory-hydration (or (:memory_hydration spec) (:memory-hydration spec) (:memoryHydration spec))
        context-policy (or (:context_policy spec) (:context-policy spec) (:contextPolicy spec) (:context spec))
        sub-agent-id (spec-value spec :sub_agent_id :sub-agent-id :subAgentId)
        parent-agent-id (spec-value spec :parent_agent_id :parent-agent-id :parentAgentId)
        parent-run-id (spec-value spec :parent_run_id :parent-run-id :parentRunId)
        spawn-kind (spec-value spec :spawn_kind :spawn-kind :spawnKind)
        trigger-id (spec-value spec :trigger_id :trigger-id :triggerId)
        event-type (spec-value spec :event_type :event-type :eventType :trigger_event_type :trigger-event-type :triggerEventType)
        event-types (->> (or (:event_types spec) (:event-types spec) (:eventTypes spec)
                             (when event-type [event-type]) [])
                         (map str) (remove str/blank?) distinct vec)
        event-id (spec-value spec :event_id :event-id :eventId)
        event-scope-id (spec-value spec :event_scope_id :event-scope-id :eventScopeId)
        schedule-id (spec-value spec :schedule_id :schedule-id :scheduleId)]
    (cond-> {}
      contract-id (assoc :contract-id contract-id)
      actor-id (assoc :actor-id actor-id)
      role (assoc :role role)
      (some? system-prompt) (assoc :system-prompt system-prompt)
      (some? task-prompt) (assoc :task-prompt task-prompt)
      model (assoc :model model)
      thinking-level (assoc :thinking-level thinking-level)
      (seq tool-policies) (assoc :tool-policies tool-policies)
      resource-policies (assoc :resource-policies resource-policies)
      (seq sources) (assoc :sources sources)
      memory-hydration (assoc :memory-hydration memory-hydration)
      context-policy (assoc :context-policy context-policy)
      sub-agent-id (assoc :sub-agent-id sub-agent-id)
      parent-agent-id (assoc :parent-agent-id parent-agent-id)
      parent-run-id (assoc :parent-run-id parent-run-id)
      spawn-kind (assoc :spawn-kind spawn-kind)
      trigger-id (assoc :trigger-id trigger-id)
      event-type (assoc :event-type event-type)
      (seq event-types) (assoc :event-types event-types)
      event-id (assoc :event-id event-id)
      event-scope-id (assoc :event-scope-id event-scope-id)
      schedule-id (assoc :schedule-id schedule-id))))

(defn direct-start-payload->turn-params
  [payload]
  (let [payload (xrunner/to-cljs-map payload)
        auth-context (or (:auth_context payload)
                         (:auth-context payload))
        template-context (or (:template_context payload)
                             (:template-context payload)
                             (:templateContext payload))]
    (cond-> {:conversation-id (or (:conversation_id payload)
                                  (:conversation-id payload))
             :session-id (or (:session_id payload)
                             (:session-id payload))
             :run-id (or (:run_id payload)
                         (:run-id payload))
             :message (or (:message payload) "")
             :content-parts (or (:content_parts payload)
                                (:content-parts payload)
                                [])
             :model (:model payload)
             :mode "direct"
             :agent-spec (normalize-agent-spec (or (:agent_spec payload)
                                                   (:agent-spec payload)))}
      template-context (assoc :template-context template-context)
      auth-context (assoc :auth-context auth-context))))

(defn- policy-model
  [config body]
  (or (:model body)
      (get-in body [:agent-spec :model])
      (:llmModel config)
      (:proxx-default-model config)))

(defn- accepted-response
  [body]
  {:ok true
   :queued true
   :run_id (:run-id body)
   :conversation_id (:conversation-id body)
   :session_id (:session-id body)
   :model (or (:model body)
              (get-in body [:agent-spec :model]))})

(defn- queue-turn!
  [runtime config body]
  (-> (agent-policy/validate-chat-policy! (:auth-context body) (policy-model config body))
      (.then (fn [_]
               (-> (agent-turns/send-agent-turn! runtime config body)
                   (.then (fn [_] nil))
                   (.catch (fn [err]
                             (xrunner/log-async-spawn-error! body err))))
               (accepted-response body)))))

(defn- busy-error
  [message]
  (js/Promise.reject (js/Error. message)))

(defn- normalize-body
  [_runtime payload]
  (let [params (direct-start-payload->turn-params payload)
        provided-session-id (:session-id params)
        session-id (agent-turns/ensure-session-id provided-session-id)
        conversation-id (or (:conversation-id params) (xturn-node/random-uuid!))
        run-id (or (:run-id params) (xturn-node/random-uuid!))]
    (assoc params
           :session-id session-id
           :conversation-id conversation-id
           :run-id run-id
           :mode "direct")))

(defn spawn-direct!
  ([config payload]
   (spawn-direct! (current-runtime) config payload))
  ([runtime config payload]
   (if-not runtime
     (busy-error "Knoxx runtime unavailable for direct agent spawn")
     (let [payload (xrunner/to-cljs-map payload)
           body (normalize-body runtime payload)
           provided-session-id (or (:session_id payload)
                                   (:session-id payload))]
       (if-not provided-session-id
         (queue-turn! runtime config body)
         (-> (session-store/get-session (redis/get-client) (:session-id body))
             (.then
              (fn [session]
                (let [can-send-result (session-store/session-can-send? session)]
                  (if-not (:can-send can-send-result)
                    (busy-error (str "agent_already_processing: " (:reason can-send-result)))
                    (let [agent-session (active-agent-session (:conversation-id body))
                          actively-streaming? (and agent-session (streaming? agent-session))]
                      (if actively-streaming?
                        (busy-error "agent_already_processing: active stream")
                        (queue-turn! runtime config body)))))))))))))

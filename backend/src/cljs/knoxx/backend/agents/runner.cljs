(ns knoxx.backend.agents.runner
  "Shared entrypoint for launching normal Knoxx agent turns.

   Event-triggered and chat-triggered work should converge on the same Knoxx
   turn runtime. This namespace provides a queue-style direct-start helper so
   non-HTTP callers can use the same semantics as /api/knoxx/direct/start."
  (:require [knoxx.backend.agent-runtime :refer [active-agent-session]]
            [knoxx.backend.agent-turns :as agent-turns]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.runtime.state :as runtime-state]
            [knoxx.backend.session-store :as session-store]))

(defn current-runtime
  []
  @runtime-state/runtime*)

(defn- normalize-tool-policy
  [policy]
  (let [policy (cond
                 (map? policy) policy
                 (some? policy) (js->clj policy :keywordize-keys true)
                 :else {})
        tool-id (some-> (or (:toolId policy)
                            (:tool-id policy)
                            (:tool_id policy))
                        str
                        not-empty)
        effect (some-> (or (:effect policy) "allow") str not-empty)]
    (when tool-id
      {:toolId tool-id :effect effect})))

(defn- normalize-agent-spec
  [value]
  (let [spec (cond
               (map? value) value
               (some? value) (js->clj value :keywordize-keys true)
               :else {})
        contract-id (some-> (or (:contract_id spec)
                                (:contract-id spec)
                                (:contractId spec)
                                (:agent_id spec)
                                (:agent-id spec)
                                (:agentId spec))
                            str
                            not-empty)
        actor-id (some-> (or (:actor_id spec)
                             (:actor-id spec)
                             (:actorId spec))
                         str
                         not-empty)
        role (some-> (or (:role spec)
                         (:role_slug spec)
                         (:role-slug spec)
                         (:roleSlug spec))
                    str
                    not-empty)
        system-prompt (some-> (or (:system_prompt spec)
                                  (:system-prompt spec)
                                  (:systemPrompt spec))
                              str)
        task-prompt (some-> (or (:task_prompt spec)
                                (:task-prompt spec)
                                (:taskPrompt spec))
                            str)
        model (some-> (:model spec) str not-empty)
        thinking-level (some-> (or (:thinking_level spec)
                                   (:thinking-level spec)
                                   (:thinkingLevel spec)
                                   (:reasoning_effort spec)
                                   (:reasoning-effort spec)
                                   (:reasoningEffort spec))
                               str
                               not-empty)
        tool-policies (->> (or (:tool_policies spec)
                               (:tool-policies spec)
                               (:toolPolicies spec)
                               [])
                           (keep normalize-tool-policy)
                           vec)
        resource-policies (or (:resource_policies spec)
                              (:resource-policies spec)
                              (:resourcePolicies spec))
        memory-hydration (or (:memory_hydration spec)
                             (:memory-hydration spec)
                             (:memoryHydration spec))
        context-policy (or (:context_policy spec)
                           (:context-policy spec)
                           (:contextPolicy spec)
                           (:context spec))
        sub-agent-id (some-> (or (:sub_agent_id spec)
                                 (:sub-agent-id spec)
                                 (:subAgentId spec))
                             str
                             not-empty)
        parent-agent-id (some-> (or (:parent_agent_id spec)
                                    (:parent-agent-id spec)
                                    (:parentAgentId spec))
                                str
                                not-empty)
        parent-run-id (some-> (or (:parent_run_id spec)
                                  (:parent-run-id spec)
                                  (:parentRunId spec))
                              str
                              not-empty)
        spawn-kind (some-> (or (:spawn_kind spec)
                               (:spawn-kind spec)
                               (:spawnKind spec))
                           str
                           not-empty)]
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
      memory-hydration (assoc :memory-hydration memory-hydration)
      context-policy (assoc :context-policy context-policy)
      sub-agent-id (assoc :sub-agent-id sub-agent-id)
      parent-agent-id (assoc :parent-agent-id parent-agent-id)
      parent-run-id (assoc :parent-run-id parent-run-id)
      spawn-kind (assoc :spawn-kind spawn-kind))))

(defn direct-start-payload->turn-params
  [payload]
  (let [auth-context (or (:auth_context payload)
                         (:auth-context payload))]
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
  (-> (agent-turns/validate-chat-policy! (:auth-context body) (policy-model config body))
      (.then (fn [_]
               (-> (agent-turns/send-agent-turn! runtime config body)
                   (.then (fn [_] nil))
                   (.catch (fn [err]
                             (.error js/console "[agents.runner] async direct spawn failed" err))))
               (accepted-response body)))))

(defn- busy-error
  [message]
  (js/Promise.reject (js/Error. message)))

(defn- normalize-body
  [runtime payload]
  (let [node-crypto (aget runtime "crypto")
        params (direct-start-payload->turn-params payload)
        provided-session-id (:session-id params)
        session-id (agent-turns/ensure-session-id node-crypto provided-session-id)
        conversation-id (or (:conversation-id params) (.randomUUID node-crypto))
        run-id (or (:run-id params) (.randomUUID node-crypto))]
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
     (let [body (normalize-body runtime payload)
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
                          actively-streaming? (and agent-session
                                                   (true? (aget agent-session "isStreaming")))]
                      (if actively-streaming?
                        (busy-error "agent_already_processing: active stream")
                        (queue-turn! runtime config body)))))))))))))

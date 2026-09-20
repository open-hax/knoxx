(ns knoxx.backend.extern.agent-turn-request
  "Decode native direct-start payloads into the existing CLJS turn request shape."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.agent-runner :as xrunner]))

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
  [spec & alternatives]
  (some-> (some (fn [k] (get spec k)) alternatives)
          str
          not-empty))

(def ^:private agent-spec-string-aliases
  {:contract-id [:contract_id :contract-id :contractId
                 :agent_id :agent-id :agentId]
   :actor-id [:actor_id :actor-id :actorId]
   :role [:role :role_slug :role-slug :roleSlug]
   :task-source [:task_source :task-source :taskSource]
   :model [:model]
   :thinking-level [:thinking_level :thinking-level :thinkingLevel
                    :reasoning_effort :reasoning-effort :reasoningEffort]
   :sub-agent-id [:sub_agent_id :sub-agent-id :subAgentId]
   :parent-agent-id [:parent_agent_id :parent-agent-id :parentAgentId]
   :parent-run-id [:parent_run_id :parent-run-id :parentRunId]
   :spawn-kind [:spawn_kind :spawn-kind :spawnKind]
   :trigger-id [:trigger_id :trigger-id :triggerId]
   :event-type [:event_type :event-type :eventType
                :trigger_event_type :trigger-event-type :triggerEventType]
   :event-id [:event_id :event-id :eventId]
   :event-scope-id [:event_scope_id :event-scope-id :eventScopeId]
   :schedule-id [:schedule_id :schedule-id :scheduleId]})

(defn- normalized-string-fields
  [spec]
  (reduce-kv
   (fn [normalized field aliases]
     (if-let [value (apply spec-value spec aliases)]
       (assoc normalized field value)
       normalized))
   {}
   agent-spec-string-aliases))

(defn- normalized-prompt-fields
  [spec]
  (let [system-prompt (or (:system_prompt spec) (:system-prompt spec) (:systemPrompt spec))
        task-prompt (or (:task_prompt spec) (:task-prompt spec) (:taskPrompt spec))
        rendered (or (:rendered_task_prompt spec) (:rendered-task-prompt spec)
                     (:renderedTaskPrompt spec))
        deprecated? (boolean (or (:deprecated_agent_task_fallback spec)
                                 (:deprecated-agent-task-fallback spec)
                                 (:deprecatedAgentTaskFallback spec)))]
    (cond-> {}
      (some? system-prompt) (assoc :system-prompt system-prompt)
      (some? task-prompt) (assoc :task-prompt task-prompt)
      (some? rendered) (assoc :rendered-task-prompt rendered)
      deprecated? (assoc :deprecated-agent-task-fallback true))))

(defn- normalized-runtime-fields
  [spec]
  (let [tool-policies (->> (or (:tool_policies spec) (:tool-policies spec)
                               (:toolPolicies spec) [])
                           (keep normalize-tool-policy) vec)
        tools-choice-value (or (:tools_choice spec) (:tools-choice spec)
                               (:toolsChoice spec) (:tools/choice spec))
        tools-choice (some-> (if (keyword? tools-choice-value)
                               (name tools-choice-value)
                               tools-choice-value)
                             str
                             str/trim
                             not-empty)
        resources (or (:resource_policies spec) (:resource-policies spec)
                      (:resourcePolicies spec))
        sources (or (:sources spec) (:runtime_sources spec)
                    (:runtime-sources spec) (:runtimeSources spec))
        memory (or (:memory_hydration spec) (:memory-hydration spec)
                   (:memoryHydration spec))
        context (or (:context_policy spec) (:context-policy spec)
                    (:contextPolicy spec) (:context spec))]
    (cond-> {}
      (seq tool-policies) (assoc :tool-policies tool-policies)
      tools-choice (assoc :tools-choice tools-choice)
      resources (assoc :resource-policies resources)
      (seq sources) (assoc :sources sources)
      memory (assoc :memory-hydration memory)
      context (assoc :context-policy context))))

(defn- normalize-agent-spec
  [value]
  (let [spec (xrunner/to-cljs-map value)
        string-fields (normalized-string-fields spec)
        event-type (:event-type string-fields)
        event-types (->> (or (:event_types spec) (:event-types spec) (:eventTypes spec)
                             (when event-type [event-type]) [])
                         (map str) (remove str/blank?) distinct vec)]
    (cond-> (merge string-fields
                   (normalized-prompt-fields spec)
                   (normalized-runtime-fields spec))
      (seq event-types) (assoc :event-types event-types))))

(defn direct-start-payload->turn-params
  "Normalize the existing direct-start payload into a turn request."
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

(defn normalize-body
  "Complete normalized direct-start coordinates using explicit session and identifier adapters."
  [payload ensure-session-id fresh-id]
  (let [params (direct-start-payload->turn-params payload)
        provided-session-id (:session-id params)
        session-id (ensure-session-id provided-session-id)
        conversation-id (or (:conversation-id params) (fresh-id))
        run-id (or (:run-id params) (fresh-id))]
    (assoc params
           :session-id session-id
           :conversation-id conversation-id
           :run-id run-id
           :mode "direct")))


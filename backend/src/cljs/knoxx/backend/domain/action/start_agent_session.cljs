(ns knoxx.backend.domain.action.start-agent-session
  "Agent session lifecycle actions."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.action.registry :refer [run-action!]]
            [knoxx.backend.domain.error-observatory :as errors]
            [knoxx.backend.infra.agent.runner :as agents-runner]
            [knoxx.backend.infra.tooling :as tooling]))

(defn- nonblank
  [value]
  (some-> value str str/trim not-empty))

(defn- id-token
  [value]
  (some-> value
          (cond-> (keyword? value) name
                  (not (keyword? value)) str)
          str/trim
          not-empty))

(defn- id-segment
  [value]
  (some-> (id-token value)
          (str/replace #"[^A-Za-z0-9._:-]+" "_")
          (str/replace #"^_+|_+$" "")
          not-empty))

(defn- payload-value
  [event k]
  (let [payload (:event/payload event)]
    (or (get payload k)
        (when-not (namespace k)
          (or (get payload (keyword (name k)))
              (get payload (name k)))))))

(defn- qualified-name
  [value]
  (cond
    (keyword? value) (if-let [ns (namespace value)]
                       (str ns "/" (name value))
                       (name value))
    (nil? value) nil
    :else (some-> value str str/trim not-empty)))

(defn- first-nonblank-path
  [m candidates]
  (some (fn [[path source]]
          (when-let [value (nonblank (get-in m path))]
            {:task value
             :task-source source
             :deprecated-agent-task-fallback? false}))
        candidates))

(def ^:private action-task-paths
  [[[:action/with :task] :action/task]
   [[:action/with :task-prompt] :action/task-prompt]
   [[:action/with :taskPrompt] :action/task-prompt]
   [[:action/with :message-template] :action/message-template]
   [[:action/with :message_template] :action/message-template]
   [[:action/with :user-message] :action/user-message]
   [[:action/with :userMessage] :action/user-message]])

(def ^:private trigger-task-paths
  [[[:trigger/task] :trigger/task]
   [[:trigger/task-prompt] :trigger/task-prompt]
   [[:trigger/message-template] :trigger/message-template]
   [[:trigger/user-message] :trigger/user-message]
   [[:trigger/context :task] :trigger/context-task]
   [[:trigger/raw :trigger/task] :trigger/task]
   [[:trigger/raw :trigger/task-prompt] :trigger/task-prompt]
   [[:trigger/raw :trigger/message-template] :trigger/message-template]
   [[:trigger/raw :trigger/user-message] :trigger/user-message]
   [[:trigger/raw :data :task] :trigger/data-task]
   [[:trigger/raw :data :message-template] :trigger/data-message-template]
   [[:trigger/raw :data :context :task] :trigger/context-task]])

(defn action-task-input
  "Resolve triggered-agent task text; agent :prompts :task is fallback only."
  [action trigger resolved]
  (or (first-nonblank-path action action-task-paths)
      (first-nonblank-path trigger trigger-task-paths)
      (when-let [task (nonblank (:task-prompt resolved))]
        {:task task
         :task-source :agent/task-prompt
         :deprecated-agent-task-fallback? true})))

(defn render-start-message
  "Render the user message for an event-triggered agent session."
  [trigger event task-input trigger-id]
  (let [payload (:event/payload event)
        task (:task task-input)
        task-source (:task-source task-input)
        task-label (if (= task-source :agent/task-prompt)
                     "Deprecated agent task prompt fallback:"
                     "Action task prompt:")]
    (str "Event: " (qualified-name (:event/type event)) "\n"
         "Trigger: " (or (:trigger/id trigger) trigger-id) "\n"
         "Reason: " (or (get-in trigger [:trigger/context :reason]) "trigger action") "\n"
         "Channel ID: " (or (payload-value event :channelId) (payload-value event :channel-id) "") "\n"
         "Author: " (or (payload-value event :authorUsername) (payload-value event :author-username) "") "\n"
         "Content: " (or (:content payload) (payload-value event :content) "") "\n\n"
         (when-not (str/blank? (str task))
           (str task-label "\n" task "\n")))))

(defn- action-agent-id
  [ctx action]
  (or (nonblank (get-in action [:action/with :agent-id]))
      (nonblank (get-in action [:action/with :agentId]))
      (nonblank (:agent/id ctx))
      (nonblank (:agent/contract ctx))))

(defn- action-resource-policies
  "The resource-policy overlay a triggered session starts with, or nil.

  Two sources, and the order is the trust order. `:action/with` is contract data
  an operator wrote, so it wins. The event's own
  `[:event/payload :resource-policies]` is only read when the trigger asked for
  it with `:resource-policies-from-event`, because a trigger that did not ask
  must not have its agent's resource scope set by whoever emitted the event.

  The opt-in exists because a trigger contract is static while a pin is
  per-event: `law.translation-agent/TranslationNeededEvent` carries the document,
  locale and claim one attempt is for, and writing that into `:trigger/with`
  would mean one trigger per document. Nothing here interprets the overlay —
  forwarding a map the agent runner already knows how to read is the whole job,
  and reading it would put a caller's domain into a generic action."
  [ctx action]
  (let [declared (or (get-in action [:action/with :resource-policies])
                     (get-in action [:action/with :resourcePolicies]))
        from-event? (or (get-in action [:action/with :resource-policies-from-event])
                        (get-in action [:action/with :resourcePoliciesFromEvent]))
        carried (when from-event?
                  (get-in ctx [:event :event/payload :resource-policies]))]
    (or declared carried)))

(defn- execution-snapshot-from-event?
  "Whether this trigger requires its resolved agent policy to match the event."
  [action]
  (boolean
   (or (get-in action [:action/with :execution-snapshot-from-event])
       (get-in action [:action/with :executionSnapshotFromEvent]))))

(defn- resolved-execution
  "Project resolved agent policy into the portable snapshot comparison shape."
  [agent-id resolved]
  {:translation-execution/agent-id agent-id
   :translation-execution/model (:model resolved)
   :translation-execution/thinking (some-> (:thinking-level resolved) name)
   :translation-execution/system-prompt (:system-prompt resolved)
   :translation-execution/tool-ids (vec (:tool-ids resolved))})

(defn event-execution-refusal
  "Describe contract drift between an event snapshot and resolved agent policy."
  [event agent-id resolved]
  (let [snapshot (get-in event [:event/payload :execution])
        expected (resolved-execution agent-id resolved)
        actual (some-> snapshot (dissoc :translation-execution/digest))]
    (when-not (= expected actual)
      {:refusal/type :agent-execution-drift
       :refusal/expected expected
       :refusal/actual actual})))

(defn- assert-event-execution!
  "Refuse an opted-in event before spawn when executable policy has drifted."
  [event agent-id resolved]
  (when-let [refusal (event-execution-refusal event agent-id resolved)]
    (throw (ex-info "event execution snapshot does not match resolved agent policy"
                    refusal)))
  (get-in event [:event/payload :execution]))

(defn- actor-id
  [ctx resolved]
  (or (nonblank (:actor/id ctx))
      (nonblank (:actor-id resolved))))

(defn- payload-id
  [event k]
  (id-segment (payload-value event k)))

(defn- trigger-id
  [trigger event agent-id]
  (or (id-segment (:trigger/id trigger))
      (id-segment (get-in trigger [:trigger/raw :contract/id]))
      (payload-id event :trigger-id)
      (payload-id event :triggerId)
      (id-segment (get-in event [:event/generator :trigger/id]))
      (id-segment agent-id)
      "manual-trigger"))

(defn- event-scope-id
  [event]
  (or (payload-id event :channelId)
      (payload-id event :channel-id)
      (payload-id event :channel_id)
      (payload-id event :threadId)
      (payload-id event :thread-id)
      (payload-id event :thread_id)
      (payload-id event :schedule/id)
      (id-segment (get-in event [:event/generator :schedule/id]))
      (id-segment (:event/id event))
      "event"))

(defn agent-source-config
  "Source/session config declared on the agent contract under :data :source."
  [resolved]
  (or (get-in resolved [:contract :data :source]) {}))

(defn sticky-session-source?
  "True when the agent contract asks event runs to share one session per
   trigger+scope instead of minting a fresh session for every event."
  [source]
  (boolean (or (:stickySession source)
               (:sticky-session source)
               (:sticky_session source))))

(defn- session-max-messages
  [source]
  (let [value (or (:sessionMaxMessages source)
                  (:session-max-messages source)
                  (:session_max_messages source))]
    (when (and (number? value) (pos? value))
      value)))

(defn sticky-context-policy
  "Context policy for the spawned agent: the contract's own policy, with the
   source's sessionMaxMessages as the message cap when none is set."
  [resolved source]
  (let [base (:context-policy resolved)
        max-messages (session-max-messages source)
        has-cap? (some #(get base %) [:max-messages :maxMessages :max_messages])]
    (if (and max-messages (not has-cap?))
      (assoc (or base {}) :max-messages max-messages)
      base)))

(defn triggered-session-identifiers
  "Return run, conversation, and session ids for event-triggered agent actions.

   Default ids embed the spawn timestamp so concurrent runs never collide.
   With {:sticky? true} the conversation and session ids drop the timestamp so
   every event for the same trigger+scope (e.g. Discord channel) continues one
   persistent session; only the run id stays unique per spawn."
  ([trigger event agent-id ts]
   (triggered-session-identifiers trigger event agent-id ts nil))
  ([trigger event agent-id ts {:keys [sticky?]}]
   (let [trigger-id' (trigger-id trigger event agent-id)
         scope-id (event-scope-id event)
         run-id (str "trigger-" trigger-id' "-" ts)]
     {:trigger-id trigger-id'
      :event-scope-id scope-id
      :run-id run-id
      :conversation-id (if sticky?
                         (str "trigger-" trigger-id' "-sticky-" scope-id)
                         (str run-id "-" scope-id))
      :session-id (if sticky?
                    (str "trigger-session-" trigger-id' "-" scope-id)
                    (str "trigger-session-" trigger-id' "-" scope-id "-" ts))})))

(defn triggered-audit-metadata
  "Return audit metadata that should follow an event-triggered run into the run store and OpenPlanner."
  [_trigger event ids]
  (let [event-types (->> (:event/types event)
                         (keep qualified-name)
                         distinct
                         vec)
        schedule-id (or (payload-id event :schedule/id)
                        (id-segment (get-in event [:event/generator :schedule/id])))]
    (cond-> {:trigger_id (:trigger-id ids)
             :event_scope_id (:event-scope-id ids)}
      (:event/id event) (assoc :event_id (:event/id event))
      (:event/type event) (assoc :event_type (qualified-name (:event/type event)))
      (seq event-types) (assoc :event_types event-types)
      schedule-id (assoc :schedule_id schedule-id))))

(defmethod run-action! :actions/start-agent-session
  [{:keys [config event trigger] :as ctx} action]
  (let [agent-id (action-agent-id ctx action)
        resolved (tooling/resolve-agent-contract config agent-id (actor-id ctx nil))
        actor-id' (actor-id ctx resolved)
        ts (.now js/Date)
        source (agent-source-config resolved)
        ids (triggered-session-identifiers trigger event agent-id ts
                                           {:sticky? (sticky-session-source? source)})
        task-input (action-task-input action trigger resolved)
        resource-policies (action-resource-policies ctx action)
        rendered-message (render-start-message trigger event task-input (:trigger-id ids))]
    (when (execution-snapshot-from-event? action)
      (assert-event-execution! event agent-id resolved))
    (when (:deprecated-agent-task-fallback? task-input)
      (errors/log-warning!
       :action/start-agent-session.deprecated-agent-task-prompt
       {:agent-id agent-id
        :actor-id actor-id'
        :trigger-id (:trigger-id ids)
        :event-id (:event/id event)}))
    (agents-runner/spawn-direct!
     config
     {:conversation_id (:conversation-id ids)
      :session_id (:session-id ids)
      :run_id (:run-id ids)
      :message rendered-message
      :agent_spec (merge (cond-> {:contract_id agent-id
                                  :actor_id actor-id'
                                  :role (:role resolved)
                                  :system_prompt (:system-prompt resolved)
                                  :model (:model resolved)
                                  :thinking_level (:thinking-level resolved)
                                  :tool_policies (:tool-policies resolved)
                                  :sources (:sources resolved)
                                  :memory_hydration (:memory-hydration resolved)
                                  :context_policy (sticky-context-policy resolved source)
                                  :task_source (some-> (:task-source task-input) qualified-name)
                                  :rendered_task_prompt (:task task-input)
                                  :deprecated_agent_task_fallback (:deprecated-agent-task-fallback? task-input)}
                           (some? resource-policies)
                           (assoc :resource_policies resource-policies))
                         (triggered-audit-metadata trigger event ids))
      :model (:model resolved)})))

(defmethod run-action! :actions/start-agent
  [ctx action]
  (run-action! ctx (assoc action :action/kind :actions/start-agent-session)))

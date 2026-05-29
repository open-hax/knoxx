(ns knoxx.backend.domain.action.start-agent-session
  "Agent session lifecycle actions."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.action.registry :refer [run-action!]]
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
        (get payload (keyword (name k))))))

(defn- qualified-name
  [value]
  (cond
    (keyword? value) (if-let [ns (namespace value)]
                       (str ns "/" (name value))
                       (name value))
    (nil? value) nil
    :else (some-> value str str/trim not-empty)))

(defn- start-message
  [trigger event resolved trigger-id]
  (let [payload (:event/payload event)
        task-prompt (:task-prompt resolved)]
    (str "Event: " (qualified-name (:event/type event)) "\n"
         "Trigger: " (or (:trigger/id trigger) trigger-id) "\n"
         "Reason: " (or (get-in trigger [:trigger/context :reason]) "trigger action") "\n"
         "Channel ID: " (or (payload-value event :channelId) (payload-value event :channel-id) "") "\n"
         "Author: " (or (payload-value event :authorUsername) (payload-value event :author-username) "") "\n"
         "Content: " (or (:content payload) "") "\n\n"
         (when-not (str/blank? (str task-prompt))
           (str "Agent task prompt:\n" task-prompt "\n")))))

(defn- action-agent-id
  [ctx action]
  (or (nonblank (get-in action [:action/with :agent-id]))
      (nonblank (get-in action [:action/with :agentId]))
      (nonblank (:agent/id ctx))
      (nonblank (:agent/contract ctx))))

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

(defn triggered-session-identifiers
  "Return collision-resistant run, conversation, and session ids for event-triggered agent actions."
  [trigger event agent-id ts]
  (let [trigger-id' (trigger-id trigger event agent-id)
        scope-id (event-scope-id event)
        run-id (str "trigger-" trigger-id' "-" ts)]
    {:trigger-id trigger-id'
     :event-scope-id scope-id
     :run-id run-id
     :conversation-id (str run-id "-" scope-id)
     :session-id (str "trigger-session-" trigger-id' "-" scope-id "-" ts)}))

(defn triggered-audit-metadata
  "Return audit metadata that should follow an event-triggered run into Redis and OpenPlanner."
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
        ids (triggered-session-identifiers trigger event agent-id ts)]
    (agents-runner/spawn-direct!
     config
     {:conversation_id (:conversation-id ids)
      :session_id (:session-id ids)
      :run_id (:run-id ids)
      :message (start-message trigger event resolved (:trigger-id ids))
      :agent_spec (merge {:contract_id agent-id
                          :actor_id actor-id'
                          :role (:role resolved)
                          :system_prompt (:system-prompt resolved)
                          :task_prompt (:task-prompt resolved)
                          :model (:model resolved)
                          :thinking_level (:thinking-level resolved)
                          :tool_policies (:tool-policies resolved)
                          :sources (:sources resolved)
                          :memory_hydration (:memory-hydration resolved)
                          :context_policy (:context-policy resolved)}
                         (triggered-audit-metadata trigger event ids))
      :model (:model resolved)})))

(defmethod run-action! :actions/start-agent
  [ctx action]
  (run-action! ctx (assoc action :action/kind :actions/start-agent-session)))

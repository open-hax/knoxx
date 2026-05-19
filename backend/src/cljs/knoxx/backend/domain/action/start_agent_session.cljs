(ns knoxx.backend.domain.action.start-agent-session
  "Agent session lifecycle actions."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.action.registry :refer [run-action!]]
            [knoxx.backend.domain.agent.runner :as agents-runner]
            [knoxx.backend.infra.tooling :as tooling]))

(defn- nonblank
  [value]
  (some-> value str str/trim not-empty))

(defn- payload-value
  [event k]
  (let [payload (:event/payload event)]
    (or (get payload k)
        (get payload (keyword (name k))))))

(defn- start-message
  [trigger event resolved]
  (let [payload (:event/payload event)
        task-prompt (:task-prompt resolved)]
    (str "Event: " (name (:event/type event)) "\n"
         "Trigger: " (:trigger/id trigger) "\n"
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

(defmethod run-action! :actions/start-agent-session
  [{:keys [config event trigger] :as ctx} action]
  (let [agent-id (action-agent-id ctx action)
        resolved (tooling/resolve-agent-contract config agent-id (actor-id ctx nil))
        actor-id' (actor-id ctx resolved)
        run-id (str "trigger-" (:trigger/id trigger) "-" (.now js/Date))
        channel-id (or (payload-value event :channelId)
                       (payload-value event :channel-id)
                       "event")]
    (agents-runner/spawn-direct!
     config
     {:conversation_id (str "trigger-" (:trigger/id trigger) "-" channel-id)
      :session_id (str "trigger-session-" (:trigger/id trigger) "-" channel-id)
      :run_id run-id
      :message (start-message trigger event resolved)
      :agent_spec {:contract_id agent-id
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
      :model (:model resolved)})))

(defmethod run-action! :actions/start-agent
  [ctx action]
  (run-action! ctx (assoc action :action/kind :actions/start-agent-session)))

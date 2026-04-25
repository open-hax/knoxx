(ns knoxx.backend.actions.invoke-agent
  (:require [knoxx.backend.actions.dispatch :refer [run-action!]]))

(defmethod run-action! :invoke/agent
  [{:keys [run-agent! config event job]} action]
  (let [job-agent-spec (:agentSpec job {})
        action-with (:action/with action {})
        merged-spec (merge job-agent-spec action-with)
        job-with-agent (assoc job :agentSpec merged-spec)]
    (run-agent! config job-with-agent event)))
(ns knoxx.backend.domain.action.loader)

(def well-known-actions
  {"run-agent" {:action/id "run-agent"
                 :action/kind :actions/start-agent-session
                 :action/label "Start Agent Session"}
   "start-agent-session" {:action/id "start-agent-session"
                          :action/kind :actions/start-agent-session
                          :action/label "Start Agent Session"}
   "invoke-sub-agent" {:action/id "invoke-sub-agent"
                         :action/kind :invoke/sub-agent
                         :action/label "Invoke Sub-Agent"}})

(defn resolve-action!
  [_config {:keys [uses with] :as _step-spec}]
  (let [base (or (get well-known-actions uses)
                 {:action/id uses
                  :action/kind :invoke/noop
                  :action/label (str "Unknown action: " uses)})]
    (js/Promise.resolve
     (if with
       (assoc base :action/with (merge (:action/with base {}) with))
       base))))

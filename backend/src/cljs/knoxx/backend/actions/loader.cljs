(ns knoxx.backend.actions.loader)

(def well-known-actions
  {"run-agent" {:action/id "run-agent"
                :action/kind :invoke/agent
                :action/label "Run Knoxx Agent"}})

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
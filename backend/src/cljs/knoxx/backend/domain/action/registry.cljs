(ns knoxx.backend.domain.action.registry
  "Action dispatch table.

   The generic resource registry protocol lives in
   knoxx.backend.domain.registry.resource. This namespace is only the action
   interpreter table: it maps an action key to executable behavior.

   Built-in actions:
     :actions/start-agent-session — spawn an agent session
     :actions/run-pipeline        — execute a pipeline resource
     :actions/noop                — no-op, succeeds immediately")

(defmulti run-action!
  "Dispatch an action map by :action/kind."
  (fn [_ctx action] (:action/kind action)))

(defn action-map
  "Build an action map from a normalized trigger contract."
  [trigger]
  (let [kind (:trigger/action trigger)]
    {:action/id (when (keyword? kind) (name kind))
     :action/kind kind
     :action/with (merge (:trigger/with trigger)
                         (when-let [agent-id (:trigger/agent trigger)]
                           {:agent-id agent-id}))}))

(defmethod run-action! :invoke/noop [_ _]
  (js/Promise.resolve {:ok true :action/kind :invoke/noop}))

(defmethod run-action! :actions/noop [_ _]
  (js/Promise.resolve {:ok true :action/kind :actions/noop}))

(defmethod run-action! :default [_ctx action]
  (let [kind (:action/kind action)]
    (if (string? kind)
      (js/console.warn "[knoxx/actions] string actions are not supported; use a keyword from the action registry. Got:" kind)
      (js/console.warn "[knoxx/actions] unknown action/kind" (pr-str kind)))
    (js/Promise.resolve {:ok false :error "unknown action/kind" :action/kind kind})))

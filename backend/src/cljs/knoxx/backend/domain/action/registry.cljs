(ns knoxx.backend.actions.registry)

(defmulti run-action!
  "Dispatch an action map by :action/kind."
  (fn [_ctx action] (:action/kind action)))

(defmethod run-action! :invoke/noop [_ _]
  (js/Promise.resolve {:ok true :action/kind :invoke/noop}))

(defmethod run-action! :default [_ctx action]
  (js/console.warn "[knoxx/actions] unknown action/kind" (pr-str (:action/kind action)))
  (js/Promise.resolve {:ok false :error "unknown action/kind"}))

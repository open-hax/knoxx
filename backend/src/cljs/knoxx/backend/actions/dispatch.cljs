(ns knoxx.backend.actions.dispatch
  (:require [shadow.cljs.modern :refer [js-await]]
            [knoxx.backend.actions.loader :as loader]
            [knoxx.backend.actions.contract :as contract]))

(defmulti run-action!
  (fn [_ctx action] (:action/kind action)))

(defmethod run-action! :invoke/noop [_ _]
  (js/Promise.resolve {:ok true :action/kind :invoke/noop}))

(defmethod run-action! :default [_ctx action]
  (js/console.warn "[knoxx/actions] unknown action/kind" (pr-str (:action/kind action)))
  (js/Promise.resolve {:ok false :error "unknown action/kind"}))

(defn dispatch!
  [ctx step-spec]
  (js-await [action (loader/resolve-action! (:config ctx) step-spec)]
    (when-not (contract/validate-action action)
      (throw (ex-info "Invalid ActionContract"
                      {:explain (contract/explain-action action)
                       :value action})))
    (run-action! ctx action)))
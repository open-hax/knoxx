(ns knoxx.backend.domain.action.dispatch
  (:require [knoxx.backend.domain.action.loader :as loader]
            [knoxx.backend.law.actions :as contract]
            [knoxx.backend.domain.action.registry :as registry]
            [knoxx.backend.domain.action.invoke-agent]        ;; side-effect: registers :invoke/agent defmethod
            [knoxx.backend.domain.action.invoke-sub-agent]
            [knoxx.backend.domain.action.start-agent-session]))  ;; side-effect: registers :actions/start-agent-session defmethod

(defn ^:async dispatch!
  [ctx step-spec]
  (let [action (await (js/Promise.resolve (loader/resolve-action! (:config ctx) step-spec)))]
    (when-not (contract/validate-action action)
      (throw (ex-info "Invalid ActionContract"
                      {:explain (contract/explain-action action)
                       :value action})))
    (registry/run-action! ctx action)))

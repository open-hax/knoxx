(ns knoxx.backend.actions.dispatch
  (:require [shadow.cljs.modern :refer [js-await]]
            [knoxx.backend.actions.loader :as loader]
            [knoxx.backend.actions.contract :as contract]
            [knoxx.backend.actions.registry :as registry]
            [knoxx.backend.actions.invoke-agent]        ;; side-effect: registers :invoke/agent defmethod
            [knoxx.backend.actions.invoke-sub-agent]))  ;; side-effect: registers :invoke/sub-agent defmethod

(defn dispatch!
  [ctx step-spec]
  (js-await [action (loader/resolve-action! (:config ctx) step-spec)]
    (when-not (contract/validate-action action)
      (throw (ex-info "Invalid ActionContract"
                      {:explain (contract/explain-action action)
                       :value action})))
    (registry/run-action! ctx action)))

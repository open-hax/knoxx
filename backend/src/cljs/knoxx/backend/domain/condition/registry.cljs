(ns knoxx.backend.domain.condition.registry
  "Delegates to the contract-runtime condition registry.
   Re-exports all public vars for backward compatibility."
  (:require [open-hax.contract-runtime.condition.registry :as core]))

(def register-condition! core/register-condition!)
(def condition-fn core/condition-fn)
(def condition-ids core/condition-ids)
(def safe-eval core/safe-eval)
(def evaluate core/evaluate)

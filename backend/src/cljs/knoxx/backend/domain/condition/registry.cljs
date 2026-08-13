(ns knoxx.backend.domain.condition.registry
  "Delegates to the katamorph condition registry.
   Re-exports all public vars for backward compatibility."
  (:require [katamorph.condition.registry :as core]))

(def register-condition! core/register-condition!)
(def condition-fn core/condition-fn)
(def condition-ids core/condition-ids)
(def safe-eval core/safe-eval)
(def evaluate core/evaluate)

(ns knoxx.backend.domain.agent.agent-context
  "Delegates to the contract-runtime agent context module.
   Re-exports all public vars for backward compatibility."
  (:require [open-hax.contract-runtime.agent.context :as core]))

(def set-context! core/set-context!)
(def clear-context! core/clear-context!)
(def get-context core/get-context)

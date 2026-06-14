(ns knoxx.backend.domain.agent.turn-guards
  "Delegates to the contract-runtime agent turn-guards module.
   Re-exports all public vars for backward compatibility."
  (:require [open-hax.contract-runtime.agent.turn-guards :as core]))

(def default-death-spiral-streak-limit core/default-death-spiral-streak-limit)
(def default-death-spiral-total-limit core/default-death-spiral-total-limit)
(def empty-tool-loop-state core/empty-tool-loop-state)
(def observe-tool-call core/observe-tool-call)

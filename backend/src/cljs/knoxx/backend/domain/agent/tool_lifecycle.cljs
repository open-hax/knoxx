(ns knoxx.backend.domain.agent.tool-lifecycle
  "Delegates to the katamorph agent tool-lifecycle module.
   Re-exports all public vars for backward compatibility."
  (:require [katamorph.agent.tool-lifecycle :as core]))

(def empty-tool-call-id-state core/empty-tool-call-id-state)
(def resolve-tool-call-start-id core/resolve-tool-call-start-id)
(def active-tool-call-id core/active-tool-call-id)
(def start-receipt core/start-receipt)
(def update-receipt core/update-receipt)
(def end-receipt core/end-receipt)
(def trace-event core/trace-event)
(def run-event-extra core/run-event-extra)

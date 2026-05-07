(ns knoxx.backend.tools.events
  "Preferred custom-tool entrypoint for the generic events surface.

   Delegates to the legacy event-agent tool factory while vocabulary and
   internals continue migrating."
  (:require [knoxx.backend.tools.event-agents :as legacy-event-tools]))

(defn create-events-custom-tools
  ([runtime config]
   (legacy-event-tools/create-event-agent-custom-tools runtime config))
  ([runtime config auth-context]
   (legacy-event-tools/create-event-agent-custom-tools runtime config auth-context)))

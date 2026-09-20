(ns knoxx.backend.infra.run-event-payload
  "Create immutable runtime occurrences once, independently from persistence attempts."
  (:require [knoxx.backend.extern.clock :as clock]
            [knoxx.backend.extern.run-event :as event]
            [knoxx.backend.law.run-store :as law]))

(defn tool-event-payload
  "Mint an occurrence ID even when another event has identical data and timestamp.
   Pass the returned map unchanged when retrying a durable write."
  [run-id conversation-id session-id type extra]
  (law/require! [:maybe :map] extra)
  (doseq [coordinate [run-id conversation-id session-id]]
    (law/require! [:maybe law/NonBlank] coordinate))
  (law/require! law/NonBlank type)
  (merge extra {:run_id run-id :conversation_id conversation-id :session_id session-id
                :type type :at (clock/instant-iso) :event_id (event/new-id)}))

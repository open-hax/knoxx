(ns knoxx.backend.infra.agent.tool-context
  "Request-local captured agent context, distinct from an operator MCP scope."
  (:require [knoxx.backend.extern.async-local-storage :as local]
            [knoxx.backend.law.platform-action :as law]))

(defonce ^:private store (local/create-store))

(defn run-with!
  "Carry the trusted agent/run ports across awaits without a process-global atom."
  [context invoke]
  (law/assert-valid! law/Context {:conversation-id (:conversation-id context)
                                  :actor-id (get-in context [:agent-spec :actor-id])})
  (local/run-with store context invoke))

(defn current
  "Return only the context of this wrapped agent tool call; direct MCP has none."
  []
  (local/current store))

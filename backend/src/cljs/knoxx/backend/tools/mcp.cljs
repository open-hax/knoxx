(ns knoxx.backend.tools.mcp
  "MCP bridge tool factory."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.domain.mcp.mcp-bridge :as mcp]))

(defn create-mcp-custom-tools
  "Create agent SDK custom tools for all connected MCP servers.
   Returns a JS array of tool objects, or an empty array if MCP is disabled."
  ([runtime config] (create-mcp-custom-tools runtime config nil))
  ([_runtime config auth-context]
   (if (and (:mcp-enabled config) (mcp/available?) (mcp/enabled?))
     (let [tools (if-let [items (mcp/mcp-tools-for-agent)]
                   (if (array? items) (array-seq items) [])
                   [])]
       (into-array
        (if (nil? auth-context)
          tools
          (filter (fn [tool]
                    (when-let [tool-id (some-> tool (aget "name") str str/trim not-empty)]
                      (ctx-tool-allowed? auth-context tool-id)))
                  tools))))
     #js [])))


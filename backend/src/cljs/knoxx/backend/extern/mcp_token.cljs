(ns knoxx.backend.extern.mcp-token
  "The native token-record boundary consumed by the MCP route.

   Authentication policy and grant construction stay in CLJS data. The MCP
   route still integrates with persisted JSON token records and downstream
   JavaScript APIs, so it owns the single conversion into their native shape.")

(defn native-record
  "Convert one CLJS token record to the JavaScript object the MCP route owns."
  [record]
  (when record (clj->js record)))

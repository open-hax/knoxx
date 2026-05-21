(ns knoxx.backend.domain.mcp.client
  "MCP HTTP bridge client protocol.

   Covers outbound MCP streamable HTTP calls made through the bridge. The
   implementation owns timeout, header marshalling, and native response access.")

(defprotocol IMcpHttpClient
  (mcp-request! [client url opts]))

(ns knoxx.backend.shape.mcp-oauth-store
  "Finite OAuth persistence operations; no database query language crosses this protocol.")

(defprotocol IMcpOAuthStore
  "Explicit OAuth registration, credential admission and revocation provider."
  (oauth-read! [store operation arguments] "Read a named OAuth projection.")
  (oauth-admit! [store operation arguments] "Admit a named immutable OAuth operation."))

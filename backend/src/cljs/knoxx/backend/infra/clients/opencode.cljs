(ns knoxx.backend.infra.clients.opencode
  "OpenCode session API client protocol.

   Covers the local OpenCode ingestion source: `/global/health`,
   `/experimental/session`, and `/session/:id/message`.")

(defprotocol IOpenCodeClient
  (health! [client])
  (sessions! [client opts])
  (session-messages! [client session-id]))

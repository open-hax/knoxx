(ns knoxx.backend.infra.clients.graph
  "Graph-weaver/graph gateway client protocol.

   Covers local graph gateway `/graphql` and `/api/status` requests.")

(defprotocol IGraphClient
  (graphql! [client payload])
  (status! [client]))

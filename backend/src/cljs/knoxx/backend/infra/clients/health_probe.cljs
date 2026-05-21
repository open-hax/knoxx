(ns knoxx.backend.infra.clients.health-probe
  "HTTP health probe client protocol.

   Covers dashboard fan-out health checks to OpenPlanner, Proxx, ingestion,
   graph, Voxx/STT, and local auxiliary services.")

(defprotocol IHealthProbeClient
  (probe! [client url headers]))

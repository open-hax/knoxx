(ns knoxx.backend.infra.clients.ingestion
  "Ingestion service client protocol.

   Covers `/api/ingestion/*` browse/file/source/job calls currently proxied by
   `infra.routes.app`.")

(defprotocol IIngestionClient
  (browse! [client query-string])
  (file! [client query-string])
  (sources! [client])
  (jobs! [client query-string])
  (create-job! [client payload])
  (proxy! [client method path query-string payload]))

(ns knoxx.backend.domain.media.remote-client
  "Remote media fetch client protocol.

   Covers arbitrary URL downloads for workspace media, Discord attachments,
   SVG/image/audio/video references, and agent media materialization. The
   implementation owns user-agent headers, size limits, content-type handling,
   and native fetch/Response details.")

(defprotocol IRemoteMediaClient
  (fetch-bytes! [client url opts])
  (fetch-text! [client url opts])
  (fetch-data-url! [client url opts]))

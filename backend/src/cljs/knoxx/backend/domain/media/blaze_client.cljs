(ns knoxx.backend.domain.media.blaze-client
  "Blaze/media-generation provider client protocol.

   Covers generation requests and provider-hosted media URL downloads in
   `domain.media.blaze`.")

(defprotocol IBlazeClient
  (generate! [client modality payload attempt-context])
  (fetch-generated-media! [client url]))

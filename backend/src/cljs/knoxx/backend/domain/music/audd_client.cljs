(ns knoxx.backend.domain.music.audd-client
  "AudD/music-identification client protocol.

   Covers music-recognition HTTP requests and remote metadata lookups.")

(defprotocol IAudDClient
  (recognize! [client media])
  (fetch-url! [client url opts]))

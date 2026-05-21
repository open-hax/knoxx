(ns knoxx.backend.infra.clients.shibboleth
  "Shibboleth import client protocol.

   Covers `/api/chat/import` calls into the configured Shibboleth service.")

(defprotocol IShibbolethClient
  (import-chat! [client payload]))

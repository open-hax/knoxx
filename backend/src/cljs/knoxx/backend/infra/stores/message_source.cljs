(ns knoxx.backend.infra.stores.message-source)

(defprotocol IMessageSource
  (fetch-messages! [src conversation-id]
    "Returns Promise<vec<stored-session-message-map>>.
     Each map has at minimum :role and :content keys."))

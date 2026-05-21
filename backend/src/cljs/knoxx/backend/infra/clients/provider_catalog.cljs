(ns knoxx.backend.infra.clients.provider-catalog
  "Provider metadata/catalog client protocol.

   Covers provider/model catalog HTTP calls such as OpenRouter model lists.")

(defprotocol IProviderCatalogClient
  (models! [client]))

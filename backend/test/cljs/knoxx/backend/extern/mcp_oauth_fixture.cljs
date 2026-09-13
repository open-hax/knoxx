(ns knoxx.backend.extern.mcp-oauth-fixture
  "Isolated real Clio OAuth data for authenticated HTTP inventory tests."
  (:require [knoxx.backend.extern.mcp-oauth-store :as codec]
            [knoxx.backend.extern.provider-recovery-fixture :as disk]
            [knoxx.backend.infra.stores.clio-mcp-oauth :as clio]
            [knoxx.backend.infra.stores.mcp-oauth-dispatch :as dispatch]
            [knoxx.backend.infra.stores.mongo-mcp-oauth :as facade]))

(defn- grant [member]
  {:clientId "inventory-fixture-client" :membershipId member
   :axxiumPrincipalId (str "principal-" member) :axxiumEntityId (str "entity-" member)
   :orgSlug "inventory-org" :tools ["read"]
   :createdAt (disk/instant (disk/now-ms))
   :expiresAt (disk/instant (+ (disk/now-ms) 3600000))})

(defn ^:async with-tokens!
  "Install exact seeded grants and restore the prior provider even on failure."
  [tokens body]
  (let [directory (disk/temporary-directory)
        previous (dispatch/current)]
    (try
      (dispatch/install! (clio/open! {:directory directory}))
      (await (facade/set-client!
              "inventory-fixture-client"
              (codec/encode
               {:client_id "inventory-fixture-client"
                :client_name "Inventory fixture"
                :redirect_uris ["https://inventory.example.test/callback"]
                :token_endpoint_auth_method "none"
                :grant_types ["authorization_code"]
                :response_types ["code"]})))
      (doseq [[token member] tokens]
        (await (facade/set-token! token (codec/encode (grant member)) 3600 member)))
      (await (body))
      (finally
        (dispatch/install! previous)
        (disk/remove! directory)))))

(defn inventory-token-ids
  "Decode the existing HTTP inventory envelope without exposing native arrays."
  [payload]
  (mapv #(get (codec/decode %) :tokenId) (array-seq (aget payload "tokens"))))

(defn token-id
  "Return the expected public digest for one fixture bearer."
  [token]
  (codec/digest token))

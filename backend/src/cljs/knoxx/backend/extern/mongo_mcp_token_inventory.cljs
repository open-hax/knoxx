(ns knoxx.backend.extern.mongo-mcp-token-inventory
  "Keep legacy Mongo bearer values inside membership-scoped inventory operations."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.mcp-oauth-store :as codec]
            [knoxx.backend.extern.mongo :as mongo]
            [knoxx.backend.law.mcp-oauth :as revocation]
            [knoxx.backend.law.mcp-oauth-store :as law]))

(def ^:private grant-fields
  [:clientId :membershipId :axxiumPrincipalId :axxiumEntityId :orgSlug :tools
   :createdAt :expiresAt :userEmail :actorId])

(defn- collection! [db member]
  (law/validate! law/Text member)
  (when-not db
    (throw (ex-info "Mongo OAuth persistence is unavailable"
                    {:status 503 :code "mcp_oauth_provider_unavailable"})))
  (mongo/collection db "knoxx_mcp_tokens"))

(defn- token-id [row]
  (when-let [token (:access_token row)]
    (when (and (string? token) (not (str/blank? token))) (codec/digest token))))

(defn ^:async list-live!
  "Expose safe grant fields and a digest for current and pre-digest Mongo rows."
  [db member]
  (let [collection (collection! db member)
        rows (await (mongo/find-docs! collection {:membership_id member}))
        now (codec/now-ms)]
    (vec (for [row rows
               :let [id (token-id row)]
               :when (and id (revocation/credential-live? (mongo/instant-ms (:expiresAt row)) now))]
           (codec/encode (assoc (select-keys (:token_data row) grant-fields) :tokenId id))))))

(defn ^:async revoke-id!
  "Resolve a legacy digest within its member, then atomically recheck ownership.
  The member-scoped scan supports existing rows without requiring a migration."
  [db id member]
  (law/validate! law/Digest id)
  (let [collection (collection! db member)
        rows (await (mongo/find-docs! collection {:membership_id member}))]
    (if-let [row (some #(when (= id (token-id %)) %) rows)]
      (let [result (await (mongo/delete-one! collection
                                             {:access_token (:access_token row) :membership_id member}))]
        (when-not (revocation/valid-revocation-result? result)
          (throw (ex-info "Mongo token revocation returned an undecodable count"
                          {:status 503 :code "mcp_oauth_revocation_result_invalid"})))
        (pos? (:deleted-count result)))
      false)))

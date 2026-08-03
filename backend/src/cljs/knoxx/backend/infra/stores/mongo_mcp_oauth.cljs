(ns knoxx.backend.infra.stores.mongo-mcp-oauth
  "Mongo twin for MCP OAuth state (replaces Redis knoxx:mcp:* keys).
   Handles clients, auth codes, and access tokens."
  (:require [knoxx.backend.extern.mongo :as extern-mongo]
            [knoxx.backend.infra.mongo-client :as mongo-client]
            [knoxx.backend.infra.system-instance :as system-instance]
            [knoxx.backend.law.mcp-oauth :as law]))

(def CLIENTS_COLLECTION "knoxx_mcp_clients")
(def CODES_COLLECTION "knoxx_mcp_codes")
(def TOKENS_COLLECTION "knoxx_mcp_tokens")

(defn- clients-coll [db] (.collection db CLIENTS_COLLECTION))
(defn- codes-coll [db] (.collection db CODES_COLLECTION))
(defn- tokens-coll [db] (.collection db TOKENS_COLLECTION))

(defn ^:async setup-indexes!
  "Create required indexes. Idempotent."
  [db]
  (let [clients (clients-coll db)
        codes (codes-coll db)
        tokens (tokens-coll db)]
    ;; Clients: unique index on client_id
    (await (.createIndex clients #js {"client_id" 1} #js {"unique" true}))
    ;; Codes: unique index on code, TTL on expiresAt
    (await (.createIndex codes #js {"code" 1} #js {"unique" true}))
    (await (.createIndex codes #js {"expiresAt" 1} #js {"expireAfterSeconds" 0}))
    ;; Tokens: unique index on access_token, TTL on expiresAt, index on membership_id
    (await (.createIndex tokens #js {"access_token" 1} #js {"unique" true}))
    (await (.createIndex tokens #js {"expiresAt" 1} #js {"expireAfterSeconds" 0}))
    (await (.createIndex tokens #js {"membership_id" 1}))
    true))

(defn- keywordize [doc]
  (when doc (js->clj doc :keywordize-keys true)))

(defn- live?
  "True when a document has a readable expiry that is still in the future.

   Reads :expiresAt — the key the writers actually use. The readers previously
   asked for :expires-at, which no document has ever carried, so the default of
   0 made every code and every token read as already expired: the token
   exchange answered 'Unknown or expired code' for codes it had just minted,
   and no access token could ever be presented successfully.

   Each layer does its own part: extern.mongo decodes the driver's instant,
   law.mcp-oauth decides what a decoded instant means, and this reads the
   clock. An unreadable or missing expiry is not live, so the check fails
   closed."
  [doc]
  (law/credential-live? (extern-mongo/instant-ms (:expiresAt doc))
                        (.now js/Date)))

;; ─── Clients ────────────────────────────────────────────────────────────────

(defn ^:async get-client!
  "Read a registered OAuth client by client_id.

   Returns the client record itself, not the storage envelope. set-client!
   nests the registration under :client_data alongside bookkeeping fields, and
   callers want the registration — every caller reads redirect_uris off the top
   level of what this returns. Handing back the envelope made those lookups
   undefined, so every registered client's redirect_uri was rejected and no MCP
   OAuth flow could be completed.

   Records written before the envelope existed are stored flat; fall back to
   the whole document for those."
  ([client-id] (get-client! (mongo-client/get-db) client-id))
  ([db client-id]
   (when (and db client-id)
     (let [c (clients-coll db)
           result (await (.findOne c #js {"client_id" (str client-id)}))]
       (when result
         (let [doc    (keywordize result)
               record (or (:client_data doc) doc)]
           (js/JSON.stringify (clj->js record))))))))

(defn ^:async set-client!
  "Store a registered OAuth client.

   The registration is nested under :client_data, beside this store's own
   bookkeeping. get-client! unwraps it again; keep the two in step. Clients
   carry no TTL, unlike codes and tokens."
  ([client-id client-json] (set-client! (mongo-client/get-db) client-id client-json))
  ([db client-id client-json]
   (when (and db client-id)
     (let [c (clients-coll db)
           now (js/Date.)
           parsed (js/JSON.parse client-json)]
       (await (.updateOne
               c
               #js {"client_id" (str client-id)}
               #js {"$set" (clj->js {:client_data parsed})
                    "$setOnInsert" (clj->js {:created_at now
                                            :system_instance_id (system-instance/current-id)})}
               #js {"upsert" true}))
       true))))

;; ─── Codes ──────────────────────────────────────────────────────────────────

(defn ^:async get-code!
  "Read an OAuth auth code without consuming it.

   Does not make the code single use. A token exchange wants consume-code!;
   reading here and deleting afterwards lets two concurrent exchanges both pass
   the read before either deletes, and both then mint a token from one code."
  ([code] (get-code! (mongo-client/get-db) code))
  ([db code]
   (when (and db code)
     (let [c (codes-coll db)
           result (await (.findOne c #js {"code" (str code)}))]
       (when result
         (let [doc (keywordize result)]
           (when (live? doc)
             (js/JSON.stringify (clj->js (:code_data doc))))))))))

(defn ^:async consume-code!
  "Atomically claim an OAuth auth code, returning its data exactly once.

   An authorization code is single use: RFC 6749 requires that presenting one
   twice does not yield two credentials. The delete and the read are one
   operation so that exactly one of any number of concurrent exchanges can
   claim it — a read followed by a separate delete leaves a window in which
   both callers see a live code and both mint a token.

   The claim happens before the code's own validity is judged, so a code that
   turns out to be expired is still consumed. That is deliberate: an expired
   code is spent either way, and leaving it readable would invite retries."
  ([code] (consume-code! (mongo-client/get-db) code))
  ([db code]
   (when (and db code)
     (let [doc (await (extern-mongo/find-one-and-delete!
                       (codes-coll db) {:code (str code)}))]
       (when (and doc (live? doc))
         (js/JSON.stringify (clj->js (:code_data doc))))))))

(defn ^:async set-code!
  "Store OAuth auth code with TTL."
  ([code code-json ttl-seconds] (set-code! (mongo-client/get-db) code code-json ttl-seconds))
  ([db code code-json ttl-seconds]
   (when (and db code)
     (let [c (codes-coll db)
           now (js/Date.)
           parsed (js/JSON.parse code-json)
           doc {:code (str code)
                :code_data parsed
                :created_at now
                :system_instance_id (system-instance/current-id)
                :expiresAt (js/Date. (+ (.now js/Date) (* ttl-seconds 1000)))}]
       (await (.updateOne
               c
               #js {"code" (str code)}
               #js {"$set" (clj->js {:code_data parsed
                                     :expiresAt (:expiresAt doc)})
                    "$setOnInsert" (clj->js {:created_at now
                                            :system_instance_id (system-instance/current-id)})}
               #js {"upsert" true}))
       true))))

(defn ^:async delete-code!
  "Delete OAuth auth code."
  ([code] (delete-code! (mongo-client/get-db) code))
  ([db code]
   (when (and db code)
     (let [c (codes-coll db)]
       (await (.deleteOne c #js {"code" (str code)}))
       true))))

;; ─── Tokens ─────────────────────────────────────────────────────────────────

(defn ^:async get-token!
  "Read access token."
  ([access-token] (get-token! (mongo-client/get-db) access-token))
  ([db access-token]
   (when (and db access-token)
     (let [c (tokens-coll db)
           result (await (.findOne c #js {"access_token" (str access-token)}))]
       (when result
         (let [doc (keywordize result)]
           (when (live? doc)
             (js/JSON.stringify (clj->js (:token_data doc))))))))))

(defn ^:async set-token!
  "Store access token with TTL."
  ([access-token token-json ttl-seconds membership-id]
   (set-token! (mongo-client/get-db) access-token token-json ttl-seconds membership-id))
  ([db access-token token-json ttl-seconds membership-id]
   (when (and db access-token)
     (let [c (tokens-coll db)
           now (js/Date.)
           parsed (js/JSON.parse token-json)
           doc {:access_token (str access-token)
                :token_data parsed
                :membership_id (str (or membership-id ""))
                :created_at now
                :system_instance_id (system-instance/current-id)
                :expiresAt (js/Date. (+ (.now js/Date) (* ttl-seconds 1000)))}]
       (await (.updateOne
               c
               #js {"access_token" (str access-token)}
               #js {"$set" (clj->js {:token_data parsed
                                     :membership_id (str (or membership-id ""))
                                     :expiresAt (:expiresAt doc)})
                    "$setOnInsert" (clj->js {:created_at now
                                            :system_instance_id (system-instance/current-id)})}
               #js {"upsert" true}))
       true))))

(defn ^:async delete-token!
  "Delete an access token by value, without regard to who owns it.

   Unscoped on purpose, for callers that have already established ownership or
   legitimately act outside a membership. Anything reachable from a user
   request wants delete-token-for-membership! instead."
  ([access-token] (delete-token! (mongo-client/get-db) access-token))
  ([db access-token]
   (when (and db access-token)
     (let [c (tokens-coll db)]
       (await (.deleteOne c #js {"access_token" (str access-token)}))
       true))))

(defn ^:async delete-token-for-membership!
  "Delete an access token only when it belongs to this membership.

   Returns true when a token was deleted and false when nothing matched, so a
   caller can tell 'revoked' from 'not yours, or not there'. Deleting by token
   value alone lets any authenticated caller revoke another membership's token
   if they learn its value.

   Blank or missing identity yields false rather than a widened query — the
   delete is never issued at all."
  ([access-token membership-id]
   (delete-token-for-membership! (mongo-client/get-db) access-token membership-id))
  ([db access-token membership-id]
   (let [request {:access-token  (str (or access-token ""))
                  :membership-id (str (or membership-id ""))}]
     (if-not (and db (law/valid-revocation-request? request))
       false
       (let [result (await (extern-mongo/delete-one!
                            (tokens-coll db)
                            {:access_token  (:access-token request)
                             :membership_id (:membership-id request)}))]
         (when-not (law/valid-revocation-result? result)
           (throw (ex-info "mongo delete-one! returned an undecodable result"
                           {:result result})))
         (pos? (:deleted-count result)))))))

(defn ^:async list-tokens-for-membership!
  "List all tokens for a membership."
  ([membership-id] (list-tokens-for-membership! (mongo-client/get-db) membership-id))
  ([db membership-id]
   (when (and db membership-id)
     (let [c (tokens-coll db)
           cursor (.find c #js {"membership_id" (str membership-id)})
           results (await (.toArray cursor))]
       (vec (for [doc results
                  :let [d (keywordize doc)]
                  :when (live? d)]
              (js/JSON.stringify (clj->js (:token_data d)))))))))

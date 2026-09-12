(ns knoxx.backend.infra.stores.mongo-mcp-oauth
  "Mongo twin for MCP OAuth state (replaces Redis knoxx:mcp:* keys).
   Handles clients, auth codes, and access tokens."
  (:require [knoxx.backend.extern.mongo :as extern-mongo]
            [knoxx.backend.extern.mcp-oauth-store :as oauth-host]
            [knoxx.backend.infra.stores.mcp-oauth-dispatch :as dispatch]
            [knoxx.backend.law.mcp-oauth-store :as oauth-law]
            [knoxx.backend.shape.mcp-oauth-store :as oauth]
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

(defn ^:async mongo-get-client!
  "Read a registered OAuth client by client_id.

   Returns the client record itself, not the storage envelope. mongo-set-client!
   nests the registration under :client_data alongside bookkeeping fields, and
   callers want the registration — every caller reads redirect_uris off the top
   level of what this returns. Handing back the envelope made those lookups
   undefined, so every registered client's redirect_uri was rejected and no MCP
   OAuth flow could be completed.

   Records written before the envelope existed are stored flat; fall back to
   the whole document for those."
  ([client-id] (mongo-get-client! (mongo-client/get-db) client-id))
  ([db client-id]
   (when (and db client-id)
     (let [c (clients-coll db)
           result (await (.findOne c #js {"client_id" (str client-id)}))]
       (when result
         (let [doc    (keywordize result)
               record (or (:client_data doc) doc)]
           (js/JSON.stringify (clj->js record))))))))

(defn ^:async mongo-set-client!
  "Store a registered OAuth client.

   The registration is nested under :client_data, beside this store's own
   bookkeeping. mongo-get-client! unwraps it again; keep the two in step. Clients
   carry no TTL, unlike codes and tokens."
  ([client-id client-json] (mongo-set-client! (mongo-client/get-db) client-id client-json))
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

(defn ^:async mongo-peek-code!
  "Read a live OAuth auth code without spending it, as CLJS data or nil.

   Deliberately non-destructive, and paired with mongo-consume-code!: an exchange
   reads the code here to check the client, redirect and PKCE bindings, and
   only claims it once those hold. Spending it first would let anyone who
   merely observed the code on the front channel destroy it with a wrong
   verifier and break the legitimate client's exchange."
  ([code] (mongo-peek-code! (mongo-client/get-db) code))
  ([db code]
   (when (and db code)
     (let [c (codes-coll db)
           result (await (.findOne c #js {"code" (str code)}))]
       (when result
         (let [doc (keywordize result)]
           (when (live? doc)
             (:code_data doc))))))))

(defn ^:async mongo-consume-code!
  "Atomically claim an OAuth auth code, returning its data exactly once.

   An authorization code is single use: RFC 6749 requires that presenting one
   twice does not yield two credentials. The delete and the read are one
   operation so that exactly one of any number of concurrent exchanges can
   claim it — a read followed by a separate delete leaves a window in which
   both callers see a live code and both mint a token.

   Call this only once an exchange has been found admissible. The claim is what
   settles a race between two otherwise-valid exchanges; it is not the place to
   discover that a request was invalid, because a rejected request must not
   spend the code."
  ([code] (mongo-consume-code! (mongo-client/get-db) code))
  ([db code]
   (when (and db code)
     (let [doc (await (extern-mongo/find-one-and-delete!
                       (codes-coll db) {:code (str code)}))]
       (when (and doc (live? doc))
         (:code_data doc))))))

(defn ^:async mongo-set-code!
  "Store OAuth auth code with TTL."
  ([code code-json ttl-seconds] (mongo-set-code! (mongo-client/get-db) code code-json ttl-seconds))
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

(defn ^:async mongo-delete-code!
  "Delete OAuth auth code."
  ([code] (mongo-delete-code! (mongo-client/get-db) code))
  ([db code]
   (when (and db code)
     (let [c (codes-coll db)]
       (await (.deleteOne c #js {"code" (str code)}))
       true))))

;; ─── Tokens ─────────────────────────────────────────────────────────────────

(defn ^:async mongo-get-token!
  "Read access token."
  ([access-token] (mongo-get-token! (mongo-client/get-db) access-token))
  ([db access-token]
   (when (and db access-token)
     (let [c (tokens-coll db)
           result (await (.findOne c #js {"access_token" (str access-token)}))]
       (when result
         (let [doc (keywordize result)]
           (when (live? doc)
             (js/JSON.stringify (clj->js (:token_data doc))))))))))

(defn ^:async mongo-set-token!
  "Store access token with TTL."
  ([access-token token-json ttl-seconds membership-id]
   (mongo-set-token! (mongo-client/get-db) access-token token-json ttl-seconds membership-id))
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

(defn ^:async mongo-delete-token!
  "Delete an access token by value, without regard to who owns it.

   Unscoped on purpose, for callers that have already established ownership or
   legitimately act outside a membership. Anything reachable from a user
   request wants mongo-delete-token-for-membership! instead."
  ([access-token] (mongo-delete-token! (mongo-client/get-db) access-token))
  ([db access-token]
   (when (and db access-token)
     (let [c (tokens-coll db)]
       (await (.deleteOne c #js {"access_token" (str access-token)}))
       true))))

(defn ^:async mongo-delete-token-for-membership!
  "Delete an access token only when it belongs to this membership.

   Returns true when a token was deleted and false when nothing matched, so a
   caller can tell 'revoked' from 'not yours, or not there'. Deleting by token
   value alone lets any authenticated caller revoke another membership's token
   if they learn its value.

   Blank or missing identity yields false rather than a widened query — the
   delete is never issued at all."
  ([access-token membership-id]
   (mongo-delete-token-for-membership! (mongo-client/get-db) access-token membership-id))
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

(defn ^:async mongo-list-tokens-for-membership!
  "List all tokens for a membership."
  ([membership-id] (mongo-list-tokens-for-membership! (mongo-client/get-db) membership-id))
  ([db membership-id]
   (when (and db membership-id)
     (let [c (tokens-coll db)
           cursor (.find c #js {"membership_id" (str membership-id)})
           results (await (.toArray cursor))]
       (vec (for [doc results
                  :let [d (keywordize doc)]
                  :when (live? d)]
              (js/JSON.stringify (clj->js (:token_data d)))))))))

(def install! "Select the finite provider at bootstrap; explicit databases remain Mongo." dispatch/install!)

(defn- selected [db] (when (nil? db) (dispatch/current)))
(defn- lookup-arguments [raw] {:id (oauth-host/digest raw) :now (oauth-host/now-ms)})
(defn- credential-arguments [raw payload ttl]
  {:id (oauth-host/digest raw) :payload (oauth-host/decode payload) :ttl ttl :now (oauth-host/now-ms)})

(defn ^:async get-client!
  "Read the registered client through the explicitly selected provider."
  ([id] (await (get-client! nil id)))
  ([db id]
   (if-let [store (selected db)]
     (oauth-host/encode (await (oauth/oauth-read! store :oauth/client {:id id})))
     (await (mongo-get-client! (or db (mongo-client/get-db)) id)))))

(defn set-client!
  "Durably register a client; changed reuse of a local immutable id conflicts."
  ([id client-json] (set-client! nil id client-json))
  ([db id client-json]
   (if-let [store (selected db)]
     (oauth/oauth-admit! store :oauth/register {:id id :payload (oauth-host/decode client-json)})
     (mongo-set-client! (or db (mongo-client/get-db)) id client-json))))

(defn peek-code!
  "Read one live code without consuming it."
  ([code] (peek-code! nil code))
  ([db code]
   (if-let [store (selected db)]
     (oauth/oauth-read! store :oauth/code (lookup-arguments code))
     (mongo-peek-code! (or db (mongo-client/get-db)) code))))

(defn ^:async consume-code!
  "Atomically consume a code once; invalid PKCE must be refused before calling."
  ([code] (await (consume-code! nil code)))
  ([db code]
   (if-let [store (selected db)]
     (:payload (await (oauth/oauth-admit! store :oauth/consume-code (lookup-arguments code))))
     (await (mongo-consume-code! (or db (mongo-client/get-db)) code)))))

(defn set-code!
  "Store only a hash and closed grant metadata in the local ledger."
  ([code code-json ttl] (set-code! nil code code-json ttl))
  ([db code code-json ttl]
   (if-let [store (selected db)]
     (oauth/oauth-admit! store :oauth/issue-code (credential-arguments code code-json ttl))
     (mongo-set-code! (or db (mongo-client/get-db)) code code-json ttl))))

(defn delete-code!
  "Revoke an unspent local code without deleting its historical fact."
  ([code] (delete-code! nil code))
  ([db code]
   (if-let [store (selected db)]
     (oauth/oauth-admit! store :oauth/revoke-code {:id (oauth-host/digest code)})
     (mongo-delete-code! (or db (mongo-client/get-db)) code))))

(defn ^:async get-token!
  "Read a live token's safe grant metadata; raw bearer bytes are never returned."
  ([token] (await (get-token! nil token)))
  ([db token]
   (if-let [store (selected db)]
     (oauth-host/encode (await (oauth/oauth-read! store :oauth/token (lookup-arguments token))))
     (await (mongo-get-token! (or db (mongo-client/get-db)) token)))))

(defn- token-arguments [token token-json ttl membership-id]
  (let [arguments (credential-arguments token token-json ttl)]
    (oauth-law/require! (= membership-id (get-in arguments [:payload :membershipId])) :mcp-oauth-membership-mismatch)
    arguments))

(defn set-token!
  "Issue a scoped local grant; membership and payload ownership must agree."
  ([token token-json ttl member] (set-token! nil token token-json ttl member))
  ([db token token-json ttl member]
   (if-let [store (selected db)]
     (oauth/oauth-admit! store :oauth/issue-token (token-arguments token token-json ttl member))
     (mongo-set-token! (or db (mongo-client/get-db)) token token-json ttl member))))

(defn delete-token!
  "Revoke a token by its raw value for a caller that has established authority."
  ([token] (delete-token! nil token))
  ([db token]
   (if-let [store (selected db)]
     (oauth/oauth-admit! store :oauth/revoke-token {:id (oauth-host/digest token)})
     (mongo-delete-token! (or db (mongo-client/get-db)) token))))

(defn delete-token-for-membership!
  "Revoke a raw token only within its exact owning membership."
  ([token member] (delete-token-for-membership! nil token member))
  ([db token member]
   (if-let [store (selected db)]
     (do (oauth-law/validate! oauth-law/Text member)
         (oauth/oauth-admit! store :oauth/revoke-token {:id (oauth-host/digest token) :membership-id member}))
     (mongo-delete-token-for-membership! (or db (mongo-client/get-db)) token member))))

(defn delete-token-id-for-membership!
  "Revoke a safe local token digest from inventory without exposing its bearer."
  [token-id member]
  (oauth-law/validate! oauth-law/Digest token-id)
  (oauth-law/validate! oauth-law/Text member)
  (if-let [store (dispatch/current)]
    (oauth/oauth-admit! store :oauth/revoke-token {:id token-id :membership-id member})
    (throw (ex-info "Token-id revocation requires the selected local provider"
                    {:status 400 :code "mcp_oauth_token_id_provider_required"}))))

(defn ^:async list-tokens-for-membership!
  "Expose safe tokenId digests and grant metadata, never bearer values."
  ([member] (await (list-tokens-for-membership! nil member)))
  ([db member]
   (if-let [store (selected db)]
     (mapv oauth-host/encode (await (oauth/oauth-read! store :oauth/tokens {:membership-id member :now (oauth-host/now-ms)})))
     (await (mongo-list-tokens-for-membership! (or db (mongo-client/get-db)) member)))))

(defn ^:async exchange-code!
  "Consume and issue one local grant in the same durable operation.
   The Mongo compatibility path keeps its atomic claim, then persists the token."
  ([code expected token token-json ttl member]
   (await (exchange-code! nil code expected token token-json ttl member)))
  ([db code expected token token-json ttl member]
   (if-let [store (selected db)]
     (await (oauth/oauth-admit! store :oauth/exchange
             {:code-id (oauth-host/digest code) :expected expected
              :token (token-arguments token token-json ttl member)}))
     (when-let [claimed (await (mongo-consume-code! (or db (mongo-client/get-db)) code))]
       (oauth-law/require! (= expected claimed) :mcp-oauth-code-changed)
       (when-not (await (mongo-set-token! (or db (mongo-client/get-db)) token token-json ttl member))
         (throw (ex-info "Token persistence failed after code claim; restart authorization"
                         {:status 503 :code "mcp_oauth_token_persistence_failed"})))
       {:accepted? true}))))

(ns knoxx.backend.infra.routes.mcp
  "Serve Knoxx tools over MCP (Model Context Protocol) Streamable HTTP."
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]
            [knoxx.backend.shape.app-shapes :refer [route!]]
            [knoxx.backend.domain.actor.acting :as actor-acting]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.auth.session :as auth-session]
            [knoxx.backend.infra.db.policy :as db-policy]
            [knoxx.backend.infra.routes.mcp.consent :as consent]
            [knoxx.backend.infra.routes.mcp.params :as params]
            [knoxx.backend.infra.routes.mcp.transport :as transport]
            [knoxx.backend.domain.mcp.mcp-expose :as mcp-expose]
            [knoxx.backend.infra.stores.mongo-mcp-oauth :as mongo-mcp]
            [knoxx.backend.law.mcp-oauth :as law]
            [knoxx.backend.law.mcp-tool-annotations :as tool-annotations]
            [knoxx.backend.runtime.state :as runtime-state]
            ["@modelcontextprotocol/sdk/server/mcp.js" :refer [McpServer]]
            ["@modelcontextprotocol/sdk/server/streamableHttp.js" :refer [StreamableHTTPServerTransport]]
            ["node:crypto" :as crypto]
            ["zod" :refer [z]])
  (:require-macros [knoxx.backend.macros :refer [defroute]]))

(declare typebox->zod-shape reply-header!)

(defonce ^:private mcp-sessions* (atom {}))

(defn- env [k default] (or (aget js/process.env k) default))

(defn- public-base-url
  [config]
  (try
    (js/URL.
     (or (aget js/process.env "KNOXX_PUBLIC_BASE_URL")
         (aget js/process.env "RENDER_EXTERNAL_URL")
         (:knoxx-base-url config)
         "http://localhost"))
    (catch :default _ (js/URL. "http://localhost"))))

(defn- base64url [buf] (.toString (js/Buffer.from buf) "base64url"))

(defn- pkce-challenge
  [^js crypto verifier]
  (base64url (-> (.createHash crypto "sha256") (.update (str verifier)) (.digest))))

(defn- json-send! [reply status payload]
  (-> (.code reply status) (.send (clj->js payload))))

(defn- text-send! [reply status body]
  (-> (.code reply status) (.send body)))

(defn- protected-resource-metadata
  "RFC 9728 metadata for the MCP resource. One document, served at every
   well-known location a client may look in.

   Checked against law.mcp-oauth/ProtectedResourceMetadata before it leaves: the
   payload is derived from the public base URL, and a client handed a blank
   resource or an empty server list has nowhere to go and no way to say so."
  [base]
  (let [issuer  (-> (.toString (js/URL. (.toString base))) (.replace (js/RegExp. "/$") ""))
        payload {:resource                 (.toString (js/URL. "/mcp" base))
                 :authorization_servers    [issuer]
                 :scopes_supported         ["mcp:tools"]
                 :bearer_methods_supported ["header"]}]
    (when-not (law/valid-protected-resource-metadata? payload)
      (throw (params/http-error 500 "metadata_unavailable"
                         "protected resource metadata failed its contract")))
    payload))

(defn- tool-execute! [^js tool params] (.execute tool "mcp" params nil nil nil))

(defn- reply-header! [^js reply name value] (.header reply name value))

(defn- ^:async browser-auth-ctx!
  [req policy-db config]
  (try
    (let [auth-ctx (await (auth-session/resolve-auth-context req policy-db))]
      (aset req "authContext" auth-ctx)
      nil)
    (catch :default _
      (let [base (public-base-url config)
            current-path (or (some-> req (aget "raw") (aget "url")) "/api/mcp/oauth/authorize")
            login-url (js/URL. "/api/auth/login" base)]
        (.set (.-searchParams login-url) "redirect" current-path)
        {:redirect (.toString login-url)}))))

(defn- require-browser-auth!
  "Returns a Fastify preHandler hook that resolves browser auth context onto request.authContext."
  [policy-db config]
  (^:async fn [req reply]
    (let [result (await (browser-auth-ctx! req policy-db config))]
      (when result
        (.redirect reply (:redirect result) 302)))))

(defn- require-bearer-token!
  "Returns a Fastify preHandler hook that extracts bearer token onto request.bearerToken."
  [base]
  (fn [req reply done]
    (let [token (transport/bearer-token req)]
      (if (str/blank? token)
        (transport/challenge-unauthorized! reply base)
        (do (aset req "bearerToken" token) (done))))))

;; ──────────────────────────────────────────────────────────────
;; Business logic helpers
;; ──────────────────────────────────────────────────────────────

(defn- ensure-oauth-request! [{:keys [client-id redirect-uri code-challenge code-challenge-method]}]
  (when (or (str/blank? client-id) (str/blank? redirect-uri)
            (str/blank? code-challenge) (not= code-challenge-method "S256"))
    (throw (params/http-error 400 "invalid_request"
                       "Missing required OAuth parameters (client_id, redirect_uri, code_challenge, S256)"))))

(defn- ensure-oauth-confirm-request! [{:keys [client-id redirect-uri code-challenge code-challenge-method]}]
  (when (or (str/blank? client-id) (str/blank? redirect-uri)
            (str/blank? code-challenge) (not= code-challenge-method "S256"))
    (throw (params/http-error 400 "invalid_request" "Missing required OAuth parameters"))))

(defn- ^:async get-registered-client [client-id]
  (if (str/blank? (str client-id))
    nil
    (try
      (let [raw (await (mongo-mcp/get-client! client-id))]
        (when raw (try (js/JSON.parse raw) (catch :default _ nil))))
      (catch :default _
        nil))))

(defn- redirect-uri-allowed? [client redirect-uri]
  (if-not client true
    (boolean (.includes (js/Array.from (or (aget client "redirect_uris") (js/Array.))) redirect-uri))))

(defn- ensure-redirect-uri-allowed! [client redirect-uri error-code]
  (when (and client (not (redirect-uri-allowed? client redirect-uri)))
    (throw (params/http-error 400 error-code "redirect_uri not allowed for registered client"))))

(defn- available-tools [runtime config auth-context]
  (or (mcp-expose/create-knoxx-custom-tools-js runtime config auth-context) (js/Array.)))

(defn- tool-name-set [tools]
  (into #{} (keep (fn [t] (some-> (aget t "name") str str/trim not-empty))) (array-seq tools)))

(defn- selected-tools-from-scope [tools requested-scope]
  (let [requested (into #{} (comp (map str/trim) (remove str/blank?))
                        (str/split (str (or requested-scope "")) #"\s+"))]
    (into #{} (keep (fn [t]
                      (let [n (some-> (aget t "name") str str/trim not-empty)]
                        (when (and n (or (contains? requested "all") (contains? requested n))) n))))
          (array-seq tools))))

(defn- default-selected-tools [tool-names]
  (into #{} (filter #(contains? tool-names %))
        ["semantic_query" "semantic_read" "memory_search"
         "memory_session" "graph_query" "websearch" "read"]))

(defn- requested-tools [runtime config auth-context selected-tools]
  (let [tools     (available-tools runtime config auth-context)
        available (tool-name-set tools)]
    (->> selected-tools
         (map (comp str/trim str))
         (remove str/blank?)
         distinct
         (filter #(contains? available %))
         vec)))

(defn- ^:async load-token-record! [access-token]
  (if (str/blank? (str access-token))
    nil
    (try
      (let [raw (await (mongo-mcp/get-token! access-token))]
        (when raw (try (js/JSON.parse raw) (catch :default _ nil))))
      (catch :default _
        nil))))

(defn- resolve-token-context! [policy-context token-record]
  (let [headers-like (cond-> {}
                       (aget token-record "membershipId")
                       (assoc "x-knoxx-membership-id" (aget token-record "membershipId"))
                       (aget token-record "userEmail")
                       (assoc "x-knoxx-user-email" (aget token-record "userEmail"))
                       (aget token-record "orgSlug")
                       (assoc "x-knoxx-org-slug" (aget token-record "orgSlug")))]
    (db-policy/resolve-context! policy-context headers-like)))

(defn- call-actor-id
  "The actor this request's tool calls run as, or nil when it has none.

   A token gets an actor only if it *carries* one. A token that carries none —
   every token minted before actors were carried — stays actor-less rather than
   inheriting whatever its membership resolves to now: the actor decides which
   Discord and Bluesky account a call posts from, and that token's consent
   screen never named one. Granting it the membership's actor would hand an
   already-issued credential a power its holder never agreed to, silently.

   When the token does carry one, the value used is the membership's current
   actor, and law/token-actor-honourable? has already refused the case where the
   two disagree. So a reassignment is a refusal rather than a quiet switch, and
   there is no window in which a token acts as an actor its membership has
   dropped."
  [token-record token-ctx]
  (let [claimed (actor-acting/normalize-actor-id (aget token-record "actorId"))
        current (actor-acting/normalize-actor-id (authz/ctx-actor-id token-ctx))]
    (when-not (law/token-actor-honourable? claimed current)
      (throw (params/http-error 403 "actor_reassigned"
                         (str "This token was authorized to act as " claimed
                              ", which is no longer this membership's actor."))))
    (when claimed current)))

(defn- apply-zod-description [^js schema-node ^js schema-json]
  (let [description (some-> (aget schema-json "description") str str/trim not-empty)]
    (if description (.describe schema-node description) schema-node)))

(defn typebox->zod-node
  "Convert one TypeBox schema node to a zod schema. Pure."
  [^js z ^js schema-json]
  (let [schema-type (aget schema-json "type")
        node (case schema-type
               "string"  (.string z)
               "number"  (.number z)
               "integer" (-> (.number z) (.int))
               "boolean" (.boolean z)
               "array"   (.array z (or (typebox->zod-node z (aget schema-json "items")) (.any z)))
               ;; A nested object must become a zod schema, not the bare field
               ;; shape typebox->zod-shape builds. Returning the shape gave the
               ;; parent a plain JS object with no zod methods, so marking the
               ;; field optional threw "fschema.optional is not a function" and
               ;; the whole tool registration failed — every MCP POST that
               ;; registers tools answered 400.
               "object"  (.object z (or (typebox->zod-shape z schema-json) (js-obj)))
               (.any z))]
    (-> node
        (apply-zod-description schema-json)
        ((fn [n] (if-let [min (aget schema-json "minimum")] (.min n min) n)))
        ((fn [n] (if-let [max (aget schema-json "maximum")] (.max n max) n))))))

(defn typebox->zod-shape
  "Convert a TypeBox object schema to a zod *field shape* — the map of field
   name to zod schema that registerTool wants. Not itself a zod schema; a
   nested object goes through typebox->zod-node, which wraps it. Pure."
  [^js z ^js schema-json]
  (let [properties   (or (aget schema-json "properties") (js/Object.))
        required-set (into #{} (map str) (array-seq (or (aget schema-json "required") (js/Array.))))
        entries      (.entries js/Object properties)]
    (when (seq (array-seq entries))
      (reduce (fn [shape entry]
                (let [fname  (aget entry 0)
                      fschema (typebox->zod-node z (aget entry 1))
                      final   (if (contains? required-set (str fname)) fschema (.optional fschema))]
                  (aset shape fname final)
                  shape))
              (js-obj)
              (array-seq entries)))))

;; ──────────────────────────────────────────────────────────────
;; Route handlers
;; ──────────────────────────────────────────────────────────────

;; Public metadata endpoints. The empty guard vector is load-bearing: without
;; it these are classic-mode defroutes, which expand to a four-argument call on
;; a `with-request-context!` this module's deps map never supplies, and both
;; documents answered 500 in production. Guards would be wrong here anyway — a
;; client reads these precisely because it has no token yet.

(defroute mcp-discovery-metadata! [base] "GET" "/.well-known/oauth-authorization-server" []
  (let [issuer (js/URL. (.toString base))]
    (json-send! reply 200
                {:issuer                              (-> (.toString issuer) (.replace (js/RegExp. "/$") ""))
                 :authorization_endpoint              (.toString (js/URL. "/api/mcp/oauth/authorize" issuer))
                 :token_endpoint                      (.toString (js/URL. "/api/mcp/oauth/token" issuer))
                 :registration_endpoint               (.toString (js/URL. "/api/mcp/oauth/register" issuer))
                 :response_types_supported            ["code"]
                 :grant_types_supported               ["authorization_code"]
                 :code_challenge_methods_supported    ["S256"]
                 :token_endpoint_auth_methods_supported ["none"]})))

(defroute mcp-protected-resource-metadata! [base] "GET" "/.well-known/oauth-protected-resource" []
  (json-send! reply 200 (protected-resource-metadata base)))

;; RFC 9728 locates a resource's metadata by inserting the resource path into
;; the well-known URI, so the document for https://host/mcp lives at
;; /.well-known/oauth-protected-resource/mcp — not only at the root. ChatGPT
;; probes the path-inserted form first and got 404 for both it and the
;; /mcp/.well-known variant some clients try (run of 2026-08-04 19:01). Serve
;; the same document at all three rather than relying on a client falling back.
(defroute mcp-protected-resource-metadata-for-mcp! [base] "GET" "/.well-known/oauth-protected-resource/mcp" []
  (json-send! reply 200 (protected-resource-metadata base)))

(defroute mcp-protected-resource-metadata-suffixed! [base] "GET" "/mcp/.well-known/oauth-protected-resource" []
  (json-send! reply 200 (protected-resource-metadata base)))

;; preHandler-mode routes

(defroute mcp-register-client! [crypto] "POST" "/api/mcp/oauth/register" []
  (let [{:keys [redirect-uris client-name]} (params/parse-register-client-body request)
        client-id (.randomUUID crypto)
        client    {:client_id                  client-id
                   :client_name                (or client-name "mcp-client")
                   :redirect_uris              redirect-uris
                   :token_endpoint_auth_method "none"
                   :grant_types                ["authorization_code"]
                   :response_types             ["code"]
                   :created_at                 (.toISOString (js/Date.))}]
    (try
      (await (mongo-mcp/set-client! client-id (js/JSON.stringify (clj->js client))))
      (json-send! reply 201 client)
      (catch :default err
        (throw (params/http-error 500 "registration_failed" (or (.-message err) (str err))))))))

(defroute mcp-authorize-client! [base config runtime browser-auth-guard] "GET" "/api/mcp/oauth/authorize" [browser-auth-guard]
  (let [auth-context (aget request "authContext")
        {:keys [client-id redirect-uri state code-challenge scope] :as params} (params/parse-authorize-query request)]
    (ensure-oauth-request! params)
    (let [client (await (get-registered-client client-id))]
      (ensure-redirect-uri-allowed! client redirect-uri "invalid_request")
      (let [tools    (available-tools runtime config auth-context)
            selected (let [explicit (selected-tools-from-scope tools scope)]
                       (if (seq explicit) explicit (default-selected-tools (tool-name-set tools))))
            html     (consent/page
                      {:base base :auth-context auth-context
                       :client-id client-id :redirect-uri redirect-uri
                       :state state :code-challenge code-challenge
                       :requested-scope (or scope "") :tools tools :selected selected})]
        (.send (reply-header! reply "content-type" "text/html; charset=utf-8") html)))))

(defn- ensure-consent-identity!
  "Refuse a confirmation whose session cannot carry an authorization.

   The consent page tolerates a partly-resolved context so a render cannot
   crash, but those empty-string fallbacks must never be minted: the code is
   copied verbatim into the access token, and a token with a blank membership
   carries no authorization identity while still being a valid bearer token.

   org-slug is deliberately not required — it is descriptive rather than an
   authorization key, and a membership without one is legitimate."
  [membership-id user-email]
  (when (or (str/blank? membership-id) (str/blank? user-email))
    (throw (params/http-error 400 "missing_identity"
                       "Session has no membership or user identity to authorize with"))))

(defn- ensure-consent-actor-unchanged!
  "Refuse when the membership's actor moved while the consent page was open.

   Minting now would authorize an actor the user never saw, and every later call
   would accept it, because it matches the membership. See
   law/consent-actor-unchanged? for why the page's value is a witness rather
   than identity."
  [displayed-actor actor-id]
  (when-not (law/consent-actor-unchanged? displayed-actor actor-id)
    (let [named (fn [v] (or (not-empty (str/trim (str (or v "")))) "no actor"))]
      (throw (params/http-error 400 "actor_changed"
                         (str "This membership now acts as " (named actor-id)
                              ", not " (named displayed-actor)
                              " as shown. Reload the authorization page and review it."))))))

(defn- authorization-code-payload
  "The code record to persist for an approved consent.

   The actor is read from the resolved context and never from the query: the
   client is on the other side of a redirect it controls, so an actorId it
   echoed back would be the client choosing its own identity. No grant is
   evaluated here — a membership carries one actor — which is why
   law/actor-grantable? is reached only through consent-actor-unchanged?, with
   the membership's actor on one side and the displayed one on the other.

   Normalized here rather than only in persist-access-token!, so the code record
   and the token minted from it cannot hold two spellings of one actor."
  [{:keys [code client-id redirect-uri code-challenge requested auth-context
           displayed-actor]}]
  (let [membership-id (str (or (authz/ctx-membership-id auth-context) ""))
        user-email    (str (or (authz/ctx-user-email auth-context) ""))
        org-slug      (str (or (authz/ctx-org-slug auth-context) ""))
        actor-id      (actor-acting/normalize-actor-id (authz/ctx-actor-id auth-context))]
    (ensure-consent-identity! membership-id user-email)
    (ensure-consent-actor-unchanged! displayed-actor actor-id)
    (cond-> {:code code :clientId client-id :redirectUri redirect-uri
             :codeChallenge code-challenge :codeChallengeMethod "S256"
             :tools requested
             :membershipId membership-id :userEmail user-email :orgSlug org-slug
             :createdAt (.toISOString (js/Date.))}
      ;; Absent rather than blank when there is no actor. A blank would
      ;; round-trip as an actor that exists and owns nothing.
      actor-id (assoc :actorId actor-id))))

(defroute mcp-authorize-confirm! [base crypto config runtime code-ttl token-ttl browser-auth-guard] "GET" "/api/mcp/oauth/authorize/confirm" [browser-auth-guard]
  (let [auth-context (aget request "authContext")
        {:keys [client-id redirect-uri state code-challenge selected-tools
                displayed-actor] :as params}
        (params/parse-authorize-confirm-query request)]
    (ensure-oauth-confirm-request! params)
    (let [client (await (get-registered-client client-id))]
      (ensure-redirect-uri-allowed! client redirect-uri "invalid_request")
      (let [requested (requested-tools runtime config auth-context selected-tools)]
        (when (empty? requested)
          (throw (params/http-error 400 "invalid_scope" "No valid tools selected")))
        (let [code    (.randomUUID crypto)
              payload (authorization-code-payload
                       {:code code :client-id client-id :redirect-uri redirect-uri
                        :code-challenge code-challenge :requested requested
                        :auth-context auth-context :displayed-actor displayed-actor})]
          (await (mongo-mcp/set-code! code (js/JSON.stringify (clj->js payload)) code-ttl))
          (let [redir (js/URL. redirect-uri)]
            (.set (.-searchParams redir) "code" code)
            (when state (.set (.-searchParams redir) "state" state))
            (.redirect reply (.toString redir) 302)))))))

(defn- ensure-code-bindings!
  "Reject an exchange whose client, redirect or PKCE verifier does not match the
   code it presents.

   Checked against a non-destructive read so a rejected request never spends the
   code: anyone who merely observed the code on the front channel could
   otherwise destroy it with any wrong verifier, and it would buy nothing, since
   a verifier carries far too much entropy to guess."
  [crypto record client-id redirect-uri code-verifier]
  ;; Parsing and the challenge computation stay here — the crypto is effectful.
  ;; Whether the result admits the exchange is law's decision, so both rules
  ;; live in one pure place that every future caller shares.
  (when-not (law/code-bound-to? record client-id redirect-uri)
    (throw (params/http-error 400 "invalid_grant" "Client/redirect mismatch")))
  (when-not (law/pkce-verified? record (pkce-challenge crypto code-verifier))
    (throw (params/http-error 400 "invalid_grant" "PKCE verification failed"))))

(defn- ^:async persist-access-token!
  "Mint and store an access token from a claimed code record (a CLJS map)."
  [crypto token-ttl client-id record]
  (let [access-token  (.randomUUID crypto)
        membership-id (:membershipId record)
        tools         (vec (:tools record))
        actor-id      (actor-acting/normalize-actor-id (:actorId record))
        token-value   (cond-> {:accessToken access-token :clientId client-id
                               :membershipId membership-id
                               :userEmail    (:userEmail record)
                               :orgSlug      (:orgSlug record)
                               :tools        tools
                               :createdAt    (.toISOString (js/Date.))
                               :expiresAt    (.toISOString (js/Date. (+ (.now js/Date) (* token-ttl 1000))))}
                        ;; Copied from the claimed code, so the actor a token
                        ;; acts as is the one the user saw on the consent page.
                        ;; Still re-checked against the membership on every
                        ;; call — see law/token-actor-honourable?.
                        actor-id (assoc :actorId actor-id))]
    (await (mongo-mcp/set-token! access-token (js/JSON.stringify (clj->js token-value)) token-ttl membership-id))
    {:access_token access-token :token_type "Bearer"
     :scope        (str/join " " tools)
     :expires_in   token-ttl}))

(defroute mcp-exchange-token! [crypto token-ttl] "POST" "/api/mcp/oauth/token" []
  (let [{:keys [grant-type code code-verifier client-id redirect-uri]} (params/parse-token-exchange-body request)]
    (when (or (not= grant-type "authorization_code")
              (str/blank? code) (str/blank? code-verifier)
              (str/blank? client-id) (str/blank? redirect-uri))
      (throw (params/http-error 400 "invalid_request" "Missing required token exchange parameters")))
    (let [client (await (get-registered-client client-id))]
      (ensure-redirect-uri-allowed! client redirect-uri "invalid_grant")
      ;; Validate against a peek, then claim. Single use is enforced by the
      ;; claim rather than the read: two concurrent exchanges can both pass the
      ;; bindings, but find-one-and-delete picks one winner and the loser is
      ;; told the code is spent. The token is minted from the claimed record,
      ;; not the peeked one, so its contents cannot have changed in between.
      (let [peeked (await (mongo-mcp/peek-code! code))]
        (when-not peeked (throw (params/http-error 400 "invalid_grant" "Unknown or expired code")))
        (ensure-code-bindings! crypto peeked client-id redirect-uri code-verifier)
        (let [record (await (mongo-mcp/consume-code! code))]
          (when-not record
            (throw (params/http-error 400 "invalid_grant" "Authorization code already used")))
          (json-send! reply 200
                      (await (persist-access-token! crypto token-ttl client-id record))))))))

(defroute mcp-list-user-tokens! [browser-auth-guard] "GET" "/api/mcp/tokens" [browser-auth-guard]
  (let [auth-context  (aget request "authContext")
        membership-id (str (or (authz/ctx-membership-id auth-context) ""))]
    (when (str/blank? membership-id)
      (throw (params/http-error 400 "missing_membership" "No membership available for this session")))
    (let [records (await (mongo-mcp/list-tokens-for-membership! membership-id))]
      (json-send! reply 200 {:ok true :tokens (->> records (remove nil?) into-array)}))))

(defroute mcp-revoke-user-token! [browser-auth-guard] "DELETE" "/api/mcp/tokens/:tokenId" [browser-auth-guard]
  (let [auth-context  (aget request "authContext")
        {:keys [token-id]}    (params/parse-revoke-token-params request)
        membership-id         (str (or (authz/ctx-membership-id auth-context) ""))]
    (when (or (str/blank? membership-id) (str/blank? token-id))
      (throw (params/http-error 400 "invalid_request" "membership and tokenId are required")))
    ;; Scoped to the caller's membership. Deleting by token value alone let any
    ;; authenticated caller revoke someone else's token if they learned its
    ;; value — the listing route is per-membership, but nothing stopped a
    ;; hand-made DELETE. A miss is reported as 404 rather than 200 so a caller
    ;; is not told their revocation succeeded when it did nothing; because the
    ;; query is membership-scoped, that 404 reveals nothing about whether the
    ;; token exists for anyone else.
    (let [revoked (await (mongo-mcp/delete-token-for-membership! token-id membership-id))]
      (when-not revoked
        (throw (params/http-error 404 "not_found" "No such token for this membership")))
      (json-send! reply 200 {:ok true}))))

(defroute mcp-handle-session! [base bearer-token-guard] "GET" "/mcp" [bearer-token-guard]
  (let [bearer     (aget request "bearerToken")
        session-id (transport/resolve-session-id request)]
    (cond
      (str/blank? (str session-id))
      (text-send! reply 400 "Missing mcp-session-id")

      :else
      (let [{:keys [transport token]} (get @mcp-sessions* session-id)]
        (cond
          (nil? transport)                   (text-send! reply 404 (str "Invalid mcp-session-id: " session-id))
          (not= (str bearer) (str token))    (transport/challenge-unauthorized! reply base)
          :else (do (transport/ensure-streamable-accept! request)
                    (transport/handle-request! transport (aget request "raw") (aget reply "raw"))))))))

(defroute mcp-handle-delete-session! [base bearer-token-guard] "DELETE" "/mcp" [bearer-token-guard]
  (let [bearer     (aget request "bearerToken")
        session-id (transport/resolve-session-id request)]
    (cond
      (str/blank? (str session-id))
      (text-send! reply 400 "Missing mcp-session-id")

      :else
      (let [{:keys [transport token]} (get @mcp-sessions* session-id)]
        (cond
          (nil? transport)                   (text-send! reply 404 (str "Invalid mcp-session-id: " session-id))
          (not= (str bearer) (str token))    (transport/challenge-unauthorized! reply base)
          :else (do (transport/ensure-streamable-accept! request)
                    (transport/handle-request! transport (aget request "raw") (aget reply "raw"))))))))

(defn- tool-config-js
  "The MCP registerTool config for one tool object. Pure interop assembly."
  [z ^js tool name]
  (let [shape  (or (when z (typebox->zod-shape z (or (aget tool "parameters") (js/Object.)))) (js-obj))
        config (clj->js {:description (str (or (aget tool "description") (aget tool "label") name))
                         :inputSchema shape})]
    (when-let [title (some-> (or (aget tool "label") (aget tool "title")) str str/trim not-empty)]
      (aset config "title" title))
    ;; A tool's own annotations win, else the declared table.
    ;; See law.mcp-tool-annotations for why absence is bad.
    (if-let [annotations (aget tool "annotations")]
      (aset config "annotations" annotations)
      ;; name may be sanitized (web.read -> web_read).
      (when-let [declared (or (tool-annotations/for-tool name)
                              (tool-annotations/for-tool (aget tool "originalName")))]
        (aset config "annotations" (clj->js declared))))
    (when-let [meta (aget tool "_meta")]
      (aset config "_meta" meta))
    config))

(defn- register-tools!
  "Register every granted tool on a fresh MCP server, bound to one actor.

   Each handler runs inside that actor's scope, so domain.actor.credentials
   resolves the actor without the MCP surface having to impersonate an agent
   spawn. The scope is entered per call rather than per request because the SDK
   invokes handlers itself: wrapping the request would put the awaits that
   matter outside it.

   A nil actor-id still enters a scope — one that says there is no actor. It has
   to: without it a credential read falls back to the process-global
   agent-context, and an actor-less token would borrow whatever actor a
   concurrent agent turn is running as. See actor-acting/run-as!."
  [^js server z tools actor-id]
  (doseq [^js tool (array-seq tools)]
    (when-let [name (some-> (aget tool "name") str str/trim not-empty)]
      (.registerTool server name
                     (tool-config-js z tool name)
                     (fn [params]
                       (actor-acting/run-as! actor-id #(tool-execute! tool params)))))))

(defn- granted-tools
  "The tools this token was granted, out of those its context can reach.

   The intersection is the whole authorization: a token carries the names ticked
   on the consent page, and a tool absent from either side is not registered.
   One consequence worth knowing — a token whose grant list is empty registers
   nothing, so initialize advertises no tool capability and tools/list answers
   \"Method not found\". That is correct, and it reads like a broken server."
  [runtime config token-ctx ^js token-record]
  (let [allowed (into #{} (map str) (array-seq (or (aget token-record "tools") (js/Array.))))]
    (->> (available-tools runtime config token-ctx)
         array-seq
         (filter (fn [^js t] (contains? allowed (str (aget t "name")))))
         into-array)))

(defn- close-when-response-ends!
  "Tear down a per-request MCP server once its response is finished.

   Stateless mode requires a fresh server and transport per exchange, so without
   this every POST leaves both behind for the process lifetime.

   Closing the *server* is enough and closing the transport as well would be a
   double close: in SDK 1.29 McpServer.close -> Server.close ->
   Protocol.close -> transport.close.

   Bound to the response's close event rather than run after handle-request!
   returns, because a response may still be streaming when the handler resolves;
   closing then would cut it off. Failures are swallowed deliberately — the
   client already has its response, and a teardown error must not replace it."
  [^js raw-res ^js server]
  (.once raw-res "close"
         (^:async fn []
           (try
             (await (.close server))
             (catch :default _ nil)))))

(defn- ^:async serve-mcp-post!
  "Answer one authenticated MCP POST on a per-request server and transport."
  [{:keys [config runtime policy-db McpServer StreamableHTTPServerTransport z
           request raw-req raw-res token-record]}]
  (let [token-ctx (await (resolve-token-context! policy-db token-record))
        actor-id  (call-actor-id token-record token-ctx)
        server    (new McpServer (clj->js {:name "knoxx" :version "0.1.0"}))
        transport (new StreamableHTTPServerTransport (transport/stateless-transport-options))]
    (register-tools! server z (granted-tools runtime config token-ctx token-record) actor-id)
    (await (.connect server transport))
    (close-when-response-ends! raw-res server)
    (transport/ensure-streamable-accept! request)
    (transport/handle-request! transport raw-req raw-res (aget request "body"))))

(defroute mcp-handle-post! [base config runtime code-ttl token-ttl policy-db McpServer StreamableHTTPServerTransport z] "POST" "/mcp" []
  (let [^js request request
        ^js reply reply]
    (.hijack reply)
    (let [^js raw-req (aget request "raw")
          ^js raw-res (aget reply "raw")
          bearer      (transport/bearer-token request)]
      (try
        (let [token-record (when-not (str/blank? bearer)
                             (await (load-token-record! bearer)))]
          (if-not token-record
            (transport/unauthorized! base raw-res)
            (await (serve-mcp-post!
                    {:config config :runtime runtime :policy-db policy-db
                     :McpServer McpServer
                     :StreamableHTTPServerTransport StreamableHTTPServerTransport
                     :z z :request request :raw-req raw-req :raw-res raw-res
                     :token-record token-record}))))
        (catch :default err
          (.error js/console "[knoxx-mcp] post failed" err)
          (when-not (.-headersSent raw-res)
            (let [status (or (aget err "statusCode") 500)]
              (.writeHead raw-res status (clj->js {"Content-Type" "application/json"}))
              (.end raw-res (js/JSON.stringify (clj->js {:error (or (aget err "code") "mcp_post_failed")
                                                         :detail (or (.-message err) (str err))}))))))))))

(def ^:private route-registrars
  "Every MCP route, in registration order. Kept as data so adding one is a line
   rather than an edit to the registration function."
  [mcp-discovery-metadata!
   mcp-protected-resource-metadata!
   mcp-protected-resource-metadata-for-mcp!
   mcp-protected-resource-metadata-suffixed!
   mcp-register-client!
   mcp-authorize-client!
   mcp-authorize-confirm!
   mcp-exchange-token!
   mcp-list-user-tokens!
   mcp-revoke-user-token!
   mcp-handle-post!
   mcp-handle-session!
   mcp-handle-delete-session!])

(defn register-mcp-http-routes!
  [app runtime config]
  (let [base         (public-base-url config)
        policy-db    (runtime-state/current-policy-db)
        code-ttl     (js/parseInt (env "KNOXX_MCP_CODE_TTL_SECONDS" "300") 10)
        token-ttl    (js/parseInt (env "KNOXX_MCP_TOKEN_TTL_SECONDS" (str (* 60 60 24 30))) 10)
        deps {:route!              route!
              :browser-auth-guard  (require-browser-auth! policy-db config)
              :bearer-token-guard  (require-bearer-token! base)
              :base                base
              :runtime             runtime
              :config              config
              :policy-db           policy-db
              :crypto              crypto
              :McpServer                     McpServer
              :StreamableHTTPServerTransport StreamableHTTPServerTransport
              :z                             z
              :code-ttl  code-ttl
              :token-ttl token-ttl}]
    (doseq [register! route-registrars]
      (register! app runtime config deps))))

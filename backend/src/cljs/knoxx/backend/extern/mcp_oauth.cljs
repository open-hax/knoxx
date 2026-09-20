(ns knoxx.backend.extern.mcp-oauth
  "Native MCP OAuth route composition over validated, principal-bound provider data."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.mcp.mcp-expose :as mcp-expose]
            [knoxx.backend.infra.actor.acting :as actor-acting]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.routes.mcp.consent :as consent]
            [knoxx.backend.infra.routes.mcp.params :as params]
            [knoxx.backend.infra.stores.mongo-mcp-oauth :as mongo-mcp]
            [knoxx.backend.law.mcp-oauth :as law])
  (:require-macros [knoxx.backend.macros :refer [defroute]]))

(defn- base64url [buf] (.toString (js/Buffer.from buf) "base64url"))

(defn- pkce-challenge
  [^js crypto verifier]
  (base64url (-> (.createHash crypto "sha256") (.update (str verifier)) (.digest))))

(defn- json-send! [reply status payload]
  (-> (.code reply status) (.send (clj->js payload))))

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

(defn- reply-header! [^js reply name value] (.header reply name value))

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

(defn available-tools [runtime config auth-context]
  (or (mcp-expose/create-knoxx-custom-tools-js runtime config auth-context) (js/Array.)))

(defn tool-name-set [tools]
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

(defroute mcp-protected-resource-metadata-for-mcp! [base] "GET" "/.well-known/oauth-protected-resource/mcp" []
  (json-send! reply 200 (protected-resource-metadata base)))

(defroute mcp-protected-resource-metadata-suffixed! [base] "GET" "/mcp/.well-known/oauth-protected-resource" []
  (json-send! reply 200 (protected-resource-metadata base)))

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
  [membership-id principal-id]
  (when (or (str/blank? membership-id) (str/blank? principal-id))
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
  "Copy immutable verified identity and the current stored actor into the grant.
   The displayed actor is a witness, never authority; email is optional metadata.
   No raw authorization code is persisted in this secret-free provider payload."
  [{:keys [client-id redirect-uri code-challenge requested auth-context
           displayed-actor]}]
  (let [membership-id (str (or (authz/ctx-membership-id auth-context) ""))
        user-email    (str (or (authz/ctx-user-email auth-context) ""))
        org-slug      (str (or (authz/ctx-org-slug auth-context) ""))
        actor-id      (actor-acting/normalize-actor-id
                       (authz/ctx-actor-binding auth-context))]
    (ensure-consent-identity! membership-id (get-in auth-context [:axxium-principal :principal/id]))
    (ensure-consent-actor-unchanged! displayed-actor actor-id)
    (cond-> {:clientId client-id :redirectUri redirect-uri
             :codeChallenge code-challenge :codeChallengeMethod "S256"
             :tools requested
             :membershipId membership-id :orgSlug org-slug
             :axxiumPrincipalId (get-in auth-context [:axxium-principal :principal/id])
             :axxiumEntityId (get-in auth-context [:axxium-principal :principal/entity-id])
             :createdAt (.toISOString (js/Date.))}
      ;; Absent rather than blank when there is no actor. A blank would
      ;; round-trip as an actor that exists and owns nothing.
      (seq user-email) (assoc :userEmail user-email)
      actor-id (assoc :actorId actor-id))))

(defroute mcp-authorize-confirm! [base crypto config runtime code-ttl token-ttl browser-auth-guard] "POST" "/api/mcp/oauth/authorize/confirm" [browser-auth-guard]
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
  "Atomically consume the unchanged code and persist only secret-free grant metadata."
  [crypto token-ttl client-id code record]
  (let [access-token (.randomUUID crypto)
        membership-id (:membershipId record)
        token-value (assoc (select-keys record [:membershipId :userEmail :orgSlug :tools :actorId
                                                :axxiumPrincipalId :axxiumEntityId])
                           :clientId client-id
                           :createdAt (.toISOString (js/Date.))
                           :expiresAt (.toISOString (js/Date. (+ (.now js/Date) (* token-ttl 1000)))))
        accepted (await (mongo-mcp/exchange-code! code record access-token
                                               (js/JSON.stringify (clj->js token-value)) token-ttl membership-id))]
    (when-not accepted
      (throw (params/http-error 400 "invalid_grant" "Authorization code expired or already used")))
    {:access_token access-token :token_type "Bearer" :scope (str/join " " (:tools record))
     :expires_in token-ttl}))

(defroute mcp-exchange-token! [crypto token-ttl] "POST" "/api/mcp/oauth/token" []
  (let [{:keys [grant-type code code-verifier client-id redirect-uri]} (params/parse-token-exchange-body request)]
    (when (or (not= grant-type "authorization_code")
              (str/blank? code) (str/blank? code-verifier)
              (str/blank? client-id) (str/blank? redirect-uri))
      (throw (params/http-error 400 "invalid_request" "Missing required token exchange parameters")))
    (let [client (await (get-registered-client client-id))]
      (ensure-redirect-uri-allowed! client redirect-uri "invalid_grant")
        ;; The provider atomically compares and consumes this exact code record
        ;; while issuing its token; concurrent exchanges have one winner.
      (let [peeked (await (mongo-mcp/peek-code! code))]
        (when-not peeked (throw (params/http-error 400 "invalid_grant" "Unknown or expired code")))
        (ensure-code-bindings! crypto peeked client-id redirect-uri code-verifier)
        (json-send! reply 200
                    (await (persist-access-token! crypto token-ttl client-id code peeked)))))))

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
    (let [revoked (await (mongo-mcp/delete-token-id-for-membership! token-id membership-id))]
      (when-not revoked
        (throw (params/http-error 404 "not_found" "No such token for this membership")))
      (json-send! reply 200 {:ok true}))))

(def route-registrars
  "OAuth endpoints registered under the common browser identity guard."
  [mcp-discovery-metadata!
   mcp-protected-resource-metadata!
   mcp-protected-resource-metadata-for-mcp!
   mcp-protected-resource-metadata-suffixed!
   mcp-register-client!
   mcp-authorize-client!
   mcp-authorize-confirm!
   mcp-exchange-token!
   mcp-list-user-tokens!
   mcp-revoke-user-token!])

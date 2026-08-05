(ns knoxx.backend.law.mcp-oauth
  "Malli contracts for Knoxx's MCP OAuth persistence boundaries.

   Contract policy only — no I/O. The store namespace calls these; it does not
   define them, so the obligations of a boundary can be read without reading
   the code that performs it."
  (:require [clojure.string :as str]
            [malli.core :as m]
            [knoxx.backend.law.mongo :as law-mongo]))

(defn credential-live?
  "True when a credential with this expiry is still admissible at this instant.

   Pure, and takes the clock reading as an argument: the effectful layer reads
   the time, this decides what it means. That keeps one rule for every reader —
   get-code!, consume-code!, get-token! and the token listing all admit exactly
   the same credentials — and lets the rule be tested without a clock.

   Both arguments must be readable instants. An unreadable expiry is not live:
   a credential whose expiry cannot be named must not be honoured, so this
   fails closed rather than treating the unknown as far future."
  [expiry-ms now-ms]
  (boolean (and (law-mongo/valid-epoch-ms? expiry-ms)
                (law-mongo/valid-epoch-ms? now-ms)
                (> expiry-ms now-ms))))

(def NonBlankString
  [:and string? [:fn {:error/message "must not be blank"} #(not (str/blank? %))]])

(defn code-bound-to?
  "True when a claimed code was issued to this client and redirect URI.

   Pure. An authorization code is bound to the client that requested it and the
   redirect it was issued for; honouring it for a different pair would let a
   client redeem someone else's code."
  [record client-id redirect-uri]
  (and (some? record)
       (= (:clientId record) client-id)
       (= (:redirectUri record) redirect-uri)))

(defn pkce-verified?
  "True when a computed PKCE challenge matches the one the code carries.

   Pure: the caller computes the challenge from the verifier — that is crypto,
   and it stays in the effectful layer — while this decides whether the result
   admits the exchange. A code carrying no challenge is never verified, so a
   record written without one cannot be redeemed by omitting the verifier."
  [record computed-challenge]
  (let [expected (str (or (:codeChallenge record) ""))
        actual   (str (or computed-challenge ""))]
    (and (not (str/blank? expected))
         (not (str/blank? actual))
         (= expected actual))))

(defn actor-grantable?
  "True when a membership may authorize a token to act as this actor.

   Today a membership carries exactly one actor, so the only grantable actor is
   its own. That is a policy statement, not a limitation of the data: a
   membership is the thing an authenticated session resolves to, and nothing in
   the policy DB says which *other* actors a user may assume. Until such a
   grant exists, the honest rule is identity.

   Kept as a named rule rather than an inline `=` so that widening it later is
   one edit in law with one place to test, instead of a search for every
   comparison. Both arguments must name an actor: a blank on either side is not
   a match, so a context that failed to resolve an actor cannot authorize one.

   Deliberately not an authorization *decision* about tools — this says only
   which actor may be named, never what that actor may do."
  [membership-actor-id requested-actor-id]
  (let [own       (str/trim (str (or membership-actor-id "")))
        requested (str/trim (str (or requested-actor-id "")))]
    (and (not (str/blank? own))
         (not (str/blank? requested))
         (= own requested))))

(defn token-actor-honourable?
  "True when a presented token's actor is still the one its membership resolves to.

   Re-checked on every call rather than trusted from the token, because the two
   can drift: a membership's actor_id can be reassigned after a token is minted,
   and the token is a bearer credential that outlives that edit. Honouring the
   token's copy would let a revoked or reassigned identity keep reading the old
   actor's credentials for the rest of the token's lifetime.

   A token carrying no actor is honourable — it simply has none, and the
   credential read will fail on the missing actor with a message that says so.
   That keeps tokens minted before actors were carried working for every tool
   that needs no credential, rather than invalidating them all at once."
  [token-actor-id membership-actor-id]
  (let [claimed (str/trim (str (or token-actor-id "")))]
    (or (str/blank? claimed)
        (actor-grantable? membership-actor-id claimed))))

(def ProtectedResourceMetadata
  "RFC 9728 protected resource metadata, as served to an unauthenticated client.

   Every field is required and non-blank because a client cannot recover from a
   partial answer: this document is how it learns which authorization server to
   use, and a blank resource or an empty server list sends it nowhere. The
   payload is derived from the public base URL, so validating it here catches a
   misconfigured base before a client is handed an unusable document."
  [:map
   [:resource                 NonBlankString]
   [:authorization_servers    [:and [:vector NonBlankString] [:fn {:error/message "must name a server"} seq]]]
   [:scopes_supported         [:vector NonBlankString]]
   [:bearer_methods_supported [:and [:vector NonBlankString] [:fn {:error/message "must name a method"} seq]]]])

(defn valid-protected-resource-metadata?
  [metadata]
  (m/validate ProtectedResourceMetadata metadata))

(def RevocationRequest
  "Identity required to revoke an access token.

   Both fields are non-blank on purpose. A blank membership would widen the
   delete from 'this caller's token' to 'this token, whoever owns it' — the
   difference between revoking your own credential and revoking someone
   else's. A blank token matches nothing against today's schema, but the
   obligation belongs here rather than in whichever route happens to call in."
  [:map
   [:access-token  NonBlankString]
   [:membership-id NonBlankString]])

(def RevocationResult
  "Decoded outcome of a revocation.

   A count rather than a boolean, and required: a result that lost its count
   must not be readable as 'nothing was deleted'."
  [:map
   [:deleted-count [:int {:min 0}]]])

(defn valid-revocation-request?
  [request]
  (m/validate RevocationRequest request))

(defn valid-revocation-result?
  [result]
  (m/validate RevocationResult result))

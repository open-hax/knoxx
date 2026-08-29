(ns knoxx.backend.infra.routes.mcp.params
  "Request shapes for the MCP OAuth endpoints, their parsers, and the HTTP error
   a failed parse raises.

   Every shape is Malli and every parser validates before returning, so a route
   body never sees a half-formed request. Extracted from infra.routes.mcp with
   http-error, because the parsers and the error they throw are one concern and
   splitting them would have left a cycle."
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]))

(def RegisterClientBody
  [:map
   [:redirect-uris [:vector string?]]
   [:client-name {:optional true} string?]])

(def AuthorizeQuery
  [:map
   [:client-id string?]
   [:redirect-uri string?]
   [:state {:optional true} [:maybe string?]]
   [:code-challenge string?]
   [:code-challenge-method string?]
   [:scope {:optional true} [:maybe string?]]])

(def AuthorizeConfirmQuery
  [:map
   [:client-id string?]
   [:redirect-uri string?]
   [:state {:optional true} [:maybe string?]]
   [:code-challenge string?]
   [:code-challenge-method string?]
   [:scope {:optional true} [:maybe string?]]
   [:selected-tools [:vector string?]]
   ;; What the consent page displayed as the acting actor. Blank is legitimate —
   ;; a membership may have none. Checked against the context, never trusted as
   ;; identity; see law/consent-actor-unchanged?.
   [:displayed-actor {:optional true} [:maybe string?]]])

(def TokenExchangeBody
  [:map
   [:grant-type string?]
   [:code string?]
   [:code-verifier string?]
   [:client-id string?]
   [:redirect-uri string?]])

(def RevokeTokenParams
  [:map
   [:token-id string?]])


;; Fastify's default error handler reads `statusCode` off the thrown object and
;; falls back to 500. A bare ex-info keeps its status in ex-data, where Fastify
;; cannot see it, so every client error here reported as 500. Stamp it where
;; Fastify looks.
(defn http-error
  ([status error detail]      (http-error status error detail nil))
  ([status error detail data]
   (let [e (ex-info detail (merge {:status status :error error :detail detail} data))]
     (aset e "statusCode" status)
     ;; And the code where a hijacked route looks. /mcp writes its own response,
     ;; so it reads the error off the object rather than through Fastify — and
     ;; ex-data is invisible from JS, so without this every refusal reported as
     ;; mcp_post_failed and a client could not tell actor_reassigned from a
     ;; crash.
     (aset e "code" error)
     e)))

(defn- validation-detail [schema value]
  (some-> (m/explain schema value) me/humanize pr-str))

(defn- validate! [schema value {:keys [status error detail]}]
  (if (m/validate schema value)
    value
    (throw (http-error (or status 400)
                       (or error "invalid_request")
                       (or detail (validation-detail schema value) "Invalid request")))))


(defn- normalize-tool-selection
  [raw]
  (cond
    (nil? raw) []
    (array? raw) (mapv str (array-seq raw))
    :else [(str raw)]))

;; ──────────────────────────────────────────────────────────────
;; Parsers
;; ──────────────────────────────────────────────────────────────

(defn parse-register-client-body [req]
  (let [body   (or (aget req "body") (js/Object.))
        ;; :client-name is omitted rather than set to nil when absent.
        ;; {:optional true} governs whether the key must be present, not what a
        ;; present value may be — so a nil under a `string?` entry fails, and a
        ;; registration that simply did not send client_name was rejected as
        ;; invalid_client_metadata. Omitting keeps the schema honest instead of
        ;; widening it to [:maybe string?], which would legitimise a nil name.
        name   (some-> (aget body "client_name") str str/trim not-empty)
        value  (cond-> {:redirect-uris (if (array? (aget body "redirect_uris"))
                                         (mapv str (array-seq (aget body "redirect_uris")))
                                         [])}
                 ;; A whitespace-only name is not a name either.
                 name (assoc :client-name name))
        parsed (validate! RegisterClientBody value
                          {:status 400 :error "invalid_client_metadata"
                           :detail "redirect_uris is required"})]
    (when (empty? (:redirect-uris parsed))
      (throw (http-error 400 "invalid_client_metadata" "redirect_uris is required")))
    parsed))

(defn parse-authorize-query [req]
  (let [q (or (aget req "query") (js/Object.))]
    (validate! AuthorizeQuery
               {:client-id             (str (or (aget q "client_id") ""))
                :redirect-uri          (str (or (aget q "redirect_uri") ""))
                :state                 (when-let [s (aget q "state")] (str s))
                :code-challenge        (str (or (aget q "code_challenge") ""))
                :code-challenge-method (str (or (aget q "code_challenge_method") "S256"))
                :scope                 (when-let [scope (aget q "scope")] (str scope))}
               {:status 400 :error "invalid_request"})))

(defn parse-authorize-confirm-query [req]
  (let [q (or (aget req "query") (js/Object.))]
    (validate! AuthorizeConfirmQuery
               {:client-id             (str (or (aget q "client_id") ""))
                :redirect-uri          (str (or (aget q "redirect_uri") ""))
                :state                 (when-let [s (aget q "state")] (str s))
                :code-challenge        (str (or (aget q "code_challenge") ""))
                :code-challenge-method (str (or (aget q "code_challenge_method") "S256"))
                :scope                 (when-let [scope (aget q "scope")] (str scope))
                :selected-tools        (normalize-tool-selection (aget q "tool"))
                :displayed-actor       (str (or (aget q "actor_id") ""))}
               {:status 400 :error "invalid_request"})))

(defn parse-token-exchange-body [req]
  (let [body (or (aget req "body") (js/Object.))]
    (validate! TokenExchangeBody
               {:grant-type    (str (or (aget body "grant_type") (aget body "grantType") ""))
                :code          (str (or (aget body "code") ""))
                :code-verifier (str (or (aget body "code_verifier") (aget body "codeVerifier") ""))
                :client-id     (str (or (aget body "client_id") (aget body "clientId") ""))
                :redirect-uri  (str (or (aget body "redirect_uri") (aget body "redirectUri") ""))}
               {:status 400 :error "invalid_request"})))

(defn parse-revoke-token-params [req]
  (let [params (or (aget req "params") (js/Object.))]
    (validate! RevokeTokenParams
               {:token-id (str (or (aget params "tokenId") ""))}
               {:status 400 :error "invalid_request"})))


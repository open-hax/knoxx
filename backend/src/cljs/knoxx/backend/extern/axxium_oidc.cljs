(ns knoxx.backend.extern.axxium-oidc
  "OIDC relying-party and browser transport boundary. Native handles never leave this adapter."
  (:require ["openid-client" :as client]
            [clojure.string :as str]
            [knoxx.backend.law.oidc :as law]))

(def session-cookie "Opaque browser admission cookie name." "__Host-knoxx_axxium_session")
(def transaction-cookie "Browser binding cookie name; contains no PKCE verifier." "__Host-knoxx_axxium_transaction")
(def callback-path "Single fixed authorization callback path." "/api/auth/axxium/callback")

(defn now-ms "Read the server clock." [] (js/Date.now))
(defn random-id "Generate a cryptographically random opaque handle." [] (client/randomState))

(defn- require-https!
  "Require an exact HTTPS URI without credentials, query or fragment."
  [value]
  (let [uri (js/URL. value)]
    (when-not (and (= "https:" (.-protocol uri))
                   (str/blank? (.-username uri)) (str/blank? (.-password uri))
                   (str/blank? (.-search uri)) (str/blank? (.-hash uri))
                   (not (str/includes? (.-pathname uri) "/.well-known/")))
      (throw (ex-info "OIDC requires an exact HTTPS URI" {:status 503 :code :oidc-config})))
    uri))

(defn require-config!
  "Validate trusted configuration before making any protocol request."
  [config]
  (law/require-config! config)
  (require-https! (:issuer config))
  (let [origin (require-https! (:public-origin config))]
    (when-not (= (:public-origin config) (.-origin origin))
      (throw (ex-info "KNOXX_PUBLIC_BASE_URL must be an exact origin without trailing slash"
                      {:status 503 :code :oidc-config}))))
  config)

(defn configured
  "Read operator-only configuration. No request field participates in configuration."
  []
  (when (= "true" (aget (.-env js/process) "KNOXX_AXXIUM_OIDC_ENABLED"))
    (require-config!
     {:issuer (or (aget (.-env js/process) "KNOXX_AXXIUM_OIDC_ISSUER")
                  "https://axxium.promethean.rest/oidc")
      :public-origin (aget (.-env js/process) "KNOXX_PUBLIC_BASE_URL")
      :client-id (aget (.-env js/process) "KNOXX_AXXIUM_OIDC_CLIENT_ID")
      :client-secret (aget (.-env js/process) "KNOXX_AXXIUM_OIDC_CLIENT_SECRET")
      :links (js->clj (js/JSON.parse (or (aget (.-env js/process) "KNOXX_AXXIUM_OIDC_LINKS") "[]"))
                      :keywordize-keys true)})))

(defn callback-uri "Build the single configured redirect URI." [config]
  (str (:public-origin (require-config! config)) callback-path))

(defn- require-metadata!
  "Pin discovered endpoints to the configured HTTPS authority."
  [config metadata]
  (when-not (= (:issuer config) (.-issuer metadata))
    (throw (ex-info "OIDC issuer mismatch" {:status 503})))
  (doseq [field ["authorization_endpoint" "token_endpoint" "jwks_uri"
                 "introspection_endpoint" "revocation_endpoint"]]
    (let [endpoint (require-https! (aget metadata field))]
      (when-not (= (.-origin (js/URL. (:issuer config))) (.-origin endpoint))
        (throw (ex-info "OIDC endpoint escaped configured authority" {:status 503})))))
  metadata)

(defn ^:async discover!
  "Discover and validate the RP configuration. Optional opaque fetch is for isolated protocol tests."
  ([config] (await (discover! config nil)))
  ([config transport]
   (require-config! config)
   (let [options #js {:timeout 5}
         _ (when transport (aset options client/customFetch transport))
         rp (await (client/discovery (js/URL. (:issuer config)) (:client-id config)
                                    #js {:id_token_signed_response_alg "RS256"}
                                    (client/ClientSecretPost (:client-secret config)) options))]
     (require-metadata! config (.serverMetadata rp))
     ;; Code flow over TLS does not otherwise require signature verification in
     ;; openid-client. Explicitly require JWKS verification as well as claims.
     (client/enableNonRepudiationChecks rp)
     rp)))

(defn ^:async authorization!
  "Create state/nonce/S256 PKCE, retaining verifier only on the server."
  [rp config]
  (let [state (random-id)
        nonce (client/randomNonce)
        verifier (client/randomPKCECodeVerifier)
        challenge (await (client/calculatePKCECodeChallenge verifier))]
    {:state state :nonce nonce :verifier verifier
     :url (.toString (client/buildAuthorizationUrl
                      rp #js {:redirect_uri (callback-uri config) :response_type "code"
                              :scope "openid profile" :state state :nonce nonce
                              :code_challenge challenge :code_challenge_method "S256"}))}))

(defn ^:async exchange!
  "Validate callback state, ID-token signature/issuer/audience/nonce and exchange PKCE code."
  [rp config query transaction]
  (let [url (js/URL. (callback-uri config))]
    (set! (.-search url) (.toString (js/URLSearchParams. (clj->js query))))
    (let [tokens (await (client/authorizationCodeGrant
                         rp url #js {:expectedState (:state transaction)
                                     :expectedNonce (:nonce transaction)
                                     :pkceCodeVerifier (:verifier transaction)
                                     :idTokenExpected true}))
          claims (.claims tokens)]
      (when-not (and (law/nonblank? (.-access_token tokens)) claims
                     (law/nonblank? (.-sub claims)) (number? (.-exp claims)))
        (throw (ex-info "OIDC response missing grant evidence" {:status 401})))
      {:subject (.-sub claims) :expires-at (* 1000 (.-exp claims))
       :access-token (.-access_token tokens)})))

(defn ^:async introspect!
  "Return client-authenticated introspection as CLJS data."
  [rp token]
  (js->clj (await (client/tokenIntrospection rp token)) :keywordize-keys true))

(defn ^:async revoke!
  "Revoke this client's grant token without exposing it to the browser."
  [rp token]
  (await (client/tokenRevocation rp token #js {:token_type_hint "access_token"})))

(defn request-data
  "Decode only the request fields used by this boundary; cookies stay opaque."
  [request]
  {:session-id (some-> request .-cookies (aget session-cookie))
   :browser-binding (some-> request .-cookies (aget transaction-cookie))
   ;; Fastify's query parser uses a null/custom prototype. Decode entries
   ;; explicitly: js->clj alone leaves that native object opaque.
   :query (let [query (or (.-query request) #js {})]
            (into {} (map (fn [key] [key (js->clj (aget query key))]))
                  (array-seq (js/Object.keys query))))
   :origin (aget (.-headers request) "origin")})

(defn protected-request?
  "Skip login/logout endpoints; protect auth context and all other admitted surfaces."
  [request]
  (let [path (first (str/split (.-url request) #"\?"))]
    (or (= path "/api/auth/context")
        (not (str/starts-with? path "/api/auth/")))))

(defn set-cookie!
  "Set an opaque, host-only HTTPS cookie. Never serialize a grant or verifier."
  [reply cookie value ttl]
  (.setCookie reply cookie value #js {:path "/" :httpOnly true :secure true
                                      :sameSite "Lax" :maxAge ttl}))

(defn clear-cookie! "Clear a host-only HTTPS cookie." [reply cookie]
  (.clearCookie reply cookie #js {:path "/" :httpOnly true :secure true :sameSite "Lax"}))

(defn redirect! "Send a no-store redirect without leaking the callback in Referer." [reply url]
  (.header reply "Cache-Control" "no-store")
  (.header reply "Referrer-Policy" "no-referrer")
  (.redirect reply url))

(defn send! "Send only a safe shaped response." [reply status body]
  (.header reply "Cache-Control" "no-store")
  (.header reply "Referrer-Policy" "no-referrer")
  (.send (.code reply status) (clj->js body)))

(defn replace-identity-headers!
  "Overwrite browser-supplied identity and tenant selectors with the verified Knoxx context."
  [request ctx]
  (let [headers (.-headers request)]
    (doseq [header-name ["x-knoxx-user-email" "x-knoxx-membership-id" "x-knoxx-org-id" "x-knoxx-org-slug"]]
      (js-delete headers header-name))
    (doseq [[header-name value] {"x-knoxx-user-email" (get-in ctx [:user :email])
                        "x-knoxx-membership-id" (get-in ctx [:membership :id])
                        "x-knoxx-org-id" (get-in ctx [:org :id])
                        "x-knoxx-org-slug" (get-in ctx [:org :slug])}]
      (when value (aset headers header-name value)))))

(defn upgraded-request?
  "Identify a WebSocket upgrade without exposing native headers to orchestration."
  [request]
  (= "websocket" (some-> (aget (.-headers request) "upgrade") str/lower-case)))

(defn limit-upgrade!
  "Give OIDC WebSocket transports a hard 25-second admission lease.
   Reconnection must pass introspection again. Close the underlying transport,
   rather than allowing an upgraded connection to retain an unlimited grant."
  [request]
  (when (upgraded-request? request)
    (when-let [socket (some-> request .-raw .-socket)]
      (let [timer (js/setTimeout (fn [] (.destroy socket)) 25000)]
        (.unref timer)
        (.once socket "close" (fn [] (js/clearTimeout timer)))))))

(defn register-routes!
  "Mount the fixed protocol routes with supplied async handlers."
  [app {:keys [admit start callback logout]}]
  ;; This hook is mandatory for OIDC and independent of the native opt-in hook.
  (.addHook app "onRequest" admit)
  (.get app "/api/auth/axxium/start" start)
  (.get app callback-path callback)
  (.post app "/api/auth/axxium/logout" logout))

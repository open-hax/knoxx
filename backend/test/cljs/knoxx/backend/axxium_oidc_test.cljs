(ns knoxx.backend.axxium-oidc-test
  "Real pinned relying-party behavior against an isolated HTTPS-shaped protocol fixture."
  (:require ["jose" :as jose]
            ["node:crypto" :as crypto]
            [cljs.test :as test]
            [knoxx.backend.extern.axxium-oidc :as wire]
            [knoxx.backend.law.oidc-test :as fixture]))

(defn- response
  "Return a protocol JSON response to the RP's injected fetch transport."
  ([body] (response body 200))
  ([body status]
   (js/Response. (js/JSON.stringify (clj->js body))
                 #js {:status status :headers #js {"content-type" "application/json"}})))

(defn- metadata
  "Discovery metadata for the fixture authority only."
  []
  {:issuer (:issuer fixture/config)
   :authorization_endpoint "https://authority.example/oidc/auth"
   :token_endpoint "https://authority.example/oidc/token"
   :jwks_uri "https://authority.example/oidc/jwks"
   :introspection_endpoint "https://authority.example/oidc/token/introspection"
   :revocation_endpoint "https://authority.example/oidc/token/revocation"
   :response_types_supported ["code"] :subject_types_supported ["public"]
   :id_token_signing_alg_values_supported ["RS256"]
   :token_endpoint_auth_methods_supported ["client_secret_post"]
   :code_challenge_methods_supported ["S256"]})

(defn- ^:async signed-token
  "Sign fixture claims; all verification remains in openid-client, never in consumer code."
  [private-key nonce overrides]
  (let [now (quot (js/Date.now) 1000)
        claims (merge {:iss (:issuer fixture/config) :aud (:client-id fixture/config)
                       :sub "subject-1" :nonce nonce :iat now :exp (+ now 300)} overrides)]
    (await (.sign (.setProtectedHeader (new jose/SignJWT (clj->js claims))
                                      #js {:alg "RS256" :kid "fixture-key"}) private-key))))

(defn- pkce-challenge
  "Model the authority's S256 check independently of the RP helper."
  [verifier]
  (-> (.createHash crypto "sha256") (.update verifier) (.digest "base64url")))

(defn- ^:async protocol-fixture!
  "Generate disposable signing keys and an in-memory transport, with no sockets or identity store."
  []
  (let [key-pair (await (jose/generateKeyPair "RS256"))
        jwk (js->clj (await (jose/exportJWK (.-publicKey key-pair))) :keywordize-keys true)
        expected (atom {}) claims (atom {}) discovery (atom (metadata)) calls (atom [])
        transport (^:async fn [input options]
                    (let [path (.-pathname (js/URL. (str input)))
                          params (js/URLSearchParams. (.-body options))]
                      (swap! calls conj path)
                      (cond
                        (.endsWith path "/.well-known/openid-configuration") (response @discovery)
                        (= path "/oidc/jwks") (response {:keys [(assoc jwk :kid "fixture-key" :alg "RS256") ]})
                        (= path "/oidc/token")
                        (if (and (= (:challenge @expected) (pkce-challenge (or (.get params "code_verifier") "")))
                                 (= (wire/callback-uri fixture/config) (.get params "redirect_uri"))
                                 (= (:client-id fixture/config) (.get params "client_id"))
                                 (= (:client-secret fixture/config) (.get params "client_secret")))
                          (response {:access_token "opaque-fixture-access-token" :token_type "Bearer" :expires_in 300
                                     :id_token (await (signed-token (.-privateKey key-pair) (:nonce @expected) @claims))})
                          (response {:error "invalid_grant"} 400))
                        :else (response {:error "unexpected-fixture-request"} 404))))]
    {:expected expected :claims claims :discovery discovery :calls calls :transport transport}))

(defn- ^:async rejection?
  "Require a protocol rejection rather than interpreting an absent assertion as coverage."
  [operation]
  (try (await (operation)) false (catch :default _ true)))

(defn- ^:async transaction!
  "Capture only what an authorization endpoint sees; never a verifier or secret in its URL."
  [{:keys [expected transport]}]
  (let [rp (await (wire/discover! fixture/config transport))
        tx (await (wire/authorization! rp fixture/config))
        params (.-searchParams (js/URL. (:url tx)))]
    (reset! expected {:challenge (.get params "code_challenge") :nonce (.get params "nonce")})
    (test/is (= "S256" (.get params "code_challenge_method")))
    (test/is (= (wire/callback-uri fixture/config) (.get params "redirect_uri")))
    (test/is (nil? (.get params "client_secret")))
    (test/is (nil? (.get params "code_verifier")))
    {:rp rp :tx tx :query {"state" (:state tx) "code" "fixture-code"}}))

(test/deftest ^:async maintained-rp-validates-pkce-state-nonce-issuer-and-audience
  (let [{:keys [claims] :as f} (await (protocol-fixture!))
        {:keys [rp tx query]} (await (transaction! f))]
    (test/is (= "subject-1" (:subject (await (wire/exchange! rp fixture/config query tx)))))
    (test/is (await (rejection? #(wire/exchange! rp fixture/config (assoc query "state" "wrong-state") tx))))
    (test/is (await (rejection? #(wire/exchange! rp fixture/config query (assoc tx :verifier "wrong-pkce")))))
    (doseq [invalid [{:nonce "wrong-nonce"} {:iss "https://other.example/oidc"}
                     {:aud "other-product"} {:exp 1}]]
      (reset! claims invalid)
      (test/is (await (rejection? #(wire/exchange! rp fixture/config query tx)))))))

(test/deftest ^:async discovery-refuses-issuer-and-endpoint-substitution
  (doseq [change [{:issuer "https://other.example/oidc"}
                  {:token_endpoint "https://other.example/token"}
                  {:introspection_endpoint "http://authority.example/introspect"}]]
    (let [{:keys [transport discovery]} (await (protocol-fixture!))]
      (swap! discovery merge change)
      (test/is (await (rejection? #(wire/discover! fixture/config transport)))))))

(test/deftest fixed-uri-and-wire-conversion
  (test/is (= "https://knoxx.example/api/auth/axxium/callback" (wire/callback-uri fixture/config)))
  (doseq [change [{:issuer "https://authority.example/oidc?issuer=evil"}
                  {:public-origin "https://knoxx.example/attacker"}
                  {:public-origin "https://user:password@knoxx.example"}
                  {:public-origin "http://knoxx.example"}]]
    (test/is (thrown? js/Error (wire/require-config! (merge fixture/config change)))))
  (let [req #js {:headers #js {"host" "attacker.example" "x-forwarded-host" "attacker.example"
                               "x-knoxx-membership-id" "admin" "x-knoxx-org-id" "other"}
                 :cookies #js {} :query #js {:redirect "https://attacker.example"}}]
    (wire/replace-identity-headers! req fixture/context)
    (test/is (= "membership-1" (aget (.-headers req) "x-knoxx-membership-id")))
    (test/is (= "org-1" (aget (.-headers req) "x-knoxx-org-id")))
    (test/is (= {"redirect" "https://attacker.example"} (:query (wire/request-data req))))))

(test/deftest upgraded-transport-cannot-retain-admission-beyond-lease
  (let [original-set js/setTimeout original-clear js/clearTimeout
        timeout (atom nil) on-close (atom nil) destroyed (atom false) cleared (atom false)
        timer #js {:unref (fn [] nil)}
        socket #js {:destroy (fn [] (reset! destroyed true))
                    :once (fn [_event callback] (reset! on-close callback))}
        request #js {:headers #js {:upgrade "websocket"} :raw #js {:socket socket}}]
    (set! (.-setTimeout js/globalThis) (fn [callback milliseconds]
                                       (reset! timeout {:callback callback :milliseconds milliseconds}) timer))
    (set! (.-clearTimeout js/globalThis) (fn [handle] (reset! cleared (identical? timer handle))))
    (try
      (wire/limit-upgrade! request)
      (test/is (= 25000 (:milliseconds @timeout)))
      ((:callback @timeout))
      (test/is @destroyed "Expired lease destroys the upgraded transport")
      (@on-close)
      (test/is @cleared "Early socket close releases the timer")
      (finally
        (set! (.-setTimeout js/globalThis) original-set)
        (set! (.-clearTimeout js/globalThis) original-clear)))))


(ns knoxx.backend.mcp-http
  "Serve Knoxx tools over MCP (Model Context Protocol) Streamable HTTP.

   This is the *server-side* MCP facade used by external tools.

   Design goals:
   - Reuse Knoxx browser login (GitHub OAuth + cookie sessions) for identity.
   - Provide an explicit consent UI (capability dials) for delegated tool access.
   - Mint opaque Redis-backed tokens, scoped to a tool allowlist.
   - Intersect token allowlist with the caller's current Knoxx policy context.

   NOTE: This namespace intentionally avoids importing Node deps directly; the
   Node host shim injects MCP/Zod/Crypto dependencies via the `runtime` object."
  (:require [clojure.string :as str]
            [knoxx.backend.auth-session :as auth-session]
            [knoxx.backend.mcp-expose :as mcp-expose]
            [knoxx.backend.redis-client :as redis]))

(defonce ^:private mcp-sessions* (atom {}))

(defn- env
  [k default]
  (or (aget js/process.env k) default))

(defn- public-base-url
  [config]
  (try
    (js/URL.
     (or (aget js/process.env "KNOXX_PUBLIC_BASE_URL")
         (aget js/process.env "RENDER_EXTERNAL_URL")
         (:knoxx-base-url config)
         "http://localhost"))
    (catch :default _
      (js/URL. "http://localhost"))))

(defn- base64url
  [buf]
  (.toString (js/Buffer.from buf) "base64url"))

(defn- pkce-challenge
  [crypto verifier]
  (base64url (-> (.createHash crypto "sha256")
                 (.update (str verifier))
                 (.digest))))

(defn- bearer-token
  [req]
  (let [raw (str (or (some-> req (aget "headers") (aget "authorization")) ""))
        m (.match raw (js/RegExp. "^Bearer\\s+(.+)$" "i"))]
    (when m (str/trim (aget m 1)))))

(defn- resolve-session-id
  [req]
  (let [headers (or (aget req "headers") #js {})
        header-id (aget headers "mcp-session-id")
        q (or (aget req "query") #js {})
        query-id (aget q "sessionId")]
    (cond
      (and (string? header-id) (not (str/blank? header-id))) header-id
      (and (string? query-id) (not (str/blank? query-id))) query-id
      :else nil)))

(defn- safe
  [s]
  (-> (str (or s ""))
      (.replaceAll "&" "&amp;")
      (.replaceAll "<" "&lt;")
      (.replaceAll ">" "&gt;")
      (.replaceAll "\"" "&quot;")))

(defn- normalize-tool-selection
  [raw]
  (cond
    (nil? raw) []
    (array? raw) (mapv str raw)
    :else [(str raw)]))

(defn- json-send!
  [reply status payload]
  (-> (.code reply status)
      (.send (clj->js payload))))

(defn- require-redis!
  [reply]
  (if-let [client (redis/get-client)]
    client
    (do
      (json-send! reply 503 {:error "redis_unavailable" :detail "Redis is not connected"})
      nil)))

(defn- require-browser-auth-context!
  [policyDb runtime config req reply]
  (-> (auth-session/resolve-auth-context req policyDb)
      (.catch
       (fn [_]
         (let [base (public-base-url config)
               current-path (or (some-> req (aget "raw") (aget "url")) "/api/mcp/oauth/authorize")
               login-url (js/URL. "/api/auth/login" base)]
           (.set (.-searchParams login-url) "redirect" current-path)
           ;; Fastify v5: reply.redirect(url, [statusCode])
           (.redirect reply (.toString login-url) 302)
           (js/Promise.resolve nil))))))

(defn- get-registered-client
  [redis-client client-id]
  (if (str/blank? (str client-id))
    (js/Promise.resolve nil)
    (-> (.get redis-client (str "knoxx:mcp:client:" client-id))
        (.then (fn [raw]
                 (when raw
                   (try (js/JSON.parse raw)
                        (catch :default _ nil)))))
        (.catch (fn [_] nil)))))

(defn- redirect-uri-allowed?
  [client redirect-uri]
  (if-not client
    true
    (let [uris (or (aget client "redirect_uris") #js [])]
      (boolean (.includes (js/Array.from uris) redirect-uri)))))

(defn- oauth-confirm-params
  [req]
  (let [q (or (aget req "query") #js {})]
    {:client-id (str (or (aget q "client_id") ""))
     :redirect-uri (str (or (aget q "redirect_uri") ""))
     :state (when-let [s (aget q "state")] (str s))
     :code-challenge (str (or (aget q "code_challenge") ""))
     :code-challenge-method (str (or (aget q "code_challenge_method") "S256"))
     :selected-tools (normalize-tool-selection (aget q "tool"))}))

(defn- oauth-confirm-invalid-request?
  [{:keys [client-id redirect-uri code-challenge code-challenge-method]}]
  (or (str/blank? client-id)
      (str/blank? redirect-uri)
      (str/blank? code-challenge)
      (not= code-challenge-method "S256")))

(defn- oauth-confirm-requested-tools
  [runtime config ctx selected-tools]
  (let [all-tools (or (mcp-expose/create-knoxx-custom-tools-js runtime config ctx) #js [])
        available (into #{} (map (fn [t] (str (aget t "name")))) (array-seq all-tools))]
    (->> selected-tools
         (map (comp str/trim str))
         (remove str/blank?)
         distinct
         (filter (fn [t] (contains? available t)))
         vec)))

(defn- handle-oauth-authorize-confirm!
  [{:keys [policyDb runtime config crypto code-ttl]} req reply]
  (if-let [r (require-redis! reply)]
    (let [params (oauth-confirm-params req)]
      (if (oauth-confirm-invalid-request? params)
        (json-send! reply 400 {:error "invalid_request" :detail "Missing required OAuth parameters"})
        (let [{:keys [client-id redirect-uri state code-challenge selected-tools]} params
              p1 (get-registered-client r client-id)
              p2 (.then p1
                        (fn [client]
                          (when (and client (not (redirect-uri-allowed? client redirect-uri)))
                            (throw (js/Error. "redirect_uri not allowed for registered client")))
                          (require-browser-auth-context! policyDb runtime config req reply)))
              p3 (.then p2
                        (fn [ctx]
                          (if-not ctx
                            (js/Promise.resolve nil)
                            (let [requested (oauth-confirm-requested-tools runtime config ctx selected-tools)]
                              (if (empty? requested)
                                (do
                                  (json-send! reply 400 {:error "invalid_scope" :detail "No valid tools selected"})
                                  (js/Promise.resolve nil))
                                (let [membership-id (str (or (aget ctx "membership" "id") (aget ctx "membershipId") ""))
                                      user-email (str (or (aget ctx "user" "email") (aget ctx "userEmail") ""))
                                      org-slug (str (or (aget ctx "org" "slug") (aget ctx "orgSlug") ""))
                                      code (.randomUUID crypto)
                                      code-key (str "knoxx:mcp:code:" code)
                                      payload #js {:code code
                                                  :clientId client-id
                                                  :redirectUri redirect-uri
                                                  :codeChallenge code-challenge
                                                  :codeChallengeMethod "S256"
                                                  :tools (clj->js requested)
                                                  :membershipId membership-id
                                                  :userEmail user-email
                                                  :orgSlug org-slug
                                                  :createdAt (.toISOString (js/Date.))}
                                      p-set (.set r code-key (js/JSON.stringify payload) #js {:EX code-ttl})]
                                  (let [p-redir (.then p-set
                                                      (fn [_]
                                                        (let [redirect (js/URL. redirect-uri)]
                                                          (.set (.-searchParams redirect) "code" code)
                                                          (when state (.set (.-searchParams redirect) "state" state))
                                                          (.redirect reply (.toString redirect) 302))))]
                                    p-redir)))))))
              p4 (.catch p3
                        (fn [err]
                          (json-send! reply 400 {:error "invalid_request" :detail (.-message err)})))]
          p4)))
    nil))

(defn- tool-checkbox-html
  [tools selected]
  (->> tools
       (map (fn [t]
              (let [name (str (or (aget t "name") ""))
                    label (str (or (aget t "label") (aget t "name") (aget t "description") name))
                    desc (str (or (aget t "description") ""))
                    checked (if (contains? selected name) "checked" "")]
                (str "\n        <label style=\"display:block; margin: 6px 0;\">\n"
                     "          <input type=\"checkbox\" name=\"tool\" value=\"" (safe name) "\" " checked " />\n"
                     "          <span style=\"font-weight:600;\">" (safe label) "</span>\n"
                     "          <span style=\"color:#666;\">(" (safe name) ")</span>\n"
                     "          <div style=\"color:#444; margin-left: 22px;\">" (safe desc) "</div>\n"
                     "        </label>\n"))))
       (str/join "\n")))

(defn- default-selected-tools
  [tool-names]
  (let [defaults ["semantic_query" "semantic_read" "memory_search" "memory_session" "graph_query" "websearch" "read"]]
    (into #{}
          (filter (fn [id] (contains? tool-names id)))
          defaults)))

(defn register-mcp-http-routes!
  [app runtime config]
  (let [crypto (aget runtime "crypto")
        McpServer (aget runtime "McpServer")
        StreamableHTTPServerTransport (aget runtime "StreamableHTTPServerTransport")
        isInitializeRequest (aget runtime "isInitializeRequest")
        z (aget runtime "z")
        code-ttl (js/parseInt (env "KNOXX_MCP_CODE_TTL_SECONDS" "300") 10)
        token-ttl (js/parseInt (env "KNOXX_MCP_TOKEN_TTL_SECONDS" (str (* 60 60 24 30))) 10)
        base (public-base-url config)
        policyDb (aget runtime "policyDb")]

    ;; OAuth discovery
    (.get app "/.well-known/oauth-authorization-server"
          (fn [_req reply]
            (let [issuer (js/URL. (.toString base))]
              (.send reply
                     #js {:issuer (-> (.toString issuer) (.replace (js/RegExp. "/$") ""))
                          :authorization_endpoint (.toString (js/URL. "/api/mcp/oauth/authorize" issuer))
                          :token_endpoint (.toString (js/URL. "/api/mcp/oauth/token" issuer))
                          :registration_endpoint (.toString (js/URL. "/api/mcp/oauth/register" issuer))
                          :response_types_supported #js ["code"]
                          :grant_types_supported #js ["authorization_code"]
                          :code_challenge_methods_supported #js ["S256"]
                          :token_endpoint_auth_methods_supported #js ["none"]}))))

    ;; OAuth Dynamic Client Registration (public clients, no secret)
    (.post app "/api/mcp/oauth/register"
           (fn [req reply]
             (if-let [r (require-redis! reply)]
               (let [body (or (aget req "body") #js {})
                     redirect-uris (aget body "redirect_uris")
                     redirect-uris (if (array? redirect-uris) (mapv str (array-seq redirect-uris)) [])]
                 (if (empty? redirect-uris)
                   (json-send! reply 400 {:error "invalid_client_metadata" :detail "redirect_uris is required"})
                   (let [client-id (.randomUUID crypto)
                         client #js {:client_id client-id
                                     :client_name (or (aget body "client_name") "mcp-client")
                                     :redirect_uris (clj->js redirect-uris)
                                     :token_endpoint_auth_method "none"
                                     :grant_types #js ["authorization_code"]
                                     :response_types #js ["code"]
                                     :created_at (.toISOString (js/Date.))}]
                     (-> (.set r (str "knoxx:mcp:client:" client-id) (js/JSON.stringify client))
                         (.then (fn [_]
                                  (-> (.code reply 201)
                                      (.send client))))
                         (.catch (fn [err]
                                   (json-send! reply 500 {:error "registration_failed" :detail (.-message err)})))))))
               nil)))

    ;; OAuth authorize (consent screen)
    (.get app "/api/mcp/oauth/authorize"
          (fn [req reply]
            (if-let [r (require-redis! reply)]
              (let [q (or (aget req "query") #js {})
                    client-id (str (or (aget q "client_id") ""))
                    redirect-uri (str (or (aget q "redirect_uri") ""))
                    state (when-let [s (aget q "state")] (str s))
                    code-challenge (str (or (aget q "code_challenge") ""))
                    method (str (or (aget q "code_challenge_method") "S256"))
                    requested-scope (str (or (aget q "scope") ""))]
                (if (or (str/blank? client-id)
                        (str/blank? redirect-uri)
                        (str/blank? code-challenge)
                        (not= method "S256"))
                  (json-send! reply 400 {:error "invalid_request"
                                        :detail "Missing required OAuth parameters (client_id, redirect_uri, code_challenge, S256)"})
                  (-> (get-registered-client r client-id)
                      (.then (fn [client]
                               (when (and client (not (redirect-uri-allowed? client redirect-uri)))
                                 (throw (js/Error. "redirect_uri not allowed for registered client")))
                               client))
                      (.then (fn [_]
                               (require-browser-auth-context! policyDb runtime config req reply)))
                      (.then
                       (fn [ctx]
                         (when ctx
                           (let [tools (or (mcp-expose/create-knoxx-custom-tools-js runtime config ctx) #js [])
                                 requested (into #{}
                                                (comp (map str/trim)
                                                      (remove str/blank?))
                                                (str/split requested-scope #"\\s+"))
                                 tool-names (into #{} (map (fn [t] (str (aget t "name")))) (array-seq tools))
                                 selected (let [sel (into #{} (for [t (array-seq tools)
                                                                   :let [name (str (aget t "name"))]
                                                                   :when (or (contains? requested "all")
                                                                             (contains? requested name))]
                                                               name))]
                                            (if (seq sel) sel (default-selected-tools tool-names)))
                                 confirm-url (js/URL. "/api/mcp/oauth/authorize/confirm" base)]
                             (.set (.-searchParams confirm-url) "client_id" client-id)
                             (.set (.-searchParams confirm-url) "redirect_uri" redirect-uri)
                             (when state (.set (.-searchParams confirm-url) "state" state))
                             (.set (.-searchParams confirm-url) "code_challenge" code-challenge)
                             (.set (.-searchParams confirm-url) "code_challenge_method" "S256")
                             (when-not (str/blank? requested-scope)
                               (.set (.-searchParams confirm-url) "scope" requested-scope))

                             (let [html (str "<!doctype html>\n"
                                             "<html><head><meta charset=\"utf-8\" />\n"
                                             "<title>Authorize MCP Client</title>\n"
                                             "<style>body{font-family:ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial;margin:24px;}"
                                             ".box{max-width:920px;} .meta{color:#555;margin-bottom:12px;}"
                                             ".tools{border:1px solid #ddd;border-radius:8px;padding:12px 16px;}"
                                             ".actions{margin-top:18px;display:flex;gap:12px;}"
                                             "button{padding:8px 14px;border-radius:8px;border:1px solid #333;background:#111;color:#fff;cursor:pointer;}"
                                             "a{color:#0b67d0;}"
                                             "</style></head><body><div class=\"box\">\n"
                                             "<h1>Authorize MCP Client</h1>\n"
                                             "<div class=\"meta\">\n"
                                             "<div><strong>Client:</strong> " (safe client-id) "</div>\n"
                                             "<div><strong>Redirect URI:</strong> " (safe redirect-uri) "</div>\n"
                                             "</div>\n"
                                             "<form method=\"GET\" action=\"" (safe (.-pathname confirm-url)) "\">\n"
                                             "<input type=\"hidden\" name=\"client_id\" value=\"" (safe client-id) "\" />\n"
                                             "<input type=\"hidden\" name=\"redirect_uri\" value=\"" (safe redirect-uri) "\" />\n"
                                             "<input type=\"hidden\" name=\"state\" value=\"" (safe (or state "")) "\" />\n"
                                             "<input type=\"hidden\" name=\"code_challenge\" value=\"" (safe code-challenge) "\" />\n"
                                             "<input type=\"hidden\" name=\"code_challenge_method\" value=\"S256\" />\n"
                                             "<input type=\"hidden\" name=\"scope\" value=\"" (safe requested-scope) "\" />\n"
                                             "<h2>Capabilities</h2>\n"
                                             "<p>Select exactly which Knoxx tools this client can call.</p>\n"
                                             "<div class=\"tools\">\n"
                                             (tool-checkbox-html tools selected)
                                             "</div>\n"
                                             "<div class=\"actions\">\n"
                                             "<button type=\"submit\">Authorize</button>\n"
                                             "<a href=\"/\">Cancel</a>\n"
                                             "</div></form></div></body></html>")]
                               (-> (.header reply "content-type" "text/html; charset=utf-8")
                                   (.send html)))))))
                      (.catch (fn [err]
                                (json-send! reply 400 {:error "invalid_request" :detail (.-message err)}))))))
              nil)))

    ;; OAuth authorize confirm (issues code)
    (.get app "/api/mcp/oauth/authorize/confirm"
          (fn [req reply]
            (handle-oauth-authorize-confirm!
             {:policyDb policyDb
              :runtime runtime
              :config config
              :crypto crypto
              :code-ttl code-ttl}
             req reply)))

    ;; OAuth token exchange endpoint (authorization_code + PKCE S256)
    (.post app "/api/mcp/oauth/token"
           (fn [req reply]
             (if-let [r (require-redis! reply)]
               (let [body (or (aget req "body") #js {})
                     grant-type (str (or (aget body "grant_type") (aget body "grantType") ""))
                     code (str (or (aget body "code") ""))
                     code-verifier (str (or (aget body "code_verifier") (aget body "codeVerifier") ""))
                     client-id (str (or (aget body "client_id") (aget body "clientId") ""))
                     redirect-uri (str (or (aget body "redirect_uri") (aget body "redirectUri") ""))]
                 (if (or (not= grant-type "authorization_code")
                         (str/blank? code)
                         (str/blank? code-verifier)
                         (str/blank? client-id)
                         (str/blank? redirect-uri))
                   (json-send! reply 400 {:error "invalid_request"})
                   (-> (get-registered-client r client-id)
                       (.then
                        (fn [client]
                          (when (and client (not (redirect-uri-allowed? client redirect-uri)))
                            (throw (js/Error. "redirect_uri not allowed for registered client")))
                          (-> (.get r (str "knoxx:mcp:code:" code))
                              (.then
                               (fn [raw]
                                 (when-not raw
                                   (throw (js/Error. "Unknown or expired code")))
                                 (let [record (js/JSON.parse raw)]
                                   (when (or (not= (aget record "clientId") client-id)
                                             (not= (aget record "redirectUri") redirect-uri))
                                     (throw (js/Error. "Client/redirect mismatch")))
                                   (let [expected (str (or (aget record "codeChallenge") ""))
                                         actual (pkce-challenge crypto code-verifier)]
                                     (when (or (str/blank? expected) (not= expected actual))
                                       (throw (js/Error. "PKCE verification failed")))
                                     (-> (.del r (str "knoxx:mcp:code:" code))
                                         (.then
                                          (fn [_]
                                            (let [access-token (.randomUUID crypto)
                                                  token-key (str "knoxx:mcp:token:" access-token)
                                                  token-value #js {:accessToken access-token
                                                                  :clientId client-id
                                                                  :membershipId (aget record "membershipId")
                                                                  :userEmail (aget record "userEmail")
                                                                  :orgSlug (aget record "orgSlug")
                                                                  :tools (aget record "tools")
                                                                  :createdAt (.toISOString (js/Date.))
                                                                  :expiresAt (.toISOString (js/Date. (+ (.now js/Date) (* token-ttl 1000))))}]
                                              (-> (.set r token-key (js/JSON.stringify token-value) #js {:EX token-ttl})
                                                  (.then (fn [_]
                                                           (when-let [mid (aget record "membershipId")]
                                                             (.sAdd r (str "knoxx:mcp:user:" mid ":tokens") access-token))
                                                           (.send reply
                                                                  #js {:access_token access-token
                                                                       :token_type "Bearer"
                                                                       :scope (->> (js/Array.from (or (aget record "tools") #js []))
                                                                                   (str/join " "))
                                                                       :expires_in token-ttl})))))))))))))))))
                       (.catch (fn [err]
                                 (json-send! reply 400 {:error "invalid_grant" :detail (.-message err)}))))))
               nil)))

    ;; Token management (browser session only)
    (.get app "/api/mcp/tokens"
          (fn [req reply]
            (if-let [r (require-redis! reply)]
              (-> (require-browser-auth-context! policyDb runtime config req reply)
                  (.then
                   (fn [ctx]
                     (when ctx
                       (let [membership-id (str (or (aget ctx "membership" "id") (aget ctx "membershipId") ""))]
                         (if (str/blank? membership-id)
                           (json-send! reply 400 {:error "missing_membership"})
                           (-> (.sMembers r (str "knoxx:mcp:user:" membership-id ":tokens"))
                               (.then
                                (fn [token-ids]
                                  (-> (js/Promise.all
                                       (clj->js
                                        (for [token-id (array-seq token-ids)]
                                          (-> (.get r (str "knoxx:mcp:token:" token-id))
                                              (.then (fn [raw]
                                                       (when raw
                                                         (try (js/JSON.parse raw)
                                                              (catch :default _ nil)))))))))
                                      (.then
                                       (fn [records]
                                         (let [filtered (->> (array-seq records) (remove nil?) (into-array))]
                                           (.send reply #js {:ok true :tokens filtered})))))))))))))
                  (.catch (fn [err]
                            (json-send! reply 401 {:error "unauthorized" :detail (.-message err)}))))
              nil)))

    (.delete app "/api/mcp/tokens/:tokenId"
             (fn [req reply]
               (if-let [r (require-redis! reply)]
                 (-> (require-browser-auth-context! policyDb runtime config req reply)
                     (.then
                      (fn [ctx]
                        (when ctx
                          (let [membership-id (str (or (aget ctx "membership" "id") (aget ctx "membershipId") ""))
                                token-id (str (or (aget (aget req "params") "tokenId") ""))]
                            (if (or (str/blank? membership-id) (str/blank? token-id))
                              (json-send! reply 400 {:error "invalid_request"})
                              (-> (.del r (str "knoxx:mcp:token:" token-id))
                                  (.then (fn [_]
                                           (.sRem r (str "knoxx:mcp:user:" membership-id ":tokens") token-id)
                                           (.send reply #js {:ok true})))))))))
                     (.catch (fn [err]
                               (json-send! reply 401 {:error "unauthorized" :detail (.-message err)}))))
                 nil)))

    ;; MCP transport
    (letfn [(load-token-record [redis-client req]
              (let [token (bearer-token req)]
                (if (str/blank? token)
                  (js/Promise.resolve nil)
                  (-> (.get redis-client (str "knoxx:mcp:token:" token))
                      (.then (fn [raw]
                               (when raw
                                 (try (js/JSON.parse raw)
                                      (catch :default _ nil)))))
                      (.catch (fn [_] nil))))))

            (handle-mcp-session! [req reply]
              (let [session-id (resolve-session-id req)]
                (cond
                  (str/blank? (str session-id))
                  (do (.code reply 400) (.send reply "Missing mcp-session-id"))

                  :else
                  (let [{:keys [transport token]} (get @mcp-sessions* session-id)]
                    (if-not transport
                      (do (.code reply 404) (.send reply (str "Invalid mcp-session-id: " session-id)))
                      (let [supplied (bearer-token req)]
                        (if (not= (str supplied) (str token))
                          (do (.code reply 401) (.send reply "Unauthorized"))
                          (.handleRequest transport (aget req "raw") (aget reply "raw")))))))))

            (handle-mcp-post! [req reply]
              (let [session-id (resolve-session-id req)
                    existing (when session-id (get @mcp-sessions* session-id))
                    supplied (bearer-token req)]
                (if existing
                  (if (not= (str supplied) (str (:token existing)))
                    (do (.code reply 401) (.send reply "Unauthorized"))
                    (.handleRequest (:transport existing) (aget req "raw") (aget reply "raw") (aget req "body")))
                  ;; New session: must initialize
                  (if-not (and isInitializeRequest (isInitializeRequest (aget req "body")))
                    (do
                      (.code reply 400)
                      (.send reply #js {:jsonrpc "2.0"
                                        :error #js {:code -32000 :message "Bad Request: Server not initialized"}
                                        :id nil}))
                    (if-let [r (require-redis! reply)]
                      (-> (load-token-record r req)
                          (.then
                           (fn [token-record]
                             (if-not token-record
                               (do (.code reply 401) (.send reply "Unauthorized"))
                               (let [headers-like #js {}
                                     mid (aget token-record "membershipId")
                                     email (aget token-record "userEmail")
                                     org (aget token-record "orgSlug")]
                                 (when mid (aset headers-like "x-knoxx-membership-id" mid))
                                 (when email (aset headers-like "x-knoxx-user-email" email))
                                 (when org (aset headers-like "x-knoxx-org-slug" org))
                                 (-> (.resolveRequestContext policyDb headers-like)
                                     (.then
                                      (fn [ctx]
                                        (let [all-tools (or (mcp-expose/create-knoxx-custom-tools-js runtime config ctx) #js [])
                                              allowed (into #{} (map str) (array-seq (or (aget token-record "tools") #js [])))
                                              effective (->> (array-seq all-tools)
                                                             (filter (fn [t] (contains? allowed (str (aget t "name")))))
                                                             (into-array))
                                              server (new McpServer #js {:name "knoxx" :version "0.1.0"})
                                              input-schema (when z (.record z (.any z)))
                                              transport (new StreamableHTTPServerTransport
                                                            #js {:sessionIdGenerator (fn [] (.randomUUID crypto))
                                                                 :onsessioninitialized (fn [sid]
                                                                                        (swap! mcp-sessions* assoc (str sid)
                                                                                               {:transport transport
                                                                                                :token (aget token-record "accessToken")}) )})]
                                          (doseq [tool (array-seq effective)]
                                            (let [tool-name (-> (str (or (aget tool "name") "")) str/trim)]
                                              (when-not (str/blank? tool-name)
                                                (.registerTool
                                                 server
                                                 tool-name
                                                 #js {:description (str (or (aget tool "description") (aget tool "label") tool-name))
                                                      :inputSchema input-schema}
                                                 (fn [params]
                                                   (.execute tool "mcp" params nil nil nil))))))

                                          (set! (.-onclose transport)
                                                (fn []
                                                  (when-let [sid (.-sessionId transport)]
                                                    (swap! mcp-sessions* dissoc (str sid)))))

                                          (-> (.connect server transport)
                                              (.then (fn [_]
                                                       (.handleRequest transport (aget req "raw") (aget reply "raw") (aget req "body")))))))))
                                     (.catch
                                      (fn [err]
                                        (.error js/console "[knoxx-mcp] resolveRequestContext failed" err)
                                        (json-send! reply 401 {:error "unauthorized"})))))))))))
                          (.catch (fn [err]
                                    (.error js/console "[knoxx-mcp] initialize failed" err)
                                    (json-send! reply 500 {:error "mcp_init_failed" :detail (.-message err)}))))
                      nil))))]


      (.get app "/mcp" (fn [req reply] (handle-mcp-session! req reply)))
      (.delete app "/mcp" (fn [req reply] (handle-mcp-session! req reply))))

    nil))

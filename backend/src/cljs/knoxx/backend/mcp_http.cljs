

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

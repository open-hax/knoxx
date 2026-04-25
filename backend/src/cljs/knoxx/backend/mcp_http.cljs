(ns knoxx.backend.mcp-http
  "Serve Knoxx tools over MCP (Model Context Protocol) Streamable HTTP.

   Design goals:
   - keep the Node host shim as dependency injection only
   - make every route a single workflow fn that accepts a context map
   - validate normalized inputs with Malli before side effects
   - keep route registration declarative via a tiny macro DSL"
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]
            [knoxx.backend.app-shapes :refer [route!]]
            [knoxx.backend.auth.session :as auth-session]
            [knoxx.backend.mcp-expose :as mcp-expose]
            [knoxx.backend.redis-client :as redis]))

(declare typebox->zod-shape
         reply-header!)

(defonce ^:private mcp-sessions* (atom {}))

(def ^:private RegisterClientBody
  [:map
   [:redirect-uris [:vector string?]]
   [:client-name {:optional true} string?]])

(def ^:private AuthorizeQuery
  [:map
   [:client-id string?]
   [:redirect-uri string?]
   [:state {:optional true} [:maybe string?]]
   [:code-challenge string?]
   [:code-challenge-method string?]
   [:scope {:optional true} [:maybe string?]]])

(def ^:private AuthorizeConfirmQuery
  [:map
   [:client-id string?]
   [:redirect-uri string?]
   [:state {:optional true} [:maybe string?]]
   [:code-challenge string?]
   [:code-challenge-method string?]
   [:scope {:optional true} [:maybe string?]]
   [:selected-tools [:vector string?]]])

(def ^:private TokenExchangeBody
  [:map
   [:grant-type string?]
   [:code string?]
   [:code-verifier string?]
   [:client-id string?]
   [:redirect-uri string?]])

(def ^:private RevokeTokenParams
  [:map
   [:token-id string?]])

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
  [^js crypto verifier]
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
    (array? raw) (mapv str (array-seq raw))
    :else [(str raw)]))

(defn- json-send!
  [reply status payload]
  (-> (.code reply status)
      (.send (clj->js payload))))

(defn- text-send!
  [reply status body]
  (-> (.code reply status)
      (.send body)))

(defn- protected-resource-metadata-url
  [base]
  (.toString (js/URL. "/.well-known/oauth-protected-resource" base)))

(defn- www-authenticate-challenge
  [base]
  (str "Bearer realm=\"mcp\", resource_metadata=\""
       (protected-resource-metadata-url base)
       "\""))

(defn- challenge-unauthorized!
  [^js reply base]
  (-> (reply-header! reply "WWW-Authenticate" (www-authenticate-challenge base))
      (.code 401)
      (.send "Unauthorized")))

(defn- redis-set!
  [^js client key value opts]
  (.set client key value opts))

(defn- redis-get!
  [^js client key]
  (.get client key))

(defn- redis-del!
  [^js client key]
  (.del client key))

(defn- redis-sadd!
  [^js client key value]
  (.sAdd client key value))

(defn- redis-smembers!
  [^js client key]
  (.sMembers client key))

(defn- redis-srem!
  [^js client key value]
  (.sRem client key value))

(defn- transport-handle-request!
  ([^js transport req reply]
   (.handleRequest transport req reply))
  ([^js transport req reply body]
   (.handleRequest transport req reply body)))

(defn- tool-execute!
  [^js tool params]
  (.execute tool "mcp" params nil nil nil))

(defn- reply-header!
  [^js reply name value]
  (.header reply name value))

(defn- ensure-streamable-accept!
  [req]
  (let [raw (aget req "raw")
        headers (or (aget raw "headers") #js {})
        raw-headers (or (aget raw "rawHeaders") #js [])
        accept-value "application/json, text/event-stream"
        accept (str/lower-case (str (or (aget headers "accept") "")))
        has-json? (str/includes? accept "application/json")
        has-sse? (str/includes? accept "text/event-stream")]
    (when (or (str/blank? accept) (not has-json?) (not has-sse?))
      (aset headers "accept" accept-value)
      (aset raw "headers" headers)
      (let [pairs (partition 2 (array-seq raw-headers))
            filtered (->> pairs
                          (remove (fn [[k _]]
                                    (= "accept" (str/lower-case (str k)))))
                          (mapcat identity)
                          vec)]
        (aset raw "rawHeaders"
              (clj->js (conj filtered "accept" accept-value)))))
    req))

(defn- http-error
  ([status error detail]
   (ex-info detail {:status status :error error :detail detail}))
  ([status error detail data]
   (ex-info detail (merge {:status status :error error :detail detail} data))))

(defn- validation-detail
  [schema value]
  (some-> (m/explain schema value)
          me/humanize
          pr-str))

(defn- validate!
  [schema value {:keys [status error detail]}]
  (if (m/validate schema value)
    value
    (throw (http-error (or status 400)
                       (or error "invalid_request")
                       (or detail (validation-detail schema value) "Invalid request")))))

(defn- handle-route-error!
  [reply err]
  (when-not (aget reply "sent")
    (let [data (ex-data err)
          status (or (:status data) 500)
          error (or (:error data) "internal_error")
          detail (or (:detail data)
                     (some-> err .-message)
                     (str err)
                     "Unexpected error")]
      (when (>= status 500)
        (.error js/console "[knoxx-mcp] route failed" err))
      (json-send! reply status {:error error :detail detail}))))

(defn- as-promise
  [value]
  (cond
    (instance? js/Promise value)
    (.then value (fn [resolved]
                   (if (nil? resolved)
                     js/undefined
                     resolved)))

    (nil? value)
    (js/Promise.resolve js/undefined)

    :else
    (js/Promise.resolve value)))

(defn- require-redis!
  [reply]
  (if-let [client (redis/get-client)]
    client
    (do
      (json-send! reply 503 {:error "redis_unavailable"
                             :detail "Redis is not connected"})
      nil)))

(defn- require-browser-auth-context!
  [policy-db _runtime config req reply]
  (-> (auth-session/resolve-auth-context req policy-db)
      (.catch
       (fn [_]
         (let [base (public-base-url config)
               current-path (or (some-> req (aget "raw") (aget "url")) "/api/mcp/oauth/authorize")
               login-url (js/URL. "/api/auth/login" base)]
           (.set (.-searchParams login-url) "redirect" current-path)
           (.redirect reply (.toString login-url) 302)
           (js/Promise.resolve nil))))))

(defn- apply-guard
  [ctx guard]
  (case guard
    :redis
    (js/Promise.resolve
     (when-let [client (require-redis! (:reply ctx))]
       (assoc ctx :redis client)))

    :browser-auth
    (-> (require-browser-auth-context! (:policy-db ctx) (:runtime ctx) (:config ctx) (:req ctx) (:reply ctx))
        (.then (fn [auth-ctx]
                 (when auth-ctx
                   (assoc ctx :auth-context auth-ctx)))))

    :bearer-token
    (let [token (bearer-token (:req ctx))]
      (if (str/blank? token)
        (do
          (challenge-unauthorized! (:reply ctx) (:base ctx))
          (js/Promise.resolve nil))
        (js/Promise.resolve (assoc ctx :bearer-token token))))

    (js/Promise.resolve ctx)))

(defn- apply-guards
  [ctx guards]
  (reduce (fn [p guard]
            (-> p
                (.then (fn [ctx*]
                         (if ctx*
                           (apply-guard ctx* guard)
                           (js/Promise.resolve nil))))))
          (js/Promise.resolve ctx)
          (or guards [])))

(defn- make-route-runner
  [deps]
  (fn [guards handler req reply]
    (-> (apply-guards (assoc deps :req req :reply reply) guards)
        (.then (fn [ctx]
                 (when ctx
                   (as-promise (handler ctx)))))
        (.catch (fn [err]
                  (handle-route-error! reply err))))))

(defn- parse-register-client-body
  [req]
  (let [body (or (aget req "body") #js {})
        value {:redirect-uris (if (array? (aget body "redirect_uris"))
                                (mapv str (array-seq (aget body "redirect_uris")))
                                [])
               :client-name (some-> (aget body "client_name") str)}
        parsed (validate! RegisterClientBody value
                          {:status 400
                           :error "invalid_client_metadata"
                           :detail "redirect_uris is required"})]
    (when (empty? (:redirect-uris parsed))
      (throw (http-error 400 "invalid_client_metadata" "redirect_uris is required")))
    parsed))

(defn- parse-authorize-query
  [req]
  (let [q (or (aget req "query") #js {})
        value {:client-id (str (or (aget q "client_id") ""))
               :redirect-uri (str (or (aget q "redirect_uri") ""))
               :state (when-let [s (aget q "state")] (str s))
               :code-challenge (str (or (aget q "code_challenge") ""))
               :code-challenge-method (str (or (aget q "code_challenge_method") "S256"))
               :scope (when-let [scope (aget q "scope")] (str scope))}]
    (validate! AuthorizeQuery value {:status 400 :error "invalid_request"})))

(defn- parse-authorize-confirm-query
  [req]
  (let [q (or (aget req "query") #js {})
        value {:client-id (str (or (aget q "client_id") ""))
               :redirect-uri (str (or (aget q "redirect_uri") ""))
               :state (when-let [s (aget q "state")] (str s))
               :code-challenge (str (or (aget q "code_challenge") ""))
               :code-challenge-method (str (or (aget q "code_challenge_method") "S256"))
               :scope (when-let [scope (aget q "scope")] (str scope))
               :selected-tools (normalize-tool-selection (aget q "tool"))}]
    (validate! AuthorizeConfirmQuery value {:status 400 :error "invalid_request"})))

(defn- parse-token-exchange-body
  [req]
  (let [body (or (aget req "body") #js {})
        value {:grant-type (str (or (aget body "grant_type")
                                    (aget body "grantType")
                                    ""))
               :code (str (or (aget body "code") ""))
               :code-verifier (str (or (aget body "code_verifier")
                                       (aget body "codeVerifier")
                                       ""))
               :client-id (str (or (aget body "client_id")
                                   (aget body "clientId")
                                   ""))
               :redirect-uri (str (or (aget body "redirect_uri")
                                      (aget body "redirectUri")
                                      ""))}]
    (validate! TokenExchangeBody value {:status 400 :error "invalid_request"})))

(defn- parse-revoke-token-params
  [req]
  (let [params (or (aget req "params") #js {})
        value {:token-id (str (or (aget params "tokenId") ""))}]
    (validate! RevokeTokenParams value {:status 400 :error "invalid_request"})))

(defn- ensure-oauth-request!
  [{:keys [client-id redirect-uri code-challenge code-challenge-method]}]
  (when (or (str/blank? client-id)
            (str/blank? redirect-uri)
            (str/blank? code-challenge)
            (not= code-challenge-method "S256"))
    (throw (http-error 400
                       "invalid_request"
                       "Missing required OAuth parameters (client_id, redirect_uri, code_challenge, S256)"))))

(defn- ensure-oauth-confirm-request!
  [{:keys [client-id redirect-uri code-challenge code-challenge-method]}]
  (when (or (str/blank? client-id)
            (str/blank? redirect-uri)
            (str/blank? code-challenge)
            (not= code-challenge-method "S256"))
    (throw (http-error 400
                       "invalid_request"
                       "Missing required OAuth parameters"))))

(defn- get-registered-client
  [redis-client client-id]
  (if (str/blank? (str client-id))
    (js/Promise.resolve nil)
    (-> (redis-get! redis-client (str "knoxx:mcp:client:" client-id))
        (.then (fn [raw]
                 (when raw
                   (try
                     (js/JSON.parse raw)
                     (catch :default _ nil)))))
        (.catch (fn [_] nil)))))

(defn- redirect-uri-allowed?
  [client redirect-uri]
  (if-not client
    true
    (let [uris (or (aget client "redirect_uris") #js [])]
      (boolean (.includes (js/Array.from uris) redirect-uri)))))

(defn- ensure-redirect-uri-allowed!
  [client redirect-uri error-code]
  (when (and client (not (redirect-uri-allowed? client redirect-uri)))
    (throw (http-error 400 error-code "redirect_uri not allowed for registered client"))))

(defn- available-tools
  [runtime config auth-context]
  (or (mcp-expose/create-knoxx-custom-tools-js runtime config auth-context) #js []))

(defn- tool-name-set
  [tools]
  (into #{}
        (keep (fn [tool]
                (some-> (aget tool "name") str str/trim not-empty)))
        (array-seq tools)))

(defn- selected-tools-from-scope
  [tools requested-scope]
  (let [requested (into #{}
                        (comp (map str/trim)
                              (remove str/blank?))
                        (str/split (str (or requested-scope "")) #"\\s+"))]
    (into #{}
          (keep (fn [tool]
                  (let [name (some-> (aget tool "name") str str/trim not-empty)]
                    (when (and name
                               (or (contains? requested "all")
                                   (contains? requested name)))
                      name))))
          (array-seq tools))))

(defn- default-selected-tools
  [tool-names]
  (let [defaults ["semantic_query"
                  "semantic_read"
                  "memory_search"
                  "memory_session"
                  "graph_query"
                  "websearch"
                  "read"]]
    (into #{}
          (filter (fn [tool-id] (contains? tool-names tool-id)))
          defaults)))

(defn- requested-tools
  [runtime config auth-context selected-tools]
  (let [tools (available-tools runtime config auth-context)
        available (tool-name-set tools)]
    (->> selected-tools
         (map (comp str/trim str))
         (remove str/blank?)
         distinct
         (filter (fn [tool-id] (contains? available tool-id)))
         vec)))

(defn- tool-checkbox-html
  [tools selected]
  (->> (array-seq tools)
       (map (fn [tool]
              (let [name (str (or (aget tool "name") ""))
                    label (str (or (aget tool "label")
                                   (aget tool "name")
                                   (aget tool "description")
                                   name))
                    desc (str (or (aget tool "description") ""))
                    checked (if (contains? selected name) "checked" "")]
                (str "\n        <label style=\"display:block; margin: 6px 0;\">\n"
                     "          <input type=\"checkbox\" name=\"tool\" value=\"" (safe name) "\" " checked " />\n"
                     "          <span style=\"font-weight:600;\">" (safe label) "</span>\n"
                     "          <span style=\"color:#666;\">(" (safe name) ")</span>\n"
                     "          <div style=\"color:#444; margin-left: 22px;\">" (safe desc) "</div>\n"
                     "        </label>\n"))))
       (str/join "\n")))

(defn- authorization-consent-html
  [{:keys [base auth-context client-id redirect-uri state code-challenge requested-scope tools selected]}]
  (let [confirm-url (js/URL. "/api/mcp/oauth/authorize/confirm" base)
        user-email (str (or (aget auth-context "user" "email")
                            (aget auth-context "userEmail")
                            ""))
        org-slug (str (or (aget auth-context "org" "slug")
                          (aget auth-context "orgSlug")
                          ""))]
    (.set (.-searchParams confirm-url) "client_id" client-id)
    (.set (.-searchParams confirm-url) "redirect_uri" redirect-uri)
    (when state (.set (.-searchParams confirm-url) "state" state))
    (.set (.-searchParams confirm-url) "code_challenge" code-challenge)
    (.set (.-searchParams confirm-url) "code_challenge_method" "S256")
    (when-not (str/blank? (str (or requested-scope "")))
      (.set (.-searchParams confirm-url) "scope" requested-scope))
    (str "<!doctype html>\n"
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
         "<div><strong>User:</strong> " (safe user-email) "</div>\n"
         "<div><strong>Org:</strong> " (safe org-slug) "</div>\n"
         "</div>\n"
         "<form method=\"GET\" action=\"" (safe (.-pathname confirm-url)) "\">\n"
         "<input type=\"hidden\" name=\"client_id\" value=\"" (safe client-id) "\" />\n"
         "<input type=\"hidden\" name=\"redirect_uri\" value=\"" (safe redirect-uri) "\" />\n"
         "<input type=\"hidden\" name=\"state\" value=\"" (safe (or state "")) "\" />\n"
         "<input type=\"hidden\" name=\"code_challenge\" value=\"" (safe code-challenge) "\" />\n"
         "<input type=\"hidden\" name=\"code_challenge_method\" value=\"S256\" />\n"
         "<input type=\"hidden\" name=\"scope\" value=\"" (safe requested-scope) "\" />\n"
         "<h2>Capabilities</h2>\n"
         "<p>Select exactly which Knoxx tools this client can call. You can always revoke tokens later.</p>\n"
         "<div class=\"tools\">\n"
         (tool-checkbox-html tools selected)
         "</div>\n"
         "<div class=\"actions\">\n"
         "<button type=\"submit\">Authorize</button>\n"
         "<a href=\"/\">Cancel</a>\n"
         "</div></form></div></body></html>")))

(defn- load-token-record!
  [redis-client access-token]
  (if (str/blank? (str access-token))
    (js/Promise.resolve nil)
    (-> (redis-get! redis-client (str "knoxx:mcp:token:" access-token))
        (.then (fn [raw]
                 (when raw
                   (try
                     (js/JSON.parse raw)
                     (catch :default _ nil)))))
        (.catch (fn [_] nil)))))

(defn- resolve-token-context!
  [policy-db token-record]
  (let [headers-like #js {}
        resolver (aget policy-db "resolveRequestContext")]
    (when-let [membership-id (aget token-record "membershipId")]
      (aset headers-like "x-knoxx-membership-id" membership-id))
    (when-let [user-email (aget token-record "userEmail")]
      (aset headers-like "x-knoxx-user-email" user-email))
    (when-let [org-slug (aget token-record "orgSlug")]
      (aset headers-like "x-knoxx-org-slug" org-slug))
    (.call resolver policy-db headers-like)))

(defn- apply-zod-description
  [^js schema-node ^js schema-json]
  (let [description (some-> (aget schema-json "description") str str/trim not-empty)]
    (if description
      (.describe schema-node description)
      schema-node)))

(defn- typebox->zod-node
  [^js z ^js schema-json]
  (let [schema-type (aget schema-json "type")
        node (case schema-type
               "string" (.string z)
               "number" (.number z)
               "integer" (-> (.number z) (.int))
               "boolean" (.boolean z)
               "array" (let [item-schema (or (typebox->zod-node z (aget schema-json "items"))
                                              (.any z))]
                         (.array z item-schema))
               "object" (or (typebox->zod-shape z schema-json)
                             (.object z #js {}))
               (.any z))]
    (-> node
        (apply-zod-description schema-json)
        ((fn [n]
           (if-let [minimum (aget schema-json "minimum")]
             (.min n minimum)
             n)))
        ((fn [n]
           (if-let [maximum (aget schema-json "maximum")]
             (.max n maximum)
             n))))))

(defn- typebox->zod-shape
  [^js z ^js schema-json]
  (let [properties (or (aget schema-json "properties") #js {})
        required-set (into #{} (map str) (array-seq (or (aget schema-json "required") #js [])))
        entries (.entries js/Object properties)]
    (when (seq (array-seq entries))
      (reduce (fn [shape entry]
                (let [field-name (aget entry 0)
                      field-schema-json (aget entry 1)
                      field-schema (typebox->zod-node z field-schema-json)
                      final-schema (if (contains? required-set (str field-name))
                                     field-schema
                                     (.optional field-schema))]
                  (aset shape field-name final-schema)
                  shape))
              (js-obj)
              (array-seq entries)))))

(defn- discovery-metadata!
  [{:keys [reply base]}]
  (let [issuer (js/URL. (.toString base))]
    (.send reply
           #js {:issuer (-> (.toString issuer)
                            (.replace (js/RegExp. "/$") ""))
                :authorization_endpoint (.toString (js/URL. "/api/mcp/oauth/authorize" issuer))
                :token_endpoint (.toString (js/URL. "/api/mcp/oauth/token" issuer))
                :registration_endpoint (.toString (js/URL. "/api/mcp/oauth/register" issuer))
                :response_types_supported #js ["code"]
                :grant_types_supported #js ["authorization_code"]
                :code_challenge_methods_supported #js ["S256"]
                :token_endpoint_auth_methods_supported #js ["none"]})))

(defn- protected-resource-metadata!
  [{:keys [reply base]}]
  (let [issuer (-> (.toString (js/URL. (.toString base)))
                   (.replace (js/RegExp. "/$") ""))]
    (.send reply
           #js {:resource (.toString (js/URL. "/mcp" base))
                :authorization_servers #js [issuer]
                :scopes_supported #js ["mcp:tools"]
                :bearer_methods_supported #js ["header"]})))

(defn- register-client!
  [{:keys [req reply redis crypto]}]
  (let [{:keys [redirect-uris client-name]} (parse-register-client-body req)
        client-id (.randomUUID crypto)
        client #js {:client_id client-id
                    :client_name (or client-name "mcp-client")
                    :redirect_uris (clj->js redirect-uris)
                    :token_endpoint_auth_method "none"
                    :grant_types #js ["authorization_code"]
                    :response_types #js ["code"]
                    :created_at (.toISOString (js/Date.))}]
    (-> (redis-set! redis (str "knoxx:mcp:client:" client-id) (js/JSON.stringify client) js/undefined)
        (.then (fn [_]
                 (-> (.code reply 201)
                     (.send client))))
        (.catch (fn [err]
                  (throw (http-error 500 "registration_failed" (or (.-message err) (str err)))))))))

(defn- authorize-client!
  [{:keys [req reply redis runtime config auth-context base]}]
  (let [{:keys [client-id redirect-uri state code-challenge scope] :as params} (parse-authorize-query req)]
    (ensure-oauth-request! params)
    (-> (get-registered-client redis client-id)
        (.then (fn [client]
                 (ensure-redirect-uri-allowed! client redirect-uri "invalid_request")
                  (let [tools (available-tools runtime config auth-context)
                        tool-names (tool-name-set tools)
                        selected (let [explicit (selected-tools-from-scope tools scope)]
                                   (if (seq explicit)
                                     explicit
                                     (default-selected-tools tool-names)))
                        html (authorization-consent-html {:base base
                                                          :auth-context auth-context
                                                          :client-id client-id
                                                          :redirect-uri redirect-uri
                                                          :state state
                                                          :code-challenge code-challenge
                                                          :requested-scope (or scope "")
                                                          :tools tools
                                                          :selected selected})]
                    (let [reply* (reply-header! reply "content-type" "text/html; charset=utf-8")]
                      (.send reply* html))))))))

(defn- authorize-confirm!
  [{:keys [req reply redis runtime config auth-context crypto code-ttl]}]
  (let [{:keys [client-id redirect-uri state code-challenge selected-tools] :as params}
        (parse-authorize-confirm-query req)]
    (ensure-oauth-confirm-request! params)
    (-> (get-registered-client redis client-id)
        (.then (fn [client]
                 (ensure-redirect-uri-allowed! client redirect-uri "invalid_request")
                 (let [requested (requested-tools runtime config auth-context selected-tools)]
                   (when (empty? requested)
                     (throw (http-error 400 "invalid_scope" "No valid tools selected")))
                   (let [membership-id (str (or (aget auth-context "membership" "id")
                                               (aget auth-context "membershipId")
                                               ""))
                         user-email (str (or (aget auth-context "user" "email")
                                             (aget auth-context "userEmail")
                                             ""))
                         org-slug (str (or (aget auth-context "org" "slug")
                                           (aget auth-context "orgSlug")
                                           ""))
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
                                      :createdAt (.toISOString (js/Date.))}]
                      (-> (redis-set! redis code-key (js/JSON.stringify payload) #js {:EX code-ttl})
                          (.then (fn [_]
                                   (let [redirect (js/URL. redirect-uri)]
                                     (.set (.-searchParams redirect) "code" code)
                                    (when state
                                      (.set (.-searchParams redirect) "state" state))
                                    (.redirect reply (.toString redirect) 302))))))))))))

(defn- persist-access-token!
  [redis crypto token-ttl client-id record]
  (let [access-token (.randomUUID crypto)
        token-key (str "knoxx:mcp:token:" access-token)
        token-value #js {:accessToken access-token
                         :clientId client-id
                         :membershipId (aget record "membershipId")
                         :userEmail (aget record "userEmail")
                         :orgSlug (aget record "orgSlug")
                         :tools (aget record "tools")
                         :createdAt (.toISOString (js/Date.))
                         :expiresAt (.toISOString (js/Date. (+ (.now js/Date)
                                                               (* token-ttl 1000))))}]
    (-> (redis-set! redis token-key (js/JSON.stringify token-value) #js {:EX token-ttl})
        (.then (fn [_]
                 (if-let [membership-id (aget record "membershipId")]
                   (redis-sadd! redis (str "knoxx:mcp:user:" membership-id ":tokens") access-token)
                   (js/Promise.resolve nil))))
        (.then (fn [_]
                 #js {:access_token access-token
                      :token_type "Bearer"
                      :scope (->> (array-seq (or (aget record "tools") #js []))
                                  (str/join " "))
                      :expires_in token-ttl})))))

(defn- exchange-token!
  [{:keys [req reply redis crypto token-ttl]}]
  (let [{:keys [grant-type code code-verifier client-id redirect-uri]} (parse-token-exchange-body req)]
    (when (or (not= grant-type "authorization_code")
              (str/blank? code)
              (str/blank? code-verifier)
              (str/blank? client-id)
              (str/blank? redirect-uri))
      (throw (http-error 400 "invalid_request" "Missing required token exchange parameters")))
    (-> (get-registered-client redis client-id)
        (.then (fn [client]
                 (ensure-redirect-uri-allowed! client redirect-uri "invalid_grant")
                 (redis-get! redis (str "knoxx:mcp:code:" code))))
        (.then (fn [raw]
                 (when-not raw
                   (throw (http-error 400 "invalid_grant" "Unknown or expired code")))
                 (let [record (js/JSON.parse raw)
                       expected (str (or (aget record "codeChallenge") ""))
                       actual (pkce-challenge crypto code-verifier)]
                   (when (or (not= (aget record "clientId") client-id)
                             (not= (aget record "redirectUri") redirect-uri))
                     (throw (http-error 400 "invalid_grant" "Client/redirect mismatch")))
                   (when (or (str/blank? expected)
                             (not= expected actual))
                     (throw (http-error 400 "invalid_grant" "PKCE verification failed")))
                   (-> (redis-del! redis (str "knoxx:mcp:code:" code))
                       (.then (fn [_]
                                (persist-access-token! redis crypto token-ttl client-id record)))))))
        (.then (fn [token-response]
                 (.send reply token-response))))))

(defn- list-user-tokens!
  [{:keys [reply redis auth-context]}]
  (let [membership-id (str (or (aget auth-context "membership" "id")
                               (aget auth-context "membershipId")
                               ""))]
    (when (str/blank? membership-id)
      (throw (http-error 400 "missing_membership" "No membership available for this session")))
    (-> (redis-smembers! redis (str "knoxx:mcp:user:" membership-id ":tokens"))
        (.then (fn [token-ids]
                 (js/Promise.all
                  (clj->js
                   (for [token-id (array-seq token-ids)]
                     (-> (redis-get! redis (str "knoxx:mcp:token:" token-id))
                         (.then (fn [raw]
                                  (when raw
                                    (try
                                      (js/JSON.parse raw)
                                      (catch :default _ nil)))))))))))
        (.then (fn [records]
                 (let [tokens (->> (array-seq records)
                                   (remove nil?)
                                   into-array)]
                   (.send reply #js {:ok true :tokens tokens})))))))

(defn- revoke-user-token!
  [{:keys [req reply redis auth-context]}]
  (let [{:keys [token-id]} (parse-revoke-token-params req)
        membership-id (str (or (aget auth-context "membership" "id")
                               (aget auth-context "membershipId")
                               ""))]
    (when (or (str/blank? membership-id)
              (str/blank? token-id))
      (throw (http-error 400 "invalid_request" "membership and tokenId are required")))
    (-> (redis-del! redis (str "knoxx:mcp:token:" token-id))
        (.then (fn [_]
                 (redis-srem! redis (str "knoxx:mcp:user:" membership-id ":tokens") token-id)))
        (.then (fn [_]
                 (.send reply #js {:ok true}))))))

(defn- handle-mcp-session!
  [{:keys [req reply bearer-token base]}]
  (let [session-id (resolve-session-id req)]
    (cond
      (str/blank? (str session-id))
      (text-send! reply 400 "Missing mcp-session-id")

      :else
      (let [{:keys [transport token]} (get @mcp-sessions* session-id)]
        (cond
          (nil? transport)
          (text-send! reply 404 (str "Invalid mcp-session-id: " session-id))

          (not= (str bearer-token) (str token))
          (challenge-unauthorized! reply base)

          :else
          (do
            (ensure-streamable-accept! req)
            (transport-handle-request! transport (aget req "raw") (aget reply "raw"))))))))

(defn- handle-mcp-post!
  [{:keys [req reply redis bearer-token policy-db runtime config crypto McpServer StreamableHTTPServerTransport isInitializeRequest z base]}]
  (let [session-id (resolve-session-id req)
        existing (when session-id (get @mcp-sessions* session-id))]
    (cond
      existing
      (if (not= (str bearer-token) (str (:token existing)))
        (challenge-unauthorized! reply base)
        (do
          (ensure-streamable-accept! req)
          (transport-handle-request! (:transport existing) (aget req "raw") (aget reply "raw") (aget req "body"))))

      (not (and isInitializeRequest (isInitializeRequest (aget req "body"))))
      (do
        (.code reply 400)
        (.send reply #js {:jsonrpc "2.0"
                          :error #js {:code -32000
                                      :message "Bad Request: Server not initialized"}
                          :id nil}))

      :else
      (let [init-promise
            (-> (load-token-record! redis bearer-token)
                (.then
                 (fn [token-record]
                   (if-not token-record
                     (challenge-unauthorized! reply base)
                     (-> (resolve-token-context! policy-db token-record)
                         (.then
                          (fn [token-ctx]
                            (let [all-tools (available-tools runtime config token-ctx)
                                  allowed (into #{}
                                                (map str)
                                                (array-seq (or (aget token-record "tools") #js [])))
                                  effective (->> (array-seq all-tools)
                                                 (filter (fn [tool]
                                                           (contains? allowed (str (aget tool "name")))))
                                                 into-array)
                                  server (new McpServer #js {:name "knoxx"
                                                             :version "0.1.0"})
                                  transport* (atom nil)
                                  transport (new StreamableHTTPServerTransport
                                                 #js {:sessionIdGenerator (fn [] (.randomUUID crypto))
                                                      :onsessioninitialized (fn [sid]
                                                                              (when-let [active-transport @transport*]
                                                                                (swap! mcp-sessions* assoc (str sid)
                                                                                       {:transport active-transport
                                                                                        :token (aget token-record "accessToken")})))})]
                              (reset! transport* transport)
                              (doseq [tool (array-seq effective)]
                                (let [tool-name (some-> (aget tool "name") str str/trim not-empty)
                                      input-schema (or (when z
                                                         (typebox->zod-shape z (or (aget tool "parameters") #js {})))
                                                       #js {})]
                                  (when tool-name
                                    (.registerTool server
                                                   tool-name
                                                   #js {:description (str (or (aget tool "description")
                                                                              (aget tool "label")
                                                                              tool-name))
                                                        :inputSchema input-schema}
                                                   (fn [params]
                                                     (tool-execute! tool params))))))
                              (set! (.-onclose transport)
                                    (fn []
                                      (when-let [sid (.-sessionId transport)]
                                        (swap! mcp-sessions* dissoc (str sid)))))
                              (-> (.connect server transport)
                                  (.then (fn [_]
                                           (ensure-streamable-accept! req)
                                           (transport-handle-request! transport
                                                                      (aget req "raw")
                                                                      (aget reply "raw")
                                                                      (aget req "body")))))))))))))]
        (-> init-promise
            (.catch (fn [err]
                      (.error js/console "[knoxx-mcp] initialize failed" err)
                      (json-send! reply 500 {:error "mcp_init_failed"
                                             :detail (or (.-message err) (str err))}))))))))

(defn register-mcp-http-routes!
  [app runtime config]
  (let [deps {:runtime runtime
              :config config
              :policy-db (aget runtime "policyDb")
              :crypto (aget runtime "crypto")
              :McpServer (aget runtime "McpServer")
              :StreamableHTTPServerTransport (aget runtime "StreamableHTTPServerTransport")
              :isInitializeRequest (aget runtime "isInitializeRequest")
              :z (aget runtime "z")
              :code-ttl (js/parseInt (env "KNOXX_MCP_CODE_TTL_SECONDS" "300") 10)
              :token-ttl (js/parseInt (env "KNOXX_MCP_TOKEN_TTL_SECONDS"
                                          (str (* 60 60 24 30)))
                                      10)
              :base (public-base-url config)}
        run! (make-route-runner deps)]

    (route! app "GET"    "/.well-known/oauth-authorization-server"    (partial run! nil             discovery-metadata!))
    (route! app "GET"    "/.well-known/oauth-protected-resource"       (partial run! nil             protected-resource-metadata!))
    (route! app "POST"   "/api/mcp/oauth/register"                    (partial run! [:redis]        register-client!))
    (route! app "GET"    "/api/mcp/oauth/authorize"                   (partial run! [:redis :browser-auth] authorize-client!))
    (route! app "GET"    "/api/mcp/oauth/authorize/confirm"           (partial run! [:redis :browser-auth] authorize-confirm!))
    (route! app "POST"   "/api/mcp/oauth/token"                      (partial run! [:redis]        exchange-token!))
    (route! app "GET"    "/api/mcp/tokens"                            (partial run! [:redis :browser-auth] list-user-tokens!))
    (route! app "DELETE" "/api/mcp/tokens/:tokenId"                  (partial run! [:redis :browser-auth] revoke-user-token!))
    (route! app "POST"   "/mcp"                                       (partial run! [:redis :bearer-token] handle-mcp-post!))
    (route! app "GET"    "/mcp"                                       (partial run! [:bearer-token] handle-mcp-session!))
    (route! app "DELETE" "/mcp"                                       (partial run! [:bearer-token] handle-mcp-session!))

    nil))

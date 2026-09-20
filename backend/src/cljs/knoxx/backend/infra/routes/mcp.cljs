(ns knoxx.backend.infra.routes.mcp
  "Serve Knoxx tools over MCP (Model Context Protocol) Streamable HTTP."
  (:require [clojure.string :as str]
            [knoxx.backend.shape.app-shapes :refer [route!]]
            [knoxx.backend.infra.actor.acting :as actor-acting]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.auth.method-config :as auth-methods]
            [knoxx.backend.infra.auth.session :as auth-session]
            [knoxx.backend.infra.identity :as identity]
            [knoxx.backend.infra.routes.mcp.params :as params]
            [knoxx.backend.infra.routes.mcp.transport :as transport]
            [knoxx.backend.extern.mcp-token :as mcp-token]
            [knoxx.backend.extern.mcp-oauth :as oauth]
            [knoxx.backend.extern.mcp-sdk :as sdk]
            [knoxx.backend.extern.fastify :as http]
            [knoxx.backend.infra.stores.mongo-mcp-oauth :as mongo-mcp]
            [knoxx.backend.law.mcp-oauth :as law]
            [knoxx.backend.runtime.state :as runtime-state]
            ["@modelcontextprotocol/sdk/server/mcp.js" :refer [McpServer]]
            ["@modelcontextprotocol/sdk/server/streamableHttp.js" :refer [StreamableHTTPServerTransport]]
            ["node:crypto" :as crypto]
            ["zod" :refer [z]])
  (:require-macros [knoxx.backend.macros :refer [defroute]]))


(def typebox->zod-node sdk/typebox->zod-node)
(def typebox->zod-shape sdk/typebox->zod-shape)

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

(defn- text-send! [reply status body]
  (-> (.code reply status) (.send body)))

(defn- ^:async browser-auth-ctx!
  [req policy-db config]
  (try
    (let [auth-ctx (await (auth-session/resolve-auth-context req policy-db))]
      (aset req "authContext" auth-ctx)
      nil)
    (catch :default error
      (if (and (= 401 (http/error-status error)) (= "GET" (aget req "method")))
        (let [base (public-base-url config)
              current-path (or (some-> req (aget "raw") (aget "url")) "/api/mcp/oauth/authorize")
              login-url (js/URL. "/login" base)]
          (.set (.-searchParams login-url) "redirect" current-path)
          {:redirect (.toString login-url)})
        (throw error)))))

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

(defn- resolve-token-context! [policy-context token-record]
  (identity/resolve-bearer-context! policy-context (aget token-record "accessToken")))

(defn- call-actor-id
  "The actor this request's tool calls run as, or nil when it has none.

   A token gets an actor only if it *carries* one. A token that carries none —
   every token minted before actors were carried — stays actor-less rather than
   inheriting whatever its membership resolves to now: the actor decides which
   Discord and Bluesky account a call posts from, and that token's consent
   screen never named one. Granting it the membership's actor would hand an
   already-issued credential a power its holder never agreed to, silently.

   When the token does carry one, the value used is the membership's current
   stored binding — not ctx-actor-id, whose role-derived default is never nil and
   would therefore let a cleared assignment keep matching — and
   law/token-actor-honourable? has already refused the case where the two
   disagree. So a reassignment is a refusal rather than a quiet switch, and
   there is no window in which a token acts as an actor its membership has
   dropped."
  [token-record token-ctx]
  (let [claimed (actor-acting/normalize-actor-id (aget token-record "actorId"))
        current (actor-acting/normalize-actor-id (authz/ctx-actor-binding token-ctx))]
    (when-not (law/token-actor-honourable? claimed current)
      (throw (params/http-error 403 "actor_reassigned"
                         (str "This token was authorized to act as " claimed
                              ", which is no longer this membership's actor."))))
    (when claimed current)))

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

(defn- granted-tools
  "The tools this token was granted, out of those its context can reach.

   The intersection is the whole authorization: a token carries the names ticked
   on the consent page, and a tool absent from either side is not registered.
   One consequence worth knowing — a token whose grant list is empty registers
   nothing, so initialize advertises no tool capability and tools/list answers
   \"Method not found\". That is correct, and it reads like a broken server."
  [runtime config token-ctx ^js token-record]
  (let [allowed (into #{} (map str) (array-seq (or (aget token-record "tools") (js/Array.))))]
    (->> (oauth/available-tools runtime config token-ctx)
         array-seq
         (filter (fn [^js t] (contains? allowed (str (aget t "name")))))
         into-array)))

(defn- ^:async serve-mcp-post!
  "Answer one authenticated MCP POST on a per-request server and transport."
  [{:keys [config runtime policy-db McpServer StreamableHTTPServerTransport z
           request raw-req raw-res token-record]}]
  (let [token-ctx (await (resolve-token-context! policy-db token-record))
        acting    {:actor-id (call-actor-id token-record token-ctx)
                   ;; From the resolved context, so a credential lookup names
                   ;; this token's owner exactly rather than searching by actor.
                   :org-id        (authz/ctx-org-id token-ctx)
                   :membership-id (authz/ctx-membership-id token-ctx)}
        server    (new McpServer (clj->js {:name "knoxx" :version "0.1.0"}))
        transport (new StreamableHTTPServerTransport (transport/stateless-transport-options))]
    (sdk/register-tools! server z (granted-tools runtime config token-ctx token-record) acting)
    (await (.connect server transport))
    (sdk/close-when-response-ends! raw-res server)
    (transport/ensure-streamable-accept! request)
    (transport/handle-request! transport raw-req raw-res (aget request "body"))))

(defn- ^:async resolve-post-token-record!
  "Build an ephemeral MCP record only from current Axxium-backed authority."
  [request runtime config bearer]
  (when (and (not (str/blank? bearer))
             (auth-methods/method-enabled? config auth-methods/mcp-surface :oauth-bearer))
    (let [policy-context (runtime-state/current-policy-db)
          ctx (await (identity/resolve-request! policy-context request))
          principal (:axxium-principal ctx)
          stored (mcp-token/stored-record (await (mongo-mcp/get-token! bearer)))
          tool-names (oauth/tool-name-set (oauth/available-tools runtime config ctx))
          grant-tools (if stored (filterv tool-names (:tools stored)) (vec (sort tool-names)))]
      (mcp-token/native-record
       (cond-> {:accessToken bearer :clientId (or (:clientId stored) "axxium-session")
                :axxiumPrincipalId (:principal/id principal) :axxiumEntityId (:principal/entity-id principal)
                :membershipId (authz/ctx-membership-id ctx) :tools grant-tools}
         (seq (authz/ctx-user-email ctx)) (assoc :userEmail (authz/ctx-user-email ctx))
         (seq (authz/ctx-org-slug ctx)) (assoc :orgSlug (authz/ctx-org-slug ctx))
         (seq (authz/ctx-actor-binding ctx)) (assoc :actorId (authz/ctx-actor-binding ctx)))))))

(defroute mcp-handle-post! [base config runtime code-ttl token-ttl policy-db McpServer StreamableHTTPServerTransport z] "POST" "/mcp" []
  (let [^js request request
        ^js reply reply]
    (.hijack reply)
    (let [^js raw-req (aget request "raw")
          ^js raw-res (aget reply "raw")
          bearer      (transport/bearer-token request)]
      (try
        (let [token-record (await (resolve-post-token-record! request runtime config bearer))]
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
            (let [status (http/error-status err)]
              (.writeHead raw-res status (clj->js {"Content-Type" "application/json"}))
              (.end raw-res (js/JSON.stringify (clj->js {:error (or (http/error-code err) "mcp_post_failed")
                                                         :detail (or (.-message err) (str err))}))))))))))

(def ^:private route-registrars
  (into oauth/route-registrars
        [mcp-handle-post! mcp-handle-session! mcp-handle-delete-session!]))

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
    (auth-methods/announce! config auth-methods/mcp-surface)
    (doseq [register! route-registrars]
      (register! app runtime config deps))))

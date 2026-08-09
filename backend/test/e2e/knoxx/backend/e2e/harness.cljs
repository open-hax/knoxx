(ns knoxx.backend.e2e.harness
  "Boot a real Knoxx HTTP server in-process, for tests that must cross the wire.

   Everything here is a *substitution at a seam the production code already
   has*, never a reimplementation. The Fastify app, the route registration, the
   MCP transport and the tool factories are the shipped ones; what is supplied
   is the policy context they resolve through, the contract root they read, and
   the fetch they reach the network with.

   That distinction is the whole value. A harness that rebuilt the serving path
   would pass while production failed — which is exactly the failure the MCP
   tool surface had, where the only way to learn whether a tool worked was to
   ask a deployed connector to try it."
  (:require [clojure.string :as str]
            [knoxx.backend.contract-runtime-deps :as contract-runtime-deps]
            [knoxx.backend.domain.models :as runtime-models]
            [knoxx.backend.infra.config :as runtime-config]
            [knoxx.backend.infra.http-server :as http-server]
            [knoxx.backend.infra.routes.mcp :as mcp-routes]
            [knoxx.backend.runtime.state :as runtime-state]
            ["node:child_process" :refer [execFile]]
            ["node:path" :as path]
            ["node:util" :refer [promisify]]))

(def ^:private exec-file-async (promisify execFile))

(def discord-bot-token
  "The bot token the seeded Discord credential carries.

   Named rather than inlined because the identity assertions are about seeing
   exactly this value leave the process — on a REST Authorization header, or
   handed to the gateway manager's start."
  "e2e-discord-bot-token")

(def loopback-token
  "The shared secret the e2e authentication contract names.

   Long enough to clear the contract's :auth-method/min-token-length, and
   fixed rather than random so a failing run can be reproduced by hand with
   curl against the same contract."
  "e2e-loopback-token")

(defn contracts-dir
  "Absolute path to the e2e contract root.

   Resolved from process.cwd, which shadow's :node-test target sets to the
   backend package root."
  []
  (.resolve path (.cwd js/process) "test/e2e/fixtures/contracts"))

;; ── the seeded principal ────────────────────────────────────────────────────

(def system-admin-context
  "A resolved auth context for the seeded system-admin membership.

   system_admin makes ctx-tool-allowed? true for everything, which is what a
   tool-surface sweep wants: the catalog under test is the whole catalog, and
   a tool missing from it is a real absence rather than a policy denial."
  {:org-id "e2e-org"
   :org-slug "open-hax"
   :user-id "e2e-user"
   :user-email "system-admin@open-hax.local"
   :membership-id "e2e-membership"
   :actor-binding "e2e_actor"
   :role-slugs ["system_admin"]
   :permissions ["agent.chat.use"]
   :tool-policies []})

(defn context-with-tool-policies
  "The seeded context demoted out of system-admin, allowing only `tool-ids`.

   Used to prove that a grant is not an authorization: the same loopback token
   that reaches every tool under system-admin reaches only these once the
   membership's policies are the thing deciding."
  [tool-ids]
  (-> system-admin-context
      (assoc :role-slugs ["knowledge_worker"])
      (assoc :tool-policies (mapv (fn [id] {:tool-id id :effect "allow"}) tool-ids))))

(defn- credential
  "One credential row in the camelCase shape policy/get-actor-credential!
   returns — the same shape mongo-policy-actor-credentials/credential-row->response
   produces. Written through this helper rather than by hand so a stub cannot
   drift into a shape the real store never emits, which is the way a seeded
   credential silently stops proving anything."
  [provider kind account-identifier secrets]
  {:credential {:id (str "e2e-" provider)
                :actorId "e2e_actor"
                :userId "e2e-user"
                :orgId "e2e-org"
                :orgSlug "open-hax"
                :provider provider
                :kind kind
                :accountIdentifier account-identifier
                :status "active"
                :secretJson secrets}})

(def seeded-credentials
  "Actor credentials the harness resolves, keyed by [actor-id provider].

   This is the seam that makes credential-backed tools reachable at all —
   Discord and Bluesky are most of the tool surface, and every one of them was
   untestable while the lookup went straight to Mongo.

   Provider strings are the ones the tools ask for, which are not always the
   platform's name: Discord asks for \"discord_bot\"."
  {["e2e_actor" "bluesky"]
   (credential "bluesky" "app-password" "e2e.test"
               {:identifier "e2e.test" :appPassword "e2e-app-password"})

   ["e2e_actor" "discord_bot"]
   (credential "discord_bot" "bot-token" "e2e-bot"
               ;; botToken, the key discord-token! names first and the one its
               ;; error message tells an operator to configure. Seeding the
               ;; :token alias instead would pass while proving the alias.
               {:botToken discord-bot-token})})

(defn ^:async docker-available?
  "True when a docker CLI on this machine can talk to a daemon.

   The sandbox tools shell out to docker, so without one they cannot be
   exercised at all. Probed rather than assumed: this is true on the deploy
   host, usually true on a developer machine, and often false in CI."
  []
  (try
    (await (exec-file-async "docker" #js ["info" "--format" "{{.ServerVersion}}"]
                            #js {:timeout 15000}))
    true
    (catch :default _ false)))

(defn require-docker?
  "Whether an absent docker should fail the suite rather than skip it.

   Set KNOXX_E2E_REQUIRE_DOCKER=true where docker is guaranteed — the deploy
   host — so a silently skipped sandbox test cannot be mistaken for a passing
   one."
  []
  (= "true" (some-> (aget js/process.env "KNOXX_E2E_REQUIRE_DOCKER") str str/lower-case)))

(defn policy-context
  "A policy context backed by the seeded principal instead of a database.

   Only the two dispatch seams production already consults are supplied, so a
   call that reaches any other policy function fails loudly rather than
   silently taking a Mongo path in a test.

   `credentials` defaults to the seeded map. Pass {} to model an actor that
   exists and owns nothing — the case that separates a tool resolving *this*
   actor's credential from one falling back to a process-wide token.

   Note what cannot be varied here: the context's :actor-binding must keep
   matching the grant in the e2e authentication contract. law/token-actor-honourable?
   refuses a token whose claimed actor is no longer the membership's, which is
   a guard doing its job — so a test that wants a different identity changes
   the credentials, not the binding."
  ([] (policy-context system-admin-context))
  ([auth-context] (policy-context auth-context seeded-credentials))
  ([auth-context credentials]
   {:resolve-context!
    (fn [_headers-like] (js/Promise.resolve auth-context))

    :get-actor-credential!
    (fn [actor-id provider _scope]
      (js/Promise.resolve
       (get credentials [(str actor-id) (str provider)] {:credential nil})))}))

;; ── the outbound network ────────────────────────────────────────────────────

(defonce ^:private captured-requests* (atom []))

(defn captured-requests
  "Every outbound request the stubbed fetch saw, oldest first."
  []
  @captured-requests*)

(defn captured-matching
  "Captured requests whose URL contains `fragment`."
  [fragment]
  (filterv #(str/includes? (:url %) fragment) (captured-requests)))

(defn- capture!
  [url init]
  (let [init (or init #js {})
        headers (or (aget init "headers") #js {})]
    (swap! captured-requests* conj
           {:url (str url)
            :method (str (or (aget init "method") "GET"))
            :headers (js->clj headers)
            :body (some-> (aget init "body") str)})))

(defn- stub-response
  "A genuine Response, not an object shaped like one.

   A hand-rolled stub with a two-method `headers` passed the first draft of
   this harness and then failed six tools with \"headers.forEach is not a
   function\" — a defect in the harness that reads exactly like a defect in the
   tools. Node's own Response gives real Headers and real body semantics, so
   what the sweep reports is the tool's behaviour and not the stub's."
  [body]
  (js/Promise.resolve
   (js/Response. (js/JSON.stringify (clj->js body))
                 (clj->js {:status 200
                           :headers {"content-type" "application/json"}}))))

(defn install-fetch-stub!
  "Replace global fetch with a recorder, and return the original.

   Every outbound call answers 200 with an empty JSON object. The assertion a
   test makes is about the request that was *attempted* — its URL, and whether
   the seeded credential reached it — not about a response body we would have
   invented anyway. Requests to the harness's own loopback server pass through
   to the real fetch so MCP calls still work."
  [base-url]
  (let [original (aget js/globalThis "fetch")]
    (aset js/globalThis "fetch"
          (fn [url init]
            (if (str/starts-with? (str url) base-url)
              (original url init)
              (do (capture! url init) (stub-response {})))))
    original))

(defn restore-fetch!
  [original]
  (aset js/globalThis "fetch" original)
  (reset! captured-requests* []))

;; ── the server ──────────────────────────────────────────────────────────────

(defn e2e-config
  "Config pointing at the e2e contract root, otherwise the ordinary one.

   Built through the same enrich/inject pipeline bootstrap uses, so a contract
   the runtime expects to have been injected is present here too."
  []
  (-> (runtime-config/cfg)
      (assoc :contracts-dir (contracts-dir))
      (assoc :host "127.0.0.1")
      (assoc :port 0)
      runtime-models/enrich-config
      contract-runtime-deps/inject-deps!))

(defn- server-base-url
  [^js app]
  (let [address (.address (.-server app))]
    (str "http://127.0.0.1:" (aget address "port"))))

(defn ^:async start!
  "Boot an MCP-serving Fastify app on an ephemeral port.

   Returns {:app :base-url :config :original-fetch}. Port 0 rather than a fixed
   one so concurrent runs, and a developer with a backend already up, do not
   collide."
  ([] (start! (policy-context)))
  ([policy-ctx]
   (aset js/process.env "KNOXX_E2E_LOOPBACK_TOKEN" loopback-token)
   (let [config  (e2e-config)
         runtime #js {}
         app     (http-server/create-app!)]
     (runtime-state/remember-context! runtime config policy-ctx)
     (http-server/ensure-json-empty-body-parser! app)
     (await (http-server/register-default-plugins! app))
     (mcp-routes/register-mcp-http-routes! app runtime config)
     (await (http-server/listen! app "127.0.0.1" 0))
     (let [base-url (server-base-url app)]
       {:app app
        :config config
        :base-url base-url
        :original-fetch (install-fetch-stub! base-url)}))))

(defn ^:async stop!
  [{:keys [app original-fetch]}]
  (restore-fetch! original-fetch)
  (js-delete js/process.env "KNOXX_E2E_LOOPBACK_TOKEN")
  (await (http-server/close! app)))

(defn client
  "An mcp-client config for a started harness, authenticated by default."
  ([started] (client started loopback-token))
  ([{:keys [base-url]} token]
   {:base-url base-url :token token}))

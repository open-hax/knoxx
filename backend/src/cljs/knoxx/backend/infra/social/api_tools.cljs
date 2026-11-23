(ns knoxx.backend.infra.social.api-tools
  "Public schema discovery and separately authorized actor-owned platform reads."
  (:require [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.domain.social.api-actions :as actions]
            [knoxx.backend.domain.social.api-catalog :as catalog]
            [knoxx.backend.domain.social.api-json-request :as json-request]
            [knoxx.backend.domain.social.api-request :as request]
            [knoxx.backend.domain.tools :as tools]
            [knoxx.backend.extern.platform-api-catalog :as asset]
            [knoxx.backend.extern.social-api-http :as http]
            [knoxx.backend.extern.social-api-codec :as codec]
            [knoxx.backend.extern.tools :as xtools]
            [knoxx.backend.infra.actor.credentials :as credentials]
            [knoxx.backend.infra.actor.acting :as acting]
            [knoxx.backend.infra.agent.social-delivery :as delivery]
            [knoxx.backend.infra.agent.tool-context :as tool-context]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.social.api-execution :as execution]
            [knoxx.backend.infra.social.bindings :as bindings]
            [knoxx.backend.law.platform-api :as law]
            [knoxx.backend.law.platform-api-request :as request-law]))

(defn- execute-discovery
  [schema interpret]
  (^:async fn [_runtime config _call-id params _a _b _c]
    (let [parameters (law/assert-valid! schema (xtools/parameter-map params))
          loaded (await (asset/read! (resources/resource-root-paths config)))
          result (assoc (interpret (:catalog loaded) parameters) :catalogSha256 (:sha256 loaded))
          rendered (asset/render result)]
      (codec/tool-result (:text rendered) (assoc result :renderTruncated (:truncated rendered)) false))))

(defn- read-authentication
  [plan credential]
  (if (= "discord" (:platform plan))
    {:token (credentials/secret-value credential :botToken :token)}
    {:identifier (or (credentials/secret-value credential :identifier) (:accountIdentifier credential))
     :password (credentials/secret-value credential :appPassword :password)
     :pds (or (credentials/secret-value credential :pds :serviceUrl) "https://bsky.social")
     :scope-key (str (credentials/current-actor-id) ":" (:id credential) ":" (:orgId credential))}))

(defn ^:async read-execute
  "Validate a pinned read before resolving the scoped actor credential; never accept request origins or headers."
  [runtime config _call-id params _a _b _c]
  (let [parameters (law/assert-valid! law/ReadParameters (xtools/parameter-map params))
        loaded (await (asset/read! (resources/resource-root-paths config)))
        plan (request/read-plan (:catalog loaded) parameters)
        credential (await (credentials/get-credential! runtime (:credential-provider plan)))
        result (await (http/read! plan (read-authentication plan credential)))
        details {:platform (:platform plan) :operation (:operation plan) :method "GET"
                 :status (:status result) :rateLimit (:rate-limit result)
                 :body (:body result) :redactedCredentialFields (:redacted-fields result)
                 :catalogSha256 (:sha256 loaded)
                 :evidence "Raw API read. Use native conversation tools for publication-context prerequisites."}
        rendered (asset/render details)]
    (codec/tool-result (:text rendered) (assoc details :renderTruncated (:truncated rendered)) (not (:ok result)))))

(defn- ^:async invoke-request!
  [runtime plan]
  (let [credential (try (await (credentials/get-credential! runtime (:credential-provider plan)))
                        (catch :default error
                          (throw (ex-info "Actor credential resolution failed before the platform operation"
                                          {:code "platform_actor_credential_missing" :phase :preflight} error))))]
    (await (http/execute-json! plan (read-authentication plan credential)))))

(defn- captured-context
  [actor-id]
  (let [context (tool-context/current)]
    (when (and (:platform-admission context) (= actor-id (get-in context [:agent-spec :actor-id])))
      {:context context :run-id (:run_id (delivery/captured-run!
                                         {:actor-id actor-id :conversation-id (:conversation-id context)} "API request"))})))

(defn- ^:async scoped-request!
  [runtime config input actor-id {:keys [context run-id]}]
  (let [loaded (await (asset/read! (resources/resource-root-paths config)))
        prepared (json-request/json-plan (:catalog loaded) input)
        actions (actions/planned-actions prepared actor-id)
        _ (when (seq actions) (bindings/ensure-write-allowed! config actor-id))
        result (await (execution/execute! context run-id actions #(invoke-request! runtime (:plan prepared))))
        response (:response result)
        details {:platform (:platform input) :operation (:operation input) :method (get-in prepared [:plan :method])
                 :status (:status response) :body (:body response) :emptyBody (:empty-body? response)
                 :rateLimit (:rate-limit response) :redactedCredentialFields (:redacted-fields response)
                 :catalogSha256 (:sha256 loaded) :effects (:effects prepared) :actionOutcomes (:outcomes result) :publications (:publications result)}
        rendered (codec/render details)
        failed? (or (not (:ok response)) (some #(contains? #{:partial :unknown} (:status %)) (:outcomes result)))]
    (codec/tool-result (:text rendered) (assoc details :renderTruncated (:truncated rendered)) failed?)))

(defn ^:async request-execute
  "Capture actor/run before the first await and preserve request-local identity
   across catalog loading, admission and credential lookup. No environment token."
  [runtime config _call-id params _a _b _c]
  (let [input (request-law/assert-valid! request-law/Input (codec/parameters params))
        current-actor (credentials/current-actor-id)
        actor-id (or (get-in (tool-context/current) [:agent-spec :actor-id]) current-actor)
        scope (assoc (when (= actor-id current-actor) (acting/current-lookup-scope)) :actor-id actor-id)
        captured (captured-context actor-id)]
    (when-not actor-id
      (throw (ex-info "A scoped acting identity is required for API execution" {:code "platform_actor_required" :phase :preflight})))
    (await (acting/run-as! scope #(scoped-request! runtime config input actor-id captured)))))

(defn create-tools
  "Expose discovery, reads and JSON execution under their separate capabilities."
  [runtime config auth-context]
  (into []
        (keep (fn [[id description schema execute]]
                (when (or (nil? auth-context) (authz/ctx-tool-allowed? auth-context id))
                  (tools/create-tool-obj
                   id id description description
                   ["Search by social intent, then inspect the exact operation and referenced fields."
                    "Discovery is public schema data; social.read executes only a schema-selected GET as the acting identity."
                    "Related native tools may support fewer fields than the full platform API."]
                   schema execute runtime config))))
        [["social.features" "Discover Discord and Bluesky features by intent, including polls, threads, lists, profiles, media and private conversations. Empty query returns feature families."
          law/FeaturesParameters (execute-discovery law/FeaturesParameters catalog/discover)]
         ["social.describe" "Inspect one exact platform operation or local schema definition: required fields, API path, encodings, scope and related native tools. Does not call the platform."
          law/DescribeParameters (execute-discovery law/DescribeParameters catalog/describe)]
         ["social.read" "Read any supported pinned Discord/Bluesky JSON GET operation by its discovered ID and named parameter values. Uses your actor's own credentials, handles path/query placement, and preserves pagination fields. Cannot publish, edit, delete, mint credentials, follow arbitrary URLs or export binary data."
          law/ReadParameters read-execute]
         ["social.request" "Execute a discovered Discord or Bluesky JSON operation as your actor, with named parameters and its JSON body. Writes require a live admitted turn and are scoped to publishing, reacting, reposting and following. Inspect social.describe first. Preserves API fields, uses platform permissions, and shares native publication evidence and attempt limits in an agent turn. Returns actual HTTP/body and per-action outcomes. Stop on unknown or partial outcomes; never retry by switching tool aliases. Credential/delegated-authentication, binary and streaming operations require their separate interfaces."
          request-law/InputSchema request-execute]]))

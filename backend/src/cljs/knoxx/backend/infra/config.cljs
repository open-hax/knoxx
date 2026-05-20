(ns knoxx.backend.infra.config
  (:require [clojure.string :as str]))

(defn- env-path-list
  [k]
  (let [raw (some-> (aget js/process.env k) str str/trim)]
    (if (str/blank? (or raw ""))
      []
      (->> (str/split raw #":")
           (map (fn [value]
                  (some-> value str str/trim not-empty)))
           (remove nil?)
           vec))))

(defn- env
  [k default]
  (or (aget js/process.env k) default))

(defn- env-int
  [k default]
  (let [raw (aget js/process.env k)
        parsed (js/parseInt (str (or raw "")) 10)]
    (if (js/Number.isFinite parsed)
      parsed
      default)))

(defn- env-kv-map
  [k]
  (let [raw (some-> (aget js/process.env k) str str/trim)]
    (if (str/blank? (or raw ""))
      {}
      (->> (str/split raw #",")
           (map (fn [entry]
                  (let [[left right] (str/split (str entry) #"=" 2)
                        key (some-> left str str/trim not-empty)
                        value (some-> right str str/trim not-empty)]
                    (when (and key value)
                      [key value]))))
           (remove nil?)
           (into {})))))

(defn cfg
  "Read Knoxx backend runtime configuration from environment variables.

   NOTE: This namespace is intentionally *env-only*.
   Any derived runtime state belongs elsewhere."
  []
  {:app-name (env "APP_NAME" "Knoxx Backend CLJS")
   :host (env "HOST" "0.0.0.0")
   :port (js/parseInt (env "PORT" "8000") 10)

   :workspace-root (env "WORKSPACE_ROOT" "/app/workspace/devel")
   :extra-workspace-roots (env-path-list "KNOXX_EXTRA_WORKSPACE_ROOTS")
   :music-library-root (let [value (some-> (aget js/process.env "KNOXX_MUSIC_LIBRARY_ROOT") str str/trim)]
                         (when-not (str/blank? (or value ""))
                           value))
   :project-name (env "WORKSPACE_PROJECT_NAME" "devel")
   :session-project-name (env "KNOXX_SESSION_PROJECT_NAME" "knoxx-session")
   :collection-name (env "KNOXX_COLLECTION_NAME" "devel_docs")

   :contracts-dir (env "CONTRACTS_DIR" "contracts")

   :proxx-base-url (env "PROXX_BASE_URL" "http://proxx:8789")
   :proxx-auth-token (env "PROXX_AUTH_TOKEN" "")
   :proxx-default-model (let [value (aget js/process.env "PROXX_DEFAULT_MODEL")]
                          (when (and (string? value) (not (str/blank? value)))
                            value))
   :proxx-embed-model (env "PROXX_EMBED_MODEL" "nomic-embed-text:latest")
   :provider-base-urls (env-kv-map "KNOXX_PROVIDER_BASE_URLS")
   :provider-auth-tokens (env-kv-map "KNOXX_PROVIDER_AUTH_TOKENS")
   :provider-auth-headers (env-kv-map "KNOXX_PROVIDER_AUTH_HEADERS")

   :knoxx-admin-url (env "KNOXX_ADMIN_URL" "http://localhost")
   :knoxx-base-url (env "KNOXX_BASE_URL" "http://localhost:8000")
   :knoxx-api-key (env "KNOXX_API_KEY" "")

   :openplanner-base-url (or (aget js/process.env "OPENPLANNER_BASE_URL")
                             (aget js/process.env "OPENPLANNER_URL")
                             "http://host.docker.internal:7777")
   :openplanner-api-key (env "OPENPLANNER_API_KEY" "")

   :ingestion-base-url (env "KMS_INGESTION_URL" "http://127.0.0.1:3003")


   :model-lab-openai-api-key (env "MODEL_LAB_OPENAI_API_KEY" "")

   :shibboleth-base-url (env "SHIBBOLETH_BASE_URL" "")
   :shibboleth-ui-url (env "SHIBBOLETH_UI_URL" "")

   :knoxx-default-role (env "KNOXX_DEFAULT_ROLE" "knowledge_worker")
   :knoxx-default-actor-id (env "KNOXX_DEFAULT_ACTOR_ID" "chat_primary")
   :knoxx-default-agent-contract (env "KNOXX_DEFAULT_AGENT_CONTRACT" "knoxx_default")

   :gmail-app-email (env "GMAIL_APP_EMAIL" "")
   :gmail-app-password (env "GMAIL_APP_PASSWORD" "")

   ;; Discord/Twitch/Bluesky tool credentials are actor-owned and are read
   ;; from the policy DB actor_credentials table, not process env vars.
   :discord-bot-token ""

   ;; Music services
   :audd-api-token (env "AUDD_API_TOKEN" "")
   :acoustid-api-key (env "ACOUSTID_API_KEY" "")

   ;; BlazeAPI multimodal generation is accessed through Proxx; Knoxx does not
   ;; read direct Blaze credentials.

   ;; Voice / speech
   :voxx-url (env "VOXX_URL" "http://127.0.0.1:8787")
   :voxx-api-key (env "VOICE_GATEWAY_API_KEY" "dev-token")
   :voxx-voice-id (env "KNOXX_VOXX_VOICE_ID" "af_jessica")
   :voxx-model-id (env "KNOXX_VOXX_MODEL_ID" "kokoro")
   :voxx-default-speed (env "KNOXX_VOXX_DEFAULT_SPEED" (env "VOICE_GATEWAY_TTS_DEFAULT_SPEED" "1.15"))
   :stt-base-url (env "KNOXX_STT_BASE_URL" "")

   ;; eta-mu agent runtime dir
   :agent-dir (env "KNOXX_AGENT_DIR" "/tmp/knoxx-agent")
   :agent-compaction-enabled? (not= "false" (str/lower-case (env "KNOXX_AGENT_COMPACTION_ENABLED" "true")))
   :agent-compaction-reserve-tokens (env-int "KNOXX_AGENT_COMPACTION_RESERVE_TOKENS" 16384)
   :agent-compaction-keep-recent-tokens (env-int "KNOXX_AGENT_COMPACTION_KEEP_RECENT_TOKENS" 20000)

   ;; Sandbox container runtime
   :sandbox-docker-bin (env "KNOXX_SANDBOX_DOCKER_BIN" "docker")
   :sandbox-image (env "KNOXX_SANDBOX_IMAGE" "knoxx-sandbox:latest")
   :sandbox-user (env "DOCKER_USER" "1000:1000")
   :sandbox-workdir (env "KNOXX_SANDBOX_WORKDIR" "/workspace")
   :sandbox-root-dir (env "KNOXX_SANDBOX_ROOT_DIR" "/tmp/knoxx-agent/sandboxes")
   :sandbox-dockerfile (env "KNOXX_SANDBOX_DOCKERFILE" "docker/sandbox/Dockerfile")
   :sandbox-build-context (env "KNOXX_SANDBOX_BUILD_CONTEXT" "docker/sandbox")
   :sandbox-default-ttl-seconds (env-int "KNOXX_SANDBOX_TTL_SECONDS" 1800)
   :sandbox-max-ttl-seconds (env-int "KNOXX_SANDBOX_MAX_TTL_SECONDS" 86400)

   ;; Redis (session persistence)
   :redis-url (env "REDIS_URL" "")

   ;; Agent recovery. Automatic resume is intentionally opt-in: backend hot reload
   ;; and ad hoc PM2 restarts must not create duplicate zombie jobs.
   :agent-auto-resume-sessions? (= "true" (str/lower-case (env "KNOXX_AGENT_AUTO_RESUME_SESSIONS" "false")))

   ;; Graceful shutdown / PM2 drain timings
   :shutdown-grace-ms (env-int "KNOXX_SHUTDOWN_GRACE_MS" 25000)
   :shutdown-poll-ms (env-int "KNOXX_SHUTDOWN_POLL_MS" 250)

   ;; MCP integration
   :mcp-enabled (not= (env "MCP_ENABLED" "false") "false")
   :mcp-servers (env "MCP_SERVERS" "")

   ;; OpenPlanner MCP server config
   :openplanner-mcp-base-url (env "OPENPLANNER_MCP_BASE_URL" "http://openplanner-mcp:8010")
   :openplanner-mcp-tool-name (env "OPENPLANNER_MCP_TOOL_NAME" "openplanner")
   :openplanner-mcp-project (env "KNOXX_OPENPLANNER_PROJECT" "devel")
   :openplanner-mcp-source (env "KNOXX_OPENPLANNER_SOURCE" "knoxx")

   ;; Shoedelussy MCP server config
   :shoedelussy-mcp-base-url (env "SHOEDELUSSY_MCP_BASE_URL" "")
   :shoedelussy-mcp-tool-name (env "SHOEDELUSSY_MCP_TOOL_NAME" "shoedelussy")
   :shoedelussy-mcp-shared-secret (env "SHOEDELUSSY_MCP_SHARED_SECRET" "")

   ;; Agent system prompt (eta-mu coding agent override)
   :agent-system-prompt (env
                         "KNOXX_AGENT_SYSTEM_PROMPT"
                         (str
                          "You are Knoxx, the grounded workspace assistant for the devel corpus. "
                          "Preserve multi-turn context within the active conversation, use workspace tools when needed, "
                          "cite file paths when they matter, and prefer grounded synthesis over shallow enumeration. "
                          "Treat passive semantic hydration as helpful but incomplete; when corpus grounding matters, "
                          "use semantic_query, semantic_read, and graph_query instead of guessing. "
                          "Long-term conversational memory lives in OpenPlanner; when the user asks about previous sessions, "
                          "prior decisions, or your own earlier actions, use memory_search and memory_session instead of pretending to remember."))})

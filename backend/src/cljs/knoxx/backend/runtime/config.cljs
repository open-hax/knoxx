(ns knoxx.backend.runtime.config
  (:require [clojure.string :as str]))

(defn- env
  [k default]
  (or (aget js/process.env k) default))

(defn cfg
  "Read Knoxx backend runtime configuration from environment variables.

   NOTE: This namespace is intentionally *env-only*.
   Any derived runtime state belongs elsewhere."
  []
  {:app-name (env "APP_NAME" "Knoxx Backend CLJS")
   :host (env "HOST" "0.0.0.0")
   :port (js/parseInt (env "PORT" "8000") 10)

   :workspace-root (env "WORKSPACE_ROOT" "/app/workspace/devel")
   :project-name (env "WORKSPACE_PROJECT_NAME" "devel")
   :session-project-name (env "KNOXX_SESSION_PROJECT_NAME" "knoxx-session")
   :collection-name (env "KNOXX_COLLECTION_NAME" "devel_docs")

   :contracts-dir (env "CONTRACTS_DIR" "contracts")

   :proxx-base-url (env "PROXX_BASE_URL" "http://proxx:8789")
   :proxx-auth-token (env "PROXX_AUTH_TOKEN" "")
   :proxx-default-model (env "PROXX_DEFAULT_MODEL" "glm-5")
   :proxx-embed-model (env "PROXX_EMBED_MODEL" "nomic-embed-text:latest")

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
   :knoxx-default-agent-contract (env "KNOXX_DEFAULT_AGENT_CONTRACT" "knoxx_default")

   :gmail-app-email (env "GMAIL_APP_EMAIL" "")
   :gmail-app-password (env "GMAIL_APP_PASSWORD" "")

   :discord-bot-token (env "DISCORD_BOT_TOKEN" "")

   ;; Voice / speech
   :stt-base-url (env "KNOXX_STT_BASE_URL" "")

   ;; TTS (ElevenLabs)
   ;; NOTE: support a few common env var names to reduce local drift.
   :elevenlabs-api-key (or (aget js/process.env "KNOXX_ELEVENLABS_API_KEY")
                           (aget js/process.env "KNOXX_ELEVENLABS_KEY")
                           (aget js/process.env "ELEVENLABS_API_KEY")
                           (aget js/process.env "ELEVEN_LABS_API_KEY")
                           (aget js/process.env "XI_API_KEY")
                           "")
   :elevenlabs-voice-id (or (aget js/process.env "KNOXX_ELEVENLABS_VOICE_ID")
                            (aget js/process.env "ELEVENLABS_VOICE_ID")
                            "")
   :elevenlabs-model-id (or (aget js/process.env "KNOXX_ELEVENLABS_MODEL_ID")
                            (aget js/process.env "ELEVENLABS_MODEL_ID")
                            "eleven_multilingual_v2")

   ;; Pi / agent runtime dir
   :agent-dir (env "KNOXX_AGENT_DIR" "/tmp/knoxx-agent")

   ;; Redis (session persistence)
   :redis-url (env "REDIS_URL" "")

   ;; MCP integration
   :mcp-enabled (not= (env "MCP_ENABLED" "false") "false")
   :mcp-servers (env "MCP_SERVERS" "")

   ;; OpenPlanner MCP server config
   :openplanner-mcp-base-url (env "OPENPLANNER_MCP_BASE_URL" "http://openplanner-mcp:8010")
   :openplanner-mcp-tool-name (env "OPENPLANNER_MCP_TOOL_NAME" "openplanner")
   :openplanner-mcp-project (env "KNOXX_OPENPLANNER_PROJECT" "devel")
   :openplanner-mcp-source (env "KNOXX_OPENPLANNER_SOURCE" "knoxx")

   ;; Agent system prompt (pi-coding-agent override)
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

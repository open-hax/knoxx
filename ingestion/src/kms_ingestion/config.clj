(ns kms-ingestion.config
  "Environment configuration."
  (:require
   [clojure.string :as str]))

(defn env-bool
  [key default]
  (let [v (System/getenv key)]
    (if (nil? v)
      default
      (contains? #{"1" "true" "yes" "on"} (.toLowerCase ^String v)))))

(defn env-int
  [key default]
  (try
    (Integer/parseInt (or (System/getenv key) (str default)))
    (catch Exception _ default)))

(defn env-double
  [key default]
  (try
    (Double/parseDouble (or (System/getenv key) (str default)))
    (catch Exception _ default)))

(defn env
  "Get environment variable with default."
  [key default]
  (or (System/getenv key) default))

(defn env-keyword
  "Read an environment variable and return a keyword.

  Normalizes to lower-case; returns default when the env var is absent or blank.
  Example: POLL -> :poll
  "
  [key default]
  (let [v (System/getenv key)]
    (if (or (nil? v) (str/blank? v))
      default
      (keyword (str/lower-case v)))))

(defn config
  "Get full configuration map."
  []
  {:port (Integer/parseInt (env "PORT" "3003"))
   :database-url (env "DATABASE_URL" "jdbc:postgresql://localhost:5432/futuresight_kms?user=kms&password=kms")
   :redis-url (env "REDIS_URL" "redis://localhost:6379")
   :ragussy-url (env "RAGUSSY_BASE_URL" "http://localhost:8000")
   :proxx-url (env "PROXX_BASE_URL" "")
   :proxx-auth-token (env "PROXX_AUTH_TOKEN" "")
   :proxx-default-model (env "PROXX_DEFAULT_MODEL" "glm-5")
   :proxx-connection-timeout-ms (env-int "PROXX_CONNECTION_TIMEOUT_MS" 15000)
   :proxx-socket-timeout-ms (env-int "PROXX_SOCKET_TIMEOUT_MS" 180000)
   :openplanner-url (env "OPENPLANNER_BASE_URL" "")
   :openplanner-api-key (env "OPENPLANNER_API_KEY" "")
   :knoxx-backend-url (env "KNOXX_BACKEND_URL" "http://knoxx-backend:8000")
   :knoxx-api-key (env "KNOXX_API_KEY" "")
   :knoxx-user-email (env "KNOXX_USER_EMAIL" "system-admin@open-hax.local")
   :audio-analysis-model (env "AUDIO_ANALYSIS_MODEL" "gemma4:e4b")
   :audio-analysis-agent-contract-id (env "AUDIO_ANALYSIS_AGENT_CONTRACT_ID" "broadcast_studio_audio_describer")
   :audio-analysis-agent-actor-id (env "AUDIO_ANALYSIS_AGENT_ACTOR_ID" "broadcast_studio_audio_task")
   :audio-analysis-agent-timeout-ms (env-int "AUDIO_ANALYSIS_AGENT_TIMEOUT_MS" 600000)
   :audio-analysis-agent-poll-ms (env-int "AUDIO_ANALYSIS_AGENT_POLL_MS" 2000)
   :audio-analysis-agent-max-concurrency (env-int "AUDIO_ANALYSIS_AGENT_MAX_CONCURRENCY" 1)
   ;; When false, audio driver sources are forced disabled (no background audio indexing).
   :audio-indexing-enabled (env-bool "AUDIO_INDEXING_ENABLED" true)
   :translation-agent-enabled (env-bool "TRANSLATION_AGENT_ENABLED" false)
   :translation-model (env "TRANSLATION_MODEL" "glm-5")
   :translation-poll-ms (env-int "TRANSLATION_POLL_MS" 5000)
   :qdrant-url (env "QDRANT_URL" "http://localhost:6333")
   :workspace-path (env "WORKSPACE_PATH" "/app/workspace")

   ;; Single-file upload endpoint (POST /api/ingestion/upload)
   :upload-dir (env "UPLOAD_DIR" "/tmp/kms-uploads")
   :upload-max-bytes (env-int "UPLOAD_MAX_BYTES" (* 50 1024 1024))

   ;; Ingestion contract defaults (used when no contract values are present)
   :ingest-hidden-policy (env-keyword "INGEST_HIDDEN_POLICY" :skip)
   :ingest-follow-symlinks? (env-bool "INGEST_FOLLOW_SYMLINKS" false)
   :ingest-schedule-mode (env-keyword "INGEST_SCHEDULE_MODE" :hybrid)
   :ingest-sync-interval-minutes (env-int "INGEST_SYNC_INTERVAL_MINUTES" 30)
   :ingest-bootstrap? (env-bool "INGEST_BOOTSTRAP" true)
   :ingest-retry-failed? (env-bool "INGEST_RETRY_FAILED" true)
   :ingest-sink-type (env-keyword "INGEST_SINK_TYPE" :openplanner)

   :ingest-scheduler-poll-ms (env-int "INGEST_SCHEDULER_POLL_MS" 60000)
   :passive-watch-enabled (env-bool "PASSIVE_WATCH_ENABLED" true)
   :passive-watch-poll-ms (env-int "PASSIVE_WATCH_POLL_MS" 60000)
   :passive-watch-debounce-ms (env-int "PASSIVE_WATCH_DEBOUNCE_MS" 5000)
   :ingest-batch-size (env-int "INGEST_BATCH_SIZE" 10)
   :ingest-batch-parallelism (env-int "INGEST_BATCH_PARALLELISM" 4)
   :ingest-throttle-enabled (env-bool "INGEST_THROTTLE_ENABLED" true)
   :ingest-max-load-per-core (env-double "INGEST_MAX_LOAD_PER_CORE" 0.85)
   :ingest-throttle-sleep-ms (env-int "INGEST_THROTTLE_SLEEP_MS" 1000)
   :ingest-batch-delay-ms (env-int "INGEST_BATCH_DELAY_MS" 100)})

(defn port [] (:port (config)))
(defn database-url [] (:database-url (config)))
(defn redis-url [] (:redis-url (config)))
(defn ragussy-url [] (:ragussy-url (config)))
(defn proxx-url [] (:proxx-url (config)))
(defn proxx-auth-token [] (:proxx-auth-token (config)))
(defn proxx-default-model [] (:proxx-default-model (config)))
(defn proxx-connection-timeout-ms [] (:proxx-connection-timeout-ms (config)))
(defn proxx-socket-timeout-ms [] (:proxx-socket-timeout-ms (config)))
(defn openplanner-url [] (:openplanner-url (config)))
(defn openplanner-api-key [] (:openplanner-api-key (config)))
(defn knoxx-backend-url [] (:knoxx-backend-url (config)))
(defn knoxx-api-key [] (:knoxx-api-key (config)))
(defn knoxx-user-email [] (:knoxx-user-email (config)))
(defn audio-analysis-model [] (:audio-analysis-model (config)))
(defn audio-analysis-agent-contract-id [] (:audio-analysis-agent-contract-id (config)))
(defn audio-analysis-agent-actor-id [] (:audio-analysis-agent-actor-id (config)))
(defn audio-analysis-agent-timeout-ms [] (:audio-analysis-agent-timeout-ms (config)))
(defn audio-analysis-agent-poll-ms [] (:audio-analysis-agent-poll-ms (config)))
(defn audio-analysis-agent-max-concurrency [] (:audio-analysis-agent-max-concurrency (config)))
(defn audio-indexing-enabled? [] (:audio-indexing-enabled (config)))
(defn translation-agent-enabled? [] (:translation-agent-enabled (config)))
(defn translation-model [] (:translation-model (config)))
(defn translation-poll-ms [] (:translation-poll-ms (config)))
(defn qdrant-url [] (:qdrant-url (config)))
(defn workspace-path [] (:workspace-path (config)))
(defn upload-dir [] (:upload-dir (config)))
(defn upload-max-bytes [] (:upload-max-bytes (config)))

;; These ingestion-* accessors exist primarily for the contracts.resolve shim layer.
(defn ingest-hidden-policy [] (:ingest-hidden-policy (config)))
(defn ingest-follow-symlinks? [] (:ingest-follow-symlinks? (config)))
(defn ingest-schedule-mode [] (:ingest-schedule-mode (config)))
(defn ingest-sync-interval-minutes [] (:ingest-sync-interval-minutes (config)))
(defn ingest-bootstrap? [] (:ingest-bootstrap? (config)))
(defn ingest-retry-failed? [] (:ingest-retry-failed? (config)))
(defn ingest-sink-type [] (:ingest-sink-type (config)))
(defn ingest-scheduler-poll-ms [] (:ingest-scheduler-poll-ms (config)))
(defn passive-watch-enabled? [] (:passive-watch-enabled (config)))
(defn passive-watch-poll-ms [] (:passive-watch-poll-ms (config)))
(defn passive-watch-debounce-ms [] (:passive-watch-debounce-ms (config)))

;; Back-compat wrappers (older naming expected by contracts.resolve)
(defn ingest-passive-watch-enabled? [] (passive-watch-enabled?))
(defn ingest-passive-watch-poll-ms [] (passive-watch-poll-ms))
(defn ingest-passive-watch-debounce-ms [] (passive-watch-debounce-ms))
(defn ingest-batch-size [] (:ingest-batch-size (config)))
(defn ingest-batch-parallelism [] (:ingest-batch-parallelism (config)))
(defn ingest-throttle-enabled? [] (:ingest-throttle-enabled (config)))
(defn ingest-max-load-per-core [] (:ingest-max-load-per-core (config)))
(defn ingest-throttle-sleep-ms [] (:ingest-throttle-sleep-ms (config)))
(defn ingest-batch-delay-ms [] (:ingest-batch-delay-ms (config)))
(defn semantic-edge-build-enabled? [] (env-bool "SEMANTIC_EDGE_BUILD_ENABLED" true))
(defn semantic-edge-build-min-similarity [] (env-double "SEMANTIC_EDGE_BUILD_MIN_SIMILARITY" 0.5))
(defn semantic-edge-build-k [] (env-int "SEMANTIC_EDGE_BUILD_K" 8))

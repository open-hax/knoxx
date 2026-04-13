(ns kms-ingestion.config
  "Environment configuration.")

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
   :translation-agent-enabled (env-bool "TRANSLATION_AGENT_ENABLED" false)
   :translation-poll-ms (env-int "TRANSLATION_POLL_MS" 5000)
   :qdrant-url (env "QDRANT_URL" "http://localhost:6333")
   :workspace-path (env "WORKSPACE_PATH" "/app/workspace")
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
(defn translation-agent-enabled? [] (:translation-agent-enabled (config)))
(defn translation-poll-ms [] (:translation-poll-ms (config)))
(defn qdrant-url [] (:qdrant-url (config)))
(defn workspace-path [] (:workspace-path (config)))
(defn ingest-scheduler-poll-ms [] (:ingest-scheduler-poll-ms (config)))
(defn passive-watch-enabled? [] (:passive-watch-enabled (config)))
(defn passive-watch-poll-ms [] (:passive-watch-poll-ms (config)))
(defn passive-watch-debounce-ms [] (:passive-watch-debounce-ms (config)))
(defn ingest-batch-size [] (:ingest-batch-size (config)))
(defn ingest-batch-parallelism [] (:ingest-batch-parallelism (config)))
(defn ingest-throttle-enabled? [] (:ingest-throttle-enabled (config)))
(defn ingest-max-load-per-core [] (:ingest-max-load-per-core (config)))
(defn ingest-throttle-sleep-ms [] (:ingest-throttle-sleep-ms (config)))
(defn ingest-batch-delay-ms [] (:ingest-batch-delay-ms (config)))
(defn semantic-edge-build-enabled? [] (env-bool "SEMANTIC_EDGE_BUILD_ENABLED" true))
(defn semantic-edge-build-min-similarity [] (env-double "SEMANTIC_EDGE_BUILD_MIN_SIMILARITY" 0.5))
(defn semantic-edge-build-k [] (env-int "SEMANTIC_EDGE_BUILD_K" 8))

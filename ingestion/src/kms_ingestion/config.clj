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
   :proxx-default-model (env "PROXX_DEFAULT_MODEL" "glm-4.7-flash")
   :openplanner-url (env "OPENPLANNER_BASE_URL" "")
   :openplanner-api-key (env "OPENPLANNER_API_KEY" "")
   :qdrant-url (env "QDRANT_URL" "http://localhost:6333")
   :workspace-path (env "WORKSPACE_PATH" "/app/workspace")
   :ingest-batch-size (env-int "INGEST_BATCH_SIZE" 10)
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
(defn openplanner-url [] (:openplanner-url (config)))
(defn openplanner-api-key [] (:openplanner-api-key (config)))
(defn qdrant-url [] (:qdrant-url (config)))
(defn workspace-path [] (:workspace-path (config)))
(defn ingest-batch-size [] (:ingest-batch-size (config)))
(defn ingest-throttle-enabled? [] (:ingest-throttle-enabled (config)))
(defn ingest-max-load-per-core [] (:ingest-max-load-per-core (config)))
(defn ingest-throttle-sleep-ms [] (:ingest-throttle-sleep-ms (config)))
(defn ingest-batch-delay-ms [] (:ingest-batch-delay-ms (config)))

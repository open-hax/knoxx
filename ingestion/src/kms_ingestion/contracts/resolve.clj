(ns kms-ingestion.contracts.resolve
  "Thin shims that extract specific runtime values from a resolved contract.

  Each function has a parallel in kms_ingestion.config and accepts either
  a contract map (preferred) or falls back to the config ns when the
  contract is nil.  This lets call sites adopt contracts incrementally
  without a flag day.

  Usage:
    (require '[kms-ingestion.contracts.resolve :as cr])
    (cr/batch-size contract)          ;; => 10
    (cr/passive-watch-enabled? contract) ;; => true"
  (:require
   [kms-ingestion.config :as config]))

;; ──────────────────────────────────────────────────────────────────────────────
;; Helpers
;; ──────────────────────────────────────────────────────────────────────────────

(defn- get-in-contract
  "Get a nested value from the contract with an env-backed fallback."
  [contract ks env-fallback]
  (let [v (get-in contract ks ::absent)]
    (if (= v ::absent)
      (env-fallback)
      v)))

;; ──────────────────────────────────────────────────────────────────────────────
;; Discovery
;; ──────────────────────────────────────────────────────────────────────────────

(defn skip-dirs       [c] (get-in-contract c [:source/discovery :skip-dirs]       #(-> {} )))
(defn skip-files      [c] (get-in-contract c [:source/discovery :skip-files]      #(-> {})))
(defn skip-extensions [c] (get-in-contract c [:source/discovery :skip-extensions] #(-> {})))
(defn text-extensions [c] (get-in-contract c [:source/discovery :text-extensions] #(-> {})))
(defn text-filenames  [c] (get-in-contract c [:source/discovery :text-filenames]  #(-> {})))
(defn file-types      [c] (get-in-contract c [:source/discovery :file-types]      #(-> [])))
(defn include-patterns [c] (get-in-contract c [:source/discovery :include-patterns] #(-> [])))
(defn exclude-patterns [c] (get-in-contract c [:source/discovery :exclude-patterns] #(-> [])))
(defn hidden-policy   [c] (get-in-contract c [:source/discovery :hidden-policy]   (constantly :skip)))
(defn follow-symlinks? [c] (get-in-contract c [:source/discovery :follow-symlinks?] (constantly false)))

;; ──────────────────────────────────────────────────────────────────────────────
;; Schedule
;; ──────────────────────────────────────────────────────────────────────────────

(defn schedule-mode          [c] (get-in-contract c [:source/schedule :mode]                      (constantly :hybrid)))
(defn sync-interval-minutes  [c] (get-in-contract c [:source/schedule :sync-interval-minutes]     (constantly 30)))
(defn scheduler-poll-ms      [c] (get-in-contract c [:source/schedule :scheduler-poll-ms]         config/ingest-scheduler-poll-ms))
(defn passive-watch-enabled? [c] (get-in-contract c [:source/schedule :passive-watch-enabled?]    config/passive-watch-enabled?))
(defn passive-watch-poll-ms  [c] (get-in-contract c [:source/schedule :passive-watch-poll-ms]     config/passive-watch-poll-ms))
(defn passive-watch-debounce-ms [c] (get-in-contract c [:source/schedule :passive-watch-debounce-ms] config/passive-watch-debounce-ms))
(defn bootstrap?             [c] (get-in-contract c [:source/schedule :bootstrap?]                (constantly true)))

;; ──────────────────────────────────────────────────────────────────────────────
;; Ingest execution
;; ──────────────────────────────────────────────────────────────────────────────

(defn batch-size           [c] (get-in-contract c [:source/ingest :batch-size]          config/ingest-batch-size))
(defn batch-parallelism    [c] (get-in-contract c [:source/ingest :batch-parallelism]   config/ingest-batch-parallelism))
(defn batch-delay-ms       [c] (get-in-contract c [:source/ingest :batch-delay-ms]      config/ingest-batch-delay-ms))
(defn throttle-enabled?    [c] (get-in-contract c [:source/ingest :throttle-enabled?]   config/ingest-throttle-enabled?))
(defn max-load-per-core    [c] (get-in-contract c [:source/ingest :max-load-per-core]   config/ingest-max-load-per-core))
(defn throttle-sleep-ms    [c] (get-in-contract c [:source/ingest :throttle-sleep-ms]   config/ingest-throttle-sleep-ms))
(defn retry-failed?        [c] (get-in-contract c [:source/ingest :retry-failed?]       (constantly true)))

;; ──────────────────────────────────────────────────────────────────────────────
;; Sink routing
;; ──────────────────────────────────────────────────────────────────────────────

(defn sink-type       [c] (get-in-contract c [:source/sink :type]         (constantly :openplanner)))
(defn sink-collections [c] (get-in-contract c [:source/sink :collections]  (constantly nil)))
(defn sink-lake       [c] (get-in-contract c [:source/sink :lake]          (constantly nil)))
(defn sink-visibility [c] (get-in-contract c [:source/sink :visibility]    (constantly "internal")))
(defn sink-language   [c] (get-in-contract c [:source/sink :language]      (constantly "en")))
(defn sink-source-label [c] (get-in-contract c [:source/sink :source-label] (constantly "kms-ingestion")))
(defn sink-created-by [c] (get-in-contract c [:source/sink :created-by]   (constantly "kms-ingestion")))
(defn sink-model      [c] (get-in-contract c [:source/sink :model]         config/proxx-default-model))

;; ──────────────────────────────────────────────────────────────────────────────
;; Semantic
;; ──────────────────────────────────────────────────────────────────────────────

(defn semantic-enabled?      [c] (get-in-contract c [:source/semantic :enabled?]           config/semantic-edge-build-enabled?))
(defn semantic-build-mode    [c] (get-in-contract c [:source/semantic :build-mode]         (constantly :incremental)))
(defn semantic-k             [c] (get-in-contract c [:source/semantic :k]                  config/semantic-edge-build-k))
(defn semantic-min-similarity [c] (get-in-contract c [:source/semantic :min-similarity]    config/semantic-edge-build-min-similarity))
(defn semantic-emit-graph-events? [c] (get-in-contract c [:source/semantic :emit-graph-events?] (constantly true)))

;; ──────────────────────────────────────────────────────────────────────────────
;; Translation
;; ──────────────────────────────────────────────────────────────────────────────

(defn translation-enabled? [c] (get-in-contract c [:source/translation :enabled?] config/translation-agent-enabled?))
(defn translation-model    [c] (get-in-contract c [:source/translation :model]    config/translation-model))
(defn translation-poll-ms  [c] (get-in-contract c [:source/translation :poll-ms]  config/translation-poll-ms))

;; ──────────────────────────────────────────────────────────────────────────────
;; Projection
;; ──────────────────────────────────────────────────────────────────────────────

(defn projection-visibility    [c] (get-in-contract c [:source/projection :visibility]   (constantly "internal")))
(defn projection-language      [c] (get-in-contract c [:source/projection :language]     (constantly "en")))
(defn projection-created-by    [c] (get-in-contract c [:source/projection :created-by]  (constantly "kms-ingestion")))
(defn projection-source        [c] (get-in-contract c [:source/projection :source]       (constantly "kms-ingestion")))
(defn domain-strategy          [c] (get-in-contract c [:source/projection :domain-strategy] (constantly :first-path-segment)))

;; ──────────────────────────────────────────────────────────────────────────────
;; Backpressure
;; ──────────────────────────────────────────────────────────────────────────────

(defn backpressure-strategy   [c] (get-in-contract c [:source/backpressure :strategy]       (constantly :exponential)))
(defn backpressure-base-ms    [c] (get-in-contract c [:source/backpressure :base-delay-ms]  (constantly 1000)))
(defn backpressure-max-ms     [c] (get-in-contract c [:source/backpressure :max-delay-ms]   (constantly 60000)))
(defn backpressure-window     [c] (get-in-contract c [:source/backpressure :failure-window] (constantly 5)))
(defn backpressure-respect-remote? [c] (get-in-contract c [:source/backpressure :respect-remote?] (constantly true)))

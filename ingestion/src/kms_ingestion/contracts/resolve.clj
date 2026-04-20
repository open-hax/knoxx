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

;; ────────────────────────────────────────────────────────────────────────────
;; Helpers
;; ────────────────────────────────────────────────────────────────────────────

(defn- get-in-contract
  "Get a nested value from the contract with an env-backed fallback.
  Treats both ::absent and nil as missing."
  [contract ks env-fallback]
  (let [v (get-in contract ks ::absent)]
    (if (or (= v ::absent) (nil? v))
      (env-fallback)
      v)))

;; ────────────────────────────────────────────────────────────────────────────
;; Discovery
;; ────────────────────────────────────────────────────────────────────────────

(defn skip-dirs       [c] (get-in-contract c [:source/discovery :skip-dirs]       (constantly #{})))
(defn skip-files      [c] (get-in-contract c [:source/discovery :skip-files]      (constantly #{})))
(defn skip-extensions [c] (get-in-contract c [:source/discovery :skip-extensions] (constantly #{})))
(defn text-extensions [c] (get-in-contract c [:source/discovery :text-extensions] (constantly #{})))
(defn text-filenames  [c] (get-in-contract c [:source/discovery :text-filenames]  (constantly #{})))
(defn hidden-policy   [c] (get-in-contract c [:source/discovery :hidden-policy]   #(config/ingest-hidden-policy)))
(defn follow-symlinks? [c] (get-in-contract c [:source/discovery :follow-symlinks?] #(config/ingest-follow-symlinks?)))
(defn file-types      [c] (get-in-contract c [:source/discovery :file-types]      (constantly [])))
(defn include-patterns [c] (get-in-contract c [:source/discovery :include-patterns] (constantly [])))
(defn exclude-patterns [c] (get-in-contract c [:source/discovery :exclude-patterns] (constantly [])))

;; ────────────────────────────────────────────────────────────────────────────
;; Schedule
;; ────────────────────────────────────────────────────────────────────────────

(defn schedule-mode           [c] (get-in-contract c [:source/schedule :mode]                       #(config/ingest-schedule-mode)))
(defn sync-interval-minutes   [c] (get-in-contract c [:source/schedule :sync-interval-minutes]      #(config/ingest-sync-interval-minutes)))
(defn scheduler-poll-ms       [c] (get-in-contract c [:source/schedule :scheduler-poll-ms]          #(config/ingest-scheduler-poll-ms)))
(defn passive-watch-enabled?  [c] (get-in-contract c [:source/schedule :passive-watch-enabled?]     #(config/ingest-passive-watch-enabled?)))
(defn passive-watch-poll-ms   [c] (get-in-contract c [:source/schedule :passive-watch-poll-ms]      #(config/ingest-passive-watch-poll-ms)))
(defn passive-watch-debounce-ms [c] (get-in-contract c [:source/schedule :passive-watch-debounce-ms] #(config/ingest-passive-watch-debounce-ms)))
(defn bootstrap?              [c] (get-in-contract c [:source/schedule :bootstrap?]                 #(config/ingest-bootstrap?)))

;; ────────────────────────────────────────────────────────────────────────────
;; Ingest
;; ────────────────────────────────────────────────────────────────────────────

(defn batch-size         [c] (get-in-contract c [:source/ingest :batch-size]         #(config/ingest-batch-size)))
(defn batch-parallelism  [c] (get-in-contract c [:source/ingest :batch-parallelism]  #(config/ingest-batch-parallelism)))
(defn batch-delay-ms     [c] (get-in-contract c [:source/ingest :batch-delay-ms]     #(config/ingest-batch-delay-ms)))
(defn throttle-enabled?  [c] (get-in-contract c [:source/ingest :throttle-enabled?]  #(config/ingest-throttle-enabled?)))
(defn max-load-per-core  [c] (get-in-contract c [:source/ingest :max-load-per-core]  #(config/ingest-max-load-per-core)))
(defn throttle-sleep-ms  [c] (get-in-contract c [:source/ingest :throttle-sleep-ms]  #(config/ingest-throttle-sleep-ms)))
(defn retry-failed?      [c] (get-in-contract c [:source/ingest :retry-failed?]      #(config/ingest-retry-failed?)))

;; ────────────────────────────────────────────────────────────────────────────
;; Sink
;; ────────────────────────────────────────────────────────────────────────────

(defn sink-type    [c] (get-in-contract c [:source/sink :type]       #(config/ingest-sink-type)))
(defn sink-url     [c] (get-in-contract c [:source/sink :url]        (constantly nil)))
(defn sink-api-key [c] (get-in-contract c [:source/sink :api-key]    (constantly nil)))

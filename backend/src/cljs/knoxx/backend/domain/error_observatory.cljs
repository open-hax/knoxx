(ns knoxx.backend.domain.error-observatory
  "Central error boundary helpers for Knoxx.

   Domain code should return explicit outcome data for expected failures.
   Boundary/adaptor code should catch thrown JS/CLJS failures here, log once with
   structured context, and then return/persist normalized failure data."
  (:require [clojure.string :as str]))

(defn- err-prop
  [err prop]
  (try
    (aget err prop)
    (catch :default _ nil)))

(defn error-message
  [err]
  (or (some-> (err-prop err "message") str str/trim not-empty)
      (some-> err str str/trim not-empty)
      "Unknown error"))

(defn- ex-data-safe
  [err]
  (try
    (ex-data err)
    (catch :default _ nil)))

(defn- json-key
  [k]
  (cond
    (keyword? k) (subs (str k) 1)
    (symbol? k) (str k)
    :else k))

(defn- preserve-map-keys
  [value]
  (cond
    (map? value) (into {} (map (fn [[k v]] [(json-key k) (preserve-map-keys v)])) value)
    (sequential? value) (mapv preserve-map-keys value)
    :else value))

(defn safe-json
  [value]
  (try
    (.stringify js/JSON (clj->js (preserve-map-keys value)) nil 2)
    (catch :default _
      (str value))))

(defn error-data
  "Normalize a thrown error into plain data suitable for logs, API responses, and
   run-event receipts. Context must be intentionally small and secret-free."
  [boundary context err]
  (cond-> {:ok false
           :failed true
           :boundary (str boundary)
           :message (error-message err)
           :context (or context {})}
    (some-> (err-prop err "name") str str/trim not-empty)
    (assoc :name (str (err-prop err "name")))

    (some-> (err-prop err "code") str str/trim not-empty)
    (assoc :code (str (err-prop err "code")))

    (some-> (or (err-prop err "status") (err-prop err "statusCode")) str str/trim not-empty)
    (assoc :status (or (err-prop err "status") (err-prop err "statusCode")))

    (some-> (err-prop err "stack") str str/trim not-empty)
    (assoc :stack (str (err-prop err "stack")))

    (ex-data-safe err)
    (assoc :data (ex-data-safe err))))

(defn log-error!
  "Log an unexpected boundary failure once and return normalized failure data."
  [boundary context err]
  (let [data (error-data boundary context err)]
    (.error js/console "[knoxx.error]" (str boundary) (safe-json data))
    (when-let [stack (:stack data)]
      (.error js/console "[knoxx.error.stack]" (str boundary) "\n" stack))
    data))

(defn log-warning!
  "Log an expected/degraded boundary outcome as structured data."
  [boundary context]
  (let [data {:ok false
              :failed false
              :boundary (str boundary)
              :context (or context {})}]
    (.warn js/console "[knoxx.warn]" (str boundary) (safe-json data))
    data))

(defn promise-like?
  [value]
  (and value (fn? (err-prop value "catch"))))

(defn observe-promise!
  "Attach a central log sink to a background promise. Returns the original promise.
   Use this when a callback is intentionally fire-and-forget."
  [boundary context promise]
  (when (promise-like? promise)
    (.catch promise (fn [err]
                      (log-error! boundary context err)
                      nil)))
  promise)

(defn call-observed!
  "Call f at a boundary. Synchronous exceptions and async rejections are logged;
   the callback result is returned so callers can still await/chain it."
  [boundary context f]
  (try
    (observe-promise! boundary context (f))
    (catch :default err
      (log-error! boundary context err)
      nil)))

(ns knoxx.backend.infra.temp-memory
  "Short-term memory tool. Agents read/write keyed blobs with TTL.
   Backed by Redis when available; falls back to a process-local atom."
  (:require [knoxx.backend.infra.redis-client :as redis]))

(defonce ^:private local-store* (atom {}))

(def ^:private default-ttl-ms (* 60 60 1000)) ;; 1 hour

(defn parse-ttl-ms
  "Accept ISO-8601 duration string or integer seconds."
  [ttl]
  (cond
    (number? ttl) (* ttl 1000)
    (string? ttl) (let [[_ h m s] (re-find #"PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?" ttl)
                        total (+ (* (or (parse-long (str h)) 0) 3600000)
                                 (* (or (parse-long (str m)) 0)   60000)
                                 (* (or (parse-long (str s)) 0)   1000))]
                    (if (pos? total) total default-ttl-ms))
    :else         default-ttl-ms))
(defn- now-ms [] (.now js/Date))

(def ^:private LOCAL_STORE_MAX 256)

;; ─── local fallback ────────────────────────────────────────────────────────────────────────

(defn- sweep-expired!
  "Remove expired entries from local-store*. Called on writes to prevent unbounded growth."
  []
  (let [now (now-ms)]
    (swap! local-store*
           (fn [store]
             (if (<= (count store) LOCAL_STORE_MAX)
               ;; Below cap: only sweep expired
               (into {} (filter (fn [[_ {:keys [expires-at]}]]
                                  (> expires-at now)))
                     store)
               ;; Over cap: sweep expired, then keep newest LOCAL_STORE_MAX
               (->> store
                    (filter (fn [[_ {:keys [expires-at]}]]
                              (> expires-at now)))
                    (sort-by (fn [[_ {:keys [expires-at]}]] (- expires-at)))
                    (take LOCAL_STORE_MAX)
                    (into {})))))))

(defn- local-set! [k v ttl-ms]
  (sweep-expired!)
  (swap! local-store* assoc k {:value v :expires-at (+ (now-ms) ttl-ms)})
  nil)

(defn- local-get [k]
  (let [{:keys [value expires-at]} (get @local-store* k)]
    (when (and value (> expires-at (now-ms)))
      value)))

(defn- local-del! [k]
  (swap! local-store* dissoc k)
  nil)

;; ─── public API ──────────────────────────────────────────────────────────────────────────────

(defn ^:async mem-set!
  "Write `value` under `key` with optional `ttl` (ISO-8601 or seconds).
   Returns a Promise resolving to {:key k :written true}."
  [k v & [{:keys [ttl]}]]
  (let [ttl-ms (parse-ttl-ms (or ttl default-ttl-ms))]
    (if-let [client (redis/get-client)]
      (let [ttl-sec (max 1 (js/Math.ceil (/ ttl-ms 1000)))]
        (try
          (await (redis/set-json client (str "temp-mem:" k) v ttl-sec))
          {:key k :written true}
          (catch :default _
            (local-set! k v ttl-ms)
            {:key k :written true :backend :local})))
      (do (local-set! k v ttl-ms)
          {:key k :written true :backend :local}))))

(defn ^:async mem-get
  "Read the value at `key`. Returns Promise<value | nil>."
  [k]
  (if-let [client (redis/get-client)]
    (try
      (await (redis/get-json client (str "temp-mem:" k)))
      (catch :default _
        (local-get k)))
    (local-get k)))

(defn ^:async mem-del!
  "Delete `key`. Returns Promise<{:key k :deleted true}>."
  [k]
  (if-let [client (redis/get-client)]
    (try
      (await (redis/del client (str "temp-mem:" k)))
      {:key k :deleted true}
      (catch :default _
        (local-del! k)
        {:key k :deleted true :backend :local}))
    (do (local-del! k)
        {:key k :deleted true :backend :local})))

;; ─── tool registration ────────────────────────────────────────────────────────────────────────

(def tool-spec
  {:id "memory.temp"
   :label "Temporary Memory"
   :description "Read or write short-lived keyed data with a TTL. Use for passing state between pipeline steps or caching within a session."
   :params {:op   {:type "string" :enum ["set" "get" "del"] :required true}
            :key  {:type "string" :required true}
            :value {:type "any"   :required false}
            :ttl  {:type "string" :description "ISO-8601 duration or integer seconds. Default PT1H."}}
   :handler (fn [{:keys [op key value ttl]}]
              (case op
                "set" (mem-set! key value {:ttl ttl})
                "get" (mem-get key)
                "del" (mem-del! key)))})

(ns knoxx.backend.infra.redis-client
  "Simple Redis client for Knoxx session storage.

   Uses node-redis under the hood with promise-based API."
  (:require [clojure.string :as str]
            ["redis" :as redis]))

(defonce redis-client* (atom nil))
(defonce redis-init-promise* (atom nil))

(defn- redis-arg
  "Coerce common CLJS/JS values into Redis-safe scalar arguments.

Cured the ERR_HTTP_HEADERS_SENT and Redis SADD TypeError.
Triumphant manifestation of intent: 'I fixed it bitch'.
Onwards to glory."
  [value]
  (cond
    (nil? value) nil
    (string? value) value
    (number? value) (.toString value)
    (boolean? value) (if value "true" "false")
    (or (map? value) (vector? value) (set? value) (seq? value))
    (js/JSON.stringify (clj->js value))
    :else
    (try
      (let [json (when (and value
                            (not= value js/undefined)
                            (or (array? value)
                                (= "object" (goog/typeOf value))))
                   (js/JSON.stringify value))]
        (if (string? json)
          json
          (str value)))
      (catch :default _
        (str value)))))

(defn create-client
  "Create a Redis client from URL. Returns nil if URL is empty or client creation fails."
  [redis-url]
  (when (and redis-url (not (str/blank? redis-url)))
    (try
      (let [client (.createClient redis #js {:url redis-url})]
        (.on client "error" (fn [err]
                               (js/console.error "Redis client error:" err)))
        (.on client "connect" (fn []
                                 (js/console.log "Redis client connected")))
        (.on client "end" (fn []
                             (js/console.warn "Redis client disconnected")))
        client)
      (catch :default e
        (js/console.error "Failed to create Redis client:" e)
        nil))))

(defn ^:async connect-redis-client!
  [client]
  (try
    (await (.connect client))
    (reset! redis-client* client)
    client
    (catch :default err
      (js/console.error "Failed to connect Redis client:" err)
      (reset! redis-client* nil)
      nil)
    (finally
      (reset! redis-init-promise* nil))))

(defn ^:async init-redis!
  "Initialize and connect the Redis client from environment.
   Returns a promise resolving to the connected client or nil."
  [redis-url]
  (cond
    (str/blank? (str redis-url))
    nil

    @redis-client*
    @redis-client*

    @redis-init-promise*
    (await @redis-init-promise*)

    :else
    (if-let [client (create-client redis-url)]
      (let [connect-promise (connect-redis-client! client)]
        (reset! redis-init-promise* connect-promise)
        (await connect-promise))
      nil)))

(defn get-client
  "Get the current connected Redis client, or nil if not initialized."
  []
  @redis-client*)

;; Promise wrappers for Redis commands

(defn ^:async get-key
  "Get a value from Redis."
  [client key]
  (try
    (await (.get client (redis-arg key)))
    (catch :default err
      (js/console.error "Redis GET error:" err)
      nil)))

(defn ^:async set-key
  "Set a value in Redis with optional TTL (seconds)."
  ([client key value]
   (set-key client key value nil))
  ([client key value ttl]
   (let [key' (redis-arg key)
         value' (redis-arg value)]
     (try
       (await (if ttl
                (.set client key' value' #js {:EX ttl})
                (.set client key' value')))
       (catch :default err
         (js/console.error "Redis SET error:" err))))))

(defn ^:async set-json
  "Set a JSON value in Redis with optional TTL."
  ([client key value]
   (set-json client key value nil))
  ([client key value ttl]
   (try
     (await (.set client (redis-arg key) (js/JSON.stringify (clj->js value))))
     (when ttl
       (await (.expire client (redis-arg key) ttl)))
     (catch :default err
       (js/console.error "Redis SET JSON error:" err)))))

(defn ^:async get-json
  "Get a JSON value from Redis, parsed to CLJ."
  [client key]
  (try
    (when-let [value (await (.get client (redis-arg key)))]
      (js->clj (js/JSON.parse value) :keywordize-keys true))
    (catch :default err
      (js/console.error "Redis GET JSON error:" err)
      nil)))

(defn ^:async del
  "Delete a key from Redis."
  [client key]
  (try
    (await (.del client (redis-arg key)))
    (catch :default err
      (js/console.error "Redis DEL error:" err))))

(defn ^:async sadd
  "Add member to set."
  [client key member]
  (try
    (await (.sAdd client (redis-arg key) (redis-arg member)))
    (catch :default err
      (js/console.error "Redis SADD error:" err))))

(defn ^:async srem
  "Remove member from set."
  [client key member]
  (try
    (await (.sRem client (redis-arg key) (redis-arg member)))
    (catch :default err
      (js/console.error "Redis SREM error:" err))))

(defn ^:async smembers
  "Get all members of a set."
  [client key]
  (try
    (js->clj (await (.sMembers client (redis-arg key))))
    (catch :default err
      (js/console.error "Redis SMEMBERS error:" err)
      [])))

(defn ^:async expire
  "Set TTL on a key."
  [client key ttl-seconds]
  (try
    (await (.expire client (redis-arg key) ttl-seconds))
    (catch :default err
      (js/console.error "Redis EXPIRE error:" err))))

(defn ^:async lpush
  "Push a value to the head of a Redis list."
  [client key value]
  (try
    (await (.lPush client (redis-arg key) (redis-arg value)))
    (catch :default err
      (js/console.error "Redis LPUSH error:" err))))

(defn ^:async lpush-json
  "Push a JSON-encoded value to the head of a Redis list."
  [client key value]
  (try
    (await (.lPush client (redis-arg key) (js/JSON.stringify (clj->js value))))
    (catch :default err
      (js/console.error "Redis LPUSH JSON error:" err))))

(defn ^:async lrange
  "Get a range of elements from a Redis list."
  [client key start stop]
  (try
    (let [items (await (.lRange client (redis-arg key) start stop))]
      (if (array? items)
        (vec (array-seq items))
        []))
    (catch :default err
      (js/console.error "Redis LRANGE error:" err)
      [])))

(defn ^:async lrange-json
  "Get a range of elements from a Redis list, parsing each as JSON."
  [client key start stop]
  (try
    (let [items (await (.lRange client (redis-arg key) start stop))]
      (if (array? items)
        (->> (array-seq items)
             (keep (fn [item]
                     (try
                       (js->clj (js/JSON.parse item) :keywordize-keys true)
                       (catch :default _ nil))))
             vec)
        []))
    (catch :default err
      (js/console.error "Redis LRANGE JSON error:" err)
      [])))

(defn ^:async llen
  "Get the length of a Redis list."
  [client key]
  (try
    (or (await (.lLen client (redis-arg key))) 0)
    (catch :default err
      (js/console.error "Redis LLEN error:" err)
      0)))

(defn ^:async ping
  "Ping Redis to check connection."
  [client]
  (try
    (= (await (.ping client)) "PONG")
    (catch :default err
      (js/console.error "Redis PING error:" err)
      false)))

(defn ^:async quit
  "Close Redis connection."
  [client]
  (reset! redis-client* nil)
  (reset! redis-init-promise* nil)
  (when client
    (try
      (await (.quit client))
      (catch :default err
        (js/console.error "Redis QUIT error:" err)))))

(ns knoxx.backend.redis-client
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

(defn init-redis!
  "Initialize and connect the Redis client from environment.
   Returns a promise resolving to the connected client or nil."
  [redis-url]
  (cond
    (str/blank? (str redis-url))
    (js/Promise.resolve nil)

    @redis-client*
    (js/Promise.resolve @redis-client*)

    @redis-init-promise*
    @redis-init-promise*

    :else
    (if-let [client (create-client redis-url)]
      (let [connect-promise (-> (.connect client)
                                (.then (fn []
                                         (reset! redis-client* client)
                                         client))
                                (.catch (fn [err]
                                          (js/console.error "Failed to connect Redis client:" err)
                                          (reset! redis-client* nil)
                                          nil))
                                (.finally (fn []
                                            (reset! redis-init-promise* nil))))]
        (reset! redis-init-promise* connect-promise)
        connect-promise)
      (js/Promise.resolve nil))))

(defn get-client
  "Get the current connected Redis client, or nil if not initialized."
  []
  @redis-client*)

;; Promise wrappers for Redis commands

(defn get-key
  "Get a value from Redis."
  [client key]
  (-> client
      (.get (redis-arg key))
      (.catch (fn [err]
                (js/console.error "Redis GET error:" err)
                nil))))

(defn set-key
  "Set a value in Redis with optional TTL (seconds)."
  ([client key value]
   (set-key client key value nil))
  ([client key value ttl]
   (let [key' (redis-arg key)
         value' (redis-arg value)]
     (-> (if ttl
           (.set client key' value' #js {:EX ttl})
           (.set client key' value'))
         (.catch (fn [err]
                   (js/console.error "Redis SET error:" err)))))))

(defn set-json
  "Set a JSON value in Redis with optional TTL."
  ([client key value]
   (set-json client key value nil))
  ([client key value ttl]
   (-> client
       (.set (redis-arg key) (js/JSON.stringify (clj->js value)))
       (.then (fn []
                (when ttl
                  (.expire client (redis-arg key) ttl))))
       (.catch (fn [err]
                 (js/console.error "Redis SET JSON error:" err))))))

(defn get-json
  "Get a JSON value from Redis, parsed to CLJ."
  [client key]
  (-> client
      (.get (redis-arg key))
      (.then (fn [value]
               (when value
                 (js->clj (js/JSON.parse value) :keywordize-keys true))))
      (.catch (fn [err]
                (js/console.error "Redis GET JSON error:" err)
                nil))))

(defn del
  "Delete a key from Redis."
  [client key]
  (-> client
      (.del (redis-arg key))
      (.catch (fn [err]
                (js/console.error "Redis DEL error:" err)))))

(defn sadd
  "Add member to set."
  [client key member]
  (-> client
      (.sAdd (redis-arg key) (redis-arg member))
      (.catch (fn [err]
                (js/console.error "Redis SADD error:" err)))))

(defn srem
  "Remove member from set."
  [client key member]
  (-> client
      (.sRem (redis-arg key) (redis-arg member))
      (.catch (fn [err]
                (js/console.error "Redis SREM error:" err)))))

(defn smembers
  "Get all members of a set."
  [client key]
  (-> client
      (.sMembers (redis-arg key))
      (.then (fn [members]
               (js->clj members)))
      (.catch (fn [err]
                (js/console.error "Redis SMEMBERS error:" err)
                []))))

(defn expire
  "Set TTL on a key."
  [client key ttl-seconds]
  (-> client
      (.expire (redis-arg key) ttl-seconds)
      (.catch (fn [err]
                (js/console.error "Redis EXPIRE error:" err)))))

(defn lpush
  "Push a value to the head of a Redis list."
  [client key value]
  (-> client
      (.lPush (redis-arg key) (redis-arg value))
      (.catch (fn [err]
                (js/console.error "Redis LPUSH error:" err)))))

(defn lpush-json
  "Push a JSON-encoded value to the head of a Redis list."
  [client key value]
  (-> client
      (.lPush (redis-arg key) (js/JSON.stringify (clj->js value)))
      (.catch (fn [err]
                (js/console.error "Redis LPUSH JSON error:" err)))))

(defn lrange
  "Get a range of elements from a Redis list."
  [client key start stop]
  (-> client
      (.lRange (redis-arg key) start stop)
      (.then (fn [items]
               (if (array? items)
                 (vec (array-seq items))
                 [])))
      (.catch (fn [err]
                (js/console.error "Redis LRANGE error:" err)
                []))))

(defn lrange-json
  "Get a range of elements from a Redis list, parsing each as JSON."
  [client key start stop]
  (-> client
      (.lRange (redis-arg key) start stop)
      (.then (fn [items]
               (if (array? items)
                 (->> (array-seq items)
                      (keep (fn [item]
                              (try
                                (js->clj (js/JSON.parse item) :keywordize-keys true)
                                (catch :default _ nil))))
                      vec)
                 [])))
      (.catch (fn [err]
                (js/console.error "Redis LRANGE JSON error:" err)
                []))))

(defn llen
  "Get the length of a Redis list."
  [client key]
  (-> client
      (.lLen (redis-arg key))
      (.then (fn [n] (or n 0)))
      (.catch (fn [err]
                (js/console.error "Redis LLEN error:" err)
                0))))

(defn ping
  "Ping Redis to check connection."
  [client]
  (-> client
      (.ping)
      (.then (fn [result]
               (= result "PONG")))
      (.catch (fn [err]
                (js/console.error "Redis PING error:" err)
                false))))

(defn quit
  "Close Redis connection."
  [client]
  (reset! redis-client* nil)
  (reset! redis-init-promise* nil)
  (when client
    (-> client
        (.quit)
        (.catch (fn [err]
                  (js/console.error "Redis QUIT error:" err))))))

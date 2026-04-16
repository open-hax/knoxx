(ns knoxx.backend.redis-client
  "Simple Redis client for Knoxx session storage.

   Uses node-redis under the hood with promise-based API."
  (:require [clojure.string :as str]))

(defonce redis-client* (atom nil))
(defonce redis-init-promise* (atom nil))

(defn create-client
  "Create a Redis client from URL. Returns nil if URL is empty or client creation fails."
  [redis-url]
  (when (and redis-url (not (str/blank? redis-url)))
    (try
      (let [redis (js/require "redis")
            client (.createClient redis #js {:url redis-url})]
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

(defn get
  "Get a value from Redis."
  [client key]
  (-> client
      (.get key)
      (.catch (fn [err]
                (js/console.error "Redis GET error:" err)
                nil))))

(defn set
  "Set a value in Redis with optional TTL (seconds)."
  ([client key value]
   (set client key value nil))
  ([client key value ttl]
   (let [args (if ttl
                #js {:EX ttl}
                #js {})]
     (-> client
         (.set key value args)
         (.catch (fn [err]
                   (js/console.error "Redis SET error:" err)))))))

(defn set-json
  "Set a JSON value in Redis with optional TTL."
  ([client key value]
   (set-json client key value nil))
  ([client key value ttl]
   (-> client
       (.set key (js/JSON.stringify (clj->js value)))
       (.then (fn []
                (when ttl
                  (.expire client key ttl))))
       (.catch (fn [err]
                 (js/console.error "Redis SET JSON error:" err))))))

(defn get-json
  "Get a JSON value from Redis, parsed to CLJ."
  [client key]
  (-> client
      (.get key)
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
      (.del key)
      (.catch (fn [err]
                (js/console.error "Redis DEL error:" err)))))

(defn sadd
  "Add member to set."
  [client key member]
  (-> client
      (.sAdd key member)
      (.catch (fn [err]
                (js/console.error "Redis SADD error:" err)))))

(defn srem
  "Remove member from set."
  [client key member]
  (-> client
      (.sRem key member)
      (.catch (fn [err]
                (js/console.error "Redis SREM error:" err)))))

(defn smembers
  "Get all members of a set."
  [client key]
  (-> client
      (.sMembers key)
      (.then (fn [members]
               (js->clj members)))
      (.catch (fn [err]
                (js/console.error "Redis SMEMBERS error:" err)
                []))))

(defn expire
  "Set TTL on a key."
  [client key ttl-seconds]
  (-> client
      (.expire key ttl-seconds)
      (.catch (fn [err]
                (js/console.error "Redis EXPIRE error:" err)))))

(defn lpush
  "Push a value to the head of a Redis list."
  [client key value]
  (-> client
      (.lPush key value)
      (.catch (fn [err]
                (js/console.error "Redis LPUSH error:" err)))))

(defn lpush-json
  "Push a JSON-encoded value to the head of a Redis list."
  [client key value]
  (-> client
      (.lPush key (js/JSON.stringify (clj->js value)))
      (.catch (fn [err]
                (js/console.error "Redis LPUSH JSON error:" err)))))

(defn lrange
  "Get a range of elements from a Redis list."
  [client key start stop]
  (-> client
      (.lRange key start stop)
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
      (.lRange key start stop)
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
      (.lLen key)
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

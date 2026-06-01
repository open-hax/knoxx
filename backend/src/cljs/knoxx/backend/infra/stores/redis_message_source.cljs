(ns knoxx.backend.infra.stores.redis-message-source
  (:require [clojure.string :as str]
            [knoxx.backend.infra.stores.message-source :refer [IMessageSource]]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.infra.stores.session-store :as session-store]))

(defn ^:async messages-from-session!
  [client session-id]
  (if (or (nil? client) (str/blank? (str (or session-id ""))))
    []
    (let [session (await (session-store/get-session client session-id))]
      (vec (or (:messages session) [])))))

(defn ^:async fetch-redis-messages!
  [preferred-session-id conversation-id]
  (let [client (redis/get-client)]
    (cond
      preferred-session-id
      (await (messages-from-session! client preferred-session-id))

      (or (str/blank? conversation-id) (nil? client))
      []

      :else
      (let [session-id (await (session-store/get-conversation-active-session client conversation-id))]
        (await (messages-from-session! client session-id))))))

(defrecord RedisMessageSource [preferred-session-id]
  IMessageSource
  (fetch-messages! [_ conversation-id]
    (fetch-redis-messages! preferred-session-id conversation-id)))

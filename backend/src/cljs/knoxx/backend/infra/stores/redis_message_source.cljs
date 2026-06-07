(ns knoxx.backend.infra.stores.redis-message-source
  (:require [clojure.string :as str]
            [knoxx.backend.infra.stores.message-source :refer [IMessageSource]]
            [knoxx.backend.infra.stores.mongo-session-store :as session-store]))

(defn ^:async messages-from-session!
  [session-id]
  (if (str/blank? (str (or session-id "")))
    []
    (let [session (await (session-store/get-session session-id))]
      (vec (or (:messages session) [])))))

(defn ^:async fetch-redis-messages!
  [preferred-session-id conversation-id]
  (cond
    preferred-session-id
    (await (messages-from-session! preferred-session-id))

    (str/blank? conversation-id)
    []

    :else
    (let [session-id (await (session-store/get-conversation-active-session conversation-id))]
      (await (messages-from-session! session-id)))))

(defrecord RedisMessageSource [preferred-session-id]
  IMessageSource
  (fetch-messages! [_ conversation-id]
    (fetch-redis-messages! preferred-session-id conversation-id)))

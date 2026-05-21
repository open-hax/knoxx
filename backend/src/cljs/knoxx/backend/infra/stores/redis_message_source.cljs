(ns knoxx.backend.infra.stores.redis-message-source
  (:require [clojure.string :as str]
            [knoxx.backend.infra.stores.message-source :refer [IMessageSource]]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.infra.stores.session-store :as session-store]))

(defn- messages-from-session!
  [client session-id]
  (if (or (nil? client) (str/blank? (str (or session-id ""))))
    (js/Promise.resolve [])
    (-> (session-store/get-session client session-id)
        (.then (fn [session] (vec (or (:messages session) [])))))))

(defrecord RedisMessageSource [preferred-session-id]
  IMessageSource
  (fetch-messages! [_ conversation-id]
    (let [client (redis/get-client)]
      (if preferred-session-id
        (messages-from-session! client preferred-session-id)
        (if (or (str/blank? conversation-id) (nil? client))
          (js/Promise.resolve [])
          (-> (session-store/get-conversation-active-session client conversation-id)
              (.then (fn [session-id]
                       (messages-from-session! client session-id)))))))))

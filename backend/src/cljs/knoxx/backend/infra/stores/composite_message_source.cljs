(ns knoxx.backend.infra.stores.composite-message-source
  (:require [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.infra.stores.message-source :refer [IMessageSource fetch-messages!]]
            [knoxx.backend.infra.agent.message :as msg]))

(defrecord CompositeMessageSource [primary secondary]
  IMessageSource
  (fetch-messages! [_ conversation-id]
    (-> (promise/all-vec [(fetch-messages! primary conversation-id)
                          (fetch-messages! secondary conversation-id)])
        (.then (fn [[primary-messages secondary-messages]]
                 (msg/merge-restored-session-messages
                  (vec (or primary-messages []))
                  (vec (or secondary-messages []))))))))

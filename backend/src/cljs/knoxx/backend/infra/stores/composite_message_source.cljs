(ns knoxx.backend.infra.stores.composite-message-source
  (:require [knoxx.backend.infra.stores.message-source :refer [IMessageSource fetch-messages!]]
            [knoxx.backend.infra.agent.message :as msg]))

(defn ^:async fetch-composite-messages!
  [primary secondary conversation-id]
  (let [[primary-messages secondary-messages]
        (await (js/Promise.all #js [(fetch-messages! primary conversation-id)
                                    (fetch-messages! secondary conversation-id)]))]
    (msg/merge-restored-session-messages
     (vec (or primary-messages []))
     (vec (or secondary-messages [])))))

(defrecord CompositeMessageSource [primary secondary]
  IMessageSource
  (fetch-messages! [_ conversation-id]
    (fetch-composite-messages! primary secondary conversation-id)))

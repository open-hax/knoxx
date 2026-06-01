(ns knoxx.backend.infra.stores.openplanner-message-source
  (:require [clojure.string :as str]
            [knoxx.backend.infra.stores.message-source :refer [IMessageSource]]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.agent.message :as msg]))

(defn ^:async fetch-openplanner-messages!
  [config conversation-id]
  (let [client (or (:openplanner-client config)
                   (openplanner-client/client config))]
    (if (or (str/blank? conversation-id)
            (not (openplanner-client/enabled? client)))
      []
      (let [response (await (openplanner-client/session! client conversation-id nil))]
        (->> (or (:rows response) [])
             (keep msg/planner-row->stored-session-message)
             vec)))))

(defrecord OpenPlannerMessageSource [config]
  IMessageSource
  (fetch-messages! [_ conversation-id]
    (fetch-openplanner-messages! config conversation-id)))

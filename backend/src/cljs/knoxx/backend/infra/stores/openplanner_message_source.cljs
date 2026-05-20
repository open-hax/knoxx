(ns knoxx.backend.infra.stores.openplanner-message-source
  (:require [clojure.string :as str]
            [knoxx.backend.infra.stores.message-source :refer [IMessageSource]]
            [knoxx.backend.infra.http :as http]
            [knoxx.backend.infra.agent.message :as msg]))

(defrecord OpenPlannerMessageSource [config]
  IMessageSource
  (fetch-messages! [_ conversation-id]
    (if (or (str/blank? conversation-id)
            (not (http/openplanner-enabled? config)))
      (js/Promise.resolve [])
      (-> (http/openplanner-request! config "GET" (str "/v1/sessions/" conversation-id))
          (.then (fn [response]
                   (->> (or (:rows response) [])
                        (keep msg/planner-row->stored-session-message)
                        vec)))))))

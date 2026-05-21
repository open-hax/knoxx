(ns knoxx.backend.infra.agent.provider.eta-mu
  "Eta-mu provider adapter port for agent runtime session construction."
  (:require [knoxx.backend.domain.models :refer [models-config]]
            [knoxx.backend.extern.eta-mu :as eta-mu-extern]
            [knoxx.backend.infra.agent.provider :refer [fetch-proxx-model-ids!]]))

(defprotocol IAgentProviderAdapter
  (ensure-runtime! [provider])
  (resolve-model [provider model-registry model-provider-id model-id fallback-model-id])
  (create-session! [provider session-request])
  (send-message! [provider provider-session message-request])
  (subscribe-stream! [provider provider-session handlers]))

(defrecord EtaMuProviderAdapter [runtime config]
  IAgentProviderAdapter
  (ensure-runtime! [_]
    (-> (fetch-proxx-model-ids! config)
        (.then (fn [model-ids]
                 (eta-mu-extern/setup-runtime!
                  config
                  (models-config config model-ids)
                  {:enabled (not= false (:agent-compaction-enabled? config))
                   :reserveTokens (or (:agent-compaction-reserve-tokens config) 16384)
                   :keepRecentTokens (or (:agent-compaction-keep-recent-tokens config) 20000)})))))

  (resolve-model [_ model-registry model-provider-id model-id fallback-model-id]
    (eta-mu-extern/find-model model-registry model-provider-id model-id fallback-model-id))

  (create-session! [_ session-request]
    (eta-mu-extern/create-session! session-request))

  (send-message! [_ provider-session message-request]
    (if-let [send! (:send-message! message-request)]
      (send! provider-session message-request)
      (js/Promise.reject (js/Error. "EtaMuProviderAdapter send-message! requires :send-message! in message-request"))))

  (subscribe-stream! [_ provider-session handlers]
    (if-let [subscribe! (:subscribe! handlers)]
      (subscribe! provider-session handlers)
      (js/Promise.reject (js/Error. "EtaMuProviderAdapter subscribe-stream! requires :subscribe! in handlers")))))

(defn eta-mu-provider
  [runtime config]
  (->EtaMuProviderAdapter runtime config))

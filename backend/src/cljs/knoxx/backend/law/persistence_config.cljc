(ns knoxx.backend.law.persistence-config
  "Closed provider admission for independently replaceable application services.")

(defn selection!
  "Validate each independently selected authority before bootstrap opens files."
  [cfg]
  (reduce (fn [result service]
            (let [provider (get cfg service :edn)]
              (when-not (contains? (if (= service :mailbox-provider) #{:edn} #{:edn :mongodb}) provider)
                (throw (ex-info "Unknown persistence provider"
                                {:code "persistence_provider_unknown"
                                 :service service :provider provider})))
              (assoc result service provider)))
          {} [:run-provider :thread-provider :cache-provider :mcp-oauth-provider :mailbox-provider]))

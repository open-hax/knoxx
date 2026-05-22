(ns knoxx.backend.infra.routes.contracts
  "Compatibility facade for legacy /contracts route callers.

   New runtime wiring should require knoxx.backend.infra.routes.resources.
   This namespace delegates to the resource route implementation and preserves
   old function names while clients migrate."
  (:require [knoxx.backend.infra.routes.resources :as resources]))

(defn sync-resource-index!
  [config]
  (resources/sync-resource-index! config))

(defn sync-contract-index!
  [config]
  (resources/sync-resource-index! config))

(defn start-resource-watcher!
  [config]
  (resources/start-resource-watcher! config))

(defn start-contract-watcher!
  [config]
  (resources/start-resource-watcher! config))

(defn stop-resource-watcher!
  []
  (resources/stop-resource-watcher!))

(defn stop-contract-watcher!
  []
  (resources/stop-resource-watcher!))

(defn handle-list-resources
  [do-json config resource-kind]
  (resources/handle-list-resources do-json config resource-kind))

(defn handle-list-contracts
  [do-json config contract-class]
  (resources/handle-list-contracts do-json config contract-class))

(defn validate-contract-edn
  [contract-class edn-text]
  (resources/validate-contract-edn contract-class edn-text))

(defn register-resource-routes!
  [app runtime config helpers]
  (resources/register-resource-routes! app runtime config helpers))

(defn register-contracts-routes!
  [app runtime config helpers]
  (resources/register-resource-routes! app runtime config helpers))

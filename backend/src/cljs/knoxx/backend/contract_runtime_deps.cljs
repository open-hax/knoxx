(ns knoxx.backend.contract-runtime-deps
  "Wires Knoxx-specific implementations into the katamorph dependency
   injection system. This is the single integration point between Knoxx's
   deployment logic and the reusable katamorph contract runtime."
  (:require [knoxx.backend.domain.action.registry :as action-registry]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
            [knoxx.backend.domain.filter.registry :as filter-registry]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.infra.store.registry :as store-registry]))

(defn build-deps
  "Build the :contract-runtime/deps map for injection into config."
  []
  {:run-action!    (fn [ctx action] (action-registry/run-action! ctx action))
   :get-action     (fn [kind] (action-registry/get-action kind))
   :get-scope-declaration (fn [kind] (action-registry/get-scope-declaration kind))
   :filter-fn      (fn [filter-id] (filter-registry/filter-fn filter-id))
   :load-resources (fn [config] (resources/load-all-resources-sync config))
   :get-store      (fn [config store-id] (store-registry/get-store! config store-id))
   :list-resource-ids (fn [config resource-kind] (resources/list-resource-ids-sync config resource-kind))
   :get-resource   (fn [config resource-kind resource-id] (resources/resource-record-sync config resource-kind resource-id))
   :resource-class (fn [resource-kind] (resources/resource-class resource-kind))})

(defn inject-deps!
  "Inject the :contract-runtime/deps map katamorph reads into the runtime
   config. Call this during bootstrap to wire the contract runtime."
  [config]
  (assoc config :contract-runtime/deps (build-deps)))

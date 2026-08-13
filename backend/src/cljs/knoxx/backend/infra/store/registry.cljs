(ns knoxx.backend.infra.store.registry
  "Store registry: delegates to the katamorph store registry.

   This is a thin wrapper that provides backward compatibility for existing
   Knoxx callers. The actual implementation lives in the extracted
   katamorph package.

   The config map must contain :contract-runtime/deps (see
   knoxx.backend.contract-runtime-deps/build-deps)."
  (:require [katamorph.store.registry :as core-registry]))

(defn register-store!
  "Register a store instance under its qualified id. Returns the store."
  [store-id store]
  (core-registry/register-store! store-id store))

(defn registered-store
  "Return the registered store instance for an id, or nil."
  [store-id]
  (core-registry/registered-store store-id))

(defn store-ids
  "Return all registered store ids."
  []
  (core-registry/store-ids))

(defn reset-stores!
  "Drop all registered store instances. Test escape hatch."
  []
  (core-registry/reset-stores!))

(defn get-store!
  "Resolve a store instance by id, instantiating a memory-backed store from
   its resource definition on first use. Returns nil when no store resource
   declares the id."
  [config store-id]
  (core-registry/get-store! config store-id))

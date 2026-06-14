(ns knoxx.backend.infra.store.mongo
  "MongoDB-backed IStore.

   MongoCollection wraps an injected native collection handle (see
   knoxx.backend.extern.mongo) — Knoxx does not own a Mongo client; whatever
   runtime provides the handle decides connection lifecycle. Documents are
   schema-guarded before they cross the boundary."
  (:require [knoxx.backend.extern.mongo :as mongo-extern]
            [open-hax.contract-runtime.store.protocol :as store]
            [open-hax.contract-runtime.store.law :as store-law]))

(defrecord MongoCollection [store-id guard collection-handle]
  store/IStore
  (-insert [_ doc]
    (try
      (mongo-extern/insert-one! collection-handle (guard doc))
      (catch :default err
        (js/Promise.reject err))))
  (-find [_ query]
    (mongo-extern/find-docs! collection-handle (or query {})))

  IFn
  (-invoke [this query] (store/-find this query)))

(defn mongo-collection
  "Build a MongoCollection store from a store resource definition and a native
   collection handle."
  [{:store/keys [id schema]} collection-handle]
  (->MongoCollection id (store-law/compile-schema-guard schema) collection-handle))

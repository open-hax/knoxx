(ns knoxx.backend.infra.store.registry
  "Store registry: resolve store resource definitions into IStore instances.

   Stores are declared by resources (:store/id + :store/schema) and requested
   by actions through :action/scope {:stores [...]}. Instances are cached by
   qualified id. Explicit registration wins — that is how database-backed
   stores (e.g. MongoCollection with an injected handle) replace the default
   memory backend."
  (:require [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.infra.store.memory :as memory]))

(defonce ^:private stores* (atom {}))

(defn register-store!
  "Register a store instance under its qualified id. Returns the store."
  [store-id store]
  (swap! stores* assoc store-id store)
  store)

(defn registered-store
  "Return the registered store instance for an id, or nil."
  [store-id]
  (get @stores* store-id))

(defn store-ids
  "Return all registered store ids."
  []
  (vec (sort (keys @stores*))))

(defn reset-stores!
  "Drop all registered store instances. Test escape hatch."
  []
  (reset! stores* {}))

(defn- id-str
  [store-id]
  (cond
    (keyword? store-id) (subs (str store-id) 1)
    (some? store-id) (str store-id)))

(defn- store-definition
  [config store-id]
  (->> (resources/load-all-resources-sync config)
       (filter #(= :store (:resource/kind %)))
       (map :resource/definition)
       (some (fn [definition]
               (when (or (= store-id (:resource/qualified-id definition))
                         (= store-id (:store/id definition))
                         (= (id-str store-id) (:contract/id definition)))
                 definition)))))

(defn get-store!
  "Resolve a store instance by id, instantiating a memory-backed store from
   its resource definition on first use. Returns nil when no store resource
   declares the id."
  [config store-id]
  (or (registered-store store-id)
      (when-let [definition (store-definition config store-id)]
        (register-store! store-id (memory/memory-collection definition)))))

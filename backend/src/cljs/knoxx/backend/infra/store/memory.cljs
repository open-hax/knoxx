(ns knoxx.backend.infra.store.memory
  "Process-local IStore backend.

   The default store backend when no database collection handle is configured.
   Documents live in an atom in insertion order; queries are field-equality
   matches with an optional :limit."
  (:require [knoxx.backend.infra.store.protocol :as store]
            [knoxx.backend.law.store :as store-law]))

(defn- matches-query?
  [doc query]
  (every? (fn [[field expected]] (= expected (get doc field)))
          (dissoc query :limit)))

(defn- find-in-docs
  [docs query]
  (let [matched (filterv #(matches-query? % query) docs)]
    (if-let [limit (:limit query)]
      (vec (take limit matched))
      matched)))

(defrecord MemoryCollection [store-id guard docs*]
  store/IStore
  (-insert [_ doc]
    (try
      (let [guarded (guard doc)]
        (swap! docs* conj guarded)
        (js/Promise.resolve guarded))
      (catch :default err
        (js/Promise.reject err))))
  (-find [_ query]
    (js/Promise.resolve (find-in-docs @docs* (or query {}))))

  IFn
  (-invoke [this query] (store/-find this query)))

(defn memory-collection
  "Build a MemoryCollection store from a store resource definition."
  [{:store/keys [id schema]}]
  (->MemoryCollection id (store-law/compile-schema-guard schema) (atom [])))

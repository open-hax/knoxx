(ns knoxx.backend.infra.store.protocol
  "IStore: keyed persistence with schemas.

   Stores are declared by resources via :store/id and :store/schema and
   resolved into action scope as instances. Both operations return Promises so
   memory- and database-backed stores share one calling convention. Store
   instances are also callable: (store query) is shorthand for find-docs."
  (:refer-clojure :exclude [-find]))

(defprotocol IStore
  (-insert [this doc] "Insert one schema-guarded document. Returns Promise<doc>.")
  (-find [this query] "Find documents matching a field-equality query map.
                       The :limit key caps the result count. Returns Promise<vector>."))

(defn insert!
  "Insert a document into a store. Returns Promise<doc>."
  [store doc]
  (-insert store doc))

(defn find-docs
  "Query a store. Query is a map of field -> expected value; :limit caps the
   result count. Returns Promise<vector<doc>>."
  [store query]
  (-find store query))

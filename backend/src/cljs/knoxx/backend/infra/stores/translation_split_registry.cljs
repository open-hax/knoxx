(ns knoxx.backend.infra.stores.translation-split-registry
  "Holds durable split-turn persistence without coupling bootstrap to routes.

  Nil is a refusal, never an in-memory fallback: provider work whose canonical
  split claim disappears on restart cannot be reviewed or safely composed.")

(defonce store* (atom nil))

(defn current
  "The durable translation split store, or nil when persistence is absent."
  []
  @store*)

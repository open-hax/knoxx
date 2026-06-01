(ns knoxx.backend.domain.graph.policy-registry
  "Registry for graph expansion policies.

   Holds the named policies the runtime may swap between without changing
   the agent-facing graph query contract. Query handlers resolve a policy
   by id (defaulting to `:default`) and call the IGraphExpansionPolicy
   methods to bound their parameters.

   The registry is initialized with the default policy on namespace load
   and again explicitly at startup via `init!`, so handlers always have a
   `:default` policy available even before bootstrap runs."
  (:require [knoxx.backend.domain.graph.expansion-policy :as expansion-policy]))

(defonce ^:private registry* (atom {}))

(def default-policy-id :default)

(defn register-policy!
  "Register a graph expansion policy under a keyword id.

   The policy must satisfy IGraphExpansionPolicy. Returns the id."
  [id policy]
  (let [pid (expansion-policy/normalize-policy-id id)]
    (when-not pid
      (throw (ex-info "Invalid graph expansion policy id" {:id id})))
    (when-not (expansion-policy/expansion-policy? policy)
      (throw (ex-info "Value does not satisfy IGraphExpansionPolicy" {:id pid})))
    (swap! registry* assoc pid policy)
    pid))

(defn get-policy
  "Look up a registered policy by id, falling back to the default policy.

   Always returns a policy satisfying IGraphExpansionPolicy: when neither
   the requested id nor `:default` is registered, a fresh default policy
   is returned so callers never need a nil guard."
  ([] (get-policy default-policy-id))
  ([id]
   (let [reg @registry*
         pid (expansion-policy/normalize-policy-id id)]
     (or (get reg pid)
         (get reg default-policy-id)
         (expansion-policy/default-expansion-policy)))))

(defn policy-ids
  "Return all registered policy ids."
  []
  (keys @registry*))

(defn init!
  "Initialize the registry with the default expansion policy.

   Idempotent and safe to call at every startup / hot reload. Returns the
   default policy id so callers (and the load-time guard) have a value."
  []
  (register-policy! default-policy-id (expansion-policy/default-expansion-policy)))

;; Ensure a default policy exists at namespace load so query handlers that
;; resolve `:default` work even before bootstrap explicitly calls `init!`.
(defonce load-time-default-policy-id
  (init!))

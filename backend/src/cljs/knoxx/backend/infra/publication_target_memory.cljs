(ns knoxx.backend.infra.publication-target-memory
  "In-memory `IPublicationTarget` and `IIdempotencyStore`.

  Implements the same protocols a production adapter does, so the whole seam —
  desired resources, pure planning, effects, observed receipts — can be proven
  with no network, no database, and no hosted publishing backend present.

  Routes are a map keyed by path, so two simultaneously public routes for one
  publication would be *observable* rather than assumed away. The store honours
  the same atomic reservation contract production must: a single swap claims the
  key, with no await between reading it and claiming it."
  (:require [knoxx.backend.infra.publication-effects :as effects]))

(defn memory-store
  []
  (let [state (atom {})]
    {:state state
     :store (reify effects/IIdempotencyStore
              (reserve! [_ idempotency-key]
                (if-let [entry (get @state idempotency-key)]
                  (if (:receipt entry)
                    {:reservation/status :done :receipt (:receipt entry)}
                    {:reservation/status :in-flight})
                  (do (swap! state assoc idempotency-key {:claimed? true})
                      {:reservation/status :reserved})))
              (complete! [_ idempotency-key receipt]
                (swap! state assoc idempotency-key {:receipt receipt}))
              (release! [_ idempotency-key]
                (swap! state dissoc idempotency-key)))}))

(defn- materialization
  [adapter-id op]
  (let [intent (:intent op)
        path (:publication/path intent)
        revision (:concrete-revision op)]
    {:receipt/type :publication/materialized
     :publication/id (:publication/id intent)
     :adapter/id adapter-id
     :idempotency/key (:idempotency/key op)
     :document/id (:publication/document intent)
     :target (:publication/garden intent)
     :locale (:publication/locale intent)
     :revision revision
     :path path
     :materialized/revision revision
     :materialized/path path}))

(defn- route-for
  "Observation keyed by publication IDENTITY, not by the desired path. Keying on
   path means that after a path move the caller cannot see the route it is
   replacing, `:previous` comes back nil, and both routes stay public — which is
   exactly the outcome this boundary exists to prevent."
  [routes intent]
  (->> (vals @routes)
       (filter #(= (:publication/id %) (:publication/id intent)))
       first))

(defn- record-route!
  "Materialize `op`, replacing the prior route rather than leaving it public
   alongside the new one."
  [routes adapter-id op]
  (let [receipt (materialization adapter-id op)]
    (swap! routes (fn [current]
                    (-> current
                        (dissoc (get-in op [:previous :materialized/path]))
                        (assoc (:materialized/path receipt) receipt))))
    receipt))

(defn memory-target
  "An in-memory publication target.

   `:fail?` makes every publish throw, for proving that failure surfaces as
   drift without mutating desired state."
  ([] (memory-target {}))
  ([{:keys [id fail?] :or {id :memory/target}}]
   (let [routes (atom {})
         publish-count (atom 0)]
     {:routes routes
      :publish-count publish-count
      :target
      (reify effects/IPublicationTarget
        (target-id [_] id)
        (publish! [_ _ctx op]
          (when fail?
            (throw (ex-info "memory target publish failed" {})))
          (swap! publish-count inc)
          (js/Promise.resolve (record-route! routes id op)))
        (remove! [_ _ctx intent observed]
          (swap! routes dissoc (:materialized/path observed))
          ;; `:publication/id` is what `observed-for` filters on. Without it the
          ;; removal is invisible to the projection, so a publish-then-remove
          ;; history still reports the old route as materialized — and a later
          ;; republish of the same revision reads as `:noop`, leaving nothing
          ;; public while the system believes it converged.
          (js/Promise.resolve {:receipt/type :publication/removed
                               :publication/id (:publication/id intent)
                               :removed/path (:materialized/path observed)}))
        (observe! [_ _ctx intent]
          (js/Promise.resolve (route-for routes intent))))})))

(defn public-routes
  [target-bundle]
  @(:routes target-bundle))

(defn materialization-count
  [target-bundle]
  @(:publish-count target-bundle))

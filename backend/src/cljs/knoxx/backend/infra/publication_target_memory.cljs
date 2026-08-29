(ns knoxx.backend.infra.publication-target-memory
  "In-memory `IPublicationTarget` and `IIdempotencyStore`.

  Implements the same protocols a production adapter does, so the whole seam —
  desired resources, pure planning, effects, observed receipts — can be proven
  with no network, no database, and no hosted publishing backend present.

  Routes are a map keyed by path, so two simultaneously public routes for one
  publication would be *observable* rather than assumed away. The store honours
  the same atomic reservation contract production must: a single swap claims the
  key, with no await between reading it and claiming it."
  (:require [knoxx.backend.domain.publication-plan :as plan]
            [knoxx.backend.infra.publication-effects :as effects]
            [knoxx.backend.law.publication-receipts :as law]))

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
    ;; The drift keys come from `plan/desired-materialization` rather than being
    ;; spelled again here. Three places independently computed "the materialized
    ;; facts" — the planner's desired state, this adapter's receipt, and the
    ;; static-site target's observation — so a key added to one drifted silently
    ;; against the others and convergence compared maps that could never be
    ;; equal. Reusing the planner's own function means this adapter cannot
    ;; disagree with the thing that will compare against it.
    (merge {:receipt/type :publication/materialized
            :publication/id (:publication/id intent)
            :adapter/id adapter-id
            :idempotency/key (:idempotency/key op)
            :document/id (:publication/document intent)
            :target (:publication/garden intent)
            :locale (:publication/locale intent)
            :revision revision
            :path path}
           (plan/desired-materialization intent revision))))

(defn- route-for
  "Observation keyed by publication IDENTITY, not by the desired path. Keying on
   path means that after a path move the caller cannot see the route it is
   replacing, `:previous` comes back nil, and both routes stay public — which is
   exactly the outcome this boundary exists to prevent."
  [routes intent]
  (->> (vals @routes)
       (filter #(= (:publication/id %) (:publication/id intent)))
       first))

(defn- assert-publishable!
  "An adapter validates what it is handed rather than trusting its caller.

   `publish!` is a protocol method, reachable without going through
   `execute-plan!`, so the boundary's check is not this adapter's check. Storing
   the artifact unexamined is what made a malformed one invisible: it became a
   *served* route while every assertion about the materialization — all of which
   read receipt metadata — stayed green. A test double that accepts anything
   proves the seam against a payload no real adapter could write.

   Deliberately the same `law` call the boundary makes, not a looser copy: an
   adapter that admitted more than the boundary would be the one place a
   contradiction could live."
  [op]
  (law/assert-artifact! (:artifact op) (:intent op) (:concrete-revision op)))

(defn- record-route!
  "Materialize `op`, replacing the prior route rather than leaving it public
   alongside the new one.

   The route stores the artifact alongside the receipt metadata, so what is
   *served* is observable and not merely asserted. Without it the artifact was
   dropped on the floor: a caller could corrupt or omit the published body and
   every assertion about the materialization still passed, because they all read
   receipt metadata. The returned receipt is unchanged — an artifact is content,
   not evidence, and receipts carry evidence.

   The artifact is stored EXACTLY as handed over. This adapter transports; it
   does not render, re-encode, or fill anything in.

   `publish-count` is bumped here rather than at the call site so both pieces of
   evidence that a materialization happened — the route and the count — are owned
   by the one function that can only reach them after the artifact was accepted.
   Counted before validation, a refused artifact reads as a materialization that
   occurred, which is the exact claim this adapter exists to make checkable."
  [routes publish-count adapter-id op]
  (assert-publishable! op)
  (let [receipt (materialization adapter-id op)]
    (swap! routes (fn [current]
                    (-> current
                        (dissoc (get-in op [:previous :materialized/path]))
                        (assoc (:materialized/path receipt)
                               (assoc receipt :route/artifact (:artifact op))))))
    (swap! publish-count inc)
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
          (js/Promise.resolve (record-route! routes publish-count id op)))
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

(defn served-artifact
  "What the public route at `path` actually serves, or nil."
  [target-bundle path]
  (get-in @(:routes target-bundle) [path :route/artifact]))

(defn materialization-count
  [target-bundle]
  @(:publish-count target-bundle))

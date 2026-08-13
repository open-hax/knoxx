(ns knoxx.backend.domain.publication-plan
  "Pure reconciliation: desired publication intent versus observed facts.

  Emits `:publish`, `:remove`, `:noop`, or `:blocked` and performs no I/O. The
  effect layer executes a plan; it never re-derives one. No adapter, transport,
  or store type appears anywhere in this namespace.

  Two orderings carry all the semantic risk, so they are explicit here rather
  than emergent:

  1. Non-public state and garden archive are decided BEFORE translation and
     review blockers. Removal must never be blocked by missing translation
     evidence — an archived publication whose translation was never finished
     still has to come down.
  2. The concrete revision comes from one `publication-evidence` result and is
     never re-resolved. The revision the gate checked evidence against is the
     revision the plan materializes."
  (:require [knoxx.backend.domain.publication-gate :as gate]
            [knoxx.backend.domain.publication-receipts :as receipts]))

(def non-public-states
  "States that can only remove or no-op. `:withheld` means \"deliberately not
   public\"; `:archived` means \"terminal\". Neither can publish."
  #{:withheld :archived})

(defn desired-materialization
  [intent revision]
  {:materialized/revision revision
   :materialized/path (:publication/path intent)})

(defn- observed-materialization
  "Compared against `desired-materialization` using the key set named by
   `receipts/drift-keys`, so the planner and the receipt projection cannot
   diverge on what convergence means."
  [observed]
  (select-keys observed receipts/drift-keys))

(defn- takedown
  "Removal when something is materialized, otherwise nothing to do. Never
   `:blocked` — translation evidence has no bearing on taking a route down."
  [intent observed reason]
  {:op (if observed :remove :noop)
   :reason reason
   :intent intent
   :observed observed})

(defn- converge
  "Decide between blocked, noop, and publish for a publicly-intended
   publication, using the revision the gate already resolved."
  [intent observed {:keys [concrete-revision blockers]}]
  (if (seq blockers)
    {:op :blocked
     :intent intent
     :blockers blockers
     :concrete-revision concrete-revision}
    (let [desired (desired-materialization intent concrete-revision)]
      (if (= desired (observed-materialization observed))
        {:op :noop
         :intent intent
         :desired desired
         :concrete-revision concrete-revision}
        ;; `:previous` is carried so the effect layer can remove a stale route
        ;; rather than orphaning it alongside the new one.
        {:op :publish
         :intent intent
         :desired desired
         :previous observed
         :concrete-revision concrete-revision}))))

(defn reconcile-plan
  "The plan for one intent. Deterministic in `resource-index`, `intent`, and
   `facts`."
  [resource-index intent facts]
  (let [garden (get-in resource-index [:gardens (:publication/garden intent)])
        observed ((:materialized-publication facts) intent)]
    (cond
      (contains? non-public-states (:publication/state intent))
      (takedown intent observed :publication-not-public)

      (not= :active (:garden/status garden))
      (takedown intent observed :garden-not-active)

      :else
      (converge intent observed (gate/publication-evidence intent facts)))))

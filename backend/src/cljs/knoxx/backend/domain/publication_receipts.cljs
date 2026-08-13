(ns knoxx.backend.domain.publication-receipts
  "Observed publication facts and the projection reconciliation reads.

  Receipts describe what an effect *did*. They are never desired-state
  authority, and desired state never carries them. This namespace holds only
  shapes and pure projections — the effect protocol and its in-memory
  implementation live in `infra.*`, because a domain namespace that also owned an
  adapter would blur exactly the boundary this card exists to prove.

  `law.publication-receipts` carries the *minimum* an adapter must return at the
  effect boundary. The shape here is the *full observation* used for drift
  analysis and deploy verification: it additionally pins which publication, on
  which adapter, under which key, so a receipt is explainable without joining it
  back to anything."
  (:require [knoxx.backend.law.publication :as publication]))

(def ConcreteRevision
  "A materialization receipt records what actually shipped, so a selector token
   like `:source/current` can never appear — only a concrete revision."
  publication/NonBlankString)

(def PublicationMaterializedReceipt
  [:map
   [:receipt/type [:= :publication/materialized]]
   [:publication/id :qualified-keyword]
   [:adapter/id :keyword]
   [:idempotency/key publication/NonBlankString]
   [:document/id :qualified-keyword]
   [:target :qualified-keyword]
   [:locale publication/Locale]
   [:revision ConcreteRevision]
   [:path publication/PublicationPath]
   [:materialized/revision ConcreteRevision]
   [:materialized/path publication/PublicationPath]])

(def drift-keys
  "The exact fields the planner compares to decide convergence. Named here and
   consumed by both the planner and this projection so the two cannot drift
   apart — a projection that returned a different key set would make every
   comparison silently fail."
  [:materialized/revision :materialized/path])

(defn materialized?
  [receipt]
  (= :publication/materialized (:receipt/type receipt)))

(defn observed-materialization
  "The observation the planner compares, or nil for any receipt that is not a
   successful materialization. A blocked or failed receipt must never be
   mistaken for something being public."
  [receipt]
  (when (materialized? receipt)
    (select-keys receipt drift-keys)))

(defn observed-for
  "Observation for one publication id, from a collection of receipts. Later
   receipts win, so a removal after a publish leaves nothing observed."
  [receipts publication-id]
  (->> receipts
       (filter #(= publication-id (:publication/id %)))
       (reduce (fn [_observed receipt]
                 (observed-materialization receipt))
               nil)))

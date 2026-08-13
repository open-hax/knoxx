(ns knoxx.backend.law.publication-receipts
  "Contracts at the publication effect boundary.

  Both directions are named. The plan handed to the effect layer is validated
  before any effect runs, and whatever the adapter hands back is validated
  before a caller reads fields off it — an adapter is replaceable, so its return
  value is untrusted input, not a promise.

  Receipts are *observed execution facts*. They never carry desired state, and
  desired state never carries them."
  (:require [knoxx.backend.law.publication :as publication]))

;; ── Plans entering the effect layer ────────────────────────────────────────

(def PlanOp
  [:enum :publish :remove :noop :blocked])

(def PublicationPlan
  "The pure planner's output. `:publish` must carry a concrete revision — an
   effect keyed by a nil revision is not idempotent and not reproducible."
  [:and
   [:map
    [:op PlanOp]
    [:intent {:optional true} :map]
    [:desired {:optional true} [:maybe :map]]
    [:previous {:optional true} [:maybe :map]]
    [:observed {:optional true} [:maybe :map]]
    [:reason {:optional true} :keyword]
    [:blockers {:optional true} [:vector :keyword]]
    [:concrete-revision {:optional true} [:maybe publication/PublicationRevision]]]
   [:fn {:error/message "a :publish plan requires a concrete revision and an intent"}
    (fn [plan]
      (or (not= :publish (:op plan))
          (and (some? (:concrete-revision plan))
               (map? (:intent plan)))))]])

;; ── Receipts leaving the effect layer ──────────────────────────────────────

(def MaterializedReceipt
  [:map
   [:receipt/type [:= :publication/materialized]]
   [:materialized/revision publication/PublicationRevision]
   [:materialized/path publication/PublicationPath]
   [:idempotency/key publication/NonBlankString]])

(def RemovedReceipt
  [:map
   [:receipt/type [:= :publication/removed]]
   [:removed/path {:optional true} [:maybe :string]]])

(def NoopReceipt
  [:map
   [:receipt/type [:= :publication/noop]]
   [:reason {:optional true} [:maybe :keyword]]])

(def BlockedReceipt
  [:map
   [:receipt/type [:= :publication/blocked]]
   [:blockers [:vector :keyword]]])

(def FailedReceipt
  "Adapter failure is evidence, not an exception the caller must catch. Drift is
   reported so reconciliation can see desired and observed disagree."
  [:map
   [:receipt/type [:= :publication/failed]]
   [:failure/reason publication/NonBlankString]
   [:failure/drift? [:= true]]
   [:idempotency/key {:optional true} [:maybe :string]]])

(def Receipt
  [:multi {:dispatch :receipt/type}
   [:publication/materialized MaterializedReceipt]
   [:publication/removed RemovedReceipt]
   [:publication/noop NoopReceipt]
   [:publication/blocked BlockedReceipt]
   [:publication/failed FailedReceipt]])

(defn assert-plan!
  [plan]
  (publication/assert-valid! :publication/plan PublicationPlan plan))

(defn assert-receipt!
  [receipt]
  (publication/assert-valid! :publication/receipt Receipt receipt))

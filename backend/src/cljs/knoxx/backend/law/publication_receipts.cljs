(ns knoxx.backend.law.publication-receipts
  "Contracts at the publication effect boundary.

  Both directions are named. The plan handed to the effect layer is validated
  before any effect runs, and whatever the adapter hands back is validated
  before a caller reads fields off it — an adapter is replaceable, so its return
  value is untrusted input, not a promise.

  Receipts are *observed execution facts*. They never carry desired state, and
  desired state never carries them."
  (:require [knoxx.backend.law.publication :as publication]))

(def ConcreteRevision
  "Re-exported so the effect boundary depends on one law namespace. Anything the
   effect layer keys by a revision needs this rather than the selector-admitting
   `PublicationRevision`."
  publication/ConcreteRevision)

(def PublicationArtifact
  "Re-exported for the same reason as `ConcreteRevision`: the effect boundary
   depends on one law namespace. Declared in `law.publication` — see the note
   above it recording that the artifact is produced ABOVE this boundary."
  publication/PublicationArtifact)

(def ArtifactRevisionConflict
  publication/ArtifactRevisionConflict)

(def ArtifactLocaleConflict
  "Re-exported locale-identity conflict so the effect boundary carries all
   publication artifact refusal evidence through one law namespace."
  publication/ArtifactLocaleConflict)

(def PublicationArtifactConflict
  "Any typed conflict that can prevent an artifact from being materialized."
  [:or ArtifactRevisionConflict ArtifactLocaleConflict])

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
    [:concrete-revision {:optional true} [:maybe publication/ConcreteRevision]]]
   [:fn {:error/message "a :publish plan requires a concrete revision and an intent"}
    (fn [plan]
      (or (not= :publish (:op plan))
          (and (some? (:concrete-revision plan))
               (map? (:intent plan)))))]
   [:fn {:error/message "a :remove plan requires an intent and the observed materialization"}
    ;; `remove!` takes both. Left optional, a `{:op :remove}` plan validated and
    ;; then called the adapter with two nils — malformed boundary input reaching
    ;; an effect, which is the one thing this contract exists to stop.
    (fn [plan]
      (or (not= :remove (:op plan))
          (and (map? (:intent plan))
               (map? (:observed plan)))))]])

;; ── Receipts leaving the effect layer ──────────────────────────────────────

(def MaterializedReceipt
  "A revision that was actually materialized is necessarily concrete: the
   selector was resolved before any effect ran, so a receipt carrying
   `:source/current` would be recording a moving target as an accomplished fact."
  [:map
   [:receipt/type [:= :publication/materialized]]
   [:materialized/revision publication/ConcreteRevision]
   [:materialized/path publication/PublicationPath]
   ;; Optional and maybe-nil: a receipt predating titles has none, and a
   ;; document may legitimately carry a blank one. Required here would make
   ;; every historical receipt unreadable.
   [:materialized/title {:optional true} [:maybe :string]]
   [:idempotency/key publication/NonBlankString]])

(def RemovedReceipt
  "`:publication/id` is required, not incidental: the observation projection
   filters receipts by it, so a removal without one is silently dropped and the
   materialization it was supposed to retract stays observed. Requiring it here
   means an adapter cannot forget."
  [:map
   [:receipt/type [:= :publication/removed]]
   [:publication/id :qualified-keyword]
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
   reported so reconciliation can see desired and observed disagree.

   `:failure/conflict` carries a *structured* refusal rather than only the
   message string `:failure/reason` already holds. An artifact-revision conflict
   is the one failure where the reader has to know which side was stale, so both
   revisions travel on the receipt; flattened into prose they are unrecoverable
   by anything but a human reading logs."
  [:map
   [:receipt/type [:= :publication/failed]]
   [:failure/reason publication/NonBlankString]
   [:failure/drift? [:= true]]
   [:idempotency/key {:optional true} [:maybe :string]]
    [:failure/conflict {:optional true} [:maybe PublicationArtifactConflict]]])

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

;; ── The artifact entering the effect layer ─────────────────────────────────

(defn assert-artifact!
  "The other half of \"both directions\": the artifact is validated on the way
   IN, exactly as the adapter's receipt is validated on the way OUT.

   Called before the idempotency key is derived and before the store is touched,
   so a publication that cannot lawfully happen leaves no reservation behind for
   a later attempt to reconcile."
  ([artifact concrete-revision]
   (publication/assert-artifact! artifact concrete-revision))
  ([artifact intent concrete-revision]
   (publication/assert-artifact! artifact intent concrete-revision)))

(defn artifact-revision-conflict?
  "True when a thrown `ex-data` is the typed artifact-revision conflict, so the
   boundary can put it on the failure receipt instead of losing both revisions
   inside a message string."
  [value]
  (publication/artifact-revision-conflict? value))

(defn artifact-locale-conflict?
  "True when a thrown `ex-data` is the typed artifact-locale conflict, so the
   boundary preserves both locale values on the failed receipt."
  [value]
  (publication/artifact-locale-conflict? value))

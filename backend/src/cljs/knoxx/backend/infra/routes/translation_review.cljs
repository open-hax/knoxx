(ns knoxx.backend.infra.routes.translation-review
  "Translation approval facade.

  One operation: record that an authorized principal accepted one translated
  revision. Everything in and out is CLJS data — no Fastify handle enters or
  leaves this namespace. The owning extern adapter is
  `knoxx.backend.extern.fastify.translation-review`.

  Approval is evidence, not an effect. Nothing here materializes anything, and
  the card is explicit about why: an approval may make a publication plan
  admissible, but it must not itself publish. That separation is what lets a
  reviewer accept a translation without also deciding when the bytes go live."
  (:require [knoxx.backend.domain.translation-evidence :as evidence-domain]
            [knoxx.backend.domain.translation-review-inventory :as inventory]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-evidence :as law]
            [promesa.core :as p]))

(defn- ^:async current-receipt!
  "The completed translation this request is about, or nil.

   Loads the receipts and indexes them through
   `domain.translation-evidence/evidence` rather than issuing a targeted query.
   That is more work than one lookup and it is deliberate: which receipt is
   *current* for a source revision is decided by comparing timestamps, and
   pushing that rule into a store query would duplicate it — one copy per store
   implementation, each free to drift from the copy the gate uses. An approval
   validated against a different receipt than the gate will later read is the one
   failure this card exists to prevent, so the rule stays in one place.

   Scoped to the acting tenant and project before indexing. A reviewer must not
   be able to approve a translation belonging to another organization, and the
   dispatch card learned that lesson at the cost of two review rounds."
  [evidence-store scope request receipt-admissible? receipts-transform
   receipts-snapshot]
  (let [query-scope (select-keys scope [:org-id :project])
        ;; Scoped in the query, and checked again here. The query is the
        ;; optimization; the filter is the guarantee, since a replaceable store
        ;; that ignored the scope must not be able to widen what a reviewer can
        ;; approve.
        receipts (->> (if (some? receipts-snapshot)
                        receipts-snapshot
                        (await (store/completed-translations!
                                evidence-store query-scope)))
                      ((or receipts-transform identity))
                      (filterv #(and (= (:org-id scope) (:translation/org-id %))
                                     (= (:project scope) (:translation/project %))
                                     (or (nil? receipt-admissible?)
                                         (receipt-admissible? %)))))
        evidence (evidence-domain/evidence {:receipts receipts})]
    (evidence-domain/receipt-for evidence
                                 (:review/document request)
                                 (:review/garden request)
                                 (:review/locale request)
                                 (:review/revision request))))

(defn ^:async approve-translation!
  "Record approval of one translated revision, or refuse with typed evidence.

   Returns one of:

     {:approval/status :recorded :approval a}   first approval of this output
     {:approval/status :existing :approval a}   already approved, idempotent
     {:approval/refusal r}                      refused, with both sides named

   The request is validated, then the receipt is looked up, then the refusal is
   computed, and only then is anything written. Nothing is persisted on a
   refusal: an approval that was rejected must leave no trace a later read could
   mistake for evidence."
  [{:keys [evidence-store clock authorize-approval! receipt-admissible?
           receipts-transform receipts-snapshot]}
   scope request]
  (when-not (fn? authorize-approval!)
    (throw (ex-info "translation approval requires content admission"
                    {:status 503
                     :code "translation_approval_admission_unavailable"})))
  (let [checked (law/assert-approval-request! request)
        receipt (await (current-receipt! evidence-store scope checked
                                         receipt-admissible? receipts-transform
                                         receipts-snapshot))]
    (if-let [refusal (law/approval-refusal checked receipt)]
      {:approval/refusal refusal}
      (do
        ;; The HTTP adapter supplies the resource/content admission guard. It is
        ;; invoked only after the receipt law has accepted the exact immutable
        ;; coordinates, and before an approval can be persisted. Keeping it as
        ;; an injected boundary makes direct law/facade callers testable while
        ;; preventing a hand-written POST from bypassing the bytes the reviewer
        ;; was required to see.
        (await (authorize-approval! checked receipt))
        (await (store/record-approval!
                evidence-store
                (law/approve checked receipt (:principal scope) (clock))))))))

(defn- scoped-receipts
  [scope receipts]
  (filterv #(and (= (:org-id scope) (:translation/org-id %))
                 (= (:project scope) (:translation/project %)))
           receipts))

(defn- scoped-approvals
  [scope approvals]
  (filterv #(and (= (:org-id scope) (:review/org-id %))
                 (= (:project scope) (:review/project %)))
           approvals))

(defn- dispatch-matches-work?
  "Whether a point-read record's body names the work encoded by its lookup key."
  [scope work record]
  (= [(:org-id scope)
      (:project scope)
      (:translation/garden work)
      (:translation/document work)
      (:translation/source-locale work)
      (:translation/locale work)
      (:translation/source-revision work)]
     [(:dispatch/org-id record)
      (:dispatch/project record)
      (:dispatch/garden record)
      (:dispatch/document record)
      (:dispatch/source-locale record)
      (:dispatch/locale record)
      (:dispatch/revision record)]))

(defn- ^:async dispatches-for-work!
  "Point-read dispatch evidence for the already-derived desired work.

   At deployment scale this is bounded by the resource inventory (the observed
   case is eighteen rows) and the reads run concurrently.  It deliberately uses
   the existing atomic dispatch key instead of introducing an incidental Mongo
   migration: legacy dispatch rows do not have tenant/project query columns,
   while the key already includes both coordinates and exactly matches the
   claim boundary."
  [evidence-store scope work]
  (await
   (p/all
    (mapv (fn [item]
            (if-let [dispatch-key (inventory/dispatch-lookup-key scope item)]
              (p/let [record (store/dispatch-for-key! evidence-store dispatch-key)]
                ;; The point lookup is an optimization supplied by a replaceable
                ;; store; the key comparison is the tenant/project guarantee.
                ;; Without it, project-inventory's resource relation (which does
                ;; not repeat tenant coordinates) could attach another scope's
                ;; outcome and diagnostic detail to this row.
                (when (and record
                           (or (not= dispatch-key (:dispatch/key record))
                               (not (dispatch-matches-work? scope item record))))
                  (throw (ex-info "translation dispatch lookup returned the wrong claim"
                                  {:expected-dispatch-key dispatch-key
                                   :actual-dispatch-key (:dispatch/key record)
                                   :expected-work (inventory/work-key item)
                                   :actual-work (inventory/work-key record)})))
                record)
              (js/Promise.resolve nil)))
          work))))

(defn ^:async reviewable-translations!
  "Resource-derived translation work, left-joined with observed evidence.

   Resource intent is cardinality authority. A missing receipt is a visible
   `:missing` row rather than no row, and an orphan receipt cannot invent work.
   This is the inverse of the old receipt-first projection that reduced eighteen
   desired relations to the only completed candidate."
  [{:keys [evidence-store publication-index source-revisions receipts-transform
           receipts-snapshot]} scope]
  (let [query-scope (select-keys scope [:org-id :project])
        work (mapv #(assoc %
                           :translation/org-id (:org-id scope)
                           :translation/project (:project scope))
                   (inventory/desired-work publication-index source-revisions))
        receipts (scoped-receipts
                  scope
                  ((or receipts-transform identity)
                   (if (some? receipts-snapshot)
                     receipts-snapshot
                     (await (store/completed-translations!
                             evidence-store query-scope)))))
        approvals (scoped-approvals
                   scope
                   (await (store/approvals! evidence-store query-scope)))
        dispatches (await (dispatches-for-work! evidence-store scope work))]
    {:project (:project scope)
     :reviews (inventory/project-inventory work receipts approvals dispatches)}))

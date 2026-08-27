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
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-evidence :as law]))

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
  [evidence-store scope request]
  (let [query-scope (select-keys scope [:org-id :project])
        ;; Scoped in the query, and checked again here. The query is the
        ;; optimization; the filter is the guarantee, since a replaceable store
        ;; that ignored the scope must not be able to widen what a reviewer can
        ;; approve.
        receipts (->> (await (store/completed-translations! evidence-store query-scope))
                      (filterv #(and (= (:org-id scope) (:translation/org-id %))
                                     (= (:project scope) (:translation/project %)))))
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
  [{:keys [evidence-store clock]} scope request]
  (let [checked (law/assert-approval-request! request)
        receipt (await (current-receipt! evidence-store scope checked))]
    (if-let [refusal (law/approval-refusal checked receipt)]
      {:approval/refusal refusal}
      (await (store/record-approval!
              evidence-store
              (law/approve checked receipt (:principal scope) (clock)))))))

(defn- current-approval
  [approvals receipt]
  (some #(when (and (law/approval-matches?
                     %
                     (:translation/document receipt)
                     (:translation/garden receipt)
                     (:translation/locale receipt)
                     (:translation/source-revision receipt))
                    (law/approval-current? % receipt))
           %)
        approvals))

(defn reviewable-wire
  "Project one current translation receipt and its current approval status.

   The revisions needed by the approval request cross this read boundary, so
   the frontend never guesses them from mutable segment state."
  [approvals publication-id receipt]
  (let [approval (current-approval approvals receipt)]
    (cond-> {:publication publication-id
             :document (:translation/document receipt)
             :garden (:translation/garden receipt)
             :locale (:translation/locale receipt)
             :revision (:translation/source-revision receipt)
             :translation_revision (:translation/revision receipt)
             :translated_at (:translation/at receipt)
             :approved (boolean approval)}
      approval (assoc :approved_at (:review/at approval)))))

(defn ^:async reviewable-translations!
  "Current completed translations in this tenant/project, with approval state."
  [{:keys [evidence-store publication-index]} scope]
  (let [query-scope (select-keys scope [:org-id :project])
        receipts (->> (await (store/completed-translations! evidence-store query-scope))
                      (filterv #(and (= (:org-id scope) (:translation/org-id %))
                                     (= (:project scope) (:translation/project %))))
                      evidence-domain/index-receipts
                      vals)
        approvals (->> (await (store/approvals! evidence-store query-scope))
                       (filterv #(and (= (:org-id scope) (:review/org-id %))
                                      (= (:project scope) (:review/project %)))))
        publication-id-for
        (fn [receipt]
          (some (fn [intent]
                  (when (and (= (:translation/document receipt)
                                (:publication/document intent))
                             (= (:translation/garden receipt)
                                (:publication/garden intent))
                             (= (:translation/locale receipt)
                                (:publication/locale intent)))
                    (:publication/id intent)))
                (:publications publication-index)))]
    {:reviews (->> receipts
                   (keep #(when-let [publication-id (publication-id-for %)]
                            (reviewable-wire approvals publication-id %)))
                   (sort-by (juxt #(str (:document %))
                                  #(str (:garden %))
                                  #(str (:locale %))))
                   vec)}))

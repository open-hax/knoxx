(ns knoxx.backend.infra.publication-gate-evidence
  "Read and authenticate one scoped publication evidence snapshot."
  (:require [knoxx.backend.domain.translation-evidence :as evidence-domain]
            [knoxx.backend.infra.publication-contract-content :as contract-content]
            [knoxx.backend.infra.publication-source-revision :as source-revision]
            [knoxx.backend.infra.source-review :as source-review]
            [knoxx.backend.infra.translation-candidate-content :as candidate-content]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.infra.translation-split-projection :as split-projection]
            [knoxx.backend.infra.wiki-runtime :as wiki-runtime]))

(defn current-source-locale-receipts
  "Only the receipts whose source locale is still the document's declared one.

   The gate's evidential key is `[document target-locale revision]` — it cannot
   carry a source locale, because a publication intent does not name one. But a
   translation *from* a different source locale is a different translation, and
   the dispatch identity says so by including `:source-locale`.

   Without this filter, changing a document's declared source locale while its
   bytes stay identical left the content digest, the document and the target
   locale all unchanged — so the old receipt satisfied the new intent and the
   retranslation that was genuinely required never happened.

   Filtered here rather than keyed in the domain because the gate's fact
   signature is fixed and the *current* source locale is a property of the
   document, which this layer has and the domain does not."
  [documents receipts]
  (let [declared (into {} (map (juxt :document/id :document/source-locale)) documents)]
    (filterv (fn [receipt]
               (= (:translation/source-locale receipt)
                  (get declared (:translation/document receipt))))
             receipts)))

(defn project-receipts
  "Only the receipts belonging to `project`.

   Translation output is project-scoped — every existing segment, document and
   export route filters by it — so output produced under one project does not
   exist under another. With the project ignored, changing
   `KNOXX_SESSION_PROJECT_NAME` left the durable evidence in place and the new
   project read the old project's receipts as its own.

   A nil active project matches only receipts that also name none, so an unset
   project is its own scope rather than a wildcard over every other."
  [project receipts]
  (filterv #(= project (:translation/project %)) receipts))

(defn tenant-receipts
  "Only the receipts belonging to `org-id`.

   Translation is tenant-scoped: the worker keys segments by organization and
   every document read requires one, so a translation produced for org A does not
   exist for org B. Loading receipts unfiltered made org B's gate report a
   document translated when the segments lived only in org A's tenant — the
   evidence half of the same leak `law.translation-dispatch/dispatch-key` closes
   on the identity half.

   A receipt naming no organization is excluded rather than treated as global.
   Admitting it into every tenant is exactly the failure being fixed."
  [org-id receipts]
  (filterv #(= org-id (:translation/org-id %)) receipts))

(defn- ^:async approvals-for-gate!
  "Load scoped approvals and optionally join current durable split history."
  [evidence-store {:keys [org-id project] :as scope} receipts
   {:keys [enforce-split-review-readiness? split-store digest-hex]}]
  (let [stored (->> (await (store/approvals!
                            evidence-store
                            (select-keys scope [:org-id :project])))
                    (filterv #(and (= org-id (:review/org-id %))
                                   (= project (:review/project %)))))]
    (if enforce-split-review-readiness?
      (await (split-projection/current-review-approvals!
              {:split-store split-store :digest-hex digest-hex}
              receipts stored))
      stored)))

(declare gate-evidence!)

(defn ^:async gate-facts!
  "Every fact `domain.publication-gate` needs, read once, scoped to one tenant.

   All four now come from real providers. `:approved?` reads recorded approvals
   rather than the `(constantly false)` this function used while no approval
   surface existed.

   Approvals are filtered by the same tenant and project as receipts, and for the
   same reason: review evidence attests to a translation that exists in one
   scope. `:approved?` remains inert for dispatch specifically —
   `translation-work` derives from the `:translation-missing` and
   `:translation-stale` blockers, never from the review blocker, so a review
   requirement does not suppress the translation that would satisfy it — but it
   is loaded here because the same facts answer whether the resulting
   publication is admissible, and computing them twice in two places is how the
   two answers drift."
  ([config evidence-store scope documents]
   (gate-facts! config evidence-store scope documents {}))
  ([config evidence-store scope documents document-roots]
   (:facts (await (gate-evidence! config evidence-store scope documents
                                  document-roots)))))

(defn- ^:async scoped-gate-receipts!
  "Read one tenant/project query; verify provider scope and current source locale."
  [evidence-store {:keys [org-id project] :as scope} documents
   {:keys [current-authored desired-work] :or {current-authored [] desired-work []}}]
  (->> (await (store/completed-translations! evidence-store
                                             (select-keys scope [:org-id :project])))
       (#(contract-content/current-authored-receipts % current-authored desired-work))
       (tenant-receipts org-id)
       (project-receipts project)
       (current-source-locale-receipts documents)))

(defn- ^:async load-gate-evidence!
  "Authenticate one receipt snapshot and join its current scoped approvals."
  [config evidence-store scope documents document-roots
   {:keys [authenticate-content? current-authored] :as options
    :or {current-authored [] authenticate-content? false}}]
  (let [scoped (await (scoped-gate-receipts! evidence-store scope documents options))
        receipts (if authenticate-content?
                   (await (candidate-content/authenticated-receipts!
                           (:publication-content-root config) document-roots
                           (into {} (map (juxt :document/id identity)) documents)
                           current-authored scoped))
                   scoped)
        approvals (await (approvals-for-gate! evidence-store scope receipts options))]
    (evidence-domain/evidence {:receipts receipts :approvals approvals})))

(defn ^:async gate-evidence!
  "Load evidence and derive facts from the same snapshot.

   Publication uses the returned receipts to identify exact accepted output bytes.
   A second receipt read could mix a new translation with an older approval."
  ([config evidence-store scope documents document-roots]
   (gate-evidence! config evidence-store scope documents document-roots {}))
  ([config evidence-store scope documents document-roots
    {:keys [source-revisions] :as options}]
   (let [revisions (or source-revisions
                       (await (source-revision/source-revisions!
                               config documents document-roots)))
         evidence (await (load-gate-evidence! config evidence-store scope
                                              documents document-roots options))
         acceptance (await (source-review/acceptance-facts!
                            config {:documents (into {} (map (juxt :document/id identity)) documents)}
                            scope (wiki-runtime/source-dependencies)))]
     {:evidence evidence
      :facts (merge (source-revision/revision-facts revisions)
                    (evidence-domain/gate-facts evidence)
                    acceptance)})))

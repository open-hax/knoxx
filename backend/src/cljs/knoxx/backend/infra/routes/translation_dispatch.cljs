(ns knoxx.backend.infra.routes.translation-dispatch
  "Translation dispatch facade.

  Loads desired state, reads the facts the gate needs, and hands both to
  `infra.translation-dispatch`. Everything in and out is CLJS data — no Fastify
  handle enters or leaves this namespace. The owning extern adapter is
  `knoxx.backend.extern.fastify.translation-dispatch`.

  The order of the four loads is the point. Resources, then source revisions,
  then translation evidence, and only then the gate — each read once, up front,
  because `domain.publication-gate` computes evidence once so a revision
  selector cannot resolve differently between the decision to translate and the
  work actually queued. A facade that let the gate's predicates go read a file
  or a collection per call would put that drift straight back."
  (:require [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.domain.translation-evidence :as evidence-domain]
            [knoxx.backend.infra.publication-source-revision :as source-revision]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.infra.translation-dispatch :as dispatch]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-dispatch :as law]))

(defn hydrated-intents
  "Hydrated publication intents, for one document or for every document.

   Walks documents rather than the raw `:publications` list on purpose.
   `hydrate-publication-intent` throws on a dangling document reference, so
   hydrating the raw list would let one broken intent abort an entire sweep. An
   intent whose document does not resolve is instead simply absent: its source
   locale is unknowable, so there is no lawful translation to derive from it,
   and the resolver already reports it as an unresolved reference elsewhere."
  [index document-id]
  (if (some? document-id)
    (resolver/desired-publications index document-id)
    (into []
          (mapcat #(resolver/desired-publications index %))
          (keys (:documents index)))))

(defn- referenced-documents
  "The document records the given intents point at."
  [index intents]
  (into []
        (keep #(get-in index [:documents (:publication/document %)]))
        (distinct (map :publication/document intents))))

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

(defn ^:async gate-facts!
  "Every fact `domain.publication-gate` needs, read once, scoped to one tenant.

   `:approved?` is `(constantly false)` and that is deliberate rather than
   provisional: no approval surface exists yet — it is
   `knoxx-translation-approval-surface`, the sibling card — so nothing has been
   approved, and claiming otherwise would be the one lie that lets unreviewed
   content publish. It is also inert for dispatch specifically:
   `translation-work` derives from the `:translation-missing` and
   `:translation-stale` blockers, never from the review blocker, so a review
   requirement does not suppress the translation that would satisfy it."
  [config evidence-store org-id documents]
  (let [revisions (await (source-revision/source-revisions! config documents))
        receipts (tenant-receipts org-id
                                  (await (store/completed-translations! evidence-store)))
        evidence (evidence-domain/evidence {:receipts receipts})]
    (merge (source-revision/revision-facts revisions)
           (evidence-domain/gate-facts evidence)
           {:approved? (constantly false)})))

(defn ^:async dispatch-translations!
  "Dispatch the derived translation work for one document, or for all of them.

   Returns `{:dispatched [...] :considered n}`. The count is reported separately
   because an empty dispatch list is ambiguous on its own — nothing needed
   translating and nothing was even looked at read identically, and an operator
   running this against the wrong scope deserves to be able to tell."
  [config {:keys [evidence-store] :as deps} scope document-id]
  (let [index (await (publications/publication-index! config))
        intents (hydrated-intents index document-id)
        documents (referenced-documents index intents)
        facts (await (gate-facts! config evidence-store (:org-id scope) documents))]
    {:considered (count intents)
     :dispatched (await (dispatch/dispatch-intents! deps intents facts scope))}))

(def worker-report-vocabulary
  "What the ingestion worker actually sends, read from
  `ingestion/src/kms_ingestion/translation/worker.clj` rather than assumed —
  because an earlier version of `resolve-batch-status!` assumed, and was wrong in
  a way that made the whole completion path dead code.

    per-document success   status \"processing\", :completed_document <id>
    all documents done     status \"complete\",   no document named
    all documents failed   status \"failed\",     :error <message>, no document
    some failed            status \"partial\",    no document named

  Two consequences. Success arrives as `\"processing\"`, so gating on
  `complete`/`partial` rejected every real success report. And
  `:failed_document` is never sent: the worker accumulates failures in a local
  atom, and its terminal report names no document at all.

  Kept as a var so the observed contract is citable and one edit updates it."
  {:per-document-success {:status "processing" :names-document? true}
   :all-done {:status "complete" :names-document? false}
   :all-failed {:status "failed" :names-document? false}
   :some-failed {:status "partial" :names-document? false}})

(defn ^:async resolve-batch-status!
  "Turn one worker batch-status report into translation evidence.

   The *document*, not the status, decides whether a per-document binding can be
   resolved; a batch-level failure is resolved by batch id alone, which is sound
   because a Knoxx-created batch carries exactly one document. See
   `worker-report-vocabulary` for what the worker actually sends and why that
   matters. `failed-document-id` is still read because the field exists in the
   contract and a different worker may populate it."
  [deps report]
  (let [failed-document (law/failed-document-id (:failed_document report))
        batch-id (:batch_id report)]
    (cond
      ;; Any status naming a completed document resolves that binding. The
      ;; worker sends "processing" here; the others are accepted so a worker
      ;; that reports differently still works.
      (some? (:completed_document report))
      (dispatch/resolve-batch-report! deps report)

      (some? failed-document)
      (dispatch/fail-batch-document! deps batch-id failed-document
                                     (or (:error report) "worker reported failure"))

      ;; A batch-level failure names nothing, so the batch id is the binding.
      ;; Without this the claim would sit in flight forever and never be retried.
      (= "failed" (:status report))
      (dispatch/fail-batch! deps batch-id
                            (or (:error report) "worker reported batch failure"))

      :else
      ;; Reported rather than dropped. `complete` and `partial` legitimately name
      ;; nothing — the per-document reports already resolved each binding — and
      ;; an operator debugging a translation that never appeared needs to see the
      ;; difference between 'nothing to resolve' and 'silently ignored'.
      (js/Promise.resolve {:translation/skipped
                           {:reason :no-document-named
                            :status (:status report)}}))))

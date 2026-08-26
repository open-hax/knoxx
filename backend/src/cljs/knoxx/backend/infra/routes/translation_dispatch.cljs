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
            [knoxx.backend.law.publication :as publication-law]
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

(defn admissible-intents
  "The intents that could actually reconcile to a public materialization.

   `domain.publication-gate` states outright that it decides only the
   *evidential* half of admissibility and assumes the structural half holds
   upstream — and until now nothing upstream of dispatch checked it. So an intent
   targeting an archived garden, or a locale its garden does not accept, reached
   the gate, derived translation work, and was enqueued on a shared worker for
   content that can never be published. `translation-work-eligible?` cannot catch
   it: it asks only whether the intent publishes and needs translating, which
   both remain true.

   `law.publication/admissible-publication?` is the contract that owns this
   question, so it is called rather than re-derived. Filtering here rather than
   inside the gate keeps the two halves where their own docstrings put them."
  [index intents]
  (filterv #(publication-law/admissible-publication? index %) intents))

(defn- referenced-documents
  "The document records the given intents point at."
  [index intents]
  (into []
        (keep #(get-in index [:documents (:publication/document %)]))
        (distinct (map :publication/document intents))))

(defn document-source-roots
  "Checkout root per canonical document id, derived from resource provenance."
  [config records]
  (into {}
        (keep (fn [record]
                (when (and (:ok? record) (= :document (:resource/kind record)))
                  (let [document (-> record
                                     publications/single-kind-definition
                                     resolver/canonicalize-document)
                        root (source-revision/resource-source-root
                              config (:resource/file-path record))]
                    (when root [(:document/id document) root])))))
        records))

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
  ([config evidence-store {:keys [org-id project] :as scope} documents document-roots]
  (let [revisions (await (source-revision/source-revisions! config documents document-roots))
        ;; Scoped in the *query*. Reading every receipt ever recorded and
        ;; narrowing afterwards made each dispatch pass grow with the global
        ;; history of every tenant, and left the collection's own indexes unused.
        ;; The in-memory filters below stay as a second check — a store is
        ;; replaceable, and one that ignored the scope must not be able to widen
        ;; what the gate sees.
        receipts (->> (await (store/completed-translations!
                              evidence-store
                              (select-keys scope [:org-id :project])))
                      (tenant-receipts org-id)
                      (project-receipts project)
                      (current-source-locale-receipts documents))
        approvals (->> (await (store/approvals!
                               evidence-store
                               (select-keys scope [:org-id :project])))
                       (filterv #(and (= org-id (:review/org-id %))
                                      (= project (:review/project %)))))
        evidence (evidence-domain/evidence {:receipts receipts
                                            :approvals approvals})]
    (merge (source-revision/revision-facts revisions)
           (evidence-domain/gate-facts evidence)))))

(defn ^:async dispatch-translations!
  "Dispatch the derived translation work for one document, or for all of them.

   Returns `{:considered n :admissible n :dispatched [...]}`. The counts are
   reported separately because an empty dispatch list is ambiguous on its own:
   nothing needed translating, nothing was looked at, and everything looked at
   was structurally inadmissible all read identically. An operator running this
   against the wrong scope — or against a garden that has been archived —
   deserves to be able to tell which."
  [config {:keys [evidence-store] :as deps} scope document-id]
  (let [records (await (publications/resource-records! config))
        index (publications/publication-index records)
        hydrated (hydrated-intents index document-id)
        intents (admissible-intents index hydrated)
        documents (referenced-documents index intents)
        roots (document-source-roots config records)
        facts (await (gate-facts! config evidence-store scope documents roots))]
    {:considered (count hydrated)
     :admissible (count intents)
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

(defn- ^:async observe-source-revision!
  "The dispatched document's current source revision, or nil if unreadable."
  [config record]
  (let [records (await (publications/resource-records! config))
        index (publications/publication-index records)
        document (get-in index [:documents (:dispatch/document record)])]
    (when document
      (get (await (source-revision/source-revisions!
                   config [document] (document-source-roots config records)))
           (:dispatch/document record)))))

(defn source-revision-observer!
  "A function that re-reads a dispatched document's current source revision.

   Supplied to `resolve-batch-report!` so it can verify the source has not moved
   between dispatch and completion — see
   `law.translation-dispatch/source-drift-refusal` for why that verification is
   the strongest thing available when the worker is handed an id rather than
   bytes.

   The resource index is loaded per call rather than closed over. A completion
   report arrives long after the dispatch that caused it, and the whole question
   being asked is what the source looks like *now*; a cached index would answer
   with what it looked like when the process last loaded one."
  [config]
  (partial observe-source-revision! config))

(defn ^:async resolve-batch-status!
  "Turn one worker batch-status report into translation evidence.

   The *document*, not the status, decides whether a per-document binding can be
   resolved; a batch-level failure is resolved by batch id alone, which is sound
   because a Knoxx-created batch carries exactly one document. See
   `worker-report-vocabulary` for what the worker actually sends and why that
   matters. `failed-document-id` is still read because the field exists in the
   contract and a different worker may populate it.

   The report is validated and its batch id resolved *before* any store lookup,
   and both checks are load-bearing. Every branch below joins on that id, and a
   nil one is not a wildcard the stores decline to match — it is the value they
   match unbound records by. See `law/report-batch-id`."
  [deps report]
  (let [checked (law/assert-valid! :translation-dispatch/status-report
                                   law/BatchStatusReport
                                   report)
        failed-document (law/failed-document-id (:failed_document checked))
        batch-id (law/report-batch-id checked)]
    (cond
      (nil? batch-id)
      {:translation/refusal {:refusal/type :batch-id-missing
                             :refusal/actual (:batch_id checked)}}

      ;; Any status naming a completed document resolves that binding. The
      ;; worker sends "processing" here; the others are accepted so a worker
      ;; that reports differently still works.
      (some? (:completed_document checked))
      (await (dispatch/resolve-batch-report! deps checked))

      (some? failed-document)
      (await (dispatch/fail-batch-document! deps batch-id failed-document
                                            (or (:error checked)
                                                "worker reported failure")))

      ;; A batch-level failure names nothing, so the batch id is the binding.
      ;; Without this the claim would sit in flight forever and never be retried.
      (= "failed" (:status checked))
      (await (dispatch/fail-batch! deps batch-id
                                   (or (:error checked)
                                       "worker reported batch failure")))

      :else
      ;; Reported rather than dropped. `complete` and `partial` legitimately name
      ;; nothing — the per-document reports already resolved each binding — and
      ;; an operator debugging a translation that never appeared needs to see the
      ;; difference between 'nothing to resolve' and 'silently ignored'.
      {:translation/skipped {:reason :no-document-named
                             :status (:status checked)}})))

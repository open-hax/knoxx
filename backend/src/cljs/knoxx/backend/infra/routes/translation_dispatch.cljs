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

(defn ^:async gate-facts!
  "Every fact `domain.publication-gate` needs, read once.

   `:approved?` is `(constantly false)` and that is deliberate rather than
   provisional: no approval surface exists yet — it is
   `knoxx-translation-approval-surface`, the sibling card — so nothing has been
   approved, and claiming otherwise would be the one lie that lets unreviewed
   content publish. It is also inert for dispatch specifically:
   `translation-work` derives from the `:translation-missing` and
   `:translation-stale` blockers, never from the review blocker, so a review
   requirement does not suppress the translation that would satisfy it."
  [config evidence-store documents]
  (let [revisions (await (source-revision/source-revisions! config documents))
        receipts (await (store/completed-translations! evidence-store))
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
        facts (await (gate-facts! config evidence-store documents))]
    {:considered (count intents)
     :dispatched (await (dispatch/dispatch-intents! deps intents facts scope))}))

(defn ^:async resolve-batch-status!
  "Turn one worker batch-status report into translation evidence.

   Called after the batch status itself has been recorded by its owner. A
   `complete` or `partial` report naming a document resolves that document's
   binding; a named failed document records the failure against it; anything
   else has no binding to resolve and is reported as such rather than silently
   ignored."
  [deps report]
  (let [failed-document (law/failed-document-id (:failed_document report))]
    (cond
      (and (contains? #{"complete" "partial"} (:status report))
           (some? (:completed_document report)))
      (dispatch/resolve-batch-report! deps report)

      (some? failed-document)
      (dispatch/fail-batch-document! deps
                                     (:batch_id report)
                                     failed-document
                                     (or (:error report) "worker reported failure"))

      :else
      ;; Reported rather than dropped. Most status updates legitimately name no
      ;; document — a batch going `processing` names none — and an operator
      ;; debugging a translation that never appeared needs to see the difference
      ;; between 'nothing to resolve' and 'silently ignored'.
      (js/Promise.resolve {:translation/skipped
                           {:reason :no-document-named
                            :status (:status report)}}))))

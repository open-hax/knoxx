(ns knoxx.backend.law.translation-dispatch-report
  "Worker status, recovery and completion evidence for bound translation claims."
  (:require [clojure.string :as str]
            [knoxx.backend.law.translation-evidence :as evidence]
            [malli.core :as m]))

(def BatchCreated
  "What `create-translation-batch!` hands back, projected onto what is actually
   read. Open, because the response carries more than this and the extra is not
   this namespace's business."
  [:map
   [:batch_id evidence/NonBlankString]])

(def batch-statuses
  "The batch statuses the worker reports, from
   `law.openplanner-translation/UpdateTranslationBatchRequest`."
  #{"processing" "complete" "partial" "failed"})

(def BatchStatusReport
  "One status update as the worker sends it.

   `:completed_document` and `:failed_document` are per-document: a batch goes
   `partial` repeatedly, naming one document each time, before it goes
   `complete`. So a status alone never identifies which binding to resolve —
   the document does."
  [:map
   [:status (into [:enum] (sort batch-statuses))]
   [:batch_id {:optional true} [:maybe :string]]
   [:completed_document {:optional true} [:maybe :string]]
   [:failed_document {:optional true} :any]
   [:error {:optional true} [:maybe :string]]])

(defn failed-document-id
  "The document wire id carried by a report's `:failed_document`, or nil.

   `law.openplanner-translation/UpdateTranslationBatchRequest` types this field
   as an open map, while the worker assembles it from a caller-supplied value —
   so both a bare id string and a map naming one occur. Reading only one shape
   would silently drop the other's failure and leave that claim in flight
   forever, which is the one outcome that makes work never happen and never be
   reported.

   The key spellings are tried in order of specificity. Nothing is guessed from
   a non-string: a nested object under `:document` is not an id, and treating it
   as one would produce a lookup that cannot match."
  [value]
  (cond
    (string? value) (not-empty (str/trim value))
    (map? value) (some (fn [field]
                         (let [id (get value field)]
                           (when (string? id) (not-empty (str/trim id)))))
                       [:document_id :document_wire_id :document :id])
    :else nil))

(defn report-batch-id
  "The batch id a worker report is bound to, or nil when it names none.

   Read through `evidence/NonBlankString` rather than taken as given. A nil or blank id
   is not a wildcard — it is a *missing* binding, and both evidence stores
   resolve it as one: `dispatch-for-batch!` matches on equality, so a nil id
   selects exactly those records that never received a batch id, which are the
   dispatches whose send failed before the worker ever answered. A batch-level
   failure report arriving without an id would therefore mark an unrelated,
   still-unbound claim failed and re-dispatch work that was never attempted.

   The route supplies the id from the URL, so in the ordinary path this is
   always present. It is checked anyway because the report is untrusted input
   and the failure mode is silent."
  [report]
  (let [id (:batch_id report)]
    (when (m/validate evidence/NonBlankString id)
      (str/trim id))))

(def refusal-types
  "Every reason a worker answer is refused as translation evidence.

   Enumerated as data so a caller classifies by lookup rather than by parsing a
   message, and so a new refusal cannot be introduced without appearing here."
  #{:batch-id-missing
    :dispatch-record-missing
    :dispatch-document-mismatch
    :dispatch-already-resolved
    :worker-revision-selector
    :worker-batch-mismatch
    :source-moved-since-dispatch
    :source-unverifiable
    :pin-not-tied-to-observable-bytes})

(def Refusal
  "A typed refusal. Both sides travel on it: told only that something
   mismatched, a caller cannot see whether the worker or the binding was stale."
  [:map
   [:refusal/type (into [:enum] (sort refusal-types))]
   [:refusal/expected {:optional true} :any]
   [:refusal/actual {:optional true} :any]])

(defn- identity-refusal
  "Refusal because the report and the binding are not about the same thing.

   A missing record comes first: every comparison after it would read fields off
   nothing and describe the wrong problem — there is no binding for this answer,
   rather than a binding that disagrees.

   The batch check tolerates an absent id on either side, because a report need
   not repeat what the route already carried; it refuses only a genuine
   disagreement, which is a re-dispatch's answer trying to resolve the previous
   attempt's binding."
  [record report]
  (cond
    (nil? record)
    {:refusal/type :dispatch-record-missing
     :refusal/actual (select-keys report [:batch_id :completed_document])}

    (not= (:dispatch/document-wire-id record) (:completed_document report))
    {:refusal/type :dispatch-document-mismatch
     :refusal/expected (:dispatch/document-wire-id record)
     :refusal/actual (:completed_document report)}

    (and (some? (:batch_id report))
         (some? (:dispatch/batch-id record))
         (not= (:dispatch/batch-id record) (:batch_id report)))
    {:refusal/type :worker-batch-mismatch
     :refusal/expected (:dispatch/batch-id record)
     :refusal/actual (:batch_id report)}))

(defn- state-refusal
  "Refusal because of the binding's own state rather than the report.

   Only an in-flight attempt can complete: re-resolving a finished record would
   mint a second receipt for one translation, and re-resolving a failed one
   would turn a refusal into evidence.

   The selector check is defence in depth. `evidence/ConcreteRevision` already refused it
   on the way in, so reaching it means a store returned something it was never
   given."
  [record]
  (cond
    (not= :dispatch/accepted (:dispatch/outcome record))
    {:refusal/type :dispatch-already-resolved
     :refusal/expected :dispatch/accepted
     :refusal/actual (:dispatch/outcome record)}

    (evidence/revision-selector? (:dispatch/revision record))
    {:refusal/type :worker-revision-selector
     :refusal/actual (:dispatch/revision record)}))

(defn completion-refusal
  "Why `report` may not become translation evidence against `record`, or nil
   when it may.

   Data rather than a throw: a mismatched worker answer is an ordinary thing to
   receive at an untrusted boundary, and the caller has to record it either way.

   Identity is checked before state, so a report about the wrong document is
   never reported as the right document being in the wrong state."
  [record report]
  (or (identity-refusal record report)
      (state-refusal record)))

(defn output-revision
  "The produced translation's own concrete revision identity.

   Minted by Knoxx, because the worker has no such concept: the batch contract
   carries no revision in either direction, so there is no output revision to
   receive. What Knoxx does know is exactly what identifies the output — which
   source revision it came from, which locale it was rendered into, and which
   batch produced it.

   The batch id is the component that matters. Re-dispatching a translation for
   the same source revision and locale necessarily creates a new batch, so the
   output revision changes, so review evidence pinned to the old one stops being
   current. Without it, two successive translations of one source revision would
   share an identity and an approval of the first would silently authorize the
   second.

   Throws when the binding has no batch id: an output that cannot be attributed
   to the run that produced it is not identifiable, and inventing a fallback
   would hand two different translations the same revision."
  [validate! record]
  (let [batch-id (:dispatch/batch-id record)]
    (when-not (m/validate evidence/NonBlankString batch-id)
      (throw (ex-info "a translation output revision requires the producing batch id"
                      {:dispatch/key (:dispatch/key record)})))
    (validate! :translation-dispatch/output-revision
                   evidence/ConcreteRevision
                   (str (:dispatch/revision record)
                        "+" (name (:dispatch/locale record))
                        "@" batch-id))))

(defn batch-created-after?
  "Whether `batch`'s creation instant is not earlier than `at`.

   The correlation signal observation needs. Matching a batch on garden, target
   locale and document alone is not enough to conclude it came from *our* send:
   a tenant that translated the same document into the same locale before has an
   older batch that matches every one of those, and binding to it would let
   `recover-settled-batch!` mint a receipt for a revision that batch never saw.

   A batch created before the claim existed cannot be the claim's. An
   unparseable or absent creation time is treated as NOT matching, because an
   unknown age is not evidence of provenance and the safe failure is to leave
   the claim in flight rather than bind the wrong batch.

   Compared as strings, which is correct only for one fixed-width UTC format —
   the same constraint `Instant` exists to impose. A creation time in any other
   shape is refused rather than guessed at."
  [batch at]
  (boolean
   (when-let [created (some-> (or (:created_at batch) (:createdAt batch)) str not-empty)]
     (and (evidence/instant? created)
          (evidence/instant? at)
          (not (neg? (compare created at)))))))

(defn pin-refusal
  "Refuse a pin that cannot be tied to the observed source bytes.

   A missing digest is unverifiable. A different digest means the requested
   revision was already unavailable before dispatch; no receipt can attest it."
  [work context]
  (let [digest (:dispatch/source-digest context)]
    (cond
      (nil? digest)
      {:refusal/type :source-unverifiable
       :refusal/expected (:revision work)}

      (not= (:revision work) digest)
      {:refusal/type :pin-not-tied-to-observable-bytes
       :refusal/expected (:revision work)
       :refusal/actual digest})))

(defn batch-matches-dispatch?
  "Whether `batch` could be the batch this dispatch created.

   Every field the batch carries is compared — document, project and source
   language, on top of the garden and target locale the query already filtered by
   — plus a creation time not earlier than the claim.

   None of that is a *unique* correlation token, and the caller must not treat a
   single match as proof on its own: two concurrent sends for the same document
   produce two batches that agree on every one of these fields. The batch
   contract has nowhere to put a dispatch id, so uniqueness is enforced by the
   caller refusing to adopt an ambiguous match rather than by this predicate.

   A field absent from the batch is not compared. The batch record is another
   repository's shape; requiring a field it may not carry would reject every
   candidate and turn every ambiguous send into a duplicate translation."
  [batch context work at]
  (let [same-when-present (fn [batch-value expected]
                            (or (nil? batch-value)
                                (= (str batch-value) (str expected))))]
    (boolean
     (and (some #(= (:dispatch/document-wire-id context) (str %))
                (:document_ids batch))
          (batch-created-after? batch at)
          (same-when-present (:project batch) (:dispatch/project context))
          (same-when-present (:source_lang batch)
                             (name (:dispatch/source-locale context)))
          (same-when-present (:target_lang batch) (name (:locale work)))))))

(def BatchView
  "The batch as `extern.openplanner-translation-mongo.common/batch-view`
   projects it, narrowed to what recovery reads.

   Open, because the projection carries more than this and the rest is not
   recovery's business. `completed_documents` and `failed_documents` are the
   fields that matter: the status route accumulates them with `$push`, so the
   batch itself records which documents finished and which did not."
  [:map
   [:status {:optional true} [:maybe :string]]
   [:completed_documents {:optional true} [:maybe [:vector :string]]]
   [:failed_documents {:optional true} [:maybe [:vector :any]]]])

(defn- failed-document-ids
  "The document ids a batch's `failed_documents` names.

   Entries are bare ids or maps naming one, depending on what the status route
   was handed. An entry naming neither is dropped rather than guessed at: a
   guess here would produce a lookup that cannot match, which reads as 'this
   document did not fail'."
  [batch]
  (into #{}
        (keep (fn [entry]
                (cond
                  (string? entry) entry
                  (map? entry) (or (:document_id entry) (:document entry)))))
        (:failed_documents batch)))

(defn batch-document-outcome
  "What `batch` says happened to `document-wire-id`: `:completed`, `:failed`, or
   nil for still running or unknown.

   Read from the batch's own per-document arrays rather than inferred from its
   status. The status is batch-wide — `partial` means *some* document failed, and
   which one it was is not in that word — so recovering from status alone only
   worked because a Knoxx-created batch happens to carry one document. That is
   true today and it is an assumption this does not need: the arrays name the
   documents directly.

   A named failure wins over a named completion. If a document appears in both,
   something has retried inside the batch and the pessimistic reading is the one
   that cannot fabricate a receipt.

   `batch` is validated first, and one that does not satisfy `BatchView` answers
   nil. It crosses an untrusted boundary from another repository's store, and
   these two arrays are what decide whether a receipt is minted — a
   `completed_documents` holding something other than ids would compare equal to
   nothing and silently report the work still running. Nil says the same thing
   honestly: the caller leaves the claim in flight, which is recoverable."
  [batch document-wire-id]
  (when (m/validate BatchView batch)
    (cond
      (contains? (failed-document-ids batch) document-wire-id) :failed
      (contains? (set (:completed_documents batch)) document-wire-id) :completed)))

(defn source-drift-refusal
  "Refuse completion when source bytes differ from the dispatch-time digest.

   Compare observed bytes with observed bytes, not opaque revision labels. A
   missing original digest cannot substantiate completion; a missing current
   digest is a change, because the previously readable source is now absent."
  [record observed-digest]
  (let [dispatched (:dispatch/source-digest record)]
    (cond
      (nil? dispatched)
      ;; Nothing was recorded to compare against, so nothing can be
      ;; substantiated. Distinct from drift on purpose: this is a dispatch that
      ;; could not read its own source, not a source that changed.
      {:refusal/type :source-unverifiable
       :refusal/actual observed-digest}

      (not= dispatched observed-digest)
      {:refusal/type :source-moved-since-dispatch
       :refusal/expected dispatched
       :refusal/actual observed-digest})))

(defn translation-receipt
  "Mint evidence using the record's identities and the concrete output revision.

   Split evidence carries complete agent lineage under the same receipt contract.
   Never take resource identity from an untrusted worker report."
  ([record produced-revision at]
   (translation-receipt record produced-revision at nil nil))
  ([record produced-revision at content-digest]
   (translation-receipt record produced-revision at content-digest nil))
  ([record produced-revision at content-digest split-evidence]
   (evidence/assert-receipt!
    (merge
     (cond->
      {:receipt/type :translation/completed
       :translation/document (:dispatch/document record)
       :translation/garden (:dispatch/garden record)
       :translation/source-locale (:dispatch/source-locale record)
       :translation/locale (:dispatch/locale record)
       :translation/source-revision (:dispatch/revision record)
       :translation/revision produced-revision
       :translation/dispatch-key (:dispatch/key record)
       :translation/org-id (:dispatch/org-id record)
       :translation/project (:dispatch/project record)
       :translation/at at}
       (some? (:dispatch/attempt-id record))
       (assoc :translation/dispatch-attempt-id (:dispatch/attempt-id record))

       (some? content-digest)
       (assoc :translation/content-digest content-digest))
     split-evidence))))

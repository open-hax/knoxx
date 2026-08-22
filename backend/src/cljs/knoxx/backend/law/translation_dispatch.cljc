(ns knoxx.backend.law.translation-dispatch
  "Contracts for turning the publication gate's derived translation work into
  ingestion-worker input, and the worker's answer back into evidence.

  ## The revision the worker cannot carry

  The card behind this namespace assumed derived work maps onto the ingestion
  worker's input contract 'carrying document identity, source and target locale,
  concrete source revision, and a stable dispatch/idempotency identity'. The
  first three map. The last two do not exist over there:
  `law.openplanner-translation/CreateTranslationBatchRequest` is
  `{garden_id, target_lang, document_ids, source_lang, project, org_id,
  membership_id}` and has no revision field and no idempotency field. The batch
  collection is not Knoxx's, and Knoxx's own guidance is not to change another
  repository to deliver a Knoxx feature.

  So the revision binding stays here. Knoxx records a `DispatchRecord` — the
  concrete revision, the locales, and the dispatch key — against the batch id
  the worker hands back, and sends the worker only the fields its contract
  admits. The worker never learns about revisions; it does not need to. When it
  reports a document complete, the binding is joined back and *that* is where a
  stale or mismatched answer is refused.

  The rejected alternative was widening the batch request with extra keys. It
  is `{:closed false}`, so it would have worked, and it would have put Knoxx's
  revision semantics into a foreign collection where nothing validates them and
  no contract here could see them drift.

  ## Dispatch facts are not translation facts

  A dispatch says an attempt was made. A translation receipt says a translation
  exists. `law.translation-evidence` holds only the latter, and the gate reads
  only the latter — which is what lets `:translated-revision?` be a lookup
  rather than a filter that has to know which attempt outcomes count.

  Portable by mandate; the client, store, and clock stay at the runtime edge."
  (:require [clojure.string :as str]
            [knoxx.backend.law.publication-locale :as locale]
            [knoxx.backend.law.translation-evidence :as evidence]
            [malli.core :as m]
            [malli.error :as me]))

(def NonBlankString
  "Re-exported so a dispatch caller depends on one law namespace."
  evidence/NonBlankString)

(def ConcreteRevision
  "Re-exported for the same reason as `NonBlankString`. Anything keyed by a
   revision needs this rather than a bare string."
  evidence/ConcreteRevision)

(def Instant
  "Re-exported so a dispatch record and a translation receipt cannot disagree
   about what a timestamp is."
  evidence/Instant)

(defn assert-valid!
  "Return `value` when it satisfies `schema`; otherwise throw a named contract
   violation."
  [contract-id schema value]
  (if (m/validate schema value)
    value
    (throw
     (ex-info (str "Translation dispatch contract violation: " contract-id)
              {:contract contract-id
               :errors (me/humanize (m/explain schema value))}))))

;; ── Derived work ───────────────────────────────────────────────────────────

(def DerivedWork
  "The `:action/with` payload `domain.publication-gate/translation-work`
   produces.

   Declared here rather than in the gate because this is the namespace that has
   to *trust* it. The gate builds it from an already-resolved concrete revision;
   validating it again on arrival is the same both-directions rule the
   publication effect boundary follows, and it is what catches a caller that
   hand-rolled a work item instead of deriving one."
  [:map {:closed true}
   [:document :qualified-keyword]
   [:locale locale/Locale]
   [:revision ConcreteRevision]
   [:replace-stale? :boolean]])

(def DispatchContext
  "What derived work does not know but the worker's contract requires.

   Derived work names a document, a target locale and a revision. A batch is
   scoped by garden, organization and membership, and addresses documents by
   their wire id — none of which is a translation fact, all of which come from
   the hydrated intent and the acting principal. Keeping them in a separate,
   closed value means a dispatch cannot silently default an organization or a
   garden into existence."
  [:map {:closed true}
   [:dispatch/garden NonBlankString]
   [:dispatch/document-wire-id NonBlankString]
   [:dispatch/source-locale locale/Locale]
   [:dispatch/org-id NonBlankString]
   [:dispatch/membership-id NonBlankString]
   [:dispatch/project {:optional true} [:maybe NonBlankString]]])

;; ── Dispatch identity ──────────────────────────────────────────────────────

(def key-dimensions
  "Everything that changes *which translation* is being asked for, and nothing
   that does not.

   The organization, membership, garden and project are deliberately absent.
   They scope who may ask and where the batch is filed; they do not change the
   answer, and folding them in would let the same translation be dispatched
   twice by two principals — which is exactly the duplicate the key exists to
   collapse."
  [:document :source-locale :locale :revision])

(defn dispatch-key
  "One stable key per logical translation request.

   A deterministic string rather than a hash, for the reason
   `infra.publication-effects/publish-idempotency-key` gives: it is reproducible
   across processes and versions, and a human can read it when a duplicate has
   to be explained.

   Refuses a selector revision explicitly. A nil check alone would not do it —
   `:source/current` is a keyword, so it would pass and produce a
   stable-looking key for a moving target."
  [{:keys [document source-locale locale revision]}]
  (when-not (m/validate ConcreteRevision revision)
    (throw (ex-info "translation dispatch key requires a concrete revision"
                    {:document document :revision revision})))
  (->> [document source-locale locale revision]
       (mapv pr-str)
       (str/join "|")))

;; ── Worker input ───────────────────────────────────────────────────────────

(def WorkerRequest
  "The ingestion worker's input contract, as this namespace produces it.

   Mirrors `law.openplanner-translation/CreateTranslationBatchRequest` rather
   than requiring it: that contract is `.cljs`, and this namespace is portable.
   A test pins the two together, so a change over there fails here instead of
   being discovered by a worker rejecting a batch in production.

   One document per batch. The worker's batch model accepts many and translates
   them in a shared agent session for terminology consistency, which is a real
   benefit — but a batch is reported complete one `completed_document` at a
   time, and every document in it would share a single revision binding. Two
   documents at different revisions in one batch cannot both be joined back
   correctly, and the failure would be silent. Consistency across documents is
   worth having; it is not worth a receipt naming the wrong revision."
  [:map {:closed true}
   [:garden_id NonBlankString]
   [:target_lang NonBlankString]
   [:document_ids [:and [:vector NonBlankString]
                   [:fn {:error/message "a revision-bound batch carries exactly one document"}
                    #(= 1 (count %))]]]
   [:source_lang NonBlankString]
   [:org_id NonBlankString]
   [:membership_id NonBlankString]
   [:project {:optional true} [:maybe NonBlankString]]])

(defn worker-request
  "Map validated derived work plus its dispatch context onto the worker's input.

   Locales become language tags with `name`: the worker's contract is strings,
   and `pr-str` on a keyword would send `\":es\"`."
  [work context]
  (assert-valid!
   :translation-dispatch/worker-request
   WorkerRequest
   (cond-> {:garden_id (:dispatch/garden context)
            :target_lang (name (:locale work))
            :document_ids [(:dispatch/document-wire-id context)]
            :source_lang (name (:dispatch/source-locale context))
            :org_id (:dispatch/org-id context)
            :membership_id (:dispatch/membership-id context)}
     (some? (:dispatch/project context))
     (assoc :project (:dispatch/project context)))))

;; ── Dispatch records ───────────────────────────────────────────────────────

(def outcomes
  "Every distinct end state of one dispatch attempt.

   `:dispatch/accepted` is not success — it is an attempt in flight. The card
   asks that failed, rejected, duplicate and completed be recorded distinctly
   'so the gate can distinguish missing work from an attempted-but-unsuccessful
   run', and that distinction only survives if in-flight is its own state too:
   collapsed into completed it fabricates a translation, collapsed into failed
   it re-dispatches work that is still running."
  #{:dispatch/accepted
    :dispatch/duplicate
    :dispatch/rejected
    :dispatch/failed
    :dispatch/completed})

(def Outcome
  "One dispatch outcome."
  (into [:enum] (sort outcomes)))

(def DispatchRecord
  "Knoxx's side of the binding the worker cannot hold.

   `:dispatch/batch-id` is optional because a rejected or failed dispatch never
   got one — the worker refused before assigning it. Every other field is
   required: a record that cannot say which revision it asked about is not a
   binding, and joining a worker answer to it would guess."
  [:map
   [:dispatch/key NonBlankString]
   [:dispatch/outcome Outcome]
   [:dispatch/document :qualified-keyword]
   [:dispatch/document-wire-id NonBlankString]
   [:dispatch/source-locale locale/Locale]
   [:dispatch/locale locale/Locale]
   [:dispatch/revision ConcreteRevision]
   [:dispatch/at Instant]
   [:dispatch/batch-id {:optional true} [:maybe NonBlankString]]
   [:dispatch/detail {:optional true} [:maybe :string]]])

(defn assert-record!
  "Validate a dispatch record before it is persisted, and again when read back."
  [record]
  (assert-valid! :translation-dispatch/record DispatchRecord record))

(defn dispatch-record
  "Build a dispatch record for one attempt. Pure: `at` is supplied, not read."
  [work context outcome at & {:keys [batch-id detail]}]
  (assert-record!
   (cond-> {:dispatch/key (dispatch-key
                           {:document (:document work)
                            :source-locale (:dispatch/source-locale context)
                            :locale (:locale work)
                            :revision (:revision work)})
            :dispatch/outcome outcome
            :dispatch/document (:document work)
            :dispatch/document-wire-id (:dispatch/document-wire-id context)
            :dispatch/source-locale (:dispatch/source-locale context)
            :dispatch/locale (:locale work)
            :dispatch/revision (:revision work)
            :dispatch/at at}
     (some? batch-id) (assoc :dispatch/batch-id batch-id)
     (some? detail) (assoc :dispatch/detail detail))))

;; ── The worker's answers ───────────────────────────────────────────────────

(def BatchCreated
  "What `create-translation-batch!` hands back, projected onto what is actually
   read. Open, because the response carries more than this and the extra is not
   this namespace's business."
  [:map
   [:batch_id NonBlankString]])

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
    (map? value) (some (fn [key]
                         (let [id (get value key)]
                           (when (string? id) (not-empty (str/trim id)))))
                       [:document_id :document_wire_id :document :id])
    :else nil))

(def refusal-types
  "Every reason a worker answer is refused as translation evidence.

   Enumerated as data so a caller classifies by lookup rather than by parsing a
   message, and so a new refusal cannot be introduced without appearing here."
  #{:dispatch-record-missing
    :dispatch-document-mismatch
    :dispatch-already-resolved
    :worker-revision-selector
    :worker-batch-mismatch})

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

   The selector check is defence in depth. `ConcreteRevision` already refused it
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
  [record]
  (let [batch-id (:dispatch/batch-id record)]
    (when-not (m/validate NonBlankString batch-id)
      (throw (ex-info "a translation output revision requires the producing batch id"
                      {:dispatch/key (:dispatch/key record)})))
    (assert-valid! :translation-dispatch/output-revision
                   ConcreteRevision
                   (str (:dispatch/revision record)
                        "+" (name (:dispatch/locale record))
                        "@" batch-id))))

(defn translation-receipt
  "Mint completed-translation evidence from a resolved binding.

   Every identity field comes from the *record*, never from the worker's report.
   The worker was never told the revision, so it cannot supply one, and a report
   that appeared to carry one would be describing something it could not know.

   `output-revision` is the produced translation's own identity, supplied by the
   caller from the worker's answer. It is validated as a concrete revision like
   any other, which is what stops a worker replying `\"source/current\"` from
   becoming the revision an approval is later pinned to."
  [record output-revision at]
  (evidence/assert-receipt!
   {:receipt/type :translation/completed
    :translation/document (:dispatch/document record)
    :translation/source-locale (:dispatch/source-locale record)
    :translation/locale (:dispatch/locale record)
    :translation/source-revision (:dispatch/revision record)
    :translation/revision output-revision
    :translation/dispatch-key (:dispatch/key record)
    :translation/at at}))

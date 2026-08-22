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
   [:dispatch/project {:optional true} [:maybe NonBlankString]]
   [:dispatch/source-digest {:optional true} [:maybe NonBlankString]]])

;; ── Dispatch identity ──────────────────────────────────────────────────────

(def key-dimensions
  "Everything that changes *which translation* is being asked for.

   The organization is one of them, and an earlier version of this namespace
   argued the opposite — that a tenant scopes who may ask rather than what the
   answer is. That was wrong: the worker's translation data is itself
   tenant-scoped, keying segments by `org_id` and requiring one on every
   document read. So a translation produced for org A does not exist for org B,
   and a key without the tenant collapsed org B's request into a duplicate of
   org A's while org B's gate went on to report the document translated.

   The project is in for the same reason as the organization. Translation output
   is project-scoped — every existing segment, document and export route filters
   by it — so output produced under one project does not exist under another.
   With the project excluded, changing `KNOXX_SESSION_PROJECT_NAME` left the
   durable evidence in place and the new project reused dispatch keys and
   receipts belonging to the old one.

   Membership and garden stay out. Those really do only scope who asked and
   where the batch was filed; folding them in would let two principals in one
   tenant dispatch the same translation twice, which is exactly the duplicate
   this key exists to collapse."
  [:org-id :project :document :source-locale :locale :revision])

(defn dispatch-key
  "One stable key per logical translation request, per tenant.

   A deterministic string rather than a hash, for the reason
   `infra.publication-effects/publish-idempotency-key` gives: it is reproducible
   across processes and versions, and a human can read it when a duplicate has
   to be explained.

   Refuses a selector revision explicitly. A nil check alone would not do it —
   `:source/current` is a keyword, so it would pass and produce a
   stable-looking key for a moving target. The organization is required for the
   same class of reason: a key missing its tenant is a key for the wrong
   question."
  [{:keys [org-id project document source-locale locale revision]}]
  (when-not (m/validate ConcreteRevision revision)
    (throw (ex-info "translation dispatch key requires a concrete revision"
                    {:document document :revision revision})))
  (when-not (m/validate NonBlankString org-id)
    (throw (ex-info "translation dispatch key requires an organization"
                    {:document document :org-id org-id})))
  (->> [org-id project document source-locale locale revision]
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
    :dispatch/completed
    :dispatch/unreachable})

(def Outcome
  "One dispatch outcome."
  (into [:enum] (sort outcomes)))

(def unreachable-outcome
  "The revision this claim names can no longer be produced.

   Terminal, and that is the whole point. The worker fetches a document's
   *current* bytes, so once the source has moved, no retry of this claim can ever
   produce the revision it was keyed on — the dispatch key contains that
   revision. Marking such a claim retriable produced an endless sequence of
   batches for an unreachable revision, each one refused on completion and then
   re-enqueued.

   Nothing is lost by being terminal. An intent tracking `:source/current`
   resolves to the new digest on the next pass, which is a *different* dispatch
   key and therefore a fresh claim this outcome does not touch. An intent that
   pinned the old revision genuinely cannot be satisfied, and the honest result
   is that its gate stays blocked rather than a translation queue that never
   drains."
  :dispatch/unreachable)

(def retriable-outcomes
  "Outcomes a later pass may replace with a fresh attempt.

   A failed or rejected dispatch is *finished*, but it is not *done*: no
   translation came of it, so the gate still reports the translation missing and
   the work genuinely still needs doing. Treating these as terminal — the same
   as completed — strands that source revision permanently: reconciliation keeps
   answering `:dispatch/duplicate`, no batch is ever enqueued again, and the only
   way out is deleting rows by hand.

   `:dispatch/completed`, `:dispatch/duplicate` and `:dispatch/unreachable` are
   the terminal ones. Completed produced a translation; duplicate never was an
   attempt of its own; unreachable can never succeed however many times it is
   tried."
  #{:dispatch/failed :dispatch/rejected})

(defn retriable?
  "Whether an existing claim with this outcome may be replaced by a new attempt."
  [outcome]
  (contains? retriable-outcomes outcome))

(defn terminal?
  "Whether an existing claim with this outcome is settled for good."
  [outcome]
  (and (contains? outcomes outcome)
       (not (retriable? outcome))
       (not= :dispatch/accepted outcome)))

(def DispatchRecord
  "Knoxx's side of the binding the worker cannot hold.

   `:dispatch/batch-id` is optional because a rejected or failed dispatch never
   got one — the worker refused before assigning it. Every other field is
   required: a record that cannot say which revision it asked about is not a
   binding, and joining a worker answer to it would guess."
  [:map
   [:dispatch/key NonBlankString]
   [:dispatch/outcome Outcome]
   [:dispatch/org-id NonBlankString]
   [:dispatch/project {:optional true} [:maybe NonBlankString]]
   [:dispatch/document :qualified-keyword]
   [:dispatch/document-wire-id NonBlankString]
   [:dispatch/source-locale locale/Locale]
   [:dispatch/locale locale/Locale]
   [:dispatch/revision ConcreteRevision]
   [:dispatch/at Instant]
   [:dispatch/source-digest {:optional true} [:maybe NonBlankString]]
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
                           {:org-id (:dispatch/org-id context)
                            :project (:dispatch/project context)
                            :document (:document work)
                            :source-locale (:dispatch/source-locale context)
                            :locale (:locale work)
                            :revision (:revision work)})
            :dispatch/outcome outcome
            :dispatch/org-id (:dispatch/org-id context)
            :dispatch/document (:document work)
            :dispatch/document-wire-id (:dispatch/document-wire-id context)
            :dispatch/source-locale (:dispatch/source-locale context)
            :dispatch/locale (:locale work)
            :dispatch/revision (:revision work)
            :dispatch/at at}
     (some? (:dispatch/project context))
     (assoc :dispatch/project (:dispatch/project context))

     (some? (:dispatch/source-digest context))
     (assoc :dispatch/source-digest (:dispatch/source-digest context))

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
    :worker-batch-mismatch
    :source-moved-since-dispatch
    :source-unverifiable})

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

(defn source-drift-refusal
  "Refusal when the source no longer hashes to the revision that was dispatched.

   The worker is handed a document *id*, not bytes, and it fetches the current
   content when it eventually runs. So the bytes it translated are whatever the
   document held at run time, while the receipt asserts the revision Knoxx
   hashed at dispatch time. If the source changed in between, that assertion is
   false: a pinned old revision would be reported translated on the strength of
   a translation of different bytes.

   ## What this does and does not establish

   It establishes that the **repository source** did not change between dispatch
   and completion. It does NOT establish that the worker translated those bytes:
   the worker fetches its input independently from OpenPlanner's document store,
   not from this checkout, so a document already divergent over there would be
   translated while both of these observations agree.

   An earlier version of this docstring claimed the unchanged digest meant the
   worker necessarily fetched those bytes. That was an overclaim and it is
   withdrawn — the two stores are different, and nothing here reaches the one the
   worker reads.

   What remains worth doing: a source that moved locally is a receipt that is
   definitely wrong, and refusing those is strictly better than refusing none.
   Closing the rest needs the batch contract to carry a digest of what was
   actually translated, which is a change in another repository — recorded on the
   card rather than papered over here.

   Compared against `:dispatch/source-digest`, **not** against
   `:dispatch/revision`. Those are different things and conflating them was a
   defect: `law.publication/PublicationRevision` admits any nonblank string, so
   an intent may pin an opaque revision like `\"abc123\"` while the observer can
   only ever produce a `sha256-...` content digest. Comparing the two reported
   drift on every completion of every pinned intent, forever. The record
   therefore carries the digest observed at dispatch time alongside whatever
   revision the intent named, and the comparison is digest to digest.

   A nil `observed-digest` means the source could not be read at all, which is
   also not proof, and is refused for the same reason."
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
    :translation/org-id (:dispatch/org-id record)
    :translation/project (:dispatch/project record)
    :translation/at at}))

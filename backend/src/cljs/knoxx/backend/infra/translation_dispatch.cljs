(ns knoxx.backend.infra.translation-dispatch
  "Dispatching the publication gate's derived translation work, and turning the
  worker's answer back into evidence.

  Two directions, both of them narrow:

    dispatch-work!        derived work -> a claim, then the worker's batch
    resolve-batch-report! the worker's status report -> a translation receipt

  Neither reimplements translation. The worker already exists — it polls
  `/api/translations/batches/next`, translates in a shared agent session, and
  reports progress back through `/api/translations/batches/:id/status`. This
  namespace only asks it for work and interprets what it says, which is what the
  card means by dispatching 'through the established worker boundary'.

  The revision the worker never sees stays here, in a dispatch record. See
  `law.translation-dispatch` for why that binding cannot live in the batch.

  ## Observe before deciding

  Three of the paths below recover rather than guess, and they share one reason:
  the worker's batch contract carries no idempotency key, so a request whose
  outcome is unknown cannot be safely repeated — a second batch would translate
  the same revision twice, and nothing would collapse the two. The same rule
  `infra.publication-effects/reconcile-in-flight!` follows for the same reason.

  So an ambiguous send is observed (`recover-ambiguous-send!`), an in-flight
  claim that may have finished is re-read (`recover-settled-batch!`), and only a
  send that provably did not land becomes retriable. Where nothing can be
  observed, the claim is left in flight: a stuck claim is visible and fixable, a
  duplicate translation is neither."
  (:require [knoxx.backend.domain.publication-gate :as gate]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-dispatch :as law]
            [knoxx.backend.shape.resource-identity :as resource-identity]))

(defn dispatch-context
  "The dispatch context for one hydrated publication intent.

   The garden, the document's wire id and the source locale are facts about the
   intent; the organization and membership are facts about the acting principal.
   Both halves are required, and neither is defaulted — a dispatch that invented
   an organization would file a batch in a tenant nobody named."
  [intent {:keys [org-id membership-id project]}]
  (law/assert-valid!
   :translation-dispatch/context
   law/DispatchContext
   (cond-> {:dispatch/garden (resource-identity/encode-keyword
                              (:publication/garden intent))
            :dispatch/document-wire-id (resource-identity/encode-keyword
                                        (:publication/document intent))
            :dispatch/source-locale (:document/source-locale intent)
            :dispatch/org-id org-id
            :dispatch/membership-id membership-id}
     (some? project) (assoc :dispatch/project project))))

;; ── Dispatch ───────────────────────────────────────────────────────────────

(defn- ^:async request-batch!
  "Ask the worker boundary for a batch and return its id.

   The response is untrusted input: `BatchCreated` is asserted before the id is
   read, so a client that answered `{:ok false}` cannot yield a nil batch id
   that later makes an output revision unattributable."
  [client work context]
  (let [request (law/worker-request work context)
        response (await (openplanner-client/create-translation-batch! client request))]
    (:batch_id (law/assert-valid! :translation-dispatch/batch-created
                                  law/BatchCreated
                                  response))))

(defn- ^:async observe-batch!
  "Ask the worker whether a batch already exists for *this* dispatch.

   Matched on garden, target locale and the document — everything the batch
   carries — and then on creation time, which is the part that makes it a match
   on this dispatch rather than on any dispatch. A tenant that translated the
   same document into the same locale before has an older batch matching all
   three, and binding to it would mint a receipt for a revision that batch never
   saw. See `law/batch-created-after?`.

   `record` supplies the claim's own instant, so only a batch created at or after
   the claim can be attributed to it."
  [client work context record]
  (let [response (await (openplanner-client/translation-batches!
                         client
                         {:org_id (:dispatch/org-id context)
                          :garden_id (:dispatch/garden context)
                          :target_lang (name (:locale work))}))
        wanted (:dispatch/document-wire-id context)]
    (->> (:batches response)
         (filter (fn [batch]
                   (and (some #(= wanted (str %)) (:document_ids batch))
                        (law/batch-created-after? batch (:dispatch/at record)))))
         first)))

(defn- ^:async recover-ambiguous-send!
  "Decide what an ambiguous batch creation left behind.

   A batch that exists is bound and the claim stays in flight; nothing found
   means the create did not land, and only then does the claim become retriable.
   See the namespace docstring for why observation comes first."
  [evidence-store client work context record detail]
  (let [dispatch-key (:dispatch/key record)]
    (try
      (if-let [batch (await (observe-batch! client work context record))]
        {:dispatch/outcome :dispatch/accepted
         :dispatch/record (or (await (store/bind-dispatch-batch!
                                      evidence-store dispatch-key
                                      (str (or (:batch_id batch) (:id batch)))))
                              record)
         :dispatch/detail detail}
        {:dispatch/outcome :dispatch/failed
         :dispatch/record (or (await (store/resolve-dispatch!
                                      evidence-store dispatch-key
                                      :dispatch/failed detail))
                              record)
         :dispatch/detail detail})
      (catch :default observe-error
        {:dispatch/outcome :dispatch/accepted
         :dispatch/record record
         :dispatch/detail (str detail
                               "; observation also failed: "
                               (ex-message observe-error))}))))

(defn- ^:async accept-dispatch!
  "Call the worker for a freshly reserved claim, and record what happened.

   A failure is not automatically a retriable failure. The worker request has no
   idempotency key, so an exception after the request went out leaves the
   outcome genuinely unknown, and treating that as failed would let a retry
   translate the same revision twice. `recover-ambiguous-send!` observes before
   deciding."
  [evidence-store client work context record]
  (try
    (let [batch-id (await (request-batch! client work context))
          bound (await (store/bind-dispatch-batch! evidence-store
                                                   (:dispatch/key record)
                                                   batch-id))]
      {:dispatch/outcome :dispatch/accepted
       :dispatch/record (or bound record)})
    (catch :default err
      (await (recover-ambiguous-send!
              evidence-store client work context record
              (or (not-empty (str (ex-message err))) "unknown worker failure"))))))

(defn- ^:async record-completion!
  "Persist the receipt, then close the claim.

   That order matters. A receipt recorded *after* a successful resolve would be
   lost if the process died between the two, and the claim would read completed
   with no translation to show — the gate would then report that work done
   forever. This order can only ever duplicate a receipt for an already-resolved
   claim, and `law/completion-refusal` refuses that on the way back in.

   `detail` distinguishes how the completion was learned: nil for the worker's
   own report, a reason string when it was recovered from the batch's state."
  [evidence-store clock record detail]
  (let [receipt (law/translation-receipt record (law/output-revision record) (clock))]
    (await (store/record-translation! evidence-store receipt))
    (await (store/resolve-dispatch! evidence-store
                                    (:dispatch/key record)
                                    :dispatch/completed
                                    detail))
    {:translation/receipt receipt}))

(defn- ^:async recover-settled-batch!
  "Re-read the batch behind an in-flight claim, and settle the claim if it ended.

   The recovery for evidence that was *earned* but never recorded. The worker
   reports a completion once and does not retry after a successful POST, so if
   the bookkeeping that turns that report into a receipt failed transiently, the
   report is gone and the claim would read in flight forever.

   Nothing extra has to be persisted, because the batch still knows. A
   Knoxx-created batch holds exactly one document, so the batch's own terminal
   status is this document's outcome. Any other status — or an unreadable batch —
   leaves the claim alone and reports a duplicate, the honest answer for work
   that really is still running."
  [{:keys [evidence-store client clock]} record]
  (let [batch-id (:dispatch/batch-id record)
        status (try
                 (:status (await (openplanner-client/translation-batch!
                                  client batch-id
                                  {:org_id (:dispatch/org-id record)})))
                 (catch :default _ nil))]
    (case status
      "complete"
      (assoc (await (record-completion! evidence-store clock record
                                        "recovered from batch status"))
             :dispatch/outcome :dispatch/completed
             :dispatch/record record)

      "failed"
      {:dispatch/outcome :dispatch/failed
       :dispatch/record (or (await (store/resolve-dispatch!
                                    evidence-store (:dispatch/key record)
                                    :dispatch/failed
                                    "recovered from batch status"))
                            record)}

      {:dispatch/outcome :dispatch/duplicate :dispatch/record record})))

(defn ^:async dispatch-work!
  "Dispatch one derived translation work item, exactly once.

   The claim is taken *before* the worker is called. Calling first and recording
   after would leave a window in which a second pass sees no claim, enqueues a
   second batch, and translates the same revision twice — and the second
   translation cannot be withdrawn, because nothing recorded that the first was
   already asked for.

   Returns the outcome and the record it applies to. A duplicate is not an
   error: it is the correct answer to asking twice."
  [{:keys [evidence-store client clock] :as deps} work context]
  (let [checked-work (law/assert-valid! :translation-dispatch/work law/DerivedWork work)
        record (law/dispatch-record checked-work context :dispatch/accepted (clock))
        reservation (await (store/reserve-dispatch! evidence-store record))]
    (case (:reservation/status reservation)
      :reserved (await (accept-dispatch! evidence-store client checked-work context
                                         (:record reservation)))

      ;; In flight is not necessarily still running. Two recoverable cases hide
      ;; here, and reporting duplicate for both is what leaves a claim stuck
      ;; forever: with no batch id an earlier send was ambiguous, and with one
      ;; the batch may have finished while the bookkeeping that should have
      ;; recorded it failed.
      :in-flight
      (if (:dispatch/batch-id (:record reservation))
        (await (recover-settled-batch! deps (:record reservation)))
        (await (recover-ambiguous-send!
                evidence-store client checked-work context (:record reservation)
                "recovering an earlier ambiguous send")))

      ;; Already settled. Nothing to re-enqueue.
      :done
      {:dispatch/outcome :dispatch/duplicate
       :dispatch/record (:record reservation)})))

;; ── Deriving work from desired state ───────────────────────────────────────

(defn derived-work
  "The translation work one hydrated intent derives, or nil.

   Delegates wholly to `domain.publication-gate/gate`, which computes evidence
   once and keys the derived work to that single concrete revision. Nothing here
   re-decides admissibility: this namespace's job is to dispatch what the gate
   already decided, and a second opinion about it would be the drift the gate's
   compute-once rule exists to prevent."
  [intent facts]
  (:translation-work (gate/gate intent facts)))

(defn ^:async dispatch-intents!
  "Dispatch the derived translation work for every intent that has any.

   Sequential rather than concurrent, deliberately. The claims are atomic, so
   concurrency would be safe for correctness — but every dispatch creates a
   batch on a shared worker queue, and fanning out an entire garden's backlog in
   one pass is how a reconciliation run becomes an incident. One at a time also
   keeps the returned report in a readable order.

   Returns one entry per intent that derived work. An intent with no derived
   work is absent rather than present-and-empty: the gate's silence means
   'nothing to do', which is not an event."
  [deps intents facts scope]
  (let [results (atom [])]
    (doseq [intent intents]
      (when-let [work (derived-work intent facts)]
        (let [outcome (await (dispatch-work! deps
                                             (:action/with work)
                                             (dispatch-context intent scope)))]
          (swap! results conj (assoc outcome
                                     :publication/id (:publication/id intent))))))
    @results))

;; ── Resolving the worker's answer ──────────────────────────────────────────

(defn- ^:async refuse-drifted-completion!
  "Refuse a completion whose source has moved, and make the claim retriable.

   Retriable rather than merely refused: the document still needs translating,
   now at its new revision, and leaving the claim in flight would strand it."
  [evidence-store record drift]
  (await (store/resolve-dispatch! evidence-store (:dispatch/key record)
                                  :dispatch/failed
                                  "source moved between dispatch and completion"))
  {:translation/refusal drift})

(defn ^:async resolve-batch-report!
  "Turn one worker status report into translation evidence, or refuse it.

   `report` names a batch and a document; only the store knows which concrete
   revision that pair was asked about, so the join happens first and every
   refusal is decided against the binding rather than against the report.

   A refusal is returned, not thrown. An untrusted boundary receiving a stale or
   mismatched answer is an ordinary event, and the caller — an HTTP route the
   worker calls — has to answer either way. The claim is left in flight on a
   refusal: the report did not resolve it, and marking it failed would discard a
   translation that may still be running.

   Returns `{:translation/receipt r}` on success, or `{:translation/refusal f}`."
  [{:keys [evidence-store clock observe-source-revision]} report]
  ;; Required, not optional, and for the same reason
  ;; `publication-target-registry` requires its locale guard: a caller that
  ;; forgot to supply the check must fail, not quietly get a version with the
  ;; check missing. Defaulting to "assume unchanged" would delete the only
  ;; evidence that the worker translated the revision this receipt names.
  (when-not (fn? observe-source-revision)
    (throw (ex-info "resolving a completion requires a source-revision observer"
                    {:batch_id (:batch_id report)})))
  (let [checked (law/assert-valid! :translation-dispatch/status-report
                                   law/BatchStatusReport
                                   report)
        record (await (store/dispatch-for-batch-document!
                       evidence-store
                       (:batch_id checked)
                       (:completed_document checked)))]
    (if-let [refusal (law/completion-refusal record checked)]
      {:translation/refusal refusal}
      ;; The worker was handed a document id and fetched the content when it ran,
      ;; so the bytes it translated are only knowably the dispatched revision if
      ;; the source has not moved since. Verified rather than assumed — see
      ;; `law/source-drift-refusal`.
      (if-let [drift (law/source-drift-refusal
                      record
                      (await (observe-source-revision record)))]
        (await (refuse-drifted-completion! evidence-store record drift))
        (await (record-completion! evidence-store clock record nil))))))

(defn ^:async fail-batch-document!
  "Record that the worker could not translate one document of a batch.

   Distinct from a refusal: the worker answered, and the answer was failure. The
   claim moves to `:dispatch/failed` so a later pass can re-dispatch it, which is
   the distinction the card asks for between missing work and an
   attempted-but-unsuccessful run."
  [{:keys [evidence-store]} batch-id document-wire-id detail]
  (if-let [record (await (store/dispatch-for-batch-document! evidence-store
                                                             batch-id
                                                             document-wire-id))]
    {:dispatch/record (await (store/resolve-dispatch! evidence-store
                                                      (:dispatch/key record)
                                                      :dispatch/failed
                                                      detail))
     :dispatch/outcome :dispatch/failed}
    {:translation/refusal {:refusal/type :dispatch-record-missing
                           :refusal/actual {:batch_id batch-id
                                            :document document-wire-id}}}))

(defn ^:async fail-batch!
  "Record that the worker could not translate a batch, by batch id alone.

   The worker's terminal failure report names no document — it sends
   `status = \"failed\"` plus an error and nothing else. A Knoxx-created batch
   carries exactly one document (`law.translation-dispatch/WorkerRequest`
   enforces it), so the batch id identifies the binding unambiguously.

   Without this the claim would stay `:dispatch/accepted` forever: later passes
   would read it as still running, never retry it, and the gate would go on
   asking for a translation that already failed."
  [{:keys [evidence-store]} batch-id detail]
  (if-let [record (await (store/dispatch-for-batch! evidence-store batch-id))]
    {:dispatch/record (await (store/resolve-dispatch! evidence-store
                                                      (:dispatch/key record)
                                                      :dispatch/failed
                                                      detail))
     :dispatch/outcome :dispatch/failed}
    {:translation/refusal {:refusal/type :dispatch-record-missing
                           :refusal/actual {:batch_id batch-id}}}))

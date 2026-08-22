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
   intent; the organization, membership and project are facts about the acting
   principal and its configuration. None is defaulted — a dispatch that invented
   an organization would file a batch in a tenant nobody named.

   `source-digest` is the content digest observed at dispatch time, and it is
   deliberately separate from the intent's `:publication/revision`. An intent may
   pin an opaque revision like `\"abc123\"`, which no observer can ever reproduce;
   completion compares digest to digest, so the digest has to be recorded
   independently of whatever the intent chose to call the revision."
  [intent {:keys [org-id membership-id project]} source-digest]
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
     (some? project) (assoc :dispatch/project project)
     (some? source-digest) (assoc :dispatch/source-digest source-digest))))

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

(def batch-listing-cap
  "The row limit the batch listing applies, mirrored from
   `extern.openplanner-translation-mongo.batches`.

   Named here because this namespace has to reason about *hitting* it: a full
   page means the listing may have truncated, so a batch missing from it is not
   proof the batch does not exist. Mirrored rather than imported because the
   store boundary does not expose it, and a test pins the two together."
  50)

(defn- ^:async observe-batch!
  "Ask the worker whether a batch already exists for *this* dispatch.

   Matched on garden, target locale and the document — everything the batch
   carries — and then on creation time, which is the part that makes it a match
   on this dispatch rather than on any dispatch. A tenant that translated the
   same document into the same locale before has an older batch matching all
   three, and binding to it would mint a receipt for a revision that batch never
   saw. See `law/batch-created-after?`.

   `record` supplies the claim's own instant, so a batch created before the claim
   is excluded outright. See `law/batch-matches-dispatch?` for the rest.

   Returns `{:candidates [...] :conclusive? bool}`. Empty candidates with
   `:conclusive?` true is the only combination that licenses treating the send as
   failed; anything else leaves the claim in flight."
  [client work context record]
  (let [response (await (openplanner-client/translation-batches!
                         client
                         {:org_id (:dispatch/org-id context)
                          :garden_id (:dispatch/garden context)
                          :target_lang (name (:locale work))}))
        batches (vec (:batches response))
        candidates (filterv #(law/batch-matches-dispatch? % context work
                                                          (:dispatch/at record))
                            batches)]
    ;; NO candidate is ever adopted, not even a sole one. Every field the batch
    ;; carries is compared and the creation time bounds it below, and none of
    ;; that identifies which *request* created it — the batch contract has
    ;; nowhere to put a dispatch id. One unrelated actor creating a matching
    ;; batch after this claim produces exactly one candidate, and adopting it
    ;; would let `recover-settled-batch!` mint a receipt for this claim's source
    ;; revision from a batch that never carried it.
    ;;
    ;; So observation is used only in the direction it can be trusted: it can
    ;; *refute* "the send did not land", never confirm which batch is ours.
    {:candidates candidates
     ;; Absence in a capped list is not absence. The direct Mongo listing sorts
     ;; newest-first and stops at `batch-listing-cap`, so a busy garden can push
     ;; our batch off the end — and reading that as "the send did not land" is
     ;; exactly how a duplicate translation happens.
     :conclusive? (< (count batches) batch-listing-cap)}))

(defn- ^:async recover-ambiguous-send!
  "Decide what an ambiguous batch creation left behind.

   Only one conclusion is drawn: a *conclusive absence* means the create did not
   land, and only then does the claim become retriable. Anything else — matching
   batches found, or a truncated listing — leaves the claim in flight.

   Nothing is ever bound from observation. A matching batch does not identify the
   request that created it, so binding one could attribute this claim to a batch
   that translated a different revision, and `recover-settled-batch!` would then
   mint a receipt for content nobody produced. Fabricated evidence is worse than
   a stranded claim, which is at least visible in the record's detail.

   The cost is real and stated on the card: an ambiguous send whose batch *did*
   land leaves a claim that no later pass can bind or retry, so that revision
   needs an operator. Closing it needs a dispatch correlation value on the batch,
   which is another repository's contract."
  [evidence-store client work context record detail]
  (let [dispatch-key (:dispatch/key record)]
    (try
      (let [{:keys [candidates conclusive?]}
            (await (observe-batch! client work context record))]
        (cond
          ;; A conclusive absence — nothing matching, from a listing that was not
          ;; truncated — is the only evidence that licenses a retry.
          (and conclusive? (empty? candidates))
          {:dispatch/outcome :dispatch/failed
           :dispatch/record (or (await (store/resolve-dispatch!
                                        evidence-store dispatch-key
                                        :dispatch/failed detail))
                                record)
           :dispatch/detail detail}

          (seq candidates)
          {:dispatch/outcome :dispatch/accepted
           :dispatch/record record
           :dispatch/detail (str detail
                                 "; " (count candidates)
                                 " batch(es) match this dispatch but none carries a"
                                 " dispatch id, so none can be attributed to it")}

          :else
          {:dispatch/outcome :dispatch/accepted
           :dispatch/record record
           :dispatch/detail (str detail
                                 "; batch listing was truncated, so absence is"
                                 " not proof the send failed")}))
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

(defn- ^:async refuse-drifted-completion!
  "Refuse a completion whose source has moved, and settle the claim for good.

   Terminal rather than retriable, which is the opposite of what an earlier
   version did. Retrying *this* claim can never work: the worker fetches current
   bytes, and the dispatch key contains the revision that is no longer current,
   so every attempt would be refused on completion and re-enqueued. That is an
   endless queue, not a recovery.

   Nothing is lost. An intent tracking `:source/current` resolves to the new
   digest next pass, which is a different key and a fresh claim. An intent that
   pinned the old revision genuinely cannot be satisfied, and its gate staying
   blocked is the honest outcome. See `law/unreachable-outcome`."
  [evidence-store record drift]
  (await (store/resolve-dispatch! evidence-store (:dispatch/key record)
                                  law/unreachable-outcome
                                  "source moved between dispatch and completion"))
  {:translation/refusal drift})

(defn- ^:async complete-if-source-agrees!
  "Mint the receipt only if the source still hashes to the dispatched revision.

   Shared by the worker's own completion report and by recovery from batch
   state. Recovery used to mint directly, which meant the one path that exists
   *because* evidence was lost was also the one path that skipped verifying it —
   the weaker check exactly where it matters most."
  [evidence-store clock observe-source-revision record]
  (if-let [drift (law/source-drift-refusal
                  record
                  (await (observe-source-revision record)))]
    (await (refuse-drifted-completion! evidence-store record drift))
    (await (record-completion! evidence-store clock record nil))))

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
  [{:keys [evidence-store client clock observe-source-revision]} record]
  ;; Required for the same reason `resolve-batch-report!` requires it: this path
  ;; can mint a receipt, and a caller that forgot the check must fail rather than
  ;; get a version of recovery with the verification missing.
  (when-not (fn? observe-source-revision)
    (throw (ex-info "recovering a settled batch requires a source-revision observer"
                    {:dispatch/key (:dispatch/key record)})))
  (let [batch-id (:dispatch/batch-id record)
        status (try
                 (:status (await (openplanner-client/translation-batch!
                                  client batch-id
                                  {:org_id (:dispatch/org-id record)})))
                 (catch :default _ nil))]
    (case status
      "complete"
      ;; Through the same drift guard as the worker's own report. This branch
      ;; exists because evidence was lost, which is no reason to trust it more.
      (let [result (await (complete-if-source-agrees!
                           evidence-store clock observe-source-revision record))]
        (assoc result
               :dispatch/outcome (if (:translation/receipt result)
                                   :dispatch/completed
                                   law/unreachable-outcome)
               :dispatch/record record))

      "failed"
      {:dispatch/outcome :dispatch/failed
       :dispatch/record (or (await (store/resolve-dispatch!
                                    evidence-store (:dispatch/key record)
                                    :dispatch/failed
                                    "recovered from batch status"))
                            record)}

      {:dispatch/outcome :dispatch/duplicate :dispatch/record record})))

(defn- ^:async reserve-and-send!
  "Claim the key, then send or report what the existing claim says."
  [{:keys [evidence-store client] :as deps} checked-work context record]
  (let [reservation (await (store/reserve-dispatch! evidence-store record))]
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
        pin-refusal (law/pin-refusal checked-work context)
        record (law/dispatch-record checked-work
                                    context
                                    (if pin-refusal
                                      law/unreachable-outcome
                                      :dispatch/accepted)
                                    (clock))]
    (if pin-refusal
      ;; Refused before any batch is created: a revision that cannot be tied to
      ;; observable bytes can never be substantiated by a receipt, so asking the
      ;; worker for it would only produce a false one. Terminal, because no retry
      ;; can make the pin resolvable. The claim is still recorded, so an operator
      ;; can see the intent was considered and why it was refused.
      (do
        (await (store/reserve-dispatch! evidence-store record))
        {:dispatch/outcome law/unreachable-outcome
         :dispatch/record record
         :translation/refusal pin-refusal})
      (await (reserve-and-send! deps checked-work context record)))))

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
        (let [digest ((:current-source-revision facts) (:publication/document intent))
              outcome (await (dispatch-work! deps
                                             (:action/with work)
                                             (dispatch-context intent scope digest)))]
          (swap! results conj (assoc outcome
                                     :publication/id (:publication/id intent))))))
    @results))

;; ── Resolving the worker's answer ──────────────────────────────────────────

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
      (await (complete-if-source-agrees! evidence-store clock
                                         observe-source-revision record)))))

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

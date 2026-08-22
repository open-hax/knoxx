(ns knoxx.backend.infra.translation-evidence-store
  "Persistence boundary for translation dispatch bindings, completed translation
  evidence, and review approvals.

  Three things are stored, and they are not the same kind of thing. A *dispatch
  record* is a claim: Knoxx asked for a translation at one concrete revision and
  is waiting. A *translation receipt* is a fact: the translation exists. An
  *approval* is a different fact: someone authorized accepted one produced
  translation. The claim is mutable exactly once, from in-flight to a terminal
  outcome; the two facts are never edited.

  `reserve-dispatch!` deliberately borrows the vocabulary of
  `infra.publication-effects/IIdempotencyStore`, including its hard rule:
  implementations must contain no `await` between reading a key and claiming
  it. A separate existence-check followed by an insert is NOT equivalent — two
  concurrent reconciler runs would both read 'absent' and both enqueue a batch,
  and the second translation is unrecoverable because nothing recorded that the
  first was already asked for. That is the duplicate this card exists to
  prevent, so the atomicity requirement is part of the protocol contract rather
  than advice to implementors.

  No adapter-specific identifier crosses upward, and every record is validated
  in both directions: a store is replaceable, so what it returns is untrusted
  input rather than a promise."
  (:require [knoxx.backend.law.translation-dispatch :as dispatch-law]
            [knoxx.backend.law.translation-evidence :as evidence-law]))

(defprotocol ITranslationEvidenceStore
  (reserve-dispatch! [store record]
    "ATOMICALLY claim `record`'s dispatch key, or report what is already known
     about it. Returns a Promise of one of:

       {:reservation/status :reserved  :record r}  freshly claimed by us
       {:reservation/status :in-flight :record r}  claimed, still running
       {:reservation/status :done      :record r}  settled for good

     A claim whose outcome is retriable — failed or rejected — is *replaced* by
     `record` and reported `:reserved`, because no translation came of it and the
     work still needs doing. Only `:dispatch/completed` and
     `:dispatch/duplicate` are `:done`. Reporting a failed attempt as done
     strands that source revision forever: the gate keeps reporting the
     translation missing while every later pass answers duplicate.

     Implementations must contain no `await` between reading the key and
     claiming it.")
  (resolve-dispatch! [store dispatch-key outcome detail]
    "Move an in-flight claim to a terminal `outcome`. Returns a Promise of the
     updated record, or nil when no in-flight claim held that key — so a caller
     can tell 'resolved it' from 'somebody else already did'.

     `detail` is free-text evidence for a human and never a decision input.")
  (bind-dispatch-batch! [store dispatch-key batch-id]
    "Record the batch id the worker assigned to an accepted claim. Returns a
     Promise of the updated record, or nil when no in-flight claim held the key.")
  (dispatch-for-key! [store dispatch-key]
    "The dispatch record for `dispatch-key`, or nil. Returns a Promise.")
  (dispatch-for-batch-document! [store batch-id document-wire-id]
    "The dispatch record bound to `batch-id` for one document, or nil. Returns a
     Promise.

     This is the join the worker's status report is resolved through: the report
     names a batch and a document, and only this store knows which concrete
     revision that pair was asked about.")
  (dispatch-for-batch! [store batch-id]
    "The dispatch record bound to `batch-id`, or nil. Returns a Promise.

     Distinct from `dispatch-for-batch-document!` because the worker's terminal
     failure report names no document at all — it sends `status = \"failed\"` and
     an error, nothing more. A Knoxx-created batch carries exactly one document
     (`law.translation-dispatch/WorkerRequest` enforces it), so the batch id
     alone identifies the binding. Without this lookup a batch-level failure
     could never be resolved and its claim would sit in flight forever.")
  (record-translation! [store receipt]
    "Append completed translation evidence. Returns a Promise of the receipt.")
  (completed-translations! [store]
    "Every completed translation receipt. Returns a Promise.

     Order is deliberately NOT part of the contract.
     `domain.translation-evidence/index-receipts` selects the current receipt by
     a total order over `:translation/at` and the output revision, so an
     implementation is free to return rows in whatever order its query produced.
     An earlier draft of this docstring promised oldest-first while the durable
     store read unsorted — a contradiction a consumer could have relied on.")
  (record-approval! [store approval]
    "Record immutable review evidence, at most once per approved output.

     Returns a Promise of `{:approval/status :recorded :approval a}` for a first
     approval, or `{:approval/status :existing :approval a}` when this exact
     output was already approved.

     Idempotent rather than append-only, and the difference matters: an approval
     is immutable evidence, so a second one for the same produced output is not a
     new fact, it is the same fact asserted twice. Appending would make
     `approved?` depend on how many times a button was clicked and leave two
     records free to disagree about who approved.

     Implementations must claim atomically, for the same reason
     `reserve-dispatch!` must: two reviewers clicking together would otherwise
     both insert.")
  (approvals! [store]
    "Every approval. Returns a Promise. Order is not part of the contract:
     `domain.translation-evidence/index-approvals` retains all of them and the
     join against the receipt decides which is current."))

;; ── Validation helpers ─────────────────────────────────────────────────────

(defn- checked-reservation
  "Validate the record inside a reservation answer.

   The status is the store's word; the record is data it may have round-tripped
   through a database, so it is checked before a caller reads a revision off it
   and dispatches on that."
  [reservation]
  (cond-> reservation
    (:record reservation) (update :record dispatch-law/assert-record!)))

;; ── In-memory state transitions ────────────────────────────────────────────
;;
;; Each transition is a pure `state -> state` function that also stashes its
;; answer under a well-known key. The answer travels *inside* the new state
;; rather than being computed before the swap because a decision computed first
;; would have been made against a map another caller could already have
;; replaced. One `swap!` therefore both decides and records, and CLJS being
;; single-threaded makes that indivisible — the same property the Mongo
;; implementation gets from a unique index refusing the second insert.

(defn- in-flight?
  [record]
  (= :dispatch/accepted (:dispatch/outcome record)))

(defn- reserve-in-state
  "Claim `record`'s key, or report the claim already there.

   A *retriable* existing claim — failed or rejected — is replaced by `record`
   outright rather than reported as settled. Replacing wholesale is what clears
   the previous attempt's batch id: left behind, the old batch's completion
   report would resolve the new attempt, minting a receipt for a translation the
   new attempt never produced. See `law.translation-dispatch/retriable-outcomes`
   for why a failed attempt must not be terminal at all."
  [current record]
  (let [dispatch-key (:dispatch/key record)
        existing (get-in current [:dispatches dispatch-key])]
    (cond
      (nil? existing)
      (-> current
          (assoc-in [:dispatches dispatch-key] record)
          (assoc :answer {:reservation/status :reserved :record record}))

      (in-flight? existing)
      (assoc current :answer {:reservation/status :in-flight :record existing})

      (dispatch-law/retriable? (:dispatch/outcome existing))
      (-> current
          (assoc-in [:dispatches dispatch-key] record)
          (assoc :answer {:reservation/status :reserved :record record}))

      :else
      (assoc current :answer {:reservation/status :done :record existing}))))

(defn- update-in-flight-state
  "Apply `f` to an in-flight claim, or answer nil when there is not one.

   Refusing to touch a terminal record is the compare-and-set the Mongo
   implementation expresses as a query predicate: whoever resolves the claim
   first wins, and the loser learns it lost instead of overwriting an outcome."
  [current dispatch-key f]
  (let [existing (get-in current [:dispatches dispatch-key])]
    (if (and existing (in-flight? existing))
      (let [updated (f existing)]
        (-> current
            (assoc-in [:dispatches dispatch-key] updated)
            (assoc :answer updated)))
      (assoc current :answer nil))))

(defn- resolved-answer
  "The answer left by a transition, validated as a record."
  [state]
  (some-> (:answer state) dispatch-law/assert-record!))

(defn- batch-document-match
  [dispatches batch-id document-wire-id]
  (->> (vals dispatches)
       (filter #(and (= batch-id (:dispatch/batch-id %))
                     (= document-wire-id (:dispatch/document-wire-id %))))
       first))

(defn approval-identity
  "The identity at most one approval may exist for.

   Includes the translation revision, not merely the gate's triple: approving a
   re-translation of the same source revision is a genuinely new act of review
   over different bytes, and collapsing the two would silently reuse the first
   reviewer's decision for content they never saw.

   Includes the tenant and project for the same reason the receipt does — an
   approval attests to a translation that exists in one scope."
  [approval]
  [(:review/org-id approval)
   (:review/project approval)
   (:review/document approval)
   (:review/locale approval)
   (:review/revision approval)
   (:review/translation-revision approval)])

(defn- record-approval-in-state
  "Store `approval` under `identity` unless one is already there, answering
   which happened."
  [current identity approval]
  (if-let [existing (get-in current [:approvals identity])]
    (assoc current :answer {:approval/status :existing :approval existing})
    (-> current
        (assoc-in [:approvals identity] approval)
        (assoc :answer {:approval/status :recorded :approval approval}))))

;; ── In-memory implementation ───────────────────────────────────────────────

(defn- resolve-outcome-in-state
  "Set an in-flight claim's outcome, and its detail when one is given."
  [current dispatch-key outcome detail]
  (update-in-flight-state current dispatch-key
                          (fn [record]
                            (cond-> (assoc record :dispatch/outcome outcome)
                              (some? detail) (assoc :dispatch/detail detail)))))

(defn- read-dispatch
  [state dispatch-key]
  (some-> (get-in state [:dispatches dispatch-key]) dispatch-law/assert-record!))

(defn- read-batch-dispatch
  [state batch-id document-wire-id]
  (some-> (batch-document-match (:dispatches state) batch-id document-wire-id)
          dispatch-law/assert-record!))

(defn- read-batch-only-dispatch
  [state batch-id]
  (some-> (->> (vals (:dispatches state))
               (filter #(= batch-id (:dispatch/batch-id %)))
               first)
          dispatch-law/assert-record!))

(defn- append-receipt-in-state
  [current receipt]
  (update current :receipts conj receipt))

(defn memory-store
  "An `ITranslationEvidenceStore` over one atom.

   Used by tests and by the verification script. It is not a fallback for the
   durable store: a binding lost on restart can never be joined to the worker's
   answer, so the translation would complete and no receipt would ever exist."
  []
  (let [state (atom {:dispatches {} :receipts [] :approvals {}})]
    (reify ITranslationEvidenceStore
      (reserve-dispatch! [_ record]
        (js/Promise.resolve
         (checked-reservation
          (:answer (swap! state reserve-in-state
                          (dispatch-law/assert-record! record))))))

      (resolve-dispatch! [_ dispatch-key outcome detail]
        (js/Promise.resolve
         (resolved-answer (swap! state resolve-outcome-in-state
                                 dispatch-key outcome detail))))

      (bind-dispatch-batch! [_ dispatch-key batch-id]
        (js/Promise.resolve
         (resolved-answer (swap! state update-in-flight-state dispatch-key
                                 #(assoc % :dispatch/batch-id batch-id)))))

      (dispatch-for-key! [_ dispatch-key]
        (js/Promise.resolve (read-dispatch @state dispatch-key)))

      (dispatch-for-batch-document! [_ batch-id document-wire-id]
        (js/Promise.resolve (read-batch-dispatch @state batch-id document-wire-id)))

      (dispatch-for-batch! [_ batch-id]
        (js/Promise.resolve (read-batch-only-dispatch @state batch-id)))

      (record-translation! [_ receipt]
        (let [checked (evidence-law/assert-receipt! receipt)]
          (swap! state append-receipt-in-state checked)
          (js/Promise.resolve checked)))

      (completed-translations! [_]
        (js/Promise.resolve (mapv evidence-law/assert-receipt! (:receipts @state))))

      (record-approval! [_ approval]
        (let [checked (evidence-law/assert-approval! approval)]
          (js/Promise.resolve
           (:answer (swap! state record-approval-in-state
                           (approval-identity checked) checked)))))

      (approvals! [_]
        (js/Promise.resolve (mapv evidence-law/assert-approval!
                                  (vals (:approvals @state))))))))

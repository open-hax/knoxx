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
     work still needs doing. A completed or duplicate claim is also replaceable
     when `record` carries the validated `:candidate-unavailable` recovery
     reason; this is the explicit repair path for translated candidate bytes
     that can no longer be read. Ordinary terminal claims remain `:done`.
     Reporting a failed attempt as done strands that source revision forever:
     the gate keeps reporting the translation missing while every later pass
     answers duplicate.

     Implementations must contain no `await` between reading the key and
     claiming it.")
  (resolve-dispatch! [store expected-record outcome detail]
    "Move the exact in-flight attempt named by `expected-record` to a terminal
     `outcome`. Returns a Promise of the updated record, or nil when that attempt
     is no longer active — so a delayed attempt cannot resolve a replacement
     that intentionally reuses the same logical dispatch key.

     `detail` is free-text evidence for a human and never a decision input.
     A completion owner blocks this transition: once receipt persistence starts,
     a racing failure may no longer invalidate the attempt that earned it.")
  (bind-dispatch-batch! [store expected-record batch-id]
    "Record the batch id the worker assigned to the exact accepted attempt.
     Returns a Promise of the updated record, or nil when that attempt is no
     longer active. A key-only compare is forbidden because a delayed bind from
     an earlier attempt could otherwise attach its batch to a replacement.")
  (claim-dispatch-completion! [store expected-record]
    "Claim exclusive completion ownership for the exact accepted attempt.

     Returns a Promise of that attempt when the claim is acquired or already
     belongs to the same attempt, else nil. While held, ordinary resolve and bind
     transitions must fail. This is the first phase of receipt-before-completed
     settlement: a loser learns it lost before it can expose a receipt, while a
     crash leaves an idempotently resumable claim rather than a completed record
     with no receipt.")
  (finish-dispatch-completion! [store expected-record detail]
    "After the receipt is durable, move its exact completion-owned attempt to
     `:dispatch/completed` and release the owner.

     Returns the completed record. An equal retry may receive the already-
     completed same attempt; a stale or different attempt receives nil.")
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
    "Record immutable completed-translation evidence at most once per stable
     receipt identity.

     A first write returns the receipt. An equal retry may carry a newly sampled
     `:translation/at`; it returns the first stored receipt so that retry timing
     cannot rewrite historical ordering. A retry that changes any other fact at
     the same identity is an immutable-receipt conflict and must fail.")
  (completed-translations! [store scope]
    "Completed translation receipts within `scope`. Returns a Promise.

     `scope` is `{:org-id o :project p}`, and it is a *query* scope rather than a
     filter applied afterwards. Receipts are append-only, so reading them all and
     narrowing in memory made every dispatch pass grow with the global history of
     every tenant — and left the collection's own indexes unused. An
     implementation is expected to push both coordinates into its query.

     A nil `:project` means receipts that name no project, not every project: an
     unset project is its own scope, exactly as it is in the dispatch key.

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
  (approvals! [store scope]
    "Approvals within `scope`, which means the same thing it does for
     `completed-translations!` and is pushed into the query for the same reason.
     Returns a Promise.

     Order is not part of the contract:
     `domain.translation-evidence/index-approvals` retains all of them and the
     join against the receipt decides which is current."))

;; ── Validation helpers ─────────────────────────────────────────────────────

(defn assert-ordinary-resolution-outcome!
  "Validate an outcome written outside the receipt-backed completion protocol.

   `:dispatch/completed` is deliberately excluded: only
   `finish-dispatch-completion!`, after a durable receipt, may create that state."
  [outcome]
  (when (or (not (contains? dispatch-law/outcomes outcome))
            (= :dispatch/accepted outcome)
            (= :dispatch/completed outcome))
    (throw (ex-info "invalid ordinary translation dispatch resolution"
                    {:dispatch/outcome outcome})))
  outcome)

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

   A replaceable existing claim is replaced by `record` outright rather than
   reported as settled. That includes ordinary failed/rejected retries and the
   explicit candidate-unavailable recovery of a completed claim. Replacing
   wholesale is what clears the previous attempt's batch id: left behind, the
   old batch's completion report would resolve the new attempt, minting a receipt
   for a translation the new attempt never produced. See
   `law.translation-dispatch/replaceable-claim?` for the complete admission law."
  [current record]
  (let [dispatch-key (:dispatch/key record)
        existing (get-in current [:dispatches dispatch-key])]
    (cond
      (nil? existing)
      (-> current
          (assoc-in [:dispatches dispatch-key] record)
          (update :completion-owners dissoc dispatch-key)
          (assoc :answer {:reservation/status :reserved :record record}))

      (in-flight? existing)
      (assoc current :answer {:reservation/status :in-flight :record existing})

      (dispatch-law/replaceable-claim? record existing)
      (-> current
          (assoc-in [:dispatches dispatch-key] record)
          (update :completion-owners dissoc dispatch-key)
          (assoc :answer {:reservation/status :reserved :record record}))

      :else
      (assoc current :answer {:reservation/status :done :record existing}))))

(defn- completion-owner
  "Stable store-private owner for one immutable dispatch attempt."
  [record]
  (pr-str (dispatch-law/attempt-binding record)))

(defn- exact-active-attempt?
  [current expected-record]
  (let [existing (get-in current [:dispatches (:dispatch/key expected-record)])]
    (and existing
         (in-flight? existing)
         (dispatch-law/same-attempt? existing expected-record))))

(defn- update-in-flight-state
  "Apply `f` to the exact active attempt, or answer nil when it moved.

   Refusing to touch a terminal record is the compare-and-set the Mongo
   implementation expresses as a query predicate: whoever resolves the claim
   first wins, and the loser learns it lost instead of overwriting an outcome.
   The completion-owner predicate closes the success-vs-failure window: once
   receipt persistence has begun, only the completion finisher may move it."
  [current expected-record f]
  (let [dispatch-key (:dispatch/key expected-record)
        existing (get-in current [:dispatches dispatch-key])]
    (if (and (exact-active-attempt? current expected-record)
             (nil? (get-in current [:completion-owners dispatch-key])))
      (let [updated (f existing)]
        (-> current
            (assoc-in [:dispatches dispatch-key] updated)
            (assoc :answer updated)))
      (assoc current :answer nil))))

(defn- claim-completion-in-state
  "Acquire, or idempotently resume, completion for one exact attempt."
  [current expected-record]
  (let [dispatch-key (:dispatch/key expected-record)
        existing (get-in current [:dispatches dispatch-key])
        owner (completion-owner expected-record)
        held-by (get-in current [:completion-owners dispatch-key])]
    (cond
      ;; An equal callback can read accepted just before another equal callback
      ;; finishes. Treat that post-read/pre-claim schedule as an idempotent claim,
      ;; not a nondeterministic already-resolved refusal.
      (and existing
           (= :dispatch/completed (:dispatch/outcome existing))
           (dispatch-law/same-attempt? existing expected-record))
      (assoc current :answer existing)

      (not (exact-active-attempt? current expected-record))
      (assoc current :answer nil)

      (or (nil? held-by) (= owner held-by))
      (-> current
          (assoc-in [:completion-owners dispatch-key] owner)
          (assoc :answer (get-in current [:dispatches dispatch-key])))

      :else
      (assoc current :answer nil))))

(defn- finish-completion-in-state
  "Complete the exact attempt after its receipt is durable.

   An already-completed equal attempt is an idempotent finish: two equal
   callbacks may both persist the same receipt, but only one changes the claim."
  [current expected-record detail]
  (let [dispatch-key (:dispatch/key expected-record)
        existing (get-in current [:dispatches dispatch-key])
        owner (completion-owner expected-record)
        held-by (get-in current [:completion-owners dispatch-key])]
    (cond
      (and existing
           (= :dispatch/completed (:dispatch/outcome existing))
           (dispatch-law/same-attempt? existing expected-record))
      (assoc current :answer existing)

      (and (exact-active-attempt? current expected-record)
           (= owner held-by))
      (let [updated (cond-> (assoc existing :dispatch/outcome :dispatch/completed)
                      (some? detail) (assoc :dispatch/detail detail))]
        (-> current
            (assoc-in [:dispatches dispatch-key] updated)
            (update :completion-owners dissoc dispatch-key)
            (assoc :answer updated)))

      :else
      (assoc current :answer nil))))

(defn- resolved-answer
  "The answer left by a transition, validated as a record."
  [state]
  (some-> (:answer state) dispatch-law/assert-record!))

(defn receipt-in-scope?
  "Whether `receipt` belongs to `scope`.

   The in-memory store's equivalent of the durable store's query predicate, so
   both narrow identically. A nil project matches only receipts naming none."
  [receipt {:keys [org-id project]}]
  (and (= org-id (:translation/org-id receipt))
       (= project (:translation/project receipt))))

(defn receipt-visible-for-dispatch?
  "Whether `receipt` is admissible as completed evidence beside `record`.

   Historical receipts have no attempt coordinate and retain their existing
   compatibility behavior. A newly written receipt is invisible until the
   current dispatch row is completed by that exact immutable attempt. This keeps
   the receipt-before-completed crash window recoverable without letting the
   gate, review inventory, or publication listing consume provisional evidence."
  [receipt record]
  (if-let [attempt-id (:translation/dispatch-attempt-id receipt)]
    (and record
         (= :dispatch/completed (:dispatch/outcome record))
         (= (:translation/dispatch-key receipt) (:dispatch/key record))
         (= attempt-id (:dispatch/attempt-id record)))
    true))

(defn- visible-memory-receipt?
  [state receipt]
  (receipt-visible-for-dispatch?
   receipt
   (get-in state [:dispatches (:translation/dispatch-key receipt)])))

(defn receipt-identity
  "The durable identity at most one completed receipt may occupy.

   Every exact coordinate is included even where the dispatch key currently
   derives from some of them. That makes the persistence law explicit and keeps
   a future dispatch-key encoding change from collapsing different resource,
   source, or output facts. Content digest and split lineage are deliberately
   not identity: changing either at these same coordinates is a conflict, not a
   second completion. `:translation/at` is retry metadata and is also excluded
   so the first recorded timestamp wins."
  [receipt]
  [(:translation/org-id receipt)
   (:translation/project receipt)
   (:translation/garden receipt)
   (:translation/document receipt)
   (:translation/source-locale receipt)
   (:translation/locale receipt)
   (:translation/source-revision receipt)
   (:translation/revision receipt)
   (:translation/dispatch-key receipt)])

(defn receipt-replay?
  "Whether `attempted` repeats `existing` without changing an immutable fact.

   A caller retrying after an uncertain response naturally samples a later
   timestamp. Ignoring only that field makes the retry idempotent while still
   refusing changed target bytes, tenant scope, or split lineage."
  [existing attempted]
  (= (dissoc existing :translation/at)
     (dissoc attempted :translation/at)))

(defn legacy-compatible-receipt-replay?
  "Whether one pre-attempt receipt and one attempt-bound receipt state one fact.

   This is a bounded rolling-deploy exception, not the ordinary replay law.
   Exactly one side must lack the attempt coordinate and every historical fact
   besides retry time must agree. Two attempt-bound receipts with different
   attempt ids remain a conflict at the same stable identity."
  [left right]
  (let [left-bound? (contains? left :translation/dispatch-attempt-id)
        right-bound? (contains? right :translation/dispatch-attempt-id)]
    (and (not= left-bound? right-bound?)
         (= (dissoc left :translation/at :translation/dispatch-attempt-id)
            (dissoc right :translation/at :translation/dispatch-attempt-id)))))

(defn first-receipt-or-conflict!
  "Return `existing` for an equal or legacy-compatible retry.

   The compatibility branch lets a new writer settle against the same receipt
   inserted by an old binary during a rolling deployment. It never masks a
   changed content, lineage, resource, or pair of concrete attempt identities."
  [existing attempted]
  (if (or (receipt-replay? existing attempted)
          (legacy-compatible-receipt-replay? existing attempted))
    existing
    (throw (ex-info "conflicting completed translation receipt"
                    {:cause :translation-receipt-conflict
                     :translation/receipt-identity
                     (receipt-identity attempted)
                     :existing existing
                     :attempted attempted}))))

(defn collapse-receipts!
  "Collapse duplicate receipt identities, failing closed on disagreement.

   Historical unkeyed rows can coexist with a keyed row when old and new
   binaries write concurrently under a sparse unique index. Equal facts reduce
   to the earliest asserted timestamp, with the full printed fact as a stable
   tie-break. A changed fact throws instead of letting store return order choose
   which translation the gate or review inventory consumes."
  [receipts]
  (->> receipts
       (group-by receipt-identity)
       (sort-by (comp pr-str key))
       (mapv (fn [[_ duplicates]]
               (let [[first-receipt & rest-receipts]
                     (sort-by (juxt :translation/at pr-str) duplicates)]
                 (doseq [receipt rest-receipts]
                   (first-receipt-or-conflict! first-receipt receipt))
                 first-receipt)))))

(defn approval-in-scope?
  "Whether `approval` belongs to `scope`. See `receipt-in-scope?`."
  [approval {:keys [org-id project]}]
  (and (= org-id (:review/org-id approval))
       (= project (:review/project approval))))

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
   (:review/garden approval)
   (:review/source-locale approval)
   (:review/locale approval)
   (:review/revision approval)
   (:review/content-digest approval)
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
  [current expected-record outcome detail]
  (update-in-flight-state current expected-record
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

(defn- record-receipt-in-state
  "Claim one immutable receipt identity, or return the first equal fact.

   The decision and write happen in the same `swap!`, so two callers cannot
   both observe absence. Throwing from the transition leaves the atom unchanged."
  [current receipt]
  (let [identity (receipt-identity receipt)]
    (if-let [existing (get-in current [:receipts identity])]
      (assoc current :answer (first-receipt-or-conflict! existing receipt))
      (-> current
          (assoc-in [:receipts identity] receipt)
          (assoc :answer receipt)))))

(defn- reserve-memory!
  [state record]
  (js/Promise.resolve
   (checked-reservation
    (:answer (swap! state reserve-in-state
                    (dispatch-law/assert-record! record))))))

(defn- resolve-memory!
  [state expected-record outcome detail]
  (let [checked-outcome (assert-ordinary-resolution-outcome! outcome)]
    (js/Promise.resolve
     (resolved-answer (swap! state resolve-outcome-in-state
                             (dispatch-law/assert-record! expected-record)
                             checked-outcome detail)))))

(defn- bind-memory!
  [state expected-record batch-id]
  (js/Promise.resolve
   (resolved-answer (swap! state update-in-flight-state
                           (dispatch-law/assert-record! expected-record)
                           #(assoc % :dispatch/batch-id batch-id)))))

(defn- claim-memory-completion!
  [state expected-record]
  (js/Promise.resolve
   (resolved-answer (swap! state claim-completion-in-state
                           (dispatch-law/assert-record! expected-record)))))

(defn- finish-memory-completion!
  [state expected-record detail]
  (js/Promise.resolve
   (resolved-answer (swap! state finish-completion-in-state
                           (dispatch-law/assert-record! expected-record)
                           detail))))

(defn- record-memory-receipt!
  [state receipt]
  (let [checked (evidence-law/assert-receipt! receipt)]
    (js/Promise.resolve
     (:answer (swap! state record-receipt-in-state checked)))))

(defn- memory-receipts
  [state scope]
  (let [snapshot @state]
    (js/Promise.resolve
     (into [] (comp (map evidence-law/assert-receipt!)
                    (filter #(receipt-in-scope? % scope))
                    (filter #(visible-memory-receipt? snapshot %)))
           (vals (:receipts snapshot))))))

(defn- record-memory-approval!
  [state approval]
  (let [checked (evidence-law/assert-approval! approval)]
    (js/Promise.resolve
     (:answer (swap! state record-approval-in-state
                     (approval-identity checked) checked)))))

(defn- memory-approvals
  [state scope]
  (js/Promise.resolve
   (into [] (comp (map evidence-law/assert-approval!)
                  (filter #(approval-in-scope? % scope)))
         (vals (:approvals @state)))))

(defn memory-store
  "An `ITranslationEvidenceStore` over one atom.

   Used by tests and by the verification script. It is not a fallback for the
   durable store: a binding lost on restart can never be joined to the worker's
   answer, so the translation would complete and no receipt would ever exist."
  []
  (let [state (atom {:dispatches {}
                     :completion-owners {}
                     :receipts {}
                     :approvals {}})]
    (reify ITranslationEvidenceStore
      (reserve-dispatch! [_ record]
        (reserve-memory! state record))

      (resolve-dispatch! [_ expected-record outcome detail]
        (resolve-memory! state expected-record outcome detail))

      (bind-dispatch-batch! [_ expected-record batch-id]
        (bind-memory! state expected-record batch-id))

      (claim-dispatch-completion! [_ expected-record]
        (claim-memory-completion! state expected-record))

      (finish-dispatch-completion! [_ expected-record detail]
        (finish-memory-completion! state expected-record detail))

      (dispatch-for-key! [_ dispatch-key]
        (js/Promise.resolve (read-dispatch @state dispatch-key)))

      (dispatch-for-batch-document! [_ batch-id document-wire-id]
        (js/Promise.resolve (read-batch-dispatch @state batch-id document-wire-id)))

      (dispatch-for-batch! [_ batch-id]
        (js/Promise.resolve (read-batch-only-dispatch @state batch-id)))

      (record-translation! [_ receipt]
        (record-memory-receipt! state receipt))

      (completed-translations! [_ scope]
        (memory-receipts state scope))

      (record-approval! [_ approval]
        (record-memory-approval! state approval))

      (approvals! [_ scope]
        (memory-approvals state scope)))))

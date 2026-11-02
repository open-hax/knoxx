(ns knoxx.backend.infra.translation-dispatch-recovery-test
  "Translation dispatch recovery contracts and finite fixtures."
  (:require [cljs.test :as t]
            [knoxx.backend.infra.translation-dispatch :as dispatch]
            [knoxx.backend.infra.translation-dispatch-fixture :as fixture]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-dispatch :as law]))

(t/deftest ^:async a-pin-that-was-never-the-bytes-on-disk-is-refused-before-dispatch
  ;; The mismatch this catches *predates* dispatch, so no drift guard can see it:
  ;; the intent pins a historical or opaque revision, the observed digest is the
  ;; current content, the worker translates that current content, and completion
  ;; compares the current digest against the current digest. They agree, and a
  ;; receipt claims the pinned revision was translated. False, with nothing ever
  ;; having changed.
  (let [{:keys [batches deps]} (fixture/fixture)
        pinned-context (dispatch/dispatch-context fixture/intent fixture/scope "sha256-what-is-there-now")
        result (await (dispatch/dispatch-work! deps (fixture/work) pinned-context))]
    (t/testing "no batch is created for a revision that cannot be substantiated"
      (t/is (empty? @batches)))

    (t/testing "the refusal names both the pin and the observable digest"
      (t/is (= :pin-not-tied-to-observable-bytes
             (:refusal/type (:translation/refusal result))))
      (t/is (= fixture/dispatched-revision (:refusal/expected (:translation/refusal result))))
      (t/is (= "sha256-what-is-there-now"
             (:refusal/actual (:translation/refusal result)))))

    (t/testing "the outcome is terminal, because no retry makes a pin resolvable"
      (t/is (= :dispatch/unreachable (:dispatch/outcome result)))
      (t/is (not (law/retriable? (:dispatch/outcome result)))))

    (t/testing "nothing is persisted, so the refusal cannot outlive its reason"
      ;; A pin refusal is a decision about *current* state, not an observed
      ;; fact. Recorded as a terminal claim it would keep blocking a pin that had
      ;; since become valid.
      (t/is (nil? (await (store/dispatch-for-key!
                        (:evidence-store deps)
                        (:dispatch/key (:dispatch/record result)))))))))

(t/deftest ^:async a-pin-becomes-dispatchable-once-its-bytes-are-current
  ;; The consequence of not persisting the refusal: restore the checkout to the
  ;; pinned bytes and the same intent dispatches normally, instead of being
  ;; blocked forever by a terminal claim that outlived its own reason.
  (let [{:keys [batches deps]} (fixture/fixture)
        stale-context (dispatch/dispatch-context fixture/intent fixture/scope "sha256-not-yet-restored")
        refused (await (dispatch/dispatch-work! deps (fixture/work) stale-context))
        after-refusal (count @batches)
        restored (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "the first attempt is refused and enqueues nothing"
      (t/is (= :dispatch/unreachable (:dispatch/outcome refused)))
      (t/is (zero? after-refusal)))

    (t/testing "once the bytes match the pin, the same work dispatches"
      (t/is (= :dispatch/accepted (:dispatch/outcome restored)))
      (t/is (= 1 (count @batches))))))

(t/deftest ^:async a-revision-equal-to-the-observed-digest-dispatches-normally
  ;; A `:source/current` intent resolves to exactly this digest, so the two are
  ;; equal by construction; a pin that happens to name the current content is
  ;; equally fine. The refusal must not catch either.
  (let [{:keys [batches deps]} (fixture/fixture)
        result (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/is (= :dispatch/accepted (:dispatch/outcome result)))
    (t/is (= 1 (count @batches)))
    (t/is (nil? (:translation/refusal result)))))

(t/deftest ^:async recovery-mints-a-receipt-when-the-batch-completed-and-the-source-held
  ;; The happy path of `recover-settled-batch!`: the batch reports complete, the
  ;; source has not moved, so the receipt lost by failed bookkeeping is
  ;; reconstructed. Without this, a regression in `complete-if-source-agrees!`
  ;; inside the recovery context would only be caught on the drift branch.
  (let [{:keys [deps]} (fixture/fixture :batch-status "complete")
        first-result (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
        recovered (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
        receipt (:translation/receipt recovered)]
    (t/testing "the first pass leaves an in-flight claim bound to a batch"
      (t/is (= :dispatch/accepted (:dispatch/outcome first-result)))
      (t/is (= "batch-1" (:dispatch/batch-id (:dispatch/record first-result)))))

    (t/testing "the next pass reconstructs the receipt from the batch's own status"
      (t/is (= :dispatch/completed (:dispatch/outcome recovered)))
      (t/is (some? receipt))
      (t/is (= fixture/dispatched-revision (:translation/source-revision receipt)))
      (t/is (= :es (:translation/locale receipt))))

    (t/testing "the receipt is durable and the claim is settled"
      (t/is (= 1 (count (await (store/completed-translations!
                              (:evidence-store deps) fixture/evidence-scope)))))
      (t/is (= :dispatch/completed
             (:dispatch/outcome (await (store/dispatch-for-key!
                                        (:evidence-store deps)
                                        (:dispatch/key (:dispatch/record first-result))))))))

    (t/testing "the record it answers with agrees with the outcome it reports"
      ;; It used to answer with the *pre-recovery* record, whose own
      ;; `:dispatch/outcome` still read `:dispatch/accepted` while the map
      ;; around it said completed. A caller reading the outcome off the record
      ;; — the obvious thing to do with a returned record — saw an in-flight
      ;; claim that had in fact been settled, and would keep waiting on it.
      (t/is (= :dispatch/completed
             (:dispatch/outcome (:dispatch/record recovered))))
      (t/is (= (:dispatch/key (:dispatch/record first-result))
             (:dispatch/key (:dispatch/record recovered)))))

    (t/testing "a third pass has nothing left to do"
      (t/is (= :dispatch/duplicate
             (:dispatch/outcome (await (dispatch/dispatch-work!
                                        deps (fixture/work) (fixture/context)))))))

    (t/testing "no second receipt was minted along the way"
      ;; The failure this guards is the loop: an outcome that never reports
      ;; completed leaves the claim readable as in-flight, so every later pass
      ;; recovers the same batch and attempts another receipt.
      (t/is (= 1 (count (await (store/completed-translations!
                              (:evidence-store deps) fixture/evidence-scope))))))))

(t/deftest ^:async a-refused-drifted-completion-answers-with-the-settled-claim
  ;; The same self-consistency the completed branch needs, on the branch that
  ;; settles a claim it cannot substantiate. `refuse-drifted-completion!` marks
  ;; the claim unreachable; answering with the pre-refusal record would report a
  ;; claim as still accepted at the same moment it was closed for good.
  (let [{:keys [deps]} (fixture/fixture :batch-status "complete"
                                :source-revision "sha256-moved00000000")
        _ (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
        drifted (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "the completion is refused for drift"
      (t/is (= :source-moved-since-dispatch
             (:refusal/type (:translation/refusal drifted))))
      (t/is (nil? (:translation/receipt drifted))))

    (t/testing "the claim is terminal, and the answer says so"
      (t/is (= law/unreachable-outcome (:dispatch/outcome drifted)))
      (t/is (= law/unreachable-outcome
             (:dispatch/outcome (:dispatch/record drifted)))))

    (t/testing "nothing was recorded as a translation"
      (t/is (empty? (await (store/completed-translations!
                          (:evidence-store deps) fixture/evidence-scope)))))))

(t/deftest ^:async a-known-batch-id-is-recovered-when-the-binding-write-fails
  ;; A failure *after* the batch id is known is not an ambiguous send: the batch
  ;; exists and can be named. Routing it through observation would be strictly
  ;; worse without durable correlation. The worker now stores the dispatch key,
  ;; so the next reconciliation pass can recover the exact batch and repair the
  ;; binding without creating a duplicate.
  (let [attempts (atom 0)
        failing-store (let [inner (store/memory-store)]
                        (reify store/ITranslationEvidenceStore
                          (reserve-dispatch! [_ r] (store/reserve-dispatch! inner r))
                          (resolve-dispatch! [_ k o d] (store/resolve-dispatch! inner k o d))
                          (bind-dispatch-batch! [_ k b]
                            (if (<= (swap! attempts inc) 2)
                              (throw (ex-info "write failed" {}))
                              (store/bind-dispatch-batch! inner k b)))
                          (claim-dispatch-completion! [_ r]
                            (store/claim-dispatch-completion! inner r))
                          (finish-dispatch-completion! [_ r d]
                            (store/finish-dispatch-completion! inner r d))
                          (dispatch-for-key! [_ k] (store/dispatch-for-key! inner k))
                          (dispatch-for-batch-document! [_ b d]
                            (store/dispatch-for-batch-document! inner b d))
                          (dispatch-for-batch! [_ b] (store/dispatch-for-batch! inner b))
                          (record-translation! [_ r] (store/record-translation! inner r))
                          (completed-translations! [_ s] (store/completed-translations! inner s))
                          (record-approval! [_ a] (store/record-approval! inner a))
                          (approvals! [_ s] (store/approvals! inner s))))
        batches (atom [])
        deps {:evidence-store failing-store
              :client (fixture/fake-client
                       {:batches batches
                        :observed (fn [opts]
                                    {:batches [{:batch_id "batch-1"
                                                :dispatch_key (:dispatch_key opts)
                                                :created_at "2026-08-22T09:00:00.000Z"
                                                :document_ids ["knoxx.docs/probe"]}]})})
              :clock fixture/clock
              :observe-source-revision (constantly (js/Promise.resolve fixture/dispatched-revision))}
        result (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
        attempts-after-first @attempts
        recovered (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "the binding is retried, bounded"
      (t/is (= 2 attempts-after-first) "one attempt plus one retry, not a loop"))

    (t/testing "the first pass stays in flight and preserves the recovery route"
      (t/is (= :dispatch/accepted (:dispatch/outcome result)))
      (t/is (re-find #"batch-1" (:dispatch/detail result)))
      (t/is (re-find #"recoverable by dispatch key" (:dispatch/detail result))))

    (t/testing "the next pass binds the exact queued batch"
      (t/is (= 3 @attempts))
      (t/is (= "batch-1" (:dispatch/batch-id (:dispatch/record recovered))))
      (t/is (re-find #"durable dispatch key" (:dispatch/detail recovered))))

    (t/testing "the worker was asked exactly once across both passes"
      (t/is (= 1 (count @batches))))))

(t/deftest ^:async recovery-reads-the-batch-per-document-not-its-overall-status
  ;; The status route accumulates `completed_documents` and `failed_documents` on
  ;; the batch with `$push`, so the batch names which documents finished. Reading
  ;; the batch-wide status instead only worked because a Knoxx-created batch
  ;; happens to carry one document — an assumption this no longer needs.
  (t/testing "a partial batch that completed OUR document mints a receipt"
    ;; `partial` means *some* document failed. Under status-based recovery this
    ;; produced a duplicate and the earned receipt stayed lost.
    (let [{:keys [deps]} (fixture/fixture :batch-view {:status "partial"
                                               :completed_documents ["knoxx.docs/probe"]
                                               :failed_documents [{:document_id "other/doc"
                                                                   :error "boom"}]})
          _ (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
          recovered (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
      (t/is (= :dispatch/completed (:dispatch/outcome recovered)))
      (t/is (some? (:translation/receipt recovered)))))

  (t/testing "a complete batch that did NOT name our document is still running"
    ;; The inverse: a batch-wide `complete` must not be read as our document
    ;; having finished when the arrays do not say so.
    (let [{:keys [deps]} (fixture/fixture :batch-view {:status "complete"
                                               :completed_documents ["someone/else"]
                                               :failed_documents []})
          _ (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
          recovered (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
      (t/is (= :dispatch/duplicate (:dispatch/outcome recovered)))
      (t/is (empty? (await (store/completed-translations!
                          (:evidence-store deps) fixture/evidence-scope))))))

  (t/testing "a named failure makes the claim retriable"
    (let [{:keys [deps]} (fixture/fixture :batch-view
                                  {:status "partial"
                                   :completed_documents []
                                   :failed_documents [{:document_id "knoxx.docs/probe"
                                                       :error "model unavailable"}]})
          _ (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
          recovered (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
      (t/is (= :dispatch/failed (:dispatch/outcome recovered)))))

  (t/testing "a document named both completed and failed reads as failed"
    ;; The pessimistic reading is the one that cannot fabricate a receipt.
    (let [{:keys [deps]} (fixture/fixture :batch-view
                                  {:status "partial"
                                   :completed_documents ["knoxx.docs/probe"]
                                   :failed_documents [{:document_id "knoxx.docs/probe"
                                                       :error "retried"}]})
          _ (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
          recovered (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
      (t/is (= :dispatch/failed (:dispatch/outcome recovered)))
      (t/is (empty? (await (store/completed-translations!
                          (:evidence-store deps) fixture/evidence-scope)))))))

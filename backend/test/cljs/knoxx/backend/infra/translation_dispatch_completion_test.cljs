(ns knoxx.backend.infra.translation-dispatch-completion-test
  "Translation dispatch completion contracts and finite fixtures."
  (:require [cljs.test :as t]
            [knoxx.backend.domain.translation-evidence :as evidence-domain]
            [knoxx.backend.infra.translation-dispatch :as dispatch]
            [knoxx.backend.infra.translation-dispatch-fixture :as fixture]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-dispatch :as law]))

(defn- completion-fault [fail-on]
  (let [failed? (atom false)]
    (fn [phase]
      (when (and (= fail-on phase)
                 (compare-and-set! failed? false true))
        (throw (ex-info (str "simulated crash at " (name phase))
                        {:phase phase}))))))

(defn- completion-fault-store
  "Delegate to `inner`, throwing once at one completion phase."
  [inner fail-on]
  (let [maybe-fail! (completion-fault fail-on)]
    (reify store/ITranslationEvidenceStore
      (reserve-dispatch! [_ record] (store/reserve-dispatch! inner record))
      (resolve-dispatch! [_ record outcome detail]
        (store/resolve-dispatch! inner record outcome detail))
      (bind-dispatch-batch! [_ record batch-id]
        (store/bind-dispatch-batch! inner record batch-id))
      (claim-dispatch-completion! [_ record]
        (store/claim-dispatch-completion! inner record))
      (finish-dispatch-completion! [_ record detail]
        (maybe-fail! :before-finish)
        (store/finish-dispatch-completion! inner record detail))
      (dispatch-for-key! [_ dispatch-key]
        (store/dispatch-for-key! inner dispatch-key))
      (dispatch-for-batch-document! [_ batch-id document-wire-id]
        (store/dispatch-for-batch-document! inner batch-id document-wire-id))
      (dispatch-for-batch! [_ batch-id]
        (store/dispatch-for-batch! inner batch-id))
      (record-translation! [_ receipt]
        (maybe-fail! :before-receipt)
        (store/record-translation! inner receipt))
      (completed-translations! [_ requested-scope]
        (store/completed-translations! inner requested-scope))
      (record-approval! [_ approval]
        (store/record-approval! inner approval))
      (approvals! [_ requested-scope]
        (store/approvals! inner requested-scope)))))

(t/deftest ^:async a-completed-report-becomes-evidence-the-gate-recognizes
  (let [{base-deps :deps} (fixture/fixture)
        deps (assoc base-deps :translation-content-digest
                    "sha256-target-content")
        _ (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
        resolved (await (dispatch/resolve-batch-report!
                         deps
                         {:status "partial"
                          :batch_id "batch-1"
                          :completed_document "knoxx.docs/probe"}))
        receipt (:translation/receipt resolved)]
    (t/testing "a receipt is minted against the bound revision"
      (t/is (some? receipt))
      (t/is (= "sha256-aaa111bbb222" (:translation/source-revision receipt)))
      (t/is (= :es (:translation/locale receipt)))
      (t/is (= :en (:translation/source-locale receipt))))

    (t/testing "the gate now sees the translation"
      ;; The DoD line this proves: 'a validated, revision-specific receipt that
      ;; the publication gate recognizes'.
      (let [loaded (evidence-domain/evidence
                    {:receipts (await (store/completed-translations!
                                       (:evidence-store deps) fixture/evidence-scope))})
            gate-facts (evidence-domain/gate-facts loaded)]
        (t/is ((:translated-revision? gate-facts)
             :knoxx.docs/probe :knoxx.docs/promethean :es "sha256-aaa111bbb222"))
        (t/is (not ((:translated-revision? gate-facts)
                  :knoxx.docs/probe :knoxx.docs/promethean :fr "sha256-aaa111bbb222")))))

    (t/testing "the same report a second time cannot mint a second receipt"
      (let [again (await (dispatch/resolve-batch-report!
                          deps
                          {:status "complete"
                           :batch_id "batch-1"
                           :completed_document "knoxx.docs/probe"}))]
        (t/is (= :dispatch-already-resolved
               (:refusal/type (:translation/refusal again))))
        (t/is (= 1 (count (await (store/completed-translations!
                                (:evidence-store deps) fixture/evidence-scope)))))))))

(t/deftest ^:async completion-crashes-resume-without-exposing-provisional-evidence
  (doseq [fail-on [:before-receipt :before-finish]]
    (let [{base-deps :deps} (fixture/fixture)
          inner (store/memory-store)
          evidence-store (completion-fault-store inner fail-on)
          deps (assoc base-deps :evidence-store evidence-store)
          dispatched (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
          report {:status "complete"
                  :batch_id "batch-1"
                  :completed_document "knoxx.docs/probe"}
          first-error (try
                        (await (dispatch/resolve-batch-report! deps report))
                        nil
                        (catch :default error error))
          after-crash (await (store/dispatch-for-key!
                              evidence-store
                              (:dispatch/key (:dispatch/record dispatched))))]
      (t/testing (str fail-on " leaves a resumable accepted claim")
        (t/is (some? first-error))
        (t/is (= fail-on (:phase (ex-data first-error))))
        (t/is (= :dispatch/accepted (:dispatch/outcome after-crash))))

      (t/testing (str fail-on " exposes no receipt to a production evidence snapshot")
        ;; `:before-finish` has already inserted the immutable receipt. It must
        ;; remain provisional until this exact attempt is durably completed.
        (t/is (empty? (await (store/completed-translations!
                            evidence-store fixture/evidence-scope)))))

      (let [recovered (await (dispatch/resolve-batch-report! deps report))
            receipts (await (store/completed-translations!
                             evidence-store fixture/evidence-scope))]
        (t/testing (str fail-on " retries the same owner and completes exactly once")
          (t/is (= :dispatch/completed
                 (:dispatch/outcome (:dispatch/record recovered))))
          (t/is (= 1 (count receipts)))
          (t/is (= (:dispatch/attempt-id (:dispatch/record recovered))
                 (:translation/dispatch-attempt-id (first receipts)))))))))

(t/deftest ^:async a-stale-completion-cannot-settle-a-replacement-attempt
  (let [{base-deps :deps} (fixture/fixture)
        observer-entered-resolve (atom nil)
        observer-entered (js/Promise.
                          (fn [resolve! _reject]
                            (reset! observer-entered-resolve resolve!)))
        source-release (atom nil)
        source-result (js/Promise.
                       (fn [resolve! _reject]
                         (reset! source-release resolve!)))
        deps (assoc base-deps
                    :observe-source-revision
                    (fn [_]
                      (@observer-entered-resolve true)
                      source-result))
        dispatched (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
        completion (dispatch/resolve-batch-report!
                    deps {:status "complete"
                          :batch_id "batch-1"
                          :completed_document "knoxx.docs/probe"})
        _ (await observer-entered)
        failed (await (dispatch/fail-batch-document!
                       deps "batch-1" "knoxx.docs/probe" "provider failed"))
        replacement (-> (:dispatch/record dispatched)
                        (assoc :dispatch/attempt-id "replacement-attempt-b"
                               :dispatch/outcome :dispatch/accepted)
                        (dissoc :dispatch/batch-id :dispatch/detail))
        replaced (await (store/reserve-dispatch!
                         (:evidence-store deps) replacement))
        _ (@source-release fixture/dispatched-revision)
        late-completion (await completion)]
    (t/testing "failure settles A and replacement B is admitted while A is paused"
      (t/is (= :dispatch/failed (:dispatch/outcome (:dispatch/record failed))))
      (t/is (= :reserved (:reservation/status replaced)))
      (t/is (= :dispatch-already-resolved
             (:refusal/type (:translation/refusal late-completion)))))

    (t/testing "the delayed A loses before receipt persistence or settlement of B"
      (t/is (empty? (await (store/completed-translations!
                          (:evidence-store deps) fixture/evidence-scope))))
      (t/is (= replacement
             (await (store/dispatch-for-key!
                     (:evidence-store deps)
                     (:dispatch/key replacement))))))))

(t/deftest ^:async stale-and-mismatched-answers-cannot-satisfy-the-gate
  (let [{:keys [deps]} (fixture/fixture)
        _ (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "an unknown document is refused"
      (t/is (= :dispatch-record-missing
             (:refusal/type
              (:translation/refusal
               (await (dispatch/resolve-batch-report!
                       deps {:status "partial"
                             :batch_id "batch-1"
                             :completed_document "knoxx.docs/other"})))))))

    (t/testing "another batch's answer is refused"
      (t/is (= :dispatch-record-missing
             (:refusal/type
              (:translation/refusal
               (await (dispatch/resolve-batch-report!
                       deps {:status "partial"
                             :batch_id "batch-99"
                             :completed_document "knoxx.docs/probe"})))))))

    (t/testing "no refusal left a translation fact behind"
      (t/is (empty? (await (store/completed-translations! (:evidence-store deps) fixture/evidence-scope)))))))

(t/deftest ^:async a-malformed-report-is-refused-by-contract
  (let [{:keys [deps]} (fixture/fixture)]
    (try
      (await (dispatch/resolve-batch-report!
              deps {:status "invented"
                    :batch_id "batch-1"
                    :completed_document "knoxx.docs/probe"}))
      (t/is false "an unrecognized worker status must not be interpreted")
      (catch :default err
        (t/is (= :translation-dispatch/status-report (:contract (ex-data err))))))))

(t/deftest ^:async a-failed-document-is-distinct-from-missing-work
  (let [{:keys [deps]} (fixture/fixture)
        _ (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
        failed (await (dispatch/fail-batch-document!
                       deps "batch-1" "knoxx.docs/probe" "model unavailable"))]
    (t/testing "the attempt is recorded as failed, with its reason"
      (t/is (= :dispatch/failed (:dispatch/outcome failed)))
      (t/is (= "model unavailable" (:dispatch/detail (:dispatch/record failed)))))

    (t/testing "a failure for a document nobody dispatched is refused"
      (t/is (= :dispatch-record-missing
             (:refusal/type
              (:translation/refusal
               (await (dispatch/fail-batch-document!
                       deps "batch-1" "knoxx.docs/unknown" "boom")))))))

    (t/testing "a failed attempt is not a translation"
      (t/is (empty? (await (store/completed-translations! (:evidence-store deps) fixture/evidence-scope)))))))

(t/deftest ^:async a-failed-dispatch-can-be-retried
  ;; The regression this guards: classifying every non-accepted claim as `:done`
  ;; made a failed dispatch permanently terminal. The gate still reported the
  ;; translation missing, but every later pass answered `:dispatch/duplicate`
  ;; and no batch was ever enqueued again — that source revision could only be
  ;; translated by deleting rows by hand.
  (let [{:keys [batches deps]}
        (fixture/fixture :answer (fn [_ n]
                           (if (= 1 n)
                             (throw (ex-info "worker unavailable" {}))
                             {:batch_id (str "batch-" n)}))
                 ;; Observation finds nothing, so the first send definitely did
                 ;; not land and the claim is safe to retry.
                 :observed (fn [_] {:batches []}))
        first-result (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
        retry (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "the first attempt failed"
      (t/is (= :dispatch/failed (:dispatch/outcome first-result))))

    (t/testing "a later pass replaces it with a fresh attempt"
      (t/is (= :dispatch/accepted (:dispatch/outcome retry)))
      (t/is (= 2 (count @batches)) "the work was actually enqueued again"))

    (t/testing "the retry is bound to the NEW batch, not the failed attempt's"
      ;; A stale batch id would let the old batch's completion resolve this
      ;; attempt, minting a receipt for a translation it never produced.
      (t/is (= "batch-2" (:dispatch/batch-id (:dispatch/record retry)))))

    (t/testing "a completed claim stays terminal"
      (await (dispatch/resolve-batch-report!
              deps {:status "complete"
                    :batch_id "batch-2"
                    :completed_document "knoxx.docs/probe"}))
      (let [after-completion (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
        (t/is (= :dispatch/duplicate (:dispatch/outcome after-completion)))
        (t/is (= 2 (count @batches))
            "a completed translation must never be re-dispatched")))))

(t/deftest ^:async a-failed-completion-report-can-also-be-retried
  (let [{:keys [batches deps]} (fixture/fixture)
        _ (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
        _ (await (dispatch/fail-batch-document!
                  deps "batch-1" "knoxx.docs/probe" "model unavailable"))
        retry (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "a worker-reported failure is retriable, like a dispatch failure"
      (t/is (= :dispatch/accepted (:dispatch/outcome retry)))
      (t/is (= 2 (count @batches))))

    (t/testing "the previous batch's completion can no longer resolve this claim"
      (let [stale (await (dispatch/resolve-batch-report!
                          deps {:status "complete"
                                :batch_id "batch-1"
                                :completed_document "knoxx.docs/probe"}))]
        ;; `:dispatch-record-missing` rather than `:worker-batch-mismatch`, and
        ;; that is the stronger outcome: the retry rebound the claim to batch-2,
        ;; so the (batch-1, document) join finds no binding at all. Had the retry
        ;; left the stale batch id in place, this lookup WOULD have found the
        ;; claim and minted a receipt for a translation batch-2 never finished.
        (t/is (= :dispatch-record-missing (:refusal/type (:translation/refusal stale))))
        (t/is (empty? (await (store/completed-translations! (:evidence-store deps) fixture/evidence-scope))))))))

(t/deftest ^:async a-source-that-moved-since-dispatch-cannot-be-completed
  ;; The worker is handed a document id, not bytes, and fetches the content when
  ;; it runs. So a receipt naming the revision Knoxx hashed at dispatch time is
  ;; only substantiated if the source has not moved since — otherwise a pinned
  ;; old revision gets reported translated on the strength of a translation of
  ;; different bytes.
  (let [{:keys [deps]} (fixture/fixture :source-revision "sha256-something-else")
        _ (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
        result (await (dispatch/resolve-batch-report!
                       deps {:status "processing"
                             :batch_id "batch-1"
                             :completed_document "knoxx.docs/probe"}))]
    (t/testing "the completion is refused, naming both revisions"
      (t/is (= :source-moved-since-dispatch
             (:refusal/type (:translation/refusal result))))
      (t/is (= fixture/dispatched-revision (:refusal/expected (:translation/refusal result))))
      (t/is (= "sha256-something-else" (:refusal/actual (:translation/refusal result)))))

    (t/testing "no receipt was minted for bytes nobody can vouch for"
      (t/is (empty? (await (store/completed-translations! (:evidence-store deps) fixture/evidence-scope)))))

    (t/testing "the claim is settled for good, not retried forever"
      ;; Retrying THIS claim can never work: the worker fetches current bytes and
      ;; the dispatch key names the revision that is no longer current, so every
      ;; attempt would be refused on completion and re-enqueued. An intent
      ;; tracking :source/current gets a different key next pass; one that pinned
      ;; the old revision genuinely cannot be satisfied.
      (let [stored (await (store/dispatch-for-key!
                           (:evidence-store deps)
                           (:dispatch/key (law/dispatch-record
                                           (fixture/work) (fixture/context) :dispatch/accepted (fixture/clock)
                                           :attempt-id "dispatch-attempt-key-only"))))]
        (t/is (= :dispatch/unreachable (:dispatch/outcome stored)))
        (t/is (not (law/retriable? (:dispatch/outcome stored))))))))

(t/deftest ^:async an-unreachable-pinned-revision-is-not-endlessly-re-dispatched
  ;; The loop this prevents: pinned revisions are deliberately never reported
  ;; superseded, so without a terminal outcome every pass would enqueue another
  ;; batch for a revision the worker can never fetch.
  (let [{:keys [batches deps]} (fixture/fixture :source-revision "sha256-something-else")
        _ (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
        _ (await (dispatch/resolve-batch-report!
                  deps {:status "processing"
                        :batch_id "batch-1"
                        :completed_document "knoxx.docs/probe"}))
        retry (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "a later pass does not enqueue the unreachable revision again"
      (t/is (= :dispatch/duplicate (:dispatch/outcome retry)))
      (t/is (= 1 (count @batches))))))

(t/deftest ^:async recovery-from-batch-state-also-checks-the-source
  ;; The recovery path exists because evidence was lost, which is no reason to
  ;; trust it more than the worker's own report. It used to mint directly.
  (let [{:keys [deps]} (fixture/fixture :source-revision "sha256-something-else"
                                :batch-status "complete")
        first-result (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
        recovered (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "the batch says complete, but the source moved"
      (t/is (= :dispatch/accepted (:dispatch/outcome first-result)))
      (t/is (= :dispatch/unreachable (:dispatch/outcome recovered))))

    (t/testing "no receipt was minted from a complete batch over changed bytes"
      (t/is (empty? (await (store/completed-translations! (:evidence-store deps) fixture/evidence-scope)))))))

(t/deftest ^:async an-unreadable-source-cannot-substantiate-a-completion
  (let [{:keys [deps]} (fixture/fixture)
        deps (assoc deps :observe-source-revision
                    (constantly (js/Promise.resolve nil)))
        _ (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
        result (await (dispatch/resolve-batch-report!
                       deps {:status "processing"
                             :batch_id "batch-1"
                             :completed_document "knoxx.docs/probe"}))]
    (t/testing "an unreadable source is not proof either"
      (t/is (= :source-moved-since-dispatch
             (:refusal/type (:translation/refusal result))))
      (t/is (nil? (:refusal/actual (:translation/refusal result)))))))

(t/deftest ^:async completing-without-a-source-observer-is-refused
  (let [{:keys [deps]} (fixture/fixture)
        without (dissoc deps :observe-source-revision)]
    (t/testing "a caller that forgot the check fails rather than skipping it"
      ;; Defaulting to 'assume unchanged' would delete the only evidence that
      ;; the worker translated the revision the receipt names.
      (try
        (await (dispatch/resolve-batch-report!
                without {:status "processing"
                         :batch_id "batch-1"
                         :completed_document "knoxx.docs/probe"}))
        (t/is false "a missing observer must not be silently tolerated")
        (catch :default err
          (t/is (re-find #"source-revision observer" (ex-message err))))))))


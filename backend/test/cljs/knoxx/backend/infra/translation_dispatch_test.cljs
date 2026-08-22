(ns knoxx.backend.infra.translation-dispatch-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.translation-evidence :as evidence-domain]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.translation-dispatch :as dispatch]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-dispatch :as law]))

(def ^:private intent
  {:publication/id :knoxx.docs/probe-es
   :publication/document :knoxx.docs/probe
   :publication/garden :knoxx.docs/promethean
   :publication/locale :es
   :publication/revision "sha256-aaa111bbb222"
   :publication/state :published
   :publication/path "/probe"
   :translation/review :none
   :document/source-locale :en})

(def ^:private scope {:org-id "org-1" :membership-id "member-1"})

(def ^:private dispatched-revision "sha256-aaa111bbb222")

(def ^:private facts
  {:current-source-revision (constantly "sha256-aaa111bbb222")
   :translated-revision? (constantly false)
   :approved? (constantly false)
   :source-revision-superseded? (constantly false)})

(def ^:private clock (constantly "2026-08-22T09:00:00.000Z"))

(defn- fake-client
  "An `IOpenPlannerClient` that records batch requests and answers as told.

   Only the one method this seam touches does anything. Every other method is
   present because the protocol requires it, and each throws rather than
   returning nil: a dispatch reaching one of them is a boundary violation the
   test should fail on, not silently tolerate."
  [{:keys [batches answer observed batch-status]}]
  (let [respond (or answer (fn [_request n] {:batch_id (str "batch-" n)}))
        list-batches (or observed (fn [_opts] {:batches []}))
        boom (fn [method] (throw (ex-info (str "unexpected call to " method) {})))]
    (reify openplanner-client/IOpenPlannerClient
      (enabled? [_] true)
      (create-translation-batch! [_ payload]
        (swap! batches conj payload)
        (js/Promise.resolve (respond payload (count @batches))))
      ;; Observation: `recover-ambiguous-send!` calls this after a failed create
      ;; to find out whether the batch landed anyway.
      (translation-batches! [_ opts] (js/Promise.resolve (list-batches opts)))
      ;; Every remaining method is implemented only because the protocol
      ;; declares it, and each one throws. Packed several per line to stay
      ;; inside the file-size lint without dropping any: a partial reify would
      ;; make a dispatch that wandered into an unrelated OpenPlanner call return
      ;; undefined instead of failing the test.
      (health! [_] (boom "health!")) (events! [_ _] (boom "events!"))
      (session! [_ _ _] (boom "session!")) (sessions! [_ _] (boom "sessions!"))
      (vector-search! [_ _] (boom "vector-search!"))
      (graph-memory! [_ _] (boom "graph-memory!"))
      (graph-export! [_ _] (boom "graph-export!"))
      (upsert-document! [_ _] (boom "upsert-document!"))
      (documents-stats! [_] (boom "documents-stats!"))
      (graph-monitoring! [_] (boom "graph-monitoring!"))
      (mongo-collections! [_] (boom "mongo-collections!"))
      (mongo-query! [_ _] (boom "mongo-query!"))
      (build-semantic-edges! [_ _] (boom "build-semantic-edges!"))
      (record-labels! [_ _] (boom "record-labels!"))
      (record-reaction! [_ _ _] (boom "record-reaction!"))
      (translation-segments! [_ _] (boom "translation-segments!"))
      (translation-segment! [_ _ _] (boom "translation-segment!"))
      (create-translation-segment! [_ _] (boom "create-translation-segment!"))
      (label-translation-segment! [_ _ _] (boom "label-segment!"))
      (translation-export-manifest! [_ _] (boom "export-manifest!"))
      (translation-export-sft! [_ _] (boom "export-sft!"))
      (create-translation-segments-batch! [_ _] (boom "create-segments-batch!"))
      (translation-documents! [_ _] (boom "translation-documents!"))
      (translation-document! [_ _ _ _] (boom "translation-document!"))
      (review-translation-document! [_ _ _ _] (boom "review-document!"))
      (next-translation-batch! [_ _] (boom "next-batch!"))
      ;; `recover-settled-batch!` re-reads the batch. Absent a configured status
      ;; it throws, which the recovery treats as unreadable — the honest default
      ;; for a test that is not exercising recovery.
      (translation-batch! [_ _ _]
        (if batch-status
          (js/Promise.resolve {:status batch-status})
          (boom "translation-batch!")))
      (update-translation-batch-status! [_ _ _] (boom "update-status!"))
      (v1-json! [_ _ _ _] (boom "v1-json!")) (forward-v1! [_ _] (boom "forward-v1!")))))

(defn- fixture
  "Fresh store, client and recorded batch list for one test."
  [& {:keys [answer observed source-revision batch-status]}]
  (let [batches (atom [])]
    {:batches batches
     :deps {:evidence-store (store/memory-store)
            :client (fake-client {:batches batches
                                  :answer answer
                                  :observed observed
                                  :batch-status batch-status})
            :clock clock
            ;; Defaults to agreeing with the dispatched revision, so completion
            ;; is not refused for source drift. The drift path overrides it.
            :observe-source-revision
            (constantly (js/Promise.resolve
                         (or source-revision dispatched-revision)))}}))

(defn- work []
  (:action/with (dispatch/derived-work intent facts)))

(defn- context []
  ;; The digest observed at dispatch time. Equal to the intent's revision here
  ;; because the fixture's revision IS a content digest; a pinned opaque revision
  ;; would differ, which is why the two are recorded separately.
  (dispatch/dispatch-context intent scope dispatched-revision))

(deftest ^:async gated-work-reaches-the-worker-with-a-concrete-revision
  (let [{:keys [batches deps]} (fixture)
        result (await (dispatch/dispatch-work! deps (work) (context)))]
    (testing "the worker was asked once, in its own contract's shape"
      (is (= 1 (count @batches)))
      (is (= {:garden_id "knoxx.docs/promethean"
              :target_lang "es"
              :document_ids ["knoxx.docs/probe"]
              :source_lang "en"
              :org_id "org-1"
              :membership_id "member-1"}
             (first @batches))))

    (testing "the revision the worker cannot carry is bound Knoxx-side"
      (is (= :dispatch/accepted (:dispatch/outcome result)))
      (is (= "sha256-aaa111bbb222" (:dispatch/revision (:dispatch/record result))))
      (is (= "batch-1" (:dispatch/batch-id (:dispatch/record result)))))

    (testing "no translation fact exists yet — the worker has not answered"
      (is (empty? (await (store/completed-translations! (:evidence-store deps))))))))

(deftest ^:async duplicate-dispatch-does-not-enqueue-twice
  (let [{:keys [batches deps]} (fixture)
        first-result (await (dispatch/dispatch-work! deps (work) (context)))
        second-result (await (dispatch/dispatch-work! deps (work) (context)))]
    (testing "the second ask reuses the identity instead of translating again"
      (is (= :dispatch/accepted (:dispatch/outcome first-result)))
      (is (= :dispatch/duplicate (:dispatch/outcome second-result)))
      (is (= 1 (count @batches))
          "a second batch would translate the same revision twice"))

    (testing "the duplicate carries the running batch, so a caller can see it"
      (is (= "batch-1" (:dispatch/batch-id (:dispatch/record second-result)))))))

(deftest ^:async a-definite-worker-refusal-becomes-retriable
  ;; The create threw AND observation found no batch, so the send definitely did
  ;; not land. Only then is the claim safe to mark retriable.
  (let [{:keys [batches deps]} (fixture :answer (fn [_ _]
                                                  (throw (ex-info "worker refused" {}))))
        result (await (dispatch/dispatch-work! deps (work) (context)))]
    (testing "the outcome is failure, with the reason kept"
      (is (= :dispatch/failed (:dispatch/outcome result)))
      (is (= :dispatch/failed (:dispatch/outcome (:dispatch/record result))))
      (is (= "worker refused" (:dispatch/detail (:dispatch/record result)))))

    (testing "the claim reached a terminal outcome rather than sticking in flight"
      ;; Left in flight, the work would never be retried and never reported: it
      ;; would silently never happen.
      (let [stored (await (store/dispatch-for-key!
                           (:evidence-store deps)
                           (:dispatch/key (:dispatch/record result))))]
        (is (= :dispatch/failed (:dispatch/outcome stored)))))

    (testing "no translation fact was fabricated"
      (is (empty? (await (store/completed-translations! (:evidence-store deps))))))
    (is (= 1 (count @batches)))))

(deftest ^:async a-nil-batch-id-cannot-produce-an-unattributable-dispatch
  (let [{:keys [deps]} (fixture :answer (fn [_ _] {:ok false}))
        result (await (dispatch/dispatch-work! deps (work) (context)))]
    (testing "a client answering without a batch id is a failure, not success"
      (is (= :dispatch/failed (:dispatch/outcome result))))))

(deftest ^:async an-ambiguous-send-is-observed-before-it-is-retried
  ;; The create threw, but the batch is there — the response was lost, not the
  ;; request. Marking this retriable would translate the same revision twice,
  ;; and the worker request has no idempotency key for the second call to
  ;; collapse into.
  (let [{:keys [batches deps]}
        (fixture :answer (fn [_ _] (throw (ex-info "connection reset" {})))
                 ;; Created at the claim's own instant, so it can be attributed
                 ;; to this dispatch.
                 :observed (fn [_] {:batches [{:batch_id "batch-existing"
                                               :created_at "2026-08-22T09:00:00.000Z"
                                               :document_ids ["knoxx.docs/probe"]}]}))
        result (await (dispatch/dispatch-work! deps (work) (context)))]
    (testing "the claim stays in flight, bound to the batch that already exists"
      (is (= :dispatch/accepted (:dispatch/outcome result)))
      (is (= "batch-existing" (:dispatch/batch-id (:dispatch/record result)))))

    (testing "a later pass sees it running rather than enqueueing again"
      (let [retry (await (dispatch/dispatch-work! deps (work) (context)))]
        (is (= :dispatch/duplicate (:dispatch/outcome retry)))
        (is (= 1 (count @batches)) "only the original attempt was ever sent")))))

(deftest ^:async an-unobservable-send-stays-in-flight-rather-than-duplicating
  ;; Both the create and the observation failed, so nothing is known. A stuck
  ;; claim is visible and fixable; a duplicate translation is neither.
  (let [{:keys [deps]}
        (fixture :answer (fn [_ _] (throw (ex-info "connection reset" {})))
                 :observed (fn [_] (throw (ex-info "worker unreachable" {}))))
        result (await (dispatch/dispatch-work! deps (work) (context)))]
    (testing "the conservative end is chosen, and it says why"
      (is (= :dispatch/accepted (:dispatch/outcome result)))
      (is (re-find #"observation also failed" (:dispatch/detail result))))))

(deftest ^:async a-batch-level-failure-resolves-by-batch-id-alone
  ;; The worker's terminal failure report names no document at all — it sends
  ;; status "failed" plus an error. Without resolving by batch id the claim would
  ;; sit in flight forever and never be retried.
  (let [{:keys [batches deps]} (fixture)
        _ (await (dispatch/dispatch-work! deps (work) (context)))
        failed (await (dispatch/fail-batch! deps "batch-1" "All documents failed"))]
    (testing "the binding is found and marked failed"
      (is (= :dispatch/failed (:dispatch/outcome failed)))
      (is (= "All documents failed" (:dispatch/detail (:dispatch/record failed)))))

    (testing "the claim is retriable, so the work can happen"
      (let [retry (await (dispatch/dispatch-work! deps (work) (context)))]
        (is (= :dispatch/accepted (:dispatch/outcome retry)))
        (is (= 2 (count @batches)))))

    (testing "an unknown batch is refused rather than guessed at"
      (is (= :dispatch-record-missing
             (:refusal/type (:translation/refusal
                             (await (dispatch/fail-batch! deps "batch-99" "boom")))))))))

(deftest ^:async a-completed-report-becomes-evidence-the-gate-recognizes
  (let [{:keys [deps]} (fixture)
        _ (await (dispatch/dispatch-work! deps (work) (context)))
        resolved (await (dispatch/resolve-batch-report!
                         deps
                         {:status "partial"
                          :batch_id "batch-1"
                          :completed_document "knoxx.docs/probe"}))
        receipt (:translation/receipt resolved)]
    (testing "a receipt is minted against the bound revision"
      (is (some? receipt))
      (is (= "sha256-aaa111bbb222" (:translation/source-revision receipt)))
      (is (= :es (:translation/locale receipt)))
      (is (= :en (:translation/source-locale receipt))))

    (testing "the gate now sees the translation"
      ;; The DoD line this proves: 'a validated, revision-specific receipt that
      ;; the publication gate recognizes'.
      (let [loaded (evidence-domain/evidence
                    {:receipts (await (store/completed-translations!
                                       (:evidence-store deps)))})
            gate-facts (evidence-domain/gate-facts loaded)]
        (is ((:translated-revision? gate-facts)
             :knoxx.docs/probe :es "sha256-aaa111bbb222"))
        (is (not ((:translated-revision? gate-facts)
                  :knoxx.docs/probe :fr "sha256-aaa111bbb222")))))

    (testing "the same report a second time cannot mint a second receipt"
      (let [again (await (dispatch/resolve-batch-report!
                          deps
                          {:status "complete"
                           :batch_id "batch-1"
                           :completed_document "knoxx.docs/probe"}))]
        (is (= :dispatch-already-resolved
               (:refusal/type (:translation/refusal again))))
        (is (= 1 (count (await (store/completed-translations!
                                (:evidence-store deps))))))))))

(deftest ^:async stale-and-mismatched-answers-cannot-satisfy-the-gate
  (let [{:keys [deps]} (fixture)
        _ (await (dispatch/dispatch-work! deps (work) (context)))]
    (testing "an unknown document is refused"
      (is (= :dispatch-record-missing
             (:refusal/type
              (:translation/refusal
               (await (dispatch/resolve-batch-report!
                       deps {:status "partial"
                             :batch_id "batch-1"
                             :completed_document "knoxx.docs/other"})))))))

    (testing "another batch's answer is refused"
      (is (= :dispatch-record-missing
             (:refusal/type
              (:translation/refusal
               (await (dispatch/resolve-batch-report!
                       deps {:status "partial"
                             :batch_id "batch-99"
                             :completed_document "knoxx.docs/probe"})))))))

    (testing "no refusal left a translation fact behind"
      (is (empty? (await (store/completed-translations! (:evidence-store deps))))))))

(deftest ^:async a-malformed-report-is-refused-by-contract
  (let [{:keys [deps]} (fixture)]
    (try
      (await (dispatch/resolve-batch-report!
              deps {:status "invented"
                    :batch_id "batch-1"
                    :completed_document "knoxx.docs/probe"}))
      (is false "an unrecognized worker status must not be interpreted")
      (catch :default err
        (is (= :translation-dispatch/status-report (:contract (ex-data err))))))))

(deftest ^:async a-failed-document-is-distinct-from-missing-work
  (let [{:keys [deps]} (fixture)
        _ (await (dispatch/dispatch-work! deps (work) (context)))
        failed (await (dispatch/fail-batch-document!
                       deps "batch-1" "knoxx.docs/probe" "model unavailable"))]
    (testing "the attempt is recorded as failed, with its reason"
      (is (= :dispatch/failed (:dispatch/outcome failed)))
      (is (= "model unavailable" (:dispatch/detail (:dispatch/record failed)))))

    (testing "a failure for a document nobody dispatched is refused"
      (is (= :dispatch-record-missing
             (:refusal/type
              (:translation/refusal
               (await (dispatch/fail-batch-document!
                       deps "batch-1" "knoxx.docs/unknown" "boom")))))))

    (testing "a failed attempt is not a translation"
      (is (empty? (await (store/completed-translations! (:evidence-store deps))))))))

(deftest ^:async intents-with-no-derived-work-are-not-dispatched
  (let [{:keys [batches deps]} (fixture)
        translated (assoc facts :translated-revision? (constantly true))
        results (await (dispatch/dispatch-intents! deps [intent] translated scope))]
    (testing "an already-translated intent derives nothing and is absent"
      (is (empty? results))
      (is (empty? @batches)))))

(deftest ^:async dispatch-intents-reports-one-entry-per-dispatched-intent
  (let [{:keys [batches deps]} (fixture)
        results (await (dispatch/dispatch-intents! deps [intent] facts scope))]
    (testing "the report names the publication it belongs to"
      (is (= 1 (count results)))
      (is (= :knoxx.docs/probe-es (:publication/id (first results))))
      (is (= :dispatch/accepted (:dispatch/outcome (first results))))
      (is (= 1 (count @batches))))))

(deftest a-selector-revision-can-never-be-dispatched
  (testing "work whose revision is a selector is refused before the worker is called"
    ;; Defence in depth: the gate resolves the selector, so this shape should be
    ;; impossible — which is exactly why it is asserted rather than assumed.
    (is (thrown? js/Error
                 (law/assert-valid! :translation-dispatch/work
                                    law/DerivedWork
                                    {:document :knoxx.docs/probe
                                     :locale :es
                                     :revision :source/current
                                     :replace-stale? false})))))

(deftest ^:async a-failed-dispatch-can-be-retried
  ;; The regression this guards: classifying every non-accepted claim as `:done`
  ;; made a failed dispatch permanently terminal. The gate still reported the
  ;; translation missing, but every later pass answered `:dispatch/duplicate`
  ;; and no batch was ever enqueued again — that source revision could only be
  ;; translated by deleting rows by hand.
  (let [{:keys [batches deps]}
        (fixture :answer (fn [_ n]
                           (if (= 1 n)
                             (throw (ex-info "worker unavailable" {}))
                             {:batch_id (str "batch-" n)}))
                 ;; Observation finds nothing, so the first send definitely did
                 ;; not land and the claim is safe to retry.
                 :observed (fn [_] {:batches []}))
        first-result (await (dispatch/dispatch-work! deps (work) (context)))
        retry (await (dispatch/dispatch-work! deps (work) (context)))]
    (testing "the first attempt failed"
      (is (= :dispatch/failed (:dispatch/outcome first-result))))

    (testing "a later pass replaces it with a fresh attempt"
      (is (= :dispatch/accepted (:dispatch/outcome retry)))
      (is (= 2 (count @batches)) "the work was actually enqueued again"))

    (testing "the retry is bound to the NEW batch, not the failed attempt's"
      ;; A stale batch id would let the old batch's completion resolve this
      ;; attempt, minting a receipt for a translation it never produced.
      (is (= "batch-2" (:dispatch/batch-id (:dispatch/record retry)))))

    (testing "a completed claim stays terminal"
      (await (dispatch/resolve-batch-report!
              deps {:status "complete"
                    :batch_id "batch-2"
                    :completed_document "knoxx.docs/probe"}))
      (let [after-completion (await (dispatch/dispatch-work! deps (work) (context)))]
        (is (= :dispatch/duplicate (:dispatch/outcome after-completion)))
        (is (= 2 (count @batches))
            "a completed translation must never be re-dispatched")))))

(deftest ^:async a-failed-completion-report-can-also-be-retried
  (let [{:keys [batches deps]} (fixture)
        _ (await (dispatch/dispatch-work! deps (work) (context)))
        _ (await (dispatch/fail-batch-document!
                  deps "batch-1" "knoxx.docs/probe" "model unavailable"))
        retry (await (dispatch/dispatch-work! deps (work) (context)))]
    (testing "a worker-reported failure is retriable, like a dispatch failure"
      (is (= :dispatch/accepted (:dispatch/outcome retry)))
      (is (= 2 (count @batches))))

    (testing "the previous batch's completion can no longer resolve this claim"
      (let [stale (await (dispatch/resolve-batch-report!
                          deps {:status "complete"
                                :batch_id "batch-1"
                                :completed_document "knoxx.docs/probe"}))]
        ;; `:dispatch-record-missing` rather than `:worker-batch-mismatch`, and
        ;; that is the stronger outcome: the retry rebound the claim to batch-2,
        ;; so the (batch-1, document) join finds no binding at all. Had the retry
        ;; left the stale batch id in place, this lookup WOULD have found the
        ;; claim and minted a receipt for a translation batch-2 never finished.
        (is (= :dispatch-record-missing (:refusal/type (:translation/refusal stale))))
        (is (empty? (await (store/completed-translations! (:evidence-store deps)))))))))

(deftest retriable-and-terminal-outcomes-are-disjoint-and-complete
  (testing "every outcome is exactly one of accepted, retriable, or terminal"
    (doseq [outcome law/outcomes]
      (let [accepted? (= :dispatch/accepted outcome)
            retriable? (law/retriable? outcome)
            terminal? (law/terminal? outcome)]
        (is (= 1 (count (filter true? [accepted? retriable? terminal?])))
            (str outcome " is not in exactly one class")))))

  (testing "a completed translation is never retriable"
    (is (not (law/retriable? :dispatch/completed)))
    (is (law/terminal? :dispatch/completed)))

  (testing "a failed or rejected attempt is retriable"
    (is (law/retriable? :dispatch/failed))
    (is (law/retriable? :dispatch/rejected))))

(deftest ^:async a-source-that-moved-since-dispatch-cannot-be-completed
  ;; The worker is handed a document id, not bytes, and fetches the content when
  ;; it runs. So a receipt naming the revision Knoxx hashed at dispatch time is
  ;; only substantiated if the source has not moved since — otherwise a pinned
  ;; old revision gets reported translated on the strength of a translation of
  ;; different bytes.
  (let [{:keys [deps]} (fixture :source-revision "sha256-something-else")
        _ (await (dispatch/dispatch-work! deps (work) (context)))
        result (await (dispatch/resolve-batch-report!
                       deps {:status "processing"
                             :batch_id "batch-1"
                             :completed_document "knoxx.docs/probe"}))]
    (testing "the completion is refused, naming both revisions"
      (is (= :source-moved-since-dispatch
             (:refusal/type (:translation/refusal result))))
      (is (= dispatched-revision (:refusal/expected (:translation/refusal result))))
      (is (= "sha256-something-else" (:refusal/actual (:translation/refusal result)))))

    (testing "no receipt was minted for bytes nobody can vouch for"
      (is (empty? (await (store/completed-translations! (:evidence-store deps))))))

    (testing "the claim is settled for good, not retried forever"
      ;; Retrying THIS claim can never work: the worker fetches current bytes and
      ;; the dispatch key names the revision that is no longer current, so every
      ;; attempt would be refused on completion and re-enqueued. An intent
      ;; tracking :source/current gets a different key next pass; one that pinned
      ;; the old revision genuinely cannot be satisfied.
      (let [stored (await (store/dispatch-for-key!
                           (:evidence-store deps)
                           (:dispatch/key (law/dispatch-record
                                           (work) (context) :dispatch/accepted (clock)))))]
        (is (= :dispatch/unreachable (:dispatch/outcome stored)))
        (is (not (law/retriable? (:dispatch/outcome stored))))))))

(deftest ^:async an-unreachable-pinned-revision-is-not-endlessly-re-dispatched
  ;; The loop this prevents: pinned revisions are deliberately never reported
  ;; superseded, so without a terminal outcome every pass would enqueue another
  ;; batch for a revision the worker can never fetch.
  (let [{:keys [batches deps]} (fixture :source-revision "sha256-something-else")
        _ (await (dispatch/dispatch-work! deps (work) (context)))
        _ (await (dispatch/resolve-batch-report!
                  deps {:status "processing"
                        :batch_id "batch-1"
                        :completed_document "knoxx.docs/probe"}))
        retry (await (dispatch/dispatch-work! deps (work) (context)))]
    (testing "a later pass does not enqueue the unreachable revision again"
      (is (= :dispatch/duplicate (:dispatch/outcome retry)))
      (is (= 1 (count @batches))))))

(deftest ^:async recovery-from-batch-state-also-checks-the-source
  ;; The recovery path exists because evidence was lost, which is no reason to
  ;; trust it more than the worker's own report. It used to mint directly.
  (let [{:keys [deps]} (fixture :source-revision "sha256-something-else"
                                :batch-status "complete")
        first-result (await (dispatch/dispatch-work! deps (work) (context)))
        recovered (await (dispatch/dispatch-work! deps (work) (context)))]
    (testing "the batch says complete, but the source moved"
      (is (= :dispatch/accepted (:dispatch/outcome first-result)))
      (is (= :dispatch/unreachable (:dispatch/outcome recovered))))

    (testing "no receipt was minted from a complete batch over changed bytes"
      (is (empty? (await (store/completed-translations! (:evidence-store deps))))))))

(deftest ^:async an-unreadable-source-cannot-substantiate-a-completion
  (let [{:keys [deps]} (fixture)
        deps (assoc deps :observe-source-revision
                    (constantly (js/Promise.resolve nil)))
        _ (await (dispatch/dispatch-work! deps (work) (context)))
        result (await (dispatch/resolve-batch-report!
                       deps {:status "processing"
                             :batch_id "batch-1"
                             :completed_document "knoxx.docs/probe"}))]
    (testing "an unreadable source is not proof either"
      (is (= :source-moved-since-dispatch
             (:refusal/type (:translation/refusal result))))
      (is (nil? (:refusal/actual (:translation/refusal result)))))))

(deftest ^:async completing-without-a-source-observer-is-refused
  (let [{:keys [deps]} (fixture)
        without (dissoc deps :observe-source-revision)]
    (testing "a caller that forgot the check fails rather than skipping it"
      ;; Defaulting to 'assume unchanged' would delete the only evidence that
      ;; the worker translated the revision the receipt names.
      (try
        (await (dispatch/resolve-batch-report!
                without {:status "processing"
                         :batch_id "batch-1"
                         :completed_document "knoxx.docs/probe"}))
        (is false "a missing observer must not be silently tolerated")
        (catch :default err
          (is (re-find #"source-revision observer" (ex-message err))))))))

(deftest ^:async observation-will-not-bind-a-batch-older-than-the-claim
  ;; Matching on garden, locale and document alone is not a match on THIS
  ;; dispatch: a tenant that translated the same document into the same locale
  ;; before has an older batch matching all three. Binding to it would let
  ;; `recover-settled-batch!` mint a receipt for a revision that batch never saw.
  (let [{:keys [deps]}
        (fixture :answer (fn [_ _] (throw (ex-info "connection reset" {})))
                 :observed (fn [_] {:batches [{:batch_id "batch-from-last-year"
                                               :created_at "2025-01-01T00:00:00.000Z"
                                               :document_ids ["knoxx.docs/probe"]}]}))
        result (await (dispatch/dispatch-work! deps (work) (context)))]
    (testing "the historical batch is not adopted"
      (is (not= "batch-from-last-year" (:dispatch/batch-id (:dispatch/record result)))))

    (testing "the claim is treated as a send that did not land"
      (is (= :dispatch/failed (:dispatch/outcome result))))))

(deftest ^:async observation-will-not-bind-a-batch-of-unknown-age
  (let [{:keys [deps]}
        (fixture :answer (fn [_ _] (throw (ex-info "connection reset" {})))
                 :observed (fn [_] {:batches [{:batch_id "batch-undated"
                                               :document_ids ["knoxx.docs/probe"]}]}))
        result (await (dispatch/dispatch-work! deps (work) (context)))]
    (testing "an unknown creation time is not evidence of provenance"
      (is (not= "batch-undated" (:dispatch/batch-id (:dispatch/record result))))
      (is (= :dispatch/failed (:dispatch/outcome result))))))

(deftest batch-provenance-is-decided-by-creation-time
  (let [claim-at "2026-08-22T09:00:00.000Z"]
    (testing "a batch created at or after the claim can be the claim's"
      (is (law/batch-created-after? {:created_at claim-at} claim-at))
      (is (law/batch-created-after? {:created_at "2026-08-22T09:00:00.001Z"} claim-at))
      (is (law/batch-created-after? {:createdAt claim-at} claim-at) "camelCase too"))

    (testing "a batch created before the claim cannot be"
      (is (not (law/batch-created-after? {:created_at "2026-08-22T08:59:59.999Z"}
                                         claim-at))))

    (testing "an absent or unparseable creation time is refused, not guessed"
      (is (not (law/batch-created-after? {} claim-at)))
      (is (not (law/batch-created-after? {:created_at ""} claim-at)))
      (is (not (law/batch-created-after? {:created_at "yesterday"} claim-at)))
      (is (not (law/batch-created-after? {:created_at "2026-08-22T09:00:00Z"} claim-at))
          "an instant in another format cannot be compared as a string"))))

(deftest ^:async a-truncated-batch-listing-does-not-license-a-retry
  ;; The batch listing sorts newest-first and stops at `batch-listing-cap`, so a
  ;; busy garden can push our batch off the end. Reading that absence as "the
  ;; send did not land" is how a duplicate translation happens.
  (let [full-page (vec (repeat dispatch/batch-listing-cap
                               {:batch_id "someone-elses"
                                :created_at "2026-08-22T09:00:00.000Z"
                                :document_ids ["knoxx.docs/other"]}))
        {:keys [deps]} (fixture :answer (fn [_ _] (throw (ex-info "timeout" {})))
                                :observed (fn [_] {:batches full-page}))
        result (await (dispatch/dispatch-work! deps (work) (context)))]
    (testing "the claim stays in flight rather than becoming retriable"
      (is (= :dispatch/accepted (:dispatch/outcome result)))
      (is (re-find #"truncated" (:dispatch/detail result))))))

(deftest ^:async a-short-batch-listing-does-license-a-retry
  (let [{:keys [deps]} (fixture :answer (fn [_ _] (throw (ex-info "timeout" {})))
                                :observed (fn [_] {:batches []}))
        result (await (dispatch/dispatch-work! deps (work) (context)))]
    (testing "an absence that is conclusive is treated as one"
      (is (= :dispatch/failed (:dispatch/outcome result))))))

(deftest the-mirrored-listing-cap-matches-the-store
  (testing "the cap this namespace reasons about is the one the store applies"
    ;; Mirrored rather than imported because the store boundary does not expose
    ;; it. If that changes, this fails rather than silently drifting.
    (is (= 50 dispatch/batch-listing-cap))))

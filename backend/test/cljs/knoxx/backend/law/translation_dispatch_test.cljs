(ns knoxx.backend.law.translation-dispatch-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.law.openplanner-translation :as openplanner-law]
            [knoxx.backend.law.translation-dispatch :as law]
            [malli.core :as m]))

(def ^:private work
  {:document :knoxx.docs/probe
   :locale :es
   :revision "sha256-abc123def456"
   :replace-stale? false})

(def ^:private context
  {:dispatch/garden "knoxx.docs/promethean"
   :dispatch/document-wire-id "knoxx.docs/probe"
   :dispatch/source-locale :en
   :dispatch/org-id "org-1"
   :dispatch/membership-id "member-1"})

(def ^:private at "2026-08-22T09:00:00.000Z")

(defn- record
  [& {:keys [outcome attempt-id batch-id recovery-reason]
      :or {outcome :dispatch/accepted
           attempt-id "dispatch-attempt-1"}}]
  (law/dispatch-record work context outcome at
                       :attempt-id attempt-id
                       :batch-id batch-id
                       :recovery-reason recovery-reason))

(deftest derived-work-is-validated-on-arrival
  (testing "the gate's payload shape is accepted"
    (is (m/validate law/DerivedWork work)))

  (testing "a selector revision in hand-rolled work is refused"
    (is (not (m/validate law/DerivedWork (assoc work :revision :source/current))))
    (is (not (m/validate law/DerivedWork (assoc work :revision "source/current")))))

  (testing "an extra key is refused, so a hand-rolled item cannot smuggle one"
    (is (not (m/validate law/DerivedWork (assoc work :publication/path "/probe"))))))

(deftest dispatch-key-collapses-duplicates-and-separates-real-differences
  (testing "the same request twice is the same key"
    (is (= (law/dispatch-key {:org-id "org-1" :garden "a/garden" :document :a/b :source-locale :en :locale :es :revision "r1"})
           (law/dispatch-key {:org-id "org-1" :garden "a/garden" :document :a/b :source-locale :en :locale :es :revision "r1"}))))

  (testing "every keyed dimension changes the key"
    (let [base {:org-id "org-1" :garden "a/garden" :document :a/b :source-locale :en :locale :es :revision "r1"}
          base-key (law/dispatch-key base)]
      (doseq [[field value] [[:org-id "org-2"] [:garden "a/other"]
                             [:document :a/other] [:source-locale :de]
                             [:locale :fr] [:revision "r2"]]]
        (is (not= base-key (law/dispatch-key (assoc base field value)))
            (str field " did not change the key")))))

  (testing "a keyword and its name cannot collide"
    ;; pr-str rather than str, so `:es` renders as ":es" and the string "es" as
    ;; "\"es\"". Under `str` both would render as "es" and share a key.
    (is (not= (law/dispatch-key {:org-id "org-1" :garden "a/garden" :document :a/b :source-locale :en :locale :es :revision "r1"})
              (law/dispatch-key {:org-id "org-1" :garden "a/garden" :document :a/b :source-locale :en :locale "es" :revision "r1"}))))

  (testing "the joining separator cannot be smuggled through a field"
    ;; Fields are joined with "|", so a revision containing one must not let two
    ;; different requests render to the same key.
    (is (not= (law/dispatch-key {:org-id "org-1" :garden "a/garden" :document :a/b :source-locale :en :locale :es :revision "r1"})
              (law/dispatch-key {:org-id "org-1" :garden "a/garden" :document :a/b :source-locale :en :locale :es :revision "r1|"}))))

  (testing "a selector revision cannot produce a key"
    (is (thrown? js/Error (law/dispatch-key {:org-id "org-1" :garden "a/garden" :document :a/b :source-locale :en
                                             :locale :es :revision :source/current})))
    (is (thrown? js/Error (law/dispatch-key {:org-id "org-1" :garden "a/garden" :document :a/b :source-locale :en
                                             :locale :es :revision nil}))))

  (testing "a key without a tenant is refused"
    ;; The translation itself is tenant-scoped, so a key missing its
    ;; organization is a key for the wrong question.
    (is (thrown? js/Error (law/dispatch-key {:garden "a/garden" :document :a/b :source-locale :en
                                             :locale :es :revision "r1"})))
    (is (thrown? js/Error (law/dispatch-key {:org-id "  " :garden "a/garden" :document :a/b :source-locale :en
                                             :locale :es :revision "r1"})))))

(deftest worker-request-matches-the-workers-own-contract
  (let [request (law/worker-request work context)]
    (testing "what this namespace produces is what the worker accepts"
      ;; The portable mirror is only safe while the two agree. This is the test
      ;; WorkerRequest's docstring promises: a change over there fails here.
      (is (m/validate openplanner-law/CreateTranslationBatchRequest request)))

    (testing "locales travel as language tags, not printed keywords"
      (is (= "es" (:target_lang request)))
      (is (= "en" (:source_lang request))))

    (testing "one document per batch, so one revision binding cannot cover two"
      (is (= ["knoxx.docs/probe"] (:document_ids request))))

    (testing "an absent project is omitted rather than sent as nil"
      (is (not (contains? request :project)))
      (is (= "proj" (:project (law/worker-request
                               work (assoc context :dispatch/project "proj"))))))))

(deftest dispatch-records-carry-the-binding-the-worker-cannot
  (testing "the record pins the concrete revision and both locales"
    (let [r (record)]
      (is (= "sha256-abc123def456" (:dispatch/revision r)))
      (is (= :en (:dispatch/source-locale r)))
      (is (= :es (:dispatch/locale r)))
      (is (= (law/dispatch-key {:org-id "org-1" :garden "knoxx.docs/promethean" :document :knoxx.docs/probe :source-locale :en
                                :locale :es :revision "sha256-abc123def456"})
             (:dispatch/key r)))))

  (testing "a batch id is optional, because a rejected dispatch never got one"
    (is (not (contains? (record) :dispatch/batch-id)))
    (is (= "batch-7" (:dispatch/batch-id (record :batch-id "batch-7")))))

  (testing "an unknown outcome cannot be recorded"
    (is (thrown? js/Error (law/dispatch-record work context :dispatch/invented at
                                               :attempt-id "dispatch-attempt-1"))))

  (testing "new attempts require identity while historical rows stay readable"
    (is (thrown? js/Error
                 (law/dispatch-record work context :dispatch/accepted at)))
    (is (= (:dispatch/key (record))
           (:dispatch/key (law/assert-record!
                           (dissoc (record) :dispatch/attempt-id)))))))

(deftest dispatch-attempt-identity-is-independent-of-wall-clock-time
  (let [first-attempt (record :attempt-id "dispatch-attempt-a")
        second-attempt (record :attempt-id "dispatch-attempt-b")]
    (testing "same key and same millisecond are still different attempts"
      (is (not (law/same-attempt? first-attempt second-attempt))))

    (testing "outcome, batch, and detail are mutable coordinates of one attempt"
      (is (law/same-attempt?
           first-attempt
           (assoc first-attempt
                  :dispatch/outcome :dispatch/failed
                  :dispatch/batch-id "batch-7"
                  :dispatch/detail "provider failed"))))))

(deftest completed-claim-recovery-is-explicit-and-narrow
  (let [completed (record :outcome :dispatch/completed :batch-id "batch-old")
        accepted (record :attempt-id "dispatch-attempt-accepted")
        ordinary (record :attempt-id "dispatch-attempt-ordinary")
        recovery (record :attempt-id "dispatch-attempt-recovery"
                         :recovery-reason :candidate-unavailable)]
    (testing "the closed recovery vocabulary is validated on the record"
      (is (= :candidate-unavailable (:dispatch/recovery-reason recovery)))
      (is (thrown? js/Error
                   (record :recovery-reason :invented-recovery))))

    (testing "an ordinary completed claim remains terminal"
      (is (law/terminal? :dispatch/completed))
      (is (not (law/replaceable-claim? ordinary completed))))

    (testing "an accepted in-flight claim is never replaceable"
      (is (not (law/replaceable-claim? ordinary accepted)))
      (is (not (law/replaceable-claim? recovery accepted))))

    (testing "the explicit candidate-unavailable proposal may reopen candidate-terminal claims"
      (is (law/replaceable-claim? recovery completed))
      (is (law/replaceable-claim?
           recovery (assoc completed :dispatch/outcome :dispatch/duplicate))))

    (testing "the exception cannot cross a key or replace unreachable work"
      (is (not (law/replaceable-claim?
                (assoc recovery :dispatch/key "some-other-key") completed)))
      (is (not (law/replaceable-claim?
                recovery (assoc completed :dispatch/outcome :dispatch/unreachable)))))

    (testing "a replacement is always a new accepted attempt"
      (is (not (law/replaceable-claim?
                (assoc recovery :dispatch/outcome :dispatch/failed) completed))))

    (testing "ordinary failed and rejected attempts remain replaceable"
      (doseq [outcome [:dispatch/failed :dispatch/rejected]]
        (is (law/replaceable-claim?
             ordinary (assoc completed :dispatch/outcome outcome)))))))

(deftest output-revision-changes-with-the-producing-batch
  (testing "the same source revision translated twice yields distinct outputs"
    ;; This is what stops an approval of the first translation authorizing the
    ;; second: the output revision an approval pins is batch-specific.
    (is (not= (law/output-revision (record :batch-id "batch-7"))
              (law/output-revision (record :batch-id "batch-8")))))

  (testing "the output revision is itself a concrete revision"
    (is (m/validate law/ConcreteRevision (law/output-revision (record :batch-id "batch-7")))))

  (testing "an unattributable output has no revision"
    (is (thrown? js/Error (law/output-revision (record))))))

(deftest completion-refusals-name-both-sides
  (let [accepted (record :batch-id "batch-7")
        report {:status "partial"
                :batch_id "batch-7"
                :completed_document "knoxx.docs/probe"}]
    (testing "a matching report is admitted"
      (is (nil? (law/completion-refusal accepted report))))

    (testing "no binding is a distinct refusal from a disagreeing one"
      (is (= :dispatch-record-missing
             (:refusal/type (law/completion-refusal nil report)))))

    (testing "a different document cannot resolve this binding"
      (is (= :dispatch-document-mismatch
             (:refusal/type (law/completion-refusal
                             accepted (assoc report :completed_document "other/doc"))))))

    (testing "another batch's completion cannot resolve this binding"
      (let [refusal (law/completion-refusal accepted (assoc report :batch_id "batch-9"))]
        (is (= :worker-batch-mismatch (:refusal/type refusal)))
        (is (= "batch-7" (:refusal/expected refusal)))
        (is (= "batch-9" (:refusal/actual refusal))
            "both sides travel, so a caller can see which was stale")))

    (testing "an already-resolved claim cannot be resolved twice"
      (doseq [outcome [:dispatch/completed :dispatch/failed :dispatch/rejected
                       :dispatch/duplicate]]
        (is (= :dispatch-already-resolved
               (:refusal/type (law/completion-refusal
                               (assoc accepted :dispatch/outcome outcome) report)))
            (str outcome " was allowed to complete"))))

    (testing "a stored selector revision is caught even though it cannot get in"
      (is (= :worker-revision-selector
             (:refusal/type (law/completion-refusal
                             (assoc accepted :dispatch/revision "source/current")
                             report)))))

    (testing "every refusal type is enumerated in the contract"
      (doseq [refusal [(law/completion-refusal nil report)
                       (law/completion-refusal accepted
                                               (assoc report :completed_document "other/doc"))
                       (law/completion-refusal accepted (assoc report :batch_id "batch-9"))
                       (law/completion-refusal (assoc accepted :dispatch/outcome :dispatch/failed)
                                               report)]]
        (is (m/validate law/Refusal refusal))))))

(deftest translation-receipt-takes-identity-from-the-binding
  (let [accepted (record :batch-id "batch-7")
        receipt (law/translation-receipt accepted (law/output-revision accepted) at)]
    (testing "every identity field comes from the record, not the worker"
      (is (= :knoxx.docs/probe (:translation/document receipt)))
      (is (= :en (:translation/source-locale receipt)))
      (is (= :es (:translation/locale receipt)))
      (is (= "sha256-abc123def456" (:translation/source-revision receipt)))
      (is (= (:dispatch/key accepted) (:translation/dispatch-key receipt)))
      (is (= (:dispatch/attempt-id accepted)
             (:translation/dispatch-attempt-id receipt))))

    (testing "a worker-supplied selector output revision is refused"
      (is (thrown? js/Error (law/translation-receipt accepted "source/current" at))))

    (testing "a producer can bind the exact translated bytes into its receipt"
      (is (= "sha256-target"
             (:translation/content-digest
              (law/translation-receipt accepted
                                       (law/output-revision accepted)
                                       at "sha256-target")))))))

(deftest drift-is-decided-against-the-recorded-digest-not-the-pinned-revision
  ;; `law.publication/PublicationRevision` admits any nonblank string, so an
  ;; intent may pin an opaque revision the observer can never reproduce — it only
  ;; ever produces `sha256-...` digests. Comparing the two reported drift on
  ;; every completion of every pinned intent, forever.
  (let [pinned (assoc (record) :dispatch/revision "rev-verify-dispatch-1"
                      :dispatch/source-digest "sha256-aaa")]
    (testing "an unchanged source is not drift, even under an opaque pin"
      (is (nil? (law/source-drift-refusal pinned "sha256-aaa"))))

    (testing "a changed source is drift, and both digests travel"
      (let [refusal (law/source-drift-refusal pinned "sha256-bbb")]
        (is (= :source-moved-since-dispatch (:refusal/type refusal)))
        (is (= "sha256-aaa" (:refusal/expected refusal)))
        (is (= "sha256-bbb" (:refusal/actual refusal)))))

    (testing "an unreadable source is refused, distinctly from drift"
      (is (= :source-moved-since-dispatch
             (:refusal/type (law/source-drift-refusal pinned nil)))))

    (testing "a dispatch that recorded no digest cannot be substantiated"
      ;; Distinct refusal type: this is a dispatch that could not read its own
      ;; source, not a source that changed.
      (is (= :source-unverifiable
             (:refusal/type (law/source-drift-refusal
                             (dissoc pinned :dispatch/source-digest)
                             "sha256-aaa")))))))

(deftest the-project-is-part-of-the-dispatch-identity
  ;; Translation output is project-scoped, so changing the session project must
  ;; not let the new project reuse the old project's claims and receipts.
  (let [base {:org-id "org-1" :project "knoxx-session" :garden "a/garden" :document :a/b
              :source-locale :en :locale :es :revision "r1"}]
    (testing "a different project is a different key"
      (is (not= (law/dispatch-key base)
                (law/dispatch-key (assoc base :project "other")))))

    (testing "no project is its own scope rather than a wildcard"
      (is (not= (law/dispatch-key base)
                (law/dispatch-key (dissoc base :project)))))))

(deftest a-reports-batch-id-is-read-as-a-binding-not-as-a-value
  ;; `report-batch-id` exists because nil is not an absent join key at either
  ;; store — it is the key that selects records which never received a batch id.
  (testing "a real id is returned, trimmed"
    (is (= "batch-1" (law/report-batch-id {:batch_id "batch-1"})))
    (is (= "batch-1" (law/report-batch-id {:batch_id "  batch-1  "}))))

  (testing "nothing that fails to name a batch becomes one"
    (is (nil? (law/report-batch-id {})))
    (is (nil? (law/report-batch-id {:batch_id nil})))
    (is (nil? (law/report-batch-id {:batch_id ""})))
    (is (nil? (law/report-batch-id {:batch_id "   "}))))

  (testing "the refusal it feeds is a declared refusal type"
    ;; A caller classifies by lookup, so a refusal this namespace cannot name
    ;; would fail `Refusal` validation at the boundary it travels over.
    (is (contains? law/refusal-types :batch-id-missing))))

(deftest a-batch-projection-is-validated-before-its-outcome-is-read
  ;; The batch arrives from another repository's store. The two arrays read here
  ;; are what decide whether a translation receipt is minted, so a projection
  ;; that does not satisfy `BatchView` answers nil and the caller leaves the
  ;; claim in flight — recoverable — rather than settling it on an unread shape.
  (let [wire-id "knoxx.docs/probe"]
    (testing "a well-formed batch answers from its own arrays"
      (is (= :completed (law/batch-document-outcome
                         {:status "complete"
                          :completed_documents [wire-id]
                          :failed_documents []}
                         wire-id)))
      (is (= :failed (law/batch-document-outcome
                      {:status "partial"
                       :completed_documents []
                       :failed_documents [{:document_id wire-id}]}
                      wire-id))))

    (testing "a named failure wins over a named completion"
      ;; Something retried inside the batch; the pessimistic reading is the one
      ;; that cannot fabricate a receipt.
      (is (= :failed (law/batch-document-outcome
                      {:completed_documents [wire-id]
                       :failed_documents [wire-id]}
                      wire-id))))

    (testing "a document the batch does not name is still running"
      (is (nil? (law/batch-document-outcome
                 {:completed_documents ["someone/else"] :failed_documents []}
                 wire-id))))

    (testing "a completion array holding something other than ids is not read"
      (is (nil? (law/batch-document-outcome
                 {:completed_documents [{:document_id wire-id}]}
                 wire-id))))

    (testing "a batch that is not a map at all is not read"
      (is (nil? (law/batch-document-outcome nil wire-id)))
      (is (nil? (law/batch-document-outcome "complete" wire-id))))

    (testing "an absent batch view is a running document, not a finished one"
      ;; Absence must never be read as completion: that is the one direction
      ;; that mints evidence for a translation nobody produced.
      (is (nil? (law/batch-document-outcome {} wire-id))))))

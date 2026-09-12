(ns knoxx.backend.infra.routes.document-admission-test
  (:require [cljs.test :as test]
            [knoxx.backend.infra.routes.document-admission :as admission]
            [knoxx.backend.infra.routes.document-admission-fixture :as fixture]))

(test/deftest ^:async missing-and-blank-sources-fail-before-any-event
  (let [doc (fixture/document :knoxx.docs/missing "docs/missing.md" true)
        resource-records (fixture/records [doc])]
    (doseq [[content expected-code] [[nil "document_source_missing"]
                                     ["  \n" "document_source_blank"]]]
      (let [persisted (atom {})
            err (try
                  (await (admission/admit-documents!
                          {} (fixture/deps resource-records
                                   {(:document/id doc) content}
                                   persisted (atom []) (atom []))
                          fixture/scope {}))
                  nil
                  (catch :default error error))]
        (test/testing (str expected-code " is explicit and leaves no durable prefix")
          (test/is (= 409 (:status (ex-data err))))
          (test/is (= expected-code (:code (ex-data err))))
          (test/is (empty? @persisted)))))))

(test/deftest ^:async out-of-scope-documents-fail-before-source-reads
  (let [doc (assoc (fixture/document :knoxx.generated/private "drafts/private.md" true)
                   :document/org-id "org-2"
                   :document/visibility :private
                   :document/derived-from :knoxx.docs/source
                   :document/derived-source-revision "sha256-source")
        resource-records (fixture/records [doc])
        persisted (atom {})
        source-reads (atom 0)
        dependencies (assoc (fixture/deps resource-records
                                  {(:document/id doc) "# Private"}
                                  persisted (atom []) (atom []))
                            :source-content!
                            (fn [_root _document]
                              (swap! source-reads inc)
                              (js/Promise.resolve "# Private")))
        err (try
              (await (admission/admit-documents!
                      {} dependencies fixture/scope {:document (:document/id doc)}))
              nil
              (catch :default error error))]
    (test/is (= 404 (:status (ex-data err))))
    (test/is (= "document_admission_document_not_found"
           (:code (ex-data err))))
    (test/is (zero? @source-reads))
    (test/is (empty? @persisted))))

(test/deftest ^:async unchanged-retry-reuses-event-identities
  (let [doc (fixture/document :knoxx.docs/anchor "docs/anchor.md" true)
        resource-records (fixture/records [doc])
        persisted (atom {})
        emitted (atom [])
        dispatches (atom [])
        dependencies (fixture/deps resource-records
                           {(:document/id doc) "# Anchor"}
                           persisted emitted dispatches)
        first-result (await (admission/admit-documents!
                             {} dependencies fixture/scope {}))
        retry-result (await (admission/admit-documents!
                             {} dependencies fixture/scope {}))
        first-row (first (:results first-result))
        retry-row (first (:results retry-result))]
    (test/testing "both durable ids are content addressed"
      (test/is (= (:index/source-event-id first-row)
             (:index/source-event-id retry-row)))
      (test/is (= (:index/event-id first-row)
             (:index/event-id retry-row)))
      (test/is (= 2 (count @persisted))))

    (test/testing "duplicates are successful existing facts, not a failed deploy"
      (test/is (true? (:ok retry-result)))
      (test/is (= 0 (:failed retry-result)))
      (test/is (= :existing (:index/source-event-status retry-row)))
      (test/is (= :existing (:index/event-status retry-row))))))

(test/deftest ^:async concurrent-identical-admissions-write-each-base-event-once
  (let [doc (fixture/document :knoxx.docs/concurrent "docs/concurrent.md" true)
        resource-records (fixture/records [doc])
        rows (atom [])
        insert-attempts (atom [])
        first-insert (fixture/deferred)
        base (fixture/deps resource-records
                   {(:document/id doc) "# Concurrent admission"}
                   (atom {}) (atom []) (atom []))
        persistence
        (^:async fn [event]
          (if (some #(= (:id event) (:id %)) @rows)
            {:ok true :existing true :ids [(:id event)]}
            (let [first-attempt? (empty? @insert-attempts)]
              (swap! insert-attempts conj (:id event))
              (when first-attempt?
                (await (:promise first-insert)))
              ;; Model OpenPlanner's current non-unique read-before-insert seam:
              ;; the decision above is not repeated after the deferred write.
              (swap! rows conj event)
              {:ok true :ids [(:id event)]})))
        dependencies (assoc base :persist-event! persistence)
        first-result (admission/admit-documents! {} dependencies fixture/scope {})
        concurrent-result (admission/admit-documents! {} dependencies fixture/scope {})]
    (try
      (await (fixture/flush-promises!))
      (test/testing "only the first pass reaches the uncommitted event insertion"
        (test/is (= 1 (count @insert-attempts)))
        (test/is (empty? @rows)))
      (finally
        ((:resolve! first-insert) nil)))
    (let [first-response (await first-result)
          concurrent-response (await concurrent-result)
          first-row (first (:results first-response))
          concurrent-row (first (:results concurrent-response))]
      (test/is (true? (:ok first-response)))
      (test/is (true? (:ok concurrent-response)))
      (test/is (= 2 (count @rows)))
      (test/is (= {"docs" 1 "publication.document.indexed" 1}
             (frequencies (map :kind @rows))))
      (test/is (= 2 (count (distinct (map :id @rows)))))
      (test/is (= :recorded (:index/source-event-status first-row)))
      (test/is (= :recorded (:index/event-status first-row)))
      (test/is (= :existing (:index/source-event-status concurrent-row)))
      (test/is (= :existing (:index/event-status concurrent-row))))))

(test/deftest ^:async admission-barrier-waits-for-every-previously-queued-pass
  (let [doc (fixture/document :knoxx.docs/barrier "docs/barrier.md" true)
        resource-records (fixture/records [doc])
        first-insert (fixture/deferred)
        barrier-settled? (atom false)
        dependencies
        (assoc (fixture/deps resource-records
                     {(:document/id doc) "# Admission barrier"}
                     (atom {}) (atom []) (atom []))
               :persist-event!
               (^:async fn [event]
                 (await (:promise first-insert))
                 {:ok true :ids [(:id event)]}))
        admission-result (admission/admit-documents!
                          {} dependencies fixture/scope {})
        barrier-result
        (.then (admission/await-document-admission-barrier!)
               (fn [result]
                 (reset! barrier-settled? true)
                 result))]
    (await (fixture/flush-promises!))
    (test/is (false? @barrier-settled?)
        "the barrier cannot pass a still-running admission")
    ((:resolve! first-insert) true)
    (test/is (true? (:ok (await admission-result))))
    (test/is (= {:settled true} (await barrier-result)))
    (test/is (true? @barrier-settled?))))

(test/deftest ^:async exact-selection-does-not-admit-other-anchors
  (let [anchor (fixture/document :knoxx.docs/anchor "docs/anchor.md" true)
        exact (fixture/document :knoxx.docs/exact "docs/exact.md" false)
        resource-records (fixture/records [anchor exact])
        persisted (atom {})
        emitted (atom [])
        dispatches (atom [])
        result (await (admission/admit-documents!
                       {} (fixture/deps resource-records
                                {:knoxx.docs/anchor "# Anchor"
                                 :knoxx.docs/exact "# Exact"}
                                persisted emitted dispatches)
                       fixture/scope {:document :knoxx.docs/exact}))]
    (test/is (= 1 (:selected result)))
    (test/is (= [:knoxx.docs/exact]
           (mapv :document/id (:results result))))
    (test/is (= [:knoxx.docs/exact]
           (mapv :document-id @dispatches)))
    (test/is (= 2 (count @persisted)))
    (test/is (= :knoxx.docs/exact
           (get-in (first @emitted) [:event/payload :document/id])))))

(test/deftest ^:async every-admitted-document-auto-dispatches-from-the-snapshot
  (let [a (fixture/document :knoxx.docs/a "docs/a.md" true)
        b (fixture/document :knoxx.docs/b "docs/b.md" true)
        resource-records (fixture/records [a b])
        dispatches (atom [])
        result (await (admission/admit-documents!
                       {} (fixture/deps resource-records
                                {:knoxx.docs/a "# A"
                                 :knoxx.docs/b "# B"}
                                (atom {}) (atom []) dispatches)
                       fixture/scope {}))]
    (test/is (= 2 (:admitted result)))
    (test/is (= 2 (get-in result [:translations :documents])))
    (test/is (= 2 (get-in result [:translations :dispatched])))
    (test/is (= #{:knoxx.docs/a :knoxx.docs/b}
           (set (map :document-id @dispatches))))

    (let [{:keys [snapshot-deps]} (first @dispatches)
          pinned-records (await ((:resource-records! snapshot-deps) {}))
          pinned-index ((:publication-index snapshot-deps) pinned-records)
          pinned-revisions (await ((:source-revisions! snapshot-deps)
                                   {} [] {}))]
      (test/testing "translation sees the already loaded resource/index/revision snapshot"
        (test/is (identical? resource-records pinned-records))
        (test/is (= #{:knoxx.docs/a :knoxx.docs/b}
               (set (keys (:documents pinned-index)))))
        (test/is (= #{:knoxx.docs/a :knoxx.docs/b}
               (set (keys pinned-revisions))))))))

(test/deftest ^:async admission-repairs-completed-translation-projections-once
  (let [doc (fixture/document :knoxx.docs/repair-before-admit
                      "docs/repair-before-admit.md" true)
        resource-records (fixture/records [doc])
        persisted (atom {})
        calls (atom [])
        base (fixture/deps resource-records
                   {(:document/id doc) "# Repair before admit"}
                   persisted (atom []) (atom []))
        dependencies
        (assoc base
               :repair-translation-events!
               (fn [actual-scope]
                 (swap! calls conj [:repair actual-scope (count @persisted)])
                 (js/Promise.resolve {:translation/event-repair-receipt-count 0}))
               :persist-event!
               (fn [event]
                 (swap! calls conj [:persist (:kind event)])
                 (fixture/persist-once! persisted event)))
        result (await (admission/admit-documents!
                       {} dependencies fixture/scope {}))]
    (test/is (true? (:ok result)))
    (test/is (= [[:repair fixture/scope 0]
            [:persist "docs"]
            [:persist "publication.document.indexed"]]
           @calls))))

(test/deftest ^:async source-preflight-failure-does-not-run-projection-repair
  (let [doc (fixture/document :knoxx.docs/missing-before-repair
                      "docs/missing-before-repair.md" true)
        resource-records (fixture/records [doc])
        repairs (atom 0)
        err (try
              (await
               (admission/admit-documents!
                {}
                (assoc (fixture/deps resource-records {} (atom {}) (atom []) (atom []))
                       :repair-translation-events!
                       (fn [_scope]
                         (swap! repairs inc)
                         (js/Promise.resolve {})))
                fixture/scope {}))
              nil
              (catch :default error error))]
    (test/is (= "document_source_missing" (:code (ex-data err))))
    (test/is (zero? @repairs))))

(test/deftest ^:async a-missing-post-drafter-trigger-fails-draft-admission
  (let [doc (fixture/document :knoxx.docs/draft-request "docs/draft-request.md" true)
        resource-records (fixture/records [doc])
        persisted (atom {})
        emitted (atom [])
        dispatches (atom [])
        err (try
              (await (admission/admit-documents!
                      {} (fixture/deps resource-records
                               {(:document/id doc) "# Draft request"}
                               persisted emitted dispatches)
                      fixture/scope {:generate-drafts? true}))
              nil
              (catch :default error error))]
    (test/testing "the admission names the absent post-draft trigger"
      (test/is (= 503 (:status (ex-data err))))
      (test/is (= "document_post_draft_trigger_missing" (:code (ex-data err))))
      (test/is (= (:document/id doc) (:document/id (ex-data err)))))
    (test/testing "translation never starts after post-draft dispatch was unhandled"
      (test/is (= 1 (count @emitted)))
      (test/is (empty? @dispatches)))
    (test/testing "the immutable source and index events remain visible"
      (test/is (= 2 (count @persisted))))))

(test/deftest ^:async deployment-draft-override-cannot-recurse-from-a-derived-document
  (let [doc (assoc (fixture/document :knoxx.docs/derived "docs/derived.md" true)
                   :document/derived-from :knoxx.docs/source
                   :document/derived-source-revision "sha256-source"
                   :document/generate-drafts? false)
        resource-records (fixture/records [doc])
        emitted (atom [])
        result (await (admission/admit-documents!
                       {} (fixture/deps resource-records
                                {(:document/id doc) "# Derived post"}
                                (atom {}) emitted (atom []))
                       fixture/scope {:generate-drafts? true}))
        row (first (:results result))
        event (first @emitted)]
    (test/is (true? (:ok result)))
    (test/is (false? (:document/generate-drafts? row)))
    (test/is (false? (get-in event [:event/payload
                               :document/generate-drafts?])))))

(test/deftest ^:async a-translation-trigger-failure-is-visible-in-the-admission-response
  (let [doc (fixture/document :knoxx.docs/unhandled-translation
                      "docs/unhandled-translation.md" true)
        resource-records (fixture/records [doc])
        dependencies
        (assoc (fixture/deps resource-records
                     {(:document/id doc) "# Unhandled translation"}
                     (atom {}) (atom []) (atom []))
               :dispatch-document!
               (fn [_document-id _snapshot-deps]
                 (js/Promise.resolve
                  {:considered 1
                   :admissible 1
                   :runner :agent
                   :dispatched
                   [{:dispatch/outcome :dispatch/failed
                     :dispatch/detail
                     "no enabled trigger subscribes to :publication/translation-needed"}]})))
        result (await (admission/admit-documents! {} dependencies fixture/scope {}))
        row (first (:results result))]
    (test/is (false? (:ok result)))
    (test/is (= 1 (:failed result)))
    (test/is (= 1 (get-in result [:translations :failed])))
    (test/is (false? (:ok row)))
    (test/is (= 1 (:failed row)))))

(test/deftest ^:async unreachable-or-refused-translation-fails-admission
  (let [doc (fixture/document :knoxx.docs/nonproductive-translation
                      "docs/nonproductive-translation.md" true)
        resource-records (fixture/records [doc])]
    (doseq [[label dispatch-result]
            [[:unreachable
              {:dispatch/outcome :dispatch/unreachable
               :translation/refusal
               {:refusal/type :dispatch-source-revision-mismatch}}]
             [:refused
              {:dispatch/outcome :dispatch/duplicate
               :translation/refusal
               {:refusal/type :dispatch-already-resolved}}]]]
      (let [dependencies
            (assoc (fixture/deps resource-records
                         {(:document/id doc) "# Nonproductive translation"}
                         (atom {}) (atom []) (atom []))
                   :dispatch-document!
                   (fn [_document-id _snapshot-deps]
                     (js/Promise.resolve
                      {:considered 1
                       :admissible 1
                       :runner :agent
                       :dispatched [dispatch-result]})))
            result (await (admission/admit-documents! {} dependencies fixture/scope {}))
            row (first (:results result))]
        (test/testing (str (name label) " terminal result is deployment-visible")
          (test/is (false? (:ok result)))
          (test/is (= 1 (:failed result)))
          (test/is (= 1 (get-in result [:translations :failed])))
          (test/is (false? (:ok row)))
          (test/is (= 1 (:failed row))))))))

(test/deftest ^:async a-completed-source-revision-draft-reuses-durable-event-identities
  (let [doc (fixture/document :knoxx.docs/already-drafted "docs/drafted.md" true)
        resource-records (fixture/records [doc])
        draft-complete (atom false)
        persisted (atom {})
        emitted (atom [])
        dependencies (assoc (fixture/deps resource-records
                                  {(:document/id doc) "# Already drafted"}
                                  persisted emitted (atom []))
                            :draft-complete?
                            (fn [policy]
                              (test/is (= (:document/id doc)
                                     (:source-document-id policy)))
                              (test/is (string? (:source-revision policy)))
                              (test/is (= :en (:source-locale policy)))
                              (test/is (= (:org-id fixture/scope) (:org-id policy)))
                              (test/is (= (:project fixture/scope) (:project policy)))
                              (test/is (= [{:garden/id :knoxx.gardens/main
                                       :garden/locales [:es]}]
                                     (:gardens policy)))
                              (js/Promise.resolve @draft-complete))
                            :emit-indexed!
                            (fn [event]
                              (swap! emitted conj event)
                              (js/Promise.resolve
                               {:matchedTriggers
                                (if (get-in event [:event/payload
                                                   :document/generate-drafts?])
                                  [:craft-post-from-indexed-document]
                                  [])})))
        first-result (await (admission/admit-documents!
                             {} dependencies fixture/scope {:generate-drafts? true}))
        _ (reset! draft-complete true)
        retry-result (await (admission/admit-documents!
                             {} dependencies fixture/scope {:generate-drafts? true}))
        first-row (first (:results first-result))
        retry-row (first (:results retry-result))
        [first-event retry-event] @emitted]
    (test/testing "requested policy remains part of both durable event identities"
      (test/is (true? (:document/generate-drafts? first-row)))
      (test/is (true? (:document/generate-drafts? retry-row)))
      (test/is (= (:index/source-event-id first-row)
             (:index/source-event-id retry-row)))
      (test/is (= (:index/event-id first-row)
             (:index/event-id retry-row)))
      (test/is (= 2 (count @persisted))))
    (test/testing "only the transient trigger decision changes after completion"
      (test/is (true? (:document/draft-generation-needed? first-row)))
      (test/is (false? (:document/draft-generation-needed? retry-row)))
      (test/is (false? (:document/draft-generation-complete? first-row)))
      (test/is (true? (:document/draft-generation-complete? retry-row)))
      (test/is (true? (get-in first-event [:event/payload
                                      :document/generate-drafts?])))
      (test/is (false? (get-in retry-event [:event/payload
                                       :document/generate-drafts?]))))))

(test/deftest ^:async completed-draft-corruption-fails-before-events-or-model-work
  (let [doc (fixture/document :knoxx.docs/corrupt-draft
                      "docs/corrupt-draft.md" true)
        resource-records (fixture/records [doc])
        persisted (atom {})
        emitted (atom [])
        dispatches (atom [])
        error (try
                (await
                 (admission/admit-documents!
                  {}
                  (assoc (fixture/deps resource-records
                               {(:document/id doc) "# Corrupt draft"}
                               persisted emitted dispatches)
                         :draft-complete?
                         (fn [_policy]
                           (js/Promise.reject
                            (ex-info
                             "completed generated draft is missing immutable files"
                             {:code :generated-draft-conflict
                              :path "/generated/missing.md"}))))
                  fixture/scope {:generate-drafts? true}))
                nil
                (catch :default cause cause))]
    (test/is (= :generated-draft-conflict (:code (ex-data error))))
    (test/testing "preflight rejects before durable events or provider dispatch"
      (test/is (empty? @persisted))
      (test/is (empty? @emitted))
      (test/is (empty? @dispatches)))))

(ns knoxx.backend.infra.routes.document-admission-test
  (:require ["@open-hax/openplanner-sdk" :as sdk-mod]
            [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.openplanner-sdk :as xsdk]
            [knoxx.backend.infra.agent.runner :as agent-runner]
            [knoxx.backend.infra.routes.document-admission :as admission]))

(defn document
  [id path anchor?]
  {:document/id id
   :document/title (name id)
   :document/source-locale :en
   :document/source {:path path}
   :document/anchor? anchor?})

(def garden
  {:garden/id :knoxx.gardens/main
   :garden/title "Main"
   :garden/status :active
   :garden/locales [:en :es]})

(defn publication
  [id document-id]
  {:publication/id id
   :publication/document document-id
   :publication/garden :knoxx.gardens/main
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :draft
   :publication/path (str "/" (name id))
   :translation/review :required})

(defn record
  [kind definition suffix]
  {:ok? true
   :resource/kind kind
   :resource/file-path (str "/workspace/contracts/" suffix ".edn")
   :resource/definition definition})

(defn records
  [documents]
  (into [(record :garden garden "garden")]
        (concat
         (map-indexed (fn [idx doc]
                        (record :document doc (str "document-" idx)))
                      documents)
         (map-indexed (fn [idx doc]
                        (record :publication
                                (publication
                                 (keyword "knoxx.publications"
                                          (str (name (:document/id doc)) "-es"))
                                 (:document/id doc))
                                (str "publication-" idx)))
                      documents))))

(def scope
  {:org-id "org-1"
   :membership-id "member-1"
   :project "knoxx-local"})

(defn- flush-promises!
  []
  (js/Promise. (fn [resolve _reject]
                 (js/setTimeout resolve 0))))

(defn duplicate-error
  []
  (doto (js/Error. "E11000 duplicate key")
    (aset "code" 11000)))

(defn deferred
  []
  (let [resolve* (atom nil)
        promise (js/Promise.
                 (fn [resolve _reject]
                   (reset! resolve* resolve)))]
    {:promise promise
     :resolve! (fn [value] (@resolve* value))}))

(defn source-roots
  [resource-records]
  (into {}
        (map (fn [entry]
               [(get-in entry [:resource/definition :document/id])
                "/workspace"])
             (filter #(= :document (:resource/kind %)) resource-records))))

(defn persist-once!
  [persisted event]
  (if (contains? @persisted (:id event))
    (throw (duplicate-error))
    (do (swap! persisted assoc (:id event) event)
        (js/Promise.resolve {:ok true :ids [(:id event)]}))))

(defn deps
  [resource-records contents persisted emitted dispatches]
  {:resource-records! (fn [_] (js/Promise.resolve resource-records))
   :document-source-roots (fn [_ _] (source-roots resource-records))
   :canonical-document-path! (fn [root doc]
                               (js/Promise.resolve
                                (str root "/" (get-in doc [:document/source :path]))))
   :source-content! (fn [_root doc]
                      (js/Promise.resolve (get contents (:document/id doc))))
   :draft-complete? (fn [_policy]
                      (js/Promise.resolve false))
   :persist-event! (fn [event] (persist-once! persisted event))
   :emit-indexed! (fn [event]
                    (swap! emitted conj event)
                    (js/Promise.resolve {:matchedTriggers []}))
   :register-turn-settler! (fn [_event-id _settle!] true)
   :unregister-turn-settler! (fn [_event-id] true)
   :release-indexed-event! (fn [_event-id] true)
   :dispatch-document! (fn [document-id snapshot-deps]
                         (swap! dispatches conj
                                {:document-id document-id
                                 :snapshot-deps snapshot-deps})
                         (js/Promise.resolve
                          {:considered 1
                           :admissible 1
                           :runner :agent
                           :dispatched [{:dispatch/outcome
                                         :dispatch/claimed}]}))
   :clock (constantly "2026-09-02T12:00:00.000Z")
   :digest-hex (fn [value] (str "digest-" (hash value)))})

(deftest ^:async missing-and-blank-sources-fail-before-any-event
  (let [doc (document :knoxx.docs/missing "docs/missing.md" true)
        resource-records (records [doc])]
    (doseq [[content expected-code] [[nil "document_source_missing"]
                                     ["  \n" "document_source_blank"]]]
      (let [persisted (atom {})
            err (try
                  (await (admission/admit-documents!
                          {} (deps resource-records
                                   {(:document/id doc) content}
                                   persisted (atom []) (atom []))
                          scope {}))
                  nil
                  (catch :default error error))]
        (testing (str expected-code " is explicit and leaves no durable prefix")
          (is (= 409 (:status (ex-data err))))
          (is (= expected-code (:code (ex-data err))))
          (is (empty? @persisted)))))))

(deftest ^:async unchanged-retry-reuses-event-identities
  (let [doc (document :knoxx.docs/anchor "docs/anchor.md" true)
        resource-records (records [doc])
        persisted (atom {})
        emitted (atom [])
        dispatches (atom [])
        dependencies (deps resource-records
                           {(:document/id doc) "# Anchor"}
                           persisted emitted dispatches)
        first-result (await (admission/admit-documents!
                             {} dependencies scope {}))
        retry-result (await (admission/admit-documents!
                             {} dependencies scope {}))
        first-row (first (:results first-result))
        retry-row (first (:results retry-result))]
    (testing "both durable ids are content addressed"
      (is (= (:index/source-event-id first-row)
             (:index/source-event-id retry-row)))
      (is (= (:index/event-id first-row)
             (:index/event-id retry-row)))
      (is (= 2 (count @persisted))))

    (testing "duplicates are successful existing facts, not a failed deploy"
      (is (true? (:ok retry-result)))
      (is (= 0 (:failed retry-result)))
      (is (= :existing (:index/source-event-status retry-row)))
      (is (= :existing (:index/event-status retry-row))))))

(deftest ^:async concurrent-identical-admissions-write-each-base-event-once
  (let [doc (document :knoxx.docs/concurrent "docs/concurrent.md" true)
        resource-records (records [doc])
        rows (atom [])
        insert-attempts (atom [])
        first-insert (deferred)
        base (deps resource-records
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
        first-result (admission/admit-documents! {} dependencies scope {})
        concurrent-result (admission/admit-documents! {} dependencies scope {})]
    (try
      (await (flush-promises!))
      (testing "only the first pass reaches the uncommitted event insertion"
        (is (= 1 (count @insert-attempts)))
        (is (empty? @rows)))
      (finally
        ((:resolve! first-insert) nil)))
    (let [first-response (await first-result)
          concurrent-response (await concurrent-result)
          first-row (first (:results first-response))
          concurrent-row (first (:results concurrent-response))]
      (is (true? (:ok first-response)))
      (is (true? (:ok concurrent-response)))
      (is (= 2 (count @rows)))
      (is (= {"docs" 1 "publication.document.indexed" 1}
             (frequencies (map :kind @rows))))
      (is (= 2 (count (distinct (map :id @rows)))))
      (is (= :recorded (:index/source-event-status first-row)))
      (is (= :recorded (:index/event-status first-row)))
      (is (= :existing (:index/source-event-status concurrent-row)))
      (is (= :existing (:index/event-status concurrent-row))))))

(deftest ^:async exact-selection-does-not-admit-other-anchors
  (let [anchor (document :knoxx.docs/anchor "docs/anchor.md" true)
        exact (document :knoxx.docs/exact "docs/exact.md" false)
        resource-records (records [anchor exact])
        persisted (atom {})
        emitted (atom [])
        dispatches (atom [])
        result (await (admission/admit-documents!
                       {} (deps resource-records
                                {:knoxx.docs/anchor "# Anchor"
                                 :knoxx.docs/exact "# Exact"}
                                persisted emitted dispatches)
                       scope {:document :knoxx.docs/exact}))]
    (is (= 1 (:selected result)))
    (is (= [:knoxx.docs/exact]
           (mapv :document/id (:results result))))
    (is (= [:knoxx.docs/exact]
           (mapv :document-id @dispatches)))
    (is (= 2 (count @persisted)))
    (is (= :knoxx.docs/exact
           (get-in (first @emitted) [:event/payload :document/id])))))

(deftest ^:async every-admitted-document-auto-dispatches-from-the-snapshot
  (let [a (document :knoxx.docs/a "docs/a.md" true)
        b (document :knoxx.docs/b "docs/b.md" true)
        resource-records (records [a b])
        dispatches (atom [])
        result (await (admission/admit-documents!
                       {} (deps resource-records
                                {:knoxx.docs/a "# A"
                                 :knoxx.docs/b "# B"}
                                (atom {}) (atom []) dispatches)
                       scope {}))]
    (is (= 2 (:admitted result)))
    (is (= 2 (get-in result [:translations :documents])))
    (is (= 2 (get-in result [:translations :dispatched])))
    (is (= #{:knoxx.docs/a :knoxx.docs/b}
           (set (map :document-id @dispatches))))

    (let [{:keys [snapshot-deps]} (first @dispatches)
          pinned-records (await ((:resource-records! snapshot-deps) {}))
          pinned-index ((:publication-index snapshot-deps) pinned-records)
          pinned-revisions (await ((:source-revisions! snapshot-deps)
                                   {} [] {}))]
      (testing "translation sees the already loaded resource/index/revision snapshot"
        (is (identical? resource-records pinned-records))
        (is (= #{:knoxx.docs/a :knoxx.docs/b}
               (set (keys (:documents pinned-index)))))
        (is (= #{:knoxx.docs/a :knoxx.docs/b}
               (set (keys pinned-revisions))))))))

(deftest ^:async admission-repairs-completed-translation-projections-once
  (let [doc (document :knoxx.docs/repair-before-admit
                      "docs/repair-before-admit.md" true)
        resource-records (records [doc])
        persisted (atom {})
        calls (atom [])
        base (deps resource-records
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
                 (persist-once! persisted event)))
        result (await (admission/admit-documents!
                       {} dependencies scope {}))]
    (is (true? (:ok result)))
    (is (= [[:repair scope 0]
            [:persist "docs"]
            [:persist "publication.document.indexed"]]
           @calls))))

(deftest ^:async source-preflight-failure-does-not-run-projection-repair
  (let [doc (document :knoxx.docs/missing-before-repair
                      "docs/missing-before-repair.md" true)
        resource-records (records [doc])
        repairs (atom 0)
        err (try
              (await
               (admission/admit-documents!
                {}
                (assoc (deps resource-records {} (atom {}) (atom []) (atom []))
                       :repair-translation-events!
                       (fn [_scope]
                         (swap! repairs inc)
                         (js/Promise.resolve {})))
                scope {}))
              nil
              (catch :default error error))]
    (is (= "document_source_missing" (:code (ex-data err))))
    (is (zero? @repairs))))

(deftest ^:async a-missing-post-drafter-trigger-fails-draft-admission
  (let [doc (document :knoxx.docs/draft-request "docs/draft-request.md" true)
        resource-records (records [doc])
        persisted (atom {})
        emitted (atom [])
        dispatches (atom [])
        err (try
              (await (admission/admit-documents!
                      {} (deps resource-records
                               {(:document/id doc) "# Draft request"}
                               persisted emitted dispatches)
                      scope {:generate-drafts? true}))
              nil
              (catch :default error error))]
    (testing "the admission names the absent post-draft trigger"
      (is (= 503 (:status (ex-data err))))
      (is (= "document_post_draft_trigger_missing" (:code (ex-data err))))
      (is (= (:document/id doc) (:document/id (ex-data err)))))
    (testing "translation never starts after post-draft dispatch was unhandled"
      (is (= 1 (count @emitted)))
      (is (empty? @dispatches)))
    (testing "the immutable source and index events remain visible"
      (is (= 2 (count @persisted))))))

(deftest ^:async deployment-draft-override-cannot-recurse-from-a-derived-document
  (let [doc (assoc (document :knoxx.docs/derived "docs/derived.md" true)
                   :document/derived-from :knoxx.docs/source
                   :document/derived-source-revision "sha256-source"
                   :document/generate-drafts? false)
        resource-records (records [doc])
        emitted (atom [])
        result (await (admission/admit-documents!
                       {} (deps resource-records
                                {(:document/id doc) "# Derived post"}
                                (atom {}) emitted (atom []))
                       scope {:generate-drafts? true}))
        row (first (:results result))
        event (first @emitted)]
    (is (true? (:ok result)))
    (is (false? (:document/generate-drafts? row)))
    (is (false? (get-in event [:event/payload
                               :document/generate-drafts?])))))

(deftest ^:async a-translation-trigger-failure-is-visible-in-the-admission-response
  (let [doc (document :knoxx.docs/unhandled-translation
                      "docs/unhandled-translation.md" true)
        resource-records (records [doc])
        dependencies
        (assoc (deps resource-records
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
        result (await (admission/admit-documents! {} dependencies scope {}))
        row (first (:results result))]
    (is (false? (:ok result)))
    (is (= 1 (:failed result)))
    (is (= 1 (get-in result [:translations :failed])))
    (is (false? (:ok row)))
    (is (= 1 (:failed row)))))

(deftest ^:async unreachable-or-refused-translation-fails-admission
  (let [doc (document :knoxx.docs/nonproductive-translation
                      "docs/nonproductive-translation.md" true)
        resource-records (records [doc])]
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
            (assoc (deps resource-records
                         {(:document/id doc) "# Nonproductive translation"}
                         (atom {}) (atom []) (atom []))
                   :dispatch-document!
                   (fn [_document-id _snapshot-deps]
                     (js/Promise.resolve
                      {:considered 1
                       :admissible 1
                       :runner :agent
                       :dispatched [dispatch-result]})))
            result (await (admission/admit-documents! {} dependencies scope {}))
            row (first (:results result))]
        (testing (str (name label) " terminal result is deployment-visible")
          (is (false? (:ok result)))
          (is (= 1 (:failed result)))
          (is (= 1 (get-in result [:translations :failed])))
          (is (false? (:ok row)))
          (is (= 1 (:failed row))))))))

(deftest ^:async a-completed-source-revision-draft-reuses-durable-event-identities
  (let [doc (document :knoxx.docs/already-drafted "docs/drafted.md" true)
        resource-records (records [doc])
        draft-complete (atom false)
        persisted (atom {})
        emitted (atom [])
        dependencies (assoc (deps resource-records
                                  {(:document/id doc) "# Already drafted"}
                                  persisted emitted (atom []))
                            :draft-complete?
                            (fn [policy]
                              (is (= (:document/id doc)
                                     (:source-document-id policy)))
                              (is (string? (:source-revision policy)))
                              (is (= :en (:source-locale policy)))
                              (is (= (:org-id scope) (:org-id policy)))
                              (is (= (:project scope) (:project policy)))
                              (is (= [{:garden/id :knoxx.gardens/main
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
                             {} dependencies scope {:generate-drafts? true}))
        _ (reset! draft-complete true)
        retry-result (await (admission/admit-documents!
                             {} dependencies scope {:generate-drafts? true}))
        first-row (first (:results first-result))
        retry-row (first (:results retry-result))
        [first-event retry-event] @emitted]
    (testing "requested policy remains part of both durable event identities"
      (is (true? (:document/generate-drafts? first-row)))
      (is (true? (:document/generate-drafts? retry-row)))
      (is (= (:index/source-event-id first-row)
             (:index/source-event-id retry-row)))
      (is (= (:index/event-id first-row)
             (:index/event-id retry-row)))
      (is (= 2 (count @persisted))))
    (testing "only the transient trigger decision changes after completion"
      (is (true? (:document/draft-generation-needed? first-row)))
      (is (false? (:document/draft-generation-needed? retry-row)))
      (is (false? (:document/draft-generation-complete? first-row)))
      (is (true? (:document/draft-generation-complete? retry-row)))
      (is (true? (get-in first-event [:event/payload
                                      :document/generate-drafts?])))
      (is (false? (get-in retry-event [:event/payload
                                       :document/generate-drafts?]))))))

(defn- draft-owner-dependencies
  [doc draft-complete settlers]
  {:draft-complete?
   (fn [policy]
     (is (= (:document/id doc) (:source-document-id policy)))
     (is (string? (:source-revision policy)))
     (js/Promise.resolve @draft-complete))
   :register-turn-settler!
   (fn [event-id settle!]
     (swap! settlers assoc event-id settle!)
     true)
   :unregister-turn-settler!
   (fn [event-id]
     (swap! settlers dissoc event-id)
     true)})

(defn- indexed-event-dedup-dependencies
  [claimed-event-ids emitted releases]
  {:release-indexed-event!
   (fn [event-id]
     (swap! releases conj event-id)
     (swap! claimed-event-ids disj event-id)
     true)
   :emit-indexed!
   (fn [event]
     (let [event-id (:event/id event)]
       (if (contains? @claimed-event-ids event-id)
         (js/Promise.resolve {:matchedTriggers [] :skipped true})
         (do
           (swap! claimed-event-ids conj event-id)
           (swap! emitted conj event-id)
           (js/Promise.resolve
            {:matchedTriggers [:craft-post-from-indexed-document]})))))})

(defn- draft-retry-dependencies
  [doc resource-records persisted draft-complete emitted releases settlers]
  (merge (deps resource-records
               {(:document/id doc) "# Retry draft"}
               persisted (atom []) (atom []))
         (draft-owner-dependencies doc draft-complete settlers)
         (indexed-event-dedup-dependencies
          (atom #{}) emitted releases)))

(defn- ^:async exercise-absent-draft-retry!
  [settlement]
  (let [doc (document :knoxx.docs/retry-draft "docs/retry-draft.md" true)
        resource-records (records [doc])
        persisted (atom {})
        draft-complete (atom false)
        emitted (atom [])
        releases (atom [])
        settlers (atom {})
        dependencies (draft-retry-dependencies
                      doc resource-records persisted draft-complete emitted
                      releases settlers)
        first-result (await (admission/admit-documents!
                             {} dependencies scope {:generate-drafts? true}))
        event-id (get-in first-result [:results 0 :index/event-id])
        settle! (get @settlers event-id)]
    (is (fn? settle!))
    (await (settle! settlement))
    (let [retry-result (await (admission/admit-documents!
                               {} dependencies scope {:generate-drafts? true}))]
      {:event-id event-id
       :first-result first-result
       :retry-result retry-result
       :persisted persisted
       :emitted emitted
       :releases releases})))

(deftest ^:async a-rejected-draft-provider-turn-is-retriable-in-process
  (let [{:keys [event-id first-result retry-result persisted emitted releases]}
        (await (exercise-absent-draft-retry!
                {:event-turn/status :failed
                 :event-turn/detail "provider unavailable"}))]
    (testing "the failed terminal owner releases only its indexed event"
      (is (= [event-id] @releases))
      (is (= [event-id event-id] @emitted)))
    (testing "re-admission reuses durable facts and enqueues a fresh attempt"
      (is (true? (:ok first-result)))
      (is (true? (:ok retry-result)))
      (is (= 2 (count @persisted)))
      (is (= :existing (get-in retry-result
                               [:results 0 :index/event-status]))))))

(deftest ^:async a-draft-turn-that-never-calls-its-tool-is-retriable-in-process
  (let [{:keys [event-id retry-result emitted releases]}
        (await (exercise-absent-draft-retry!
                {:event-turn/status :completed}))]
    (testing "successful provider completion is not mistaken for a saved draft"
      (is (= [event-id] @releases))
      (is (= [event-id event-id] @emitted))
      (is (true? (:ok retry-result))))))

(deftest ^:async a-vanished-completed-draft-reclaims-its-dispatch-in-process
  (let [doc (document :knoxx.docs/vanished-draft
                      "docs/vanished-draft.md" true)
        resource-records (records [doc])
        persisted (atom {})
        draft-complete (atom false)
        dispatch-states (atom {})
        settlers (atom {})
        emitted (atom [])
        releases (atom [])
        dependencies
        (merge
         (deps resource-records
               {(:document/id doc) "# Vanished draft"}
               persisted (atom []) (atom []))
         {:draft-complete?
          (fn [_policy]
            (js/Promise.resolve @draft-complete))
          :indexed-event-state
          (fn [event-id]
            (get @dispatch-states event-id))
          :draft-event-owner-state
          (fn [event-id]
            (when (contains? @settlers event-id) :in-flight))
          :release-indexed-event!
          (fn [event-id]
            (swap! releases conj event-id)
            (swap! dispatch-states dissoc event-id)
            true)
          :register-turn-settler!
          (fn [event-id settle!]
            (swap! settlers assoc event-id
                   (^:async fn [settlement]
                     (let [result (await (settle! settlement))]
                       (swap! settlers dissoc event-id)
                       result)))
            true)
          :unregister-turn-settler!
          (fn [event-id]
            (swap! settlers dissoc event-id)
            true)
          :emit-indexed!
          (fn [event]
            (let [event-id (:event/id event)]
              (if-let [state (get @dispatch-states event-id)]
                (js/Promise.resolve
                 {:matchedTriggers []
                  :skipped true
                  :dedup/status state})
                (do
                  (swap! dispatch-states assoc event-id :completed)
                  (swap! emitted conj event-id)
                  (js/Promise.resolve
                   {:matchedTriggers
                    [:craft-post-from-indexed-document]
                    :dedup/status :completed})))))})
        first-result (await (admission/admit-documents!
                             {} dependencies scope {:generate-drafts? true}))
        event-id (get-in first-result [:results 0 :index/event-id])
        first-owner (get @settlers event-id)
        live-retry (await (admission/admit-documents!
                           {} dependencies scope {:generate-drafts? true}))]
    (testing "a currently owned generation is not duplicated"
      (is (true? (:ok live-retry)))
      (is (= [event-id] @emitted))
      (is (identical? first-owner (get @settlers event-id))))

    (reset! draft-complete true)
    (await (first-owner {:event-turn/status :completed}))
    (is (empty? @settlers))
    (is (= :completed (get @dispatch-states event-id)))

    ;; Model an operator removing the completion marker/immutable files after
    ;; this process already recorded the deterministic dispatch as completed.
    (reset! draft-complete false)
    (let [repair (await (admission/admit-documents!
                         {} dependencies scope {:generate-drafts? true}))]
      (testing "the ownerless completed claim is released and dispatched again"
        (is (true? (:ok repair)))
        (is (= [event-id] @releases))
        (is (= [event-id event-id] @emitted))
        (is (= :existing (get-in repair [:results 0 :index/event-status])))
        (is (true? (get-in repair
                           [:results 0 :document/draft-generation-needed?])))))))

(deftest ^:async re-admission-redelivers-a-transiently-rejected-draft-settlement
  (agent-runner/reset-event-turn-queue!)
  (agent-runner/reset-event-turn-settlers!)
  (let [doc (document :knoxx.docs/redeliver-draft
                      "docs/redeliver-draft.md" true)
        resource-records (records [doc])
        persisted (atom {})
        checks (atom 0)
        emitted (atom [])
        releases (atom [])
        dependencies
        (merge (deps resource-records
                     {(:document/id doc) "# Redeliver draft"}
                     persisted (atom []) (atom []))
               (indexed-event-dedup-dependencies
                (atom #{}) emitted releases)
               {:draft-complete?
                (fn [_policy]
                  (if (= 2 (swap! checks inc))
                    (js/Promise.reject
                     (js/Error. "transient draft-store read failure"))
                    (js/Promise.resolve false)))
                :register-turn-settler!
                agent-runner/register-event-turn-settler!
                :unregister-turn-settler!
                agent-runner/unregister-event-turn-settler!})
        first-result (await (admission/admit-documents!
                             {} dependencies scope {:generate-drafts? true}))
        event-id (get-in first-result [:results 0 :index/event-id])
        run-id "draft-settlement-redelivery"]
    (agent-runner/enqueue-event-turn!
     {:llmModel "test-model" :collection-name "test"}
     {:run-id run-id
      :conversation-id run-id
      :session-id run-id
      :message "craft the draft"
      :agent-spec {:trigger-id "craft-post-from-indexed-document"
                   :event-id event-id}}
     (fn [] (js/Promise.resolve {:ok true})))
    (await (flush-promises!))
    (is (= 2 @checks)
        "the first terminal draft check rejected and remained cached")

    (let [retry-result (await (admission/admit-documents!
                               {} dependencies scope
                               {:generate-drafts? true}))]
      (testing "registration redelivers before the equal event is emitted"
        (is (= [event-id] @releases))
        (is (= [event-id event-id] @emitted)))
      (testing "the same admission pass retries against existing durable facts"
        (is (true? (:ok retry-result)))
        (is (= :existing (get-in retry-result
                                 [:results 0 :index/event-status])))))

    (testing "the redelivered owner is re-armed for the newly emitted turn"
      (agent-runner/enqueue-event-turn!
       {:llmModel "test-model" :collection-name "test"}
       {:run-id (str run-id "-retry")
        :conversation-id (str run-id "-retry")
        :session-id (str run-id "-retry")
        :message "craft the retried draft"
        :agent-spec {:trigger-id "craft-post-from-indexed-document"
                     :event-id event-id}}
       (fn [] (js/Promise.resolve {:ok true})))
      (await (flush-promises!))
      (is (= [event-id event-id] @releases)))
    (agent-runner/reset-event-turn-queue!)
    (agent-runner/reset-event-turn-settlers!)))

(deftest ^:async repeatedly-rejected-draft-settlement-fails-admission
  (agent-runner/reset-event-turn-queue!)
  (agent-runner/reset-event-turn-settlers!)
  (let [doc (document :knoxx.docs/repeated-draft-redelivery
                      "docs/repeated-draft-redelivery.md" true)
        resource-records (records [doc])
        checks (atom 0)
        emitted (atom [])
        releases (atom [])
        dependencies
        (merge (deps resource-records
                     {(:document/id doc) "# Repeated draft redelivery"}
                     (atom {}) (atom []) (atom []))
               (indexed-event-dedup-dependencies
                (atom #{}) emitted releases)
               {:draft-complete?
                (fn [_policy]
                  (if (contains? #{2 4} (swap! checks inc))
                    (js/Promise.reject
                     (js/Error. "persistent draft-store read failure"))
                    (js/Promise.resolve false)))
                :register-turn-settler!
                agent-runner/register-event-turn-settler!
                :unregister-turn-settler!
                agent-runner/unregister-event-turn-settler!})
        first-result (await (admission/admit-documents!
                             {} dependencies scope {:generate-drafts? true}))
        event-id (get-in first-result [:results 0 :index/event-id])
        run-id "draft-settlement-repeated-redelivery"]
    (agent-runner/enqueue-event-turn!
     {:llmModel "test-model" :collection-name "test"}
     {:run-id run-id
      :conversation-id run-id
      :session-id run-id
      :message "craft the draft"
      :agent-spec {:trigger-id "craft-post-from-indexed-document"
                   :event-id event-id}}
     (fn [] (js/Promise.resolve {:ok true})))
    (await (flush-promises!))

    (let [error (try
                  (await (admission/admit-documents!
                          {} dependencies scope {:generate-drafts? true}))
                  nil
                  (catch :default err err))]
      (testing "a second rejected callback cannot masquerade as a live owner"
        (is (= 503 (:status (ex-data error))))
        (is (= "document_post_draft_settlement_redelivery_failed"
               (:code (ex-data error))))
        (is (= [event-id] @emitted))
        (is (empty? @releases))
        (is (= :settled (agent-runner/event-turn-owner-state event-id)))))

    (let [retry-result (await (admission/admit-documents!
                               {} dependencies scope
                               {:generate-drafts? true}))]
      (testing "the retained settlement remains recoverable on a later pass"
        (is (true? (:ok retry-result)))
        (is (= [event-id] @releases))
        (is (= [event-id event-id] @emitted))))
    (agent-runner/reset-event-turn-queue!)
    (agent-runner/reset-event-turn-settlers!)))

(deftest ^:async default-openplanner-persistence-detects-a-replay
  (let [event {:schema "openplanner.event.v1"
               :id "knoxx-document-admission-real-adapter-replay"
               :ts "2026-09-02T12:00:00.000Z"
               :source "knoxx-publication"
               :kind "docs"
               :source_ref {:project "knoxx-local"
                            :message "knoxx.docs/replay"}
               :text "# Replay"}
        first-result (await (admission/persist-openplanner-event! {} event))
        retry-result (await (admission/persist-openplanner-event! {} event))]
    (is (true? (:ok first-result)))
    (is (not (:existing first-result)))
    (is (true? (:existing retry-result)))
    (is (= [(:id event)] (:ids retry-result)))))

(deftest ^:async default-openplanner-replay-repairs-a-missing-vector
  (sdk-mod/__setEventVectorMode "missing")
  (try
    (let [event {:schema "openplanner.event.v1"
                 :id "knoxx-document-admission-real-adapter-vector-repair"
                 :ts "2026-09-02T12:00:00.000Z"
                 :source "knoxx-publication"
                 :kind "docs"
                 :source_ref {:project "knoxx-local"
                              :message "knoxx.docs/vector-repair"}
                 :text "# Repair this durable document event"}
          _ (await (xsdk/events! [event]))
          result (await (admission/persist-openplanner-event! {} event))
          stored (await (xsdk/mongo-query
                         {:collection "events"
                          :filter {:id (:id event)}}))]
      (is (true? (:existing result)))
      (is (= [(:id event)]
             (get-in result [:index-result :repaired-event-ids])))
      (is (= 1 (get-in result [:index-result :vector-count])))
      (is (= 1 (:total stored))
          "repair must not append a second immutable base event"))
    (finally
      (sdk-mod/__setEventVectorMode "valid"))))

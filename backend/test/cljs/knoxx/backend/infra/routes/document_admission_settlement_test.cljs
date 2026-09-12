(ns knoxx.backend.infra.routes.document-admission-settlement-test
  (:require [cljs.test :as test]
            [knoxx.backend.extern.event-queue-fixture :as queue-fixture]
            [knoxx.backend.infra.agent.runner :as agent-runner]
            [knoxx.backend.infra.routes.document-admission :as admission]
            [knoxx.backend.infra.routes.document-admission-fixture :as fixture]))

(defn- draft-owner-dependencies
  [doc draft-complete settlers]
  {:draft-complete?
   (fn [policy]
     (test/is (= (:document/id doc) (:source-document-id policy)))
     (test/is (string? (:source-revision policy)))
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
  (merge (fixture/deps resource-records
               {(:document/id doc) "# Retry draft"}
               persisted (atom []) (atom []))
         (draft-owner-dependencies doc draft-complete settlers)
         (indexed-event-dedup-dependencies
          (atom #{}) emitted releases)))

(defn- ^:async exercise-absent-draft-retry!
  [settlement]
  (let [doc (fixture/document :knoxx.docs/retry-draft "docs/retry-draft.md" true)
        resource-records (fixture/records [doc])
        persisted (atom {})
        draft-complete (atom false)
        emitted (atom [])
        releases (atom [])
        settlers (atom {})
        dependencies (draft-retry-dependencies
                      doc resource-records persisted draft-complete emitted
                      releases settlers)
        first-result (await (admission/admit-documents!
                             {} dependencies fixture/scope {:generate-drafts? true}))
        event-id (get-in first-result [:results 0 :index/event-id])
        settle! (get @settlers event-id)]
    (test/is (fn? settle!))
    (await (settle! settlement))
    (let [retry-result (await (admission/admit-documents!
                               {} dependencies fixture/scope {:generate-drafts? true}))]
      {:event-id event-id
       :first-result first-result
       :retry-result retry-result
       :persisted persisted
       :emitted emitted
       :releases releases})))

(test/deftest ^:async a-rejected-draft-provider-turn-is-retriable-in-process
  (let [{:keys [event-id first-result retry-result persisted emitted releases]}
        (await (exercise-absent-draft-retry!
                {:event-turn/status :failed
                 :event-turn/detail "provider unavailable"}))]
    (test/testing "the failed terminal owner releases only its indexed event"
      (test/is (= [event-id] @releases))
      (test/is (= [event-id event-id] @emitted)))
    (test/testing "re-admission reuses durable facts and enqueues a fresh attempt"
      (test/is (true? (:ok first-result)))
      (test/is (true? (:ok retry-result)))
      (test/is (= 2 (count @persisted)))
      (test/is (= :existing (get-in retry-result
                               [:results 0 :index/event-status]))))))

(test/deftest ^:async a-draft-turn-that-never-calls-its-tool-is-retriable-in-process
  (let [{:keys [event-id retry-result emitted releases]}
        (await (exercise-absent-draft-retry!
                {:event-turn/status :completed}))]
    (test/testing "successful provider completion is not mistaken for a saved draft"
      (test/is (= [event-id] @releases))
      (test/is (= [event-id event-id] @emitted))
      (test/is (true? (:ok retry-result))))))

(test/deftest ^:async an-explicitly-reset-draft-reclaims-its-dispatch-in-process
  (let [doc (fixture/document :knoxx.docs/vanished-draft
                      "docs/vanished-draft.md" true)
        resource-records (fixture/records [doc])
        persisted (atom {})
        draft-complete (atom false)
        dispatch-states (atom {})
        settlers (atom {})
        emitted (atom [])
        releases (atom [])
        dependencies
        (merge
         (fixture/deps resource-records
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
                             {} dependencies fixture/scope {:generate-drafts? true}))
        event-id (get-in first-result [:results 0 :index/event-id])
        first-owner (get @settlers event-id)
        live-retry (await (admission/admit-documents!
                           {} dependencies fixture/scope {:generate-drafts? true}))]
    (test/testing "a currently owned generation is not duplicated"
      (test/is (true? (:ok live-retry)))
      (test/is (= [event-id] @emitted))
      (test/is (identical? first-owner (get @settlers event-id))))

    (reset! draft-complete true)
    (await (first-owner {:event-turn/status :completed}))
    (test/is (empty? @settlers))
    (test/is (= :completed (get @dispatch-states event-id)))

    ;; Model an operator explicitly removing the completion marker before its
    ;; immutable files. Unlike a surviving marker with missing files, removing
    ;; the marker deliberately reopens generation.
    (reset! draft-complete false)
    (let [repair (await (admission/admit-documents!
                         {} dependencies fixture/scope {:generate-drafts? true}))]
      (test/testing "the ownerless completed claim is released and dispatched again"
        (test/is (true? (:ok repair)))
        (test/is (= [event-id] @releases))
        (test/is (= [event-id event-id] @emitted))
        (test/is (= :existing (get-in repair [:results 0 :index/event-status])))
        (test/is (true? (get-in repair
                           [:results 0 :document/draft-generation-needed?])))))))

(test/deftest ^:async re-admission-redelivers-a-transiently-rejected-draft-settlement
  (await (queue-fixture/with-queue!
          (^:async fn []
  (agent-runner/reset-event-turn-queue!)
  (agent-runner/reset-event-turn-settlers!)
  (let [doc (fixture/document :knoxx.docs/redeliver-draft
                      "docs/redeliver-draft.md" true)
        resource-records (fixture/records [doc])
        persisted (atom {})
        checks (atom 0)
        emitted (atom [])
        releases (atom [])
        dependencies
        (merge (fixture/deps resource-records
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
                             {} dependencies fixture/scope {:generate-drafts? true}))
        event-id (get-in first-result [:results 0 :index/event-id])
        run-id "draft-settlement-redelivery"]
    (await (agent-runner/enqueue-event-turn!
     {:llmModel "test-model" :collection-name "test"}
     {:run-id run-id
      :conversation-id run-id
      :session-id run-id
      :message "craft the draft"
      :agent-spec {:trigger-id "craft-post-from-indexed-document"
                   :event-id event-id}}
     (fn [] (js/Promise.resolve {:ok true}))))
    (await (queue-fixture/wait-idle!))
    (test/is (= 2 @checks)
        "the first terminal draft check rejected and remained cached")
    (test/is (= [event-id] @releases)
        "a rejected completion read immediately releases the event claim")

    (let [retry-result (await (admission/admit-documents!
                               {} dependencies fixture/scope
                               {:generate-drafts? true}))]
      (test/testing "registration redelivers before the equal event is emitted"
        (test/is (= [event-id event-id] @releases))
        (test/is (= [event-id event-id] @emitted)))
      (test/testing "the same admission pass retries against existing durable facts"
        (test/is (true? (:ok retry-result)))
        (test/is (= :existing (get-in retry-result
                                 [:results 0 :index/event-status])))))

    (test/testing "the redelivered owner is re-armed for the newly emitted turn"
      (await (agent-runner/enqueue-event-turn!
       {:llmModel "test-model" :collection-name "test"}
       {:run-id (str run-id "-retry")
        :conversation-id (str run-id "-retry")
        :session-id (str run-id "-retry")
        :message "craft the retried draft"
        :agent-spec {:trigger-id "craft-post-from-indexed-document"
                     :event-id event-id}}
       (fn [] (js/Promise.resolve {:ok true}))))
      (await (queue-fixture/wait-idle!))
      (test/is (= [event-id event-id event-id] @releases)))
    (agent-runner/reset-event-turn-queue!)
    (agent-runner/reset-event-turn-settlers!))))))

(test/deftest ^:async repeatedly-rejected-draft-settlement-fails-admission
  (await (queue-fixture/with-queue!
          (^:async fn []
  (agent-runner/reset-event-turn-queue!)
  (agent-runner/reset-event-turn-settlers!)
  (let [doc (fixture/document :knoxx.docs/repeated-draft-redelivery
                      "docs/repeated-draft-redelivery.md" true)
        resource-records (fixture/records [doc])
        checks (atom 0)
        emitted (atom [])
        releases (atom [])
        dependencies
        (merge (fixture/deps resource-records
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
                             {} dependencies fixture/scope {:generate-drafts? true}))
        event-id (get-in first-result [:results 0 :index/event-id])
        run-id "draft-settlement-repeated-redelivery"]
    (await (agent-runner/enqueue-event-turn!
     {:llmModel "test-model" :collection-name "test"}
     {:run-id run-id
      :conversation-id run-id
      :session-id run-id
      :message "craft the draft"
      :agent-spec {:trigger-id "craft-post-from-indexed-document"
                   :event-id event-id}}
     (fn [] (js/Promise.resolve {:ok true}))))
    (await (queue-fixture/wait-idle!))
    (test/is (= [event-id] @releases)
        "the first rejected completion read releases its exact event")

    (let [error (try
                  (await (admission/admit-documents!
                          {} dependencies fixture/scope {:generate-drafts? true}))
                  nil
                  (catch :default err err))]
      (test/testing "a second rejected callback cannot masquerade as a live owner"
        (test/is (= 503 (:status (ex-data error))))
        (test/is (= "document_post_draft_settlement_redelivery_failed"
               (:code (ex-data error))))
        (test/is (= [event-id] @emitted))
        (test/is (= [event-id event-id] @releases))
        (test/is (= :settled (agent-runner/event-turn-owner-state event-id)))))

    (let [retry-result (await (admission/admit-documents!
                               {} dependencies fixture/scope
                               {:generate-drafts? true}))]
      (test/testing "the retained settlement remains recoverable on a later pass"
        (test/is (true? (:ok retry-result)))
        (test/is (= [event-id event-id event-id] @releases))
        (test/is (= [event-id event-id] @emitted))))
    (agent-runner/reset-event-turn-queue!)
    (agent-runner/reset-event-turn-settlers!))))))

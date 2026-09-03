(ns knoxx.backend.infra.translation-event-writer-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.openplanner-sdk :as openplanner]
            [knoxx.backend.infra.translation-evidence-store :as evidence-store]
            [knoxx.backend.infra.translation-event-writer :as writer]
            [knoxx.backend.infra.translation-split-store :as split-store]
            [knoxx.backend.law.translation-event :as event]))

(def ^:private durable-extra
  {:source_lang "en"
   :target_lang "fr"
   :source_text "source"
   :mt_model "gemma4:e2b"
   :status "in_review"})

(defn- deferred
  []
  (let [resolve* (atom nil)
        promise (js/Promise. (fn [resolve _reject]
                               (reset! resolve* resolve)))]
    {:promise promise
     :resolve! (fn [] (@resolve* nil))}))

(defn- ^:async append-after!
  [gate rows events]
  (await gate)
  (swap! rows into events)
  {:ok true :ids (mapv :id events)})

(deftest ^:async writer-appends-the-pure-stable-event-vector
  (let [events [{:id "translation-segment-a" :extra durable-extra}
                {:id "translation-segment-b" :extra durable-extra}]
        appended (atom nil)
        ensured (atom [])
        verified (atom nil)
        queries (atom [])]
    (with-redefs [event/candidate-events (fn [_digest _receipt _turn _candidate-set]
                                          events)
                  openplanner/mongo-query (fn [query]
                                            (swap! queries conj query)
                                            (js/Promise.resolve
                                             {:ok true
                                              :total 1
                                              :rows [{:id "translation-segment-a"}]}))
                  ;; Match the production var's multi-arity shape. CLJS emits
                  ;; direct arity dispatch for that var even while it is
                  ;; temporarily rebound by with-redefs.
                  openplanner/events! (fn
                                        ([actual]
                                         (reset! appended actual)
                                         (js/Promise.resolve
                                          {:ok true :ids (mapv :id actual)}))
                                        ([actual _opts]
                                         (reset! appended actual)
                                         (js/Promise.resolve
                                          {:ok true :ids (mapv :id actual)})))
                  openplanner/ensure-event-vectors!
                  (fn [ids]
                    (reset! verified ids)
                    (js/Promise.resolve {:ok true :event-ids ids}))
                  openplanner/ensure-event-extra-fields!
                  (fn [id required]
                    (swap! ensured conj [id required])
                    (js/Promise.resolve {:ok true :event-id id}))]
      (let [result (await (writer/emit-candidate-events!
                           {:receipt :receipt
                            :turn :turn
                            :candidate-set :candidate-set}))]
        (is (= [(second events)] @appended))
        (is (= ["translation-segment-a"]
               (:translation/event-existing-ids result)))
        (is (= ["translation-segment-b"]
               (:translation/event-recorded-ids result)))
        (is (= {:collection "events"
                :filter {:id {:$in ["translation-segment-a"
                                    "translation-segment-b"]}}
                :projection {:id 1}
                :limit 2}
               (first @queries)))
        (is (= ["translation-segment-a" "translation-segment-b"]
               (:translation/event-ids result)))
        (is (= ["translation-segment-a" "translation-segment-b"] @verified))
        (is (= [["translation-segment-a" durable-extra]
                ["translation-segment-b" durable-extra]]
               @ensured))
        (is (= true (get-in result [:translation/event-result :ok])))))))

(deftest ^:async writer-does-not-append-an-equal-durable-replay
  (let [events [{:id "translation-segment-a" :extra durable-extra}
                {:id "translation-segment-b" :extra durable-extra}]
        append-count (atom 0)]
    (with-redefs [event/candidate-events (fn [_digest _receipt _turn _candidate-set]
                                          events)
                  openplanner/mongo-query (fn [_]
                                            (js/Promise.resolve
                                             {:ok true :total 2 :rows events}))
                  openplanner/events! (fn
                                        ([_]
                                         (swap! append-count inc)
                                         (js/Promise.resolve {:ok true}))
                                        ([_ _]
                                         (swap! append-count inc)
                                         (js/Promise.resolve {:ok true})))
                  openplanner/ensure-event-vectors!
                  (fn [ids]
                    (js/Promise.resolve {:ok true :event-ids ids}))
                  openplanner/ensure-event-extra-fields!
                  (fn [id _required]
                    (js/Promise.resolve {:ok true :event-id id}))]
      (let [result (await (writer/emit-candidate-events!
                           {:receipt :receipt
                            :turn :turn
                            :candidate-set :candidate-set}))]
        (is (zero? @append-count))
        (is (empty? (:translation/event-recorded-ids result)))
        (is (= true (get-in result [:translation/event-result :existing])))))))

(deftest ^:async failed-writer-does-not-poison-the-serialization-tail
  (let [events [{:id "translation-segment-a" :extra durable-extra}]
        failure (js/Error. "durable query failed")
        query-count (atom 0)
        append-count (atom 0)]
    (with-redefs [event/candidate-events (fn [_digest _receipt _turn _candidate-set]
                                          events)
                  openplanner/mongo-query
                  (fn [_]
                    (if (= 1 (swap! query-count inc))
                      (js/Promise.reject failure)
                      (js/Promise.resolve {:ok true :total 0 :rows []})))
                  openplanner/events! (fn
                                        ([actual]
                                         (swap! append-count inc)
                                         (js/Promise.resolve
                                          {:ok true :ids (mapv :id actual)}))
                                        ([actual _opts]
                                         (swap! append-count inc)
                                         (js/Promise.resolve
                                          {:ok true :ids (mapv :id actual)})))
                  openplanner/ensure-event-vectors!
                  (fn [ids]
                    (js/Promise.resolve {:ok true :event-ids ids}))
                  openplanner/ensure-event-extra-fields!
                  (fn [id _required]
                    (js/Promise.resolve {:ok true :event-id id}))]
      (let [completion {:receipt :receipt
                        :turn :turn
                        :candidate-set :candidate-set}
            first-result (writer/emit-candidate-events! completion)
            second-result (writer/emit-candidate-events! completion)
            first-error (try
                          (await first-result)
                          nil
                          (catch :default err
                            err))
            second-completion (await second-result)]
        (is (identical? failure first-error))
        (is (= 2 @query-count))
        (is (= 1 @append-count))
        (is (= ["translation-segment-a"]
               (:translation/event-recorded-ids second-completion)))))))

(deftest ^:async concurrent-writers-cannot-append-the-same-stable-events
  (let [events [{:id "translation-segment-a" :extra durable-extra}
                {:id "translation-segment-b" :extra durable-extra}]
        rows (atom [])
        query-count (atom 0)
        append-count (atom 0)
        first-append-started (deferred)
        release-first-append (deferred)
        append! (fn [actual]
                  (let [attempt (swap! append-count inc)]
                    (when (= 1 attempt)
                      ((:resolve! first-append-started)))
                    (append-after!
                     (if (= 1 attempt)
                       (:promise release-first-append)
                       (js/Promise.resolve nil))
                     rows
                     actual)))]
    (with-redefs [event/candidate-events (fn [_digest _receipt _turn _candidate-set]
                                          events)
                  openplanner/mongo-query
                  (fn [_]
                    (swap! query-count inc)
                    (js/Promise.resolve
                     {:ok true
                      :total (count @rows)
                      :rows (mapv #(select-keys % [:id]) @rows)}))
                  openplanner/events! (fn
                                        ([actual]
                                         (append! actual))
                                        ([actual _opts]
                                         (append! actual)))
                  openplanner/ensure-event-vectors!
                  (fn [ids]
                    (js/Promise.resolve {:ok true :event-ids ids}))
                  openplanner/ensure-event-extra-fields!
                  (fn [id _required]
                    (js/Promise.resolve {:ok true :event-id id}))]
      (let [completion {:receipt :receipt
                        :turn :turn
                        :candidate-set :candidate-set}
            first-result (writer/emit-candidate-events! completion)
            second-result (writer/emit-candidate-events! completion)]
        (await (:promise first-append-started))
        (is (= 1 @query-count)
            "the second writer waits before reading durable state")
        (is (= 1 @append-count)
            "only the first append reaches the forced overlap gate")
        ((:resolve! release-first-append))
        (let [results (await (js/Promise.all #js [first-result second-result]))
              first-completion (aget results 0)
              second-completion (aget results 1)
              ids (mapv :id @rows)]
          (is (= ["translation-segment-a" "translation-segment-b"] ids))
          (is (= {"translation-segment-a" 1 "translation-segment-b" 1}
                 (frequencies ids)))
          (is (= 1 @append-count))
          (is (= ["translation-segment-a" "translation-segment-b"]
                 (:translation/event-recorded-ids first-completion)))
          (is (empty?
               (:translation/event-recorded-ids second-completion)))
          (is (= ["translation-segment-a" "translation-segment-b"]
                 (:translation/event-existing-ids second-completion))))))))

(deftest ^:async completed-receipts-can-repair-events-without-agent-tool-replay
  (let [scope {:org-id "open-hax" :project "knoxx"}
        legacy-receipt {:translation/revision "legacy-revision"}
        receipt {:translation/candidate-set-id "candidate-set-1"
                 :translation/revision "candidate-revision-1"}
        candidate-set {:candidate-set/id "candidate-set-1"}
        turn {:translation-turn/id "turn-1"}
        projected (atom [])]
    (with-redefs [evidence-store/completed-translations!
                  (fn [store actual-scope]
                    (is (= :evidence store))
                    (is (= scope actual-scope))
                    (js/Promise.resolve [legacy-receipt receipt]))
                  split-store/candidate-set-by-id!
                  (fn [store candidate-set-id]
                    (is (= :splits store))
                    (is (= "candidate-set-1" candidate-set-id))
                    (js/Promise.resolve candidate-set))
                  split-store/turn-for-candidate-set!
                  (fn [store candidate-set-id]
                    (is (= :splits store))
                    (is (= "candidate-set-1" candidate-set-id))
                    (js/Promise.resolve turn))
                  writer/emit-candidate-events!
                  (fn [completion]
                    (swap! projected conj completion)
                    (js/Promise.resolve
                     {:translation/event-ids ["translation-segment-1"]
                      :translation/event-existing-ids []
                      :translation/event-recorded-ids
                      ["translation-segment-1"]}))]
      (let [result (await
                    (writer/repair-completed-event-projections!
                     {:evidence-store :evidence :split-store :splits}
                     scope))]
        (is (= [{:receipt receipt
                 :turn turn
                 :candidate-set candidate-set}]
               @projected))
        (is (= 1 (:translation/event-repair-receipt-count result)))
        (is (= 1 (:translation/event-repair-skipped-count result)))
        (is (= 1 (:translation/event-repair-recorded-count result)))
        (is (zero? (:translation/event-repair-existing-count result)))))))

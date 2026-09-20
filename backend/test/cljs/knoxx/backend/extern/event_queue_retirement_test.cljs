(ns knoxx.backend.extern.event-queue-retirement-test
  "An abandoned run must release its queue state without losing an unobserved failure."
  (:require [cljs.test :as test]
            [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.extern.run-event-queue :as queue]
            [knoxx.backend.infra.agent.turn-finalization :as finalization]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.shape.session-persistence :as persistence]))

(defn- deferred []
  (let [settle* (atom nil)
        promise (js/Promise. (fn [complete reject]
                               (reset! settle* {:complete! complete :reject! reject})))]
    (assoc @settle* :promise promise)))

(defn- tick! [] (js/Promise. (fn [complete _reject] (js/setImmediate complete))))

(defn- ^:async outcome!
  "Observe a settled admission without letting a refusal escape the assertion."
  [operation]
  (try {:value (await (operation))} (catch :default error {:error error})))

(defn- event [id sequence] {:run_id id :sequence sequence})

(test/deftest ^:async a-failed-run-retains-its-failure-until-retirement
  (await
   ((^:async fn []
      (let [{:keys [submit! flush! retire!]}
            (queue/create (fn [_event] (js/Promise.reject (ex-info "Event store unavailable" {}))))]
        (submit! (event "abandoned" 1))
        (test/is (= "Event store unavailable"
                    (ex-message (:error (await (outcome! #(flush! "abandoned"))))))
                 "the owner observes the durable failure")
        (test/is (= "Event store unavailable"
                    (ex-message (:error (await (outcome! #(flush! "abandoned"))))))
                 "a repeated flush still reports the unretired failure")
        (retire! "abandoned")
        (test/is (true? (:value (await (outcome! #(flush! "abandoned")))))
                 "retirement releases the abandoned run's tail and failure"))))))

(test/deftest ^:async retirement-fences-a-late-failure-from-its-successor
  (await
   ((^:async fn []
      (let [abandoned (deferred)
            writes* (atom [])
            {:keys [submit! flush! retire!]}
            (queue/create (fn [event]
                            (swap! writes* conj (:sequence event))
                            (if (= 1 (:sequence event))
                              (:promise abandoned)
                              (js/Promise.resolve true))))]
        (submit! (event "reused" 1))
        (retire! "reused")
        ((:reject! abandoned) (ex-info "Outage during abandonment" {}))
        (await (tick!))
        (await (tick!))
        (submit! (event "reused" 2))
        (test/is (true? (:value (await (outcome! #(flush! "reused")))))
                 "a retired generation's late failure cannot reach the successor")
        (test/is (= [1 2] @writes*) "the successor's own write still runs"))))))

(test/deftest ^:async a-retired-incarnations-pending-write-still-precedes-its-successor
  (await
   ((^:async fn []
      (let [abandoned (deferred)
            writes* (atom [])
            {:keys [submit! flush! retire!]}
            (queue/create (fn [event]
                            (swap! writes* conj (:sequence event))
                            (if (= 1 (:sequence event))
                              (:promise abandoned)
                              (js/Promise.resolve true))))]
        (submit! (event "reused" 1))
        (retire! "reused")
        (submit! (event "reused" 2))
        (await (tick!))
        (await (tick!))
        (test/is (= [1] @writes*)
                 "the successor cannot write while the retired incarnation is unsettled")
        ((:complete! abandoned) true)
        (test/is (true? (:value (await (outcome! #(flush! "reused")))))
                 "the successor settles once the abandoned write lands")
        (test/is (= [1 2] @writes*)
                 "the retired incarnation's write precedes the successor's"))))))

(test/deftest ^:async retiring-an-unknown-run-is-a-safe-release
  (await
   ((^:async fn []
      (let [{:keys [flush! retire!]} (queue/create (fn [_event] (js/Promise.resolve true)))]
        (retire! "never-admitted")
        (test/is (true? (:value (await (outcome! #(flush! "never-admitted")))))
                 "releasing a run that admitted no event leaves the queue usable"))))))

(defrecord RefusingEventStore []
  persistence/ISessionStore
  (put-run! [_ _run] (js/Promise.resolve true))
  persistence/IRunEventStore
  (append-event! [_ _event]
    (js/Promise.reject (ex-info "Event collections unavailable"
                                {:code "run_event_store_unavailable"})))
  (events-since [_ _run-id _since] (js/Promise.resolve [])))

(test/deftest ^:async an-evicted-run-still-observes-its-terminal-event-refusal
  (await
   ((^:async fn []
      (try
        ;; The heap run is absent, exactly as after MAX_RUNS eviction, so the
        ;; callers' `(when completed-run ...)` memory-indexing flush is skipped
        ;; and settlement is the only place left to observe this write.
        (events/install! (->RefusingEventStore))
        (state/append-run-event! "evicted-terminal"
                                 {:run_id "evicted-terminal" :type "run_completed"})
        (test/is (nil? (get @state/runs* "evicted-terminal"))
                 "the run is absent from the bounded heap, as after eviction")
        (let [{:keys [error]} (await (outcome!
                                      #(finalization/settle!
                                        {:run-id "evicted-terminal" :conversation-id "evicted-terminal"}
                                        (fn [] (js/Promise.resolve true))
                                        (fn [] (js/Promise.resolve true)))))]
          (test/is (some? error) "a refused terminal write must not settle as success")
          (test/is (= "run_event_store_unavailable" (:code (ex-data error)))
                   "the original durable refusal reaches the owner"))
        (finally (events/install! nil)))))))

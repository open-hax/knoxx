(ns knoxx.backend.extern.event-queue-retirement-test
  "An abandoned run must release its queue state without losing an unobserved failure."
  (:require [cljs.test :as test]
            [knoxx.backend.extern.run-event-queue :as queue]))

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

(test/deftest ^:async retiring-an-unknown-run-is-a-safe-release
  (await
   ((^:async fn []
      (let [{:keys [flush! retire!]} (queue/create (fn [_event] (js/Promise.resolve true)))]
        (retire! "never-admitted")
        (test/is (true? (:value (await (outcome! #(flush! "never-admitted")))))
                 "releasing a run that admitted no event leaves the queue usable"))))))

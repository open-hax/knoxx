(ns knoxx.backend.extern.event-queue-fixture
  "Real Clio persistence and finite completion waits for event FIFO regressions."
  (:require [knoxx.backend.extern.agent-turn-fixture :as fixture]
            [knoxx.backend.infra.agent.runner :as runner]
            [knoxx.backend.infra.run-events :as events]))

(defn ^:async wait-until!
  "Wait for the asserted state, refusing an unbounded or silently unfinished test."
  [ready?]
  (let [deadline (+ (.now js/Date) 5000)]
    (loop []
      (when-not (ready?)
        (when (> (.now js/Date) deadline)
          (throw (ex-info "Event queue fixture did not settle" {})))
        (await (js/Promise. (fn [complete _reject] (js/setImmediate complete))))
        (recur)))))

(defn ^:async wait-idle!
  "Wait until all admitted turns and their settlement callbacks have completed."
  []
  (await (wait-until! #(let [{:keys [active queued]} (runner/event-turn-queue-snapshot)]
                        (and (zero? active) (zero? queued))))))

(defn ^:async collect!
  "Join native admission promises while preserving the requested FIFO order."
  [admissions]
  (vec (array-seq (await (js/Promise.all (to-array admissions))))))

(defn ^:async with-queue!
  "Install real isolated run providers, flush admitted events, and restore state."
  [verify!]
  (await
   (fixture/with-run!
    {:run_id "queue-fixture-seed" :session_id "queue-fixture-seed"
     :conversation_id "queue-fixture-seed"}
    (^:async fn []
      (let [persist! events/persist-run! ids* (atom #{})]
        (runner/reset-event-turn-queue!)
        (runner/reset-event-turn-settlers!)
        (with-redefs [events/persist-run! (^:async fn [run]
                                          (swap! ids* conj (:run_id run))
                                          (await (persist! run)))]
          (try
            (await (verify!))
            (await (wait-idle!))
            (doseq [id @ids*] (await (events/flush! id)))
            (finally
              (runner/reset-event-turn-queue!)
              (runner/reset-event-turn-settlers!)))))))))

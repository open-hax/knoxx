(ns knoxx.backend.extern.event-queue-retention-test
  "FIFO ownership outlives diagnostic retention, with real durable terminal snapshots."
  (:require [cljs.test :as test]
            [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.extern.agent-runner :as host]
            [knoxx.backend.extern.event-queue-fixture :as fixture]
            [knoxx.backend.infra.agent.runner :as runner]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.shape.session-persistence :as runs]))

(defn- body [id]
  {:run-id id :session-id (str id "-session") :conversation-id (str id "-conversation")
   :message "Retain until settled." :model "finite-fixture"
   :agent-spec {:trigger-id "retention" :event-id (str id "-event")}})

(defn- deferred []
  (let [complete* (atom nil)
        promise (js/Promise. (fn [complete _reject] (reset! complete* complete)))]
    {:promise promise :complete! (fn [] (@complete* nil))}))

(defn- ^:async complete-run! [id]
  (state/update-run! id #(assoc % :status "completed" :answer "Completed."))
  (await (events/persist-run! (get @state/runs* id)))
  {:answer "Completed."})

(def ^:private config {:event-agent-concurrency 1 :event-agent-queue-limit 2})

(defn- ^:async submit-owned-turn! [id first-id failed-id gate observed*]
  (await (runner/register-event-turn-settler!
          (str id "-event")
          (^:async fn [settlement]
            (let [persisted (await (runs/get-run @registry/session-store* id))]
              (swap! observed* assoc id [(:event-turn/status settlement) (:status persisted)]))
            true)))
  (await (runner/enqueue-event-turn!
          config (body id)
          (^:async fn []
            (when (= first-id id) (await (:promise gate)))
            (when (= failed-id id) (throw (ex-info "Expected startup refusal" {})))
            (await (complete-run! id))))))

(test/deftest ^:async diagnostic-churn-keeps-every-owner-through-terminal-persistence
  (await
   (fixture/with-queue!
    (^:async fn []
      (let [gate (deferred) observed* (atom {}) reported* (atom [])
            ids ["active-success" "pending-failure" "pending-success"]
            first-id (first ids) failed-id (second ids)]
        (with-redefs [host/log-async-spawn-error! (fn [_body error] (swap! reported* conj (ex-message error)))]
          (try
            (doseq [id ids] (await (submit-owned-turn! id first-id failed-id gate observed*)))
            (test/is (= [1 2] ((juxt :active :queued) (runner/event-turn-queue-snapshot))))
            (doseq [index (range state/MAX_RUNS)]
              (state/store-run! (str "diagnostic-" index) {:run_id (str "diagnostic-" index)}))
            (test/is (= state/MAX_RUNS (count @state/run-order*)) "diagnostic order stays bounded")
            (test/is (every? #(some? (get @state/runs* %)) ids) "active and pending owners survive diagnostic eviction")
            (let [rejected (try (await (runner/enqueue-event-turn! config (body "capacity-rejected") (fn [] nil)))
                                nil (catch :default error error))]
              (test/is (some? rejected) "retention does not expand queue admission"))
            ((:complete! gate))
            (await (fixture/wait-idle!))
            (test/is (= (set ids) (set (keys @observed*))) "each admitted owner receives its committed terminal result")
            (test/is (= [:failed "failed"] (get @observed* failed-id)))
            (test/is (every? #(= [:completed "completed"] (get @observed* %)) (remove #{failed-id} ids)))
            (test/is (= 2 (count @reported*)) "only the expected startup refusal and full-queue rejection are logged")
            (test/is (= state/MAX_RUNS (count @state/runs*)) "finished and rejected owners leave no retention pin")
            (test/is (= (set @state/run-order*) (set (keys @state/runs*))))
            (finally
              ((:complete! gate))
              (await (fixture/wait-idle!))))))))))

(test/deftest ^:async failed-admission-releases-retention-after-an-event-write-refusal
  (await
   (fixture/with-queue!
    (^:async fn []
      (let [failed-id "retention-admission-refused" flush! events/flush!]
        (with-redefs [events/flush! (^:async fn [id]
                                    (await (flush! id))
                                    (when (= failed-id id) (throw (ex-info "Expected admission refusal" {}))))]
          (let [error (try (await (runner/enqueue-event-turn! {} (body failed-id) (fn [] nil)))
                           nil (catch :default error error))]
            (test/is (= "Expected admission refusal" (ex-message error)))))
        (test/is (zero? (:active (runner/event-turn-queue-snapshot))))
        (test/is (zero? (:queued (runner/event-turn-queue-snapshot))))
        (doseq [index (range state/MAX_RUNS)]
          (state/store-run! (str "post-refusal-" index) {:run_id (str "post-refusal-" index)}))
        (test/is (nil? (get @state/runs* failed-id)) "failed durable admission must not pin an abandoned run")
        (test/is (= state/MAX_RUNS (count @state/runs*))))))))

(test/deftest ^:async queue-reset-preserves-an-executing-owner-until-it-settles
  (await
   (fixture/with-queue!
    (^:async fn []
      (let [id "active-across-reset" gate (deferred) observed* (atom {})]
        (try
          (await (submit-owned-turn! id id nil gate observed*))
          (await (fixture/wait-until! #(= "running" (get-in @state/runs* [id :status]))))
          (doseq [index (range state/MAX_RUNS)]
            (state/store-run! (str "reset-diagnostic-" index) {:run_id (str "reset-diagnostic-" index)}))
          (runner/reset-event-turn-queue!)
          (test/is (some? (get @state/runs* id)) "resetting bookkeeping cannot release an executing owner's record")
          ((:complete! gate))
          (await (fixture/wait-until! #(contains? @observed* id)))
          (await (fixture/wait-until! #(not (contains? @state/runs* id))))
          (test/is (= [:completed "completed"] (get @observed* id)))
          (test/is (= state/MAX_RUNS (count @state/runs*)))
          (finally ((:complete! gate)))))))))

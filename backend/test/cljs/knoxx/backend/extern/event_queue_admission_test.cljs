(ns knoxx.backend.extern.event-queue-admission-test
  "Run admission must precede every queue event in the actual Clio provider."
  (:require [cljs.test :as test]
            [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.extern.agent-turn-fixture :as fixture]
            [knoxx.backend.infra.agent.runner :as runner]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.shape.session-persistence :as runs]))

(def ^:private config {:event-agent-concurrency 1 :event-agent-queue-limit 1})

(defn- body [id]
  {:run-id id :session-id (str id "-session") :conversation-id (str id "-conversation")
   :message "Translate." :model "finite-fixture"
   :auth-context {:org-id "verified-org" :membership-id "verified-member" :project "verified-project"}
   :agent-spec {:trigger-id "translation" :event-id (str id "-event")}})

(defn- deferred []
  (let [complete* (atom nil)
        promise (js/Promise. (fn [complete _reject] (reset! complete* complete)))]
    {:promise promise :complete! (fn [value] (@complete* value))}))

(defn- tick! [] (js/Promise. (fn [complete _reject] (js/setImmediate complete))))

(defn- ^:async wait-until! [ready?]
  (let [deadline (+ (.now js/Date) 2000)]
    (loop []
      (when-not (ready?)
        (when (> (.now js/Date) deadline) (throw (ex-info "Queue fixture timed out" {})))
        (await (tick!))
        (recur)))))

(defn- ^:async outcome! [operation]
  (try {:value (await (operation))} (catch :default error {:error error})))

(defn- ^:async with-queue! [verify!]
  (await (fixture/with-run!
          {:run_id "queue-seed" :session_id "queue-seed" :conversation_id "queue-seed"}
          (^:async fn []
            (runner/reset-event-turn-queue!)
            (runner/reset-event-turn-settlers!)
            (try (await (verify! @registry/session-store*))
                 (finally (runner/reset-event-turn-queue!) (runner/reset-event-turn-settlers!)))))))

(test/deftest ^:async durable-run-precedes-queued-and-started-events
  (await
   (with-queue!
    (^:async fn [provider]
      (let [id "admission-order" observed* (atom nil)]
        (await (runner/enqueue-event-turn! config (body id)
                 (^:async fn []
                   (reset! observed* {:run (await (runs/get-run provider id))
                                      :flush (await (outcome! #(events/flush! id)))}))))
        (await (wait-until! #(some? @observed*)))
        (test/is (some? (:run @observed*)) "the provider run exists before model execution")
        (test/is (nil? (get-in @observed* [:flush :error])) "first durable event cannot precede admission")
        (test/is (= "verified-org" (get-in @observed* [:run :org_id])))
        (test/is (= "verified-member" (get-in @observed* [:run :membership_id])))
        (test/is (= ["event_turn_queued" "event_turn_started"]
                    (mapv :type (await (runs/events-since provider id nil))))))))))

(test/deftest ^:async pending-entry-cannot-start-before-its-own-durable-admission
  (await
   (with-queue!
    (^:async fn [_provider]
      (let [admission (deferred) first-turn (deferred) started* (atom [])
            persist! events/persist-run!]
        (with-redefs [events/persist-run!
                      (^:async fn [run]
                        (when (= "waiting-second" (:run_id run)) (await (:promise admission)))
                        (await (persist! run)))]
          (await (runner/enqueue-event-turn! config (body "waiting-first")
                   (fn [] (swap! started* conj "first") (:promise first-turn))))
          (let [second-result (runner/enqueue-event-turn! config (body "waiting-second")
                                (fn [] (swap! started* conj "second") nil))]
            ((:complete! first-turn) nil)
            (await (tick!))
            (test/is (= ["first"] @started*) "releasing the active slot cannot bypass pending admission")
            ((:complete! admission) nil)
            (await second-result)
            (await (wait-until! #(= ["first" "second"] @started*)))
            (test/is (= ["first" "second"] @started*)))))))))

(test/deftest ^:async full-queue-rejection-is-durable-before-its-failure-events
  (await
   (with-queue!
    (^:async fn [provider]
      (let [first-turn (deferred) second-turn (deferred) rejected "queue-rejected"]
        (try
          (await (runner/enqueue-event-turn! config (body "full-first") #(:promise first-turn)))
          (await (runner/enqueue-event-turn! config (body "full-second") #(:promise second-turn)))
          (let [result (await (outcome! #(runner/enqueue-event-turn! config (body rejected) (fn [] nil))))]
            (test/is (some? (:error result)))
            (test/is (= "failed" (:status (await (runs/get-run provider rejected)))))
            (test/is (nil? (:error (await (outcome! #(events/flush! rejected))))))
            (test/is (= ["event_turn_queue_rejected" "async_spawn_failed"]
                        (mapv :type (await (runs/events-since provider rejected nil))))))
          (finally
            ((:complete! first-turn) nil) ((:complete! second-turn) nil)
            (await (wait-until! #(zero? (:active (runner/event-turn-queue-snapshot))))))))))))

(test/deftest ^:async failed-initial-admission-neither-publishes-nor-leaks-a-queue-slot
  (await
   (with-queue!
    (^:async fn [_provider]
      (let [persist! events/persist-run! started* (atom []) id "admission-refused"]
        (with-redefs [events/persist-run!
                      (^:async fn [run]
                        (if (= id (:run_id run))
                          (throw (ex-info "disk refused" {:code "queue_admission_refused"}))
                          (await (persist! run))))]
          (let [result (await (outcome! #(runner/enqueue-event-turn! config (body id)
                                           (fn [] (swap! started* conj "refused")))))]
            (test/is (= "queue_admission_refused" (:code (ex-data (:error result)))))
            (test/is (nil? (get @state/runs* id)))
            (test/is (empty? @started*)))
          (await (runner/enqueue-event-turn! config (body "after-refusal")
                   (fn [] (swap! started* conj "next") nil)))
          (await (wait-until! #(zero? (:active (runner/event-turn-queue-snapshot)))))
          (test/is (= ["next"] @started*))))))))

(test/deftest ^:async failed-pending-admission-releases-only-its-reservation
  (await
   (with-queue!
    (^:async fn [_provider]
      (let [persist! events/persist-run! first-turn (deferred) reject-pending (deferred)
            started* (atom [])]
        (with-redefs [events/persist-run!
                      (^:async fn [run]
                        (when (= "pending-refused" (:run_id run))
                          (await (:promise reject-pending))
                          (throw (ex-info "pending write refused" {:code "pending_refused"})))
                        (await (persist! run)))]
          (try
            (await (runner/enqueue-event-turn! config (body "pending-owner")
                     (fn [] (swap! started* conj "owner") (:promise first-turn))))
            (let [pending-result (outcome! #(runner/enqueue-event-turn! config (body "pending-refused")
                                              (fn [] (swap! started* conj "refused"))))]
              ((:complete! reject-pending) nil)
              (test/is (= "pending_refused" (:code (ex-data (:error (await pending-result))))))
              (test/is (= ["pending-owner"] (:active-run-ids (runner/event-turn-queue-snapshot))))
              (test/is (empty? (:queued-run-ids (runner/event-turn-queue-snapshot))))
              (await (runner/enqueue-event-turn! config (body "pending-replacement")
                       (fn [] (swap! started* conj "replacement") nil)))
              ((:complete! first-turn) nil)
              (await (wait-until! #(zero? (:active (runner/event-turn-queue-snapshot)))))
              (test/is (= ["owner" "replacement"] @started*)))
            (finally ((:complete! first-turn) nil)))))))))

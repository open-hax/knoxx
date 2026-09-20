(ns knoxx.backend.mongo-run-events-e2e
  "Explicit native Mongo proof, excluded from the ordinary -test namespace discovery."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.mongo-run-native-fixture :as native]
            [knoxx.backend.extern.provider-recovery-fixture :as concurrent]
            [knoxx.backend.infra.stores.mongo-run-store :as store]
            [knoxx.backend.shape.session-persistence :as protocol]))

(def at "2026-09-12T12:00:00.000Z")
(def run {:run_id "native-run" :session_id "session" :conversation_id "conversation"
          :org_id "org" :user_id "user" :status "running" :created_at at :updated_at at})
(def event {:run_id "native-run" :session_id "session" :conversation_id "conversation"
            :event_id "native-event" :type "tool_update" :at at :payload {:domain/key :review/accepted}})

(defn- open [fixture clock owner]
  (store/create-mongo-run-store (:db fixture) {:clock! #(deref clock) :instance-id owner}))

(deftest ^:async actual-mongo-cas-restart-and-expiry
  (let [fixture (atom (await (native/open!))) clock (atom at)]
    (try
      (await (store/setup-indexes! (:db @fixture)))
      (let [provider-a (open @fixture clock "a") provider-b (open @fixture clock "b")]
        (await (protocol/put-run! provider-a run))
        (let [results (await (concurrent/settled
                             (mapv #(protocol/append-event! (if (even? %) provider-a provider-b)
                                                             (assoc event :event_id (str "event-" %))) (range 20))))]
          (is (every? #(= :fulfilled (:status %)) results))
          (is (= (set (range 1 21)) (set (map #(get-in % [:value :sequence]) results)))))
        (let [same (assoc event :event_id "same-id")
              results (await (concurrent/settled [(protocol/append-event! provider-a same)
                                                  (protocol/append-event! provider-b same)]))]
          (is (= [21 21] (mapv #(get-in % [:value :sequence]) results))))
        (let [results (await (concurrent/settled
                             [(protocol/append-event! provider-a (assoc event :event_id "conflict"))
                              (protocol/append-event! provider-b (assoc event :event_id "conflict" :type "different"))]))]
          (is (= 1 (count (filter #(= :fulfilled (:status %)) results))))
          (is (= [409] (mapv #(get-in % [:error :status]) (filter #(= :rejected (:status %)) results))))))
      (reset! fixture (await (native/restart! @fixture)))
      (let [reopened (open @fixture clock "restart")
            events (await (protocol/events-since reopened "native-run" nil))]
        (is (= (vec (range 1 23)) (mapv :sequence events)))
        (is (every? #(= :review/accepted (get-in % [:payload :domain/key])) events))
        (is (= "a" (:system_instance_id (await (protocol/get-run reopened "native-run")))))
        (is (= 21 (:sequence (await (protocol/append-event! reopened (assoc event :event_id "same-id"))))))
        (is (= 2 (count (await (protocol/events-since reopened "native-run" 20)))))
        (reset! clock "2026-09-12T14:00:00.000Z")
        (is (nil? (await (protocol/get-run reopened "native-run"))))
        (is (= [] (await (protocol/events-since reopened "native-run" nil))))
        (try (await (protocol/put-run! reopened (assoc run :org_id "another-org")))
             (is false "Expired identity must not be rebound")
             (catch :default error (is (= 409 (:status (ex-data error)))))))
      (finally (await (native/close! @fixture))))))

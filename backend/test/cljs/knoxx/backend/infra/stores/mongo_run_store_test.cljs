(ns knoxx.backend.infra.stores.mongo-run-store-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.mongo-run-store-fixture :as fixture]
            [knoxx.backend.extern.provider-recovery-fixture :as concurrent]
            [knoxx.backend.infra.stores.mongo-run-store :as store]
            [knoxx.backend.shape.run-directory :as directory]
            [knoxx.backend.shape.session-persistence :as protocol]))

(def at "2026-09-12T12:00:00.000Z")
(def run {:run_id "run" :session_id "session" :conversation_id "conversation"
          :org_id "org" :user_id "user" :status "running" :created_at at :updated_at at})
(def event {:run_id "run" :session_id "session" :conversation_id "conversation"
            :event_id "first" :type "tool_update" :at at :payload {:review/state :review/accepted}})

(defn- open [fixture clock]
  (store/create-mongo-run-store (:db fixture) {:clock! #(deref clock) :instance-id "instance"}))

(defn- ^:async refusal [operation]
  (try (await (operation)) nil (catch :default error (ex-data error))))

(deftest ^:async snapshot-lifecycle-retains-event-authority
  (let [db (fixture/create) provider (open db (atom at))]
    (await (store/setup-indexes! (:db db)))
    (is (satisfies? protocol/IRunEventStore provider))
    (is (= "run" (:run_id (await (protocol/put-run! provider run)))))
    (is (= ["run"] (mapv :run_id (await (protocol/list-active-runs provider "session")))))
    (await (protocol/append-event! provider event))
    (await (protocol/patch-run! provider "run" {:answer "kept"}))
    (is (= "completed" (:status (await (protocol/complete-run! provider "run" {:answer "done"})))))
    (is (= [] (await (protocol/list-active-runs provider "session"))))
    (is (= "done" (:answer (await (protocol/get-run provider "run")))))
    (is (= [1] (mapv :sequence (await (protocol/events-since provider "run" nil)))))
    (is (true? (await (protocol/delete-run! provider "run"))))
    (is (nil? (await (protocol/get-run provider "run"))))
    (is (= [] (await (protocol/events-since provider "run" nil))))
    (is (= 409 (:status (await (refusal #(protocol/put-run! provider (assoc run :org_id "other")))))))))

(deftest ^:async concurrent-writers-and-exact-retries-keep-contiguous-events
  (let [db (fixture/create) clock (atom at) provider-a (open db clock) provider-b (open db clock)]
    (await (protocol/put-run! provider-a run))
    (let [results (await (concurrent/settled [(protocol/append-event! provider-a event)
                                             (protocol/append-event! provider-b (assoc event :event_id "second"))]))]
      (is (every? #(= :fulfilled (:status %)) results))
      (is (= #{1 2} (set (map #(get-in % [:value :sequence]) results)))))
    (let [accepted (await (protocol/events-since (open db clock) "run" nil))]
      (is (= [1 2] (mapv :sequence accepted)))
      (is (= [:review/accepted :review/accepted] (mapv #(get-in % [:payload :review/state]) accepted)))
      (is (= (first accepted) (await (protocol/append-event! provider-b event))))
      (is (= accepted (await (protocol/events-since provider-a "run" 0))))
      (is (= [(second accepted)] (await (protocol/events-since provider-a "run" 1)))))
    (is (= 409 (:status (await (refusal #(protocol/append-event! provider-a (assoc event :type "changed")))))))
    (is (= 409 (:status (await (refusal #(protocol/append-event! provider-a (assoc event :event_id "cross" :session_id "foreign")))))))
    (is (= 409 (:status (await (refusal #(protocol/patch-run! provider-a "run" {:run_events []}))))))))

(deftest ^:async same-id-overlap-and-lost-acknowledgement-remain-idempotent
  (let [db (fixture/create) clock (atom at) provider-a (open db clock) provider-b (open db clock)]
    (await (protocol/put-run! provider-a run))
    (let [results (await (concurrent/settled [(protocol/append-event! provider-a event)
                                             (protocol/append-event! provider-b event)]))]
      (is (every? #(= :fulfilled (:status %)) results))
      (is (= [1 1] (mapv #(get-in % [:value :sequence]) results))))
    (reset! (:fail-after-commit? db) true)
    (try (await (protocol/append-event! provider-a (assoc event :event_id "lost-ack")))
         (is false "A lost acknowledgement must surface")
         (catch :default error (is (some? error))))
    (is (= 2 (:sequence (await (protocol/append-event! provider-b (assoc event :event_id "lost-ack"))))))
    (is (= [1 2] (mapv :sequence (await (protocol/events-since provider-a "run" nil)))))))

(deftest ^:async expiry-is-a-view-and-never-rebinds-identity
  (let [db (fixture/create) clock (atom at) provider (open db clock)]
    (await (protocol/put-run! provider run))
    (await (protocol/append-event! provider event))
    (is (not (contains? (get @(:documents db) "run") :expiresAt)) "TTL deletion must not erase identity tombstones")
    (reset! clock "2026-09-12T14:00:00.000Z")
    (is (nil? (await (protocol/get-run provider "run"))))
    (is (= [] (await (protocol/events-since provider "run" nil))))
    (is (= 404 (:status (await (refusal #(protocol/append-event! provider (assoc event :event_id "late")))))))
    (is (= 409 (:status (await (refusal #(protocol/put-run! provider (assoc run :user_id "different")))))))
    (is (= [1] (mapv :sequence (get-in (fixture/persisted-state db "run") [:events "run"]))))))

(deftest ^:async legacy-unordered-events-and-corrupt-snapshots-refuse
  (let [db (fixture/create) provider (open db (atom at))]
    (swap! (:documents db) assoc "run" (assoc run :run_events [{:type "old"}] :expiresAt (fixture/date "2026-09-12T14:00:00.000Z")))
    (is (= "run_events_migration_required" (:code (await (refusal #(protocol/get-run provider "run"))))))
    (swap! (:documents db) update "run" assoc :run_events [])
    (is (= "run" (:run_id (await (protocol/get-run provider "run")))))
    (await (protocol/append-event! provider event))
    (is (= 1 (:persistence_revision (get @(:documents db) "run"))))
    (swap! (:documents db) update "run" assoc :run_state_edn "{:broken true}")
    (is (= "run_store_corrupt" (:code (await (refusal #(protocol/get-run provider "run"))))))))

(deftest ^:async exact-retry-reasserts-journal-durability-without-changing-events
  (let [db (fixture/create) provider (open db (atom at))]
    (await (protocol/put-run! provider run))
    (let [accepted (await (protocol/append-event! provider event))
          previous-revision (:persistence_revision (get @(:documents db) "run"))]
      (is (= accepted (await (protocol/append-event! provider event))))
      (is (= (inc previous-revision) (:persistence_revision (get @(:documents db) "run"))))
      (is (every? #(= {:writeConcern {:w "majority" :j true}} %) @(:writes db)))
      (is (= [accepted] (await (protocol/events-since provider "run" nil)))))))

(deftest ^:async complete-snapshot-input-is-required
  (let [db (fixture/create) provider (open db (atom at))]
    (await (protocol/put-run! provider run))
    (let [original (get-in @(:documents db) ["run" :run_state_edn])]
      (doseq [encoded [(str original "\n{:ignored :tail}") (str original "\n}") "" nil]]
        (swap! (:documents db) assoc-in ["run" :run_state_edn] encoded)
        (is (= {:status 503 :code "run_store_corrupt"}
               (await (refusal #(protocol/get-run provider "run"))))))
      (swap! (:documents db) assoc-in ["run" :run_state_edn] (str "\n" original "\n"))
      (is (= "run" (:run_id (await (protocol/get-run provider "run"))))))))

(deftest ^:async event-history-is-not-trimmed-at-the-old-thousand-event-cap
  (let [db (fixture/create) provider (open db (atom at))]
    (await (protocol/put-run! provider run))
    (let [history (mapv #(assoc event :sequence % :event_id (str "seed-" %)) (range 1 1001))
          state (assoc-in (fixture/persisted-state db "run") [:events "run"] history)]
      (swap! (:documents db) update "run" assoc :run_state_edn (pr-str state)))
    (is (= 1001 (:sequence (await (protocol/append-event! provider event)))))
    (is (= 1001 (count (await (protocol/events-since provider "run" nil)))))
    (is (= "seed-1" (:event_id (first (await (protocol/events-since provider "run" nil))))))))

(deftest ^:async durable-directory-scopes-and-expiry
  (let [db (fixture/create) clock (atom at) provider (open db clock)]
    (await (protocol/put-run! provider run))
    (await (protocol/put-run! provider (assoc run :run_id "foreign" :org_id "foreign")))
    (is (= ["run"] (mapv :run_id (await (directory/list-runs (open db clock) {:org-id "org"})))) )
    (is (= #{"run" "foreign"} (set (map :run_id (await (directory/list-runs provider {:all? true}))))))
    (is (= 400 (:status (await (refusal #(directory/list-runs provider {}))))))
    (reset! clock "2026-09-12T14:00:00.000Z")
    (is (= [] (await (directory/list-runs provider {:all? true}))))))

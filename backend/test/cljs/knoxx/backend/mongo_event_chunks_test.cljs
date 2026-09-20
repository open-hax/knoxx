(ns knoxx.backend.mongo-event-chunks-test
  "Crash candidates, exact head identity and representation parity for bounded Mongo events."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.mongo-run-store :as snapshot]
            [knoxx.backend.extern.mongo-run-events :as native-events]
            [knoxx.backend.extern.mongo-run-store :as native]
            [knoxx.backend.extern.mongo-run-store-fixture :as fixture]
            [knoxx.backend.infra.stores.mongo-run-store :as mongo]
            [knoxx.backend.shape.session-persistence :as protocol]))

(def at "2026-09-12T12:00:00.000Z")
(def run {:run_id "run" :session_id "session" :conversation_id "conversation"
          :org_id "org" :user_id "user" :status "completed" :created_at at :updated_at at})
(def event {:run_id "run" :session_id "session" :conversation_id "conversation"
            :event_id "event" :type "tool_update" :at at :payload {:review/state :review/accepted}})

(defn- open [fixture clock]
  (mongo/create-mongo-run-store (:db fixture) {:clock! #(deref clock) :instance-id "instance"}))

(defn- ^:async refusal [f]
  (try (await (f)) nil (catch :default cause (ex-data cause))))

(deftest ^:async an-uncommitted-candidate-never-reserves-its-id-or-sequence
  (let [fixture (fixture/create) provider (open fixture (atom at))]
    (await (protocol/put-run! provider run))
    (await (native-events/prepare! (:db fixture) (assoc event :sequence 1) nil))
    (is (= [] (await (protocol/events-since provider "run" nil))))
    (await (protocol/append-event! provider (assoc event :event_id "other")))
    (let [accepted (await (protocol/append-event! provider (assoc event :payload {:different "lawful new payload"})))]
      (is (= 2 (:sequence accepted)) "The orphan's older sequence does not prove accepted membership")
      (is (= [1 2] (mapv :sequence (await (protocol/events-since provider "run" nil)))))
      (is (= 3 (count @(:headers fixture))))
      (is (= accepted (await (protocol/append-event! provider (assoc event :payload {:different "lawful new payload"}))))))
    (is (every? #(= "primary" (:readPreference %)) @(:reads fixture)))))

(deftest ^:async old-exact-retry-reacks-without-changing-expiry-snapshot-or-tail
  (let [fixture (fixture/create) clock (atom at) provider (open fixture clock)]
    (await (protocol/put-run! provider run))
    (let [first-event (await (protocol/append-event! provider event))]
      (reset! clock "2026-09-12T12:10:00.000Z")
      (await (protocol/append-event! provider (assoc event :event_id "later")))
      (let [before (get @(:documents fixture) "run") heads (count @(:headers fixture))]
        (reset! clock "2026-09-12T12:20:00.000Z")
        (is (= first-event (await (protocol/append-event! provider event))))
        (let [after (get @(:documents fixture) "run")]
          (is (= (inc (:persistence_revision before)) (:persistence_revision after)))
          (is (= (dissoc before :persistence_revision :persistence_token) (dissoc after :persistence_revision :persistence_token)))
          (is (= heads (count @(:headers fixture)))))
        (is (= at (:updated_at (await (protocol/get-run provider "run")))))
        (is (= "instance" (:system_instance_id (await (protocol/get-run provider "run")))))
        (reset! clock "2026-09-12T14:10:00.000Z")
        (is (= 404 (:status (await (refusal #(protocol/append-event! provider event))))))))))

(deftest ^:async identical-revision-with-a-different-committed-branch-loses-cas
  (let [fixture (fixture/create) provider (open fixture (atom at))]
    (await (protocol/put-run! provider run))
    (let [stale (await (native/read! (:db fixture) "run"))]
      (await (protocol/append-event! provider event))
      ;; Models a rolled-back unacknowledged revision later reused by a different branch.
      (swap! (:documents fixture) assoc-in ["run" :persistence_revision] (:revision stale))
      (is (false? (await (native/compare-and-swap! (:db fixture) "run" stale (:state stale) (:event-chain stale)))))
      (is (= [1] (mapv :sequence (await (protocol/events-since provider "run" nil))))))))

(deftest ^:async revisionless-legacy-migration-also-cas-compares-the-complete-observation
  (let [fixture (fixture/create)]
    (swap! (:documents fixture) assoc "run" (assoc run :expiresAt (fixture/date "2026-09-12T14:00:00.000Z")))
    (let [previous (await (native/read! (:db fixture) "run"))
          state (snapshot/restore previous "run")]
      (swap! (:documents fixture) assoc-in ["run" :answer] "concurrent legacy write")
      (is (false? (await (native/compare-and-swap! (:db fixture) "run" previous state {:tail nil :last-sequence 0}))))
      (is (= "concurrent legacy write" (get-in @(:documents fixture) ["run" :answer]))))))

(deftest ^:async unicode-fragments-are-lossless-and-corruption-is-not-replacement-text
  (let [fixture (fixture/create) provider (open fixture (atom at))
        text (str "\uD800" (apply str (repeat 20000 "🌱é")) "\uDC00")]
    (await (protocol/put-run! provider run))
    (let [accepted (await (protocol/append-event! provider (assoc event :payload {:text text})))]
      (is (> (count @(:fragments fixture)) 1))
      (is (= [accepted] (await (protocol/events-since provider "run" nil)))))
      (is (= text (get-in (first (await (protocol/events-since provider "run" nil))) [:payload :text])))
      (let [id (first (keys @(:fragments fixture)))]
        (swap! (:fragments fixture) update id assoc :data "AA==")
        (is (= "run_store_corrupt" (:code (await (refusal #(protocol/events-since provider "run" nil)))))))))

(deftest ^:async version-two-cannot-look-like-an-empty-event-history-to-an-old-writer
  (let [fixture (fixture/create) provider (open fixture (atom at))]
    (await (protocol/put-run! provider run))
    (await (protocol/append-event! provider event))
    (let [stored (fixture/persisted-state fixture "run")]
      (is (= :knoxx.run/v2 (:format stored)))
      (is (not (contains? stored :events)))
      (try (snapshot/validate-state! stored "run") (is false "Old full-state validation must refuse")
           (catch :default cause (is (= "run_store_corrupt" (:code (ex-data cause)))))))
    (await (protocol/delete-run! provider "run"))
    (is (= 404 (:status (await (refusal #(protocol/append-event! provider event))))))
    (await (protocol/put-run! provider run))
    (is (= [1] (mapv :sequence (await (protocol/events-since provider "run" nil)))))
    (is (= 2 (:sequence (await (protocol/append-event! provider (assoc event :event_id "new"))))))))

(deftest ^:async malformed-present-head-token-refuses-authority
  (let [fixture (fixture/create) provider (open fixture (atom at))]
    (await (protocol/put-run! provider run))
    (doseq [token [false nil "not-a-uuid"]]
      (swap! (:documents fixture) assoc-in ["run" :persistence_token] token)
      (is (= "run_store_corrupt" (:code (await (refusal #(protocol/get-run provider "run")))))))))

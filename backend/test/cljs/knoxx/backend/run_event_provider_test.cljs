(ns knoxx.backend.run-event-provider-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.clock :as clock]
            [knoxx.backend.extern.provider-recovery-fixture :as fixture]
            [knoxx.backend.infra.run-event-payload :as payload]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.clio-run-store :as clio]
            [knoxx.backend.infra.stores.openplanner-session-store :as archive]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.law.persistence-config :as config]
            [knoxx.backend.shape.session-persistence :as protocol]))

(def at "2026-09-12T12:00:00.000Z")
(def run {:run_id "run" :session_id "session" :conversation_id "conversation"
          :status "running" :created_at at :updated_at at})

(deftest ^:async identical-same-millisecond-occurrences-have-distinct-durable-identities
  (let [directory (fixture/temporary-directory)]
    (try
      (let [provider (clio/open! {:directory directory :clock! (constantly at) :instance-id "test"})
            occurrences (with-redefs [clock/instant-iso (constantly at)]
                          [(payload/tool-event-payload "run" "conversation" "session" "tool_update" {:text "same"})
                           (payload/tool-event-payload "run" "conversation" "session" "tool_update" {:text "same"})])]
        (is (= (apply dissoc (first occurrences) [:event_id]) (dissoc (second occurrences) :event_id)))
        (is (not= (:event_id (first occurrences)) (:event_id (second occurrences))))
        (await (protocol/put-run! provider run))
        (doseq [event occurrences] (await (protocol/append-event! provider event)))
        (is (= [1 2] (mapv :sequence (await (protocol/events-since provider "run" nil)))))
        (is (= 1 (:sequence (await (protocol/append-event! provider (first occurrences))))))
        (is (= 2 (count (await (protocol/events-since
                               (clio/open! {:directory directory :clock! (constantly at) :instance-id "restart"}) "run" nil))))))
      (finally (fixture/remove! directory)))))

(deftest unsupported-archive-cannot-be-installed-as-a-durable-run-authority
  (let [provider (archive/->OpenPlannerSessionStore {})]
    (is (not (satisfies? protocol/IRunEventStore provider)))
    (try (events/install! provider) (is false "Archive selection must refuse before clearing the active writer")
         (catch :default error (is (= {:status 503 :code "run_event_provider_unsupported"} (ex-data error)))))
    (try (config/selection! {:run-provider :openplanner}) (is false "Config must not advertise approximate ordering")
         (catch :default error (is (= "persistence_provider_unknown" (:code (ex-data error))))))))

(deftest ^:async missing-provider-cannot-acknowledge-a-run-snapshot
  (let [flushed? (atom false)]
    (with-redefs [registry/session-store* (atom nil)
                  events/flush! (fn [_] (reset! flushed? true))]
      (try (await (events/persist-run! run)) (is false "Missing persistence must refuse admission")
           (catch :default error
             (is (= {:status 503 :code "run_provider_unavailable"} (ex-data error)))))
      (is (false? @flushed?)))))

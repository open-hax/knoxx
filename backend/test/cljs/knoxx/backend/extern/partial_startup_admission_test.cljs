(ns knoxx.backend.extern.partial-startup-admission-test
  "Partial startup failures become conditionally failed facts, never phantom active work."
  (:require [cljs.test :as test]
            [knoxx.backend.domain.error-observatory :as errors]
            [knoxx.backend.extern.agent-turn-fixture :as fixture]
            [knoxx.backend.infra.agent.initial-admission :as initial]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.mongo-session-store :as threads]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.shape.session-persistence :as runs]
            [knoxx.backend.shape.startup-admission :as startup]))

(def ^:private seed {:run_id "seed" :session_id "seed-session" :conversation_id "seed-conversation"})
(defn- arguments [id session]
  [id session "startup-conversation" "2026-09-20T00:00:00.000Z" "owned-model" "direct" "off"
   nil {:org_id "owned-org" :user_id "owned-user"} [] {}])
(defn- ^:async start! [id session continuation]
  (await (initial/create-run! {:conversation-id "startup-conversation"} (arguments id session) continuation)))
(defn- ^:async refusal! [operation]
  (try (await (operation)) nil (catch :default error error)))

(defn- controls [phase lost-ack? failure]
  (let [run-store @registry/session-store* claim! startup/claim-startup! append! runs/append-event!]
    {:claim! (^:async fn [store record view]
               (let [target? (= phase (if (identical? store run-store) :run :thread))]
                 (when (and target? (not lost-ack?)) (throw failure))
                 (let [result (await (claim! store record view))]
                   (when target? (throw failure)) result)))
     :event! (^:async fn [store event]
               (when (= phase :event) (throw failure))
               (await (append! store event)))}))

(defn- ^:async verify-failure! [phase lost-ack?]
  (let [id (str "attempt-" (name phase) "-" lost-ack?) session (str "thread-" id)
        provider @registry/session-store* failure (ex-info "Owned private refusal" {:status 503 :code "owned_refusal"})
        ports (controls phase lost-ack? failure) model* (atom 0)]
    (with-redefs [startup/claim-startup! (:claim! ports) runs/append-event! (:event! ports)]
      (test/is (identical? failure (await (refusal! #(start! id session
                                                        (fn [] (if (= phase :continuation) (throw failure)
                                                                  (swap! model* inc)))))))))
    (test/is (zero? @model*))
    (test/is (= "failed" (:status (await (runs/get-run provider id)))))
    (test/is (empty? (await (runs/list-active-runs provider session))))
    (test/is (:can-send (threads/session-can-send? (await (threads/get-session session))))
             "No failed startup leaves a busy conversation")
    (test/is (= (if (= phase :continuation) ["run_started"] [])
                (mapv :type (await (runs/events-since provider id nil))))
             "Accepted event facts survive compensation")
    (events/install! provider)
    (await (start! (str id "-retry") session (fn [])))
    (test/is (= (str id "-retry") (:run_id (await (threads/get-session session)))))))

(test/deftest ^:async each-partial-admission-and-lost-ack-settles-before-retry
  (doseq [phase [:run :thread :event :continuation] lost-ack? [false true]]
    (await (fixture/with-run! seed #(verify-failure! phase lost-ack?)))))

(test/deftest ^:async old-run-or-active-thread-is-never-claimed-by-new-attempt
  (await (fixture/with-run!
          seed
          (^:async fn []
            (let [provider @registry/session-store* old (await (runs/get-run provider "seed"))
                  old-thread (await (threads/get-session "seed-session"))]
              (test/is (= "startup_admission_conflict"
                          (:code (ex-data (await (refusal! #(start! "seed" "seed-session" (fn []))))))))
              (test/is (= old (await (runs/get-run provider "seed"))))
              (test/is (= old-thread (await (threads/get-session "seed-session"))))
              (let [args (assoc (arguments "new-run" "seed-session") 2 "seed-conversation")]
                (test/is (= "startup_admission_conflict"
                            (:code (ex-data (await (refusal! #(initial/create-run! {} args))))))))
              (test/is (= old-thread (await (threads/get-session "seed-session")))))))))

(test/deftest ^:async unconfirmed-settlement-is-visible-with-original-error-preserved
  (await (fixture/with-run!
          seed
          (^:async fn []
            (let [provider @registry/session-store* settle! startup/settle-startup!
                  failure (ex-info "Original admission refusal" {:code "first"}) diagnostics* (atom [])]
              (with-redefs [startup/settle-startup!
                            (^:async fn [store record view]
                              (if (identical? provider store) (throw (ex-info "Storage unavailable" {}))
                                  (await (settle! store record view))))
                            errors/log-error! (fn [kind _ error] (swap! diagnostics* conj [kind (:code (ex-data error))]))]
                (test/is (identical? failure (await (refusal! #(start! "unconfirmed" "unconfirmed-thread" (fn [] (throw failure)))))))
                (test/is (= [[:agent-turn/startup-compensation-unconfirmed "startup_compensation_unconfirmed"]] @diagnostics*))
                (test/is (= "running" (:status (await (runs/get-run provider "unconfirmed"))))
                         "An unavailable provider is not reported as settled")
                (test/is (= "failed" (:status (await (threads/get-session "unconfirmed-thread")))))
                (test/is (:can-send (threads/session-can-send? (await (threads/get-session "unconfirmed-thread")))))))))))

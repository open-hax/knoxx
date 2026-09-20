(ns knoxx.backend.agent-run-persistence-test
  "Strict durable run admission and completion remain independent of OpenPlanner."
  (:require [cljs.test :as test]
            [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.extern.agent-turn-fixture :as fixture]
            [knoxx.backend.infra.agent.policy :as policy]
            [knoxx.backend.infra.agent.turn :as turns]
            [knoxx.backend.infra.openplanner.memory :as memory]
            [knoxx.backend.infra.run-event-payload :as payload]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.infra.stores.session-titles :as titles]
            [knoxx.backend.shape.session-persistence :as runs]))

(def ^:private coordinates
  {:run_id "persistence-fixture" :session_id "persistence-session"
   :conversation_id "persistence-conversation"})

(test/deftest ^:async rejected-run-admission-prevents-prompt-and-event-publication
  (await
   (fixture/with-run!
    coordinates
    (^:async fn []
      (let [failure (ex-info "Run ledger refused admission" {:status 503})
            called* (atom false)]
        (with-redefs [policy/enforce-chat-policy! (fn [_ _] true)
                      titles/maybe-prime-session-title! (fn [& _] nil)
                      turns/hydrate-and-materialize! (fn [& _] [nil nil [] nil])
                      events/persist-run! (^:async fn [_run] (throw failure))
                      turns/prompt-and-await! (fixture/prompt-stub #(reset! called* true))]
          (try
            (await (turns/send-agent-turn! {} {}
                     {:run-id "provider-refused-run" :session-id "provider-refused-session"
                      :conversation-id "provider-refused-conversation" :model "test-model" :message "Stop at admission."
                      :auth-context {:org-id "fixture-org" :user-id "fixture-user" :permissions ["agent.chat.use"]}}))
            (test/is false "A rejected durable snapshot cannot become an accepted prompt")
            (catch :default error (test/is (identical? failure error)))))
        (test/is (false? @called*))
        (test/is (nil? (get @state/runs* "provider-refused-run")))
        (let [durable-events (await (runs/events-since @registry/session-store* "provider-refused-run" nil))]
          (test/is (empty? durable-events))))))))

(test/deftest ^:async completed-run-and-final-event-survive-without-openplanner
  (await
   (fixture/with-run!
    coordinates
    (^:async fn []
      (let [id (:run_id coordinates)
            event (payload/tool-event-payload id (:conversation_id coordinates) (:session_id coordinates)
                                             "run_completed" {:status "completed"})]
        (state/append-run-event! id event)
        (state/update-run! id #(assoc % :status "completed" :answer "Persisted answer."))
        (await (memory/index-run-memory! {} (get @state/runs* id) (constantly []) (constantly [])))
        (let [stored (await (runs/get-run @registry/session-store* id))
              durable-events (await (runs/events-since @registry/session-store* id nil))]
          (test/is (= "completed" (:status stored)))
          (test/is (= "Persisted answer." (:answer stored)))
          (test/is (not-any? #(contains? stored %) [:events :run_events :sequence]))
          (test/is (= ["run_completed"] (mapv :type durable-events)))))))))
